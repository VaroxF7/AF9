package com.af9.core.machine;

import com.af9.core.AF9Core;
import com.af9.core.litho.Coolant;

import com.gregtechceu.gtceu.api.block.ICoilType;
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
import com.gregtechceu.gtceu.api.registry.GTRegistries;

import com.lowdragmc.lowdraglib.gui.texture.ColorBorderTexture;
import com.lowdragmc.lowdraglib.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib.gui.widget.ProgressWidget;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.utils.Position;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fluids.FluidStack;

import java.util.List;

/**
 * The Sanguinite Hearth Furnace's recipes in EMI / JEI, in the Particle Accelerator page's style
 * ({@link AcceleratorRecipeUI}): the dusts top left (3x3), the coolant bottom left apart from everything else,
 * marked as coolant (its slot in ice, "COOLANT" over it, hover lines naming the grade and the Coolant Hatch),
 * helium beside it as a plain fluid, GT's arrow and the molten fluid out.
 * Temperature and coil come from the recipe's {@code ebf_temp} as data info (like the EBF modes), so the page
 * shows what coils it needs with no mistakes.
 */
public class SanguiniteHearthRecipeUI extends GTRecipeTypeUI {

    public static final String TYPE = "sanguinite_hearth";
    public static final int WIDTH = 176, HEIGHT = 92;
    /** Layout: 3x3 dusts, coolant + helium, arrow, molten out. */
    public static final int ITEMS_X = 4, ITEMS_Y = 4;
    public static final int COOLANT_X = 4, COOLANT_Y = 64, HELIUM_X = 26, HELIUM_Y = 64;
    public static final int ARROW_X = 78, ARROW_Y = 30, OUT_X = 116, OUT_Y = 30;
    public static final String FLOW_ID = "af9_hearth_flow";

    private final GTRecipeType type;

    public SanguiniteHearthRecipeUI(GTRecipeType type) {
        super(type);
        this.type = type;
    }

    /** Gives the hearth type this page, keeping what its KubeJS definition set, plus temp/coil/heating lines. */
    @SuppressWarnings("removal")
    public static void install() {
        GTRecipeType type = GTRegistries.RECIPE_TYPES.get(new ResourceLocation("gtceu", TYPE));
        if (type == null) {
            AF9Core.LOGGER.warn("Recipe type gtceu:{} not found - is the AF9 KubeJS startup script loaded?", TYPE);
            return;
        }
        GTRecipeTypeUI old = type.getRecipeUI();
        SanguiniteHearthRecipeUI ui = new SanguiniteHearthRecipeUI(type);
        ui.setSlotOverlays(old.getSlotOverlays());
        ui.setProgressBarTexture(old.getProgressBarTexture());
        ui.setMaxTooltips(old.getMaxTooltips());
        type.setRecipeUI(ui);
        // rendered as plain labels, so the texts must not contain '%'
        type.addDataInfo(data -> Component.translatable("af9.recipe.hearth_temperature", data.getInt("ebf_temp"))
                .getString());
        type.addDataInfo(data -> {
            ICoilType coil = ICoilType.getMinRequiredType(data.getInt("ebf_temp"));
            if (coil == null || coil.getMaterial().isNull()) return "";
            return Component.translatable("af9.recipe.hearth_coil",
                    Component.translatable(coil.getMaterial().getUnlocalizedName())).getString();
        });
        type.addDataInfo(data -> Component.translatable("af9.recipe.hearth_heating").getString());
    }

    /** Our layout; GT's binding of the recipe to the slots (by their ids) and the arrow. */
    @Override
    public IEditableUI<WidgetGroup, RecipeHolder> createEditableUITemplate(boolean isSteam, boolean isHighPressure) {
        IEditableUI<WidgetGroup, RecipeHolder> gt = super.createEditableUITemplate(isSteam, isHighPressure);
        return new IEditableUI.Normal<>(this::layout, gt::setupUI);
    }

    private WidgetGroup layout() {
        int items = Math.min(9, type.maxInputs.getInt(ItemRecipeCapability.CAP));
        WidgetGroup group = new WidgetGroup(0, 0, WIDTH, HEIGHT);
        for (int i = 0; i < items; i++) {
            slot(group, ItemRecipeCapability.CAP, IO.IN, i, items, ITEMS_X + 18 * (i % 3), ITEMS_Y + 18 * (i / 3));
        }
        // fluid_in_0 is always the coolant (normal print: only fluid; helium print: coolant + helium)
        Widget coolant = FluidRecipeCapability.CAP.createWidget();
        coolant.setSelfPosition(new Position(COOLANT_X, COOLANT_Y));
        coolant.setBackground(new GuiTextureGroup(new ColorRectTexture(0xFF0B2530),
                new ColorBorderTexture(1, AcceleratorFlowWidget.ICE)));
        coolant.setId(FluidRecipeCapability.CAP.slotName(IO.IN, 0));
        group.addWidget(coolant);
        // fluid_in_1 is the helium boost (empty on the normal print)
        slot(group, FluidRecipeCapability.CAP, IO.IN, 1, 2, HELIUM_X, HELIUM_Y);
        // fluid_out_0 is the molten sanguinite
        slot(group, FluidRecipeCapability.CAP, IO.OUT, 0, 1, OUT_X, OUT_Y);
        var arrow = new ProgressWidget(ProgressWidget.JEIProgress, ARROW_X, ARROW_Y, 20, 20,
                getProgressBarTexture());
        arrow.setId("progress");
        group.addWidget(arrow);
        var machine = new Widget(OUT_X - 4, OUT_Y + 24, 26, 12);
        machine.setHoverTooltips(Component.translatable("block.gtceu.sanguinite_hearth_furnace"),
                Component.translatable("gtceu." + TYPE));
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

    /**
     * The coolant's hover text says what it is (after GT's own lines for the slot); then the type's own page
     * builder.
     */
    @Override
    public void appendJEIUI(GTRecipe recipe, WidgetGroup widgetGroup) {
        Coolant least = leastCoolant(recipe);
        WidgetUtils.widgetByIdForEach(widgetGroup, "^" + FluidRecipeCapability.CAP.slotName(IO.IN) + "_0$",
                TankWidget.class, tank -> markCoolant(tank, least));
        super.appendJEIUI(recipe, widgetGroup);
    }

    /** The weakest coolant the recipe takes (its grade tag holds it and the colder ones), or null. */
    private static Coolant leastCoolant(GTRecipe recipe) {
        Coolant least = null;
        for (Content content : recipe.inputs.getOrDefault(FluidRecipeCapability.CAP, List.of())) {
            // only fluid_in_0 is coolant; helium (fluid_in_1) is never a supercooled fluid
            for (FluidStack stack : FluidRecipeCapability.CAP.of(content.content).getStacks()) {
                Coolant coolant = Coolant.of(stack);
                if (coolant != null && (least == null || coolant.ordinal() < least.ordinal())) least = coolant;
            }
        }
        return least;
    }

    /** Adds the coolant's lines to the tank's tooltip. */
    private static void markCoolant(TankWidget tank, Coolant least) {
        AcceleratorRecipeUI.addTooltips(tank, tooltips -> {
            tooltips.add(Component.literal("\u2744 ").append(
                    Component.translatable("af9.recipe.hearth_page.coolant"))
                    .withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD));
            if (least != null) {
                tooltips.add(Component.translatable("af9.recipe.hearth_page.coolant_grade",
                        Component.translatable("af9.litho.coolant." + least.id)).withStyle(ChatFormatting.AQUA));
            }
            tooltips.add(Component.translatable("af9.recipe.hearth_page.coolant_hatch")
                    .withStyle(ChatFormatting.GRAY));
            tooltips.add(Component.translatable("af9.recipe.hearth_page.coolant_hearth")
                    .withStyle(ChatFormatting.DARK_AQUA));
        });
    }
}
