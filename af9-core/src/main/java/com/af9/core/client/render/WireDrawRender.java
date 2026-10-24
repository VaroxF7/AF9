package com.af9.core.client.render;

import com.af9.core.AF9Core;
import com.af9.core.machine.IWiremillMachine;

import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine;
import com.gregtechceu.gtceu.api.pattern.util.RelativeDirection;
import com.gregtechceu.gtceu.client.renderer.machine.DynamicRender;
import com.gregtechceu.gtceu.client.renderer.machine.DynamicRenderManager;
import com.gregtechceu.gtceu.client.renderer.machine.DynamicRenderType;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
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
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import java.util.HashMap;
import java.util.Map;

/**
 * The Large Wiremill's wire drawing machine, a cabling machine like the real one, in the glass channel of its 9 x 4 x 3
 * structure (kubejs/startup_scripts/gtceu/large_wiremill.js): a 4-long, one-block-wide air channel between the left
 * casing wall and the die plate of steel frames. Two solid orange discs on six rods carry six white bobbins; a strand
 * runs from each bobbin to a closing die on the axis, where they come together into one wire that runs on through the
 * die in the plate (the hole in the middle of the frames); behind the discs it goes through to the take-up spool at the
 * casing wall on the far side. While a recipe runs the carrier turns about the axis and the wire feeds (light and dark
 * stripes run towards the spool); it eases up to speed and coasts down. The wire is one unbroken line from the die
 * plate's right face (where the casing box starts) into the spool: no gap between the casings, the die and the spool.
 * <p>
 * Everything is drawn in "structure columns": the machine's axis runs along the controller's left (the pattern's
 * column direction); the controller is column 7, aisle 2 (the front), row 1, and the axis is the middle of the
 * channel (aisle 1, row 1), one block behind the controller's aisle and half a block above its floor. Model side:
 * {@link com.af9.core.machine.AF9MachineModels#workableCasingWithWireDraw}. One render object serves every Large
 * Wiremill, so the spin is kept per controller position (capped).
 */
@SuppressWarnings("removal") // new ResourceLocation(ns, path) is the only constructor on 1.20.1
public class WireDrawRender extends DynamicRender<IWiremillMachine, WireDrawRender> {

    public static final Codec<WireDrawRender> CODEC = Codec.unit(WireDrawRender::new);
    public static final DynamicRenderType<IWiremillMachine, WireDrawRender> TYPE = new DynamicRenderType<>(CODEC);

    /** The controller's column in the pattern (its middle), and the axis: columns count along the left of the controller. */
    private static final float CONTROLLER_COLUMN = 7.5F;
    /** Axis height above the controller block's floor, and its distance behind the controller block's middle. */
    private static final float AXIS_UP = 0.5F;
    private static final float AXIS_BACK = 1.0F;
    /** The middle of the channel along the columns (for the render's bounds and light). */
    private static final float CHANNEL_MIDDLE = 3.0F;

    /** Turns per tick at full speed (one turn a second) and ticks to get up to speed / coast down. */
    private static final float SPEED = (float) (2 * Math.PI / 20.0);
    private static final float INERTIA_TICKS = 30F;
    /** Wire feed at full speed, blocks per tick (stripes run this fast). */
    private static final float FEED = 0.1F;
    private static final float STRIPE = 0.15F;

    // positions along the axis (structure columns; ax() makes them axis-relative)
    private static final float WALL = 6.0F;       // the right face of the die plate: the casing box starts here
    private static final float DIE = 5.5F;        // the die, in the middle of the frame plate (column 5)
    private static final float EYELET = 4.8F;     // the wire guide on its strut
    private static final float CLOSER = 4.0F;     // the closing die where the six strands meet
    private static final float DISC_FRONT = 3.2F; // the bobbin carrier's two discs
    private static final float DISC_REAR = 2.75F;
    private static final float SPOOL_RIGHT = 2.4F;
    private static final float SPOOL_LEFT = 1.4F;
    private static final float WALL_LEFT = 1.0F;  // the left casing wall's inner face
    private static final float FLOOR = -0.5F;     // the channel floor, relative to the axis

    private static final float WIRE = 0.02F;
    private static final float DISC_R = 0.46F;
    private static final float BOBBIN_R = 0.36F;
    private static final float WOUND_R = 0.28F;

    // colours (flat: the entity shader lights them by their normals)
    private static final int STEEL = 0x6C7480, DARK = 0x2F343C, DARKER = 0x1E2228, ORANGE = 0xD2691E;
    private static final int WHITE = 0xF0F0E8, BOBBIN_FLANGE = 0xD0D0C8, TUNGSTEN = 0x8E96A2;
    private static final int COPPER_A = 0xC9772F, COPPER_B = 0xA75F24, WIND_A = 0xB8662B, WIND_B = 0x93501D;

    /** Per controller position: the carrier's angle, speed (0 to 1 of full), the feed phase and when it was last advanced. */
    private final Map<BlockPos, Spin> spins = new HashMap<>();

    public WireDrawRender() {}

    /** Typed as GT's base class, so common code that builds a model never loads this client class. */
    public static DynamicRender<?, ?> create() {
        return new WireDrawRender();
    }

    /** Client, before the models are built (mod construction). */
    public static void register() {
        DynamicRenderManager.register(new ResourceLocation(AF9Core.MOD_ID, "wire_draw"), TYPE);
    }

    @Override
    public DynamicRenderType<IWiremillMachine, WireDrawRender> getType() {
        return TYPE;
    }

    @Override
    public int getViewDistance() {
        return 64;
    }

    private static Direction dir(MetaMachine self, RelativeDirection relative) {
        boolean flipped = self instanceof MultiblockControllerMachine controller && controller.isFlipped();
        return relative.getRelative(self.getFrontFacing(), self.getUpwardsFacing(), flipped);
    }

    /** The middle of the channel (column 3, one aisle behind the controller's) in the world. */
    private Vec3 centre(MetaMachine self) {
        Direction left = dir(self, RelativeDirection.LEFT);
        Direction back = dir(self, RelativeDirection.BACK);
        BlockPos pos = self.getPos();
        return new Vec3(pos.getX() + 0.5 + left.getStepX() * (CHANNEL_MIDDLE - CONTROLLER_COLUMN) + back.getStepX() * AXIS_BACK,
                pos.getY() + AXIS_UP, pos.getZ() + 0.5 + left.getStepZ() * (CHANNEL_MIDDLE - CONTROLLER_COLUMN) +
                        back.getStepZ() * AXIS_BACK);
    }

    @Override
    public boolean shouldRender(IWiremillMachine machine, Vec3 cameraPos) {
        return machine.wiremillFormed() && centre(machine.self()).closerThan(cameraPos, getViewDistance());
    }

    @Override
    public boolean shouldRenderOffScreen(IWiremillMachine machine) {
        return false;
    }

    @Override
    public AABB getRenderBoundingBox(IWiremillMachine machine) {
        Vec3 c = centre(machine.self());
        return new AABB(c.x - 5, c.y - 1.5, c.z - 5, c.x + 5, c.y + 1.5, c.z + 5);
    }

    private static float ax(float column) {
        return column - CONTROLLER_COLUMN;
    }

    /** The frame whose x axis is {@code left}, y is up and z is {@code back}. */
    private static Matrix4f basis(Direction left, Direction back) {
        return new Matrix4f(
                left.getStepX(), 0, left.getStepZ(), 0,
                0, 1, 0, 0,
                back.getStepX(), 0, back.getStepZ(), 0,
                0, 0, 0, 1);
    }

    @Override
    public void render(IWiremillMachine machine, float partialTick, PoseStack poseStack, MultiBufferSource buffer,
                       int packedLight, int packedOverlay) {
        if (!machine.wiremillFormed()) return;
        if (IrisCompat.renderingShadowPass()) return;
        var level = Minecraft.getInstance().level;
        if (level == null) return;
        MetaMachine self = machine.self();
        BlockPos key = self.getPos().immutable();

        double now = level.getGameTime() + partialTick;
        if (spins.size() > 64) spins.clear();
        Spin spin = spins.computeIfAbsent(key, k -> new Spin(now));
        float dt = (float) Mth.clamp(now - spin.lastTime, 0, 10);
        spin.lastTime = now;
        float target = machine.wiremillRunning() ? 1F : 0F;
        spin.speed += (target - spin.speed) * Math.min(1F, dt / INERTIA_TICKS);
        if (target == 0F && spin.speed < 0.003F) spin.speed = 0F;
        spin.angle = (spin.angle + spin.speed * SPEED * dt) % Mth.TWO_PI;
        spin.feed = (spin.feed + spin.speed * FEED * dt) % (2 * STRIPE);

        // the chamber's own light (glass lets the sky in), never darker than a lit room
        Vec3 c = centre(self);
        int light = LevelRenderer.getLightColor(level, BlockPos.containing(c));
        light = LightTexture.pack(Math.max(LightTexture.block(light), 9), LightTexture.sky(light));

        Direction left = dir(self, RelativeDirection.LEFT);
        Direction back = dir(self, RelativeDirection.BACK);
        Mesh m = new Mesh(buffer.getBuffer(RenderType.entityCutoutNoCull(AF9RenderTypes.WHITE)), light);

        poseStack.pushPose();
        poseStack.translate(0.5, 0, 0.5);
        poseStack.mulPoseMatrix(basis(left, back));
        poseStack.translate(0, AXIS_UP, AXIS_BACK);
        m.use(poseStack.last());
        drawFixed(m, spin.feed);

        poseStack.pushPose();
        poseStack.mulPose(Axis.XP.rotation(spin.angle));
        m.use(poseStack.last());
        drawFlyer(m);
        poseStack.popPose();
        poseStack.popPose();
    }

    /** What stands still: the die, the eyelet and its strut, the spool, and the wire from the die plate into the spool. */
    private static void drawFixed(Mesh m, float feed) {
        // the die in the frame plate's hole: a plate with a bore, a tungsten bell behind it
        m.ring(ax(DIE) - 0.05F, ax(DIE) + 0.05F, 0.03F, 0.34F, 20, DARKER);
        m.ring(ax(DIE) - 0.07F, ax(DIE) - 0.05F, 0.03F, 0.3F, 20, STEEL);
        m.ring(ax(DIE) + 0.05F, ax(DIE) + 0.12F, 0.025F, 0.1F, 16, TUNGSTEN);

        // the eyelet on its strut from the floor
        m.ring(ax(EYELET) - 0.03F, ax(EYELET) + 0.03F, 0.03F, 0.08F, 14, STEEL);
        m.box(ax(EYELET) - 0.02F, FLOOR, -0.02F, ax(EYELET) + 0.02F, -0.08F, 0.02F, DARK);
        m.box(ax(EYELET) - 0.1F, FLOOR, -0.08F, ax(EYELET) + 0.1F, FLOOR + 0.04F, 0.08F, DARKER);

        // the take-up spool: flanges, core, the winding as bands of two coppers, a shaft out to the wall
        m.ring(ax(SPOOL_LEFT), ax(SPOOL_LEFT) + 0.07F, 0.1F, 0.36F, 24, STEEL);
        m.ring(ax(SPOOL_RIGHT) - 0.07F, ax(SPOOL_RIGHT), 0.1F, 0.36F, 24, STEEL);
        m.ring(ax(SPOOL_LEFT) - 0.02F, ax(SPOOL_LEFT), 0.2F, 0.36F, 24, DARKER);
        m.ring(ax(SPOOL_LEFT) + 0.07F, ax(SPOOL_RIGHT) - 0.07F, 0.2F, 0.22F, 24, DARK);
        float band = 0.035F;
        float x = ax(SPOOL_LEFT) + 0.07F;
        float end = ax(SPOOL_RIGHT) - 0.07F;
        for (int k = 0; x < end - 1e-3F; k++) {
            float x1 = Math.min(x + band, end);
            m.tube(x, x1, 0F, 0F, WOUND_R, 20, k % 2 == 0 ? WIND_A : WIND_B);
            x = x1;
        }
        m.tube(ax(WALL_LEFT), ax(SPOOL_LEFT), 0F, 0F, 0.06F, 12, DARK);

        // the wire: one line from the die plate into the spool, light and dark stripes running to the spool
        float from = ax(SPOOL_RIGHT - 0.1F);
        float to = ax(WALL) + 0.01F;
        int k = 0;
        for (float s = from - feed; s < to; s += STRIPE, k++) {
            float a = Math.max(s, from), b = Math.min(s + STRIPE, to);
            if (b > a) m.box(a, -WIRE, -WIRE, b, WIRE, WIRE, k % 2 == 0 ? COPPER_A : COPPER_B);
        }
    }

    /**
     * What turns about the axis (drawn in the carrier's frame): the cabling machine's bobbin carrier. Two solid orange
     * discs joined by six rods that are the bobbins' axles, a white bobbin on each at the front disc, and a strand from
     * every bobbin to the closing die on the axis, where they come together into the wire.
     */
    private static void drawFlyer(Mesh m) {
        for (float column : new float[] { DISC_FRONT, DISC_REAR }) {
            float x0 = ax(column) - 0.04F, x1 = ax(column) + 0.04F;
            m.ring(x0, x1, 0.05F, DISC_R, 28, ORANGE);
            for (int k = 0; k < 6; k++) {
                float a = k * Mth.PI / 3F + 0.26F;
                m.ring(x0 - 0.005F, x1 + 0.005F, 0.1F, 0.14F, 8, DARK, 0.3F * Mth.cos(a), 0.3F * Mth.sin(a));
            }
        }
        for (int k = 0; k < 6; k++) {
            float a = k * Mth.PI / 3F + 0.26F;
            float y = BOBBIN_R * Mth.cos(a), z = BOBBIN_R * Mth.sin(a);
            m.bar(ax(DISC_REAR), y, z, ax(DISC_FRONT), y, z, 0.015F, DARK);
            float bx0 = ax(DISC_FRONT) + 0.04F, bx1 = ax(DISC_FRONT) + 0.26F;
            m.tube(bx0, bx1, y, z, 0.075F, 10, WHITE);
            m.tube(bx0, bx0 + 0.03F, y, z, 0.085F, 10, BOBBIN_FLANGE);
            m.tube(bx1 - 0.03F, bx1, y, z, 0.085F, 10, BOBBIN_FLANGE);
            m.bar(bx1, y, z, ax(CLOSER), 0F, 0F, 0.012F, COPPER_A);
        }
        m.ring(ax(CLOSER) - 0.04F, ax(CLOSER) + 0.04F, 0.02F, 0.12F, 12, TUNGSTEN);
    }

    /** One render's spin state per controller. */
    private static final class Spin {
        float angle;
        float speed;
        float feed;
        double lastTime;

        Spin(double now) {
            this.lastTime = now;
        }
    }

    /** Opaque solids in the current pose (entity shader, white texture, coloured by vertex colour, lit by normals). */
    private static final class Mesh {

        private final VertexConsumer vc;
        private final int light;
        private Matrix4f m;
        private Matrix3f n;

        Mesh(VertexConsumer vc, int light) {
            this.vc = vc;
            this.light = light;
        }

        void use(PoseStack.Pose pose) {
            this.m = pose.pose();
            this.n = pose.normal();
        }

        void box(float x0, float y0, float z0, float x1, float y1, float z1, int rgb) {
            quad(x0, y1, z0, x1, y1, z0, x1, y1, z1, x0, y1, z1, 0, 1, 0, rgb);
            quad(x0, y0, z1, x1, y0, z1, x1, y0, z0, x0, y0, z0, 0, -1, 0, rgb);
            quad(x1, y0, z0, x1, y0, z1, x1, y1, z1, x1, y1, z0, 1, 0, 0, rgb);
            quad(x0, y0, z1, x0, y0, z0, x0, y1, z0, x0, y1, z1, -1, 0, 0, rgb);
            quad(x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1, 0, 0, 1, rgb);
            quad(x1, y0, z0, x0, y0, z0, x0, y1, z0, x1, y1, z0, 0, 0, -1, rgb);
        }

        /** A square bar of half-width {@code h} from one point to another (the caps are left open: they sit in solids). */
        void bar(float x0, float y0, float z0, float x1, float y1, float z1, float h, int rgb) {
            float dx = x1 - x0, dy = y1 - y0, dz = z1 - z0;
            float len = Mth.sqrt(dx * dx + dy * dy + dz * dz);
            if (len < 1e-5F) return;
            dx /= len;
            dy /= len;
            dz /= len;
            // u = d x reference, with the reference the axis least aligned with d
            float rx = 0, ry = 0, rz = 0;
            if (Math.abs(dy) < 0.9F) ry = 1F;
            else rx = 1F;
            float ux = dy * rz - dz * ry, uy = dz * rx - dx * rz, uz = dx * ry - dy * rx;
            float ul = Mth.sqrt(ux * ux + uy * uy + uz * uz);
            ux /= ul;
            uy /= ul;
            uz /= ul;
            float vx = dy * uz - dz * uy, vy = dz * ux - dx * uz, vz = dx * uy - dy * ux;
            float[][] c = new float[4][];
            float[] su = { 1, -1, -1, 1 }, sv = { 1, 1, -1, -1 };
            for (int i = 0; i < 4; i++) {
                c[i] = new float[] { (ux * su[i] + vx * sv[i]) * h, (uy * su[i] + vy * sv[i]) * h,
                        (uz * su[i] + vz * sv[i]) * h };
            }
            for (int i = 0; i < 4; i++) {
                int j = (i + 1) % 4;
                float mx = (c[i][0] + c[j][0]) / 2, my = (c[i][1] + c[j][1]) / 2, mz = (c[i][2] + c[j][2]) / 2;
                float nl = Mth.sqrt(mx * mx + my * my + mz * mz);
                quad(x0 + c[i][0], y0 + c[i][1], z0 + c[i][2], x0 + c[j][0], y0 + c[j][1], z0 + c[j][2],
                        x1 + c[j][0], y1 + c[j][1], z1 + c[j][2], x1 + c[i][0], y1 + c[i][1], z1 + c[i][2],
                        mx / nl, my / nl, mz / nl, rgb);
            }
        }

        /** An open tube along x (centre {@code cy, cz}): the outer surface only. */
        void tube(float x0, float x1, float cy, float cz, float r, int sides, int rgb) {
            for (int k = 0; k < sides; k++) {
                float a0 = Mth.TWO_PI * k / sides, a1 = Mth.TWO_PI * (k + 1) / sides, am = (a0 + a1) / 2;
                float y0 = cy + r * Mth.cos(a0), z0 = cz + r * Mth.sin(a0);
                float y1 = cy + r * Mth.cos(a1), z1 = cz + r * Mth.sin(a1);
                quad(x0, y0, z0, x1, y0, z0, x1, y1, z1, x0, y1, z1, 0, Mth.cos(am), Mth.sin(am), rgb);
            }
        }

        /** A ring about the axis between two x: both faces, the outer surface and the bore. */
        void ring(float x0, float x1, float rin, float rout, int sides, int rgb) {
            ring(x0, x1, rin, rout, sides, rgb, 0F, 0F);
        }

        /** As above, the ring's centre moved to {@code cy, cz}. */
        void ring(float x0, float x1, float rin, float rout, int sides, int rgb, float cy, float cz) {
            for (int k = 0; k < sides; k++) {
                float a0 = Mth.TWO_PI * k / sides, a1 = Mth.TWO_PI * (k + 1) / sides, am = (a0 + a1) / 2;
                float c0 = Mth.cos(a0), s0 = Mth.sin(a0), c1 = Mth.cos(a1), s1 = Mth.sin(a1);
                float iy0 = cy + rin * c0, iz0 = cz + rin * s0, iy1 = cy + rin * c1, iz1 = cz + rin * s1;
                float oy0 = cy + rout * c0, oz0 = cz + rout * s0, oy1 = cy + rout * c1, oz1 = cz + rout * s1;
                quad(x1, iy0, iz0, x1, oy0, oz0, x1, oy1, oz1, x1, iy1, iz1, 1, 0, 0, rgb);
                quad(x0, iy1, iz1, x0, oy1, oz1, x0, oy0, oz0, x0, iy0, iz0, -1, 0, 0, rgb);
                quad(x0, oy0, oz0, x1, oy0, oz0, x1, oy1, oz1, x0, oy1, oz1, 0, Mth.cos(am), Mth.sin(am), rgb);
                quad(x0, iy1, iz1, x1, iy1, iz1, x1, iy0, iz0, x0, iy0, iz0, 0, -Mth.cos(am), -Mth.sin(am), rgb);
            }
        }

        private void quad(float x0, float y0, float z0, float x1, float y1, float z1, float x2, float y2, float z2,
                          float x3, float y3, float z3, float nx, float ny, float nz, int rgb) {
            float r = ((rgb >> 16) & 0xFF) / 255F, g = ((rgb >> 8) & 0xFF) / 255F, b = (rgb & 0xFF) / 255F;
            vertex(x0, y0, z0, nx, ny, nz, r, g, b);
            vertex(x1, y1, z1, nx, ny, nz, r, g, b);
            vertex(x2, y2, z2, nx, ny, nz, r, g, b);
            vertex(x3, y3, z3, nx, ny, nz, r, g, b);
        }

        private void vertex(float x, float y, float z, float nx, float ny, float nz, float r, float g, float b) {
            vc.vertex(m, x, y, z).color(r, g, b, 1F).uv(0.5F, 0.5F).overlayCoords(OverlayTexture.NO_OVERLAY)
                    .uv2(light).normal(n, nx, ny, nz).endVertex();
        }
    }
}
