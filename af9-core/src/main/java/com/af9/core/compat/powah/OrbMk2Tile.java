package com.af9.core.compat.powah;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import owmii.powah.block.energizing.EnergizingOrbTile;

/**
 * The Energizing Orb Mk2's block entity: Powah's orb tile (so its rods and wrench see an orb) that holds a stack in every
 * input slot, takes items while the product is out of the way, and has a menu. Powah's item handler capability sits on the
 * tile by its inventory interface: hoppers, pipes and AE2 reach the slots through it, with the slot limit and
 * {@link #canInsert} of this class. The product is taken out of slot 0 (an export bus or a hopper can).
 */
public class OrbMk2Tile extends EnergizingOrbTile implements MenuProvider {

    public OrbMk2Tile(BlockPos pos, BlockState state) {
        super(pos, state);
        // Powah's orb has seven slots (the product and six inputs); the mold slot is the eighth
        getInventory().set(OrbMk2Container.SIZE);
    }

    /** Powah's constructor gives its own type; the saved id and the update packets must be this block's. */
    @Override
    public BlockEntityType<?> getType() {
        return OrbMk2.TILE.get();
    }

    @Override
    public int getSlotLimit(int index) {
        return 64;
    }

    /**
     * Inputs while the product has been taken; a slot that holds the item takes more of it (stacking decides). The mold
     * slot takes the items the recipes keep (molds) and nothing else.
     */
    @Override
    public boolean canInsert(int index, ItemStack stack) {
        if (index == 0 || !getInventory().getStackInSlot(0).isEmpty()) return false;
        if (index == OrbMk2Menu.MOLD) return OrbRecipes.isMold(getLevel(), stack);
        return true;
    }

    @Override
    public Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    /** An orb saved with Powah's seven slots grows the eighth (the mold slot) when it is opened, keeping what it holds. */
    private void ensureSlots() {
        var inventory = getInventory();
        if (inventory.getSlots() >= OrbMk2Container.SIZE) return;
        ItemStack[] kept = new ItemStack[inventory.getSlots()];
        for (int i = 0; i < kept.length; i++) kept[i] = inventory.getStackInSlot(i).copy();
        inventory.set(OrbMk2Container.SIZE);
        for (int i = 0; i < kept.length; i++) inventory.setStackInSlot(i, kept[i]);
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        ensureSlots();
        return new OrbMk2Menu(id, playerInventory, new OrbMk2Container(this), new OrbMk2Menu.TileData(this), getBlockPos());
    }
}
