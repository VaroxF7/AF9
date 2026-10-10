// AF9 - the Energizing Orb Mk2: Powah's orb with a screen (AF9 Core, compat/powah). Six slots that hold a stack each, so the
// counted parts of orb_recipes.js go in as they are, a hopper, a pipe or an AE2 export bus reaches them, and the energizing
// rods round it charge it like a plain orb. The plain orb stays for Powah's own single-item recipes.
//
// Made at MV from steel, MV circuits and motors round Powah's orb, so it is there before the HV components need it.

ServerEvents.recipes(event => {
    event.shaped('af9:energizing_orb_mk2', [
        'PCP',
        'MOM',
        'PCP'
    ], {
        P: '#forge:plates/steel',
        C: 'gtceu:good_electronic_circuit',
        M: 'gtceu:mv_electric_motor',
        O: 'powah:energizing_orb'
    }).id('af9:powah/energizing_orb_mk2')
})
