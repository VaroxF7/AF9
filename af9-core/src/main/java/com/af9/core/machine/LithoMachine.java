package com.af9.core.machine;

import com.af9.core.AF9Config;
import com.af9.core.AF9Core;
import com.af9.core.common.IPowerGated;
import com.af9.core.litho.Coolant;
import com.af9.core.litho.LithoMode;
import com.af9.core.machine.console.ConsoleWidget;
import com.af9.core.machine.part.AirConditioningHatchPartMachine;

import com.gregtechceu.gtceu.api.capability.IDataAccessHatch;
import com.gregtechceu.gtceu.api.capability.IOpticalComputationProvider;
import com.gregtechceu.gtceu.api.capability.recipe.CWURecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.capability.recipe.IRecipeHandler;
import com.gregtechceu.gtceu.api.capability.recipe.ItemRecipeCapability;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.TickableSubscription;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMaintenanceMachine;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiPart;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine;
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.RecipeHelper;
import com.gregtechceu.gtceu.api.recipe.chance.logic.ChanceLogic;
import com.gregtechceu.gtceu.api.recipe.content.Content;
import com.gregtechceu.gtceu.api.recipe.ingredient.SizedIngredient;
import com.gregtechceu.gtceu.api.recipe.modifier.ModifierFunction;
import com.gregtechceu.gtceu.api.recipe.modifier.RecipeModifier;
import com.gregtechceu.gtceu.common.recipe.condition.ResearchCondition;

import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;

import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

/**
 * What the Photolithography Line and the Orbital Lithography Station share: the exposure vacuum, the break roll and
 * the print counters.
 * <p>
 * Vacuum: a cleanliness score of 0-100. It starts at 0 when the structure forms (breaking the structure vents it).
 * While the pumps have power (1/8 A of the hatch voltage, drawn all the time to hold the vacuum) it rises linearly to
 * 100 in {@link #pumpDownSeconds()} (10 s per machine level) and stays there; finished wafers do not lower it. Without
 * power it vents linearly from 100 to 0 in {@link #VENT_SECONDS}, after a short grace ({@link #POWER_GRACE_TICKS}) so
 * that a brief dip does not flip the state back and forth.
 * <p>
 * A print only starts on a sealed vacuum ({@link #LITHO_GATE}), so a freshly formed machine first pumps down. Every
 * finished print rolls the mode's break chance ({@link LithoMode#breakChance}) from the lowest vacuum the print went
 * through: a power loss that vents the chamber mid-print is likely to ruin the wafer. A broken print puts out the
 * substrate's broken wafer instead. The recipes list the broken wafer as a
 * chanced output (the base chance) for the recipe viewers; {@link #STRIP_BROKEN} takes it out before a run, so only
 * this roll decides.
 */
@SuppressWarnings("removal") // new ResourceLocation(ns, path) is the only constructor on 1.20.1; GT uses it too
public abstract class LithoMachine extends WorkableElectricMultiblockMachine
        implements IPowerGated, ILithoChamberMachine {

    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(LithoMachine.class,
            WorkableElectricMultiblockMachine.MANAGED_FIELD_HOLDER);

    /** Vacuum states, as the console and Jade show them. */
    public static final int VACUUM_OFF = 0;
    public static final int VACUUM_PUMPING = 1;
    public static final int VACUUM_SEALED = 2;
    public static final int VACUUM_VENTING = 3;
    /** Pump-down time from 0 to 100 per machine level ({@link #vacuumLevel()}). */
    public static final int PUMP_DOWN_SECONDS_PER_LEVEL = 10;
    /** Time a full vacuum takes to vent to 0 without power. */
    public static final int VENT_SECONDS = 60;
    /** Ticks between two vacuum updates. */
    public static final int VACUUM_INTERVAL = 10;
    /** Ticks the pumps may go without power before the vacuum starts venting. */
    public static final int POWER_GRACE_TICKS = 60;

    /**
     * Only starts a print the machine can do right now (line version / orbit, see {@link #canPrint}), whose EU/t the
     * hatches can supply, and only on a sealed vacuum. GT keeps retrying a gated recipe, so the machine starts by
     * itself once the vacuum seals. A researched print also needs a data hatch: GT's data hatches only block recipes
     * they do not hold, a machine without one would run them all. Last, the machine's own check of the recipe
     * ({@link #canRun}).
     */
    public static final RecipeModifier LITHO_GATE = (machine, recipe) -> {
        if (!(machine instanceof LithoMachine litho)) {
            return RecipeModifier.nullWrongType(LithoMachine.class, machine);
        }
        LithoMode mode = LithoMode.of(recipe.recipeType);
        if (mode != null && !litho.canPrint(mode)) return ModifierFunction.NULL;
        // the air conditioning has to carry the print's heat
        if (mode != null && !litho.hasCooling(mode)) return ModifierFunction.NULL;
        if (litho.getAvailableEUt() < RecipeHelper.getRealEUt(recipe).getTotalEU()) return ModifierFunction.NULL;
        if (!litho.isVacuumSealed()) return ModifierFunction.NULL;
        if (recipe.conditions.stream().anyMatch(ResearchCondition.class::isInstance) && !litho.hasDataHatch()) {
            return ModifierFunction.NULL;
        }
        if (!litho.canRun(recipe)) return ModifierFunction.NULL;
        return ModifierFunction.IDENTITY;
    };

    /** Removes the chanced broken wafer the recipes show; the break roll at the end of the run replaces it. */
    public static final RecipeModifier STRIP_BROKEN = (machine, recipe) -> {
        List<Content> outputs = recipe.outputs.get(ItemRecipeCapability.CAP);
        if (outputs == null || outputs.stream().noneMatch(LithoMachine::isBrokenWafer)) {
            return ModifierFunction.IDENTITY;
        }
        return modified -> {
            GTRecipe stripped = modified.copy();
            List<Content> kept = new ArrayList<>();
            for (Content content : stripped.outputs.getOrDefault(ItemRecipeCapability.CAP, List.of())) {
                if (!isBrokenWafer(content)) kept.add(content);
            }
            stripped.outputs.put(ItemRecipeCapability.CAP, kept);
            return stripped;
        };
    };

    /** Multi-patterning switched on (a screwdriver on the controller; only machines with versions, see {@link #canMultiPattern}). */
    @Persisted
    private boolean multiPatterning;
    @Persisted
    private double cleanliness;
    @Persisted
    private int vacuumState;
    /** Lowest cleanliness of the running print (its break roll uses this). */
    @Persisted
    private double printLow = 100;
    @Persisted
    private long printed;
    @Persisted
    private long broken;
    /** Whether the air conditioning went without power during the running print (its break roll doubles). */
    @Persisted
    private boolean coolingLapsed;
    /** Over the running print: the share of its OPC demand the computation met, summed per tick, and the ticks. */
    @Persisted
    private double opcSum;
    @Persisted
    private int opcTicks;

    private TickableSubscription vacuumSubs;
    private int unpoweredTicks;

    protected LithoMachine(IMachineBlockEntity holder) {
        super(holder);
    }

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    @Override
    protected RecipeLogic createRecipeLogic(Object... args) {
        return new LithoRecipeLogic(this);
    }

    /** Re-modify every run: the break roll replaces the finished run's outputs, the next run starts clean. */
    @Override
    public boolean alwaysTryModifyRecipe() {
        return true;
    }

    //////////////////////////////////////
    // ******** Machine kind *********//
    //////////////////////////////////////

    /** The modes this machine runs, in its recipe type order. */
    public abstract List<LithoMode> getModes();

    /** Whether the machine may run the mode right now (line version, orbit). */
    public abstract boolean canPrint(LithoMode mode);

    /** Line versions above the mode's own (0 for the orbital station). */
    public abstract int surplusFor(LithoMode mode);

    /** Whether the machine can multi-pattern: print the mode one version above its own (the Line and the Scanner). */
    public boolean canMultiPattern() {
        return false;
    }

    /** Multi-patterning is on (always off where it cannot be). */
    public boolean isMultiPatterning() {
        return multiPatterning && canMultiPattern();
    }

    /** Whether a print of the mode is multi-patterned: switched on, and the mode is above the machine's version. */
    public boolean isMultiPatterned(LithoMode mode) {
        return isMultiPatterning() && mode.level() > getVersion();
    }

    /** A screwdriver on the controller switches multi-patterning, between prints. */
    @Override
    protected InteractionResult onScrewdriverClick(Player player, InteractionHand hand, Direction side,
                                                   BlockHitResult hit) {
        if (!canMultiPattern()) return super.onScrewdriverClick(player, hand, side, hit);
        if (isRemote()) return InteractionResult.SUCCESS;
        if (getRecipeLogic().isWorking()) {
            player.displayClientMessage(Component.translatable("af9.litho.multipatterning.busy"), true);
            return InteractionResult.SUCCESS;
        }
        multiPatterning = !multiPatterning;
        markDirty();
        player.displayClientMessage(Component.translatable(multiPatterning ? "af9.litho.multipatterning.on" :
                "af9.litho.multipatterning.off"), true);
        return InteractionResult.SUCCESS;
    }

    /** Machine level for the pump-down time: Mk1 line 1-3, Mk2 scanner 4-5, orbital station 6. */
    protected abstract int vacuumLevel();

    /** Console title key. */
    public abstract String titleKey();

    /** Line version, 0 where there are none. */
    public int getVersion() {
        return 0;
    }

    /** Whether this machine may start the recipe, beyond its mode ({@link #LITHO_GATE}). */
    public boolean canRun(GTRecipe recipe) {
        return true;
    }

    /** Status while the machine is not ready to print yet ({@link #isVacuumSealed()} false): pumping down. */
    public int notReadyStatus() {
        return ConsoleWidget.STATUS_PUMPING_DOWN;
    }

    /** The Array Mk2's beam focus in permille (0-1000), -1 where the machine has none. */
    public int focusPermille() {
        return -1;
    }

    /** Why the active mode cannot run, as a console status code, or -1 if it can. */
    public int blockedStatus(LithoMode mode) {
        if (!canPrint(mode)) return ConsoleWidget.STATUS_LOCKED;
        return hasCooling(mode) ? -1 : ConsoleWidget.STATUS_NO_COOLING;
    }

    //////////////////////////////////////
    // ********** Vacuum ***********//
    //////////////////////////////////////

    @Override
    public void onStructureFormed() {
        super.onStructureFormed();
        vacuumSubs = subscribeServerTick(vacuumSubs, this::tickProcess);
    }

    /** Everything the machine does besides the recipe, each tick: vacuum, cooling, OPC. */
    private void tickProcess() {
        updateVacuum();
        updateCooling();
        updateOpc();
    }

    /** A broken structure loses its vacuum: the next time it forms it pumps down from 0. */
    @Override
    public void onStructureInvalid() {
        super.onStructureInvalid();
        unsubscribeVacuum();
        cleanliness = 0;
        vacuumState = VACUUM_OFF;
        printLow = 0;
        unpoweredTicks = 0;
        markDirty();
    }

    @Override
    public void onUnload() {
        super.onUnload();
        unsubscribeVacuum();
    }

    private void unsubscribeVacuum() {
        if (vacuumSubs != null) {
            vacuumSubs.unsubscribe();
            vacuumSubs = null;
        }
    }

    protected void updateVacuum() {
        if (getOffsetTimer() % VACUUM_INTERVAL != 0 || !isFormed()) return;
        double before = cleanliness;
        int stateBefore = vacuumState;
        long drain = pumpDrainPerInterval();
        if (energyContainer != null && drain > 0 && energyContainer.getEnergyStored() >= drain) {
            // the pumps run all the time: they pump down, then hold the vacuum
            energyContainer.removeEnergy(drain);
            unpoweredTicks = 0;
            cleanliness = Math.min(100, cleanliness + 100.0 * VACUUM_INTERVAL / (20.0 * pumpDownSeconds()));
            vacuumState = cleanliness >= 100 ? VACUUM_SEALED : VACUUM_PUMPING;
        } else {
            unpoweredTicks = Math.min(POWER_GRACE_TICKS, unpoweredTicks + VACUUM_INTERVAL);
            if (unpoweredTicks >= POWER_GRACE_TICKS) {
                cleanliness = Math.max(0, cleanliness - 100.0 * VACUUM_INTERVAL / (20.0 * VENT_SECONDS));
                vacuumState = cleanliness > 0 ? VACUUM_VENTING : VACUUM_OFF;
            }
        }
        if (getRecipeLogic().isActive() && cleanliness < printLow) {
            // a print in progress (running or starved of power) remembers the worst vacuum it went through
            printLow = cleanliness;
            markDirty();
        }
        if (cleanliness != before || vacuumState != stateBefore) markDirty();
    }

    /**
     * The pumps draw 1/8 A of the hatch voltage (checked every {@link #VACUUM_INTERVAL} ticks): of one hatch. GT's
     * combined input voltage is the whole supply as one amp (two 2A LuV hatches: 4A LuV = 1A of ZPM), 1/8 of that
     * on top of a print overclocked to the hatches' tier (15/16 of the supply) drained the hatches.
     */
    public long pumpDrainPerInterval() {
        return energyContainer == null ? 0 : energyContainer.getHighestInputVoltage() / 8 * VACUUM_INTERVAL;
    }

    /** Seconds from 0 to 100 at this machine's level. */
    public int pumpDownSeconds() {
        return PUMP_DOWN_SECONDS_PER_LEVEL * Math.max(1, vacuumLevel());
    }

    public double getCleanliness() {
        return cleanliness;
    }

    /** One of {@link #VACUUM_OFF}, {@link #VACUUM_PUMPING}, {@link #VACUUM_SEALED}, {@link #VACUUM_VENTING}. */
    public int getVacuumState() {
        return isFormed() ? vacuumState : VACUUM_OFF;
    }

    public boolean isPumping() {
        return getVacuumState() == VACUUM_PUMPING;
    }

    /** Pumped down to 100: prints may start. */
    public boolean isVacuumSealed() {
        return getVacuumState() == VACUUM_SEALED;
    }

    /** Lowest vacuum of the running print, 100 while nothing is printing. */
    public double getPrintVacuum() {
        return getRecipeLogic().isActive() ? Math.min(printLow, cleanliness) : 100;
    }

    //////////////////////////////////////
    // ********** Prints ***********//
    //////////////////////////////////////

    @Override
    public boolean beforeWorking(GTRecipe recipe) {
        if (!super.beforeWorking(recipe)) return false;
        printLow = getCleanliness();
        coolingLapsed = false;
        opcSum = 0;
        opcTicks = 0;
        return true;
    }

    /**
     * A run of the mode finished: every print in it (parallels x batch) rolls the break chance on its own, against the
     * lowest vacuum the run went through (the vacuum itself is not changed by a print), times the machine's own factor
     * for that run ({@link #breakFactor}). Returns the run to put out: unchanged if nothing broke, else with a broken
     * wafer for each broken print and the chip wafers of the others.
     */
    GTRecipe finishPrints(LithoMode mode, GTRecipe recipe) {
        RandomSource random = getLevel() != null ? getLevel().getRandom() : RandomSource.create();
        double chance = Math.min(LithoMode.MAX_BREAK,
                mode.breakChance(rollVacuum(), surplusFor(mode)) * breakFactor(mode, true));
        int prints = printsIn(recipe);
        int brokenNow = 0;
        for (int i = 0; i < prints; i++) {
            if (random.nextDouble() < chance) brokenNow++;
        }
        printed += prints - brokenNow;
        broken += brokenNow;
        markDirty();
        return brokenNow == 0 ? recipe : withBroken(recipe, mode, prints, brokenNow);
    }

    /** The vacuum a finished run's break roll uses: the lowest the run went through. */
    protected double rollVacuum() {
        return Math.min(printLow, cleanliness);
    }

    /** Prints in a run: its parallels times its batch. */
    public static int printsIn(GTRecipe recipe) {
        return Math.max(1, recipe.parallels) * Math.max(1, recipe.batchParallels);
    }

    /**
     * The finished run with some of its prints broken: the chip wafers of the others (each print's share of the run's
     * outputs) and one broken wafer of the mode's substrate per broken print (a print is one blank wafer).
     */
    static GTRecipe withBroken(GTRecipe recipe, LithoMode mode, int prints, int brokenCount) {
        Item brokenItem = ForgeRegistries.ITEMS.getValue(new ResourceLocation(AF9Core.MOD_ID, mode.brokenWafer()));
        if (brokenItem == null || brokenItem == Items.AIR) {
            AF9Core.LOGGER.warn("Item af9:{} not found", mode.brokenWafer());
            return recipe;
        }
        int kept = prints - brokenCount;
        GTRecipe result = recipe.copy();
        List<Content> outputs = new ArrayList<>();
        for (Content content : recipe.outputs.getOrDefault(ItemRecipeCapability.CAP, List.of())) {
            if (kept <= 0 || isBrokenWafer(content)) continue;
            if (!(content.content instanceof Ingredient ingredient) || ingredient.getItems().length == 0) continue;
            ItemStack stack = ingredient.getItems()[0].copy();
            stack.setCount(stack.getCount() / prints * kept);
            if (stack.isEmpty()) continue;
            outputs.add(new Content(SizedIngredient.create(stack), ChanceLogic.getMaxChancedValue(),
                    ChanceLogic.getMaxChancedValue(), 0));
        }
        outputs.add(new Content(SizedIngredient.create(new ItemStack(brokenItem, brokenCount)),
                ChanceLogic.getMaxChancedValue(), ChanceLogic.getMaxChancedValue(), 0));
        result.outputs.put(ItemRecipeCapability.CAP, outputs);
        return result;
    }

    static boolean isBrokenWafer(Content content) {
        if (!(content.content instanceof Ingredient ingredient)) return false;
        ItemStack[] items = ingredient.getItems();
        if (items.length == 0) return false;
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(items[0].getItem());
        return id != null && id.getNamespace().equals(AF9Core.MOD_ID) && id.getPath().startsWith("broken_") &&
                id.getPath().endsWith("_wafer");
    }

    public long getPrinted() {
        return printed;
    }

    public long getBroken() {
        return broken;
    }

    public void resetCounters() {
        printed = 0;
        broken = 0;
        markDirty();
    }

    //////////////////////////////////////
    // ********* Machine state ********//
    //////////////////////////////////////

    @Override
    public long getAvailableEUt() {
        return energyContainer == null || !isFormed() ? 0 :
                energyContainer.getInputVoltage() * energyContainer.getInputAmperage();
    }

    public LithoMode getActiveMode() {
        LithoMode mode = LithoMode.of(getRecipeType());
        return mode != null ? mode : getModes().get(0);
    }

    public boolean hasMaintenanceProblems() {
        return getParts().stream().anyMatch(part -> part instanceof IMaintenanceMachine maintenance &&
                maintenance.hasMaintenanceProblems());
    }

    /** Break chance of the running print (from its lowest vacuum), or of the next one (on a sealed vacuum). */
    public double currentBreakChance(LithoMode mode) {
        GTRecipe running = getRecipeLogic().isActive() ? getRecipeLogic().getLastRecipe() : null;
        return Math.min(LithoMode.MAX_BREAK,
                mode.breakChance(getPrintVacuum(), surplusFor(mode)) * breakFactor(mode, running != null));
    }

    /**
     * Everything on a print's break chance besides the vacuum and the version: the machine's own factor (the orbital
     * station's coolant), the air conditioning and the OPC the computation gave.
     *
     * @param measured true for a print that ran (what its cooling and computation really were), false for the next
     *                 one (what they would be now)
     */
    private double breakFactor(LithoMode mode, boolean measured) {
        return machineBreakFactor(mode, measured) * coolingBreakFactor(mode, measured) *
                opcBreakFactor(mode, measured) *
                (isMultiPatterned(mode) ? LithoMode.MULTI_PATTERNING_BREAK : 1);
    }

    /**
     * Factor on a print's break chance of the machine's own kind (the orbital station's coolant); the recipe the
     * machine would take its coolant from is the running one, or null for the next print. 1 by default.
     */
    protected double machineBreakFactor(LithoMode mode, boolean measured) {
        return 1;
    }

    //////////////////////////////////////
    // ********** Cooling ***********//
    //////////////////////////////////////

    /** Cooling units the Air Conditioning Hatches of the structure give together. */
    public int getCoolingCapacity() {
        if (!isFormed()) return 0;
        int units = 0;
        for (IMultiPart part : getParts()) {
            if (part instanceof AirConditioningHatchPartMachine hatch) units += hatch.getCoolingUnits();
        }
        return units;
    }

    /** Whether the mode's print puts heat into the air that has to be carried off (not on the orbital station). */
    public boolean needsAirCooling(LithoMode mode) {
        return mode.heatLoad() > 0 && AF9Config.LITHO_AIR_COOLING.get();
    }

    /** The air conditioning carries the mode's heat load (or none is needed). */
    public boolean hasCooling(LithoMode mode) {
        return !needsAirCooling(mode) || getCoolingCapacity() >= mode.heatLoad();
    }

    /** Doublings of the cooling units above the mode's heat load, 0 to {@link LithoMode#MAX_COOLING_STEPS}. */
    public int coolingSteps(LithoMode mode) {
        if (!needsAirCooling(mode)) return 0;
        int capacity = getCoolingCapacity();
        int steps = 0;
        while (steps < LithoMode.MAX_COOLING_STEPS && capacity >= mode.heatLoad() * (2 << steps)) steps++;
        return steps;
    }

    /** A print is running that the air conditioning is cooling (and it still has its power). */
    public boolean isCooling() {
        return isFormed() && getRecipeLogic().isWorking() && needsAirCooling(getActiveMode()) && !coolingLapsed;
    }

    /** Whether the air conditioning lost its power during the running print. */
    public boolean hasCoolingLapsed() {
        return coolingLapsed;
    }

    /** What the hatches draw in one {@link #VACUUM_INTERVAL}. */
    public long coolingDrawPerInterval() {
        long draw = 0;
        for (IMultiPart part : getParts()) {
            if (part instanceof AirConditioningHatchPartMachine hatch) draw += hatch.getDraw();
        }
        return draw * VACUUM_INTERVAL;
    }

    /** Pays the hatches while a print runs; a print whose cooling went without power breaks twice as often. */
    private void updateCooling() {
        if (getOffsetTimer() % VACUUM_INTERVAL != 0 || !isFormed() || !getRecipeLogic().isWorking()) return;
        if (!needsAirCooling(getActiveMode()) || getCoolingCapacity() <= 0) return;
        long draw = coolingDrawPerInterval();
        if (energyContainer != null && energyContainer.getEnergyStored() >= draw) {
            energyContainer.removeEnergy(draw);
        } else if (!coolingLapsed) {
            coolingLapsed = true;
            markDirty();
        }
    }

    private double coolingBreakFactor(LithoMode mode, boolean measured) {
        if (!needsAirCooling(mode)) return 1;
        if (measured && coolingLapsed) return LithoMode.COOLING_LAPSE_FACTOR;
        return Math.pow(Coolant.BREAK_FACTOR, coolingSteps(mode));
    }

    //////////////////////////////////////
    // ************* OPC **************//
    //////////////////////////////////////

    /** Whether a computation source (a computation hatch) is in the structure. */
    public boolean hasOpcSource() {
        if (!isFormed()) return false;
        for (IRecipeHandler<?> handler : getCapabilitiesFlat(IO.IN, CWURecipeCapability.CAP)) {
            if (handler instanceof IOpticalComputationProvider) return true;
        }
        return false;
    }

    /** Takes up to {@code cwut} CWU/t from the computation sources of the structure (or only asks). */
    private int requestOpc(int cwut, boolean simulate) {
        int got = 0;
        for (IRecipeHandler<?> handler : getCapabilitiesFlat(IO.IN, CWURecipeCapability.CAP)) {
            if (got >= cwut) break;
            if (handler instanceof IOpticalComputationProvider provider) {
                got += Math.max(0, provider.requestCWUt(cwut - got, simulate, new ArrayList<>()));
            }
        }
        return got;
    }

    /** While a print runs, draws its OPC and alignment computation and remembers how much of it there was. */
    private void updateOpc() {
        if (!isFormed() || !getRecipeLogic().isWorking()) return;
        int demand = opcDemandOf(getActiveMode());
        if (demand <= 0 || !hasOpcSource()) return;
        opcSum += Math.min(1.0, (double) requestOpc(demand, false) / demand);
        opcTicks++;
    }

    /**
     * The share of the mode's OPC demand the computation meets: over the running print, or what it would be now (the
     * sources asked without taking anything). -1 for a mode that has none or a machine without a source.
     */
    public double getOpcRatio(LithoMode mode, boolean measured) {
        int demand = opcDemandOf(mode);
        if (demand <= 0 || !hasOpcSource()) return -1;
        if (measured) return opcTicks == 0 ? 0 : opcSum / opcTicks;
        return Math.min(1.0, (double) requestOpc(demand, true) / demand);
    }

    /** CWU/t of OPC computation the mode asks for: twice as much when its print is multi-patterned. */
    public int opcDemandOf(LithoMode mode) {
        return mode.opcDemand() * (isMultiPatterned(mode) ? LithoMode.MULTI_PATTERNING_FACTOR : 1);
    }

    private double opcBreakFactor(LithoMode mode, boolean measured) {
        double ratio = getOpcRatio(mode, measured);
        return ratio <= 0 ? 1 : 1 - LithoMode.OPC_BONUS * ratio;
    }

    /** A data access hatch or an optical data hatch in the structure (for researched prints). */
    public boolean hasDataHatch() {
        return getParts().stream().anyMatch(part -> part instanceof IDataAccessHatch);
    }

    /** Registry id of the first item the running print puts out (before the break roll), or null. */
    public ResourceLocation getCurrentProduct() {
        GTRecipe recipe = getRecipeLogic().getLastRecipe();
        if (recipe == null || !getRecipeLogic().isWorking()) return null;
        for (Content content : recipe.outputs.getOrDefault(ItemRecipeCapability.CAP, List.of())) {
            if (content.content instanceof Ingredient ingredient && ingredient.getItems().length > 0) {
                return ForgeRegistries.ITEMS.getKey(ingredient.getItems()[0].getItem());
            }
        }
        return null;
    }

    //////////////////////////////////////
    // ******** Exposure chamber *******//
    //////////////////////////////////////

    /**
     * The exposure chamber render (Mk1 stepper, Mk2 tube): lit while a print runs, in the active mode's colour.
     * Progress follows the print (smooth with {@code partialTick}); -1 idle. When GT has not synced a duration
     * yet the render falls back to its own loop, so the chamber still animates while working.
     */
    @Override
    public boolean isChamberFormed() {
        return isFormed();
    }

    @Override
    public boolean isChamberLit() {
        return isFormed() && getRecipeLogic().isWorking();
    }

    @Override
    public int getChamberColor() {
        LithoMode active = getActiveMode();
        return active != null ? active.argb : 0xFFB978FF;
    }

    @Override
    public float getChamberProgress(float partialTick) {
        if (!getRecipeLogic().isWorking()) return -1F;
        int duration = getRecipeLogic().getDuration();
        if (duration > 0) {
            float p = (getRecipeLogic().getProgress() + partialTick) / duration;
            return Math.max(0F, Math.min(1F, p));
        }
        // not synced yet: loop so the laser still sweeps while working
        return ((getOffsetTimer() + partialTick) % 360F) / 360F;
    }
}
