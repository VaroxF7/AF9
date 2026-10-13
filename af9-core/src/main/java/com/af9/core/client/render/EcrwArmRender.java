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
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import java.util.HashMap;
import java.util.Map;

/**
 * The ECRW as a circular clarifier (a Klärbecken): a rotating bridge on the central pier, scraper blades dragging
 * along the floor, a scum board skimming the surface, and the water between the walls. Everything is drawn in
 * controller-relative blocks with y 0 at the controller's floor, matching the structure in
 * {@code startup_scripts/gtceu/ecrw.js}:
 * <ul>
 * <li>the pond floor's top is at y 0, the wall is y 0 to 3 with its glass waterline at y 1 to 2, the static walkway
 * and the rim of frames are y 3 to 4;</li>
 * <li>the bridge therefore runs at {@code armY} (about 2.5), under the walkway and above the water, with the
 * scrapers hanging from it to the floor;</li>
 * <li>the arm's {@code radius} stays inside the wall's jagged inner edge (nearest corner about 5.7 blocks out);</li>
 * <li>the water covers exactly the cells the pattern leaves hollow (r &lt; 6.4, outside the 3 x 3 pier).</li>
 * </ul>
 * The bridge turns while a recipe runs and coasts to a stop; it parks where it stops. One render object serves every
 * well, so state is kept per controller position (capped). Model side:
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

    private static final int FULL_BRIGHT = 0xF000F0;

    /** Three turns a minute at full speed, in radians per tick. */
    private static final float SPEED = (float) (3 * 2 * Math.PI / 1200);
    /** Ticks the bridge takes to get up to speed or coast to a stop (time constant). */
    private static final float INERTIA_TICKS = 25F;

    /** The pond's footprint: cells with r below this are hollow, the 3 x 3 pier (|dx|, |dz| below 2) is not. */
    private static final float POND_RADIUS = 6.4F;
    private static final int POND_CELLS = 6;
    /** Water surface and the darker layer above the floor, controller-relative y. */
    private static final float WATER_Y = 1.5F, DEEP_Y = 0.25F;
    /** The bridge's inner end (just outside the 3 x 3 pier's collar) and the counterweight side. */
    private static final float HUB = 1.65F;
    private static final float BACK_ARM = 2.6F;
    /** Fallback water tint (vanilla default) when no biome is loaded. */
    private static final int FALLBACK_WATER = 0x3F76E4;

    // colours (flat: the entity shader lights them by their normals)
    private static final int STEEL = 0x8A94A6, DARK_STEEL = 0x4B5567, DARKER = 0x2E343E, DECK = 0x9AA3B0;
    private static final int HAZARD = 0xE0B020, BLADE = 0x5B6574, RUBBER = 0x1E2228, BOARD = 0x7A838F;

    /** Per controller position: the bridge's angle, its speed (0 to 1 of full) and when it was last advanced. */
    private final Map<BlockPos, Rake> rakes = new HashMap<>();

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
        BlockPos key = self.getPos().immutable();

        // --- the bridge's angle: eases up to speed and coasts down, keeps its place when parked ---
        double now = level.getGameTime() + partialTick;
        if (rakes.size() > 64) rakes.clear();
        Rake rake = rakes.computeIfAbsent(key, k -> new Rake(now, (k.hashCode() & 0xFF) * Mth.TWO_PI / 256F));
        float dt = (float) Mth.clamp(now - rake.lastTime, 0, 10);
        rake.lastTime = now;
        float target = machine.rakeSpinning() ? 1F : 0F;
        rake.speed += (target - rake.speed) * Math.min(1F, dt / INERTIA_TICKS);
        if (target == 0F && rake.speed < 0.003F) rake.speed = 0F;
        rake.angle = (rake.angle + rake.speed * SPEED * dt) % Mth.TWO_PI;
        boolean working = machine.rakeSpinning();

        float[] pivot = pivot(self);
        Vec3 centre = centre(self);
        // light: the open air above the pier's cap (the controller's own block is solid wall, so dark)
        int light = LevelRenderer.getLightColor(level, BlockPos.containing(centre.x, centre.y + 5.5, centre.z));

        poseStack.pushPose();
        poseStack.translate(pivot[0], 0, pivot[1]);

        // ---------- solids ----------
        Solid s = new Solid(buffer.getBuffer(RenderType.entityCutoutNoCull(AF9RenderTypes.WHITE)), light);
        s.use(poseStack.last());
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotation(rake.angle));
        s.use(poseStack.last());
        drawBridge(s, poseStack, working);
        poseStack.popPose();

        // ---------- water: drawn last, translucent ----------
        s.use(poseStack.last());
        drawWater(buffer.getBuffer(AF9RenderTypes.LIGHT_RING), poseStack.last().pose(), level, centre, now, working);

        poseStack.popPose();
    }

    /** The turning part, in the rotated frame: the bridge along +x, the counterweight along -x. */
    private void drawBridge(Solid s, PoseStack pose, boolean working) {
        float y = armY;
        float tip = radius - 0.12F;
        float top = y + 0.3F, bot = y - 0.3F;
        float zi = 0.3F;

        // the drive turntable round the pier: a collar with a hazard band
        s.box(-1.62F, y - 0.55F, -1.62F, 1.62F, y + 0.35F, 1.62F, DARK_STEEL);
        s.box(-1.66F, y - 0.08F, -1.66F, 1.66F, y + 0.1F, 1.66F, HAZARD);

        // --- bridge: two Warren trusses, the deck on top, ties between them ---
        for (float z : new float[] { -zi, zi }) {
            s.strut(HUB - 0.05F, top, z, tip - 0.6F, top, z, 0.045F, STEEL);
            s.strut(HUB - 0.05F, bot, z, tip - 0.6F, bot, z, 0.045F, STEEL);
            int bays = Math.max(2, (int) ((tip - 0.6F - HUB) / 0.8F));
            float step = (tip - 0.6F - HUB) / bays;
            for (int i = 0; i < bays; i++) {
                float x0 = HUB + i * step;
                float x1 = x0 + step;
                if ((i & 1) == 0) s.strut(x0, bot, z, x1, top, z, 0.035F, DARK_STEEL);
                else s.strut(x0, top, z, x1, bot, z, 0.035F, DARK_STEEL);
            }
        }
        s.box(HUB, top, -zi - 0.2F, tip - 0.5F, top + 0.06F, zi + 0.2F, DECK);
        // hazard edge stripes on the deck
        s.box(HUB, top + 0.06F, -zi - 0.2F, tip - 0.5F, top + 0.075F, -zi - 0.14F, HAZARD);
        s.box(HUB, top + 0.06F, zi + 0.14F, tip - 0.5F, top + 0.075F, zi + 0.2F, HAZARD);

        // --- the drive carriage at the tip: a bogie with its motor, a beacon on top ---
        s.box(tip - 0.7F, y - 0.5F, -0.4F, tip, y + 0.34F, 0.4F, DARKER);
        s.box(tip - 0.62F, y - 0.55F, -0.46F, tip - 0.08F, y - 0.2F, 0.46F, DARK_STEEL);
        s.box(tip - 0.45F, y + 0.34F, -0.18F, tip - 0.2F, y + 0.5F, 0.18F, STEEL);
        s.glow = working;
        s.box(tip - 0.36F, y + 0.5F, -0.06F, tip - 0.29F, y + 0.58F, 0.06F, working ? 0xFFB400 : 0x5A4A18);
        s.glow = false;
        // guide wheels on the wall side
        s.box(tip - 0.05F, y - 0.3F, -0.34F, tip + 0.1F, y - 0.1F, -0.2F, RUBBER);
        s.box(tip - 0.05F, y - 0.3F, 0.2F, tip + 0.1F, y - 0.1F, 0.34F, RUBBER);

        // --- counterweight side: the same chords, a stub, and the weight ---
        for (float z : new float[] { -zi, zi }) {
            s.strut(-BACK_ARM + 0.4F, top, z, -HUB + 0.05F, top, z, 0.045F, STEEL);
            s.strut(-BACK_ARM + 0.4F, bot, z, -HUB + 0.05F, bot, z, 0.045F, STEEL);
            s.strut(-BACK_ARM + 0.4F, bot, z, -HUB - 0.1F, top, z, 0.035F, DARK_STEEL);
        }
        s.box(-BACK_ARM + 0.4F, top, -zi - 0.2F, -HUB, top + 0.06F, zi + 0.2F, DECK);
        s.box(-BACK_ARM - 0.5F, y - 0.5F, -0.42F, -BACK_ARM + 0.45F, y + 0.36F, 0.42F, DARKER);
        s.box(-BACK_ARM - 0.5F, y + 0.1F, -0.44F, -BACK_ARM + 0.45F, y + 0.2F, 0.44F, HAZARD);

        // --- scum board: a skimming plate just through the water's surface, hung under the trusses ---
        float boardTop = WATER_Y + 0.35F, boardBot = WATER_Y - 0.28F;
        float boardEnd = tip - 0.9F;
        s.box(HUB + 0.1F, boardBot, zi - 0.025F, boardEnd, boardTop, zi + 0.025F, BOARD);
        for (float x : new float[] { HUB + 0.7F, (HUB + boardEnd) / 2, boardEnd - 0.5F }) {
            s.strut(x, bot, zi, x, boardTop, zi, 0.03F, DARK_STEEL);
        }

        // --- scraper blades: plough plates hanging to the floor, staggered along the arm ---
        float span = tip - 0.9F - (HUB + 0.7F);
        int blades = 4;
        for (int i = 0; i < blades; i++) {
            float x = HUB + 0.7F + span * (i + 0.5F) / blades;
            float half = 0.5F;
            double yaw = Math.toRadians(35);
            float ex = (float) (Math.cos(yaw) * half);
            float ez = (float) (Math.sin(yaw) * half);
            // hangers from the bottom chord to the blade at both its ends
            s.strut(x + ex, bot, -ez, x + ex, 0.95F, -ez, 0.03F, DARK_STEEL);
            s.strut(x - ex, bot, ez, x - ex, 0.95F, ez, 0.03F, DARK_STEEL);
            pose.pushPose();
            pose.translate(x, 0, 0);
            pose.mulPose(Axis.YP.rotationDegrees(35F));
            s.use(pose.last());
            s.box(-half, 0.1F, -0.035F, half, 0.95F, 0.035F, BLADE);
            s.box(-half, 0.02F, -0.05F, half, 0.12F, 0.05F, RUBBER);
            pose.popPose();
            s.use(pose.last());
        }
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

    /** One pond's bridge: where it points, how fast it goes (0 to 1), and the tick it was last advanced at. */
    private static final class Rake {
        float angle;
        float speed;
        double lastTime;

        Rake(double now, float angle) {
            this.angle = angle;
            this.lastTime = now;
        }
    }

    /** Opaque shapes (entity shader, white texture, coloured by vertex colour, lit by their normals). */
    private static final class Solid {

        private final VertexConsumer vc;
        private final int light;
        /** Full bright, for the beacon. */
        boolean glow;
        private Matrix4f m;
        private Matrix3f n;

        Solid(VertexConsumer vc, int light) {
            this.vc = vc;
            this.light = light;
        }

        void use(PoseStack.Pose pose) {
            this.m = pose.pose();
            this.n = pose.normal();
        }

        void box(float x0, float y0, float z0, float x1, float y1, float z1, int rgb) {
            face(x0, y1, z0, x1, y1, z0, x1, y1, z1, x0, y1, z1, 0, 1, 0, rgb);
            face(x0, y0, z1, x1, y0, z1, x1, y0, z0, x0, y0, z0, 0, -1, 0, rgb);
            face(x1, y0, z0, x1, y0, z1, x1, y1, z1, x1, y1, z0, 1, 0, 0, rgb);
            face(x0, y0, z1, x0, y0, z0, x0, y1, z0, x0, y1, z1, -1, 0, 0, rgb);
            face(x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1, 0, 0, 1, rgb);
            face(x1, y0, z0, x0, y0, z0, x0, y1, z0, x1, y1, z0, 0, 0, -1, rgb);
        }

        /** A square-section bar from a to b, h the half width: truss chords, diagonals, hangers. */
        void strut(float ax, float ay, float az, float bx, float by, float bz, float h, int rgb) {
            float dx = bx - ax, dy = by - ay, dz = bz - az;
            float len = Mth.sqrt(dx * dx + dy * dy + dz * dz);
            if (len < 1e-4F) return;
            dx /= len;
            dy /= len;
            dz /= len;
            float rx = 0, ry = 1, rz = 0;
            if (Math.abs(dy) > 0.9F) {
                rx = 1;
                ry = 0;
            }
            float ux = dy * rz - dz * ry, uy = dz * rx - dx * rz, uz = dx * ry - dy * rx;
            float ul = Mth.sqrt(ux * ux + uy * uy + uz * uz);
            ux /= ul;
            uy /= ul;
            uz /= ul;
            float vx = dy * uz - dz * uy, vy = dz * ux - dx * uz, vz = dx * uy - dy * ux;
            // the four corners round a (and b): +u+v, -u+v, -u-v, +u-v
            float[][] c = {
                    { ux * h + vx * h, uy * h + vy * h, uz * h + vz * h },
                    { -ux * h + vx * h, -uy * h + vy * h, -uz * h + vz * h },
                    { -ux * h - vx * h, -uy * h - vy * h, -uz * h - vz * h },
                    { ux * h - vx * h, uy * h - vy * h, uz * h - vz * h } };
            // outward normals of the four sides: +v, -u, -v, +u
            float[][] nn = { { vx, vy, vz }, { -ux, -uy, -uz }, { -vx, -vy, -vz }, { ux, uy, uz } };
            for (int i = 0; i < 4; i++) {
                float[] p = c[i], q = c[(i + 1) % 4], nr = nn[i];
                face(ax + p[0], ay + p[1], az + p[2], ax + q[0], ay + q[1], az + q[2],
                        bx + q[0], by + q[1], bz + q[2], bx + p[0], by + p[1], bz + p[2],
                        nr[0], nr[1], nr[2], rgb);
            }
            face(ax + c[0][0], ay + c[0][1], az + c[0][2], ax + c[1][0], ay + c[1][1], az + c[1][2],
                    ax + c[2][0], ay + c[2][1], az + c[2][2], ax + c[3][0], ay + c[3][1], az + c[3][2],
                    -dx, -dy, -dz, rgb);
            face(bx + c[0][0], by + c[0][1], bz + c[0][2], bx + c[1][0], by + c[1][1], bz + c[1][2],
                    bx + c[2][0], by + c[2][1], bz + c[2][2], bx + c[3][0], by + c[3][1], bz + c[3][2],
                    dx, dy, dz, rgb);
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
                    .uv2(glow ? FULL_BRIGHT : light).normal(n, nx, ny, nz).endVertex();
        }
    }
}
