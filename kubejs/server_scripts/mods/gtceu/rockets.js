// AF9 - Gregified rockets and their propellant. Spec: docs/asteroid-fission.md
//
// Ad Astra's rockets keep their item graph (nose cone, fins, engine frame, a tank and an engine per tier, the rocket put
// together in the NASA Workbench), but the parts are made in the assembler from GT's metals and machine parts, a tier
// of GT per tier of rocket:
//   rocket   metal             parts          chip in the engine   the engine / the tank
//   tier 1   stainless steel   HV             MCU                  steel_engine, steel_tank     (the Moon)
//   tier 2   titanium          EV             ASIC                 desh_engine, desh_tank       (Mars, Ceres: the Asteroid Field)
//   tier 3   tungsten steel    IV             MRAM                 ostrum_engine, ostrum_tank   (Venus, Mercury)
//   tier 4   HSS-E             LuV            VPU                  calorite_engine, calorite_tank (Glacio)
// An engine and a tank take the previous tier's, as Ad Astra's crafting does.
//
// The propellant is not Ad Astra's fuel (oil, the Fuel Refinery) and not GT's rocket fuel: Aluminised Hydrolox, hydrogen
// and oxygen with aluminium powder and a little triethylaluminium (aluminium, ethylene and hydrogen), all from MV.
// 3,000 mB is one launch. The fluid tags of the rockets hold nothing else.

ServerEvents.recipes(event => {
    const VA = GTValues.VA

    // ---- What Ad Astra crafts: gone ----
    const parts = ['rocket_fin', 'rocket_nose_cone', 'engine_frame', 'launch_pad', 'nasa_workbench',
        'steel_engine', 'desh_engine', 'ostrum_engine', 'calorite_engine',
        'steel_tank', 'desh_tank', 'ostrum_tank', 'calorite_tank']
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
    // tier, rocket's hull block, parts' metal, GT tier of the parts, chip, Ad Astra's name of the tier's engine / tank,
    // the drum's metal, the previous tier's name (none for the first)
    const tiers = [
        [1, 'stainless_steel', 'stainless_steel', GTValues.HV, 'kubejs:mcu_chip', 'steel', 'stainless_steel', null],
        [2, 'titanium', 'titanium', GTValues.EV, 'kubejs:asic_chip', 'desh', 'titanium', 'steel'],
        [3, 'tungsten_steel', 'tungsten_steel', GTValues.IV, 'kubejs:mram_chip', 'ostrum', 'tungsten_steel', 'desh'],
        [4, 'hsse', 'hsse', GTValues.LuV, 'kubejs:vpu_chip', 'calorite', 'tungsten_steel', 'ostrum']]
    tiers.forEach(([tier, hull, metal, voltage, chip, name, drum, previous]) => {
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
        // Tank: the previous tank, a drum of the tier, plates and a regulator for the feed
        event.recipes.gtceu.assembler(`af9:rocket_${name}_tank`)
            .itemInputs(previous ? `ad_astra:${previous}_tank` : `gtceu:${metal}_drum`,
                previous ? `gtceu:${drum}_drum` : `gtceu:${v}_electric_pump`, `8x gtceu:${metal}_plate`,
                `gtceu:${v}_fluid_regulator`)
            .itemOutputs(`ad_astra:${name}_tank`)
            .duration(300)
            .EUt(VA[voltage])
        // The rocket: the NASA Workbench's 14 slots, as Ad Astra has them (nose cone, six hull blocks, four fins, two tanks
        // and the engine), with the hull of the tier's metal
        const slots = [{ item: 'ad_astra:rocket_nose_cone' }]
        ;[1, 2, 3, 4, 5, 6].forEach(() => slots.push({ item: `gtceu:${hull}_block` }))
        slots.push({ item: 'ad_astra:rocket_fin' }, { item: `ad_astra:${name}_tank` }, { item: `ad_astra:${name}_tank` },
            { item: 'ad_astra:rocket_fin' }, { item: 'ad_astra:rocket_fin' }, { item: `ad_astra:${name}_engine` },
            { item: 'ad_astra:rocket_fin' })
        event.custom({
            type: 'ad_astra:nasa_workbench',
            ingredients: slots,
            result: { count: 1, id: `ad_astra:tier_${tier}_rocket` }
        }).id(`af9:nasa_workbench/tier_${tier}_rocket`)
    })

    // ---- The propellant ----
    // Triethylaluminium: aluminium, ethylene and hydrogen (the Ziegler synthesis), MV
    event.recipes.gtceu.chemical_reactor('af9:triethylaluminium')
        .itemInputs('gtceu:aluminium_dust')
        .inputFluids(Fluid.of('gtceu:ethylene', 3000))
        .inputFluids(Fluid.of('gtceu:hydrogen', 1500))
        .outputFluids(Fluid.of('gtceu:triethylaluminium', 1000))
        .duration(200)
        .EUt(VA[GTValues.MV])
    // Aluminised Hydrolox, MV: 3,000 mB, one launch
    event.recipes.gtceu.chemical_reactor('af9:aluminised_hydrolox')
        .itemInputs('2x gtceu:aluminium_dust')
        .inputFluids(Fluid.of('gtceu:hydrogen', 4000))
        .inputFluids(Fluid.of('gtceu:oxygen', 2000))
        .inputFluids(Fluid.of('gtceu:triethylaluminium', 400))
        .outputFluids(Fluid.of('gtceu:aluminised_hydrolox', 3000))
        .duration(300)
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
