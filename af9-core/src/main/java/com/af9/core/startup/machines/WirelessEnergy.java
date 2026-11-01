package com.af9.core.startup.machines;

import com.af9.core.machine.NetworkNodeMachine;
import com.af9.core.startup.AF9Machines;
import com.af9.core.wireless.WirelessComputationHatch;
import com.af9.core.wireless.WirelessReceiverHatch;
import com.af9.core.wireless.WirelessTransmitterHatch;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.data.RotationState;
import com.gregtechceu.gtceu.api.machine.multiblock.PartAbility;
import com.gregtechceu.gtceu.api.pattern.FactoryBlockPattern;
import com.gregtechceu.gtceu.api.pattern.Predicates;
import com.gregtechceu.gtceu.common.data.GTRecipeTypes;

import net.minecraft.network.chat.Component;

/**
 * AF9 - Wireless energy hatches (behaviour: af9-core com.af9.core.wireless). Recipes:
 * server_scripts/mods/gtceu/wireless_energy.js
 *
 * Wireless Energy Transmitter: an output (dynamo) hatch for the Power Substation (or any multiblock that puts out
 * energy). Wireless Energy Receiver: an energy input hatch for any multiblock. A data stick links them: right-click the
 * transmitter to take its link, right-click a receiver to link it (shift-right-click a receiver copies its link). Any
 * distance, any dimension: the energy goes through a channel saved with the world.
 *
 * The hatches have no voltage of their own, only a maximum amperage: the transmitter sends at the highest voltage of
 * its multiblock's energy inputs (a substation fed with UV hatches sends UV; without inputs, set it on its screen), the
 * receiver works at its transmitter's voltage. The hull tier of a variant is its look and crafting tier only:
 *   LuV 16A   ZPM 100A   UV 256A   UHV 1000A
 */
public final class WirelessEnergy {

    private WirelessEnergy() {}

    /** Hull tier (LuV, ZPM, UV, UHV) -> [maximum amps, GT model of the receiver, GT model of the transmitter]. */
    private static final Object[][] VARIANTS = {
        { 16, "energy_input_hatch_16a", "energy_output_hatch_16a" },
        { 100, "energy_input_hatch_64a", "energy_output_hatch_64a" },
        { 256, "laser_target_hatch", "laser_source_hatch" },
        { 1000, "laser_target_hatch", "laser_source_hatch" }
    };

    public static void register() {

        AF9Machines.tiered("wireless_energy_receiver", new int[] { GTValues.LuV, GTValues.ZPM, GTValues.UV, GTValues.UHV },
                (holder, tier) -> new WirelessReceiverHatch(holder, tier, (int) VARIANTS[tier - GTValues.LuV][0]),
                (tier, builder) -> builder
                        .langValue("Wireless Energy Receiver (" + (int) VARIANTS[tier - GTValues.LuV][0] + "A)")
                        .rotationState(RotationState.ALL)
                        .abilities(PartAbility.INPUT_ENERGY)
                        // GT has a String and a ResourceLocation version: name the String one (a GT part model)
                        .overlayTieredHullModel((String) VARIANTS[tier - GTValues.LuV][1])
                        .tooltips(AF9Machines.linesWith("af9.wireless_receiver.tooltip.", 3, (int) VARIANTS[tier - GTValues.LuV][0])));

        AF9Machines.tiered("wireless_energy_transmitter", new int[] { GTValues.LuV, GTValues.ZPM, GTValues.UV, GTValues.UHV },
                (holder, tier) -> new WirelessTransmitterHatch(holder, tier, (int) VARIANTS[tier - GTValues.LuV][0]),
                (tier, builder) -> builder
                        .langValue("Wireless Energy Transmitter (" + (int) VARIANTS[tier - GTValues.LuV][0] + "A)")
                        .rotationState(RotationState.ALL)
                        .abilities(PartAbility.OUTPUT_ENERGY)
                        .overlayTieredHullModel((String) VARIANTS[tier - GTValues.LuV][2])
                        .tooltips(AF9Machines.linesWith("af9.wireless_transmitter.tooltip.", 3, (int) VARIANTS[tier - GTValues.LuV][0])));

        AF9Machines.tiered("wireless_computation_receiver", new int[] { GTValues.UV },
                (holder, tier) -> new WirelessComputationHatch(holder, false),
                (tier, builder) -> builder
                        .langValue("Wireless Computation Reception Hatch")
                        .rotationState(RotationState.ALL)
                        .abilities(PartAbility.COMPUTATION_DATA_RECEPTION)
                        .overlayTieredHullModel("computation_data_hatch")
                        .tooltips(Component.translatable("af9.wireless_cwu_receiver.tooltip.0"),
                            Component.translatable("af9.wireless_cwu_receiver.tooltip.1")));

        AF9Machines.tiered("wireless_computation_transmitter", new int[] { GTValues.UV },
                (holder, tier) -> new WirelessComputationHatch(holder, true),
                (tier, builder) -> builder
                        .langValue("Wireless Computation Transmission Hatch")
                        .rotationState(RotationState.ALL)
                        .abilities(PartAbility.COMPUTATION_DATA_TRANSMISSION)
                        .overlayTieredHullModel("computation_data_hatch")
                        .tooltips(Component.translatable("af9.wireless_cwu_transmitter.tooltip.0"),
                            Component.translatable("af9.wireless_cwu_transmitter.tooltip.1")));

        AF9Machines.multiblock("network_node", holder -> new NetworkNodeMachine(holder), b -> b
                .langValue("Network Node")
                .rotationState(RotationState.NON_Y_AXIS)
                .recipeTypes(GTRecipeTypes.DUMMY_RECIPES)
                .appearanceBlock(AF9Machines.blockSupplier("gtceu:computer_casing"))
                .tooltips(AF9Machines.lines("af9.network_node.tooltip.", 4))
                .pattern(definition -> FactoryBlockPattern.start()
                    .aisle("XDDDX", "XDDDX", "XDDDX")
                    .aisle("XDDDX", "XAAAX", "XDDDX")
                    .aisle("XCCCX", "XCSCX", "XCCCX")
                    .where('S', Predicates.controller(Predicates.blocks(definition.get())))
                    .where('X', AF9Machines.blocks("gtceu:computer_heat_vent"))
                    .where('D', AF9Machines.blocks("gtceu:computer_casing")
                        .or(Predicates.abilities(PartAbility.DATA_ACCESS).setMaxGlobalLimited(9, 3))
                        .or(Predicates.abilities(PartAbility.COMPUTATION_DATA_RECEPTION).setMaxGlobalLimited(2, 1))
                        .or(Predicates.abilities(PartAbility.COMPUTATION_DATA_TRANSMISSION).setMaxGlobalLimited(2, 1)))
                    .where('A', AF9Machines.blocks("gtceu:computer_casing"))
                    .where('C', AF9Machines.blocks("gtceu:high_power_casing")
                        .or(Predicates.abilities(PartAbility.INPUT_ENERGY).setMaxGlobalLimited(2, 1))
                        .or(Predicates.abilities(PartAbility.MAINTENANCE).setMaxGlobalLimited(1, 1)))
                    .build())
                .workableCasingModel(AF9Machines.rl("gtceu:block/casings/hpca/high_power_casing"), AF9Machines.rl("gtceu:block/multiblock/network_switch")));
    }
}
