package com.af9.core.space;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Oil Regolith: a sand-like rock of the asteroids that is soaked in oil. It falls like sand; a centrifuge presses the
 * Impure Oil out of it (KubeJS, docs/oil.md). {@link AsteroidFieldFeature} grows it in pockets of the rocks.
 */
public class OilRegolithBlock extends FallingBlock {

    public OilRegolithBlock(Properties properties) {
        super(properties);
    }

    @Override
    public int getDustColor(BlockState state, BlockGetter level, BlockPos pos) {
        return 0x3b2a1c;
    }
}
