package com.af9.core.client.render;

import com.af9.core.AF9Core;
import com.af9.core.machine.ILaserEngraverMachine;

import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine;
import com.gregtechceu.gtceu.api.pattern.util.RelativeDirection;
import com.gregtechceu.gtceu.client.renderer.machine.DynamicRender;
import com.gregtechceu.gtceu.client.renderer.machine.DynamicRenderManager;
import com.gregtechceu.gtceu.client.renderer.machine.DynamicRenderType;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
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
import org.joml.Matrix4f;

/**
 * The Hyper-Intensity Laser Engraver's beam while it works: a hot column of light from the laser hatch's face down the glass
 * shaft onto the plate, where it burns a bright spot. Made of crossed quads in three layers (a wide faint glow, a narrow
 * strong one, a white core) in vanilla's additive lightning render type, full brightness.
 * <p>
 * Placed relative to the controller: the shaft is {@code back} blocks behind it, and runs from {@code bottom} to {@code top}
 * blocks above the controller block's floor (so the model turns with the controller's facing). Model side:
 * {@link com.af9.core.machine.AF9MachineModels#workableCasingWithLaserBeam}.
 */
@SuppressWarnings("removal") // new ResourceLocation(ns, path) is the only constructor on 1.20.1
public class LaserEngraverRender extends DynamicRender<ILaserEngraverMachine, LaserEngraverRender> {

    // spotless:off
    public static final Codec<LaserEngraverRender> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.FLOAT.fieldOf("back").forGetter(render -> render.back),
            Codec.FLOAT.fieldOf("bottom").forGetter(render -> render.bottom),
            Codec.FLOAT.fieldOf("top").forGetter(render -> render.top)
    ).apply(instance, LaserEngraverRender::new));
    // spotless:on
    public static final DynamicRenderType<ILaserEngraverMachine, LaserEngraverRender> TYPE = new DynamicRenderType<>(CODEC);

    /** Half-width of each layer of the beam, and its colour (red-hot to white): radius, red, green, blue, alpha. */
    private static final float[][] LAYERS = {
            { 0.34F, 1F, 0.22F, 0.10F, 0.10F },
            { 0.20F, 1F, 0.35F, 0.18F, 0.22F },
            { 0.09F, 1F, 0.62F, 0.45F, 0.55F },
            { 0.035F, 1F, 0.95F, 0.90F, 0.95F },
    };

    private final float back;
    private final float bottom;
    private final float top;

    public LaserEngraverRender(float back, float bottom, float top) {
        this.back = back;
        this.bottom = bottom;
        this.top = top;
    }

    /** Typed as GT's base class, so common code that builds a model never loads this client class. */
    public static DynamicRender<?, ?> create(float back, float bottom, float top) {
        return new LaserEngraverRender(back, bottom, top);
    }

    /** Client, before the models are built (mod construction). */
    public static void register() {
        DynamicRenderManager.register(new ResourceLocation(AF9Core.MOD_ID, "laser_engraver"), TYPE);
    }

    @Override
    public DynamicRenderType<ILaserEngraverMachine, LaserEngraverRender> getType() {
        return TYPE;
    }

    @Override
    public int getViewDistance() {
        return 96;
    }

    /** The shaft's foot, relative to the controller's position: x and z at the middle of the block behind it. */
    private float[] foot(MetaMachine self) {
        boolean flipped = self instanceof MultiblockControllerMachine controller && controller.isFlipped();
        Direction backDir = RelativeDirection.BACK.getRelative(self.getFrontFacing(), self.getUpwardsFacing(), flipped);
        return new float[] { 0.5F + backDir.getStepX() * back, 0.5F + backDir.getStepZ() * back };
    }

    @Override
    public boolean shouldRender(ILaserEngraverMachine machine, Vec3 cameraPos) {
        return machine.beamOn() && Vec3.atCenterOf(machine.self().getPos()).closerThan(cameraPos, getViewDistance());
    }

    @Override
    public boolean shouldRenderOffScreen(ILaserEngraverMachine machine) {
        return true;
    }

    /** The shaft and its glow, round the controller. */
    @Override
    public AABB getRenderBoundingBox(ILaserEngraverMachine machine) {
        BlockPos pos = machine.self().getPos();
        return new AABB(pos).inflate(back + 1, top + 1, back + 1);
    }

    @Override
    public void render(ILaserEngraverMachine machine, float partialTick, PoseStack poseStack, MultiBufferSource buffer,
                       int packedLight, int packedOverlay) {
        if (!machine.beamOn()) return;
        var level = Minecraft.getInstance().level;
        if (level == null) return;
        float[] foot = foot(machine.self());
        // a slow flicker, not a blink
        float time = level.getGameTime() + partialTick;
        float flicker = 0.88F + 0.12F * Mth.sin(time * 0.9F);

        poseStack.pushPose();
        poseStack.translate(foot[0], 0, foot[1]);
        Matrix4f pose = poseStack.last().pose();
        VertexConsumer vc = buffer.getBuffer(RenderType.lightning());
        for (float[] layer : LAYERS) {
            float r = layer[0], alpha = layer[4] * flicker;
            // two planes crossing along the shaft
            quad(vc, pose, -r, bottom, 0, r, top, 0, layer, alpha);
            quad(vc, pose, 0, bottom, -r, 0, top, r, layer, alpha);
        }
        // where it lands: a bright spot on the plate, a little wider than the shaft
        float spot = (0.30F + 0.04F * Mth.sin(time * 1.3F));
        float y = bottom + 0.02F;
        vc.vertex(pose, -spot, y, -spot).color(1F, 0.45F, 0.25F, 0.5F * flicker).endVertex();
        vc.vertex(pose, -spot, y, spot).color(1F, 0.45F, 0.25F, 0.5F * flicker).endVertex();
        vc.vertex(pose, spot, y, spot).color(1F, 0.45F, 0.25F, 0.5F * flicker).endVertex();
        vc.vertex(pose, spot, y, -spot).color(1F, 0.45F, 0.25F, 0.5F * flicker).endVertex();
        poseStack.popPose();
    }

    /** A vertical quad between two bottom / top corners (a plane of the beam), drawn from both sides. */
    private static void quad(VertexConsumer vc, Matrix4f pose, float x0, float y0, float z0, float x1, float y1, float z1,
                             float[] c, float alpha) {
        vc.vertex(pose, x0, y0, z0).color(c[1], c[2], c[3], alpha).endVertex();
        vc.vertex(pose, x1, y0, z1).color(c[1], c[2], c[3], alpha).endVertex();
        vc.vertex(pose, x1, y1, z1).color(c[1], c[2], c[3], alpha).endVertex();
        vc.vertex(pose, x0, y1, z0).color(c[1], c[2], c[3], alpha).endVertex();
    }
}
