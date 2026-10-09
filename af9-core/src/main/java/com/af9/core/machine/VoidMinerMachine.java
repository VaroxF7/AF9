package com.af9.core.machine;

import com.af9.core.AF9Core;
import com.af9.core.machine.console.ConsoleWidget;
import com.af9.core.machine.console.SidePanelsUIWidget;
import com.af9.core.machine.console.VoidMinerConsoleWidget;
import com.af9.core.staged.StagedRecipes;

import com.gregtechceu.gtceu.api.capability.recipe.FluidRecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.capability.recipe.IRecipeHandler;
import com.gregtechceu.gtceu.api.capability.recipe.ItemRecipeCapability;
import com.gregtechceu.gtceu.api.gui.fancy.ConfiguratorPanel;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.fancyconfigurator.CircuitFancyConfigurator;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableItemStackHandler;
import com.gregtechceu.gtceu.api.machine.MachineDefinition;
import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.GTRecipeType;
import com.gregtechceu.gtceu.api.recipe.RecipeHelper;
import com.gregtechceu.gtceu.api.recipe.content.Content;
import com.gregtechceu.gtceu.api.recipe.ingredient.FluidIngredient;
import com.gregtechceu.gtceu.api.registry.GTRegistries;
import com.gregtechceu.gtceu.common.item.IntCircuitBehaviour;

import com.lowdragmc.lowdraglib.gui.modular.ModularUI;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringJoiner;
import java.util.TreeMap;

/**
 * The Void Miner, rebuilt: GT's controller block and structure stay exactly as they are ({@link #install} only
 * extends the definition), AF9 supplies the behaviour around them. Four areas as machine modes (Overworld, Nether,
 * End, Asteroids; recipe types {@code void_mining_*}, defined in
 * {@code kubejs/startup_scripts/gtceu/void_mining.js}): a programmed circuit picks the ore in the area, drilling
 * fluid goes in, tenfold raw ore comes out for the same time and energy. No data sticks.
 * <p>
 * MK2 and MK3 are separate machine definitions ({@code gtceu:void_miner_mk2}, {@code gtceu:void_miner_mk3}) created
 * by KubeJS ({@code kubejs/startup_scripts/gtceu/void_mining.js}); {@link #install} wires them up here just like the
 * base miner — machine supplier, tooltip lines, and each with its own recipe-type set
 * ({@link VoidMinerMachineMK2#RECIPE_TYPES}, {@link VoidMinerMachineMK3#RECIPE_TYPES}). Their tier behaviour (MK2:
 * 2x output and power at half the duration, MK3: 3x at a third) is data in the KubeJS recipes
 * ({@code kubejs/server_scripts/mods/gtceu/miner.js}), never in Java.
 * <p>
 * It mines standing in it: every recipe carries GT's dimension condition ({@code miner.js}), so a mode only runs in
 * its own dimension — Overworld also in the Mining Dimension, Asteroids in either belt. That makes the dimension
 * the mode: when the structure forms, the miner switches itself to the area of the dimension it stands in
 * ({@link #selectArea}); there is nothing to set by hand. A formed miner in none of the four dimensions reports
 * {@link ConsoleWidget#STATUS_NO_DIMENSION}.
 * <ul>
 * <li>Its own screen in the Orbital Lithography Station's layout: {@link VoidMinerConsoleWidget} in a
 * {@link SidePanelsUIWidget}. It lists the area's ores by their circuit ({@link #oreChart}) and says what a run still
 * waits for: the circuit in the buses ({@link #circuitSet}), the drilling fluid in the hatches
 * ({@link #fluidAvailable}).</li>
 * <li>Its recipe pages name the area ({@link #registerRecipeInfo}).</li>
 * </ul>
 */
public class VoidMinerMachine extends ProcessMachine {

    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(
            VoidMinerMachine.class, ProcessMachine.MANAGED_FIELD_HOLDER);

    private static final int[] COLORS = { 0xFF4ADE80, 0xFFEF4444, 0xFFFBBF24, 0xFFC084FC };
    /** The miner's recipe types, in mode order (gtceu namespace). */
    public static final String[] RECIPE_TYPES = { "void_mining_overworld", "void_mining_nether", "void_mining_end",
            "void_mining_asteroids" };
    /**
     * The dimensions each area mines in, in area order: the lists of the recipes' dimension conditions
     * ({@code kubejs/server_scripts/mods/gtceu/miner.js}). Change both together.
     */
    public static final String[][] AREA_DIMENSIONS = {
            { "minecraft:overworld", "allthemodium:mining" },
            { "minecraft:the_nether" },
            { "minecraft:the_end" },
            { "af9:asteroid_field", "af9:ceres" } };
    /** Ticks after which the ore chart is read anew (the recipes change with a data reload). */
    private static final int CHART_REFRESH = 100;
    /** Runs completed, for the screen's counter. */
    @Persisted
    private long runs;
    /** The recipes of the active area by their circuit, and the chart the screen lists them in. */
    private final TreeMap<Integer, GTRecipe> chartRecipes = new TreeMap<>();
    private GTRecipeType chartType;
    private long chartTime;
    private String chart = "";
    /** The circuit in the buses as of this tick (the screen asks several times a tick). */
    private long circuitTime = -1;
    private int circuitNow = -1;

    /**
     * The miner's own programmed circuit, set from the button above the void button on its screen (the mouse wheel on it
     * turns the number): the recipes take it like one in an input bus, so the miner needs no bus for it.
     */
    @Persisted
    protected final NotifiableItemStackHandler circuitSlot;

    public VoidMinerMachine(IMachineBlockEntity holder) {
        super(holder);
        this.circuitSlot = new NotifiableItemStackHandler(this, 1, IO.IN, IO.NONE)
                .setFilter(IntCircuitBehaviour::isIntegratedCircuit).shouldSearchContent(false);
    }

    /** The circuit button, on top of the void button (the panel stacks the buttons in the order they are added). */
    @Override
    public void attachConfigurators(ConfiguratorPanel configuratorPanel) {
        configuratorPanel.attachConfigurators(new CircuitFancyConfigurator(circuitSlot.storage));
        super.attachConfigurators(configuratorPanel);
    }


    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    @Override
    public String titleKey() {
        return "af9.voidminer.console.title";
    }

    @Override
    public int modeColor(int index) {
        return modeColorOf(index);
    }

    /**
     * The area (0-3, the order of {@link #RECIPE_TYPES}) a recipe type mines, whichever miner runs it (the
     * {@code _mk2} / {@code _mk3} copies included), or -1 for a type that is none of the four (the pack's old one).
     */
    public static int areaOf(GTRecipeType type) {
        String path = type.registryName.getPath().replaceAll("_mk[23]$", "");
        for (int i = 0; i < RECIPE_TYPES.length; i++) {
            if (RECIPE_TYPES[i].equals(path)) return i;
        }
        return -1;
    }

    /**
     * The index, among this miner's recipe types, of the area of the dimension it stands in; -1 when that is none
     * of the four.
     */
    public int typeIndexHere() {
        var level = getLevel();
        if (level == null) return -1;
        String dimension = level.dimension().location().toString();
        GTRecipeType[] types = getRecipeTypes();
        for (int i = 0; i < types.length; i++) {
            int area = areaOf(types[i]);
            if (area < 0) continue;
            for (String id : AREA_DIMENSIONS[area]) {
                if (id.equals(dimension)) return i;
            }
        }
        return -1;
    }

    /**
     * Switches to the area of the dimension the miner stands in, then lets the recipe logic look again. Without it
     * the miner would stay on its first recipe type (the base miner's is the pack's old one, which has no recipes).
     */
    public void selectArea() {
        if (isRemote()) return;
        int wanted = typeIndexHere();
        if (wanted >= 0 && wanted != getActiveRecipeType()) {
            setActiveRecipeType(wanted);
            getRecipeLogic().updateTickSubscription();
        }
    }

    @Override
    public void onStructureFormed() {
        super.onStructureFormed();
        selectArea();
    }

    /** ARGB colour of an area (recipe type index): overworld green, nether red, end amber, asteroids violet. */
    public static int modeColorOf(int index) {
        return COLORS[Math.max(0, Math.min(COLORS.length - 1, index))];
    }

    /**
     * Common setup, before any machine is created: takes over GT's void miner definition (block, pattern and
     * structure stay exactly as they are) and adds the four AF9 areas as its modes, this class as its machine and
     * AF9's tooltip lines. MK2 and MK3, separate KubeJS machine definitions, are wired up the same way, each with
     * its own recipe types and tooltip prefix.
     */
    @SuppressWarnings("removal") // new ResourceLocation(ns, path) is the only constructor on 1.20.1
    public static void install() {
        installVoidMiner("void_miner", VoidMinerMachine::new, "af9.void_miner.tooltip", RECIPE_TYPES);
        installVoidMiner("void_miner_mk2", VoidMinerMachineMK2::new, "af9.void_miner_mk2.tooltip",
                VoidMinerMachineMK2.RECIPE_TYPES);
        installVoidMiner("void_miner_mk3", VoidMinerMachineMK3::new, "af9.void_miner_mk3.tooltip",
                VoidMinerMachineMK3.RECIPE_TYPES);
    }

    /**
     * Installs a void miner variant with the given id, machine constructor, tooltip prefix and recipe types: the
     * variant's own types are appended to whatever its definition already runs (the base miner keeps ATM9's), and
     * its machine and tooltip lines are set.
     */
    @SuppressWarnings("removal")
    private static void installVoidMiner(String id,
            java.util.function.Function<IMachineBlockEntity, ? extends VoidMinerMachine> constructor,
            String tooltipPrefix, String[] recipeTypes) {
        MachineDefinition definition = GTRegistries.MACHINES.get(new ResourceLocation("gtceu", id));
        if (!(definition instanceof MultiblockMachineDefinition multiblock)) {
            AF9Core.LOGGER.warn("gtceu:{} is not a multiblock - is the base pack loaded?", id);
            return;
        }
        List<GTRecipeType> types = new ArrayList<>(Arrays.asList(multiblock.getRecipeTypes()));
        for (String path : recipeTypes) {
            GTRecipeType type = GTRegistries.RECIPE_TYPES.get(new ResourceLocation("gtceu", path));
            if (type == null) {
                AF9Core.LOGGER.warn("Recipe type gtceu:{} not found - is the AF9 KubeJS startup script loaded?",
                        path);
                continue;
            }
            if (!types.contains(type)) types.add(type);
        }
        multiblock.setRecipeTypes(types.toArray(GTRecipeType[]::new));
        // Use lambda to properly cast to Function<IMachineBlockEntity, MetaMachine>
        multiblock.setMachineSupplier(holder -> constructor.apply(holder));
        var tooltips = multiblock.getTooltipBuilder();
        multiblock.setTooltipBuilder((stack, lines) -> {
            if (tooltips != null) tooltips.accept(stack, lines);
            for (int i = 0; i < 4; i++) lines.add(Component.translatable(tooltipPrefix + "." + i));
        });
    }

    /**
     * Common setup: each variant's own recipe types get their area named on their EMI / JEI pages, and that
     * variant's controller as their icon where GT left none.
     */
    @SuppressWarnings("removal") // new ResourceLocation(ns, path) is the only constructor on 1.20.1
    public static void registerRecipeInfo() {
        registerRecipeInfoFor("void_miner", RECIPE_TYPES);
        registerRecipeInfoFor("void_miner_mk2", VoidMinerMachineMK2.RECIPE_TYPES);
        registerRecipeInfoFor("void_miner_mk3", VoidMinerMachineMK3.RECIPE_TYPES);
    }

    /** The recipe types already given their data info; each type is registered only once. */
    private static final Set<String> RECIPE_INFO_DONE = new HashSet<>();

    /**
     * Registers recipe info for a void miner variant: every recipe type it runs gets its area named on its
     * EMI / JEI page and the variant's controller as its icon where GT left none.
     */
    @SuppressWarnings("removal")
    private static void registerRecipeInfoFor(String id, String[] recipeTypes) {
        MachineDefinition definition = GTRegistries.MACHINES.get(new ResourceLocation("gtceu", id));
        for (String path : recipeTypes) {
            GTRecipeType type = GTRegistries.RECIPE_TYPES.get(new ResourceLocation("gtceu", path));
            if (type == null) {
                AF9Core.LOGGER.warn("Recipe type gtceu:{} not found - is the AF9 KubeJS startup script loaded?",
                        path);
                continue;
            }
            if (RECIPE_INFO_DONE.add(path)) {
                VoidMiningRecipeUI.install(type);
                // rendered as plain labels, so the texts must not contain '%'
                type.addDataInfo(data -> Component.translatable("af9.recipe.voidminer.area",
                        Component.translatable(ProcessMachine.modeKey(type) + ".short")).getString());
            }
            if (type.getIconSupplier() == null && definition != null) type.setIconSupplier(definition::asStack);
        }
    }

    //////////////////////////////////////
    // *********** Runs ************//
    //////////////////////////////////////

    @Override
    public int getStatus() {
        int status = super.getStatus();
        if ((status == ConsoleWidget.STATUS_IDLE || status == ConsoleWidget.STATUS_NO_POWER) && typeIndexHere() < 0) {
            return ConsoleWidget.STATUS_NO_DIMENSION;
        }
        return status;
    }

    @Override
    public void afterWorking() {
        super.afterWorking();
        runs++;
    }

    public long getRuns() {
        return runs;
    }

    public void resetRuns() {
        runs = 0;
    }

    /** EU of one run: of the running recipe as it runs, else of the recipe the circuit in the buses picks. */
    public long getEnergyPerRun() {
        GTRecipe recipe = getRecipeLogic().isWorking() ? getRecipeLogic().getLastRecipe() : selectedRecipe();
        return recipe == null ? 0 : RecipeHelper.getRealEUt(recipe).getTotalEU() * recipe.duration;
    }

    //////////////////////////////////////
    // ********* Ore chart **********//
    //////////////////////////////////////

    /** Reads the active area's recipes by their circuit, when the area changed or the last look is old. */
    private void refreshChart() {
        GTRecipeType type = getRecipeType();
        long now = getOffsetTimer();
        if (type == chartType && now >= chartTime && now - chartTime < CHART_REFRESH) return;
        chartType = type;
        chartTime = now;
        chartRecipes.clear();
        for (GTRecipe recipe : StagedRecipes.allRecipes(type)) {
            int circuit = circuitOf(recipe);
            if (circuit >= 0) chartRecipes.putIfAbsent(circuit, recipe);
        }
        StringJoiner entries = new StringJoiner(";");
        for (var entry : chartRecipes.entrySet()) {
            StringJoiner ores = new StringJoiner(",");
            for (Content content : entry.getValue().outputs.getOrDefault(ItemRecipeCapability.CAP, List.of())) {
                ItemStack[] stacks = ItemRecipeCapability.CAP.of(content.content).getItems();
                if (stacks.length == 0 || stacks[0].isEmpty()) continue;
                ResourceLocation id = ForgeRegistries.ITEMS.getKey(stacks[0].getItem());
                if (id == null) continue;
                int percent = content.maxChance <= 0 ? 100 : Math.round(100F * content.chance / content.maxChance);
                ores.add(id + "*" + stacks[0].getCount() + "*" + percent);
            }
            entries.add(entry.getKey() + "=" + ores);
        }
        chart = entries.toString();
    }

    /**
     * The ores of the active area, for the screen: "circuit=id*count*percent,..." per recipe, joined by ";", in the
     * order of the circuits. The percent is the chance a run brings that stack.
     */
    public String oreChart() {
        refreshChart();
        return chart;
    }

    /** The programmed circuit a recipe asks for, -1 when it asks for none. */
    private static int circuitOf(GTRecipe recipe) {
        for (Content content : recipe.inputs.getOrDefault(ItemRecipeCapability.CAP, List.of())) {
            for (ItemStack stack : ItemRecipeCapability.CAP.of(content.content).getItems()) {
                if (IntCircuitBehaviour.isIntegratedCircuit(stack)) {
                    return IntCircuitBehaviour.getCircuitConfiguration(stack);
                }
            }
        }
        return -1;
    }

    /** The programmed circuit in the input buses (their circuit slots included), -1 when there is none. */
    public int circuitSet() {
        long now = getOffsetTimer();
        if (now != circuitTime) {
            circuitTime = now;
            circuitNow = findCircuit();
        }
        return circuitNow;
    }

    private int findCircuit() {
        if (!isFormed()) return -1;
        for (IRecipeHandler<?> handler : getCapabilitiesFlat(IO.IN, ItemRecipeCapability.CAP)) {
            for (Object content : handler.getContents()) {
                if (content instanceof ItemStack stack && IntCircuitBehaviour.isIntegratedCircuit(stack)) {
                    return IntCircuitBehaviour.getCircuitConfiguration(stack);
                }
            }
        }
        return -1;
    }

    /** The circuit of the running recipe, -1 while nothing runs. */
    public int circuitRunning() {
        GTRecipe recipe = getRecipeLogic().getLastRecipe();
        return recipe == null || !getRecipeLogic().isWorking() ? -1 : circuitOf(recipe);
    }

    /** The recipe of the active area the circuit in the buses picks, null when there is none. */
    private GTRecipe selectedRecipe() {
        refreshChart();
        return chartRecipes.get(circuitSet());
    }

    /** The voltage tier of the recipe the circuit in the buses picks, -1 when it picks none. */
    public int tierNeeded() {
        GTRecipe recipe = selectedRecipe();
        return recipe == null ? -1 : RecipeHelper.getRecipeEUtTier(recipe);
    }

    /** EU/t of the running recipe, else of the one the circuit in the buses picks. */
    @Override
    public long getNeededEUt() {
        if (getRecipeLogic().isWorking()) return super.getNeededEUt();
        GTRecipe recipe = selectedRecipe();
        return recipe == null ? 0 : RecipeHelper.getRealEUt(recipe).getTotalEU();
    }

    /** The recipe the fluid figures are of: the selected one, else any of the area (they all drill alike). */
    private GTRecipe fluidRecipe() {
        GTRecipe recipe = selectedRecipe();
        return recipe != null || chartRecipes.isEmpty() ? recipe : chartRecipes.firstEntry().getValue();
    }

    /** mB of drilling fluid a run takes, 0 when the area has no recipe. */
    public long fluidNeeded() {
        GTRecipe recipe = fluidRecipe();
        if (recipe == null) return 0;
        long amount = 0;
        for (Content content : recipe.inputs.getOrDefault(FluidRecipeCapability.CAP, List.of())) {
            amount += FluidRecipeCapability.CAP.of(content.content).getAmount();
        }
        return amount;
    }

    /** mB of the fluid a run takes in the input hatches. */
    public long fluidAvailable() {
        GTRecipe recipe = fluidRecipe();
        if (recipe == null || !isFormed()) return 0;
        List<FluidIngredient> wanted = new ArrayList<>();
        for (Content content : recipe.inputs.getOrDefault(FluidRecipeCapability.CAP, List.of())) {
            wanted.add(FluidRecipeCapability.CAP.of(content.content));
        }
        long amount = 0;
        for (IRecipeHandler<?> handler : getCapabilitiesFlat(IO.IN, FluidRecipeCapability.CAP)) {
            for (Object content : handler.getContents()) {
                if (!(content instanceof FluidStack stack)) continue;
                for (FluidIngredient ingredient : wanted) {
                    if (ingredient.test(stack)) {
                        amount += stack.getAmount();
                        break;
                    }
                }
            }
        }
        return amount;
    }

    //////////////////////////////////////
    // *********** Screen **********//
    //////////////////////////////////////

    @Override
    public Widget createUIWidget() {
        return VoidMinerConsoleWidget.createPage(this);
    }

    /** GT's machine screen with the miner's page and a panel on each side of the player inventory. */
    @Override
    public ModularUI createUI(Player entityPlayer) {
        return new ModularUI(SidePanelsUIWidget.width(VoidMinerConsoleWidget.WIDTH),
                SidePanelsUIWidget.height(VoidMinerConsoleWidget.HEIGHT), this, entityPlayer)
                .widget(new SidePanelsUIWidget<>(this, VoidMinerConsoleWidget.WIDTH,
                        VoidMinerConsoleWidget.HEIGHT, VoidMinerConsoleWidget.class,
                        VoidMinerConsoleWidget.SidePanel::new));
    }

    @Override
    public List<Component> infoLines() {
        List<Component> lines = new ArrayList<>();
        GTRecipeType[] types = getRecipeTypes();
        int active = Math.max(0, Math.min(types.length - 1, getActiveRecipeType()));
        lines.add(Component.translatable(ProcessMachine.modeKey(types[active]) + ".short"));
        boolean running = getRecipeLogic().isWorking();
        lines.add(Component.translatable(running ? "af9.voidminer.console.mining" :
                "af9.console.status." + getStatus()));
        lines.add(Component.translatable("af9.voidminer.console.runs", ConsoleWidget.compact(runs)));
        return lines;
    }
}
