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
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

/**
 * The machine bus: the Bus Connectors joined by one run of Optical Bus Cable, and the computation and research on it.
 * Walked from a connector's port (its front face) through the cables, in loaded chunks only (an unloaded stretch cuts
 * the bus there, it never loads chunks). Two connectors whose ports touch are on one bus without cable.
 * <p>
 * Sources: GT's transmitter hatches whose front faces a cable of the run (or the port itself): an HPCA's or Network
 * Switch's Computation Transmitter Hatch gives CWU/t, a Data Bank's Optical Data Transmitter Hatch its research; and a
 * connector in a Data Bank gives that bank's research ({@link BusConnectorPartMachine#getDataSource}).
 * <p>
 * Limits: a bus carries at most {@link #MAX_CWUT} CWU/t (counted each tick, {@link BusLoad}) and research without
 * limit, for at most {@link #MAX_MACHINES} machines; with more it is overloaded and carries nothing.
 * <p>
 * A Bus Controller ties up to four buses together (a Bus Connector on each), and Interconnect Hatches on one run of
 * cable tie Bus Controllers together: the {@link Net network}, over which computation, research and supplies flow.
 */
public final class BusNetwork {

    /** Cable blocks one walk follows at most. */
    public static final int MAX_CABLES = 4096;
    /** Machines one bus serves at most (Central Monitors' and Bus Controllers' ports do not count). */
    public static final int MAX_MACHINES = 16;
    /** CWU/t one bus carries at most. */
    public static final int MAX_CWUT = 1024;

    private BusNetwork() {}

    /**
     * One walk of a bus.
     *
     * @param id          the bus's identity: its lowest connector position (the same whichever connector walks it)
     * @param connectors  every connector on it, the walking one first
     * @param machines    how many of them are machines' ports
     * @param computation CWU/t sources (the transmitter hatches' computation containers)
     * @param data        research sources (Data Banks' transmitter hatches, connectors in Data Banks)
     */
    public record Bus(long id, List<BusConnectorPartMachine> connectors, int machines,
                      List<IOpticalComputationProvider> computation, List<IDataAccessHatch> data) {

        /** More machines than a bus serves: it carries no computation, research or supplies. */
        public boolean overloaded() {
            return machines > MAX_MACHINES;
        }
    }

    /**
     * The buses and Bus Controllers tied together by the controllers' ports and Interconnect Hatches.
     *
     * @param buses       every bus of it, the one it was found from first
     * @param controllers every Bus Controller of it, in the order found
     */
    public record Net(List<Bus> buses, List<BusControllerMachine> controllers) {}

    //////////////////////////////////////
    // ************ A bus *************//
    //////////////////////////////////////

    /** The bus of {@code origin}. */
    public static Bus walk(BusConnectorPartMachine origin) {
        Map<BlockPos, BusConnectorPartMachine> found = new LinkedHashMap<>();
        Set<MetaMachine> transmitters = new LinkedHashSet<>();
        found.put(origin.getPos(), origin);
        Level level = origin.getLevel();
        if (level != null) {
            walkCables(level, origin.getPos(), origin.getFrontFacing(), (pos, side) -> {
                MetaMachine machine = MetaMachine.getMachine(level, pos);
                if (machine == null || machine.getFrontFacing() != side) return;
                if (machine instanceof BusConnectorPartMachine connector) {
                    found.putIfAbsent(pos, connector);
                } else if (isTransmitter(machine)) {
                    transmitters.add(machine);
                }
            });
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
        long id = Long.MAX_VALUE;
        int machines = 0;
        for (BusConnectorPartMachine connector : connectors) {
            id = Math.min(id, connector.getPos().asLong());
            if (connector.getMachineController() != null) machines++;
            IDataAccessHatch source = connector.getDataSource();
            if (source != null) data.add(source);
        }
        return new Bus(id, List.copyOf(connectors), machines, List.copyOf(computation), List.copyOf(data));
    }

    /** The Interconnect Hatches on the cable run of {@code origin}'s port, {@code origin} left out. */
    public static List<BusInterconnectPartMachine> walkInterconnects(BusInterconnectPartMachine origin) {
        Level level = origin.getLevel();
        if (level == null) return List.of();
        Set<BusInterconnectPartMachine> found = new LinkedHashSet<>();
        walkCables(level, origin.getPos(), origin.getFrontFacing(), (pos, side) -> {
            if (MetaMachine.getMachine(level, pos) instanceof BusInterconnectPartMachine hatch &&
                    hatch.getFrontFacing() == side && hatch != origin) {
                found.add(hatch);
            }
        });
        return List.copyOf(found);
    }

    /**
     * From a port at {@code origin} facing {@code facing}: every block next to the port or to a cable of its run that
     * is no cable, with the side of it that faces the port or cable.
     */
    private static void walkCables(Level level, BlockPos origin, Direction facing,
                                   BiConsumer<BlockPos, Direction> visit) {
        BlockPos start = origin.relative(facing);
        if (!level.isLoaded(start)) return;
        if (!(level.getBlockState(start).getBlock() instanceof OpticalBusCableBlock)) {
            visit.accept(start, facing.getOpposite());
            return;
        }
        Set<BlockPos> seen = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        seen.add(start);
        seen.add(origin);
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
                    visit.accept(next, direction.getOpposite());
                }
            }
        }
    }

    /** GT's Computation Transmitter Hatch or Optical Data Transmitter Hatch (not the reception hatches). */
    public static boolean isTransmitter(MetaMachine machine) {
        return machine instanceof OpticalComputationHatchMachine computation && computation.isTransmitter() ||
                machine instanceof OpticalDataHatchMachine data && data.isTransmitter();
    }

    //////////////////////////////////////
    // ********** The network **********//
    //////////////////////////////////////

    /**
     * The network around some buses and controllers: every Bus Controller with a port on one of its buses, every bus
     * one of its controllers has a port on, every controller linked to one of its controllers.
     */
    public static Net network(Collection<Bus> buses, Collection<BusControllerMachine> controllers) {
        Map<Long, Bus> foundBuses = new LinkedHashMap<>();
        Set<BusControllerMachine> foundControllers = new LinkedHashSet<>();
        ArrayDeque<BusControllerMachine> queue = new ArrayDeque<>();
        for (BusControllerMachine controller : controllers) {
            if (controller.isFormed() && foundControllers.add(controller)) queue.add(controller);
        }
        for (Bus bus : buses) {
            if (foundBuses.putIfAbsent(bus.id(), bus) == null) addControllers(bus, foundControllers, queue);
        }
        while (!queue.isEmpty()) {
            BusControllerMachine controller = queue.poll();
            for (BusConnectorPartMachine port : controller.getPorts()) {
                Bus bus = port.getBus();
                if (foundBuses.putIfAbsent(bus.id(), bus) == null) addControllers(bus, foundControllers, queue);
            }
            for (BusControllerMachine linked : controller.getLinkedControllers()) {
                if (linked.isFormed() && foundControllers.add(linked)) queue.add(linked);
            }
        }
        return new Net(List.copyOf(foundBuses.values()), List.copyOf(foundControllers));
    }

    private static void addControllers(Bus bus, Set<BusControllerMachine> found,
                                       ArrayDeque<BusControllerMachine> queue) {
        for (BusConnectorPartMachine connector : bus.connectors()) {
            BusControllerMachine controller = connector.getBusController();
            if (controller != null && controller.isFormed() && found.add(controller)) queue.add(controller);
        }
    }
}
