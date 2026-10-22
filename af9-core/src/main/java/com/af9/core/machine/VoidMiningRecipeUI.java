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

import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * The Void Miner's recipes in EMI / JEI, one page a recipe: on top the drilling fluid and the circuit it takes and the arrow
 * to the ores, under them a frame of {@link #COLUMNS} x {@link #ROWS} slots with the ores one run of this recipe brings, in
 * its first slots. The page is drawn as one dark panel in the colours of the multiblock consoles, the slots as dark cells
 * of it. GT's own widget adds everything under the frame (duration, energy of a run, draw at the chosen tier with its
 * clickable tier, the dimension icons) after the page's builder has run and in the room the type's {@link #TEXT_LINES}
 * reserve, so none of it is added here: lines of our own sat on top of GT's and cut each other off. GT's slots (same
 * ids) for the recipe, so the viewers' lookups of an ore still find the circuit that makes it.
 */
public class VoidMiningRecipeUI extends GTRecipeTypeUI {

    public static final int WIDTH = 176, GRID_X = 7, GRID_Y = 26, COLUMNS = 9, ROWS = 3, TOP_Y = 4;
    public static final int HEIGHT = GRID_Y + 18 * ROWS + 4;
    /** The lines of text GT puts under the page (duration, total, draw), whose room the page's panel spans too. */
    public static final int TEXT_LINES = 3;
    public static final String BACKDROP_ID = "af9_void_mining_backdrop";

    /** The consoles' colours: panel, its edge, the empty cell, the cell's lit edge, and the amber of the accents. */
    private static final int PANEL = 0xFF111827, EDGE = 0xFF25324A, CELL = 0xFF0B0F17, CELL_EDGE = 0xFF1E293B,
            ACCENT = 0xFFFFC857;

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
        Backdrop backdrop = new Backdrop(getMaxTooltips());
        backdrop.setId(BACKDROP_ID);
        group.addWidget(backdrop);
        slot(group, FluidRecipeCapability.CAP, IO.IN, 0, GRID_X, TOP_Y, overlay(false, FluidRecipeCapability.CAP));
        slot(group, ItemRecipeCapability.CAP, IO.IN, 0, GRID_X + 20, TOP_Y, overlay(false, ItemRecipeCapability.CAP));
        var arrow = new ProgressWidget(ProgressWidget.JEIProgress, GRID_X + 46, TOP_Y, 20, 18,
                getProgressBarTexture());
        arrow.setId("progress");
        group.addWidget(arrow);
        // the recipe's ores are the first slots of the frame
        for (int i = 0; i < type.maxOutputs.getInt(ItemRecipeCapability.CAP); i++) {
            slot(group, ItemRecipeCapability.CAP, IO.OUT, i, GRID_X + 1 + 18 * (i % COLUMNS),
                    GRID_Y + 1 + 18 * (i / COLUMNS), overlay(true, ItemRecipeCapability.CAP));
        }
        return group;
    }

    /** A slot's picture without GT's grey cell, which the backdrop draws dark instead: just the type's overlay. */
    private IGuiTexture overlay(boolean output, RecipeCapability<?> cap) {
        byte key = (byte) ((output ? 2 : 0) + (cap == FluidRecipeCapability.CAP ? 1 : 0));
        IGuiTexture overlay = getSlotOverlays().get(key);
        return overlay == null ? IGuiTexture.EMPTY : overlay;
    }

    private static void slot(WidgetGroup group, RecipeCapability<?> cap, IO io, int index, int x, int y,
                             IGuiTexture background) {
        Widget slot = cap.createWidget();
        slot.setSelfPosition(new Position(x, y));
        slot.setBackground(background);
        slot.setId(cap.slotName(io, index));
        group.addWidget(slot);
    }

    /** The page's panel, with the cells of its slots. It reaches down over the lines GT writes under the page. */
    private static class Backdrop extends Widget {

        private final int pageHeight;

        Backdrop(int textLines) {
            super(0, 0, WIDTH, HEIGHT);
            this.pageHeight = HEIGHT + 5 + 10 * textLines;
        }

        private static void cell(GuiGraphics graphics, int x, int y) {
            graphics.fill(x, y, x + 18, y + 18, CELL_EDGE);
            graphics.fill(x + 1, y + 1, x + 17, y + 17, CELL);
        }

        @Override
        @OnlyIn(Dist.CLIENT)
        public void drawInBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
            int x0 = getPosition().x;
            int y0 = getPosition().y;
            graphics.fill(x0, y0, x0 + WIDTH, y0 + pageHeight, EDGE);
            graphics.fill(x0 + 1, y0 + 1, x0 + WIDTH - 1, y0 + pageHeight - 1, PANEL);
            // amber corner ticks and the rules over the frame and over the text
            for (int[] corner : new int[][] { { 1, 1, 1, 1 }, { WIDTH - 1, 1, -1, 1 },
                    { 1, pageHeight - 1, 1, -1 }, { WIDTH - 1, pageHeight - 1, -1, -1 } }) {
                int cx = x0 + corner[0], cy = y0 + corner[1];
                graphics.fill(Math.min(cx, cx + 6 * corner[2]), cy, Math.max(cx, cx + 6 * corner[2]),
                        cy + corner[3], ACCENT);
                graphics.fill(cx, Math.min(cy, cy + 6 * corner[3]), cx + corner[2],
                        Math.max(cy, cy + 6 * corner[3]), ACCENT);
            }
            graphics.fill(x0 + GRID_X, y0 + GRID_Y - 4, x0 + GRID_X + 18 * COLUMNS, y0 + GRID_Y - 3, EDGE);
            graphics.fill(x0 + GRID_X, y0 + GRID_Y - 4, x0 + GRID_X + 24, y0 + GRID_Y - 3, ACCENT);
            graphics.fill(x0 + GRID_X, y0 + HEIGHT, x0 + GRID_X + 18 * COLUMNS, y0 + HEIGHT + 1, EDGE);
            // the input cells and the frame's
            cell(graphics, x0 + GRID_X, y0 + TOP_Y);
            cell(graphics, x0 + GRID_X + 20, y0 + TOP_Y);
            for (int row = 0; row < ROWS; row++) {
                for (int column = 0; column < COLUMNS; column++) {
                    cell(graphics, x0 + GRID_X + 18 * column, y0 + GRID_Y + 18 * row);
                }
            }
            super.drawInBackground(graphics, mouseX, mouseY, partialTicks);
        }
    }
}
