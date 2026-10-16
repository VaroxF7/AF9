package com.af9.core.client.render;

import com.af9.core.AF9Core;
import com.af9.core.machine.ISawMachine;

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

import java.util.HashMap;
import java.util.Map;

/**
 * The Industrial Cutting Machine's saw blade: a big toothed disc standing in the chamber behind the front window, its
 * axis along the controller's front-back line, rising out of the floor and spinning while a recipe runs (it eases up
 * to speed and coasts down). Concentric bands (hub, steel, scorched steel, a green-tinted edge band) and a ring of
 * alternately set teeth, in flat colours on the white texture. Drawn in controller-relative blocks, only while the
 * structure is formed. One render object serves every cutter, so the angle is kept per controller position (capped).
 * Model side: {@link com.af9.core.machine.AF9MachineModels#workableCasingWithSawBlade}. The look follows the animated
 * saw of GTNH's cutting multiblock; the geometry is written from scratch for this renderer.
 */
@SuppressWarnings("removal") // new ResourceLocation(ns, path) is the only constructor on 1.20.1
public class SawBladeRender extends DynamicRender<ISawMachine, SawBladeRender> {

    // spotless:off
    public static final Codec<SawBladeRender> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.FLOAT.fieldOf("back").forGetter(render -> render.back),
            Codec.FLOAT.fieldOf("up").forGetter(render -> render.up),
            Codec.FLOAT.fieldOf("radius").forGetter(render -> render.radius)
    ).apply(instance, SawBladeRender::new));
    // spotless:on
    public static final DynamicRenderType<ISawMachine, SawBladeRender> TYPE = new DynamicRenderType<>(CODEC);

    private static final int FULL_BRIGHT = 0xF000F0;
    /** The chamber is closed (no sky, no torch): lit like a workshop instead of the dark. */
    private static final int CHAMBER_LIGHT = LightTexture.pack(12, 4);

    /** One turn in two seconds at full speed, in radians per tick; ticks to get up to speed or coast down. */
    private static final float SPEED = (float) (2 * Math.PI / 40);
    private static final float INERTIA_TICKS = 30F;
    private static final int SEGMENTS = 64;
    private static final int TEETH = 40;

    // flat colours (the entity shader lights them by their normals)
    private static final int HUB = 0x2A2D33, HUB_LIGHT = 0x4C515B, STEEL_A = 0x7A808A, STEEL_B = 0x6A7078,
            SCORCH_A = 0x5C5348, SCORCH_B = 0x504A42, EDGE_A = 0x4E8A5A, EDGE_B = 0x437C50, TOOTH = 0x59B36B;
    private static final int LAMP_ON = 0x7CFF8A, LAMP_OFF = 0x24402A;

    private final float back;
    private final float up;
    private final float radius;
    /** Per controller position: the blade's angle, speed (0 to 1 of full) and when it was last advanced. */
    private final Map<BlockPos, Spin> spins = new HashMap<>();

    public SawBladeRender(float back, float up, float radius) {
        this.back = back;
        this.up = up;
        this.radius = radius;
    }

    /** Typed as GT's base class, so common code that builds a model never loads this client class. */
    public static DynamicRender<?, ?> create(float back, float up, float radius) {
        return new SawBladeRender(back, up, radius);
    }

    /** Client, before the models are built (mod construction). */
    public static void register() {
        DynamicRenderManager.register(new ResourceLocation(AF9Core.MOD_ID, "saw_blade"), TYPE);
    }

    @Override
    public DynamicRenderType<ISawMachine, SawBladeRender> getType() {
        return TYPE;
    }

    @Override
    public int getViewDistance() {
        return 64;
    }

    private Direction backDir(MetaMachine self) {
        boolean flipped = self instanceof MultiblockControllerMachine controller && controller.isFlipped();
        return RelativeDirection.BACK.getRelative(self.getFrontFacing(), self.getUpwardsFacing(), flipped);
    }

    /** The blade's centre in the controller block's local space. */
    private Vec3 pivot(MetaMachine self) {
        Direction backDir = backDir(self);
        return new Vec3(0.5 + backDir.getStepX() * back, up, 0.5 + backDir.getStepZ() * back);
    }

    private Vec3 centre(MetaMachine self) {
        Vec3 pivot = pivot(self);
        BlockPos pos = self.getPos();
        return new Vec3(pos.getX() + pivot.x, pos.getY() + pivot.y, pos.getZ() + pivot.z);
    }

    @Override
    public boolean shouldRender(ISawMachine machine, Vec3 cameraPos) {
        return machine.sawFormed() && centre(machine.self()).closerThan(cameraPos, getViewDistance());
    }

    @Override
    public boolean shouldRenderOffScreen(ISawMachine machine) {
        return false;
    }

    @Override
    public AABB getRenderBoundingBox(ISawMachine machine) {
        Vec3 c = centre(machine.self());
        double r = radius + 0.5;
        return new AABB(c.x - r, c.y - r, c.z - r, c.x + r, c.y + r, c.z + r);
    }

    @Override
    public void render(ISawMachine machine, float partialTick, PoseStack poseStack, MultiBufferSource buffer,
                       int packedLight, int packedOverlay) {
        if (!machine.sawFormed()) return;
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
        boolean working = machine.sawSpinning();
        float target = working ? 1F : 0F;
        spin.speed += (target - spin.speed) * Math.min(1F, dt / INERTIA_TICKS);
        if (target == 0F && spin.speed < 0.003F) spin.speed = 0F;
        // clockwise seen from the front
        spin.angle = (spin.angle - spin.speed * SPEED * dt) % Mth.TWO_PI;

        Vec3 pivot = pivot(self);
        Direction backDir = backDir(self);
        Solid s = new Solid(buffer.getBuffer(RenderType.entityCutoutNoCull(AF9RenderTypes.WHITE)));
        poseStack.pushPose();
        poseStack.translate(pivot.x, pivot.y, pivot.z);
        // local +z along the blade's axis (the controller's back direction), then the spin about it
        poseStack.mulPose(Axis.YP.rotation((float) Math.atan2(backDir.getStepX(), backDir.getStepZ())));
        poseStack.mulPose(Axis.ZP.rotation(spin.angle));
        s.use(poseStack.last());
        drawBlade(s, radius, working);
        poseStack.popPose();
    }

    /** The blade in its spinning frame: the disc lies in the xy plane, 0.2 thick, centred on z = 0. */
    private static void drawBlade(Solid s, float r, boolean working) {
        float k = r / 1.85F;
        // hub drum, thicker than the plate, with a lamp ring on both faces
        band(s, 0F, 0.26F * k, 0.16F, 0.16F, HUB, HUB_LIGHT, false);
        s.glow = working;
        band(s, 0.26F * k, 0.34F * k, 0.17F, 0.17F, working ? LAMP_ON : LAMP_OFF, working ? LAMP_ON : LAMP_OFF, false);
        s.glow = false;
        // steel plate, a scorched band and the green edge band, each a hair thinner than the one inside
        band(s, 0.34F * k, 0.75F * k, 0.1F, 0.1F, STEEL_A, STEEL_B, true);
        band(s, 0.75F * k, 1.2F * k, 0.09F, 0.09F, SCORCH_A, SCORCH_B, true);
        band(s, 1.2F * k, 1.6F * k, 0.08F, 0.08F, EDGE_A, EDGE_B, true);
        // teeth: an alternately set ring of wedges around the edge
        float root = 1.6F * k, tip = r;
        for (int i = 0; i < TEETH; i++) {
            float a0 = (float) (2 * Math.PI * i / TEETH);
            float a1 = (float) (2 * Math.PI * (i + 0.5) / TEETH);
            float a2 = (float) (2 * Math.PI * (i + 1) / TEETH);
            float set = (i % 2 == 0 ? 1 : -1) * 0.035F;
            float z = 0.06F;
            // the face towards +z and the one towards -z, the tooth leaning round the circle
            tooth(s, root, tip, a0, a1, a2, set, z, 1F);
            tooth(s, root, tip, a0, a1, a2, set, -z, -1F);
        }
    }

    /** One tooth: a triangle from two root points to the tip, drawn as a degenerate quad. */
    private static void tooth(Solid s, float root, float tip, float a0, float a1, float a2, float set, float z,
                              float side) {
        float x0 = Mth.cos(a0) * root, y0 = Mth.sin(a0) * root;
        float x2 = Mth.cos(a2) * root, y2 = Mth.sin(a2) * root;
        float x1 = Mth.cos(a1 + 0.12F) * tip, y1 = Mth.sin(a1 + 0.12F) * tip;
        float zz = z + set * side;
        s.quad(x0, y0, zz, x1, y1, zz, x1, y1, zz, x2, y2, zz, 0F, 0F, side, TOOTH);
        // the cutting edge, a thin wall between the two faces along the leading flank
        if (side > 0) {
            float z0 = zz, z1 = -z + set * -1F;
            float dx = x1 - x0, dy = y1 - y0;
            float len = Mth.sqrt(dx * dx + dy * dy);
            s.quad(x0, y0, z0, x1, y1, z0, x1, y1, z1, x0, y0, z1, dy / len, -dx / len, 0F, EDGE_A);
            dx = x2 - x1;
            dy = y2 - y1;
            len = Mth.sqrt(dx * dx + dy * dy);
            s.quad(x1, y1, z0, x2, y2, z0, x2, y2, z1, x1, y1, z1, dy / len, -dx / len, 0F, EDGE_A);
        }
    }

    /**
     * A flat ring from {@code r0} to {@code r1}, {@code zFront} / {@code zBack} thick to each side, in alternating
     * colours per segment; {@code rim} also closes the outer wall.
     */
    private static void band(Solid s, float r0, float r1, float zFront, float zBack, int colA, int colB,
                             boolean rim) {
        for (int i = 0; i < SEGMENTS; i++) {
            float a0 = (float) (2 * Math.PI * i / SEGMENTS), a1 = (float) (2 * Math.PI * (i + 1) / SEGMENTS);
            float c0 = Mth.cos(a0), s0 = Mth.sin(a0), c1 = Mth.cos(a1), s1 = Mth.sin(a1);
            int rgb = (i % 2 == 0) ? colA : colB;
            if (r0 > 0F) {
                s.quad(c0 * r0, s0 * r0, zFront, c1 * r0, s1 * r0, zFront, c1 * r1, s1 * r1, zFront, c0 * r1, s0 * r1,
                        zFront, 0F, 0F, 1F, rgb);
                s.quad(c0 * r1, s0 * r1, -zBack, c1 * r1, s1 * r1, -zBack, c1 * r0, s1 * r0, -zBack, c0 * r0,
                        s0 * r0, -zBack, 0F, 0F, -1F, rgb);
            } else {
                // a disc: a fan, as a quad whose first two points are the centre
                s.quad(0F, 0F, zFront, 0F, 0F, zFront, c1 * r1, s1 * r1, zFront, c0 * r1, s0 * r1, zFront, 0F, 0F, 1F,
                        rgb);
                s.quad(c0 * r1, s0 * r1, -zBack, c1 * r1, s1 * r1, -zBack, 0F, 0F, -zBack, 0F, 0F, -zBack, 0F, 0F,
                        -1F, rgb);
            }
            if (rim || r0 == 0F) {
                float mx = (c0 + c1) * 0.5F, my = (s0 + s1) * 0.5F;
                s.quad(c0 * r1, s0 * r1, zFront, c0 * r1, s0 * r1, -zBack, c1 * r1, s1 * r1, -zBack, c1 * r1, s1 * r1,
                        zFront, mx, my, 0F, rgb == colA ? colB : colA);
            }
        }
    }

    /** One blade: where it points, how fast it goes (0 to 1), and the tick it was last advanced at. */
    private static final class Spin {
        float angle;
        float speed;
        double lastTime;

        Spin(double now) {
            this.lastTime = now;
        }
    }

    /** Opaque quads (entity shader, white texture, coloured by vertex colour, lit by their normals). */
    private static final class Solid {

        private final VertexConsumer vc;
        /** Full bright, for the lamp ring. */
        boolean glow;
        private Matrix4f m;
        private Matrix3f n;

        Solid(VertexConsumer vc) {
            this.vc = vc;
        }

        void use(PoseStack.Pose pose) {
            this.m = pose.pose();
            this.n = pose.normal();
        }

        void quad(float x0, float y0, float z0, float x1, float y1, float z1, float x2, float y2, float z2, float x3,
                  float y3, float z3, float nx, float ny, float nz, int rgb) {
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
