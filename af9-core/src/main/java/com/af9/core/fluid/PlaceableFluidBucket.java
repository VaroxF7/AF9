package com.af9.core.fluid;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlockContainer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.function.Supplier;

/**
 * A bucket for a GregTech fluid. Vanilla's placement writes the fluid's own legacy block, which GT fluids do not
 * have, so this one places this bucket's liquid block ({@link PlaceableFluidBlock}) instead. Everything else
 * (raycast, pickup through the block, sounds, stats) is vanilla's.
 */
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class PlaceableFluidBucket extends BucketItem {

    private final Supplier<? extends Block> placedBlock;

    public PlaceableFluidBucket(Supplier<? extends Fluid> fluid, Supplier<? extends Block> block,
            Properties properties) {
        super(fluid, properties);
        this.placedBlock = block;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        BlockHitResult hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.SOURCE_ONLY);
        InteractionResultHolder<ItemStack> ret = ForgeEventFactory.onBucketUse(player, level, held, hit);
        if (ret != null) return ret;
        if (hit.getType() == HitResult.Type.MISS) return InteractionResultHolder.pass(held);
        if (hit.getType() != HitResult.Type.BLOCK) return InteractionResultHolder.pass(held);
        BlockPos pos = hit.getBlockPos();
        if (!level.mayInteract(player, pos) || !player.mayUseItemAt(pos, hit.getDirection(), held)) {
            return InteractionResultHolder.fail(held);
        }
        BlockState state = level.getBlockState(pos);
        BlockPos placeAt = state.getBlock() instanceof LiquidBlockContainer
                && ((LiquidBlockContainer) state.getBlock()).canPlaceLiquid(level, pos, state, getFluid())
                        ? pos
                        : hit.getBlockPos().relative(hit.getDirection());
        if (this.emptyContents(player, level, placeAt, hit, held)) {
            this.checkExtraContent(player, level, held, placeAt);
            if (player instanceof ServerPlayer server) {
                CriteriaTriggers.PLACED_BLOCK.trigger(server, placeAt, held);
            }
            player.awardStat(Stats.ITEM_USED.get(this));
            return InteractionResultHolder.sidedSuccess(getEmptySuccessItem(held, player), level.isClientSide());
        }
        return InteractionResultHolder.fail(held);
    }

    /** Vanilla's placement, except the block written is this bucket's liquid block. */
    public boolean emptyContents(@Nullable Player player, Level level, BlockPos pos, @Nullable BlockHitResult hit,
            @Nullable ItemStack container) {
        BlockState state = level.getBlockState(pos);
        Block block = state.getBlock();
        boolean replaceable = state.canBeReplaced(getFluid());
        boolean placeable = state.isAir() || replaceable || block instanceof LiquidBlockContainer containerBlock
                && containerBlock.canPlaceLiquid(level, pos, state, getFluid());
        if (!placeable) {
            return hit != null && this.emptyContents(player, level,
                    hit.getBlockPos().relative(hit.getDirection()), null, container);
        }
        FluidStack contained = container == null ? null : FluidUtil.getFluidContained(container).orElse(null);
        if (contained != null
                && getFluid().getFluidType().isVaporizedOnPlacement(level, pos, contained)) {
            getFluid().getFluidType().onVaporize(player, level, pos, contained);
            return true;
        }
        if (block instanceof LiquidBlockContainer containerBlock) {
            containerBlock.placeLiquid(level, pos, state, getFluid().defaultFluidState());
            this.playEmptySound(player, level, pos);
            return true;
        }
        if (!level.isClientSide && replaceable && state.getFluidState().isEmpty()) {
            level.destroyBlock(pos, true);
        }
        level.setBlock(pos, placedBlock.get().defaultBlockState(), 11);
        this.playEmptySound(player, level, pos);
        return true;
    }
}
