package com.af9.core.machine.fab;

import com.af9.core.fab.FabFamily;
import com.af9.core.fab.FabRecipeLogic;
import com.af9.core.fab.IFabMachine;

import com.gregtechceu.gtceu.api.gui.fancy.FancyMachineUIWidget;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.SimpleTieredMachine;
import com.gregtechceu.gtceu.api.machine.feature.ICleanroomProvider;
import com.gregtechceu.gtceu.api.machine.multiblock.CleanroomType;
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.GTRecipeType;

import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;
import com.lowdragmc.lowdraglib.utils.Position;

import it.unimi.dsi.fastutil.ints.Int2IntFunction;

import static com.gregtechceu.gtceu.api.GTValues.MV;

/**
 * Single-block fab machine (one per family and voltage tier, defined in kubejs/startup_scripts/gtceu/fab_machines.js).
 * GT's slot layout of the active mode stays; the fab console strip sits above it. Same changeover purge as the
 * multiblocks; the furnaces reach a fixed temperature per tier (see {@link #getMaxTemperature()}).
 */
public class FabTieredMachine extends SimpleTieredMachine implements IFabMachine {

    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(FabTieredMachine.class,
            SimpleTieredMachine.MANAGED_FIELD_HOLDER);

    private static final int STRIP_GAP = 4;

    private final FabFamily family;

    @Persisted
    private String lastProduct = "";
    @Persisted
    private boolean purgeRun;
    @Persisted
    private long purges;
    @Persisted
    private long count0;
    @Persisted
    private long count1;
    @Persisted
    private long count2;
    @Persisted
    private long count3;

    public FabTieredMachine(IMachineBlockEntity holder, int tier, Int2IntFunction tankScalingFunction,
                            FabFamily family) {
        super(holder, tier, tankScalingFunction);
        this.family = family;
    }

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    @Override
    protected RecipeLogic createRecipeLogic(Object... args) {
        // called from the super constructor: must not touch this class's fields
        return new FabRecipeLogic(this);
    }

    /** Furnace temperature of a tier, the same as the coil of that tier: MV 1800 K, +900 K per tier. */
    public static int temperatureOf(int tier) {
        return 1800 + 900 * Math.max(0, tier - MV);
    }

    public int getMaxTemperature() {
        return temperatureOf(getTier());
    }

    //////////////////////////////////////
    // ****** Recipe handling *******//
    //////////////////////////////////////

    @Override
    public boolean alwaysTryModifyRecipe() {
        return true;
    }

    @Override
    public boolean beforeWorking(GTRecipe recipe) {
        if (!super.beforeWorking(recipe)) return false;
        purgeRun = recipe != null && needsPurge(recipe);
        return true;
    }

    @Override
    public void onFabRecipeFinished(GTRecipe recipe) {
        if (purgeRun) purges++;
        purgeRun = false;
        if (recipe.id != null) lastProduct = recipe.id.toString();
        GTRecipeType[] types = getRecipeTypes();
        for (int i = 0; i < types.length && i < MAX_MODES; i++) {
            if (types[i] != recipe.recipeType) continue;
            switch (i) {
                case 0 -> count0++;
                case 1 -> count1++;
                case 2 -> count2++;
                default -> count3++;
            }
            break;
        }
        markDirty();
    }

    //////////////////////////////////////
    // ********* IFabMachine *********//
    //////////////////////////////////////

    @Override
    public FabFamily getFabFamily() {
        return family;
    }

    @Override
    public String getLastProduct() {
        return lastProduct;
    }

    @Override
    public boolean isPurgeRun() {
        return purgeRun;
    }

    @Override
    public boolean isFabFormed() {
        return true;
    }

    @Override
    public long getFabAvailableEUt() {
        return energyContainer.getEnergyStored() <= 0 ? 0 :
                energyContainer.getInputVoltage() * energyContainer.getInputAmperage();
    }

    @Override
    public int getFabParallel() {
        return 1;
    }

    @Override
    public int getFabCleanClass() {
        ICleanroomProvider provider = getCleanroom();
        if (provider == null || !provider.isClean()) return 0;
        return provider.getTypes().contains(CleanroomType.STERILE_CLEANROOM) ? 2 :
                provider.getTypes().contains(CleanroomType.CLEANROOM) ? 1 : 0;
    }

    @Override
    public int getFabHeat() {
        return family == FabFamily.THERMAL ? getMaxTemperature() : 0;
    }

    @Override
    public long[] getFabCounters() {
        return new long[] { count0, count1, count2, count3 };
    }

    @Override
    public long getFabPurges() {
        return purges;
    }

    @Override
    public boolean hasFabMaintenanceProblems() {
        return false;
    }

    //////////////////////////////////////
    // ************ GUI *************//
    //////////////////////////////////////

    /** GT's page for the active mode, with the console strip above it. */
    @Override
    public Widget createMainPage(FancyMachineUIWidget widget) {
        Widget page = super.createMainPage(widget);
        int width = Math.max(page.getSize().width, FabConsoleWidget.STRIP_WIDTH);
        int top = FabConsoleWidget.STRIP_HEIGHT + STRIP_GAP;
        var group = new WidgetGroup(0, 0, width, top + page.getSize().height);
        group.addWidget(new FabConsoleWidget(this, (width - FabConsoleWidget.STRIP_WIDTH) / 2, 0, true));
        page.setSelfPosition(new Position((width - page.getSize().width) / 2, top));
        group.addWidget(page);
        return group;
    }
}
