package com.af9.core.ae2;

import com.af9.core.AF9Config;

import appeng.api.networking.GridFlags;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridMultiblock;
import appeng.api.networking.IGridNode;
import appeng.api.networking.IGridService;
import appeng.api.networking.IGridServiceProvider;
import appeng.api.networking.pathing.ControllerState;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * An ME network's computation (one per AE2 grid): a network with an ME Controller needs CWU/t for its channels, one
 * CWU/t per {@link AF9Config#ME_CHANNELS_PER_CWUT} channels, drawn every tick through its ME Computation Links. When
 * it gets all it needs, its channels are not limited; short of it, it may use only the channels its average supply
 * over a second pays for ({@link #getChannelCap}, applied by AE2's channel assignment through
 * {@code PathingCalculationMixin}): the devices farthest from the controller lose theirs first. Networks without a
 * controller (ad-hoc, 8 channels) need none.
 * <p>
 * The channels are only assigned again when the cap really changes: at once when the network gets all it needs
 * again, else at most every {@link #MIN_REPATH_TICKS} and only for a change of at least a tenth.
 */
public class MEComputationService implements IGridService, IGridServiceProvider {

    /** Ticks the supply is averaged over. */
    private static final int WINDOW = 20;
    /** Ticks between two channel reassignments for a changed shortage. */
    private static final int MIN_REPATH_TICKS = 100;

    private final IGrid grid;
    private long age;
    private long lastRepath = -MIN_REPATH_TICKS;
    private int tick;
    private long suppliedSum;
    /** Channels the network wants (counted each window). */
    private int channelsNeeded;
    /** CWU/t it asks for. */
    private int demand;
    /** Average CWU/t it got over the last window. */
    private int supplied;
    /** Channels it may use, -1 no limit. */
    private int cap = -1;
    private boolean controller;

    public MEComputationService(IGrid grid) {
        this.grid = grid;
    }

    /** The channels the network may use now, -1 for no limit. */
    public int getChannelCap() {
        return AF9Config.ME_NEEDS_COMPUTATION.get() ? cap : -1;
    }

    /** A grid's channel cap, -1 for no limit (and for a grid without the service). */
    public static int channelCap(IGrid grid) {
        try {
            MEComputationService service = grid.getService(MEComputationService.class);
            return service == null ? -1 : service.getChannelCap();
        } catch (RuntimeException e) {
            return -1;
        }
    }

    public boolean hasController() {
        return controller;
    }

    public int getChannelsNeeded() {
        return channelsNeeded;
    }

    public int getDemand() {
        return demand;
    }

    public int getSupplied() {
        return supplied;
    }

    @Override
    public void onServerEndTick() {
        age++;
        controller = grid.getPathingService().getControllerState() == ControllerState.CONTROLLER_ONLINE;
        if (!AF9Config.ME_NEEDS_COMPUTATION.get() || !controller) {
            // no controller: AE2 assigns the channels anew anyway when one comes or goes; switched off in the
            // settings: give a limited network its channels back
            if (cap != -1) {
                cap = -1;
                if (controller) grid.getPathingService().repath();
            }
            tick = 0;
            suppliedSum = 0;
            supplied = 0;
            demand = 0;
            return;
        }
        int perCwut = AF9Config.ME_CHANNELS_PER_CWUT.get();
        if (tick == 0) {
            channelsNeeded = countChannels();
            demand = (channelsNeeded + perCwut - 1) / perCwut;
        }
        suppliedSum += draw(demand);
        if (++tick < WINDOW) return;

        supplied = (int) (suppliedSum / WINDOW);
        tick = 0;
        suppliedSum = 0;
        long pays = (long) supplied * perCwut;
        int newCap = pays >= channelsNeeded ? -1 : (int) pays;
        if (newCap == cap) return;
        boolean freed = newCap == -1;
        boolean cut = cap == -1;
        int change = Math.abs(newCap - cap);
        boolean big = change >= Math.max(perCwut, Math.max(newCap, cap) / 10);
        if (freed || cut || (big && age - lastRepath >= MIN_REPATH_TICKS)) {
            cap = newCap;
            lastRepath = age;
            grid.getPathingService().repath();
        }
    }

    /** Draws up to {@code cwut} CWU/t through the network's ME Computation Links, in turn. */
    private int draw(int cwut) {
        int got = 0;
        for (MEComputationLinkBlockEntity link : grid.getMachines(MEComputationLinkBlockEntity.class)) {
            if (got >= cwut) break;
            got += link.draw(cwut - got);
        }
        return got;
    }

    /** Devices that want a channel; a multiblock (a crafting CPU) counts once, as AE2 gives it one channel. */
    private int countChannels() {
        int count = 0;
        Set<IGridNode> multiblocks = new HashSet<>();
        for (IGridNode node : grid.getNodes()) {
            if (!node.hasFlag(GridFlags.REQUIRE_CHANNEL)) continue;
            if (node.hasFlag(GridFlags.MULTIBLOCK)) {
                if (multiblocks.contains(node)) continue;
                IGridMultiblock multiblock = node.getService(IGridMultiblock.class);
                if (multiblock != null) {
                    for (Iterator<IGridNode> it = multiblock.getMultiblockNodes(); it.hasNext();) {
                        IGridNode other = it.next();
                        if (other != null) multiblocks.add(other);
                    }
                }
            }
            count++;
        }
        return count;
    }
}
