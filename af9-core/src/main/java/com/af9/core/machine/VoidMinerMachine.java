package com.af9.core.machine;

import com.af9.core.AF9Core;
import com.af9.core.machine.console.ConsoleWidget;
import com.af9.core.machine.console.SidePanelsUIWidget;
import com.af9.core.machine.console.VoidMinerConsoleWidget;

import com.gregtechceu.gtceu.api.capability.recipe.ItemRecipeCapability;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.MachineDefinition;
import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.GTRecipeType;
import com.gregtechceu.gtceu.api.recipe.RecipeHelper;
import com.gregtechceu.gtceu.api.recipe.content.Content;
import com.gregtechceu.gtceu.api.registry.GTRegistries;

import com.lowdragmc.lowdraglib.gui.modular.ModularUI;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringJoiner;

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
 * {@link SidePanelsUIWidget}.</li>
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
    /** Runs completed, for the screen's counter. */
    @Persisted
    private long runs;

    public VoidMinerMachine(IMachineBlockEntity holder) {
        super(holder);
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

    /** EU of one run of the running (or last) recipe. */
    public long getEnergyPerRun() {
        GTRecipe recipe = getRecipeLogic().getLastRecipe();
        return recipe == null ? 0 : RecipeHelper.getRealEUt(recipe).getTotalEU() * recipe.duration;
    }

    /**
     * The items of the recipe the screen shows (the running one, else the last run of the active mode), "id*count"
     * joined by ";": its inputs or its outputs, at most three.
     */
    public String shownRecipeItems(boolean inputs) {
        GTRecipe recipe = getRecipeLogic().getLastRecipe();
        if (recipe == null || recipe.recipeType != getRecipeType()) return "";
        List<Content> contents = (inputs ? recipe.inputs : recipe.outputs)
                .getOrDefault(ItemRecipeCapability.CAP, List.of());
        StringJoiner joined = new StringJoiner(";");
        int shown = 0;
        for (Content content : contents) {
            if (shown >= 3) break;
            ItemStack[] stacks = ItemRecipeCapability.CAP.of(content.content).getItems();
            if (stacks.length == 0 || stacks[0].isEmpty()) continue;
            ResourceLocation id = ForgeRegistries.ITEMS.getKey(stacks[0].getItem());
            if (id == null) continue;
            joined.add(id + "*" + stacks[0].getCount());
            shown++;
        }
        return joined.toString();
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
