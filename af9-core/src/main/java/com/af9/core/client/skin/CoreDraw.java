package com.af9.core.client.skin;

import net.minecraft.util.Mth;

import com.mojang.blaze3d.vertex.VertexConsumer;
import org.joml.Matrix4f;

/**
 * The energy core in the Orbital Array Mk2's hollow tower, drawn as added light in the 3 x 3 hollow of the superconducting coil tube that runs up the
 * middle: a plasma column with pulses running up it, a pulsing core in the upper chamber with crossed halo rings and fans
 * of light turning round it, orbs circling the column on their paths with a trail behind them. Everything stays within
 * the tube's hollow (1.5 blocks out of the axis), is dim while the station idles and bright and fast while it
 * works. In the pattern's own space (right / up / back from the controller, the origin the controller block's corner).
 */
final class CoreDraw {

    /** The column's span along the back axis (levels below the controller): the chamber's first floor to the foot's opening. */
    private static final float Z0 = 24F, Z1 = 97F;
    /** The core's place, in the middle of the upper chamber, and the orbits' span. */
    private static final float CORE_Z = 30F, ORBIT_Z0 = 26F, ORBIT_Z1 = 56F;
    private static final float CX = 0.5F, CY = 0.5F;
    /** The lance's span along the back axis: from the bottom ring's level down through the foot (levels 104) and 20 beyond. */
    private static final float LANCE_Z0 = 59F, LANCE_Z1 = 124F, FOOT_Z = 104F;

    private CoreDraw() {}

    /**
     * @param phase the animation clock (ticks, speeding up with the machine), {@code power} 0..1 (idle to working)
     */
    static void draw(VertexConsumer c, Matrix4f m, int rgb, float phase, float power, float ignite, int lod) {
        float r = (rgb >> 16 & 255) / 255F, g = (rgb >> 8 & 255) / 255F, b = (rgb & 255) / 255F;
        float level = 0.45F + 0.55F * power;
        float pulse = 0.5F + 0.5F * Mth.sin(phase * 0.12F);

        lance(c, m, phase, power);
        nozzle(c, m, r, g, b, phase, level, ignite, lod);

        // the plasma column round the frame, a glow in the colour and a white-hot thread in it
        prism(c, m, Z0, Z1, 1.35F + 0.1F * pulse, lod > 1 ? 6 : 12, r, g, b, 0.16F * level);
        prism(c, m, Z0, Z1, 0.9F, lod > 0 ? 6 : 10, r, g, b, 0.22F * level);
        prism(c, m, Z0, Z1, 0.28F, 8, 0.9F, 0.96F, 1F, 0.55F * level);

        // pulses running up the column: flat rings that grow and fade as they climb
        int pulses = lod > 1 ? 4 : lod > 0 ? 6 : 9;
        for (int k = 0; k < pulses; k++) {
            float t = ((phase * 0.011F + k / (float) pulses) % 1F);
            float z = Z1 - (Z1 - Z0) * t;
            float fade = Mth.sin(t * Mth.PI);
            disc(c, m, CX, CY, z, 0.5F + 0.9F * t, 0.35F, lod > 0 ? 6 : 10, r, g, b, 0.5F * fade * level);
        }

        // the core: a pulsing sphere with a bright heart
        float radius = 0.8F + 0.2F * pulse;
        sphere(c, m, CX, CY, CORE_Z, radius, lod > 0 ? 5 : 8, lod > 0 ? 8 : 12, r, g, b, 0.22F * level);
        sphere(c, m, CX, CY, CORE_Z, radius * 0.55F, 6, 10, 0.85F, 0.95F, 1F, 0.45F * level);

        // halo rings round the core, each turning in its own plane
        for (int k = 0; k < (lod > 1 ? 1 : 3); k++) {
            float a = phase * (0.035F + 0.012F * k) + k * 2.1F, tilt = Mth.PI * (0.18F + 0.27F * k);
            // the plane's basis: the ring's x axis turned about z by a, its y axis tipped by the tilt
            float ux = Mth.cos(a), uy = Mth.sin(a), uz = 0;
            float vx = -Mth.sin(a) * Mth.cos(tilt), vy = Mth.cos(a) * Mth.cos(tilt), vz = Mth.sin(tilt);
            ring(c, m, CX, CY, CORE_Z, ux, uy, uz, vx, vy, vz, 1.3F, 0.12F, lod > 0 ? 20 : 36, r, g, b, 0.55F * level);
        }

        // fans of light turning round the core: crossed ribbons from it out to the walls
        for (int k = 0; k < (lod > 0 ? 3 : 6); k++) {
            float a = phase * 0.02F + k * Mth.PI / 3F;
            float len = 1.1F + 0.2F * Mth.sin(phase * 0.1F + k);
            float dx = Mth.cos(a), dy = Mth.sin(a);
            ribbon(c, m, CX, CY, CORE_Z, CX + dx * len, CY + dy * len, CORE_Z, -dy * 0.13F, dx * 0.13F, 0, r, g, b,
                    0.45F * level, 0.02F);
            ribbon(c, m, CX, CY, CORE_Z, CX + dx * len, CY + dy * len, CORE_Z, 0, 0, 0.13F, r, g, b, 0.3F * level,
                    0.02F);
        }

        // orbs on their paths round the column, a trail behind each
        for (int k = 0; k < (lod > 1 ? 0 : lod > 0 ? 3 : 5); k++) {
            float speed = 0.05F + 0.013F * k;
            float rad = 0.95F + 0.1F * k;
            float zMid = ORBIT_Z0 + (ORBIT_Z1 - ORBIT_Z0) * ((k * 0.37F + 0.1F) % 1F);
            float zSwing = 3.5F + 1.2F * k;
            int tails = lod > 0 ? 4 : 7;
            for (int tail = 0; tail < tails; tail++) {
                float a = phase * speed * (k % 2 == 0 ? 1 : -1) - tail * 0.09F + k * 1.3F;
                float z = zMid + zSwing * Mth.sin(phase * 0.013F * (1 + k * 0.3F) + k - tail * 0.04F);
                float fade = 1F - tail / (float) tails;
                float size = 0.2F * (0.4F + 0.6F * fade);
                sphere(c, m, CX + Mth.cos(a) * rad, CY + Mth.sin(a) * rad, z, size, 3, 5, r, g, b, 0.5F * fade * level);
                if (tail == 0) {
                    sphere(c, m, CX + Mth.cos(a) * rad, CY + Mth.sin(a) * rad, z, size * 0.5F, 3, 5, 1F, 1F, 1F,
                            0.9F * level);
                }
            }
        }
    }

    /**
     * The start of the column under the emitter: a hot disc at the nozzle with a ring shock-waving down the hollow,
     * sparks flaring out from it and a white flash while the station ignites ({@code ignite} 1 at the start, 0 once
     * it runs).
     */
    private static void nozzle(VertexConsumer c, Matrix4f m, float r, float g, float b, float phase, float level,
                               float ignite, int lod) {
        float z = EmitterDraw.NOZZLE_Z;
        float flare = 0.5F + 0.5F * Mth.sin(phase * 0.2F);
        float k = (0.55F + 0.45F * flare) * level + ignite;
        disc(c, m, CX, CY, z + 0.05F, 0.75F + 0.4F * ignite, 0.7F, 10, r, g, b, Math.min(1F, 0.5F * k));
        disc(c, m, CX, CY, z + 0.1F, 0.4F, 0.4F, 8, 1F, 1F, 1F, Math.min(1F, 0.8F * k));
        sphere(c, m, CX, CY, z + 0.4F, 0.5F + 0.3F * ignite, 4, 8, 0.9F, 0.96F, 1F, Math.min(1F, 0.4F * k));
        // a short cone of light below the nozzle, wider while it ignites
        fade(c, m, z, z + 4F, 0.5F, 1.35F, lod > 0 ? 6 : 10, r, g, b, Math.min(1F, 0.55F * k), 0F);
        // sparks flaring out of the nozzle
        int sparks = lod > 0 ? 4 : 8;
        for (int i = 0; i < sparks; i++) {
            float a = phase * 0.04F + i * Mth.TWO_PI / sparks;
            float t = (phase * 0.03F + i * 0.37F) % 1F;
            float len = 0.6F + 1.2F * t + ignite;
            float dx = Mth.cos(a), dy = Mth.sin(a);
            ribbon(c, m, CX + dx * 0.3F, CY + dy * 0.3F, z + 0.3F, CX + dx * len, CY + dy * len, z + 1.2F + 2.5F * t,
                    -dy * 0.07F, dx * 0.07F, 0, 0.9F, 0.96F, 1F, 0.7F * (1F - t) * Math.min(1F, k), 0.02F);
        }
        // ignition: expanding shock rings and a flash down the tube
        if (ignite > 0.02F) {
            for (int i = 0; i < 3; i++) {
                float t = (phase * 0.05F + i / 3F) % 1F;
                disc(c, m, CX, CY, z + 1F + 9F * t, 0.6F + 0.8F * t, 0.3F, 10, 1F, 1F, 1F, 0.7F * (1F - t) * ignite);
            }
            prism(c, m, z, z + 12F, 0.9F, 8, 1F, 1F, 1F, 0.25F * ignite);
        }
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

        // the source: a glow and a turning ring at the bottom ring's level, inside the tower
        sphere(c, m, CX, CY, LANCE_Z0, 1.2F, 6, 10, r, g, b, 0.28F * k);
        sphere(c, m, CX, CY, LANCE_Z0, 0.6F, 5, 8, 0.9F, 0.95F, 1F, 0.5F * k);
        float a = phase * 0.08F;
        ring(c, m, CX, CY, LANCE_Z0, Mth.cos(a), Mth.sin(a), 0, -Mth.sin(a) * 0.5F, Mth.cos(a) * 0.5F, 0.866F, 1.4F,
                0.14F, 28, r, g, b, 0.5F * k);

        // the beam: steady down the hollow, then widening and fading out 20 blocks below the foot
        fade(c, m, LANCE_Z0, FOOT_Z, 1.2F, 1.35F, 12, r, g, b, 0.36F * k, 0.3F * k);
        fade(c, m, FOOT_Z, LANCE_Z1, 1.35F, 2.6F, 12, r, g, b, 0.3F * k, 0F);
        fade(c, m, LANCE_Z0, FOOT_Z, 0.7F, 0.85F, 10, r, g, b, 0.3F * k, 0.28F * k);
        fade(c, m, FOOT_Z, LANCE_Z1, 0.85F, 1.4F, 10, r, g, b, 0.28F * k, 0F);
        fade(c, m, LANCE_Z0, LANCE_Z1 - 4F, 0.25F, 0.4F, 8, 1F, 1F, 1F, 0.8F * k, 0F);

        // shock rings running down it, widening and fading
        for (int i = 0; i < 9; i++) {
            float t = (phase * 0.012F + i / 9F) % 1F;
            float z = LANCE_Z0 + (LANCE_Z1 - LANCE_Z0) * t;
            float rad = z < FOOT_Z ? 1.25F : 1.25F + 1.3F * (z - FOOT_Z) / (LANCE_Z1 - FOOT_Z);
            disc(c, m, CX, CY, z, rad, 0.16F, 12, r, g, b, 0.6F * Mth.sin(t * Mth.PI) * k);
        }
    }

    /** An open prism whose radius and alpha run from the first end to the second (per-vertex alpha). */
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

    /** An open prism round the axis between two heights. */
    private static void prism(VertexConsumer c, Matrix4f m, float z0, float z1, float radius, int sides, float r,
                              float g, float b, float a) {
        for (int i = 0; i < sides; i++) {
            float a0 = Mth.TWO_PI * i / sides, a1 = Mth.TWO_PI * (i + 1) / sides;
            quad(c, m, new float[] {
                    CX + Mth.cos(a0) * radius, CY + Mth.sin(a0) * radius, z0,
                    CX + Mth.cos(a1) * radius, CY + Mth.sin(a1) * radius, z0,
                    CX + Mth.cos(a1) * radius, CY + Mth.sin(a1) * radius, z1,
                    CX + Mth.cos(a0) * radius, CY + Mth.sin(a0) * radius, z1 }, r, g, b, a);
        }
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

    /** A flat ribbon from a to b, wide by (wx, wy, wz) each side. */
    private static void ribbon(VertexConsumer c, Matrix4f m, float ax, float ay, float az, float bx, float by,
                               float bz, float wx, float wy, float wz, float r, float g, float b, float a,
                               float tipFade) {
        c.vertex(m, ax + wx, ay + wy, az + wz).color(r, g, b, a).endVertex();
        c.vertex(m, ax - wx, ay - wy, az - wz).color(r, g, b, a).endVertex();
        c.vertex(m, bx - wx * tipFade, by - wy * tipFade, bz - wz * tipFade).color(r, g, b, 0F).endVertex();
        c.vertex(m, bx + wx * tipFade, by + wy * tipFade, bz + wz * tipFade).color(r, g, b, 0F).endVertex();
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
