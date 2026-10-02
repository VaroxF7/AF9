package com.af9.core.client.render;

import com.af9.core.AF9Core;
import com.af9.core.elevator.ISpaceElevatorMachine;

import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine;
import com.gregtechceu.gtceu.api.pattern.util.RelativeDirection;
import com.gregtechceu.gtceu.client.renderer.machine.DynamicRender;
import com.gregtechceu.gtceu.client.renderer.machine.DynamicRenderManager;
import com.gregtechceu.gtceu.client.renderer.machine.DynamicRenderType;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
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
import com.mojang.math.Axis;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import org.joml.Vector3f;

/**
 * The upper part of the Space Elevator: a platform that rides the top of the shaft, drawn while the structure is formed and
 * turning slowly around the cable ({@link #SPIN} degrees a tick, twice that while a run is on). It is a model made in code
 * from a few shapes (no model file): a hub that holds the cable, a ring with its light strips on eight spokes, eight posts with
 * lamps, six blue tank pods hanging off the ring and orange spotlights under it; above it the cable runs up into the sky
 * to a small station with solar wings (after GTNH's, whose climber rides the cable).
 * <p>
 * Every colour is a cell of a 4 x 4 palette texture ({@link #TEXTURE}); the glass of the pods is translucent, the lights are
 * drawn at full brightness (and a little brighter while a run is on).
 * <p>
 * Placed relative to the controller: {@code up} blocks above it and {@code back} behind it, so it turns with the controller's
 * facing; the cable goes {@code cable} blocks up from there (as far as the world's top). Model side:
 * {@link com.af9.core.machine.AF9MachineModels#workableCasingWithSpaceElevator}.
 */
@SuppressWarnings("removal") // new ResourceLocation(ns, path) is the only constructor on 1.20.1
public class SpaceElevatorRender extends DynamicRender<ISpaceElevatorMachine, SpaceElevatorRender> {

    // spotless:off
    public static final Codec<SpaceElevatorRender> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.FLOAT.fieldOf("up").forGetter(render -> render.up),
            Codec.FLOAT.fieldOf("back").forGetter(render -> render.back),
            Codec.FLOAT.fieldOf("cable").forGetter(render -> render.cable)
    ).apply(instance, SpaceElevatorRender::new));
    // spotless:on
    public static final DynamicRenderType<ISpaceElevatorMachine, SpaceElevatorRender> TYPE = new DynamicRenderType<>(
            CODEC);

    /** The palette: a 4 x 4 grid of 4-pixel cells, left to right and top to bottom (assets/af9/textures/entity). */
    public static final ResourceLocation TEXTURE = new ResourceLocation(AF9Core.MOD_ID,
            "textures/entity/space_elevator.png");
    private static final int DARK = 0, STEEL = 1, LIGHT_STEEL = 2, WHITE = 3, GLASS = 4, BRIGHT_BLUE = 5, CYAN = 6,
            ORANGE = 7, BLACK = 8, DEEP_BLUE = 9;

    /** Degrees of turn a tick: a full turn in a little under four minutes. */
    public static final float SPIN = 0.08F;
    private static final int FULL_BRIGHT = 0xF000F0;

    private final float up;
    private final float back;
    private final float cable;

    public SpaceElevatorRender(float up, float back, float cable) {
        this.up = up;
        this.back = back;
        this.cable = cable;
    }

    /** Typed as GT's base class, so common code that builds a model never loads this client class. */
    public static DynamicRender<?, ?> create(float up, float back, float cable) {
        return new SpaceElevatorRender(up, back, cable);
    }

    /** Client, before the models are built (mod construction). */
    public static void register() {
        DynamicRenderManager.register(new ResourceLocation(AF9Core.MOD_ID, "space_elevator"), TYPE);
    }

    @Override
    public DynamicRenderType<ISpaceElevatorMachine, SpaceElevatorRender> getType() {
        return TYPE;
    }

    @Override
    public int getViewDistance() {
        return 256;
    }

    /** Where the shaft's axis is at the platform, relative to the controller's position. */
    private Vector3f centre(MetaMachine self) {
        boolean flipped = self instanceof MultiblockControllerMachine controller && controller.isFlipped();
        Direction upDir = RelativeDirection.UP.getRelative(self.getFrontFacing(), self.getUpwardsFacing(), flipped);
        Direction backDir = RelativeDirection.BACK.getRelative(self.getFrontFacing(), self.getUpwardsFacing(), flipped);
        return new Vector3f(0.5F + upDir.getStepX() * up + backDir.getStepX() * back,
                0.5F + upDir.getStepY() * up + backDir.getStepY() * back,
                0.5F + upDir.getStepZ() * up + backDir.getStepZ() * back);
    }

    @Override
    public boolean shouldRender(ISpaceElevatorMachine machine, Vec3 cameraPos) {
        if (!machine.isElevatorFormed()) return false;
        BlockPos pos = machine.self().getPos();
        Vector3f c = centre(machine.self());
        return new Vec3(pos.getX() + c.x, pos.getY() + c.y, pos.getZ() + c.z).closerThan(cameraPos,
                getViewDistance());
    }

    /** The platform and the cable above it (the block entity would be culled with the controller's own box otherwise). */
    @Override
    public AABB getRenderBoundingBox(ISpaceElevatorMachine machine) {
        BlockPos pos = machine.self().getPos();
        Vector3f c = centre(machine.self());
        double x = pos.getX() + c.x, y = pos.getY() + c.y, z = pos.getZ() + c.z;
        return new AABB(pos).minmax(new AABB(x - 8, y - 4, z - 8, x + 8, y + cable + 3, z + 8));
    }

    @Override
    public void render(ISpaceElevatorMachine machine, float partialTick, PoseStack poseStack, MultiBufferSource buffer,
                       int packedLight, int packedOverlay) {
        if (!machine.isElevatorFormed()) return;
        MetaMachine self = machine.self();
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return;
        BlockPos pos = self.getPos();
        Vector3f c = centre(self);
        boolean working = machine.isElevatorWorking();
        float time = level.getGameTime() + partialTick;
        float angle = (time * SPIN * (working ? 2F : 1F)) % 360F;
        // the light of the sky at the platform: the controller's own light is far below it
        int light = LevelRenderer.getLightColor(level, new BlockPos(pos.getX() + Mth.floor(c.x),
                pos.getY() + Mth.floor(c.y), pos.getZ() + Mth.floor(c.z)));
        float glow = working ? 1F : 0.75F + 0.25F * Mth.sin(time * 0.05F);
        VertexConsumer solid = buffer.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        VertexConsumer glass = buffer.getBuffer(RenderType.entityTranslucent(TEXTURE));
        Shapes shapes = new Shapes(poseStack);

        poseStack.pushPose();
        poseStack.translate(c.x, c.y, c.z);
        // the cable, and the station on its end
        float top = Math.min(cable, level.getMaxBuildHeight() - 2 - (pos.getY() + c.y));
        if (top > 3) cableAndStation(shapes, solid, light, top);
        // the platform
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(angle));
        platform(shapes, solid, glass, poseStack, light, glow);
        poseStack.popPose();
        poseStack.popPose();
    }

    //////////////////////////////////////
    // *********** The model **********//
    //////////////////////////////////////

    /** The cable (black, with a thin light core and a node every 16 blocks) up to the station. */
    private static void cableAndStation(Shapes s, VertexConsumer solid, int light, float top) {
        s.prism(solid, 0.2F, 0, top, 8, BLACK, light, 255);
        s.prism(solid, 0.07F, 0, top, 6, CYAN, FULL_BRIGHT, 255);
        for (float y = 16; y < top - 2; y += 16) s.prism(solid, 0.32F, y - 0.25F, y + 0.25F, 8, STEEL, light, 255);
        // the station: a drum on the cable with a light band, and two solar wings
        s.prism(solid, 0.9F, top - 0.7F, top + 0.7F, 10, DARK, light, 255);
        s.prism(solid, 0.95F, top - 0.12F, top + 0.12F, 10, CYAN, FULL_BRIGHT, 255);
        s.prism(solid, 0.45F, top + 0.7F, top + 1.6F, 8, STEEL, light, 255);
        s.box(solid, -3.4F, top - 0.05F, -0.6F, -0.9F, top + 0.05F, 0.6F, DEEP_BLUE, light, 255);
        s.box(solid, 0.9F, top - 0.05F, -0.6F, 3.4F, top + 0.05F, 0.6F, DEEP_BLUE, light, 255);
        s.box(solid, -0.9F, top - 0.04F, -0.06F, 0.9F, top + 0.04F, 0.06F, LIGHT_STEEL, light, 255);
    }

    /** The platform around the cable, turning: hub, ring, spokes, posts, pods, lights. */
    private static void platform(Shapes s, VertexConsumer solid, VertexConsumer glass, PoseStack pose, int light,
                                 float glow) {
        int lamps = glow >= 1F ? FULL_BRIGHT : 0xF000D0;
        // the hub that holds the cable
        s.prism(solid, 1.15F, -1.4F, 1.4F, 12, DARK, light, 255);
        s.prism(solid, 1.2F, -0.15F, 0.15F, 12, CYAN, lamps, 255);
        s.prism(solid, 1.32F, 1.4F, 1.75F, 12, STEEL, light, 255);
        s.prism(solid, 1.32F, -1.75F, -1.4F, 12, STEEL, light, 255);
        // the ring and its light strips, above and below
        s.ringPrism(solid, 4.0F, 4.7F, -0.3F, 0.3F, 24, STEEL, light, 255);
        s.ringPrism(solid, 4.15F, 4.55F, 0.3F, 0.34F, 24, CYAN, lamps, 255);
        s.ringPrism(solid, 4.15F, 4.55F, -0.34F, -0.3F, 24, CYAN, lamps, 255);
        // eight spokes: four heavy, four light
        for (int i = 0; i < 8; i++) {
            pose.pushPose();
            pose.mulPose(Axis.YP.rotationDegrees(i * 45F));
            boolean heavy = i % 2 == 0;
            float w = heavy ? 0.2F : 0.11F, h = heavy ? 0.15F : 0.1F;
            s.box(solid, 1.2F, -h, -w, 4.05F, h, w, heavy ? DARK : STEEL, light, 255);
            // a post with a lamp on top, on the ring
            s.box(solid, 4.2F, 0.3F, -0.14F, 4.48F, 1.5F, 0.14F, STEEL, light, 255);
            s.box(solid, 4.24F, 1.5F, -0.1F, 4.44F, 1.7F, 0.1F, WHITE, lamps, 255);
            // a spotlight under it
            if (heavy) s.box(solid, 4.25F, -0.55F, -0.1F, 4.45F, -0.3F, 0.1F, ORANGE, lamps, 255);
            pose.popPose();
        }
        // six tank pods on the outside of the ring: glass round a bright core, capped in steel, on arms
        for (int i = 0; i < 6; i++) {
            pose.pushPose();
            pose.mulPose(Axis.YP.rotationDegrees(30F + i * 60F));
            s.box(solid, 4.6F, -0.12F, -0.18F, 4.95F, 0.12F, 0.18F, DARK, light, 255);
            pose.translate(5.55F, 0, 0);
            s.prism(solid, 0.3F, -1.45F, 1.45F, 8, BRIGHT_BLUE, FULL_BRIGHT, 255);
            s.prism(solid, 0.72F, 1.6F, 1.95F, 10, DARK, light, 255);
            s.prism(solid, 0.72F, -1.95F, -1.6F, 10, DARK, light, 255);
            s.prism(glass, 0.62F, -1.6F, 1.6F, 10, GLASS, FULL_BRIGHT, 150);
            pose.popPose();
        }
    }

    /** Shapes drawn into a vertex consumer at the pose stack's current pose. */
    private static final class Shapes {

        private final PoseStack stack;

        Shapes(PoseStack stack) {
            this.stack = stack;
        }

        /** A quad of the palette cell, the corners counter-clockwise seen from outside. */
        void quad(VertexConsumer c, int cell, int light, int alpha, float x1, float y1, float z1, float x2, float y2,
                  float z2, float x3, float y3, float z3, float x4, float y4, float z4) {
            float ux = x2 - x1, uy = y2 - y1, uz = z2 - z1, vx = x4 - x1, vy = y4 - y1, vz = z4 - z1;
            float nx = uy * vz - uz * vy, ny = uz * vx - ux * vz, nz = ux * vy - uy * vx;
            float len = Mth.sqrt(nx * nx + ny * ny + nz * nz);
            if (len < 1.0E-6F) return;
            nx /= len;
            ny /= len;
            nz /= len;
            PoseStack.Pose pose = stack.last();
            float u = ((cell % 4) * 4 + 2) / 16F, v = ((cell / 4) * 4 + 2) / 16F;
            vertex(c, pose, x1, y1, z1, u, v, light, alpha, nx, ny, nz);
            vertex(c, pose, x2, y2, z2, u, v, light, alpha, nx, ny, nz);
            vertex(c, pose, x3, y3, z3, u, v, light, alpha, nx, ny, nz);
            vertex(c, pose, x4, y4, z4, u, v, light, alpha, nx, ny, nz);
        }

        private static void vertex(VertexConsumer c, PoseStack.Pose pose, float x, float y, float z, float u, float v,
                                   int light, int alpha, float nx, float ny, float nz) {
            c.vertex(pose.pose(), x, y, z).color(255, 255, 255, alpha).uv(u, v)
                    .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(pose.normal(), nx, ny, nz).endVertex();
        }

        /** An axis-aligned box. */
        void box(VertexConsumer c, float x0, float y0, float z0, float x1, float y1, float z1, int cell, int light,
                 int alpha) {
            quad(c, cell, light, alpha, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1);   // +z
            quad(c, cell, light, alpha, x1, y0, z0, x0, y0, z0, x0, y1, z0, x1, y1, z0);   // -z
            quad(c, cell, light, alpha, x1, y0, z1, x1, y0, z0, x1, y1, z0, x1, y1, z1);   // +x
            quad(c, cell, light, alpha, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0);   // -x
            quad(c, cell, light, alpha, x0, y1, z1, x1, y1, z1, x1, y1, z0, x0, y1, z0);   // +y
            quad(c, cell, light, alpha, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1);   // -y
        }

        /** A prism of {@code sides} sides round the Y axis (a cylinder), capped. */
        void prism(VertexConsumer c, float radius, float y0, float y1, int sides, int cell, int light, int alpha) {
            for (int i = 0; i < sides; i++) {
                float a0 = Mth.TWO_PI * i / sides, a1 = Mth.TWO_PI * (i + 1) / sides;
                float x0 = Mth.cos(a0) * radius, z0 = Mth.sin(a0) * radius;
                float x1 = Mth.cos(a1) * radius, z1 = Mth.sin(a1) * radius;
                quad(c, cell, light, alpha, x0, y0, z0, x1, y0, z1, x1, y1, z1, x0, y1, z0);   // the side
                quad(c, cell, light, alpha, x0, y1, z0, x1, y1, z1, 0, y1, 0, 0, y1, 0);       // the top
                quad(c, cell, light, alpha, x1, y0, z1, x0, y0, z0, 0, y0, 0, 0, y0, 0);       // the bottom
            }
        }

        /** A ring (a prism with a hole), sides and faces. */
        void ringPrism(VertexConsumer c, float inner, float outer, float y0, float y1, int sides, int cell, int light,
                       int alpha) {
            for (int i = 0; i < sides; i++) {
                float a0 = Mth.TWO_PI * i / sides, a1 = Mth.TWO_PI * (i + 1) / sides;
                float c0 = Mth.cos(a0), s0 = Mth.sin(a0), c1 = Mth.cos(a1), s1 = Mth.sin(a1);
                quad(c, cell, light, alpha, c0 * outer, y0, s0 * outer, c1 * outer, y0, s1 * outer, c1 * outer, y1,
                        s1 * outer, c0 * outer, y1, s0 * outer);                                       // outside
                quad(c, cell, light, alpha, c1 * inner, y0, s1 * inner, c0 * inner, y0, s0 * inner, c0 * inner, y1,
                        s0 * inner, c1 * inner, y1, s1 * inner);                                       // inside
                quad(c, cell, light, alpha, c0 * inner, y1, s0 * inner, c0 * outer, y1, s0 * outer, c1 * outer, y1,
                        s1 * outer, c1 * inner, y1, s1 * inner);                                       // top
                quad(c, cell, light, alpha, c1 * inner, y0, s1 * inner, c1 * outer, y0, s1 * outer, c0 * outer, y0,
                        s0 * outer, c0 * inner, y0, s0 * inner);                                       // bottom
            }
        }
    }
}
