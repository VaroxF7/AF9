// AF9 - Rubber without rubber trees. Spec: docs/rubber.md
//
// GT's rubber trees are gone from the pack, so every recipe that takes or makes
// a rubber-tree block goes, and latex comes from farmable plants instead.
// The chain stays 3 steps at steam age on purpose:
//
//   plant (vine, sugar cane, kelp, leaves, saplings, slime) -> extractor -> sticky resin
//   sticky resin -> extractor (steam) or centrifuge (LV) -> raw rubber dust
//   raw rubber + sulfur -> alloy smelter (steam, solid) or chemical reactor (LV, liquid)
//
// Silicone and styrene-butadiene keep GT's own chemistry (no trees ever in it):
// silicone is Si + HCl + methanol -> polydimethylsiloxane -> silicone (LV),
// SBR is ethylene + benzene -> styrene and oil cracking -> butadiene -> raw SBR
// -> SBR (needs asteroid oil, so it stays the late rubber). This file only
// purges the trees and plants the new latex; it does not touch those chains.

ServerEvents.recipes(event => {
    const VA = GTValues.VA
    const ULV = VA[GTValues.ULV]
    const LV = VA[GTValues.LV]

    // ---- The trees go ----
    // Every recipe that takes a rubber-tree block: extractor latex, centrifuge
    // separation, and whatever else the pack added. Removals by item id are safe
    // when the id does not exist (KubeJS skips them), and tag recipes that take
    // any log or sapling (charcoal, sticks) are left alone on purpose: they make
    // charcoal and sticks, not rubber.
    const treeInputs = [
        'gtceu:rubber_log',
        'gtceu:rubber_wood',
        'gtceu:stripped_rubber_log',
        'gtceu:stripped_rubber_wood',
        'gtceu:rubber_leaves',
        'gtceu:rubber_sapling'
    ]
    treeInputs.forEach(item => {
        event.remove({ input: item })
    })
    // Known GT separations by id, in case a future GT keeps them without the
    // direct item input (chanced outputs sometimes slip input filters).
    event.remove({ id: 'gtceu:centrifuge/rubber_log_separation' })
    event.remove({ id: 'gtceu:extractor/rubber_log_extraction' })
    event.remove({ id: 'gtceu:extractor/rubber_wood_extraction' })
    event.remove({ id: 'gtceu:extractor/rubber_leaves_extraction' })
    event.remove({ id: 'gtceu:extractor/rubber_sapling_extraction' })

    // ---- Latex from plants (steam extractor, ULV) ----
    // One resin per handful of plants. Leaves and saplings take any tree
    // (minecraft tags), so old tree farms keep working with oak and friends.
    // Counts stay 1 item in / 1 item out: the extractor has a single slot each.
    event.recipes.gtceu.extractor('af9:sticky_resin_from_vines')
        .itemInputs('4x minecraft:vine')
        .itemOutputs('gtceu:sticky_resin')
        .duration(200)
        .EUt(ULV)
    event.recipes.gtceu.extractor('af9:sticky_resin_from_sugar_cane')
        .itemInputs('4x minecraft:sugar_cane')
        .itemOutputs('gtceu:sticky_resin')
        .duration(200)
        .EUt(ULV)
    event.recipes.gtceu.extractor('af9:sticky_resin_from_kelp')
        .itemInputs('4x minecraft:kelp')
        .itemOutputs('gtceu:sticky_resin')
        .duration(200)
        .EUt(ULV)
    event.recipes.gtceu.extractor('af9:sticky_resin_from_leaves')
        .itemInputs('16x #minecraft:leaves')
        .itemOutputs('gtceu:sticky_resin')
        .duration(200)
        .EUt(ULV)
    event.recipes.gtceu.extractor('af9:sticky_resin_from_saplings')
        .itemInputs('2x #minecraft:saplings')
        .itemOutputs('gtceu:sticky_resin')
        .duration(200)
        .EUt(ULV)
    // Slime stays: the pre-tree fallback (furnace smelting keeps working too).
    event.recipes.gtceu.extractor('af9:sticky_resin_from_slime')
        .itemInputs('minecraft:slime_ball')
        .itemOutputs('gtceu:sticky_resin')
        .duration(100)
        .EUt(ULV)

    // ---- Resin to raw rubber ----
    // Steam path: 1 resin -> 3 raw rubber dust, the old extractor yield.
    event.recipes.gtceu.extractor('af9:raw_rubber_from_sticky_resin')
        .itemInputs('gtceu:sticky_resin')
        .itemOutputs('3x gtceu:raw_rubber_dust')
        .duration(200)
        .EUt(ULV)
    // Electric path: the centrifuge is faster per resin and spins off a little
    // glue, with a small chance of a plant ball, like GT's own separation.
    event.recipes.gtceu.centrifuge('af9:raw_rubber_from_sticky_resin_centrifuge')
        .itemInputs('gtceu:sticky_resin')
        .itemOutputs('3x gtceu:raw_rubber_dust')
        .chancedOutput('gtceu:plant_ball', 1500, 0)
        .outputFluids(Fluid.of('gtceu:glue', 100))
        .duration(400)
        .EUt(LV)
})
