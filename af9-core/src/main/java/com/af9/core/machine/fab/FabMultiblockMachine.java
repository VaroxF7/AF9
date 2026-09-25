package com.af9.core.machine.fab;

import com.af9.core.fab.FabFamily;
import com.af9.core.fab.FabRecipeLogic;
import com.af9.core.fab.IFabMachine;

import com.gregtechceu.gtceu.api.block.IFilterType;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.feature.ICleanroomProvider;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMaintenanceMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.CleanroomType;
import com.gregtechceu.gtceu.api.machine.multiblock.CoilWorkableElectricMultiblockMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.DummyCleanroom;
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.GTRecipeType;
import com.gregtechceu.gtceu.common.data.GTBlocks;

import com.lowdragmc.lowdraglib.gui.texture.ColorBorderTexture;
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.widget.ButtonWidget;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import org.jetbrains.annotations.Nullable;

import java.util.List;

import static com.gregtechceu.gtceu.api.GTValues.MV;

/**
 * Controller of the AF9 fab multiblocks (SMC Chemical Reactor, Rectification Column, Membrane Cell Hall, Thermal
 * Processing Furnace; structures and recipe types are defined in kubejs/startup_scripts/gtceu/fab_machines.js).
 * <ul>
 * <li>Machine modes are GT recipe types, switchable with GT's mode tab or the console's mode tiles.</li>
 * <li>A roof of cleanroom filter casings makes the machine its own clean environment (ISO 5, sterile filters ISO 3,
 * which covers both cleanroom types); without one it needs a GT cleanroom around it like any machine. The
 * provider is a GT {@link DummyCleanroom}, so the machine itself can still stand inside a GT cleanroom.</li>
 * <li>PTFE pipe casings are the column trays / membrane cells: each one is a parallel (see
 * {@link com.af9.core.fab.FabModifiers#STRUCTURE_PARALLEL}); the SMC reactor gets a second parallel from
 * sterile filters instead.</li>
 * <li>Product changeover: see {@link com.af9.core.fab.FabModifiers#PURGE}.</li>
 * </ul>
 */
public class FabMultiblockMachine extends CoilWorkableElectricMultiblockMachine implements IFabMachine {

    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(
            FabMultiblockMachine.class, CoilWorkableElectricMultiblockMachine.MANAGED_FIELD_HOLDER);

    private static final ICleanroomProvider CLEAN_ISO5 = DummyCleanroom.createForTypes(
            List.of(CleanroomType.CLEANROOM));
    private static final ICleanroomProvider CLEAN_ISO3 = DummyCleanroom.createForTypes(
            List.of(CleanroomType.CLEANROOM, CleanroomType.STERILE_CLEANROOM));

    private final FabFamily family;

    @Persisted
    private String lastProduct = "";
    @Persisted
    private boolean purgeRun;
    @Persisted
    private long purges;
    // finished runs per machine mode (separate fields: LDLib persists primitives reliably)
    @Persisted
    private long count0;
    @Persisted
    private long count1;
    @Persisted
    private long count2;
    @Persisted
    private long count3;

    // structure, rebuilt on every form
    @Nullable
    private CleanroomType builtInClean;
    private int structureParallel = 1;

    public FabMultiblockMachine(IMachineBlockEntity holder, FabFamily family) {
        super(holder);
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

    //////////////////////////////////////
    // *** Multiblock LifeCycle ***//
    //////////////////////////////////////

    @Override
    public void onStructureFormed() {
        super.onStructureFormed();
        Object filter = getMultiblockState().getMatchContext().get("FilterType");
        builtInClean = filter instanceof IFilterType type ? type.getCleanroomType() : null;
        int pipes = countBlocks(GTBlocks.CASING_POLYTETRAFLUOROETHYLENE_PIPE.get());
        structureParallel = switch (family) {
            case CHEMISTRY -> builtInClean == CleanroomType.STERILE_CLEANROOM ? 2 : 1;
            case SEPARATION, ELECTROCHEMISTRY -> Math.max(1, pipes);
            case THERMAL -> 1;
        };
    }

    @Override
    public void onStructureInvalid() {
        super.onStructureInvalid();
        builtInClean = null;
        structureParallel = 1;
    }

    private int countBlocks(Block block) {
        Level level = getLevel();
        if (level == null) return 0;
        int count = 0;
        for (BlockPos pos : getMultiblockState().getCache()) {
            if (level.getBlockState(pos).is(block)) count++;
        }
        return count;
    }

    public int getStructureParallel() {
        return isFormed() ? structureParallel : 1;
    }

    @Override
    @Nullable
    public ICleanroomProvider getCleanroom() {
        if (builtInClean != null && isFormed()) {
            return builtInClean == CleanroomType.STERILE_CLEANROOM ? CLEAN_ISO3 : CLEAN_ISO5;
        }
        return super.getCleanroom();
    }

    //////////////////////////////////////
    // ****** Recipe handling *******//
    //////////////////////////////////////

    @Override
    public boolean alwaysTryModifyRecipe() {
        // the next run of the same recipe must be modified again, so it loses the changeover purge
        return true;
    }

    @Override
    public boolean beforeWorking(@Nullable GTRecipe recipe) {
        if (!super.beforeWorking(recipe)) return false;
        // the recipe was modified right before this, against the same last product
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
        return isFormed();
    }

    @Override
    public long getFabAvailableEUt() {
        return energyContainer == null ? 0 : energyContainer.getInputVoltage() * energyContainer.getInputAmperage();
    }

    @Override
    public int getFabParallel() {
        GTRecipe running = recipeLogic.getLastRecipe();
        if (recipeLogic.isActive() && running != null) return Math.max(1, running.parallels);
        return getStructureParallel();
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
        if (!isFormed()) return 0;
        return switch (family) {
            // same as GT's EBF: coil temperature, +100 K per energy hatch tier above MV
            case THERMAL -> getCoilType().getCoilTemperature() + 100 * Math.max(0, getTier() - MV);
            // jacket of the reactor vessel
            case CHEMISTRY -> getCoilType().getCoilTemperature();
            default -> 0;
        };
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
        return getParts().stream().anyMatch(part -> part instanceof IMaintenanceMachine maintenance &&
                maintenance.hasMaintenanceProblems());
    }

    public void resetFabCounters() {
        count0 = 0;
        count1 = 0;
        count2 = 0;
        count3 = 0;
        purges = 0;
        markDirty();
    }

    //////////////////////////////////////
    // ************ GUI *************//
    //////////////////////////////////////

    /** Replaces GT's text display with the fab console; GT's side tabs (power, mode, parts) stay. */
    @Override
    public Widget createUIWidget() {
        var group = new WidgetGroup(0, 0, FabConsoleWidget.WIDTH, FabConsoleWidget.HEIGHT);
        group.addWidget(new FabConsoleWidget(this, 0, 0, false));
        GTRecipeType[] types = getRecipeTypes();
        int count = Math.min(types.length, MAX_MODES);
        for (int i = 0; i < count; i++) {
            final int index = i;
            GTRecipeType type = types[i];
            String path = type.registryName.getPath();
            // clicks arrive on the client first and are then forwarded; only act on the server copy
            var tile = new ButtonWidget(FabConsoleWidget.tileX(i, count), FabConsoleWidget.TILE_Y,
                    FabConsoleWidget.tileW(count), FabConsoleWidget.TILE_H, IGuiTexture.EMPTY, click -> {
                        if (!click.isRemote) selectFabMode(index);
                    });
            tile.setHoverTexture(new ColorBorderTexture(1, 0xFFFFFFFF));
            tile.setHoverTooltips(
                    Component.translatable("gtceu." + path),
                    Component.translatable("af9.fab.console.tile_stages",
                            String.join(" > ", FabFamily.stagesOf(type))),
                    Component.translatable("af9.fab.console.tile_select"));
            group.addWidget(tile);
        }
        var reset = new ButtonWidget(FabConsoleWidget.RESET_X, FabConsoleWidget.RESET_Y, FabConsoleWidget.RESET_W,
                FabConsoleWidget.RESET_H, IGuiTexture.EMPTY, click -> {
                    if (!click.isRemote) resetFabCounters();
                });
        reset.setHoverTexture(new ColorBorderTexture(1, 0xFFFFFFFF));
        reset.setHoverTooltips(Component.translatable("af9.fab.console.reset_tooltip"));
        group.addWidget(reset);
        return group;
    }
}
