package com.af9.core.elevator;

import com.af9.core.client.ClientOreVeins;
import com.af9.core.machine.AcceleratorFlowWidget;
import com.af9.core.machine.AcceleratorRecipeUI;

import com.gregtechceu.gtceu.api.capability.recipe.FluidRecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.capability.recipe.ItemRecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.RecipeCapability;
import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.gui.WidgetUtils;
import com.gregtechceu.gtceu.api.gui.editor.IEditableUI;
import com.gregtechceu.gtceu.api.gui.widget.SlotWidget;
import com.gregtechceu.gtceu.api.gui.widget.TankWidget;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.GTRecipeType;
import com.gregtechceu.gtceu.api.recipe.ui.GTRecipeTypeUI;
import com.gregtechceu.gtceu.integration.xei.handlers.item.CycleItemStackHandler;

import com.lowdragmc.lowdraglib.gui.texture.ColorBorderTexture;
import com.lowdragmc.lowdraglib.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.widget.ProgressWidget;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.jei.IngredientIO;
import com.lowdragmc.lowdraglib.utils.Position;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;

import java.util.ArrayList;
import java.util.List;

/**
 * The Space Elevator's expeditions in EMI / JEI, in the style of the other AF9 pages
 * ({@link com.af9.core.machine.AcceleratorRecipeUI}): the Mining Drone top left (kept), the hydrogen and, marked as
 * coolant (its hover text names the Coolant Hatch it goes into), the supercooled fluid under it, piped into the
 * tower; the tower with its cable, the climber and a drone on its way to the asteroid in the middle
 * ({@link SpaceMiningFlowWidget}); GT's arrow; and on the right what the recipe itself cannot say, because a run gets
 * its asteroid only when it starts: <b>the ores this drone's asteroids hold</b>, taking turns in nine slots, with the
 * stacks an expedition brings under them. The ores are outputs to the viewers, so looking up a raw ore finds the
 * expedition that brings it. GT's own slots (same ids) for the inputs.
 */
public class SpaceMiningRecipeUI extends GTRecipeTypeUI {

    public static final int WIDTH = 176, HEIGHT = 76;
    /** Layout: the drone and the two fluids, the scene, the arrow, the ores (so many columns and rows of slots). */
    public static final int IN_X = 4, DRONE_Y = 2, HYDROGEN_Y = 27, COOLANT_Y = 53;
    public static final int SCENE_X = 38, SCENE_W = 54, ARROW_X = 95, ARROW_Y = 28;
    public static final int ORES_X = 118, ORES_Y = 4, ORE_COLUMNS = 3, ORE_ROWS = 3;
    public static final String FLOW_ID = "af9_space_mining_flow";

    private final GTRecipeType type;

    public SpaceMiningRecipeUI(GTRecipeType type) {
        super(type);
        this.type = type;
    }

    /** Gives the recipe type this page, keeping what its KubeJS definition set on GT's. */
    public static void install(GTRecipeType type) {
        GTRecipeTypeUI old = type.getRecipeUI();
        SpaceMiningRecipeUI ui = new SpaceMiningRecipeUI(type);
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
        int fluids = Math.min(2, type.maxInputs.getInt(FluidRecipeCapability.CAP));
        WidgetGroup group = new WidgetGroup(0, 0, WIDTH, HEIGHT);
        // the scene and the pipes first: the slots draw over them
        SpaceMiningFlowWidget flow = new SpaceMiningFlowWidget();
        flow.setId(FLOW_ID);
        group.addWidget(flow);
        slot(group, ItemRecipeCapability.CAP, IO.IN, 0, IN_X, DRONE_Y,
                getOverlaysForSlot(false, ItemRecipeCapability.CAP, true, false, false));
        for (int i = 0; i < fluids; i++) {
            // the second fluid is the coolant, not a fluid like the others: its slot in ice on dark frost
            boolean coolant = i == 1;
            slot(group, FluidRecipeCapability.CAP, IO.IN, i, IN_X, coolant ? COOLANT_Y : HYDROGEN_Y, coolant ?
                    new GuiTextureGroup(new ColorRectTexture(0xFF0B2530),
                            new ColorBorderTexture(1, AcceleratorFlowWidget.ICE)) :
                    getOverlaysForSlot(false, FluidRecipeCapability.CAP, false, false, false));
        }
        var arrow = new ProgressWidget(ProgressWidget.JEIProgress, ARROW_X, ARROW_Y, 20, 20,
                getProgressBarTexture());
        arrow.setId("progress");
        group.addWidget(arrow);
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
     * The drone's tier to the scene, and the ores it reaches into the slots beside it; the coolant's hover text names
     * the Coolant Hatch (after GT's own lines for the slot); then the type's own builder.
     */
    @Override
    public void appendJEIUI(GTRecipe recipe, WidgetGroup widgetGroup) {
        int tier = SpaceElevatorMachine.droneTier(recipe);
        WidgetUtils.widgetByIdForEach(widgetGroup, "^" + FluidRecipeCapability.CAP.slotName(IO.IN, 1) + "$",
                TankWidget.class, tank -> AcceleratorRecipeUI.addTooltips(tank, tooltips -> {
                    tooltips.add(Component.literal("\u2744 ").append(
                            Component.translatable("af9.recipe.space_mining.coolant_tooltip.0"))
                            .withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD));
                    tooltips.add(Component.translatable("af9.recipe.space_mining.coolant_tooltip.1")
                            .withStyle(ChatFormatting.GRAY));
                }));
        WidgetUtils.widgetByIdForEach(widgetGroup, "^" + FLOW_ID + "$", SpaceMiningFlowWidget.class, flow -> {
            flow.setTier(tier);
            // into the page's own group: GT builds that anew when the page is redrawn, the slots with it
            if (flow.getParent() != null && FMLEnvironment.dist == Dist.CLIENT) {
                addOres(flow.getParent(), tier, reach(tier));
            }
        });
        super.appendJEIUI(recipe, widgetGroup);
    }

    /** The ores a drone tier's asteroids hold, as items (the raw ores), from the veins the client knows. */
    private static List<ItemStack> reach(int tier) {
        List<ItemStack> ores = new ArrayList<>();
        for (String material : OreCatalog.reach(tier, ClientOreVeins.get())) {
            ItemStack ore = OreCatalog.ore(material, 1);
            if (ore != null) ores.add(ore);
        }
        return ores;
    }

    /** Nine slots the ores take turns in: every ore is an output the recipe viewers know. */
    private static void addOres(WidgetGroup group, int tier, List<ItemStack> ores) {
        int slots = ORE_COLUMNS * ORE_ROWS;
        List<List<ItemStack>> turns = new ArrayList<>();
        for (int i = 0; i < slots; i++) turns.add(new ArrayList<>());
        for (int i = 0; i < ores.size(); i++) turns.get(i % slots).add(ores.get(i));
        CycleItemStackHandler handler = new CycleItemStackHandler(turns);
        for (int i = 0; i < slots; i++) {
            SlotWidget slot = new SlotWidget(handler, i, ORES_X + 18 * (i % ORE_COLUMNS),
                    ORES_Y + 18 * (i / ORE_COLUMNS), false, false);
            slot.setBackgroundTexture(GuiTextures.SLOT);
            slot.setIngredientIO(IngredientIO.OUTPUT);
            slot.setOnAddedTooltips((widget, tooltips) -> {
                tooltips.add(Component.translatable("af9.recipe.space_mining.ore_tooltip.0")
                        .withStyle(ChatFormatting.AQUA));
                tooltips.add(Component.translatable("af9.recipe.space_mining.ore_tooltip.1",
                        SpaceElevatorMachine.minStacks(tier), SpaceElevatorMachine.maxStacks(tier))
                        .withStyle(ChatFormatting.GRAY));
            });
            group.addWidget(slot);
        }
    }
}
