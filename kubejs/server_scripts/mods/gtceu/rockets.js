// AF9 - Gregified rockets and their propellant. Spec: docs/asteroid-fission.md
//
// Ad Astra's rockets keep their item graph (nose cone, fins, engine frame, an engine and a tank per tier), but nothing of
// it is crafted by hand or in the NASA Workbench any more: the parts are made in the assembler from GT's metals and
// machine parts, and the rocket itself in a GT machine too, a tier of GT per tier of rocket:
//   rocket   metal             parts   chip in the engine   rocket made in             engine / tank
//   tier 1   stainless steel   LV      4x LV circuits       Assembler, MV (expensive)  steel_engine, steel_tank       (the Moon)
//   tier 2   desh              HV      ASIC                 Assembler, HV              desh_engine, desh_tank         (Mars, Ceres: the Asteroid Field)
//   tier 3   titanium          EV      2x EV circuits       Assembler, EV              ostrum_engine, ostrum_tank     (Venus, Mercury)
//   tier 4   HSS-E             LuV     VPU                  Assembly Line, LuV         calorite_engine, calorite_tank (Glacio)
// An engine and a tank take the previous tier's, as Ad Astra's crafting did. The Assembly Line recipes of tiers 3 and 4
// are researched on the previous rocket (a Scanner scans it into a data stick). The Rover is an assembler recipe too.
//
// Tier 1 flies on LV parts out of an expensive MV assembler recipe on purpose: polysilicon (and with it every chip)
// comes only from the Moon, so a Moon rocket that needed a chip or an HV part could never be built. Nothing of the
// tier 1 chain takes silicon or anything above LV components; the price is paid in steel and time instead.
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
    // the nose cone carries the guidance: an LV sensor and two LV circuits (no chip: tier 1 must fly before silicon)
    event.recipes.gtceu.assembler('af9:rocket_nose_cone')
        .itemInputs('4x gtceu:stainless_steel_plate', 'gtceu:lv_sensor', '2x #gtceu:circuits/lv')
        .circuit(2)
        .inputFluids(Fluid.of('gtceu:soldering_alloy', 288))
        .itemOutputs('ad_astra:rocket_nose_cone')
        .duration(400)
        .EUt(VA[GTValues.MV])
    event.recipes.gtceu.assembler('af9:rocket_engine_frame')
        .itemInputs('8x gtceu:stainless_steel_rod', '2x gtceu:stainless_steel_plate', 'gtceu:stainless_steel_gear')
        .itemOutputs('ad_astra:engine_frame')
        .duration(200)
        .EUt(VA[GTValues.MV])
    event.recipes.gtceu.assembler('af9:nasa_workbench')
        .itemInputs('6x gtceu:stainless_steel_plate', '4x gtceu:stainless_steel_rod', 'gtceu:lv_robot_arm',
            'gtceu:lv_sensor', 'minecraft:crafting_table')
        .itemOutputs('ad_astra:nasa_workbench')
        .duration(400)
        .EUt(VA[GTValues.MV])
    event.recipes.gtceu.assembler('af9:rocket_launch_pad')
        .itemInputs('12x gtceu:stainless_steel_plate', '6x gtceu:stainless_steel_rod', '4x gtceu:lv_electric_piston')
        .circuit(3)
        .itemOutputs('ad_astra:launch_pad')
        .duration(500)
        .EUt(VA[GTValues.MV])

    // ---- Engines, tanks and the rocket, per tier ----
    // tier, rocket's hull block and parts' metal, GT tier of the parts, chip, Ad Astra's name of the tier's engine / tank,
    // the drum's metal, the previous tier's name (none for the first), the rocket's machine. Tier 1 has no chip (null):
    // its flight computer is four LV circuits, and its assembler recipes run one tier above its parts (MV), big and slow.
    // Tier 2 flies on Desh, the Moon's metal (Ad Astra's desh blocks for the hull, GT desh plates and screws), tier 3 on
    // titanium from Mars sand: neither takes anything from further out. Tier 3's computer is two EV circuits (tag).
    const tiers = [
        [1, 'stainless_steel', GTValues.LV, null, 'steel', 'stainless_steel', null, 'assembler'],
        [2, 'desh', GTValues.HV, 'af9:asic_chip', 'desh', 'stainless_steel', 'steel', 'assembler'],
        [3, 'titanium', GTValues.EV, null, 'ostrum', 'titanium', 'desh', 'assembler'],
        [4, 'hsse', GTValues.LuV, 'af9:vpu_chip', 'calorite', 'tungsten_steel', 'ostrum', 'assembly_line']]
    tiers.forEach(([tier, metal, voltage, chip, name, drum, previous, machine]) => {
        const v = GTValues.VN[voltage].toLowerCase()
        const t1 = tier === 1
        const machineVoltage = t1 ? GTValues.MV : voltage
        const computer = t1 ? '4x #gtceu:circuits/lv' : tier === 3 ? '2x #gtceu:circuits/ev' : `2x ${chip}`
        // The tier 2 hull is Ad Astra's blocks (GT makes no block of Desh, only plates, rods, bolts and screws).
        const hull = tier === 2 ? 'ad_astra:desh_block' : `gtceu:${metal}_block`
        // The research of an assembly line rocket: the previous rocket. Declared here, not in the if block below: Rhino keeps
        // a const of a nested block once for the whole script, so the second tier that takes the block failed with
        // "redeclaration of var" (and left the first one without its outputs and duration).
        const scanned = machine === 'assembly_line' ? Item.of(`ad_astra:tier_${tier - 1}_rocket`) : null
        // Engine: the previous engine (the frame for the first), two pumps for the turbopumps, a motor, the casing plates and
        // the flight computer (tier 1: more plates, more screws, LV circuits, more solder, slower)
        event.recipes.gtceu.assembler(`af9:rocket_${name}_engine`)
            .itemInputs(previous ? `ad_astra:${previous}_engine` : 'ad_astra:engine_frame', `2x gtceu:${v}_electric_pump`,
                `gtceu:${v}_electric_motor`, `${t1 ? 12 : 8}x gtceu:${metal}_plate`, `${t1 ? 8 : 4}x gtceu:${metal}_screw`,
                computer)
            .inputFluids(Fluid.of('gtceu:soldering_alloy', t1 ? 576 : 288))
            .itemOutputs(`ad_astra:${name}_engine`)
            .duration(t1 ? 600 : 400)
            .EUt(VA[machineVoltage])
        // Tank: the previous tank and a drum of the tier (tier 1: a drum and a pump), plates and a regulator for the feed
        // (tier 1 solders its seams shut: solder and time on top)
        const tank = event.recipes.gtceu.assembler(`af9:rocket_${name}_tank`)
            .itemInputs(previous ? `ad_astra:${previous}_tank` : `gtceu:${metal}_drum`,
                previous ? `gtceu:${drum}_drum` : `gtceu:${v}_electric_pump`, `${t1 ? 12 : 8}x gtceu:${metal}_plate`,
                `gtceu:${v}_fluid_regulator`)
            .itemOutputs(`ad_astra:${name}_tank`)
            .duration(t1 ? 500 : 300)
            .EUt(VA[machineVoltage])
        if (t1) tank.inputFluids(Fluid.of('gtceu:soldering_alloy', 288))

        // The rocket, as Ad Astra's workbench had it (nose cone, six hull blocks, four fins, two tanks and the engine) with
        // the hull of the tier's metal, and two robot arms that put it together
        const rocket = event.recipes.gtceu[machine](`af9:tier_${tier}_rocket`)
            .itemInputs('ad_astra:rocket_nose_cone', `6x ${hull}`, '4x ad_astra:rocket_fin',
                `2x ad_astra:${name}_tank`, `ad_astra:${name}_engine`, `2x gtceu:${v}_robot_arm`)
        if (machine === 'assembly_line') {
            // the control circuits of the tier; the previous rocket is the research (a Scanner, one tier below the line)
            rocket.itemInputs(`4x #gtceu:circuits/${v}`)
                .inputFluids(Fluid.of('gtceu:soldering_alloy', 144 * 4 * (tier - 2)))
            if (scanned.isEmpty() === true) {
                // an empty research stack would fail to build and show an error on every world load
                console.error(`rockets.js: ad_astra:tier_${tier - 1}_rocket is no item: the tier ${tier} rocket has no research`)
            } else {
                rocket['scannerResearch(java.util.function.UnaryOperator)'](b => b
                    .researchStack(scanned)
                    .duration(1200)
                    .EUt(VA[voltage - 1]))
            }
        } else {
            rocket.inputFluids(Fluid.of('gtceu:soldering_alloy', t1 ? 1152 : 576))
        }
        rocket.itemOutputs(`ad_astra:tier_${tier}_rocket`)
            .duration(t1 ? 800 : 600)
            .EUt(VA[machineVoltage])
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
