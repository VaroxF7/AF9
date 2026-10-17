package com.af9.core.client.skin;

import net.minecraft.util.Mth;

import com.mojang.blaze3d.vertex.VertexConsumer;
import org.joml.Matrix4f;

/**
 * The Orbital Array Mk2's thruster: a plasma lance leaving the sealed foot of the tower, a source glow with a turning ring
 * under the foot's plate and a violet beam widening and fading below it, a white-hot thread in it and shock rings running
 * down it. Nothing is drawn inside the tower. In the pattern's own space (right / up / back from the controller, the origin
 * the controller block's corner).
 */
final class CoreDraw {

    private static final float CX = 0.5F, CY = 0.5F;
    /** The lance's span along the back axis: from just under the foot's plate (level 104) to 20 beyond. */
    private static final float LANCE_Z0 = 105F, LANCE_Z1 = 125F;

    private CoreDraw() {}

    /**
     * @param phase the animation clock (ticks, speeding up with the machine), {@code power} 0..1 (idle to working)
     */
    static void draw(VertexConsumer c, Matrix4f m, float phase, float power) {
        lance(c, m, phase, power);
    }

    /**
     * The plasma lance under the tower: a source glow in the hollow of the foot chamber and a beam leaving it through the
     * opening, a violet sheath widening and fading as it goes, a white-hot thread in it, shock rings running down it.
     */
    private static void lance(VertexConsumer c, Matrix4f m, float phase, float power) {
        float level = 0.45F + 0.55F * power;
        float flicker = 0.85F + 0.15F * Mth.sin(phase * 0.7F) * Mth.sin(phase * 0.31F);
        float k = level * flicker;
        float r = 0.6F, g = 0.35F, b = 1F;

        // the source: a glow and a turning ring under the foot's plate
        sphere(c, m, CX, CY, LANCE_Z0, 1.1F, 6, 10, r, g, b, 0.3F * k);
        sphere(c, m, CX, CY, LANCE_Z0, 0.55F, 5, 8, 0.9F, 0.95F, 1F, 0.55F * k);
        float a = phase * 0.08F;
        ring(c, m, CX, CY, LANCE_Z0 + 0.3F, Mth.cos(a), Mth.sin(a), 0, -Mth.sin(a) * 0.5F, Mth.cos(a) * 0.5F, 1.3F,
                0.14F, 28, r, g, b, 0.5F * k);

        // the beam: widening and fading out 20 blocks below the foot
        fade(c, m, LANCE_Z0, LANCE_Z1, 1.1F, 2.6F, 12, r, g, b, 0.38F * k, 0F);
        fade(c, m, LANCE_Z0, LANCE_Z1, 0.6F, 1.4F, 10, r, g, b, 0.32F * k, 0F);
        fade(c, m, LANCE_Z0, LANCE_Z1 - 4F, 0.22F, 0.4F, 8, 1F, 1F, 1F, 0.85F * k, 0F);

        // shock rings running down it, widening and fading
        for (int i = 0; i < 9; i++) {
            float t = (phase * 0.012F + i / 9F) % 1F;
            float z = LANCE_Z0 + (LANCE_Z1 - LANCE_Z0) * t;
            disc(c, m, CX, CY, z, 1.1F + 1.5F * t, 0.16F, 12, r, g, b, 0.6F * Mth.sin(t * Mth.PI) * k);
        }
    }

    private static void fade(VertexConsumer c, Matrix4f m, float z0, float z1, float rad0, float rad1, int sides,
                             float r, float g, float b, float a0, float a1) {
        for (int i = 0; i < sides; i++) {
            float p0 = Mth.TWO_PI * i / sides, p1 = Mth.TWO_PI * (i + 1) / sides;
            c.vertex(m, CX + Mth.cos(p0) * rad0, CY + Mth.sin(p0) * rad0, z0).color(r, g, b, a0).endVertex();
            c.vertex(m, CX + Mth.cos(p1) * rad0, CY + Mth.sin(p1) * rad0, z0).color(r, g, b, a0).endVertex();
            c.vertex(m, CX + Mth.cos(p1) * rad1, CY + Mth.sin(p1) * rad1, z1).color(r, g, b, a1).endVertex();
            c.vertex(m, CX + Mth.cos(p0) * rad1, CY + Mth.sin(p0) * rad1, z1).color(r, g, b, a1).endVertex();
        }
    }

    private static void vertex(VertexConsumer c, Matrix4f m, float x, float y, float z, float r, float g, float b,
                               float a) {
        c.vertex(m, x, y, z).color(r, g, b, a).endVertex();
    }

    private static void quad(VertexConsumer c, Matrix4f m, float[] p, float r, float g, float b, float a) {
        for (int i = 0; i < 4; i++) vertex(c, m, p[i * 3], p[i * 3 + 1], p[i * 3 + 2], r, g, b, a);
    }


    /** A flat annulus across the axis. */
    private static void disc(VertexConsumer c, Matrix4f m, float cx, float cy, float z, float radius, float width,
                             int segs, float r, float g, float b, float a) {
        ring(c, m, cx, cy, z, 1, 0, 0, 0, 1, 0, radius, width, segs, r, g, b, a);
    }

    /** An annulus in the plane spanned by the unit vectors u and v, of the given outer radius and width. */
    private static void ring(VertexConsumer c, Matrix4f m, float cx, float cy, float cz, float ux, float uy, float uz,
                             float vx, float vy, float vz, float radius, float width, int segs, float r, float g,
                             float b, float a) {
        float inner = Math.max(0.01F, radius - width);
        for (int i = 0; i < segs; i++) {
            float a0 = Mth.TWO_PI * i / segs, a1 = Mth.TWO_PI * (i + 1) / segs;
            float c0 = Mth.cos(a0), s0 = Mth.sin(a0), c1 = Mth.cos(a1), s1 = Mth.sin(a1);
            quad(c, m, new float[] {
                    cx + (ux * c0 + vx * s0) * inner, cy + (uy * c0 + vy * s0) * inner, cz + (uz * c0 + vz * s0) * inner,
                    cx + (ux * c0 + vx * s0) * radius, cy + (uy * c0 + vy * s0) * radius, cz + (uz * c0 + vz * s0) * radius,
                    cx + (ux * c1 + vx * s1) * radius, cy + (uy * c1 + vy * s1) * radius, cz + (uz * c1 + vz * s1) * radius,
                    cx + (ux * c1 + vx * s1) * inner, cy + (uy * c1 + vy * s1) * inner, cz + (uz * c1 + vz * s1) * inner },
                    r, g, b, a);
        }
    }


    /** A low-poly sphere (latitude bands by longitude segments). */
    private static void sphere(VertexConsumer c, Matrix4f m, float cx, float cy, float cz, float radius, int bands,
                               int segs, float r, float g, float b, float a) {
        for (int i = 0; i < bands; i++) {
            float p0 = Mth.PI * i / bands - Mth.HALF_PI, p1 = Mth.PI * (i + 1) / bands - Mth.HALF_PI;
            float z0 = Mth.sin(p0) * radius, z1 = Mth.sin(p1) * radius, q0 = Mth.cos(p0) * radius,
                    q1 = Mth.cos(p1) * radius;
            for (int j = 0; j < segs; j++) {
                float a0 = Mth.TWO_PI * j / segs, a1 = Mth.TWO_PI * (j + 1) / segs;
                quad(c, m, new float[] {
                        cx + Mth.cos(a0) * q0, cy + Mth.sin(a0) * q0, cz + z0,
                        cx + Mth.cos(a1) * q0, cy + Mth.sin(a1) * q0, cz + z0,
                        cx + Mth.cos(a1) * q1, cy + Mth.sin(a1) * q1, cz + z1,
                        cx + Mth.cos(a0) * q1, cy + Mth.sin(a0) * q1, cz + z1 }, r, g, b, a);
            }
        }
    }
}
