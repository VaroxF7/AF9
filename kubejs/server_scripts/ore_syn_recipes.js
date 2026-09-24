ServerEvents.recipes(event => {
    event.recipes.gtceu.ore_syn_chamber("olivine_vein")
    .itemInputs("immersiveengineering:graphite_electrode")
    .inputFluids([
        Fluid.of("gtceu:molten_molybdenum_disilicide",1000),
        Fluid.of("gtceu:radon",6000)
    ])
    .itemOutputs([
        Item.of("256x gtceu:raw_bentonite"),
        Item.of("125x gtceu:raw_magnetite"),
        Item.of("125x gtceu:raw_olivine"),
        Item.of("125x gtceu:raw_glauconite_sand"),
        Item.of("125x gtceu:raw_lithium")
    ])
    .circuit(1).duration(800).EUt(GTValues.VA[GTValues.IV])

    event.recipes.gtceu.ore_syn_chamber("sapphire_vein")
    .itemInputs("immersiveengineering:graphite_electrode")
    .inputFluids([
        Fluid.of("gtceu:molten_molybdenum_disilicide",1000),
        Fluid.of("gtceu:radon",6000)
    ])
    .itemOutputs([
        Item.of("256x gtceu:raw_almandine"),
        Item.of("256x gtceu:raw_pyrope"),
        Item.of("125x gtceu:raw_sapphire"),
        Item.of("256x gtceu:raw_green_sapphire"),
        Item.of("125x gtceu:raw_ruby")
    ])
    .circuit(2).duration(800).EUt(GTValues.VA[GTValues.IV])

    event.recipes.gtceu.ore_syn_chamber("iron_vein")
    .itemInputs("immersiveengineering:graphite_electrode")
    .inputFluids([
        Fluid.of("gtceu:molten_molybdenum_disilicide",1000),
        Fluid.of("gtceu:radon",6000)
    ])
    .itemOutputs([
        Item.of("256x gtceu:raw_goethite"),
        Item.of("512x gtceu:raw_magnetite"),
        Item.of("256x gtceu:raw_hematite"),
        Item.of("256x gtceu:raw_malachite")
    ])
    .circuit(3).duration(800).EUt(GTValues.VA[GTValues.IV])

    event.recipes.gtceu.ore_syn_chamber("lubri_vein")
    .itemInputs("immersiveengineering:graphite_electrode")
    .inputFluids([
        Fluid.of("gtceu:molten_molybdenum_disilicide",1000),
        Fluid.of("gtceu:radon",6000)
    ])
    .itemOutputs([
        Item.of("256x gtceu:raw_soapstone"),
        Item.of("512x gtceu:raw_talc"),
        Item.of("125x gtceu:raw_glauconite_sand"),
        Item.of("512x gtceu:raw_pentlandite")
    ])
    .circuit(4).duration(800).EUt(GTValues.VA[GTValues.IV])

    event.recipes.gtceu.ore_syn_chamber("tetra_vein")
    .itemInputs("immersiveengineering:graphite_electrode")
    .inputFluids([
        Fluid.of("gtceu:molten_molybdenum_disilicide",1800),
        Fluid.of("gtceu:radon",12000)
    ])
    .itemOutputs([
        Item.of("256x gtceu:raw_tetrahedrite"),
        Item.of("512x minecraft:raw_copper"),
        Item.of("125x gtceu:raw_stibnite"),
        Item.of("512x gtceu:raw_bastnasite")
    ])
    .circuit(5).duration(800).EUt(GTValues.VA[GTValues.IV])

    event.recipes.gtceu.ore_syn_chamber("monazite_vein")
    .itemInputs("immersiveengineering:graphite_electrode")
    .inputFluids([
        Fluid.of("gtceu:molten_molybdenum_disilicide",1000),
        Fluid.of("gtceu:radon",9000)
    ])
    .itemOutputs([
        Item.of("256x gtceu:raw_bastnasite"),
        Item.of("512x gtceu:raw_monazite"),
        Item.of("125x gtceu:raw_neodymium"),
        Item.of("512x gtceu:raw_cinnabar")
    ])
    .circuit(6).duration(800).EUt(GTValues.VA[GTValues.IV])

    event.recipes.gtceu.ore_syn_chamber("redstone_vein")
    .itemInputs("immersiveengineering:graphite_electrode")
    .inputFluids([
        Fluid.of("gtceu:molten_molybdenum_disilicide",2000),
        Fluid.of("gtceu:radon",6000)
    ])
    .itemOutputs([
        Item.of("256x gtceu:raw_redstone"),
        Item.of("512x gtceu:raw_ruby"),
        Item.of("512x gtceu:raw_cinnabar")
    ])
    .circuit(7).duration(800).EUt(GTValues.VA[GTValues.IV])

    event.recipes.gtceu.ore_syn_chamber("manganese_vein")
    .itemInputs("immersiveengineering:graphite_electrode")
    .inputFluids([
        Fluid.of("gtceu:molten_molybdenum_disilicide",3000),
        Fluid.of("gtceu:radon",12000)
    ])
    .itemOutputs([
        Item.of("256x gtceu:raw_grossular"),
        Item.of("512x gtceu:raw_spessartine"),
        Item.of("512x gtceu:raw_pyrolusite"),
        Item.of("256x gtceu:raw_tantalite")
    ])
    .circuit(8).duration(800).EUt(GTValues.VA[GTValues.IV])

    event.recipes.gtceu.ore_syn_chamber("copper_tin_vein")
    .itemInputs("immersiveengineering:graphite_electrode")
    .inputFluids([
        Fluid.of("gtceu:molten_molybdenum_disilicide",1000),
        Fluid.of("gtceu:radon",1200)
    ])
    .itemOutputs([
        Item.of("256x gtceu:raw_chalcopyrite"),
        Item.of("512x gtceu:raw_zeolite"),
        Item.of("512x gtceu:raw_cassiterite"),
        Item.of("256x gtceu:raw_realgar")
    ])
    .circuit(9).duration(800).EUt(GTValues.VA[GTValues.IV])

    event.recipes.gtceu.ore_syn_chamber("salpeter_vein")
    .itemInputs("immersiveengineering:graphite_electrode")
    .inputFluids([
        Fluid.of("gtceu:molten_molybdenum_disilicide",3000),
        Fluid.of("gtceu:radon",5000)
    ])
    .itemOutputs([
        Item.of("256x gtceu:raw_saltpeter"),
        Item.of("512x gtceu:raw_diatomite"),
        Item.of("512x gtceu:raw_electrotine"),
        Item.of("256x gtceu:raw_alunite")
    ])
    .circuit(10).duration(800).EUt(GTValues.VA[GTValues.IV])

    event.recipes.gtceu.ore_syn_chamber("coal_vein")
    .itemInputs("immersiveengineering:graphite_electrode")
    .inputFluids([
        Fluid.of("gtceu:molten_molybdenum_disilicide",1000),
        Fluid.of("gtceu:radon",6000)
    ])
    .itemOutputs([
        Item.of("512x gtceu:raw_coal"),
        Item.of("256x gtceu:raw_magnetite"),
        Item.of("512x gtceu:raw_electrotine"),
        Item.of("256x gtceu:raw_alunite")
    ])
    .circuit(10).duration(800).EUt(GTValues.VA[GTValues.IV])

    event.recipes.gtceu.ore_syn_chamber("naq_vein")
    .itemInputs("immersiveengineering:graphite_electrode")
    .inputFluids([
        Fluid.of("gtceu:molten_molybdenum_disilicide",1000),
        Fluid.of("gtceu:radon",6000)
    ])
    .itemOutputs([
        Item.of("512x gtceu:raw_naquadah"),
        Item.of("256x gtceu:raw_plutonium"),
        Item.of("512x gtceu:raw_uraninite"),
        Item.of("256x minecraft:raw_gold")
    ])
    .circuit(11).duration(800).EUt(GTValues.VA[GTValues.LuV])

    event.recipes.gtceu.ore_syn_chamber("mag_end_vein")
    .itemInputs("immersiveengineering:graphite_electrode")
    .inputFluids([
        Fluid.of("gtceu:molten_molybdenum_disilicide",1000),
        Fluid.of("gtceu:radon",6000)
    ])
    .itemOutputs([
        Item.of("512x gtceu:raw_magnetite"),
        Item.of("256x gtceu:raw_vanadium_magnetite"),
        Item.of("125x gtceu:raw_chromite"),
        Item.of("512x minecraft:raw_gold")
    ])
    .circuit(12).duration(800).EUt(GTValues.VA[GTValues.IV])

    event.recipes.gtceu.ore_syn_chamber("beryliium_vein")
    .itemInputs("immersiveengineering:graphite_electrode")
    .inputFluids([
        Fluid.of("gtceu:molten_molybdenum_disilicide",1000),
        Fluid.of("gtceu:radon",6000)
    ])
    .itemOutputs([
        Item.of("512x gtceu:raw_beryllium"),
        Item.of("256x gtceu:raw_emerald"),
        Item.of("125x gtceu:raw_thorium")
    ])
    .circuit(13).duration(800).EUt(GTValues.VA[GTValues.IV])

    event.recipes.gtceu.ore_syn_chamber("lapis_vein")
    .itemInputs("immersiveengineering:graphite_electrode")
    .inputFluids([
        Fluid.of("gtceu:molten_molybdenum_disilicide",1000),
        Fluid.of("gtceu:radon",6000)
    ])
    .itemOutputs([
        Item.of("512x gtceu:raw_lazurite"),
        Item.of("256x gtceu:raw_sodalite"),
        Item.of("512x gtceu:raw_lapis"),
        Item.of("256x gtceu:raw_calcite")
    ])
    .circuit(14).duration(800).EUt(GTValues.VA[GTValues.IV])

    event.recipes.gtceu.ore_syn_chamber("scheelite_vein")
    .itemInputs("immersiveengineering:graphite_electrode")
    .inputFluids([
        Fluid.of("gtceu:molten_molybdenum_disilicide",1000),
        Fluid.of("gtceu:radon",6000)
    ])
    .itemOutputs([
        Item.of("512x gtceu:raw_scheelite"),
        Item.of("256x gtceu:raw_tungstate"),
        Item.of("512x gtceu:raw_lithium"),
        Item.of("256x gtceu:raw_calcite")
    ])
    .circuit(15).duration(800).EUt(GTValues.VA[GTValues.IV])

    event.recipes.gtceu.ore_syn_chamber("galena_vein")
    .itemInputs("immersiveengineering:graphite_electrode")
    .inputFluids([
        Fluid.of("gtceu:molten_molybdenum_disilicide",1000),
        Fluid.of("gtceu:radon",6000)
    ])
    .itemOutputs([
        Item.of("512x gtceu:raw_galena"),
        Item.of("256x gtceu:raw_silver"),
        Item.of("512x gtceu:raw_lead"),
        Item.of("256x gtceu:raw_mica")
    ])
    .circuit(16).duration(800).EUt(GTValues.VA[GTValues.IV])

    event.recipes.gtceu.ore_syn_chamber("cassiterite_vein")
    .itemInputs("immersiveengineering:graphite_electrode")
    .inputFluids([
        Fluid.of("gtceu:molten_molybdenum_disilicide",1000),
        Fluid.of("gtceu:radon",6000)
    ])
    .itemOutputs([
        Item.of("256x gtceu:raw_cassiterite"),
        Item.of("256x gtceu:raw_tin"),
        Item.of("125x gtceu:raw_lead"),
        Item.of("256x gtceu:raw_copper")
    ])
    .circuit(17).duration(800).EUt(GTValues.VA[GTValues.IV])

    event.recipes.gtceu.ore_syn_chamber("garnet_vein")
    .itemInputs("immersiveengineering:graphite_electrode")
    .inputFluids([
        Fluid.of("gtceu:molten_molybdenum_disilicide",1000),
        Fluid.of("gtceu:radon",6000)
    ])
    .itemOutputs([
        Item.of("256x gtceu:raw_red_garnet"),
        Item.of("256x gtceu:raw_yellow_garnet"),
        Item.of("125x gtceu:raw_amethyst"),
        Item.of("256x gtceu:raw_opal")
    ])
    .circuit(18).duration(800).EUt(GTValues.VA[GTValues.IV])

    event.recipes.gtceu.ore_syn_chamber("mineral_sand_vein")
    .itemInputs("immersiveengineering:graphite_electrode")
    .inputFluids([
        Fluid.of("gtceu:molten_molybdenum_disilicide",1000),
        Fluid.of("gtceu:radon",6000)
    ])
    .itemOutputs([
        Item.of("256x gtceu:raw_basaltic_mineral_sand"),
        Item.of("256x gtceu:raw_granitic_mineral_sand"),
        Item.of("125x gtceu:raw_fullers_earth"),
        Item.of("256x gtceu:raw_gypsum")
    ])
    .circuit(19).duration(800).EUt(GTValues.VA[GTValues.IV])

    event.recipes.gtceu.ore_syn_chamber("baxuite_end_vein")
    .itemInputs("immersiveengineering:graphite_electrode")
    .inputFluids([
        Fluid.of("gtceu:molten_molybdenum_disilicide",1000),
        Fluid.of("gtceu:radon",6000)
    ])
    .itemOutputs([
        Item.of("256x gtceu:raw_bauxite"),
        Item.of("256x gtceu:raw_ilmenite"),
        Item.of("125x gtceu:raw_aluminium")
    ])
    .circuit(20).duration(800).EUt(GTValues.VA[GTValues.IV])

    event.recipes.gtceu.ore_syn_chamber("molybdenum_vein")
    .itemInputs("immersiveengineering:graphite_electrode")
    .inputFluids([
        Fluid.of("gtceu:molten_molybdenum_disilicide",1000),
        Fluid.of("gtceu:radon",6000)
    ])
    .itemOutputs([
        Item.of("256x gtceu:raw_wulfenite"),
        Item.of("256x gtceu:raw_molybdenite"),
        Item.of("125x gtceu:raw_molybdenum"),
        Item.of("125x gtceu:raw_powellite")
    ])
    .circuit(21).duration(800).EUt(GTValues.VA[GTValues.IV])

    event.recipes.gtceu.ore_syn_chamber("sheldonite_vein")
    .itemInputs("immersiveengineering:graphite_electrode")
    .inputFluids([
        Fluid.of("gtceu:molten_molybdenum_disilicide",1000),
        Fluid.of("gtceu:radon",6000)
    ])
    .itemOutputs([
        Item.of("256x gtceu:raw_bornite"),
        Item.of("256x gtceu:raw_cooperite"),
        Item.of("125x gtceu:raw_platinum"),
        Item.of("125x gtceu:raw_palladium")
    ])
    .circuit(22).duration(800).EUt(GTValues.VA[GTValues.IV])

    event.recipes.gtceu.ore_syn_chamber("pitchblend_vein")
    .itemInputs("immersiveengineering:graphite_electrode")
    .inputFluids([
        Fluid.of("gtceu:molten_molybdenum_disilicide",1000),
        Fluid.of("gtceu:radon",6000)
    ])
    .itemOutputs([
        Item.of("512x gtceu:raw_pitchblende"),
        Item.of("512x gtceu:raw_uraninite")
    ])
    .circuit(23).duration(800).EUt(GTValues.VA[GTValues.IV])

    event.recipes.gtceu.ore_syn_chamber("salts_vein")
    .itemInputs("immersiveengineering:graphite_electrode")
    .inputFluids([
        Fluid.of("gtceu:molten_molybdenum_disilicide",1000),
        Fluid.of("gtceu:radon",6000)
    ])
    .itemOutputs([
        Item.of("512x gtceu:raw_rock_salt"),
        Item.of("512x gtceu:raw_salt"),
        Item.of("256x gtceu:raw_lepidolite"),
        Item.of("125x gtceu:raw_spodumene")
    ])
    .circuit(24).duration(800).EUt(GTValues.VA[GTValues.IV])

    event.recipes.gtceu.ore_syn_chamber("band_iron_vein")
    .itemInputs("immersiveengineering:graphite_electrode")
    .inputFluids([
        Fluid.of("gtceu:molten_molybdenum_disilicide",1000),
        Fluid.of("gtceu:radon",6000)
    ])
    .itemOutputs([
        Item.of("512x gtceu:raw_goethite"),
        Item.of("512x gtceu:raw_yellow_limonite"),
        Item.of("256x gtceu:raw_hematite"),
        Item.of("125x minecraft:raw_gold"),
        Item.of("512x minecraft:redstone")
    ])
    .circuit(25).duration(500).EUt(GTValues.VA[GTValues.IV])

    event.recipes.gtceu.ore_syn_chamber("apatite_vein")
    .itemInputs("immersiveengineering:graphite_electrode")
    .inputFluids([
        Fluid.of("gtceu:molten_molybdenum_disilicide",1000),
        Fluid.of("gtceu:radon",6000)
    ])
    .itemOutputs([
        Item.of("512x gtceu:raw_apatite"),
        Item.of("512x gtceu:raw_tricalcium_phosphate"),
        Item.of("256x gtceu:raw_pyrochlore"),
        Item.of("125x gtceu:raw_fluorite")
    ])
    .circuit(26).duration(600).EUt(GTValues.VA[GTValues.IV])

    event.recipes.gtceu.ore_syn_chamber("certus_vein")
    .itemInputs("immersiveengineering:graphite_electrode")
    .inputFluids([
        Fluid.of("gtceu:molten_molybdenum_disilicide",2200),
        Fluid.of("gtceu:radon",6000)
    ])
    .itemOutputs([
        Item.of("512x gtceu:raw_quartzite"),
        Item.of("512x gtceu:raw_certus_quartz"),
        Item.of("256x gtceu:raw_barite"),
        Item.of("125x gtceu:raw_diamond")
    ])
    .circuit(27).duration(800).EUt(GTValues.VA[GTValues.IV])

    event.recipes.gtceu.ore_syn_chamber("diamond_vein")
    .itemInputs("immersiveengineering:graphite_electrode")
    .inputFluids([
        Fluid.of("gtceu:molten_molybdenum_disilicide",2000),
        Fluid.of("gtceu:radon",12000)
    ])
    .itemOutputs([
        Item.of("512x gtceu:raw_diamond"),
        Item.of("256x gtceu:raw_graphite"),
        Item.of("125x gtceu:raw_coal"),
    ])
    .circuit(28).duration(800).EUt(GTValues.VA[GTValues.IV])

    event.recipes.gtceu.ore_syn_chamber("sulfur_vein")
    .itemInputs("immersiveengineering:graphite_electrode")
    .inputFluids([
        Fluid.of("gtceu:molten_molybdenum_disilicide",2300),
        Fluid.of("gtceu:radon",7000)
    ])
    .itemOutputs([
        Item.of("512x gtceu:raw_sulfur"),
        Item.of("256x gtceu:raw_pyrite"),
        Item.of("125x gtceu:raw_sphalerite")
    ])
    .circuit(29).duration(800).EUt(GTValues.VA[GTValues.IV])

    event.recipes.gtceu.ore_syn_chamber("nickel_vein")
    .itemInputs("immersiveengineering:graphite_electrode")
    .inputFluids([
        Fluid.of("gtceu:molten_molybdenum_disilicide",2000),
        Fluid.of("gtceu:radon",3000)
    ])
    .itemOutputs([
        Item.of("256x gtceu:raw_garnierite"),
        Item.of("125x gtceu:raw_nickel"),
        Item.of("125x gtceu:raw_cobaltite"),
        Item.of("125x gtceu:raw_pentlandite")
    ])
    .circuit(30).duration(600).EUt(GTValues.VA[GTValues.IV])

    // Fuel recipes
    event.recipes.gtceu.electric_blast_furnace("hob_graphite_rod")
    .itemInputs("32x gtceu:activated_carbon_dust")
    .itemOutputs("immersiveengineering:graphite_electrode")
    .inputFluids([Fluid.of("gtceu:oxygen",2500)])
    .duration(200).EUt(125).blastFurnaceTemp(1800).circuit(2)

    // controller
    event.recipes.gtceu.assembler("syn_controller")
    .itemInputs("4x gtceu:high_temperature_smelting_casing","8x #gtceu:circuits/hv","4x gtceu:stainless_steel_frame")
    .itemOutputs("gtceu:ore_syn_chamber")
    .duration(160).EUt(GTValues.VA[GTValues.MV]).circuit(30)
})




