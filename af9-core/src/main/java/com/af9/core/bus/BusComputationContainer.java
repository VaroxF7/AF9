package com.af9.core.bus;

import com.gregtechceu.gtceu.api.capability.IOpticalComputationProvider;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.machine.feature.IRecipeLogicMachine;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiController;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableComputationContainer;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * A Bus Connector's computation: GT's computation reception container, but the CWU/t come over the machine bus (every
 * HPCA / Network Switch transmitter hatch on it, {@link BusNetwork}) instead of one Optical Fiber Cable. The connector's
 * machine draws its recipes' CWU/t through it, and GT's Research Station takes it as its computation provider.
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
        int left = cwut;
        for (IOpticalComputationProvider provider : connector.getBus().computation()) {
            if (left <= 0) break;
            if (seen.contains(provider)) continue;
            left -= Math.max(0, provider.requestCWUt(left, simulate, seen));
        }
        return cwut - left;
    }

    @Override
    public int getMaxCWUt(Collection<IOpticalComputationProvider> seen) {
        seen.add(this);
        long sum = 0;
        for (IOpticalComputationProvider provider : connector.getBus().computation()) {
            if (!seen.contains(provider)) sum += Math.max(0, provider.getMaxCWUt(seen));
        }
        return (int) Math.min(Integer.MAX_VALUE, sum);
    }

    /** Bridgeable (for a Network Switch) if every source on the bus is; an empty bus passes quietly, as GT's cable. */
    @Override
    public boolean canBridge(Collection<IOpticalComputationProvider> seen) {
        seen.add(this);
        for (IOpticalComputationProvider provider : connector.getBus().computation()) {
            if (!seen.contains(provider) && !provider.canBridge(seen)) return false;
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
