package com.af9.core.machine;

import com.af9.core.AF9Core;
import com.af9.core.litho.LithoMode;

import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine;
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.GTRecipeType;
import com.gregtechceu.gtceu.api.recipe.RecipeHelper;
import com.gregtechceu.gtceu.api.recipe.modifier.ModifierFunction;
import com.gregtechceu.gtceu.api.recipe.modifier.RecipeModifier;
import com.gregtechceu.gtceu.api.registry.GTRegistries;
import com.gregtechceu.gtceu.utils.FormattingUtil;

import com.lowdragmc.lowdraglib.gui.util.ClickData;
import com.lowdragmc.lowdraglib.gui.widget.ComponentPanelWidget;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Locale;

/**
 * Controller logic for the Photolithography Line (structure and recipes are defined in KubeJS).
 * <p>
 * The line has one recipe type per {@link LithoMode}; the active one is GT's machine mode, switchable with GT's
 * mode tab or the buttons in the controller display. Wafers it prints carry the mode's node and transistor count as
 * NBT (see {@link LithoMode#TAG}).
 */
@SuppressWarnings("removal") // new ResourceLocation(ns, path) is the only constructor on 1.20.1; GT uses it too
public class PhotolithographyLineMachine extends WorkableElectricMultiblockMachine {

    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(
            PhotolithographyLineMachine.class, WorkableElectricMultiblockMachine.MANAGED_FIELD_HOLDER);

    /**
     * Only starts a recipe when the energy hatches can actually supply its EU/t (GT's own voltage check would let
     * two MV hatches start an HV-voltage recipe and then starve).
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

    private void switchMode(int step) {
        int count = getRecipeTypes().length;
        int next = Math.floorMod(getActiveRecipeType() + step, count);
        if (next == getActiveRecipeType()) return;
        setActiveRecipeType(next);
        // same as GT's own mode tab, plus dropping the cached recipe of the previous mode
        recipeLogic.updateTickSubscription();
        recipeLogic.markLastRecipeDirty();
    }

    //////////////////////////////////////
    // ************ GUI *************//
    //////////////////////////////////////

    @Override
    public void addDisplayText(List<Component> textList) {
        super.addDisplayText(textList);
        if (!isFormed()) return;
        LithoMode mode = getActiveMode();
        if (mode == null) return;

        MutableComponent modeLine = Component.translatable("af9.litho.display.mode");
        modeLine.append(ComponentPanelWidget.withButton(Component.literal(" [<] "), "mode_prev"));
        modeLine.append(Component.translatable("af9.litho.mode." + mode.id).withStyle(mode.color));
        modeLine.append(ComponentPanelWidget.withButton(Component.literal(" [>]"), "mode_next"));
        textList.add(modeLine);

        long needed = mode.eut();
        long available = getAvailableEUt();
        textList.add(Component.translatable("af9.litho.display.power", FormattingUtil.formatNumbers(available),
                FormattingUtil.formatNumbers(needed))
                .withStyle(available >= needed ? ChatFormatting.GREEN : ChatFormatting.RED));
        if (available < needed) {
            textList.add(Component.translatable("af9.litho.display.power_hint",
                    Component.translatable("af9.litho.hatch." + mode.hatchTier)).withStyle(ChatFormatting.RED));
        }

        textList.add(Component.translatable("af9.litho.display.output",
                Component.literal(mode.nodeNm + " nm").withStyle(mode.color),
                Component.literal(String.format(Locale.ROOT, "x%.2f", mode.transistorDensity())).withStyle(ChatFormatting.GREEN),
                Component.literal(String.format(Locale.ROOT, "x%.2f", mode.dieFactor())).withStyle(ChatFormatting.GREEN))
                .withStyle(ChatFormatting.GRAY));

        textList.add(Component.translatable("af9.litho.display.printed", printedMuv, printedHuv, printedEuv,
                printedXuv, printedLuv).withStyle(ChatFormatting.GRAY));
        textList.add(ComponentPanelWidget.withButton(Component.translatable("af9.litho.button.reset"), "reset"));
    }

    @Override
    public void handleDisplayClick(String componentData, ClickData clickData) {
        // clicks arrive on the client first and are then forwarded; only act on the server copy
        if (clickData.isRemote || !isFormed()) return;
        switch (componentData) {
            case "mode_prev" -> switchMode(-1);
            case "mode_next" -> switchMode(1);
            case "reset" -> {
                printedMuv = 0;
                printedHuv = 0;
                printedEuv = 0;
                printedXuv = 0;
                printedLuv = 0;
                markDirty();
            }
            default -> {}
        }
    }

    //////////////////////////////////////
    // ********* Recipe viewer ********//
    //////////////////////////////////////

    /** Adds the mode's node, density and die factor to its recipes in EMI/JEI. */
    public static void registerRecipeInfo() {
        for (LithoMode mode : LithoMode.values()) {
            GTRecipeType type = GTRegistries.RECIPE_TYPES.get(new ResourceLocation("gtceu", mode.recipeTypeId()));
            if (type == null) {
                AF9Core.LOGGER.warn("Recipe type gtceu:{} not found - is the AF9 KubeJS startup script loaded?",
                        mode.recipeTypeId());
                continue;
            }
            // rendered as a plain label, so the text must not contain '%'
            type.addDataInfo(data -> Component.translatable("af9.recipe.litho_info", mode.nodeNm,
                    String.format(Locale.ROOT, "%.2f", mode.transistorDensity()), String.format(Locale.ROOT, "%.2f", mode.dieFactor()))
                    .getString());
            type.addDataInfo(data -> Component.translatable("af9.recipe.litho_power",
                    Component.translatable("af9.litho.hatch." + mode.hatchTier)).getString());
        }
    }
}
