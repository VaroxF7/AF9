package com.af9.core.compat.xei;

import com.af9.core.AF9Core;
import com.af9.core.machine.StagedAssemblyMachine;
import com.af9.core.staged.StagedRecipes;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.capability.recipe.FluidRecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.capability.recipe.RecipeCapability;
import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.gui.editor.IEditableUI;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.GTRecipeType;
import com.gregtechceu.gtceu.api.recipe.OverclockingLogic;
import com.gregtechceu.gtceu.api.recipe.category.GTRecipeCategory;
import com.gregtechceu.gtceu.api.recipe.content.Content;
import com.gregtechceu.gtceu.api.recipe.ui.GTRecipeTypeUI;
import com.gregtechceu.gtceu.api.recipe.ui.GTRecipeTypeUI.RecipeHolder;
import com.gregtechceu.gtceu.api.registry.GTRegistries;
import com.gregtechceu.gtceu.integration.xei.widgets.GTRecipeWidget;
import com.gregtechceu.gtceu.utils.FormattingUtil;
import com.gregtechceu.gtceu.utils.GTUtil;

import com.lowdragmc.lowdraglib.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.texture.ProgressTexture;
import com.lowdragmc.lowdraglib.gui.widget.ButtonWidget;
import com.lowdragmc.lowdraglib.gui.widget.LabelWidget;
import com.lowdragmc.lowdraglib.gui.widget.ProgressWidget;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import com.google.common.collect.Tables;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;

import static com.lowdragmc.lowdraglib.gui.texture.ProgressTexture.FillDirection.LEFT_TO_RIGHT;

/**
 * The JEI / EMI / REI page of staged recipes, laid out like Star Technology's layered recipes (the {@code
 * LayeredRecipeUIHelper} of its GTCEu fork): one column of inputs per step, headed by its roman numeral (hover for
 * the step's duration and energy), then the arrow (click it for the type's recipe list) and the final outputs. GT's
 * own footer (total duration, total EU, EU/t, tier) is drawn by the widget around it and stays as it is.
 * <p>
 * A click on the footer's voltage tier shows the recipe overclocked, as on every GT page; the steps' durations,
 * energy and chances follow it. GT 7.2.0's recipe widget keeps the chosen tier to itself, so it is read out of its
 * fields; where that fails the steps show their base values.
 * <p>
 * The page fits {@value #MAX_STEPS} steps of {@value #MAX_STEP_INPUTS} inputs; the KubeJS helper refuses more.
 */
public final class StagedRecipeUI {

    private static final int WIDTH = 164;
    /** Star Technology's page is 128 high, for three inputs a step: this one holds {@value #MAX_STEP_INPUTS}. */
    private static final int HEIGHT = 150;
    /** Widest page that still fits the recipe viewer: seven 20 px step columns plus the outputs. */
    public static final int MAX_STEPS = 7;
    /** Widest column: item and fluid inputs of one step stack below its numeral. */
    public static final int MAX_STEP_INPUTS = 5;

    /** The recipe widget's chosen and lowest voltage tier (private there). */
    private static final Field TIER = widgetField("tier");
    private static final Field MIN_TIER = widgetField("minTier");

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

    /** GT calls this when the page opens and again on every click on the voltage tier. */
    private static void build(GTRecipe root, WidgetGroup group) {
        List<GTRecipe> steps = StagedRecipes.getSteps(root);
        if (steps.isEmpty() || steps.size() > MAX_STEPS || !(group instanceof GTRecipeWidget widget)) return;
        var content = (WidgetGroup) widget.getFirstWidgetById(GTRecipeWidget.RECIPE_CONTENT_GROUP_ID_REGEX);
        if (content == null) return;
        content.clearAllWidgets();
        new Builder(widget, content, root, steps).build();
    }

    private static Field widgetField(String name) {
        try {
            Field field = GTRecipeWidget.class.getDeclaredField(name);
            field.setAccessible(true);
            return field;
        } catch (ReflectiveOperationException | RuntimeException e) {
            AF9Core.LOGGER.warn("Staged recipe page: GT's recipe widget has no readable '{}', showing base values",
                    name);
            return null;
        }
    }

    private static int read(Field field, GTRecipeWidget widget, int fallback) {
        if (field == null) return fallback;
        try {
            return field.getInt(widget);
        } catch (ReflectiveOperationException | RuntimeException e) {
            return fallback;
        }
    }

    /**
     * Opens the type's recipe list in the recipe viewer that is there, as the arrow of GT's own pages does. GT's
     * code for it names the viewers' classes; AF9 Core is built without them, so they are reached by name.
     */
    private static void showRecipes(GTRecipeType type) {
        try {
            List<GTRecipeCategory> categories = type.getCategories().stream()
                    .filter(GTRecipeCategory::isXEIVisible).toList();
            if (GTCEu.Mods.isREILoaded()) {
                var toId = Class.forName("com.gregtechceu.gtceu.integration.rei.recipe.GTRecipeREICategory")
                        .getMethod("machineCategory", GTRecipeCategory.class);
                List<Object> ids = new ArrayList<>();
                for (GTRecipeCategory category : categories) ids.add(toId.invoke(null, category));
                Class<?> search = Class.forName("me.shedaniel.rei.api.client.view.ViewSearchBuilder");
                Object builder = search.getMethod("builder").invoke(null);
                search.getMethod("addCategories", Collection.class).invoke(builder, ids);
                search.getMethod("open").invoke(builder);
            } else if (GTCEu.Mods.isJEILoaded()) {
                var toType = Class.forName("com.gregtechceu.gtceu.integration.jei.recipe.GTRecipeJEICategory")
                        .getMethod("machineType", GTRecipeCategory.class);
                List<Object> types = new ArrayList<>();
                for (GTRecipeCategory category : categories) types.add(toType.invoke(null, category));
                Object runtime = Class.forName("com.lowdragmc.lowdraglib.jei.JEIPlugin").getField("jeiRuntime")
                        .get(null);
                if (runtime == null) return;
                Object recipes = Class.forName("mezz.jei.api.runtime.IJeiRuntime").getMethod("getRecipesGui")
                        .invoke(runtime);
                Class.forName("mezz.jei.api.runtime.IRecipesGui").getMethod("showTypes", List.class)
                        .invoke(recipes, types);
            } else if (GTCEu.Mods.isEMILoaded()) {
                Object category = Class.forName("com.gregtechceu.gtceu.integration.emi.recipe.GTRecipeEMICategory")
                        .getMethod("machineCategory", GTRecipeCategory.class).invoke(null, type.getCategory());
                Class.forName("dev.emi.emi.api.EmiApi")
                        .getMethod("displayRecipeCategory", Class.forName("dev.emi.emi.api.recipe.EmiRecipeCategory"))
                        .invoke(null, category);
            }
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            AF9Core.LOGGER.debug("Staged recipe page: cannot open the recipe list", e);
        }
    }

    private static final class Builder {

        private final GTRecipeWidget widget;
        private final WidgetGroup group;
        private final GTRecipe root;
        private final List<GTRecipe> steps;
        private final GTRecipeTypeUI recipeUI;
        /** The tier the page shows the recipe at, and the recipe's own. */
        private final int tier;
        private final int minTier;

        Builder(GTRecipeWidget widget, WidgetGroup group, GTRecipe root, List<GTRecipe> steps) {
            this.widget = widget;
            this.group = group;
            this.root = root;
            this.steps = steps;
            this.recipeUI = root.recipeType.getRecipeUI();
            this.minTier = read(MIN_TIER, widget, 0);
            this.tier = Math.max(minTier, read(TIER, widget, minTier));
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
            if (GTCEu.Mods.isREILoaded() || GTCEu.Mods.isJEILoaded() || GTCEu.Mods.isEMILoaded()) {
                group.addWidget(new ButtonWidget(progress.getPosition().x, progress.getPosition().y,
                        progress.getSize().width, progress.getSize().height, IGuiTexture.EMPTY, click -> {
                            if (click.isRemote) showRecipes(root.getType());
                        }).setHoverTooltips("gtceu.recipe_type.show_recipes"));
            }

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
            var numeral = new LabelWidget(posX + 2, posY + 2,
                    Component.literal(FormattingUtil.toRomanNumeral(step + 1)));
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

        /** The step's number, duration and energy at the tier the page shows (a shift-click's overclocks are perfect). */
        private List<Component> stepTooltips(int step, GTRecipe recipe) {
            var eut = recipe.getInputEUt();
            int duration = recipe.duration;
            if (tier > minTier && !eut.isEmpty()) {
                int overclocks = tier - minTier;
                if (minTier == GTValues.ULV) overclocks--;
                OverclockingLogic logic = GTUtil.isShiftDown() ? OverclockingLogic.PERFECT_OVERCLOCK :
                        OverclockingLogic.NON_PERFECT_OVERCLOCK;
                var result = logic.runOverclockingLogic(
                        new OverclockingLogic.OCParams(eut.voltage(), recipe.duration, overclocks, 1),
                        GTValues.V[tier]);
                duration = (int) (duration * result.durationMultiplier());
                eut = eut.multiplyVoltage(result.eutMultiplier());
            }
            var result = new ArrayList<Component>();
            result.add(Component.translatable("af9.recipe.staged.step", Integer.toString(step + 1)));
            result.add(Component.translatable("gtceu.recipe.duration", FormattingUtil.formatNumbers(duration / 20f)));
            result.add(Component.translatable("gtceu.recipe.total",
                    FormattingUtil.formatNumbers(eut.getTotalEU() * duration)));
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
            cap.applyWidgetInfo(slot, index, true, io, holder, recipe.getType(), recipe, input, storage, minTier,
                    tier);
            slot.setOverlay(input.createOverlay(false, minTier, tier, recipe.getType().getChanceFunction()));
            return slot;
        }
    }
}
