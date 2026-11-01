package com.af9.core.recipes.gen;

import com.af9.core.recipes.Dsl;
import com.af9.core.recipes.Removals;

import net.minecraft.data.recipes.FinishedRecipe;

import java.util.function.Consumer;

/**
 * Staged Assembly recipes (was kubejs/server_scripts/mods/gtceu/staged_assembly.js; machine and StagedRecipes in af9-core).
 * A staged recipe is one recipe whose inputs arrive in layers: every layer's inputs are appended in order and addStaged records
 * which content index belongs to which layer in the recipe's data (af9_staged, read by StagedRecipes); the machine splits it
 * back into stages at runtime. Inputs outside the layers (global items and fluids) go into every stage; energy always does.
 * Limits (the JEI page fits this much): 1-7 layers, at most 5 item + fluid inputs per layer.
 */
public final class GenStagedAssembly {

    private GenStagedAssembly() {}

    public static void register(Consumer<FinishedRecipe> out) {
        removals();
        part0(out);
    }

    private static void part0(Consumer<FinishedRecipe> out) {
        Dsl.json(out, "af9:staged_assembly", "{\"type\":\"minecraft:crafting_shaped\",\"pattern\":[\"RCR\",\"VAV\",\"WCW\"],\"key\":{\"R\":{\"item\":\"gtceu:lv_robot_arm\"},\"C\":{\"tag\":\"gtceu:circuits/lv\"},\"V\":{\"item\":\"gtceu:lv_conveyor_module\"},\"A\":{\"item\":\"gtceu:lv_assembler\"},\"W\":{\"item\":\"gtceu:tin_single_cable\"}},\"result\":{\"item\":\"gtceu:staged_assembly\"}}");
        Dsl.json(out, "af9:staged_step_detector", "{\"type\":\"minecraft:crafting_shapeless\",\"ingredients\":[{\"item\":\"gtceu:activity_detector_cover\"},{\"item\":\"minecraft:comparator\"}],\"result\":{\"item\":\"af9:staged_step_detector\"}}");

        try {
            Dsl.gt(out, "staged_assembly", "af9:staged_demo_arm")
                    .itemInputs("2x gtceu:steel_plate", "gtceu:lv_electric_motor", "2x gtceu:copper_single_cable", "gtceu:lv_sensor", "gtceu:epoxy_printed_circuit_board")
                    .addStaged(0, 0, 200, 2, 0, 200, 2, 0, 200, 1, 0)
                    .itemOutputs("gtceu:lv_robot_arm")
                    .duration(600)
                    .EUt(32)
                    .save();
        } catch (RuntimeException e) {
            Dsl.fail("af9:staged_demo_arm", e);
        }
        try {
            Dsl.gt(out, "staged_assembly", "af9:staged_demo_conveyor")
                    .itemInputs("gtceu:aluminium_frame", "4x gtceu:aluminium_plate", "2x gtceu:stainless_steel_rod", "gtceu:mv_electric_piston", "4x gtceu:gold_single_cable")
                    .inputFluids(Dsl.fluid("gtceu:lubricant", 500))
                    .addStaged(1, 1, 300, 2, 0, 300, 2, 0)
                    .itemOutputs("gtceu:mv_conveyor_module")
                    .duration(600)
                    .EUt(128)
                    .save();
        } catch (RuntimeException e) {
            Dsl.fail("af9:staged_demo_conveyor", e);
        }
        try {
            Dsl.gt(out, "staged_assembly", "af9:staged_lv_conveyor")
                    .itemInputs("2x gtceu:lv_electric_motor", "gtceu:tin_single_cable", "6x #forge:plates/rubber")
                    .addStaged(0, 0, 100, 2, 0, 100, 1, 0)
                    .itemOutputs("gtceu:lv_conveyor_module")
                    .duration(200)
                    .EUt(32)
                    .save();
        } catch (RuntimeException e) {
            Dsl.fail("af9:staged_lv_conveyor", e);
        }
        try {
            Dsl.gt(out, "staged_assembly", "af9:staged_lv_pump")
                    .itemInputs("gtceu:lv_electric_motor", "gtceu:tin_single_cable", "gtceu:bronze_normal_fluid_pipe", "#forge:screws/tin", "#forge:rotors/tin", "2x #forge:rings/rubber")
                    .addStaged(0, 0, 100, 3, 0, 100, 3, 0)
                    .itemOutputs("gtceu:lv_electric_pump")
                    .duration(200)
                    .EUt(32)
                    .save();
        } catch (RuntimeException e) {
            Dsl.fail("af9:staged_lv_pump", e);
        }
        try {
            Dsl.gt(out, "staged_assembly", "af9:staged_lv_piston")
                    .itemInputs("gtceu:lv_electric_motor", "2x #forge:rods/steel", "2x gtceu:tin_single_cable", "3x #forge:plates/steel", "#forge:small_gears/steel")
                    .addStaged(0, 0, 100, 3, 0, 100, 2, 0)
                    .itemOutputs("gtceu:lv_electric_piston")
                    .duration(200)
                    .EUt(32)
                    .save();
        } catch (RuntimeException e) {
            Dsl.fail("af9:staged_lv_piston", e);
        }
        try {
            Dsl.gt(out, "staged_assembly", "af9:staged_lv_arm")
                    .itemInputs("2x gtceu:lv_electric_motor", "2x #forge:rods/steel", "3x gtceu:tin_single_cable", "gtceu:lv_electric_piston", "#gtceu:circuits/lv")
                    .addStaged(0, 0, 150, 3, 0, 150, 2, 0)
                    .itemOutputs("gtceu:lv_robot_arm")
                    .duration(300)
                    .EUt(32)
                    .save();
        } catch (RuntimeException e) {
            Dsl.fail("af9:staged_lv_arm", e);
        }
        try {
            Dsl.gt(out, "staged_assembly", "af9:staged_lv_emitter")
                    .itemInputs("4x #forge:rods/brass", "2x gtceu:tin_single_cable", "2x #gtceu:circuits/lv", "#forge:gems/quartzite")
                    .addStaged(0, 0, 100, 2, 0, 100, 2, 0)
                    .itemOutputs("gtceu:lv_emitter")
                    .duration(200)
                    .EUt(32)
                    .save();
        } catch (RuntimeException e) {
            Dsl.fail("af9:staged_lv_emitter", e);
        }
        try {
            Dsl.gt(out, "staged_assembly", "af9:staged_lv_sensor")
                    .itemInputs("#forge:rods/brass", "4x #forge:plates/steel", "#gtceu:circuits/lv", "#forge:gems/quartzite")
                    .addStaged(0, 0, 100, 2, 0, 100, 2, 0)
                    .itemOutputs("gtceu:lv_sensor")
                    .duration(200)
                    .EUt(32)
                    .save();
        } catch (RuntimeException e) {
            Dsl.fail("af9:staged_lv_sensor", e);
        }
        try {
            Dsl.gt(out, "staged_assembly", "af9:staged_lv_field")
                    .itemInputs("#forge:ender_pearls", "2x #forge:plates/steel", "2x #gtceu:circuits/lv", "4x gtceu:manganese_phosphide_quadruple_wire")
                    .addStaged(0, 0, 300, 2, 0, 300, 2, 0)
                    .itemOutputs("gtceu:lv_field_generator")
                    .duration(600)
                    .EUt(32)
                    .save();
        } catch (RuntimeException e) {
            Dsl.fail("af9:staged_lv_field", e);
        }
        try {
            Dsl.gt(out, "staged_assembly", "af9:staged_mv_conveyor")
                    .itemInputs("2x gtceu:mv_electric_motor", "gtceu:copper_single_cable", "6x #forge:plates/rubber")
                    .addStaged(0, 0, 100, 2, 0, 100, 1, 0)
                    .itemOutputs("gtceu:mv_conveyor_module")
                    .duration(200)
                    .EUt(128)
                    .save();
        } catch (RuntimeException e) {
            Dsl.fail("af9:staged_mv_conveyor", e);
        }
        try {
            Dsl.gt(out, "staged_assembly", "af9:staged_mv_pump")
                    .itemInputs("gtceu:mv_electric_motor", "gtceu:copper_single_cable", "gtceu:steel_normal_fluid_pipe", "#forge:screws/bronze", "#forge:rotors/bronze", "2x #forge:rings/rubber")
                    .addStaged(0, 0, 100, 3, 0, 100, 3, 0)
                    .itemOutputs("gtceu:mv_electric_pump")
                    .duration(200)
                    .EUt(128)
                    .save();
        } catch (RuntimeException e) {
            Dsl.fail("af9:staged_mv_pump", e);
        }
        try {
            Dsl.gt(out, "staged_assembly", "af9:staged_mv_piston")
                    .itemInputs("gtceu:mv_electric_motor", "2x #forge:rods/aluminium", "2x gtceu:copper_single_cable", "3x #forge:plates/aluminium", "#forge:small_gears/aluminium")
                    .addStaged(0, 0, 100, 3, 0, 100, 2, 0)
                    .itemOutputs("gtceu:mv_electric_piston")
                    .duration(200)
                    .EUt(128)
                    .save();
        } catch (RuntimeException e) {
            Dsl.fail("af9:staged_mv_piston", e);
        }
        try {
            Dsl.gt(out, "staged_assembly", "af9:staged_mv_arm")
                    .itemInputs("2x gtceu:mv_electric_motor", "2x #forge:rods/aluminium", "3x gtceu:copper_single_cable", "gtceu:mv_electric_piston", "#gtceu:circuits/mv")
                    .addStaged(0, 0, 150, 3, 0, 150, 2, 0)
                    .itemOutputs("gtceu:mv_robot_arm")
                    .duration(300)
                    .EUt(128)
                    .save();
        } catch (RuntimeException e) {
            Dsl.fail("af9:staged_mv_arm", e);
        }
        try {
            Dsl.gt(out, "staged_assembly", "af9:staged_mv_emitter")
                    .itemInputs("4x #forge:rods/electrum", "2x gtceu:copper_single_cable", "2x #gtceu:circuits/mv", "#forge:flawless_gems/emerald")
                    .addStaged(0, 0, 100, 2, 0, 100, 2, 0)
                    .itemOutputs("gtceu:mv_emitter")
                    .duration(200)
                    .EUt(128)
                    .save();
        } catch (RuntimeException e) {
            Dsl.fail("af9:staged_mv_emitter", e);
        }
        try {
            Dsl.gt(out, "staged_assembly", "af9:staged_mv_sensor")
                    .itemInputs("#forge:rods/electrum", "4x #forge:plates/aluminium", "#gtceu:circuits/mv", "#forge:flawless_gems/emerald")
                    .addStaged(0, 0, 100, 2, 0, 100, 2, 0)
                    .itemOutputs("gtceu:mv_sensor")
                    .duration(200)
                    .EUt(128)
                    .save();
        } catch (RuntimeException e) {
            Dsl.fail("af9:staged_mv_sensor", e);
        }
        try {
            Dsl.gt(out, "staged_assembly", "af9:staged_mv_field")
                    .itemInputs("minecraft:ender_eye", "2x #forge:plates/aluminium", "2x #gtceu:circuits/mv", "4x gtceu:magnesium_diboride_quadruple_wire")
                    .addStaged(0, 0, 300, 2, 0, 300, 2, 0)
                    .itemOutputs("gtceu:mv_field_generator")
                    .duration(600)
                    .EUt(128)
                    .save();
        } catch (RuntimeException e) {
            Dsl.fail("af9:staged_mv_field", e);
        }
        try {
            Dsl.gt(out, "staged_assembly", "af9:staged_hv_conveyor")
                    .itemInputs("2x gtceu:hv_electric_motor", "gtceu:gold_single_cable", "6x #forge:plates/rubber")
                    .addStaged(0, 0, 100, 2, 0, 100, 1, 0)
                    .itemOutputs("gtceu:hv_conveyor_module")
                    .duration(200)
                    .EUt(512)
                    .save();
        } catch (RuntimeException e) {
            Dsl.fail("af9:staged_hv_conveyor", e);
        }
        try {
            Dsl.gt(out, "staged_assembly", "af9:staged_hv_pump")
                    .itemInputs("gtceu:hv_electric_motor", "gtceu:gold_single_cable", "gtceu:stainless_steel_normal_fluid_pipe", "#forge:screws/steel", "#forge:rotors/steel", "2x #forge:rings/rubber")
                    .addStaged(0, 0, 100, 3, 0, 100, 3, 0)
                    .itemOutputs("gtceu:hv_electric_pump")
                    .duration(200)
                    .EUt(512)
                    .save();
        } catch (RuntimeException e) {
            Dsl.fail("af9:staged_hv_pump", e);
        }
        try {
            Dsl.gt(out, "staged_assembly", "af9:staged_hv_piston")
                    .itemInputs("gtceu:hv_electric_motor", "2x #forge:rods/stainless_steel", "2x gtceu:gold_single_cable", "3x #forge:plates/stainless_steel", "#forge:small_gears/stainless_steel")
                    .addStaged(0, 0, 100, 3, 0, 100, 2, 0)
                    .itemOutputs("gtceu:hv_electric_piston")
                    .duration(200)
                    .EUt(512)
                    .save();
        } catch (RuntimeException e) {
            Dsl.fail("af9:staged_hv_piston", e);
        }
        try {
            Dsl.gt(out, "staged_assembly", "af9:staged_hv_arm")
                    .itemInputs("2x gtceu:hv_electric_motor", "2x #forge:rods/stainless_steel", "3x gtceu:gold_single_cable", "gtceu:hv_electric_piston", "#gtceu:circuits/hv")
                    .addStaged(0, 0, 150, 3, 0, 150, 2, 0)
                    .itemOutputs("gtceu:hv_robot_arm")
                    .duration(300)
                    .EUt(512)
                    .save();
        } catch (RuntimeException e) {
            Dsl.fail("af9:staged_hv_arm", e);
        }
        try {
            Dsl.gt(out, "staged_assembly", "af9:staged_hv_emitter")
                    .itemInputs("4x #forge:rods/chromium", "2x gtceu:gold_single_cable", "2x #gtceu:circuits/hv", "minecraft:ender_eye")
                    .addStaged(0, 0, 100, 2, 0, 100, 2, 0)
                    .itemOutputs("gtceu:hv_emitter")
                    .duration(200)
                    .EUt(512)
                    .save();
        } catch (RuntimeException e) {
            Dsl.fail("af9:staged_hv_emitter", e);
        }
        try {
            Dsl.gt(out, "staged_assembly", "af9:staged_hv_sensor")
                    .itemInputs("#forge:rods/chromium", "4x #forge:plates/stainless_steel", "#gtceu:circuits/hv", "minecraft:ender_eye")
                    .addStaged(0, 0, 100, 2, 0, 100, 2, 0)
                    .itemOutputs("gtceu:hv_sensor")
                    .duration(200)
                    .EUt(512)
                    .save();
        } catch (RuntimeException e) {
            Dsl.fail("af9:staged_hv_sensor", e);
        }
        try {
            Dsl.gt(out, "staged_assembly", "af9:staged_hv_field")
                    .itemInputs("gtceu:quantum_eye", "2x #forge:plates/stainless_steel", "2x #gtceu:circuits/hv", "4x gtceu:mercury_barium_calcium_cuprate_quadruple_wire")
                    .addStaged(0, 0, 300, 2, 0, 300, 2, 0)
                    .itemOutputs("gtceu:hv_field_generator")
                    .duration(600)
                    .EUt(512)
                    .save();
        } catch (RuntimeException e) {
            Dsl.fail("af9:staged_hv_field", e);
        }
    }

    /** The removals the script made (see Removals): the recipes of other mods / the base pack that this theme replaces. */
    private static void removals() {

    }
}
