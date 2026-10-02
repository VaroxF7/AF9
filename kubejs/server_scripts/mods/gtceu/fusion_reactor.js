// AF9 - The Fusion Reactor Mk1 (GT's LuV fusion reactor). Spec: docs/asteroid-fission.md
//
// The whole fission chain (asteroid_fission.js) leads here: GT's recipe took one double plate of plutonium-241 and a
// few circuits; this one takes three of them and four ingots of plutonium (the fission trigger of the ignition system),
// and AF9's chips run the plasma control: VPUs (plasma imaging), spin logic (the field coils' feedback), photonic ICs
// (the optical diagnostics). Plutonium-241 is the part that decides it: 30 % of a reactor's plutonium, so about 20 spent
// fuel rods (and the ore they took). The rest is GT's: the superconducting coil, ZPM circuits, field generators, the
// research (a scan of the ITBTC wire).

const $FusionChemicalHelper = Java.loadClass('com.gregtechceu.gtceu.api.data.chemical.ChemicalHelper')
const $FusionTagPrefix = Java.loadClass('com.gregtechceu.gtceu.api.data.tag.TagPrefix')

ServerEvents.recipes(event => {
    const VA = GTValues.VA

    // The scan: the single ITBTC wire, as GT's own recipe has it (asked of GT's ChemicalHelper, which knows the item
    // whatever its id), else the superconducting coil. A scan with no item would fail to build ("Research recipe must
    // have an item or fluid stack") and show an error on every world load: with neither, the recipe has no research
    // (and the log says so) instead.
    // (the signature is named: ChemicalHelper.get has overloads of two and three arguments that Rhino cannot tell apart)
    let researched = $FusionChemicalHelper['get(com.gregtechceu.gtceu.api.data.tag.TagPrefix,com.gregtechceu.gtceu.api.data.chemical.material.Material)'](
        $FusionTagPrefix.wireGtSingle, GTMaterials.get('indium_tin_barium_titanium_cuprate'))
    if (researched.isEmpty() === true) {
        console.warn('fusion_reactor.js: GT has no single ITBTC wire item, the research scans the superconducting coil')
        researched = Item.of('gtceu:superconducting_coil')
    }

    // by output, so whatever else the pack has made it with goes too
    event.remove({ output: 'gtceu:luv_fusion_reactor' })
    const reactor = event.recipes.gtceu.assembly_line('af9:fusion_reactor_mk1')
        .itemInputs('gtceu:superconducting_coil', '4x #gtceu:circuits/zpm', '3x gtceu:double_plutonium_241_plate',
            '4x gtceu:plutonium_ingot', '2x gtceu:double_osmiridium_plate', '2x gtceu:iv_field_generator',
            '64x gtceu:uhpic_chip', '32x gtceu:indium_tin_barium_titanium_cuprate_single_wire',
            '8x kubejs:vpu_chip', '8x kubejs:spin_logic_chip', '8x kubejs:photonic_ic_chip', '4x gtceu:fusion_glass')
        .inputFluids(Fluid.of('gtceu:soldering_alloy', 2304))
        .inputFluids(Fluid.of('gtceu:niobium_titanium', 2304))
        .itemOutputs('gtceu:luv_fusion_reactor')
    if (researched.isEmpty() === true) {
        console.error('fusion_reactor.js: nothing to scan for the Fusion Reactor Mk1: the recipe has no research')
    } else {
        // The overload by its signature: scannerResearch also exists for an ItemStack, and Rhino picks that one for the function
        // (which then has no stack: "Research recipe must have an item or fluid stack" on every world load)
        reactor['scannerResearch(java.util.function.UnaryOperator)'](b => b
            .researchStack(researched)
            .duration(1200)
            .EUt(VA[GTValues.IV]))
    }
    reactor.duration(1000).EUt(VA[GTValues.LuV])
})
