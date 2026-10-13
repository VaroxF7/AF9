package com.af9.core.client.render;

import com.af9.core.AF9Core;
import com.af9.core.machine.IFiltrationPlantMachine;

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
import org.joml.Matrix4f;

import java.util.Random;

/**
 * The Water Filtration Plant's open basin, seen through the glass:
 * <ul>
 * <li>animated <b>water</b> in the biome's own water colour: a translucent surface with a slow ripple
 * and a highlight that travels across it while the plant runs;</li>
 * <li><b>debris</b> floating on top: 5-15 pieces of flotsam (sand, gravel, leaves, sticks, clay),
 * seeded by the basin's position so every plant floats its own mix; they bob, drift, and pile up
 * ahead of the skimmer while it sweeps;</li>
 * <li>a 3D <b>skimmer arm</b>: side rails along the basin and a gantry beam with a blade dipping into
 * the water, sweeping front to back at a steady speed while the plant runs (parked at the back idle).</li>
 * </ul>
 * Placed relative to the controller: {@code up} along its up and {@code back} behind it, centred on
 * the basin. Model side: {@code AF9MachineModels.workableCasingWithFiltrationBasin}.
 */
@SuppressWarnings("removal") // new ResourceLocation(ns, path) is the only constructor on 1.20.1
public class FiltrationBasinRender extends DynamicRender<IFiltrationPlantMachine, FiltrationBasinRender> {

    // spotless:off
    public static final Codec<FiltrationBasinRender> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.FLOAT.fieldOf("up").forGetter(render -> render.up),
            Codec.FLOAT.fieldOf("back").forGetter(render -> render.back)
    ).apply(instance, FiltrationBasinRender::new));
    // spotless:on
    public static final DynamicRenderType<IFiltrationPlantMachine, FiltrationBasinRender> TYPE =
            new DynamicRenderType<>(CODEC);

    private static final int FULL_BRIGHT = 0xF000F0;

    /** Basin half extents: 3 x 3 of open water. */
    private static final float HX = 1.5F, HZ = 1.5F;
    /** Water surface above the basin centre, and the darker depth layer below it. */
    private static final float WATER_Y = 0.30F, DEEP_Y = 0.22F;
    /** Skimmer travel along the basin (blocks from the centre); parked at the back while idle. */
    private static final float SWEEP_FRONT = -1.2F, SWEEP_BACK = 1.2F;
    /** Steady sweep speed: one full pass across the basin in this many ticks. */
    private static final float SWEEP_PASS_TICKS = 120F;
    /** Gantry beam height and the blade dipping into the water. */
    private static final float BEAM_Y = 0.78F, BLADE_TOP = 0.34F, BLADE_BOTTOM = 0.18F;

    /** Debris palette: sand, gravel, leaf, stick, clay. */
    private static final int[] DEBRIS_PALETTE = { 0xC2B280, 0x8A8D91, 0x4E9A3D, 0x7A5A33, 0x9AA4B0 };
    /** Fallback water tint (vanilla default) when no biome is loaded. */
    private static final int FALLBACK_WATER = 0x3F76E4;

    private final float up;
    private final float back;

    public FiltrationBasinRender(float up, float back) {
        this.up = up;
        this.back = back;
    }

    /** Typed as GT's base class, so common code that builds a model never loads this client class. */
    public static DynamicRender<?, ?> create(float up, float back) {
        return new FiltrationBasinRender(up, back);
    }

    /** Client, before the models are built (mod construction). */
    public static void register() {
        DynamicRenderManager.register(new ResourceLocation(AF9Core.MOD_ID, "filtration_basin"), TYPE);
    }

    @Override
    public DynamicRenderType<IFiltrationPlantMachine, FiltrationBasinRender> getType() {
        return TYPE;
    }

    @Override
    public int getViewDistance() {
        return 64;
    }

    @Override
    public boolean shouldRender(IFiltrationPlantMachine machine, Vec3 cameraPos) {
        if (!machine.isBasinFormed()) return false;
        MetaMachine self = machine.self();
        Frame frame = frame(self);
        Vec3 centre = Vec3.atLowerCornerOf(self.getPos()).add(frame.x, frame.y, frame.z);
        return centre.closerThan(cameraPos, getViewDistance());
    }

    @Override
    public boolean shouldRenderOffScreen(IFiltrationPlantMachine machine) {
        return false;
    }

    @Override
    public AABB getRenderBoundingBox(IFiltrationPlantMachine machine) {
        MetaMachine self = machine.self();
        Frame frame = frame(self);
        Vec3 centre = Vec3.atLowerCornerOf(self.getPos()).add(frame.x, frame.y, frame.z);
        return new AABB(centre, centre).inflate(2.5);
    }

    @Override
    public void render(IFiltrationPlantMachine machine, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight, int packedOverlay) {
        if (!machine.isBasinFormed()) return;
        if (IrisCompat.renderingShadowPass()) return;
        MetaMachine self = machine.self();
        Frame frame = frame(self);
        float progress = machine.getBasinProgress(partialTick);
        boolean working = machine.isBasinActive() && progress >= 0;
        double time = self.getOffsetTimer() + partialTick;

        poseStack.pushPose();
        poseStack.translate(frame.x, frame.y, frame.z);
        Matrix4f mat = poseStack.last().pose();

        VertexConsumer solid = buffer.getBuffer(RenderType.entityCutoutNoCull(AF9RenderTypes.WHITE));
        VertexConsumer water = buffer.getBuffer(AF9RenderTypes.LIGHT_RING);

        Vec3 centre = Vec3.atLowerCornerOf(self.getPos()).add(frame.x, frame.y, frame.z);
        BlockPos basinPos = BlockPos.containing(centre);

        // --- water tint: the biome's own water colour at the basin ---
        int tint = FALLBACK_WATER;
        var clientLevel = Minecraft.getInstance().level;
        if (clientLevel != null) {
            try {
                tint = clientLevel.getBiome(basinPos).value().getSpecialEffects().getWaterColor();
            } catch (RuntimeException ignored) {
                // biome not ready: keep the fallback
            }
        }
        int wr = (tint >> 16) & 0xFF, wg = (tint >> 8) & 0xFF, wb = tint & 0xFF;

        // --- water: depth layer + rippling surface ---
        float ripple = 0.5F + 0.5F * Mth.sin((float) (time * 2 * Math.PI / 46));
        quad(water, mat, frame, -HX, DEEP_Y, -HZ, HX, DEEP_Y, HZ, wr / 2, wg / 2, wb / 2, 0.35F);
        float surfaceAlpha = working ? 0.50F + 0.06F * ripple : 0.34F + 0.04F * ripple;
        quad(water, mat, frame, -HX, WATER_Y, -HZ, HX, WATER_Y, HZ, wr, wg, wb, surfaceAlpha);
        // travelling highlight while running: a bright band crossing front to back
        if (working) {
            float band = (float) ((time % 60) / 60);
            float bz = Mth.lerp(band, -HZ, HZ);
            quad(water, mat, frame, -HX, WATER_Y + 0.005F, bz - 0.12F, HX, WATER_Y + 0.005F, bz + 0.12F,
                    Math.min(255, wr + 110), Math.min(255, wg + 110), Math.min(255, wb + 110), 0.28F);
        }

        // --- skimmer: steady ping-pong sweep while running, parked at the back idle ---
        float carriageZ = SWEEP_BACK;
        if (working) {
            float phase = (float) (time / SWEEP_PASS_TICKS) % 2.0F;
            float sweep = phase < 1.0F ? phase : 2.0F - phase;
            carriageZ = Mth.lerp(sweep, SWEEP_FRONT, SWEEP_BACK);
        }

        // --- debris floating on top: this basin's own mix, bobbing, drifting, piling ahead of the blade ---
        float[][] debris = debrisFor(basinPos);
        for (int i = 0; i < debris.length; i++) {
            float bx = debris[i][0];
            float bz = debris[i][1];
            float size = debris[i][2];
            int rgb = (int) debris[i][3];
            float phase = i * 1.1F;
            float bob = 0.02F * Mth.sin((float) (time * 2 * Math.PI / 30) + phase);
            float driftX = 0.08F * Mth.sin((float) (time * 2 * Math.PI / 70) + phase * 2);
            float driftZ = 0.08F * Mth.cos((float) (time * 2 * Math.PI / 60) + phase);
            float dx = bx + driftX;
            float dz = bz + driftZ;
            if (working) {
                // the blade pushes what floats just ahead of it
                float ahead = dz - carriageZ;
                if (ahead > 0 && ahead < 0.5F) dz += (0.5F - ahead) * 0.6F;
            }
            float dy = WATER_Y + 0.015F + bob;
            SolidBox.box(solid, mat, frame, dx - size / 2, dy, dz - size / 4,
                    dx + size / 2, dy + size / 2, dz + size / 4, rgb);
        }

        // --- skimmer arm: static side rails, moving gantry beam, posts, dipping blade ---
        // rails along both sides (always there)
        SolidBox.box(solid, mat, frame, -1.45F, BEAM_Y - 0.05F, -HZ, -1.25F, BEAM_Y + 0.05F, HZ, 0x4B5567);
        SolidBox.box(solid, mat, frame, 1.25F, BEAM_Y - 0.05F, -HZ, 1.45F, BEAM_Y + 0.05F, HZ, 0x4B5567);
        for (float tie = -1.2F; tie <= 1.21F; tie += 0.4F) {
            SolidBox.box(solid, mat, frame, -1.45F, BEAM_Y - 0.06F, tie - 0.03F, -1.25F, BEAM_Y - 0.02F,
                    tie + 0.03F, 0x6B7688);
            SolidBox.box(solid, mat, frame, 1.25F, BEAM_Y - 0.06F, tie - 0.03F, 1.45F, BEAM_Y - 0.02F,
                    tie + 0.03F, 0x6B7688);
        }
        // gantry beam across the basin at the carriage
        SolidBox.box(solid, mat, frame, -1.45F, BEAM_Y - 0.06F, carriageZ - 0.09F, 1.45F, BEAM_Y + 0.06F,
                carriageZ + 0.09F, 0x8A94A6);
        // posts down to the blade at both ends
        SolidBox.box(solid, mat, frame, -1.42F, BLADE_TOP, carriageZ - 0.06F, -1.28F, BEAM_Y - 0.06F,
                carriageZ + 0.06F, 0x6B7688);
        SolidBox.box(solid, mat, frame, 1.28F, BLADE_TOP, carriageZ - 0.06F, 1.42F, BEAM_Y - 0.06F,
                carriageZ + 0.06F, 0x6B7688);
        // blade dipping into the water
        SolidBox.box(solid, mat, frame, -1.35F, BLADE_BOTTOM, carriageZ - 0.05F, 1.35F, BLADE_TOP,
                carriageZ + 0.05F, working ? 0xB8C4D4 : 0x4B5567);
        if (working) {
            float blink = 0.45F + 0.25F * Mth.sin((float) (time * 2 * Math.PI / 30));
            quad(water, mat, frame, -0.2F, BEAM_Y + 0.065F, carriageZ - 0.06F, 0.2F, BEAM_Y + 0.065F,
                    carriageZ + 0.06F, 120, 200, 255, blink);
        }

        poseStack.popPose();
    }

    /** Basin centre in block-local coords (0.5 + up/back along the controller's axes). */
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

    /**
     * This basin's debris: 5-15 pieces of flotsam on the water, seeded by the basin's position so every
     * plant floats its own mix but it never flickers. Each piece is x, z, size, colour.
     */
    private static float[][] debrisFor(BlockPos basin) {
        Random rand = new Random(basin.asLong());
        int count = 5 + rand.nextInt(11);
        float[][] out = new float[count][];
        for (int i = 0; i < count; i++) {
            float x = rand.nextFloat() * 2.6F - 1.3F;
            float z = rand.nextFloat() * 2.6F - 1.3F;
            float size = 0.08F + rand.nextFloat() * 0.09F;
            out[i] = new float[] { x, z, size, DEBRIS_PALETTE[rand.nextInt(DEBRIS_PALETTE.length)] };
        }
        return out;
    }

    private record Frame(float x, float y, float z, Direction left, Direction up, Direction back) {
        /** Local (left/up/back) to pose-space: the pose is already at the basin centre. */
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

    /** Flat horizontal quad (y constant): two corners. */
    private static void quad(VertexConsumer c, Matrix4f mat, Frame f, float x0, float y0, float z0, float x1,
                             float y1, float z1, int r, int g, int b, float alpha) {
        quadRaw(c, mat, f.px(x0, y0, z0), f.py(x0, y0, z0), f.pz(x0, y0, z0), f.px(x1, y0, z0), f.py(x1, y0, z0),
                f.pz(x1, y0, z0), f.px(x1, y1, z1), f.py(x1, y1, z1), f.pz(x1, y1, z1), f.px(x0, y1, z1),
                f.py(x0, y1, z1), f.pz(x0, y1, z1), r / 255F, g / 255F, b / 255F, alpha);
    }

    private static void quadRaw(VertexConsumer c, Matrix4f mat, float x0, float y0, float z0, float x1, float y1,
                                float z1, float x2, float y2, float z2, float x3, float y3, float z3, float r,
                                float g, float b, float alpha) {
        if (alpha <= 0.003F) return;
        c.vertex(mat, x0, y0, z0).color(r, g, b, alpha).endVertex();
        c.vertex(mat, x1, y1, z1).color(r, g, b, alpha).endVertex();
        c.vertex(mat, x2, y2, z2).color(r, g, b, alpha).endVertex();
        c.vertex(mat, x3, y3, z3).color(r, g, b, alpha).endVertex();
    }

    /** Opaque boxes (entity shader, full bright, white texture). */
    private static final class SolidBox {

        static void box(VertexConsumer c, Matrix4f mat, Frame f, float x0, float y0, float z0, float x1, float y1,
                        float z1, int rgb) {
            float r = ((rgb >> 16) & 0xFF) / 255F, g = ((rgb >> 8) & 0xFF) / 255F, b = (rgb & 0xFF) / 255F;
            // +y / -y
            face(c, mat, f, x0, y1, z0, x1, y1, z0, x1, y1, z1, x0, y1, z1, 0, 1, 0, r, g, b);
            face(c, mat, f, x0, y0, z1, x1, y0, z1, x1, y0, z0, x0, y0, z0, 0, -1, 0, r * 0.6F, g * 0.6F, b * 0.6F);
            // +x / -x (left axis)
            face(c, mat, f, x1, y0, z0, x1, y0, z1, x1, y1, z1, x1, y1, z0, 1, 0, 0, r * 0.8F, g * 0.8F, b * 0.8F);
            face(c, mat, f, x0, y0, z1, x0, y0, z0, x0, y1, z0, x0, y1, z1, -1, 0, 0, r * 0.8F, g * 0.8F, b * 0.8F);
            // +z / -z (back axis)
            face(c, mat, f, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1, 0, 0, 1, r * 0.9F, g * 0.9F, b * 0.9F);
            face(c, mat, f, x1, y0, z0, x0, y0, z0, x0, y1, z0, x1, y1, z0, 0, 0, -1, r * 0.9F, g * 0.9F, b * 0.9F);
        }

        private static void face(VertexConsumer c, Matrix4f mat, Frame f, float lx0, float ly0, float lz0, float lx1,
                                 float ly1, float lz1, float lx2, float ly2, float lz2, float lx3, float ly3,
                                 float lz3, float nx, float ny, float nz, float r, float g, float b) {
            float wx = nx * f.left.getStepX() + ny * f.up.getStepX() + nz * f.back.getStepX();
            float wy = nx * f.left.getStepY() + ny * f.up.getStepY() + nz * f.back.getStepY();
            float wz = nx * f.left.getStepZ() + ny * f.up.getStepZ() + nz * f.back.getStepZ();
            float len = Mth.sqrt(wx * wx + wy * wy + wz * wz);
            if (len > 1e-6F) {
                wx /= len;
                wy /= len;
                wz /= len;
            }
            vertex(c, mat, f, lx0, ly0, lz0, wx, wy, wz, r, g, b);
            vertex(c, mat, f, lx1, ly1, lz1, wx, wy, wz, r, g, b);
            vertex(c, mat, f, lx2, ly2, lz2, wx, wy, wz, r, g, b);
            vertex(c, mat, f, lx3, ly3, lz3, wx, wy, wz, r, g, b);
        }

        private static void vertex(VertexConsumer c, Matrix4f mat, Frame f, float lx, float ly, float lz, float nx,
                                   float ny, float nz, float r, float g, float b) {
            c.vertex(mat, f.px(lx, ly, lz), f.py(lx, ly, lz), f.pz(lx, ly, lz)).color(r, g, b, 1F).uv(0.5F, 0.5F)
                    .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(FULL_BRIGHT).normal(nx, ny, nz).endVertex();
        }
    }
}
