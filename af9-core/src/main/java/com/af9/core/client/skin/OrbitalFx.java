package com.af9.core.client.skin;

import net.minecraft.util.Mth;

import com.mojang.blaze3d.vertex.VertexConsumer;
import org.joml.Matrix4f;

/**
 * The light of the Orbital Array Mk2's tower, in the pattern's own space (right / up / back from the controller, the origin
 * the controller block's corner, so the tower's axis is at 0.5 / 0.5):
 * <ul>
 * <li><b>The power lasers.</b> Four blue plasma beams run in from the tips of the halo's arms, over the arms, into the
 * computation core round the controller; packets of light run along them and they burn brighter while the beam is
 * charged.</li>
 * <li><b>The plasma field.</b> Round the magnets (samarium and neodymium) at the middle of the spire a field of plasma
 * is held, the shield's job: a barrel of magnetic field lines twisting round the coils, a sheath, rings of plasma, a scan
 * ring that sweeps it, arcs jumping from the coils to the sheath. It is what imprints the chips, so it flares when the
 * wafer is hit.</li>
 * <li><b>The loop</b> (200 ticks, 10 seconds, a fixed clock: it does not speed up with the machine): a new wafer is built
 * out of dust in the printing chamber, the core charges, the central beam runs down the spire and hits the wafer, the
 * light spreads over it die by die, the wafer is atomized into dust and the beam is drawn back up.</li>
 * </ul>
 * Everything is added light ({@link FxGeo}); the wafer itself is an item ({@link WaferDraw}). The motions are functions of
 * the loop's clock and the level's time alone, so nothing is stored.
 */
final class OrbitalFx {

    /** The tower's axis. */
    static final float CX = 0.5F, CY = 0.5F;
    /** The lasers run over the halo's arms at this level (the arms' top face is at 1), from the tips in to the core. */
    private static final float LASER_Z = 0.5F, ARM_TIP = 14.3F, ARM_IN = 1.7F;
    /** Where the wafer floats (the middle of the red wool's block), and the chamber's deck under it. */
    static final float WAFER_Z = 49.5F, DECK_Z = 52F;
    /** The field's span along the spire, and the radius it starts from (just outside the magnets). */
    private static final float FIELD_Z0 = 20.5F, FIELD_Z1 = 41.5F, FIELD_R0 = 3.75F, FIELD_BULGE = 2.7F;

    /** The loop's length in ticks, and its beats. */
    static final int LOOP = 200;
    static final float BUILD_END = 30F, FIRE = 58F, ARRIVE = 94F, EXPOSE = 100F, ATOMIZE = 130F, DRAW_BACK = 140F,
            BEAM_END = 168F;

    // blue plasma, and the white-hot of its core
    private static final float PR = 0.2F, PG = 0.55F, PB = 1F;
    private static final float HR = 0.72F, HG = 0.9F, HB = 1F;

    private OrbitalFx() {}

    /** The loop's beats at {@code cycle} ticks (negative: no loop, the machine is not working). */
    static final class Beat {

        /** The beam is charged (0..1): the core and the lasers burn up to the shot and cool down after it. */
        float charge;
        /** The light is on the wafer (0..1). */
        float expose;
        /** The wafer is atomized: 1 at the moment and dying away. */
        float flash;
        /** The beam's ends (blocks back from the controller) and half width; no beam when the head is above the tail. */
        float head = 0F, tail = 1F, width;

        void set(float cycle) {
            charge = expose = flash = width = 0F;
            head = 0F;
            tail = 1F;
            if (cycle < 0F) return;
            charge = FxGeo.smooth((cycle - 26F) / 36F) * (1F - FxGeo.smooth((cycle - DRAW_BACK) / 24F));
            expose = FxGeo.smooth((cycle - EXPOSE) / 14F) * (1F - FxGeo.smooth((cycle - ATOMIZE) / 3F));
            flash = cycle >= ATOMIZE ? Mth.exp(-(cycle - ATOMIZE) / 9F) : 0F;
            if (cycle < FIRE || cycle >= BEAM_END) return;
            // the head runs down to the wafer, and on to the deck once the wafer is gone
            float down = FxGeo.smooth((cycle - FIRE) / (ARRIVE - FIRE));
            float stop = cycle < ATOMIZE ? WAFER_Z - 0.12F : WAFER_Z - 0.12F
                    + (DECK_Z - WAFER_Z) * FxGeo.smooth((cycle - ATOMIZE) / 4F);
            head = 0.3F + (stop - 0.3F) * (cycle < ARRIVE ? down : 1F);
            // then the tail catches up with it: the beam is drawn back
            tail = cycle < DRAW_BACK ? 0.3F : 0.3F + (head - 0.3F) * FxGeo.smooth((cycle - DRAW_BACK) / (BEAM_END - DRAW_BACK));
            float thin = 0.04F + 0.46F * FxGeo.smooth((cycle - 70F) / 40F);
            float burst = 0.6F * flash;
            width = (thin + burst) * (1F - 0.75F * FxGeo.smooth((cycle - DRAW_BACK) / (BEAM_END - DRAW_BACK)));
        }
    }

    /**
     * The tower's light.
     *
     * @param time    the level's time in ticks, with the partial tick
     * @param cycle   the loop's clock (0..{@value #LOOP}), negative while there is no loop
     * @param standby 0..1, how far the station has spun up (it burns dimly then)
     * @param power   0..1, how hard it works
     * @param lod     0 near, 1 and 2 farther: fewer pieces
     */
    static void draw(VertexConsumer c, Matrix4f m, Beat beat, float time, float cycle, float standby, float power,
                     int lod) {
        beat.set(cycle);
        // how bright the lasers and the field burn: dim on standby, more with the charge, a flare at the shot
        float level = standby * (0.32F + 0.2F * power + 0.5F * beat.charge * power) + beat.flash * power * 0.5F;
        lasers(c, m, time, level, beat, lod);
        nexus(c, m, time, level, beat);
        if (lod < 2 || standby > 0.5F) field(c, m, time, standby, power, beat, lod);
        if (cycle >= 0F) {
            centralBeam(c, m, time, power, beat, lod);
            impact(c, m, time, cycle, power, beat);
            dust(c, m, cycle, power, lod);
        }
    }

    //////////////////////////////////////
    // ********** The lasers **********//
    //////////////////////////////////////

    /** Four beams from the arm tips in to the core, a packet of light running along each. */
    private static void lasers(VertexConsumer c, Matrix4f m, float time, float level, Beat beat, int lod) {
        if (level <= 0.01F) return;
        float len = ARM_TIP - ARM_IN;
        for (int side = 0; side < 4; side++) {
            float dx = side == 0 ? 1F : side == 1 ? -1F : 0F, dy = side == 2 ? 1F : side == 3 ? -1F : 0F;
            float ax = CX + dx * ARM_TIP, ay = CY + dy * ARM_TIP, bx = CX + dx * ARM_IN, by = CY + dy * ARM_IN;
            float flicker = 1F + 0.1F * Mth.sin(time * 0.9F + side * 1.7F);
            float k = Math.min(1F, level) * flicker;
            // the body: a sheath and a white-hot thread, fading a little towards the arm's tip
            int pieces = lod == 0 ? 6 : 3;
            for (int i = 0; i < pieces; i++) {
                float s0 = i / (float) pieces, s1 = (i + 1) / (float) pieces;
                float x0 = ax + (bx - ax) * s0, y0 = ay + (by - ay) * s0, x1 = ax + (bx - ax) * s1,
                        y1 = ay + (by - ay) * s1;
                float f0 = 0.55F + 0.45F * s0, f1 = 0.55F + 0.45F * s1;
                FxGeo.line(c, m, x0, y0, LASER_Z, x1, y1, LASER_Z, 0.42F, PR, PG, PB, 0.3F * k * f0, 0.3F * k * f1);
                FxGeo.line(c, m, x0, y0, LASER_Z, x1, y1, LASER_Z, 0.12F, HR, HG, HB, 0.9F * k * f0, 0.9F * k * f1);
            }
            // packets of light running in, faster the more the beam is charged
            int packets = lod == 0 ? 3 : 2;
            float speed = 0.028F + 0.03F * beat.charge;
            for (int p = 0; p < packets; p++) {
                float s = (time * speed + p / (float) packets + side * 0.137F) % 1F;
                float s0 = Math.max(0F, s - 0.08F), s1 = Math.min(1F, s + 0.08F);
                FxGeo.line(c, m, ax + (bx - ax) * s0, ay + (by - ay) * s0, LASER_Z, ax + (bx - ax) * s, ay + (by - ay) * s,
                        LASER_Z, 0.62F, PR, PG, PB, 0F, 0.6F * k);
                FxGeo.line(c, m, ax + (bx - ax) * s, ay + (by - ay) * s, LASER_Z, ax + (bx - ax) * s1,
                        ay + (by - ay) * s1, LASER_Z, 0.62F, PR, PG, PB, 0.6F * k, 0F);
                FxGeo.glow(c, m, ax + (bx - ax) * s, ay + (by - ay) * s, LASER_Z, 0.55F, 6, PR, PG, PB, 0.32F * k);
            }
            // the emitter at the tip of the arm
            FxGeo.glow(c, m, ax, ay, LASER_Z, 0.95F + 0.15F * Mth.sin(time * 0.2F + side), 8, PR, PG, PB, 0.55F * k);
        }
    }

    /** The computation core round the controller, where the four lasers meet: a ball of light and a spike up. */
    private static void nexus(VertexConsumer c, Matrix4f m, float time, float level, Beat beat) {
        if (level <= 0.01F) return;
        float k = Math.min(1.2F, level);
        float pulse = 1F + 0.08F * Mth.sin(time * 0.35F);
        FxGeo.glow(c, m, CX, CY, 0F, (0.8F + 0.8F * beat.charge + 1.4F * beat.flash) * pulse, 10, PR, PG, PB, 0.5F * k);
        FxGeo.line(c, m, CX, CY, 0F, CX, CY, -1.5F - 4.5F * beat.charge, 0.24F, HR, HG, HB, 0.7F * k, 0F);
        float a = time * 0.06F;
        FxGeo.softRing(c, m, CX, CY, -0.2F, Mth.cos(a), Mth.sin(a), 0F, -Mth.sin(a) * 0.4F, Mth.cos(a) * 0.4F, 0.9F,
                1.25F, 0.12F, 20, PR, PG, PB, 0.55F * k);
    }

    //////////////////////////////////////
    // ********* The plasma field *****//
    //////////////////////////////////////

    /** The field's radius at {@code u} (0..1) along its span: a barrel, narrow where it meets the spire. */
    private static float fieldRadius(float u) {
        float s = Mth.sin(u * Mth.PI);
        return FIELD_R0 + FIELD_BULGE * s * Math.abs(s);
    }

    /**
     * The plasma held round the magnets: a barrel of field lines twisting about the coils, a faint sheath, rings of
     * plasma, a bright ring sweeping up and down, arcs from the coils to the sheath. The flare of the shot and the
     * beam on the wafer make it burn up.
     */
    private static void field(VertexConsumer c, Matrix4f m, float time, float standby, float power, Beat beat, int lod) {
        float burn = standby * (0.35F + 0.25F * power) + power * (0.45F * beat.expose + 0.4F * beat.charge)
                + power * 0.9F * beat.flash;
        if (burn <= 0.02F) return;
        burn = Math.min(1.6F, burn);
        float span = FIELD_Z1 - FIELD_Z0;
        // the colour: blue, going to violet and white where it is imprinting
        float hot = FxGeo.clamp01(0.5F * beat.expose + beat.flash);
        float r = PR + (0.75F - PR) * hot, g = PG + (0.5F - PG) * hot, b = 1F;

        // the sheath: a barrel in bands, drifting
        int sides = lod == 0 ? 20 : lod == 1 ? 12 : 8, slices = lod == 0 ? 8 : 5;
        for (int sl = 0; sl < slices; sl++) {
            float u0 = sl / (float) slices, u1 = (sl + 1) / (float) slices;
            float z0 = FIELD_Z0 + span * u0, z1 = FIELD_Z0 + span * u1;
            float r0 = fieldRadius(u0) + 0.15F, r1 = fieldRadius(u1) + 0.15F;
            float a0 = sheathAlpha(u0, time) * burn, a1 = sheathAlpha(u1, time) * burn;
            for (int i = 0; i < sides; i++) {
                float c0 = FxGeo.cos(i, sides), s0 = FxGeo.sin(i, sides), c1 = FxGeo.cos(i + 1, sides),
                        s1 = FxGeo.sin(i + 1, sides);
                FxGeo.vertex(c, m, CX + c0 * r0, CY + s0 * r0, z0, r, g, b, a0);
                FxGeo.vertex(c, m, CX + c1 * r0, CY + s1 * r0, z0, r, g, b, a0);
                FxGeo.vertex(c, m, CX + c1 * r1, CY + s1 * r1, z1, r, g, b, a1);
                FxGeo.vertex(c, m, CX + c0 * r1, CY + s0 * r1, z1, r, g, b, a1);
            }
        }

        // the field lines: meridians of the barrel, twisting, each with a bright packet running along it
        int lines = lod == 0 ? 10 : lod == 1 ? 7 : 5, steps = lod == 0 ? 16 : 10;
        for (int k = 0; k < lines; k++) {
            float dir = (k & 1) == 0 ? 1F : -1F;
            float base = Mth.TWO_PI * k / lines + time * 0.012F * dir;
            float packet = (time * 0.011F + k * 0.19F) % 1F;
            float px = 0F, py = 0F, pz = 0F, pa = 0F;
            for (int i = 0; i <= steps; i++) {
                float u = i / (float) steps;
                float psi = base + 2.3F * (u - 0.5F) * dir;
                float rad = fieldRadius(u) + 0.05F;
                float x = CX + Mth.cos(psi) * rad, y = CY + Mth.sin(psi) * rad, z = FIELD_Z0 + span * u;
                float d = (u - packet) * 7F;
                float a = (0.2F + 0.55F * Mth.exp(-d * d)) * Mth.sqrt(Math.max(0F, Mth.sin(u * Mth.PI))) * burn;
                if (i > 0) FxGeo.line(c, m, px, py, pz, x, y, z, 0.11F, r, g, b, pa, a);
                px = x;
                py = y;
                pz = z;
                pa = a;
            }
        }

        // rings of plasma round the barrel at three heights, and a bright one sweeping up and down
        for (int i = 0; i < 3; i++) {
            float u = 0.17F + 0.33F * i;
            float breathe = 0.75F + 0.25F * Mth.sin(time * 0.1F + i * 2.1F);
            FxGeo.softRing(c, m, CX, CY, FIELD_Z0 + span * u, 1F, 0F, 0F, 0F, 1F, 0F, fieldRadius(u) + 0.1F, 0.28F,
                    lod == 0 ? 28 : 16, r, g, b, 0.55F * burn * breathe);
        }
        float sweep = (time * 0.0145F) % 2F;
        float us = sweep < 1F ? sweep : 2F - sweep;
        FxGeo.softRing(c, m, CX, CY, FIELD_Z0 + span * us, 1F, 0F, 0F, 0F, 1F, 0F, fieldRadius(us) + 0.2F, 0.55F,
                lod == 0 ? 28 : 16, 0.8F, 0.92F, 1F, 0.7F * burn);

        // the arms of plasma from the coils out to the sheath, new ones every few ticks
        if (lod < 2) {
            int tick = (int) (time / 3F);
            int arcs = lod == 0 ? 7 : 4;
            for (int i = 0; i < arcs; i++) {
                int seed = i * 977 + tick * 131;
                float u0 = 0.1F + 0.8F * FxGeo.hash(seed, 1), psi0 = Mth.TWO_PI * FxGeo.hash(seed, 2);
                float u1 = FxGeo.clamp01(u0 + (FxGeo.hash(seed, 3) - 0.5F) * 0.5F),
                        psi1 = psi0 + (FxGeo.hash(seed, 4) - 0.5F) * 2F;
                float fade = FxGeo.hash(seed, 5) < 0.2F + 0.5F * burn ? 1F : 0F;
                if (fade == 0F) continue;
                float px = CX + Mth.cos(psi0) * 3.05F, py = CY + Mth.sin(psi0) * 3.05F, pz = FIELD_Z0 + span * u0;
                for (int j = 1; j <= 5; j++) {
                    float t = j / 5F;
                    float u = u0 + (u1 - u0) * t, psi = psi0 + (psi1 - psi0) * t;
                    float rad = 3.05F + (fieldRadius(u) - 3.05F) * Mth.sin(t * Mth.HALF_PI);
                    float x = CX + Mth.cos(psi) * rad + (FxGeo.hash(seed, 10 + j) - 0.5F) * 0.5F * Math.min(t, 1F - t) * 2F;
                    float y = CY + Mth.sin(psi) * rad + (FxGeo.hash(seed, 20 + j) - 0.5F) * 0.5F * Math.min(t, 1F - t) * 2F;
                    float z = FIELD_Z0 + span * u + (FxGeo.hash(seed, 30 + j) - 0.5F) * 0.5F * Math.min(t, 1F - t) * 2F;
                    FxGeo.line(c, m, px, py, pz, x, y, z, 0.05F, 0.85F, 0.95F, 1F, 0.8F * burn, 0.8F * burn);
                    px = x;
                    py = y;
                    pz = z;
                }
            }
        }
    }

    private static float sheathAlpha(float u, float time) {
        float band = 0.6F + 0.4F * Mth.sin(u * 17F - time * 0.09F);
        return 0.07F * band * Mth.sin(Mth.clamp(u, 0.05F, 0.95F) * Mth.PI);
    }

    //////////////////////////////////////
    // ******** The central beam ******//
    //////////////////////////////////////

    /** The beam from the core down the spire to the wafer (what shows of it is the stretch seen through the chamber's glass). */
    private static void centralBeam(VertexConsumer c, Matrix4f m, float time, float power, Beat beat, int lod) {
        if (beat.head <= beat.tail + 0.05F || beat.width <= 0.01F) return;
        float k = Math.min(1F, 0.5F + power);
        float w = beat.width;
        // the whole run is a thin line; the stretch in view (from the field down) is drawn rich
        FxGeo.line(c, m, CX, CY, beat.tail, CX, CY, beat.head, w * 0.5F + 0.1F, PR, PG, PB, 0.35F * k, 0.5F * k);
        FxGeo.line(c, m, CX, CY, beat.tail, CX, CY, beat.head, w * 0.2F + 0.04F, HR, HG, HB, 0.8F * k, 0.95F * k);
        // rings of light running down it
        float from = Math.max(beat.tail, 43F);
        if (beat.head > from) {
            int rings = lod == 0 ? 5 : 3;
            for (int i = 0; i < rings; i++) {
                float z = from + ((time * 1.3F + i * 7.3F) % 36F) * 0.28F;
                if (z > beat.head) continue;
                FxGeo.softRing(c, m, CX, CY, z, 1F, 0F, 0F, 0F, 1F, 0F, w * 1.4F + 0.35F, 0.16F, 12, PR, PG, PB,
                        0.6F * k);
            }
        }
        // the head: a bright point leading the beam while it is still running down
        if (beat.expose < 0.5F && beat.flash < 0.05F) {
            FxGeo.glow(c, m, CX, CY, beat.head, 0.5F + w, 8, HR, HG, HB, 0.7F * k);
        }
    }

    //////////////////////////////////////
    // ********* On the wafer *********//
    //////////////////////////////////////

    /**
     * The light on the wafer: spreading over it die by die while it is exposed, a flash and shock rings when it is
     * atomized, a glow on the deck where the beam ends. (The wafer is drawn by {@link WaferDraw}.)
     */
    private static void impact(VertexConsumer c, Matrix4f m, float time, float cycle, float power, Beat beat) {
        float k = Math.min(1F, 0.5F + power);
        // the new wafer: a ring sweeps across it as it is built out of dust
        if (cycle < BUILD_END + 4F) {
            float s = cycle / BUILD_END;
            float scan = -1.2F + 2.4F * FxGeo.smooth(s);
            FxGeo.line(c, m, CX - 1.15F, CY + scan, WAFER_Z - 0.16F, CX + 1.15F, CY + scan, WAFER_Z - 0.16F, 0.1F, HR, HG,
                    HB, 0.7F * FxGeo.clamp01(1F - s), 0.7F * FxGeo.clamp01(1F - s));
            float ring = FxGeo.smooth(s * 1.4F);
            FxGeo.softRing(c, m, CX, CY, WAFER_Z, 1F, 0F, 0F, 0F, 1F, 0F, 0.2F + 1.1F * ring, 0.14F, 20, PR, PG, PB,
                    0.6F * (1F - FxGeo.clamp01(s * 1.1F - 0.1F)));
        }
        // exposure: the light spreads over the wafer and the dies light up in rows
        if (beat.expose > 0.01F) {
            float spread = FxGeo.smooth((cycle - EXPOSE) / 26F);
            float rad = 1.12F * spread + 0.05F;
            float flicker = 0.85F + 0.15F * Mth.sin(time * 1.3F);
            FxGeo.disc(c, m, CX, CY, WAFER_Z - 0.16F, 1F, 0F, 0F, 0F, 1F, 0F, rad, 24, 0.85F, 0.93F, 1F,
                    0.75F * beat.expose * k * flicker, 0.12F * beat.expose);
            for (int i = 1; i <= 4; i++) {
                float rr = rad * i / 4F;
                FxGeo.softRing(c, m, CX, CY, WAFER_Z - 0.17F, 1F, 0F, 0F, 0F, 1F, 0F, rr, 0.05F, 24, PR, PG, PB,
                        0.5F * beat.expose * k);
            }
            FxGeo.glow(c, m, CX, CY, WAFER_Z - 0.3F, 0.9F + 0.6F * spread, 8, PR, PG, PB, 0.45F * beat.expose * k);
        }
        // atomized
        if (cycle >= ATOMIZE && cycle < ATOMIZE + 24F) {
            float tau = cycle - ATOMIZE;
            float a = 1F - tau / 24F;
            FxGeo.glow(c, m, CX, CY, WAFER_Z, 0.6F + 0.45F * tau, 10, HR, HG, HB, 1F * a * a);
            for (int i = 0; i < 2; i++) {
                float t2 = tau - i * 3F;
                if (t2 < 0F) continue;
                float fade = 1F - t2 / (20F - i * 3F);
                if (fade <= 0F) continue;
                FxGeo.softRing(c, m, CX, CY, WAFER_Z, 1F, 0F, 0F, 0F, 1F, 0F, 0.9F + 0.65F * t2, 0.28F + 0.02F * t2, 28,
                        i == 0 ? HR : PR, i == 0 ? HG : PG, 1F, 0.9F * fade);
            }
            // the vertical shock, up and down the beam
            FxGeo.softRing(c, m, CX, CY, WAFER_Z, 1F, 0F, 0F, 0F, 0F, 1F, 0.8F + 0.55F * tau, 0.2F, 20, PR, PG, PB,
                    0.6F * a);
            FxGeo.softRing(c, m, CX, CY, WAFER_Z, 0F, 1F, 0F, 0F, 0F, 1F, 0.8F + 0.55F * tau, 0.2F, 20, PR, PG, PB,
                    0.6F * a);
        }
        // where the beam ends on the deck after the wafer is gone
        if (beat.head > DECK_Z - 0.8F && beat.width > 0.05F) {
            FxGeo.disc(c, m, CX, CY, DECK_Z - 0.06F, 1F, 0F, 0F, 0F, 1F, 0F, 0.8F + beat.width * 2.4F, 16, PR, PG, PB,
                    0.7F * k, 0F);
        }
    }

    //////////////////////////////////////
    // ************ Dust **************//
    //////////////////////////////////////

    /** The dust: building the wafer out of it, and the wafer atomized into it. */
    private static void dust(VertexConsumer c, Matrix4f m, float cycle, float power, int lod) {
        int burst = lod == 0 ? 150 : lod == 1 ? 80 : 40;
        if (cycle >= ATOMIZE && cycle < ATOMIZE + 70F) {
            for (int i = 0; i < burst; i++) {
                float tau = cycle - ATOMIZE - FxGeo.hash(i, 1) * 3F;
                if (tau < 0F) continue;
                float life = 34F + 36F * FxGeo.hash(i, 6);
                if (tau > life) continue;
                float f = 1F - tau / life;
                float ang = Mth.TWO_PI * FxGeo.hash(i, 2), speed = 0.05F + 0.13F * FxGeo.hash(i, 3) * FxGeo.hash(i, 3);
                float raw = speed * (1F - Mth.exp(-0.07F * tau)) / 0.07F;
                // the chamber is small: the dust piles up against its walls rather than leaving it
                float rad = 2F * (float) Math.tanh(raw / 2F) + 1.0F * FxGeo.hash(i, 5) * (1F - f * f);
                float lift = (FxGeo.hash(i, 4) - 0.5F) * 1.2F * (float) Math.tanh(raw / 1.5F);
                float x = CX + Mth.cos(ang) * rad, y = CY + Mth.sin(ang) * rad;
                float z = WAFER_Z + lift + 0.0016F * tau * tau;
                float heat = FxGeo.clamp01(1F - tau / 10F);
                float size = 0.04F + 0.06F * FxGeo.hash(i, 7);
                FxGeo.mote(c, m, x, y, z, size, 0.6F + 0.4F * heat, 0.72F + 0.1F * heat - 0.25F * heat * heat,
                        1F - 0.7F * heat, (0.25F + 0.75F * f) * (0.55F + 0.45F * FxGeo.hash(i, 8)) * Math.min(1F, 0.6F + power));
            }
        }
        if (cycle < BUILD_END) {
            // dust gathering into the new wafer
            int gather = lod == 0 ? 70 : 35;
            float s = cycle / BUILD_END;
            for (int i = 0; i < gather; i++) {
                float delay = 0.45F * FxGeo.hash(i, 1);
                float t = FxGeo.clamp01((s - delay) / (1F - delay));
                float away = (1F - FxGeo.smooth(t));
                float ang = Mth.TWO_PI * FxGeo.hash(i, 2) + 2F * away;
                float rad = (0.15F + 1.0F * FxGeo.hash(i, 3)) * (1F - away) + 2.2F * away;
                float x = CX + Mth.cos(ang) * rad, y = CY + Mth.sin(ang) * rad;
                float z = WAFER_Z + (FxGeo.hash(i, 4) - 0.5F) * 1.8F * away;
                FxGeo.mote(c, m, x, y, z, 0.04F + 0.04F * FxGeo.hash(i, 5), PR + 0.4F, PG + 0.3F, 1F,
                        0.8F * (1F - 0.6F * t) * Math.min(1F, 0.6F + power));
            }
        }
    }

    /** The wafer's size as a factor of its full size at {@code cycle}: built up out of nothing, whole, then gone at the shot. */
    static float waferScale(float cycle) {
        if (cycle < 0F) return 1F;
        if (cycle < BUILD_END) {
            float t = cycle / BUILD_END;
            // grows with a little overshoot
            float s = t - 1F;
            return 1F + 2.4F * s * s * s + 1.4F * s * s;
        }
        if (cycle < ATOMIZE) return 1F;
        if (cycle < ATOMIZE + 5F) {
            float t = (cycle - ATOMIZE) / 5F;
            return 1F - t * t;
        }
        return 0F;
    }
}
