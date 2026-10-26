// AF9 - Stainless Steel Firebox Casing (af9:stainless_firebox_casing): GT's firebox recipe, stainless steel
// between the steel and the titanium one (LV and HV); this one is HV.

ServerEvents.recipes(event => {
    event.shaped('2x af9:stainless_firebox_casing', ['PSP', 'SFS', 'PSP'], {
        P: 'gtceu:stainless_steel_plate', F: 'gtceu:stainless_steel_frame', S: 'gtceu:stainless_steel_rod'
    }).id('af9:stainless_firebox_casing')

    event.recipes.gtceu.assembler('af9:stainless_firebox_casing')
        .itemInputs('3x gtceu:stainless_steel_rod', 'gtceu:stainless_steel_frame', '3x gtceu:stainless_steel_plate')
        .itemOutputs('2x af9:stainless_firebox_casing')
        .duration(250)
        .EUt(VA[GTValues.HV])
})
