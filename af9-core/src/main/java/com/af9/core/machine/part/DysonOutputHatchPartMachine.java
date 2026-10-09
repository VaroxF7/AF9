package com.af9.core.machine.part;

import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.TickableSubscription;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiController;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiPart;
import com.gregtechceu.gtceu.api.machine.multiblock.PartAbility;
import com.gregtechceu.gtceu.common.machine.multiblock.electric.PowerSubstationMachine;
import com.gregtechceu.gtceu.common.machine.multiblock.part.EnergyHatchPartMachine;

import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;

import net.minecraft.core.Direction;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Dyson Output Hatch: the output of the Dyson Swarm, UHV at 10,000 A (the power of a full swarm of the best sails).
 * It is the only power output the swarm takes ({@link #DYSON_OUTPUT}). As a dynamo hatch it puts out of its front to
 * whatever takes energy there; and it feeds a Power Substation directly: any part (or the controller) of a formed
 * substation touching the hatch takes the power straight into its energy bank, past the 64 A of its own input hatches,
 * and from there it goes on to the substation's output hatches and the batteries in it.
 */
public class DysonOutputHatchPartMachine extends EnergyHatchPartMachine {

    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(
            DysonOutputHatchPartMachine.class, EnergyHatchPartMachine.MANAGED_FIELD_HOLDER);

    /** Registered by the hatch definition (KubeJS), required by the Dyson Swarm's pattern. */
    public static final PartAbility DYSON_OUTPUT = new PartAbility("af9_dyson_output");
    /** A full swarm of the best sails: 10,000 sails of one amp of UHV each. */
    public static final int AMPERAGE = 10000;

    private TickableSubscription depositSubs;

    public DysonOutputHatchPartMachine(IMachineBlockEntity holder, int tier) {
        super(holder, tier, IO.OUT, AMPERAGE);
    }

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (!isRemote()) depositSubs = subscribeServerTick(depositSubs, this::deposit);
    }

    @Override
    public void onUnload() {
        super.onUnload();
        if (depositSubs != null) {
            depositSubs.unsubscribe();
            depositSubs = null;
        }
    }

    /** Puts what the hatch holds into the substation next to it. */
    private void deposit() {
        long stored = energyContainer.getEnergyStored();
        if (stored <= 0 || !isWorkingEnabled()) return;
        for (Direction side : Direction.values()) {
            PowerSubstationMachine substation = substationAt(side);
            if (substation == null) continue;
            long filled = Substation.fill(substation, stored);
            if (filled <= 0) continue;
            energyContainer.changeEnergy(-filled);
            stored -= filled;
            if (stored <= 0) return;
        }
    }

    private PowerSubstationMachine substationAt(Direction side) {
        MetaMachine machine = MetaMachine.getMachine(getLevel(), getPos().relative(side));
        if (machine instanceof PowerSubstationMachine substation) return substation;
        if (machine instanceof IMultiPart part) {
            for (IMultiController controller : part.getControllers()) {
                if (controller instanceof PowerSubstationMachine substation) return substation;
            }
        }
        return null;
    }

    /**
     * The substation's energy bank, which GT keeps private: reached by reflection, once. When GT's names are not the
     * ones looked for the hatch is an ordinary dynamo hatch (it outputs from its front like GT's).
     */
    private static final class Substation {

        private static final Logger LOGGER = LogManager.getLogger("AF9 Dyson Output Hatch");
        private static final Field BANK;
        private static final Method FILL;
        private static final Field NET_IN;

        static {
            Field bank = null;
            Method fill = null;
            Field netIn = null;
            try {
                bank = PowerSubstationMachine.class.getDeclaredField("energyBank");
                bank.setAccessible(true);
                fill = bank.getType().getMethod("fill", long.class);
                netIn = PowerSubstationMachine.class.getDeclaredField("netInLastSec");
                netIn.setAccessible(true);
            } catch (ReflectiveOperationException | RuntimeException e) {
                LOGGER.warn("Power Substation's energy bank not reachable, the hatch only outputs from its front", e);
                if (fill == null) bank = null;
            }
            BANK = bank;
            FILL = fill;
            NET_IN = netIn;
        }

        /** Puts up to the amount into the substation's bank (when it is formed and working); returns what fitted. */
        static long fill(PowerSubstationMachine substation, long amount) {
            if (BANK == null || FILL == null || !substation.isFormed() || !substation.isWorkingEnabled()) return 0;
            try {
                Object bank = BANK.get(substation);
                if (bank == null) return 0;
                long filled = (Long) FILL.invoke(bank, amount);
                // the substation's own "input per second" counts it
                if (filled > 0 && NET_IN != null) NET_IN.setLong(substation, NET_IN.getLong(substation) + filled);
                return filled;
            } catch (ReflectiveOperationException | RuntimeException e) {
                return 0;
            }
        }
    }
}
