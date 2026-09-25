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
 * Vacuum: a cleanliness score of 0-100. While the structure is formed, has energy and no maintenance problems, the
 * pumps raise it every second by a share of what is missing ({@link #pumpRate()}, at least 0.5) and draw 1/8 A of the
 * hatch voltage for it; without energy (or with maintenance problems) it leaks 0.5 per second. Every finished wafer
 * drops it by 10-15 points.
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

    public static final double MIN_PUMP_STEP = 0.5;
    public static final double LEAK_PER_SECOND = 0.5;
    public static final int CLEAN_DROP_MIN = 10;
    public static final int CLEAN_DROP_MAX = 15;

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
    private long printed;
    @Persisted
    private long broken;

    private TickableSubscription vacuumSubs;

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

    /** Share of the missing cleanliness the pumps recover per second. */
    protected abstract double pumpRate();

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

    @Override
    public void onStructureInvalid() {
        super.onStructureInvalid();
        unsubscribeVacuum();
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
        if (getOffsetTimer() % 20 != 0 || !isFormed()) return;
        double before = cleanliness;
        long drain = pumpDrainPerSecond();
        boolean powered = energyContainer != null && energyContainer.getEnergyStored() >= Math.max(1, drain);
        if (!powered || hasMaintenanceProblems()) {
            cleanliness = Math.max(0, cleanliness - LEAK_PER_SECOND);
        } else if (cleanliness < 100) {
            energyContainer.removeEnergy(drain);
            cleanliness = Math.min(100, cleanliness + Math.max(MIN_PUMP_STEP, (100 - cleanliness) * pumpRate()));
        }
        if (cleanliness != before) markDirty();
    }

    /** The pumps draw 1/8 A of the hatch voltage while they work (checked once a second, so x20). */
    public long pumpDrainPerSecond() {
        return energyContainer == null ? 0 : energyContainer.getInputVoltage() / 8 * 20;
    }

    public double getCleanliness() {
        return cleanliness;
    }

    public boolean isPumping() {
        return isFormed() && cleanliness < 100 && energyContainer != null &&
                energyContainer.getEnergyStored() >= Math.max(1, pumpDrainPerSecond()) && !hasMaintenanceProblems();
    }

    //////////////////////////////////////
    // ********** Prints ***********//
    //////////////////////////////////////

    /**
     * A print of the mode finished: rolls the break chance against the current cleanliness, then the vacuum loses
     * 10-15 points. Returns whether the print broke.
     */
    boolean finishPrint(LithoMode mode) {
        RandomSource random = getLevel() != null ? getLevel().getRandom() : RandomSource.create();
        boolean broke = random.nextDouble() < mode.breakChance(cleanliness, surplusFor(mode));
        int drop = CLEAN_DROP_MIN + random.nextInt(CLEAN_DROP_MAX - CLEAN_DROP_MIN + 1);
        cleanliness = Math.max(0, cleanliness - drop);
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
