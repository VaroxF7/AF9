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
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import org.joml.Matrix4f;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * The ECRW's pond, drawn like a round clarifier ("Rundbecken" of a German sewage works) while the structure is formed:
 * <ul>
 * <li>the <b>water</b>: a translucent surface on exactly the cells the pattern leaves hollow (r &lt; 6.4, outside the
 * 3 x 3 pier), tinted by the biome's water colour;</li>
 * <li>a smooth <b>round wall</b> and a round pier in the casings' own textures, drawn in place of the stepped blocks
 * ({@link PondWalls} hides those while the pond is formed; without Embeddium a coping ring on the rim hides the
 * steps from above instead);</li>
 * <li>the <b>scraper bridge</b> (Raeumerbruecke): a lattice girder walkway with handrails from a turntable on the
 * pier to a drive carriage with wheels on the rim; it turns slowly round the pier while a recipe runs, and runs up and
 * down to a stop;</li>
 * <li>the <b>skimmer</b> under the bridge: an angled blade hanging from struts at the water line, which pushes the
 * scum (foam on the water) outwards to a fixed <b>scum trough</b> at the wall.</li>
 * </ul>
 * In controller-relative blocks, y 0 at the controller block's floor: the water at 1.5, the rim's top at 3, the pier's
 * top at 4. The codec keeps the {@code armY} and {@code radius} parameters of the machine definition. Model side:
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

    /** The pond's footprint: cells with r below this are hollow, the 3 x 3 pier (|dx|, |dz| below 2) is not. */
    private static final float POND_RADIUS = 5.4F;
    private static final int POND_CELLS = 5;
    /** Water surface and the darker layer above the floor, controller-relative y. */
    private static final float WATER_Y = 1.5F, DEEP_Y = 0.25F;
    /** Fallback water tint (vanilla default) when no biome is loaded. */
    private static final int FALLBACK_WATER = 0x3F76E4;

    /** Heights: the rim's top (the coping ring sits on it), the pier's top (the turntable), the walkway deck. */
    private static final float RIM_Y = 3.0F, COPING_Y = 3.12F, PIER_Y = 4.0F, DECK_Y = 4.06F, RAIL_Y = 5.05F;
    /** The bridge: from the turntable (radius) to the drive carriage on the rim, half the walkway's width. */
    private static final float BRIDGE_IN = 0.6F, BRIDGE_OUT = 5.2F, HALF_WIDTH = 0.30F;
    /** The coping ring's radii. */
    private static final float COPING_IN = 4.0F, COPING_OUT = 6.15F;
    /** The round wall (inner and outer radius), the round pier's radius, the water disc under the wall's thickness. */
    private static final float WALL_IN = 4.35F, WALL_OUT = 5.45F, PIER_R = 1.45F, WATER_R = 4.5F;
    /** The carriage's track on the wall's top, radii. */
    private static final float TRACK_IN = 4.7F, TRACK_OUT = 5.35F;
    /** The machine house in the controller's frame (x to its right, z behind it, 0 at the pond's middle): its box. */
    private static final float HOUSE_HALF = 2.5F, HOUSE_Z0 = -8.5F, HOUSE_Z1 = -5.5F, HOUSE_H = 3F, INSET = 0.04F;
    /** The roof: ridge height above the base, eave overhang to the sides and to the front. */
    private static final float RIDGE_Y = 4.3F, EAVE_Y = 2.95F, EAVE_X = 2.9F, EAVE_Z = -8.8F;
    private static final int ZINC = 0x58626B, FASCIA = 0x3A3F45;
    private static final ResourceLocation WALL_TEXTURE = new ResourceLocation("gtceu",
            "block/casings/gcym/watertight_casing");
    private static final ResourceLocation PIER_TEXTURE = new ResourceLocation("gtceu",
            "block/casings/solid/machine_casing_clean_stainless_steel");
    /** One turn of the bridge at full speed, in ticks, and how quickly it gets there. */
    private static final float REVOLUTION_TICKS = 640F, RAMP = 0.05F;
    /** The skimmer blade in the bridge's frame: from (radius, lateral) to (radius, lateral), its height centre. */
    private static final float BLADE_U0 = 1.6F, BLADE_V0 = 0.95F, BLADE_U1 = 3.75F, BLADE_V1 = 0.45F;
    private static final float BLADE_Y = 1.55F, BLADE_HALF_HEIGHT = 0.2F;
    private static final int FOAM_PATCHES = 9;

    private static final int GALVANIZED = 0x9AA3AB, STEEL = 0x6E7782, GRATING = 0x3A3F45, DARK = 0x2B2D30,
            MOTOR = 0x3F6B8E, STAINLESS = 0xB5BCC4, CONCRETE = 0x8A8F94, RUBBER = 0x25262A, FOAM = 0xEFEBDD;

    /** The bridge's turn and speed per machine, client side. */
    private static final Map<MetaMachine, Turn> TURNS = new WeakHashMap<>();

    private static final class Turn {
        float angle;
        float speed;
        float foam;
        double last = Double.NaN;
    }

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
                c.x + POND_CELLS + 1.5, c.y + 6.5, c.z + POND_CELLS + 1.5);
    }

    @Override
    public void render(IEcrwMachine machine, float partialTick, PoseStack poseStack, MultiBufferSource buffer,
                       int packedLight, int packedOverlay) {
        if (!machine.pondFormed()) return;
        if (IrisCompat.renderingShadowPass()) return;
        var level = Minecraft.getInstance().level;
        if (level == null) return;
        MetaMachine self = machine.self();
        float[] pivot = pivot(self);
        Vec3 centre = centre(self);
        double now = level.getGameTime() + partialTick;
        Turn turn = advance(self, machine.rakeSpinning(), now);
        // the controller's own block is solid and reads as dark: take the light of the open air over the pier
        int light = LevelRenderer.getLightColor(level, BlockPos.containing(centre).above(5));
        boolean flipped = self instanceof MultiblockControllerMachine controller && controller.isFlipped();
        Direction left = RelativeDirection.LEFT.getRelative(self.getFrontFacing(), self.getUpwardsFacing(), flipped);
        float troughAngle = (float) Math.atan2(left.getStepZ(), left.getStepX());
        Direction right = RelativeDirection.RIGHT.getRelative(self.getFrontFacing(), self.getUpwardsFacing(), flipped);
        float houseAngle = (float) Math.atan2(right.getStepZ(), right.getStepX());

        poseStack.pushPose();
        poseStack.translate(pivot[0], 0, pivot[1]);
        Matrix4f mat = poseStack.last().pose();
        boolean round = PondWalls.isTracked(self.getPos());
        // one render type at a time: a buffer from the source ends when another is asked for. First the round wall and
        // pier in the casings' textures (their blocks are hidden exactly then)...
        if (round) {
            TextureAtlas atlas = Minecraft.getInstance().getModelManager().getAtlas(InventoryMenu.BLOCK_ATLAS);
            Gfx textured = new Gfx(buffer.getBuffer(RenderType.entityCutoutNoCull(InventoryMenu.BLOCK_ATLAS)), mat,
                    light);
            drawRoundWall(textured, atlas.getSprite(WALL_TEXTURE));
            drawPier(textured, atlas.getSprite(PIER_TEXTURE));
            drawHouseWalls(textured, atlas.getSprite(WALL_TEXTURE), houseAngle);
        }
        // ...then the plain-coloured solids...
        Gfx solid = new Gfx(buffer.getBuffer(RenderType.entityCutoutNoCull(AF9RenderTypes.WHITE)), mat, light);
        if (round) {
            drawTrack(solid);
            drawHouseRoof(solid, houseAngle);
        } else {
            drawCoping(solid);
        }
        drawTrough(solid, troughAngle);
        solid.rotate(turn.angle);
        drawBridge(solid);
        // ...then the see-through water and the scum on it
        VertexConsumer water = buffer.getBuffer(AF9RenderTypes.LIGHT_RING);
        drawWater(water, mat, level, centre, now, machine.rakeSpinning(), round);
        drawFoam(water, mat, turn);
        poseStack.popPose();
    }

    /** Moves the bridge on by the time since it was last drawn: up to speed while a recipe runs, down to a stop after. */
    private static Turn advance(MetaMachine self, boolean spinning, double now) {
        Turn turn = TURNS.computeIfAbsent(self, key -> new Turn());
        if (Double.isNaN(turn.last) || now < turn.last || now - turn.last > 40) turn.last = now;
        float dt = (float) (now - turn.last);
        turn.last = now;
        float target = spinning ? 1F : 0F;
        turn.speed += (target - turn.speed) * Math.min(1F, dt * RAMP);
        if (target == 0F && turn.speed < 0.003F) turn.speed = 0F;
        turn.angle = (turn.angle + turn.speed * dt * Mth.TWO_PI / REVOLUTION_TICKS) % Mth.TWO_PI;
        turn.foam = (turn.foam + turn.speed * dt / 360F) % 1F;
        return turn;
    }

    // ------------------------------------------------------------------------------------------------ the parts

    /**
     * The round wall: a ring of casing, one block per layer (two facets to a block, so the texture runs on seamlessly),
     * the outer face from the foot to the top, the inner face from the floor's top, the top between them.
     */
    private static void drawRoundWall(Gfx g, TextureAtlasSprite sprite) {
        g.rotate(0F);
        wallFaces(g, sprite, WALL_OUT, 0, 3, true);
        wallFaces(g, sprite, WALL_IN, 1, 3, false);
        float mid = (WALL_IN + WALL_OUT) / 2;
        int n = facets(mid);
        for (int i = 0; i < n; i++) {
            float a0 = Mth.TWO_PI * i / n, a1 = Mth.TWO_PI * (i + 1) / n;
            float ua = i % 2 == 0 ? 0F : 0.5F, ub = ua + 0.5F;
            g.quadTex(v3(WALL_IN * Mth.cos(a0), RIM_Y, WALL_IN * Mth.sin(a0)),
                    v3(WALL_IN * Mth.cos(a1), RIM_Y, WALL_IN * Mth.sin(a1)),
                    v3(WALL_OUT * Mth.cos(a1), RIM_Y, WALL_OUT * Mth.sin(a1)),
                    v3(WALL_OUT * Mth.cos(a0), RIM_Y, WALL_OUT * Mth.sin(a0)), v3(0, 1, 0), sprite, ua, 0F, ub, 1F);
        }
    }

    /** An even number of facets round a circle, each about half a block wide. */
    private static int facets(float radius) {
        int n = Math.round(Mth.TWO_PI * radius * 2F);
        return n + (n % 2);
    }

    private static void wallFaces(Gfx g, TextureAtlasSprite sprite, float radius, int fromY, int toY, boolean outward) {
        int n = facets(radius);
        for (int y = fromY; y < toY; y++) {
            for (int i = 0; i < n; i++) {
                float a0 = Mth.TWO_PI * i / n, a1 = Mth.TWO_PI * (i + 1) / n, am = (a0 + a1) / 2;
                float ua = i % 2 == 0 ? 0F : 0.5F, ub = ua + 0.5F;
                float[] normal = outward ? v3(Mth.cos(am), 0, Mth.sin(am)) : v3(-Mth.cos(am), 0, -Mth.sin(am));
                g.quadTex(v3(radius * Mth.cos(a0), y, radius * Mth.sin(a0)),
                        v3(radius * Mth.cos(a1), y, radius * Mth.sin(a1)),
                        v3(radius * Mth.cos(a1), y + 1, radius * Mth.sin(a1)),
                        v3(radius * Mth.cos(a0), y + 1, radius * Mth.sin(a0)), normal, sprite, ua, 1F, ub, 0F);
            }
        }
    }

    /** The round pier in place of the plus of casings: a 16-sided column from the floor to the turntable. */
    private static void drawPier(Gfx g, TextureAtlasSprite sprite) {
        g.rotate(0F);
        int n = facets(PIER_R);
        wallFaces(g, sprite, PIER_R, 1, 4, true);
        for (int i = 0; i < n; i++) {
            float a0 = Mth.TWO_PI * i / n, a1 = Mth.TWO_PI * (i + 1) / n;
            g.quadTex(v3(0, PIER_Y, 0), v3(PIER_R * Mth.cos(a0), PIER_Y, PIER_R * Mth.sin(a0)),
                    v3(PIER_R * Mth.cos(a1), PIER_Y, PIER_R * Mth.sin(a1)),
                    v3(PIER_R * Mth.cos(a1), PIER_Y, PIER_R * Mth.sin(a1)), v3(0, 1, 0), sprite, 0.3F, 0.3F, 0.7F,
                    0.7F);
        }
    }

    /**
     * The machine house's walls (the controller's frame turned into the world by {@code angle}): casing tiles a hair
     * behind the faces of the real blocks, so the controller and the hatches stand out of them.
     */
    private static void drawHouseWalls(Gfx g, TextureAtlasSprite sprite, float angle) {
        g.rotate(angle);
        float zf = HOUSE_Z0 + INSET, zb = HOUSE_Z1 - INSET, xs = HOUSE_HALF - INSET;
        for (int y = 0; y < (int) HOUSE_H; y++) {
            for (int i = -2; i <= 2; i++) {
                g.quadTex(v3(i - 0.5F, y, zf), v3(i + 0.5F, y, zf), v3(i + 0.5F, y + 1, zf), v3(i - 0.5F, y + 1, zf),
                        v3(0, 0, -1), sprite, 0F, 1F, 1F, 0F);
                g.quadTex(v3(i + 0.5F, y, zb), v3(i - 0.5F, y, zb), v3(i - 0.5F, y + 1, zb), v3(i + 0.5F, y + 1, zb),
                        v3(0, 0, 1), sprite, 0F, 1F, 1F, 0F);
            }
            for (int k = 0; k < 3; k++) {
                float z0 = HOUSE_Z0 + k, z1 = z0 + 1;
                g.quadTex(v3(-xs, y, z1), v3(-xs, y, z0), v3(-xs, y + 1, z0), v3(-xs, y + 1, z1), v3(-1, 0, 0),
                        sprite, 0F, 1F, 1F, 0F);
                g.quadTex(v3(xs, y, z0), v3(xs, y, z1), v3(xs, y + 1, z1), v3(xs, y + 1, z0), v3(1, 0, 0), sprite,
                        0F, 1F, 1F, 0F);
            }
        }
    }

    /**
     * The machine house's roof: a zinc gable roof with its ridge towards the pond, eaves, fascia, gutters, a vent in the
     * front gable and a stack on the roof. Everything stands above the blocks' top, so no hatch is covered.
     */
    private static void drawHouseRoof(Gfx g, float angle) {
        g.rotate(angle);
        float zf = EAVE_Z, zb = HOUSE_Z1;
        float slope = (RIDGE_Y - EAVE_Y) / EAVE_X;
        float inv = 1F / Mth.sqrt(1F + slope * slope);
        for (int side = -1; side <= 1; side += 2) {
            float x = side * EAVE_X;
            g.quad(v3(x, EAVE_Y, zf), v3(x, EAVE_Y, zb), v3(0, RIDGE_Y, zb), v3(0, RIDGE_Y, zf),
                    v3(side * slope * inv, inv, 0), ZINC);
            g.rod(v3(x, EAVE_Y, zf), v3(0, RIDGE_Y, zf), 0.04F, FASCIA);
            g.rod(v3(x, EAVE_Y - 0.03F, zf), v3(x, EAVE_Y - 0.03F, zb), 0.05F, GALVANIZED);
        }
        g.rod(v3(0, RIDGE_Y + 0.02F, zf), v3(0, RIDGE_Y + 0.02F, zb), 0.07F, STEEL);
        // the gables (front and back), a louvred vent in the front one
        float gx = HOUSE_HALF + 0.01F;
        for (float z : new float[] { HOUSE_Z0 + 0.02F, HOUSE_Z1 - 0.02F }) {
            g.quad(v3(-gx, HOUSE_H, z), v3(gx, HOUSE_H, z), v3(0, RIDGE_Y - 0.05F, z), v3(0, RIDGE_Y - 0.05F, z),
                    v3(0, 0, z < -7F ? -1 : 1), FASCIA);
        }
        g.prism(v3(0, 3.55F, HOUSE_Z0 + 0.02F), v3(0, 3.55F, HOUSE_Z0 - 0.04F), 0.42F, 8, DARK);
        g.prism(v3(0, 3.55F, HOUSE_Z0 - 0.04F), v3(0, 3.55F, HOUSE_Z0 - 0.08F), 0.3F, 8, STEEL);
        // the roof stack with its rain cap
        float sx = 1.3F, sz = -6.7F, base = RIDGE_Y - sx * slope - 0.1F;
        g.prism(v3(sx, base, sz), v3(sx, base + 1.5F, sz), 0.26F, 8, STEEL);
        g.prism(v3(sx, base + 1.5F, sz), v3(sx, base + 1.6F, sz), 0.42F, 8, DARK);
    }

    /** The carriage's track: a steel rail on the round wall's top. */
    private static void drawTrack(Gfx g) {
        g.rotate(0F);
        int n = 72;
        for (int i = 0; i < n; i++) {
            float a0 = Mth.TWO_PI * i / n, a1 = Mth.TWO_PI * (i + 1) / n, am = (a0 + a1) / 2;
            float c0 = Mth.cos(a0), s0 = Mth.sin(a0), c1 = Mth.cos(a1), s1 = Mth.sin(a1);
            g.quad(v3(TRACK_IN * c0, COPING_Y, TRACK_IN * s0), v3(TRACK_OUT * c0, COPING_Y, TRACK_OUT * s0),
                    v3(TRACK_OUT * c1, COPING_Y, TRACK_OUT * s1), v3(TRACK_IN * c1, COPING_Y, TRACK_IN * s1),
                    v3(0, 1, 0), STEEL);
            g.quad(v3(TRACK_OUT * c0, RIM_Y, TRACK_OUT * s0), v3(TRACK_OUT * c1, RIM_Y, TRACK_OUT * s1),
                    v3(TRACK_OUT * c1, COPING_Y, TRACK_OUT * s1), v3(TRACK_OUT * c0, COPING_Y, TRACK_OUT * s0),
                    v3(Mth.cos(am), 0, Mth.sin(am)), DARK);
            g.quad(v3(TRACK_IN * c0, RIM_Y, TRACK_IN * s0), v3(TRACK_IN * c1, RIM_Y, TRACK_IN * s1),
                    v3(TRACK_IN * c1, COPING_Y, TRACK_IN * s1), v3(TRACK_IN * c0, COPING_Y, TRACK_IN * s0),
                    v3(-Mth.cos(am), 0, -Mth.sin(am)), DARK);
        }
    }

    /** A smooth round coping on the rim: it covers the steps of the block circle from above. */
    private static void drawCoping(Gfx g) {
        g.rotate(0F);
        int segments = 56;
        for (int i = 0; i < segments; i++) {
            float a0 = Mth.TWO_PI * i / segments, a1 = Mth.TWO_PI * (i + 1) / segments, am = (a0 + a1) / 2;
            float c0 = Mth.cos(a0), s0 = Mth.sin(a0), c1 = Mth.cos(a1), s1 = Mth.sin(a1);
            g.quad(v3(COPING_IN * c0, COPING_Y, COPING_IN * s0), v3(COPING_OUT * c0, COPING_Y, COPING_OUT * s0),
                    v3(COPING_OUT * c1, COPING_Y, COPING_OUT * s1), v3(COPING_IN * c1, COPING_Y, COPING_IN * s1),
                    v3(0, 1, 0), CONCRETE);
            g.quad(v3(COPING_OUT * c0, RIM_Y, COPING_OUT * s0), v3(COPING_OUT * c1, RIM_Y, COPING_OUT * s1),
                    v3(COPING_OUT * c1, COPING_Y, COPING_OUT * s1), v3(COPING_OUT * c0, COPING_Y, COPING_OUT * s0),
                    v3(Mth.cos(am), 0, Mth.sin(am)), STEEL);
            g.quad(v3(COPING_IN * c0, RIM_Y, COPING_IN * s0), v3(COPING_IN * c1, RIM_Y, COPING_IN * s1),
                    v3(COPING_IN * c1, COPING_Y, COPING_IN * s1), v3(COPING_IN * c0, COPING_Y, COPING_IN * s0),
                    v3(-Mth.cos(am), 0, -Mth.sin(am)), STEEL);
        }
    }

    /** The scum trough at the wall (a fixed spot, to the controller's left): a funnel the skimmer pushes the scum into. */
    private static void drawTrough(Gfx g, float angle) {
        g.rotate(angle);
        g.aabb(3.95F, 1.30F, -0.5F, 4.35F, 1.40F, 0.5F, STAINLESS);
        g.aabb(3.95F, 1.30F, -0.5F, 4.35F, 1.72F, -0.42F, STAINLESS);
        g.aabb(3.95F, 1.30F, 0.42F, 4.35F, 1.72F, 0.5F, STAINLESS);
        g.aabb(4.27F, 1.30F, -0.5F, 4.35F, 1.72F, 0.5F, STAINLESS);
        g.aabb(3.95F, 1.30F, -0.5F, 4.03F, 1.52F, 0.5F, STAINLESS);
        g.rod(v3(3.95F, 1.72F, -0.46F), v3(3.55F, 1.72F, -0.85F), 0.025F, STAINLESS);
        g.rod(v3(3.95F, 1.72F, 0.46F), v3(3.55F, 1.72F, 0.85F), 0.025F, STAINLESS);
        g.rod(v3(4.31F, 1.40F, 0F), v3(4.31F, 0.95F, 0F), 0.07F, STEEL);  // the drain
    }

    /** The scraper bridge, in its own frame (u along the arm from the pier, y up, v across): turned by the caller. */
    private static void drawBridge(Gfx g) {
        // the turntable on the pier
        g.prism(v3(0, PIER_Y - 0.02F, 0), v3(0, PIER_Y + 0.12F, 0), 1.0F, 16, STEEL);
        g.prism(v3(0, PIER_Y + 0.12F, 0), v3(0, PIER_Y + 0.7F, 0), 0.8F, 12, GALVANIZED);
        g.aabb(-0.32F, PIER_Y + 0.7F, -0.26F, 0.32F, PIER_Y + 1.1F, 0.26F, MOTOR);
        // the deck
        g.aabb(BRIDGE_IN, PIER_Y, -HALF_WIDTH, BRIDGE_OUT, DECK_Y, HALF_WIDTH, GRATING);
        // two lattice girders with handrails: chords, posts, diagonals
        int bays = 6;
        float step = (BRIDGE_OUT - BRIDGE_IN) / bays;
        for (int side = -1; side <= 1; side += 2) {
            float v = side * HALF_WIDTH;
            g.rod(v3(BRIDGE_IN, DECK_Y + 0.03F, v), v3(BRIDGE_OUT, DECK_Y + 0.03F, v), 0.035F, GALVANIZED);
            g.rod(v3(BRIDGE_IN, RAIL_Y, v), v3(BRIDGE_OUT, RAIL_Y, v), 0.035F, GALVANIZED);
            g.rod(v3(BRIDGE_IN, (DECK_Y + RAIL_Y) / 2, v), v3(BRIDGE_OUT, (DECK_Y + RAIL_Y) / 2, v), 0.02F, GALVANIZED);
            for (int i = 0; i <= bays; i++) {
                float u = BRIDGE_IN + step * i;
                g.rod(v3(u, DECK_Y, v), v3(u, RAIL_Y, v), 0.03F, GALVANIZED);
                if (i < bays) {
                    boolean up = i % 2 == 0;
                    g.rod(v3(u, up ? DECK_Y : RAIL_Y, v), v3(u + step, up ? RAIL_Y : DECK_Y, v), 0.022F, STEEL);
                }
            }
        }
        for (int i = 0; i <= bays; i += 2) {
            float u = BRIDGE_IN + step * i;
            g.rod(v3(u, RAIL_Y, -HALF_WIDTH), v3(u, RAIL_Y, HALF_WIDTH), 0.025F, STEEL);
        }
        // the drive carriage on the rim: legs, a bogie with two wheels on the coping, a motor
        for (int side = -1; side <= 1; side += 2) {
            float v = side * HALF_WIDTH;
            g.rod(v3(BRIDGE_OUT - 0.1F, COPING_Y + 0.34F, v), v3(BRIDGE_OUT - 0.1F, DECK_Y, v), 0.05F, STEEL);
            g.prism(v3(BRIDGE_OUT - 0.3F, COPING_Y + 0.16F, side * 0.2F),
                    v3(BRIDGE_OUT - 0.04F, COPING_Y + 0.16F, side * 0.2F), 0.16F, 10, DARK);
        }
        g.aabb(BRIDGE_OUT - 0.5F, COPING_Y + 0.3F, -0.36F, BRIDGE_OUT + 0.12F, COPING_Y + 0.42F, 0.36F, STEEL);
        g.aabb(BRIDGE_OUT + 0.05F, COPING_Y + 0.46F, -0.18F, BRIDGE_OUT + 0.42F, COPING_Y + 0.86F, 0.18F, MOTOR);
        g.prism(v3(BRIDGE_OUT - 0.1F, COPING_Y + 0.65F, 0), v3(BRIDGE_OUT + 0.05F, COPING_Y + 0.65F, 0), 0.14F, 8, DARK);
        // the skimmer: an angled blade at the water line (its rubber lip just under the surface), on three struts
        float du = BLADE_U1 - BLADE_U0, dv = BLADE_V1 - BLADE_V0;
        g.blade(v3(BLADE_U0, BLADE_Y, BLADE_V0), v3(BLADE_U1, BLADE_Y, BLADE_V1), BLADE_HALF_HEIGHT, 0.025F, STAINLESS);
        g.blade(v3(BLADE_U0, BLADE_Y - BLADE_HALF_HEIGHT + 0.03F, BLADE_V0),
                v3(BLADE_U1, BLADE_Y - BLADE_HALF_HEIGHT + 0.03F, BLADE_V1), 0.04F, 0.035F, RUBBER);
        for (float t : new float[] { 0.15F, 0.55F, 0.95F }) {
            float u = BLADE_U0 + du * t, v = BLADE_V0 + dv * t;
            g.rod(v3(u, DECK_Y, HALF_WIDTH), v3(u, BLADE_Y + BLADE_HALF_HEIGHT, v), 0.03F, STEEL);
        }
    }

    /** Scum on the water: patches that slide along the blade to the trough while the bridge turns. */
    private static void drawFoam(VertexConsumer vc, Matrix4f mat, Turn turn) {
        float cos = Mth.cos(turn.angle), sin = Mth.sin(turn.angle);
        for (int i = 0; i < FOAM_PATCHES; i++) {
            float p = (turn.foam + i / (float) FOAM_PATCHES) % 1F;
            float u = Mth.lerp(p, BLADE_U0, BLADE_U1) + 0.12F * ((i * 37 % 5) / 5F - 0.4F);
            float v = Mth.lerp(p, BLADE_V0, BLADE_V1) + 0.14F + 0.05F * (i * 53 % 4);
            float size = 0.12F + 0.04F * (i * 29 % 4);
            float x = u * cos - v * sin, z = u * sin + v * cos;
            flat(vc, mat, x - size, WATER_Y + 0.01F, z - size, x + size, z + size, (FOAM >> 16) & 0xFF,
                    (FOAM >> 8) & 0xFF, FOAM & 0xFF, 0.6F * Mth.sin(p * Mth.PI));
        }
    }

    /** The water: a darker layer above the floor and a surface with a ripple, on the pond's own cells. */
    private void drawWater(VertexConsumer vc, Matrix4f mat, ClientLevel level,
                           Vec3 centre, double time, boolean working, boolean round) {
        int tint = FALLBACK_WATER;
        try {
            tint = level.getBiome(BlockPos.containing(centre)).value().getSpecialEffects().getWaterColor();
        } catch (RuntimeException ignored) {
            // biome not ready: keep the fallback
        }
        int r = (tint >> 16) & 0xFF, g = (tint >> 8) & 0xFF, b = tint & 0xFF;
        float ripple = 0.5F + 0.5F * Mth.sin((float) (time * 2 * Math.PI / 46));
        float surfaceAlpha = working ? 0.58F + 0.06F * ripple : 0.46F + 0.04F * ripple;
        if (round) {
            // a disc up to under the round wall (its inner face hides the edge)
            int segments = 64;
            for (int i = 0; i < segments; i++) {
                float a0 = Mth.TWO_PI * i / segments, a1 = Mth.TWO_PI * (i + 1) / segments;
                float x0 = WATER_R * Mth.cos(a0), z0 = WATER_R * Mth.sin(a0);
                float x1 = WATER_R * Mth.cos(a1), z1 = WATER_R * Mth.sin(a1);
                fan(vc, mat, DEEP_Y, x0, z0, x1, z1, r / 2, g / 2, b / 2, 0.55F);
                fan(vc, mat, WATER_Y, x0, z0, x1, z1, r, g, b, surfaceAlpha);
            }
            return;
        }
        for (int dx = -POND_CELLS; dx <= POND_CELLS; dx++) {
            for (int dz = -POND_CELLS; dz <= POND_CELLS; dz++) {
                // the pier only: the plus the pattern builds, not the whole 3 x 3 (its diagonal hollows hold water)
                if ((dx == 0 || dz == 0) && Math.abs(dx) <= 1 && Math.abs(dz) <= 1) continue;
                if (Math.sqrt(dx * dx + dz * dz) >= POND_RADIUS) continue;
                float x0 = dx - 0.5F, x1 = dx + 0.5F, z0 = dz - 0.5F, z1 = dz + 0.5F;
                flat(vc, mat, x0, DEEP_Y, z0, x1, z1, r / 2, g / 2, b / 2, 0.55F);
                flat(vc, mat, x0, WATER_Y, z0, x1, z1, r, g, b, surfaceAlpha);
            }
        }
    }

    /** A triangle of a disc round the pond's axis (a quad with a repeated corner). */
    private static void fan(VertexConsumer vc, Matrix4f mat, float y, float x0, float z0, float x1, float z1, int r,
                            int g, int b, float alpha) {
        float fr = r / 255F, fg = g / 255F, fb = b / 255F;
        vc.vertex(mat, 0, y, 0).color(fr, fg, fb, alpha).endVertex();
        vc.vertex(mat, x0, y, z0).color(fr, fg, fb, alpha).endVertex();
        vc.vertex(mat, x1, y, z1).color(fr, fg, fb, alpha).endVertex();
        vc.vertex(mat, x1, y, z1).color(fr, fg, fb, alpha).endVertex();
    }

    private static void flat(VertexConsumer vc, Matrix4f mat, float x0, float y, float z0, float x1, float z1, int r,
                             int g, int b, float alpha) {
        float fr = r / 255F, fg = g / 255F, fb = b / 255F;
        vc.vertex(mat, x0, y, z0).color(fr, fg, fb, alpha).endVertex();
        vc.vertex(mat, x1, y, z0).color(fr, fg, fb, alpha).endVertex();
        vc.vertex(mat, x1, y, z1).color(fr, fg, fb, alpha).endVertex();
        vc.vertex(mat, x0, y, z1).color(fr, fg, fb, alpha).endVertex();
    }

    private static float[] v3(float x, float y, float z) {
        return new float[] { x, y, z };
    }

    /** Solid shapes (entity shader, white texture, lit by their normals) in a frame turned about the pond's axis. */
    private static final class Gfx {

        private static final float[] AX = { 1, 0, 0 }, AY = { 0, 1, 0 }, AZ = { 0, 0, 1 };

        private final VertexConsumer c;
        private final Matrix4f mat;
        private final int light;
        private float cos = 1F, sin = 0F;

        Gfx(VertexConsumer c, Matrix4f mat, int light) {
            this.c = c;
            this.mat = mat;
            this.light = light;
        }

        void rotate(float angle) {
            cos = Mth.cos(angle);
            sin = Mth.sin(angle);
        }

        private void vertex(float[] p, float[] n, int rgb) {
            float x = p[0] * cos - p[2] * sin, z = p[0] * sin + p[2] * cos;
            float nx = n[0] * cos - n[2] * sin, nz = n[0] * sin + n[2] * cos;
            c.vertex(mat, x, p[1], z).color(((rgb >> 16) & 0xFF) / 255F, ((rgb >> 8) & 0xFF) / 255F,
                    (rgb & 0xFF) / 255F, 1F).uv(0.5F, 0.5F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light)
                    .normal(nx, n[1], nz).endVertex();
        }

        /** A quad on a block atlas sprite: the corners take (u0, v0), (u1, v0), (u1, v1), (u0, v1) of its fraction. */
        void quadTex(float[] a, float[] b, float[] d, float[] e, float[] n, TextureAtlasSprite sprite, float u0,
                     float v0, float u1, float v1) {
            float su = sprite.getU1() - sprite.getU0(), sv = sprite.getV1() - sprite.getV0();
            vertexTex(a, n, sprite.getU0() + su * u0, sprite.getV0() + sv * v0);
            vertexTex(b, n, sprite.getU0() + su * u1, sprite.getV0() + sv * v0);
            vertexTex(d, n, sprite.getU0() + su * u1, sprite.getV0() + sv * v1);
            vertexTex(e, n, sprite.getU0() + su * u0, sprite.getV0() + sv * v1);
        }

        private void vertexTex(float[] p, float[] n, float u, float v) {
            float x = p[0] * cos - p[2] * sin, z = p[0] * sin + p[2] * cos;
            float nx = n[0] * cos - n[2] * sin, nz = n[0] * sin + n[2] * cos;
            c.vertex(mat, x, p[1], z).color(1F, 1F, 1F, 1F).uv(u, v).overlayCoords(OverlayTexture.NO_OVERLAY)
                    .uv2(light).normal(nx, n[1], nz).endVertex();
        }

        /** A quad (both sides are drawn) with the outward normal n. */
        void quad(float[] a, float[] b, float[] d, float[] e, float[] n, int rgb) {
            vertex(a, n, rgb);
            vertex(b, n, rgb);
            vertex(d, n, rgb);
            vertex(e, n, rgb);
        }

        private void face(float[] centre, float[] n, float hn, float[] p, float hp, float[] q, float hq, int rgb) {
            float[] f = add(centre, scale(n, hn));
            float[] a = add(add(f, scale(p, hp)), scale(q, hq));
            float[] b = add(add(f, scale(p, hp)), scale(q, -hq));
            float[] d = add(add(f, scale(p, -hp)), scale(q, -hq));
            float[] e = add(add(f, scale(p, -hp)), scale(q, hq));
            quad(a, b, d, e, n, rgb);
        }

        /** A box round a centre, with half sizes along three perpendicular unit axes. */
        void box(float[] centre, float[] u, float[] v, float[] w, float hu, float hv, float hw, int rgb) {
            face(centre, u, hu, v, hv, w, hw, rgb);
            face(centre, scale(u, -1), hu, v, hv, w, hw, rgb);
            face(centre, v, hv, u, hu, w, hw, rgb);
            face(centre, scale(v, -1), hv, u, hu, w, hw, rgb);
            face(centre, w, hw, u, hu, v, hv, rgb);
            face(centre, scale(w, -1), hw, u, hu, v, hv, rgb);
        }

        void aabb(float x0, float y0, float z0, float x1, float y1, float z1, int rgb) {
            box(v3((x0 + x1) / 2, (y0 + y1) / 2, (z0 + z1) / 2), AX, AY, AZ, (x1 - x0) / 2, (y1 - y0) / 2,
                    (z1 - z0) / 2, rgb);
        }

        /** A square rod from a to b. */
        void rod(float[] a, float[] b, float half, int rgb) {
            float[] w = sub(b, a);
            float len = length(w);
            if (len < 1e-5F) return;
            w = scale(w, 1F / len);
            float[] u = across(w);
            float[] v = cross(w, u);
            box(scale(add(a, b), 0.5F), u, v, w, half, half, len / 2, rgb);
        }

        /** A thin upright plate from a to b (centre height), half its height and thickness. */
        void blade(float[] a, float[] b, float halfHeight, float halfThickness, int rgb) {
            float[] w = sub(b, a);
            float len = length(w);
            w = scale(w, 1F / len);
            float[] n = cross(w, AY);
            box(scale(add(a, b), 0.5F), n, AY, w, halfThickness, halfHeight, len / 2, rgb);
        }

        /** A regular n-sided prism round the line from a to b. */
        void prism(float[] a, float[] b, float radius, int n, int rgb) {
            float[] w = sub(b, a);
            w = scale(w, 1F / length(w));
            float[] u = across(w);
            float[] v = cross(w, u);
            for (int k = 0; k < n; k++) {
                float t0 = Mth.TWO_PI * k / n, t1 = Mth.TWO_PI * (k + 1) / n, tm = (t0 + t1) / 2;
                float[] d0 = add(scale(u, Mth.cos(t0)), scale(v, Mth.sin(t0)));
                float[] d1 = add(scale(u, Mth.cos(t1)), scale(v, Mth.sin(t1)));
                float[] p0 = add(a, scale(d0, radius)), p1 = add(a, scale(d1, radius));
                float[] q0 = add(b, scale(d0, radius)), q1 = add(b, scale(d1, radius));
                quad(p0, p1, q1, q0, add(scale(u, Mth.cos(tm)), scale(v, Mth.sin(tm))), rgb);
                quad(a, p0, p1, p1, scale(w, -1), rgb);
                quad(b, q0, q1, q1, w, rgb);
            }
        }

        private static float[] add(float[] a, float[] b) {
            return new float[] { a[0] + b[0], a[1] + b[1], a[2] + b[2] };
        }

        private static float[] sub(float[] a, float[] b) {
            return new float[] { a[0] - b[0], a[1] - b[1], a[2] - b[2] };
        }

        private static float[] scale(float[] a, float s) {
            return new float[] { a[0] * s, a[1] * s, a[2] * s };
        }

        private static float length(float[] a) {
            return Mth.sqrt(a[0] * a[0] + a[1] * a[1] + a[2] * a[2]);
        }

        private static float[] cross(float[] a, float[] b) {
            return new float[] { a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0] };
        }

        /** A unit vector perpendicular to the unit vector w. */
        private static float[] across(float[] w) {
            float[] ref = Math.abs(w[1]) > 0.95F ? AX : AY;
            float[] u = cross(ref, w);
            return scale(u, 1F / length(u));
        }
    }
}
