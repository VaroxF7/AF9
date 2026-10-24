// AF9 - LV and MV Quadruple Input / Output Hatch (startup_scripts/gtceu/quad_hatches.js): the tier's hull and four of the
// plain hatches of the same tier and direction.

ServerEvents.recipes(event => {
    ;[['lv', GTValues.LV], ['mv', GTValues.MV]].forEach(([tier, volt]) => {
        ;['input', 'output'].forEach(direction => {
            event.recipes.gtceu.assembler(`af9:${tier}_${direction}_hatch_4x`)
                .itemInputs(`gtceu:${tier}_machine_hull`, `4x gtceu:${tier}_${direction}_hatch`)
                .itemOutputs(`gtceu:${tier}_${direction}_hatch_4x`)
                .duration(200)
                .EUt(GTValues.VA[volt])
        })
    })
})
