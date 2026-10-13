package com.af9.core.machine;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.TickableSubscription;
import com.gregtechceu.gtceu.api.recipe.RecipeHelper;
import com.gregtechceu.gtceu.api.recipe.modifier.ModifierFunction;
import com.gregtechceu.gtceu.api.recipe.modifier.RecipeModifier;
import com.gregtechceu.gtceu.utils.GTUtil;

import com.af9.core.machine.part.PlasmaHatchPartMachine;

import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;

import net.minecraft.network.chat.Component;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;

import java.util.ArrayList;
import java.util.List;

/**
 * The Dimensionally Transcendent Plasma Forge (structure and recipes in KubeJS: {@code startup_scripts/gtceu/dtpf.js},
 * {@code server_scripts/mods/gtceu/dtpf.js}), after GTNH's: a vast hall that forges plasma into matter. What it keeps
 * from the original is the running-time ramp: the longer it runs without a pause, the cheaper and the faster it forges
 * ({@link #RAMP}: up to {@value #ENERGY_PERCENT} % less EU/t and {@value #TIME_PERCENT} % shorter runs after
 * {@value #RAMP_MINUTES} minutes), and a pause costs it that warmth ({@value #COOLING_FACTOR} times as fast as it
 * built). It runs only a recipe its hatches can fully supply ({@link #FORGE_GATE}).
 * <p>
 * The forge also keeps a core temperature like the Sanguinite Hearth's heat: it burns osmium plasma out of its
 * plasma hatches (100 mB a second ramping, 50 mB holding; the Mk2 halves both) and only forges while ready
 * ({@link #FORGE_READY} K). Starved of plasma it cools three times as fast as it climbed and goes cold on a
 * broken structure. The Mk2 forges the same recipes for half the plasma but only on four
 * times the input amperage ({@link #MK2_AMPS}).
 */
public class PlasmaForgeMachine extends ProcessMachine {

    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(PlasmaForgeMachine.class,
            ProcessMachine.MANAGED_FIELD_HOLDER);

    /** Minutes of running for the full ramp; the discounts at the full ramp, percent; how much faster it cools. */
    public static final int RAMP_MINUTES = 30, ENERGY_PERCENT = 50, TIME_PERCENT = 25, COOLING_FACTOR = 3;
    public static final int RAMP_TICKS = RAMP_MINUTES * 60 * 20;
    /** Ticks between two updates of the ramp. */
    private static final int RAMP_INTERVAL = 20;

    /** Core heat, K: ready at {@link #FORGE_READY}, hottest at {@link #FORGE_MAX}. */
    public static final int FORGE_READY = 9000, FORGE_MAX = 12000;
    /** Heat a fed second builds; an unfed second cools this many times as fast. */
    private static final int HEAT_CLIMB = 150, HEAT_COOLING_FACTOR = 3;
    /** Osmium plasma a fed second burns ramping (full feed) and holding (topped up); the Mk2 halves both. */
    private static final int PLASMA_RAMP = 100, PLASMA_STABLE = 50;
    /** The Mk2's power: four times the input amperage of the recipe. */
    public static final int MK2_AMPS = 4;

    /** The hall's colour in its console: forge orange. */
    private static final int COLOR = 0xFFFF9A3C;

    /**
     * Only starts a recipe the hatches can fully supply: GT's own voltage check would let a hatch of the tier below
     * start it and then starve it. GT keeps retrying a gated recipe. The forge must also be ready (burning osmium
     * plasma), and a Mk2 needs four times the recipe's input amperage on top.
     */
    public static final RecipeModifier FORGE_GATE = (machine, recipe) -> {
        if (!(machine instanceof PlasmaForgeMachine forge)) {
            return RecipeModifier.nullWrongType(PlasmaForgeMachine.class, machine);
        }
        long need = RecipeHelper.getRealEUt(recipe).getTotalEU();
        if (forge.mk2) need *= MK2_AMPS;
        if (forge.getAvailableEUt() < need) return ModifierFunction.NULL;
        if (!forge.isReady()) return ModifierFunction.NULL;
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
    /** Core heat, K, 0 to {@link #FORGE_MAX}. */
    @Persisted
    private int forgeHeat;
    /** The Mk2: half the plasma, four times the amperage. Structural, from the KubeJS definition. */
    private final boolean mk2;

    private TickableSubscription rampSubs;

    public PlasmaForgeMachine(IMachineBlockEntity holder) {
        this(holder, false);
    }

    public PlasmaForgeMachine(IMachineBlockEntity holder, boolean mk2) {
        super(holder);
        this.mk2 = mk2;
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
        forgeHeat = 0;
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
        tickHeat();
    }

    /** Burns osmium plasma out of the plasma hatches: climbing to readiness fed, cooling starved. */
    private void tickHeat() {
        int before = forgeHeat;
        int pull = forgeHeat < FORGE_MAX ? PLASMA_RAMP : PLASMA_STABLE;
        if (mk2) pull /= 2;
        if (drainPlasma(pull) >= pull) {
            forgeHeat = Math.min(FORGE_MAX, forgeHeat + HEAT_CLIMB);
        } else {
            forgeHeat = Math.max(0, forgeHeat - HEAT_CLIMB * HEAT_COOLING_FACTOR);
        }
        if (forgeHeat != before) markDirty();
    }

    /** mB of osmium plasma drawn from the plasma hatches this second. */
    private long drainPlasma(long need) {
        long left = need;
        for (var part : getParts()) {
            if (!(part instanceof PlasmaHatchPartMachine hatch) || left <= 0) continue;
            for (int i = 0; i < hatch.tank.getTanks() && left > 0; i++) {
                FluidStack stack = hatch.tank.getFluidInTank(i);
                if (!PlasmaHatchPartMachine.isPlasma(stack)) continue;
                FluidStack take = new FluidStack(stack, (int) Math.min(left, stack.getAmount()));
                left -= hatch.tank.drain(take, FluidAction.EXECUTE).getAmount();
            }
        }
        return need - left;
    }

    /** Hot enough to forge. */
    public boolean isReady() {
        return isFormed() && forgeHeat >= FORGE_READY;
    }

    /** How warm the forge has run, 0 to 1. */
    public double getRamp() {
        return isFormed() ? (double) warmth / RAMP_TICKS : 0;
    }

    @Override
    public List<Component> infoLines() {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable("af9.dtpf.console.heat", forgeHeat, FORGE_READY,
                Component.translatable(isReady() ? "af9.dtpf.console.ready" : "af9.dtpf.console.cold")));
        double ramp = getRamp();
        lines.add(Component.translatable("af9.dtpf.console.ramp", Math.round(ramp * 100)));
        lines.add(Component.translatable("af9.dtpf.console.discount", Math.round(ENERGY_PERCENT * ramp),
                Math.round(TIME_PERCENT * ramp)));
        lines.add(Component.translatable(getRecipeLogic().isWorking() ? "af9.dtpf.console.warming" :
                warmth > 0 ? "af9.dtpf.console.cooling" : "af9.dtpf.console.cold"));
        return lines;
    }
}
