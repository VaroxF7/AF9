package com.af9.core.mixin.client;

import com.af9.core.client.skin.HiddenBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * A block the skins hide neither occludes (the chunk's visibility graph and the lighting of the blocks round it treat it
 * as open) nor shades (no dark corner on the water and the floor beside it). The render asks with the client's level or
 * Embeddium's world slice; the integrated server's own level is left alone.
 */
@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class HiddenBlockStateMixin {

    @Inject(method = "isSolidRender", at = @At("HEAD"), cancellable = true, require = 0)
    private void af9$hiddenIsOpen(BlockGetter level, BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (HiddenBlocks.any() && !(level instanceof ServerLevel) && HiddenBlocks.isHidden(pos.asLong())) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "getShadeBrightness", at = @At("HEAD"), cancellable = true, require = 0)
    private void af9$hiddenCastsNoShade(BlockGetter level, BlockPos pos, CallbackInfoReturnable<Float> cir) {
        if (HiddenBlocks.any() && !(level instanceof ServerLevel) && HiddenBlocks.isHidden(pos.asLong())) {
            cir.setReturnValue(1.0F);
        }
    }
}