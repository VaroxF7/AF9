ServerEvents.recipes((event) => {
    const gtr = event.recipes.gtceu

    // AF9: the void miner runs four AF9 modes now (void_mining_overworld, void_mining_nether, void_mining_end,
    // void_mining_asteroids). GT's and ATM's recipes of the old void_miner and world_data_scanner types go; only
    // af9: ids may remain. The removal mirrors the repo's event.remove({ id: ... }) filter style (boule_melting.js):
    // no precedent removes whole types, so the foreign ids are collected first and removed one by one.
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
            "4x kubejs:asic_chip", // AF9: the mining ASIC (chip_uses.js)
            // (the assembler has 9 item slots: with the ASIC the recipe had 10 and could not be loaded; the long titanium
            // rod is gone, the plates stay)
            "4x gtceu:titanium_plate")
        .inputFluids("gtceu:soldering_alloy 1440")
        .itemOutputs("gtceu:void_miner")
        .EUt(GTValues.VA[GTValues.HV])
        .duration(800)

    // The data sticks are gone: the void miner picks its dimension with the machine mode, not with a data item.
    // One recipe per table entry: [outputs (all counts x10 of GT's), circuit, id]. The id names the FIRST ore's
    // material. Circuit 8 of the overworld also opens on chalcopyrite, so the circuit number disambiguates it.
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

        [["120x gtceu:raw_oilsands"], "20", "af9:vm_overworld_oilsands"],

        [["60x gtceu:raw_graphite",
            "40x gtceu:raw_diamond",
            "20x gtceu:raw_coal"], "21", "af9:vm_overworld_graphite"],

        [["60x gtceu:raw_garnierite",
            "40x gtceu:raw_nickel",
            "40x gtceu:raw_cobaltite",
            "20x gtceu:raw_pentlandite"], "22", "af9:vm_overworld_garnierite"]]

    overworld_raw_ores.forEach((overworld_ore) => {
        let recipe = gtr.void_mining_overworld(overworld_ore[2])
            .inputFluids("gtceu:drilling_fluid 1000")
            .circuit(overworld_ore[1])
            .EUt(GTValues.VA[GTValues.EV])
            .duration(20)
        let output = overworld_ore[0]
        output.forEach(item => {
            recipe.chancedOutput(item, 2000, 0)
        })
    })
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

    nether_raw_ores.forEach((nether_ore) => {
        let recipe = gtr.void_mining_nether(nether_ore[2])
            .inputFluids("gtceu:drilling_fluid 1000")
            .circuit(nether_ore[1])
            .EUt(2 * GTValues.VA[GTValues.EV])
            .duration(20)
        let output = nether_ore[0]
        output.forEach(item => {
            recipe.chancedOutput(item, 2000, 0)
        })
    })

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

    end_raw_ores.forEach((end_ore) => {
        let recipe = gtr.void_mining_end(end_ore[2])
            .inputFluids("gtceu:drilling_fluid 1000")
            .circuit(end_ore[1])
            .EUt(GTValues.VA[GTValues.IV])
            .duration(20)
        let output = end_ore[0]
        output.forEach(item => {
            recipe.chancedOutput(item, 2000, 0)
        })
    })

    const asteroids_raw_ores =
        [[["40x gtceu:raw_brannerite"], "1", "af9:vm_asteroids_brannerite"],

        [["90x gtceu:raw_magnetite"], "2", "af9:vm_asteroids_magnetite"],

        [["40x gtceu:raw_pentlandite"], "3", "af9:vm_asteroids_pentlandite"],

        [["20x gtceu:raw_cooperite"], "4", "af9:vm_asteroids_cooperite"],

        [["90x gtceu:raw_naquadah"], "5", "af9:vm_asteroids_naquadah"],

        [["20x gtceu:raw_platinum"], "6", "af9:vm_asteroids_platinum"],

        [["40x af9:oil_regolith"], "7", "af9:vm_asteroids_oil_regolith"]]

    asteroids_raw_ores.forEach((asteroids_ore) => {
        let recipe = gtr.void_mining_asteroids(asteroids_ore[2])
            .inputFluids("gtceu:drilling_fluid 1000")
            .circuit(asteroids_ore[1])
            .EUt(GTValues.VA[GTValues.IV])
            .duration(20)
        let output = asteroids_ore[0]
        output.forEach(item => {
            recipe.chancedOutput(item, 2000, 0)
        })
    })
})
