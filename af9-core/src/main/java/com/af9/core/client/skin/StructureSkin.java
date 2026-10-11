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
 */
public record StructureSkin(Supplier<? extends MachineDefinition> definition, int right, int up, int back,
                            ResourceLocation lower, ResourceLocation upper, ResourceLocation glowLower,
                            ResourceLocation glowUpper, int[] coilProbe) {}
