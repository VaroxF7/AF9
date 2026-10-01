package com.af9.core.thermal;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

/**
 * Something that gives off waste heat into the world: the hook for the temperature system (the Temperature Update:
 * warmth around machines that the player feels). Nothing reads it yet; a machine that runs hot implements it and the
 * system can then collect the emitters of a level, warm the blocks around {@link #getHeatPos()} (the air leaves in
 * {@link #getHeatDirection()}) and warn the players standing in it.
 * <p>
 * Heat is counted in <em>heat units per tick</em> ({@link #getHeatOutput()}): 1 unit is what one MV Air Conditioning
 * Hatch removes in a tick, and its compressor gives off twice that into the room. A unit has no fixed relation to EU or
 * degrees: the temperature system decides what a unit does to the air.
 */
public interface IHeatEmitter {

    /** Waste heat released right now, in heat units per tick; 0 when the machine is idle or switched off. */
    int getHeatOutput();

    /** The block the warm air leaves from (the hatch's own block). */
    BlockPos getHeatPos();

    /** The direction the warm air leaves in (the side the exhaust faces). */
    Direction getHeatDirection();

    /** The level the emitter is in; null while it is not in a level. */
    Level getHeatLevel();
}
