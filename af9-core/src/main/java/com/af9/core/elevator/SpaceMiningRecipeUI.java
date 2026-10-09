package com.af9.core.elevator;

import com.af9.core.client.ClientOreVeins;
import com.af9.core.machine.AcceleratorFlowWidget;
import com.af9.core.machine.AcceleratorRecipeUI;

import com.gregtechceu.gtceu.api.capability.recipe.FluidRecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.capability.recipe.ItemRecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.RecipeCapability;
import com.gregtechceu.gtceu.api.data.DimensionMarker;
import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.gui.WidgetUtils;
import com.gregtechceu.gtceu.api.gui.editor.IEditableUI;
import com.gregtechceu.gtceu.api.gui.widget.SlotWidget;
import com.gregtechceu.gtceu.api.gui.widget.TankWidget;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.GTRecipeType;
import com.gregtechceu.gtceu.api.recipe.ui.GTRecipeTypeUI;
import com.gregtechceu.gtceu.api.registry.GTRegistries;
import com.gregtechceu.gtceu.api.transfer.item.CustomItemStackHandler;
import com.gregtechceu.gtceu.integration.xei.handlers.fluid.CycleFluidStackHandler;
import com.gregtechceu.gtceu.integration.xei.handlers.item.CycleItemStackHandler;
import com.gregtechceu.gtceu.utils.FormattingUtil;

import com.lowdragmc.lowdraglib.gui.texture.ColorBorderTexture;
import com.lowdragmc.lowdraglib.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib.gui.widget.ProgressWidget;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.jei.IngredientIO;
import com.lowdragmc.lowdraglib.utils.Position;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fluids.FluidStack;
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
 * <p>
 * The liquid missions' page ({@code space_pumping}) is the same with a planet for the asteroid and, in the nine slots,
 * <b>the fluids of the planets this drone reaches</b> ({@link PlanetCatalog}), each with the buckets a mission brings:
 * one of them a mission, the one picked on the elevator's screen.
 */
public class SpaceMiningRecipeUI extends GTRecipeTypeUI {

    public static final int WIDTH = 176;
    /**
     * Layout, as GTNH's Eye of Harmony page: a row of inputs (the drone, the hydrogen, the coolant) and the arrow on top,
     * under them a panel of slots, one for each ore (or each fluid) the drone can bring: {@link #GRID_COLUMNS} wide,
     * {@link #ORE_ROWS} rows for the ores (a drone's asteroids hold more than that on a big server: they take turns in
     * the slots), as many rows as the table has for the fluids.
     */
    public static final int TOP_Y = 4, MARKER_X = 79, DRONE_X = 101, FLUIDS_X = 4, CIRCUIT_X = 119, DRILL_X = 137, CRATE_X = 155;
    public static final int GRID_X = 7, GRID_Y = 48, GRID_COLUMNS = 9, ORE_ROWS = 5;
    public static final String FLOW_ID = "af9_space_mining_flow";

    private final GTRecipeType type;
    /** The liquid missions' page: fluids for ores, a planet for the asteroid. */
    private final boolean liquid;

    public SpaceMiningRecipeUI(GTRecipeType type, boolean liquid) {
        super(type);
        this.type = type;
        this.liquid = liquid;
    }

    /** Gives the recipe type this page, keeping what its KubeJS definition set on GT's. */
    public static void install(GTRecipeType type, boolean liquid) {
        GTRecipeTypeUI old = type.getRecipeUI();
        SpaceMiningRecipeUI ui = new SpaceMiningRecipeUI(type, liquid);
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

    /** Rows of the grid: the ores' fixed, the fluids as many as the table needs. */
    private int rows() {
        if (!liquid) return ORE_ROWS;
        return Math.max(1, Math.min(ORE_ROWS, (PlanetCatalog.all().size() + GRID_COLUMNS - 1) / GRID_COLUMNS));
    }

    private int height() {
        return GRID_Y + 18 * rows() + 6;
    }

    private WidgetGroup layout() {
        int fluids = Math.min(2, type.maxInputs.getInt(FluidRecipeCapability.CAP));
        WidgetGroup group = new WidgetGroup(0, 0, WIDTH, height());
        // the panel first: the slots draw over it
        SpaceMiningFlowWidget flow = new SpaceMiningFlowWidget(liquid, height(), rows());
        flow.setId(FLOW_ID);
        group.addWidget(flow);
        slot(group, ItemRecipeCapability.CAP, IO.IN, 0, DRONE_X, TOP_Y,
                getOverlaysForSlot(false, ItemRecipeCapability.CAP, true, false, false));
        // the circuit that picks the asteroid, and what a run uses up: a drill head and a crate
        if (!liquid && type.maxInputs.getInt(ItemRecipeCapability.CAP) >= 4) {
            slot(group, ItemRecipeCapability.CAP, IO.IN, 1, CIRCUIT_X, TOP_Y,
                    getOverlaysForSlot(false, ItemRecipeCapability.CAP, true, false, false));
            slot(group, ItemRecipeCapability.CAP, IO.IN, 2, DRILL_X, TOP_Y,
                    getOverlaysForSlot(false, ItemRecipeCapability.CAP, false, false, false));
            slot(group, ItemRecipeCapability.CAP, IO.IN, 3, CRATE_X, TOP_Y,
                    getOverlaysForSlot(false, ItemRecipeCapability.CAP, false, false, false));
        }
        for (int i = 0; i < fluids; i++) {
            // the second fluid is the coolant, not a fluid like the others: its slot in ice on dark frost
            boolean coolant = i == 1;
            slot(group, FluidRecipeCapability.CAP, IO.IN, i, FLUIDS_X + (coolant ? 0 : 20), TOP_Y, coolant ?
                    new GuiTextureGroup(new ColorRectTexture(0xFF0B2530),
                            new ColorBorderTexture(1, AcceleratorFlowWidget.ICE)) :
                    getOverlaysForSlot(false, FluidRecipeCapability.CAP, false, false, false));
        }
        var arrow = new ProgressWidget(ProgressWidget.JEIProgress, MARKER_X + 3, TOP_Y + 20, 12, 22,
                com.af9.core.machine.DownArrow.texture());
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
     * The drone's tier to the scene, and the ores (or the fluids) it reaches into the slots beside it; the coolant's
     * hover text names the Coolant Hatch (after GT's own lines for the slot); then the type's own builder.
     */
    @Override
    public void appendJEIUI(GTRecipe recipe, WidgetGroup widgetGroup) {
        int tier = SpaceMissionMachine.droneTier(recipe);
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
                addMarker(flow.getParent(), tier);
                if (liquid) addFluids(flow.getParent(), tier);
                else addAsteroid(flow.getParent(), tier, SpaceMissionMachine.circuitOf(recipe));
            }
        });
        super.appendJEIUI(recipe, widgetGroup);
    }

    /** The dimension marker where the drone used to be: the asteroid field, or the planet of the drone's tier. */
    private void addMarker(WidgetGroup group, int tier) {
        String[] planets = { "ad_astra:moon", "af9:zephyr", "af9:kronos", "af9:helios" };
        String dimension = liquid ? planets[Math.max(0, Math.min(planets.length - 1, tier - 1))] : "af9:asteroid_field";
        DimensionMarker marker = GTRegistries.DIMENSION_MARKERS.getOrDefault(ResourceLocation.tryParse(dimension), null);
        CustomItemStackHandler handler = new CustomItemStackHandler(1);
        handler.setStackInSlot(0, marker == null ? new ItemStack(Items.BARRIER) : marker.getIcon());
        SlotWidget slot = new SlotWidget(handler, 0, MARKER_X, TOP_Y, false, false);
        slot.setBackgroundTexture(GuiTextures.SLOT);
        slot.setOnAddedTooltips((widget, tooltips) -> tooltips.add(Component.literal(dimension)
                .withStyle(ChatFormatting.AQUA)));
        group.addWidget(slot);
    }

    /**
     * The ores of the asteroid this recipe's circuit picks (the n-th of the drone tier's, as the run counts them: the veins
     * the client knows), one slot each with what a run brings of it as a range; nothing while the circuit has no asteroid.
     */
    private static void addAsteroid(WidgetGroup group, int tier, int circuit) {
        var source = ClientOreVeins.get();
        List<String> veins = OreCatalog.veinIds(tier, source);
        if (circuit < 1 || circuit > veins.size()) return;
        List<String> ores = OreCatalog.veinOreItems(veins.get(circuit - 1), source);
        int least = SpaceMissionMachine.minStacks(tier), most = SpaceMissionMachine.maxStacks(tier);
        int count = Math.min(ores.size(), GRID_COLUMNS * ORE_ROWS);
        List<ItemStack> stacks = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            var item = net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(new ResourceLocation(ores.get(i)));
            stacks.add(item == null ? ItemStack.EMPTY : new ItemStack(item));
        }
        CustomItemStackHandler handler = new CustomItemStackHandler(Math.max(1, count));
        for (int i = 0; i < count; i++) handler.setStackInSlot(i, stacks.get(i));
        for (int i = 0; i < count; i++) {
            // the main ore half of the stacks, the others share the rest (SpaceMissionMachine.recipeOre)
            boolean alone = ores.size() == 1;
            int others = Math.max(1, ores.size() - 1);
            int lo = i == 0 ? (alone ? least : least / 2) : Math.max(1, (least - least / 2) / others);
            int hi = i == 0 ? (alone ? most : most / 2) : Math.max(lo, (most - most / 2) / others);
            SlotWidget slot = new SlotWidget(handler, i, GRID_X + 18 * (i % GRID_COLUMNS),
                    GRID_Y + 18 * (i / GRID_COLUMNS), false, false);
            slot.setBackgroundTexture(GuiTextures.SLOT);
            slot.setIngredientIO(IngredientIO.OUTPUT);
            // the amount is a range, not the stack size the item shows
            slot.setOverlay(new TextTexture(lo * 64 + "-" + hi * 64).scale(0.5F).transform(0F, 5F));
            slot.setOnAddedTooltips((widget, tooltips) -> tooltips.add(
                    Component.translatable("af9.recipe.space_mining.ore_tooltip.1", least, most)
                            .withStyle(ChatFormatting.GRAY)));
            group.addWidget(slot);
        }
    }

    /**
     * The slots the fluids take turns in, the buckets a mission brings on each: every fluid is an output the recipe
     * viewers know.
     */
    private static void addFluids(WidgetGroup group, int tier) {
        List<PlanetCatalog.Cargo> cargoes = new ArrayList<>();
        for (PlanetCatalog.Cargo cargo : PlanetCatalog.all()) {
            if (cargo.drone() == tier || (tier >= 4 && cargo.drone() > 4)) cargoes.add(cargo);
        }
        int slots = Math.max(1, Math.min(GRID_COLUMNS * ORE_ROWS, cargoes.size()));
        List<List<FluidStack>> turns = new ArrayList<>();
        for (int i = 0; i < slots; i++) turns.add(new ArrayList<>());
        for (int i = 0; i < cargoes.size(); i++) {
            PlanetCatalog.Cargo cargo = cargoes.get(i);
            turns.get(i % slots).add(new FluidStack(cargo.fluid(), cargo.millibuckets()));
        }
        CycleFluidStackHandler handler = new CycleFluidStackHandler(turns);
        for (int i = 0; i < slots; i++) {
            TankWidget tank = new TankWidget(handler, i, GRID_X + 18 * (i % GRID_COLUMNS),
                    GRID_Y + 18 * (i / GRID_COLUMNS), false, false);
            tank.setBackground(GuiTextures.FLUID_SLOT);
            tank.setIngredientIO(IngredientIO.OUTPUT);
            tank.setOnAddedTooltips((widget, tooltips) -> {
                PlanetCatalog.Cargo shown = cargoOf(widget.getFluid());
                if (shown == null) return;
                tooltips.add(Component.translatable("af9.recipe.space_pumping.fluid_tooltip.0", shown.planet(),
                        FormattingUtil.formatNumbers(shown.buckets())).withStyle(ChatFormatting.AQUA));
                tooltips.add(Component.translatable("af9.recipe.space_pumping.fluid_tooltip.1",
                        SpaceElevatorMachine.mark(shown.drone())).withStyle(ChatFormatting.GRAY));
                tooltips.add(Component.translatable("af9.recipe.space_pumping.fluid_tooltip.2")
                        .withStyle(ChatFormatting.GRAY));
            });
            group.addWidget(tank);
        }
    }

    /** The table's entry of a fluid a slot shows, null for none. */
    private static PlanetCatalog.Cargo cargoOf(FluidStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        for (PlanetCatalog.Cargo cargo : PlanetCatalog.all()) {
            if (cargo.fluid().isSame(stack.getFluid())) return cargo;
        }
        return null;
    }
}
