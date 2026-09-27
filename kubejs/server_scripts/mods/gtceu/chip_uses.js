// AF9 - Where AF9's own chips (startup_scripts/gtceu/chips.js) go into other mods' recipes: chips added to GT's and
// AE2's recipes, and alternative recipes that take a chip. The uses inside AF9's own recipes sit with those recipes
// (wireless energy hatches, MV circuits, tiered circuits, fab machines, void miner, Orbital Lithography Station).
// Spec: docs/semiconductor-factory.md §8.

ServerEvents.recipes(allthemods => {
    const VA = GTValues.VA
    // Appends items to a recipe of another mod (GT's recipes keep their id, research and everything else)
    const addInputs = (id, items) => allthemods.forEachRecipe({ id: id }, recipe => {
        items.forEach(item => recipe.itemInputs(item))
    })

    // ---- RF Transceiver: the radio of every wireless link ----
    // AE2's Wireless Receiver, the part of the Wireless Access Point and of every wireless terminal
    allthemods.remove({ id: 'ae2:network/wireless_part' })
    allthemods.shaped('ae2:wireless_receiver', ['F', 'IQI', 'IRI'], {
        F: 'ae2:fluix_pearl', I: '#forge:ingots/iron', Q: 'ae2:quartz_fiber', R: 'kubejs:rf_transceiver_chip'
    }).id('af9:ae2/wireless_receiver')

    // ---- MCU: the small controller in GT's logic parts ----
    // GT's logic covers take one (they had no circuit: the lever / redstone parts and an iron plate)
    const logicCovers = ['cover_machine_controller', 'cover_activity_detector', 'cover_item_detector',
        'cover_fluid_detector']
    logicCovers.forEach(id => addInputs(`gtceu:assembler/${id}`, ['kubejs:mcu_chip']))
    // LV and MV robot arms and sensors: an MCU instead of the circuit, beside GT's recipes (the circuit versions stay:
    // the first ones come before the Photolithography Line)
    const mcuParts = [
        // tier, cable, rod, plate, sensor rod, sensor gem (GT's crafting components of the tier)
        ['lv', 'tin', 'steel', 'steel', 'brass', '#forge:gems/quartzite'],
        ['mv', 'copper', 'aluminium', 'aluminium', 'electrum', '#forge:flawless_gems/emerald']]
    mcuParts.forEach(([t, cable, rod, plate, sensorRod, gem]) => {
        allthemods.recipes.gtceu.assembler(`af9:${t}_robot_arm_mcu`)
            .itemInputs(`3x gtceu:${cable}_single_cable`, `2x gtceu:${rod}_rod`, `2x gtceu:${t}_electric_motor`,
                `gtceu:${t}_electric_piston`, 'kubejs:mcu_chip')
            .itemOutputs(`gtceu:${t}_robot_arm`)
            .duration(100)
            .EUt(VA[GTValues.LV])
        allthemods.recipes.gtceu.assembler(`af9:${t}_sensor_mcu`)
            .itemInputs(`gtceu:${sensorRod}_rod`, `4x gtceu:${plate}_plate`, 'kubejs:mcu_chip', gem)
            .itemOutputs(`gtceu:${t}_sensor`)
            .duration(100)
            .EUt(VA[GTValues.LV])
    })

    // ---- ASIC: the mining ASIC ----
    // GT's Large Miners and Fluid Drilling Rigs (HV and up: the MV rig comes before phosphorus wafers, it stays as it
    // is); the Void Miner takes them in miner.js
    const miningAsics = [['ev_large_miner', 2], ['iv_large_miner', 4], ['luv_large_miner', 8],
        ['hv_fluid_drilling_rig', 2], ['ev_fluid_drilling_rig', 4]]
    miningAsics.forEach(([id, count]) => addInputs(`gtceu:assembler/${id}`, [`${count}x kubejs:asic_chip`]))
})
