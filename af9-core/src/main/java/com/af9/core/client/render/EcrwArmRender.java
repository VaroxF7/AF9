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
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import java.util.HashMap;
import java.util.Map;

/**
 * The ECRW's rake arm: a steel truss across the pond on the central drive, with squeegee blades hanging into the
 * brine. It turns while a recipe runs (about 3 RPM) and parks where it stopped otherwise. One render object serves
 * every well, so the parked angles are kept per controller position (capped: positions beyond it fall back to facing
 * north). Model side: {@link com.af9.core.machine.AF9MachineModels#workableCasingWithEcrwArm}.
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

    /** Three turns a minute, in radians per tick. */
    private static final float SPEED = (float) (3 * 2 * Math.PI / 1200);
    /** Parked angles by controller position (the render object is shared, the ponds are not). */
    private final Map<BlockPos, Float> angles = new HashMap<>();

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
        return 128;
    }

    /** The pond's centre, relative to the controller's position: straight behind it, on the middle of the block. */
    private float[] pivot(MetaMachine self) {
        boolean flipped = self instanceof MultiblockControllerMachine controller && controller.isFlipped();
        Direction backDir = RelativeDirection.BACK.getRelative(self.getFrontFacing(), self.getUpwardsFacing(), flipped);
        return new float[] { 0.5F + backDir.getStepX() * back, 0.5F + backDir.getStepZ() * back };
    }

    @Override
    public boolean shouldRender(IEcrwMachine machine, Vec3 cameraPos) {
        return Vec3.atCenterOf(machine.self().getPos()).closerThan(cameraPos, getViewDistance());
    }

    @Override
    public boolean shouldRenderOffScreen(IEcrwMachine machine) {
        return true;
    }

    @Override
    public AABB getRenderBoundingBox(IEcrwMachine machine) {
        BlockPos pos = machine.self().getPos();
        return new AABB(pos).inflate(radius + 2, 8, radius + 2);
    }

    @Override
    public void render(IEcrwMachine machine, float partialTick, PoseStack poseStack, MultiBufferSource buffer,
                       int packedLight, int packedOverlay) {
        var level = Minecraft.getInstance().level;
        if (level == null) return;
        BlockPos key = machine.self().getPos().immutable();
        float angle;
        if (machine.rakeSpinning()) {
            angle = (float) ((level.getGameTime() + partialTick) * SPEED);
            angles.put(key, angle);
            if (angles.size() > 64) angles.clear();
        } else {
            angle = angles.getOrDefault(key, 0F);
        }
        float[] pivot = pivot(machine.self());
        TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
                .apply(new ResourceLocation("minecraft:block/iron_block"));

        poseStack.pushPose();
        poseStack.translate(pivot[0], armY, pivot[1]);
        poseStack.mulPose(Axis.YP.rotation(angle));
        Matrix4f pose = poseStack.last().pose();
        Matrix3f normal = poseStack.last().normal();
        VertexConsumer vc = buffer.getBuffer(RenderType.solid());
        float r = radius;
        // the drive hub and its housing on the central pier
        box(vc, pose, normal, sprite, packedLight, -0.35F, -0.1F, -0.35F, 0.35F, 0.55F, 0.35F);
        box(vc, pose, normal, sprite, packedLight, -0.2F, 0.55F, -0.2F, 0.2F, 1.0F, 0.2F);
        // the truss, wall to wall through the hub
        box(vc, pose, normal, sprite, packedLight, -r, -0.12F, -0.14F, r, 0.12F, 0.14F);
        // the squeegee blades hanging into the brine, and the end scrapers
        for (float x : new float[] { -r * 0.75F, -r * 0.35F, r * 0.35F, r * 0.75F }) {
            box(vc, pose, normal, sprite, packedLight, x - 0.09F, -1.35F, -0.09F, x + 0.09F, -0.12F, 0.09F);
        }
        box(vc, pose, normal, sprite, packedLight, -r - 0.1F, -1.1F, -0.35F, -r + 0.1F, -0.12F, 0.35F);
        box(vc, pose, normal, sprite, packedLight, r - 0.1F, -1.1F, -0.35F, r + 0.1F, -0.12F, 0.35F);
        poseStack.popPose();
    }

    /** A shaded steel box, full sprite on every face, classic top-lit shading that stays put while the arm turns. */
    private static void box(VertexConsumer vc, Matrix4f pose, Matrix3f normal, TextureAtlasSprite sprite, int light,
                            float x0, float y0, float z0, float x1, float y1, float z1) {
        float u0 = sprite.getU0(), u1 = sprite.getU1(), v0 = sprite.getV0(), v1 = sprite.getV1();
        // top and bottom
        quad(vc, pose, normal, u0, u1, v0, v1, light, 1.0F, 1.0F, 1.0F, 0, 1, 0,
                x0, y1, z0, x1, y1, z0, x1, y1, z1, x0, y1, z1);
        quad(vc, pose, normal, u0, u1, v0, v1, light, 0.5F, 0.5F, 0.5F, 0, -1, 0,
                x0, y0, z0, x0, y0, z1, x1, y0, z1, x1, y0, z0);
        // north and south
        quad(vc, pose, normal, u0, u1, v0, v1, light, 0.8F, 0.8F, 0.8F, 0, 0, -1,
                x0, y0, z0, x1, y0, z0, x1, y1, z0, x0, y1, z0);
        quad(vc, pose, normal, u0, u1, v0, v1, light, 0.8F, 0.8F, 0.8F, 0, 0, 1,
                x0, y0, z1, x0, y1, z1, x1, y1, z1, x1, y0, z1);
        // west and east
        quad(vc, pose, normal, u0, u1, v0, v1, light, 0.6F, 0.6F, 0.6F, -1, 0, 0,
                x0, y0, z0, x0, y1, z0, x0, y1, z1, x0, y0, z1);
        quad(vc, pose, normal, u0, u1, v0, v1, light, 0.6F, 0.6F, 0.6F, 1, 0, 0,
                x1, y0, z0, x1, y0, z1, x1, y1, z1, x1, y1, z0);
    }

    private static void quad(VertexConsumer vc, Matrix4f pose, Matrix3f normal, float u0, float u1, float v0, float v1,
                             int light,
                             float shade, float shadeG, float shadeB, float nx, float ny, float nz,
                             float x0, float y0, float z0, float x1, float y1, float z1,
                             float x2, float y2, float z2, float x3, float y3, float z3) {
        vc.vertex(pose, x0, y0, z0).color(shade, shadeG, shadeB, 1F).uv(u0, v0).uv2(light).normal(normal, nx, ny, nz)
                .endVertex();
        vc.vertex(pose, x1, y1, z1).color(shade, shadeG, shadeB, 1F).uv(u1, v0).uv2(light).normal(normal, nx, ny, nz)
                .endVertex();
        vc.vertex(pose, x2, y2, z2).color(shade, shadeG, shadeB, 1F).uv(u1, v1).uv2(light).normal(normal, nx, ny, nz)
                .endVertex();
        vc.vertex(pose, x3, y3, z3).color(shade, shadeG, shadeB, 1F).uv(u0, v1).uv2(light).normal(normal, nx, ny, nz)
                .endVertex();
    }
}
