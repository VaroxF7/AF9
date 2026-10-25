package com.af9.core.machine;

import com.af9.core.AF9Core;

import com.gregtechceu.gtceu.api.capability.recipe.FluidRecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.capability.recipe.ItemRecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.RecipeCapability;
import com.gregtechceu.gtceu.api.gui.WidgetUtils;
import com.gregtechceu.gtceu.api.gui.editor.IEditableUI;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.GTRecipeType;
import com.gregtechceu.gtceu.api.recipe.ui.GTRecipeTypeUI;
import com.gregtechceu.gtceu.api.registry.GTRegistries;

import com.lowdragmc.lowdraglib.gui.widget.ProgressWidget;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.utils.Position;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * The Dimensionally Transcendent Plasma Forge's recipes in EMI / JEI, in the Particle Accelerator page's style
 * ({@link AcceleratorRecipeUI}): the items in a row top left and the fluids in a row under them, both piped into the
 * forge's plasma bore in the middle (a glass tube through three coil rings, a plasma bar that burns hotter the more
 * the recipe takes), GT's arrow over the bore, and the forged matter piped out to the right, items over fluids
 * ({@link PlasmaForgeFlowWidget}). GT's own slots (same ids), so EMI shows and looks them up as usual. The page says
 * what the forge needs to start it: its magnetic field up, which takes osmium plasma and 10 A of LuV stabiliser power
 * (two short lines under the recipe: a data line is one unwrapped label, so each is kept to the page's width).
 */
public class PlasmaForgeRecipeUI extends GTRecipeTypeUI {

    public static final String TYPE = "plasma_forge";
    public static final int WIDTH = 176, HEIGHT = 70;
    /** Layout: slots in rows of three (18 px apart), the bore between the two groups. */
    public static final int IN_X = 4, OUT_X = 118, ITEMS_Y = 6, FLUIDS_Y = 40, SLOTS = 3;
    /** The bore: x from, x to, centre y; the manifolds the pipes meet at. */
    public static final int BORE_X0 = 70, BORE_X1 = 108, BORE_Y = 33, LEFT_MANIFOLD = 62, RIGHT_MANIFOLD = 112;
    public static final String FLOW_ID = "af9_forge_flow";

    private final GTRecipeType type;

    public PlasmaForgeRecipeUI(GTRecipeType type) {
        super(type);
        this.type = type;
    }

    /** Gives the forge's type this page, keeping what its KubeJS definition set, plus the readiness line. */
    @SuppressWarnings("removal") // new ResourceLocation(ns, path) is the only constructor on 1.20.1
    public static void install() {
        GTRecipeType type = GTRegistries.RECIPE_TYPES.get(new ResourceLocation("gtceu", TYPE));
        if (type == null) {
            AF9Core.LOGGER.warn("Recipe type gtceu:{} not found - is the AF9 KubeJS startup script loaded?", TYPE);
            return;
        }
        GTRecipeTypeUI old = type.getRecipeUI();
        PlasmaForgeRecipeUI ui = new PlasmaForgeRecipeUI(type);
        ui.setSlotOverlays(old.getSlotOverlays());
        ui.setProgressBarTexture(old.getProgressBarTexture());
        ui.setMaxTooltips(old.getMaxTooltips());
        type.setRecipeUI(ui);
        // rendered as plain labels, so the texts must not contain '%' and must fit the page (176 px) in one line
        type.addDataInfo(data -> Component.translatable("af9.recipe.forge_field").getString());
        type.addDataInfo(data -> Component.translatable("af9.recipe.forge_stabilisers").getString());
        // the field heat the recipe needs, Kelvin (modules: the Superheating Element makes the field ten times hotter)
        type.addDataInfo(data -> data.contains(PlasmaForgeMachine.FORGE_HEAT_KEY) ?
                Component.translatable("af9.recipe.forge_heat", String.format("%,d",
                        data.getInt(PlasmaForgeMachine.FORGE_HEAT_KEY))).getString() : "");
    }

    /** Our layout; GT's binding of the recipe to the slots (by their ids) and the arrow. */
    @Override
    public IEditableUI<WidgetGroup, RecipeHolder> createEditableUITemplate(boolean isSteam, boolean isHighPressure) {
        IEditableUI<WidgetGroup, RecipeHolder> gt = super.createEditableUITemplate(isSteam, isHighPressure);
        return new IEditableUI.Normal<>(this::layout, gt::setupUI);
    }

    private WidgetGroup layout() {
        int itemsIn = Math.min(SLOTS, type.maxInputs.getInt(ItemRecipeCapability.CAP));
        int fluidsIn = Math.min(SLOTS, type.maxInputs.getInt(FluidRecipeCapability.CAP));
        int itemsOut = Math.min(SLOTS, type.maxOutputs.getInt(ItemRecipeCapability.CAP));
        int fluidsOut = Math.min(SLOTS, type.maxOutputs.getInt(FluidRecipeCapability.CAP));
        WidgetGroup group = new WidgetGroup(0, 0, WIDTH, HEIGHT);
        // the pipes and the bore first: the slots draw over them
        PlasmaForgeFlowWidget flow = new PlasmaForgeFlowWidget();
        flow.setId(FLOW_ID);
        group.addWidget(flow);
        slots(group, ItemRecipeCapability.CAP, IO.IN, itemsIn, IN_X, ITEMS_Y);
        slots(group, FluidRecipeCapability.CAP, IO.IN, fluidsIn, IN_X, FLUIDS_Y);
        slots(group, ItemRecipeCapability.CAP, IO.OUT, itemsOut, OUT_X, ITEMS_Y);
        slots(group, FluidRecipeCapability.CAP, IO.OUT, fluidsOut, OUT_X, FLUIDS_Y);
        var arrow = new ProgressWidget(ProgressWidget.JEIProgress, (BORE_X0 + BORE_X1) / 2 - 10, BORE_Y - 10, 20, 20,
                getProgressBarTexture());
        arrow.setId("progress");
        group.addWidget(arrow);
        // the bore's tooltip: the machines and what they need
        var bore = new Widget(BORE_X0, BORE_Y - 14, BORE_X1 - BORE_X0, 28);
        bore.setHoverTooltips(Component.translatable("block.gtceu.dtpf"),
                Component.translatable("af9.recipe.forge_page.fuel"),
                Component.translatable("af9.recipe.forge_page.power"),
                Component.translatable("af9.recipe.forge_page.run"));
        group.addWidget(bore);
        return group;
    }

    private void slots(WidgetGroup group, RecipeCapability<?> cap, IO io, int count, int x, int y) {
        for (int i = 0; i < count; i++) {
            Widget slot = cap.createWidget();
            slot.setSelfPosition(new Position(x + 18 * i, y));
            slot.setBackground(getOverlaysForSlot(io == IO.OUT, cap, i == count - 1, false, false));
            slot.setId(cap.slotName(io, i));
            group.addWidget(slot);
        }
    }

    /** The recipe to the pipes (how much goes in and out, the plasma's colour), then the type's own page builder. */
    @Override
    public void appendJEIUI(GTRecipe recipe, WidgetGroup widgetGroup) {
        WidgetUtils.widgetByIdForEach(widgetGroup, "^" + FLOW_ID + "$", PlasmaForgeFlowWidget.class,
                flow -> flow.setRecipe(recipe));
        super.appendJEIUI(recipe, widgetGroup);
    }
}
