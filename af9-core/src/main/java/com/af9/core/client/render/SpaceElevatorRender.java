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
 * The upper part of the Space Elevator, drawn while the structure is formed, as GTNH draws its elevator
 * (gtnhintergalactic's RenderSpaceElevatorCable; the shapes here are made by hand after pictures of it, GTNH's own model
 * and textures are not used):
 * <ul>
 * <li>the <b>cable</b>: four bands wound round each other like a rope, from the floor of the tower's shaft
 * ({@link #CABLE_BELOW} blocks under the Space Elevator Cable block) {@link #CABLE_HEIGHT} blocks up, far past the
 * world's top; a blue light runs up it every three seconds, lighting the lamps of the bands it passes;</li>
 * <li>the <b>climber</b>: a wheel round the cable with a hub and four spokes, a white pod on the end of three of them and a
 * rack of six blue tanks on the fourth. It rests {@link #CLIMBER_REST} blocks above the cable block and rides the cable
 * as the machine says ({@link ISpaceElevatorMachine#climberHeight}, {@link ISpaceElevatorMachine#climberTurn}).</li>
 * </ul>
 * Both are drawn at full brightness, as GTNH's are. The climber's colours are cells of a 4 x 4 palette texture
 * ({@link #TEXTURE}); a band of the cable is a tile of {@link #STRAND}.
 * <p>
 * Placed relative to the controller: the cable block is {@code up} blocks above it and {@code back} behind it (so the model
 * turns with the controller's facing). Model side: {@link com.af9.core.machine.AF9MachineModels#workableCasingWithSpaceElevator}.
 */
@SuppressWarnings("removal") // new ResourceLocation(ns, path) is the only constructor on 1.20.1
public class SpaceElevatorRender extends DynamicRender<ISpaceElevatorMachine, SpaceElevatorRender> {

    // spotless:off
    public static final Codec<SpaceElevatorRender> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.FLOAT.fieldOf("up").forGetter(render -> render.up),
            Codec.FLOAT.fieldOf("back").forGetter(render -> render.back)
    ).apply(instance, SpaceElevatorRender::new));
    // spotless:on
    public static final DynamicRenderType<ISpaceElevatorMachine, SpaceElevatorRender> TYPE = new DynamicRenderType<>(
            CODEC);

    /** The climber's palette: a 4 x 4 grid of 4-pixel cells, left to right and top to bottom (assets/af9/textures/entity). */
    public static final ResourceLocation TEXTURE = new ResourceLocation(AF9Core.MOD_ID,
            "textures/entity/space_elevator.png");
    /** One tile of a band of the cable; its middle 2 x 2 pixels are the lamp the running light lights. */
    public static final ResourceLocation STRAND = new ResourceLocation(AF9Core.MOD_ID,
            "textures/entity/space_elevator_strand.png");
    private static final int DARK = 0, LIGHT_STEEL = 2, WHITE = 3, TANK = 4, TANK_DARK = 5, GOLD = 7, BLACK = 8, CAP = 9,
            NOZZLE = 10;
    private static final int FULL_BRIGHT = 0xF000F0;

    /** GTNH's numbers: the cable starts this far under its block (the shaft's floor) and is this long. */
    public static final float CABLE_BELOW = 23F, CABLE_HEIGHT = 512F;
    /**
     * GTNH's numbers: the climber rests this far above the cable block; where that would be under y {@link #CLIMBER_FLOOR}
     * (a cable block lower than 50), it rests {@link #CLIMBER_REST_LOW} above it instead.
     */
    public static final float CLIMBER_REST = 50F, CLIMBER_REST_LOW = 100F, CLIMBER_FLOOR = 100F;

    /**
     * The rope, as GTNH winds it: a band climbs round an octagon (half-width {@code LONG}, its sides {@code 2 * SHORT}),
     * {@code RISE} a side, so once round in {@code PITCH}; a band is {@code BAND} high, and four of them a quarter turn
     * apart cover the rope.
     */
    private static final float LONG = (1F + Mth.SQRT_OF_TWO) / 5.4F, SHORT = 1F / 5.4F, RISE = 2F / 5.4F,
            PITCH = 8F * RISE, BAND = 0.75F;
    private static final float[] EDGE_X = { LONG, LONG, SHORT, -SHORT, -LONG, -LONG, -SHORT, SHORT };
    private static final float[] EDGE_Z = { SHORT, -SHORT, -LONG, -LONG, -SHORT, SHORT, LONG, LONG };
    private static final int TURNS = Mth.ceil(CABLE_HEIGHT / PITCH);
    /** Turns of the rope further from the camera than this are drawn as a plain eight-sided column. */
    private static final double FINE_DISTANCE = 96;
    /** The running light: ticks it takes from the cable's foot to its top, and how far up and down it shines. */
    private static final float LIGHT_PERIOD = 60F, LIGHT_REACH = 0.03F * CABLE_HEIGHT;
    /** The lamp of a tile: its middle eighth, each way. */
    private static final float LAMP_MIN = 7F / 16F, LAMP_MAX = 9F / 16F;

    private final float up;
    private final float back;

    public SpaceElevatorRender(float up, float back) {
        this.up = up;
        this.back = back;
    }

    /** Typed as GT's base class, so common code that builds a model never loads this client class. */
    public static DynamicRender<?, ?> create(float up, float back) {
        return new SpaceElevatorRender(up, back);
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
        return 288;
    }

    /** The middle of the cable block, relative to the controller's position. */
    private Vector3f centre(MetaMachine self) {
        boolean flipped = self instanceof MultiblockControllerMachine controller && controller.isFlipped();
        Direction upDir = RelativeDirection.UP.getRelative(self.getFrontFacing(), self.getUpwardsFacing(), flipped);
        Direction backDir = RelativeDirection.BACK.getRelative(self.getFrontFacing(), self.getUpwardsFacing(), flipped);
        return new Vector3f(0.5F + upDir.getStepX() * up + backDir.getStepX() * back,
                0.5F + upDir.getStepY() * up + backDir.getStepY() * back,
                0.5F + upDir.getStepZ() * up + backDir.getStepZ() * back);
    }

    /** By the distance to the cable, level: it is seen from far below and from far above. */
    @Override
    public boolean shouldRender(ISpaceElevatorMachine machine, Vec3 cameraPos) {
        if (!machine.isElevatorFormed()) return false;
        BlockPos pos = machine.self().getPos();
        Vector3f c = centre(machine.self());
        double dx = pos.getX() + c.x - cameraPos.x, dz = pos.getZ() + c.z - cameraPos.z;
        return dx * dx + dz * dz < (double) getViewDistance() * getViewDistance();
    }

    /**
     * Drawn whether or not the controller's own chunk section is on the screen. A block entity is otherwise only drawn
     * with its section, and the cable and the climber stand far above the controller: looking up at them, the
     * controller is out of view and they would be gone.
     */
    @Override
    public boolean shouldRenderOffScreen(ISpaceElevatorMachine machine) {
        return true;
    }

    /** The cable and the climber's whole way: what the frustum check asks, instead of the controller's own box. */
    @Override
    public AABB getRenderBoundingBox(ISpaceElevatorMachine machine) {
        BlockPos pos = machine.self().getPos();
        Vector3f c = centre(machine.self());
        double x = pos.getX() + c.x, y = pos.getY() + c.y, z = pos.getZ() + c.z;
        return new AABB(pos).minmax(new AABB(x - 14, y - CABLE_BELOW - 1, z - 14, x + 14, y + CABLE_HEIGHT, z + 14));
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
        // the cable block: its level in the world, and the camera from its middle
        int cableY = pos.getY() + Mth.floor(c.y);
        Vec3 camera = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        double cameraX = camera.x - (pos.getX() + c.x), cameraY = camera.y - (pos.getY() + c.y),
                cameraZ = camera.z - (pos.getZ() + c.z);

        poseStack.pushPose();
        poseStack.translate(c.x, c.y, c.z);
        float light = ((level.getGameTime() % (long) LIGHT_PERIOD) + partialTick) / LIGHT_PERIOD * CABLE_HEIGHT;
        cable(poseStack, buffer, cameraX * cameraX + cameraZ * cameraZ, cameraY, light);

        float rest = cableY + CLIMBER_REST < CLIMBER_FLOOR ? CLIMBER_REST_LOW : CLIMBER_REST;
        poseStack.pushPose();
        poseStack.translate(0, rest + machine.climberHeight(partialTick), 0);
        poseStack.mulPose(Axis.YP.rotationDegrees(machine.climberTurn(partialTick)));
        climber(new Shapes(poseStack), buffer.getBuffer(RenderType.entityCutoutNoCull(TEXTURE)), poseStack);
        poseStack.popPose();
        poseStack.popPose();
    }

    //////////////////////////////////////
    // *********** The cable **********//
    //////////////////////////////////////

    /**
     * The rope, turn by turn from the shaft's floor up: wound bands near the camera, a plain column further off, and
     * the lamps of the turns the running light is passing.
     *
     * @param levelSquare the camera's level distance from the cable, squared
     * @param cameraY     the camera's height above the middle of the cable block
     * @param light       how far up the cable the running light is
     */
    private static void cable(PoseStack poseStack, MultiBufferSource buffer, double levelSquare, double cameraY,
                              float light) {
        PoseStack.Pose pose = poseStack.last();
        VertexConsumer strand = buffer.getBuffer(RenderType.entityCutoutNoCull(STRAND));
        float bottom = -0.5F - CABLE_BELOW;
        boolean[] fine = new boolean[TURNS];
        for (int turn = 0; turn < TURNS; turn++) {
            float y = bottom + turn * PITCH;
            double dy = cameraY - (y + PITCH / 2);
            fine[turn] = levelSquare + dy * dy < FINE_DISTANCE * FINE_DISTANCE;
            if (fine[turn]) {
                wound(strand, pose, y);
            } else {
                column(strand, pose, y);
            }
        }
        // the lamps: light added on top of the bands, strongest where the running light is
        if (IrisCompat.renderingShadowPass()) return;
        VertexConsumer glow = buffer.getBuffer(AF9RenderTypes.LIGHTNING);
        for (int turn = 0; turn < TURNS; turn++) {
            if (!fine[turn]) continue;
            float away = Math.abs(light - turn * PITCH);
            if (away > LIGHT_REACH) continue;
            float glare = Mth.cos(away / LIGHT_REACH * Mth.HALF_PI);
            lamps(glow, pose, bottom + turn * PITCH, (int) (glare * glare * 255F));
        }
    }

    /** One turn of the four bands from height {@code y}: 32 slanted tiles. */
    private static void wound(VertexConsumer c, PoseStack.Pose pose, float y) {
        for (int band = 0; band < 4; band++) {
            for (int side = 0; side < 8; side++) {
                int from = (side + 2 * band) & 7, to = (from + 1) & 7;
                float low = y + RISE * side, high = low + RISE;
                float nx = EDGE_X[from] + EDGE_X[to], nz = EDGE_Z[from] + EDGE_Z[to];
                float length = Mth.sqrt(nx * nx + nz * nz);
                nx /= length;
                nz /= length;
                vertex(c, pose, EDGE_X[to], high, EDGE_Z[to], 0F, 1F, nx, nz);
                vertex(c, pose, EDGE_X[to], high + BAND, EDGE_Z[to], 0F, 0F, nx, nz);
                vertex(c, pose, EDGE_X[from], low + BAND, EDGE_Z[from], 1F, 0F, nx, nz);
                vertex(c, pose, EDGE_X[from], low, EDGE_Z[from], 1F, 1F, nx, nz);
            }
        }
    }

    /** One turn's height of the rope as a plain eight-sided column, for far away. */
    private static void column(VertexConsumer c, PoseStack.Pose pose, float y) {
        for (int from = 0; from < 8; from++) {
            int to = (from + 1) & 7;
            float nx = EDGE_X[from] + EDGE_X[to], nz = EDGE_Z[from] + EDGE_Z[to];
            float length = Mth.sqrt(nx * nx + nz * nz);
            nx /= length;
            nz /= length;
            vertex(c, pose, EDGE_X[to], y, EDGE_Z[to], 0F, 1F, nx, nz);
            vertex(c, pose, EDGE_X[to], y + PITCH, EDGE_Z[to], 0F, 0F, nx, nz);
            vertex(c, pose, EDGE_X[from], y + PITCH, EDGE_Z[from], 1F, 0F, nx, nz);
            vertex(c, pose, EDGE_X[from], y, EDGE_Z[from], 1F, 1F, nx, nz);
        }
    }

    /** The lamps of one turn's 32 tiles, lit: GTNH's blue (0, 0.65, 1), a hair outside the bands. */
    private static void lamps(VertexConsumer c, PoseStack.Pose pose, float y, int alpha) {
        if (alpha <= 0) return;
        for (int band = 0; band < 4; band++) {
            for (int side = 0; side < 8; side++) {
                int from = (side + 2 * band) & 7, to = (from + 1) & 7;
                float nx = EDGE_X[from] + EDGE_X[to], nz = EDGE_Z[from] + EDGE_Z[to];
                float length = Mth.sqrt(nx * nx + nz * nz);
                float ox = nx / length * 0.004F, oz = nz / length * 0.004F;
                // along the tile from `to` (0) to `from` (1); the tile's foot falls by RISE on the way
                float x0 = Mth.lerp(LAMP_MIN, EDGE_X[to], EDGE_X[from]) + ox, x1 = Mth.lerp(LAMP_MAX, EDGE_X[to], EDGE_X[from]) + ox;
                float z0 = Mth.lerp(LAMP_MIN, EDGE_Z[to], EDGE_Z[from]) + oz, z1 = Mth.lerp(LAMP_MAX, EDGE_Z[to], EDGE_Z[from]) + oz;
                float foot0 = y + RISE * (side + 1 - LAMP_MIN), foot1 = y + RISE * (side + 1 - LAMP_MAX);
                float bottom = BAND * (1F - LAMP_MAX), top = BAND * (1F - LAMP_MIN);
                c.vertex(pose.pose(), x0, foot0 + bottom, z0).color(0, 166, 255, alpha).endVertex();
                c.vertex(pose.pose(), x0, foot0 + top, z0).color(0, 166, 255, alpha).endVertex();
                c.vertex(pose.pose(), x1, foot1 + top, z1).color(0, 166, 255, alpha).endVertex();
                c.vertex(pose.pose(), x1, foot1 + bottom, z1).color(0, 166, 255, alpha).endVertex();
            }
        }
    }

    private static void vertex(VertexConsumer c, PoseStack.Pose pose, float x, float y, float z, float u, float v,
                               float nx, float nz) {
        c.vertex(pose.pose(), x, y, z).color(255, 255, 255, 255).uv(u, v).overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(FULL_BRIGHT).normal(pose.normal(), nx, 0F, nz).endVertex();
    }

    //////////////////////////////////////
    // ********** The climber *********//
    //////////////////////////////////////

    /** The climber round the cable: hub, wheel, four spokes, three pods and the tanks (about 23 blocks across). */
    private static void climber(Shapes s, VertexConsumer c, PoseStack pose) {
        // the hub that holds the cable, a dark collar at each end
        s.prism(c, 1.2F, -1.5F, 1.5F, 12, LIGHT_STEEL, LIGHT_STEEL);
        s.prism(c, 1.45F, 1.5F, 1.9F, 12, DARK, DARK);
        s.prism(c, 1.45F, -1.9F, -1.5F, 12, DARK, DARK);
        // the wheel: dark, blue on top, gold underneath
        s.ring(c, 5F, 6.2F, -0.5F, 0.5F, 32, DARK, TANK, GOLD);
        // four spokes through the wheel: a white pod stands on the end of three, the tanks hang on the fourth
        for (int spoke = 0; spoke < 4; spoke++) {
            pose.pushPose();
            pose.mulPose(Axis.YP.rotationDegrees(45F + spoke * 90F));
            s.box(c, 1.2F, -0.2F, -0.2F, spoke < 3 ? 9.6F : 10.6F, 0.2F, 0.2F, DARK);
            if (spoke < 3) {
                pose.translate(9.9F, 0, 0);
                s.prism(c, 0.55F, -0.6F, 0F, 8, BLACK, BLACK);
                s.prism(c, 0.95F, 0F, 6.2F, 10, WHITE, WHITE);
                s.prism(c, 0.65F, 6.2F, 6.6F, 10, WHITE, WHITE);
            } else {
                tanks(s, c, pose);
            }
            pose.popPose();
        }
    }

    /** Six blue tanks in two rows of three on two bars across a spoke, close by the wheel; a grey cap with a nozzle under each. */
    private static void tanks(Shapes s, VertexConsumer c, PoseStack pose) {
        s.box(c, 7.7F, -0.2F, -2.9F, 8.1F, 0.2F, 2.9F, DARK);
        s.box(c, 10.2F, -0.2F, -2.9F, 10.6F, 0.2F, 2.9F, DARK);
        for (int row = 0; row < 2; row++) {
            for (int column = -1; column <= 1; column++) {
                pose.pushPose();
                pose.translate(7.9F + row * 2.5F, 0, column * 2.5F);
                s.prism(c, 1.15F, -0.75F, 6.3F, 10, TANK, TANK_DARK);
                s.prism(c, 0.95F, -1.35F, -1.05F, 10, CAP, CAP);
                s.prism(c, 0.5F, -1.6F, -1.35F, 8, NOZZLE, NOZZLE);
                pose.popPose();
            }
        }
    }

    /** Shapes of palette cells, drawn at full brightness into a vertex consumer at the pose stack's current pose. */
    private static final class Shapes {

        private final PoseStack stack;

        Shapes(PoseStack stack) {
            this.stack = stack;
        }

        /** A quad of the palette cell, the corners counter-clockwise seen from outside. */
        void quad(VertexConsumer c, int cell, float x1, float y1, float z1, float x2, float y2, float z2, float x3,
                  float y3, float z3, float x4, float y4, float z4) {
            float ux = x2 - x1, uy = y2 - y1, uz = z2 - z1, vx = x4 - x1, vy = y4 - y1, vz = z4 - z1;
            float nx = uy * vz - uz * vy, ny = uz * vx - ux * vz, nz = ux * vy - uy * vx;
            float len = Mth.sqrt(nx * nx + ny * ny + nz * nz);
            if (len < 1.0E-6F) return;
            nx /= len;
            ny /= len;
            nz /= len;
            PoseStack.Pose pose = stack.last();
            float u = ((cell % 4) * 4 + 2) / 16F, v = ((cell / 4) * 4 + 2) / 16F;
            vertex(c, pose, x1, y1, z1, u, v, nx, ny, nz);
            vertex(c, pose, x2, y2, z2, u, v, nx, ny, nz);
            vertex(c, pose, x3, y3, z3, u, v, nx, ny, nz);
            vertex(c, pose, x4, y4, z4, u, v, nx, ny, nz);
        }

        private static void vertex(VertexConsumer c, PoseStack.Pose pose, float x, float y, float z, float u, float v,
                                   float nx, float ny, float nz) {
            c.vertex(pose.pose(), x, y, z).color(255, 255, 255, 255).uv(u, v)
                    .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(FULL_BRIGHT).normal(pose.normal(), nx, ny, nz)
                    .endVertex();
        }

        /** An axis-aligned box. */
        void box(VertexConsumer c, float x0, float y0, float z0, float x1, float y1, float z1, int cell) {
            quad(c, cell, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1);   // +z
            quad(c, cell, x1, y0, z0, x0, y0, z0, x0, y1, z0, x1, y1, z0);   // -z
            quad(c, cell, x1, y0, z1, x1, y0, z0, x1, y1, z0, x1, y1, z1);   // +x
            quad(c, cell, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0);   // -x
            quad(c, cell, x0, y1, z1, x1, y1, z1, x1, y1, z0, x0, y1, z0);   // +y
            quad(c, cell, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1);   // -y
        }

        /** A prism of {@code sides} sides round the Y axis (a cylinder): its side in one cell, both ends in another. */
        void prism(VertexConsumer c, float radius, float y0, float y1, int sides, int cell, int ends) {
            for (int i = 0; i < sides; i++) {
                float a0 = Mth.TWO_PI * i / sides, a1 = Mth.TWO_PI * (i + 1) / sides;
                float x0 = Mth.cos(a0) * radius, z0 = Mth.sin(a0) * radius;
                float x1 = Mth.cos(a1) * radius, z1 = Mth.sin(a1) * radius;
                quad(c, cell, x1, y0, z1, x0, y0, z0, x0, y1, z0, x1, y1, z1);   // the side
                quad(c, ends, x1, y1, z1, x0, y1, z0, 0, y1, 0, 0, y1, 0);       // the top
                quad(c, ends, x0, y0, z0, x1, y0, z1, 0, y0, 0, 0, y0, 0);       // the bottom
            }
        }

        /** A ring (a prism with a hole): both sides in one cell, the top and the bottom each in their own. */
        void ring(VertexConsumer c, float inner, float outer, float y0, float y1, int sides, int cell, int top,
                  int bottom) {
            for (int i = 0; i < sides; i++) {
                float a0 = Mth.TWO_PI * i / sides, a1 = Mth.TWO_PI * (i + 1) / sides;
                float c0 = Mth.cos(a0), s0 = Mth.sin(a0), c1 = Mth.cos(a1), s1 = Mth.sin(a1);
                quad(c, cell, c1 * outer, y0, s1 * outer, c0 * outer, y0, s0 * outer, c0 * outer, y1, s0 * outer,
                        c1 * outer, y1, s1 * outer);                                                   // outside
                quad(c, cell, c0 * inner, y0, s0 * inner, c1 * inner, y0, s1 * inner, c1 * inner, y1, s1 * inner,
                        c0 * inner, y1, s0 * inner);                                                   // inside
                quad(c, top, c0 * inner, y1, s0 * inner, c1 * inner, y1, s1 * inner, c1 * outer, y1, s1 * outer,
                        c0 * outer, y1, s0 * outer);                                                   // top
                quad(c, bottom, c0 * outer, y0, s0 * outer, c1 * outer, y0, s1 * outer, c1 * inner, y0, s1 * inner,
                        c0 * inner, y0, s0 * inner);                                                   // bottom
            }
        }
    }
}
