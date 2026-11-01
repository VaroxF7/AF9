package com.af9.core.startup.machines;

import com.af9.core.machine.PrecisionFabricatorMachine;
import com.af9.core.machine.part.PrecisionPortPartMachine;
import com.af9.core.startup.AF9Machines;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.data.RotationState;
import com.gregtechceu.gtceu.api.machine.multiblock.PartAbility;
import com.gregtechceu.gtceu.api.pattern.FactoryBlockPattern;
import com.gregtechceu.gtceu.api.pattern.Predicates;
import com.gregtechceu.gtceu.common.data.GTBlocks;
import com.gregtechceu.gtceu.common.data.GTRecipeModifiers;

import net.minecraft.network.chat.Component;

/**
 * AF9 - High Temperature Precision Fabricator (TerraFirmaGreg's boule machine, available at MV). The seed crystal on
 * its holder rod is dipped into liquid polysilicon in a quartz crucible and pulled: silicon, phosphorus, naquadah and
 * neutronium boules. A small steel hall: a layer of steel firebox round the controller and a layer of solid steel casing
 * over it, a steel gearbox turning the rod. Hatches: the Precision Fabricator Input Port and Output Port (an input hatch
 * and bus in one, and the output bus) and one energy hatch, in the solid casings of the bottom layer. No maintenance, no
 * muffler, no laser, no parallel hatch; batch mode and perfect overclocking only. Heat is no coil any more: the boule's
 * voltage tier (the hatch) sets what the machine can pull.
 * Recipes: server_scripts/mods/gtceu/precision_fabricator.js. Ports: af9-core PrecisionPortPartMachine.
 */
public final class PrecisionFabricator {

    private PrecisionFabricator() {}

    public static void register() {
        // Precision Fabricator Input Port: input hatch and input bus in one. 9 fluid tanks (3 x 3), 3 item inputs, 1
        // non-consumable slot, a ghost circuit (the circuit button on the screen). Tank: 80,000 mB each.
        // Output Port: the one item output slot. One tier only, MV: the energy hatch sets the voltage the boules run at.

        AF9Machines.tiered("precision_fabricator_input_port", new int[] { GTValues.MV },
                (holder, tier) -> new PrecisionPortPartMachine(holder, tier, IO.IN),
                (tier, builder) -> builder
                        .langValue("" + GTValues.VN[tier] + " Precision Fabricator Input Port")
                        .rotationState(RotationState.ALL)
                        .abilities(PrecisionPortPartMachine.INPUT)
                        .colorOverlayTieredHullModel("overlay_pipe_in_emissive", (String) null, "overlay_fluid_hatch")
                        .tooltips(Component.translatable("af9.precision_port.input.tooltip.0"),
                            Component.translatable("af9.precision_port.input.tooltip.1"),
                            Component.translatable("gtceu.universal.tooltip.fluid_storage_capacity_mult", 9,
                                "" + (PrecisionPortPartMachine.TANK_CAPACITY))));

        AF9Machines.tiered("precision_fabricator_output_port", new int[] { GTValues.MV },
                (holder, tier) -> new PrecisionPortPartMachine(holder, tier, IO.OUT),
                (tier, builder) -> builder
                        .langValue("" + GTValues.VN[tier] + " Precision Fabricator Output Port")
                        .rotationState(RotationState.ALL)
                        .abilities(PrecisionPortPartMachine.OUTPUT)
                        .colorOverlayTieredHullModel("overlay_pipe_out_emissive", (String) null, "overlay_fluid_hatch")
                        .tooltips(Component.translatable("af9.precision_port.output.tooltip.0")));

        // The hall (3 wide x 2 high x 2 deep), aisles back -> front (the controller's last), rows bottom -> top:
        //   bottom layer: back row solid steel casings (the hatches go here), front row firebox, controller, firebox
        //   top layer:    back row solid casing, steel gearbox, solid casing; front row solid casings
        // Hatches: a maximum only, never a required count.

        AF9Machines.multiblock("high_temperature_precision_fabricator", holder -> new PrecisionFabricatorMachine(holder), b -> b
                .langValue("High Temperature Precision Fabricator")
                .rotationState(RotationState.NON_Y_AXIS)
                .recipeTypes(AF9Machines.type("high_temperature_precision_fabricator"))
                .recipeModifiers(GTRecipeModifiers.OC_PERFECT, GTRecipeModifiers.BATCH_MODE)
                .appearanceBlock(GTBlocks.CASING_STEEL_SOLID)
                .tooltips(AF9Machines.lines("af9.high_temperature_precision_fabricator.tooltip.", 3))
                .pattern(definition -> FactoryBlockPattern.start()
                    .aisle("XXX", "CGC")
                    .aisle("FSF", "CCC")
                    .where('S', Predicates.controller(Predicates.blocks(definition.get())))
                    .where('F', Predicates.blocks(GTBlocks.FIREBOX_STEEL.get()))
                    .where('G', AF9Machines.blocks("gtceu:steel_gearbox"))
                    .where('C', Predicates.blocks(GTBlocks.CASING_STEEL_SOLID.get()))
                    .where('X', Predicates.blocks(GTBlocks.CASING_STEEL_SOLID.get())
                        .or(Predicates.abilities(PartAbility.INPUT_ENERGY).setMaxGlobalLimited(1, 1))
                        .or(Predicates.abilities(PrecisionPortPartMachine.INPUT).setMaxGlobalLimited(1, 1))
                        .or(Predicates.abilities(PrecisionPortPartMachine.OUTPUT).setMaxGlobalLimited(1, 1)))
                    .build())
                .workableCasingModel(AF9Machines.rl("gtceu:block/casings/solid/machine_casing_solid_steel"),
                    AF9Machines.rl("gtceu:block/multiblock/electric_blast_furnace")));
    }
}
