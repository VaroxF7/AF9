package com.af9.core.startup.machines;

import com.af9.core.machine.part.AirConditioningHatchPartMachine;
import com.af9.core.startup.AF9Machines;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.data.RotationState;
import com.gregtechceu.gtceu.api.machine.property.GTMachineModelProperties;

import net.minecraft.network.chat.Component;

/**
 * Air Conditioning Hatch: the cooling of the Photolithography Line and Scanner. Behaviour:
 * {@link AirConditioningHatchPartMachine} (LithoMachine: cooling units, gate and break factor). Recipes:
 * recipes/gen (litho_process). Spec: docs/semiconductor-factory.md §18
 *
 * <p>The two machines are built as clean rooms, so they cool with air. A print puts 1 (350 nm) to 16 (65 nm) cooling
 * units (CU) of heat into the chamber; the hatches of the structure have to carry it away. A hatch is worth 1 CU at MV
 * and doubles per tier (HV 2, EV 4, IV 8), two fit. Every doubling above the load makes prints 10% faster and breaks
 * 20% fewer wafers. The warm air leaves from the hatch's front (the hook for the Temperature Update).
 */
public final class AirConditioning {

    private AirConditioning() {}

    public static void register() {
        AF9Machines.tiered("air_conditioning_hatch", new int[] { GTValues.MV, GTValues.HV, GTValues.EV, GTValues.IV },
                (holder, tier) -> new AirConditioningHatchPartMachine(holder, tier), (tier, builder) -> builder
                        .langValue(GTValues.VN[tier] + " Air Conditioning Hatch")
                        .rotationState(RotationState.ALL)
                        .abilities(AirConditioningHatchPartMachine.AIR_CONDITIONING)
                        // GT's overlayTieredHullModel(String) sets this, the ResourceLocation version we need does not:
                        // without it the hatch never knows it is formed and keeps its tier hull instead of taking the
                        // casing's texture
                        .modelProperty(GTMachineModelProperties.IS_FORMED, false)
                        .overlayTieredHullModel(AF9Machines.rl("af9:block/machine/part/air_conditioning"))
                        .tooltips(Component.translatable("af9.air_conditioning_hatch.tooltip.0"),
                                Component.translatable("af9.air_conditioning_hatch.tooltip.1"),
                                Component.translatable("af9.air_conditioning_hatch.tooltip.2"),
                                Component.translatable("af9.air_conditioning_hatch.tooltip.3")));
    }
}
