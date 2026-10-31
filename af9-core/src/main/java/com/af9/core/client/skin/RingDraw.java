package com.af9.core.client.skin;

import com.af9.core.client.render.AF9RenderTypes;
import com.af9.core.machine.OrbitalLithographyMachine;
import com.af9.core.machine.OrbitalLithographyMachine.Band;

import com.gregtechceu.gtceu.api.machine.MetaMachine;

import com.mojang.blaze3d.platform.Lighting;
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
 * The wheels of the Orbital Array Mk2's tower ({@link StructureSkin.Rings}), drawn in place of their blocks while the
 * station is formed, the way GTNH's Godforge turns its rings: each wheel is its own blocks (rim, spokes, and on the big
 * one the shield that holds the plasma field), with their own models, turning round the tower ({@link RingBlocks}; its
 * speed and direction are the band's), the middle one against the other two. The light of the tower (power lasers, the
 * plasma field, the central beam, the wafer loop, the hover lance) is {@link OrbitalFx} / {@link WaferDraw} /
 * {@link CoreDraw}.
 * <p>
 * Drawn in the pattern's own space (right / up / back from the controller): a ring lies across the back axis, its centre
 * {@code d0 + t/2} blocks back. The wheels' blocks are hidden by {@link StructureSkins}.
 */
final class RingDraw {

    /** The rings' one speed (they spin up to it when the station is powered and never speed up while it works). */
    private static final float RUN_SPEED = 0.5F;

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
        /** The wafer loop: whether it runs, and the level time (ticks) it started at. */
        boolean looping;
        float loopStart;
        final OrbitalFx.Beat beat = new OrbitalFx.Beat();
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
        boolean powered = working || !(self instanceof OrbitalLithographyMachine station) || station.isAuxPowered();
        float[] angle = advance(self, bands, working, powered, level.getGameTime() + partialTick);
        State state = STATES.get(self.getPos().asLong());
        state.origin = self.getPos();
        state.right = RelativeDirection.RIGHT.getRelative(self.getFrontFacing(), self.getUpwardsFacing(), false);
        state.up = RelativeDirection.UP.getRelative(self.getFrontFacing(), self.getUpwardsFacing(), false);
        state.back = RelativeDirection.BACK.getRelative(self.getFrontFacing(), self.getUpwardsFacing(), false);
        state.bands = bands;
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
        // how far the light is looked at from: the nearest ring's distance, for the level of detail
        Vector3f centre = new Vector3f();
        float near = Float.MAX_VALUE;
        for (Band band : bands) {
            poseStack.last().pose().transformPosition(0.5F, 0.5F, band.d0() + band.t() / 2F, centre);
            near = Math.min(near, Math.max(0F, centre.length() - band.radius()));
        }
        int lod = near < 48F ? 0 : near < 110F ? 1 : 2;
        boolean online = state.factor > 0.04F;
        // the wafer loop plays while a recipe runs, from its start, on the level's
        // clock: it does not speed up with the machine
        if (!state.looping && working) {
            state.looping = true;
            state.loopStart = time;
        } else if (state.looping && !working) {
            state.looping = false;
        }
        float cycle = state.looping ? (time - state.loopStart) % OrbitalFx.LOOP : -1F;
        // the wafer (an item, drawn with its own render types), then the light, added, after the solids (a buffer from
        // the source ends when another is asked for)
        WaferDraw.draw(poseStack, buffer, time, cycle);
        VertexConsumer fx = buffer.getBuffer(AF9RenderTypes.LIGHTNING);
        if (online) {
            Matrix4f mat = poseStack.last().pose();
            OrbitalFx.draw(fx, mat, state.beat, time, cycle, Math.min(1F, state.factor / RUN_SPEED), state.power, lod);
            CoreDraw.draw(fx, mat, time, state.power, lod);
        }
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
}
