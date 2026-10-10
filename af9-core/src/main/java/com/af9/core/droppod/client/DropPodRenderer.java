package com.af9.core.droppod.client;

import com.af9.core.AF9Core;
import com.af9.core.droppod.DropPodEntity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/** Draws the {@link DropPodModel}, turned the way its rider looks, with its door and restraint from the entity. */
public class DropPodRenderer extends EntityRenderer<DropPodEntity> {

    private static final ResourceLocation TEXTURE = new ResourceLocation(AF9Core.MOD_ID,
            "textures/entity/drop_pod.png");

    private final DropPodModel model;

    public DropPodRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new DropPodModel(context.bakeLayer(DropPodModel.LAYER));
        this.shadowRadius = 0.9F;
    }

    @Override
    public void render(DropPodEntity pod, float yaw, float partialTick, PoseStack stack, MultiBufferSource buffers,
                       int light) {
        stack.pushPose();
        stack.mulPose(Axis.YP.rotationDegrees(180.0F - yaw));
        // the model is in vanilla's convention: upside down, 24 px above the feet
        stack.scale(-1.0F, -1.0F, 1.0F);
        stack.translate(0.0F, -1.501F, 0.0F);
        model.setup(pod.openness(partialTick));
        VertexConsumer consumer = buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        model.render(stack, consumer, light, OverlayTexture.NO_OVERLAY);
        stack.popPose();
        super.render(pod, yaw, partialTick, stack, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(DropPodEntity pod) {
        return TEXTURE;
    }
}
