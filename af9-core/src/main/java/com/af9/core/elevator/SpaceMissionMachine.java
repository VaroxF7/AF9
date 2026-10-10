package com.af9.core.elevator;

import com.af9.core.AF9Core;
import com.af9.core.common.IPowerGated;
import com.af9.core.machine.console.ConsoleWidget;
import com.af9.core.machine.console.SidePanelsUIWidget;
import com.af9.core.machine.console.SpaceElevatorConsoleWidget;
import com.af9.core.machine.part.CoolantHatchPartMachine;
import com.af9.core.registry.AF9Blocks;
import com.af9.core.registry.AF9Items;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.block.MetaMachineBlock;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.capability.recipe.FluidRecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.capability.recipe.IRecipeHandler;
import com.gregtechceu.gtceu.api.capability.recipe.ItemRecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.RecipeCapability;
import com.gregtechceu.gtceu.api.gui.fancy.TabsWidget;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import com.gregtechceu.gtceu.api.machine.TickableSubscription;
import com.gregtechceu.gtceu.api.machine.fancyconfigurator.CombinedDirectionalFancyConfigurator;
import com.gregtechceu.gtceu.api.machine.feature.IMachineLife;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableItemStackHandler;
import com.gregtechceu.gtceu.api.pattern.BlockPattern;
import com.gregtechceu.gtceu.api.pattern.MultiblockShapeInfo;
import com.gregtechceu.gtceu.api.pattern.MultiblockWorldSavedData;
import com.gregtechceu.gtceu.api.pattern.TraceabilityPredicate;
import com.gregtechceu.gtceu.api.pattern.error.PatternStringError;
import com.gregtechceu.gtceu.api.pattern.predicates.PredicateBlocks;
import com.gregtechceu.gtceu.api.pattern.predicates.SimplePredicate;
import com.gregtechceu.gtceu.api.pattern.util.PatternMatchContext;
import com.gregtechceu.gtceu.api.pattern.util.RelativeDirection;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.GTRecipeType;
import com.gregtechceu.gtceu.api.recipe.RecipeHelper;
import com.gregtechceu.gtceu.utils.GTUtil;
import com.gregtechceu.gtceu.api.recipe.chance.logic.ChanceLogic;
import com.gregtechceu.gtceu.api.recipe.content.Content;
import com.gregtechceu.gtceu.api.recipe.content.ContentModifier;
import com.gregtechceu.gtceu.api.recipe.ingredient.EnergyStack;
import com.gregtechceu.gtceu.api.recipe.ingredient.FluidIngredient;
import com.gregtechceu.gtceu.api.recipe.ingredient.SizedIngredient;
import com.gregtechceu.gtceu.api.recipe.modifier.ModifierFunction;
import com.gregtechceu.gtceu.api.recipe.modifier.ParallelLogic;
import com.gregtechceu.gtceu.api.recipe.modifier.RecipeModifier;
import com.gregtechceu.gtceu.api.registry.GTRegistries;
import com.gregtechceu.gtceu.common.data.GTMaterials;

import com.lowdragmc.lowdraglib.gui.modular.ModularUI;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;
import com.lowdragmc.lowdraglib.utils.BlockInfo;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.ForgeRegistries;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * What flies a Space Elevator's missions: the Mining Modules (GTNH's elevator modules, {@link SpaceModuleMachine}), each a small
 * multiblock of its own. A Mining Drone (not used up) in the drone slot of its screen ({@link #droneSlot}; or in an input
 * bus of the module's slot), hydrogen in the fluid hatches, a supercooled coolant in the Coolant Hatches and a very great deal of
 * energy (from the elevator it stands in) send expeditions to a random asteroid; when they are back the output buses hold the
 * ore, tens of stacks of raw ore.
 * <p>
 * The asteroid is made from one of GT's ore veins (weighted by the vein's weight, among the veins of the drone's tier and
 * below; the best drone also finds the exotic asteroid, whose ores no vein holds): about half the stacks are the vein's
 * main ore, the rest are shared by its other ores. Re-rolled for every run ({@link #MISSION}); the recipes themselves
 * (KubeJS: {@code server_scripts/mods/gtceu/space_elevator.js}) only name the drone, the fluids and the energy.
 * <p>
 * Instead of the asteroids a mission can go to a planet and bring home a fluid: a <b>liquid mission</b>
 * ({@link PlanetCatalog}: GTNH's Space Pumping table), picked on the screen ({@link #setMission}). It takes the same
 * drone, hydrogen, coolant and energy as the drone's ore mission; its fluid goes to the fluid output hatches. A module flies one
 * kind of mission at a time: the kind is its recipe type that is on ({@code space_mining} or {@code space_pumping}).
 * <p>
 * Its screen is a console in the Orbital Lithography Station's layout ({@link SpaceElevatorConsoleWidget}); what it shows,
 * and above all what the module lacks when nothing flies, comes from {@link #getStatus()}.
 */
@SuppressWarnings("removal") // new ResourceLocation(ns, path) is the only constructor on 1.20.1
public abstract class SpaceMissionMachine extends WorkableElectricMultiblockMachine implements IPowerGated, IMachineLife {

    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(
            SpaceMissionMachine.class, WorkableElectricMultiblockMachine.MANAGED_FIELD_HOLDER);

    /** Stacks of ore an expedition of each drone tier brings home: at least and at most. */
    private static final int[] MIN_STACKS = { 8, 12, 16, 24 };
    private static final int[] MAX_STACKS = { 16, 24, 32, 48 };
    private static final int STACK = 64;
    /** The share of the stacks of the vein's main ore. */
    private static final double MAIN_SHARE = 0.5;
    /** How many of the exotic ores one exotic asteroid holds, and its share of the Mk4 draws (1 in this many). */
    private static final int EXOTIC_ORES = 3;
    private static final int EXOTIC_ONE_IN = 6;
    private static final String DRONE = AF9Items.DRONE;
    /** The recipe type of the expeditions ({@code gtceu:space_mining}, from the startup script). */
    public static final String RECIPE_TYPE = "space_mining";
    /** The recipe type of the liquid missions ({@code gtceu:space_pumping}): the same flights, for a planet's fluid. */
    public static final String LIQUID_RECIPE_TYPE = "space_pumping";
    /** Recipe data key of the asteroid a run flies to (the vein it is made from, or "exotic"). */
    public static final String ASTEROID_TAG = "af9_asteroid";
    /** Recipe data key of the fluid a liquid run brings ({@link PlanetCatalog.Cargo#code()}). */
    public static final String CARGO_TAG = "af9_cargo";
    /** Recipe data key that marks an ore expedition whose asteroid the recipe's circuit picks. */
    public static final String EXPEDITION_TAG = "af9_expedition";

    /**
     * The Mining Drone slot of the elevator's screen: a recipe input (GT reads the controller's own handlers as it
     * reads an input bus), kept there: a drone is not used up. No pipe access (capability IO NONE), which also makes the
     * handler itself refuse inserts: the screen's slot works on its {@code storage}.
     */
    @Persisted
    public final NotifiableItemStackHandler droneSlot;
    /**
     * The fluid picked for the liquid missions: GTNH's planet type and gas type ({@link PlanetCatalog}), 0 and 0 while
     * the elevator mines the asteroids.
     */
    @Persisted
    private int planetType;
    @Persisted
    private int gasType;
    /**
     * Expeditions flown, items of ore and millibuckets of fluid brought home since the counters were reset (the screen
     * shows them).
     */
    @Persisted
    private long flown;
    @Persisted
    private long mined;
    @Persisted
    private long pumped;

    /** The cargo of the run tried last had no room in the outputs: the run waits for room (the screen says so). */
    private boolean outputFull;
    /**
     * The programmed circuit of the module, set with the button on the left of its screen: its number picks the ore of the
     * drone's tier (the recipe with that circuit). A recipe input like a circuit in a bus, so the module needs no bus for it.
     */
    @Persisted
    protected final NotifiableItemStackHandler circuitSlot;

    protected SpaceMissionMachine(IMachineBlockEntity holder) {
        super(holder);
        droneSlot = new NotifiableItemStackHandler(this, 1, IO.IN, IO.NONE).setFilter(SpaceMissionMachine::isDrone);
        circuitSlot = new NotifiableItemStackHandler(this, 1, IO.IN, IO.NONE)
                .setFilter(com.gregtechceu.gtceu.common.item.IntCircuitBehaviour::isIntegratedCircuit).shouldSearchContent(false);
    }

    /**
     * GT matches a recipe inside one colour group of input handlers: the hatches and buses are in the group of their paint
     * (the default paint is a colour), the machine's own slots in the undyed one. The drone and the circuit of the screen are
     * in the second, the drill head and the crate in a bus in the first: no group has them all and the recipe never starts
     * ("Insufficient Inputs: Item" for the drone and the circuit). The undyed lists take the paint of the first painted one.
     */
    @Override
    protected void collectRecipeHandlers() {
        super.collectRecipeHandlers();
        alignInputGroups();
    }

    /** Puts the undyed input lists (the machine's own slots) into the group of the painted ones. */
    protected void alignInputGroups() {
        var lists = getCapabilitiesProxy().get(IO.IN);
        if (lists == null || lists.isEmpty()) return;
        int color = -1;
        for (var list : lists) {
            if (list.getColor() != -1 && !list.isDistinct()) {
                color = list.getColor();
                break;
            }
        }
        if (color == -1) return;
        for (var list : lists) {
            if (list.getColor() == -1 && !list.isDistinct()) list.setColor(color);
        }
    }

    /** The circuit button, on top of the others on the left of the screen. */
    @Override
    public void attachConfigurators(com.gregtechceu.gtceu.api.gui.fancy.ConfiguratorPanel configuratorPanel) {
        configuratorPanel.attachConfigurators(
                new com.gregtechceu.gtceu.api.machine.fancyconfigurator.CircuitFancyConfigurator(circuitSlot.storage));
        super.attachConfigurators(configuratorPanel);
    }

    /** The programmed circuit set on the screen, -1 for none. */
    public int circuitSet() {
        ItemStack stack = circuitSlot.getStackInSlot(0);
        return com.gregtechceu.gtceu.common.item.IntCircuitBehaviour.isIntegratedCircuit(stack) ? com.gregtechceu.gtceu.common.item.IntCircuitBehaviour.getCircuitConfiguration(stack) : -1;
    }

    /** The circuit a recipe asks for, -1 for none. */
    public static int circuitOf(GTRecipe recipe) {
        for (Content content : recipe.inputs.getOrDefault(ItemRecipeCapability.CAP, List.of())) {
            for (ItemStack stack : ItemRecipeCapability.CAP.of(content.content).getItems()) {
                if (com.gregtechceu.gtceu.common.item.IntCircuitBehaviour.isIntegratedCircuit(stack)) return com.gregtechceu.gtceu.common.item.IntCircuitBehaviour.getCircuitConfiguration(stack);
            }
        }
        return -1;
    }

    /** Whether an ore expedition of the drone and the circuit set exists. */
    public boolean hasOreRecipe(int drone) {
        Level level = getLevel();
        if (level == null || drone < 1) return false;
        int circuit = circuitSet();
        for (GTRecipe recipe : level.getRecipeManager().getAllRecipesFor(getRecipeType())) {
            if (droneTier(recipe) == drone && circuitOf(recipe) == circuit &&
                    circuit <= OreCatalog.veinIds(drone).size()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Whether the item inputs an ore expedition of the drone and the circuit set uses up (the drill head and the crate) are in
     * the item input buses. The drone and the circuit are kept, they are not counted.
     */
    public boolean hasPartsFor(int drone) {
        Level level = getLevel();
        if (level == null || drone < 1) return false;
        int circuit = circuitSet();
        for (GTRecipe recipe : level.getRecipeManager().getAllRecipesFor(getRecipeType())) {
            if (droneTier(recipe) != drone || circuitOf(recipe) != circuit) continue;
            for (Content content : recipe.inputs.getOrDefault(ItemRecipeCapability.CAP, List.of())) {
                if (content.chance == 0) continue;
                Ingredient ingredient = ItemRecipeCapability.CAP.of(content.content);
                if (ingredient.isEmpty()) continue;
                int needed = ingredient instanceof SizedIngredient sized ? sized.getAmount() : 1;
                long have = 0;
                for (IRecipeHandler<?> handler : getCapabilitiesFlat(IO.IN, ItemRecipeCapability.CAP)) {
                    for (Object held : handler.getContents()) {
                        if (held instanceof ItemStack stack && ingredient.test(stack)) have += stack.getCount();
                    }
                }
                if (have < needed) return false;
            }
            return true;
        }
        return true;
    }

    /** Broken controller: the drone drops. */
    @Override
    public void onMachineRemoved() {
        clearInventory(droneSlot.storage);
    }

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    /**
     * Common setup: the missions get their own EMI / JEI pages ({@link SpaceMiningRecipeUI}: the drone and the fluids
     * piped into the tower, and the ores the drone's asteroids hold or the fluids of the planets it reaches).
     */
    public static void registerRecipeInfo() {
        for (String name : new String[] { RECIPE_TYPE, LIQUID_RECIPE_TYPE }) {
            GTRecipeType type = GTRegistries.RECIPE_TYPES.get(new ResourceLocation("gtceu", name));
            if (type == null) {
                AF9Core.LOGGER.warn("Recipe type gtceu:{} not found", name);
                continue;
            }
            SpaceMiningRecipeUI.install(type, name.equals(LIQUID_RECIPE_TYPE));
            if (!name.equals(LIQUID_RECIPE_TYPE)) {
                type.addDataInfo(data -> Component.translatable("af9.recipe.space_mining.module",
                        data.getInt("af9_module")).getString());
            }
        }
    }

    /** The voltage the machine overclocks to: the hatches' (a module: those of its tower), 0 for none. */
    public long overclockVoltage() {
        return 0;
    }

    /** Re-modify every run: each expedition goes to a new asteroid, and the mission may have been changed. */
    @Override
    public boolean alwaysTryModifyRecipe() {
        return true;
    }

    // what the elevator the module stands in tells (the screen shows it; GTNH's modules read it off their parent)

    /** Whether the cable has open sky above it (something built over a formed tower stops the expeditions). */
    public abstract boolean isSkyClear();

    /** Expeditions this machine flies at once: its tier's when the motors power it, 0 when nothing flies. */
    public abstract int getExpeditions();

    public abstract int getMotorTier();

    public abstract int getModules();

    public abstract int getPoweredModules();

    public abstract int getTopModule();

    public abstract boolean isExtended();

    public abstract void setExtended(boolean extended);

    /** How far above its rest the climber is now (the screen draws it), 0 where there is none. */
    public abstract float climberHeight(float partialTick);

    //////////////////////////////////////
    // ************ Drone *************//
    //////////////////////////////////////

    /** A Mining Drone ({@code af9:space_mining_drone_mk<n>}). */
    public static boolean isDrone(ItemStack stack) {
        return droneTierOf(stack) > 0;
    }

    /** The tier of a Mining Drone (1 to 4), 0 for anything else. */
    public static int droneTierOf(ItemStack stack) {
        if (stack.isEmpty()) return 0;
        ResourceLocation key = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (key == null || !key.getNamespace().equals(AF9Core.MOD_ID) || !key.getPath().startsWith(DRONE)) return 0;
        try {
            return Integer.parseInt(key.getPath().substring(DRONE.length()));
        } catch (NumberFormatException exception) {
            return 0;
        }
    }

    /** Whether the drone slot of the screen holds a drone. */
    public boolean isDroneInSlot() {
        return isDrone(droneSlot.getStackInSlot(0));
    }

    /**
     * The tier of the drone that flies: the one in the screen's slot; with that slot empty, the best one in the input
     * buses. 0: there is none. Only this drone's expedition runs ({@link #ASTEROID}), so a second drone in a bus does
     * not make GT pick between them.
     */
    public int chosenDrone() {
        int slot = droneTierOf(droneSlot.getStackInSlot(0));
        if (slot > 0 || !isFormed()) return slot;
        int best = 0;
        for (IRecipeHandler<?> handler : getCapabilitiesFlat(IO.IN, ItemRecipeCapability.CAP)) {
            for (Object content : handler.getContents()) {
                if (content instanceof ItemStack stack) best = Math.max(best, droneTierOf(stack));
            }
        }
        return best;
    }

    /** What one expedition of a drone tier takes: its recipe's inputs. */
    public record Expedition(int tier, int hydrogen, Fluid coolant, int coolantAmount, long voltage, long amperage,
                             int duration) {

        public long eut() {
            return voltage * amperage;
        }
    }

    /**
     * The expedition of a drone tier, read from its recipe (server and client both hold the recipes); null when no
     * recipe sends that drone.
     */
    public Expedition expedition(int tier) {
        Level level = getLevel();
        if (level == null || tier < 1) return null;
        for (GTRecipe recipe : level.getRecipeManager().getAllRecipesFor(getRecipeType())) {
            if (droneTier(recipe) != tier) continue;
            int hydrogen = 0, coolantAmount = 0;
            Fluid coolant = Fluids.EMPTY;
            for (Content content : recipe.inputs.getOrDefault(FluidRecipeCapability.CAP, List.of())) {
                FluidStack[] stacks = FluidRecipeCapability.CAP.of(content.content).getStacks();
                if (stacks.length == 0) continue;
                if (isHydrogen(stacks[0].getFluid())) {
                    hydrogen += stacks[0].getAmount();
                } else {
                    coolant = stacks[0].getFluid();
                    coolantAmount += stacks[0].getAmount();
                }
            }
            EnergyStack energy = RecipeHelper.getRealEUt(recipe);
            return new Expedition(tier, hydrogen, coolant, coolantAmount, energy.voltage(), energy.amperage(),
                    recipe.duration);
        }
        return null;
    }

    /** The expeditions' fuel. */
    public static boolean isHydrogen(Fluid fluid) {
        return fluid.isSame(GTMaterials.Hydrogen.getFluid());
    }

    /**
     * Millibuckets of a fluid in the fluid input hatches (hydrogen: {@link #isHydrogen}), the Coolant Hatches among
     * them.
     */
    public long stockOf(Fluid fluid) {
        if (!isFormed()) return 0;
        long total = 0;
        for (IRecipeHandler<?> handler : getCapabilitiesFlat(IO.IN, FluidRecipeCapability.CAP)) {
            for (Object content : handler.getContents()) {
                if (content instanceof FluidStack stack && stack.getFluid().isSame(fluid)) total += stack.getAmount();
            }
        }
        return total;
    }

    //////////////////////////////////////
    // *********** Mission ************//
    //////////////////////////////////////

    /** Whether the mission that is picked is a liquid one: the recipe type that is on says so. */
    public boolean isLiquidMission() {
        return LIQUID_RECIPE_TYPE.equals(getRecipeType().registryName.getPath());
    }

    /** GTNH's planet type and gas type of the fluid picked for the liquid missions, 0 while the asteroids are mined. */
    public int getPlanetType() {
        return planetType;
    }

    public int getGasType() {
        return gasType;
    }

    /** The fluid a liquid mission brings: the one picked on the screen. Null on an ore mission. */
    public PlanetCatalog.Cargo chosenCargo() {
        if (!isLiquidMission()) return null;
        // the planet is picked on the screen, its fluid by the programmed circuit (the n-th of the planet's, the first without one)
        List<PlanetCatalog.Cargo> cargoes = PlanetCatalog.of(planetType);
        if (cargoes.isEmpty()) return null;
        return cargoes.get(Math.max(0, Math.min(circuitSet() - 1, cargoes.size() - 1)));
    }

    /**
     * Picks the mission: a planet type and a gas type of {@link PlanetCatalog} for a liquid mission, anything else for
     * the asteroids. A run that is on flies to its end; what was drawn for the mission before does not start.
     */
    public void setMission(int planet, int gas) {
        PlanetCatalog.Cargo cargo = PlanetCatalog.find(planet, gas);
        int type = typeIndex(cargo == null ? RECIPE_TYPE : LIQUID_RECIPE_TYPE);
        // no such recipe type: the startup script is not loaded
        if (type < 0) return;
        planetType = cargo == null ? 0 : planet;
        gasType = cargo == null ? 0 : gas;
        setActiveRecipeType(type);
        outputFull = false;
        recipeLogic.markLastRecipeDirty();
        markDirty();
    }

    private int typeIndex(String name) {
        GTRecipeType[] types = getRecipeTypes();
        for (int i = 0; i < types.length; i++) {
            if (types[i].registryName.getPath().equals(name)) return i;
        }
        return -1;
    }

    /** The screen's target selector: the asteroids, then the planet types from the nearest, round and round. */
    public void cycleTarget(int step) {
        List<Integer> planets = PlanetCatalog.planets();
        int index = isLiquidMission() ? planets.indexOf(planetType) + 1 : 0;
        index = Math.floorMod(index + step, planets.size() + 1);
        if (index == 0) {
            setMission(0, 0);
        } else {
            int planet = planets.get(index - 1);
            setMission(planet, PlanetCatalog.of(planet).get(0).gas());
        }
    }

    /** The screen's fluid selector: the fluids of the planet type that is picked, round and round. */
    public void cycleCargo(int step) {
        PlanetCatalog.Cargo cargo = chosenCargo();
        if (cargo == null) return;
        List<PlanetCatalog.Cargo> cargoes = PlanetCatalog.of(planetType);
        int index = Math.floorMod(cargoes.indexOf(cargo) + step, cargoes.size());
        setMission(planetType, cargoes.get(index).gas());
    }

    /** GT's own mode tab (it would switch the recipe type) is left out: the mission is picked on the console. */
    @Override
    public void attachSideTabs(TabsWidget sideTabs) {
        sideTabs.setMainTab(this);
        var directional = CombinedDirectionalFancyConfigurator.of(self(), self());
        if (directional != null) sideTabs.attachSubTab(directional);
        sideTabs.attachSubTab(new com.gregtechceu.gtceu.api.machine.fancyconfigurator.PowerManagementFancyTab(this));
    }

    //////////////////////////////////////
    // ************ Status ************//
    //////////////////////////////////////

    /**
     * What the elevator is doing or, while nothing flies, the first thing it lacks ({@link ConsoleWidget}'s status
     * codes): its console shows it and names what to do about it. In the order a run needs things: the structure, the
     * switch, the sky above the cable, a powered module, a drone (for a liquid mission one that reaches the planet), the
     * energy and the fluids of one expedition, room for its ore or its fluid.
     */
    public int getStatus() {
        if (!isFormed()) return ConsoleWidget.STATUS_OFFLINE;
        if (!recipeLogic.isWorkingEnabled()) return ConsoleWidget.STATUS_PAUSED;
        if (recipeLogic.isWorking()) return ConsoleWidget.STATUS_RUNNING;
        // a run that is on and out of energy
        if (recipeLogic.isWaiting()) return ConsoleWidget.STATUS_NO_POWER;
        if (!isSkyClear()) return ConsoleWidget.STATUS_NO_SKY;
        if (getExpeditions() <= 0) return ConsoleWidget.STATUS_NO_MODULE;
        int drone = chosenDrone();
        Expedition needs = expedition(drone);
        if (needs == null) return ConsoleWidget.STATUS_NO_DRONE;
        boolean liquid = isLiquidMission();
        if (liquid) {
            // the planet that is picked lies beyond the drone
            PlanetCatalog.Cargo cargo = chosenCargo();
            if (cargo == null || cargo.drone() > drone) return ConsoleWidget.STATUS_NO_DRONE;
        }
        if (getAvailableEUt() < needs.eut()) return ConsoleWidget.STATUS_NO_POWER;
        if (stockOf(GTMaterials.Hydrogen.getFluid()) < needs.hydrogen()) return ConsoleWidget.STATUS_NO_FUEL;
        if (stockOf(needs.coolant()) < needs.coolantAmount()) return ConsoleWidget.STATUS_NO_COOLANT;
        if (!liquid && !hasOreRecipe(drone)) return ConsoleWidget.STATUS_NO_DATA;
        // the drill head and the crate an ore expedition uses up, in an item input bus of the tower or of the module
        if (!liquid && !hasPartsFor(drone)) return ConsoleWidget.STATUS_NO_PARTS;
        RecipeCapability<?> kind = liquid ? FluidRecipeCapability.CAP : ItemRecipeCapability.CAP;
        if (outputFull || getCapabilitiesFlat(IO.OUT, kind).isEmpty()) return ConsoleWidget.STATUS_OUTPUT_FULL;
        return ConsoleWidget.STATUS_IDLE;
    }

    /** Expeditions of the run that is on, 0 while nothing flies. */
    public int flyingExpeditions() {
        GTRecipe run = recipeLogic.isWorking() ? recipeLogic.getLastRecipe() : null;
        return run == null ? 0 : Math.max(1, run.parallels);
    }

    /** The asteroid of the run that is on (the vein it is made from, or "exotic"), "" while nothing flies. */
    public String flyingAsteroid() {
        GTRecipe run = recipeLogic.isWorking() ? recipeLogic.getLastRecipe() : null;
        return run == null ? "" : run.data.getString(ASTEROID_TAG);
    }

    /** The fluid of the liquid run that is on ({@link PlanetCatalog.Cargo#code()}), 0 on an ore run or with none. */
    public int flyingCargo() {
        GTRecipe run = recipeLogic.isWorking() ? recipeLogic.getLastRecipe() : null;
        return run == null ? 0 : run.data.getInt(CARGO_TAG);
    }

    /** The ore of the run that is on, for the screen: "id*count;id*count" ("" while nothing flies). */
    public String flyingOre() {
        GTRecipe run = recipeLogic.isWorking() ? recipeLogic.getLastRecipe() : null;
        if (run == null) return "";
        StringBuilder text = new StringBuilder();
        for (Content content : run.outputs.getOrDefault(ItemRecipeCapability.CAP, List.of())) {
            Ingredient ingredient = ItemRecipeCapability.CAP.of(content.content);
            ItemStack[] items = ingredient.getItems();
            if (items.length == 0) continue;
            ResourceLocation id = ForgeRegistries.ITEMS.getKey(items[0].getItem());
            if (id == null) continue;
            if (text.length() > 0) text.append(';');
            text.append(id).append('*').append(countOf(ingredient));
        }
        return text.toString();
    }

    private static int countOf(Ingredient ingredient) {
        if (ingredient instanceof SizedIngredient sized) return sized.getAmount();
        ItemStack[] items = ingredient.getItems();
        return items.length == 0 ? 0 : items[0].getCount();
    }

    /** Expeditions flown since the counters were reset. */
    public long getFlown() {
        return flown;
    }

    /** Items of ore brought home since the counters were reset. */
    public long getMined() {
        return mined;
    }

    /** Millibuckets of fluid brought home since the counters were reset. */
    public long getPumped() {
        return pumped;
    }

    public void resetCounters() {
        flown = 0;
        mined = 0;
        pumped = 0;
        markDirty();
    }

    /** A run is over: its expeditions and their ore or fluid go on the counters. */
    @Override
    public void afterWorking() {
        super.afterWorking();
        GTRecipe run = recipeLogic.getLastRecipe();
        if (run == null) return;
        flown += Math.max(1, run.parallels);
        for (Content content : run.outputs.getOrDefault(ItemRecipeCapability.CAP, List.of())) {
            mined += countOf(ItemRecipeCapability.CAP.of(content.content));
        }
        for (Content content : run.outputs.getOrDefault(FluidRecipeCapability.CAP, List.of())) {
            pumped += FluidRecipeCapability.CAP.of(content.content).getAmount();
        }
        markDirty();
    }

    //////////////////////////////////////
    // *********** Missions ***********//
    //////////////////////////////////////

    /**
     * Starts a run of as many missions at once as the powered Mining Modules fly, the hatches can supply in full
     * (EU/t), the hydrogen and the coolant last for and the outputs have room for, while the cable is free, and gives
     * it its cargo (the recipe's own outputs are dropped). An ore mission gets its asteroid: the ore the run puts out,
     * the same asteroid for every expedition of the run. A liquid mission gets the fluid picked on the screen,
     * {@link PlanetCatalog}'s amount of it an expedition, if the drone reaches its planet. Only the kind of mission
     * that is picked runs (the recipe type that is on), with the {@link #chosenDrone() chosen drone}.
     */
    public static final RecipeModifier MISSION = (machine, recipe) -> {
        if (!(machine instanceof SpaceMissionMachine elevator)) {
            return RecipeModifier.nullWrongType(SpaceMissionMachine.class, machine);
        }
        long eut = RecipeHelper.getRealEUt(recipe).getTotalEU();
        int tier = droneTier(recipe);
        if (tier < 1 || eut < 1 || recipe.recipeType != elevator.getRecipeType() || tier != elevator.chosenDrone() ||
                !elevator.isSkyClear()) {
            return ModifierFunction.NULL;
        }
        // a module flies the drones of its own tier and below (the MK-3 module also the MK-4 drone)
        if (elevator instanceof SpaceModuleMachine module && module.getModuleTier() < Math.min(tier, 3)) {
            return ModifierFunction.NULL;
        }
        boolean liquid = elevator.isLiquidMission();
        PlanetCatalog.Cargo cargo = elevator.chosenCargo();
        if (liquid && (cargo == null || cargo.drone() > tier)) return ModifierFunction.NULL;
        // one expedition a run, no parallels: more modules fly more expeditions, a faster run comes from overclocking
        if (elevator.getExpeditions() < 1 || elevator.getAvailableEUt() < eut) return ModifierFunction.NULL;
        int runs = 1;
        // the cargo of one expedition
        GTRecipe one = recipe.copy();
        List<ItemStack> ores = List.of();
        StringBuilder where = new StringBuilder();
        if (liquid) {
            // a run's fluid is one stack: an int of millibuckets
            runs = Math.min(runs, Integer.MAX_VALUE / cargo.millibuckets());
            one.outputs.put(FluidRecipeCapability.CAP, List.of(content(cargo, 1)));
        } else {
            RandomSource random = elevator.getLevel() != null ? elevator.getLevel().getRandom() :
                    RandomSource.create();
            ores = recipeOre(recipe, random, where);
            if (ores == null) ores = asteroid(tier, random, where);
            if (ores.isEmpty()) return ModifierFunction.NULL;
            one.outputs.put(ItemRecipeCapability.CAP, contents(ores, 1));
        }
        // as many of them as the outputs can take the cargo of; not even one: the run waits for room
        runs = ParallelLogic.limitByOutputMerging(elevator, one, runs, elevator::canVoidRecipeOutputs, List.of());
        elevator.outputFull = runs < 1;
        if (runs < 1) return ModifierFunction.NULL;
        String name = where.toString();
        int code = liquid ? cargo.code() : 0;
        RecipeCapability<?> kind = liquid ? FluidRecipeCapability.CAP : ItemRecipeCapability.CAP;
        List<Content> outputs = liquid ? List.of(content(cargo, runs)) : contents(ores, runs);
        // overclocked like any GT machine, as far as the tower's hatches go in voltage and carry in power: each overclock
        // four times the EU/t (the voltage of the amps stays the amps) and half the time; the inputs stay the same
        int recipeTier = GTUtil.getTierByVoltage(RecipeHelper.getRealEUt(recipe).voltage());
        int maxTier = GTUtil.getOCTierByVoltage(elevator.overclockVoltage());
        int overclocks = 0;
        while (recipeTier + overclocks < maxTier && recipe.duration >> (overclocks + 1) >= 1 &&
                (double) eut * Math.pow(4, overclocks + 1) <= elevator.getAvailableEUt()) {
            overclocks++;
        }
        ModifierFunction parallel = ModifierFunction.builder()
                .eutMultiplier(Math.pow(4, overclocks))
                .durationMultiplier(Math.pow(0.5, overclocks))
                .parallels(1)
                .build();
        return modified -> {
            GTRecipe result = parallel.apply(modified);
            if (result == null) return null;
            result.outputs.put(kind, outputs);
            // where the run goes is carried with it (the builder shares the data tag with the recipe it was made from)
            result.data = result.data.copy();
            if (liquid) result.data.putInt(CARGO_TAG, code);
            else result.data.putString(ASTEROID_TAG, name);
            return result;
        };
    };

    /** A liquid mission's fluid as a recipe's fluid output, {@code times} over. */
    private static Content content(PlanetCatalog.Cargo cargo, int times) {
        return new Content(FluidIngredient.of(new FluidStack(cargo.fluid(), cargo.millibuckets() * times)),
                ChanceLogic.getMaxChancedValue(), ChanceLogic.getMaxChancedValue(), 0);
    }

    /** Stacks of ore an expedition of a drone tier brings home at least, and at most. */
    public static int minStacks(int tier) {
        return MIN_STACKS[Math.max(1, Math.min(tier, MIN_STACKS.length)) - 1];
    }

    public static int maxStacks(int tier) {
        return MAX_STACKS[Math.max(1, Math.min(tier, MAX_STACKS.length)) - 1];
    }

    /**
     * The asteroid of a recipe that names its own (data {@code af9_vein}, and {@code af9_min} / {@code af9_max} stacks): that
     * vein's ores in the drone tier's number of stacks, about half of them the main ore. Null for a recipe that does not,
     * which then flies to a random asteroid.
     */
    private static List<ItemStack> recipeOre(GTRecipe recipe, RandomSource random, StringBuilder name) {
        if (!recipe.data.contains(EXPEDITION_TAG)) return null;
        // the n-th asteroid of the drone's tier, n the recipe's circuit
        List<String> veins = OreCatalog.veinIds(droneTier(recipe));
        int circuit = circuitOf(recipe);
        if (circuit < 1 || circuit > veins.size()) return List.of();
        String id = veins.get(circuit - 1);
        int tier = Math.min(Math.max(droneTier(recipe), 1), MIN_STACKS.length);
        int min = MIN_STACKS[tier - 1];
        int max = MAX_STACKS[tier - 1];
        int stacks = min + random.nextInt(max - min + 1);
        List<String> materials = new ArrayList<>(OreCatalog.veinMaterials(id));
        if (OreCatalog.EXOTIC.equals(id)) {
            List<String> bag = new ArrayList<>(materials);
            materials.clear();
            while (materials.size() < EXOTIC_ORES && !bag.isEmpty()) materials.add(bag.remove(random.nextInt(bag.size())));
        }
        name.append(id);
        List<ItemStack> result = new ArrayList<>();
        int main = materials.size() <= 1 ? stacks : Math.max(1, (int) Math.round(stacks * MAIN_SHARE));
        int others = Math.max(1, materials.size() - 1);
        for (int i = 0; i < materials.size(); i++) {
            int share = i == 0 ? main : (stacks - main) / others;
            if (i == 1) share += (stacks - main) - share * others;
            ItemStack ore = share < 1 ? null : OreCatalog.ore(materials.get(i), share * STACK);
            if (ore != null) result.add(ore);
        }
        return result;
    }

    /** The ore of an asteroid as a recipe's item outputs, {@code times} over. */
    private static List<Content> contents(List<ItemStack> ores, int times) {
        List<Content> result = new ArrayList<>();
        for (ItemStack ore : ores) {
            result.add(new Content(SizedIngredient.create(ore.copyWithCount(ore.getCount() * times)),
                    ChanceLogic.getMaxChancedValue(), ChanceLogic.getMaxChancedValue(), 0));
        }
        return result;
    }

    /** The tier of the Mining Drone a recipe takes ({@code space_mining_drone_mk<n>}), 0 if there is none. */
    public static int droneTier(GTRecipe recipe) {
        for (Content content : recipe.inputs.getOrDefault(ItemRecipeCapability.CAP, List.of())) {
            if (!(content.content instanceof Ingredient ingredient)) continue;
            for (ItemStack stack : ingredient.getItems()) {
                int tier = droneTierOf(stack);
                if (tier > 0) return tier;
            }
        }
        return 0;
    }

    /**
     * The ore of one expedition of a drone tier, one stack an ore (its count the whole of it, stacks of 64 times);
     * {@code name} gets where it went.
     */
    static List<ItemStack> asteroid(int tier, RandomSource random, StringBuilder name) {
        int index = Math.min(tier, MIN_STACKS.length) - 1;
        int stacks = MIN_STACKS[index] + random.nextInt(MAX_STACKS[index] - MIN_STACKS[index] + 1);
        List<String> materials = new ArrayList<>();
        List<OreCatalog.Vein> pool = new ArrayList<>();
        int weight = 0;
        for (OreCatalog.Vein vein : OreCatalog.veins()) {
            if (vein.tier() <= tier) {
                pool.add(vein);
                weight += vein.weight();
            }
        }
        List<String> exotics = OreCatalog.exotics();
        boolean exotic = !exotics.isEmpty() &&
                (pool.isEmpty() || tier >= OreCatalog.TIERS && random.nextInt(EXOTIC_ONE_IN) == 0);
        if (exotic) {
            name.append("exotic");
            List<String> bag = new ArrayList<>(exotics);
            while (materials.size() < EXOTIC_ORES && !bag.isEmpty()) {
                materials.add(bag.remove(random.nextInt(bag.size())));
            }
        } else if (!pool.isEmpty()) {
            int roll = random.nextInt(weight);
            OreCatalog.Vein picked = pool.get(0);
            for (OreCatalog.Vein vein : pool) {
                if (roll < vein.weight()) {
                    picked = vein;
                    break;
                }
                roll -= vein.weight();
            }
            name.append(picked.id());
            materials.addAll(picked.materials());
        }
        List<ItemStack> result = new ArrayList<>();
        int main = materials.size() <= 1 ? stacks : Math.max(1, (int) Math.round(stacks * MAIN_SHARE));
        int others = Math.max(1, materials.size() - 1);
        for (int i = 0; i < materials.size(); i++) {
            int share = i == 0 ? main : (stacks - main) / others;
            if (i == 1) share += (stacks - main) - share * others;
            // nothing when GT has no item of the ore, or the ore got no stack
            ItemStack ore = share < 1 ? null : OreCatalog.ore(materials.get(i), share * STACK);
            if (ore != null) result.add(ore);
        }
        return result;
    }


    //////////////////////////////////////
    // ************ Screen ************//
    //////////////////////////////////////

    @Override
    public Widget createUIWidget() {
        return SpaceElevatorConsoleWidget.createPage(this);
    }

    /** GT's machine screen with the module's page and a panel on each side of the player inventory. */
    @Override
    public ModularUI createUI(Player entityPlayer) {
        return new ModularUI(SidePanelsUIWidget.width(SpaceElevatorConsoleWidget.WIDTH),
                SidePanelsUIWidget.height(SpaceElevatorConsoleWidget.HEIGHT), this, entityPlayer)
                .widget(new SidePanelsUIWidget<>(this, SpaceElevatorConsoleWidget.WIDTH,
                        SpaceElevatorConsoleWidget.HEIGHT, SpaceElevatorConsoleWidget.class,
                        SpaceElevatorConsoleWidget.SidePanel::new));
    }
}
