package com.af9.core.client.skin;

import net.minecraft.util.Mth;

import com.mojang.blaze3d.vertex.VertexConsumer;
import org.joml.Matrix4f;

/**
 * The Orbital Array Mk2's hover lance: the plasma that holds the tower up, leaving the opening in the sealed foot. A
 * source glow in the opening with a turning ring, a violet plume widening and fading below it with strands of plasma
 * twisting round it, standing shock diamonds in it like a rocket's exhaust, shock rings running down it and embers
 * streaming away. In the pattern's own space (right / up / back from the controller, the origin the controller block's
 * corner). It is a few hundred added-light quads written straight into the buffer; the farther it is looked at from, the
 * fewer.
 */
final class CoreDraw {

    private static final float CX = 0.5F, CY = 0.5F;
    /** The lance's span along the back axis: from the foot's opening (the foot's underside is at 99) to 22 below it. */
    private static final float Z0 = 99.3F, Z1 = 121F;
    /** Violet plasma, and its white-hot core. */
    private static final float R = 0.6F, G = 0.35F, B = 1F;

    private CoreDraw() {}

    /**
     * @param time  the level's time in ticks, with the partial tick
     * @param power 0..1, idle to working
     * @param lod   0 near, 1 and 2 farther
     */
    static void draw(VertexConsumer c, Matrix4f m, float time, float power, int lod) {
        float level = 0.5F + 0.5F * power;
        float flicker = 0.88F + 0.12F * Mth.sin(time * 0.7F) * Mth.sin(time * 0.31F);
        float k = level * flicker;
        float len = Z1 - Z0;

        // the source: a glow in the opening and a ring turning under the foot
        FxGeo.glow(c, m, CX, CY, Z0 + 0.2F, 1.6F, lod == 0 ? 10 : 6, R, G, B, 0.5F * k);
        float a = time * 0.09F;
        FxGeo.softRing(c, m, CX, CY, Z0 + 0.5F, Mth.cos(a), Mth.sin(a), 0F, -Mth.sin(a) * 0.5F, Mth.cos(a) * 0.5F,
                0.866F, 1.55F, 0.22F, lod == 0 ? 28 : 16, R, G, B, 0.6F * k);

        // the plume: three nested cones, each fading with its length
        int sides = lod == 0 ? 16 : lod == 1 ? 10 : 6, slices = lod == 0 ? 7 : 4;
        cone(c, m, 1.3F, 3.6F, len, sides, slices, R, G, B, 0.3F * k, 1.6F);
        cone(c, m, 0.75F, 1.9F, len, sides, slices, R, G, B, 0.34F * k, 1.4F);
        cone(c, m, 0.28F, 0.6F, len * 0.8F, Math.max(6, sides / 2), slices, 0.92F, 0.9F, 1F, 0.8F * k, 1.2F);

        if (lod < 2) {
            // strands of plasma twisting down the plume
            int strands = lod == 0 ? 4 : 2, steps = 14;
            for (int s = 0; s < strands; s++) {
                float phase = Mth.TWO_PI * s / strands;
                float px = 0F, py = 0F, pz = 0F, pa = 0F;
                for (int i = 0; i <= steps; i++) {
                    float u = i / (float) steps;
                    float rad = 0.9F + 1.9F * u;
                    float ang = phase + time * 0.12F + u * 5.5F;
                    float x = CX + Mth.cos(ang) * rad, y = CY + Mth.sin(ang) * rad, z = Z0 + len * u;
                    float al = 0.55F * k * (1F - u) * (1F - u);
                    if (i > 0) FxGeo.line(c, m, px, py, pz, x, y, z, 0.07F + 0.05F * u, 0.8F, 0.65F, 1F, pa, al);
                    px = x;
                    py = y;
                    pz = z;
                    pa = al;
                }
            }
            // standing shock diamonds: a bright spindle in the plume every few blocks, throbbing
            int diamonds = lod == 0 ? 4 : 2;
            for (int i = 0; i < diamonds; i++) {
                float u = 0.16F + 0.17F * i;
                float throb = 0.75F + 0.25F * Mth.sin(time * 0.5F - i * 1.3F);
                float rad = (0.55F + 1.1F * u) * 0.9F, half = 1.25F - 0.15F * i;
                float z = Z0 + len * u;
                float al = 0.65F * k * (1F - u) * throb;
                spindle(c, m, z, rad, half, 8, 0.85F, 0.8F, 1F, al);
            }
            // shock rings running down it, widening and fading
            int rings = lod == 0 ? 7 : 4;
            for (int i = 0; i < rings; i++) {
                float t = (time * 0.014F + i / (float) rings) % 1F;
                float z = Z0 + len * t;
                FxGeo.softRing(c, m, CX, CY, z, 1F, 0F, 0F, 0F, 1F, 0F, 1.1F + 2.3F * t, 0.2F, lod == 0 ? 20 : 12, R, G, B,
                        0.6F * Mth.sin(t * Mth.PI) * k);
            }
        }
        if (lod == 0) {
            // embers streaming away from the opening
            for (int i = 0; i < 28; i++) {
                float t = (time * (0.009F + 0.006F * FxGeo.hash(i, 1)) + FxGeo.hash(i, 2)) % 1F;
                float ang = Mth.TWO_PI * FxGeo.hash(i, 3) + t * 3F;
                float rad = (0.4F + 1.8F * FxGeo.hash(i, 4)) * (0.5F + 1.2F * t);
                FxGeo.mote(c, m, CX + Mth.cos(ang) * rad, CY + Mth.sin(ang) * rad, Z0 + len * t * 1.05F,
                        0.05F + 0.04F * FxGeo.hash(i, 5), 0.85F, 0.75F, 1F, 0.8F * k * (1F - t));
            }
        }
    }

    /**
     * An open cone from the opening: radius {@code r0} at the top widening to {@code r1} at {@code len} below, its alpha
     * falling off with the distance (steeper with a bigger {@code falloff}).
     */
    private static void cone(VertexConsumer c, Matrix4f m, float r0, float r1, float len, int sides, int slices, float r,
                             float g, float b, float alpha, float falloff) {
        for (int sl = 0; sl < slices; sl++) {
            float u0 = sl / (float) slices, u1 = (sl + 1) / (float) slices;
            float z0 = Z0 + len * u0, z1 = Z0 + len * u1;
            float rad0 = r0 + (r1 - r0) * u0, rad1 = r0 + (r1 - r0) * u1;
            float a0 = alpha * (float) Math.pow(1F - u0, falloff), a1 = alpha * (float) Math.pow(1F - u1, falloff);
            for (int i = 0; i < sides; i++) {
                float c0 = FxGeo.cos(i, sides), s0 = FxGeo.sin(i, sides), c1 = FxGeo.cos(i + 1, sides),
                        s1 = FxGeo.sin(i + 1, sides);
                FxGeo.vertex(c, m, CX + c0 * rad0, CY + s0 * rad0, z0, r, g, b, a0);
                FxGeo.vertex(c, m, CX + c1 * rad0, CY + s1 * rad0, z0, r, g, b, a0);
                FxGeo.vertex(c, m, CX + c1 * rad1, CY + s1 * rad1, z1, r, g, b, a1);
                FxGeo.vertex(c, m, CX + c0 * rad1, CY + s0 * rad1, z1, r, g, b, a1);
            }
        }
    }

    /** Two cones base to base: a diamond of light {@code half} long each way, {@code rad} across at its middle. */
    private static void spindle(VertexConsumer c, Matrix4f m, float zMid, float rad, float half, int sides, float r,
                                float g, float b, float alpha) {
        for (int i = 0; i < sides; i++) {
            float c0 = FxGeo.cos(i, sides), s0 = FxGeo.sin(i, sides), c1 = FxGeo.cos(i + 1, sides),
                    s1 = FxGeo.sin(i + 1, sides);
            // upper half (towards the foot), then lower; the tips are dark, the middle bright
            FxGeo.vertex(c, m, CX, CY, zMid - half, r, g, b, 0F);
            FxGeo.vertex(c, m, CX, CY, zMid - half, r, g, b, 0F);
            FxGeo.vertex(c, m, CX + c1 * rad, CY + s1 * rad, zMid, r, g, b, alpha);
            FxGeo.vertex(c, m, CX + c0 * rad, CY + s0 * rad, zMid, r, g, b, alpha);
            FxGeo.vertex(c, m, CX + c0 * rad, CY + s0 * rad, zMid, r, g, b, alpha);
            FxGeo.vertex(c, m, CX + c1 * rad, CY + s1 * rad, zMid, r, g, b, alpha);
            FxGeo.vertex(c, m, CX, CY, zMid + half, r, g, b, 0F);
            FxGeo.vertex(c, m, CX, CY, zMid + half, r, g, b, 0F);
        }
    }
}
