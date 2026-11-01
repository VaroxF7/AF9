#!/usr/bin/env node
// Runs KubeJS server scripts once under a recording stub and writes the recipes they make as Java
// (af9-core/.../recipes/gen/<Name>.java, built on recipes/Dsl). Scripts that use something the recorder cannot
// model (removals, shaped/custom recipes, lambdas, unknown globals) are listed and stay KubeJS until ported by hand.
//
//   node tools/port/record.js <script.js> [more.js ...]       (paths relative to the repo root)
//
// Writes the Java files and prints, per script, "ok N recipes" or "manual: <why>". It never deletes anything:
// the caller deletes a script after the Java for it compiled in CI.
'use strict'
const vm = require('vm'), fs = require('fs'), path = require('path')

const root = path.resolve(__dirname, '..', '..')
const OUT = path.join(root, 'af9-core/src/main/java/com/af9/core/recipes/gen')
const TIERS = ['ULV', 'LV', 'MV', 'HV', 'EV', 'IV', 'LuV', 'ZPM', 'UV', 'UHV', 'UEV', 'UIV', 'UXV', 'OpV', 'MAX']
const VOLT = [8, 32, 128, 512, 2048, 8192, 32768, 131072, 524288, 2097152, 8388608, 33554432, 134217728, 536870912, 2147483647]
const SUPPORTED = new Set(['itemInputs', 'notConsumable', 'itemOutputs', 'inputFluids', 'outputFluids', 'notConsumableFluid',
    'chancedOutput', 'chancedInput', 'circuit', 'duration', 'EUt', 'CWUt', 'cleanroom', 'blastFurnaceTemp', 'addCondition',
    'addData', 'dimension', 'stationResearch', 'scannerResearch'])

class Unsupported extends Error {}
const no = why => { throw new Unsupported(why) }

const flat = a => a.flatMap(x => Array.isArray(x) ? flat(x) : [x])
const lit = s => '"' + s.replace(/\\/g, '\\\\').replace(/"/g, '\\"') + '"'

function item(x) {
    if (typeof x === 'string') return x
    if (x && x.__item) return (x.count > 1 ? x.count + 'x ' : '') + x.__item
    return no('item argument ' + JSON.stringify(x))
}
function fluid(x) {
    if (typeof x === 'string') {
        const m = x.match(/^#(\S+) (\d+)$/)
        if (m) return `Dsl.fluidTag(${lit(m[1])}, ${m[2]})`
        const f = x.match(/^([a-z0-9_.-]+:[a-z0-9_\/.-]+) (\d+)$/)
        if (f) return `Dsl.fluid(${lit(f[1])}, ${f[2]})`
    }
    if (x && x.__fluid) return `Dsl.fluid(${lit(x.__fluid)}, ${x.amount})`
    return no('fluid argument ' + JSON.stringify(x))
}
const num = x => {
    if (typeof x === 'string' && /^-?\d+$/.test(x)) x = Number(x)
    if (typeof x !== 'number' || !Number.isFinite(x)) return no('number argument ' + String(x))
    if (!Number.isInteger(x)) return no('non-integer number ' + x)
    return Math.abs(x) > 2147483647 ? x + 'L' : String(x)
}

function research(a) {
    const r = a[0]
    if (!r || !r.kind) return no('research')
    const c = r.calls
    const get = n => c[n]
    const stack = get('researchStack')
    if (!stack) no('research without a stack')
    const id = get('researchId') ? lit(String(get('researchId')[0])) : 'null'
    const eu = get('EUt') ? get('EUt') : [-1]
    const eut = num(eu[0]), amps = eu.length > 1 ? num(eu[1]) : '1'
    if (r.kind === 'station') {
        const cw = get('CWUt') ? get('CWUt') : [-1]
        return `.stationResearch(${lit(item(stack[0]))}, ${id}, ${eut}, ${amps}, ${num(cw[0])}, ${cw.length > 1 ? num(cw[1]) : -1})`
    }
    const d = get('duration') ? num(get('duration')[0]) : -1
    return `.scannerResearch(${lit(item(stack[0]))}, ${id}, ${eut}, ${amps}, ${d})`
}

function emit(c) {            // one recorded call -> Java call text
    const [name, a] = c
    switch (name) {
        case 'itemInputs': case 'itemOutputs': return `.${name}(${flat(a).map(item).map(lit).join(', ')})`
        case 'notConsumable': return flat(a).map(x => `.notConsumable(${lit(item(x))})`).join('')
        case 'inputFluids': case 'outputFluids': return `.${name}(${flat(a).map(fluid).join(', ')})`
        case 'notConsumableFluid': return flat(a).map(x => `.notConsumableFluid(${fluid(x)})`).join('')
        case 'chancedOutput': case 'chancedInput': return `.${name}(${lit(item(a[0]))}, ${num(a[1])}, ${num(a[2] === undefined ? 0 : a[2])})`
        case 'circuit': case 'duration': case 'CWUt': case 'blastFurnaceTemp': return `.${name}(${num(a[0])})`
        case 'EUt': return a.length === 1 ? `.EUt(${num(a[0])})` : a.length === 2 ? `.EUt(${num(a[0])}, ${num(a[1])})` : no('EUt arguments')
        case 'dimension': return `.dimension(${lit(String(a[0]))})`
        case 'research': return research(a)
        case 'cleanroom': return `.cleanroom(${lit(String(a[0]))})`
        case 'addData': return typeof a[1] === 'number' ? `.addData(${lit(String(a[0]))}, ${num(a[1])})` : `.addData(${lit(String(a[0]))}, ${lit(String(a[1]))})`
        case 'addCondition':
            if (a[0] && a[0].__new === 'OxygenatedCondition') return `.oxygenated(${a[0].args[0]}, ${a[0].args[1]})`
            return no('condition ' + JSON.stringify(a[0]))
        default: return no('builder method ' + name)
    }
}

const unsupportedFn = n => () => no(n)

function runScript(file) {
    const ops = []
    const recorderFor = (type, id) => {
        if (typeof id !== 'string') no('recipe without a string id')
        const calls = []
        ops.push({ type, id, calls })
        const p = new Proxy({}, { get(_, name) {
            if (name === 'then' || name === Symbol.toPrimitive) return undefined
            name = String(name)
            if (!SUPPORTED.has(name.replace(/\(.*$/, ''))) no('builder method ' + name)
            const base = name.replace(/\(.*$/, '')
            if (base === 'stationResearch' || base === 'scannerResearch') {
                return fn => {
                    const rc = {}
                    const rb = new Proxy({}, { get: (_, n) => (...x) => { rc[String(n)] = x; return rb } })
                    fn(rb)
                    calls.push(['research', [{ kind: base === 'stationResearch' ? 'station' : 'scanner', calls: rc }]])
                    return p
                }
            }
            return (...a) => { calls.push([name, a]); return p }
        } })
        return p
    }
    const gtceu = new Proxy({}, { get: (_, type) => id => recorderFor(String(type), id) })
    const removeOp = f => {
        if (!f || typeof f !== 'object') no('remove filter ' + String(f))
        const keys = Object.keys(f)
        const out = { removal: true, parts: [] }
        const list = v => (Array.isArray(v) ? v : [v]).map(x => { if (typeof x !== 'string') no('remove filter value ' + String(x)); return x })
        keys.forEach(k => {
            const v = f[k]
            if (k === 'id' && Object.prototype.toString.call(v) === '[object RegExp]') out.parts.push(['idRegex', [v.source]])
            else if (['id', 'mod', 'type', 'output', 'input'].includes(k)) out.parts.push([k, list(v)])
            else no('remove filter key ' + k)
        })
        if (!out.parts.length) no('empty remove filter')
        ops.push(out)
    }
    // vanilla-style recipes become JSON the Java side feeds into the same recipe pack (Dsl.json)
    let autoId = 0
    const ing = x => {
        if (typeof x === 'string') {
            if (/^\d+x /.test(x)) no('counted ingredient ' + x)
            return x.startsWith('#') ? { tag: x.slice(1) } : { item: x }
        }
        if (x && x.__item) { if (x.count !== 1) no('counted ingredient'); return { item: x.__item } }
        return no('ingredient ' + JSON.stringify(x))
    }
    const stackJson = x => {
        if (typeof x === 'string') { const m = x.match(/^(\d+)x (.+)$/); return m ? { item: m[2], count: Number(m[1]) } : { item: x } }
        if (x && x.__item) return x.count > 1 ? { item: x.__item, count: x.count } : { item: x.__item }
        return no('result ' + JSON.stringify(x))
    }
    const jsonOp = (json, name) => {
        const op = { json: true, id: null, body: json, hint: name }
        ops.push(op)
        return { id(i) { op.id = String(i); return this } }
    }
    const shaped = (out, pattern, keys) => {
        const key = {}
        Object.keys(keys).forEach(k => { key[k] = ing(keys[k]) })
        return jsonOp({ type: 'minecraft:crafting_shaped', pattern, key, result: stackJson(out) }, out)
    }
    const shapeless = (out, ins) => jsonOp({ type: 'minecraft:crafting_shapeless', ingredients: flat([ins]).map(ing), result: stackJson(out) }, out)
    const smelting = (out, input) => jsonOp({ type: 'minecraft:smelting', ingredient: ing(input), result: stackJson(out).item, experience: 0, cookingtime: 200 }, out)
    const custom = json => jsonOp(JSON.parse(JSON.stringify(json)), json && json.type)
    const event = new Proxy({ recipes: { gtceu }, remove: removeOp, shaped, shapeless, smelting, custom }, { get: (t, k) => k in t ? t[k] : no('event.' + String(k)) })
    const tierConsts = {}
    TIERS.forEach((t, i) => { tierConsts[t] = i })
    const ctx = {
        console: { log() {}, warn() {}, error() {} }, GTValues: { VA: VOLT, VHA: VOLT, V: VOLT, VN: TIERS, ...tierConsts }, ...tierConsts,
        Fluid: { of: (f, n) => ({ __fluid: String(f), amount: n }) },
        Item: { exists: () => true, of: (i, n) => ({ __item: String(i), count: n === undefined ? 1 : n, isEmpty: () => false }) },
        InputItem: { exists: () => true, of: i => typeof i === 'string' ? i : item(i) }, Ingredient: { of: i => i },
        CleanroomType: { CLEANROOM: 'cleanroom', STERILE_CLEANROOM: 'sterile' },
        Java: { loadClass: n => { const cls = String(n).split('.').pop(); return function (...args) { return { __new: cls.replace(/^\$/, ''), args } } } },
        Platform: { isLoaded: m => { ctx.__mods.add(String(m)); return true } }, __mods: new Set(),
        ServerEvents: { recipes: fn => { ctx.__handlers.push(fn) }, tags: (kind, fn) => { ctx.__tagHandlers.push([String(kind), fn]) } }, __handlers: [], __tagHandlers: []
    }
    const src = fs.readFileSync(file, 'utf8')
    const proxyCtx = new Proxy(ctx, { has: () => true, get: (t, k) => k in t ? t[k] : (typeof k === 'symbol' ? undefined : k in globalThis ? globalThis[k] : no('global ' + String(k))) })
    vm.runInContext(src, vm.createContext(proxyCtx), { filename: file })
    // the proxy above makes unknown globals throw, but `has: () => true` also hides real built-ins: re-run built-ins fine
    if (!ctx.__handlers.length && !ctx.__tagHandlers.length) no('no ServerEvents.recipes or tags handler')
    ctx.__handlers.forEach(fn => fn(event))
    const tags = []
    ctx.__tagHandlers.forEach(([kind, fn]) => {
        if (!['item', 'fluid', 'block'].includes(kind)) no('tags of ' + kind)
        fn({ removeAll(tag) { tags.push([kind, String(tag), null]) }, add(tag, values) {
            if (typeof tag !== 'string') no('tag ' + String(tag))
            tags.push([kind, tag, flat([values]).map(v => { if (typeof v !== 'string') no('tag value ' + String(v)); return v })])
        } })
    })
    ops.tags = tags
    if (ctx.__out !== undefined) console.log('OUT ' + JSON.stringify(ctx.__out))
    return ops
}

const className = f => 'Gen' + path.basename(f, '.js').split(/[^a-zA-Z0-9]+/).map(w => w[0].toUpperCase() + w.slice(1)).join('')

function toJava(file, allOps) {
    let ops = allOps
    const cls = className(file)
    const removals = ops.filter(o => o.removal)
    ops = ops.filter(o => !o.removal)
    let autoN = 0
    ops.forEach(o => { if (o.json && !o.id) { const h = typeof o.hint === 'string' ? o.hint : (o.hint && o.hint.__item) || 'recipe'; o.id = `af9:auto/${h.replace(/^\d+x /, '').replace(/[^a-z0-9_]/gi, '_').toLowerCase()}_${++autoN}` } })
    const chunks = []
    for (let i = 0; i < ops.length; i += 40) chunks.push(ops.slice(i, i + 40))
    const body = chunks.map((chunk, n) => `    private static void part${n}(Consumer<FinishedRecipe> out) {\n` +
        chunk.map(op => {
            if (op.json) return `        Dsl.json(out, ${lit(op.id)}, ${lit(JSON.stringify(op.body))});`
            const calls = op.calls.map(emit).join('\n                ')
            return `        try {\n            Dsl.gt(out, ${lit(op.type)}, ${lit(op.id)})\n                    ${calls.split('\n                ').join('\n                    ')}\n                    .save();\n        } catch (RuntimeException e) {\n            Dsl.fail(${lit(op.id)}, e);\n        }`
        }).join('\n') + '\n    }\n').join('\n')
    return `package com.af9.core.recipes.gen;

import com.af9.core.recipes.Dsl;
import com.af9.core.recipes.Removals;

import net.minecraft.data.recipes.FinishedRecipe;

import java.util.function.Consumer;

/** Generated by tools/port/record.js from ${file.replace(root + path.sep, '')}; ${ops.length} recipes. Edit freely: the script is gone. */
public final class ${cls} {

    private ${cls}() {}

    public static void register(Consumer<FinishedRecipe> out) {
        removals();
${chunks.map((_, n) => `        part${n}(out);`).join('\n')}
    }

${body}
    /** The removals the script made (see Removals): the recipes of other mods / the base pack that this theme replaces. */
    private static void removals() {
${removals.map(r => `        Removals.filter()${r.parts.map(([k, v]) => `.${k}(${v.map(lit).join(', ')})`).join('')}.remove();`).join('\n')}
    }
}
`
}

const RES = path.join(root, 'af9-core/src/main/resources')
// tags: data/<ns>/tags/<items|fluids|blocks>/<path>.json, merged with what is already there (several scripts add to one tag)
function writeTags(tags) {
    tags.forEach(([kind, tag, values]) => {
        const [ns, p] = tag.includes(':') ? tag.split(':') : ['minecraft', tag]
        const f = path.join(RES, 'data', ns, 'tags', kind + 's', p + '.json')
        fs.mkdirSync(path.dirname(f), { recursive: true })
        let have = fs.existsSync(f) ? JSON.parse(fs.readFileSync(f, 'utf8')) : { replace: false, values: [] }
        if (values === null) { fs.writeFileSync(f, JSON.stringify({ replace: true, values: [] }, null, 2) + '\n'); return }   // event.removeAll: the tag starts again from what follows
        const idOf = v => typeof v === 'string' ? v : v.id
        values.forEach(v => { if (!have.values.some(x => idOf(x) === v)) have.values.push({ id: v, required: false }) })
        fs.writeFileSync(f, JSON.stringify(have, null, 2) + '\n')
    })
}

fs.mkdirSync(OUT, { recursive: true })
const done = []
const dupCheck = new Map()
for (const arg of process.argv.slice(2)) {
    const file = path.resolve(root, arg)
    try {
        const ops = runScript(file)
        const seen = new Set()
        ops.filter(o => !o.removal).forEach(o => { const k = o.type + '/' + o.id; if (seen.has(k)) no('duplicate recipe id ' + k); seen.add(k) })
        fs.writeFileSync(path.join(OUT, className(file) + '.java'), toJava(file, ops))
        writeTags(ops.tags)
        console.log(`ok ${ops.length} recipes: ${arg}`)
        done.push(className(file))
    } catch (e) {
        if (e instanceof Unsupported) console.log(`manual (${e.message}): ${arg}`)
        else console.log(`manual (error ${e.message}): ${arg}`)
    }
}

// GenAll lists every generated class, so a new one needs no hand-wiring
const names = fs.readdirSync(OUT).filter(f => /^Gen.*\.java$/.test(f) && f !== 'GenAll.java').map(f => f.replace('.java', '')).sort()
fs.writeFileSync(path.join(OUT, 'GenAll.java'), `package com.af9.core.recipes.gen;

import net.minecraft.data.recipes.FinishedRecipe;

import java.util.function.Consumer;

/** Registers every generated recipe class (written by tools/port/record.js). */
public final class GenAll {

    private GenAll() {}

    public static void register(Consumer<FinishedRecipe> out) {
${names.map(n => `        ${n}.register(out);`).join('\n')}
    }
}
`)
