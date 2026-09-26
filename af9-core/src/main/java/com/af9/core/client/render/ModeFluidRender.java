package com.af9.core.client.render;

import com.af9.core.AF9Core;

import com.gregtechceu.gtceu.api.machine.feature.multiblock.IFluidRenderMulti;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.client.renderer.block.FluidBlockRenderer;
import com.gregtechceu.gtceu.client.renderer.machine.DynamicRender;
import com.gregtechceu.gtceu.client.renderer.machine.DynamicRenderManager;
import com.gregtechceu.gtceu.client.renderer.machine.DynamicRenderType;
import com.gregtechceu.gtceu.client.util.RenderUtil;
import com.gregtechceu.gtceu.config.ConfigHolder;

import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.client.RenderTypeHelper;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.registries.ForgeRegistries;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Fake fluid inside a running multiblock, chosen by its machine mode (the recipe type of the recipe it runs): the
 * fluid fills the machine's {@link IFluidRenderMulti#getFluidOffsets() fluid blocks} as one volume, drawn on every
 * outer face, so it shows through glass walls. Nothing is drawn while the machine is idle, or for a mode without a
 * fluid. The offsets are relative to the controller, so the volume turns with it.
 * <p>
 * Model side: {@link com.af9.core.machine.AF9MachineModels#workableCasingWithModeFluids}.
 */
@SuppressWarnings("removal") // new ResourceLocation(ns, path) is the only constructor on 1.20.1
public class ModeFluidRender extends DynamicRender<IFluidRenderMulti, ModeFluidRender> {

    // spotless:off
    public static final Codec<ModeFluidRender> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            FluidBlockRenderer.CODEC.forGetter(ModeFluidRender::getFluidBlockRenderer),
            Codec.unboundedMap(ResourceLocation.CODEC, ResourceLocation.CODEC).fieldOf("mode_fluids")
                    .forGetter(ModeFluidRender::getModeFluids)
    ).apply(instance, ModeFluidRender::new));
    // spotless:on
    public static final DynamicRenderType<IFluidRenderMulti, ModeFluidRender> TYPE = new DynamicRenderType<>(CODEC);

    /** The faces sit this far inside the fluid blocks: off the glass walls, the surface below the ceiling. */
    private static final float INSET = 1 / 16f;

    private final FluidBlockRenderer fluidBlockRenderer;
    /** Recipe type id -> fluid id. */
    private final Map<ResourceLocation, ResourceLocation> modeFluids;
    private final Map<ResourceLocation, Optional<Fluid>> fluids = new HashMap<>();
    private final Map<Direction, Vector3f[]> faceVertices = new EnumMap<>(Direction.class);

    public ModeFluidRender(FluidBlockRenderer fluidBlockRenderer, Map<ResourceLocation, ResourceLocation> modeFluids) {
        this.fluidBlockRenderer = fluidBlockRenderer;
        this.modeFluids = Map.copyOf(modeFluids);
        for (Direction face : Direction.values()) {
            faceVertices.put(face, fluidBlockRenderer.transformVertices(RenderUtil.getVertices(face), face));
        }
    }

    /** Typed as GT's base class, so common code that builds a model never loads this client class. */
    public static DynamicRender<?, ?> create(Map<ResourceLocation, ResourceLocation> modeFluids) {
        return new ModeFluidRender(FluidBlockRenderer.Builder.create().setFaceOffset(-INSET).getRenderer(),
                modeFluids);
    }

    /** Client, before the models are built (mod construction). */
    public static void register() {
        DynamicRenderManager.register(new ResourceLocation(AF9Core.MOD_ID, "mode_fluid_area"), TYPE);
    }

    public FluidBlockRenderer getFluidBlockRenderer() {
        return fluidBlockRenderer;
    }

    public Map<ResourceLocation, ResourceLocation> getModeFluids() {
        return modeFluids;
    }

    @Override
    public DynamicRenderType<IFluidRenderMulti, ModeFluidRender> getType() {
        return TYPE;
    }

    @Override
    public int getViewDistance() {
        return 64;
    }

    @Override
    public void render(IFluidRenderMulti machine, float partialTick, PoseStack poseStack, MultiBufferSource buffer,
                       int packedLight, int packedOverlay) {
        if (!ConfigHolder.INSTANCE.client.renderer.renderFluids) return;
        if (!machine.isFormed() || !machine.isActive()) return;
        // the machine mode is not synced to the client; the running recipe (synced) is of that mode
        GTRecipe recipe = machine.getRecipeLogic().getLastRecipe();
        if (recipe == null || recipe.recipeType == null) return;
        Fluid fluid = fluidOf(recipe.recipeType.registryName);
        if (fluid == null) return;
        Set<BlockPos> offsets = machine.getFluidOffsets();
        if (offsets.isEmpty()) return;

        var extensions = IClientFluidTypeExtensions.of(fluid);
        TextureAtlasSprite sprite = RenderUtil.FluidTextureType.STILL.map(extensions);
        float u0 = sprite.getU0(), u1 = sprite.getU1(), v0 = sprite.getV0(), v1 = sprite.getV1();
        int color = extensions.getTintColor();
        int r = FastColor.ARGB32.red(color), g = FastColor.ARGB32.green(color), b = FastColor.ARGB32.blue(color),
                a = FastColor.ARGB32.alpha(color);
        VertexConsumer consumer = buffer.getBuffer(RenderTypeHelper.getEntityRenderType(
                ItemBlockRenderTypes.getRenderLayer(fluid.defaultFluidState()), false));
        BlockPos origin = machine.self().getPos();

        for (BlockPos offset : offsets) {
            int light = RenderUtil.getFluidLight(fluid, origin.offset(offset));
            poseStack.pushPose();
            poseStack.translate(offset.getX(), offset.getY(), offset.getZ());
            Matrix4f pose = poseStack.last().pose();
            for (Direction side : Direction.values()) {
                if (offsets.contains(offset.relative(side))) continue; // inside the volume
                // GT's face table names the side faces by the direction they are seen from
                Direction face = side.getAxis().isHorizontal() ? side.getOpposite() : side;
                fluidBlockRenderer.drawFace(pose, consumer, faceVertices.get(face), RenderUtil.getNormal(face),
                        u0, u1, v0, v1, r, g, b, a, packedOverlay, light);
            }
            poseStack.popPose();
        }
    }

    private Fluid fluidOf(ResourceLocation recipeType) {
        ResourceLocation id = modeFluids.get(recipeType);
        if (id == null) return null;
        return fluids.computeIfAbsent(id, key -> {
            Fluid fluid = ForgeRegistries.FLUIDS.getValue(key);
            if (fluid == null || fluid == Fluids.EMPTY) {
                AF9Core.LOGGER.warn("Machine fluid render: no fluid {}", key);
                return Optional.empty();
            }
            return Optional.of(fluid);
        }).orElse(null);
    }

    @Override
    public boolean shouldRenderOffScreen(IFluidRenderMulti machine) {
        return true;
    }

    @Override
    public AABB getRenderBoundingBox(IFluidRenderMulti machine) {
        BlockPos origin = machine.self().getPos();
        AABB box = new AABB(origin);
        for (BlockPos offset : machine.getFluidOffsets()) {
            box = box.minmax(new AABB(origin.offset(offset)));
        }
        return box;
    }
}
