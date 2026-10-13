package com.af9.core.fluid;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.function.Supplier;

/**
 * A liquid block for a GregTech fluid that ships without one. Behaves like water; picking it up with a bucket
 * hands back this fluid's own bucket ({@link PlaceableFluidBucket}) instead of the empty one GT's fluids would give.
 */
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class PlaceableFluidBlock extends LiquidBlock {

    private final Supplier<Item> bucket;

    public PlaceableFluidBlock(FlowingFluid fluid, Supplier<Item> bucket, BlockBehaviour.Properties properties) {
        super(fluid, properties);
        this.bucket = bucket;
    }

    @Override
    public ItemStack pickupBlock(LevelAccessor level, BlockPos pos, BlockState state) {
        if (state.getValue(LEVEL) != 0) return ItemStack.EMPTY;
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 11);
        return new ItemStack(bucket.get());
    }
}
