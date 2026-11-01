package com.af9.core.startup.machines;

import com.af9.core.machine.AtmosphereRoomMachine;
import com.af9.core.startup.AF9Machines;
import com.af9.core.startup.layout.AtmosphereLayout;

import com.gregtechceu.gtceu.api.data.RotationState;
import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import com.gregtechceu.gtceu.api.machine.multiblock.PartAbility;
import com.gregtechceu.gtceu.api.pattern.BlockPattern;
import com.gregtechceu.gtceu.api.pattern.FactoryBlockPattern;
import com.gregtechceu.gtceu.api.pattern.MultiblockShapeInfo;
import com.gregtechceu.gtceu.api.pattern.Predicates;
import com.gregtechceu.gtceu.common.data.GTBlocks;
import com.gregtechceu.gtceu.common.data.GTRecipeModifiers;

import java.util.Arrays;
import java.util.List;

/**
 * AF9 - Atmospheric Simulation Room: a pressure room after GT's Cleanroom that holds a Lunar
 * atmosphere anywhere. The controller in the ceiling centre houses the Atmospheric Simulator;
 * XPS and NVM circuits build in its lunar air. Recipes: server_scripts/mods/gtceu/atmospheric_simulation.js
 *
 * 31 x 6 x 31 room (a step past the cleanroom's 15 max), aisles back -&gt; front, rows bottom -&gt; top.
 * The middle is open air (AF9 Core watches it for unsuited players); the controller sits
 * ceiling-centre, a 1 x 2 door stands front-bottom-centre. The pattern is generated below (like the
 * DTPF's cell function, in a loop: back wall closed, front wall with door, hollow middles).
 */
public final class AtmosphericSimulation {

    private AtmosphericSimulation() {}

    private static final String[][] SIM_AISLES = AtmosphereLayout.aisles(AtmosphereLayout.WIDTH, AtmosphereLayout.DEPTH);
    private static final String[][] SIM_PREVIEW_AISLES = AtmosphereLayout.aisles(9, 9);

    private static BlockPattern pattern(MultiblockMachineDefinition definition, String[][] aisles) {
        return AF9Machines.aisles(FactoryBlockPattern.start(), aisles)
                .where('S', Predicates.controller(Predicates.blocks(definition.get())))
                .where('X', Predicates.blocks(GTBlocks.CASING_STAINLESS_CLEAN.get())
                    .or(Predicates.abilities(PartAbility.INPUT_ENERGY).setMaxGlobalLimited(2, 1))
                    .or(Predicates.abilities(PartAbility.IMPORT_ITEMS).setMaxGlobalLimited(2, 1))
                    .or(Predicates.abilities(PartAbility.EXPORT_ITEMS).setMaxGlobalLimited(1, 1))
                    .or(Predicates.abilities(PartAbility.IMPORT_FLUIDS).setMaxGlobalLimited(2, 1))
                    .or(Predicates.abilities(PartAbility.EXPORT_FLUIDS).setMaxGlobalLimited(1, 1))
                    .or(Predicates.abilities(PartAbility.MAINTENANCE).setMaxGlobalLimited(1, 1)))
                .where('G', AF9Machines.blocks("gtceu:tempered_glass"))
                .where('#', Predicates.air())
                .build();
    }

    /** The preview page: the same room, 9 x 9, once. */
    private static List<MultiblockShapeInfo> preview(MultiblockMachineDefinition definition) {
        int[] once = new int[SIM_PREVIEW_AISLES.length];
        Arrays.fill(once, 1);
        return List.of(new MultiblockShapeInfo(pattern(definition, SIM_PREVIEW_AISLES).getPreview(once)));
    }

    public static void register() {
        AF9Machines.multiblock("atmospheric_simulation_room", holder -> new AtmosphereRoomMachine(holder), b -> b
                .langValue("Atmospheric Simulation Room")
                .rotationState(RotationState.NON_Y_AXIS)
                .recipeTypes(AF9Machines.type("atmospheric_simulation"))
                .recipeModifiers(GTRecipeModifiers.OC_PERFECT)
                .appearanceBlock(GTBlocks.CASING_STAINLESS_CLEAN)
                .tooltips(AF9Machines.lines("af9.atmospheric_simulation_room.tooltip.", 4))
                .pattern(definition -> pattern(definition, SIM_AISLES))
                .shapeInfos(definition -> preview(definition))
                .workableCasingModel(AF9Machines.rl("gtceu:block/casings/solid/machine_casing_clean_stainless_steel"),
                    AF9Machines.rl("gtceu:block/multiblock/assembly_line")));
    }
}
