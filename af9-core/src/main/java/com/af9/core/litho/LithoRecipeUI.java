package com.af9.core.litho;

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
 * The lithography recipes' page in EMI / JEI, laid out like an assembly line: the items on top (blank wafer, reticle,
 * light source or resist cartridge) and the track chemicals below in rows of four, every row piped into a manifold that
 * feeds the machine (its controller, drawn large in a frame of the node's colour, the node above it and the machine
 * below), then GT's arrow and the outputs stacked (the printed wafers, the chanced broken wafer). The pipes carry
 * dashes flowing to the machine, each fluid row's in the colours of its fluids ({@link LithoFlowWidget}).
 * <p>
 * The slots are GT's own (same ids, so GT binds the recipe to them and EMI shows, looks up and moves them as usual);
 * only the template around them is ours. Replaces the recipe types' UI in common setup
 * ({@link #install}), keeping their slot overlays, progress bar and tooltip count.
 */
public class LithoRecipeUI extends GTRecipeTypeUI {

    public static final int WIDTH = 176, HEIGHT = 66;
    /** Layout: slot columns and rows, the manifold, the machine frame, the arrow, the outputs. */
    public static final int SLOTS_X = 4, ITEMS_Y = 4, FLUIDS_Y = 26, PER_ROW = 4, FLUID_ROWS = 2;
    public static final int MANIFOLD_X = 80, BOX_X = 88, BOX_SIZE = 34, ARROW_X = 126, OUT_X = 152;
    public static final int CENTER_Y = HEIGHT / 2;
    public static final String FLOW_ID = "af9_litho_flow";

    private final GTRecipeType type;
    private final LithoMode mode;

    public LithoRecipeUI(GTRecipeType type, LithoMode mode) {
        super(type);
        this.type = type;
        this.mode = mode;
    }

    /** Gives the recipe type this page, keeping what its KubeJS definition set on GT's. */
    public static void install(GTRecipeType type, LithoMode mode) {
        GTRecipeTypeUI old = type.getRecipeUI();
        LithoRecipeUI ui = new LithoRecipeUI(type, mode);
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
        int items = Math.min(PER_ROW, type.maxInputs.getInt(ItemRecipeCapability.CAP));
        int fluids = Math.min(PER_ROW * FLUID_ROWS, type.maxInputs.getInt(FluidRecipeCapability.CAP));
        int outputs = Math.min(2, type.maxOutputs.getInt(ItemRecipeCapability.CAP));
        WidgetGroup group = new WidgetGroup(0, 0, WIDTH, HEIGHT);
        // the pipes and the machine first: the slots draw over them
        LithoFlowWidget flow = new LithoFlowWidget(mode, items, fluids);
        flow.setId(FLOW_ID);
        group.addWidget(flow);
        for (int i = 0; i < items; i++) {
            slot(group, ItemRecipeCapability.CAP, IO.IN, i, items, SLOTS_X + 18 * i, ITEMS_Y);
        }
        for (int i = 0; i < fluids; i++) {
            slot(group, FluidRecipeCapability.CAP, IO.IN, i, fluids, SLOTS_X + 18 * (i % PER_ROW),
                    FLUIDS_Y + 18 * (i / PER_ROW));
        }
        for (int i = 0; i < outputs; i++) {
            slot(group, ItemRecipeCapability.CAP, IO.OUT, i, outputs, OUT_X,
                    CENTER_Y - 9 - (outputs - 1) * 10 + 20 * i);
        }
        var arrow = new ProgressWidget(ProgressWidget.JEIProgress, ARROW_X, CENTER_Y - 10, 20, 20,
                getProgressBarTexture());
        arrow.setId("progress");
        group.addWidget(arrow);
        // the machine frame's tooltip: which machine prints this node
        var machine = new Widget(BOX_X, CENTER_Y - BOX_SIZE / 2, BOX_SIZE, BOX_SIZE);
        machine.setHoverTooltips(Component.translatable("block.gtceu." + LithoFlowWidget.machineId(mode)),
                Component.translatable("af9.recipe.litho_page.node", mode.nodeNm,
                        Component.translatable("af9.litho.light." + mode.light)));
        group.addWidget(machine);
        return group;
    }

    private void slot(WidgetGroup group, RecipeCapability<?> cap, IO io, int index, int count, int x, int y) {
        Widget slot = cap.createWidget();
        slot.setSelfPosition(new Position(x, y));
        slot.setBackground(getOverlaysForSlot(io == IO.OUT, cap, index == count - 1, false, false));
        slot.setId(cap.slotName(io, index));
        group.addWidget(slot);
    }

    /** The recipe to the pipes (its fluids' colours), then the type's own page builder. */
    @Override
    public void appendJEIUI(GTRecipe recipe, WidgetGroup widgetGroup) {
        WidgetUtils.widgetByIdForEach(widgetGroup, "^" + FLOW_ID + "$", LithoFlowWidget.class,
                flow -> flow.setRecipe(recipe));
        super.appendJEIUI(recipe, widgetGroup);
    }
}
