package com.af9.core.machine;

import com.af9.core.client.render.LaserEngraverRender;
import com.af9.core.client.render.LightRingRender;
import com.af9.core.client.render.LithoChamberRender;
import com.af9.core.client.render.ModeFluidRender;
import com.af9.core.client.render.SpaceElevatorRender;

import com.gregtechceu.gtceu.api.pattern.util.RelativeDirection;
import com.gregtechceu.gtceu.api.registry.registrate.MachineBuilder;
import com.gregtechceu.gtceu.common.data.models.GTMachineModels;

import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Machine models for KubeJS machine definitions ({@code .model(...)}): GT's workable casing model plus an AF9 dynamic
 * render. Models are only built on the client; the render classes are touched only then.
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

    /**
     * GT's workable casing model plus the Hyper-Intensity Laser Engraver's beam while it works
     * ({@link LaserEngraverRender}). The machine must implement {@link ILaserEngraverMachine} and have a block entity
     * renderer ({@code .hasBER(true)}).
     *
     * @param back   how far behind the controller the shaft is
     * @param bottom height of the beam's foot above the controller block's floor
     * @param top    height of the beam's start
     */
    public static MachineBuilder.ModelInitializer workableCasingWithLaserBeam(ResourceLocation casing,
                                                                               ResourceLocation overlay, float back,
                                                                               float bottom, float top) {
        return GTMachineModels.createWorkableCasingMachineModel(casing, overlay)
                .andThen(model -> model.addDynamicRenderer(() -> LaserEngraverRender.create(back, bottom, top)));
    }

    /**
     * GT's workable casing model plus the Space Elevator's cable and the climber on it ({@link SpaceElevatorRender}).
     * The machine must implement {@code ISpaceElevatorMachine} and have a block entity renderer
     * ({@code .hasBER(true)}).
     *
     * @param up   the cable block above the controller (blocks)
     * @param back the cable block behind the controller (blocks)
     */
    public static MachineBuilder.ModelInitializer workableCasingWithSpaceElevator(ResourceLocation casing,
                                                                                   ResourceLocation overlay, float up,
                                                                                   float back) {
        return GTMachineModels.createWorkableCasingMachineModel(casing, overlay)
                .andThen(model -> model.addDynamicRenderer(() -> SpaceElevatorRender.create(up, back)));
    }

    /**
     * GT's workable casing model plus a glowing light ring while the machine works, like GT's Fusion Reactor
     * ({@link LightRingRender}). The machine must implement {@link ILightRingMachine} and have a block entity renderer
     * ({@code .hasBER(true)}).
     *
     * @param up        ring centre along the controller's up (negative: below it)
     * @param back      ring centre behind the controller
     * @param radius    ring radius in blocks
     * @param thickness tube radius in blocks
     */
    public static MachineBuilder.ModelInitializer workableCasingWithLightRing(ResourceLocation casing,
                                                                               ResourceLocation overlay, float up,
                                                                               float back, float radius,
                                                                               float thickness) {
        return workableCasingWithLightRing(casing, overlay, up, back, radius, thickness, "up");
    }

    /**
     * As {@link #workableCasingWithLightRing(ResourceLocation, ResourceLocation, float, float, float, float)}, the ring
     * lying across the given axis of the controller ("up", "front", "left", ...): "front" for a controller that faces
     * the ring's axis, like the orbital station's facing up out of its deck.
     */
    public static MachineBuilder.ModelInitializer workableCasingWithLightRing(ResourceLocation casing,
                                                                               ResourceLocation overlay, float up,
                                                                               float back, float radius,
                                                                               float thickness, String normal) {
        return workableCasingWithLightRing(casing, overlay, up, back, radius, thickness, normal, false);
    }

    /**
     * As above, with lightning: while the ring glows, bolts leap from it into its middle, branching into arms, and
     * short darts crackle off it (the Particle Accelerator's ring).
     */
    public static MachineBuilder.ModelInitializer workableCasingWithLightRing(ResourceLocation casing,
                                                                               ResourceLocation overlay, float up,
                                                                               float back, float radius,
                                                                               float thickness, String normal,
                                                                               boolean arcs) {
        return workableCasingWithLightRing(casing, overlay, up, back, radius, thickness, normal, arcs, 0);
    }

    /**
     * As above, the ring running inside its machine: {@code wall} blocks out from the housing's inner face (the Particle
     * Accelerator's, through its magnets). The blocks hide it, it glows out through the glass; the lightning leaps off
     * the inner face into the middle and the sparks spit off it.
     */
    public static MachineBuilder.ModelInitializer workableCasingWithLightRing(ResourceLocation casing,
                                                                               ResourceLocation overlay, float up,
                                                                               float back, float radius,
                                                                               float thickness, String normal,
                                                                               boolean arcs, float wall) {
        RelativeDirection axis = RelativeDirection.valueOf(normal.toUpperCase(Locale.ROOT));
        return GTMachineModels.createWorkableCasingMachineModel(casing, overlay)
                .andThen(model -> model.addDynamicRenderer(
                        () -> LightRingRender.create(up, back, radius, thickness, axis, arcs, wall)));
    }

    /**
     * GT's workable casing model plus the lithography exposure chamber ({@link LithoChamberRender}): violet UV
     * fill, the wafer exposing die by die, the scanning laser and the wafer robot on its slide. The machine must
     * implement {@link ILithoChamberMachine} and have a block entity renderer ({@code .hasBER(true)}).
     *
     * @param up   chamber centre along the controller's up
     * @param back chamber centre behind the controller
     */
    public static MachineBuilder.ModelInitializer workableCasingWithChamber(ResourceLocation casing,
                                                                             ResourceLocation overlay, float up,
                                                                             float back) {
        return GTMachineModels.createWorkableCasingMachineModel(casing, overlay)
                .andThen(model -> model.addDynamicRenderer(() -> LithoChamberRender.create(up, back)));
    }
}
