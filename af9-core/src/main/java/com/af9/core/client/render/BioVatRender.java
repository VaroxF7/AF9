package com.af9.core.client.render;

import com.af9.core.AF9Core;
import com.af9.core.machine.IBioVatMachine;

import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine;
import com.gregtechceu.gtceu.api.pattern.util.RelativeDirection;
import com.gregtechceu.gtceu.client.renderer.machine.DynamicRender;
import com.gregtechceu.gtceu.client.renderer.machine.DynamicRenderManager;
import com.gregtechceu.gtceu.client.renderer.machine.DynamicRenderType;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.registries.ForgeRegistries;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import org.joml.Matrix4f;

/**
 * The Bio Vat's culture, seen through the glass: a translucent body of the stored fluid in the fluid's own colour,
 * standing as high as the hatches are full (0-2 blocks of the 3 x 3 x 2 tank), with a slowly rippling surface and, while
 * a run goes on, bubbles rising through it. GTNH's vat places fluid blocks in the world for this; this draws instead, so
 * nothing is left behind in the world. Placed relative to the controller: {@code up} along its up (the tank's floor) and
 * {@code back} behind it (the tank's centre). Model side: {@code AF9MachineModels.workableCasingWithBioVat}.
 */
@SuppressWarnings("removal") // new ResourceLocation(ns, path) is the only constructor on 1.20.1
public class BioVatRender extends DynamicRender<IBioVatMachine, BioVatRender> {

    // spotless:off
    public static final Codec<BioVatRender> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.FLOAT.fieldOf("up").forGetter(render -> render.up),
            Codec.FLOAT.fieldOf("back").forGetter(render -> render.back)
    ).apply(instance, BioVatRender::new));
    // spotless:on
    public static final DynamicRenderType<IBioVatMachine, BioVatRender> TYPE = new DynamicRenderType<>(CODEC);

    /** Half extent of the tank (3 x 3), its height, and the gap kept from the glass. */
    private static final float HALF = 1.5F - 0.02F, TANK_HEIGHT = 2.0F;
    /** The colour when the fluid has none of its own (the plain culture's green). */
    private static final int FALLBACK = 0x4A9A3D;
    private static final int BUBBLES = 14;

    private final float up;
    private final float back;

    public BioVatRender(float up, float back) {
        this.up = up;
        this.back = back;
    }

    /** Typed as GT's base class, so common code that builds a model never loads this client class. */
    public static DynamicRender<?, ?> create(float up, float back) {
        return new BioVatRender(up, back);
    }

    /** Client, before the models are built (mod construction). */
    public static void register() {
        DynamicRenderManager.register(new ResourceLocation(AF9Core.MOD_ID, "bio_vat"), TYPE);
    }

    @Override
    public DynamicRenderType<IBioVatMachine, BioVatRender> getType() {
        return TYPE;
    }

    @Override
    public int getViewDistance() {
        return 64;
    }

    @Override
    public boolean shouldRender(IBioVatMachine machine, Vec3 cameraPos) {
        if (!machine.isVatFormed()) return false;
        MetaMachine self = machine.self();
        Frame frame = frame(self);
        return Vec3.atLowerCornerOf(self.getPos()).add(frame.x, frame.y, frame.z).closerThan(cameraPos,
                getViewDistance());
    }

    @Override
    public boolean shouldRenderOffScreen(IBioVatMachine machine) {
        return false;
    }

    @Override
    public AABB getRenderBoundingBox(IBioVatMachine machine) {
        MetaMachine self = machine.self();
        Frame frame = frame(self);
        Vec3 centre = Vec3.atLowerCornerOf(self.getPos()).add(frame.x, frame.y, frame.z);
        return new AABB(centre, centre).inflate(3);
    }

    @Override
    public void render(IBioVatMachine machine, float partialTick, PoseStack poseStack, MultiBufferSource buffer,
                       int packedLight, int packedOverlay) {
        if (!machine.isVatFormed()) return;
        if (IrisCompat.renderingShadowPass()) return;
        MetaMachine self = machine.self();
        Frame frame = frame(self);
        double time = self.getOffsetTimer() + partialTick;
        // a formed vat is always liquid-filled: full of the faint culture green while the hatches are empty, then as
        // high as they are full, in the fluid's colour
        boolean stocked = machine.getVatFill() > 0;
        float height = stocked ? Mth.clamp(machine.getVatFill() / 1000F, 0F, 1F) * TANK_HEIGHT : TANK_HEIGHT;
        if (height < 0.05F) return;
        float fade = stocked ? 1F : 0.6F;

        int rgb = stocked ? colour(machine.getVatFluid()) : FALLBACK;
        int r = (rgb >> 16) & 0xFF, g = (rgb >> 8) & 0xFF, b = rgb & 0xFF;
        boolean working = machine.isVatActive();

        poseStack.pushPose();
        poseStack.translate(frame.x, frame.y, frame.z);
        Matrix4f mat = poseStack.last().pose();
        VertexConsumer c = buffer.getBuffer(AF9RenderTypes.LIGHT_RING);

        // the body: four sides and the surface, darker down the tank
        float side = 0.22F * fade;
        float ripple = 0.5F + 0.5F * Mth.sin((float) (time * 2 * Math.PI / 54));
        float top = height - 0.02F * ripple;
        wall(c, mat, frame, -HALF, 0, -HALF, HALF, top, -HALF, r, g, b, side);
        wall(c, mat, frame, -HALF, 0, HALF, HALF, top, HALF, r, g, b, side);
        wall(c, mat, frame, -HALF, 0, -HALF, -HALF, top, HALF, r, g, b, side);
        wall(c, mat, frame, HALF, 0, -HALF, HALF, top, HALF, r, g, b, side);
        float surface = (working ? 0.42F : 0.34F) * fade;
        quad(c, mat, frame, -HALF, top, -HALF, HALF, top, -HALF, HALF, top, HALF, -HALF, top, HALF, r, g, b,
                surface);

        // bubbles while a run goes on: small crossed quads rising through the body
        if (working) {
            int br = Math.min(255, r + 90), bg = Math.min(255, g + 90), bb = Math.min(255, b + 90);
            for (int i = 0; i < BUBBLES; i++) {
                float fx = hash(i * 2) * 2F * (HALF - 0.2F) - (HALF - 0.2F);
                float fz = hash(i * 2 + 1) * 2F * (HALF - 0.2F) - (HALF - 0.2F);
                float speed = 0.012F + 0.01F * hash(i + 40);
                float y = (float) ((time * speed + hash(i + 80)) % 1.0) * Math.max(top - 0.1F, 0.01F);
                float s = 0.05F + 0.05F * hash(i + 120);
                float wobble = 0.05F * Mth.sin((float) (time * 0.15 + i));
                float x = fx + wobble;
                quad(c, mat, frame, x - s, y, fz, x + s, y, fz, x + s, y + 2 * s, fz, x - s, y + 2 * s, fz, br, bg,
                        bb, 0.4F);
                quad(c, mat, frame, x, y, fz - s, x, y, fz + s, x, y + 2 * s, fz + s, x, y + 2 * s, fz - s, br, bg,
                        bb, 0.4F);
            }
        }

        poseStack.popPose();
    }

    /** A fixed pseudo-random 0-1 per index, so the bubbles never flicker between frames. */
    private static float hash(int i) {
        int h = i * 0x9E3779B1;
        h ^= h >>> 15;
        h *= 0x85EBCA6B;
        h ^= h >>> 13;
        return (h & 0xFFFF) / 65535F;
    }

    /** The fluid's own tint, or the culture green. */
    private static int colour(String fluidId) {
        if (fluidId == null || fluidId.isEmpty()) return FALLBACK;
        try {
            Fluid fluid = ForgeRegistries.FLUIDS.getValue(new ResourceLocation(fluidId));
            if (fluid == null) return FALLBACK;
            int tint = IClientFluidTypeExtensions.of(fluid).getTintColor() & 0xFFFFFF;
            return tint == 0 || tint == 0xFFFFFF ? FALLBACK : tint;
        } catch (RuntimeException e) {
            return FALLBACK;
        }
    }

    /** Tank floor centre in block-local coords ({@code up} / {@code back} along the controller's axes). */
    private Frame frame(MetaMachine self) {
        boolean flipped = self instanceof MultiblockControllerMachine controller && controller.isFlipped();
        Direction upDir = RelativeDirection.UP.getRelative(self.getFrontFacing(), self.getUpwardsFacing(), flipped);
        Direction backDir = RelativeDirection.BACK.getRelative(self.getFrontFacing(), self.getUpwardsFacing(),
                flipped);
        Direction leftDir = RelativeDirection.LEFT.getRelative(self.getFrontFacing(), self.getUpwardsFacing(),
                flipped);
        float x = 0.5F + upDir.getStepX() * up + backDir.getStepX() * back;
        float y = 0.5F + upDir.getStepY() * up + backDir.getStepY() * back;
        float z = 0.5F + upDir.getStepZ() * up + backDir.getStepZ() * back;
        return new Frame(x, y, z, leftDir, upDir, backDir);
    }

    private record Frame(float x, float y, float z, Direction left, Direction up, Direction back) {
        float px(float lx, float ly, float lz) {
            return lx * left.getStepX() + ly * up.getStepX() + lz * back.getStepX();
        }

        float py(float lx, float ly, float lz) {
            return lx * left.getStepY() + ly * up.getStepY() + lz * back.getStepY();
        }

        float pz(float lx, float ly, float lz) {
            return lx * left.getStepZ() + ly * up.getStepZ() + lz * back.getStepZ();
        }
    }

    /** A vertical wall between two bottom/top corners (same x or same z), drawn from both sides by the render type. */
    private static void wall(VertexConsumer c, Matrix4f mat, Frame f, float x0, float y0, float z0, float x1,
                             float y1, float z1, int r, int g, int b, float alpha) {
        quad(c, mat, f, x0, y0, z0, x1, y0, z1, x1, y1, z1, x0, y1, z0, r, g, b, alpha);
    }

    private static void quad(VertexConsumer c, Matrix4f mat, Frame f, float x0, float y0, float z0, float x1,
                             float y1, float z1, float x2, float y2, float z2, float x3, float y3, float z3, int r,
                             int g, int b, float alpha) {
        v(c, mat, f, x0, y0, z0, r, g, b, alpha);
        v(c, mat, f, x1, y1, z1, r, g, b, alpha);
        v(c, mat, f, x2, y2, z2, r, g, b, alpha);
        v(c, mat, f, x3, y3, z3, r, g, b, alpha);
    }

    private static void v(VertexConsumer c, Matrix4f mat, Frame f, float lx, float ly, float lz, int r, int g, int b,
                          float alpha) {
        c.vertex(mat, f.px(lx, ly, lz), f.py(lx, ly, lz), f.pz(lx, ly, lz)).color(r / 255F, g / 255F, b / 255F, alpha)
                .endVertex();
    }
}
