package com.af9.core.bus;

import com.gregtechceu.gtceu.api.capability.IDataAccessHatch;
import com.gregtechceu.gtceu.api.capability.IOpticalComputationProvider;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.trait.MachineTrait;
import com.gregtechceu.gtceu.common.machine.multiblock.part.OpticalComputationHatchMachine;
import com.gregtechceu.gtceu.common.machine.multiblock.part.OpticalDataHatchMachine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The machine bus: the Bus Connectors joined by one run of Optical Bus Cable, and the computation and research on it.
 * Walked from a connector's port (its front face) through the cables, in loaded chunks only (an unloaded stretch cuts
 * the bus there, it never loads chunks). Two connectors whose ports touch are on one bus without cable.
 * <p>
 * Sources: GT's transmitter hatches whose front faces a cable of the run (or the port itself): an HPCA's or Network
 * Switch's Computation Transmitter Hatch gives CWU/t, a Data Bank's Optical Data Transmitter Hatch its research; and a
 * connector in a Data Bank gives that bank's research ({@link BusConnectorPartMachine#getDataSource}).
 */
public final class BusNetwork {

    /** Cable blocks one walk follows at most. */
    public static final int MAX_CABLES = 4096;

    private BusNetwork() {}

    /**
     * One walk of the bus.
     *
     * @param connectors  every connector on it, the walking one first
     * @param computation CWU/t sources (the transmitter hatches' computation containers)
     * @param data        research sources (Data Banks' transmitter hatches, connectors in Data Banks)
     */
    public record Bus(List<BusConnectorPartMachine> connectors, List<IOpticalComputationProvider> computation,
                      List<IDataAccessHatch> data) {}

    /** The bus of {@code origin}. */
    public static Bus walk(BusConnectorPartMachine origin) {
        Level level = origin.getLevel();
        Map<BlockPos, BusConnectorPartMachine> found = new LinkedHashMap<>();
        Set<MetaMachine> transmitters = new LinkedHashSet<>();
        found.put(origin.getPos(), origin);
        if (level != null) {
            BlockPos start = origin.getPos().relative(origin.getFrontFacing());
            if (level.isLoaded(start)) {
                visit(level, start, origin.getFrontFacing().getOpposite(), found, transmitters);
                if (level.getBlockState(start).getBlock() instanceof OpticalBusCableBlock) {
                    walkCables(level, start, found, transmitters);
                }
            }
        }
        List<BusConnectorPartMachine> connectors = new ArrayList<>(found.values());
        List<IOpticalComputationProvider> computation = new ArrayList<>();
        List<IDataAccessHatch> data = new ArrayList<>();
        for (MetaMachine transmitter : transmitters) {
            if (transmitter instanceof OpticalDataHatchMachine hatch) {
                data.add(hatch);
            } else {
                for (MachineTrait trait : transmitter.getTraits()) {
                    if (trait instanceof IOpticalComputationProvider provider) {
                        computation.add(provider);
                        break;
                    }
                }
            }
        }
        for (BusConnectorPartMachine connector : connectors) {
            IDataAccessHatch source = connector.getDataSource();
            if (source != null) data.add(source);
        }
        return new Bus(List.copyOf(connectors), List.copyOf(computation), List.copyOf(data));
    }

    private static void walkCables(Level level, BlockPos start, Map<BlockPos, BusConnectorPartMachine> found,
                                   Set<MetaMachine> transmitters) {
        Set<BlockPos> seen = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        seen.add(start);
        queue.add(start);
        while (!queue.isEmpty() && seen.size() <= MAX_CABLES) {
            BlockPos cable = queue.poll();
            for (Direction direction : Direction.values()) {
                BlockPos next = cable.relative(direction);
                if (seen.contains(next) || !level.isLoaded(next)) continue;
                if (level.getBlockState(next).getBlock() instanceof OpticalBusCableBlock) {
                    seen.add(next);
                    queue.add(next);
                } else {
                    visit(level, next, direction.getOpposite(), found, transmitters);
                }
            }
        }
    }

    /** A connector or transmitter hatch at {@code pos} whose front faces {@code side} joins the bus. */
    private static void visit(Level level, BlockPos pos, Direction side, Map<BlockPos, BusConnectorPartMachine> found,
                              Set<MetaMachine> transmitters) {
        MetaMachine machine = MetaMachine.getMachine(level, pos);
        if (machine == null || machine.getFrontFacing() != side) return;
        if (machine instanceof BusConnectorPartMachine connector) {
            found.putIfAbsent(pos, connector);
        } else if (isTransmitter(machine)) {
            transmitters.add(machine);
        }
    }

    /** GT's Computation Transmitter Hatch or Optical Data Transmitter Hatch (not the reception hatches). */
    public static boolean isTransmitter(MetaMachine machine) {
        return machine instanceof OpticalComputationHatchMachine computation && computation.isTransmitter() ||
                machine instanceof OpticalDataHatchMachine data && data.isTransmitter();
    }
}
