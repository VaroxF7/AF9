#!/usr/bin/env node
// AF9 lint for the KubeJS scripts: loads kubejs/startup_scripts and kubejs/server_scripts with stubs (nothing of GT or
// Minecraft runs), records what they register and which recipes they make, and checks it.
//
//   node tools/lint/scripts.js [repo root] [--json]
//
// Checks (each finding is one line: LEVEL code message):
//   S1  a script threw while loading (a typo, an undefined name, a stub that is missing: see tools/lint/README.md)
//   S2  an item, block, fluid, material, machine or recipe type registered twice (the second one would fail or replace the first)
//   R1  duplicate recipe id within a recipe type
//   R2  a recipe over the slots of its machine (items / fluids in and out, the not-consumed ones and circuits count)
//   R3  an item, block or fluid id nobody defines: kubejs: not registered, gtceu: neither an AF9 material nor a name
//       in GT's lists (tools/lint/data), af9: not an item of AF9 Core
//   R4  a recipe type that is neither AF9's nor GT's
//   R5  an AF9 material, fluid or kubejs item a recipe takes that no recipe makes (nor a tag or a loot source)
//   R6  an AF9 material or kubejs item registered that no recipe makes or takes (dead content)
//   R13 a furnace recipe (EBF, boule melting, fab calcination / CVD / crystal growth) without blastFurnaceTemp()
//   M1  multiblock pattern: aisles / rows of different size, a pattern character without `where`, a `where` that is
//       never used, no or several controllers, a part with a minimum or an exact count (AF9 rule: maximums only)
//   M2  multiblock: a recipe type that does not exist, a machine without tooltips keys in the lang files
//   L1  a translatable key (tooltips of machines and items) missing from every lang file
//
// Needs Node 18+. GT's names come from tools/lint/data (made from GT's sources, see the README there).
'use strict'
const vm = require('vm'), fs = require('fs'), path = require('path')

const args = process.argv.slice(2)
const root = path.resolve(args.find(a => !a.startsWith('--')) || '.')
const asJson = args.includes('--json')
const dump = args.includes('--dump')   // every recipe as one JSON line (for ad-hoc analysis)

const read = f => fs.readFileSync(f, 'utf8')
const lines = f => fs.existsSync(f) ? read(f).split('\n').map(s => s.trim()).filter(Boolean) : []
const DATA = path.join(__dirname, 'data')
const gtMaterials = new Set(lines(path.join(DATA, 'gt-materials.txt')))
const gtNames = new Set(lines(path.join(DATA, 'gt-names.txt')))
const gtPatterns = lines(path.join(DATA, 'gt-patterns.txt')).filter(l => !l.startsWith('#')).map(r => new RegExp('^(?:' + r + ')$'))
const gtSlots = new Map(lines(path.join(DATA, 'gt-recipe-slots.txt')).map(l => l.split(' ')).map(([n, ...v]) => [n, v.map(Number)]))
const gtTypes = new Set(lines(path.join(DATA, 'gt-recipe-types.txt')))
// what the base pack (ATM9's kubejs, which this repo is laid over) provides: items, recipe types, GT names that GT's lists lack
const packIds = new Set(lines(path.join(DATA, 'pack.txt')))
// things that need no recipe to make them (Java makes them, worldgen, loot, quests): ids, `*` as a wildcard
const globs = lines(path.join(DATA, 'sources.txt')).map(g => new RegExp('^' + g.replace(/[.+?^${}()|[\]\\]/g, '\\$&').replace(/\*/g, '.*') + '$'))
const isSource = id => globs.some(r => r.test(id))

const VOLT = [8, 32, 128, 512, 2048, 8192, 32768, 131072, 524288, 2097152, 8388608, 33554432, 134217728, 536870912,
    2147483647]
const TIERS = ['ULV', 'LV', 'MV', 'HV', 'EV', 'IV', 'LuV', 'ZPM', 'UV', 'UHV', 'UEV', 'UIV', 'UXV', 'OpV', 'MAX']
const GTValues = { VA: VOLT, V: VOLT, VN: TIERS, VNF: TIERS, VLVH: TIERS, VLVT: TIERS }
TIERS.forEach((t, i) => { GTValues[t] = i })
const TIER_PREFIX = TIERS.map(t => t.toLowerCase())

// what an AF9 Core item is called: parsed from the Java sources
function javaItems() {
    const ids = new Set()
    const walk = d => fs.existsSync(d) ? fs.readdirSync(d).flatMap(f => {
        const p = path.join(d, f)
        return fs.statSync(p).isDirectory() ? walk(p) : [p]
    }) : []
    const src = walk(path.join(root, 'af9-core/src/main/java')).filter(f => f.endsWith('.java'))
    src.forEach(f => {
        const t = read(f)
        for (const m of t.matchAll(/(?:ITEMS|BLOCKS)\.register\("([a-z0-9_]+)"/g)) ids.add(m[1])
    })
    // ComputeCard: af9:<constant lower case>_card
    const card = path.join(root, 'af9-core/src/main/java/com/af9/core/compute/ComputeCard.java')
    if (fs.existsSync(card)) {
        for (const m of read(card).matchAll(/^\s+([A-Z_]+)\((\d+), Kind\./gm)) ids.add(m[1].toLowerCase() + '_card')
    }
    return ids
}
const af9Items = javaItems()
// block / item models of AF9 Core that exist as files (af9:<id> items of blocks registered otherwise)
const walkDir = d => fs.existsSync(d) ? fs.readdirSync(d).flatMap(f => {
    const p = path.join(d, f)
    return fs.statSync(p).isDirectory() ? walkDir(p) : [p]
}) : []
walkDir(path.join(root, 'af9-core/src/main/resources/assets/af9/models/item')).forEach(f =>
    af9Items.add(path.basename(f, '.json')))

// ---- stubs ---------------------------------------------------------------------------------------------------------
const permissive = () => new Proxy(function () {}, {
    get: (_, k) => k === Symbol.toPrimitive ? () => 'x' : k === 'then' ? undefined : permissive(),
    apply: () => permissive(), construct: () => permissive()
})
// a stand-in that knows its name: PartAbility.IMPORT_ITEMS and $Class.CONSTANT stay distinguishable (String() gives the path)
const named = n => new Proxy(function () {}, {
    get: (_, k) => k === Symbol.toPrimitive ? () => n : k === 'then' ? undefined : k === 'toString' ? () => n : named(`${n}.${String(k)}`),
    apply: () => named(`${n}()`), construct: () => named(`new ${n}`)
})
const flat = a => a.flatMap(x => Array.isArray(x) ? flat(x) : [x])
const parseId = s => {
    if (typeof s !== 'string') return null
    const m = s.match(/^(?:\d+x\s+)?(#?[a-z0-9_.-]+:[a-z0-9_/.-]+)/)
    return m ? m[1] : null
}

const state = {
    recipes: [], items: new Map(), materials: new Map(), recipeTypes: new Map(), machines: [], errors: [],
    startup: [], handlers: [], translatables: new Set(), currentFile: ''
}
const findings = []
const foreign = new Set()   // files of the base pack (they carry AllTheMods' licence header): not AF9's, checked only for the record
const report = (level, code, msg, where) => {
    if (where && code !== 'F1' && [...foreign].some(f => where.startsWith(f)) && level !== 'INFO') { level = 'INFO'; msg = `(AllTheMods file) ${msg}` }
    findings.push({ level, code, msg, where })
}

// a chainable recorder: every call returns itself, `record` sees (name, args)
const recorder = record => {
    const p = new Proxy({}, {
        get(_, name) {
            if (name === 'then' || name === Symbol.toPrimitive) return undefined
            return (...a) => { record(String(name), a); return p }
        }
    })
    return p
}

function recipeBuilder(type, id) {
    const rec = { type, id, file: state.currentFile, itemIn: [], itemOut: [], fluidIn: [], fluidOut: [], circuits: 0,
        notConsumed: 0, calls: {}, fluidAmounts: [], chances: [], itemQty: new Map() }
    state.recipes.push(rec)
    const ids = x => typeof x === 'string' ? parseId(x) : x && x.fluid ? x.fluid : null
    const amount = x => { if (x && x.fluid) rec.fluidAmounts.push([x.fluid, x.amount]) }
    // how many of an item a recipe takes ('4x ns:id'; 1 otherwise): for the conflict check R7
    const qty = x => {
        const id = ids(x)
        if (!id) return
        const m = typeof x === 'string' ? x.match(/^(\d+)x\s/) : null
        rec.itemQty.set(id, (rec.itemQty.get(id) || 0) + (m ? Number(m[1]) : 1))
    }
    return recorder((name, a) => {
        const items = flat(a)
        rec.calls[name] = a
        if (/Fluids?$/.test(name) || name === 'chancedFluidOutput') items.forEach(amount)
        if (name === 'chancedOutput' || name === 'chancedFluidOutput') rec.chances.push(a[1])
        if (name === 'chancedOutput') (rec.chanceItems || (rec.chanceItems = [])).push(ids(a[0]))
        switch (name) {
            case 'itemInputs': items.forEach(i => { rec.itemIn.push(ids(i)); qty(i) }); break
            case 'notConsumable': items.forEach(i => { rec.itemIn.push(ids(i)); rec.notConsumed++; qty(i) }); break
            case 'circuit': rec.itemIn.push('circuit:' + a[0]); rec.circuits++; rec.itemQty.set('circuit:' + a[0], 1); break
            case 'itemOutputs': items.forEach(i => rec.itemOut.push(ids(i))); break
            case 'chancedOutput': rec.itemOut.push(ids(a[0])); break
            case 'inputFluids': items.forEach(i => rec.fluidIn.push(ids(i))); break
            case 'notConsumableFluid': items.forEach(i => { rec.fluidIn.push(ids(i)); rec.notConsumed++ }); break
            case 'outputFluids': items.forEach(i => rec.fluidOut.push(ids(i))); break
            case 'chancedFluidOutput': rec.fluidOut.push(ids(a[0])); break
            default: break
        }
    })
}

// crafting-table recipes: recorded as recipes of their own type (no slot rules), so what they make and take counts as used
let craftingCount = 0
function crafting(type, out, ins) {
    const ingredient = x => typeof x === 'string' ? parseId(x) : x && typeof x === 'object' ? (x.item || (x.tag ? '#' + x.tag : null)) : null
    const rec = { type, id: `auto#${++craftingCount}`, file: state.currentFile, itemIn: flat([ins]).map(ingredient).filter(Boolean),
        itemOut: [parseId(typeof out === 'string' ? out : out && out.item)].filter(Boolean), fluidIn: [], fluidOut: [], circuits: 0, notConsumed: 0,
        calls: {}, fluidAmounts: [], chances: [], itemQty: new Map() }
    state.recipes.push(rec)
    return { id(i) { rec.id = String(i); return this } }
}

const gtceu = new Proxy({}, { get: (_, type) => id => recipeBuilder(String(type), id) })
const event = {
    recipes: { gtceu, minecraft: new Proxy({}, { get: () => () => recorder(() => {}) }) },
    shaped: (out, pattern, keys) => crafting('crafting_shaped', out, Object.values(keys || {})),
    shapeless: (out, ins) => crafting('crafting_shapeless', out, ins), smelting: () => recorder(() => {}),
    remove() {}, replaceInput() {}, replaceOutput() {}, forEachRecipe() {}, custom: () => recorder(() => {}),
    add() {}, get: () => ({ add() {} })
}

// predicates and the pattern builder: record what a pattern asks for
function mkPred(kind, arg) {
    const t = { kind, arg, ors: [], max: null, min: null, exact: null, __af9pred: true }
    const proxy = new Proxy(t, {
        get(o, k) {
            if (k in o) return o[k]
            if (k === 'or') return other => { o.ors.push(other); return proxy }
            if (k === 'setMaxGlobalLimited' || k === 'setMaxLayerLimited') return n => { o.max = n; return proxy }
            if (k === 'setMinGlobalLimited' || k === 'setMinLayerLimited') return n => { o.min = n; return proxy }
            if (k === 'setExactLimit') return n => { o.exact = n; return proxy }
            return () => proxy
        }
    })
    return proxy
}
const PredicatesBase = {
    blocks: (...a) => mkPred('blocks', flat(a).map(String).join(',')),
    abilities: a => mkPred('ability', String(a)),
    // GT's own helpers, kept as plain predicates
    controller: p => mkPred('controller', p),
    air: () => mkPred('air', ''), any: () => mkPred('any', ''), machines: (...a) => mkPred('machines', a.join(',')),
    autoAbilities: (...a) => mkPred('auto', a.join(',')), frames: () => mkPred('frames', ''), fluids: () => mkPred('fluids', ''),
    heatingCoils: () => mkPred('coils', ''), states: () => mkPred('states', ''), custom: () => mkPred('custom', '')
}
// GT's Java overloads are called by name with the signature ('autoAbilities(boolean,boolean)'): drop the signature
const Predicates = new Proxy(PredicatesBase, { get: (t, k) => t[String(k).split('(')[0]] || (() => mkPred('other', String(k))) })
function FactoryBlockPatternStart() {
    const pat = { aisles: [], where: {}, repeat: [] }
    const b = {
        aisle: (...rows) => { pat.aisles.push(rows); pat.repeat.push(null); return b },
        setRepeatable: (...n) => { pat.repeat[pat.repeat.length - 1] = n; return b },
        where: (ch, p) => { pat.where[ch] = p; return b },
        build: () => pat
    }
    return b
}
const FactoryBlockPattern = { start: () => FactoryBlockPatternStart() }

// af9-core's OreCatalog (the ores GT has, by microverse tier): a few real ones, so the recipes made from it are linted
const oreCatalog = {
    materials: tier => ({ 1: ['iron', 'copper'], 2: ['gold'], 3: ['tungstate'], 4: ['pitchblende'] })[tier] || [],
    ore: material => 'gtceu:raw_' + material
}

const ctx = {
    console, GTValues, Predicates, FactoryBlockPattern, GTCEu: { id: x => 'gtceu:' + x, MOD_ID: 'gtceu' },
    Fluid: { of: (fluid, amount) => ({ fluid, amount }) }, Item: { of: s => ({ item: s }), exists: () => true, getBlock: () => ({}) },
    Block: { getBlock: () => ({}) }, CleanroomType: { CLEANROOM: 'cleanroom', STERILE_CLEANROOM: 'sterile' },
    Ingredient: { of: s => s }, Platform: { isLoaded: () => true },
    Component: { translatable: (k, ...a) => { state.translatables.add(k); return { key: k } }, literal: k => k },
    Java: { loadClass: n => String(n).endsWith('.OreCatalog') ? oreCatalog : named(String(n).split('.').pop()) },
    JsonIO: { read: () => ({}) },
    GuiTextures: permissive(), FillDirection: permissive(), RotationState: permissive(),
    GTRecipeModifiers: permissive(), GTMaterialIconSet: permissive(), GTMaterialFlags: permissive(),
    PropertyKey: permissive(), GTSoundEntries: permissive(), GTCEuServerEvents: permissive(),
    PartAbility: named('PartAbility'), GTBlocks: permissive(), GTMachines: permissive(), GTRecipeCategories: permissive(),
    GCYMBlocks: permissive(), RecipeCapability: permissive(), GTResearchManager: permissive(),
    ChanceLogic: permissive(), GTMaterials: permissive(), GTRecipeTypes: { get: x => x, DUMMY_RECIPES: 'dummy_recipes' },
    GTMaterialRegistry: permissive(), Utils: { newList: () => [], newMap: () => ({}) }, global: {},
    ServerEvents: {
        recipes: fn => state.handlers.push([state.currentFile, fn]), tags() {}, highPriorityData() {},
        lowPriorityData() {}, loaded() {}
    },
    ForgeEvents: permissive(), NetworkEvents: permissive(), LevelEvents: permissive(), PlayerEvents: permissive(),
    BlockEvents: permissive(), ItemEvents: permissive(), EntityEvents: permissive(), ClientEvents: permissive(),
    FTBQuestsEvents: permissive(), JEIEvents: permissive(), RecipeViewerEvents: permissive()
}
const startup = kind => (type, fn) => {
    if (typeof type === 'function') { fn = type; type = kind === 'startup' ? 'unknown' : 'gtceu:unknown' }
    state.startup.push([kind, type, fn, state.currentFile])
}
ctx.StartupEvents = { registry: startup('startup'), modifyCreativeTab() {}, postInit() {}, init() {} }
ctx.GTCEuStartupEvents = { registry: startup('gtceu'), materialModification() {}, craftingComponents() {},
    modifyMaterial() {}, postMaterialRegistry() {} }
const vmctx = vm.createContext(ctx)

// S3: KubeJS runs the scripts on Rhino, which keeps a `const` declared in a loop's body at the value of the first pass
// (Node, which runs them here, gives every pass its own), so the script would work in this linter and fail in the game.
// Returns the lines of such declarations: a `const` inside the braces of a for / while loop, not inside a function
// that starts in the loop (a callback has its own scope each call).
function loopConsts(source) {
    const lines = []
    const blocks = []           // 'loop', 'function' or 'block' for every open brace
    let pendingLoop = -1        // paren depth at which a loop's header closes, -1 none
    let parens = 0, line = 1, i = 0
    let next = 'block'          // what the next `{` opens
    const skipTo = (end, escapes) => {
        i++
        while (i < source.length && !source.startsWith(end, i)) {
            if (source[i] === '\n') line++
            if (escapes && source[i] === '\\') i++
            i++
        }
        i += end.length
    }
    while (i < source.length) {
        const c = source[i]
        if (c === '\n') { line++; i++; continue }
        if (source.startsWith('//', i)) { while (i < source.length && source[i] !== '\n') i++; continue }
        if (source.startsWith('/*', i)) { i++; skipTo('*/', false); continue }
        if (c === '\'' || c === '"' || c === '`') { skipTo(c, true); continue }
        if (/[A-Za-z_$]/.test(c)) {
            let j = i
            while (j < source.length && /[\w$]/.test(source[j])) j++
            const word = source.slice(i, j)
            if (word === 'for' || word === 'while') pendingLoop = parens
            else if (word === 'function') next = 'function'
            else if (word === 'const') {
                const fn = blocks.lastIndexOf('function'), loop = blocks.lastIndexOf('loop')
                if (loop > fn) lines.push(line)
            } else if (next === 'loop') next = 'block'      // a loop with a single statement: no body of braces
            i = j
            continue
        }
        if (c === '(') parens++
        else if (c === ')') {
            parens--
            if (pendingLoop === parens) { pendingLoop = -1; next = 'loop' }
        } else if (source.startsWith('=>', i)) { next = 'function'; i += 2; continue } else if (c === '{') {
            blocks.push(next)
            next = 'block'
        } else if (c === '}') blocks.pop()
        else if (!/\s/.test(c) && next !== 'function') next = 'block'     // a loop with a single statement, an expression
        i++
    }
    return lines
}

function run(file) {
    state.currentFile = path.relative(root, file)
    if (/authored by AllTheMods/.test(read(file).slice(0, 400))) {
        foreign.add(state.currentFile)
        report('WARN', 'F1', 'a file of the base pack (AllTheMods header, All Rights Reserved) is in this repository', state.currentFile)
    }
    for (const line of loopConsts(read(file))) {
        report('ERROR', 'S3', `line ${line}: a const declared in a loop's body (Rhino keeps it at its first pass's value)`, state.currentFile)
    }
    try {
        vm.runInContext(read(file), vmctx, { filename: state.currentFile })
    } catch (e) {
        report('ERROR', 'S1', `${e.message.split('\n')[0]}`, state.currentFile)
    }
}

// ---- load ----------------------------------------------------------------------------------------------------------
const scriptsOf = dir => walkDir(path.join(root, 'kubejs', dir)).filter(f => f.endsWith('.js')).sort()
scriptsOf('startup_scripts').forEach(run)

// run the registry handlers, recording what they create
const creator = (kind, type, file) => ({
    create: (id, form) => {
        const info = { id, form: form || null, file, flags: [], forms: new Set(), tiers: null, types: null, pattern: null,
            tooltips: [], io: null }
        if (kind === 'startup') {
            const t = type
            if (t === 'item' || t === 'block' || t === 'fluid') {
                info.kind = t; info.textures = []
                if (state.items.has(id)) report('ERROR', 'S2', `${t} ${id} is registered twice (also in ${state.items.get(id).file})`, file)
                state.items.set(id, info)
            }
        } else if (type === 'gtceu:material') {
            if (state.materials.has(id)) report('ERROR', 'S2', `material ${id} is registered twice (also in ${state.materials.get(id).file})`, file)
            state.materials.set(id, info)
        } else if (type === 'gtceu:recipe_type') {
            if (state.recipeTypes.has(id)) report('ERROR', 'S2', `recipe type ${id} is registered twice (also in ${state.recipeTypes.get(id).file})`, file)
            state.recipeTypes.set(id, info)
        } else if (type === 'gtceu:machine') {
            if (state.machines.some(m => m.id === id)) report('ERROR', 'S2', `machine ${id} is registered twice`, file)
            state.machines.push(info)
        }
        const p = new Proxy({}, {
            get(_, name) {
                if (name === 'then' || name === Symbol.toPrimitive) return undefined
                if (typeof name === 'string' && name.startsWith('tooltips(')) {
                    return list => { info.tooltips.push(...flat([list]).map(c => c && c.key).filter(Boolean)); return p }
                }
                return (...a) => {
                    const n = String(name)
                    if (['liquid', 'gas', 'dust', 'ingot', 'fluid', 'plasma', 'gem'].includes(n)) info.forms.add(n)
                    if (n === 'setMaxIOSize') info.io = a
                    if (n === 'tiers') info.tiers = a
                    if (n === 'recipeTypes' || n === 'recipeType') info.types = flat(a)
                    if (n === 'pattern') info.pattern = a[0]
                    if (n === 'flags') info.flags.push(...a.map(String))
                    if (n === 'displayName' && kind === 'startup') info.displayName = a[0]
                    if (n === 'langValue') info.langValue = a[0]
                    if (n === 'definition' && typeof a[0] === 'function') {
                        // the builder of a tiered machine: run it once for its first tier to see what it names and tags
                        try { a[0](info.tiers ? Number(info.tiers[0]) : 0, p) } catch (e) {
                            report('WARN', 'S1', `definition of ${id}: ${String(e.message).split('\n')[0]}`, file)
                        }
                    }
                    if (n === 'texture' || n === 'textureAll' || n === 'textureSide' || n === 'model') {
                        a.filter(x => typeof x === 'string' && /^[a-z0-9_.]+:[a-z0-9_/.]+$/.test(x)).forEach(x => (info.textures || (info.textures = [])).push(x))
                    }
                    if (n === 'tooltip') info.tooltips.push(a[0])
                    return p
                }
            }
        })
        return p
    }
})
state.startup.forEach(([kind, type, fn, file]) => {
    try { fn(creator(kind, type, file)) } catch (e) { report('ERROR', 'S1', `${type}: ${e.message.split('\n')[0]}`, file) }
})

scriptsOf('server_scripts').forEach(run)
state.handlers.forEach(([file, fn], i) => {
    state.currentFile = file
    try { fn(event) } catch (e) { report('ERROR', 'S1', `recipe handler: ${e.message.split('\n')[0]}`, file) }
})

// ---- what exists ---------------------------------------------------------------------------------------------------
const baseOf = id => {
    // strip tier and shape words from a gtceu id to find its material
    let n = id
    for (const t of TIER_PREFIX) if (n.startsWith(t + '_')) { n = n.slice(t.length + 1); break }
    return n
}
const SHAPES = ['dust', 'small_dust', 'tiny_dust', 'ingot', 'hot_ingot', 'plate', 'rod', 'long_rod', 'foil', 'gear',
    'small_gear', 'nugget', 'block', 'bolt', 'screw', 'ring', 'frame', 'gem', 'lens', 'rotor', 'spring', 'wire',
    'single_wire', 'double_wire', 'quadruple_wire', 'octal_wire', 'hex_wire', 'single_cable', 'double_cable', 'quadruple_cable', 'octal_cable', 'hex_cable', 'crushed', 'pure_dust', 'impure_dust',
    'crushed_purified', 'crushed_centrifuged', 'raw', 'dense_plate', 'round', 'buzz_saw_blade', 'turbine_blade',
    'drill_head', 'bucket']
function materialOfItem(name) {
    // <mat>_<shape>, <prefix>_<mat>_<shape>, small_/tiny_ prefixes, fine_<mat>_wire
    let n = name
    for (const p of ['small_', 'tiny_', 'fine_', 'long_', 'dense_', 'double_', 'hot_', 'raw_', 'crushed_', 'purified_', 'impure_', 'pure_',
        'refined_', 'flawed_', 'flawless_', 'exquisite_', 'chipped_']) {
        if (n.startsWith(p)) { n = n.slice(p.length); break }
    }
    for (const s of [...SHAPES].sort((a, b) => b.length - a.length)) if (n.endsWith('_' + s)) return n.slice(0, -s.length - 1)
    return n
}
const af9Materials = new Set(state.materials.keys())
function gtceuKnown(id) {
    const name = id.replace(/^gtceu:/, '')
    if (gtNames.has(name) || gtNames.has(baseOf(name)) || gtPatterns.some(r => r.test(name)) || packIds.has(name) || packIds.has(baseOf(name))) return true
    if (gtMaterials.has(name) || af9Materials.has(name)) return true
    const mat = materialOfItem(name)
    if (gtMaterials.has(mat) || af9Materials.has(mat) || gtMaterials.has(baseOf(mat)) || af9Materials.has(baseOf(mat))) return true
    // machines AF9 registers (tiered: <tier>_<id>)
    for (const m of state.machines) {
        if (m.id === name || m.id === baseOf(name)) return true
    }
    return false
}
const kubejsItem = p => state.items.has(p)

// ---- R1 duplicates, R2 slots, R4 types ----------------------------------------------------------------------------
const seen = new Map()
state.recipes.forEach(r => {
    const k = `${r.type}/${r.id}`
    if (seen.has(k)) report('ERROR', 'R1', `duplicate recipe id ${k} (also in ${seen.get(k)})`, r.file)
    else seen.set(k, r.file)
})
const slots = {
    fab_synthesis: [3, 2, 4, 3], fab_blending: [3, 2, 4, 3], fab_wet_processing: [3, 2, 4, 3],
    fab_distillation: [1, 1, 2, 6], fab_cryogenic_rectification: [1, 1, 2, 6], fab_fractionation: [2, 1, 3, 2],
    fab_purification: [2, 1, 3, 2], fab_electrolysis: [2, 2, 3, 4], fab_electrofluorination: [2, 2, 3, 4],
    fab_calcination: [3, 2, 2, 2], fab_cvd: [3, 2, 2, 2], fab_crystal_growth: [3, 2, 2, 2],
    circuit_assembler: [6, 1, 1, 0], cutter: [1, 2, 1, 0], laser_engraver: [2, 1, 0, 0], macerator: [1, 4, 0, 0]
}
gtSlots.forEach((v, id) => { slots[id] = v })   // GT's own types: the recipes AF9 adds to them have to fit too
state.recipeTypes.forEach((info, id) => { if (info.io) slots[id] = info.io.map(Number) })
state.recipes.forEach(r => {
    const s = slots[r.type]
    if (!s) return
    if (r.itemIn.length > s[0] || r.itemOut.length > s[1] || r.fluidIn.length > s[2] || r.fluidOut.length > s[3]) {
        report('ERROR', 'R2', `${r.type} ${r.id}: items ${r.itemIn.length}/${s[0]} -> ${r.itemOut.length}/${s[1]}, ` +
            `fluids ${r.fluidIn.length}/${s[2]} -> ${r.fluidOut.length}/${s[3]}`, r.file)
    }
})
const af9Types = new Set(state.recipeTypes.keys())
const missingTypes = new Set()
state.recipes.forEach(r => {
    if (!r.type.startsWith('crafting_') && !af9Types.has(r.type) && !gtTypes.has(r.type) && !packIds.has(r.type) && !missingTypes.has(r.type)) {
        missingTypes.add(r.type)
        report('WARN', 'R4', `recipe type ${r.type} is neither AF9's nor in GT's list (${r.id})`, r.file)
    }
})

// ---- R8 numbers of a recipe -----------------------------------------------------------------------------------------------
//   R8  a GT recipe without .duration() or .EUt() (it would run for free or for the default 100 ticks), a duration or EUt that is
//       not a positive integer, an EUt above MAX, an amount of a fluid that is not a positive integer, a chance outside 1..10000,
//       a circuit outside 0..32, an item count of 0
const num = a => a && a.length ? Number(a[0]) : NaN
state.recipes.forEach(r => {
    if (r.type.startsWith('crafting_')) return
    const where = `${r.file}: ${r.type}/${r.id}`
    const noEU = r.calls.EUt === undefined && r.calls.CWUt === undefined && r.calls.inputEU === undefined && r.calls.duration !== undefined
    if (r.calls.duration === undefined) report('WARN', 'R8', `has no duration()`, where)
    else {
        const d = num(r.calls.duration)
        if (!Number.isInteger(d) || d < 1) report('ERROR', 'R8', `duration ${d} is not a positive integer`, where)
    }
    if (r.calls.EUt !== undefined) {
        const e = num(r.calls.EUt)
        if (!Number.isFinite(e) || e === 0 || Math.abs(e) > 2147483647) report('ERROR', 'R8', `EUt ${e} is zero, not a number or above the maximum`, where)
        else if (!Number.isInteger(e)) report('ERROR', 'R8', `EUt ${e} is not an integer`, where)
    } else if (r.calls.CWUt === undefined && r.calls.duration !== undefined && !/^(dummy|research|scanner)/.test(r.type)) {
        report('WARN', 'R8', 'has no EUt() (the recipe is free to run)', where)
    }
    r.fluidAmounts.forEach(([f, a]) => {
        if (typeof a !== 'number' || !Number.isInteger(a) || a < 1) report('ERROR', 'R8', `fluid ${f}: the amount ${a} is not a positive integer`, where)
    })
    r.chances.forEach(c => {
        if (typeof c !== 'number' || !Number.isInteger(c) || c < 1 || c > 10000) report('ERROR', 'R8', `a chance of ${c} (1..10000 is 0.01 % .. 100 %)`, where)
    })
    if (r.calls.circuit) { const c = num(r.calls.circuit); if (!Number.isInteger(c) || c < 0 || c > 32) report('ERROR', 'R8', `circuit ${c} is outside 0..32`, where) }
    const all = [].concat(r.itemOut, r.itemIn)
})
state.recipes.forEach(r => {
    // an item count of 0 or less in a string ('0x ns:id')
    ;[...r.itemIn, ...r.itemOut].forEach(i => { if (typeof i === 'string' && /^0x\s/.test(i)) report('ERROR', 'R8', `item count 0 in ${i}`, `${r.file}: ${r.type}/${r.id}`) })
})

// ---- R9 AF9's fab rules --------------------------------------------------------------------------------------------------------
//   R9  a recipe of a fab_* type at HV or above (EUt >= 512) that has neither a cleanroom() condition nor a blastFurnaceTemp()
//       (the thermal modes): the cleanroom is what keeps the wafer from contaminating, thermal modes run in a furnace
state.recipes.forEach(r => {
    if (!/^fab_/.test(r.type)) return
    const e = num(r.calls.EUt)
    if (e >= 512 && r.calls.cleanroom === undefined && r.calls.blastFurnaceTemp === undefined && r.calls.addCondition === undefined) {
        report('WARN', 'R9', `${r.type}/${r.id}: EUt ${e} (HV or above) with neither cleanroom() nor blastFurnaceTemp()`, r.file)
    }
})

// ---- R13 thermal recipes -----------------------------------------------------------------------------------------------------
//   R13 a recipe of a furnace type (electric_blast_furnace, boule_melting, fab_calcination, fab_cvd, fab_crystal_growth) without
//       blastFurnaceTemp(): GT's furnace conditions and AF9's thermal fab modes need the temperature
const THERMAL = new Set(['electric_blast_furnace', 'boule_melting', 'fab_calcination', 'fab_cvd', 'fab_crystal_growth'])
state.recipes.forEach(r => {
    if (THERMAL.has(r.type) && r.calls.blastFurnaceTemp === undefined) {
        report('ERROR', 'R13', `${r.type}/${r.id} has no blastFurnaceTemp()`, r.file)
    }
})

// ---- R7 recipe conflicts: within a recipe type, a recipe of AF9 whose inputs are all among another's (circuits count) ----
// GT picks the first recipe the machine's contents satisfy; if A's inputs are a subset of B's, a machine holding B's
// inputs can run A instead (when B holds at least as much of every input of A).
// AF9's own types, and in any GT type the recipes AF9 adds (`af9:` ids) among themselves: two reticles written with the same blank and
// lens would be one such conflict
const fabLike = id => id.startsWith('fab_') || id.startsWith('lithography_') || af9Types.has(id)
const byType = new Map()
state.recipes.forEach(r => {
    if (r.type.startsWith('crafting_')) return
    if (fabLike(r.type) || String(r.id).startsWith('af9:')) {
        if (!byType.has(r.type)) byType.set(r.type, [])
        byType.get(r.type).push(r)
    }
})
byType.forEach((list, type) => {
    const sets = list.map(r => new Set(r.itemIn.concat(r.fluidIn).filter(Boolean)))
    // amounts per input: items from the recipe's quantities, fluids from its amounts (the larger one when a fluid repeats)
    const qtys = list.map(r => {
        const m = new Map(r.itemQty)
        r.fluidAmounts.forEach(([f, a]) => m.set(f, Math.max(m.get(f) || 0, Number(a) || 0)))
        return m
    })
    for (let i = 0; i < list.length; i++) {
        if (sets[i].size === 0) continue
        for (let j = 0; j < list.length; j++) {
            if (i === j || sets[i].size > sets[j].size) continue
            let subset = true
            // all of A's inputs are B's, and B holds at least as much of each: a machine loaded for B can run A
            for (const x of sets[i]) {
                if (!sets[j].has(x) || (qtys[j].get(x) || 1) < (qtys[i].get(x) || 1)) { subset = false; break }
            }
            if (!subset) continue
            // equal sets are reported once
            if (sets[i].size === sets[j].size && i > j) continue
            report('WARN', 'R7', `${type}: ${list[i].id} takes only inputs that ${list[j].id} also has${sets[i].size === sets[j].size ? ' (the same inputs)' : ''}: a machine holding both runs either`, list[i].file)
        }
    }
})

// ---- R3 unknown ids, R5 no producer ------------------------------------------------------------------------------
const checkId = (id, where, role) => {
    if (!id || id.startsWith('circuit:') || id.startsWith('#') || id.startsWith('minecraft:')) return
    const [ns, name] = id.split(':')
    if (packIds.has(id)) return
    if (ns === 'kubejs') { if (!kubejsItem(name) && !af9Materials.has(name)) report('ERROR', 'R3', `${role} ${id} is not registered`, where) }
    else if (ns === 'af9') { if (!af9Items.has(name)) report('WARN', 'R3', `${role} ${id}: no such item in AF9 Core`, where) }
    else if (ns === 'gtceu') { if (!gtceuKnown(id)) report('WARN', 'R3', `${role} ${id}: not an AF9 material nor a name in GT's lists`, where) }
}
const reported = new Set()
state.recipes.forEach(r => {
    const tag = (id, role) => { const k = `${role}|${id}|${r.id}`; if (!reported.has(k)) { reported.add(k); checkId(id, `${r.file}: ${r.type}/${r.id}`, role) } }
    r.itemIn.forEach(i => tag(i, 'input')); r.fluidIn.forEach(i => tag(i, 'fluid input'))
    r.itemOut.forEach(i => tag(i, 'output')); r.fluidOut.forEach(i => tag(i, 'fluid output'))
})
const produced = new Set(), consumed = new Set()
const matOfId = id => {
    const name = id.split(':')[1]
    return af9Materials.has(name) ? name : af9Materials.has(materialOfItem(name)) ? materialOfItem(name) : null
}
state.recipes.forEach(r => {
    r.itemOut.concat(r.fluidOut).forEach(i => { if (i) { produced.add(i); const m = matOfId(i); if (m) produced.add('mat:' + m) } })
    r.itemIn.concat(r.fluidIn).forEach(i => { if (i) { consumed.add(i); const m = matOfId(i); if (m) consumed.add('mat:' + m) } })
})
// creative / loot / worldgen sources of AF9 things are declared here, one place: ids that need no recipe to make them
const noRecipeNeeded = { has: id => isSource(id) }
state.recipes.forEach(r => {
    r.itemIn.concat(r.fluidIn).forEach(i => {
        if (!i || i.startsWith('#') || i.startsWith('circuit:')) return
        const [ns, name] = i.split(':')
        if (noRecipeNeeded.has(i)) return
        const mat = matOfId(i)
        const mine = (ns === 'kubejs' && kubejsItem(name)) || mat
        if (mine && !produced.has(i) && !(mat && produced.has('mat:' + mat))) {
            const k = `R5|${i}`
            if (!reported.has(k)) { reported.add(k); report('WARN', 'R5', `${i} is taken by ${r.type}/${r.id} but no recipe makes it (a source? add it to tools/lint/data/sources.txt)`, r.file) }
        }
    })
})
// R6: AF9 content nobody makes or uses
state.items.forEach((info, id) => {
    const i = `kubejs:${id}`
    if (!produced.has(i) && !consumed.has(i) && !noRecipeNeeded.has(i)) report('INFO', 'R6', `${i} (${info.kind}) is registered but no recipe makes or takes it`, info.file)
})
state.materials.forEach((info, id) => {
    if (!produced.has('mat:' + id) && !consumed.has('mat:' + id) && !noRecipeNeeded.has(`gtceu:${id}`)) {
        report('INFO', 'R6', `material ${id} is registered but no recipe makes or takes it`, info.file)
    }
})

// ---- R10 reachability: can every AF9 item and material be made from what the pack gives? -------------------------------------
//   R10 an AF9 item or material that a recipe makes only from ingredients that themselves cannot be made (a cycle, a missing step).
//       Anything that is not AF9's (GT, other mods, tags) counts as available.
{
    const node = id => {
        if (!id || id.startsWith('#') || id.startsWith('circuit:')) return null
        const m = matOfId(id)
        if (m) return 'mat:' + m
        const [ns, name] = id.split(':')
        return ns === 'kubejs' && kubejsItem(name) ? id : null
    }
    const nodes = new Set()
    state.recipes.forEach(r => [...r.itemIn, ...r.fluidIn, ...r.itemOut, ...r.fluidOut].forEach(i => { const n = node(i); if (n) nodes.add(n) }))
    const have = new Set([...nodes].filter(n => noRecipeNeeded.has(n.startsWith('mat:') ? `gtceu:${n.slice(4)}` : n)))
    // any item of a source pattern counts (contaminated_*), and a material that a source covers by one of its forms
    nodes.forEach(n => { if (!n.startsWith('mat:') && isSource(n)) have.add(n) })
    // a material one of whose forms (zircon_dust) is a source (an ore, loot) is made by the world
    state.recipes.forEach(r => [...r.itemIn, ...r.fluidIn, ...r.itemOut, ...r.fluidOut].forEach(i => {
        if (i && isSource(i)) { const n = node(i); if (n) have.add(n) }
    }))
    let grew = true
    while (grew) {
        grew = false
        state.recipes.forEach(r => {
            const need = [...r.itemIn, ...r.fluidIn].map(node).filter(Boolean)
            if (!need.every(n => have.has(n))) return
            ;[...r.itemOut, ...r.fluidOut].map(node).filter(Boolean).forEach(n => { if (!have.has(n)) { have.add(n); grew = true } })
        })
    }
    nodes.forEach(n => {
        if (have.has(n)) return
        // find a recipe that makes it, for the message
        const mk = state.recipes.find(r => [...r.itemOut, ...r.fluidOut].map(node).includes(n))
        const where = mk ? `${mk.file}: ${mk.type}/${mk.id}` : ''
        const missing = mk ? [...mk.itemIn, ...mk.fluidIn].map(node).filter(x => x && !have.has(x)) : []
        report('ERROR', 'R10', `${n} can never be made${mk ? ` (${mk.type}/${mk.id} needs ${[...new Set(missing)].join(', ')}, which cannot be made either)` : ' (no recipe makes it)'}`, where)
    })
}

// ---- M1 / M2 multiblocks ------------------------------------------------------------------------------------------
const isPred = p => p && typeof p === 'object' && p.__af9pred === true
const allPreds = p => isPred(p) ? [p].concat(p.ors.flatMap(allPreds)) : []
state.machines.forEach(m => {
    const where = `${m.file}: machine ${m.id}`
    if (m.types) m.types.forEach(t => {
        if (typeof t === 'string' && !af9Types.has(t) && !gtTypes.has(t) && !packIds.has(t) && t !== 'dummy_recipes') {
            report('ERROR', 'M2', `multiblock ${m.id}: recipe type ${t} does not exist`, where)
        }
    })
    if (!m.pattern) return
    let pat
    try { pat = m.pattern({ get: () => 'CONTROLLER' }) } catch (e) { report('ERROR', 'M1', `${m.id}: the pattern function threw: ${e.message.split('\n')[0]}`, where); return }
    if (!pat || !pat.aisles) { report('ERROR', 'M1', `${m.id}: the pattern has no aisles`, where); return }
    // sizes
    const rowCount = new Set(pat.aisles.map(a => a.length))
    if (rowCount.size > 1) report('ERROR', 'M1', `${m.id}: aisles of different height ${[...rowCount].join('/')}`, where)
    const widths = new Set(pat.aisles.flatMap(a => a.map(r => r.length)))
    if (widths.size > 1) report('ERROR', 'M1', `${m.id}: rows of different width ${[...widths].join('/')}`, where)
    // characters
    const used = new Set(pat.aisles.flatMap(a => a.flatMap(r => [...r])))
    used.delete(' ')
    used.forEach(ch => { if (!(ch in pat.where)) report('ERROR', 'M1', `${m.id}: character '${ch}' is used but has no where()`, where) })
    Object.keys(pat.where).forEach(ch => { if (ch !== ' ' && !used.has(ch)) report('WARN', 'M1', `${m.id}: where('${ch}') is never used`, where) })
    // controller
    const ctrlChars = Object.keys(pat.where).filter(ch => isPred(pat.where[ch]) && pat.where[ch].kind === 'controller')
    if (ctrlChars.length !== 1) report('ERROR', 'M1', `${m.id}: ${ctrlChars.length} controller characters (needs exactly one)`, where)
    else {
        let count = 0
        pat.aisles.forEach((a, i) => a.forEach(r => { for (const ch of r) if (ch === ctrlChars[0]) count += pat.repeat[i] ? 2 : 1 }))
        if (count !== 1) report('ERROR', 'M1', `${m.id}: the controller '${ctrlChars[0]}' appears ${count} times`, where)
    }
    // limits: maximums only
    Object.entries(pat.where).forEach(([ch, p]) => allPreds(p).forEach(q => {
        if (q.min != null) report('ERROR', 'M1', `${m.id}: '${ch}' has a minimum (${q.kind} ${q.arg}); AF9 parts have maximums only`, where)
        if (q.exact != null) report('ERROR', 'M1', `${m.id}: '${ch}' has an exact count (${q.kind} ${q.arg}); AF9 parts have maximums only`, where)
        if (q.kind === 'auto') report('ERROR', 'M1', `${m.id}: '${ch}' uses autoAbilities (it forces energy and maintenance hatches)`, where)
    }))
    // a part with several maximums that count the same hatch twice is a GT trap: flag two limited ability predicates on one char
    Object.entries(pat.where).forEach(([ch, p]) => {
        const limited = allPreds(p).filter(q => q.kind === 'ability' && q.max != null).map(q => q.arg)
        const dup = limited.filter((a, i) => limited.indexOf(a) !== i)
        if (dup.length) report('ERROR', 'M1', `${m.id}: '${ch}' limits the same ability twice (${dup.join(', ')})`, where)
    })
})

// ---- M3 blocks of a pattern, M4 recipe types nobody runs, R11 fluid hatches against recipes ---------------------------------------
//   M3  a `Predicates.blocks('ns:id')` of a pattern that names a block nobody defines (the structure could never form)
//   M4  an AF9 recipe type that no machine runs (its recipes can never run)
//   R11 a recipe with more different fluid inputs / outputs than the machine has hatches for (at most, counting one tank a hatch)
const blockKnown = id => {
    const [ns, name] = id.split(':')
    if (!name || id.startsWith('#') || packIds.has(id)) return true
    if (ns === 'kubejs') return kubejsItem(name)
    if (ns === 'af9') return af9Items.has(name)
    if (ns === 'gtceu') return gtceuKnown(id)
    return true
}
const machineTypes = new Set()
state.machines.forEach(m => (m.types || []).forEach(t => machineTypes.add(String(t))))
// tiered single-block machines run their recipe type through `recipeType(s)` of the builder; GT's recipe types are run by GT's machines
const javaSource = walkDir(path.join(root, 'af9-core/src/main/java')).filter(f => f.endsWith('.java')).map(read).join('\n')
state.recipeTypes.forEach((info, id) => {
    if (machineTypes.has(id) || foreign.has(info.file)) return
    // AF9 Core can attach a type to a GT machine in Java (BouleMelting: the EBF's second mode)
    if (javaSource.includes(`"${id}"`)) report('INFO', 'M4', `recipe type ${id} is run by a machine AF9 Core sets up in Java`, info.file)
    else report('ERROR', 'M4', `recipe type ${id} is run by no machine`, info.file)
})
state.machines.forEach(m => {
    if (!m.pattern) return
    let pat
    try { pat = m.pattern({ get: () => 'CONTROLLER' }) } catch (e) { return }
    if (!pat || !pat.where) return
    const where = `${m.file}: machine ${m.id}`
    let fluidIn = 0, fluidOut = 0
    Object.entries(pat.where).forEach(([ch, p]) => allPreds(p).forEach(q => {
        if (q.kind === 'blocks') String(q.arg).split(',').filter(Boolean).forEach(id => {
            if (!blockKnown(id)) report('ERROR', 'M3', `${m.id}: '${ch}' names the block ${id}, which nobody defines`, where)
        })
        if (q.kind === 'ability') {
            const max = q.max == null ? Infinity : q.max
            if (/(IMPORT_FLUIDS|COOLANT_INPUT)$/.test(q.arg)) fluidIn += max   // AF9's coolant hatches are fluid inputs too
            if (/EXPORT_FLUIDS$/.test(q.arg)) fluidOut += max
        }
    }))
    ;(m.types || []).forEach(t => {
        state.recipes.filter(r => r.type === String(t)).forEach(r => {
            const di = new Set(r.fluidIn.filter(Boolean)).size, dout = new Set(r.fluidOut.filter(Boolean)).size
            if (di > fluidIn) report('ERROR', 'R11', `${r.type}/${r.id}: ${di} fluid inputs, but ${m.id} takes at most ${fluidIn} fluid input hatches`, r.file)
            if (dout > fluidOut) report('ERROR', 'R11', `${r.type}/${r.id}: ${dout} fluid outputs, but ${m.id} takes at most ${fluidOut} fluid output hatches`, r.file)
        })
    })
})

// ---- R12 power: a recipe that no machine of its type has the voltage for ------------------------------------------------------
//   R12 an AF9 recipe whose EUt is above the highest tier of the single-block machines that run its type, when no multiblock runs it
//       (multiblocks take any energy hatch)
{
    const byType = new Map()
    state.machines.forEach(m => (m.types || []).forEach(t => {
        const e = byType.get(String(t)) || { multi: 0, tmax: -1 }
        if (m.pattern) e.multi++
        else if (m.tiers) e.tmax = Math.max(e.tmax, ...m.tiers.map(Number).filter(Number.isFinite))
        byType.set(String(t), e)
    }))
    state.recipes.forEach(r => {
        const e = byType.get(r.type)
        if (!e || e.multi > 0 || e.tmax < 0 || !af9Types.has(r.type)) return
        const eu = num(r.calls.EUt)
        if (Number.isFinite(eu) && eu > VOLT[e.tmax]) {
            report('ERROR', 'R12', `${r.type}/${r.id}: EUt ${eu} is above ${TIERS[e.tmax]} (${VOLT[e.tmax]}), the highest single-block machine of the type, and no multiblock runs it`, r.file)
        }
    })
}

// ---- L1 lang keys of tooltips -------------------------------------------------------------------------------------
const langKeys = new Set()
// (paths with forward slashes, so the filters hold on Windows too)
const langFiles = walkDir(root).filter(f => {
    const p = f.replace(/\\/g, '/')
    return /\/lang\/en_us\.json$/.test(p) && !p.includes('/node_modules/') && !p.includes('/build/')
})
langFiles.forEach(f => { try { Object.keys(JSON.parse(read(f))).forEach(k => langKeys.add(k)) } catch (e) { report('ERROR', 'L1', `${path.relative(root, f)} is not valid JSON: ${e.message}`, f) } })
// GT's own keys (gtceu.*, block.gtceu.*) are in GT's jar: with a checkout of GT's source (GT_SRC=dir or --gt dir) they are checked too
const gtSrc = process.env.GT_SRC || (args.includes('--gt') ? args[args.indexOf('--gt') + 1] : null)
const gtLangFile = gtSrc && path.join(gtSrc, 'src/generated/resources/assets/gtceu/lang/en_us.json')
const gtLang = gtLangFile && fs.existsSync(gtLangFile) ? new Set(Object.keys(JSON.parse(read(gtLangFile)))) : null
state.translatables.forEach(k => {
    if (langKeys.has(k) || (gtLang && gtLang.has(k))) return
    if (!gtLang && /^(gtceu|block\.gtceu|item\.gtceu)\./.test(k)) {
        report('INFO', 'L1', `translatable key ${k} is GT's? (set GT_SRC to a GT checkout to check it)`, 'scripts')
    } else report('ERROR', 'L1', `translatable key ${k} is in no en_us.json`, 'scripts')
})
state.machines.forEach(m => {
    // the name of a machine: block.gtceu.<id> (tiered machines: <tier>_<id>) comes from langValue in the script itself
})

// ---- output --------------------------------------------------------------------------------------------------------
if (dump) {
    state.recipes.forEach(r => console.log(JSON.stringify({ type: r.type, id: r.id, file: r.file, itemIn: r.itemIn, itemOut: r.itemOut,
        fluidIn: r.fluidIn, fluidOut: r.fluidOut, EUt: r.calls.EUt || null, duration: r.calls.duration || null,
        fluidAmounts: r.fluidAmounts, chances: r.chances, chanceItems: r.chanceItems || [], calls: Object.keys(r.calls) })))
    process.exitCode = 0
    findings.length = 0   // nothing more to print
}
const order = { ERROR: 0, WARN: 1, INFO: 2 }
findings.sort((a, b) => order[a.level] - order[b.level] || a.code.localeCompare(b.code))
if (dump) {
    // printed above
} else if (asJson) {
    const registry = {
        items: [...state.items].map(([id, i]) => ({ id, kind: i.kind, file: i.file, textures: i.textures || [], displayName: i.displayName ? String(i.displayName) : null })),
        materials: [...state.materials].map(([id, m]) => ({ id, file: m.file })),
        machines: state.machines.map(m => ({ id: m.id, tiers: m.tiers ? m.tiers.map(String) : null, file: m.file, tooltips: m.tooltips,
            langValue: m.langValue ? String(m.langValue) : null })),
        recipeTypes: [...state.recipeTypes].map(([id, t]) => ({ id, file: t.file, langValue: t.langValue ? String(t.langValue) : null })),
        textures: [...state.items].map(([id, i]) => id).length
    }
    console.log(JSON.stringify({ registry, stats: { recipes: state.recipes.length, items: state.items.size, materials: state.materials.size,
        machines: state.machines.length }, findings }, null, 1))
} else {
    console.log(`scripts: ${state.recipes.length} recipes, ${state.items.size} kubejs items, ${state.materials.size} materials, ` +
        `${state.machines.length} machines`)
    const count = l => findings.filter(f => f.level === l).length
    console.log(`${count('ERROR')} errors, ${count('WARN')} warnings, ${count('INFO')} notes`)
    findings.forEach(f => console.log(`${f.level.padEnd(5)} ${f.code} ${f.msg}${f.where ? `  [${f.where}]` : ''}`))
}
// no process.exit: it would cut off a large JSON output on a pipe (64 KB)
process.exitCode = findings.some(f => f.level === 'ERROR') ? 1 : 0
