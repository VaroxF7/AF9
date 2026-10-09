package com.af9.core.compat.powah;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;
import owmii.powah.block.energizing.EnergizingOrbBlock;

/**
 * The Energizing Orb Mk2's block. It is a Powah orb block (a subclass), so Powah's energizing rods find it, link to it and
 * charge it, the wrench works on it and it places like the orb does. A click opens its screen instead of putting one item in.
 */
public class OrbMk2Block extends EnergizingOrbBlock {

    public OrbMk2Block() {
        super(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(2.0F, 20.0F).sound(SoundType.METAL)
                .noOcclusion());
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new OrbMk2Tile(pos, state);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                 BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (level.getBlockEntity(pos) instanceof OrbMk2Tile tile && player instanceof ServerPlayer serverPlayer) {
            NetworkHooks.openScreen(serverPlayer, tile, pos);
        }
        return InteractionResult.CONSUME;
    }
}
