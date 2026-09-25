package com.af9.core.machine;

import com.af9.core.AF9Core;
import com.af9.core.common.IPowerGated;
import com.af9.core.litho.LithoMode;
import com.af9.core.machine.console.ConsoleWidget;

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
 * 100 in {@link #pumpDownSeconds()} (10 s per machine level: line version 1 = 10 s ... version 8 = 80 s) and stays
 * there; finished wafers do not lower it. Without power it vents linearly from 100 to 0 in {@link #VENT_SECONDS},
 * after a short grace ({@link #POWER_GRACE_TICKS}) so that a brief dip does not flip the state back and forth.
 * <p>
 * Every finished print rolls the mode's break chance ({@link LithoMode#breakChance}, from the cleanliness at that
 * moment): a broken print puts out the substrate's broken wafer instead. The recipes list the broken wafer as a
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
    /** Pump-down time from 0 to 100 per machine level (line version; the orbital station counts as level 9). */
    public static final int PUMP_DOWN_SECONDS_PER_LEVEL = 10;
    /** Time a full vacuum takes to vent to 0 without power. */
    public static final int VENT_SECONDS = 60;
    /** Ticks between two vacuum updates. */
    public static final int VACUUM_INTERVAL = 10;
    /** Ticks the pumps may go without power before the vacuum starts venting. */
    public static final int POWER_GRACE_TICKS = 60;

    /**
     * Only starts a print the machine can do right now (line version / orbit, see {@link #canPrint}) and whose EU/t
     * the hatches can supply.
     */
    public static final RecipeModifier LITHO_GATE = (machine, recipe) -> {
        if (!(machine instanceof LithoMachine litho)) {
            return RecipeModifier.nullWrongType(LithoMachine.class, machine);
        }
        LithoMode mode = LithoMode.of(recipe.recipeType);
        if (mode != null && !litho.canPrint(mode)) return ModifierFunction.NULL;
        if (litho.getAvailableEUt() < RecipeHelper.getRealEUt(recipe).getTotalEU()) return ModifierFunction.NULL;
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

    /** Machine level for the pump-down time: the line version, 9 for the orbital station. */
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

    //////////////////////////////////////
    // ********** Prints ***********//
    //////////////////////////////////////

    /**
     * A print of the mode finished: rolls the break chance against the current cleanliness (the vacuum itself is not
     * changed by a print). Returns whether the print broke.
     */
    boolean finishPrint(LithoMode mode) {
        RandomSource random = getLevel() != null ? getLevel().getRandom() : RandomSource.create();
        boolean broke = random.nextDouble() < mode.breakChance(cleanliness, surplusFor(mode));
        if (broke) {
            broken++;
        } else {
            printed++;
        }
        markDirty();
        return broke;
    }

    /**
     * The finished run with its chip wafers replaced by broken wafers of the mode's substrate: one per substrate wafer
     * that went in (one per print; a print yields up to 64 chip wafers from one blank).
     */
    static GTRecipe asBroken(GTRecipe recipe, LithoMode mode) {
        Item brokenItem = ForgeRegistries.ITEMS.getValue(new ResourceLocation("kubejs", mode.brokenWafer()));
        if (brokenItem == null || brokenItem == Items.AIR) {
            AF9Core.LOGGER.warn("Item kubejs:{} not found - is the AF9 KubeJS startup script loaded?",
                    mode.brokenWafer());
            return recipe;
        }
        int count = Math.max(1, recipe.parallels);
        GTRecipe result = recipe.copy();
        List<Content> outputs = new ArrayList<>();
        outputs.add(new Content(SizedIngredient.create(new ItemStack(brokenItem, count)),
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

    /** Break chance of the mode at the current cleanliness. */
    public double currentBreakChance(LithoMode mode) {
        return mode.breakChance(cleanliness, surplusFor(mode));
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

    /** Switches to the given mode, the same way GT's mode tab does, and drops the recipe cached for the old one. */
    public void selectMode(LithoMode mode) {
        var types = getRecipeTypes();
        for (int i = 0; i < types.length; i++) {
            if (LithoMode.of(types[i]) != mode) continue;
            if (i == getActiveRecipeType()) return;
            setActiveRecipeType(i);
            recipeLogic.updateTickSubscription();
            recipeLogic.markLastRecipeDirty();
            return;
        }
    }
}
