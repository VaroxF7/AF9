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
import java.util.List;
import java.util.StringJoiner;

/**
 * The Void Miner, rebuilt: GT's controller block and structure stay exactly as they are ({@link #install} only
 * extends the definition), AF9 supplies the behaviour around them. Four areas as machine modes (Overworld, Nether,
 * End, Asteroids; recipe types {@code void_mining_*}, defined in
 * {@code kubejs/startup_scripts/gtceu/void_mining.js}): a programmed circuit picks the ore in the area, drilling
 * fluid goes in, tenfold raw ore comes out for the same time and energy. No data sticks.
 * <p>
 * It mines standing in it: every recipe carries GT's dimension condition ({@code miner.js}), so a mode only runs in
 * its own dimension — Overworld also in the Mining Dimension, Asteroids in either belt. A formed miner in the wrong
 * dimension reports {@link ConsoleWidget#STATUS_NO_DIMENSION}.
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
    /** Their lang keys ("gtceu.&lt;type&gt;"; "+ .short" is the short name, "+ .desc" the description). */
    public static final String[] RECIPE_TYPE_KEYS = { "gtceu.void_mining_overworld", "gtceu.void_mining_nether",
            "gtceu.void_mining_end", "gtceu.void_mining_asteroids" };
    /**
     * The dimensions a mode mines in, in mode order (dimension ids): the same lists the recipes' dimension
     * conditions name ({@code kubejs/server_scripts/mods/gtceu/miner.js}). Change both together; the screen reads
     * this side.
     */
    public static final String[][] MODE_DIMENSIONS = {
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

    /** ARGB colour of an area (recipe type index): overworld green, nether red, end amber, asteroids violet. */
    public static int modeColorOf(int index) {
        return COLORS[Math.max(0, Math.min(COLORS.length - 1, index))];
    }

    /**
     * Common setup, before any machine is created: takes over GT's void miner definition (block, pattern and
     * structure stay exactly as they are) and adds the four AF9 areas as its modes, this class as its machine and
     * AF9's tooltips.
     */
    @SuppressWarnings("removal") // new ResourceLocation(ns, path) is the only constructor on 1.20.1
    public static void install() {
        MachineDefinition definition = GTRegistries.MACHINES.get(new ResourceLocation("gtceu", "void_miner"));
        if (!(definition instanceof MultiblockMachineDefinition multiblock)) {
            AF9Core.LOGGER.warn("gtceu:void_miner is not a multiblock - is the base pack loaded?");
            return;
        }
        List<GTRecipeType> types = new ArrayList<>(Arrays.asList(multiblock.getRecipeTypes()));
        for (String path : RECIPE_TYPES) {
            GTRecipeType type = GTRegistries.RECIPE_TYPES.get(new ResourceLocation("gtceu", path));
            if (type == null) {
                AF9Core.LOGGER.warn("Recipe type gtceu:{} not found - is the AF9 KubeJS startup script loaded?",
                        path);
                continue;
            }
            if (!types.contains(type)) types.add(type);
        }
        multiblock.setRecipeTypes(types.toArray(GTRecipeType[]::new));
        multiblock.setMachineSupplier(VoidMinerMachine::new);
        var tooltips = multiblock.getTooltipBuilder();
        multiblock.setTooltipBuilder((stack, lines) -> {
            if (tooltips != null) tooltips.accept(stack, lines);
            for (int i = 0; i < 4; i++) lines.add(Component.translatable("af9.void_miner.tooltip." + i));
        });
    }

    /**
     * Common setup: the miner's recipe types get their area named on their EMI / JEI pages, and the controller as
     * their icon where GT left none.
     */
    @SuppressWarnings("removal") // new ResourceLocation(ns, path) is the only constructor on 1.20.1
    public static void registerRecipeInfo() {
        MachineDefinition definition = GTRegistries.MACHINES.get(new ResourceLocation("gtceu", "void_miner"));
        for (String path : RECIPE_TYPES) {
            GTRecipeType type = GTRegistries.RECIPE_TYPES.get(new ResourceLocation("gtceu", path));
            if (type == null) {
                AF9Core.LOGGER.warn("Recipe type gtceu:{} not found - is the AF9 KubeJS startup script loaded?",
                        path);
                continue;
            }
            // rendered as plain labels, so the texts must not contain '%'
            type.addDataInfo(data -> Component.translatable("af9.recipe.voidminer.area",
                    Component.translatable(ProcessMachine.modeKey(type) + ".short")).getString());
            type.addDataInfo(data -> Component.translatable("af9.recipe.voidminer.chance").getString());
            if (type.getIconSupplier() == null && definition != null) type.setIconSupplier(definition::asStack);
        }
    }

    //////////////////////////////////////
    // *********** Runs ************//
    //////////////////////////////////////

    /** Whether the miner stands in a dimension its active area mines in ({@link #MODE_DIMENSIONS}). */
    public boolean dimensionMatches() {
        GTRecipeType[] types = getRecipeTypes();
        if (types.length == 0) return true;
        int active = Math.max(0, Math.min(types.length - 1, getActiveRecipeType()));
        String path = types[active].registryName.getPath();
        String[] allowed = null;
        for (int i = 0; i < RECIPE_TYPES.length && i < MODE_DIMENSIONS.length; i++) {
            if (RECIPE_TYPES[i].equals(path)) {
                allowed = MODE_DIMENSIONS[i];
                break;
            }
        }
        if (allowed == null) return true;
        var level = getLevel();
        if (level == null) return true;
        String dimension = level.dimension().location().toString();
        for (String id : allowed) {
            if (id.equals(dimension)) return true;
        }
        return false;
    }

    @Override
    public int getStatus() {
        int status = super.getStatus();
        if ((status == ConsoleWidget.STATUS_IDLE || status == ConsoleWidget.STATUS_NO_POWER) && !dimensionMatches()) {
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
