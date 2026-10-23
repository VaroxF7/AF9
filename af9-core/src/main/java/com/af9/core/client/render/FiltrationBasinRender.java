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
 * <li>animated <b>water</b> in the biome's own water colour: a translucent surface that is murky brown-green
 * at the inlet (the front) and clears toward the back, a slow drifting cloud in it, and a highlight that
 * travels across it while the plant runs;</li>
 * <li><b>scum and foam</b> floating on top: flat patches, thickest at the inlet, seeded by the basin's
 * position so every plant floats its own mix; they drift and pile up ahead of the skimmer while it sweeps;</li>
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
            Codec.FLOAT.fieldOf("back").forGetter(render -> render.back),
            Codec.FLOAT.fieldOf("left").forGetter(render -> render.left),
            Codec.FLOAT.fieldOf("width").forGetter(render -> render.hx * 2F),
            Codec.FLOAT.fieldOf("length").forGetter(render -> render.hz * 2F)
    ).apply(instance, FiltrationBasinRender::new));
    // spotless:on
    public static final DynamicRenderType<IFiltrationPlantMachine, FiltrationBasinRender> TYPE =
            new DynamicRenderType<>(CODEC);

    private static final int FULL_BRIGHT = 0xF000F0;

    /** Water surface above the basin centre, and the darker depth layer below it. */
    private static final float WATER_Y = 0.30F, DEEP_Y = 0.22F;
    /** Steady sweep speed: one full pass across the basin in this many ticks. */
    private static final float SWEEP_PASS_TICKS = 120F;
    /** Gantry beam height and the blade dipping into the water. */
    private static final float BEAM_Y = 0.44F, BLADE_TOP = 0.34F, BLADE_BOTTOM = 0.18F;

    /** Dirty inlet water (murky brown-green), the scum on it and the pale foam clumps. */
    private static final int[] DIRTY = { 98, 104, 62 }, SCUM = { 86, 78, 48 }, FOAM = { 206, 198, 170 };
    /** Edge of one cell of the water surface's colour grid (blocks): the water clears from the inlet to the far end. */
    private static final float CELL = 0.25F;
    /** Fallback water tint (vanilla default) when no biome is loaded. */
    private static final int FALLBACK_WATER = 0x3F76E4;

    private final float up;
    private final float back;
    private final float left;
    /** Basin half extents across (left) and along (back) the basin: the open water. */
    private final float hx;
    private final float hz;

    public FiltrationBasinRender(float up, float back, float left, float width, float length) {
        this.up = up;
        this.back = back;
        this.left = left;
        this.hx = width / 2F;
        this.hz = length / 2F;
    }

    /** Typed as GT's base class, so common code that builds a model never loads this client class. */
    public static DynamicRender<?, ?> create(float up, float back, float left, float width, float length) {
        return new FiltrationBasinRender(up, back, left, width, length);
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
        return new AABB(centre, centre).inflate(Math.max(hx, hz) + 1F);
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
        quad(water, mat, frame, -hx, DEEP_Y, -hz, hx, DEEP_Y, hz, wr / 2, wg / 2, wb / 2, 0.35F);
        float surfaceAlpha = working ? 0.50F + 0.06F * ripple : 0.34F + 0.04F * ripple;
        // murky at the inlet (the front, by the hatches), clearing toward the back, slowly drifting cloud
        int nx = Math.max(1, Math.round(2 * hx / CELL)), nz = Math.max(1, Math.round(2 * hz / CELL));
        float cw = 2 * hx / nx, cd = 2 * hz / nz;
        for (int i = 0; i < nx; i++) {
            for (int j = 0; j < nz; j++) {
                float x0 = -hx + i * cw, z0 = -hz + j * cd;
                float cx = x0 + cw / 2, cz = z0 + cd / 2;
                float dirt = Mth.clamp((hz - cz) / (2 * hz), 0F, 1F);
                float n = 0.5F + 0.25F * Mth.sin(cx * 2.3F + cz * 1.7F + (float) time / 90F)
                        + 0.25F * Mth.sin(cx * 1.1F - cz * 2.9F - (float) time / 70F);
                float k = Mth.clamp(dirt * (0.55F + 0.45F * n), 0F, 1F);
                float shade = 0.85F + 0.3F * n;
                int r = Mth.clamp((int) (Mth.lerp(k, wr, DIRTY[0]) * shade), 0, 255);
                int g = Mth.clamp((int) (Mth.lerp(k, wg, DIRTY[1]) * shade), 0, 255);
                int b = Mth.clamp((int) (Mth.lerp(k, wb, DIRTY[2]) * shade), 0, 255);
                quad(water, mat, frame, x0, WATER_Y, z0, x0 + cw, WATER_Y, z0 + cd, r, g, b,
                        Math.min(0.92F, surfaceAlpha + 0.28F * k));
            }
        }
        // travelling highlight while running: a bright band crossing front to back
        if (working) {
            float band = (float) ((time % 60) / 60);
            float bz = Mth.lerp(band, -hz, hz);
            quad(water, mat, frame, -hx, WATER_Y + 0.005F, bz - 0.12F, hx, WATER_Y + 0.005F, bz + 0.12F,
                    Math.min(255, wr + 110), Math.min(255, wg + 110), Math.min(255, wb + 110), 0.28F);
        }

        // --- skimmer: steady ping-pong sweep while running, parked at the back idle ---
        float sweepFront = -(hz - 0.3F), sweepBack = hz - 0.3F;
        float carriageZ = sweepBack;
        if (working) {
            float phase = (float) (time / SWEEP_PASS_TICKS) % 2.0F;
            float sweep = phase < 1.0F ? phase : 2.0F - phase;
            carriageZ = Mth.lerp(sweep, sweepFront, sweepBack);
        }

        // --- scum and foam floating on top: flat patches, thicker at the inlet, drifting, piling ahead of the blade ---
        float[][] debris = debrisFor(basinPos, hx, hz);
        for (int i = 0; i < debris.length; i++) {
            float bx = debris[i][0];
            float bz = debris[i][1];
            float size = debris[i][2];
            boolean foam = debris[i][3] > 0.5F;
            float phase = i * 1.1F;
            float driftX = 0.08F * Mth.sin((float) (time * 2 * Math.PI / 70) + phase * 2);
            float driftZ = 0.08F * Mth.cos((float) (time * 2 * Math.PI / 60) + phase);
            float dx = bx + driftX;
            float dz = bz + driftZ;
            if (working) {
                // the blade pushes what floats just ahead of it
                float ahead = dz - carriageZ;
                if (ahead > 0 && ahead < 0.5F) dz += (0.5F - ahead) * 0.6F;
            }
            float dy = WATER_Y + 0.006F;
            int[] c = foam ? FOAM : SCUM;
            quad(water, mat, frame, dx - size * 1.2F, dy, dz - size * 0.7F, dx + size * 1.2F, dy, dz + size * 0.7F,
                    c[0], c[1], c[2], foam ? 0.80F : 0.55F);
            quad(water, mat, frame, dx - size * 0.6F, dy + 0.002F, dz - size * 1.1F, dx + size * 0.5F, dy + 0.002F,
                    dz + size * 0.2F, c[0], c[1], c[2], foam ? 0.70F : 0.45F);
        }

        // --- skimmer arm: static side rails, moving gantry beam, posts, dipping blade ---
        // rails along both sides (always there)
        SolidBox.box(solid, mat, frame, -(hx - 0.05F), BEAM_Y - 0.05F, -hz, -(hx - 0.25F), BEAM_Y + 0.05F, hz, 0x4B5567);
        SolidBox.box(solid, mat, frame, (hx - 0.25F), BEAM_Y - 0.05F, -hz, (hx - 0.05F), BEAM_Y + 0.05F, hz, 0x4B5567);
        for (float tie = -(hz - 0.3F); tie <= hz - 0.29F; tie += 0.4F) {
            SolidBox.box(solid, mat, frame, -(hx - 0.05F), BEAM_Y - 0.06F, tie - 0.03F, -(hx - 0.25F), BEAM_Y - 0.02F,
                    tie + 0.03F, 0x6B7688);
            SolidBox.box(solid, mat, frame, (hx - 0.25F), BEAM_Y - 0.06F, tie - 0.03F, (hx - 0.05F), BEAM_Y - 0.02F,
                    tie + 0.03F, 0x6B7688);
        }
        // gantry beam across the basin at the carriage
        SolidBox.box(solid, mat, frame, -(hx - 0.05F), BEAM_Y - 0.06F, carriageZ - 0.09F, (hx - 0.05F), BEAM_Y + 0.06F,
                carriageZ + 0.09F, 0x8A94A6);
        // posts down to the blade at both ends
        SolidBox.box(solid, mat, frame, -(hx - 0.08F), BLADE_TOP, carriageZ - 0.06F, -(hx - 0.22F), BEAM_Y - 0.06F,
                carriageZ + 0.06F, 0x6B7688);
        SolidBox.box(solid, mat, frame, (hx - 0.22F), BLADE_TOP, carriageZ - 0.06F, (hx - 0.08F), BEAM_Y - 0.06F,
                carriageZ + 0.06F, 0x6B7688);
        // blade dipping into the water
        SolidBox.box(solid, mat, frame, -(hx - 0.15F), BLADE_BOTTOM, carriageZ - 0.05F, (hx - 0.15F), BLADE_TOP,
                carriageZ + 0.05F, working ? 0xB8C4D4 : 0x4B5567);
        if (working) {
            float blink = 0.45F + 0.25F * Mth.sin((float) (time * 2 * Math.PI / 30));
            quad(water, mat, frame, -0.2F, BEAM_Y + 0.065F, carriageZ - 0.06F, 0.2F, BEAM_Y + 0.065F,
                    carriageZ + 0.06F, 120, 200, 255, blink);
        }

        poseStack.popPose();
    }

    /** Basin centre in block-local coords (0.5 + up/back/left along the controller's axes). */
    private Frame frame(MetaMachine self) {
        boolean flipped = self instanceof MultiblockControllerMachine controller && controller.isFlipped();
        Direction upDir = RelativeDirection.UP.getRelative(self.getFrontFacing(), self.getUpwardsFacing(), flipped);
        Direction backDir = RelativeDirection.BACK.getRelative(self.getFrontFacing(), self.getUpwardsFacing(),
                flipped);
        Direction leftDir = RelativeDirection.LEFT.getRelative(self.getFrontFacing(), self.getUpwardsFacing(),
                flipped);
        float x = 0.5F + upDir.getStepX() * up + backDir.getStepX() * back + leftDir.getStepX() * left;
        float y = 0.5F + upDir.getStepY() * up + backDir.getStepY() * back + leftDir.getStepY() * left;
        float z = 0.5F + upDir.getStepZ() * up + backDir.getStepZ() * back + leftDir.getStepZ() * left;
        return new Frame(x, y, z, leftDir, upDir, backDir);
    }

    /**
     * This basin's debris: 10-20 scum and foam patches (scaled to the basin) on the water, seeded by the basin's position so every
     * plant floats its own mix but it never flickers. Each piece is x, z, size, kind (0 scum, 1 foam).
     */
    private static float[][] debrisFor(BlockPos basin, float hx, float hz) {
        Random rand = new Random(basin.asLong());
        int count = (int) ((10 + rand.nextInt(11)) * hx * hz / 2.25F);
        float[][] out = new float[count][];
        for (int i = 0; i < count; i++) {
            float x = (rand.nextFloat() * 2F - 1F) * (hx - 0.2F);
            // thickest at the front (the inlet): z from -hz at the front, squared toward the back
            float z = -(hz - 0.2F) + (float) Math.pow(rand.nextFloat(), 1.8F) * 2F * (hz - 0.2F);
            float size = 0.06F + rand.nextFloat() * 0.10F;
            out[i] = new float[] { x, z, size, rand.nextInt(3) == 0 ? 1F : 0F };
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
