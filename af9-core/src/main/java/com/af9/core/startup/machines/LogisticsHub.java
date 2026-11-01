package com.af9.core.startup.machines;

import com.af9.core.fiber.FiberLinkPortPartMachine;
import com.af9.core.fiber.LogisticsHubMachine;
import com.af9.core.startup.AF9Machines;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.data.RotationState;
import com.gregtechceu.gtceu.api.machine.multiblock.PartAbility;
import com.gregtechceu.gtceu.api.pattern.FactoryBlockPattern;
import com.gregtechceu.gtceu.api.pattern.Predicates;
import com.gregtechceu.gtceu.api.pattern.util.RelativeDirection;
import com.gregtechceu.gtceu.common.data.GTRecipeTypes;

import net.minecraft.network.chat.Component;

/**
 * AF9 - Fiber Link Port and Logistics Hub: the brain and logistics centre of the Photolithography Scanner Mk2.
 * The hub is a small 4 x 2 x 2 multiblock that stands anywhere; a Fiber Link Port in the scanner and one in the hub are
 * linked with a data stick (right-click one port, then the other; shift-right-click cuts the link). Everything put into
 * the hub's input buses and hatches reaches the scanner, everything the scanner makes comes out of the hub's output
 * buses and hatches: items, fluids and gases. Behaviour: AF9 Core (com.af9.core.fiber).
 * The Logistics Rack (2 wide x 3 high x 1 deep) adds onto the scanner's ports: the scanner takes a second Fiber Link Port,
 * the rack has its own, and it brings four more buses and hatches. Same behaviour as the hub.
 * Data (computation) is not carried yet: the scanner keeps its own computation hatch.
 */
public final class LogisticsHub {

    private LogisticsHub() {}

    public static void register() {
        AF9Machines.tiered("fiber_link_port", new int[] { GTValues.EV },
                (holder, tier) -> new FiberLinkPortPartMachine(holder, tier),
                (tier, builder) -> builder
                        .langValue("Fiber Link Port")
                        .rotationState(RotationState.ALL)
                        .abilities(FiberLinkPortPartMachine.FIBER_LINK)
                        .overlayTieredHullModel(AF9Machines.rl("af9:block/machine/part/fiber_link_port"))
                        .tooltips(Component.translatable("af9.fiber_link_port.tooltip.0"),
                            Component.translatable("af9.fiber_link_port.tooltip.1"),
                            Component.translatable("af9.fiber_link_port.tooltip.2")));

        AF9Machines.multiblock("logistics_hub", holder -> new LogisticsHubMachine(holder), b -> b
                .langValue("Logistics Hub")
                .rotationState(RotationState.NON_Y_AXIS)
                .recipeType(GTRecipeTypes.DUMMY_RECIPES)
                .appearanceBlock(AF9Machines.blockSupplier("gtceu:plascrete"))
                .tooltips(Component.translatable("af9.logistics_hub.tooltip.0"),
                    Component.translatable("af9.logistics_hub.tooltip.1"),
                    Component.translatable("af9.logistics_hub.tooltip.2"),
                    Component.translatable("af9.logistics_hub.tooltip.3"))
                // 4 wide x 2 high x 2 deep, plascrete like the scanner. Aisles front -> back, rows bottom -> top. The
                // controller is in the front, bottom row; the Fiber Link Port is the one block at the back, top row, left.
                .pattern(definition -> FactoryBlockPattern.start(RelativeDirection.LEFT, RelativeDirection.UP,
                    RelativeDirection.BACK)
                    .aisle("CMCC", "CCCC")
                    .aisle("CCCC", "LCCC")
                    .where('M', Predicates.controller(Predicates.blocks(definition.get())))
                    .where('L', Predicates.abilities(FiberLinkPortPartMachine.FIBER_LINK))
                    // parts have a maximum only, never a required count; any plascrete of the shell may be one
                    .where('C', AF9Machines.blocks("gtceu:plascrete")
                        .or(Predicates.abilities(PartAbility.IMPORT_ITEMS).setMaxGlobalLimited(4, 1))
                        .or(Predicates.abilities(PartAbility.EXPORT_ITEMS).setMaxGlobalLimited(4, 1))
                        .or(Predicates.abilities(PartAbility.IMPORT_FLUIDS).setMaxGlobalLimited(4, 1))
                        .or(Predicates.abilities(PartAbility.EXPORT_FLUIDS).setMaxGlobalLimited(4, 1))
                        .or(Predicates.abilities(PartAbility.MAINTENANCE).setMaxGlobalLimited(1, 1)))
                    .build())
                .workableCasingModel(AF9Machines.rl("gtceu:block/casings/cleanroom/plascrete"),
                    AF9Machines.rl("gtceu:block/multiblock/gcym/large_engraving_laser")));

        AF9Machines.multiblock("logistics_rack", holder -> new LogisticsHubMachine(holder), b -> b
                .langValue("Logistics Rack")
                .rotationState(RotationState.NON_Y_AXIS)
                .recipeType(GTRecipeTypes.DUMMY_RECIPES)
                .appearanceBlock(AF9Machines.blockSupplier("gtceu:plascrete"))
                .tooltips(Component.translatable("af9.logistics_rack.tooltip.0"),
                    Component.translatable("af9.logistics_rack.tooltip.1"),
                    Component.translatable("af9.logistics_rack.tooltip.2"))
                // 2 wide x 3 high x 1 deep, standing upright. The controller is the bottom row, left; the port the top row, left.
                .pattern(definition -> FactoryBlockPattern.start(RelativeDirection.LEFT, RelativeDirection.UP,
                    RelativeDirection.BACK)
                    .aisle("MC", "CC", "LC")
                    .where('M', Predicates.controller(Predicates.blocks(definition.get())))
                    .where('L', Predicates.abilities(FiberLinkPortPartMachine.FIBER_LINK))
                    .where('C', AF9Machines.blocks("gtceu:plascrete")
                        .or(Predicates.abilities(PartAbility.IMPORT_ITEMS).setMaxGlobalLimited(2, 0))
                        .or(Predicates.abilities(PartAbility.EXPORT_ITEMS).setMaxGlobalLimited(2, 0))
                        .or(Predicates.abilities(PartAbility.IMPORT_FLUIDS).setMaxGlobalLimited(2, 0))
                        .or(Predicates.abilities(PartAbility.EXPORT_FLUIDS).setMaxGlobalLimited(2, 0)))
                    .build())
                .workableCasingModel(AF9Machines.rl("gtceu:block/casings/cleanroom/plascrete"),
                    AF9Machines.rl("gtceu:block/multiblock/gcym/large_engraving_laser")));
    }
}
