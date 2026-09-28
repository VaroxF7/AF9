package com.af9.core.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.joml.Matrix4f;

/**
 * GUI drawing (a machine console's) onto a surface in the world, the Central Monitor's screen: {@link GuiGraphics}
 * whose fills, text and items go into the world's buffers with depth, as block entity renderers draw. The GUI's own
 * way (flushing each call with the depth test off) would show the console through walls. Every call lies a step
 * nearer the viewer than the one before, so the layers keep the GUI's order whatever the world sorts; the whole
 * console is as thick as its calls times the step.
 */
@OnlyIn(Dist.CLIENT)
public class WorldGuiGraphics extends GuiGraphics {

    private final MultiBufferSource buffer;
    /** Depth each call adds, canvas units. */
    private final float step;
    private float depth;

    /**
     * @param surface the canvas's placement in the world: GUI units, x right and y down on the surface
     * @param step    depth per call, canvas units
     */
    public WorldGuiGraphics(Matrix4f surface, MultiBufferSource buffer, float step) {
        super(Minecraft.getInstance(), Minecraft.getInstance().renderBuffers().bufferSource());
        pose().last().pose().set(surface);
        this.buffer = buffer;
        this.step = step;
    }

    private float next() {
        depth += step;
        return depth;
    }

    @Override
    public void fill(RenderType renderType, int x1, int y1, int x2, int y2, int z, int color) {
        float minX = Math.min(x1, x2), maxX = Math.max(x1, x2);
        float minY = Math.min(y1, y2), maxY = Math.max(y1, y2);
        if (minX == maxX || minY == maxY) return;
        float d = next();
        Matrix4f m = pose().last().pose();
        VertexConsumer vc = buffer.getBuffer(RenderType.textBackground());
        int a = color >>> 24, r = (color >> 16) & 255, g = (color >> 8) & 255, b = color & 255;
        int light = LightTexture.FULL_BRIGHT;
        // both windings: it shows whatever the face culling
        vc.vertex(m, minX, minY, d).color(r, g, b, a).uv2(light).endVertex();
        vc.vertex(m, minX, maxY, d).color(r, g, b, a).uv2(light).endVertex();
        vc.vertex(m, maxX, maxY, d).color(r, g, b, a).uv2(light).endVertex();
        vc.vertex(m, maxX, minY, d).color(r, g, b, a).uv2(light).endVertex();
        vc.vertex(m, minX, minY, d).color(r, g, b, a).uv2(light).endVertex();
        vc.vertex(m, maxX, minY, d).color(r, g, b, a).uv2(light).endVertex();
        vc.vertex(m, maxX, maxY, d).color(r, g, b, a).uv2(light).endVertex();
        vc.vertex(m, minX, maxY, d).color(r, g, b, a).uv2(light).endVertex();
    }

    @Override
    public int drawString(Font font, String text, float x, float y, int color, boolean dropShadow) {
        if (text == null) return 0;
        Matrix4f m = new Matrix4f(pose().last().pose()).translate(0, 0, next());
        return font.drawInBatch(text, x, y, color, dropShadow, m, buffer, Font.DisplayMode.NORMAL, 0,
                LightTexture.FULL_BRIGHT);
    }

    @Override
    public int drawString(Font font, FormattedCharSequence text, float x, float y, int color, boolean dropShadow) {
        Matrix4f m = new Matrix4f(pose().last().pose()).translate(0, 0, next());
        return font.drawInBatch(text, x, y, color, dropShadow, m, buffer, Font.DisplayMode.NORMAL, 0,
                LightTexture.FULL_BRIGHT);
    }

    @Override
    public void renderItem(ItemStack stack, int x, int y) {
        renderItem(stack, x, y, 0, 0);
    }

    @Override
    public void renderItem(ItemStack stack, int x, int y, int seed) {
        renderItem(stack, x, y, seed, 0);
    }

    /** The item as in a slot (16 x 16), flat on the surface. */
    @Override
    public void renderItem(ItemStack stack, int x, int y, int seed, int guiOffset) {
        if (stack.isEmpty()) return;
        Minecraft minecraft = Minecraft.getInstance();
        BakedModel model = minecraft.getItemRenderer().getModel(stack, minecraft.level, null, seed);
        PoseStack pose = pose();
        pose.pushPose();
        pose.translate(x + 8, y + 8, next());
        pose.scale(16, -16, 0.02F);
        minecraft.getItemRenderer().render(stack, ItemDisplayContext.GUI, false, pose, buffer,
                LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, model);
        pose.popPose();
        depth += step;
    }

    @Override
    public void renderItemDecorations(Font font, ItemStack stack, int x, int y) {
        renderItemDecorations(font, stack, x, y, null);
    }

    /** The count only (no durability bar or cooldown on a console's items). */
    @Override
    public void renderItemDecorations(Font font, ItemStack stack, int x, int y, String text) {
        if (stack.isEmpty() || stack.getCount() == 1 && text == null) return;
        String count = text == null ? String.valueOf(stack.getCount()) : text;
        drawString(font, count, x + 17 - font.width(count), y + 9, 0xFFFFFF, true);
    }

    /** Nothing to flush: everything went into the world's buffers. */
    @Override
    public void flush() {}
}
