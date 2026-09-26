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
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import org.joml.Vector3f;

import java.util.Map;
import java.util.WeakHashMap;

import static net.minecraft.util.FastColor.ARGB32.*;

/**
 * GT's fusion ring for any {@link ILightRingMachine}: a glowing torus that lights up while the machine works, pulses
 * between the machine's colour and white, and fades out when it stops (with Shimmer installed it blooms, like GT's).
 * Around it: a soft halo in the machine's colour and small sparks and glints along the ring; a low throb
 * ({@link AF9Sounds#ORBITAL_PULSE}) on every white flash and a deep roar ({@link AF9Sounds#ORBITAL_IGNITE}) when it
 * lights up.
 * <p>
 * Placed relative to the controller: the centre {@code up} along the controller's up and {@code back} behind it, the
 * ring lying across the {@code normal} axis (a relative direction), so it turns with the controller.
 * Model side: {@link com.af9.core.machine.AF9MachineModels#workableCasingWithLightRing}.
 */
@SuppressWarnings("removal") // new ResourceLocation(ns, path) is the only constructor on 1.20.1
public class LightRingRender extends DynamicRender<ILightRingMachine, LightRingRender> {

    // spotless:off
    public static final Codec<LightRingRender> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.FLOAT.fieldOf("up").forGetter(render -> render.up),
            Codec.FLOAT.fieldOf("back").forGetter(render -> render.back),
            Codec.FLOAT.fieldOf("radius").forGetter(render -> render.radius),
            Codec.FLOAT.fieldOf("thickness").forGetter(render -> render.thickness),
            RelativeDirection.CODEC.optionalFieldOf("normal", RelativeDirection.UP).forGetter(render -> render.normal)
    ).apply(instance, LightRingRender::new));
    // spotless:on
    public static final DynamicRenderType<ILightRingMachine, LightRingRender> TYPE = new DynamicRenderType<>(CODEC);

    /** Frames the ring takes to fade out (GT's fusion ring). */
    public static final float FADEOUT = 60;
    /** Ticks of one pulse (colour to white and back), as GT's fusion ring. */
    public static final int PULSE_TICKS = 50;
    /** Halo layers around the ring: tube radius and opacity relative to the ring's. */
    private static final float[][] HALO = { { 2.5F, 0.3F }, { 5F, 0.1F } };

    private final float up;
    private final float back;
    private final float radius;
    private final float thickness;
    private final RelativeDirection normal;

    private float delta;
    private int lastColor = -1;
    /** Per machine: effects already done this tick, lit last tick, last pulse (client thread only). */
    private final Map<MetaMachine, Effects> effects = new WeakHashMap<>();

    public LightRingRender(float up, float back, float radius, float thickness, RelativeDirection normal) {
        this.up = up;
        this.back = back;
        this.radius = radius;
        this.thickness = thickness;
        this.normal = normal;
    }

    /** Typed as GT's base class, so common code that builds a model never loads this client class. */
    public static DynamicRender<?, ?> create(float up, float back, float radius, float thickness,
                                             RelativeDirection normal) {
        return new LightRingRender(up, back, radius, thickness, normal);
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
        if (!machine.isRingLit()) {
            // not rendered while dark: remember it went dark, so the next ignition roars again
            Effects state = effects.get(machine.self());
            if (state != null) state.lit = false;
        }
        return (machine.isRingLit() || delta > 0) &&
                Vec3.atCenterOf(machine.self().getPos()).closerThan(cameraPos, getViewDistance());
    }

    @Override
    public void render(ILightRingMachine machine, float partialTick, PoseStack poseStack, MultiBufferSource buffer,
                       int packedLight, int packedOverlay) {
        tickEffects(machine);
        if (!machine.isRingLit() && delta <= 0) return;
        if (GTCEu.Mods.isShimmerLoaded()) {
            // bloom draws later: it needs its own copy of the pose
            PoseStack copy = new PoseStack();
            copy.last().pose().set(poseStack.last().pose());
            copy.last().normal().set(poseStack.last().normal());
            BloomUtils.entityBloom(source -> renderRing(machine, partialTick, copy,
                    source.getBuffer(GTRenderTypes.getLightRing())));
        } else {
            renderRing(machine, partialTick, poseStack, buffer.getBuffer(GTRenderTypes.getLightRing()));
        }
    }

    private void renderRing(ILightRingMachine machine, float partialTick, PoseStack poseStack,
                            VertexConsumer consumer) {
        float alpha = 1;
        if (machine.isRingLit()) {
            lastColor = machine.getRingColor();
            delta = FADEOUT;
        } else {
            alpha = delta / FADEOUT;
            delta -= Minecraft.getInstance().getDeltaFrameTime();
        }
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
        // the halo in the plain colour, breathing with the pulse
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
        if (lit && !state.lit) {
            level.playLocalSound(cx, cy, cz, AF9Sounds.ORBITAL_IGNITE, SoundSource.BLOCKS, 1F, 1F, false);
        }
        state.lit = lit;
        if (!lit) return;
        long cycle = self.getOffsetTimer() / PULSE_TICKS;
        if (cycle != state.cycle) {
            state.cycle = cycle;
            level.playLocalSound(cx, cy, cz, AF9Sounds.ORBITAL_PULSE, SoundSource.BLOCKS, 1F, 1F, false);
        }
        RandomSource random = level.getRandom();
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

        long tick = Long.MIN_VALUE;
        boolean lit;
        long cycle = Long.MIN_VALUE;
    }

    @Override
    public boolean shouldRenderOffScreen(ILightRingMachine machine) {
        return machine.isRingLit() || delta > 0;
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
