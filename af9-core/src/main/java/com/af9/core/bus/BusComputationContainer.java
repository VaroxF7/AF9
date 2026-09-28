package com.af9.core.bus;

import com.gregtechceu.gtceu.api.capability.IOpticalComputationProvider;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.machine.feature.IRecipeLogicMachine;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiController;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableComputationContainer;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;

import net.minecraft.world.level.Level;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * A Bus Connector's computation: GT's computation reception container, but the CWU/t come over the machine bus instead
 * of one Optical Fiber Cable: from the HPCA / Network Switch transmitter hatches on the connector's own bus first, then
 * from those on the other buses of its network ({@link BusNetwork.Net}). A bus carries at most
 * {@link BusNetwork#MAX_CWUT} CWU/t a tick ({@link BusLoad}); computation from another bus counts on both. An
 * overloaded bus gives none. The connector's machine draws its recipes' CWU/t through it, and GT's Research Station
 * takes it as its computation provider.
 */
public class BusComputationContainer extends NotifiableComputationContainer {

    private final BusConnectorPartMachine connector;

    public BusComputationContainer(BusConnectorPartMachine connector) {
        super(connector, IO.IN, false);
        this.connector = connector;
    }

    @Override
    public int requestCWUt(int cwut, boolean simulate, Collection<IOpticalComputationProvider> seen) {
        seen.add(this);
        Level level = connector.getLevel();
        BusNetwork.Bus own = connector.getBus();
        if (level == null || cwut <= 0 || own.overloaded()) return 0;
        int want = Math.min(cwut, BusLoad.remaining(level, own.id()));
        int got = draw(own.computation(), want, simulate, seen);
        for (BusNetwork.Bus bus : connector.getNetwork().buses()) {
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

    /** What the bus and its network could give at most: each bus's sources, each bus at most its limit. */
    @Override
    public int getMaxCWUt(Collection<IOpticalComputationProvider> seen) {
        seen.add(this);
        BusNetwork.Bus own = connector.getBus();
        if (own.overloaded()) return 0;
        long sum = 0;
        for (BusNetwork.Bus bus : connector.getNetwork().buses()) {
            if (bus.overloaded()) continue;
            long onBus = 0;
            for (IOpticalComputationProvider source : bus.computation()) {
                if (!seen.contains(source)) onBus += Math.max(0, source.getMaxCWUt(seen));
            }
            sum += Math.min(BusNetwork.MAX_CWUT, onBus);
        }
        return (int) Math.min(BusNetwork.MAX_CWUT, sum);
    }

    /** Bridgeable (for a Network Switch) if every source on the network is; none passes quietly, as GT's cable. */
    @Override
    public boolean canBridge(Collection<IOpticalComputationProvider> seen) {
        seen.add(this);
        if (connector.getBus().overloaded()) return true;
        for (BusNetwork.Bus bus : connector.getNetwork().buses()) {
            if (bus.overloaded()) continue;
            for (IOpticalComputationProvider source : bus.computation()) {
                if (!seen.contains(source) && !source.canBridge(seen)) return false;
            }
        }
        return true;
    }

    @Override
    public IOpticalComputationProvider getComputationProvider() {
        return this;
    }

    /** GT's reception logic, drawing from the bus. */
    @Override
    public List<Integer> handleRecipeInner(IO io, GTRecipe recipe, List<Integer> left, boolean simulate) {
        if (io != IO.IN) return left;
        int sum = left.stream().mapToInt(Integer::intValue).sum();
        int available = requestCWUt(Integer.MAX_VALUE, true);
        if (available >= sum) {
            if (recipe.data.getBoolean("duration_is_total_cwu")) {
                int drawn = requestCWUt(available, simulate);
                if (!simulate) {
                    // the recipe logic adds 1 progress a tick; a total-CWU recipe advances by the CWU drawn instead
                    for (IMultiController controller : connector.getControllers()) {
                        if (controller instanceof IRecipeLogicMachine rlm) {
                            var logic = rlm.getRecipeLogic();
                            logic.setProgress(logic.getProgress() - 1 + drawn);
                        }
                    }
                }
                sum -= drawn;
            } else {
                sum -= requestCWUt(sum, simulate);
            }
        }
        return sum <= 0 ? null : Collections.singletonList(sum);
    }
}
