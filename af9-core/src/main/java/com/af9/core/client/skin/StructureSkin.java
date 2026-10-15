package com.af9.core.client.skin;

import com.gregtechceu.gtceu.api.machine.MachineDefinition;

import net.minecraft.resources.ResourceLocation;

import java.util.List;
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
 * @param parts       the models the skin is made of: a vanilla model may only hold coordinates from -16 to 32 on an
 *                    axis, so the skin is a set of windows of that size, each drawn at its offset (the generator cuts
 *                    the shapes at the windows' borders and prints this list). Their parts that use the glow sprite
 *                    light up while the machine works
 * @param coilProbe   {right, up, back} of a block of the pattern whose model gives the coil texture, or null
 * @param window      {x, y, z, half width, half height} in model blocks of the furnace window the flames come out of
 *                    while the machine works, or null
 */
public record StructureSkin(Supplier<? extends MachineDefinition> definition, int right, int up, int back,
                            List<Part> parts, int[] coilProbe, float[] window) {

    /** One model of the skin and where it is drawn: its offset from the controller block's corner, in blocks. */
    public record Part(ResourceLocation model, float x, float y, float z) {}
}
