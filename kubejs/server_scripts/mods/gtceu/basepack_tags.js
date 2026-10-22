// priority: 100
// AF9 - the apiary recipes of the base pack (mods/gtceu/apiary_recipes.js) read the flower of every GT bee from the item tag
// productivebees:flowers/<bee>, which Productive Bees builds from the bee's flowerTag while the data loads. The lepidolite
// bee's tag (forge:storage_blocks/raw_lepidolite, filled by the base pack's tags.js) came out empty in the 2026-10-09 18:10
// launch, and its 33 apiary recipes failed ("empty or unknown tag: #productivebees:flowers/lepidolite"). The tag is
// filled here as well, so the recipes get their flower whatever the order the two mods' data came in.

// The 2026-10-22 server log: sixteen more of these tags are missing in this Productive Bees version (66 or 33 recipe
// errors each, 660 in all). Each gets one item that exists, the kind of block or flower the bee is named for. The
// recipes only hold the flower as a non-consumed input, so what the item is changes nothing else.
const AF9_BEE_FLOWERS = {
    redstone: 'minecraft:redstone_block',
    ender: 'minecraft:chorus_flower',
    crystalline: 'minecraft:amethyst_cluster',
    swamp_flowers: 'minecraft:blue_orchid',
    wither_flowers: 'minecraft:wither_rose',
    souled_flowers: 'minecraft:soul_soil',
    prismarine: 'minecraft:prismarine',
    magmatic_flowers: 'minecraft:magma_block',
    graves: 'minecraft:bone_block',
    glowing_flowers: 'minecraft:glow_lichen',
    gilded_flowers: 'minecraft:raw_gold_block',
    fiery: 'minecraft:blaze_powder',
    ferric_flowers: 'minecraft:raw_iron_block',
    draconic_flowers: 'minecraft:dragon_head',
    cupric_flowers: 'minecraft:raw_copper_block',
    burning: 'minecraft:crimson_roots'
}

ServerEvents.tags('item', event => {
    event.add('productivebees:flowers/lepidolite', 'gtceu:raw_lepidolite_block')
    Object.keys(AF9_BEE_FLOWERS).forEach(bee => event.add(`productivebees:flowers/${bee}`, AF9_BEE_FLOWERS[bee]))
})
