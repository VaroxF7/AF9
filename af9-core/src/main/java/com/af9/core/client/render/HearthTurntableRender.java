package com.af9.core.client.render;

import com.af9.core.AF9Core;
import com.af9.core.machine.IHearthMachine;

import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine;
import com.gregtechceu.gtceu.api.pattern.util.RelativeDirection;
import com.gregtechceu.gtceu.client.renderer.machine.DynamicRender;
import com.gregtechceu.gtceu.client.renderer.machine.DynamicRenderManager;
import com.gregtechceu.gtceu.client.renderer.machine.DynamicRenderType;

import net.minecraft.client.Minecraft;
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
import com.mojang.serialization.codecs.RecordCodecBuilder;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;

/**
 * The Sanguinite Hearth's rotary hearth floors: two turntables in the 5 x 5 air chamber, a heavy brutalist disc
 * (hub, spokes, a segmented rim with slots that glow while it smelts), one at the chamber's floor and one at the
 * second deck, turning against each other while a recipe runs and coasting to a stop. Drawn in controller-relative
 * blocks (the controller block's floor is y 0; the chamber is 1 block up and its middle {@code back} blocks behind);
 * only while the structure is formed. One render object serves every hearth, so the angle is kept per controller
 * position (capped). Model side: {@link com.af9.core.machine.AF9MachineModels#workableCasingWithHearthTurntable}.
 */
@SuppressWarnings("removal") // new ResourceLocation(ns, path) is the only constructor on 1.20.1
public class HearthTurntableRender extends DynamicRender<IHearthMachine, HearthTurntableRender> {

    // spotless:off
    public static final Codec<HearthTurntableRender> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.FLOAT.fieldOf("back").forGetter(render -> render.back)
    ).apply(instance, HearthTurntableRender::new));
    // spotless:on
    public static final DynamicRenderType<IHearthMachine, HearthTurntableRender> TYPE = new DynamicRenderType<>(CODEC);

    private static final int FULL_BRIGHT = 0xF000F0;
    /** The chamber is closed (no sky, no torch): lit like a hot room instead of the dark. */
    private static final int CHAMBER_LIGHT = LightTexture.pack(11, 4);

    /** Two turns in a minute at full speed, in radians per tick; ticks to get up to speed or coast down. */
    private static final float SPEED = (float) (2 * 2 * Math.PI / 1200);
    private static final float INERTIA_TICKS = 40F;
    /** The turntables' heights (controller-relative y): the chamber's floor and the second deck, 8 blocks up. */
    private static final float[] DECKS = { 1.08F, 9.08F };
    /** Disc radius: the chamber is 5 wide, so 2.5 to the wall; a hand of clearance. */
    private static final float RADIUS = 2.3F;

    // colours (flat: the entity shader lights them by their normals)
    private static final int STEEL = 0x6C7480, DARK = 0x2F343C, DARKER = 0x1E2228, HAZARD = 0xE0A020;
    private static final int EMBER_ON = 0xFF6A18, EMBER_OFF = 0x4A2410;

    private final float back;
    /** Per controller position: the turntables' angle, speed (0 to 1 of full) and when they were last advanced. */
    private final Map<BlockPos, Spin> spins = new HashMap<>();

    public HearthTurntableRender(float back) {
        this.back = back;
    }

    /** Typed as GT's base class, so common code that builds a model never loads this client class. */
    public static DynamicRender<?, ?> create(float back) {
        return new HearthTurntableRender(back);
    }

    /** Client, before the models are built (mod construction). */
    public static void register() {
        DynamicRenderManager.register(new ResourceLocation(AF9Core.MOD_ID, "hearth_turntable"), TYPE);
    }

    @Override
    public DynamicRenderType<IHearthMachine, HearthTurntableRender> getType() {
        return TYPE;
    }

    @Override
    public int getViewDistance() {
        return 96;
    }

    /** The chamber's centre in the controller block's local space: straight behind it, on the middle of the block. */
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
    public boolean shouldRender(IHearthMachine machine, Vec3 cameraPos) {
        return machine.hearthFormed() && centre(machine.self()).closerThan(cameraPos, getViewDistance());
    }

    @Override
    public boolean shouldRenderOffScreen(IHearthMachine machine) {
        return false;
    }

    @Override
    public AABB getRenderBoundingBox(IHearthMachine machine) {
        Vec3 c = centre(machine.self());
        return new AABB(c.x - 3.5, c.y, c.z - 3.5, c.x + 3.5, c.y + 11, c.z + 3.5);
    }

    @Override
    public void render(IHearthMachine machine, float partialTick, PoseStack poseStack, MultiBufferSource buffer,
                       int packedLight, int packedOverlay) {
        if (!machine.hearthFormed()) return;
        if (IrisCompat.renderingShadowPass()) return;
        var level = Minecraft.getInstance().level;
        if (level == null) return;
        MetaMachine self = machine.self();
        BlockPos key = self.getPos().immutable();

        // the angle: eases up to speed and coasts down, keeps its place when parked
        double now = level.getGameTime() + partialTick;
        if (spins.size() > 64) spins.clear();
        Spin spin = spins.computeIfAbsent(key, k -> new Spin(now));
        float dt = (float) Mth.clamp(now - spin.lastTime, 0, 10);
        spin.lastTime = now;
        boolean working = machine.hearthTurning();
        float target = working ? 1F : 0F;
        spin.speed += (target - spin.speed) * Math.min(1F, dt / INERTIA_TICKS);
        if (target == 0F && spin.speed < 0.003F) spin.speed = 0F;
        spin.angle = (spin.angle + spin.speed * SPEED * dt) % Mth.TWO_PI;

        float[] pivot = pivot(self);
        Solid s = new Solid(buffer.getBuffer(RenderType.entityCutoutNoCull(AF9RenderTypes.WHITE)));
        poseStack.pushPose();
        poseStack.translate(pivot[0], 0, pivot[1]);
        for (int i = 0; i < DECKS.length; i++) {
            poseStack.pushPose();
            poseStack.translate(0, DECKS[i], 0);
            poseStack.mulPose(Axis.YP.rotation(i == 0 ? spin.angle : -spin.angle));
            s.use(poseStack.last());
            drawTurntable(s, working);
            poseStack.popPose();
        }
        poseStack.popPose();
    }

    /** One disc in its rotated frame: hub, eight spokes, the rim in sixteen blocks with an ember slot between each pair. */
    private static void drawTurntable(Solid s, boolean working) {
        // hub: a drum with a hazard band
        s.box(-0.42F, 0F, -0.42F, 0.42F, 0.34F, 0.42F, DARK);
        s.box(-0.46F, 0.1F, -0.46F, 0.46F, 0.16F, 0.46F, HAZARD);
        // spokes: heavy girders on the axes and the diagonals
        for (int k = 0; k < 4; k++) {
            s.pushYaw(k * 45F);
            s.box(-RADIUS + 0.2F, 0.02F, -0.14F, RADIUS - 0.2F, 0.2F, 0.14F, STEEL);
            s.box(-RADIUS + 0.2F, 0.2F, -0.05F, RADIUS - 0.2F, 0.27F, 0.05F, DARKER);
            s.popYaw();
        }
        // rim: sixteen plates, an ember slot over the joint of each pair
        for (int k = 0; k < 16; k++) {
            s.pushYaw(k * 22.5F);
            s.box(RADIUS - 0.42F, 0F, -0.4F, RADIUS, 0.22F, 0.4F, k % 2 == 0 ? DARK : STEEL);
            s.box(RADIUS - 0.5F, 0.22F, -0.4F, RADIUS - 0.34F, 0.3F, 0.4F, DARKER);
            s.glow = working;
            s.box(RADIUS - 0.34F, 0.22F, -0.1F, RADIUS - 0.14F, 0.25F, 0.1F, working ? EMBER_ON : EMBER_OFF);
            s.glow = false;
            s.popYaw();
        }
    }

    /** One hearth's turntables: where they point, how fast they go (0 to 1), and the tick they were last advanced at. */
    private static final class Spin {
        float angle;
        float speed;
        double lastTime;

        Spin(double now) {
            this.lastTime = now;
        }
    }

    /** Opaque boxes (entity shader, white texture, coloured by vertex colour, lit by their normals). */
    private static final class Solid {

        private final VertexConsumer vc;
        /** Full bright, for the embers. */
        boolean glow;
        private Matrix4f m;
        private Matrix3f n;
        private final ArrayDeque<Matrix4f> saved = new ArrayDeque<>();
        private final ArrayDeque<Matrix3f> savedNormals = new ArrayDeque<>();

        Solid(VertexConsumer vc) {
            this.vc = vc;
        }

        void use(PoseStack.Pose pose) {
            this.m = pose.pose();
            this.n = pose.normal();
        }

        /** Turns the frame by {@code degrees} about the up axis until {@link #popYaw()}. */
        void pushYaw(float degrees) {
            saved.push(new Matrix4f(m));
            savedNormals.push(new Matrix3f(n));
            float radians = (float) Math.toRadians(degrees);
            m = new Matrix4f(m).rotateY(radians);
            n = new Matrix3f(n).rotateY(radians);
        }

        void popYaw() {
            m = saved.pop();
            n = savedNormals.pop();
        }

        void box(float x0, float y0, float z0, float x1, float y1, float z1, int rgb) {
            face(x0, y1, z0, x1, y1, z0, x1, y1, z1, x0, y1, z1, 0, 1, 0, rgb);
            face(x0, y0, z1, x1, y0, z1, x1, y0, z0, x0, y0, z0, 0, -1, 0, rgb);
            face(x1, y0, z0, x1, y0, z1, x1, y1, z1, x1, y1, z0, 1, 0, 0, rgb);
            face(x0, y0, z1, x0, y0, z0, x0, y1, z0, x0, y1, z1, -1, 0, 0, rgb);
            face(x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1, 0, 0, 1, rgb);
            face(x1, y0, z0, x0, y0, z0, x0, y1, z0, x1, y1, z0, 0, 0, -1, rgb);
        }

        private void face(float x0, float y0, float z0, float x1, float y1, float z1, float x2, float y2, float z2,
                          float x3, float y3, float z3, float nx, float ny, float nz, int rgb) {
            float r = ((rgb >> 16) & 0xFF) / 255F, g = ((rgb >> 8) & 0xFF) / 255F, b = (rgb & 0xFF) / 255F;
            vertex(x0, y0, z0, nx, ny, nz, r, g, b);
            vertex(x1, y1, z1, nx, ny, nz, r, g, b);
            vertex(x2, y2, z2, nx, ny, nz, r, g, b);
            vertex(x3, y3, z3, nx, ny, nz, r, g, b);
        }

        private void vertex(float x, float y, float z, float nx, float ny, float nz, float r, float g, float b) {
            vc.vertex(m, x, y, z).color(r, g, b, 1F).uv(0.5F, 0.5F).overlayCoords(OverlayTexture.NO_OVERLAY)
                    .uv2(glow ? FULL_BRIGHT : CHAMBER_LIGHT).normal(n, nx, ny, nz).endVertex();
        }
    }
}
