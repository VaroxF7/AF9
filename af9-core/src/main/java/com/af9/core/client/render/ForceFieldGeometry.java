package com.af9.core.client.render;

/**
 * The DTPF's force field as plain geometry (no Minecraft types, so it can be run and looked at on its own): a tube of
 * light round the beam, {@code radius} blocks wide and running from {@code start} to {@code end} blocks behind the
 * controller's centre, built from quads handed to a {@link Sink}. {@link ForceFieldRender} draws them in the game.
 * <p>
 * Everything is in the field's own frame: {@code x} to the controller's left, {@code y} up, {@code z} behind the
 * controller, the axis through the beam at {@code x = y = 0}. Four styles (the pack's owner picks one, see
 * {@link ForceFieldRender}):
 * <ol start="0">
 * <li>{@link #HOOPS}: rings of light that run down the tube, each dragging a trail and long streaks behind it, with
 * sparks drifting about; the tube itself a faint blue sheath;</li>
 * <li>{@link #SHEATH}: a plasma sheath: two nested shells with spiralling bands of light that wind down the tube in
 * opposite turns, slow wide pulses and glowing end caps;</li>
 * <li>{@link #HELIX}: four strands twisting down the tube with beads of light on them, and a counter-turning inner
 * three joined to them by rungs;</li>
 * <li>{@link #LATTICE}: a diamond mesh of counter-wound lines and rings that flickers cell by cell, with a pulse running
 * down it and the rings at the hall's stations flaring as it passes.</li>
 * </ol>
 * Colours are the forge's: deep blue, ice cyan and a white-hot core. The quads are drawn from both sides.
 * {@code power} (0 to 1) is how hard it burns: low while the hall only stands, high while a recipe runs.
 */
public final class ForceFieldGeometry {

    public static final int HOOPS = 0, SHEATH = 1, HELIX = 2, LATTICE = 3, STYLES = 4;
    /** Layers: a translucent shell (alpha blended), and added light. */
    public static final int SHELL = 0, GLOW = 1;

    /** Where the hall's rings stand along the tube (blocks behind the controller's centre). */
    private static final float[] STATIONS = { 6F, 11F, 16F, 21F, 26F, 31F, 36F };
    private static final float TAU = (float) (Math.PI * 2);

    /** Receives the quads: 4 vertices of x, y, z, red, green, blue, alpha (28 floats), valid until the call returns. */
    public interface Sink {

        void quad(int layer, float[] vertices);
    }

    private final Sink sink;
    private final float radius, start, end, time, power;
    private final float[] q = new float[28];
    private float cr, cg, cb;

    public ForceFieldGeometry(Sink sink, float radius, float start, float end, float time, float power) {
        this.sink = sink;
        this.radius = radius;
        this.start = start;
        this.end = end;
        this.time = time;
        this.power = power;
    }

    /** Emits one frame of the given style. */
    public void emit(int style) {
        switch (style) {
            case SHEATH -> sheath();
            case HELIX -> helix();
            case LATTICE -> lattice();
            default -> hoops();
        }
    }

    // ---------------------------------------------------------------------------------------------- colours

    private static final float[] BLUE = { 0.16F, 0.42F, 1F }, CYAN = { 0.40F, 0.82F, 1F }, WHITE = { 0.88F, 0.96F, 1F };

    private void colour(float[] c) {
        cr = c[0];
        cg = c[1];
        cb = c[2];
    }

    private void colour(float[] a, float[] b, float mix) {
        mix = clamp(mix);
        cr = a[0] + (b[0] - a[0]) * mix;
        cg = a[1] + (b[1] - a[1]) * mix;
        cb = a[2] + (b[2] - a[2]) * mix;
    }

    // ---------------------------------------------------------------------------------------------- the tube

    /** Fades the field in and out over the last blocks at both ends. */
    private float env(float z) {
        float e = Math.min((z - start) / 2.5F, (end - z) / 2.5F);
        e = clamp(e);
        return e * e * (3 - 2 * e);
    }

    private float length() {
        return end - start;
    }

    private static float clamp(float v) {
        return v < 0 ? 0 : Math.min(v, 1F);
    }

    private static float frac(float v) {
        return v - (float) Math.floor(v);
    }

    private static float hash(int i) {
        int h = i * 0x9E3779B1;
        h ^= h >>> 15;
        h *= 0x85EBCA6B;
        h ^= h >>> 13;
        h *= 0xC2B2AE35;
        h ^= h >>> 16;
        return (h & 0xFFFF) / 65535F;
    }

    private static float cos(float a) {
        return (float) Math.cos(a);
    }

    private static float sin(float a) {
        return (float) Math.sin(a);
    }

    private void vertex(int i, float x, float y, float z, float a) {
        int o = i * 7;
        q[o] = x;
        q[o + 1] = y;
        q[o + 2] = z;
        q[o + 3] = cr;
        q[o + 4] = cg;
        q[o + 5] = cb;
        q[o + 6] = a;
    }

    /** A quad in the colour set last, with an alpha per corner. */
    private void quad(int layer, float x0, float y0, float z0, float a0, float x1, float y1, float z1, float a1,
                      float x2, float y2, float z2, float a2, float x3, float y3, float z3, float a3) {
        if (a0 <= 0.002F && a1 <= 0.002F && a2 <= 0.002F && a3 <= 0.002F) return;
        vertex(0, x0, y0, z0, a0);
        vertex(1, x1, y1, z1, a1);
        vertex(2, x2, y2, z2, a2);
        vertex(3, x3, y3, z3, a3);
        sink.quad(layer, q);
    }

    /** A patch of a cylinder of radius r between two angles and two depths, an alpha at each corner. */
    private void patch(int layer, float r, float th0, float th1, float z0, float z1, float a00, float a10, float a11,
                       float a01) {
        quad(layer, r * cos(th0), r * sin(th0), z0, a00, r * cos(th1), r * sin(th1), z0, a10, r * cos(th1),
                r * sin(th1), z1, a11, r * cos(th0), r * sin(th0), z1, a01);
    }

    /** A band of light round the tube at depth z: strongest on its middle line, fading to nothing half a width out. */
    private void band(int layer, float z, float halfWidth, float r, float alpha, int segments) {
        for (int i = 0; i < segments; i++) {
            float th0 = TAU * i / segments, th1 = TAU * (i + 1) / segments;
            patch(layer, r, th0, th1, z - halfWidth, z, 0, 0, alpha, alpha);
            patch(layer, r, th0, th1, z, z + halfWidth, alpha, alpha, 0, 0);
        }
    }

    /** A flat ring (annulus) across the tube at depth z, from r0 to r1. */
    private void disc(int layer, float z, float r0, float r1, float a0, float a1, int segments) {
        for (int i = 0; i < segments; i++) {
            float th0 = TAU * i / segments, th1 = TAU * (i + 1) / segments;
            quad(layer, r0 * cos(th0), r0 * sin(th0), z, a0, r0 * cos(th1), r0 * sin(th1), z, a0, r1 * cos(th1),
                    r1 * sin(th1), z, a1, r1 * cos(th0), r1 * sin(th0), z, a1);
        }
    }

    /** A soft dot: three crossing discs of eight triangles, bright in the middle, nothing at the rim. */
    private void dot(float x, float y, float z, float size, float alpha) {
        for (int plane = 0; plane < 3; plane++) {
            for (int i = 0; i < 8; i++) {
                float a0 = TAU * i / 8, a1 = TAU * (i + 1) / 8;
                float u0 = cos(a0) * size, v0 = sin(a0) * size, u1 = cos(a1) * size, v1 = sin(a1) * size;
                if (plane == 0) {
                    quad(GLOW, x, y, z, alpha, x + u0, y + v0, z, 0, x + u1, y + v1, z, 0, x, y, z, alpha);
                } else if (plane == 1) {
                    quad(GLOW, x, y, z, alpha, x + u0, y, z + v0, 0, x + u1, y, z + v1, 0, x, y, z, alpha);
                } else {
                    quad(GLOW, x, y, z, alpha, x, y + u0, z + v0, 0, x, y + u1, z + v1, 0, x, y, z, alpha);
                }
            }
        }
    }

    /** A strip of light along the tube at angle theta, from z0 to z1, fading from a0 to a1 (width in blocks). */
    private void streak(int layer, float theta, float r, float z0, float z1, float a0, float a1, float width) {
        float ox = -sin(theta) * width / 2, oy = cos(theta) * width / 2;
        float x = r * cos(theta), y = r * sin(theta);
        quad(layer, x - ox, y - oy, z0, a0, x + ox, y + oy, z0, a0, x + ox, y + oy, z1, a1, x - ox, y - oy, z1, a1);
    }

    /** The faint blue sheath of the tube: a cylinder, a touch brighter where the light passes (shell layer). */
    private void shellFill(float r, float base, float ripple, float speed, int segments) {
        colour(BLUE);
        int steps = Math.round(length());
        for (int k = 0; k < steps; k++) {
            float z0 = start + length() * k / steps, z1 = start + length() * (k + 1) / steps;
            for (int i = 0; i < segments; i++) {
                float th0 = TAU * i / segments, th1 = TAU * (i + 1) / segments;
                float a0 = shellAlpha(base, ripple, speed, th0, z0), a1 = shellAlpha(base, ripple, speed, th1, z0);
                float a2 = shellAlpha(base, ripple, speed, th1, z1), a3 = shellAlpha(base, ripple, speed, th0, z1);
                patch(SHELL, r, th0, th1, z0, z1, a0, a1, a2, a3);
            }
        }
    }

    private float shellAlpha(float base, float ripple, float speed, float theta, float z) {
        float wave = 0.5F + 0.5F * sin(theta * 3 + z * 0.45F - time * speed);
        return (base + ripple * wave) * env(z) * (0.55F + 0.45F * power);
    }

    // ---------------------------------------------------------------------------------------------- 0: hoops

    private void hoops() {
        shellFill(radius, 0.035F, 0.04F, 0.07F, 32);
        float speed = 0.12F + 0.26F * power;
        int count = 9;
        float spacing = length() / count;
        for (int k = 0; k < count; k++) {
            float z = start + frac((time * speed / length()) + (float) k / count) * length();
            float r = radius * (0.90F + 0.08F * sin(k * 2.3F + time * 0.03F));
            float e = env(z) * (0.55F + 0.45F * power);
            // the hoop: a white-hot thin band inside a blue glow
            colour(BLUE);
            band(GLOW, z, 0.55F, r, 0.30F * e, 36);
            colour(CYAN);
            band(GLOW, z, 0.22F, r, 0.55F * e, 36);
            colour(WHITE);
            band(GLOW, z, 0.07F, r, 0.95F * e, 36);
            colour(CYAN);
            disc(GLOW, z, r, r + 0.35F, 0.28F * e, 0F, 36);
            // trail: ghosts of the hoop behind it, smaller and fainter
            for (int g = 1; g <= 6; g++) {
                float zg = z - g * 0.45F;
                float eg = env(zg) * (0.55F + 0.45F * power) * (1F - g / 7F);
                colour(CYAN);
                band(GLOW, zg, 0.12F, r * (1F - 0.012F * g), 0.40F * eg, 36);
            }
            // long streaks dragged behind it, along the tube
            for (int s = 0; s < 18; s++) {
                float theta = TAU * (s + hash(k * 31 + s) * 0.6F) / 18;
                float zs = z - 0.2F;
                float len = 2.2F + 2.6F * hash(k * 57 + s * 3);
                float z2 = Math.max(start, zs - len);
                colour(s % 3 == 0 ? WHITE : CYAN);
                streak(GLOW, theta, r * 0.985F, zs, z2, 0.55F * e, 0F, 0.05F);
            }
        }
        // sparks drifting about in the middle of the tube
        for (int s = 0; s < 48; s++) {
            float z = start + frac(hash(s) + time * (0.004F + 0.01F * hash(s + 99)) * (0.4F + power)) * length();
            float ang = hash(s + 200) * TAU + time * 0.01F * (hash(s + 300) - 0.5F);
            float rr = radius * (0.2F + 0.75F * hash(s + 400));
            float tw = 0.5F + 0.5F * sin(time * 0.2F + s * 1.7F);
            colour(s % 4 == 0 ? WHITE : CYAN);
            dot(rr * cos(ang), rr * sin(ang), z, 0.05F + 0.05F * hash(s + 500), 0.7F * tw * env(z));
        }
    }

    // ---------------------------------------------------------------------------------------------- 1: sheath

    private void sheath() {
        // slow, as Janne asked: a fifth of the first version's pace
        float speed = 0.02F + 0.03F * power;
        float step = 0.4F;
        int steps = Math.round(length() / step);
        int segments = 48;
        for (int shell = 0; shell < 2; shell++) {
            float r = shell == 0 ? radius : 1.0F;
            float dir = shell == 0 ? 1F : -1F;
            for (int k = 0; k < steps; k++) {
                float z0 = start + length() * k / steps, z1 = start + length() * (k + 1) / steps;
                for (int i = 0; i < segments; i++) {
                    float th0 = TAU * i / segments, th1 = TAU * (i + 1) / segments;
                    float a00 = sheathBand(shell, dir, speed, th0, z0), a10 = sheathBand(shell, dir, speed, th1, z0);
                    float a11 = sheathBand(shell, dir, speed, th1, z1), a01 = sheathBand(shell, dir, speed, th0, z1);
                    colour(BLUE);
                    float f0 = 0.05F * env(z0), f1 = 0.05F * env(z1);
                    patch(SHELL, r, th0, th1, z0, z1, f0 + a00 * 0.25F, f0 + a10 * 0.25F, f1 + a11 * 0.25F,
                            f1 + a01 * 0.25F);
                    colour(CYAN, WHITE, (a00 + a11) * 0.8F - 0.2F);
                    patch(GLOW, r, th0, th1, z0, z1, a00 * 0.6F, a10 * 0.6F, a11 * 0.6F, a01 * 0.6F);
                }
            }
        }
        // slow wide pulses running down the tube
        for (int p = 0; p < 4; p++) {
            float z = start + frac(time * speed * 0.035F + p * 0.25F) * length();
            float e = env(z) * (0.5F + 0.5F * power);
            colour(BLUE);
            band(GLOW, z, 1.6F, radius * 0.97F, 0.20F * e, 48);
            colour(CYAN);
            band(GLOW, z, 0.35F, radius * 0.97F, 0.34F * e, 48);
        }
        // glowing end caps where the tube meets the gear plates
        for (int cap = 0; cap < 2; cap++) {
            float z = cap == 0 ? start + 0.02F : end - 0.02F;
            float pulse = 0.65F + 0.35F * sin(time * 0.03F + cap * 2F);
            colour(BLUE);
            disc(GLOW, z, 1.2F, radius, 0.30F * pulse * (0.4F + 0.6F * power), 0F, 48);
            colour(CYAN);
            disc(GLOW, z, 0.7F, 1.6F, 0.40F * pulse * (0.4F + 0.6F * power), 0F, 48);
        }
    }

    /** Thin bright filaments winding down a shell (two sets crossing), 0 to 1. */
    private float sheathBand(int shell, float dir, float speed, float theta, float z) {
        float a = 0.5F + 0.5F * sin(dir * (theta * (shell == 0 ? 3 : 4)) + z * (shell == 0 ? 0.5F : 0.8F) -
                dir * time * speed * 1.3F);
        float b = 0.5F + 0.5F * sin(theta * (shell == 0 ? 5 : 3) - dir * z * 0.7F + time * speed);
        float a2 = a * a, a4 = a2 * a2, b2 = b * b, b4 = b2 * b2;
        float ridge = Math.max(a4 * a4, 0.8F * b4 * b4) * (0.6F + 0.4F * sin(z * 0.6F - time * speed * 2F));
        return clamp(ridge) * env(z) * (0.5F + 0.5F * power);
    }

    // ---------------------------------------------------------------------------------------------- 2: helix

    private void helix() {
        shellFill(radius, 0.04F, 0.035F, 0.05F, 32);
        float turn = time * (0.025F + 0.05F * power);
        // outer strands (four) and inner strands (three, turning against them)
        for (int layer = 0; layer < 2; layer++) {
            int strands = layer == 0 ? 4 : 3;
            float r = layer == 0 ? radius * 0.93F : 1.5F;
            float twist = layer == 0 ? 0.28F : -0.55F;
            for (int s = 0; s < strands; s++) {
                float phase = TAU * s / strands + (layer == 0 ? turn : -turn * 1.4F);
                float prevX = 0, prevY = 0, prevZ = 0;
                boolean have = false;
                float step = 0.25F;
                for (float z = start; z <= end + 0.001F; z += step) {
                    float th = phase + z * twist;
                    float x = r * cos(th), y = r * sin(th);
                    if (have) {
                        float e = env(z) * (0.5F + 0.5F * power);
                        // a ribbon facing the axis, wide in the tangent
                        float tx = -sin(th) * 0.10F, ty = cos(th) * 0.10F;
                        colour(layer == 0 ? BLUE : CYAN);
                        float wide = 0.9F;
                        quad(GLOW, prevX - tx * 3, prevY - ty * 3, prevZ, 0, prevX, prevY, prevZ, 0.5F * e * wide,
                                x, y, z, 0.5F * e * wide, x - tx * 3, y - ty * 3, z, 0);
                        quad(GLOW, prevX, prevY, prevZ, 0.5F * e * wide, prevX + tx * 3, prevY + ty * 3, prevZ, 0,
                                x + tx * 3, y + ty * 3, z, 0, x, y, z, 0.5F * e * wide);
                        colour(WHITE);
                        quad(GLOW, prevX - tx * 0.5F, prevY - ty * 0.5F, prevZ, 0.8F * e, prevX + tx * 0.5F,
                                prevY + ty * 0.5F, prevZ, 0.8F * e, x + tx * 0.5F, y + ty * 0.5F, z, 0.8F * e,
                                x - tx * 0.5F, y - ty * 0.5F, z, 0.8F * e);
                    }
                    prevX = x;
                    prevY = y;
                    prevZ = z;
                    have = true;
                }
                // beads of light running along the strand
                for (int b = 0; b < 5; b++) {
                    float z = start + frac(time * (0.006F + 0.012F * power) * (layer == 0 ? 1F : -1F) + b / 5F + s * 0.07F) *
                            length();
                    float th = phase + z * twist;
                    colour(WHITE);
                    dot(r * cos(th), r * sin(th), z, 0.34F, 0.95F * env(z));
                    colour(CYAN);
                    dot(r * cos(th), r * sin(th), z, 0.8F, 0.40F * env(z));
                }
            }
        }
        // rungs: the inner strands joined to the outer ones
        for (float z = start + 1.25F; z < end; z += 2.5F) {
            float e = env(z) * (0.35F + 0.65F * power) * (0.6F + 0.4F * sin(time * 0.1F + z));
            float thOuter = turn + z * 0.28F, thInner = -turn * 1.4F + z * -0.55F;
            colour(CYAN);
            float xo = radius * 0.93F * cos(thOuter), yo = radius * 0.93F * sin(thOuter);
            float xi = 1.5F * cos(thInner), yi = 1.5F * sin(thInner);
            quad(GLOW, xo, yo, z - 0.04F, 0.35F * e, xo, yo, z + 0.04F, 0.35F * e, xi, yi, z + 0.04F, 0.05F * e, xi, yi,
                    z - 0.04F, 0.05F * e);
        }
    }

    // ---------------------------------------------------------------------------------------------- 3: lattice

    private void lattice() {
        int lines = 12;
        float step = 0.5F;
        float travel = frac(time * (0.0035F + 0.007F * power)) * (length() + 12F) - 6F;
        // the cells: a faint fill that flickers cell by cell
        int cells = Math.round(length());
        colour(BLUE);
        for (int k = 0; k < cells; k++) {
            float z0 = start + k, z1 = z0 + 1;
            for (int i = 0; i < lines * 2; i++) {
                float h = hash(k * 131 + i * 7 + (int) (time / 7F) * 977);
                if (h > 0.34F) continue;
                float th0 = TAU * i / (lines * 2), th1 = TAU * (i + 1) / (lines * 2);
                float a = 0.05F + 0.10F * h * 3F;
                patch(SHELL, radius, th0, th1, z0, z1, a * env(z0), a * env(z0), a * env(z1), a * env(z1));
            }
        }
        // the diamond mesh: two counter-wound sets of lines
        for (int dir = -1; dir <= 1; dir += 2) {
            for (int l = 0; l < lines; l++) {
                float phase = TAU * l / lines;
                float px = 0, py = 0, pz = 0, ptheta = 0, pa = 0;
                boolean have = false;
                for (float z = start; z <= end + 0.001F; z += step) {
                    float th = phase + dir * (z - start) * 0.24F + time * 0.004F * dir;
                    float x = radius * cos(th), y = radius * sin(th);
                    float flare = pulse(z, travel) + stationFlare(z);
                    float a = (0.40F + 0.60F * flare) * env(z) * (0.5F + 0.5F * power);
                    if (have) {
                        colour(BLUE, WHITE, flare * 1.1F);
                        float ox = -sin(th) * 0.045F, oy = cos(th) * 0.07F;
                        float oxp = -sin(ptheta) * 0.07F, oyp = cos(ptheta) * 0.07F;
                        quad(GLOW, px - oxp, py - oyp, pz, pa, px + oxp, py + oyp, pz, pa, x + ox, y + oy, z, a, x - ox,
                                y - oy, z, a);
                    }
                    px = x;
                    py = y;
                    pz = z;
                    ptheta = th;
                    pa = a;
                    have = true;
                }
            }
        }
        // the rings: one a block, the hall's stations thick and bright
        for (float z = start + 0.5F; z < end; z += 1F) {
            float flare = pulse(z, travel);
            float a = (0.22F + 0.55F * flare) * env(z) * (0.5F + 0.5F * power);
            colour(BLUE, WHITE, flare);
            band(GLOW, z, 0.07F, radius, a * 2F, 56);
        }
        for (float station : STATIONS) {
            float z = station;
            if (z < start || z > end) continue;
            float flare = 0.5F + 0.5F * sin(time * 0.1F + z) + 1.4F * pulse(z, travel);
            float a = (0.30F + 0.40F * clamp(flare)) * (0.5F + 0.5F * power);
            colour(CYAN);
            band(GLOW, z, 0.28F, radius, a * 0.6F, 56);
            colour(WHITE);
            band(GLOW, z, 0.07F, radius, a, 56);
        }
    }

    /** A soft bump of light centred on {@code at}, 1.8 blocks wide. */
    private static float pulse(float z, float at) {
        float d = (z - at) / 1.8F;
        return (float) Math.exp(-d * d);
    }

    private float stationFlare(float z) {
        float best = 0;
        for (float station : STATIONS) {
            float d = Math.abs(z - station);
            if (d < 0.9F) best = Math.max(best, 0.18F * (1F - d / 0.9F));
        }
        return best;
    }
}
