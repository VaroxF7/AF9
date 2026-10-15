package com.af9.core.client.skin;

import com.af9.core.AF9Core;

import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.feature.IRecipeLogicMachine;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiController;
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic;
import com.gregtechceu.gtceu.api.pattern.util.RelativeDirection;
import com.gregtechceu.gtceu.client.renderer.machine.DynamicRender;
import com.gregtechceu.gtceu.client.renderer.machine.DynamicRenderManager;
import com.gregtechceu.gtceu.client.renderer.machine.DynamicRenderType;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.client.model.data.ModelData;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.serialization.Codec;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Draws a {@link StructureSkin}: the model of a formed multiblock, in place of the plain blocks that
 * {@link StructureSkins} stops drawing. The model is authored in the pattern's own space (right / up / back from the
 * controller), so one basis, taken from the controller's facing, puts it into the world.
 * <p>
 * Two things of the model are swapped at draw time, by texture: the coil band takes the texture of the coil the player
 * built with (read from the block in the structure), and the window takes the lit or the cold texture; the parts of the
 * model that glow are drawn at full brightness while the machine works.
 */
@SuppressWarnings("removal") // new ResourceLocation(ns, path) is the only constructor on 1.20.1
public class StructureSkinRender extends DynamicRender<IMultiController, StructureSkinRender> {

    // spotless:off
    public static final Codec<StructureSkinRender> CODEC = Codec.unit(StructureSkinRender::new);
    // spotless:on
    public static final DynamicRenderType<IMultiController, StructureSkinRender> TYPE = new DynamicRenderType<>(CODEC);

    private static final ResourceLocation COIL_MARKER = new ResourceLocation(AF9Core.MOD_ID,
            "block/multiblock/ebf/coil_marker");
    private static final ResourceLocation GLOW_ON = new ResourceLocation(AF9Core.MOD_ID,
            "block/multiblock/ebf/glow_on");
    private static final ResourceLocation GLOW_OFF = new ResourceLocation(AF9Core.MOD_ID,
            "block/multiblock/ebf/glow_off");
    private static final ResourceLocation GLOW_WAIT = new ResourceLocation(AF9Core.MOD_ID,
            "block/multiblock/ebf/glow_wait");
    private static final int FULL_BRIGHT = 0xF000F0;

    /**
     * Embeddium animates only the sprites it has seen drawn, and it sees the ones of its own chunk meshes: a sprite that
     * only a block entity renderer draws (the window's heat) stands still unless it is marked. Null without Embeddium.
     */
    private static final MethodHandle MARK_ACTIVE = markActive();

    private static MethodHandle markActive() {
        try {
            Class<?> util = Class.forName("me.jellysquid.mods.sodium.client.render.texture.SpriteUtil");
            return MethodHandles.publicLookup().unreflect(util.getMethod("markSpriteActive", TextureAtlasSprite.class));
        } catch (ReflectiveOperationException | LinkageError | RuntimeException e) {
            return null;
        }
    }

    private static void markActive(TextureAtlasSprite sprite) {
        if (MARK_ACTIVE == null) return;
        try {
            MARK_ACTIVE.invoke(sprite);
        } catch (Throwable ignored) {
            // an animation that stands still is the worst of it
        }
    }

    /** The model's quads with the sprites swapped, by (model, coil sprite, glow sprite). Client thread. */
    private static final Map<List<Object>, List<BakedQuad>> QUADS = new HashMap<>();

    /** Client, before the models are built (mod construction). */
    public static void register() {
        DynamicRenderManager.register(new ResourceLocation(AF9Core.MOD_ID, "structure_skin"), TYPE);
    }

    static void clearCaches() {
        QUADS.clear();
    }

    @Override
    public DynamicRenderType<IMultiController, StructureSkinRender> getType() {
        return TYPE;
    }

    @Override
    public int getViewDistance() {
        return 112;
    }

    @Override
    public boolean shouldRenderOffScreen(IMultiController machine) {
        return true;
    }

    /** The structure's whole box: a section-wise visibility check would clip the model when only its top is in view. */
    @Override
    public AABB getRenderBoundingBox(IMultiController machine) {
        StructureSkin skin = StructureSkins.skinOf(machine.self().getDefinition());
        BlockPos pos = machine.self().getPos();
        if (skin == null) return super.getRenderBoundingBox(machine);
        AABB box = new AABB(pos);
        if (skin.cabinet() != null) {
            int length = Math.max(StructureSkins.lengthOf(pos), skin.cabinet().maxLength());
            return box.minmax(new AABB(StructureSkins.at(machine.self(), -skin.right(), -skin.cabinet().below(), 0)))
                    .minmax(new AABB(StructureSkins.at(machine.self(), skin.right(), skin.up(), length))).inflate(1);
        }
        for (int right : new int[] { -skin.right(), skin.right() }) {
            for (int up : new int[] { 0, skin.up() - 1 }) {
                for (int back : new int[] { 0, skin.back() - 1 }) {
                    box = box.minmax(new AABB(StructureSkins.at(machine.self(), right, up, back)));
                }
            }
        }
        return box.inflate(1);
    }

    @Override
    public void render(IMultiController machine, float partialTick, PoseStack poseStack, MultiBufferSource buffer,
                       int packedLight, int packedOverlay) {
        MetaMachine self = machine.self();
        BlockPos pos = self.getPos();
        if (!machine.isFormed()) {
            StructureSkins.untrack(pos);
            return;
        }
        // the blocks are hidden when the controller is tracked: draw the model exactly then
        if (!StructureSkins.isTracked(pos)) return;
        StructureSkin skin = StructureSkins.skinOf(self.getDefinition());
        ClientLevel level = Minecraft.getInstance().level;
        if (skin == null || level == null) return;

        TextureAtlas atlas = Minecraft.getInstance().getModelManager().getAtlas(InventoryMenu.BLOCK_ATLAS);
        TextureAtlasSprite marker = atlas.getSprite(COIL_MARKER);
        TextureAtlasSprite coil = marker;
        if (skin.coilProbe() != null) {
            int[] p = skin.coilProbe();
            var state = level.getBlockState(StructureSkins.at(self, p[0], p[1], p[2]));
            if (!state.isAir()) {
                TextureAtlasSprite found = Minecraft.getInstance().getBlockRenderer().getBlockModel(state)
                        .getParticleIcon(ModelData.EMPTY);
                if (found != null) coil = found;
            }
        }
        // the machine's state: working (coils lit, window alive), waiting for power or room (an ember), or cold
        boolean working = machine instanceof IRecipeLogicMachine logic && logic.isActive();
        boolean waiting = !working && machine instanceof IRecipeLogicMachine logic
                && logic.getRecipeLogic().getStatus() == RecipeLogic.Status.WAITING;
        TextureAtlasSprite glowOn = atlas.getSprite(GLOW_ON);
        TextureAtlasSprite glow = working ? glowOn : atlas.getSprite(waiting ? GLOW_WAIT : GLOW_OFF);
        markActive(glow);
        // the coil's own bloom texture, drawn over the coil bands at full brightness while it works
        TextureAtlasSprite bloom = null;
        if (working && coil != marker) {
            ResourceLocation name = coil.contents().name();
            TextureAtlasSprite found = atlas.getSprite(
                    new ResourceLocation(name.getNamespace(), name.getPath() + "_bloom"));
            if (!found.contents().name().equals(MissingTextureAtlasSprite.getLocation())) bloom = found;
        }

        Direction front = self.getFrontFacing(), upwards = self.getUpwardsFacing();
        Direction r = RelativeDirection.RIGHT.getRelative(front, upwards, false);
        Direction u = RelativeDirection.UP.getRelative(front, upwards, false);
        Direction b = RelativeDirection.BACK.getRelative(front, upwards, false);
        Matrix4f basis = new Matrix4f(
                r.getStepX(), r.getStepY(), r.getStepZ(), 0,
                u.getStepX(), u.getStepY(), u.getStepZ(), 0,
                b.getStepX(), b.getStepY(), b.getStepZ(), 0,
                0, 0, 0, 1);

        poseStack.pushPose();
        // the model's origin is the controller block's minimum corner; turn about the block's middle
        poseStack.translate(0.5, 0.5, 0.5);
        poseStack.last().pose().mul(basis);
        poseStack.last().normal().mul(new Matrix3f(basis));
        poseStack.translate(-0.5, -0.5, -0.5);

        if (skin.cabinet() != null) {
            int length = StructureSkins.lengthOf(pos);
            if (length > 0) {
                CabinetDraw.draw(self, skin, length, poseStack, buffer, packedOverlay,
                        maxLight(openAirLight(level, self, 0), openAirLight(level, self, 2)));
            }
            poseStack.popPose();
            return;
        }
        // the controller's own block is solid wall, which has no light of its own: take the light of the open air in
        // front of the structure, per half (the lower and the upper two blocks)
        int[] lights = { openAirLight(level, self, 0), openAirLight(level, self, 2) };

        // one render type at a time: a buffer from the source ends when another is asked for, so every pass gets its
        // consumer when it starts. Pass 1: the bodies and the parts that light up, entity shader (lit by the normals)
        VertexConsumer solid = buffer.getBuffer(RenderType.entityCutoutNoCull(InventoryMenu.BLOCK_ATLAS));
        for (int half = 0; half < 2; half++) {
            poseStack.pushPose();
            poseStack.translate(0, half * 2, 0);
            ResourceLocation body = half == 0 ? skin.lower() : skin.upper();
            ResourceLocation lit = half == 0 ? skin.glowLower() : skin.glowUpper();
            draw(poseStack, solid, quads(body, marker, coil, glowOn, glow), lights[half], packedOverlay);
            draw(poseStack, solid, quads(lit, marker, coil, glowOn, glow), working ? FULL_BRIGHT : lights[half],
                    packedOverlay);
            poseStack.popPose();
        }
        // Pass 2: the coil's bloom over the coil bands, at full brightness
        if (bloom != null) {
            markActive(bloom);
            VertexConsumer lightsUp = buffer.getBuffer(RenderType.entityTranslucentEmissive(InventoryMenu.BLOCK_ATLAS));
            for (int half = 0; half < 2; half++) {
                poseStack.pushPose();
                poseStack.translate(0, half * 2, 0);
                ResourceLocation body = half == 0 ? skin.lower() : skin.upper();
                draw(poseStack, lightsUp, bloomQuads(body, marker, bloom), FULL_BRIGHT, packedOverlay);
                poseStack.popPose();
            }
        }
        poseStack.popPose();
    }

    /** The quads through the pose (the entity shader lights them by their normals, so no shade is baked in). */
    private static void draw(PoseStack poseStack, VertexConsumer consumer, List<BakedQuad> quads, int light,
                             int overlay) {
        PoseStack.Pose pose = poseStack.last();
        for (BakedQuad quad : quads) {
            consumer.putBulkData(pose, quad, 1F, 1F, 1F, light, overlay);
        }
    }

    /**
     * The light in front of the structure at {@code up} blocks above the controller, and above it: the brightest of the
     * open blocks there. The controller's own position is a solid block and reads as dark.
     */
    private static int openAirLight(ClientLevel level, MetaMachine controller, int up) {
        int best = 0;
        for (int dy = 0; dy <= 2; dy++) {
            for (int back : new int[] { -1, 0 }) {
                BlockPos at = StructureSkins.at(controller, 0, up + dy, back);
                if (back == 0 && !level.getBlockState(at).isAir()) continue;
                int light = LevelRenderer.getLightColor(level, at);
                best = maxLight(best, light);
            }
        }
        // the cell above the whole structure, for the case that the front is walled in
        best = maxLight(best, LevelRenderer.getLightColor(level, StructureSkins.at(controller, 0, 5, 0)));
        return best;
    }

    /** The brighter of two packed lights, sky and block channel separately. */
    private static int maxLight(int a, int b) {
        int sky = Math.max(LightTexture.sky(a), LightTexture.sky(b));
        int block = Math.max(LightTexture.block(a), LightTexture.block(b));
        return LightTexture.pack(block, sky);
    }

    /** A model's quads with the coil marker and the window's sprite replaced, cached. */
    private static List<BakedQuad> quads(ResourceLocation location, TextureAtlasSprite marker,
                                         TextureAtlasSprite coil, TextureAtlasSprite glowOn,
                                         TextureAtlasSprite glow) {
        return QUADS.computeIfAbsent(List.of(location, coil, glow), key -> {
            BakedModel model = Minecraft.getInstance().getModelManager().getModel(location);
            RandomSource random = RandomSource.create(42);
            List<BakedQuad> all = new ArrayList<>();
            all.addAll(model.getQuads(null, null, random, ModelData.EMPTY, null));
            for (Direction side : Direction.values()) {
                all.addAll(model.getQuads(null, side, random, ModelData.EMPTY, null));
            }
            List<BakedQuad> out = new ArrayList<>(all.size());
            for (BakedQuad quad : all) {
                TextureAtlasSprite from = quad.getSprite();
                TextureAtlasSprite to = from == marker ? coil : from == glowOn ? glow : from;
                out.add(to == from ? quad : retextured(quad, from, to));
            }
            return out;
        });
    }

    /** Only the coil band's quads of a model, on the coil's bloom sprite. */
    private static List<BakedQuad> bloomQuads(ResourceLocation location, TextureAtlasSprite marker,
                                              TextureAtlasSprite bloom) {
        return QUADS.computeIfAbsent(List.of(location, bloom, marker), key -> {
            BakedModel model = Minecraft.getInstance().getModelManager().getModel(location);
            RandomSource random = RandomSource.create(42);
            List<BakedQuad> out = new ArrayList<>();
            List<BakedQuad> all = new ArrayList<>(model.getQuads(null, null, random, ModelData.EMPTY, null));
            for (Direction side : Direction.values()) {
                all.addAll(model.getQuads(null, side, random, ModelData.EMPTY, null));
            }
            for (BakedQuad quad : all) {
                if (quad.getSprite() == marker) out.add(retextured(quad, marker, bloom));
            }
            return out;
        });
    }

    /** The quad on another sprite: each vertex keeps its place on the old sprite, now on the new one. */
    private static BakedQuad retextured(BakedQuad quad, TextureAtlasSprite from, TextureAtlasSprite to) {
        int[] vertices = quad.getVertices().clone();
        float fromU = from.getU1() - from.getU0(), fromV = from.getV1() - from.getV0();
        float toU = to.getU1() - to.getU0(), toV = to.getV1() - to.getV0();
        for (int vertex = 0; vertex < 4; vertex++) {
            int at = vertex * 8 + 4;
            float u = Float.intBitsToFloat(vertices[at]), v = Float.intBitsToFloat(vertices[at + 1]);
            float nu = to.getU0() + (fromU == 0 ? 0 : (u - from.getU0()) / fromU) * toU;
            float nv = to.getV0() + (fromV == 0 ? 0 : (v - from.getV0()) / fromV) * toV;
            vertices[at] = Float.floatToRawIntBits(nu);
            vertices[at + 1] = Float.floatToRawIntBits(nv);
        }
        return new BakedQuad(vertices, quad.getTintIndex(), quad.getDirection(), to, quad.isShade());
    }
}
