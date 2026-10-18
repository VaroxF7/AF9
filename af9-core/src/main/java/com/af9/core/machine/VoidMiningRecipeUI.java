package com.af9.core.machine;

import com.gregtechceu.gtceu.api.GTValues;
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
import com.gregtechceu.gtceu.api.recipe.RecipeHelper;
import com.gregtechceu.gtceu.api.recipe.ingredient.EnergyStack;
import com.gregtechceu.gtceu.api.recipe.ui.GTRecipeTypeUI;
import com.gregtechceu.gtceu.api.registry.GTRegistries;
import com.gregtechceu.gtceu.api.transfer.item.CustomItemStackHandler;
import com.gregtechceu.gtceu.utils.FormattingUtil;
import com.gregtechceu.gtceu.utils.GTUtil;

import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.widget.LabelWidget;
import com.lowdragmc.lowdraglib.gui.widget.ProgressWidget;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.jei.IngredientIO;
import com.lowdragmc.lowdraglib.utils.Position;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.loading.FMLEnvironment;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * The Void Miner's recipes in EMI / JEI, one page a recipe: on top the drilling fluid and the circuit it takes and the arrow
 * to the ores, under them a frame of {@link #COLUMNS} x {@link #ROWS} slots with the ores one run of this recipe brings, in
 * its first slots; at the bottom the duration, the energy of a run, the draw in amps at the recipe's tier (the tier in its
 * colour on the right) and the icon of the dimension the miner works in. GT's own slots (same ids) for the recipe, so the
 * viewers' lookups of an ore still find the circuit that makes it; GT's text lines give way to ours.
 */
public class VoidMiningRecipeUI extends GTRecipeTypeUI {

    public static final int WIDTH = 176, GRID_X = 7, GRID_Y = 26, COLUMNS = 9, ROWS = 3, TOP_Y = 4;
    public static final int HEIGHT = GRID_Y + 18 * ROWS + 4;
    /** The text lines under the page: their first line's top, their spacing, their left margin. */
    public static final int TEXT_Y = HEIGHT + 2, LINE = 10, TEXT_X = 3;
    public static final String BACKDROP_ID = "af9_void_mining_backdrop";

    private static final DecimalFormat AMPS = new DecimalFormat("0.##", DecimalFormatSymbols.getInstance(Locale.ROOT));

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
        // our three text lines
        ui.setMaxTooltips(Math.max(3, old.getMaxTooltips()));
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
        slot(group, FluidRecipeCapability.CAP, IO.IN, 0, GRID_X, TOP_Y,
                getOverlaysForSlot(false, FluidRecipeCapability.CAP, false, false, false));
        slot(group, ItemRecipeCapability.CAP, IO.IN, 0, GRID_X + 20, TOP_Y,
                getOverlaysForSlot(false, ItemRecipeCapability.CAP, true, false, false));
        var arrow = new ProgressWidget(ProgressWidget.JEIProgress, GRID_X + 46, TOP_Y, 20, 18,
                getProgressBarTexture());
        arrow.setId("progress");
        group.addWidget(arrow);
        // the recipe's ores are the first slots of the frame
        for (int i = 0; i < type.maxOutputs.getInt(ItemRecipeCapability.CAP); i++) {
            slot(group, ItemRecipeCapability.CAP, IO.OUT, i, GRID_X + 1 + 18 * (i % COLUMNS),
                    GRID_Y + 1 + 18 * (i / COLUMNS),
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

    /**
     * The text lines of the page (duration, energy of a run, draw and tier) and the dimension icons of the recipe's area
     * on the right of them, in place of GT's lines.
     */
    @Override
    public void appendJEIUI(GTRecipe recipe, WidgetGroup widgetGroup) {
        super.appendJEIUI(recipe, widgetGroup);
        if (FMLEnvironment.dist != Dist.CLIENT) return;
        // GT's own lines and dimension markers sit under the page and in its corner: the page says it itself
        // (the slots of the recipe itself are in the page's group, not here)
        for (Widget widget : new java.util.ArrayList<>(widgetGroup.widgets)) {
            if (widget instanceof SlotWidget || widget instanceof LabelWidget) widgetGroup.removeWidget(widget);
        }
        addTexts(recipe, widgetGroup);
        Widget backdrop = findBackdrop(widgetGroup);
        if (backdrop == null || backdrop.getParent() == null) return;
        int area = VoidMinerMachine.areaOf(type);
        if (area < 0) return;
        String[] dimensions = VoidMinerMachine.AREA_DIMENSIONS[area];
        int x = WIDTH - 3 - 18 * dimensions.length;
        for (String dimension : dimensions) {
            DimensionMarker marker = GTRegistries.DIMENSION_MARKERS.getOrDefault(ResourceLocation.tryParse(dimension),
                    null);
            ItemStack icon = marker == null ? new ItemStack(Items.BARRIER) : marker.getIcon();
            CustomItemStackHandler handler = new CustomItemStackHandler(1);
            handler.setStackInSlot(0, icon);
            SlotWidget slot = new SlotWidget(handler, 0, x, TEXT_Y, false, false);
            slot.setBackgroundTexture(IGuiTexture.EMPTY);
            slot.setIngredientIO(IngredientIO.INPUT);
            backdrop.getParent().addWidget(slot);
            x += 18;
        }
    }

    @OnlyIn(Dist.CLIENT)
    private static void addTexts(GTRecipe recipe, WidgetGroup group) {
        EnergyStack energy = RecipeHelper.getRealEUt(recipe);
        long total = energy.getTotalEU();
        int tier = Math.max(0, Math.min(GTValues.VNF.length - 1, GTUtil.getTierByVoltage(energy.voltage())));
        double amps = total / (double) GTValues.VA[tier];
        double seconds = recipe.duration / 20.0;
        group.addWidget(new LabelWidget(TEXT_X, TEXT_Y, I18n.get("af9.recipe.void_mining.duration",
                new DecimalFormat("0.##", DecimalFormatSymbols.getInstance(Locale.ROOT)).format(seconds))));
        group.addWidget(new LabelWidget(TEXT_X, TEXT_Y + LINE, I18n.get("af9.recipe.void_mining.total",
                FormattingUtil.formatNumbers(total * recipe.duration))));
        group.addWidget(new LabelWidget(TEXT_X, TEXT_Y + 2 * LINE, I18n.get("af9.recipe.void_mining.usage",
                AMPS.format(amps), GTValues.VN[tier])));
        // the tier, in its colour, at the right of the last line
        String name = GTValues.VNF[tier];
        int width = Minecraft.getInstance().font.width(name);
        group.addWidget(new LabelWidget(WIDTH - TEXT_X - width, TEXT_Y + 2 * LINE, name));
    }

    private static Widget findBackdrop(WidgetGroup group) {
        for (Widget widget : group.getWidgetsByType(Widget.class)) {
            if (BACKDROP_ID.equals(widget.getId())) return widget;
        }
        return null;
    }

    /** The empty slots of the frame. */
    private static class Backdrop extends Widget {

        Backdrop() {
            super(0, 0, WIDTH, HEIGHT);
        }

        @Override
        @OnlyIn(Dist.CLIENT)
        public void drawInBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
            int x0 = getPosition().x;
            int y0 = getPosition().y;
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
    }
}
