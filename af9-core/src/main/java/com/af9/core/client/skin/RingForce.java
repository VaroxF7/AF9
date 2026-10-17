package com.af9.core.client.skin;

import com.af9.core.AF9Core;

import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import com.mojang.blaze3d.vertex.PoseStack;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;

/**
 * Keeps the Mk2's rings on screen whatever the world renderer decides about the controller's own block: a block entity
 * renderer is only called while the chunk section of its block is visible (in the view and not hidden by the cave
 * culling), so the 130-block tower vanished when the crown was out of sight. The renderer marks the controllers it drew
 * this frame; after the block entities, those tracked controllers it did not reach are drawn from here, inside the view
 * frustum and the same view distance.
 */
@Mod.EventBusSubscriber(modid = AF9Core.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class RingForce {

    private static final StructureSkinRender RENDER = new StructureSkinRender();
    /** Controller (packed) -> the frame the block entity renderer last drew it in. */
    private static final Long2LongOpenHashMap DRAWN = new Long2LongOpenHashMap();
    private static long frame;

    private RingForce() {}

    static void markDrawn(BlockPos controller) {
        DRAWN.put(controller.asLong(), frame);
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_SKY) {
            frame++;
            return;
        }
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES) return;
        var level = Minecraft.getInstance().level;
        if (level == null) return;
        Vec3 camera = event.getCamera().getPosition();
        MultiBufferSource.BufferSource buffer = Minecraft.getInstance().renderBuffers().bufferSource();
        boolean drew = false;
        for (MultiblockControllerMachine controller : StructureSkins.trackedControllers()) {
            BlockPos pos = controller.getPos();
            if (DRAWN.getOrDefault(pos.asLong(), -1L) == frame || !controller.isFormed()) continue;
            StructureSkin skin = StructureSkins.skinOf(controller.getDefinition());
            if (skin == null || skin.rings() == null || !StructureSkins.isTracked(pos)) continue;
            if (!RENDER.shouldRender(controller, camera)) continue;
            if (event.getFrustum() != null && !event.getFrustum().isVisible(RENDER.getRenderBoundingBox(controller))) {
                continue;
            }
            PoseStack poseStack = event.getPoseStack();
            poseStack.pushPose();
            poseStack.translate(pos.getX() - camera.x, pos.getY() - camera.y, pos.getZ() - camera.z);
            RENDER.render(controller, event.getPartialTick(), poseStack, buffer, LevelRenderer.getLightColor(level, pos),
                    net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY);
            poseStack.popPose();
            drew = true;
        }
        if (drew) buffer.endBatch();
    }
}
