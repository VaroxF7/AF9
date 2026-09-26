package com.af9.core.client.render;

import com.af9.core.AF9Core;
import com.af9.core.machine.ILightRingMachine;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine;
import com.gregtechceu.gtceu.api.pattern.util.RelativeDirection;
import com.gregtechceu.gtceu.client.renderer.GTRenderTypes;
import com.gregtechceu.gtceu.client.renderer.machine.DynamicRender;
import com.gregtechceu.gtceu.client.renderer.machine.DynamicRenderManager;
import com.gregtechceu.gtceu.client.renderer.machine.DynamicRenderType;
import com.gregtechceu.gtceu.client.util.BloomUtils;
import com.gregtechceu.gtceu.client.util.RenderBufferHelper;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
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

import static net.minecraft.util.FastColor.ARGB32.*;

/**
 * GT's fusion ring for any {@link ILightRingMachine}: a glowing torus that lights up while the machine works, pulses
 * between the machine's colour and white, and fades out when it stops (with Shimmer installed it blooms, like GT's).
 * Placed relative to the controller ({@code up} along the controller's up, {@code back} behind it), lying across the
 * controller's up axis, so it turns with the controller.
 * <p>
 * Model side: {@link com.af9.core.machine.AF9MachineModels#workableCasingWithLightRing}.
 */
@SuppressWarnings("removal") // new ResourceLocation(ns, path) is the only constructor on 1.20.1
public class LightRingRender extends DynamicRender<ILightRingMachine, LightRingRender> {

    // spotless:off
    public static final Codec<LightRingRender> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.FLOAT.fieldOf("up").forGetter(render -> render.up),
            Codec.FLOAT.fieldOf("back").forGetter(render -> render.back),
            Codec.FLOAT.fieldOf("radius").forGetter(render -> render.radius),
            Codec.FLOAT.fieldOf("thickness").forGetter(render -> render.thickness)
    ).apply(instance, LightRingRender::new));
    // spotless:on
    public static final DynamicRenderType<ILightRingMachine, LightRingRender> TYPE = new DynamicRenderType<>(CODEC);

    /** Frames the ring takes to fade out (GT's fusion ring). */
    public static final float FADEOUT = 60;

    private final float up;
    private final float back;
    private final float radius;
    private final float thickness;

    private float delta;
    private int lastColor = -1;

    public LightRingRender(float up, float back, float radius, float thickness) {
        this.up = up;
        this.back = back;
        this.radius = radius;
        this.thickness = thickness;
    }

    /** Typed as GT's base class, so common code that builds a model never loads this client class. */
    public static DynamicRender<?, ?> create(float up, float back, float radius, float thickness) {
        return new LightRingRender(up, back, radius, thickness);
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
        return (machine.isRingLit() || delta > 0) &&
                Vec3.atCenterOf(machine.self().getPos()).closerThan(cameraPos, getViewDistance());
    }

    @Override
    public void render(ILightRingMachine machine, float partialTick, PoseStack poseStack, MultiBufferSource buffer,
                       int packedLight, int packedOverlay) {
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
        // pulse to white and back every 50 ticks, like GT's fusion ring
        float pulse = Math.abs((Math.abs(machine.self().getOffsetTimer() % 50) + partialTick) - 25) / 25;
        float r = Mth.lerp(pulse, red(lastColor), 255) / 255f;
        float g = Mth.lerp(pulse, green(lastColor), 255) / 255f;
        float b = Mth.lerp(pulse, blue(lastColor), 255) / 255f;

        var self = machine.self();
        boolean flipped = self instanceof MultiblockControllerMachine controller && controller.isFlipped();
        Direction upDir = RelativeDirection.UP.getRelative(self.getFrontFacing(), self.getUpwardsFacing(), flipped);
        Direction backDir = RelativeDirection.BACK.getRelative(self.getFrontFacing(), self.getUpwardsFacing(), flipped);
        float x = 0.5F + upDir.getStepX() * up + backDir.getStepX() * back;
        float y = 0.5F + upDir.getStepY() * up + backDir.getStepY() * back;
        float z = 0.5F + upDir.getStepZ() * up + backDir.getStepZ() * back;
        int segments = Math.max(20, Math.round(radius * 5));
        RenderBufferHelper.renderRing(poseStack, consumer, x, y, z, radius, thickness, 10, segments, r, g, b, alpha,
                upDir.getAxis());
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
        float reach = Math.abs(up) + Math.abs(back) + radius + thickness + 1;
        return new AABB(pos).inflate(reach);
    }
}
