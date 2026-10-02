package com.af9.core.ae2;

import com.af9.core.mixin.ae2.CraftingCPUClusterAccessor;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import appeng.api.networking.IGridNode;
import appeng.api.networking.events.GridCraftingCpuChange;
import appeng.blockentity.crafting.CraftingBlockEntity;
import appeng.me.cluster.implementations.CraftingCPUCluster;

/**
 * The block entity of the Crafting CPU Core: an ordinary AE2 crafting block (so AE2 forms a crafting CPU from it, with
 * the usual grid node, job saving and terminal listing) whose cluster gets its bytes and co-processors from the
 * Crafting CPU Array instead of from the blocks around it ({@link #apply}).
 * <p>
 * AE2 sums a cluster's storage and co-processors when it forms it (a block gives at most 16 threads), so a core on
 * its own forms a CPU of 1 byte. The array overwrites both values each second, which also puts them back after AE2
 * has re-formed the cluster (a neighbour placed or removed).
 */
public class CpuCoreBlockEntity extends CraftingBlockEntity {

    public CpuCoreBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    /**
     * Sets the CPU of the core at {@code pos} to this many bytes and co-processors (0 and 0: nothing can be crafted on
     * it). Returns whether there is a CPU there to set: the core is in the world, on a network, and AE2 has formed
     * its cluster.
     */
    public static boolean apply(Level level, BlockPos pos, long bytes, int threads) {
        if (!level.isLoaded(pos) || !(level.getBlockEntity(pos) instanceof CpuCoreBlockEntity core)) return false;
        return core.set(bytes, threads);
    }

    private boolean set(long bytes, int threads) {
        CraftingCPUCluster cluster = getCluster();
        if (cluster == null || cluster.isDestroyed()) return false;
        var access = (CraftingCPUClusterAccessor) (Object) cluster;
        if (access.af9$getStorage() != bytes || access.af9$getAccelerator() != threads) {
            access.af9$setStorage(bytes);
            access.af9$setAccelerator(threads);
            cluster.markDirty();
            IGridNode node = getGridNode();
            if (node != null && node.getGrid() != null) node.getGrid().postEvent(new GridCraftingCpuChange(node));
        }
        return true;
    }
}
