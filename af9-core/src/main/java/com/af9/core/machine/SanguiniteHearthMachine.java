package com.af9.core.machine;

import com.af9.core.machine.console.ConsoleWidget;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.TickableSubscription;
import com.gregtechceu.gtceu.api.machine.multiblock.CoilWorkableElectricMultiblockMachine;
import com.gregtechceu.gtceu.api.recipe.RecipeHelper;
import com.gregtechceu.gtceu.api.recipe.modifier.ModifierFunction;
import com.gregtechceu.gtceu.api.recipe.modifier.RecipeModifier;
import com.gregtechceu.gtceu.utils.GTUtil;

import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;

/**
 * Controller logic of the Sanguinite Hearth Furnace (structure and recipe type in
 * {@code kubejs/startup_scripts/gtceu/sanguinite_hearth.js}): a standalone copy of GT's Rotary Hearth Furnace
 * ({@code gtceu:mega_blast_furnace}) that runs only its own recipe type ({@code gtceu:sanguinite_hearth}), never the
 * EBF family. The EBF cannot smelt sanguinite at all: the molten print lives on the hearth's type, and the
 * material's auto EBF/hot-ingot recipes are removed.
 * <p>
 * The hearth is a thermal mass, not an instant furnace: it preheats toward its coils' maximum
 * ({@link #getMaxHeat()}, coil temperature plus 100 K per energy hatch tier above MV, like the EBF's display) in
 * {@link #PREHEAT_SECONDS} while switched on and powered (the heaters draw {@link #heaterDrainPerInterval()} on top
 * of everything: 4 A of LuV minimum, the readiness heat), and cools over {@link #COOL_SECONDS} without power or
 * while switched off. A print only starts preheated ({@link #HEARTH_GATE}); between runs an enabled, powered hearth
 * holds its heat in readiness, so back-to-back smelts start at once and a cold hearth makes you wait. Breaking the
 * structure vents it back to 0 K. Heating needs at least LuV hatches: below that the hearth only cools.
 * <p>
 * Automation angles: keep it switched on under power with an ME level emitter on the dust stock (or a clock) and it
 * stays hot and self-starts; a parallel hatch multiplies the molten prints; batch mode folds overclocked runs.
 * Coolant (supercooled fluids, Coolant Hatches) is a recipe input and shows as its own JEI slot.
 */
public class SanguiniteHearthMachine extends CoilWorkableElectricMultiblockMachine {

    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(
            SanguiniteHearthMachine.class, CoilWorkableElectricMultiblockMachine.MANAGED_FIELD_HOLDER);

    /** Hearth temperature a print needs (K): the molten sanguinite smelt (Tritanium coils). */
    public static final int HEARTH_TEMP = 10800;
    /** Seconds from cold to full heat while powered, and back to cold without power. */
    public static final int PREHEAT_SECONDS = 300;
    public static final int COOL_SECONDS = 500;
    /** Ticks between two heat updates. */
    public static final int HEAT_INTERVAL = 10;
    /** Cold hearth the structure resets to when broken. */
    public static final int AMBIENT_K = 0;

    /**
     * Only starts a print on a preheated hearth whose hatches can supply its EU/t. GT keeps retrying a gated recipe,
     * so a cold hearth starts by itself once the preheat finishes.
     */
    public static final RecipeModifier HEARTH_GATE = (machine, recipe) -> {
        if (!(machine instanceof SanguiniteHearthMachine hearth)) {
            return RecipeModifier.nullWrongType(SanguiniteHearthMachine.class, machine);
        }
        if (!hearth.isPreheated()) return ModifierFunction.NULL;
        if (hearth.getAvailableEUt() < RecipeHelper.getRealEUt(recipe).getTotalEU()) return ModifierFunction.NULL;
        return ModifierFunction.IDENTITY;
    };

    /** Hearth heat, K. */
    @Persisted
    private int hearthHeat = AMBIENT_K;

    private TickableSubscription heatSubs;

    public SanguiniteHearthMachine(IMachineBlockEntity holder) {
        super(holder);
    }

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    public long getAvailableEUt() {
        return energyContainer == null || !isFormed() ? 0 :
                energyContainer.getInputVoltage() * energyContainer.getInputAmperage();
    }

    //////////////////////////////////////
    // ************ Heat *************//
    //////////////////////////////////////

    @Override
    public void onStructureFormed() {
        super.onStructureFormed();
        heatSubs = subscribeServerTick(heatSubs, this::tickHeat);
    }

    @Override
    public void onStructureInvalid() {
        super.onStructureInvalid();
        unsubscribeHeat();
        hearthHeat = AMBIENT_K;
        markDirty();
    }

    @Override
    public void onUnload() {
        super.onUnload();
        unsubscribeHeat();
    }

    private void unsubscribeHeat() {
        if (heatSubs != null) {
            heatSubs.unsubscribe();
            heatSubs = null;
        }
    }

    private void tickHeat() {
        if (getOffsetTimer() % HEAT_INTERVAL != 0 || !isFormed()) return;
        int before = hearthHeat;
        int max = getMaxHeat();
        boolean working = getRecipeLogic().isWorking();
        long drain = heaterDrainPerInterval();
        boolean powered = energyContainer != null && drain > 0 && energyContainer.getEnergyStored() >= drain &&
                isHeatingTier();
        if ((getRecipeLogic().isWorkingEnabled() || working) && powered) {
            energyContainer.removeEnergy(drain);
            // a running print holds at least its heat; an idle hearth climbs toward the coils' maximum
            hearthHeat = Math.min(max, hearthHeat + Math.max(1, max * HEAT_INTERVAL / (20 * PREHEAT_SECONDS)));
        } else {
            hearthHeat = Math.max(AMBIENT_K, hearthHeat - Math.max(1, max * HEAT_INTERVAL / (20 * COOL_SECONDS)));
        }
        if (hearthHeat != before) markDirty();
    }

    /** Heating needs at least LuV hatches: below that the hearth only cools, never climbs. */
    public boolean isHeatingTier() {
        if (energyContainer == null) return false;
        return GTUtil.getTierByVoltage(energyContainer.getInputVoltage()) >= GTValues.LuV;
    }

    /** Readiness heat: 4 A of LuV minimum, drawn whenever enabled (or running) to climb or hold the heat. */
    public long heaterDrainPerInterval() {
        if (energyContainer == null) return 0;
        return 4L * GTValues.VA[GTValues.LuV] * HEAT_INTERVAL;
    }

    /**
     * Hottest the hearth gets: the coils' temperature plus 100 K per energy hatch tier above MV (the EBF's own
     * display maths). Tritanium coils (10,800 K) with ZPM hatches reach 11,300 K: just past the smelt.
     */
    public int getMaxHeat() {
        if (!isFormed() || energyContainer == null) return AMBIENT_K;
        int tier = GTUtil.getTierByVoltage(energyContainer.getInputVoltage());
        int coilTemp = 0;
        try {
            coilTemp = getCoilType().getCoilTemperature();
        } catch (RuntimeException ignored) {
            // no coils yet
        }
        return Math.max(AMBIENT_K, coilTemp + 100 * Math.max(0, tier - GTValues.MV));
    }

    /** Current hearth heat, K. */
    public int getHearthHeat() {
        return isFormed() ? hearthHeat : AMBIENT_K;
    }

    /** Hot enough to smelt. */
    public boolean isPreheated() {
        return isFormed() && hearthHeat >= HEARTH_TEMP;
    }

    /** Preheat progress 0-1. */
    public double getPreheatFraction() {
        int max = Math.max(HEARTH_TEMP, getMaxHeat());
        return Math.max(0, Math.min(1, (double) (getHearthHeat() - AMBIENT_K) / (max - AMBIENT_K)));
    }

    /** Console/Jade status: running, preheating, or idle-hot. */
    public int hearthStatus() {
        if (getRecipeLogic().isWorking()) return ConsoleWidget.STATUS_RUNNING;
        if (!isPreheated()) return ConsoleWidget.STATUS_PUMPING_DOWN;
        return ConsoleWidget.STATUS_IDLE;
    }
}
