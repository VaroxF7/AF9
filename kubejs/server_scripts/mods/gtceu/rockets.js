// AF9 - Gregified rockets and their propellant. Spec: docs/asteroid-fission.md
//
// Ad Astra's rockets keep their item graph (nose cone, fins, engine frame, an engine and a tank per tier), but nothing of
// it is crafted by hand or in the NASA Workbench any more: the parts are made in the assembler from GT's metals and
// machine parts, and the rocket itself in a GT machine too, a tier of GT per tier of rocket:
//   rocket   metal             parts   chip in the engine   rocket made in             engine / tank
//   tier 1   stainless steel   HV      MCU                  Assembler, HV              steel_engine, steel_tank       (the Moon)
//   tier 2   titanium          EV      ASIC                 Assembler, EV              desh_engine, desh_tank         (Mars, Ceres: the Asteroid Field)
//   tier 3   tungsten steel    IV      MRAM                 Assembly Line, IV          ostrum_engine, ostrum_tank     (Venus, Mercury)
//   tier 4   HSS-E             LuV     VPU                  Assembly Line, LuV         calorite_engine, calorite_tank (Glacio)
// An engine and a tank take the previous tier's, as Ad Astra's crafting did. The Assembly Line recipes of tiers 3 and 4
// are researched on the previous rocket (a Scanner scans it into a data stick). The Rover is an assembler recipe too.
//
// The propellant is not Ad Astra's fuel (oil, the Fuel Refinery) and not GT's rocket fuel: Aluminised Hydrolox, two
// recipes from MV, 3,000 mB (one launch) at the end:
//   aluminium dust + ethylene                                  -> Triethylaluminium (the igniter)
//   aluminium dust + hydrogen + oxygen + Triethylaluminium     -> Aluminised Hydrolox

ServerEvents.recipes(event => {
    const VA = GTValues.VA

    // ---- What Ad Astra crafts: gone ----
    const parts = ['rocket_fin', 'rocket_nose_cone', 'engine_frame', 'launch_pad', 'nasa_workbench',
        'steel_engine', 'desh_engine', 'ostrum_engine', 'calorite_engine',
        'steel_tank', 'desh_tank', 'ostrum_tank', 'calorite_tank', 'tier_1_rover']
    // by output and by Ad Astra's own id: the workbench's recipes are Ad Astra's own recipe type, and a filter by output
    // needs that type to say what it makes
    parts.forEach(part => {
        event.remove({ output: `ad_astra:${part}`, type: 'minecraft:crafting_shaped' })
        event.remove({ id: `ad_astra:${part}` })
    })
    const rockets = [1, 2, 3, 4]
    rockets.forEach(tier => {
        event.remove({ output: `ad_astra:tier_${tier}_rocket`, type: 'ad_astra:nasa_workbench' })
        event.remove({ id: `ad_astra:nasa_workbench/tier_${tier}_rocket_from_nasa_workbench` })
    })
    // the Fuel Refinery's fuel: the rockets do not take it any more (the tags below)
    event.remove({ id: 'ad_astra:refining/fuel_from_refining_oil' })

    // ---- The parts every rocket has ----
    event.recipes.gtceu.assembler('af9:rocket_fins')
        .itemInputs('4x gtceu:stainless_steel_plate', '4x gtceu:stainless_steel_rod')
        .circuit(1)
        .itemOutputs('4x ad_astra:rocket_fin')
        .duration(200)
        .EUt(VA[GTValues.MV])
    // the nose cone carries the guidance: a sensor and two MCUs
    event.recipes.gtceu.assembler('af9:rocket_nose_cone')
        .itemInputs('3x gtceu:stainless_steel_plate', 'gtceu:hv_sensor', '2x kubejs:mcu_chip')
        .circuit(2)
        .inputFluids(Fluid.of('gtceu:soldering_alloy', 144))
        .itemOutputs('ad_astra:rocket_nose_cone')
        .duration(300)
        .EUt(VA[GTValues.HV])
    event.recipes.gtceu.assembler('af9:rocket_engine_frame')
        .itemInputs('8x gtceu:stainless_steel_rod', '2x gtceu:stainless_steel_plate', 'gtceu:stainless_steel_gear')
        .itemOutputs('ad_astra:engine_frame')
        .duration(200)
        .EUt(VA[GTValues.MV])
    event.recipes.gtceu.assembler('af9:nasa_workbench')
        .itemInputs('4x gtceu:stainless_steel_plate', '2x gtceu:stainless_steel_rod', 'gtceu:hv_robot_arm',
            'gtceu:hv_sensor', 'minecraft:crafting_table')
        .itemOutputs('ad_astra:nasa_workbench')
        .duration(300)
        .EUt(VA[GTValues.HV])
    event.recipes.gtceu.assembler('af9:rocket_launch_pad')
        .itemInputs('8x gtceu:stainless_steel_plate', '4x gtceu:stainless_steel_rod', '2x gtceu:hv_electric_piston')
        .circuit(3)
        .itemOutputs('ad_astra:launch_pad')
        .duration(300)
        .EUt(VA[GTValues.HV])

    // ---- Engines, tanks and the rocket, per tier ----
    // tier, rocket's hull block and parts' metal, GT tier of the parts, chip, Ad Astra's name of the tier's engine / tank,
    // the drum's metal, the previous tier's name (none for the first), the rocket's machine
    const tiers = [
        [1, 'stainless_steel', GTValues.HV, 'kubejs:mcu_chip', 'steel', 'stainless_steel', null, 'assembler'],
        [2, 'titanium', GTValues.EV, 'kubejs:asic_chip', 'desh', 'titanium', 'steel', 'assembler'],
        [3, 'tungsten_steel', GTValues.IV, 'kubejs:mram_chip', 'ostrum', 'tungsten_steel', 'desh', 'assembly_line'],
        [4, 'hsse', GTValues.LuV, 'kubejs:vpu_chip', 'calorite', 'tungsten_steel', 'ostrum', 'assembly_line']]
    tiers.forEach(([tier, metal, voltage, chip, name, drum, previous, machine]) => {
        const v = GTValues.VN[voltage].toLowerCase()
        // Engine: the previous engine (the frame for the first), two pumps for the turbopumps, a motor, the casing plates and
        // the flight computer
        event.recipes.gtceu.assembler(`af9:rocket_${name}_engine`)
            .itemInputs(previous ? `ad_astra:${previous}_engine` : 'ad_astra:engine_frame', `2x gtceu:${v}_electric_pump`,
                `gtceu:${v}_electric_motor`, `8x gtceu:${metal}_plate`, `4x gtceu:${metal}_screw`, `2x ${chip}`)
            .inputFluids(Fluid.of('gtceu:soldering_alloy', 288))
            .itemOutputs(`ad_astra:${name}_engine`)
            .duration(400)
            .EUt(VA[voltage])
        // Tank: the previous tank and a drum of the tier (tier 1: a drum and a pump), plates and a regulator for the feed
        event.recipes.gtceu.assembler(`af9:rocket_${name}_tank`)
            .itemInputs(previous ? `ad_astra:${previous}_tank` : `gtceu:${metal}_drum`,
                previous ? `gtceu:${drum}_drum` : `gtceu:${v}_electric_pump`, `8x gtceu:${metal}_plate`,
                `gtceu:${v}_fluid_regulator`)
            .itemOutputs(`ad_astra:${name}_tank`)
            .duration(300)
            .EUt(VA[voltage])

        // The rocket, as Ad Astra's workbench had it (nose cone, six hull blocks, four fins, two tanks and the engine) with
        // the hull of the tier's metal, and two robot arms that put it together
        const rocket = event.recipes.gtceu[machine](`af9:tier_${tier}_rocket`)
            .itemInputs('ad_astra:rocket_nose_cone', `6x gtceu:${metal}_block`, '4x ad_astra:rocket_fin',
                `2x ad_astra:${name}_tank`, `ad_astra:${name}_engine`, `2x gtceu:${v}_robot_arm`)
        if (machine === 'assembly_line') {
            // the control circuits of the tier; the previous rocket is the research (a Scanner, one tier below the line)
            rocket.itemInputs(`4x #gtceu:circuits/${v}`)
                .inputFluids(Fluid.of('gtceu:soldering_alloy', 144 * 4 * (tier - 2)))
                .scannerResearch(b => b
                    .researchStack(Item.of(`ad_astra:tier_${tier - 1}_rocket`))
                    .duration(1200)
                    .EUt(VA[voltage - 1]))
        } else {
            rocket.inputFluids(Fluid.of('gtceu:soldering_alloy', 576))
        }
        rocket.itemOutputs(`ad_astra:tier_${tier}_rocket`)
            .duration(600)
            .EUt(VA[voltage])
    })

    // The Rover (Ad Astra: a desh engine, two wheels, a radio, a large gas tank, desh blocks and plates)
    event.recipes.gtceu.assembler('af9:tier_1_rover')
        .itemInputs('ad_astra:desh_engine', '2x ad_astra:wheel', 'ad_astra:radio', 'ad_astra:large_gas_tank',
            '2x gtceu:titanium_block', '4x gtceu:titanium_plate', '2x gtceu:ev_electric_motor')
        .inputFluids(Fluid.of('gtceu:soldering_alloy', 288))
        .itemOutputs('ad_astra:tier_1_rover')
        .duration(400)
        .EUt(VA[GTValues.EV])

    // ---- The propellant: two steps, both in the Chemical Reactor, MV ----
    // 1. the igniter: aluminium and ethylene make Triethylaluminium, which burns on contact with air
    event.recipes.gtceu.chemical_reactor('af9:triethylaluminium')
        .itemInputs('gtceu:aluminium_dust')
        .inputFluids(Fluid.of('gtceu:ethylene', 1000))
        .outputFluids(Fluid.of('gtceu:triethylaluminium', 1000))
        .duration(100)
        .EUt(VA[GTValues.MV])
    // 2. the fuel: hydrogen and oxygen in the ratio of water, aluminium powder burning in them, the igniter; 3,000 mB is one
    // launch (Ad Astra's rocket tank)
    event.recipes.gtceu.chemical_reactor('af9:aluminised_hydrolox')
        .itemInputs('2x gtceu:aluminium_dust')
        .inputFluids(Fluid.of('gtceu:hydrogen', 2000), Fluid.of('gtceu:oxygen', 1000),
            Fluid.of('gtceu:triethylaluminium', 500))
        .outputFluids(Fluid.of('gtceu:aluminised_hydrolox', 3000))
        .duration(200)
        .EUt(VA[GTValues.MV])
})

// The rockets' fuel: Aluminised Hydrolox and nothing else (Ad Astra's fuel tag brings in oil fuel, diesel, biodiesel)
ServerEvents.tags('fluid', event => {
    const fuels = ['tier_1_rocket_fuel', 'tier_2_rocket_fuel', 'tier_3_rocket_fuel', 'tier_4_rocket_fuel',
        'tier_1_rover_fuel']
    fuels.forEach(fuel => {
        event.removeAll(`ad_astra:${fuel}`)
        event.add(`ad_astra:${fuel}`, 'gtceu:aluminised_hydrolox')
    })
})
