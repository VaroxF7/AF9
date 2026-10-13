package com.af9.core.client.render;

import com.af9.core.AF9Core;
import com.af9.core.machine.IEcrwMachine;

import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine;
import com.gregtechceu.gtceu.api.pattern.util.RelativeDirection;
import com.gregtechceu.gtceu.client.renderer.machine.DynamicRender;
import com.gregtechceu.gtceu.client.renderer.machine.DynamicRenderManager;
import com.gregtechceu.gtceu.client.renderer.machine.DynamicRenderType;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
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
import org.joml.Matrix4f;

/**
 * The ECRW's pond: a translucent water surface on exactly the cells the pattern leaves hollow (r &lt; 6.4, outside the
 * 3 x 3 pier), in controller-relative blocks with y 0 at the pond floor's top. Drawn only while the pond is formed.
 * The rotating bridge was taken out: the whole machine gets its own multiblock model. The codec keeps the
 * {@code armY} and {@code radius} parameters of the machine definition for that model. Model side:
 * {@link com.af9.core.machine.AF9MachineModels#workableCasingWithEcrwArm}.
 */
@SuppressWarnings("removal") // new ResourceLocation(ns, path) is the only constructor on 1.20.1
public class EcrwArmRender extends DynamicRender<IEcrwMachine, EcrwArmRender> {

    // spotless:off
    public static final Codec<EcrwArmRender> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.FLOAT.fieldOf("back").forGetter(render -> render.back),
            Codec.FLOAT.fieldOf("armY").forGetter(render -> render.armY),
            Codec.FLOAT.fieldOf("radius").forGetter(render -> render.radius)
    ).apply(instance, EcrwArmRender::new));
    // spotless:on
    public static final DynamicRenderType<IEcrwMachine, EcrwArmRender> TYPE = new DynamicRenderType<>(CODEC);

    /** The pond's footprint: cells with r below this are hollow, the 3 x 3 pier (|dx|, |dz| below 2) is not. */
    private static final float POND_RADIUS = 6.4F;
    private static final int POND_CELLS = 6;
    /** Water surface and the darker layer above the floor, controller-relative y. */
    private static final float WATER_Y = 1.5F, DEEP_Y = 0.25F;
    /** Fallback water tint (vanilla default) when no biome is loaded. */
    private static final int FALLBACK_WATER = 0x3F76E4;

    private final float back;
    private final float armY;
    private final float radius;

    public EcrwArmRender(float back, float armY, float radius) {
        this.back = back;
        this.armY = armY;
        this.radius = radius;
    }

    /** Typed as GT's base class, so common code that builds a model never loads this client class. */
    public static DynamicRender<?, ?> create(float back, float armY, float radius) {
        return new EcrwArmRender(back, armY, radius);
    }

    /** Client, before the models are built (mod construction). */
    public static void register() {
        DynamicRenderManager.register(new ResourceLocation(AF9Core.MOD_ID, "ecrw_arm"), TYPE);
    }

    @Override
    public DynamicRenderType<IEcrwMachine, EcrwArmRender> getType() {
        return TYPE;
    }

    @Override
    public int getViewDistance() {
        return 96;
    }

    /** The pond's centre in the controller block's local space: straight behind it, on the middle of the block. */
    private float[] pivot(MetaMachine self) {
        boolean flipped = self instanceof MultiblockControllerMachine controller && controller.isFlipped();
        Direction backDir = RelativeDirection.BACK.getRelative(self.getFrontFacing(), self.getUpwardsFacing(), flipped);
        return new float[] { 0.5F + backDir.getStepX() * back, 0.5F + backDir.getStepZ() * back };
    }

    private Vec3 centre(MetaMachine self) {
        float[] pivot = pivot(self);
        BlockPos pos = self.getPos();
        return new Vec3(pos.getX() + pivot[0], pos.getY(), pos.getZ() + pivot[1]);
    }

    @Override
    public boolean shouldRender(IEcrwMachine machine, Vec3 cameraPos) {
        return machine.pondFormed() && centre(machine.self()).closerThan(cameraPos, getViewDistance());
    }

    @Override
    public boolean shouldRenderOffScreen(IEcrwMachine machine) {
        return false;
    }

    @Override
    public AABB getRenderBoundingBox(IEcrwMachine machine) {
        Vec3 c = centre(machine.self());
        return new AABB(c.x - POND_CELLS - 1.5, c.y - 1, c.z - POND_CELLS - 1.5,
                c.x + POND_CELLS + 1.5, c.y + 5, c.z + POND_CELLS + 1.5);
    }

    @Override
    public void render(IEcrwMachine machine, float partialTick, PoseStack poseStack, MultiBufferSource buffer,
                       int packedLight, int packedOverlay) {
        if (!machine.pondFormed()) return;
        if (IrisCompat.renderingShadowPass()) return;
        var level = Minecraft.getInstance().level;
        if (level == null) return;
        MetaMachine self = machine.self();
        float[] pivot = pivot(self);
        Vec3 centre = centre(self);
        double now = level.getGameTime() + partialTick;

        poseStack.pushPose();
        poseStack.translate(pivot[0], 0, pivot[1]);
        drawWater(buffer.getBuffer(AF9RenderTypes.LIGHT_RING), poseStack.last().pose(), level, centre, now,
                machine.rakeSpinning());
        poseStack.popPose();
    }

    /** The water: a darker layer above the floor and a surface with a ripple, on the pond's own cells. */
    private void drawWater(VertexConsumer vc, Matrix4f mat, ClientLevel level,
                           Vec3 centre, double time, boolean working) {
        int tint = FALLBACK_WATER;
        try {
            tint = level.getBiome(BlockPos.containing(centre)).value().getSpecialEffects().getWaterColor();
        } catch (RuntimeException ignored) {
            // biome not ready: keep the fallback
        }
        int r = (tint >> 16) & 0xFF, g = (tint >> 8) & 0xFF, b = tint & 0xFF;
        float ripple = 0.5F + 0.5F * Mth.sin((float) (time * 2 * Math.PI / 46));
        float surfaceAlpha = working ? 0.58F + 0.06F * ripple : 0.46F + 0.04F * ripple;
        for (int dx = -POND_CELLS; dx <= POND_CELLS; dx++) {
            for (int dz = -POND_CELLS; dz <= POND_CELLS; dz++) {
                if (Math.max(Math.abs(dx), Math.abs(dz)) < 2) continue;
                if (Math.sqrt(dx * dx + dz * dz) >= POND_RADIUS) continue;
                float x0 = dx - 0.5F, x1 = dx + 0.5F, z0 = dz - 0.5F, z1 = dz + 0.5F;
                flat(vc, mat, x0, DEEP_Y, z0, x1, z1, r / 2, g / 2, b / 2, 0.55F);
                flat(vc, mat, x0, WATER_Y, z0, x1, z1, r, g, b, surfaceAlpha);
            }
        }
    }

    private static void flat(VertexConsumer vc, Matrix4f mat, float x0, float y, float z0, float x1, float z1, int r,
                             int g, int b, float alpha) {
        float fr = r / 255F, fg = g / 255F, fb = b / 255F;
        vc.vertex(mat, x0, y, z0).color(fr, fg, fb, alpha).endVertex();
        vc.vertex(mat, x1, y, z0).color(fr, fg, fb, alpha).endVertex();
        vc.vertex(mat, x1, y, z1).color(fr, fg, fb, alpha).endVertex();
        vc.vertex(mat, x0, y, z1).color(fr, fg, fb, alpha).endVertex();
    }
}
