package com.af9.core.machine;

import com.af9.core.litho.Coolant;

import com.gregtechceu.gtceu.api.capability.recipe.FluidRecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.capability.recipe.ItemRecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.RecipeCapability;
import com.gregtechceu.gtceu.api.gui.WidgetUtils;
import com.gregtechceu.gtceu.api.gui.editor.IEditableUI;
import com.gregtechceu.gtceu.api.gui.widget.TankWidget;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.GTRecipeType;
import com.gregtechceu.gtceu.api.recipe.content.Content;
import com.gregtechceu.gtceu.api.recipe.ui.GTRecipeTypeUI;

import com.lowdragmc.lowdraglib.gui.texture.ColorBorderTexture;
import com.lowdragmc.lowdraglib.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib.gui.widget.ProgressWidget;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.utils.Position;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraftforge.fluids.FluidStack;

import java.lang.reflect.Field;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * The Particle Accelerator's recipes in EMI / JEI, in the lithography page's style
 * ({@link com.af9.core.litho.LithoRecipeUI}): the items top left and, apart from them, the coolant bottom left, marked
 * as coolant (its slot in ice, "COOLANT" over it, its own cryo line, a hover text saying what it is), both piped
 * through a manifold into the ring's west gate; the ring drawn from above in the middle (beam pipe, four gates, the machine in
 * the middle) with the mode's particles running round it; then GT's arrow and the outputs stacked
 * ({@link AcceleratorFlowWidget}). GT's own slots (same ids), so EMI shows and looks them up as usual.
 */
public class AcceleratorRecipeUI extends GTRecipeTypeUI {

    public static final int WIDTH = 176, HEIGHT = 66;
    /** Layout: items, coolant, manifold, ring (centre x, radius), arrow, outputs. */
    public static final int ITEMS_X = 4, ITEMS_Y = 4, COOLANT_X = 4, COOLANT_Y = 44, MANIFOLD_X = 50;
    public static final int RING_X = 86, RING_R = 22, ARROW_X = 120, OUT_X = 150;
    public static final int CENTER_Y = HEIGHT / 2;
    public static final String FLOW_ID = "af9_accelerator_flow";

    private final GTRecipeType type;
    private final int mode;

    public AcceleratorRecipeUI(GTRecipeType type, int mode) {
        super(type);
        this.type = type;
        this.mode = mode;
    }

    /** Gives the recipe type this page, keeping what its KubeJS definition set on GT's. */
    public static void install(GTRecipeType type, int mode) {
        GTRecipeTypeUI old = type.getRecipeUI();
        AcceleratorRecipeUI ui = new AcceleratorRecipeUI(type, mode);
        ui.setSlotOverlays(old.getSlotOverlays());
        ui.setProgressBarTexture(old.getProgressBarTexture());
        ui.setMaxTooltips(old.getMaxTooltips());
        type.setRecipeUI(ui);
    }

    /** Our layout; GT's binding of the recipe to the slots (by their ids) and the arrow. */
    @Override
    public IEditableUI<WidgetGroup, RecipeHolder> createEditableUITemplate(boolean isSteam, boolean isHighPressure) {
        IEditableUI<WidgetGroup, RecipeHolder> gt = super.createEditableUITemplate(isSteam, isHighPressure);
        return new IEditableUI.Normal<>(this::layout, gt::setupUI);
    }

    private WidgetGroup layout() {
        int items = Math.min(2, type.maxInputs.getInt(ItemRecipeCapability.CAP));
        int fluids = Math.min(1, type.maxInputs.getInt(FluidRecipeCapability.CAP));
        int outputs = Math.min(2, type.maxOutputs.getInt(ItemRecipeCapability.CAP));
        WidgetGroup group = new WidgetGroup(0, 0, WIDTH, HEIGHT);
        // the pipes and the ring first: the slots draw over them
        AcceleratorFlowWidget flow = new AcceleratorFlowWidget(mode, items, fluids);
        flow.setId(FLOW_ID);
        group.addWidget(flow);
        for (int i = 0; i < items; i++) {
            slot(group, ItemRecipeCapability.CAP, IO.IN, i, items, ITEMS_X + 18 * i, ITEMS_Y);
        }
        for (int i = 0; i < fluids; i++) {
            // the coolant, not a fluid like the others: its slot in ice on dark frost
            Widget tank = FluidRecipeCapability.CAP.createWidget();
            tank.setSelfPosition(new Position(COOLANT_X, COOLANT_Y));
            tank.setBackground(new GuiTextureGroup(new ColorRectTexture(0xFF0B2530),
                    new ColorBorderTexture(1, AcceleratorFlowWidget.ICE)));
            tank.setId(FluidRecipeCapability.CAP.slotName(IO.IN, i));
            group.addWidget(tank);
        }
        for (int i = 0; i < outputs; i++) {
            slot(group, ItemRecipeCapability.CAP, IO.OUT, i, outputs, OUT_X,
                    CENTER_Y - 9 - (outputs - 1) * 10 + 20 * i);
        }
        var arrow = new ProgressWidget(ProgressWidget.JEIProgress, ARROW_X, CENTER_Y - 10, 20, 20,
                getProgressBarTexture());
        arrow.setId("progress");
        group.addWidget(arrow);
        // the ring's tooltip: the machine and the mode
        var ring = new Widget(RING_X - RING_R - 3, CENTER_Y - RING_R - 3, 2 * RING_R + 6, 2 * RING_R + 6);
        ring.setHoverTooltips(Component.translatable("block.gtceu.particle_accelerator"),
                Component.translatable(type.registryName.getNamespace() + "." + type.registryName.getPath()));
        group.addWidget(ring);
        return group;
    }

    private void slot(WidgetGroup group, RecipeCapability<?> cap, IO io, int index, int count, int x, int y) {
        Widget slot = cap.createWidget();
        slot.setSelfPosition(new Position(x, y));
        slot.setBackground(getOverlaysForSlot(io == IO.OUT, cap, index == count - 1, false, false));
        slot.setId(cap.slotName(io, index));
        group.addWidget(slot);
    }

    /**
     * The recipe to the pipes; the coolant's hover text says what it is (after GT's own lines for the slot); then the
     * type's own page builder.
     */
    @Override
    public void appendJEIUI(GTRecipe recipe, WidgetGroup widgetGroup) {
        WidgetUtils.widgetByIdForEach(widgetGroup, "^" + FLOW_ID + "$", AcceleratorFlowWidget.class,
                flow -> flow.setRecipe(recipe));
        Coolant least = leastCoolant(recipe);
        WidgetUtils.widgetByIdForEach(widgetGroup, "^" + FluidRecipeCapability.CAP.slotName(IO.IN) + "_[0-9]+$",
                TankWidget.class, tank -> markCoolant(tank, least));
        super.appendJEIUI(recipe, widgetGroup);
    }

    /** The weakest coolant the recipe's fluid takes (its grade: the tag holds it and the colder ones), or null. */
    private static Coolant leastCoolant(GTRecipe recipe) {
        Coolant least = null;
        for (Content content : recipe.inputs.getOrDefault(FluidRecipeCapability.CAP, List.of())) {
            for (FluidStack stack : FluidRecipeCapability.CAP.of(content.content).getStacks()) {
                Coolant coolant = Coolant.of(stack);
                if (coolant != null && (least == null || coolant.ordinal() < least.ordinal())) least = coolant;
            }
        }
        return least;
    }

    /** Adds the coolant's lines to the tank's tooltip. */
    private static void markCoolant(TankWidget tank, Coolant least) {
        addTooltips(tank, tooltips -> {
            tooltips.add(Component.literal("\u2744 ").append(
                    Component.translatable("af9.recipe.accelerator_page.coolant"))
                    .withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD));
            if (least != null) {
                tooltips.add(Component.translatable("af9.recipe.accelerator_page.coolant_grade",
                        Component.translatable("af9.litho.coolant." + least.id)).withStyle(ChatFormatting.AQUA));
            }
            tooltips.add(Component.translatable("af9.recipe.accelerator_page.coolant_hatch")
                    .withStyle(ChatFormatting.GRAY));
            tooltips.add(Component.translatable("af9.recipe.accelerator_page.coolant_magnets")
                    .withStyle(ChatFormatting.DARK_AQUA));
        });
    }

    /**
     * Adds lines to a recipe page tank's tooltip, after the ones GT gave it (its chance and such): the pages mark their
     * coolant with it.
     */
    @SuppressWarnings("unchecked")
    public static void addTooltips(TankWidget tank, Consumer<List<Component>> lines) {
        BiConsumer<TankWidget, List<Component>> gt = null;
        try {
            Field field = TankWidget.class.getDeclaredField("onAddedTooltips");
            field.setAccessible(true);
            gt = (BiConsumer<TankWidget, List<Component>>) field.get(tank);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            // no GT lines to keep
        }
        BiConsumer<TankWidget, List<Component>> before = gt;
        tank.setOnAddedTooltips((widget, tooltips) -> {
            if (before != null) before.accept(widget, tooltips);
            lines.accept(tooltips);
        });
    }
}
