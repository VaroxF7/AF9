package com.af9.core.client.skin;

import net.minecraft.util.Mth;

import com.mojang.blaze3d.vertex.VertexConsumer;
import org.joml.Matrix4f;

/**
 * The little pieces of light the Orbital Array Mk2's effects are made of ({@link OrbitalFx}, {@link CoreDraw}), written
 * straight into an additive colour buffer in the pattern's own space: soft ribbons, rings, discs and glows. Nothing here
 * allocates, so an effect costs the vertices it writes and nothing else.
 */
final class FxGeo {

    private static final int TABLE = 64;
    private static final float[] COS = new float[TABLE + 1], SIN = new float[TABLE + 1];

    static {
        for (int i = 0; i <= TABLE; i++) {
            COS[i] = (float) Math.cos(Math.PI * 2 * i / TABLE);
            SIN[i] = (float) Math.sin(Math.PI * 2 * i / TABLE);
        }
    }

    private FxGeo() {}

    /** The cosine of the {@code i}-th of {@code n} steps round a circle (n divides 64, or the nearest table entry). */
    static float cos(int i, int n) {
        return COS[Math.round(i * (float) TABLE / n)];
    }

    static float sin(int i, int n) {
        return SIN[Math.round(i * (float) TABLE / n)];
    }

    /** A pseudo random number from 0 (inclusive) to 1 for the pair {@code (n, k)}: the same each time. */
    static float hash(int n, int k) {
        int h = n * 374761393 + k * 668265263;
        h = (h ^ (h >>> 13)) * 1274126177;
        h ^= h >>> 16;
        return (h & 0xFFFFFF) / 16777216F;
    }

    static float clamp01(float x) {
        return x < 0F ? 0F : x > 1F ? 1F : x;
    }

    static float smooth(float x) {
        x = clamp01(x);
        return x * x * (3F - 2F * x);
    }

    static void vertex(VertexConsumer c, Matrix4f m, float x, float y, float z, float r, float g, float b, float a) {
        c.vertex(m, x, y, z).color(r, g, b, a).endVertex();
    }

    /**
     * A ribbon of light from a to b, {@code w} half wide along the given direction: bright along its middle, fading to
     * nothing at both edges (two quads meeting on the middle line).
     */
    static void ribbon(VertexConsumer c, Matrix4f m, float ax, float ay, float az, float bx, float by, float bz,
                       float wx, float wy, float wz, float r, float g, float b, float aA, float aB) {
        vertex(c, m, ax + wx, ay + wy, az + wz, r, g, b, 0F);
        vertex(c, m, ax, ay, az, r, g, b, aA);
        vertex(c, m, bx, by, bz, r, g, b, aB);
        vertex(c, m, bx + wx, by + wy, bz + wz, r, g, b, 0F);
        vertex(c, m, ax, ay, az, r, g, b, aA);
        vertex(c, m, ax - wx, ay - wy, az - wz, r, g, b, 0F);
        vertex(c, m, bx - wx, by - wy, bz - wz, r, g, b, 0F);
        vertex(c, m, bx, by, bz, r, g, b, aB);
    }

    /**
     * A soft round line from a to b: two ribbons crossed along it, {@code w} half wide, so it reads as a glowing rod from
     * every side. {@code aA} / {@code aB}: the alpha at each end.
     */
    static void line(VertexConsumer c, Matrix4f m, float ax, float ay, float az, float bx, float by, float bz, float w,
                     float r, float g, float b, float aA, float aB) {
        float dx = bx - ax, dy = by - ay, dz = bz - az;
        float len = Mth.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 1.0E-4F) return;
        dx /= len;
        dy /= len;
        dz /= len;
        float hx = 0F, hy = 0F, hz = 1F;
        if (Math.abs(dz) >= 0.9F) {
            hx = 1F;
            hz = 0F;
        }
        // w1 = d x h, w2 = d x w1 (both normalised, w long)
        float w1x = dy * hz - dz * hy, w1y = dz * hx - dx * hz, w1z = dx * hy - dy * hx;
        float n1 = w / Mth.sqrt(w1x * w1x + w1y * w1y + w1z * w1z);
        w1x *= n1;
        w1y *= n1;
        w1z *= n1;
        float w2x = (dy * w1z - dz * w1y), w2y = (dz * w1x - dx * w1z), w2z = (dx * w1y - dy * w1x);
        float n2 = w / Mth.sqrt(w2x * w2x + w2y * w2y + w2z * w2z);
        w2x *= n2;
        w2y *= n2;
        w2z *= n2;
        ribbon(c, m, ax, ay, az, bx, by, bz, w1x, w1y, w1z, r, g, b, aA, aB);
        ribbon(c, m, ax, ay, az, bx, by, bz, w2x, w2y, w2z, r, g, b, aA, aB);
    }

    /**
     * A ring of light in the plane spanned by the unit vectors u and v: bright along {@code radius}, fading to nothing
     * {@code halfWidth} to either side of it.
     */
    static void softRing(VertexConsumer c, Matrix4f m, float cx, float cy, float cz, float ux, float uy, float uz,
                         float vx, float vy, float vz, float radius, float halfWidth, int segs, float r, float g,
                         float b, float a) {
        if (a <= 0.003F) return;
        float inner = Math.max(0.01F, radius - halfWidth), outer = radius + halfWidth;
        for (int i = 0; i < segs; i++) {
            float c0 = cos(i, segs), s0 = sin(i, segs), c1 = cos(i + 1, segs), s1 = sin(i + 1, segs);
            float x0 = ux * c0 + vx * s0, y0 = uy * c0 + vy * s0, z0 = uz * c0 + vz * s0;
            float x1 = ux * c1 + vx * s1, y1 = uy * c1 + vy * s1, z1 = uz * c1 + vz * s1;
            vertex(c, m, cx + x0 * inner, cy + y0 * inner, cz + z0 * inner, r, g, b, 0F);
            vertex(c, m, cx + x1 * inner, cy + y1 * inner, cz + z1 * inner, r, g, b, 0F);
            vertex(c, m, cx + x1 * radius, cy + y1 * radius, cz + z1 * radius, r, g, b, a);
            vertex(c, m, cx + x0 * radius, cy + y0 * radius, cz + z0 * radius, r, g, b, a);
            vertex(c, m, cx + x0 * radius, cy + y0 * radius, cz + z0 * radius, r, g, b, a);
            vertex(c, m, cx + x1 * radius, cy + y1 * radius, cz + z1 * radius, r, g, b, a);
            vertex(c, m, cx + x1 * outer, cy + y1 * outer, cz + z1 * outer, r, g, b, 0F);
            vertex(c, m, cx + x0 * outer, cy + y0 * outer, cz + z0 * outer, r, g, b, 0F);
        }
    }

    /** A disc in the plane spanned by u and v: {@code aCentre} in the middle, {@code aEdge} at the rim. */
    static void disc(VertexConsumer c, Matrix4f m, float cx, float cy, float cz, float ux, float uy, float uz,
                     float vx, float vy, float vz, float radius, int segs, float r, float g, float b, float aCentre,
                     float aEdge) {
        if (aCentre <= 0.003F && aEdge <= 0.003F) return;
        for (int i = 0; i < segs; i++) {
            float c0 = cos(i, segs), s0 = sin(i, segs), c1 = cos(i + 1, segs), s1 = sin(i + 1, segs);
            vertex(c, m, cx, cy, cz, r, g, b, aCentre);
            vertex(c, m, cx, cy, cz, r, g, b, aCentre);
            vertex(c, m, cx + (ux * c1 + vx * s1) * radius, cy + (uy * c1 + vy * s1) * radius,
                    cz + (uz * c1 + vz * s1) * radius, r, g, b, aEdge);
            vertex(c, m, cx + (ux * c0 + vx * s0) * radius, cy + (uy * c0 + vy * s0) * radius,
                    cz + (uz * c0 + vz * s0) * radius, r, g, b, aEdge);
        }
    }

    /** A ball of light seen from every side: three discs across each other, white-hot in the middle. */
    static void glow(VertexConsumer c, Matrix4f m, float x, float y, float z, float radius, int segs, float r, float g,
                     float b, float a) {
        if (a <= 0.003F || radius <= 0F) return;
        float hot = Math.min(1F, a * 1.4F);
        for (int plane = 0; plane < 3; plane++) {
            // the planes xy, xz and yz: u and v are the two axes of the plane
            int u = plane == 2 ? 1 : 0, v = plane == 0 ? 1 : 2;
            for (int i = 0; i < segs; i++) {
                float c0 = cos(i, segs) * radius, s0 = sin(i, segs) * radius;
                float c1 = cos(i + 1, segs) * radius, s1 = sin(i + 1, segs) * radius;
                vertex(c, m, x, y, z, 1F, 1F, 1F, hot);
                vertex(c, m, x, y, z, 1F, 1F, 1F, hot);
                vertex(c, m, x + (u == 0 ? c1 : 0F), y + (u == 1 ? c1 : v == 1 ? s1 : 0F), z + (v == 2 ? s1 : 0F),
                        r, g, b, 0F);
                vertex(c, m, x + (u == 0 ? c0 : 0F), y + (u == 1 ? c0 : v == 1 ? s0 : 0F), z + (v == 2 ? s0 : 0F),
                        r, g, b, 0F);
            }
        }
    }

    /** A mote of light: two small squares across each other. */
    static void mote(VertexConsumer c, Matrix4f m, float x, float y, float z, float size, float r, float g, float b,
                     float a) {
        if (a <= 0.003F) return;
        vertex(c, m, x - size, y, z - size, r, g, b, a);
        vertex(c, m, x + size, y, z - size, r, g, b, a);
        vertex(c, m, x + size, y, z + size, r, g, b, a);
        vertex(c, m, x - size, y, z + size, r, g, b, a);
        vertex(c, m, x - size, y - size, z, r, g, b, a);
        vertex(c, m, x + size, y - size, z, r, g, b, a);
        vertex(c, m, x + size, y + size, z, r, g, b, a);
        vertex(c, m, x - size, y + size, z, r, g, b, a);
    }
}
