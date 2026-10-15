package com.af9.core.mixin.client;

import com.af9.core.client.skin.HiddenBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Embeddium culls a block's face that looks at a full block. A block the skins hide is not drawn, so its neighbours must
 * draw their faces towards it, or a thin see-through seam shows wherever the model does not fill the block (the floor
 * under a hidden wall, the sky or the water behind it).
 */
@Pseudo
@Mixin(targets = "me.jellysquid.mods.sodium.client.render.chunk.compile.pipeline.BlockOcclusionCache", remap = false)
public abstract class EmbeddiumOcclusionMixin {

    @Inject(method = "shouldDrawSide", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void af9$hiddenNeighbour(BlockState self, BlockGetter view, BlockPos pos, Direction facing,
                                     CallbackInfoReturnable<Boolean> cir) {
        if (HiddenBlocks.any() && HiddenBlocks.isHidden(pos.relative(facing).asLong())) {
            cir.setReturnValue(true);
        }
    }
}