package com.af9.core.ae2;

import net.minecraft.world.item.Item;

import appeng.block.crafting.ICraftingUnitType;

/**
 * The Crafting CPU Core as AE2 sees a crafting unit: one byte and no co-processors, only so that AE2 forms a CPU
 * (a cluster needs a block with storage). What the CPU really has is set from the Crafting CPU Array that holds the
 * core ({@link CpuCoreBlockEntity#apply}).
 */
public enum CpuCoreType implements ICraftingUnitType {
    CORE;

    @Override
    public long getStorageBytes() {
        return 1;
    }

    @Override
    public int getAcceleratorThreads() {
        return 0;
    }

    @Override
    public Item getItemFromType() {
        return AF9AE2.CORE_ITEM.get();
    }
}
