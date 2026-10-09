package com.af9.core.machine;

import com.af9.core.staged.StagedRecipes;

import com.gregtechceu.gtceu.api.capability.recipe.FluidRecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.capability.recipe.ItemRecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.RecipeCapability;
import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.gui.editor.IEditableUI;
import com.gregtechceu.gtceu.api.gui.widget.SlotWidget;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.GTRecipeType;
import com.gregtechceu.gtceu.api.recipe.content.Content;
import com.gregtechceu.gtceu.api.recipe.ui.GTRecipeTypeUI;
import com.gregtechceu.gtceu.common.item.IntCircuitBehaviour;

import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.widget.LabelWidget;
import com.lowdragmc.lowdraglib.gui.widget.ProgressWidget;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.jei.IngredientIO;
import com.lowdragmc.lowdraglib.utils.Position;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.items.ItemStackHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * The Void Miner's recipes in EMI / JEI, still and with room: on top the recipe (the programmed circuit and the drilling
 * fluid it takes, the arrow, the four ore slots), under it a chart of every circuit of the same area with its ores, two
 * circuits a row, so a page says which circuit to set for what, not only what this one gives. GT's own slots (same ids)
 * for the recipe, so the viewers' lookups of an ore still find the circuit that makes it.
 */
public class VoidMiningRecipeUI extends GTRecipeTypeUI {

    public static final int WIDTH = 176, HEAD = 52, ROW = 18, COLUMNS = 2, CELL = 86;
    public static final String BACKDROP_ID = "af9_void_mining_backdrop";

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
        ui.setMaxTooltips(old.getMaxTooltips());
        type.setRecipeUI(ui);
    }

    /** The recipes of the area by circuit, as many as the area has (the page is as high as its chart). */
    private Map<Integer, GTRecipe> chart() {
        Map<Integer, GTRecipe> chart = new TreeMap<>();
        for (GTRecipe recipe : StagedRecipes.allRecipes(type)) {
            int circuit = circuitOf(recipe);
            if (circuit >= 0) chart.putIfAbsent(circuit, recipe);
        }
        return chart;
    }

    private static int circuitOf(GTRecipe recipe) {
        for (Content content : recipe.inputs.getOrDefault(ItemRecipeCapability.CAP, List.of())) {
            for (ItemStack stack : ItemRecipeCapability.CAP.of(content.content).getItems()) {
                if (IntCircuitBehaviour.isIntegratedCircuit(stack)) return IntCircuitBehaviour.getCircuitConfiguration(stack);
            }
        }
        return -1;
    }

    private int height() {
        int entries = Math.max(chart().size(), 8);
        return HEAD + 14 + ROW * ((entries + COLUMNS - 1) / COLUMNS) + 4;
    }

    @Override
    public IEditableUI<WidgetGroup, RecipeHolder> createEditableUITemplate(boolean isSteam, boolean isHighPressure) {
        IEditableUI<WidgetGroup, RecipeHolder> gt = super.createEditableUITemplate(isSteam, isHighPressure);
        return new IEditableUI.Normal<>(this::layout, gt::setupUI);
    }

    private WidgetGroup layout() {
        WidgetGroup group = new WidgetGroup(0, 0, WIDTH, height());
        Backdrop backdrop = new Backdrop(height());
        backdrop.setId(BACKDROP_ID);
        group.addWidget(backdrop);
        slot(group, ItemRecipeCapability.CAP, IO.IN, 0, 8, 6,
                getOverlaysForSlot(false, ItemRecipeCapability.CAP, true, false, false));
        slot(group, FluidRecipeCapability.CAP, IO.IN, 0, 34, 6,
                getOverlaysForSlot(false, FluidRecipeCapability.CAP, false, false, false));
        var arrow = new ProgressWidget(ProgressWidget.JEIProgress, 62, 6, 20, 20, getProgressBarTexture());
        arrow.setId("progress");
        group.addWidget(arrow);
        for (int i = 0; i < 4; i++) {
            slot(group, ItemRecipeCapability.CAP, IO.OUT, i, 92 + 18 * i, 6,
                    getOverlaysForSlot(true, ItemRecipeCapability.CAP, true, false, false));
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

    /** The chart of the area under the recipe. */
    @Override
    public void appendJEIUI(GTRecipe recipe, WidgetGroup widgetGroup) {
        super.appendJEIUI(recipe, widgetGroup);
        if (FMLEnvironment.dist != Dist.CLIENT) return;
        Widget backdrop = findBackdrop(widgetGroup);
        if (backdrop == null || backdrop.getParent() == null) return;
        WidgetGroup page = backdrop.getParent();
        int own = circuitOf(recipe);
        int index = 0;
        for (var entry : chart().entrySet()) {
            int x = 6 + CELL * (index % COLUMNS);
            int y = HEAD + 14 + ROW * (index / COLUMNS);
            boolean mine = entry.getKey() == own;
            page.addWidget(new LabelWidget(x, y + 5, (mine ? ChatFormatting.WHITE : ChatFormatting.GRAY) +
                    Integer.toString(entry.getKey())));
            List<ItemStack> ores = new ArrayList<>();
            for (Content content : entry.getValue().outputs.getOrDefault(ItemRecipeCapability.CAP, List.of())) {
                ItemStack[] stacks = ItemRecipeCapability.CAP.of(content.content).getItems();
                if (stacks.length > 0 && !stacks[0].isEmpty()) ores.add(stacks[0].copy());
            }
            ItemStackHandler handler = new ItemStackHandler(Math.max(1, ores.size()));
            for (int i = 0; i < ores.size(); i++) handler.setStackInSlot(i, ores.get(i));
            for (int i = 0; i < ores.size(); i++) {
                SlotWidget slot = new SlotWidget(handler, i, x + 12 + 18 * i, y, false, false);
                slot.setBackgroundTexture(GuiTextures.SLOT);
                slot.setIngredientIO(IngredientIO.OUTPUT);
                final int circuit = entry.getKey();
                slot.setOnAddedTooltips((widget, tooltips) -> tooltips.add(
                        Component.translatable("af9.recipe.voidminer.chart_tooltip", circuit)
                                .withStyle(ChatFormatting.AQUA)));
                page.addWidget(slot);
            }
            index++;
        }
    }

    private static Widget findBackdrop(WidgetGroup group) {
        for (Widget widget : group.getWidgetsByType(Widget.class)) {
            if (BACKDROP_ID.equals(widget.getId())) return widget;
        }
        return null;
    }

    /** The names under the inputs and the chart's panel with its heading. */
    private static class Backdrop extends Widget {

        Backdrop(int height) {
            super(0, 0, WIDTH, height);
        }

        @Override
        @OnlyIn(Dist.CLIENT)
        public void drawInBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
            int x0 = getPosition().x;
            int y0 = getPosition().y;
            drawSmall(graphics, Component.translatable("af9.recipe.voidminer.circuit").getString(), x0 + 4, y0 + 26,
                    0xFFB8C2D6);
            drawSmall(graphics, Component.translatable("af9.recipe.voidminer.fluid").getString(), x0 + 32, y0 + 26,
                    0xFFB8C2D6);
            drawSmall(graphics, Component.translatable("af9.recipe.voidminer.ores").getString(), x0 + 92, y0 + 26,
                    0xFFB8C2D6);
            int top = y0 + HEAD - 2;
            graphics.fill(x0 + 2, top, x0 + WIDTH - 2, y0 + getSize().height - 2, 0xFF070A11);
            graphics.fill(x0 + 2, top, x0 + WIDTH - 2, top + 1, 0xFF25324A);
            graphics.fill(x0 + 2, y0 + getSize().height - 3, x0 + WIDTH - 2, y0 + getSize().height - 2, 0xFF25324A);
            graphics.fill(x0 + 2, top, x0 + 3, y0 + getSize().height - 2, 0xFF25324A);
            graphics.fill(x0 + WIDTH - 3, top, x0 + WIDTH - 2, y0 + getSize().height - 2, 0xFF25324A);
            drawSmall(graphics, Component.translatable("af9.recipe.voidminer.chart").getString(), x0 + 6, top + 4,
                    0xFFFFFFFF);
            super.drawInBackground(graphics, mouseX, mouseY, partialTicks);
        }

        @OnlyIn(Dist.CLIENT)
        private static void drawSmall(GuiGraphics graphics, String text, int x, int y, int color) {
            Font font = Minecraft.getInstance().font;
            graphics.pose().pushPose();
            graphics.pose().translate(x, y, 0);
            graphics.pose().scale(0.75F, 0.75F, 1F);
            graphics.drawString(font, text, 0, 0, color, true);
            graphics.pose().popPose();
        }
    }
}
