package com.af9.core.machine;

import com.af9.core.AF9Core;
import com.af9.core.common.IPowerGated;
import com.af9.core.litho.LithoMode;
import com.af9.core.machine.console.ConsoleWidget;

import com.gregtechceu.gtceu.api.capability.IDataAccessHatch;
import com.gregtechceu.gtceu.api.capability.recipe.ItemRecipeCapability;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.TickableSubscription;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMaintenanceMachine;
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

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
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
public abstract class LithoMachine extends WorkableElectricMultiblockMachine implements IPowerGated {

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
     * they do not hold, a machine without one would run them all.
     */
    public static final RecipeModifier LITHO_GATE = (machine, recipe) -> {
        if (!(machine instanceof LithoMachine litho)) {
            return RecipeModifier.nullWrongType(LithoMachine.class, machine);
        }
        LithoMode mode = LithoMode.of(recipe.recipeType);
        if (mode != null && !litho.canPrint(mode)) return ModifierFunction.NULL;
        if (litho.getAvailableEUt() < RecipeHelper.getRealEUt(recipe).getTotalEU()) return ModifierFunction.NULL;
        if (!litho.isVacuumSealed()) return ModifierFunction.NULL;
        if (recipe.conditions.stream().anyMatch(ResearchCondition.class::isInstance) && !litho.hasDataHatch()) {
            return ModifierFunction.NULL;
        }
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

    /** Machine level for the pump-down time: Mk1 line 1-3, Mk2 scanner 4-5, orbital station 6. */
    protected abstract int vacuumLevel();

    /** Console title key. */
    public abstract String titleKey();

    /** Line version, 0 where there are none. */
    public int getVersion() {
        return 0;
    }

    /** Why the active mode cannot run, as a console status code, or -1 if it can. */
    public int blockedStatus(LithoMode mode) {
        return canPrint(mode) ? -1 : ConsoleWidget.STATUS_LOCKED;
    }

    //////////////////////////////////////
    // ********** Vacuum ***********//
    //////////////////////////////////////

    @Override
    public void onStructureFormed() {
        super.onStructureFormed();
        vacuumSubs = subscribeServerTick(vacuumSubs, this::updateVacuum);
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

    /** The pumps draw 1/8 A of the hatch voltage (checked every {@link #VACUUM_INTERVAL} ticks). */
    public long pumpDrainPerInterval() {
        return energyContainer == null ? 0 : energyContainer.getInputVoltage() / 8 * VACUUM_INTERVAL;
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
        printLow = cleanliness;
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
        double chance = mode.breakChance(Math.min(printLow, cleanliness), surplusFor(mode)) * breakFactor(mode, recipe);
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

    /** Prints in a run: its parallels times its batch. */
    public static int printsIn(GTRecipe recipe) {
        return Math.max(1, recipe.parallels) * Math.max(1, recipe.batchParallels);
    }

    /**
     * The finished run with some of its prints broken: the chip wafers of the others (each print's share of the run's
     * outputs) and one broken wafer of the mode's substrate per broken print (a print is one blank wafer).
     */
    static GTRecipe withBroken(GTRecipe recipe, LithoMode mode, int prints, int brokenCount) {
        Item brokenItem = ForgeRegistries.ITEMS.getValue(new ResourceLocation("kubejs", mode.brokenWafer()));
        if (brokenItem == null || brokenItem == Items.AIR) {
            AF9Core.LOGGER.warn("Item kubejs:{} not found - is the AF9 KubeJS startup script loaded?",
                    mode.brokenWafer());
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
        return id != null && id.getNamespace().equals("kubejs") && id.getPath().startsWith("broken_") &&
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
        return mode.breakChance(getPrintVacuum(), surplusFor(mode)) * breakFactor(mode, running);
    }

    /**
     * Factor on a print's break chance besides the vacuum and the version (the orbital station's coolant). The recipe
     * is the run (as modified for this machine), or null for the next print. 1 by default.
     */
    protected double breakFactor(LithoMode mode, GTRecipe recipe) {
        return 1;
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
}
