package com.af9.core.client.render;

import com.af9.core.AF9Core;
import com.af9.core.machine.AgriSimMachine;
import com.af9.core.machine.IAgriSimMachine;

import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine;
import com.gregtechceu.gtceu.api.pattern.util.RelativeDirection;
import com.gregtechceu.gtceu.client.renderer.machine.DynamicRender;
import com.gregtechceu.gtceu.client.renderer.machine.DynamicRenderManager;
import com.gregtechceu.gtceu.client.renderer.machine.DynamicRenderType;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.Codec;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The Agriculture Simulation Bed's garden: while a recipe runs, the seed it simulates stands on every soil block of
 * the bed, in its growth stage (the recipe's progress, per run when batching). The plants are the real crop blocks of
 * the seeds (a seed item is a {@code BlockItem} of its crop; Mystical Agriculture's essence crops tint their own
 * flowers), drawn through the block renderer one block above the soil, so each looks as it does on a field. Idle, the
 * garden is bare. Nothing is placed in the world; the draw is skipped in a shader pack's shadow pass and beyond the
 * view distance, and the render bounds let the frustum cull the whole garden.
 * <p>
 * The soil blocks are found on the client by scanning the garden's cells round the controller (the bed's own
 * {@link AgriSimMachine#soilLevel}), kept for a second. Model side: {@code AF9MachineModels.workableCasingWithAgriSim}.
 */
@SuppressWarnings("removal") // new ResourceLocation(String) is the only constructor on 1.20.1
public class AgriSimRender extends DynamicRender<IAgriSimMachine, AgriSimRender> {

    public static final Codec<AgriSimRender> CODEC = Codec.unit(AgriSimRender::new);
    public static final DynamicRenderType<IAgriSimMachine, AgriSimRender> TYPE = new DynamicRenderType<>(CODEC);

    /** The garden's middle, relative to the controller: this many cells behind it. */
    private static final float MIDDLE_BACK = AgriSimMachine.GARDEN_BACK / 2F;
    /** The soil cells are rescanned this often (ticks). */
    private static final long SCAN_TICKS = 20;

    /** The soil cells of each bed (offsets from the controller in world axes), by controller position. */
    private final Map<BlockPos, Garden> gardens = new HashMap<>();
    /** The crop block of each seed id (null: the seed plants no crop). */
    private final Map<String, Block> crops = new HashMap<>();

    public AgriSimRender() {}

    /** Typed as GT's base class, so common code that builds a model never loads this client class. */
    public static DynamicRender<?, ?> create() {
        return new AgriSimRender();
    }

    /** Client, before the models are built (mod construction). */
    public static void register() {
        DynamicRenderManager.register(new ResourceLocation(AF9Core.MOD_ID, "agri_sim"), TYPE);
    }

    @Override
    public DynamicRenderType<IAgriSimMachine, AgriSimRender> getType() {
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

    /** The middle of the garden, one block above its soil. */
    private Vec3 centre(MetaMachine self) {
        Direction back = dir(self, RelativeDirection.BACK);
        Direction up = dir(self, RelativeDirection.UP);
        BlockPos pos = self.getPos();
        return new Vec3(pos.getX() + 0.5 + back.getStepX() * MIDDLE_BACK + up.getStepX() * (AgriSimMachine.GARDEN_UP + 1),
                pos.getY() + 0.5 + back.getStepY() * MIDDLE_BACK + up.getStepY() * (AgriSimMachine.GARDEN_UP + 1),
                pos.getZ() + 0.5 + back.getStepZ() * MIDDLE_BACK + up.getStepZ() * (AgriSimMachine.GARDEN_UP + 1));
    }

    @Override
    public boolean shouldRender(IAgriSimMachine machine, Vec3 cameraPos) {
        return machine.isAgriFormed() && !machine.getAgriSeed().isEmpty()
                && centre(machine.self()).closerThan(cameraPos, getViewDistance());
    }

    @Override
    public boolean shouldRenderOffScreen(IAgriSimMachine machine) {
        return false;
    }

    @Override
    public AABB getRenderBoundingBox(IAgriSimMachine machine) {
        Vec3 c = centre(machine.self());
        return new AABB(c.x - 7, c.y - 2, c.z - 7, c.x + 7, c.y + 2, c.z + 7);
    }

    @Override
    public void render(IAgriSimMachine machine, float partialTick, PoseStack poseStack, MultiBufferSource buffer,
                       int packedLight, int packedOverlay) {
        if (!machine.isAgriFormed() || machine.getAgriSeed().isEmpty()) return;
        if (IrisCompat.renderingShadowPass()) return;
        Level level = Minecraft.getInstance().level;
        if (level == null) return;
        Block crop = cropOf(machine.getAgriSeed());
        if (crop == null) return;
        BlockState state = grown(crop, machine.getAgriGrowth());

        MetaMachine self = machine.self();
        BlockPos origin = self.getPos();
        Garden garden = gardens.get(origin);
        if (garden == null || level.getGameTime() - garden.scanned >= SCAN_TICKS) {
            if (gardens.size() > 64) gardens.clear();
            garden = scan(self, level);
            gardens.put(origin.immutable(), garden);
        }

        var dispatcher = Minecraft.getInstance().getBlockRenderer();
        for (BlockPos offset : garden.cells) {
            poseStack.pushPose();
            poseStack.translate(offset.getX(), offset.getY(), offset.getZ());
            int light = LevelRenderer.getLightColor(level, origin.offset(offset));
            try {
                dispatcher.renderSingleBlock(state, poseStack, buffer, light, OverlayTexture.NO_OVERLAY);
            } catch (RuntimeException e) {
                // a crop whose model or tint will not draw outside the world: leave it out, never crash the frame
                crops.put(machine.getAgriSeed(), null);
                poseStack.popPose();
                return;
            }
            poseStack.popPose();
        }
    }

    /** The cells above the garden's soil blocks, as offsets from the controller. */
    private static Garden scan(MetaMachine self, Level level) {
        Direction up = dir(self, RelativeDirection.UP);
        Direction back = dir(self, RelativeDirection.BACK);
        Direction left = dir(self, RelativeDirection.LEFT);
        List<BlockPos> cells = new ArrayList<>();
        BlockPos origin = self.getPos();
        for (int depth = 0; depth <= AgriSimMachine.GARDEN_BACK; depth++) {
            for (int side = -AgriSimMachine.GARDEN_SIDE; side <= AgriSimMachine.GARDEN_SIDE; side++) {
                BlockPos soil = origin.relative(up, AgriSimMachine.GARDEN_UP).relative(back, depth).relative(left, side);
                if (AgriSimMachine.soilLevel(level.getBlockState(soil)) < 0) continue;
                BlockPos crop = soil.relative(up);
                if (!level.getBlockState(crop).isAir()) continue;
                cells.add(crop.subtract(origin));
            }
        }
        return new Garden(level.getGameTime(), cells);
    }

    /** The crop block a seed plants, or null. */
    private Block cropOf(String seedId) {
        if (crops.containsKey(seedId)) return crops.get(seedId);
        Block crop = null;
        try {
            Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(seedId));
            if (item instanceof BlockItem blockItem) crop = blockItem.getBlock();
        } catch (RuntimeException ignored) {
            // a malformed id
        }
        crops.put(seedId, crop);
        return crop;
    }

    /** The crop at the given growth (0-1000): its age property spread over the stages it has. */
    private static BlockState grown(Block crop, int growth) {
        BlockState state = crop.defaultBlockState();
        for (Property<?> property : state.getProperties()) {
            if (property instanceof IntegerProperty age && age.getName().equals("age")) {
                int max = age.getPossibleValues().stream().mapToInt(Integer::intValue).max().orElse(0);
                int min = age.getPossibleValues().stream().mapToInt(Integer::intValue).min().orElse(0);
                int stage = min + Math.min(max - min, (int) (growth / 1000.0 * (max - min + 1)));
                return state.setValue(age, stage);
            }
        }
        return state;
    }

    private record Garden(long scanned, List<BlockPos> cells) {}
}
