package com.af9.core.client.skin;

import com.af9.core.client.render.PondWalls;

import net.minecraft.core.BlockPos;
import net.minecraftforge.client.event.RenderHighlightEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * The positions the client does not draw because a model takes their place (the structure skins and the pond's round
 * wall). A hidden block is as good as air to the eye, so the render treats it that way: its neighbours draw their faces
 * towards it (no see-through seam where the model does not fill the block), liquids do not stop at it, and it casts
 * neither occlusion nor ambient shade (no dark squares round it). Used by the mixins of {@code com.af9.core.mixin.client}.
 * Only the client's render asks: the server and the physics never see a hidden block.
 */
public final class HiddenBlocks {

    private HiddenBlocks() {}

    /** Whether anything is hidden at all: the fast exit of every hook. */
    public static boolean any() {
        return StructureSkins.anyHidden() || PondWalls.anyHidden();
    }

    public static boolean isHidden(long packedPos) {
        return StructureSkins.isHidden(packedPos) || PondWalls.isHidden(packedPos);
    }

    public static boolean isHidden(BlockPos pos) {
        return any() && isHidden(pos.asLong());
    }

    public static boolean isHidden(int x, int y, int z) {
        return any() && isHidden(BlockPos.asLong(x, y, z));
    }

    /** No selection box on a block that is not drawn: it would float in the model. */
    @SubscribeEvent
    public static void onHighlight(RenderHighlightEvent.Block event) {
        if (isHidden(event.getTarget().getBlockPos())) event.setCanceled(true);
    }
}