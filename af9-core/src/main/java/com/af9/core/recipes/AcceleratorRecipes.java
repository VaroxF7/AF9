package com.af9.core.recipes;

import net.minecraft.data.recipes.FinishedRecipe;

import java.util.function.Consumer;

/**
 * Particle Accelerator recipes (was kubejs/server_scripts/mods/gtceu/particle_accelerator.js; machine in
 * ParticleAcceleratorMachine, spec docs/semiconductor-factory.md).
 *
 * <p>Fluids come only through Coolant Hatches, so every recipe's only fluid is its supercooled coolant: a recipe takes its
 * coolant or any colder one, the fluid tag af9:coolant/&lt;grade&gt; (data/af9/tags/fluids/coolant). A recipe names the magnetic field it
 * needs to start (min_field) and what a run drains (decay_rate). The cell recipes work on Universal Cells matched by the fluid
 * they hold (NBT predicate); the collisions differ in coolant grade or trap so GT's lookup keeps them apart.
 */
final class AcceleratorRecipes {

    private AcceleratorRecipes() {}

    static void register(Consumer<FinishedRecipe> out) {
        try {
            Dsl.gt(out, "assembler", "af9:particle_accelerator")
                    .itemInputs("12x gtceu:uv_fusion_reactor", "gtceu:zpm_machine_hull", "4x gtceu:zpm_field_generator", "4x gtceu:zpm_emitter", "4x #gtceu:circuits/zpm", "2x gtceu:zpm_sensor", "8x gtceu:niobium_titanium_plate")
                    .inputFluids(Dsl.fluid("gtceu:supercooled_hydrogen", 4000))
                    .itemOutputs("gtceu:particle_accelerator")
                    .duration(3000)
                    .EUt(524288)
                    .save();
        } catch (RuntimeException e) {
            Dsl.fail("af9:particle_accelerator", e);
        }
        try {
            Dsl.gt(out, "assembler", "af9:collision_containment_casing")
                    .itemInputs("2x gtceu:stable_machine_casing", "4x gtceu:tungsten_steel_plate", "2x gtceu:niobium_titanium_plate")
                    .itemOutputs("2x af9:collision_containment_casing")
                    .duration(200)
                    .EUt(32768)
                    .save();
        } catch (RuntimeException e) {
            Dsl.fail("af9:collision_containment_casing", e);
        }
        try {
            Dsl.gt(out, "assembler", "af9:collision_shield_casing")
                    .itemInputs("2x gtceu:stable_machine_casing", "4x gtceu:naquadah_alloy_plate", "2x gtceu:niobium_titanium_plate")
                    .itemOutputs("2x af9:collision_shield_casing")
                    .duration(200)
                    .EUt(32768)
                    .save();
        } catch (RuntimeException e) {
            Dsl.fail("af9:collision_shield_casing", e);
        }
        try {
            Dsl.gt(out, "chemical_reactor", "af9:xenoxid")
                    .inputFluids(Dsl.fluid("gtceu:xenon", 1000), Dsl.fluid("gtceu:oxygen", 4000))
                    .outputFluids(Dsl.fluid("gtceu:xenoxid", 1000))
                    .duration(200)
                    .EUt(8192)
                    .save();
        } catch (RuntimeException e) {
            Dsl.fail("af9:xenoxid", e);
        }
        try {
            Dsl.gt(out, "assembler", "af9:beryllium_spallation_target")
                    .itemInputs("4x gtceu:beryllium_plate", "2x gtceu:tungsten_steel_plate")
                    .itemOutputs("af9:beryllium_spallation_target")
                    .duration(200)
                    .EUt(32768)
                    .save();
        } catch (RuntimeException e) {
            Dsl.fail("af9:beryllium_spallation_target", e);
        }
        try {
            Dsl.gt(out, "assembler", "af9:magnetic_trap")
                    .itemInputs("gtceu:zpm_field_generator", "4x gtceu:niobium_titanium_plate", "2x gtceu:tungsten_steel_plate")
                    .itemOutputs("af9:magnetic_trap")
                    .duration(400)
                    .EUt(131072)
                    .save();
        } catch (RuntimeException e) {
            Dsl.fail("af9:magnetic_trap", e);
        }
        try {
            Dsl.gt(out, "ion_collision", "af9:qgp_trap")
                    .itemInputs("af9:magnetic_trap", "16x gtceu:cobalt_ingot")
                    .inputFluids(Dsl.fluidTag("af9:coolant/argon", 2000))
                    .itemOutputs("af9:qgp_trap")
                    .duration(600)
                    .EUt(524288, 4)
                    .addData("min_field", 3000)
                    .addData("decay_rate", 800)
                    .save();
        } catch (RuntimeException e) {
            Dsl.fail("af9:qgp_trap", e);
        }
        try {
            Dsl.gt(out, "canner", "af9:accelerator_cell/tungsten")
                    .itemInputs("gtceu:universal_fluid_cell")
                    .inputFluids(Dsl.fluid("gtceu:tungsten", 1000))
                    .itemOutputs(Dsl.cell("gtceu:tungsten"))
                    .duration(32)
                    .EUt(32)
                    .save();
        } catch (RuntimeException e) {
            Dsl.fail("af9:accelerator_cell/tungsten", e);
        }
        try {
            Dsl.gt(out, "canner", "af9:accelerator_cell/rhenium_127")
                    .itemInputs("gtceu:universal_fluid_cell")
                    .inputFluids(Dsl.fluid("gtceu:rhenium_127", 1000))
                    .itemOutputs(Dsl.cell("gtceu:rhenium_127"))
                    .duration(32)
                    .EUt(32)
                    .save();
        } catch (RuntimeException e) {
            Dsl.fail("af9:accelerator_cell/rhenium_127", e);
        }
        try {
            Dsl.gt(out, "canner", "af9:accelerator_cell/osmium_plasma")
                    .itemInputs("gtceu:universal_fluid_cell")
                    .inputFluids(Dsl.fluid("gtceu:osmium_plasma", 1000))
                    .itemOutputs(Dsl.cell("gtceu:osmium_plasma"))
                    .duration(32)
                    .EUt(32)
                    .save();
        } catch (RuntimeException e) {
            Dsl.fail("af9:accelerator_cell/osmium_plasma", e);
        }
        try {
            Dsl.gt(out, "ion_collision", "af9:rhenium_127")
                    .inputItemNbtPredicate("gtceu:universal_fluid_cell", "gtceu:tungsten")
                    .inputFluids(Dsl.fluidTag("af9:coolant/argon", 1000))
                    .itemOutputs(Dsl.cell("gtceu:rhenium_127"))
                    .duration(400)
                    .EUt(524288, 4)
                    .addData("min_field", 3000)
                    .addData("decay_rate", 800)
                    .save();
        } catch (RuntimeException e) {
            Dsl.fail("af9:rhenium_127", e);
        }
        try {
            Dsl.gt(out, "ion_collision", "af9:osmium_plasma")
                    .inputItemNbtPredicate("gtceu:universal_fluid_cell", "gtceu:rhenium_127")
                    .inputFluids(Dsl.fluidTag("af9:coolant/xenon", 1000))
                    .itemOutputs(Dsl.cell("gtceu:osmium_plasma"))
                    .duration(400)
                    .EUt(524288, 4)
                    .addData("min_field", 3000)
                    .addData("decay_rate", 800)
                    .save();
        } catch (RuntimeException e) {
            Dsl.fail("af9:osmium_plasma", e);
        }
        try {
            Dsl.gt(out, "canner", "af9:accelerator_cell/iridium")
                    .itemInputs("gtceu:universal_fluid_cell")
                    .inputFluids(Dsl.fluid("gtceu:iridium", 1000))
                    .itemOutputs(Dsl.cell("gtceu:iridium"))
                    .duration(32)
                    .EUt(32)
                    .save();
        } catch (RuntimeException e) {
            Dsl.fail("af9:accelerator_cell/iridium", e);
        }
        try {
            Dsl.gt(out, "canner", "af9:accelerator_cell/intridium")
                    .itemInputs("gtceu:universal_fluid_cell")
                    .inputFluids(Dsl.fluid("gtceu:intridium", 1000))
                    .itemOutputs(Dsl.cell("gtceu:intridium"))
                    .duration(32)
                    .EUt(32)
                    .save();
        } catch (RuntimeException e) {
            Dsl.fail("af9:accelerator_cell/intridium", e);
        }
        try {
            Dsl.gt(out, "ion_collision", "af9:intridium")
                    .inputItemNbtPredicate("gtceu:universal_fluid_cell", "gtceu:iridium")
                    .notConsumable("af9:magnetic_trap")
                    .inputFluids(Dsl.fluidTag("af9:coolant/argon", 1000))
                    .itemOutputs(Dsl.cell("gtceu:intridium"))
                    .duration(400)
                    .EUt(524288, 4)
                    .addData("min_field", 3000)
                    .addData("decay_rate", 800)
                    .save();
        } catch (RuntimeException e) {
            Dsl.fail("af9:intridium", e);
        }
        try {
            Dsl.gt(out, "neutron_irradiation", "af9:transmuted_neutronium_wafer")
                    .itemInputs("4x gtceu:neutronium_wafer")
                    .inputItemNbtPredicate("gtceu:universal_fluid_cell", "gtceu:intridium")
                    .inputFluids(Dsl.fluidTag("af9:coolant/hydrogen", 1000))
                    .itemOutputs("4x af9:transmuted_neutronium_wafer", "gtceu:universal_fluid_cell")
                    .duration(1200)
                    .EUt(524288, 2)
                    .addData("min_field", 2000)
                    .addData("decay_rate", 500)
                    .save();
        } catch (RuntimeException e) {
            Dsl.fail("af9:transmuted_neutronium_wafer", e);
        }
        try {
            Dsl.gt(out, "quark_synthesis", "af9:strange_matter_dust")
                    .itemInputs("4x af9:qgp_trap", "gtceu:neutronium_dust")
                    .inputFluids(Dsl.fluidTag("af9:coolant/xenon", 4000))
                    .itemOutputs("gtceu:strange_matter_dust", "4x af9:magnetic_trap")
                    .duration(1200)
                    .EUt(524288, 4)
                    .addData("min_field", 4000)
                    .addData("decay_rate", 1500)
                    .save();
        } catch (RuntimeException e) {
            Dsl.fail("af9:strange_matter_dust", e);
        }
        try {
            Dsl.gt(out, "quark_synthesis", "af9:chromodynium_dust")
                    .itemInputs("8x af9:qgp_trap", "gtceu:strange_matter_dust")
                    .inputFluids(Dsl.fluidTag("af9:coolant/endion", 4000))
                    .itemOutputs("gtceu:chromodynium_dust", "8x af9:magnetic_trap")
                    .duration(2400)
                    .EUt(2097152, 4)
                    .addData("min_field", 12000)
                    .addData("decay_rate", 3000)
                    .save();
        } catch (RuntimeException e) {
            Dsl.fail("af9:chromodynium_dust", e);
        }
    }
}
