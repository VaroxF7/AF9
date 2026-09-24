package com.af9.core.machine;

import com.af9.core.AF9Core;
import com.af9.core.litho.LithoMode;

import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMaintenanceMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine;
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.GTRecipeType;
import com.gregtechceu.gtceu.api.recipe.RecipeHelper;
import com.gregtechceu.gtceu.api.recipe.modifier.ModifierFunction;
import com.gregtechceu.gtceu.api.recipe.modifier.RecipeModifier;
import com.gregtechceu.gtceu.api.registry.GTRegistries;

import com.lowdragmc.lowdraglib.gui.texture.ColorBorderTexture;
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.widget.ButtonWidget;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * Controller logic for the Photolithography Line (structure and recipes are defined in KubeJS).
 * <p>
 * The line has one recipe type per {@link LithoMode}; the active one is GT's machine mode, switchable with GT's mode
 * tab or the console's mode tiles ({@link LithoConsoleWidget}). Wafers it prints carry the mode's node and transistor
 * count as NBT (see {@link LithoMode#TAG}).
 */
@SuppressWarnings("removal") // new ResourceLocation(ns, path) is the only constructor on 1.20.1; GT uses it too
public class PhotolithographyLineMachine extends WorkableElectricMultiblockMachine {

    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(
            PhotolithographyLineMachine.class, WorkableElectricMultiblockMachine.MANAGED_FIELD_HOLDER);

    /**
     * Only starts a recipe when the energy hatches can actually supply its EU/t (GT's own voltage check would let
     * two hatches of the tier below start the recipe and then starve).
     */
    public static final RecipeModifier LITHO_GATE = (machine, recipe) -> {
        if (!(machine instanceof PhotolithographyLineMachine line)) {
            return RecipeModifier.nullWrongType(PhotolithographyLineMachine.class, machine);
        }
        if (line.getAvailableEUt() < RecipeHelper.getRealEUt(recipe).getTotalEU()) return ModifierFunction.NULL;
        return ModifierFunction.IDENTITY;
    };

    // wafers printed per mode (separate fields: LDLib persists primitives reliably)
    @Persisted
    private long printedMuv;
    @Persisted
    private long printedHuv;
    @Persisted
    private long printedEuv;
    @Persisted
    private long printedXuv;
    @Persisted
    private long printedLuv;

    public PhotolithographyLineMachine(IMachineBlockEntity holder) {
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

    public long getAvailableEUt() {
        return energyContainer == null ? 0 : energyContainer.getInputVoltage() * energyContainer.getInputAmperage();
    }

    public static LithoMode modeOf(GTRecipeType type) {
        return type == null ? null : LithoMode.fromRecipeTypePath(type.registryName.getPath());
    }

    public LithoMode getActiveMode() {
        return modeOf(getRecipeType());
    }

    public boolean hasMaintenanceProblems() {
        return getParts().stream().anyMatch(part -> part instanceof IMaintenanceMachine maintenance &&
                maintenance.hasMaintenanceProblems());
    }

    /** Printed wafers per mode, indexed by {@link LithoMode#ordinal()}. */
    public long[] getPrintedCounts() {
        return new long[] { printedMuv, printedHuv, printedEuv, printedXuv, printedLuv };
    }

    void recordPrinted(GTRecipe recipe) {
        LithoMode mode = modeOf(recipe.recipeType);
        if (mode == null) return;
        switch (mode) {
            case MUV -> printedMuv++;
            case HUV -> printedHuv++;
            case EUV -> printedEuv++;
            case XUV -> printedXuv++;
            case LUV -> printedLuv++;
        }
        markDirty();
    }

    /** Switches to the given mode, the same way GT's mode tab does, and drops the recipe cached for the old one. */
    public void selectMode(LithoMode mode) {
        GTRecipeType[] types = getRecipeTypes();
        for (int i = 0; i < types.length; i++) {
            if (modeOf(types[i]) != mode) continue;
            if (i == getActiveRecipeType()) return;
            setActiveRecipeType(i);
            recipeLogic.updateTickSubscription();
            recipeLogic.markLastRecipeDirty();
            return;
        }
    }

    public void resetCounters() {
        printedMuv = 0;
        printedHuv = 0;
        printedEuv = 0;
        printedXuv = 0;
        printedLuv = 0;
        markDirty();
    }

    //////////////////////////////////////
    // ************ GUI *************//
    //////////////////////////////////////

    /** Replaces GT's text display with the console; GT's side tabs (power, mode, parts) stay. */
    @Override
    public Widget createUIWidget() {
        var group = new WidgetGroup(0, 0, LithoConsoleWidget.WIDTH, LithoConsoleWidget.HEIGHT);
        group.addWidget(new LithoConsoleWidget(this, 0, 0));
        for (LithoMode mode : LithoMode.values()) {
            // clicks arrive on the client first and are then forwarded; only act on the server copy
            var tile = new ButtonWidget(LithoConsoleWidget.tileX(mode.ordinal()), LithoConsoleWidget.TILE_Y,
                    LithoConsoleWidget.TILE_W, LithoConsoleWidget.TILE_H, IGuiTexture.EMPTY,
                    click -> {
                        if (!click.isRemote) selectMode(mode);
                    });
            tile.setHoverTexture(new ColorBorderTexture(1, 0xFFFFFFFF));
            tile.setHoverTooltips(
                    Component.translatable("af9.litho.mode." + mode.id).withStyle(mode.color),
                    Component.translatable("af9.litho.console.tile_power",
                            Component.translatable("af9.litho.hatch." + mode.hatchTier)),
                    Component.translatable("af9.litho.console.tile_substrate",
                            Component.translatable("af9.litho.substrate." + mode.substrate)));
            group.addWidget(tile);
        }
        var reset = new ButtonWidget(LithoConsoleWidget.RESET_X, LithoConsoleWidget.RESET_Y, LithoConsoleWidget.RESET_W,
                LithoConsoleWidget.RESET_H, IGuiTexture.EMPTY, click -> {
                    if (!click.isRemote) resetCounters();
                });
        reset.setHoverTexture(new ColorBorderTexture(1, 0xFFFFFFFF));
        reset.setHoverTooltips(Component.translatable("af9.litho.console.reset_tooltip"));
        group.addWidget(reset);
        return group;
    }

    //////////////////////////////////////
    // ********* Recipe viewer ********//
    //////////////////////////////////////

    /** Adds the mode's node to its recipes in EMI/JEI, as one short line (the console shows the rest). */
    public static void registerRecipeInfo() {
        for (LithoMode mode : LithoMode.values()) {
            GTRecipeType type = GTRegistries.RECIPE_TYPES.get(new ResourceLocation("gtceu", mode.recipeTypeId()));
            if (type == null) {
                AF9Core.LOGGER.warn("Recipe type gtceu:{} not found - is the AF9 KubeJS startup script loaded?",
                        mode.recipeTypeId());
                continue;
            }
            // rendered as a plain label, so the text must not contain '%'
            type.addDataInfo(data -> Component.translatable("af9.recipe.litho_node", mode.nodeNm).getString());
        }
    }
}
