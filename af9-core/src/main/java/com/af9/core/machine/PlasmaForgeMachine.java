package com.af9.core.machine;

import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.TickableSubscription;
import com.gregtechceu.gtceu.api.recipe.RecipeHelper;
import com.gregtechceu.gtceu.api.recipe.modifier.ModifierFunction;
import com.gregtechceu.gtceu.api.recipe.modifier.RecipeModifier;

import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;

import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * The Dimensionally Transcendent Plasma Forge (structure and recipes in KubeJS: {@code startup_scripts/gtceu/dtpf.js},
 * {@code server_scripts/mods/gtceu/dtpf.js}), after GTNH's: a vast hall that forges plasma into matter. What it keeps
 * from the original is the running-time ramp: the longer it runs without a pause, the cheaper and the faster it forges
 * ({@link #RAMP}: up to {@value #ENERGY_PERCENT} % less EU/t and {@value #TIME_PERCENT} % shorter runs after
 * {@value #RAMP_MINUTES} minutes), and a pause costs it that warmth ({@value #COOLING_FACTOR} times as fast as it
 * built). It runs only a recipe its hatches can fully supply ({@link #FORGE_GATE}).
 */
public class PlasmaForgeMachine extends ProcessMachine {

    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(PlasmaForgeMachine.class,
            ProcessMachine.MANAGED_FIELD_HOLDER);

    /** Minutes of running for the full ramp; the discounts at the full ramp, percent; how much faster it cools. */
    public static final int RAMP_MINUTES = 30, ENERGY_PERCENT = 50, TIME_PERCENT = 25, COOLING_FACTOR = 3;
    public static final int RAMP_TICKS = RAMP_MINUTES * 60 * 20;
    /** Ticks between two updates of the ramp. */
    private static final int RAMP_INTERVAL = 20;

    /** The hall's colour in its console: forge orange. */
    private static final int COLOR = 0xFFFF9A3C;

    /**
     * Only starts a recipe the hatches can fully supply: GT's own voltage check would let a hatch of the tier below
     * start it and then starve it. GT keeps retrying a gated recipe.
     */
    public static final RecipeModifier FORGE_GATE = (machine, recipe) -> {
        if (!(machine instanceof PlasmaForgeMachine forge)) {
            return RecipeModifier.nullWrongType(PlasmaForgeMachine.class, machine);
        }
        if (forge.getAvailableEUt() < RecipeHelper.getRealEUt(recipe).getTotalEU()) return ModifierFunction.NULL;
        return ModifierFunction.IDENTITY;
    };

    /** The running-time ramp: the recipe's EU/t and run time, scaled by how warm the forge has run. */
    public static final RecipeModifier RAMP = (machine, recipe) -> {
        if (!(machine instanceof PlasmaForgeMachine forge)) {
            return RecipeModifier.nullWrongType(PlasmaForgeMachine.class, machine);
        }
        double ramp = forge.getRamp();
        if (ramp <= 0) return ModifierFunction.IDENTITY;
        return ModifierFunction.builder()
                .eutMultiplier(1 - ENERGY_PERCENT / 100.0 * ramp)
                .durationMultiplier(1 - TIME_PERCENT / 100.0 * ramp)
                .build();
    };

    /** Ticks of warmth, 0 to {@link #RAMP_TICKS}. */
    @Persisted
    private int warmth;

    private TickableSubscription rampSubs;

    public PlasmaForgeMachine(IMachineBlockEntity holder) {
        super(holder);
    }

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    @Override
    public String titleKey() {
        return "af9.dtpf.console.title";
    }

    @Override
    public int modeColor(int index) {
        return COLOR;
    }

    //////////////////////////////////////
    // ************ Ramp *************//
    //////////////////////////////////////

    @Override
    public void onStructureFormed() {
        super.onStructureFormed();
        rampSubs = subscribeServerTick(rampSubs, this::tickRamp);
    }

    /** A broken hall loses its warmth: the next time it forms it starts cold. */
    @Override
    public void onStructureInvalid() {
        super.onStructureInvalid();
        unsubscribeRamp();
        warmth = 0;
        markDirty();
    }

    @Override
    public void onUnload() {
        super.onUnload();
        unsubscribeRamp();
    }

    private void unsubscribeRamp() {
        if (rampSubs != null) {
            rampSubs.unsubscribe();
            rampSubs = null;
        }
    }

    private void tickRamp() {
        if (getOffsetTimer() % RAMP_INTERVAL != 0 || !isFormed()) return;
        int before = warmth;
        if (getRecipeLogic().isWorking()) {
            warmth = Math.min(RAMP_TICKS, warmth + RAMP_INTERVAL);
        } else {
            warmth = Math.max(0, warmth - RAMP_INTERVAL * COOLING_FACTOR);
        }
        if (warmth != before) markDirty();
    }

    /** How warm the forge has run, 0 to 1. */
    public double getRamp() {
        return isFormed() ? (double) warmth / RAMP_TICKS : 0;
    }

    @Override
    public List<Component> infoLines() {
        List<Component> lines = new ArrayList<>();
        double ramp = getRamp();
        lines.add(Component.translatable("af9.dtpf.console.ramp", Math.round(ramp * 100)));
        lines.add(Component.translatable("af9.dtpf.console.discount", Math.round(ENERGY_PERCENT * ramp),
                Math.round(TIME_PERCENT * ramp)));
        lines.add(Component.translatable(getRecipeLogic().isWorking() ? "af9.dtpf.console.warming" :
                warmth > 0 ? "af9.dtpf.console.cooling" : "af9.dtpf.console.cold"));
        return lines;
    }
}
