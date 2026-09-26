package com.af9.core.wafer;

import com.af9.core.AF9Config;
import com.af9.core.AF9Core;
import com.af9.core.compat.curios.CuriosCompat;
import com.af9.core.litho.LithoMode;

import com.gregtechceu.gtceu.api.blockentity.MetaMachineBlockEntity;
import com.gregtechceu.gtceu.common.machine.multiblock.electric.CleanroomMachine;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Wafers and chips belong in a clean room: when a player takes a wafer (blank, printed or derived) or a chip into
 * their inventory, holds it on the cursor or puts it in the inventory crafting grid, it turns into its contaminated
 * version: the contaminated wafer of its substrate (kubejs:contaminated_&lt;substrate&gt;_wafer), or the contaminated
 * chip (kubejs:contaminated_&lt;chip&gt;, e.g. kubejs:contaminated_ram_chip).
 * <p>
 * Protected are players who wear gloves (an item of {@code #af9:wafer_gloves}, GT's Rubber Gloves or Hazmat chestpiece,
 * in an armor slot or a Curios slot) or who stand inside a formed, clean GT Cleanroom. Spectators are exempt, creative players only if
 * {@link AF9Config#CONTAMINATE_IN_CREATIVE} is off. Machines, pipes, chests and ME systems never contaminate anything.
 * <p>
 * The wafers of a substrate are the item tag {@code #af9:wafers/<substrate>}, all of them {@code #af9:wafers}; the
 * chips are {@code #af9:chips} (kubejs/server_scripts/mods/gtceu/photolithography.js).
 */
@Mod.EventBusSubscriber(modid = AF9Core.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
@SuppressWarnings("removal") // new ResourceLocation(ns, path) is the only constructor on 1.20.1
public final class WaferContamination {

    public static final TagKey<Item> ALL_WAFERS = ItemTags.create(new ResourceLocation(AF9Core.MOD_ID, "wafers"));
    public static final TagKey<Item> ALL_CHIPS = ItemTags.create(new ResourceLocation(AF9Core.MOD_ID, "chips"));
    public static final TagKey<Item> GLOVES = ItemTags.create(new ResourceLocation(AF9Core.MOD_ID, "wafer_gloves"));
    /** Ticks between two checks of a player's inventory. */
    public static final int CHECK_INTERVAL = 10;

    /** Substrate id -> its wafer tag, in {@link LithoMode} order. */
    private static final Map<String, TagKey<Item>> SUBSTRATE_TAGS = new LinkedHashMap<>();
    private static final Map<String, Item> CONTAMINATED = new LinkedHashMap<>();
    private static final Map<Item, Item> CONTAMINATED_CHIPS = new LinkedHashMap<>();

    static {
        for (LithoMode mode : LithoMode.values()) {
            SUBSTRATE_TAGS.put(mode.substrate,
                    ItemTags.create(new ResourceLocation(AF9Core.MOD_ID, "wafers/" + mode.substrate)));
        }
    }

    private WaferContamination() {}

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.side != LogicalSide.SERVER) return;
        if (!(event.player instanceof ServerPlayer player)) return;
        if (player.tickCount % CHECK_INTERVAL != 0 || player.isSpectator()) return;
        if (player.isCreative() && !AF9Config.CONTAMINATE_IN_CREATIVE.get()) return;

        Inventory inventory = player.getInventory();
        Container craft = player.inventoryMenu.getCraftSlots();
        ItemStack carried = player.containerMenu.getCarried();
        if (!hasSensitive(inventory) && !hasSensitive(craft) && !isSensitive(carried)) return;
        if (wearsGloves(player) || inCleanroom(player)) return;

        boolean changed = contaminateAll(inventory);
        changed |= contaminateAll(craft);
        ItemStack dirtyCarried = contaminate(carried);
        if (dirtyCarried != null) {
            player.containerMenu.setCarried(dirtyCarried);
            changed = true;
        }
        if (changed) {
            inventory.setChanged();
            player.containerMenu.broadcastChanges();
            player.displayClientMessage(
                    Component.translatable("af9.wafer.contaminated").withStyle(ChatFormatting.RED), true);
        }
    }

    /** A wafer or a chip: something that contaminates in a player's hands. */
    public static boolean isSensitive(ItemStack stack) {
        return !stack.isEmpty() && (stack.is(ALL_WAFERS) || stack.is(ALL_CHIPS));
    }

    private static boolean hasSensitive(Container container) {
        for (int i = 0; i < container.getContainerSize(); i++) {
            if (isSensitive(container.getItem(i))) return true;
        }
        return false;
    }

    private static boolean contaminateAll(Container container) {
        boolean changed = false;
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack dirty = contaminate(container.getItem(i));
            if (dirty != null) {
                container.setItem(i, dirty);
                changed = true;
            }
        }
        return changed;
    }

    /**
     * The contaminated wafer / chip for a wafer or chip stack (same count), or null if the stack does not contaminate.
     */
    public static ItemStack contaminate(ItemStack stack) {
        if (stack.isEmpty()) return null;
        if (stack.is(ALL_CHIPS)) {
            Item dirty = contaminatedChip(stack.getItem());
            return dirty == null ? null : new ItemStack(dirty, stack.getCount());
        }
        if (!stack.is(ALL_WAFERS)) return null;
        for (Map.Entry<String, TagKey<Item>> entry : SUBSTRATE_TAGS.entrySet()) {
            if (!stack.is(entry.getValue())) continue;
            Item dirty = contaminatedWafer(entry.getKey());
            return dirty == null ? null : new ItemStack(dirty, stack.getCount());
        }
        return null;
    }

    private static Item contaminatedWafer(String substrate) {
        return CONTAMINATED.computeIfAbsent(substrate, key -> {
            Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation("kubejs",
                    "contaminated_" + key + "_wafer"));
            if (item == null || item == Items.AIR) {
                AF9Core.LOGGER.warn("Item kubejs:contaminated_{}_wafer not found - is the AF9 KubeJS startup " +
                        "script loaded?", key);
                return null;
            }
            return item;
        });
    }

    /** kubejs:contaminated_&lt;chip's registry path&gt;. */
    private static Item contaminatedChip(Item chip) {
        return CONTAMINATED_CHIPS.computeIfAbsent(chip, key -> {
            ResourceLocation id = ForgeRegistries.ITEMS.getKey(key);
            Item item = id == null ? null :
                    ForgeRegistries.ITEMS.getValue(new ResourceLocation("kubejs", "contaminated_" + id.getPath()));
            if (item == null || item == Items.AIR) {
                AF9Core.LOGGER.warn("No contaminated item kubejs:contaminated_{} - is the AF9 KubeJS startup " +
                        "script loaded?", id == null ? "?" : id.getPath());
                return null;
            }
            return item;
        });
    }

    /** In an armor slot or a Curios slot (GT puts its Rubber Gloves in the hands slot). */
    public static boolean wearsGloves(ServerPlayer player) {
        for (ItemStack armor : player.getInventory().armor) {
            if (armor.is(GLOVES)) return true;
        }
        return CuriosCompat.isEquipped(player, GLOVES);
    }

    /** Inside the walls of a formed, clean GT Cleanroom whose controller is at most one chunk away. */
    public static boolean inCleanroom(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        BlockPos pos = player.blockPosition();
        int chunkX = pos.getX() >> 4;
        int chunkZ = pos.getZ() >> 4;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(chunkX + dx, chunkZ + dz);
                if (chunk == null) continue;
                List<BlockEntity> blockEntities = new ArrayList<>(chunk.getBlockEntities().values());
                for (BlockEntity blockEntity : blockEntities) {
                    if (blockEntity instanceof MetaMachineBlockEntity machineEntity &&
                            machineEntity.getMetaMachine() instanceof CleanroomMachine cleanroom &&
                            cleanroom.isFormed() && cleanroom.isClean() && isInside(cleanroom, pos)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /** Strictly inside the bounding box of the cleanroom's walls, floor and ceiling. */
    private static boolean isInside(CleanroomMachine cleanroom, BlockPos pos) {
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
        for (BlockPos wall : cleanroom.getMultiblockState().getCache()) {
            minX = Math.min(minX, wall.getX());
            minY = Math.min(minY, wall.getY());
            minZ = Math.min(minZ, wall.getZ());
            maxX = Math.max(maxX, wall.getX());
            maxY = Math.max(maxY, wall.getY());
            maxZ = Math.max(maxZ, wall.getZ());
        }
        return pos.getX() > minX && pos.getX() < maxX && pos.getY() > minY && pos.getY() < maxY &&
                pos.getZ() > minZ && pos.getZ() < maxZ;
    }
}
