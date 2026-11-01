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
    'addData'])

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
    if (x && x.__fluid) return `Dsl.fluid(${lit(x.__fluid)}, ${x.amount})`
    return no('fluid argument ' + JSON.stringify(x))
}
const num = x => {
    if (typeof x !== 'number' || !Number.isFinite(x)) return no('number argument ' + String(x))
    if (!Number.isInteger(x)) return no('non-integer number ' + x)
    return Math.abs(x) > 2147483647 ? x + 'L' : String(x)
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
        case 'EUt': return a.length === 1 ? `.EUt(${num(a[0])})` : no('EUt with amperage')
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
            if (!SUPPORTED.has(name)) no('builder method ' + name)
            return (...a) => { calls.push([name, a]); return p }
        } })
        return p
    }
    const gtceu = new Proxy({}, { get: (_, type) => id => recorderFor(String(type), id) })
    const event = new Proxy({ recipes: { gtceu } }, { get: (t, k) => k in t ? t[k] : no('event.' + String(k)) })
    const tierConsts = {}
    TIERS.forEach((t, i) => { tierConsts[t] = i })
    const ctx = {
        console: { log() {}, warn() {}, error() {} }, GTValues: { VA: VOLT, VHA: VOLT, V: VOLT, VN: TIERS, ...tierConsts }, ...tierConsts,
        Fluid: { of: (f, n) => ({ __fluid: String(f), amount: n }) },
        Item: { of: (i, n) => ({ __item: String(i), count: n === undefined ? 1 : n }) },
        InputItem: { of: i => typeof i === 'string' ? i : item(i) }, Ingredient: { of: i => i },
        CleanroomType: { CLEANROOM: 'cleanroom', STERILE_CLEANROOM: 'sterile' },
        Java: { loadClass: n => { const cls = String(n).split('.').pop(); return function (...args) { return { __new: cls.replace(/^\$/, ''), args } } } },
        ServerEvents: { recipes: fn => { ctx.__handlers.push(fn) } }, __handlers: []
    }
    const src = fs.readFileSync(file, 'utf8')
    const proxyCtx = new Proxy(ctx, { has: () => true, get: (t, k) => k in t ? t[k] : (typeof k === 'symbol' ? undefined : k in globalThis ? globalThis[k] : no('global ' + String(k))) })
    vm.runInContext(src, vm.createContext(proxyCtx), { filename: file })
    // the proxy above makes unknown globals throw, but `has: () => true` also hides real built-ins: re-run built-ins fine
    if (!ctx.__handlers.length) no('no ServerEvents.recipes handler')
    ctx.__handlers.forEach(fn => fn(event))
    return ops
}

const className = f => 'Gen' + path.basename(f, '.js').split(/[^a-zA-Z0-9]+/).map(w => w[0].toUpperCase() + w.slice(1)).join('')

function toJava(file, ops) {
    const cls = className(file)
    const chunks = []
    for (let i = 0; i < ops.length; i += 100) chunks.push(ops.slice(i, i + 100))
    const body = chunks.map((chunk, n) => `    private static void part${n}(Consumer<FinishedRecipe> out) {\n` +
        chunk.map(op => {
            const calls = op.calls.map(emit).join('\n                ')
            return `        Dsl.gt(out, ${lit(op.type)}, ${lit(op.id)})\n                ${calls}\n                .save();`
        }).join('\n') + '\n    }\n').join('\n')
    return `package com.af9.core.recipes.gen;

import com.af9.core.recipes.Dsl;

import net.minecraft.data.recipes.FinishedRecipe;

import java.util.function.Consumer;

/** Generated by tools/port/record.js from ${file.replace(root + path.sep, '')}; ${ops.length} recipes. Edit freely: the script is gone. */
public final class ${cls} {

    private ${cls}() {}

    public static void register(Consumer<FinishedRecipe> out) {
${chunks.map((_, n) => `        part${n}(out);`).join('\n')}
    }

${body}}
`
}

fs.mkdirSync(OUT, { recursive: true })
const done = []
const dupCheck = new Map()
for (const arg of process.argv.slice(2)) {
    const file = path.resolve(root, arg)
    try {
        const ops = runScript(file)
        const seen = new Set()
        ops.forEach(o => { const k = o.type + '/' + o.id; if (seen.has(k)) no('duplicate recipe id ' + k); seen.add(k) })
        fs.writeFileSync(path.join(OUT, className(file) + '.java'), toJava(file, ops))
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
