package com.af9.core.client.skin;

import com.af9.core.client.render.AF9RenderTypes;
import com.af9.core.machine.ILightRingMachine;
import com.af9.core.machine.OrbitalLithographyMachine;
import com.af9.core.machine.OrbitalLithographyMachine.Band;

import com.gregtechceu.gtceu.api.machine.MetaMachine;

import net.minecraft.client.Lighting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.gregtechceu.gtceu.api.pattern.util.RelativeDirection;
import com.mojang.math.Axis;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The rings of the Orbital Array Mk2's tower ({@link StructureSkin.Rings}), drawn in place of their blocks while the
 * station is formed, the way GTNH's Godforge turns its rings: each ring is its own blocks, with their own models,
 * turning round the tower ({@link RingBlocks}; its speed and direction are the band's), the middle one against the other two.
 * Plasma beams run from the tower to each ring at the four points of the compass and hold it: they glow in the machine's
 * colour while it works (and pulse towards the ring), a dim blue while it idles.
 * <p>
 * Drawn in the pattern's own space (right / up / back from the controller): a ring lies across the back axis, its centre
 * {@code d0 + t/2} blocks back. The rings' blocks are hidden by {@link StructureSkins}; the spokes' too, the beams take
 * their place.
 */
final class RingDraw {

    /** The rings' one speed (they spin up to it when the station is powered and never speed up while it works). */
    private static final float RUN_SPEED = 0.5F;
    private static final int IDLE_COLOR = 0x6FA8FF;

    /** Per controller: each ring's angle (radians), the speed factor, the time of the last frame (client thread). */
    private static final Map<Long, State> STATES = new HashMap<>();

    private RingDraw() {}

    private static final class State {

        float[] angle;
        float factor = 0F;
        /** How hard the effects burn: eased from 0 (idle) to 1 (working). */
        float power;
        float last = Float.NaN;
        /** The effects' clock: ticks, run on faster while the machine works. */
        float phase;
        // for carrying the player: the controller block, the directions of the pattern's axes, the rings
        BlockPos origin;
        Direction right, up, back;
        List<Band> bands;
    }

    static void forget(long controller) {
        STATES.remove(controller);
    }

    static void clear() {
        STATES.clear();
    }

    static void draw(MetaMachine self, StructureSkin.Rings rings, boolean working, float partialTick,
                     PoseStack poseStack, MultiBufferSource buffer, int overlay, Matrix4f view) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return;
        List<Band> bands = rings.bands();
        boolean powered = working || !(self instanceof OrbitalLithographyMachine station) || (station.isFieldActive() || station.getStartupPercent() > 0);
        float[] angle = advance(self, bands, working, powered, level.getGameTime() + partialTick);
        State state = STATES.get(self.getPos().asLong());
        state.origin = self.getPos();
        state.right = RelativeDirection.RIGHT.getRelative(self.getFrontFacing(), self.getUpwardsFacing(), false);
        state.up = RelativeDirection.UP.getRelative(self.getFrontFacing(), self.getUpwardsFacing(), false);
        state.back = RelativeDirection.BACK.getRelative(self.getFrontFacing(), self.getUpwardsFacing(), false);
        state.bands = bands;
        int color = working && self instanceof ILightRingMachine ring ? ring.getRingColor() : IDLE_COLOR;
        float time = level.getGameTime() + partialTick;

        // the rings: their own blocks, drawn with their own models turning about the tower (RingBlocks). Each ring is
        // uploaded once and the GPU turns it (the level's lights are set into the ring's frame for it); under a shader
        // pack, or if that fails, the quads are written out every frame instead.
        boolean upload = RingBlocks.canUpload();
        boolean uploaded = false;
        VertexConsumer blocks = null;
        for (int i = 0; i < bands.size(); i++) {
            Band band = bands.get(i);
            poseStack.pushPose();
            poseStack.translate(0.5, 0.5, band.d0() + band.t() / 2F);
            poseStack.mulPose(Axis.ZP.rotation(angle[i]));
            boolean done = false;
            if (upload && RingBlocks.canUpload()) {
                Lighting.setupLevel(new Matrix4f(poseStack.last().pose()).invert().mul(view));
                uploaded = true;
                done = RingBlocks.drawUploaded(self.getPos().asLong(), i, poseStack.last());
            }
            if (!done) {
                if (blocks == null) blocks = buffer.getBuffer(RenderType.entityCutoutNoCull(TextureAtlas.LOCATION_BLOCKS));
                RingBlocks.draw(self.getPos().asLong(), i, blocks, poseStack.last());
            }
            poseStack.popPose();
        }
        if (uploaded) Lighting.setupLevel(view);
        // how far the beams are looked at from: the nearest ring's distance, for the level of detail
        Vector3f centre = new Vector3f();
        float near = Float.MAX_VALUE;
        for (Band band : bands) {
            poseStack.last().pose().transformPosition(0.5F, 0.5F, band.d0() + band.t() / 2F, centre);
            near = Math.min(near, Math.max(0F, centre.length() - band.radius()));
        }
        int lod = near < 48F ? 0 : near < 110F ? 1 : 2;
        // the plasma beams, added light, after the solids (a buffer from the source ends when another is asked for)
        VertexConsumer beams = buffer.getBuffer(AF9RenderTypes.LIGHTNING);
        boolean online = state.factor > 0.04F;
        for (Band band : online ? bands : List.<Band>of()) {
            Matrix4f mat = poseStack.last().pose();
            for (int side = 0; side < 4; side++) {
                beam(beams, mat, band, rings.towerHalf(), side, color, working, time, lod);
            }
        }
        // the thruster under the foot (nothing is drawn inside the tower)
        if (online) CoreDraw.draw(beams, poseStack.last().pose(), state.phase, state.power);
    }

    /** The rings' angles now: each keeps turning from where it was, still without power, spinning up when the station is switched on and powered. */
    private static float[] advance(MetaMachine self, List<Band> bands, boolean working, boolean powered, float time) {
        State state = STATES.computeIfAbsent(self.getPos().asLong(), key -> new State());
        if (state.angle == null) state.angle = new float[bands.size()];
        float dt = Float.isNaN(state.last) ? 0 : Math.max(0, Math.min(time - state.last, 20));
        state.last = time;
        // without standby power the rings stand; with it they spin up on their own to one speed, whatever the machine does
        float target = powered ? RUN_SPEED : 0F;
        state.factor += (target - state.factor) * Math.min(1F, dt * 0.012F);
        if (target == 0F && state.factor < 0.002F) state.factor = 0F;
        // the effects (beams, core, lance) burn brighter while it works, but nothing speeds up
        state.power += ((working && powered ? 1F : 0F) - state.power) * Math.min(1F, dt * 0.03F);
        state.phase += dt * state.factor;
        for (int i = 0; i < state.angle.length && i < bands.size(); i++) {
            state.angle[i] = (state.angle[i] + dt * bands.get(i).spin() * state.factor) % Mth.TWO_PI;
        }
        return state.angle;
    }

    /**
     * Every client tick: a player standing on a ring is turned with it, like a moving platform. The ring's blocks stay
     * where they are and are solid (the drawn ring is the same band), so this only moves the rider: the position (and the
     * view, when the ring turns about the vertical) is rotated about the ring's axis by the ring's step of the tick.
     */
    static void carry(Minecraft mc) {
        LocalPlayer player = mc.player;
        if (player == null || STATES.isEmpty() || !player.onGround() || player.isPassenger()
                || player.getAbilities().flying || player.isSpectator()) {
            return;
        }
        for (State st : STATES.values()) {
            if (st.bands == null || st.origin == null || st.angle == null) continue;
            Vec3 centre = Vec3.atCenterOf(st.origin);
            Vec3 rel = player.position().subtract(centre);
            Vec3 r = vec(st.right), u = vec(st.up), b = vec(st.back);
            double lx = rel.dot(r), ly = rel.dot(u), lz = rel.dot(b) + 0.5;
            double radius = Math.sqrt(lx * lx + ly * ly);
            if (Math.abs(lx) > 80 || Math.abs(ly) > 80) continue;
            for (Band band : st.bands) {
                // on the ring's top face (the level nearest the controller), within its band
                if (Math.abs(lz - band.d0()) > 0.3 || radius < band.radius() - band.width() - 0.3
                        || radius > band.radius() + 1.0) {
                    continue;
                }
                float step = band.spin() * st.factor;
                double cos = Math.cos(step), sin = Math.sin(step);
                double nx = lx * cos - ly * sin, ny = lx * sin + ly * cos;
                Vec3 move = r.scale(nx - lx).add(u.scale(ny - ly));
                player.setPos(player.getX() + move.x, player.getY() + move.y, player.getZ() + move.z);
                // about the vertical axis, the view turns with the ring (the sense follows the axes' handedness)
                if (st.back.getAxis() == Direction.Axis.Y) {
                    double hand = Math.signum(r.dot(u.cross(b)));
                    float degrees = (float) Math.toDegrees(step * hand * st.back.getStepY());
                    player.setYRot(player.getYRot() - degrees);
                    player.yRotO -= degrees;
                    player.setYHeadRot(player.getYHeadRot() - degrees);
                }
                break;
            }
        }
    }

    private static Vec3 vec(Direction d) {
        return new Vec3(d.getStepX(), d.getStepY(), d.getStepZ());
    }

    //////////////////////////////////////
    // ******** Plasma beams **********//
    //////////////////////////////////////

    /**
     * A plasma beam from the tower's face to the middle of the ring's band (so its end is inside the turning ring's
     * blocks and never shows), along the right or the up axis in the ring's plane: three ribbons round the beam's axis, each
     * brightest along its middle and fading to nothing at its edges (a soft, round glow in the colour), a white-hot thread
     * in it, and packets of light running out along it to the ring.
     */
    private static void beam(VertexConsumer c, Matrix4f mat, Band band, int towerHalf, int side, int rgb,
                             boolean working, float time, int lod) {
        float cx = 0.5F, cy = 0.5F, cz = band.d0() + band.t() / 2F;
        float dx = side == 0 ? 1 : side == 1 ? -1 : 0, dy = side == 2 ? 1 : side == 3 ? -1 : 0;
        float from = towerHalf + 0.3F, to = band.radius() - band.width() / 2F;
        float r = (rgb >> 16 & 255) / 255F, g = (rgb >> 8 & 255) / 255F, b = (rgb & 255) / 255F;
        float len = to - from;
        // across the beam: in the ring's plane, and along the tower's axis
        float px = -dy, py = dx;
        float base = working ? 0.55F : 0.35F;
        int pieces = Math.max(6 >> lod, Math.round(len / (2F * (1 << lod))));
        for (int i = 0; i < pieces; i++) {
            float s0 = i / (float) pieces, s1 = (i + 1) / (float) pieces;
            float t0 = from + len * s0, t1 = from + len * s1;
            float a0 = alongBeam(s0, len, base, working, time, side), a1 = alongBeam(s1, len, base, working, time, side);
            // a soft sheath, the colour of the ring, and a white-hot thread in it
            tube(c, mat, cx, cy, cz, dx, dy, px, py, t0, t1, 0.62F, 8 >> lod, r, g, b, a0 * 0.14F, a1 * 0.14F);
            if (lod < 2) tube(c, mat, cx, cy, cz, dx, dy, px, py, t0, t1, 0.3F, 6 >> lod, r, g, b, a0 * 0.32F, a1 * 0.32F);
            tube(c, mat, cx, cy, cz, dx, dy, px, py, t0, t1, 0.09F, 4 >> (lod / 2), 0.92F, 0.97F, 1F, Math.min(1F, a0 * 1.5F),
                    Math.min(1F, a1 * 1.5F));
        }
        // bright packets of energy running out to the ring, a short fat tube each, rising and falling
        int packets = (working ? 4 : 2) >> (lod / 2);
        float speed = working ? 0.5F : 0.25F;
        for (int k = 0; k < packets; k++) {
            float s = ((time * speed / len + k / (float) packets + side * 0.21F) % 1F);
            float tc = from + len * s, half = 1.6F;
            float t0 = Math.max(from, tc - half), t1 = Math.min(to, tc + half);
            tube(c, mat, cx, cy, cz, dx, dy, px, py, t0, tc, 0.5F, 8 >> lod, r, g, b, 0F, 0.55F);
            tube(c, mat, cx, cy, cz, dx, dy, px, py, tc, t1, 0.5F, 8 >> lod, r, g, b, 0.55F, 0F);
            if (lod < 2) {
                tube(c, mat, cx, cy, cz, dx, dy, px, py, t0, tc, 0.2F, 6 >> lod, 1F, 1F, 1F, 0F, 0.9F);
                tube(c, mat, cx, cy, cz, dx, dy, px, py, tc, t1, 0.2F, 6 >> lod, 1F, 1F, 1F, 0.9F, 0F);
            }
        }
        // a flare where the beam leaves the tower and where it lands on the ring
        float pulse = 0.7F + 0.3F * Mth.sin(time * 0.2F + side);
        flare(c, mat, cx + dx * from, cy + dy * from, cz, px, py, 0.9F * pulse, r, g, b, base);
        flare(c, mat, cx + dx * to, cy + dy * to, cz, px, py, 1.3F * pulse, r, g, b, base * 1.2F);
    }

    /** An open prism round the beam's axis between two distances along it, with an alpha at each end. */
    private static void tube(VertexConsumer c, Matrix4f mat, float cx, float cy, float cz, float dx, float dy,
                             float px, float py, float t0, float t1, float radius, int sides, float r, float g,
                             float b, float a0, float a1) {
        sides = Math.max(4, sides);
        for (int i = 0; i < sides; i++) {
            float p0 = Mth.TWO_PI * i / sides, p1 = Mth.TWO_PI * (i + 1) / sides;
            float c0 = Mth.cos(p0) * radius, s0 = Mth.sin(p0) * radius, c1 = Mth.cos(p1) * radius,
                    s1 = Mth.sin(p1) * radius;
            c.vertex(mat, cx + dx * t0 + px * c0, cy + dy * t0 + py * c0, cz + s0).color(r, g, b, a0).endVertex();
            c.vertex(mat, cx + dx * t0 + px * c1, cy + dy * t0 + py * c1, cz + s1).color(r, g, b, a0).endVertex();
            c.vertex(mat, cx + dx * t1 + px * c1, cy + dy * t1 + py * c1, cz + s1).color(r, g, b, a1).endVertex();
            c.vertex(mat, cx + dx * t1 + px * c0, cy + dy * t1 + py * c0, cz + s0).color(r, g, b, a1).endVertex();
        }
    }

    /** A glowing disc across the beam (in the plane of the across axes), bright in the middle and fading out. */
    private static void flare(VertexConsumer c, Matrix4f mat, float x, float y, float z, float px, float py,
                              float radius, float r, float g, float b, float alpha) {
        int sides = 10;
        for (int i = 0; i < sides; i++) {
            float p0 = Mth.TWO_PI * i / sides, p1 = Mth.TWO_PI * (i + 1) / sides;
            c.vertex(mat, x, y, z).color(1F, 1F, 1F, Math.min(1F, alpha)).endVertex();
            c.vertex(mat, x, y, z).color(1F, 1F, 1F, Math.min(1F, alpha)).endVertex();
            c.vertex(mat, x + px * Mth.cos(p1) * radius, y + py * Mth.cos(p1) * radius, z + Mth.sin(p1) * radius)
                    .color(r, g, b, 0F).endVertex();
            c.vertex(mat, x + px * Mth.cos(p0) * radius, y + py * Mth.cos(p0) * radius, z + Mth.sin(p0) * radius)
                    .color(r, g, b, 0F).endVertex();
        }
    }

    private static float alongBeam(float s, float len, float base, boolean working, float time, int side) {
        float packet = 0.5F + 0.5F * Mth.sin(s * len * 0.55F - time * (working ? 0.45F : 0.25F) + side * 1.7F);
        packet *= packet * packet;
        float start = Math.min(1F, s * len / 1.2F);
        return start * (base + (working ? 0.5F : 0.3F) * packet);
    }

    /**
     * A ribbon of light from a to b, {@code w} half wide along the given direction: bright along its middle, fading to
     * nothing at both edges (two quads meeting on the middle line).
     */
    private static void ribbon(VertexConsumer c, Matrix4f mat, float ax, float ay, float az, float bx, float by,
                               float bz, float wx, float wy, float wz, float r, float g, float b, float alphaA,
                               float alphaB) {
        c.vertex(mat, ax + wx, ay + wy, az + wz).color(r, g, b, 0F).endVertex();
        c.vertex(mat, ax, ay, az).color(r, g, b, alphaA).endVertex();
        c.vertex(mat, bx, by, bz).color(r, g, b, alphaB).endVertex();
        c.vertex(mat, bx + wx, by + wy, bz + wz).color(r, g, b, 0F).endVertex();
        c.vertex(mat, ax, ay, az).color(r, g, b, alphaA).endVertex();
        c.vertex(mat, ax - wx, ay - wy, az - wz).color(r, g, b, 0F).endVertex();
        c.vertex(mat, bx - wx, by - wy, bz - wz).color(r, g, b, 0F).endVertex();
        c.vertex(mat, bx, by, bz).color(r, g, b, alphaB).endVertex();
    }
}
