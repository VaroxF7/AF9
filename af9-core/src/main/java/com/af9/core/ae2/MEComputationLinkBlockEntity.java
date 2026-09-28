package com.af9.core.ae2;

import com.af9.core.AF9Config;
import com.af9.core.bus.BusConsumer;
import com.af9.core.bus.BusNetwork;
import com.af9.core.bus.OpticalBusCableBlock;

import com.gregtechceu.gtceu.api.capability.IOpticalComputationProvider;
import com.gregtechceu.gtceu.api.capability.forge.GTCapability;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;

import appeng.api.networking.GridHelper;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.IGridNodeListener;
import appeng.api.networking.IInWorldGridNodeHost;
import appeng.api.networking.IManagedGridNode;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;

/**
 * ME Computation Link: an AE2 network device (it needs no channel itself, only a trickle of AE) that draws computation
 * for its ME network ({@link MEComputationService}) from what is against any of its faces:
 * <ul>
 * <li>Optical Bus Cable: the machine bus, where it counts as one of the bus's machines ({@link BusConsumer}) and draws
 * from the bus's 1024 CWU/t and its network. One bus a link: with cable on several faces, the first
 * ({@link #getPortSide}: down, up, north, south, west, east);</li>
 * <li>anything that gives GT computation on that face, directly, no cable needed: a CWU Server, a Computation
 * Transmitter Hatch (an HPCA's, a Network Switch's, a computation array's), GT's Optical Fiber Cable to one.</li>
 * </ul>
 * The bus first, then the other faces in that order, until the request is met. The AE2 cable connects on any face.
 * Any number per network; the network draws through them in turn.
 */
public class MEComputationLinkBlockEntity extends BlockEntity implements IInWorldGridNodeHost, BusConsumer {

    /** Ticks a walk of the bus is kept. */
    private static final int BUS_CACHE_TICKS = 20;

    private final IManagedGridNode node;
    /** What GT's Optical Fiber Cable sees on each face: a receiver, it gives nothing. */
    private final IOpticalComputationProvider port = new Port();
    private final LazyOptional<IOpticalComputationProvider> portCap = LazyOptional.of(() -> port);
    private BusNetwork.Bus bus;
    private BusNetwork.Net network;
    private long busTime = -1;
    /** CWU/t drawn in the last tick it drew, and that tick. */
    private int drawn, drawing;
    private long drawTick = -1;

    public MEComputationLinkBlockEntity(BlockPos pos, BlockState state) {
        super(AF9AE2.LINK_ENTITY.get(), pos, state);
        node = GridHelper.createManagedNode(this, Listener.INSTANCE)
                .setInWorldNode(true)
                .setTagName("node")
                .setIdlePowerUsage(1.0)
                .setVisualRepresentation(AF9AE2.LINK.get());
    }

    /** Its bus's side: the first face with Optical Bus Cable against it, or null. */
    @Override
    public Direction getPortSide() {
        if (level == null) return null;
        for (Direction side : Direction.values()) {
            BlockPos at = worldPosition.relative(side);
            if (level.isLoaded(at) && level.getBlockState(at).getBlock() instanceof OpticalBusCableBlock) return side;
        }
        return null;
    }

    //////////////////////////////////////
    // ************* AE2 ***************//
    //////////////////////////////////////

    @Override
    public IGridNode getGridNode(Direction dir) {
        return node.getNode();
    }

    @Override
    public void onLoad() {
        super.onLoad();
        node.setExposedOnSides(EnumSet.allOf(Direction.class));
        if (level != null && !level.isClientSide) {
            GridHelper.onFirstTick(this, link -> link.node.create(link.level, link.worldPosition));
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        node.destroy();
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        node.destroy();
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        node.loadFromNBT(tag);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        node.saveToNBT(tag);
    }

    private enum Listener implements IGridNodeListener<MEComputationLinkBlockEntity> {

        INSTANCE;

        @Override
        public void onSaveChanges(MEComputationLinkBlockEntity link, IGridNode node) {
            link.setChanged();
        }
    }

    //////////////////////////////////////
    // ********** Computation **********//
    //////////////////////////////////////

    /** Draws up to {@code cwut} CWU/t for the ME network: from its bus, then from the GT sources against it. */
    public int draw(int cwut) {
        Level level = getLevel();
        if (level == null || cwut <= 0) return 0;
        List<IOpticalComputationProvider> seen = new ArrayList<>();
        seen.add(port);
        int got = 0;
        if (getPortSide() != null) got = BusNetwork.requestCWUt(level, getBus(), getNetwork(), cwut, false, seen);
        for (IOpticalComputationProvider provider : getDirectSources()) {
            if (got >= cwut) break;
            if (seen.contains(provider)) continue;
            got += Math.max(0, provider.requestCWUt(cwut - got, false, seen));
        }
        long now = level.getGameTime();
        if (now != drawTick) {
            drawn = drawTick == now - 1 ? drawing : 0;
            drawing = 0;
            drawTick = now;
        }
        drawing += got;
        return got;
    }

    /** The bus on its port side (walked at most once a second); without cable, a bus of itself alone. */
    public BusNetwork.Bus getBus() {
        long now = level == null ? 0 : level.getGameTime();
        if (bus == null || busTime < 0 || now < busTime || now - busTime >= BUS_CACHE_TICKS) {
            bus = BusNetwork.walk(this);
            network = null;
            busTime = now;
        }
        return bus;
    }

    public BusNetwork.Net getNetwork() {
        BusNetwork.Bus own = getBus();
        if (network == null) network = BusNetwork.network(List.of(own), List.of());
        return network;
    }

    /**
     * What gives GT computation against its faces, the bus's side and other ME Computation Links left out: a CWU
     * Server, a transmitter hatch, a computation array's or HPCA's controller, GT's Optical Fiber Cable.
     */
    public List<IOpticalComputationProvider> getDirectSources() {
        List<IOpticalComputationProvider> sources = new ArrayList<>();
        if (level == null) return sources;
        Direction bus = getPortSide();
        for (Direction side : Direction.values()) {
            BlockPos at = worldPosition.relative(side);
            if (side == bus || !level.isLoaded(at)) continue;
            BlockEntity next = level.getBlockEntity(at);
            if (next == null || next instanceof MEComputationLinkBlockEntity) continue;
            next.getCapability(GTCapability.CAPABILITY_COMPUTATION_PROVIDER, side.getOpposite())
                    .ifPresent(sources::add);
        }
        return sources;
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> capability, Direction side) {
        if (capability == GTCapability.CAPABILITY_COMPUTATION_PROVIDER) {
            return portCap.cast();
        }
        return super.getCapability(capability, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        portCap.invalidate();
    }

    /** A face as GT's fibre sees it: a receiving end, never a source. */
    private static final class Port implements IOpticalComputationProvider {

        @Override
        public int requestCWUt(int cwut, boolean simulate, Collection<IOpticalComputationProvider> seen) {
            return 0;
        }

        @Override
        public int getMaxCWUt(Collection<IOpticalComputationProvider> seen) {
            return 0;
        }

        @Override
        public boolean canBridge(Collection<IOpticalComputationProvider> seen) {
            return true;
        }
    }

    //////////////////////////////////////
    // ************ Report *************//
    //////////////////////////////////////

    /** Right-click: what it brings in and what its network needs. */
    public void report(Player player) {
        Level level = getLevel();
        if (level == null) return;
        player.sendSystemMessage(
                Component.translatable("block.af9.me_computation_link").withStyle(ChatFormatting.AQUA));
        boolean onBus = getPortSide() != null;
        if (onBus) {
            BusNetwork.Bus own = getBus();
            player.sendSystemMessage(Component.translatable("af9.me_link.from_bus", own.machines(),
                    BusNetwork.MAX_MACHINES, BusNetwork.maxCWUt(own, getNetwork(), new ArrayList<>()))
                    .withStyle(own.overloaded() ? ChatFormatting.RED : ChatFormatting.GRAY));
        }
        List<IOpticalComputationProvider> direct = getDirectSources();
        if (!direct.isEmpty()) {
            player.sendSystemMessage(Component.translatable("af9.me_link.from_gt", direct.size())
                    .withStyle(ChatFormatting.GRAY));
        } else if (!onBus) {
            player.sendSystemMessage(Component.translatable("af9.me_link.from_nothing").withStyle(ChatFormatting.RED));
        }
        player.sendSystemMessage(Component.translatable("af9.me_link.drawn", level.getGameTime() - drawTick <= 1 ?
                drawn : 0).withStyle(ChatFormatting.GRAY));

        IGrid grid = node.getGrid();
        MEComputationService service = grid == null ? null : grid.getService(MEComputationService.class);
        if (service == null) {
            player.sendSystemMessage(Component.translatable("af9.me_link.no_network").withStyle(ChatFormatting.RED));
        } else if (!AF9Config.ME_NEEDS_COMPUTATION.get()) {
            player.sendSystemMessage(Component.translatable("af9.me_link.off").withStyle(ChatFormatting.GRAY));
        } else if (!service.hasController()) {
            player.sendSystemMessage(Component.translatable("af9.me_link.no_controller")
                    .withStyle(ChatFormatting.GRAY));
        } else {
            player.sendSystemMessage(Component.translatable("af9.me_link.network", service.getChannelsNeeded(),
                    service.getDemand(), AF9Config.ME_CHANNELS_PER_CWUT.get(), service.getSupplied())
                    .withStyle(ChatFormatting.GRAY));
            int cap = service.getChannelCap();
            player.sendSystemMessage(cap < 0 ?
                    Component.translatable("af9.me_link.all_channels").withStyle(ChatFormatting.GREEN) :
                    Component.translatable("af9.me_link.short", cap, service.getChannelsNeeded())
                            .withStyle(ChatFormatting.YELLOW));
        }
    }
}
