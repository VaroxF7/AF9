package com.af9.core.client.skin;

import com.af9.core.client.render.AF9RenderTypes;
import com.af9.core.machine.ILightRingMachine;
import com.af9.core.machine.OrbitalLithographyMachine;
import com.af9.core.machine.OrbitalLithographyMachine.Band;

import com.gregtechceu.gtceu.api.machine.MetaMachine;

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
                     PoseStack poseStack, MultiBufferSource buffer, int overlay) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return;
        List<Band> bands = rings.bands();
        boolean powered = !(self instanceof OrbitalLithographyMachine station) || station.isFieldActive();
        float[] angle = advance(self, bands, working, powered, level.getGameTime() + partialTick);
        State state = STATES.get(self.getPos().asLong());
        state.origin = self.getPos();
        state.right = RelativeDirection.RIGHT.getRelative(self.getFrontFacing(), self.getUpwardsFacing(), false);
        state.up = RelativeDirection.UP.getRelative(self.getFrontFacing(), self.getUpwardsFacing(), false);
        state.back = RelativeDirection.BACK.getRelative(self.getFrontFacing(), self.getUpwardsFacing(), false);
        state.bands = bands;
        int color = working && self instanceof ILightRingMachine ring ? ring.getRingColor() : IDLE_COLOR;
        float time = level.getGameTime() + partialTick;

        // the rings: their own blocks, drawn with their own models turning about the tower (RingBlocks)
        VertexConsumer blocks = buffer.getBuffer(RenderType.entityCutoutNoCull(TextureAtlas.LOCATION_BLOCKS));
        for (int i = 0; i < bands.size(); i++) {
            Band band = bands.get(i);
            poseStack.pushPose();
            poseStack.translate(0.5, 0.5, band.d0() + band.t() / 2F);
            poseStack.mulPose(Axis.ZP.rotation(angle[i]));
            RingBlocks.draw(self.getPos().asLong(), i, blocks, poseStack.last());
            poseStack.popPose();
        }
        // the plasma beams, added light, after the solids (a buffer from the source ends when another is asked for)
        VertexConsumer beams = buffer.getBuffer(AF9RenderTypes.LIGHTNING);
        boolean online = state.factor > 0.04F;
        for (Band band : online ? bands : List.<Band>of()) {
            Matrix4f mat = poseStack.last().pose();
            for (int side = 0; side < 4; side++) {
                beam(beams, mat, band, rings.towerHalf(), side, color, working, time);
            }
        }
        // the energy core in the tower's hollow
        if (online) CoreDraw.draw(beams, poseStack.last().pose(), color, state.phase, state.power);
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
            float wave = 0.5F + 0.5F * Mth.sin(i * 0.9F - time * 0.3F + side * 1.7F);
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
