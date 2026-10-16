package com.af9.core.client.skin;

import com.af9.core.client.render.AF9RenderTypes;
import com.af9.core.machine.ILightRingMachine;
import com.af9.core.machine.OrbitalLithographyMachine.Band;

import com.gregtechceu.gtceu.api.machine.MetaMachine;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import org.joml.Matrix4f;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The rings of the Orbital Array Mk2's tower ({@link StructureSkin.Rings}), drawn in place of their blocks while the
 * station is formed, the way GTNH's Godforge turns its rings: each ring is a band of white panels with raised modules and
 * lit windows that turns round the tower (its speed and direction are the band's), the middle one against the other two.
 * Plasma beams run from the tower to each ring at the four points of the compass and hold it: they glow in the machine's
 * colour while it works (and pulse towards the ring), a dim blue while it idles.
 * <p>
 * Drawn in the pattern's own space (right / up / back from the controller): a ring lies across the back axis, its centre
 * {@code d0 + t/2} blocks back. The rings' blocks are hidden by {@link StructureSkins}; the spokes' too, the beams take
 * their place.
 */
final class RingDraw {

    private static final int PANEL = 0xE9ECF0, BODY = 0x5C616B, DARK = 0x2E3138;
    private static final int FULL_BRIGHT = 0xF000F0;
    /** Blocks of ring circumference to a panel. */
    private static final float PANEL_LENGTH = 3F;
    /** Every this many panels the ring carries a module (a raised block with a lit window). */
    private static final int MODULE_EVERY = 5;
    /** The speed of a ring that idles, against one that works (the rings never stop while the station is formed). */
    private static final float IDLE_SPEED = 0.35F;
    private static final int IDLE_COLOR = 0x6FA8FF;
    /** How far the raised panels stand off the ring's body: clear of the depth buffer's resolution at a distance. */
    private static final float RAISE = 0.12F;

    /** Per controller: each ring's angle (radians), the speed factor, the time of the last frame (client thread). */
    private static final Map<Long, State> STATES = new HashMap<>();

    private RingDraw() {}

    private static final class State {

        float[] angle;
        float factor = IDLE_SPEED;
        float last = Float.NaN;
    }

    static void forget(long controller) {
        STATES.remove(controller);
    }

    static void clear() {
        STATES.clear();
    }

    static void draw(MetaMachine self, StructureSkin.Rings rings, boolean working, float partialTick,
                     PoseStack poseStack, MultiBufferSource buffer, int overlay) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return;
        List<Band> bands = rings.bands();
        float[] angle = advance(self, bands, working, level.getGameTime() + partialTick);
        int color = working && self instanceof ILightRingMachine ring ? ring.getRingColor() : IDLE_COLOR;
        float time = level.getGameTime() + partialTick;

        // the rings: panels and modules, entity shader, lit by the normals (the sky of the orbit is the light)
        int light = LightTexture.pack(10, 15);
        VertexConsumer solid = buffer.getBuffer(RenderType.entityCutoutNoCull(AF9RenderTypes.WHITE));
        for (int i = 0; i < bands.size(); i++) {
            Band band = bands.get(i);
            poseStack.pushPose();
            poseStack.translate(0.5, 0.5, band.d0() + band.t() / 2F);
            poseStack.mulPose(Axis.ZP.rotation(angle[i]));
            ring(solid, poseStack.last(), overlay, light, band, color);
            poseStack.popPose();
        }
        // the plasma beams, added light, after the solids (a buffer from the source ends when another is asked for)
        VertexConsumer beams = buffer.getBuffer(AF9RenderTypes.LIGHTNING);
        for (Band band : bands) {
            Matrix4f mat = poseStack.last().pose();
            for (int side = 0; side < 4; side++) {
                beam(beams, mat, band, rings.towerHalf(), side, color, working, time);
            }
        }
    }

    /** The rings' angles now: each keeps turning from where it was, faster while the machine works. */
    private static float[] advance(MetaMachine self, List<Band> bands, boolean working, float time) {
        State state = STATES.computeIfAbsent(self.getPos().asLong(), key -> new State());
        if (state.angle == null) state.angle = new float[bands.size()];
        float dt = Float.isNaN(state.last) ? 0 : Math.max(0, Math.min(time - state.last, 20));
        state.last = time;
        // the speed eases between idling and working
        float target = working ? 1F : IDLE_SPEED;
        state.factor += (target - state.factor) * Math.min(1F, dt * 0.03F);
        for (int i = 0; i < state.angle.length && i < bands.size(); i++) {
            state.angle[i] = (state.angle[i] + dt * bands.get(i).spin() * state.factor) % Mth.TWO_PI;
        }
        return state.angle;
    }

    //////////////////////////////////////
    // ************* Rings ************//
    //////////////////////////////////////

    /** One ring in its own frame: centre at the origin, lying in the x/y plane, z along the tower's axis. */
    private static void ring(VertexConsumer c, PoseStack.Pose pose, int overlay, int light, Band band, int color) {
        float inner = band.radius() - band.width() - 0.5F, outer = band.radius() + 0.5F, half = band.t() / 2F;
        int segments = Math.max(24, Math.round(Mth.TWO_PI * outer / PANEL_LENGTH));
        float step = Mth.TWO_PI / segments;
        for (int k = 0; k < segments; k++) {
            float a0 = k * step, a1 = a0 + step;
            // the body, dark inside and on the underside, grey outside
            sector(c, pose, overlay, light, a0, a1, inner, outer, -half, half, BODY, DARK, PANEL, BODY, light);
            // the raised white panels: on the outer face, and across the top. Every face of one lies a clear 0.12 off
            // the body's (and none of them is coplanar with it), or the depth buffer shimmers between the two.
            float gap = step * 0.06F;
            sector(c, pose, overlay, light, a0 + gap, a1 - gap, inner + 0.4F, outer + RAISE, -half + 0.3F,
                    half - 0.3F, PANEL, PANEL, PANEL, PANEL, light);
            sector(c, pose, overlay, light, a0 + gap, a1 - gap, inner + 0.4F, outer - 0.4F, -half + 0.3F, half + RAISE,
                    PANEL, PANEL, PANEL, PANEL, light);
            if (k % MODULE_EVERY == 0) {
                // a module: a raised block with a lit window on its outer face
                float m0 = a0 + step * 0.2F, m1 = a1 - step * 0.2F;
                sector(c, pose, overlay, light, m0, m1, outer - 0.5F, outer + 1.3F, -half - 0.7F, half + 0.7F, BODY,
                        DARK, PANEL, DARK, light);
                sector(c, pose, overlay, FULL_BRIGHT, m0 + step * 0.06F, m1 - step * 0.06F, outer + 1.0F,
                        outer + 1.42F, -half - 0.3F, half + 0.3F, color, color, color, color, FULL_BRIGHT);
            }
        }
    }

    /**
     * A block of a ring: the sector from {@code a0} to {@code a1} between the radii and between the heights {@code z0}
     * and {@code z1}, each face a flat quad (the outer, the inner, the top at z1, the bottom at z0, the two ends).
     */
    private static void sector(VertexConsumer c, PoseStack.Pose pose, int overlay, int light, float a0, float a1,
                               float r0, float r1, float z0, float z1, int outerRgb, int innerRgb, int topRgb,
                               int bottomRgb, int outerLight) {
        float s0 = Mth.sin(a0), c0 = Mth.cos(a0), s1 = Mth.sin(a1), c1 = Mth.cos(a1);
        float mid = (a0 + a1) / 2, nx = Mth.cos(mid), ny = Mth.sin(mid);
        // outer and inner faces
        quad(c, pose, overlay, outerLight, r1 * c0, r1 * s0, z0, r1 * c1, r1 * s1, z0, r1 * c1, r1 * s1, z1, r1 * c0,
                r1 * s0, z1, nx, ny, 0, outerRgb);
        quad(c, pose, overlay, light, r0 * c0, r0 * s0, z0, r0 * c1, r0 * s1, z0, r0 * c1, r0 * s1, z1, r0 * c0,
                r0 * s0, z1, -nx, -ny, 0, innerRgb);
        // top and bottom
        quad(c, pose, overlay, light, r0 * c0, r0 * s0, z1, r1 * c0, r1 * s0, z1, r1 * c1, r1 * s1, z1, r0 * c1,
                r0 * s1, z1, 0, 0, 1, topRgb);
        quad(c, pose, overlay, light, r0 * c0, r0 * s0, z0, r1 * c0, r1 * s0, z0, r1 * c1, r1 * s1, z0, r0 * c1,
                r0 * s1, z0, 0, 0, -1, bottomRgb);
        // the two ends
        quad(c, pose, overlay, light, r0 * c0, r0 * s0, z0, r1 * c0, r1 * s0, z0, r1 * c0, r1 * s0, z1, r0 * c0,
                r0 * s0, z1, s0, -c0, 0, bottomRgb);
        quad(c, pose, overlay, light, r0 * c1, r0 * s1, z0, r1 * c1, r1 * s1, z0, r1 * c1, r1 * s1, z1, r0 * c1,
                r0 * s1, z1, -s1, c1, 0, bottomRgb);
    }

    private static void quad(VertexConsumer c, PoseStack.Pose pose, int overlay, int light, float x0, float y0,
                             float z0, float x1, float y1, float z1, float x2, float y2, float z2, float x3, float y3,
                             float z3, float nx, float ny, float nz, int rgb) {
        float r = (rgb >> 16 & 255) / 255F, g = (rgb >> 8 & 255) / 255F, b = (rgb & 255) / 255F;
        vertex(c, pose, overlay, light, x0, y0, z0, nx, ny, nz, r, g, b);
        vertex(c, pose, overlay, light, x1, y1, z1, nx, ny, nz, r, g, b);
        vertex(c, pose, overlay, light, x2, y2, z2, nx, ny, nz, r, g, b);
        vertex(c, pose, overlay, light, x3, y3, z3, nx, ny, nz, r, g, b);
    }

    private static void vertex(VertexConsumer c, PoseStack.Pose pose, int overlay, int light, float x, float y,
                               float z, float nx, float ny, float nz, float r, float g, float b) {
        c.vertex(pose.pose(), x, y, z).color(r, g, b, 1F).uv(0.5F, 0.5F).overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(light).normal(pose.normal(), nx, ny, nz).endVertex();
    }

    //////////////////////////////////////
    // ******** Plasma beams **********//
    //////////////////////////////////////

    /**
     * A plasma beam from the tower's face to the ring's inner edge, along the right or the up axis in the ring's plane
     * (in the pattern's space, the origin at the controller block's corner): two crossed ribbons, a glow in the colour
     * under a white-hot core, a pulse of brightness running out along it.
     */
    private static void beam(VertexConsumer c, Matrix4f mat, Band band, int towerHalf, int side, int rgb,
                             boolean working, float time) {
        float cx = 0.5F, cy = 0.5F, cz = band.d0() + band.t() / 2F;
        float dx = side == 0 ? 1 : side == 1 ? -1 : 0, dy = side == 2 ? 1 : side == 3 ? -1 : 0;
        float from = towerHalf + 0.6F, to = band.radius() - band.width() - 0.7F;
        float r = (rgb >> 16 & 255) / 255F, g = (rgb >> 8 & 255) / 255F, b = (rgb & 255) / 255F;
        int pieces = Math.max(4, Math.round((to - from) / 1.5F));
        for (int i = 0; i < pieces; i++) {
            float t0 = from + (to - from) * i / pieces, t1 = from + (to - from) * (i + 1) / pieces;
            // pulses running out along the beam (faster and brighter while the machine works)
            float wave = 0.5F + 0.5F * Mth.sin(i * 0.9F - time * (working ? 0.45F : 0.15F) + side * 1.7F);
            float alpha = (working ? 0.35F : 0.2F) + (working ? 0.55F : 0.3F) * wave;
            for (int cross = 0; cross < 2; cross++) {
                // two ribbons standing across each other, both along the beam: one wide across the ring's plane
                // (in y for a beam along x, in x for a beam along y), one wide along the tower's axis (z)
                float wx = cross == 0 && dx == 0 ? 1 : 0, wy = cross == 0 && dx != 0 ? 1 : 0, wz = cross == 1 ? 1 : 0;
                strip(c, mat, cx + dx * t0, cy + dy * t0, cz, cx + dx * t1, cy + dy * t1, cz, wx * 0.22F, wy * 0.22F,
                        wz * 0.22F, r, g, b, alpha * 0.55F);
                strip(c, mat, cx + dx * t0, cy + dy * t0, cz, cx + dx * t1, cy + dy * t1, cz, wx * 0.06F, wy * 0.06F,
                        wz * 0.06F, 0.9F, 0.95F, 1F, alpha);
            }
        }
    }

    private static void strip(VertexConsumer c, Matrix4f mat, float ax, float ay, float az, float bx, float by,
                              float bz, float wx, float wy, float wz, float r, float g, float b, float alpha) {
        c.vertex(mat, ax + wx, ay + wy, az + wz).color(r, g, b, alpha).endVertex();
        c.vertex(mat, ax - wx, ay - wy, az - wz).color(r, g, b, alpha).endVertex();
        c.vertex(mat, bx - wx, by - wy, bz - wz).color(r, g, b, alpha).endVertex();
        c.vertex(mat, bx + wx, by + wy, bz + wz).color(r, g, b, alpha).endVertex();
    }
}
