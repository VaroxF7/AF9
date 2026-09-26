package com.af9.core.client.render;

import com.af9.core.AF9Core;
import com.af9.core.common.AF9Sounds;
import com.af9.core.machine.ILightRingMachine;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine;
import com.gregtechceu.gtceu.api.pattern.util.RelativeDirection;
import com.gregtechceu.gtceu.client.renderer.GTRenderTypes;
import com.gregtechceu.gtceu.client.renderer.machine.DynamicRender;
import com.gregtechceu.gtceu.client.renderer.machine.DynamicRenderManager;
import com.gregtechceu.gtceu.client.renderer.machine.DynamicRenderType;
import com.gregtechceu.gtceu.client.util.BloomUtils;
import com.gregtechceu.gtceu.client.util.RenderBufferHelper;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import static net.minecraft.util.FastColor.ARGB32.*;

/**
 * GT's fusion ring for any {@link ILightRingMachine}: a glowing torus that lights up while the machine works, pulses
 * between the machine's colour and white, and fades out when it stops (with Shimmer installed it blooms, like GT's).
 * A white-hot core runs inside the tube; around it a wide glow in the machine's colour, four layers fading out, and
 * small sparks and glints along the ring; optionally lightning ({@code arcs}): bolts that leap from the ring into its
 * middle and branch into arms, and short darts crackling off it; the machine's sounds
 * ({@link ILightRingMachine#ringPulseSound()}, by default a low throb
 * ({@link AF9Sounds#ORBITAL_PULSE}), on every white flash and {@link ILightRingMachine#ringIgniteSound()}, a deep roar
 * ({@link AF9Sounds#ORBITAL_IGNITE}), when it lights up).
 * <p>
 * Placed relative to the controller: the centre {@code up} along the controller's up and {@code back} behind it, the
 * ring lying across the {@code normal} axis (a relative direction), so it turns with the controller.
 * Model side: {@link com.af9.core.machine.AF9MachineModels#workableCasingWithLightRing}.
 * <p>
 * One render serves every machine of the model: whether a ring shows, and its fade-out and colour, are kept per
 * machine, so a station only glows while it works itself.
 */
@SuppressWarnings("removal") // new ResourceLocation(ns, path) is the only constructor on 1.20.1
public class LightRingRender extends DynamicRender<ILightRingMachine, LightRingRender> {

    // spotless:off
    public static final Codec<LightRingRender> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.FLOAT.fieldOf("up").forGetter(render -> render.up),
            Codec.FLOAT.fieldOf("back").forGetter(render -> render.back),
            Codec.FLOAT.fieldOf("radius").forGetter(render -> render.radius),
            Codec.FLOAT.fieldOf("thickness").forGetter(render -> render.thickness),
            RelativeDirection.CODEC.optionalFieldOf("normal", RelativeDirection.UP).forGetter(render -> render.normal),
            Codec.BOOL.optionalFieldOf("arcs", false).forGetter(render -> render.arcs)
    ).apply(instance, LightRingRender::new));
    // spotless:on
    public static final DynamicRenderType<ILightRingMachine, LightRingRender> TYPE = new DynamicRenderType<>(CODEC);

    /** Frames the ring takes to fade out (GT's fusion ring). */
    public static final float FADEOUT = 60;
    /** Ticks of one pulse (colour to white and back), as GT's fusion ring. */
    public static final int PULSE_TICKS = 50;
    /** Glow layers around the ring: tube radius and opacity relative to the ring's. */
    private static final float[][] HALO = { { 1.7F, 0.45F }, { 2.8F, 0.26F }, { 4.5F, 0.14F }, { 7F, 0.06F } };
    /** The white-hot core inside the tube: radius and opacity relative to the ring's. */
    private static final float CORE = 0.45F, CORE_ALPHA = 0.85F;

    private final float up;
    private final float back;
    private final float radius;
    private final float thickness;
    private final RelativeDirection normal;
    /** Lightning from the ring into its middle. */
    private final boolean arcs;

    /** Per machine: fade-out, last colour, effects already done this tick, lit last tick, last pulse (client thread). */
    private final Map<MetaMachine, Effects> effects = new WeakHashMap<>();

    public LightRingRender(float up, float back, float radius, float thickness, RelativeDirection normal,
                           boolean arcs) {
        this.up = up;
        this.back = back;
        this.radius = radius;
        this.thickness = thickness;
        this.normal = normal;
        this.arcs = arcs;
    }

    /** Typed as GT's base class, so common code that builds a model never loads this client class. */
    public static DynamicRender<?, ?> create(float up, float back, float radius, float thickness,
                                             RelativeDirection normal, boolean arcs) {
        return new LightRingRender(up, back, radius, thickness, normal, arcs);
    }

    /** Client, before the models are built (mod construction). */
    public static void register() {
        DynamicRenderManager.register(new ResourceLocation(AF9Core.MOD_ID, "light_ring"), TYPE);
    }

    @Override
    public DynamicRenderType<ILightRingMachine, LightRingRender> getType() {
        return TYPE;
    }

    @Override
    public boolean shouldRender(ILightRingMachine machine, Vec3 cameraPos) {
        Effects state = effects.get(machine.self());
        if (!machine.isRingLit() && state != null) {
            // not rendered while dark: remember it went dark, so the next ignition roars again
            state.lit = false;
        }
        return (machine.isRingLit() || fading(state)) &&
                Vec3.atCenterOf(machine.self().getPos()).closerThan(cameraPos, getViewDistance());
    }

    private static boolean fading(Effects state) {
        return state != null && state.delta > 0;
    }

    @Override
    public void render(ILightRingMachine machine, float partialTick, PoseStack poseStack, MultiBufferSource buffer,
                       int packedLight, int packedOverlay) {
        tickEffects(machine);
        Effects state = effects.computeIfAbsent(machine.self(), key -> new Effects());
        if (!machine.isRingLit() && state.delta <= 0) return;
        if (GTCEu.Mods.isShimmerLoaded()) {
            // bloom draws later: it needs its own copy of the pose
            PoseStack copy = new PoseStack();
            copy.last().pose().set(poseStack.last().pose());
            copy.last().normal().set(poseStack.last().normal());
            BloomUtils.entityBloom(source -> renderRing(machine, state, partialTick, copy,
                    source.getBuffer(GTRenderTypes.getLightRing())));
        } else {
            renderRing(machine, state, partialTick, poseStack, buffer.getBuffer(GTRenderTypes.getLightRing()));
        }
        if (arcs && machine.isRingLit() && !state.bolts.isEmpty()) {
            renderBolts(machine, state, poseStack, buffer.getBuffer(RenderType.lightning()));
        }
    }

    private void renderRing(ILightRingMachine machine, Effects state, float partialTick, PoseStack poseStack,
                            VertexConsumer consumer) {
        float alpha = 1;
        if (machine.isRingLit()) {
            state.lastColor = machine.getRingColor();
            state.delta = FADEOUT;
        } else {
            alpha = state.delta / FADEOUT;
            state.delta -= Minecraft.getInstance().getDeltaFrameTime();
        }
        int lastColor = state.lastColor;
        // pulse to white and back, like GT's fusion ring (white on every multiple of PULSE_TICKS)
        float half = PULSE_TICKS / 2F;
        float pulse = Math.abs((Math.abs(machine.self().getOffsetTimer() % PULSE_TICKS) + partialTick) - half) / half;
        float r = Mth.lerp(pulse, red(lastColor), 255) / 255f;
        float g = Mth.lerp(pulse, green(lastColor), 255) / 255f;
        float b = Mth.lerp(pulse, blue(lastColor), 255) / 255f;
        Frame frame = frame(machine.self());
        int segments = Math.max(20, Math.round(radius * 5));
        RenderBufferHelper.renderRing(poseStack, consumer, frame.x, frame.y, frame.z, radius, thickness, 10,
                segments, r, g, b, alpha, frame.axis);
        // the white-hot core inside it
        RenderBufferHelper.renderRing(poseStack, consumer, frame.x, frame.y, frame.z, radius, thickness * CORE, 8,
                segments, 1F, 1F, 1F, alpha * CORE_ALPHA, frame.axis);
        // the glow in the plain colour, breathing with the pulse
        float cr = red(lastColor) / 255f, cg = green(lastColor) / 255f, cb = blue(lastColor) / 255f;
        for (float[] layer : HALO) {
            RenderBufferHelper.renderRing(poseStack, consumer, frame.x, frame.y, frame.z, radius,
                    thickness * layer[0], 10, segments, cr, cg, cb, alpha * layer[1] * (0.6F + 0.4F * pulse),
                    frame.axis);
        }
    }

    /** Once per tick and machine: sparks and glints on the ring, the ignition roar and the throb on each flash. */
    private void tickEffects(ILightRingMachine machine) {
        MetaMachine self = machine.self();
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null || self.getLevel() != level) return;
        long tick = level.getGameTime();
        Effects state = effects.computeIfAbsent(self, key -> new Effects());
        if (state.tick == tick) return;
        state.tick = tick;
        boolean lit = machine.isRingLit();
        Frame frame = frame(self);
        BlockPos pos = self.getPos();
        double cx = pos.getX() + frame.x, cy = pos.getY() + frame.y, cz = pos.getZ() + frame.z;
        SoundEvent ignite = machine.ringIgniteSound();
        if (lit && !state.lit && ignite != null) {
            level.playLocalSound(cx, cy, cz, ignite, SoundSource.BLOCKS, 1F, 1F, false);
        }
        state.lit = lit;
        if (!lit) {
            state.bolts.clear();
            return;
        }
        long cycle = self.getOffsetTimer() / PULSE_TICKS;
        SoundEvent pulse = machine.ringPulseSound();
        if (cycle != state.cycle) {
            state.cycle = cycle;
            if (pulse != null) level.playLocalSound(cx, cy, cz, pulse, SoundSource.BLOCKS, 1F, 1F, false);
        }
        RandomSource random = level.getRandom();
        if (arcs) tickBolts(state, random, frame, level, pos);
        int color = machine.getRingColor();
        Vector3f tint = new Vector3f(red(color) / 255f, green(color) / 255f, blue(color) / 255f);
        int count = 2 + random.nextInt(3);
        for (int i = 0; i < count; i++) {
            float angle = random.nextFloat() * Mth.TWO_PI;
            float dist = radius + (random.nextFloat() - 0.5F) * thickness * 6;
            Vec3 at = frame.onRing(angle, dist, (random.nextFloat() - 0.5F) * thickness * 4);
            double x = pos.getX() + at.x, y = pos.getY() + at.y, z = pos.getZ() + at.z;
            float kind = random.nextFloat();
            ParticleOptions particle = kind < 0.55F ? ParticleTypes.ELECTRIC_SPARK :
                    kind < 0.8F ? new DustParticleOptions(tint, 0.8F) : ParticleTypes.END_ROD;
            double speed = particle == ParticleTypes.END_ROD ? 0.01 : 0.05;
            level.addParticle(particle, x, y, z, (random.nextDouble() - 0.5) * speed,
                    (random.nextDouble() - 0.5) * speed, (random.nextDouble() - 0.5) * speed);
        }
    }

    //////////////////////////////////////
    // ********** Lightning *********//
    //////////////////////////////////////

    /**
     * Once a tick: the bolts age and die; new ones leap off the ring (a big bolt every few ticks, darts more often), a
     * spark where each lands. At most 12 at a time.
     */
    private void tickBolts(Effects state, RandomSource random, Frame frame, ClientLevel level, BlockPos pos) {
        state.bolts.removeIf(bolt -> ++bolt.age >= bolt.life);
        if (state.bolts.size() >= 12) return;
        List<Bolt> born = new ArrayList<>(2);
        if (random.nextFloat() < 0.4F) {
            born.add(new Bolt(random.nextFloat() * Mth.TWO_PI, radius * (0.25F + random.nextFloat() * 0.45F),
                    (random.nextFloat() - 0.5F) * 3F, (random.nextFloat() - 0.5F) * 0.4F, random.nextLong(),
                    4 + random.nextInt(4), false));
        }
        if (random.nextFloat() < 0.5F) {
            born.add(new Bolt(random.nextFloat() * Mth.TWO_PI, 1.5F + random.nextFloat() * 3F,
                    (random.nextFloat() - 0.5F) * 1.6F, (random.nextFloat() - 0.5F) * 0.15F, random.nextLong(),
                    2, true));
        }
        for (Bolt bolt : born) {
            state.bolts.add(bolt);
            Vec3 end = bolt.end(frame, radius);
            level.addParticle(ParticleTypes.ELECTRIC_SPARK, pos.getX() + end.x, pos.getY() + end.y,
                    pos.getZ() + end.z, (random.nextDouble() - 0.5) * 0.2, (random.nextDouble() - 0.5) * 0.2,
                    (random.nextDouble() - 0.5) * 0.2);
        }
    }

    /**
     * The bolts, re-forked every other tick so they crackle: a jagged path from the ring towards its middle (midpoint
     * displacement) with a few arms off it, each segment a camera-facing ribbon, a glow in the machine's colour under a
     * white-hot core, fading with the bolt's age. Vanilla's lightning render type (additive).
     */
    private void renderBolts(ILightRingMachine machine, Effects state, PoseStack poseStack, VertexConsumer consumer) {
        MetaMachine self = machine.self();
        Frame frame = frame(self);
        BlockPos pos = self.getPos();
        Vec3 eye = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition()
                .subtract(pos.getX(), pos.getY(), pos.getZ());
        Matrix4f mat = poseStack.last().pose();
        int color = machine.getRingColor();
        float cr = red(color) / 255f, cg = green(color) / 255f, cb = blue(color) / 255f;
        long tick = self.getOffsetTimer() / 2;
        for (Bolt bolt : state.bolts) {
            float fade = 1 - (float) bolt.age / bolt.life;
            RandomSource random = RandomSource.create(bolt.seed ^ tick * 0x9E3779B97F4A7C15L);
            Vec3 from = frame.onRing(bolt.angle, radius - thickness, 0);
            Vec3 to = bolt.end(frame, radius);
            List<Vec3> path = jagged(from, to, random, bolt.dart ? 3 : 5, bolt.length * 0.16F);
            float width = bolt.dart ? 0.55F : 1F;
            drawPath(consumer, mat, eye, path, width, cr, cg, cb, fade);
            if (bolt.dart) continue;
            // arms: off a point of the bolt, on towards the middle but swerving
            int arms = 1 + random.nextInt(3);
            Vec3 ahead = to.subtract(from).normalize();
            for (int i = 0; i < arms; i++) {
                Vec3 start = path.get(1 + random.nextInt(path.size() - 2));
                Vec3 swerve = new Vec3(random.nextFloat() - 0.5, random.nextFloat() - 0.5, random.nextFloat() - 0.5)
                        .normalize();
                float armLength = bolt.length * (0.2F + random.nextFloat() * 0.3F);
                Vec3 end = start.add(ahead.add(swerve).normalize().scale(armLength));
                drawPath(consumer, mat, eye, jagged(start, end, random, 3, armLength * 0.2F), width * 0.6F, cr, cg,
                        cb, fade * 0.8F);
            }
        }
    }

    /** Midpoint displacement: 2^depth segments between the ends, each level half as wild as the last. */
    private static List<Vec3> jagged(Vec3 from, Vec3 to, RandomSource random, int depth, float amplitude) {
        List<Vec3> points = new ArrayList<>(List.of(from, to));
        float amp = amplitude;
        for (int d = 0; d < depth; d++) {
            List<Vec3> next = new ArrayList<>(points.size() * 2);
            for (int i = 0; i < points.size() - 1; i++) {
                Vec3 a = points.get(i), b = points.get(i + 1);
                next.add(a);
                next.add(a.add(b).scale(0.5).add((random.nextFloat() - 0.5) * 2 * amp,
                        (random.nextFloat() - 0.5) * 2 * amp, (random.nextFloat() - 0.5) * 2 * amp));
            }
            next.add(points.get(points.size() - 1));
            points = next;
            amp *= 0.55F;
        }
        return points;
    }

    /** A path as camera-facing ribbons: a wide glow in the colour, then a thin white-hot core. */
    private static void drawPath(VertexConsumer consumer, Matrix4f mat, Vec3 eye, List<Vec3> path, float width,
                                 float r, float g, float b, float alpha) {
        for (int i = 0; i < path.size() - 1; i++) {
            Vec3 a = path.get(i), c = path.get(i + 1);
            Vec3 side = c.subtract(a).cross(eye.subtract(a));
            if (side.lengthSqr() < 1.0E-8) continue;
            side = side.normalize();
            ribbon(consumer, mat, a, c, side.scale(0.13F * width), r, g, b, 0.45F * alpha);
            ribbon(consumer, mat, a, c, side.scale(0.035F * width), 0.85F, 0.93F, 1F, 0.95F * alpha);
        }
    }

    /** One quad from a to c, side wide each way, both windings (the lightning render type culls). */
    private static void ribbon(VertexConsumer consumer, Matrix4f mat, Vec3 a, Vec3 c, Vec3 side, float r, float g,
                               float b, float alpha) {
        Vec3 a1 = a.add(side), a2 = a.subtract(side), c1 = c.add(side), c2 = c.subtract(side);
        for (Vec3 v : new Vec3[] { a1, a2, c2, c1, c1, c2, a2, a1 }) {
            consumer.vertex(mat, (float) v.x, (float) v.y, (float) v.z).color(r, g, b, alpha).endVertex();
        }
    }

    /** A bolt: where it leaves the ring, how far in it reaches, how far off the ring's plane and sideways it ends. */
    private static final class Bolt {

        final float angle, length, lift, twist;
        final long seed;
        final int life;
        final boolean dart;
        int age;

        Bolt(float angle, float length, float lift, float twist, long seed, int life, boolean dart) {
            this.angle = angle;
            this.length = length;
            this.lift = lift;
            this.twist = twist;
            this.seed = seed;
            this.life = life;
            this.dart = dart;
        }

        Vec3 end(Frame frame, float radius) {
            return frame.onRing(angle + twist, radius - length, lift);
        }
    }

    /** The ring's centre (block-local) and axis for the machine's current facing. */
    private Frame frame(MetaMachine self) {
        boolean flipped = self instanceof MultiblockControllerMachine controller && controller.isFlipped();
        Direction upDir = RelativeDirection.UP.getRelative(self.getFrontFacing(), self.getUpwardsFacing(), flipped);
        Direction backDir = RelativeDirection.BACK.getRelative(self.getFrontFacing(), self.getUpwardsFacing(),
                flipped);
        Direction normalDir = normal.getRelative(self.getFrontFacing(), self.getUpwardsFacing(), flipped);
        float x = 0.5F + upDir.getStepX() * up + backDir.getStepX() * back;
        float y = 0.5F + upDir.getStepY() * up + backDir.getStepY() * back;
        float z = 0.5F + upDir.getStepZ() * up + backDir.getStepZ() * back;
        return new Frame(x, y, z, normalDir.getAxis());
    }

    private record Frame(float x, float y, float z, Direction.Axis axis) {

        /** A point of the ring at the angle, the distance from the centre and an offset along the axis. */
        Vec3 onRing(float angle, float dist, float along) {
            float sin = Mth.sin(angle) * dist, cos = Mth.cos(angle) * dist;
            // the same parametrisation as GT's RenderBufferHelper.renderRing
            return switch (axis) {
                case Y -> new Vec3(x + sin, y + along, z + cos);
                case X -> new Vec3(x + along, y + sin, z + cos);
                case Z -> new Vec3(x + cos, y + sin, z + along);
            };
        }
    }

    private static final class Effects {

        /** Fade-out left, frames ({@link #FADEOUT} while lit). */
        float delta;
        int lastColor = -1;
        long tick = Long.MIN_VALUE;
        boolean lit;
        long cycle = Long.MIN_VALUE;
        final List<Bolt> bolts = new ArrayList<>();
    }

    @Override
    public boolean shouldRenderOffScreen(ILightRingMachine machine) {
        return machine.isRingLit() || fading(effects.get(machine.self()));
    }

    @Override
    public int getViewDistance() {
        return 96;
    }

    @Override
    public AABB getRenderBoundingBox(ILightRingMachine machine) {
        BlockPos pos = machine.self().getPos();
        float reach = Math.abs(up) + Math.abs(back) + radius + thickness * HALO[HALO.length - 1][0] + 1;
        return new AABB(pos).inflate(reach);
    }
}
