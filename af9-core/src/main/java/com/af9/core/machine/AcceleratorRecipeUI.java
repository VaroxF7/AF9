package com.af9.core.machine;

import com.gregtechceu.gtceu.api.capability.recipe.FluidRecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.capability.recipe.ItemRecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.RecipeCapability;
import com.gregtechceu.gtceu.api.gui.WidgetUtils;
import com.gregtechceu.gtceu.api.gui.editor.IEditableUI;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.GTRecipeType;
import com.gregtechceu.gtceu.api.recipe.ui.GTRecipeTypeUI;

import com.lowdragmc.lowdraglib.gui.widget.ProgressWidget;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.utils.Position;

import net.minecraft.network.chat.Component;

/**
 * The Particle Accelerator's recipes in EMI / JEI, in the lithography page's style
 * ({@link com.af9.core.litho.LithoRecipeUI}): the items top left and the coolant tank bottom left, both piped through a
 * manifold into the ring's west gate; the ring drawn from above in the middle (beam pipe, four gates, the machine in
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
            slot(group, FluidRecipeCapability.CAP, IO.IN, i, fluids, COOLANT_X, COOLANT_Y);
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

    /** The recipe to the pipes (its coolant's colour), then the type's own page builder. */
    @Override
    public void appendJEIUI(GTRecipe recipe, WidgetGroup widgetGroup) {
        WidgetUtils.widgetByIdForEach(widgetGroup, "^" + FLOW_ID + "$", AcceleratorFlowWidget.class,
                flow -> flow.setRecipe(recipe));
        super.appendJEIUI(recipe, widgetGroup);
    }
}
