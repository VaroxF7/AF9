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
        return BusNetwork.requestCWUt(connector.getLevel(), connector.getBus(), connector.getNetwork(), cwut, simulate,
                seen);
    }

    /** What the bus and its network could give at most: each bus's sources, each bus at most its limit. */
    @Override
    public int getMaxCWUt(Collection<IOpticalComputationProvider> seen) {
        seen.add(this);
        return BusNetwork.maxCWUt(connector.getBus(), connector.getNetwork(), seen);
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
