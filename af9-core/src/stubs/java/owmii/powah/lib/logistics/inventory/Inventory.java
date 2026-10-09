package owmii.powah.lib.logistics.inventory;

import net.minecraft.world.item.ItemStack;

/** Compile-time stand-in for Powah's class (not shipped). */
public abstract class Inventory {

    public abstract ItemStack getStackInSlot(int slot);

    public abstract void setStackInSlot(int slot, ItemStack stack);

    public abstract void clear();

    public abstract int getSlots();

    public abstract void setSendUpdates(boolean sendUpdates);
}
