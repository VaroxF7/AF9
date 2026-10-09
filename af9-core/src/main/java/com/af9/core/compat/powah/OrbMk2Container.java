package com.af9.core.compat.powah;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import owmii.powah.lib.logistics.inventory.Inventory;

/**
 * The orb's seven slots (0 the product, 1 to 6 the inputs) as a vanilla container for the menu. Every change goes through
 * Powah's inventory, which tells the tile to look for a recipe again.
 */
final class OrbMk2Container implements Container {

    static final int SIZE = 7;

    private final OrbMk2Tile tile;

    OrbMk2Container(OrbMk2Tile tile) {
        this.tile = tile;
    }

    private Inventory inventory() {
        return tile.getInventory();
    }

    @Override
    public int getContainerSize() {
        return SIZE;
    }

    @Override
    public boolean isEmpty() {
        for (int i = 0; i < SIZE; i++) {
            if (!getItem(i).isEmpty()) return false;
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return inventory().getStackInSlot(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack stack = getItem(slot);
        if (stack.isEmpty() || amount <= 0) return ItemStack.EMPTY;
        ItemStack taken = stack.split(amount);
        // the same stack, changed in place: told to the inventory so that the recipe is looked for again
        inventory().setStackInSlot(slot, stack.isEmpty() ? ItemStack.EMPTY : stack);
        return taken;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack stack = getItem(slot);
        if (stack.isEmpty()) return ItemStack.EMPTY;
        inventory().setStackInSlot(slot, ItemStack.EMPTY);
        return stack;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        inventory().setStackInSlot(slot, stack);
    }

    @Override
    public int getMaxStackSize() {
        return 64;
    }

    @Override
    public void setChanged() {
        tile.setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(tile, player);
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return tile.canInsert(slot, stack);
    }

    @Override
    public void clearContent() {
        for (int i = 0; i < SIZE; i++) inventory().setStackInSlot(i, ItemStack.EMPTY);
    }
}
