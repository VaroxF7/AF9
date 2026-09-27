package com.af9.core.bus;

import com.gregtechceu.gtceu.api.machine.MetaMachine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The machine bus: the Bus Connectors joined by one run of Polycat Cable. Walked from a connector's port (its front
 * face) through the cables, in loaded chunks only (an unloaded stretch cuts the bus there, it never loads chunks). Two
 * connectors whose ports touch are on one bus without cable.
 */
public final class BusNetwork {

    /** Cable blocks one walk follows at most. */
    public static final int MAX_CABLES = 4096;

    private BusNetwork() {}

    /** Every connector on the bus of {@code origin}, the origin first. */
    public static List<BusConnectorPartMachine> walk(BusConnectorPartMachine origin) {
        Level level = origin.getLevel();
        Map<BlockPos, BusConnectorPartMachine> found = new LinkedHashMap<>();
        found.put(origin.getPos(), origin);
        if (level == null) return new ArrayList<>(found.values());

        BlockPos start = origin.getPos().relative(origin.getFrontFacing());
        if (!level.isLoaded(start)) return new ArrayList<>(found.values());
        BusConnectorPartMachine facing = connectorFacing(level, start, origin.getFrontFacing().getOpposite());
        if (facing != null) found.put(facing.getPos(), facing);
        if (!(level.getBlockState(start).getBlock() instanceof PolycatCableBlock)) {
            return new ArrayList<>(found.values());
        }

        Set<BlockPos> seen = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        seen.add(start);
        queue.add(start);
        while (!queue.isEmpty() && seen.size() <= MAX_CABLES) {
            BlockPos cable = queue.poll();
            for (Direction direction : Direction.values()) {
                BlockPos next = cable.relative(direction);
                if (seen.contains(next) || !level.isLoaded(next)) continue;
                if (level.getBlockState(next).getBlock() instanceof PolycatCableBlock) {
                    seen.add(next);
                    queue.add(next);
                } else {
                    BusConnectorPartMachine connector = connectorFacing(level, next, direction.getOpposite());
                    if (connector != null) found.putIfAbsent(connector.getPos(), connector);
                }
            }
        }
        return new ArrayList<>(found.values());
    }

    /** The connector at {@code pos} if its port faces {@code side}, or null. */
    private static BusConnectorPartMachine connectorFacing(Level level, BlockPos pos, Direction side) {
        return MetaMachine.getMachine(level, pos) instanceof BusConnectorPartMachine connector &&
                connector.getFrontFacing() == side ? connector : null;
    }
}
