package com.af9.core.compat.xei;

import com.af9.core.AF9Core;
import com.af9.core.machine.StagedAssemblyMachine;
import com.af9.core.staged.StagedRecipes;

import com.gregtechceu.gtceu.api.capability.recipe.FluidRecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.capability.recipe.RecipeCapability;
import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.gui.editor.IEditableUI;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.GTRecipeType;
import com.gregtechceu.gtceu.api.recipe.content.Content;
import com.gregtechceu.gtceu.api.recipe.ui.GTRecipeTypeUI;
import com.gregtechceu.gtceu.api.recipe.ui.GTRecipeTypeUI.RecipeHolder;
import com.gregtechceu.gtceu.api.registry.GTRegistries;
import com.gregtechceu.gtceu.integration.xei.widgets.GTRecipeWidget;
import com.gregtechceu.gtceu.utils.FormattingUtil;

import com.lowdragmc.lowdraglib.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.texture.ProgressTexture;
import com.lowdragmc.lowdraglib.gui.widget.LabelWidget;
import com.lowdragmc.lowdraglib.gui.widget.ProgressWidget;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import com.google.common.collect.Tables;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;

import static com.lowdragmc.lowdraglib.gui.texture.ProgressTexture.FillDirection.LEFT_TO_RIGHT;

/**
 * The JEI / EMI / REI page of staged recipes: one column of inputs per step, headed by its roman numeral (hover for
 * the step's duration and energy), then the arrow and the final outputs. GT's own footer (total duration, total EU,
 * EU/t, tier) is drawn by the widget around it and stays as it is.
 * <p>
 * The durations and totals shown are the base values: GT 7.2.0's recipe widget does not expose the selected
 * overclock tier to the UI builder, so per-tier simulation like Star Technology's fork has is not possible here.
 * The page fits {@value #MAX_STEPS} steps; the KubeJS helper refuses more.
 */
public final class StagedRecipeUI {

    private static final int WIDTH = 164;
    private static final int HEIGHT = 150;
    /** Widest page that still fits the recipe viewer: seven 20 px step columns plus the outputs. */
    public static final int MAX_STEPS = 7;
    /** Widest column: item and fluid inputs of one step stack below its numeral. */
    public static final int MAX_STEP_INPUTS = 5;

    private StagedRecipeUI() {}

    /**
     * Common setup, after KubeJS registered the type: swaps GT's default recipe page for the staged one. Called
     * from {@link AF9Core} setup, never from a script (script timing cannot guarantee the type exists yet).
     */
    @SuppressWarnings("removal") // new ResourceLocation(ns, path) is the only constructor on 1.20.1
    public static void install() {
        GTRecipeType type = GTRegistries.RECIPE_TYPES.get(
                new ResourceLocation("gtceu", StagedAssemblyMachine.RECIPE_TYPE));
        if (type == null) {
            AF9Core.LOGGER.warn("Recipe type gtceu:{} not found - is the AF9 KubeJS startup script loaded?",
                    StagedAssemblyMachine.RECIPE_TYPE);
            return;
        }
        var ui = new GTRecipeTypeUI(type) {
            @Override
            public IEditableUI<WidgetGroup, RecipeHolder> createEditableUITemplate(boolean isSteam,
                    boolean isHighPressure) {
                return new IEditableUI.Normal<>(
                        () -> new WidgetGroup(0, 0, WIDTH, HEIGHT),
                        (container, recipeHolder) -> {});
            }
        };
        ui.setUiBuilder(StagedRecipeUI::build);
        type.setRecipeUI(ui);
    }

    private static void build(GTRecipe root, WidgetGroup group) {
        List<GTRecipe> steps = StagedRecipes.getSteps(root);
        if (steps.isEmpty() || steps.size() > MAX_STEPS || !(group instanceof GTRecipeWidget widget)) return;
        var content = (WidgetGroup) widget.getFirstWidgetById(GTRecipeWidget.RECIPE_CONTENT_GROUP_ID_REGEX);
        if (content == null) return;
        content.clearAllWidgets();
        new Builder(widget, content, root, steps).build();
    }

    private static final class Builder {

        private final GTRecipeWidget widget;
        private final WidgetGroup group;
        private final GTRecipe root;
        private final List<GTRecipe> steps;
        private final GTRecipeTypeUI recipeUI;

        Builder(GTRecipeWidget widget, WidgetGroup group, GTRecipe root, List<GTRecipe> steps) {
            this.widget = widget;
            this.group = group;
            this.root = root;
            this.steps = steps;
            this.recipeUI = root.recipeType.getRecipeUI();
        }

        private void build() {
            float centerX = WIDTH / 2f;
            float sliceWidth = 20f;
            float startX = centerX - (steps.size() / 2f) * sliceWidth + 1f;
            int inputsHeight = 0;
            for (int step = 0; step < steps.size(); step++) {
                GTRecipe recipe = steps.get(step);
                int height = buildStepInput((int) (startX + step * sliceWidth), step, holder(recipe), recipe);
                if (height > inputsHeight) inputsHeight = height;
            }

            int bottomY = inputsHeight + 2;
            var progressBarTexture = new ProgressTexture(
                    GuiTextures.PROGRESS_BAR_ARROW.getSubTexture(0, 0, 1, 0.5),
                    GuiTextures.PROGRESS_BAR_ARROW.getSubTexture(0, 0.5, 1, 0.5))
                    .setFillDirection(LEFT_TO_RIGHT);
            progressBarTexture.rotate(90f);
            float outputsEndX = centerX + (steps.size() / 2f) * sliceWidth + 1f;
            var progress = new ProgressWidget(ProgressWidget.JEIProgress,
                    (int) (outputsEndX - 21f), bottomY, 20, 20, progressBarTexture);
            group.addWidget(progress);
            progress.setProgressSupplier(ProgressWidget.JEIProgress);

            buildStepOutput(outputsEndX, bottomY + 22, holder(steps.get(steps.size() - 1)),
                    steps.get(steps.size() - 1));
        }

        private RecipeHolder holder(GTRecipe recipe) {
            @SuppressWarnings("UnstableApiUsage")
            var storages = Tables.newCustomTable(new EnumMap<>(IO.class),
                    LinkedHashMap<RecipeCapability<?>, Object>::new);
            @SuppressWarnings("UnstableApiUsage")
            var extraStorages = Tables.newCustomTable(new EnumMap<>(IO.class),
                    LinkedHashMap<RecipeCapability<?>, List<Content>>::new);
            widget.collectStorage(storages, extraStorages, recipe);
            return new RecipeHolder(ProgressWidget.JEIProgress, storages, recipe.data, recipe.conditions, false,
                    false);
        }

        private void buildStepOutput(float outputsEndX, int posY, RecipeHolder holder, GTRecipe recipe) {
            var slots = new ArrayList<Widget>();
            for (var entry : holder.storages().row(IO.OUT).entrySet()) {
                var cap = entry.getKey();
                if (cap.getWidgetClass() == null) continue;
                var storage = entry.getValue();
                var contents = recipe.getOutputContents(cap);
                for (int index = 0; index < contents.size(); index++) {
                    slots.add(buildSlot(IO.OUT, recipe, holder, cap, storage, index, contents.get(index),
                            index == contents.size() - 1));
                }
                var tickContents = recipe.getTickOutputContents(cap);
                for (int i = 0; i < tickContents.size(); i++) {
                    slots.add(buildSlot(IO.OUT, recipe, holder, cap, storage, i + contents.size(),
                            tickContents.get(i), i == tickContents.size() - 1));
                }
            }
            float slotWidth = 18f;
            float startX = outputsEndX - slots.size() * slotWidth - 2;
            for (int i = 0; i < slots.size(); i++) {
                Widget slot = slots.get(i);
                if (slot == null) continue;
                slot.setSelfPosition((int) (startX + i * slotWidth), posY);
                group.addWidget(slot);
            }
        }

        private int buildStepInput(int posX, int step, RecipeHolder holder, GTRecipe recipe) {
            int slotSize = 18;
            int posY = 2;
            group.addWidget(new Widget(posX, posY, slotSize, 11).setBackground(new ColorRectTexture(0xffa8a8a8)));
            var numeral = new LabelWidget(posX + 2, posY + 2, FormattingUtil.toRomanNumeral(step + 1));
            numeral.setDropShadow(true);
            numeral.setHoverTooltips(stepTooltips(step, recipe));
            group.addWidget(numeral);
            posY += 11 + 2;

            for (var entry : holder.storages().row(IO.IN).entrySet()) {
                var cap = entry.getKey();
                if (cap.getWidgetClass() == null) continue;
                var storage = entry.getValue();
                var contents = recipe.getInputContents(cap);
                for (int index = 0; index < contents.size(); index++) {
                    Widget slot = buildSlot(IO.IN, recipe, holder, cap, storage, index, contents.get(index),
                            index == contents.size() - 1);
                    if (slot == null) continue;
                    slot.setSelfPosition(posX, posY);
                    group.addWidget(slot);
                    posY += slotSize;
                }
                var tickContents = recipe.getTickInputContents(cap);
                for (int i = 0; i < tickContents.size(); i++) {
                    Widget slot = buildSlot(IO.IN, recipe, holder, cap, storage, i + contents.size(),
                            tickContents.get(i), i == tickContents.size() - 1);
                    if (slot == null) continue;
                    slot.setSelfPosition(posX, posY);
                    group.addWidget(slot);
                    posY += slotSize;
                }
            }
            return posY;
        }

        private List<Component> stepTooltips(int step, GTRecipe recipe) {
            var result = new ArrayList<Component>();
            result.add(Component.translatable("af9.recipe.staged.step", Integer.toString(step + 1)));
            result.add(Component.translatable("gtceu.recipe.duration",
                    FormattingUtil.formatNumbers(recipe.duration / 20f)));
            long total = recipe.getInputEUt().getTotalEU() * recipe.duration;
            result.add(Component.translatable("gtceu.recipe.total", FormattingUtil.formatNumbers(total)));
            return result;
        }

        private IGuiTexture overlays(boolean isOutput, RecipeCapability<?> capability, boolean isLast) {
            IGuiTexture base = capability == FluidRecipeCapability.CAP ? GuiTextures.FLUID_SLOT : GuiTextures.SLOT;
            byte key = (byte) ((isOutput ? 2 : 0) + (capability == FluidRecipeCapability.CAP ? 1 : 0) +
                    (isLast ? 4 : 0));
            if (recipeUI.getSlotOverlays().containsKey(key)) {
                return new GuiTextureGroup(base, recipeUI.getSlotOverlays().get(key));
            }
            return base;
        }

        private Widget buildSlot(IO io, GTRecipe recipe, RecipeHolder holder, RecipeCapability<?> cap,
                Object storage, int index, Content input, boolean isLast) {
            Widget slot = cap.createWidget();
            if (slot == null) return null;
            slot.setBackground(overlays(io == IO.OUT, cap, isLast));
            cap.applyWidgetInfo(slot, index, true, io, holder, recipe.getType(), recipe, input, storage, 0, 0);
            slot.setOverlay(input.createOverlay(false, 0, 0, recipe.getType().getChanceFunction()));
            return slot;
        }
    }
}
