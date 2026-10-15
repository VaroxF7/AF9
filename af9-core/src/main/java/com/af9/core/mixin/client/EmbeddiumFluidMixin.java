package com.af9.core.mixin.client;

import com.af9.core.client.skin.HiddenBlocks;

import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.material.Fluid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * A liquid next to a hidden block is next to air as far as the eye goes: its side towards the hidden block is drawn
 * (otherwise the water has a missing wall where the model stands in it).
 */
@Pseudo
@Mixin(targets = "me.jellysquid.mods.sodium.client.render.chunk.compile.pipeline.FluidRenderer", remap = false)
public abstract class EmbeddiumFluidMixin {

    @Inject(method = "isFluidOccluded", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void af9$openToHidden(BlockAndTintGetter world, int x, int y, int z, Direction dir, Fluid fluid,
                                  CallbackInfoReturnable<Boolean> cir) {
        if (HiddenBlocks.any() && HiddenBlocks.isHidden(x + dir.getStepX(), y + dir.getStepY(), z + dir.getStepZ())) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "isSideExposed", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void af9$exposedToHidden(BlockAndTintGetter world, int x, int y, int z, Direction dir, float height,
                                     CallbackInfoReturnable<Boolean> cir) {
        if (HiddenBlocks.any() && HiddenBlocks.isHidden(x + dir.getStepX(), y + dir.getStepY(), z + dir.getStepZ())) {
            cir.setReturnValue(true);
        }
    }
}