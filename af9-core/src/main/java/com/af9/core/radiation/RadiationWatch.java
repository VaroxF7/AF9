package com.af9.core.radiation;

import com.af9.core.AF9Config;
import com.af9.core.AF9Core;

import com.gregtechceu.gtceu.api.blockentity.MetaMachineBlockEntity;
import com.gregtechceu.gtceu.api.data.chemical.ChemicalHelper;
import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.data.chemical.material.properties.HazardProperty;
import com.gregtechceu.gtceu.api.data.chemical.material.properties.PropertyKey;
import com.gregtechceu.gtceu.api.block.MaterialBlock;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableMultiblockMachine;
import com.gregtechceu.gtceu.common.data.GTMedicalConditions;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

/**
 * A warning above the hotbar while a player is near radioactive material.
 * <p>
 * Radioactive is what GregTech calls radioactive: a material with a carcinogenic hazard that anything triggers
 * ({@code Material.Builder#radioactiveHazard}: plutonium, uranium, the Brannerite of the asteroids ...), or an element
 * with a half-life. Counted are the player's inventory, dropped items within {@value #ITEM_RANGE} blocks, ore blocks of
 * such a material within {@value #BLOCK_RANGE} blocks and running FX-1 Reactors within {@value #REACTOR_RANGE} blocks;
 * items of the tag {@code #af9:radioactive} (fuel pellets and rods, the spent ones, the reactor's waste) count too. The
 * warning says how strong the source is and whether the player wears a full hazmat suit; the damage itself is GT's
 * (its hazard system works on the inventory), this only tells the player before it happens.
 * <p>
 * Setting: {@link AF9Config#RADIATION_HINTS}.
 */
@Mod.EventBusSubscriber(modid = AF9Core.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
@SuppressWarnings("removal") // new ResourceLocation(ns, path) is the only constructor on 1.20.1
public final class RadiationWatch {

    /** Items that are radioactive without being GT materials. */
    public static final TagKey<Item> RADIOACTIVE_ITEMS = ItemTags.create(
            new ResourceLocation(AF9Core.MOD_ID, "radioactive"));
    /** The controller of the FX-1 Reactor (kubejs/startup_scripts/gtceu/asteroid_fission.js). */
    public static final ResourceLocation REACTOR_ID = new ResourceLocation("gtceu", "fx1_reactor");

    public static final int INTERVAL = 20;
    public static final double ITEM_RANGE = 6.0;
    public static final int BLOCK_RANGE = 3;
    public static final double REACTOR_RANGE = 16.0;
    /** Strength of a running reactor right next to it (it falls off with the distance). */
    public static final double REACTOR_STRENGTH = 8.0;

    /** Exposure below which the warning is the mild one, and from which it is the severe one. */
    private static final double MILD = 2.5;
    private static final double SEVERE = 7.0;

    private RadiationWatch() {}

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.side != LogicalSide.SERVER) return;
        if (!(event.player instanceof ServerPlayer player)) return;
        if (player.tickCount % INTERVAL != 3 || player.isSpectator()) return;
        if (!AF9Config.RADIATION_HINTS.get()) return;

        double exposure = exposure(player);
        if (exposure <= 0) return;
        boolean protectedFully = HazardProperty.ProtectionType.FULL.isProtected(player);
        String key = exposure >= SEVERE ? "af9.radiation.severe" : exposure >= MILD ? "af9.radiation.strong" :
                "af9.radiation.mild";
        ChatFormatting color = exposure >= SEVERE ? ChatFormatting.RED : exposure >= MILD ? ChatFormatting.GOLD :
                ChatFormatting.YELLOW;
        Component message = Component.translatable(key).withStyle(color)
                .append(Component.literal("  "))
                .append(Component.translatable(protectedFully ? "af9.radiation.protected" :
                        "af9.radiation.unprotected").withStyle(protectedFully ? ChatFormatting.GREEN :
                        ChatFormatting.GRAY));
        player.displayClientMessage(message, true);
    }

    /** How much radiation is around the player: 1 per radioactive stack, less from dropped items, more from reactors. */
    public static double exposure(ServerPlayer player) {
        double total = 0;
        Inventory inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (isRadioactive(stack)) total += 1.0 + Math.min(stack.getCount(), 64) / 64.0;
        }
        ServerLevel level = player.serverLevel();
        AABB around = player.getBoundingBox().inflate(ITEM_RANGE);
        for (ItemEntity entity : level.getEntitiesOfClass(ItemEntity.class, around)) {
            ItemStack stack = entity.getItem();
            if (isRadioactive(stack)) total += 0.6 + Math.min(stack.getCount(), 64) / 128.0;
        }
        total += blocks(level, player.blockPosition());
        total += reactors(level, player);
        return total;
    }

    /** Ore and storage blocks of radioactive materials close by. */
    private static double blocks(ServerLevel level, BlockPos center) {
        double total = 0;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int dx = -BLOCK_RANGE; dx <= BLOCK_RANGE; dx++) {
            for (int dy = -BLOCK_RANGE; dy <= BLOCK_RANGE; dy++) {
                for (int dz = -BLOCK_RANGE; dz <= BLOCK_RANGE; dz++) {
                    pos.set(center.getX() + dx, center.getY() + dy, center.getZ() + dz);
                    BlockState state = level.getBlockState(pos);
                    if (state.isAir() || !(state.getBlock() instanceof MaterialBlock block)) continue;
                    if (isRadioactive(block.material)) {
                        total += 0.5 / (1.0 + Math.sqrt(dx * dx + dy * dy + dz * dz));
                    }
                }
            }
        }
        return total;
    }

    /** Running FX-1 Reactors in the 3 x 3 chunks around the player. */
    private static double reactors(ServerLevel level, ServerPlayer player) {
        double total = 0;
        int chunkX = player.blockPosition().getX() >> 4;
        int chunkZ = player.blockPosition().getZ() >> 4;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(chunkX + dx, chunkZ + dz);
                if (chunk == null) continue;
                List<BlockEntity> blockEntities = new ArrayList<>(chunk.getBlockEntities().values());
                for (BlockEntity blockEntity : blockEntities) {
                    if (!(blockEntity instanceof MetaMachineBlockEntity machineEntity)) continue;
                    MetaMachine machine = machineEntity.getMetaMachine();
                    if (!REACTOR_ID.equals(machine.getDefinition().getId())) continue;
                    if (!(machine instanceof WorkableMultiblockMachine reactor) || !reactor.isFormed() ||
                            !reactor.getRecipeLogic().isWorking()) {
                        continue;
                    }
                    double distance = Math.sqrt(blockEntity.getBlockPos().distToCenterSqr(player.position()));
                    if (distance <= REACTOR_RANGE) total += REACTOR_STRENGTH * (1.0 - distance / (REACTOR_RANGE + 1.0));
                }
            }
        }
        return total;
    }

    public static boolean isRadioactive(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (stack.is(RADIOACTIVE_ITEMS)) return true;
        return isRadioactive(ChemicalHelper.getMaterialStack(stack).material());
    }

    /** GT's notion: a hazard that anything triggers and that gives cancer, or an element with a half-life. */
    public static boolean isRadioactive(Material material) {
        if (material == null || material.isNull()) return false;
        HazardProperty hazard = material.getProperty(PropertyKey.HAZARD);
        if (hazard != null && hazard.condition == GTMedicalConditions.CARCINOGEN &&
                hazard.hazardTrigger == HazardProperty.HazardTrigger.ANY) {
            return true;
        }
        return material.isRadioactive();
    }
}
