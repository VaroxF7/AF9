package com.af9.core.startup.machines;

import com.af9.core.machine.CWUServerMachine;
import com.af9.core.startup.AF9Machines;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.data.RotationState;

import net.minecraft.network.chat.Component;

/**
 * The CWU Server: a single block that turns EU into computation (LV-IV). Behaviour: {@link CWUServerMachine}. Recipes:
 * recipes/gen. Spec: docs/computation.md. CWU Server (LV-IV): 1 CWU/t at LV, one more each tier, 5 at IV, one amp of
 * its tier at full output. A GT computation source: GT Optical Fiber Cable leads it away. Its front lights: a steady
 * red dot offline (off, unpowered or nothing to give to), steady green idle, blinking while it gives (two patterns,
 * scattered by position); the models come from {@link CWUServerMachine#lightsModel}.
 */
public final class CwuServer {

    private CwuServer() {}

    public static void register() {
        AF9Machines.tiered("cwu_server", new int[] { GTValues.LV, GTValues.MV, GTValues.HV, GTValues.EV, GTValues.IV },
                (holder, tier) -> new CWUServerMachine(holder, tier), (tier, builder) -> {
                    CWUServerMachine.lightsModel(builder);
                    builder.langValue(GTValues.VLVH[tier] + " CWU Server " + GTValues.VLVT[tier])
                            .rotationState(RotationState.ALL)
                            .tooltips(Component.translatable("af9.cwu_server.tooltip.0",
                                    String.valueOf(CWUServerMachine.cwutFor(tier))),
                                    Component.translatable("af9.cwu_server.tooltip.1", String.valueOf(GTValues.VA[tier])),
                                    Component.translatable("af9.cwu_server.tooltip.2"));
                });
    }
}
