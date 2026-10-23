// AF9 - LV Quadruple Input / Output Hatch (startup_scripts/gtceu/quad_hatches.js): an LV hull and four of the plain
// LV hatches of the same direction.

ServerEvents.recipes(event => {
    ;['input', 'output'].forEach(direction => {
        event.recipes.gtceu.assembler(`af9:lv_${direction}_hatch_4x`)
            .itemInputs('gtceu:lv_machine_hull', `4x gtceu:lv_${direction}_hatch`)
            .itemOutputs(`gtceu:lv_${direction}_hatch_4x`)
            .duration(200)
            .EUt(GTValues.VA[GTValues.LV])
    })
})
