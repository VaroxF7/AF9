package com.af9.core.mixin.ae2;

import appeng.me.cluster.implementations.CraftingCPUCluster;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Opens the bytes and the co-processor count of AE2's crafting CPU: the Crafting CPU Array decides them
 * ({@code CpuCoreBlockEntity}), where AE2 adds up the blocks of the cluster.
 */
@Mixin(value = CraftingCPUCluster.class, remap = false)
public interface CraftingCPUClusterAccessor {

    @Accessor("storage")
    long af9$getStorage();

    @Accessor("storage")
    void af9$setStorage(long storage);

    @Accessor("accelerator")
    int af9$getAccelerator();

    @Accessor("accelerator")
    void af9$setAccelerator(int accelerator);
}
