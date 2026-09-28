package com.af9.core.bus;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

/**
 * A block (not a GT machine) that draws computation from the machine bus through a port: the ME Computation Link.
 * Optical Bus Cable plugs into its port; on the bus it counts as one machine ({@link BusNetwork#MAX_MACHINES}) and
 * draws from the bus's budget ({@link BusNetwork#requestCWUt}).
 */
public interface BusConsumer {

    Level getLevel();

    BlockPos getBlockPos();

    /** The side the bus's cable plugs into. */
    Direction getPortSide();
}
