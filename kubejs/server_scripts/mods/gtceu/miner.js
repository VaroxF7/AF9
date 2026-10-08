ServerEvents.recipes((event) => {
    const gtr = event.recipes.gtceu

    // AF9: the void miner runs four AF9 modes now (void_mining_overworld, void_mining_nether, void_mining_end,
    // void_mining_asteroids) - and three machines run them. MK1 (gtceu:void_miner) takes the base types; MK2
    // (gtceu:void_miner_mk2) and MK3 (gtceu:void_miner_mk3) take their own _mk2 / _mk3 copies of the same four
    // (recipe types registered in kubejs/startup_scripts/gtceu/void_mining.js), so a machine only ever sees the
    // recipes of its own tier: MK2 gives 2x every output at 2 A for 600 ticks, MK3 3x at 3 A for 400
    // ticks - the same energy per ore, drawn faster and at more power. The extra power is amperage, not voltage: a
    // recipe keeps the base miner's voltage, so every tier runs on the hatches of its area (x2 / x3 voltage would put
    // MK2's End and MK3's Overworld a whole hatch tier higher). GT's and ATM's recipes of the old void_miner
    // and world_data_scanner types go; only af9: ids may remain. The removal mirrors the repo's
    // event.remove({ id: ... }) filter style (boule_melting.js): no precedent removes whole types, so the foreign
    // ids are collected first and removed one by one.
    const foreign = []
    event.forEachRecipe({ type: "gtceu:void_miner" }, (recipe) => {
        let id = String(recipe.getId())
        if (!id.startsWith("af9:")) foreign.push(id)
    })
    event.forEachRecipe({ type: "gtceu:world_data_scanner" }, (recipe) => {
        let id = String(recipe.getId())
        if (!id.startsWith("af9:")) foreign.push(id)
    })
    foreign.forEach((id) => {
        event.remove({ id: id })
    })

    gtr.assembler("gtceu:void_miner")
        .itemInputs("gtceu:lv_miner",
            "gtceu:mv_miner",
            "gtceu:hv_miner",
            "4x gtceu:lv_field_generator",
            "4x gtceu:mv_field_generator",
            "4x gtceu:hv_field_generator",
            "4x #gtceu:circuits/hv",
            "4x af9:asic_chip", // AF9: the mining ASIC (chip_uses.js)
            // (the assembler has 9 item slots: with the ASIC the recipe had 10 and could not be loaded; the long titanium
            // rod is gone, the plates stay)
            "4x gtceu:titanium_plate")
        .inputFluids("gtceu:soldering_alloy 1440")
        .itemOutputs("gtceu:void_miner")
        .EUt(GTValues.VA[GTValues.HV])
        .duration(800)

    // MK2 and MK3 assemble from the tier below plus that tier's parts (field generator, circuits, plates, the ASIC
    // again): 800 ticks each, at the tier's own voltage. The recipe ids are the machine ids; the KubeJS startup
    // script registers both machines (along with the _mk2 / _mk3 recipe types).
    // GT's tungsten steel material is tungsten_steel, so its plate is gtceu:tungsten_steel_plate: the material ids
    // keep their underscore (as in particle_accelerator.js, niobium_titanium_plate).
    gtr.assembler("gtceu:void_miner_mk2")
        .itemInputs("gtceu:void_miner",
            "4x gtceu:ev_field_generator",
            "4x #gtceu:circuits/ev",
            "4x af9:asic_chip", // AF9: the mining ASIC (chip_uses.js)
            "4x gtceu:tungsten_plate")
        .inputFluids("gtceu:soldering_alloy 1440")
        .itemOutputs("gtceu:void_miner_mk2")
        .EUt(GTValues.VA[GTValues.EV])
        .duration(800)

    gtr.assembler("gtceu:void_miner_mk3")
        .itemInputs("gtceu:void_miner_mk2",
            "4x gtceu:luv_field_generator",
            "4x #gtceu:circuits/luv",
            "4x af9:asic_chip", // AF9: the mining ASIC (chip_uses.js)
            "4x gtceu:tungsten_steel_plate")
        .inputFluids("gtceu:soldering_alloy 1440")
        .itemOutputs("gtceu:void_miner_mk3")
        .EUt(GTValues.VA[GTValues.IV])
        .duration(800)

    // The data sticks are gone: the void miner picks its dimension with the machine mode, not with a data item.
    // And it mines standing in it: every recipe carries GT's dimension condition, written NON-reversed for each
    // dimension of the mode. GT ORs conditions of one type, and a reversed one reads "the machine is NOT there" -
    // two reversed conditions (the form this file used to have) are true anywhere, so every mode ran in every
    // dimension. The lists are the *_dimensions consts below; a formed miner standing in the wrong one has no
    // recipe to run and its console reports VoidMinerMachine's STATUS_NO_DIMENSION instead.
    const overworld_dimensions = ['minecraft:overworld', 'allthemodium:mining']
    const nether_dimensions = ['minecraft:the_nether']
    const end_dimensions = ['minecraft:the_end']
    const asteroids_dimensions = ['af9:asteroid_field', 'af9:ceres']

    // emitRecipes(typeBase, table, euT, dimensions, m): the tables are MK1's, m scales them (counts and amperage up,
    // duration down: 1200 / m ticks), so each ore costs the same energy however the tier draws it. Drilling fluid,
    // circuit, chances and dimensions stay as the table says. The recipe type is typeBase plus '' / '_mk2' /
    // '_mk3' - gtceu:void_miner runs the four base types, void_miner_mk2 and void_miner_mk3 the suffixed copies.
    function emitRecipes(typeBase, table, euT, dimensions, m) {
        const suffix = m === 1 ? '' : '_mk' + m
        const type = typeBase + suffix
        table.forEach((entry) => {
            let recipe = gtr[type](entry[2] + suffix)
                .inputFluids("gtceu:drilling_fluid 2000")
            dimensions.forEach((dimension) => {
                recipe.dimension(dimension)
            })
            recipe.circuit(entry[1])
                .EUt(euT, m)
                .duration(1200 / m)
            entry[0].forEach((line) => {
                recipe.chancedOutput(scaleCount(line, m), 2000, 0)
            })
        })
    }

    // "30x gtceu:raw_bentonite" -> "60x gtceu:raw_bentonite" at m = 2: the table strings carry MK1's counts
    function scaleCount(line, m) {
        if (m === 1) return line
        const counted = line.match(/^(\d+)x\s+(.+)$/)
        return counted ? String(Number(counted[1]) * m) + 'x ' + counted[2] : line
    }

    // One recipe per table entry: [outputs (all counts x10 of GT's), circuit, id]. The id names the FIRST ore's
    // material. Circuit 8 of the overworld also opens on chalcopyrite, so the circuit number disambiguates it.
    // Each table is written ONCE, in MK1 counts, and emitted once per tier by emitRecipes() above: MK1 as written,
    // MK2 with every count and the amperage times 2 for 600 ticks, MK3 with times 3 for 400 ticks. The tier suffix
    // (_mk2 / _mk3) goes on the recipe id too - the base keeps af9:vm_..., two types would otherwise hold recipes
    // with one id (lint R1).
    // MK1, MK2, MK3: the multipliers every table is emitted with
    const tier_multipliers = [1, 2, 3]

    const overworld_raw_ores =
        [[["30x gtceu:raw_bentonite",
            "20x gtceu:raw_magnetite",
            "20x gtceu:raw_olivine",
            "10x gtceu:raw_glauconite_sand"], "1", "af9:vm_overworld_bentonite"],

        [["90x gtceu:raw_almandine",
            "60x gtceu:raw_pyrope",
            "30x gtceu:raw_sapphire",
            "30x gtceu:raw_green_sapphire"], "2", "af9:vm_overworld_almandine"],

        [["30x gtceu:raw_goethite",
            "120x gtceu:raw_yellow_limonite",
            "120x gtceu:raw_hematite",
            "60x gtceu:raw_malachite"], "3", "af9:vm_overworld_goethite"],

        [["60x gtceu:raw_soapstone",
            "40x gtceu:raw_talc",
            "40x gtceu:raw_glauconite_sand",
            "20x gtceu:raw_pentlandite"], "4", "af9:vm_overworld_soapstone"],

        [["30x gtceu:raw_grossular",
            "20x gtceu:raw_spessartine",
            "20x gtceu:raw_pyrolusite",
            "10x gtceu:raw_tantalite"], "5", "af9:vm_overworld_grossular"],

        [["130x gtceu:raw_chalcopyrite",
            "10x gtceu:raw_zeolite",
            "10x gtceu:raw_cassiterite",
            "30x gtceu:raw_realgar"], "6", "af9:vm_overworld_chalcopyrite"],

        [["120x gtceu:raw_coal"], "7", "af9:vm_overworld_coal"],

        [["20x gtceu:raw_chalcopyrite",
            "80x minecraft:raw_iron",
            "80x gtceu:raw_pyrite",
            "80x minecraft:raw_copper"], "8", "af9:vm_overworld_chalcopyrite_8"],

        [["120x gtceu:raw_magnetite",
            "80x gtceu:raw_vanadium_magnetite",
            "40x minecraft:raw_gold"], "9", "af9:vm_overworld_magnetite"],

        [["60x gtceu:raw_lazurite",
            "40x gtceu:raw_sodalite",
            "40x gtceu:raw_lapis",
            "20x gtceu:raw_calcite"], "10", "af9:vm_overworld_lazurite"],

        [["60x gtceu:raw_galena",
            "40x gtceu:raw_silver",
            "20x gtceu:raw_lead"], "11", "af9:vm_overworld_galena"],

        [["30x gtceu:raw_kyanite",
            "20x gtceu:raw_mica",
            "20x gtceu:raw_bauxite",
            "10x gtceu:raw_pollucite"], "12", "af9:vm_overworld_kyanite"],

        [["160x gtceu:raw_tin",
            "80x gtceu:raw_cassiterite"], "13", "af9:vm_overworld_tin"],

        [["60x gtceu:raw_red_garnet",
            "40x gtceu:raw_yellow_garnet",
            "40x gtceu:raw_amethyst",
            "20x gtceu:raw_opal"], "14", "af9:vm_overworld_red_garnet"],

        [["120x gtceu:raw_basaltic_mineral_sand",
            "80x gtceu:raw_granitic_mineral_sand",
            "80x gtceu:raw_fullers_earth",
            "40x gtceu:raw_gypsum"], "15", "af9:vm_overworld_basaltic_mineral_sand"],

        [["80x gtceu:raw_rock_salt",
            "10x gtceu:raw_salt",
            "30x gtceu:raw_lepidolite",
            "30x gtceu:raw_spodumene"], "16", "af9:vm_overworld_rock_salt"],

        [["90x gtceu:raw_redstone",
            "60x gtceu:raw_ruby",
            "30x gtceu:raw_cinnabar"], "17", "af9:vm_overworld_redstone"],

        [["60x gtceu:raw_apatite",
            "40x gtceu:raw_tricalcium_phosphate",
            "20x gtceu:raw_pyrochlore"], "18", "af9:vm_overworld_apatite"],

        [["120x gtceu:raw_cassiterite_sand",
            "80x gtceu:raw_garnet_sand",
            "80x gtceu:raw_asbestos",
            "40x gtceu:raw_diatomite"], "19", "af9:vm_overworld_cassiterite_sand"],

        [["60x gtceu:raw_graphite",
            "40x gtceu:raw_diamond",
            "20x gtceu:raw_coal"], "21", "af9:vm_overworld_graphite"],

        [["60x gtceu:raw_garnierite",
            "40x gtceu:raw_nickel",
            "40x gtceu:raw_cobaltite",
            "20x gtceu:raw_pentlandite"], "22", "af9:vm_overworld_garnierite"]]

    // Overworld: the Mining Dimension counts as the Overworld, hence both dimensions in the list
    tier_multipliers.forEach((m) => emitRecipes("void_mining_overworld", overworld_raw_ores,
        GTValues.VA[GTValues.EV], overworld_dimensions, m))
    const nether_raw_ores =
        [[["140x gtceu:raw_tetrahedrite",
            "70x minecraft:raw_copper",
            "40x gtceu:raw_stibnite"], "1", "af9:vm_nether_tetrahedrite"],

        [["50x gtceu:raw_bastnasite",
            "20x gtceu:raw_molybdenum",
            "20x gtceu:raw_neodymium",
            "20x gtceu:raw_monazite"], "2", "af9:vm_nether_bastnasite"],

        [["90x gtceu:raw_redstone",
            "60x gtceu:raw_ruby",
            "30x gtceu:raw_cinnabar"], "3", "af9:vm_nether_redstone"],

        [["60x gtceu:raw_saltpeter",
            "40x gtceu:raw_diatomite",
            "40x gtceu:raw_electrotine",
            "20x gtceu:raw_alunite"], "4", "af9:vm_nether_saltpeter"],

        [["50x gtceu:raw_beryllium",
            "60x gtceu:raw_emerald"], "5", "af9:vm_nether_beryllium"],

        [["30x gtceu:raw_grossular",
            "20x gtceu:raw_pyrolusite",
            "10x gtceu:raw_tantalite"], "6", "af9:vm_nether_grossular"],

        [["80x gtceu:raw_wulfenite",
            "50x gtceu:raw_molybdenite",
            "30x gtceu:raw_molybdenum",
            "30x gtceu:raw_powellite"], "7", "af9:vm_nether_wulfenite"],

        [["50x gtceu:raw_goethite",
            "30x gtceu:raw_yellow_limonite",
            "30x gtceu:raw_hematite",
            "20x minecraft:raw_gold"], "8", "af9:vm_nether_goethite"],

        [["60x gtceu:raw_quartzite",
            "40x gtceu:raw_certus_quartz",
            "20x gtceu:raw_barite"], "9", "af9:vm_nether_quartzite"],

        [["110x gtceu:raw_blue_topaz",
            "70x gtceu:raw_topaz",
            "70x gtceu:raw_chalcocite",
            "40x gtceu:raw_bornite"], "10", "af9:vm_nether_blue_topaz"],

        [["120x gtceu:raw_nether_quartz",
            "40x gtceu:raw_quartzite"], "11", "af9:vm_nether_nether_quartz"],

        [["150x gtceu:raw_sulfur",
            "100x gtceu:raw_pyrite",
            "50x gtceu:raw_sphalerite"], "12", "af9:vm_nether_sulfur"]]

    // Nether: the mode is the Nether alone
    tier_multipliers.forEach((m) => emitRecipes("void_mining_nether", nether_raw_ores,
        2 * GTValues.VA[GTValues.EV], nether_dimensions, m))

    const end_raw_ores =
        [[["90x gtceu:raw_magnetite",
            "60x gtceu:raw_vanadium_magnetite",
            "60x gtceu:raw_chromite",
            "30x minecraft:raw_gold"], "1", "af9:vm_end_magnetite"],

        [["80x gtceu:raw_bauxite",
            "40x gtceu:raw_ilmenite",
            "40x gtceu:raw_aluminium"], "2", "af9:vm_end_bauxite"],

        [["30x gtceu:raw_bornite",
            "20x gtceu:raw_cooperite",
            "20x gtceu:raw_platinum",
            "10x gtceu:raw_palladium"], "3", "af9:vm_end_bornite"],

        [["60x gtceu:raw_scheelite",
            "40x gtceu:raw_tungstate",
            "20x gtceu:raw_lithium"], "4", "af9:vm_end_scheelite"],

        // Circuit 5 was pitchblende and uraninite, and circuit 6 carried raw plutonium with the naquadah: uranium and
        // plutonium come from the Asteroid Field now (asteroid_fission.js), no void miner makes them
        [["90x gtceu:raw_naquadah"], "6", "af9:vm_end_naquadah"]]

    // End: the mode is the End alone
    tier_multipliers.forEach((m) => emitRecipes("void_mining_end", end_raw_ores,
        GTValues.VA[GTValues.IV], end_dimensions, m))

    const asteroids_raw_ores =
        [[["40x gtceu:raw_brannerite"], "1", "af9:vm_asteroids_brannerite"],

        [["90x gtceu:raw_magnetite"], "2", "af9:vm_asteroids_magnetite"],

        [["40x gtceu:raw_pentlandite"], "3", "af9:vm_asteroids_pentlandite"],

        [["20x gtceu:raw_cooperite"], "4", "af9:vm_asteroids_cooperite"],

        [["90x gtceu:raw_naquadah"], "5", "af9:vm_asteroids_naquadah"],

        [["20x gtceu:raw_platinum"], "6", "af9:vm_asteroids_platinum"],

        [["40x af9:oil_regolith"], "7", "af9:vm_asteroids_oil_regolith"]]

    // Asteroids: the belt and Ceres are one area, the machine may stand in either
    tier_multipliers.forEach((m) => emitRecipes("void_mining_asteroids", asteroids_raw_ores,
        GTValues.VA[GTValues.IV], asteroids_dimensions, m))
})
