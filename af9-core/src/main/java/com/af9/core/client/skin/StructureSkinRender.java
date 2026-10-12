package com.af9.core.client.skin;

import com.af9.core.AF9Core;

import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.feature.IRecipeLogicMachine;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiController;
import com.gregtechceu.gtceu.api.pattern.util.RelativeDirection;
import com.gregtechceu.gtceu.client.renderer.machine.DynamicRender;
import com.gregtechceu.gtceu.client.renderer.machine.DynamicRenderManager;
import com.gregtechceu.gtceu.client.renderer.machine.DynamicRenderType;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
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
    private static final int FULL_BRIGHT = 0xF000F0;

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
        boolean working = machine instanceof IRecipeLogicMachine logic && logic.isActive();
        TextureAtlasSprite glowOn = atlas.getSprite(GLOW_ON);
        TextureAtlasSprite glow = working ? glowOn : atlas.getSprite(GLOW_OFF);

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

        VertexConsumer solid = buffer.getBuffer(RenderType.cutout());
        for (int half = 0; half < 2; half++) {
            poseStack.pushPose();
            poseStack.translate(0, half * 2, 0);
            ResourceLocation body = half == 0 ? skin.lower() : skin.upper();
            ResourceLocation lit = half == 0 ? skin.glowLower() : skin.glowUpper();
            draw(poseStack, solid, level, quads(body, marker, coil, glowOn, glow), r, u, b, packedLight,
                    packedOverlay, false);
            draw(poseStack, solid, level, quads(lit, marker, coil, glowOn, glow), r, u, b,
                    working ? FULL_BRIGHT : packedLight, packedOverlay, true);
            poseStack.popPose();
        }
        poseStack.popPose();
    }

    private static void draw(PoseStack poseStack, VertexConsumer consumer, ClientLevel level, List<BakedQuad> quads,
                             Direction r, Direction u, Direction b, int light, int overlay, boolean glowing) {
        PoseStack.Pose pose = poseStack.last();
        for (BakedQuad quad : quads) {
            float shade = 1F;
            if (!glowing) {
                Direction local = quad.getDirection();
                // the face's direction in the world: the basis maps the model's axes
                int x = local.getStepX() * r.getStepX() + local.getStepY() * u.getStepX() + local.getStepZ() * b.getStepX();
                int y = local.getStepX() * r.getStepY() + local.getStepY() * u.getStepY() + local.getStepZ() * b.getStepY();
                int z = local.getStepX() * r.getStepZ() + local.getStepY() * u.getStepZ() + local.getStepZ() * b.getStepZ();
                shade = level.getShade(Direction.getNearest(x, y, z), quad.isShade());
            }
            consumer.putBulkData(pose, quad, shade, shade, shade, light, overlay);
        }
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
