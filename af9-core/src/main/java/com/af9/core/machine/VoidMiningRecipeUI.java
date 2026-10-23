package com.af9.core.machine;

import com.gregtechceu.gtceu.api.capability.recipe.FluidRecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.capability.recipe.ItemRecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.RecipeCapability;
import com.gregtechceu.gtceu.api.gui.editor.IEditableUI;
import com.gregtechceu.gtceu.api.recipe.GTRecipeType;
import com.gregtechceu.gtceu.api.recipe.ui.GTRecipeTypeUI;

import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.widget.ProgressWidget;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.utils.Position;

/**
 * The Void Miner's recipes in EMI / JEI, one page a recipe, in GT's own look (no panel, GT's slot pictures): on top the
 * drilling fluid and the circuit it takes, side by side and centred, under them the arrow pointing down, and under that
 * a frame of {@link #COLUMNS} x {@link #ROWS} slots with the ores one run of this recipe brings, in its first slots.
 * GT's own widget adds everything under the frame (duration, energy of a run, draw at the chosen tier with its
 * clickable tier, the dimension icons) after the page's builder has run and in the room the type's {@link #TEXT_LINES}
 * reserve, so none of it is added here. GT's slots (same ids) for the recipe, so the viewers' lookups of an ore still
 * find the circuit that makes it.
 */
public class VoidMiningRecipeUI extends GTRecipeTypeUI {

    public static final int WIDTH = 176, COLUMNS = 5, ROWS = 3, TOP_Y = 4, ARROW_Y = TOP_Y + 20, GRID_Y = ARROW_Y + 24;
    public static final int GRID_X = (WIDTH - 18 * COLUMNS) / 2;
    public static final int HEIGHT = GRID_Y + 18 * ROWS + 4;
    /** The lines of text GT puts under the page (duration, total, draw). */
    public static final int TEXT_LINES = 3;

    private final GTRecipeType type;

    public VoidMiningRecipeUI(GTRecipeType type) {
        super(type);
        this.type = type;
    }

    /** Gives the recipe type this page, keeping what its KubeJS definition set on GT's. */
    public static void install(GTRecipeType type) {
        GTRecipeTypeUI old = type.getRecipeUI();
        VoidMiningRecipeUI ui = new VoidMiningRecipeUI(type);
        ui.setSlotOverlays(old.getSlotOverlays());
        ui.setProgressBarTexture(old.getProgressBarTexture());
        ui.setMaxTooltips(Math.max(TEXT_LINES, old.getMaxTooltips()));
        type.setRecipeUI(ui);
    }

    @Override
    public IEditableUI<WidgetGroup, RecipeHolder> createEditableUITemplate(boolean isSteam, boolean isHighPressure) {
        IEditableUI<WidgetGroup, RecipeHolder> gt = super.createEditableUITemplate(isSteam, isHighPressure);
        return new IEditableUI.Normal<>(this::layout, gt::setupUI);
    }

    private WidgetGroup layout() {
        WidgetGroup group = new WidgetGroup(0, 0, WIDTH, HEIGHT);
        // the drilling fluid and the circuit, centred over the arrow
        int inputsX = WIDTH / 2 - 19;
        slot(group, FluidRecipeCapability.CAP, IO.IN, 0, inputsX, TOP_Y,
                getOverlaysForSlot(false, FluidRecipeCapability.CAP, false, false, false));
        slot(group, ItemRecipeCapability.CAP, IO.IN, 0, inputsX + 20, TOP_Y,
                getOverlaysForSlot(false, ItemRecipeCapability.CAP, true, false, false));
        var arrow = new ProgressWidget(ProgressWidget.JEIProgress, WIDTH / 2 - 6, ARROW_Y, 12, 22,
                DownArrow.texture());
        arrow.setId("progress");
        group.addWidget(arrow);
        // the recipe's ores are the first slots of the frame
        int slots = Math.max(COLUMNS * ROWS, type.maxOutputs.getInt(ItemRecipeCapability.CAP));
        for (int i = 0; i < slots; i++) {
            slot(group, ItemRecipeCapability.CAP, IO.OUT, i, GRID_X + 18 * (i % COLUMNS),
                    GRID_Y + 18 * (i / COLUMNS), getOverlaysForSlot(true, ItemRecipeCapability.CAP, false, false, false));
        }
        return group;
    }

    private static void slot(WidgetGroup group, RecipeCapability<?> cap, IO io, int index, int x, int y,
                             IGuiTexture background) {
        Widget slot = cap.createWidget();
        slot.setSelfPosition(new Position(x, y));
        slot.setBackground(background);
        slot.setId(cap.slotName(io, index));
        group.addWidget(slot);
    }
}
