package com.af9.core.machine;

import com.af9.core.client.render.ModeFluidRender;

import com.gregtechceu.gtceu.api.registry.registrate.MachineBuilder;
import com.gregtechceu.gtceu.common.data.models.GTMachineModels;

import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Machine models for KubeJS machine definitions ({@code .model(...)}). Models are only built on the client; the
 * render classes are touched only then.
 */
@SuppressWarnings("removal") // new ResourceLocation(String) is the only constructor on 1.20.1
public final class AF9MachineModels {

    private AF9MachineModels() {}

    /**
     * GT's workable casing model (like {@code .workableCasingModel(casing, overlay)}) plus a fake fluid inside the
     * running machine, per machine mode ({@link ModeFluidRender}). The machine must implement GT's
     * {@code IFluidRenderMulti} and have a block entity renderer ({@code .hasBER(true)}).
     *
     * @param modeFluids recipe type id -> fluid id, e.g. {@code {'gtceu:fab_blending': 'gtceu:distilled_water'}}
     */
    public static MachineBuilder.ModelInitializer workableCasingWithModeFluids(ResourceLocation casing,
                                                                                ResourceLocation overlay,
                                                                                Map<?, ?> modeFluids) {
        Map<ResourceLocation, ResourceLocation> fluids = new LinkedHashMap<>();
        modeFluids.forEach((type, fluid) -> fluids.put(new ResourceLocation(String.valueOf(type)),
                new ResourceLocation(String.valueOf(fluid))));
        return GTMachineModels.createWorkableCasingMachineModel(casing, overlay)
                .andThen(model -> model.addDynamicRenderer(() -> ModeFluidRender.create(fluids)));
    }
}
