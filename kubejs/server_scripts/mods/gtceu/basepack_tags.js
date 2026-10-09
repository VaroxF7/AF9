// priority: 100
// AF9 - the apiary recipes of the base pack (mods/gtceu/apiary_recipes.js) read the flower of every GT bee from the item tag
// productivebees:flowers/<bee>, which Productive Bees builds from the bee's flowerTag while the data loads. The lepidolite
// bee's tag (forge:storage_blocks/raw_lepidolite, filled by the base pack's tags.js) came out empty in the 2026-10-09 18:10
// launch, and its 33 apiary recipes failed ("empty or unknown tag: #productivebees:flowers/lepidolite"). The tag is
// filled here as well, so the recipes get their flower whatever the order the two mods' data came in.

ServerEvents.tags('item', event => {
    event.add('productivebees:flowers/lepidolite', 'gtceu:raw_lepidolite_block')
})
