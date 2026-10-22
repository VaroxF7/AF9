// AF9 - Electric Implosion Compressor (machines: startup_scripts/gtceu/electric_implosion.js). Spec:
// docs/electric-implosion.md
//
//   Lunar Acid Vapor   a new cut of Lunar Air (distillation: fab_chemistry.js); acidic, so it needs acid-proof pipes
//                      and tanks. The shock wave of the electric compressor: the discharge flashes it into plasma.
//   implosions         every implosion recipe, explosive and electric, is in the IMPLOSIONS table below
//   naquadah           4 U-235 dust + 2,000 mB vapor -> 1 naquadah dust, EV: the discharge fuses the uranium nuclei
//   Twi-re-sten        twinite (Thermal Extra) + redstone + tungsten dust -> 3 dust (mixer, EV); GT's blast property melts it in the
//                      EBF, and its plate, foil, fine wire and dense plate come from the usual machines
//   nichrome coil      GT's HV assembler recipe is replaced: EV, 4 quadruple nichrome wires, stainless foil AND
//                      Twi-re-sten foil, and the vapor
//   machines           casing, controller and single blocks (HV-LuV) are crafted from what an implosion compressor and
//                      an electric piston give

ServerEvents.recipes(event => {
    const VA = GTValues.VA
    const V = GTValues.V
    const BURST_EU = 200000 // EU a charged implosion takes from the bank, per parallel
    const ALLOY = 'gtceu:desh_titanium_rocket_alloy'

    // ---- Implosion recipes: the one place ----
    // One rule for every implosion product. 4 dust in, 1 product out, two routes:
    //   explosive   GT's Implosion Compressor, LV, 20 ticks: 2 dynamite or 1 industrial TNT
    //   electric    the Electric Implosion Compressor, 500 mB Lunar Acid Vapor, no explosives
    // Add a product here and both routes exist. (GT's own gem implosions stay explosive-only.)
    const IMPLOSIONS = [
        // id, dust, product, electric tier, electric ticks
        ['desh_titanium_rocket_alloy_dense_plate', `4x ${ALLOY}_dust`, 'gtceu:dense_desh_titanium_rocket_alloy_plate',
            GTValues.HV, 200],
    ]
    IMPLOSIONS.forEach(([id, dust, product, tier, ticks]) => {
        event.recipes.gtceu.implosion_compressor(`af9:implosion/${id}/dynamite`)
            .itemInputs(dust, '2x gtceu:dynamite')
            .itemOutputs(product)
            .duration(20)
            .EUt(VA[GTValues.LV])
        event.recipes.gtceu.implosion_compressor(`af9:implosion/${id}/industrial_tnt`)
            .itemInputs(dust, 'gtceu:industrial_tnt')
            .itemOutputs(product)
            .duration(20)
            .EUt(VA[GTValues.LV])
        event.recipes.gtceu.electric_implosion_compressor(`af9:implosion/${id}/electric`)
            .itemInputs(dust)
            .inputFluids(Fluid.of('gtceu:lunar_acid_vapor', 500))
            .itemOutputs(product)
            .duration(ticks)
            .EUt(VA[tier])
        // charged: the same without vapor, fired from the Large machine's capacitor bank
        event.recipes.gtceu.charged_implosion(`af9:implosion/${id}/charged`)
            .itemInputs(dust)
            .itemOutputs(product)
            .duration(20)
            .EUt(VA[GTValues.LV])
            .addData('burst_eu', BURST_EU)
    })

    // Every other implosion recipe of the pack (GT's gems, other mods) becomes a charged recipe: the same inputs
    // without the explosive, the same outputs, fired from the bank. Our own (af9:) are made above.
    const seen = new Set()
    const explosive = /dynamite|industrial_tnt|minecraft:tnt|powderbarrel/
    event.forEachRecipe({ type: 'gtceu:implosion_compressor' }, r => {
        const id = String(r.getId())
        if (id.startsWith('af9:')) return
        const json = JSON.parse(String(r.json))
        const items = (json.inputs && json.inputs.item) || []
        const kept = items.filter(c => !explosive.test(JSON.stringify(c)))
        if (kept.length === 0 || kept.length === items.length) return
        const key = JSON.stringify(kept) + JSON.stringify(json.outputs)
        if (seen.has(key)) return
        seen.add(key)
        json.type = 'gtceu:charged_implosion'
        json.inputs.item = kept
        json.data = { burst_eu: BURST_EU }
        const made = event.custom(json)
        if (made && typeof made.id === 'function') made.id(`af9:implosion_charged/${id.replace(':', '/')}`)
        else console.warn(`electric_implosion.js: no recipe object for ${id}`)
    })

    // Naquadah from U-235: the capacitor bank's discharge drives the fissile nuclei together hard enough to fuse them
    // into naquadah. Four enriched dusts per dust, EV, two buckets of vapor: the uranium chain (asteroid_fission.js)
    // is the feed, so this is a deliberate sink for enrichment, not a replacement for the field vein yet.
    event.recipes.gtceu.electric_implosion_compressor('af9:naquadah_from_uranium_235')
        .itemInputs('4x gtceu:uranium_235_dust')
        .inputFluids(Fluid.of('gtceu:lunar_acid_vapor', 2000))
        .itemOutputs('gtceu:naquadah_dust')
        .duration(600)
        .EUt(V[GTValues.EV])

    // ---- Twi-re-sten Insulation ----
    event.recipes.gtceu.mixer('af9:twiresten_insulation_dust')
        .itemInputs('thermal_extra:twinite_dust', 'minecraft:redstone', 'gtceu:tungsten_dust')
        .itemOutputs('3x gtceu:twiresten_insulation_dust')
        .duration(300)
        .EUt(VA[GTValues.EV])

    // ---- The nichrome coil: EV, the vapor and the new foil ----
    event.remove({ output: 'gtceu:nichrome_coil_block', type: 'gtceu:assembler' })
    event.recipes.gtceu.assembler('af9:nichrome_coil_block')
        .itemInputs('4x gtceu:nichrome_quadruple_wire', '8x gtceu:stainless_steel_foil',
            '8x gtceu:twiresten_insulation_foil')
        .inputFluids(Fluid.of('gtceu:lunar_acid_vapor', 500))
        .itemOutputs('gtceu:nichrome_coil_block')
        .duration(400)
        .EUt(VA[GTValues.EV])

    // ---- Crafting of the machines ----
    // the casing: four capacitors on a stainless frame, ringed with stainless plate
    event.shaped('2x af9:shock_capacitor_casing', ['PCP', 'CFC', 'PCP'], {
        P: 'gtceu:stainless_steel_plate', C: 'gtceu:capacitor', F: 'gtceu:stainless_steel_frame'
    }).id('af9:shock_capacitor_casing')

    // the multiblock: GT's Implosion Compressor, a piston, circuits, the new casing and gold cable
    event.shaped('gtceu:large_electric_implosion_compressor', ['CPC', 'AXA', 'WCW'], {
        C: '#gtceu:circuits/hv', P: 'gtceu:hv_electric_piston', A: 'af9:shock_capacitor_casing',
        X: 'gtceu:implosion_compressor', W: 'gtceu:gold_quadruple_cable'
    }).id('af9:large_electric_implosion_compressor')

    // the single blocks, HV-LuV: the tier's hull, piston, emitter, circuits and cable
    const tiers = [
        // tier, cable (GT's crafting components of that tier)
        ['hv', 'gold'],
        ['ev', 'aluminium'],
        ['iv', 'platinum'],
        ['luv', 'niobium_titanium'],
    ]
    tiers.forEach(([t, cable]) => {
        event.shaped(`gtceu:${t}_electric_implosion_compressor`, ['CEC', 'PHP', 'WCW'], {
            C: `#gtceu:circuits/${t}`, E: `gtceu:${t}_emitter`, P: `gtceu:${t}_electric_piston`,
            H: `gtceu:${t}_machine_hull`, W: `gtceu:${cable}_single_cable`
        }).id(`af9:${t}_electric_implosion_compressor`)
    })
})
