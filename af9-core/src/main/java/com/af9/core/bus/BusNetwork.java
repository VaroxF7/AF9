package com.af9.core.bus;

import com.af9.core.machine.CWUServerMachine;

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
 * Switch's Computation Transmitter Hatch and a CWU Server give CWU/t, a Data Bank's Optical Data Transmitter Hatch its
 * research; and a connector in a Data Bank gives that bank's research ({@link BusConnectorPartMachine#getDataSource}).
 * <p>
 * Limits: a bus carries at most {@link #MAX_CWUT} CWU/t (counted each tick, {@link BusLoad}) and research without
 * limit, for at most {@link #MAX_MACHINES} machines (machines' ports and {@link BusConsumer}s such as the ME
 * Computation Link); with more it is overloaded and carries nothing.
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
     * @param id          the bus's identity: its lowest connector or consumer position (the same whichever walks it)
     * @param connectors  every connector on it, the walking one first
     * @param consumers   the other blocks drawing from it ({@link BusConsumer}: ME Computation Links)
     * @param machines    how many machines it serves: machines' ports and consumers
     * @param computation CWU/t sources (the transmitter hatches' computation containers, CWU Servers, computation
     *                    arrays with a connector)
     * @param data        research sources (Data Banks' transmitter hatches, connectors in Data Banks)
     */
    public record Bus(long id, List<BusConnectorPartMachine> connectors, List<BusConsumer> consumers, int machines,
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

    /** The bus of a connector. */
    public static Bus walk(BusConnectorPartMachine origin) {
        return walk(origin.getLevel(), origin.getPos(), origin.getFrontFacing(), origin, null);
    }

    /** The bus of a consumer (an ME Computation Link). */
    public static Bus walk(BusConsumer origin) {
        return walk(origin.getLevel(), origin.getBlockPos(), origin.getPortSide(), null, origin);
    }

    private static Bus walk(Level level, BlockPos originPos, Direction originPort,
                            BusConnectorPartMachine originConnector, BusConsumer originConsumer) {
        Map<BlockPos, BusConnectorPartMachine> found = new LinkedHashMap<>();
        Set<BusConsumer> consumers = new LinkedHashSet<>();
        Set<MetaMachine> transmitters = new LinkedHashSet<>();
        if (originConnector != null) found.put(originPos, originConnector);
        if (originConsumer != null) consumers.add(originConsumer);
        if (level != null) {
            walkCables(level, originPos, originPort, (pos, side) -> {
                if (level.getBlockEntity(pos) instanceof BusConsumer consumer) {
                    if (consumer.getPortSide() == side) consumers.add(consumer);
                    return;
                }
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
            } else if (transmitter instanceof CWUServerMachine server) {
                computation.add(server);
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
        int machines = consumers.size();
        for (BusConnectorPartMachine connector : connectors) {
            id = Math.min(id, connector.getPos().asLong());
            if (connector.getMachineController() != null) machines++;
            IDataAccessHatch source = connector.getDataSource();
            if (source != null) data.add(source);
            IOpticalComputationProvider array = connector.getComputationSource();
            if (array != null) computation.add(array);
        }
        for (BusConsumer consumer : consumers) id = Math.min(id, consumer.getBlockPos().asLong());
        return new Bus(id, List.copyOf(connectors), List.copyOf(consumers), machines, List.copyOf(computation),
                List.copyOf(data));
    }

    //////////////////////////////////////
    // ********** Computation **********//
    //////////////////////////////////////

    /**
     * Draws up to {@code cwut} CWU/t for something on {@code own}: from the sources on that bus first, then from those
     * on the other buses of {@code net}. A bus gives at most what is left of its {@link #MAX_CWUT} this tick
     * ({@link BusLoad}); computation from another bus counts on both. An overloaded bus gives and passes none.
     *
     * @return the CWU/t drawn (or that could be, when simulating)
     */
    public static int requestCWUt(Level level, Bus own, Net net, int cwut, boolean simulate,
                                  Collection<IOpticalComputationProvider> seen) {
        if (level == null || cwut <= 0 || own.overloaded()) return 0;
        int want = Math.min(cwut, BusLoad.remaining(level, own.id()));
        int got = draw(own.computation(), want, simulate, seen);
        for (Bus bus : net.buses()) {
            if (got >= want) break;
            if (bus.id() == own.id() || bus.overloaded()) continue;
            int drawn = draw(bus.computation(), Math.min(want - got, BusLoad.remaining(level, bus.id())), simulate,
                    seen);
            if (!simulate) BusLoad.use(level, bus.id(), drawn);
            got += drawn;
        }
        if (!simulate) BusLoad.use(level, own.id(), got);
        return got;
    }

    /** Up to {@code cwut} from the sources, in order. */
    private static int draw(List<IOpticalComputationProvider> sources, int cwut, boolean simulate,
                            Collection<IOpticalComputationProvider> seen) {
        int got = 0;
        for (IOpticalComputationProvider source : sources) {
            if (got >= cwut) break;
            if (seen.contains(source)) continue;
            got += Math.max(0, source.requestCWUt(cwut - got, simulate, seen));
        }
        return got;
    }

    /** What {@code own} and its network could give at most: each bus's sources, each bus and the whole at most 1024. */
    public static int maxCWUt(Bus own, Net net, Collection<IOpticalComputationProvider> seen) {
        if (own.overloaded()) return 0;
        long sum = 0;
        for (Bus bus : net.buses()) {
            if (bus.overloaded()) continue;
            long onBus = 0;
            for (IOpticalComputationProvider source : bus.computation()) {
                if (!seen.contains(source)) onBus += Math.max(0, source.getMaxCWUt(seen));
            }
            sum += Math.min(MAX_CWUT, onBus);
        }
        return (int) Math.min(MAX_CWUT, sum);
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

    /**
     * A source whose front may face the bus: GT's Computation Transmitter Hatch or Optical Data Transmitter Hatch (not
     * the reception hatches), or a CWU Server.
     */
    public static boolean isTransmitter(MetaMachine machine) {
        return machine instanceof OpticalComputationHatchMachine computation && computation.isTransmitter() ||
                machine instanceof OpticalDataHatchMachine data && data.isTransmitter() ||
                machine instanceof CWUServerMachine;
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
