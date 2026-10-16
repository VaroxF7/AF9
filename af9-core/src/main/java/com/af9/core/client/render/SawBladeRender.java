package com.af9.core.client.render;

import com.af9.core.AF9Core;
import com.af9.core.machine.ISawMachine;

import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine;
import com.gregtechceu.gtceu.api.pattern.util.RelativeDirection;
import com.gregtechceu.gtceu.client.renderer.machine.DynamicRender;
import com.gregtechceu.gtceu.client.renderer.machine.DynamicRenderManager;
import com.gregtechceu.gtceu.client.renderer.machine.DynamicRenderType;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The Industrial Cutting Machine's saw blade, ported from GTNH's {@code MTEIndustrialCuttingMachine.renderTESR}
 * (GT5-Unofficial, LGPL-3.0; see CREDITS-textures.md): one flat textured square standing in the chamber behind the front
 * window, turning about the controller's front-back axis. The blade textures ({@code cutter_t1..t4}, 48 x 192) hold
 * four frames of motion blur; the frame follows the spin-up, exactly as in GTNH (still, then three steps of blur).
 * GTNH clips the blade with a GL clip plane half a block under its centre so it does not poke through the floor;
 * there is none in 1.20, so the rotated square is clipped there by hand. Speed, spin-up (30 ticks) and the
 * "finish on a quarter turn" behaviour follow the original, driven by frame time instead of client ticks. Drawn in
 * controller-relative blocks, only while the structure is formed; one render object serves every cutter, so the state
 * is kept per controller position (capped). Model side:
 * {@link com.af9.core.machine.AF9MachineModels#workableCasingWithSawBlade}.
 */
@SuppressWarnings("removal") // new ResourceLocation(ns, path) is the only constructor on 1.20.1
public class SawBladeRender extends DynamicRender<ISawMachine, SawBladeRender> {

    // spotless:off
    public static final Codec<SawBladeRender> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.FLOAT.fieldOf("back").forGetter(render -> render.back),
            Codec.FLOAT.fieldOf("side").forGetter(render -> render.side),
            Codec.FLOAT.fieldOf("up").forGetter(render -> render.up),
            Codec.FLOAT.fieldOf("size").forGetter(render -> render.size)
    ).apply(instance, SawBladeRender::new));
    // spotless:on
    public static final DynamicRenderType<ISawMachine, SawBladeRender> TYPE = new DynamicRenderType<>(CODEC);

    private static final int FULL_BRIGHT = 0xF000F0;
    /** Degrees per tick at full speed, ticks to spin up, and how far under the centre the blade is cut (blocks). */
    private static final float SPEED = 64.72132F;
    private static final float SPINUP_TICKS = 30F;
    private static final float CLIP_BELOW = 0.5F;

    private static final ResourceLocation[] TEXTURES = new ResourceLocation[4];
    static {
        for (int i = 0; i < TEXTURES.length; i++) {
            TEXTURES[i] = new ResourceLocation(AF9Core.MOD_ID, "textures/entity/cutter/cutter_t" + (i + 1) + ".png");
        }
    }

    private final float back;
    private final float side;
    private final float up;
    private final float size;
    /** Per controller position: the blade's spin state. */
    private final Map<BlockPos, Spin> spins = new HashMap<>();

    public SawBladeRender(float back, float side, float up, float size) {
        this.back = back;
        this.side = side;
        this.up = up;
        this.size = size;
    }

    /** Typed as GT's base class, so common code that builds a model never loads this client class. */
    public static DynamicRender<?, ?> create(float back, float side, float up, float size) {
        return new SawBladeRender(back, side, up, size);
    }

    /** Client, before the models are built (mod construction). */
    public static void register() {
        DynamicRenderManager.register(new ResourceLocation(AF9Core.MOD_ID, "saw_blade"), TYPE);
    }

    @Override
    public DynamicRenderType<ISawMachine, SawBladeRender> getType() {
        return TYPE;
    }

    @Override
    public int getViewDistance() {
        return 64;
    }

    private Direction dir(MetaMachine self, RelativeDirection relative) {
        boolean flipped = self instanceof MultiblockControllerMachine controller && controller.isFlipped();
        return relative.getRelative(self.getFrontFacing(), self.getUpwardsFacing(), flipped);
    }

    /** The blade's centre in the controller block's local space. */
    private Vec3 pivot(MetaMachine self) {
        Direction backDir = dir(self, RelativeDirection.BACK);
        Direction leftDir = dir(self, RelativeDirection.LEFT);
        return new Vec3(0.5 + backDir.getStepX() * back + leftDir.getStepX() * side, up,
                0.5 + backDir.getStepZ() * back + leftDir.getStepZ() * side);
    }

    private Vec3 centre(MetaMachine self) {
        Vec3 pivot = pivot(self);
        BlockPos pos = self.getPos();
        return new Vec3(pos.getX() + pivot.x, pos.getY() + pivot.y, pos.getZ() + pivot.z);
    }

    @Override
    public boolean shouldRender(ISawMachine machine, Vec3 cameraPos) {
        return machine.sawFormed() && centre(machine.self()).closerThan(cameraPos, getViewDistance());
    }

    @Override
    public boolean shouldRenderOffScreen(ISawMachine machine) {
        return false;
    }

    @Override
    public AABB getRenderBoundingBox(ISawMachine machine) {
        Vec3 c = centre(machine.self());
        double r = size * 0.75;
        return new AABB(c.x - r, c.y - r, c.z - r, c.x + r, c.y + r, c.z + r);
    }

    @Override
    public void render(ISawMachine machine, float partialTick, PoseStack poseStack, MultiBufferSource buffer,
                       int packedLight, int packedOverlay) {
        if (!machine.sawFormed()) return;
        if (IrisCompat.renderingShadowPass()) return;
        var level = Minecraft.getInstance().level;
        if (level == null) return;
        MetaMachine self = machine.self();
        BlockPos key = self.getPos().immutable();

        double now = level.getGameTime() + partialTick;
        if (spins.size() > 64) spins.clear();
        Spin spin = spins.computeIfAbsent(key, k -> new Spin(now));
        float dt = (float) Mth.clamp(now - spin.lastTime, 0, 10);
        spin.lastTime = now;
        spin.advance(machine.sawSpinning(), dt);

        int blur = spin.blur();
        Vec3 pivot = pivot(self);
        Direction backDir = dir(self, RelativeDirection.BACK);
        VertexConsumer vc = buffer
                .getBuffer(RenderType.entityTranslucent(TEXTURES[Mth.clamp(machine.sawTier(), 0, TEXTURES.length - 1)]));

        poseStack.pushPose();
        poseStack.translate(pivot.x, pivot.y, pivot.z);
        // local x along the controller's left, y up, z along its back: the blade lies in the xy plane
        Direction left = dir(self, RelativeDirection.LEFT);
        poseStack.mulPoseMatrix(basis(left, backDir));
        PoseStack.Pose pose = poseStack.last();
        drawBlade(vc, pose.pose(), pose.normal(), spin.angle, blur);
        poseStack.popPose();
    }

    /** The frame whose x axis is {@code left}, y is up and z is {@code back} (a right-handed turn about y, or a mirror). */
    private static Matrix4f basis(Direction left, Direction back) {
        return new Matrix4f(
                left.getStepX(), 0, left.getStepZ(), 0,
                0, 1, 0, 0,
                back.getStepX(), 0, back.getStepZ(), 0,
                0, 0, 0, 1);
    }

    /**
     * The blade: a {@code size} square turned by {@code angle} degrees, its four-frame texture at the blur step,
     * cut off {@link #CLIP_BELOW} under the centre. One quad, no culling (so both sides show); full bright, like GTNH.
     */
    private void drawBlade(VertexConsumer vc, Matrix4f m, Matrix3f n, float angle, int blur) {
        float half = size / 2F;
        float rad = (float) Math.toRadians(-angle);
        float cos = Mth.cos(rad), sin = Mth.sin(rad);
        float vMin = blur / 4F, vMax = (blur + 1) / 4F;
        // corners (local x, y) with their uv: left/bottom first, as GTNH's quad
        float[][] corners = { { -half, -half, 0F, vMax }, { -half, half, 0F, vMin }, { half, half, 1F, vMin },
                { half, -half, 1F, vMax } };
        List<float[]> poly = new ArrayList<>(8);
        for (float[] c : corners) {
            poly.add(new float[] { c[0] * cos - c[1] * sin, c[0] * sin + c[1] * cos, c[2], c[3] });
        }
        // Sutherland-Hodgman against y >= -CLIP_BELOW
        List<float[]> clipped = new ArrayList<>(8);
        for (int i = 0; i < poly.size(); i++) {
            float[] a = poly.get(i), b = poly.get((i + 1) % poly.size());
            boolean aIn = a[1] >= -CLIP_BELOW, bIn = b[1] >= -CLIP_BELOW;
            if (aIn) clipped.add(a);
            if (aIn != bIn) {
                float t = (-CLIP_BELOW - a[1]) / (b[1] - a[1]);
                clipped.add(new float[] { Mth.lerp(t, a[0], b[0]), -CLIP_BELOW, Mth.lerp(t, a[2], b[2]),
                        Mth.lerp(t, a[3], b[3]) });
            }
        }
        // a fan of quads: first vertex, then each following pair (degenerate when only a triangle is left)
        for (int i = 1; i + 1 < clipped.size(); i += 2) {
            float[] p0 = clipped.get(0), p1 = clipped.get(i), p2 = clipped.get(i + 1);
            float[] p3 = i + 2 < clipped.size() ? clipped.get(i + 2) : p2;
            for (float[] p : new float[][] { p0, p1, p2, p3 }) {
                vc.vertex(m, p[0], p[1], 0F).color(1F, 1F, 1F, 1F).uv(p[2], p[3])
                        .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(FULL_BRIGHT).normal(n, 0F, 0F, 1F).endVertex();
            }
        }
    }

    /** One blade's state: its angle (degrees), how far it has spun up (0 to {@link #SPINUP_TICKS} ticks) and the last tick. */
    private static final class Spin {
        float angle;
        float active;
        boolean wasWorking;
        /** Extra degrees per tick while spinning down, so the blade ends on a quarter turn (GTNH's trick). */
        float offset;
        double lastTime;

        Spin(double now) {
            this.lastTime = now;
        }

        void advance(boolean working, float dt) {
            if (working != wasWorking) {
                wasWorking = working;
                offset = 0F;
                if (!working && active > 0F) {
                    // where the coast-down would leave the blade, and the push that lands it on a multiple of 90
                    float finalRotation = angle;
                    for (int i = 1; i <= (int) active; i++) {
                        finalRotation += Mth.lerp(i / SPINUP_TICKS, 0F, SPEED);
                    }
                    finalRotation %= 90F;
                    offset = (90F - finalRotation) / active;
                    // can't land smoothly (not at full speed to begin with): give up
                    if (Math.abs(offset) > 5F) offset = 0F;
                }
            }
            active = Mth.clamp(active + (working ? dt : -dt), 0F, SPINUP_TICKS);
            if (active > 0F) {
                angle = (angle + (Mth.lerp(active / SPINUP_TICKS, 0F, SPEED) + offset) * dt) % 360F;
            }
        }

        /** The motion-blur frame: none standing still, three steps up to full speed. */
        int blur() {
            if (active >= SPINUP_TICKS) return 3;
            if (active > SPINUP_TICKS * 0.66F) return 2;
            if (active > SPINUP_TICKS * 0.33F) return 1;
            return 0;
        }
    }
}
