package com.af9.core.client.skin;

import com.af9.core.client.render.AF9RenderTypes;

import com.gregtechceu.gtceu.api.machine.MetaMachine;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/**
 * The white paneled cabinet of the lithography machines ({@link StructureSkin.Cabinet}): every plain block of the
 * structure that {@link StructureSkins} hides gets raised white panels on the faces that look out, a dark band round the
 * machine at the height of the controller's row, tall narrow panel columns on the sides, and "NOVIUM" on both long
 * sides. The cleanroom glass and the machine blocks (controller, hatches) are not hidden, so they stay as they are.
 * Drawn in the pattern's own space (right / up / back from the controller), like the model skins.
 */
final class CabinetDraw {

    private static final int PANEL = 0xEEF0F3, BODY = 0x4E525B, BAND = 0x30333A, LOGO = 0xFF2B3A8F;
    private static final float GAP = 0.02F, RAISE = 0.035F, BAND_HALF = 0.04F;
    private static final String TEXT = "NOVIUM";

    private CabinetDraw() {}

    static void draw(MetaMachine self, StructureSkin skin, int length, PoseStack poseStack, MultiBufferSource buffer,
                     int overlay, int light) {
        int minU = -skin.cabinet().below(), maxU = skin.up() - 1, right = skin.right();
        PoseStack.Pose pose = poseStack.last();
        VertexConsumer solid = buffer.getBuffer(RenderType.entityCutoutNoCull(AF9RenderTypes.WHITE));
        for (int r = -right; r <= right; r++) {
            for (int u = minU; u <= maxU; u++) {
                for (int b = 0; b < length; b++) {
                    if (!StructureSkins.isHidden(StructureSkins.at(self, r, u, b).asLong())) continue;
                    // faces that look out of the structure's box: -x, +x, front, back, top
                    if (r == -right) face(solid, pose, overlay, light, r, u, b, 0, u);
                    if (r == right) face(solid, pose, overlay, light, r, u, b, 1, u);
                    if (b == 0) face(solid, pose, overlay, light, r, u, b, 2, u);
                    if (b == length - 1) face(solid, pose, overlay, light, r, u, b, 3, u);
                    if (u == maxU) face(solid, pose, overlay, light, r, u, b, 4, u);
                }
            }
        }
        // the name, after the solids (a buffer from the source ends when another is asked for)
        Font font = Minecraft.getInstance().font;
        Component text = Component.literal(TEXT).withStyle(ChatFormatting.BOLD);
        boolean mirrored = pose.pose().determinant() < 0;
        float y = maxU + 0.5F, z = length / 2F, scale = 0.04F;
        for (int side = 0; side < 2; side++) {
            float x = side == 0 ? -right - 0.05F : right + 1.05F;
            float normal = side == 0 ? -1F : 1F;
            // reads from the front to the back on the left side and the other way on the right, flipped when the
            // model space is a mirror image of the world
            float reading = (side == 0 ? 1F : -1F) * (mirrored ? -1F : 1F);
            poseStack.pushPose();
            poseStack.translate(x, y, z);
            Matrix4f turn = new Matrix4f(0, 0, reading, 0, 0, 1, 0, 0, normal, 0, 0, 0, 0, 0, 0, 1);
            poseStack.last().pose().mul(turn);
            poseStack.last().normal().mul(new Matrix3f(turn));
            poseStack.scale(scale, -scale, scale);
            font.drawInBatch(text, -font.width(text) / 2F, -4F, LOGO, false, poseStack.last().pose(), buffer,
                    Font.DisplayMode.NORMAL, 0, light);
            poseStack.popPose();
        }
    }

    /**
     * One outward face of a cell: the dark body, then the raised white panels (two columns on the sides, a 2 x 2 grid on
     * top), with a dark band round the machine between the controller's row and the row above it.
     */
    private static void face(VertexConsumer c, PoseStack.Pose pose, int overlay, int light, int r, int u, int b,
                             int dir, int row) {
        // origin corner, the two edges, the normal
        float ox, oy, oz, hx, hy, hz, vx, vy, vz, nx, ny, nz;
        switch (dir) {
            case 0 -> { ox = r; oy = u; oz = b; hx = 0; hy = 0; hz = 1; vx = 0; vy = 1; vz = 0; nx = -1; ny = 0; nz = 0; }
            case 1 -> { ox = r + 1; oy = u; oz = b; hx = 0; hy = 0; hz = 1; vx = 0; vy = 1; vz = 0; nx = 1; ny = 0; nz = 0; }
            case 2 -> { ox = r; oy = u; oz = b; hx = 1; hy = 0; hz = 0; vx = 0; vy = 1; vz = 0; nx = 0; ny = 0; nz = -1; }
            case 3 -> { ox = r; oy = u; oz = b + 1; hx = 1; hy = 0; hz = 0; vx = 0; vy = 1; vz = 0; nx = 0; ny = 0; nz = 1; }
            default -> { ox = r; oy = u + 1; oz = b; hx = 1; hy = 0; hz = 0; vx = 0; vy = 0; vz = 1; nx = 0; ny = 1; nz = 0; }
        }
        Face f = new Face(c, pose, overlay, light, ox, oy, oz, hx, hy, hz, vx, vy, vz, nx, ny, nz);
        f.rect(0, 1, 0, 1, 0F, BODY);
        if (dir == 4) {
            for (int i = 0; i < 2; i++) {
                for (int j = 0; j < 2; j++) {
                    f.rect(i * 0.5F + GAP, i * 0.5F + 0.5F - GAP, j * 0.5F + GAP, j * 0.5F + 0.5F - GAP, RAISE, PANEL);
                }
            }
            return;
        }
        // the band: the top of the controller's row (row 0) and the bottom of the row above it
        float lo = row == 1 ? BAND_HALF : GAP, hi = row == 0 ? 1F - BAND_HALF : 1F - GAP;
        if (row == 0 || row == 1) f.rect(0, 1, row == 0 ? 1F - BAND_HALF : 0F, row == 0 ? 1F : BAND_HALF, 0.002F, BAND);
        // tall narrow columns: two to the block
        for (int i = 0; i < 2; i++) {
            f.rect(i * 0.5F + GAP, i * 0.5F + 0.5F - GAP, lo, hi, RAISE, PANEL);
        }
    }

    /** A face's own frame: rectangles in its (h, v) coordinates, lifted off it along the normal. */
    private record Face(VertexConsumer c, PoseStack.Pose pose, int overlay, int light, float ox, float oy, float oz,
                        float hx, float hy, float hz, float vx, float vy, float vz, float nx, float ny, float nz) {

        void rect(float a0, float a1, float b0, float b1, float lift, int rgb) {
            float r = (rgb >> 16 & 255) / 255F, g = (rgb >> 8 & 255) / 255F, bl = (rgb & 255) / 255F;
            corner(a0, b0, lift, r, g, bl);
            corner(a1, b0, lift, r, g, bl);
            corner(a1, b1, lift, r, g, bl);
            corner(a0, b1, lift, r, g, bl);
        }

        private void corner(float a, float b, float lift, float r, float g, float bl) {
            c.vertex(pose.pose(), ox + a * hx + b * vx + lift * nx, oy + a * hy + b * vy + lift * ny,
                    oz + a * hz + b * vz + lift * nz).color(r, g, bl, 1F).uv(0.5F, 0.5F)
                    .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(pose.normal(), nx, ny, nz).endVertex();
        }
    }
}
