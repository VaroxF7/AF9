// AF9 - Hostile Neural Networks, the GregTech way. The mod's workbench recipes are gone; everything assembles.
// HV (normal tier): the Model Framework, the Deep Learner, the Prediction Matrix and the Simulation Chamber.
// IV (high tier): the Loot Fabricator, the machine that turns a prediction into items. Data models themselves are
// not crafted (they level by kills and simulation), so they have no recipe to change.

ServerEvents.recipes(event => {
    const VA = GTValues.VA
    const gt = event.recipes.gtceu

    ;['blank_data_model', 'deep_learner', 'prediction_matrix', 'sim_chamber', 'loot_fabricator']
        .forEach(id => event.remove({ output: 'hostilenetworks:' + id }))

    // ---- HV ----
    gt.assembler('af9:hostilenetworks/blank_data_model')
        .itemInputs('2x #gtceu:circuits/hv', '2x gtceu:hv_sensor', '4x gtceu:aluminium_plate',
            '8x gtceu:fine_gold_wire')
        .inputFluids(Fluid.of('gtceu:polyethylene', 288))
        .itemOutputs('hostilenetworks:blank_data_model')
        .duration(200)
        .EUt(VA[GTValues.HV])

    gt.assembler('af9:hostilenetworks/deep_learner')
        .itemInputs('hostilenetworks:blank_data_model', '#gtceu:circuits/hv', 'gtceu:hv_emitter',
            '4x gtceu:stainless_steel_plate', 'gtceu:computer_monitor_cover')
        .inputFluids(Fluid.of('gtceu:soldering_alloy', 144))
        .itemOutputs('hostilenetworks:deep_learner')
        .duration(300)
        .EUt(VA[GTValues.HV])

    gt.assembler('af9:hostilenetworks/prediction_matrix')
        .itemInputs('2x #gtceu:circuits/hv', '2x gtceu:hv_sensor', '4x gtceu:copper_foil',
            '2x gtceu:aluminium_plate')
        .inputFluids(Fluid.of('gtceu:soldering_alloy', 144))
        .itemOutputs('hostilenetworks:prediction_matrix')
        .duration(200)
        .EUt(VA[GTValues.HV])

    gt.assembler('af9:hostilenetworks/sim_chamber')
        .itemInputs('gtceu:hv_machine_hull', '2x hostilenetworks:prediction_matrix', '4x #gtceu:circuits/hv',
            '2x gtceu:hv_field_generator', '2x gtceu:hv_electric_motor', '4x gtceu:stainless_steel_plate')
        .inputFluids(Fluid.of('gtceu:soldering_alloy', 288))
        .itemOutputs('hostilenetworks:sim_chamber')
        .duration(400)
        .EUt(VA[GTValues.HV])

    // ---- IV ----
    gt.assembler('af9:hostilenetworks/loot_fabricator')
        .itemInputs('gtceu:iv_machine_hull', '4x hostilenetworks:prediction_matrix', '4x #gtceu:circuits/iv',
            '2x gtceu:iv_robot_arm', '2x gtceu:iv_emitter', '4x gtceu:titanium_plate',
            '8x gtceu:tungsten_steel_plate')
        .inputFluids(Fluid.of('gtceu:soldering_alloy', 576))
        .itemOutputs('hostilenetworks:loot_fabricator')
        .duration(600)
        .EUt(VA[GTValues.IV])
})
