package com.af9.core.client.skin;

import com.gregtechceu.gtceu.api.machine.MachineDefinition;

import net.minecraft.resources.ResourceLocation;

import java.util.function.Supplier;

/**
 * A formed multiblock drawn as one model instead of its blocks: the model replaces the structure's plain blocks (casings,
 * coils; the machine blocks stay: the controller and the hatches, the player works with them), the way Immersive
 * Engineering draws its multiblocks. The machine works exactly as before, only the picture changes.
 * <p>
 * Positions are relative to the controller as GT's pattern names them: {@code right} to its right, {@code up} above it,
 * {@code back} behind its front face. The model is authored in the same space (see {@code tools/ebf_model.py}).
 *
 * @param definition  the machine this skin is for
 * @param right       the pattern's extent to the right of the controller, -right..right
 * @param up          how many blocks high the pattern is, from the controller's block up (exclusive end)
 * @param back        how many blocks deep the pattern is, from the controller's block back (exclusive end)
 * @param lower       the model of the lowest two blocks of the pattern
 * @param upper       the model of the next two blocks
 * @param glowLower   the parts of {@code lower} that light up while the machine works
 * @param glowUpper   as {@code glowLower}, for {@code upper}
 * @param coilProbe   {right, up, back} of a block of the pattern whose model gives the coil texture, or null
 * @param window      {x, y, z, half width, half height} in model blocks of the furnace window the flames come out of
 *                    while the machine works, or null
 */
public record StructureSkin(Supplier<? extends MachineDefinition> definition, int right, int up, int back,
                            ResourceLocation lower, ResourceLocation upper, ResourceLocation glowLower,
                            ResourceLocation glowUpper, int[] coilProbe, float[] window, Cabinet cabinet, Rings rings) {

    /**
     * A white paneled cabinet drawn in code instead of a model (the lithography machines): any length, found from the
     * blocks in front of the player ({@link StructureSkins#measure}).
     *
     * @param below     rows of the pattern under the controller's row
     * @param minLength the shortest the structure is (blocks, back from the controller's face)
     * @param maxLength the longest the structure is
     */
    public record Cabinet(int below, int minLength, int maxLength) {}

    /**
     * The rings of the Orbital Array Mk2's tower: their blocks are hidden while it is formed and the rings are drawn
     * turning instead, held in place by plasma beams from the tower ({@link RingDraw}).
     *
     * @param bands     the rings
     * @param towerHalf half width of the tower's body (the beams start at its face)
     */
    public record Rings(java.util.List<com.af9.core.machine.OrbitalLithographyMachine.Band> bands, int towerHalf) {}

    /** A model skin (the Electric Blast Furnace). */
    public StructureSkin(Supplier<? extends MachineDefinition> definition, int right, int up, int back,
                         ResourceLocation lower, ResourceLocation upper, ResourceLocation glowLower,
                         ResourceLocation glowUpper, int[] coilProbe, float[] window) {
        this(definition, right, up, back, lower, upper, glowLower, glowUpper, coilProbe, window, null, null);
    }

    /** A cabinet skin: {@code up} rows from the bottom row's offset up (exclusive end, as for a model skin). */
    public static StructureSkin cabinet(Supplier<? extends MachineDefinition> definition, int right, int up,
                                        Cabinet cabinet) {
        return new StructureSkin(definition, right, up, cabinet.maxLength(), null, null, null, null, null, null,
                cabinet, null);
    }

    /** A ring skin: drawn in code, the blocks are the rings' (see {@link Rings}). */
    public static StructureSkin rings(Supplier<? extends MachineDefinition> definition, Rings rings) {
        return new StructureSkin(definition, 0, 0, 0, null, null, null, null, null, null, null, rings);
    }
}
