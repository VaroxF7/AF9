package com.af9.core.client.render;

import com.af9.core.AF9Core;

import com.gregtechceu.gtceu.api.capability.recipe.ItemRecipeCapability;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IWorkableMultiController;
import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.content.Content;
import com.gregtechceu.gtceu.client.renderer.machine.DynamicRender;
import com.gregtechceu.gtceu.client.renderer.machine.DynamicRenderManager;
import com.gregtechceu.gtceu.client.renderer.machine.DynamicRenderType;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.mojang.serialization.Codec;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import java.util.Arrays;

/**
 * The High Temperature Precision Fabricator's crucible, ported from TerraFirmaGreg's {@code BouleRender} and its
 * {@code quartz_crucible} block model (TerraFirmaGreg-Team/Core-Modern, LGPL-3.0; credits in CREDITS-textures.md,
 * licence in licenses/). TFG's crucible is a block in the structure; here the hall is closed, so the cup stands on
 * the hall's roof, behind the controller on top of the gearbox: the quartz crucible (its model's six boxes, its two
 * textures), a melt of polysilicon in it while a recipe runs, and the holder rod and the boule, which the machine
 * dips in and pulls out (the same movement as TFG's: dip, then pull with the progress while the rod turns).
 * Drawn only while the structure is formed.
 */
@SuppressWarnings("removal") // new ResourceLocation(ns, path) is the only constructor on 1.20.1
public class CrucibleRender extends DynamicRender<IWorkableMultiController, CrucibleRender> {

    public static final Codec<CrucibleRender> CODEC = Codec.unit(CrucibleRender::new);
    public static final DynamicRenderType<IWorkableMultiController, CrucibleRender> TYPE = new DynamicRenderType<>(
            CODEC);

    private static final ResourceLocation SIDE = new ResourceLocation(AF9Core.MOD_ID,
            "textures/entity/precision_fabricator/quartz_crucible_side.png");
    private static final ResourceLocation TOP = new ResourceLocation(AF9Core.MOD_ID,
            "textures/entity/precision_fabricator/quartz_crucible_top.png");
    private static final TagKey<Item> HOLDER_RODS = TagKey.create(Registries.ITEM,
            new ResourceLocation(AF9Core.MOD_ID, "precision_fabricator_holder_rods"));
    /** The cup stands this many blocks above the controller's floor and behind it (the hall's roof, over the gearbox). */
    private static final float ROOF = 2F, BEHIND = 1F;

    /** Typed as GT's base class, so common code that builds a model never loads this client class. */
    public static DynamicRender<?, ?> create() {
        return new CrucibleRender();
    }

    /** Client, before the models are built (mod construction). */
    public static void register() {
        DynamicRenderManager.register(new ResourceLocation(AF9Core.MOD_ID, "crucible"), TYPE);
    }

    @Override
    public DynamicRenderType<IWorkableMultiController, CrucibleRender> getType() {
        return TYPE;
    }

    @Override
    public int getViewDistance() {
        return 32;
    }

    @Override
    public boolean shouldRenderOffScreen(IWorkableMultiController machine) {
        return false;
    }

    @Override
    public AABB getRenderBoundingBox(IWorkableMultiController machine) {
        BlockPos pos = machine.self().getPos();
        return new AABB(pos.getX() - 2, pos.getY(), pos.getZ() - 2, pos.getX() + 3, pos.getY() + 4, pos.getZ() + 3);
    }

    @Override
    public void render(IWorkableMultiController machine, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight, int packedOverlay) {
        if (!machine.isFormed()) return;
        if (IrisCompat.renderingShadowPass()) return;
        var level = Minecraft.getInstance().level;
        if (level == null) return;
        MultiblockControllerMachine self = machine.self();
        BlockPos at = self.getPos().relative(self.getFrontFacing().getOpposite()).above((int) ROOF);
        int light = LevelRenderer.getLightColor(level, at);

        poseStack.pushPose();
        // the frame of the controller facing north: +z is behind it (TFG's rotation)
        float rotation = 180F - self.getFrontFacing().toYRot();
        poseStack.translate(0.5F, 0F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(rotation));
        poseStack.translate(-0.5F, 0F, -0.5F);
        poseStack.translate(0F, ROOF, BEHIND);

        drawCrucible(poseStack, buffer, light);

        GTRecipe recipe = machine.getRecipeLogic().getLastRecipe();
        if (recipe != null && machine.isActive()) {
            double progress = machine.getRecipeLogic().getProgressPercent();
            drawMelt(poseStack, buffer, light);
            drawItems(machine, recipe, progress, poseStack, buffer, light, packedOverlay);
        }
        poseStack.popPose();
    }

    /** The quartz crucible's model: a 12 x 2 x 12 floor and four 2 thick walls round it, the top face of each wall the rim. */
    private void drawCrucible(PoseStack poseStack, MultiBufferSource buffer, int light) {
        VertexConsumer side = buffer.getBuffer(RenderType.entityCutoutNoCull(SIDE));
        VertexConsumer top = buffer.getBuffer(RenderType.entityCutoutNoCull(TOP));
        PoseStack.Pose pose = poseStack.last();
        box(top, top, pose, 2, 0, 2, 14, 2, 14, light);
        box(side, top, pose, 0, 0, 0, 16, 16, 2, light);
        box(side, top, pose, 0, 0, 14, 16, 16, 16, light);
        box(side, top, pose, 0, 0, 0, 2, 16, 16, light);
        box(side, top, pose, 14, 0, 0, 16, 16, 16, light);
    }

    /** The polysilicon melt: a flat glowing square in the cup, just under the rim. */
    private void drawMelt(PoseStack poseStack, MultiBufferSource buffer, int light) {
        VertexConsumer vc = buffer.getBuffer(RenderType.entityCutoutNoCull(TOP));
        PoseStack.Pose pose = poseStack.last();
        float y = 12F / 16F;
        float[][] corners = { { 2, 2 }, { 2, 14 }, { 14, 14 }, { 14, 2 } };
        for (float[] c : corners) {
            vc.vertex(pose.pose(), c[0] / 16F, y, c[1] / 16F).color(1F, 0.55F, 0.2F, 1F).uv(c[0] / 16F, c[1] / 16F)
                    .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(0xF000F0).normal(pose.normal(), 0F, 1F, 0F)
                    .endVertex();
        }
    }

    /** TFG's movement: the rod (with the seed) dips in while the progress is under 5 %, then rod and boule are pulled out. */
    private void drawItems(IWorkableMultiController machine, GTRecipe recipe, double progress, PoseStack poseStack,
                           MultiBufferSource buffer, int light, int overlay) {
        var level = machine.self().getLevel();
        var renderer = Minecraft.getInstance().getItemRenderer();
        ItemStack rod = recipe.getInputContents(ItemRecipeCapability.CAP).stream().map(Content::getContent)
                .map(ItemRecipeCapability.CAP::of).map(Ingredient::getItems).flatMap(Arrays::stream)
                .filter(stack -> stack.is(HOLDER_RODS)).findFirst().orElse(ItemStack.EMPTY);
        var outputs = recipe.getOutputContents(ItemRecipeCapability.CAP);
        ItemStack output = outputs.isEmpty() ? ItemStack.EMPTY :
                ItemRecipeCapability.CAP.of(outputs.get(0).getContent()).getItems()[0];
        float turn = (float) (progress * 36000F);

        if (progress < 0.05) {
            if (!rod.isEmpty()) {
                poseStack.pushPose();
                poseStack.translate(0.5, 1.2 - progress * 20, 0.5);
                poseStack.mulPose(Axis.YP.rotationDegrees(turn));
                poseStack.mulPose(Axis.ZP.rotationDegrees(-45));
                poseStack.scale(1.05F, 1.05F, 1.05F);
                renderer.renderStatic(rod, ItemDisplayContext.FIXED, light, overlay, poseStack, buffer, level, 0);
                poseStack.popPose();
            }
        } else {
            if (!rod.isEmpty()) {
                poseStack.pushPose();
                poseStack.translate(0.5, 0.2 + progress, 0.5);
                poseStack.mulPose(Axis.YP.rotationDegrees(turn));
                poseStack.mulPose(Axis.ZP.rotationDegrees(-45));
                renderer.renderStatic(rod, ItemDisplayContext.FIXED, light, overlay, poseStack, buffer, level, 0);
                poseStack.popPose();
            }
            if (!output.isEmpty()) {
                poseStack.pushPose();
                poseStack.translate(0.5, 0.15 + progress, 0.5);
                poseStack.mulPose(Axis.YP.rotationDegrees(turn + 90F));
                poseStack.scale(0.9F, 0.9F, 0.9F);
                renderer.renderStatic(output, ItemDisplayContext.FIXED, light, overlay, poseStack, buffer, level, 0);
                poseStack.popPose();
            }
        }
    }

    /** A box in sixteenths: its up face from {@code topVc}, the others from {@code sideVc}; no culling. */
    private static void box(VertexConsumer sideVc, VertexConsumer topVc, PoseStack.Pose pose, float x0, float y0,
                            float z0, float x1, float y1, float z1, int light) {
        Matrix4f m = pose.pose();
        Matrix3f n = pose.normal();
        float a = x0 / 16F, b = y0 / 16F, c = z0 / 16F, d = x1 / 16F, e = y1 / 16F, f = z1 / 16F;
        // up, down, north (z0), south (z1), west (x0), east (x1)
        quad(topVc, m, n, light, 0, 1, 0, a, e, c, a, e, f, d, e, f, d, e, c, x0, z0, x1, z1);
        quad(sideVc, m, n, light, 0, -1, 0, a, b, c, d, b, c, d, b, f, a, b, f, x0, z0, x1, z1);
        quad(sideVc, m, n, light, 0, 0, -1, a, b, c, a, e, c, d, e, c, d, b, c, x0, y0, x1, y1);
        quad(sideVc, m, n, light, 0, 0, 1, d, b, f, d, e, f, a, e, f, a, b, f, x0, y0, x1, y1);
        quad(sideVc, m, n, light, -1, 0, 0, a, b, f, a, e, f, a, e, c, a, b, c, z0, y0, z1, y1);
        quad(sideVc, m, n, light, 1, 0, 0, d, b, c, d, e, c, d, e, f, d, b, f, z0, y0, z1, y1);
    }

    /** One quad: four corners, the texture window (u0, v0, u1, v1) in sixteenths stretched over it. */
    private static void quad(VertexConsumer vc, Matrix4f m, Matrix3f n, int light, float nx, float ny, float nz,
                             float ax, float ay, float az, float bx, float by, float bz, float cx, float cy, float cz,
                             float dx, float dy, float dz, float u0, float v0, float u1, float v1) {
        float[][] corners = { { ax, ay, az, u0, v0 }, { bx, by, bz, u0, v1 }, { cx, cy, cz, u1, v1 },
                { dx, dy, dz, u1, v0 } };
        for (float[] p : corners) {
            vc.vertex(m, p[0], p[1], p[2]).color(1F, 1F, 1F, 1F).uv(p[3] / 16F, p[4] / 16F)
                    .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(n, nx, ny, nz).endVertex();
        }
    }
}
