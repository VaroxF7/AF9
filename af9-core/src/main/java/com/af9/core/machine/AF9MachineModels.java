package com.af9.core.machine;

import com.af9.core.client.render.BioVatRender;
import com.af9.core.client.render.FiltrationBasinRender;
import com.af9.core.client.render.HearthTurntableRender;
import com.af9.core.client.render.LaserEngraverRender;
import com.af9.core.client.render.LightRingRender;
import com.af9.core.client.render.SawBladeRender;
import com.af9.core.client.render.SpaceElevatorRender;
import com.af9.core.client.render.WireDrawRender;

import com.gregtechceu.gtceu.api.pattern.util.RelativeDirection;
import com.gregtechceu.gtceu.api.registry.registrate.MachineBuilder;
import com.gregtechceu.gtceu.common.data.models.GTMachineModels;

import net.minecraft.resources.ResourceLocation;

import java.util.Locale;

/**
 * Machine models for KubeJS machine definitions ({@code .model(...)}): GT's workable casing model plus an AF9 dynamic
 * render. Models are only built on the client; the render classes are touched only then.
 */
@SuppressWarnings("removal") // new ResourceLocation(String) is the only constructor on 1.20.1
public final class AF9MachineModels {

    private AF9MachineModels() {}

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
     * GT's workable casing model plus the Sanguinite Hearth's two turntables ({@link HearthTurntableRender}): rotary
     * hearth floors in the air chamber that turn while a recipe runs. The machine must implement
     * {@link IHearthMachine} and have a block entity renderer ({@code .hasBER(true)}).
     *
     * @param back chamber centre behind the controller (blocks)
     */
    public static MachineBuilder.ModelInitializer workableCasingWithHearthTurntable(ResourceLocation casing,
                                                                                    ResourceLocation overlay,
                                                                                    float back) {
        return GTMachineModels.createWorkableCasingMachineModel(casing, overlay)
                .andThen(model -> model.addDynamicRenderer(() -> HearthTurntableRender.create(back)));
    }

    /**
     * GT's workable casing model plus the Industrial Cutting Machine's saw blade ({@link SawBladeRender}): a flat
     * blade standing in the chamber behind the front window, spinning while a recipe runs. The machine must implement
     * {@link ISawMachine} and have a block entity renderer ({@code .hasBER(true)}). Offsets are in blocks from the
     * controller block's origin: {@code back} behind it, {@code side} along the controller's left (the pattern's
     * column direction), {@code up} above its base.
     */
    public static MachineBuilder.ModelInitializer workableCasingWithSawBlade(ResourceLocation casing,
                                                                             ResourceLocation overlay, float back,
                                                                             float side, float up, float size) {
        return GTMachineModels.createWorkableCasingMachineModel(casing, overlay)
                .andThen(model -> model.addDynamicRenderer(() -> SawBladeRender.create(back, side, up, size)));
    }

    /**
     * GT's workable casing model plus the Water Filtration Plant's basin: animated water, floating
     * debris and the 3D skimmer arm. The machine must implement IFiltrationPlantMachine and have a
     * block entity renderer ({@code .hasBER(true)}).
     *
     * @param up     basin centre along the controller's up
     * @param back   basin centre behind the controller
     * @param left   basin centre to the controller's left
     * @param width  open water across the basin (blocks)
     * @param length open water along the basin, front to back (blocks)
     */
    public static MachineBuilder.ModelInitializer workableCasingWithFiltrationBasin(ResourceLocation casing,
            ResourceLocation overlay, float up, float back, float left, float width, float length) {
        return GTMachineModels.createWorkableCasingMachineModel(casing, overlay)
                .andThen(model -> model.addDynamicRenderer(() -> FiltrationBasinRender.create(up, back, left, width, length)));
    }

    /**
     * GT's workable casing model plus the Bio Vat's culture: a translucent body of the stored fluid standing as high
     * as the hatches are full, bubbling while a run goes on. The machine must implement IBioVatMachine and have a
     * block entity renderer ({@code .hasBER(true)}).
     *
     * @param up   the tank's floor above the controller's centre
     * @param back the tank's centre behind the controller
     */
    public static MachineBuilder.ModelInitializer workableCasingWithBioVat(ResourceLocation casing,
            ResourceLocation overlay, float up, float back) {
        return GTMachineModels.createWorkableCasingMachineModel(casing, overlay)
                .andThen(model -> model.addDynamicRenderer(() -> BioVatRender.create(up, back)));
    }

    /**
     * GT's workable casing model plus the Large Wiremill's wire drawing machine ({@link WireDrawRender}): a die, a
     * flyer and a take-up spool in the glass chamber, turning while a recipe runs. The machine must implement
     * {@link IWiremillMachine} and have a block entity renderer ({@code .hasBER(true)}).
     */
    public static MachineBuilder.ModelInitializer workableCasingWithWireDraw(ResourceLocation casing,
                                                                             ResourceLocation overlay) {
        return GTMachineModels.createWorkableCasingMachineModel(casing, overlay)
                .andThen(model -> model.addDynamicRenderer(WireDrawRender::create));
    }
}
