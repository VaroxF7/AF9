// AF9 - Atmospheres: a Gas Collector set down on a planet or in its orbit breathes what is in the air there. Spec: docs/fusion-power.md
//
// GT's Gas Collector (circuit 1, 2, 3) takes the air of the Overworld, the Nether and the End by the dimension it stands in.
// These are the other worlds' (Ad Astra's), the gases the fusion reactors want: helium-3 off the Moon's regolith, deuterium out of
// Glacio's ice caps, fluorine from Venus's acid clouds, the solar wind in orbit. The circuit picks the gas, one number each (4-26,
// so no two recipes share one); the machine has to be in that dimension (GT's dimension condition, as the Void Miner's modes
// have). Each cycle is 200 ticks like GT's air.
//
//   dimension          circuit  gas (mB a cycle)             EU/t    what it is for
//   ad_astra:moon         4     helium-3 1,000               HV      deuterium + helium-3 -> helium plasma (Mk1)
//   ad_astra:mercury      5     helium 5,000                 HV      the turbines' and the cryostat's helium
//                         6     deuterium 500                EV      fusion, heavy water
//   ad_astra:venus        7     carbon dioxide 10,000        HV      chemistry
//                         8     fluorine 500                 EV      iridium + fluorine -> radon plasma (Mk3)
//                         9     sulfur dioxide 2,000         HV      sulfuric acid
//   ad_astra:mars        10     carbon dioxide 10,000        HV      chemistry
//                        11     argon 1,000                  HV      carbon + magnesium -> argon plasma
//                        12     neon 250                     HV      the light gases
//                        13     nitrogen 2,000               HV      beryllium + deuterium -> nitrogen plasma
//   ad_astra:glacio      14     deuterium 1,000              EV      the heavy-ice deuterium: the first fusion's fuel
//                        15     methane 4,000                HV      chemistry
//                        16     nitrogen 5,000               HV      nitrogen plasma
//   orbits: the solar wind, collector plates in vacuum
//   ad_astra:earth_orbit 17     helium-3 500                 IV
//                        18     hydrogen 8,000               IV
//   ad_astra:moon_orbit  19     helium-3 500                 IV
//                        20     hydrogen 8,000               IV
//   ad_astra:mars_orbit  21     helium-3 1,000               IV
//   ad_astra:venus_orbit 22     helium-3 1,000               IV
//   ad_astra:mercury_orbit 23   helium-3 2,000               IV      nearest the Sun: the richest wind
//                        24     tritium 100                  IV      flare spallation: the D + T fuel's rare half
//                        25     deuterium 1,000              IV
//   ad_astra:glacio_orbit 26    deuterium 2,000              IV

ServerEvents.recipes(event => {
    const VA = GTValues.VA
    // collect(dimension, fluid, mB, EUt): the circuit counts up from 4, one for each
    let circuit = 3
    const collect = (dimension, fluid, amount, eut) => {
        circuit += 1
        const name = dimension.replace(/^ad_astra:/, '')
        event.recipes.gtceu.gas_collector(`af9:atmosphere/${name}_${fluid.replace(/^gtceu:/, '')}`)
            .circuit(circuit)
            .outputFluids(Fluid.of(fluid, amount))
            .dimension(dimension)
            .duration(200)
            .EUt(eut)
    }
    const HV = VA[GTValues.HV], EV = VA[GTValues.EV], IV = VA[GTValues.IV]

    // the worlds
    collect('ad_astra:moon', 'gtceu:helium_3', 1000, HV)

    collect('ad_astra:mercury', 'gtceu:helium', 5000, HV)
    collect('ad_astra:mercury', 'gtceu:deuterium', 500, EV)

    collect('ad_astra:venus', 'gtceu:carbon_dioxide', 10000, HV)
    collect('ad_astra:venus', 'gtceu:fluorine', 500, EV)
    collect('ad_astra:venus', 'gtceu:sulfur_dioxide', 2000, HV)

    collect('ad_astra:mars', 'gtceu:carbon_dioxide', 10000, HV)
    collect('ad_astra:mars', 'gtceu:argon', 1000, HV)
    collect('ad_astra:mars', 'gtceu:neon', 250, HV)
    collect('ad_astra:mars', 'gtceu:nitrogen', 2000, HV)

    collect('ad_astra:glacio', 'gtceu:deuterium', 1000, EV)
    collect('ad_astra:glacio', 'gtceu:methane', 4000, HV)
    collect('ad_astra:glacio', 'gtceu:nitrogen', 5000, HV)

    // the orbits: the solar wind
    collect('ad_astra:earth_orbit', 'gtceu:helium_3', 500, IV)
    collect('ad_astra:earth_orbit', 'gtceu:hydrogen', 8000, IV)
    collect('ad_astra:moon_orbit', 'gtceu:helium_3', 500, IV)
    collect('ad_astra:moon_orbit', 'gtceu:hydrogen', 8000, IV)
    collect('ad_astra:mars_orbit', 'gtceu:helium_3', 1000, IV)
    collect('ad_astra:venus_orbit', 'gtceu:helium_3', 1000, IV)
    collect('ad_astra:mercury_orbit', 'gtceu:helium_3', 2000, IV)
    collect('ad_astra:mercury_orbit', 'gtceu:tritium', 100, IV)
    collect('ad_astra:mercury_orbit', 'gtceu:deuterium', 1000, IV)
    collect('ad_astra:glacio_orbit', 'gtceu:deuterium', 2000, IV)
})
