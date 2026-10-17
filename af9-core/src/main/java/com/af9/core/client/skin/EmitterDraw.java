package com.af9.core.client.skin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import org.joml.Matrix4f;
import org.joml.Matrix3f;

/**
 * The laser emitter at the top of the energy column: a dark stack of square plates narrowing downward to a stem that
 * ends in a nozzle, drawn solid (and unlit by the world, so it does not go black in the dark chamber) in the tube's
 * hollow. In the pattern's own space like {@link CoreDraw}, the column's glow starts at the nozzle ({@link #NOZZLE_Z}).
 */
final class EmitterDraw {

    /** The levels below the controller: the top plate's roof, and the nozzle where the plasma column starts. */
    static final float TOP_Z = 13.2F, NOZZLE_Z = 23.4F;
    private static final int LIGHT = 0xF000F0;
    private static final float CX = 0.5F, CY = 0.5F;
    /** Plates from the top down: z, half width, thickness, grey level. */
    private static final float[][] PLATES = {
            {13.2F, 1.40F, 0.9F, 0.16F},
            {14.4F, 1.15F, 0.5F, 0.30F},
            {15.2F, 1.40F, 0.7F, 0.14F},
            {16.2F, 1.00F, 0.5F, 0.28F},
            {17.0F, 1.20F, 0.6F, 0.13F},
            {17.9F, 0.80F, 0.5F, 0.26F},
            {18.7F, 0.95F, 0.5F, 0.12F},
            {19.5F, 0.62F, 0.5F, 0.24F},
    };

    private EmitterDraw() {}

    static void draw(VertexConsumer c, PoseStack.Pose pose) {
        for (float[] p : PLATES) box(c, pose, p[1], p[0], p[0] + p[2], p[3]);
        // the stem: a thin dark shaft, then the nozzle's collar
        box(c, pose, 0.32F, 20.0F, 22.4F, 0.2F);
        box(c, pose, 0.55F, 22.4F, NOZZLE_Z - 0.5F, 0.1F);
        box(c, pose, 0.42F, NOZZLE_Z - 0.5F, NOZZLE_Z, 0.32F);
    }

    /** A square box about the axis between two levels, grey level g with a slight blue tint, shaded per face. */
    private static void box(VertexConsumer c, PoseStack.Pose pose, float h, float z0, float z1, float g) {
        float x0 = CX - h, x1 = CX + h, y0 = CY - h, y1 = CY + h;
        // top (towards smaller z) and bottom
        face(c, pose, g * 1.25F, 0, 0, -1, x0, y0, z0, x1, y0, z0, x1, y1, z0, x0, y1, z0);
        face(c, pose, g * 0.7F, 0, 0, 1, x0, y1, z1, x1, y1, z1, x1, y0, z1, x0, y0, z1);
        // four sides
        face(c, pose, g, 0, -1, 0, x0, y0, z1, x1, y0, z1, x1, y0, z0, x0, y0, z0);
        face(c, pose, g, 0, 1, 0, x0, y1, z0, x1, y1, z0, x1, y1, z1, x0, y1, z1);
        face(c, pose, g * 0.9F, -1, 0, 0, x0, y1, z1, x0, y0, z1, x0, y0, z0, x0, y1, z0);
        face(c, pose, g * 0.9F, 1, 0, 0, x1, y0, z1, x1, y1, z1, x1, y1, z0, x1, y0, z0);
    }

    private static void face(VertexConsumer c, PoseStack.Pose pose, float g, float nx, float ny, float nz,
                             float... p) {
        Matrix4f m = pose.pose();
        Matrix3f n = pose.normal();
        float r = Math.min(1F, g), gg = Math.min(1F, g), b = Math.min(1F, g * 1.15F);
        for (int i = 0; i < 4; i++) {
            c.vertex(m, p[i * 3], p[i * 3 + 1], p[i * 3 + 2]).color(r, gg, b, 1F).uv(0.5F, 0.5F)
                    .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(LIGHT).normal(n, nx, ny, nz).endVertex();
        }
    }
}
