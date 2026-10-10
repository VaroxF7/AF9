// AF9 - Create steam age: no GT steam machines, Create does the work.
//
// The LP/HP steam single-blocks (boilers, extractor, macerator, compressor,
// forge hammer, furnace, alloy smelter, rock crusher, miner), the primitive
// pump and the charcoal pile igniter are gone (recipes removed, hidden from
// the recipe viewer by client_scripts/steam_hide.js). Everything they did in
// the steam/LV age now runs on Create:
//
//   sand -> mixer (heated) -> molten glass (gtceu:glass) -> spout -> glass tube
//   coal -> mixer (superheated) -> coke + creosote (instead of the coke oven)
//   iron + coke (+ calcite flux) -> mixer (superheated) -> steel
//   copper + tin dusts -> mixer (heated) -> bronze dust
//   ingots -> press -> plates; logs -> millstone -> wood dust
//   plants -> compactor -> plant ball; resin -> press -> raw rubber,
//     raw rubber + sulfur -> mixer (heated) -> rubber
//   vacuum tube + polished rose quartz -> electron tube (deployer or by hand),
//     and the LV/MV circuits take electron tubes instead of vacuum tubes.
//
// The coke oven and the primitive blast furnace STAY (they are multiblocks,
// not steam single-blocks) but both are far more expensive than before, so
// Create is the main path and GT is the slow bulk path. Steam turbines (LV+
// power gen, incl. the large turbine the FX-1 feeds) are untouched.
//
// Why plain Create JSON (event.custom): the linter has no Create stubs, so
// event.recipes.create.* would fail the offline check while raw JSON passes
// through it, exactly like early_circuits.js already does. Heat levels are
// the raw strings Create reads ("heated", "superheated").

ServerEvents.recipes(event => {
    // ================= A. removals =================
    // LP/HP steam single-blocks: every machine block becomes uncraftable.
    const steamMachines = [
        'gtceu:lp_steam_solid_boiler', 'gtceu:hp_steam_solid_boiler',
        'gtceu:lp_steam_liquid_boiler', 'gtceu:hp_steam_liquid_boiler',
        'gtceu:lp_steam_solar_boiler', 'gtceu:hp_steam_solar_boiler',
        'gtceu:lp_steam_extractor', 'gtceu:hp_steam_extractor',
        'gtceu:lp_steam_macerator', 'gtceu:hp_steam_macerator',
        'gtceu:lp_steam_compressor', 'gtceu:hp_steam_compressor',
        'gtceu:lp_steam_forge_hammer', 'gtceu:hp_steam_forge_hammer',
        'gtceu:lp_steam_furnace', 'gtceu:hp_steam_furnace',
        'gtceu:lp_steam_alloy_smelter', 'gtceu:hp_steam_alloy_smelter',
        'gtceu:lp_steam_rock_crusher', 'gtceu:hp_steam_rock_crusher',
        'gtceu:lp_steam_miner', 'gtceu:hp_steam_miner',
        'gtceu:primitive_pump',
        'gtceu:charcoal_pile_igniter'
    ]
    steamMachines.forEach(id => {
        event.remove({ output: id })
    })

    // Create's own electron tube (polished rose quartz + iron sheet): gone,
    // the only way is vacuum tube + rose quartz below.
    event.remove({ output: 'create:electron_tube' })

    // GT glass tubes (alloy smelter / solidifier / press): the spout below
    // is the only way now.
    event.remove({ id: 'gtceu:alloy_smelter/alloy_smelt_glass_tube' })
    event.remove({ id: 'gtceu:fluid_solidifier/solidify_glass_tube' })
    event.remove({ id: 'gtceu:forming_press/press_glass_tube' })

    // Cheap coke oven bricks: the shaped clay/sand brick, its smelting is
    // left alone (it smelts whatever compressed clay exists), and the alloy
    // smelter shortcut. Expensive versions are re-added in section C.
    event.remove({ output: 'gtceu:compressed_coke_clay' })
    event.remove({ id: 'gtceu:alloy_smelter/coke_oven_brick' })

    // GT steel, re-added far more expensive in section C.
    event.remove({ type: 'gtceu:primitive_blast_furnace' })
    event.remove({ id: 'gtceu:electric_blast_furnace/steel_from_iron' })
    event.remove({ id: 'gtceu:electric_blast_furnace/steel_from_wrought_iron' })

    // LV circuits, re-added with electron tubes in section D.
    event.remove({ id: 'gtceu:shaped/electronic_circuit_lv' })
    event.remove({ id: 'gtceu:circuit_assembler/electronic_circuit_lv' })

    // ================= B. molten glass + glass tube =================
    // Mixer, heated (blaze burner): 2 sand -> 288 mB molten glass, i.e. one
    // sand per glass tube. The fluid is GT's own glass fluid, so the spout,
    // GT's fluid pipes and every glass recipe keep working with no new fluid.
    event.custom({
        type: 'create:mixing',
        ingredients: [{ item: 'minecraft:sand' }, { item: 'minecraft:sand' }],
        results: [{ fluid: 'gtceu:glass', amount: 288 }],
        heatRequirement: 'heated'
    }).id('af9:create/molten_glass_from_sand')

    // Spout: a stick as the disposable mandrel + 144 mB molten glass -> tube.
    // The stick is consumed (it chars away); sand is the only real cost.
    event.custom({
        type: 'create:filling',
        ingredients: [{ item: 'minecraft:stick' }, { fluid: 'gtceu:glass', amount: 144 }],
        results: [{ item: 'gtceu:glass_tube' }]
    }).id('af9:create/glass_tube_spout')

    // ================= C. coke, steel, bricks =================
    // Coke without the coke oven: superheated mixing of coal. Matches the
    // oven's own numbers (1 coal -> 1 coke + 500 mB creosote), two at a time.
    event.custom({
        type: 'create:mixing',
        ingredients: [{ item: 'minecraft:coal' }, { item: 'minecraft:coal' }],
        results: [{ item: 'gtceu:coke_gem', count: 2 }, { fluid: 'gtceu:creosote', amount: 1000 }],
        heatRequirement: 'superheated'
    }).id('af9:create/coke_from_coal')

    // Steel, the main path: superheated mixing. Plain (no flux) is 1 iron +
    // 2 coke -> 1 steel; with a calcite flux it is 1 iron + 1 coke + 1 flux
    // -> 2 steel, so flux doubles the yield. Both need the blaze cake heat.
    event.custom({
        type: 'create:mixing',
        ingredients: [{ item: 'minecraft:iron_ingot' }, { item: 'gtceu:coke_gem' }, { item: 'gtceu:coke_gem' }],
        results: [{ item: 'gtceu:steel_ingot' }],
        heatRequirement: 'superheated'
    }).id('af9:create/steel_from_iron_and_coke')
    event.custom({
        type: 'create:mixing',
        ingredients: [{ item: 'minecraft:iron_ingot' }, { item: 'gtceu:coke_gem' }, { item: 'gtceu:calcite_dust' }],
        results: [{ item: 'gtceu:steel_ingot', count: 2 }],
        heatRequirement: 'superheated'
    }).id('af9:create/steel_from_iron_coke_and_flux')

    // GT steel, kept but far more expensive: the primitive furnace takes 4x
    // the fuel for twice the time, the EBF takes twice the iron and 5x the
    // oxygen. Create stays the cheap way; GT is the bulk way.
    event.recipes.gtceu.primitive_blast_furnace('af9:steel_from_coke_dust_hard')
        .itemInputs('gtceu:iron_ingot', '4x gtceu:coke_dust')
        .itemOutputs('gtceu:steel_ingot')
        .duration(3000)
    event.recipes.gtceu.primitive_blast_furnace('af9:steel_from_coal_dust_hard')
        .itemInputs('gtceu:iron_ingot', '8x gtceu:coal_dust')
        .itemOutputs('gtceu:steel_ingot')
        .duration(3600)
    event.recipes.gtceu.primitive_blast_furnace('af9:steel_from_charcoal_dust_hard')
        .itemInputs('gtceu:iron_ingot', '8x gtceu:charcoal_dust')
        .itemOutputs('gtceu:steel_ingot')
        .duration(3600)
    event.recipes.gtceu.electric_blast_furnace('af9:steel_from_iron_hard')
        .itemInputs('2x gtceu:iron_ingot')
        .inputFluids(Fluid.of('gtceu:oxygen', 1000))
        .itemOutputs('gtceu:steel_ingot')
        .blastFurnaceTemp(1000)
        .duration(800)
        .EUt(GTValues.VA[GTValues.MV])
    event.recipes.gtceu.electric_blast_furnace('af9:steel_from_wrought_iron_hard')
        .itemInputs('2x gtceu:wrought_iron_ingot')
        .inputFluids(Fluid.of('gtceu:oxygen', 800))
        .itemOutputs('gtceu:steel_ingot')
        .blastFurnaceTemp(1000)
        .duration(600)
        .EUt(GTValues.VA[GTValues.MV])

    // Coke oven bricks, expensive: 5 clay + 3 sand + the wooden form -> only
    // 2 compressed clay (was 3 clay + 4 sand -> 3). The smelting itself is
    // untouched. The alloy smelter shortcut takes 4x the inputs for 2 bricks
    // (was 1 sand + 1 clay -> 2) and twice the time.
    event.shaped('2x gtceu:compressed_coke_clay', ['CCC', 'CMC', 'SSS'], {
        C: 'minecraft:clay_ball',
        M: 'gtceu:brick_wooden_form',
        S: '#minecraft:sand'
    }).id('af9:shaped/compressed_coke_clay')
    event.recipes.gtceu.alloy_smelter('af9:coke_oven_brick_hard')
        .itemInputs('4x #minecraft:sand', '4x minecraft:clay_ball')
        .itemOutputs('2x gtceu:coke_oven_brick')
        .duration(300)
        .EUt(GTValues.VA[GTValues.ULV])

    // ================= D. electron tubes + LV circuits =================
    // Vacuum tube + polished rose quartz -> electron tube. Deployer version
    // for the line, shapeless version for the hand. Both give 1 tube.
    event.custom({
        type: 'create:deploying',
        ingredients: [{ item: 'gtceu:vacuum_tube' }, { item: 'create:polished_rose_quartz' }],
        results: [{ item: 'create:electron_tube' }]
    }).id('af9:create/electron_tube_deploying')
    event.shapeless('create:electron_tube', ['gtceu:vacuum_tube', 'create:polished_rose_quartz'])
        .id('af9:shapeless/electron_tube')

    // LV basic circuit with electron tubes: shaped + assembler, same shape
    // and numbers as GT's, only the tube changes (1 tube shaped, 2 tubes
    // in the assembler for 2 circuits).
    event.shaped('gtceu:basic_electronic_circuit', ['RPR', 'EBE', 'CCC'], {
        R: 'gtceu:resistor',
        P: 'gtceu:steel_plate',
        E: 'create:electron_tube',
        B: 'gtceu:resin_circuit_board',
        C: 'gtceu:red_alloy_single_wire'
    }).id('af9:shaped/basic_electronic_circuit')
    event.recipes.gtceu.circuit_assembler('af9:electronic_circuit_lv_electron')
        .itemInputs('gtceu:resin_circuit_board', '2x gtceu:resistor', '2x gtceu:red_alloy_single_wire', '2x create:electron_tube')
        .itemOutputs('2x gtceu:basic_electronic_circuit')
        .duration(200)
        .EUt(16)

    // ================= E. Create for every steam function =================
    // Bronze (was: steam alloy smelter): heated mixing of the dusts in GT's
    // own 3:1 ratio -> 4 bronze dust, smelt as usual.
    event.custom({
        type: 'create:mixing',
        ingredients: [{ item: 'gtceu:copper_dust' }, { item: 'gtceu:copper_dust' }, { item: 'gtceu:copper_dust' }, { item: 'gtceu:tin_dust' }],
        results: [{ item: 'gtceu:bronze_dust', count: 4 }],
        heatRequirement: 'heated'
    }).id('af9:create/bronze_dust_from_copper_and_tin')

    // Plates (was: steam forge hammer): the press flattens one ingot into
    // one plate for every early metal.
    const plates = [
        ['minecraft:iron_ingot', 'gtceu:iron_plate'],
        ['gtceu:wrought_iron_ingot', 'gtceu:wrought_iron_plate'],
        ['gtceu:bronze_ingot', 'gtceu:bronze_plate'],
        ['gtceu:steel_ingot', 'gtceu:steel_plate'],
        ['minecraft:copper_ingot', 'gtceu:copper_plate'],
        ['gtceu:tin_ingot', 'gtceu:tin_plate']
    ]
    plates.forEach(pair => {
        const ingot = pair[0]
        const plate = pair[1]
        event.custom({
            type: 'create:pressing',
            ingredients: [{ item: ingot }],
            results: [{ item: plate }]
        }).id('af9:create/' + plate.split(':')[1] + '_pressing')
    })

    // Wood dust (was: steam macerator): the millstone grinds any log.
    event.custom({
        type: 'create:milling',
        ingredients: [{ tag: 'minecraft:logs' }],
        results: [{ item: 'gtceu:wood_dust', count: 2 }]
    }).id('af9:create/wood_dust_milling')

    // Plant balls (was: steam compressor): the compactor squeezes garden
    // waste. Three greens, 4 plants each, matching the latex garden.
    const greens = ['minecraft:vine', 'minecraft:sugar_cane', 'minecraft:kelp']
    greens.forEach(plant => {
        event.custom({
            type: 'create:compacting',
            ingredients: [{ item: plant }, { item: plant }, { item: plant }, { item: plant }],
            results: [{ item: 'gtceu:plant_ball' }]
        }).id('af9:create/plant_ball_from_' + plant.split(':')[1])
    })

    // Rubber (was: steam extractor + steam alloy smelter): the press wrings
    // 3 raw rubber dust out of one resin, then heated mixing vulcanizes
    // 3 raw + 1 sulfur into solid rubber, GT's own 3:1 numbers.
    event.custom({
        type: 'create:pressing',
        ingredients: [{ item: 'gtceu:sticky_resin' }],
        results: [{ item: 'gtceu:raw_rubber_dust', count: 3 }]
    }).id('af9:create/raw_rubber_pressing')
    event.custom({
        type: 'create:mixing',
        ingredients: [{ item: 'gtceu:raw_rubber_dust' }, { item: 'gtceu:raw_rubber_dust' }, { item: 'gtceu:raw_rubber_dust' }, { item: 'gtceu:sulfur_dust' }],
        results: [{ item: 'gtceu:rubber_ingot' }],
        heatRequirement: 'heated'
    }).id('af9:create/rubber_vulcanization')
})
