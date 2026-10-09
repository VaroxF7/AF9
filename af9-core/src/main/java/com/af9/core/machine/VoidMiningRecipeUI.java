package com.af9.core.machine;

import com.gregtechceu.gtceu.api.capability.recipe.FluidRecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.capability.recipe.ItemRecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.RecipeCapability;
import com.gregtechceu.gtceu.api.data.DimensionMarker;
import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.gui.editor.IEditableUI;
import com.gregtechceu.gtceu.api.gui.widget.SlotWidget;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.GTRecipeType;
import com.gregtechceu.gtceu.api.recipe.ui.GTRecipeTypeUI;
import com.gregtechceu.gtceu.api.registry.GTRegistries;
import com.gregtechceu.gtceu.api.transfer.item.CustomItemStackHandler;

import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.loading.FMLEnvironment;

/**
 * The Void Miner's recipes in EMI / JEI, one page a recipe, still: on top the circuit and the drilling fluid it takes, the arrow
 * and the dimension markers of the area it mines in (the cube planets, {@link com.af9.core.registry.AF9DimensionMarkers}); under
 * them a frame of {@link #COLUMNS} x {@link #ROWS} slots with the ores one run of this recipe brings, in its first slots. GT's
 * own slots (same ids) for the recipe, so the viewers' lookups of an ore still find the circuit that makes it.
 */
public class VoidMiningRecipeUI extends GTRecipeTypeUI {

    public static final int WIDTH = 176, GRID_X = 52, GRID_Y = 30, COLUMNS = 4, ROWS = 2;
    public static final int BOTTOM_Y = GRID_Y + 18 * ROWS + 6;
    public static final int HEIGHT = BOTTOM_Y + 18 + 6;
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

    @Override
    public IEditableUI<WidgetGroup, RecipeHolder> createEditableUITemplate(boolean isSteam, boolean isHighPressure) {
        IEditableUI<WidgetGroup, RecipeHolder> gt = super.createEditableUITemplate(isSteam, isHighPressure);
        return new IEditableUI.Normal<>(this::layout, gt::setupUI);
    }

    private WidgetGroup layout() {
        WidgetGroup group = new WidgetGroup(0, 0, WIDTH, HEIGHT);
        Backdrop backdrop = new Backdrop();
        backdrop.setId(BACKDROP_ID);
        group.addWidget(backdrop);
        slot(group, ItemRecipeCapability.CAP, IO.IN, 0, 58, BOTTOM_Y,
                getOverlaysForSlot(false, ItemRecipeCapability.CAP, true, false, false));
        slot(group, FluidRecipeCapability.CAP, IO.IN, 0, 80, BOTTOM_Y,
                getOverlaysForSlot(false, FluidRecipeCapability.CAP, false, false, false));
        var arrow = new ProgressWidget(ProgressWidget.JEIProgress, 106, BOTTOM_Y - 1, 20, 20, getProgressBarTexture());
        arrow.setId("progress");
        group.addWidget(arrow);
        // the recipe's ores are the first slots of the frame
        for (int i = 0; i < 4; i++) {
            slot(group, ItemRecipeCapability.CAP, IO.OUT, i, GRID_X + 1 + 18 * i, GRID_Y + 1,
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

    /** The dimension markers of the recipe's area, in the corner above the frame. */
    @Override
    public void appendJEIUI(GTRecipe recipe, WidgetGroup widgetGroup) {
        super.appendJEIUI(recipe, widgetGroup);
        if (FMLEnvironment.dist != Dist.CLIENT) return;
        // GT's own dimension markers of the recipe's conditions sit in the corner of the page, one on the other: the page's
        // own markers (above) say it, so GT's go (the slots of the recipe itself are in the page's group, not here)
        for (Widget widget : new java.util.ArrayList<>(widgetGroup.widgets)) {
            if (widget instanceof SlotWidget) widgetGroup.removeWidget(widget);
        }
        Widget backdrop = findBackdrop(widgetGroup);
        if (backdrop == null || backdrop.getParent() == null) return;
        int area = VoidMinerMachine.areaOf(type);
        if (area < 0) return;
        String[] dimensions = VoidMinerMachine.AREA_DIMENSIONS[area];
        int x = (WIDTH - 20 * dimensions.length + 2) / 2;
        for (String dimension : dimensions) {
            DimensionMarker marker = GTRegistries.DIMENSION_MARKERS.getOrDefault(ResourceLocation.tryParse(dimension),
                    null);
            ItemStack icon = marker == null ? new ItemStack(Items.BARRIER) : marker.getIcon();
            CustomItemStackHandler handler = new CustomItemStackHandler(1);
            handler.setStackInSlot(0, icon);
            SlotWidget slot = new SlotWidget(handler, 0, x, 4, false, false);
            slot.setBackgroundTexture(GuiTextures.SLOT);
            slot.setIngredientIO(IngredientIO.INPUT);
            backdrop.getParent().addWidget(slot);
            x += 20;
        }
    }

    private static Widget findBackdrop(WidgetGroup group) {
        for (Widget widget : group.getWidgetsByType(Widget.class)) {
            if (BACKDROP_ID.equals(widget.getId())) return widget;
        }
        return null;
    }

    /** The names under the inputs, and the frame of slots with its heading. */
    private static class Backdrop extends Widget {

        Backdrop() {
            super(0, 0, WIDTH, HEIGHT);
        }

        @Override
        @OnlyIn(Dist.CLIENT)
        public void drawInBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
            int x0 = getPosition().x;
            int y0 = getPosition().y;
            // the tube from the markers down to the grid
            int centre = x0 + WIDTH / 2;
            graphics.fill(centre - 3, y0 + 22, centre + 3, y0 + GRID_Y - 1, 0xFF373737);
            graphics.fill(centre - 2, y0 + 22, centre + 2, y0 + GRID_Y - 2, 0xFFFFFFFF);
            // the empty slots of the frame
            for (int row = 0; row < ROWS; row++) {
                for (int column = 0; column < COLUMNS; column++) {
                    int x = x0 + GRID_X + 18 * column, y = y0 + GRID_Y + 18 * row;
                    graphics.fill(x, y, x + 18, y + 18, 0xFF373737);
                    graphics.fill(x + 1, y + 1, x + 17, y + 17, 0xFF8B8B8B);
                    graphics.fill(x + 1, y + 1, x + 17, y + 2, 0xFF555555);
                    graphics.fill(x + 1, y + 1, x + 2, y + 17, 0xFF555555);
                }
            }
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
