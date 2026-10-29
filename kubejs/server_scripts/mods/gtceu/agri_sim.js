// AF9 - Agriculture Simulation Bed recipes (startup_scripts/gtceu/agri_sim.js).
// One recipe per seed, a minute at HV (1200 ticks, 512 EU/t): one seed and water in; the crop's yield and, mostly, the
// seed back out. The six recipe types are the bed's machine modes:
//   agri_sim_elemental  Mystical Agriculture elemental (0) and tier 1 essence seeds
//   agri_sim_tier2 .. 4 their tiers 2, 3 and 4
//   agri_sim_tier5      tier 5 and Mystical Agradditions' tier 6
//   agri_sim_crops      every other seed of the pack: the crops of vanilla, Farmer's Delight, Croptopia, Thermal ...
// Higher tiers give less essence per run, take more water, and bring fewer seeds back. Every seed is checked with
// Item.exists: a seed whose mod is not in the pack, or whose crop is switched off, simply has no recipe. The soil
// bonus, parallel hatch and batch mode are the machine's (af9-core AgriSimMachine). The seed id goes into the recipe's
// data (`seed`): the render grows that seed on the soil. The seed's tier and mode are in this file only; the list of
// Mystical Agriculture's crops is its ModCrops (7.0.23) and Mystical Agradditions' (7.0.11).

const AGRI_RUN_TICKS = 1200

// essence per run, water per run (mB), and the chance (hundredths of a percent) of the seed coming back, by crop tier
const AGRI_ESSENCE = [16, 16, 12, 10, 8, 6, 4]
const AGRI_WATER = [250, 500, 750, 1000, 1250, 1500, 1750]
const AGRI_SEED_BACK = [9500, 9500, 9000, 8500, 8000, 7500, 7000]

// the essence crops of Mystical Agriculture by tier (0 is the elemental tier); tier 6 is Mystical Agradditions'
const AGRI_ESSENCE_CROPS = {
    0: [
        'air', 'earth', 'water', 'fire'
    ],
    1: [
        'inferium', 'stone', 'dirt', 'wood', 'ice', 'deepslate'
    ],
    2: [
        'nature', 'dye', 'nether', 'coal', 'coral', 'honey', 'amethyst', 'pig', 'chicken', 'cow', 'sheep', 'squid',
        'fish', 'slime', 'turtle', 'rubber', 'silicon', 'sulfur', 'aluminum', 'saltpeter', 'apatite',
        'grains_of_infinity', 'mystical_flower', 'marble', 'limestone', 'basalt', 'menril'
    ],
    3: [
        'iron', 'copper', 'nether_quartz', 'glowstone', 'redstone', 'obsidian', 'prismarine', 'zombie', 'skeleton',
        'creeper', 'spider', 'rabbit', 'tin', 'bronze', 'zinc', 'brass', 'silver', 'lead', 'graphite', 'blizz',
        'blitz', 'basalz', 'amethyst_bronze', 'slimesteel', 'pig_iron', 'copper_alloy', 'redstone_alloy',
        'conductive_alloy', 'manasteel', 'steeleaf', 'ironwood', 'aquamarine', 'sky_stone', 'certus_quartz',
        'quartz_enriched_iron'
    ],
    4: [
        'gold', 'lapis_lazuli', 'end', 'experience', 'blaze', 'ghast', 'enderman', 'steel', 'nickel', 'constantan',
        'electrum', 'invar', 'mithril', 'tungsten', 'titanium', 'uranium', 'chrome', 'ruby', 'sapphire', 'peridot',
        'soulium', 'signalum', 'lumium', 'flux_infused_ingot', 'hop_graphite', 'cobalt', 'rose_gold', 'soularium',
        'dark_steel', 'pulsating_alloy', 'energetic_alloy', 'elementium', 'osmium', 'fluorite', 'refined_glowstone',
        'refined_obsidian', 'knightmetal', 'fiery_ingot', 'starmetal', 'compressed_iron', 'fluix', 'energized_steel',
        'blazing_crystal'
    ],
    5: [
        'diamond', 'emerald', 'netherite', 'wither_skeleton', 'platinum', 'iridium', 'enderium', 'flux_infused_gem',
        'manyullyn', 'queens_slime', 'hepatizon', 'vibrant_alloy', 'end_steel', 'terrasteel', 'rock_crystal',
        'draconium', 'yellorium', 'cyanite', 'niotic_crystal', 'spirited_crystal', 'uraninite'
    ],
    6: [
        'nether_star', 'dragon_egg', 'gaia_spirit', 'awakened_draconium', 'neutronium', 'nitro_crystal'
    ]
}

// the mode (recipe type) of each tier
const AGRI_MODE = ['agri_sim_elemental', 'agri_sim_elemental', 'agri_sim_tier2', 'agri_sim_tier3', 'agri_sim_tier4',
    'agri_sim_tier5', 'agri_sim_tier5']

// every other seed: [seed, [the crop's produce, the first that exists], how many per run]; with none of the produce
// there, the run multiplies the seed instead (the produce of those crops is not known to be in the pack)
const AGRI_CROPS = [
    ['minecraft:wheat_seeds', ['minecraft:wheat'], 12],
    ['minecraft:beetroot_seeds', ['minecraft:beetroot'], 12],
    ['minecraft:melon_seeds', ['minecraft:melon_slice'], 24],
    ['minecraft:pumpkin_seeds', ['minecraft:pumpkin'], 6],
    ['minecraft:torchflower_seeds', ['minecraft:torchflower'], 6],
    ['minecraft:pitcher_pod', ['minecraft:pitcher_plant'], 4],
    ['farmersdelight:cabbage_seeds', ['farmersdelight:cabbage'], 8],
    ['farmersdelight:tomato_seeds', ['farmersdelight:tomato'], 8],
    ['thermal:frost_melon_seeds', ['thermal:frost_melon_slice'], 12],
    ['croptopia:vanilla_seeds', ['croptopia:vanilla'], 8],
    ['croptopia:coffee_seed', ['croptopia:coffee_beans'], 8],
    ['croptopia:tea_seed', ['croptopia:tea_leaves'], 8],
    ['delightful:cantaloupe_seeds', ['delightful:cantaloupe_slice', 'delightful:cantaloupe'], 12],
    ['corn_delight:corn_seeds', ['corn_delight:corn_item', 'corn_delight:corn'], 8],
    ['immersiveengineering:seed', ['immersiveengineering:hemp_fiber'], 8],
    ['silentgear:flax_seeds', [], 6],
    ['supplementaries:flax_seeds', ['supplementaries:flax'], 8],
    ['occultism:datura_seeds', ['occultism:datura'], 6],
    ['forbidden_arcanus:golden_orchid_seeds', ['forbidden_arcanus:golden_orchid'], 4],
    ['blue_skies:fiery_bean_seeds', ['blue_skies:fiery_beans'], 6],
    ['blue_skies:pine_fruit_seeds', ['blue_skies:pine_fruit'], 6],
    ['blue_skies:scalefruit_seeds', ['blue_skies:scalefruit'], 6],
    ['blue_skies:winter_leaf_seeds', ['blue_skies:winter_leaf'], 6],
    ['undergarden:gloomgourd_seeds', ['undergarden:gloomgourd'], 4],
    ['maidensmerrymaking:ginger_seeds', [], 6],
    ['maidensmerrymaking:mint_seeds', [], 6],
    ['sushigocrafting:cucumber_seeds', [], 6],
    ['sushigocrafting:rice_seeds', [], 6],
    ['sushigocrafting:sesame_seeds', [], 6],
    ['sushigocrafting:sesame_seed', [], 6],
    ['sushigocrafting:soy_seeds', [], 6],
    ['sushigocrafting:wasabi_seeds', [], 6]
]
const THERMAL_CROPS = [
        'amaranth', 'barley', 'bell_pepper', 'coffee', 'corn', 'eggplant', 'flax', 'green_bean', 'hops', 'onion',
        'peanut', 'radish', 'rice', 'sadiroot', 'spinach', 'strawberry', 'tea', 'tomato'
]
const CROPTOPIA_CROPS = [
        'artichoke', 'asparagus', 'barley', 'basil', 'bellpepper', 'blackbean', 'blackberry', 'blueberry',
        'broccoli', 'cabbage', 'cantaloupe', 'cauliflower', 'celery', 'chile_pepper', 'corn', 'cranberry',
        'cucumber', 'currant', 'eggplant', 'elderberry', 'garlic', 'ginger', 'grape', 'greenbean', 'greenonion',
        'honeydew', 'hops', 'kale', 'kiwi', 'leek', 'lettuce', 'mustard', 'oat', 'olive', 'onion', 'peanut',
        'pepper', 'pineapple', 'radish', 'raspberry', 'rhubarb', 'rice', 'rutabaga', 'saguaro', 'soybean', 'spinach',
        'squash', 'strawberry', 'sweetpotato', 'tomatillo', 'tomato', 'turmeric', 'turnip', 'yam', 'zucchini'
]

// the first of a list that the test accepts, or null (Array.find is not trusted in Rhino)
const agriFirst = (list, test) => {
    for (let i = 0; i < list.length; i++) if (test(list[i])) return list[i]
    return null
}

ServerEvents.recipes(event => {
    const HV = GTValues.VA[GTValues.HV]

    // ---- the bed itself: an EV hull with a garden's worth of glass, rich soil and pumps ----
    event.shaped('gtceu:agri_sim', [
        'PCP',
        'GHG',
        'MSM'
    ], {
        P: '#forge:plates/titanium',
        C: '#gtceu:circuits/ev',
        G: 'gtceu:tempered_glass',
        H: 'gtceu:ev_machine_hull',
        M: 'gtceu:ev_electric_pump',
        S: 'farmersdelight:rich_soil'
    }).id('af9:agri_sim')

    // one run: the seed and water in, `produce` and the seed (with its chance) out
    const run = (type, id, seed, water, produce, count, seedBack) => {
        const r = event.recipes.gtceu[type]('af9:agri/' + id)
            .itemInputs(seed)
            .inputFluids(Fluid.of('minecraft:water', water))
            .itemOutputs(count + 'x ' + produce)
        if (produce === seed) {}
        else if (seedBack >= 10000) r.itemOutputs(seed)
        else r.chancedOutput(seed, seedBack, 0)
        r.addData('seed', seed).duration(AGRI_RUN_TICKS).EUt(HV)
    }

    // ---- Mystical Agriculture and Agradditions: their essence ----
    Object.keys(AGRI_ESSENCE_CROPS).forEach(key => {
        const tier = parseInt(key)
        AGRI_ESSENCE_CROPS[key].forEach(name => {
            const namespaces = tier == 6 ? ['mysticalagradditions', 'mysticalagriculture'] : ['mysticalagriculture']
            const ns = agriFirst(namespaces, n => Item.exists(n + ':' + name + '_seeds') && Item.exists(n + ':' + name + '_essence'))
            if (!ns) return
            run(AGRI_MODE[tier], 'essence/' + name, ns + ':' + name + '_seeds', AGRI_WATER[tier],
                ns + ':' + name + '_essence', AGRI_ESSENCE[tier], AGRI_SEED_BACK[tier])
        })
    })

    // ---- every other seed ----
    const crop = (id, seed, produce, count) => {
        if (!Item.exists(seed)) return
        const found = agriFirst(produce, p => Item.exists(p))
        if (found) run('agri_sim_crops', id, seed, 500, found, count, 9500)
        else run('agri_sim_crops', id, seed, 500, seed, count, 10000)
    }
    AGRI_CROPS.forEach(c => crop(c[0].replace(':', '/'), c[0], c[1], c[2]))
    THERMAL_CROPS.forEach(n => crop('thermal/' + n, 'thermal:' + n + '_seeds', ['thermal:' + n], 8))
    CROPTOPIA_CROPS.forEach(n => crop('croptopia/' + n, 'croptopia:' + n + '_seed', ['croptopia:' + n], 8))
})
