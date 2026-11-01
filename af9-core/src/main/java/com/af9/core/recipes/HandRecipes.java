package com.af9.core.recipes;

import com.af9.core.compat.tconstruct.TinkersGtRecipes;

import com.gregtechceu.gtceu.api.GTCEuAPI;
import com.gregtechceu.gtceu.api.data.chemical.ChemicalHelper;
import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.data.chemical.material.info.MaterialFlags;
import com.gregtechceu.gtceu.api.data.tag.TagPrefix;
import com.gregtechceu.gtceu.common.data.GTMaterials;

import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.function.Consumer;

/**
 * Recipes that were KubeJS scripts with logic the recorder cannot turn into a flat list: they ask GregTech or Java
 * for something while they run (the material registry, a ChemicalHelper lookup, TinkersGtRecipes).
 */
final class HandRecipes {

    private static final Logger LOGGER = LogManager.getLogger("AF9/Recipes");

    private HandRecipes() {}

    static void register(Consumer<FinishedRecipe> out) {
        fusionReactor(out);
        wireSpools(out);
        tinkersMolds(out);
    }

    /** fusion_reactor.js: the Fusion Reactor Mk1 and anti matter plasma. */
    private static void fusionReactor(Consumer<FinishedRecipe> out) {
        Removals.filter().output("gtceu:luv_fusion_reactor").remove();
        try {
            // the scan: GT's single ITBTC wire (whatever its id), else the superconducting coil
            ItemStack wire = ChemicalHelper.get(TagPrefix.wireGtSingle,
                    GTMaterials.get("indium_tin_barium_titanium_cuprate"));
            String scan = wire.isEmpty() ? "gtceu:superconducting_coil"
                    : ForgeRegistries.ITEMS.getKey(wire.getItem()).toString();
            Dsl.gt(out, "assembly_line", "af9:fusion_reactor_mk1")
                    .itemInputs("gtceu:superconducting_coil", "4x #gtceu:circuits/zpm",
                            "3x gtceu:double_plutonium_241_plate", "4x gtceu:plutonium_ingot",
                            "2x gtceu:double_osmiridium_plate", "2x gtceu:iv_field_generator",
                            "64x gtceu:uhpic_chip", "32x gtceu:indium_tin_barium_titanium_cuprate_single_wire",
                            "8x af9:vpu_chip", "8x af9:spin_logic_chip", "8x af9:photonic_ic_chip",
                            "4x gtceu:fusion_glass")
                    .inputFluids(Dsl.fluid("gtceu:soldering_alloy", 2304))
                    .inputFluids(Dsl.fluid("gtceu:niobium_titanium", 2304))
                    .itemOutputs("gtceu:luv_fusion_reactor")
                    .scannerResearch(scan, null, 8192, 1, 1200)
                    .duration(1000)
                    .EUt(32768)
                    .save();
        } catch (RuntimeException e) {
            Dsl.fail("af9:fusion_reactor_mk1", e);
        }
        try {
            Dsl.gt(out, "mega_fusion_reactor", "af9:anti_matter_plasma")
                    .inputFluids(Dsl.fluid("gtceu:niobium_plasma", 4000), Dsl.fluid("gtceu:sulfur_plasma", 4000),
                            Dsl.fluid("gtceu:nickel_plasma", 4000))
                    .outputFluids(Dsl.fluid("gtceu:anti_matter_plasma", 2000))
                    .duration(160)
                    .EUt(4194304)
                    .fusionStartEU(640000000L)
                    .save();
        } catch (RuntimeException e) {
            Dsl.fail("af9:anti_matter_plasma", e);
        }
    }

    /** wire_spools.js: 64 fine wires wind to one spool, for every material with fine wire. */
    private static void wireSpools(Consumer<FinishedRecipe> out) {
        for (Material material : GTCEuAPI.materialManager.getRegisteredMaterials()) {
            if (!material.hasFlag(MaterialFlags.GENERATE_FINE_WIRE)) continue;
            if (material.hasFlag(MaterialFlags.DISABLE_MATERIAL_RECIPES)) continue;
            String name = material.getName();
            try {
                Dsl.gt(out, "spooling", "af9:spool_" + name)
                        .itemInputs("64x #forge:fine_wires/" + name)
                        .itemOutputs("gtceu:" + name + "_wire_spool")
                        .duration((int) Math.max(20, material.getMass() * 4))
                        .EUt(32)
                        .save();
            } catch (RuntimeException e) {
                Dsl.fail("af9:spool_" + name, e);
            }
        }
    }

    /** tconstruct/gt_molds.js: the Smeltery and casting recipes TinkersGtRecipes builds from GT's materials. */
    private static void tinkersMolds(Consumer<FinishedRecipe> out) {
        String[] flat = TinkersGtRecipes.build();
        int n = 0;
        for (int i = 0; i + 1 < flat.length; i += 2) {
            try {
                Dsl.json(out, flat[i], flat[i + 1]);
                n++;
            } catch (RuntimeException e) {
                Dsl.fail(flat[i], e);
            }
        }
        LOGGER.info("Tinkers x GT: {} recipes", n);
    }
}
