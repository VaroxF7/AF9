package com.af9.core.elevator;

import com.af9.core.AF9Core;
import com.af9.core.common.IPowerGated;
import com.af9.core.machine.console.ConsoleWidget;
import com.af9.core.machine.console.SidePanelsUIWidget;
import com.af9.core.machine.console.SpaceElevatorConsoleWidget;
import com.af9.core.machine.part.CoolantHatchPartMachine;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.block.MetaMachineBlock;
import com.gregtechceu.gtceu.api.capability.recipe.FluidRecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.capability.recipe.IRecipeHandler;
import com.gregtechceu.gtceu.api.capability.recipe.ItemRecipeCapability;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import com.gregtechceu.gtceu.api.machine.TickableSubscription;
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
import com.gregtechceu.gtceu.api.recipe.chance.logic.ChanceLogic;
import com.gregtechceu.gtceu.api.recipe.content.Content;
import com.gregtechceu.gtceu.api.recipe.content.ContentModifier;
import com.gregtechceu.gtceu.api.recipe.ingredient.EnergyStack;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.IntStream;

/**
 * Space Elevator (GTNH's, docs/space-elevator.md): a tower on a cable that reaches into space. A Mining Drone (not used
 * up) in the drone slot of its screen ({@link #droneSlot}; or in an input bus), hydrogen in the fluid hatches, a
 * supercooled coolant in the Coolant Hatches (to GT fluid input hatches as the others: the recipe takes its fluids from
 * all of them) and a very great deal of energy send an expedition to a random asteroid; when it is back the output
 * buses hold its ore, tens of stacks of raw ore.
 * <p>
 * The structure is GTNH's (the startup script {@code startup_scripts/gtceu/space_elevator.js} holds it): its blocks
 * with rules of their own are checked here. The {@link #motors() motors} round the shaft are all of one tier, the
 * elevator's; the {@link #cable() cable} block on top of the shaft needs open sky above it; the module slots hold
 * {@link #modules() Mining Modules}. It has GTNH's two sizes: the basic tower, and the extended one with a ring round
 * its foot and twelve more module slots, switched on the screen ({@link #setExtended}; {@link #getPattern()}).
 * <p>
 * As in GTNH the modules do the work: a Mining Module flies 2, 4 or 8 expeditions at once (MK-I to MK-III), each with the
 * recipe's full hydrogen, coolant and energy, all to the same asteroid. The motors' tier powers 6, 12, 15, 18 or 24 module
 * slots, and only modules of its own tier or lower. Without a powered module nothing flies.
 * <p>
 * The asteroid is made from one of GT's ore veins (weighted by the vein's weight, among the veins of the drone's tier and
 * below; the best drone also finds the exotic asteroid, whose ores no vein holds): about half the stacks are the vein's
 * main ore, the rest are shared by its other ores. Re-rolled for every run ({@link #ASTEROID}); the recipes themselves
 * (KubeJS: {@code server_scripts/mods/gtceu/space_elevator.js}) only name the drone, the fluids and the energy.
 * <p>
 * While the structure is formed the cable runs up into the sky and the climber rides it as GTNH's does
 * ({@link ClimberRide}; drawn by {@link com.af9.core.client.render.SpaceElevatorRender}).
 * <p>
 * Its screen is a console in the Orbital Lithography Station's layout ({@link SpaceElevatorConsoleWidget}); what it
 * shows, and above all what the elevator lacks when nothing flies, comes from {@link #getStatus()}.
 */
@SuppressWarnings("removal") // new ResourceLocation(ns, path) is the only constructor on 1.20.1
public class SpaceElevatorMachine extends WorkableElectricMultiblockMachine implements ISpaceElevatorMachine,
        IPowerGated, IMachineLife {

    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(
            SpaceElevatorMachine.class, WorkableElectricMultiblockMachine.MANAGED_FIELD_HOLDER);

    /**
     * Where the cable is (the model's, space_elevator.js reads these): the Space Elevator Cable block sits
     * {@code CABLE_UP} blocks above the controller and {@code CABLE_BACK} behind it, on top of the motor shaft in the
     * middle of the tower.
     */
    public static final float CABLE_UP = 22, CABLE_BACK = 3;

    /** The motor tiers there are: {@code kubejs:space_elevator_motor_mk1} to {@code mk5}. */
    public static final int MOTOR_TIERS = 5;
    private static final String[] ROMAN = { "I", "II", "III", "IV", "V" };
    private static final String MOTOR = "space_elevator_motor_mk";
    private static final ResourceLocation CABLE = new ResourceLocation("kubejs", "space_elevator_cable");
    /** The Mining Module tiers there are: {@code kubejs:space_mining_module_mk1} to {@code mk3}. */
    public static final int MODULE_TIERS = 3;
    private static final String MODULE = "space_mining_module_mk";
    /** The pattern check's notes (GT's match context): the motor tier, the tiers of the modules found. */
    private static final String MOTOR_KEY = "SpaceElevatorMotor";
    private static final String MODULES_KEY = "SpaceElevatorModules";
    /** Module slots the motors of each tier power (GTNH's). */
    private static final int[] MODULE_SLOTS = { 6, 12, 15, 18, 24 };
    /** Expeditions a Mining Module of each tier flies at once (GTNH's parallels). */
    private static final int[] MODULE_EXPEDITIONS = { 2, 4, 8 };

    /** Stacks of ore an expedition of each drone tier brings home: at least and at most. */
    private static final int[] MIN_STACKS = { 8, 12, 16, 24 };
    private static final int[] MAX_STACKS = { 16, 24, 32, 48 };
    private static final int STACK = 64;
    /** The share of the stacks of the vein's main ore. */
    private static final double MAIN_SHARE = 0.5;
    /** How many of the exotic ores one exotic asteroid holds, and its share of the Mk4 draws (1 in this many). */
    private static final int EXOTIC_ORES = 3;
    private static final int EXOTIC_ONE_IN = 6;
    private static final String DRONE = "space_mining_drone_mk";
    /** The recipe type of the expeditions ({@code gtceu:space_mining}, from the startup script). */
    public static final String RECIPE_TYPE = "space_mining";
    /** Recipe data key of the asteroid a run flies to (the vein it is made from, or "exotic"). */
    public static final String ASTEROID_TAG = "af9_asteroid";

    /**
     * The pattern of the extended structure. The startup script builds it together with the basic one (the machine
     * definition's own pattern) and hands it over, the first time GT asks for the definition's pattern.
     */
    private static volatile BlockPattern extendedPattern;

    /** The size the structure is checked for: GTNH's extended elevator instead of the basic one. */
    @Persisted
    private boolean extended;
    /**
     * The climber's ride ({@link ClimberRide}): which one is on, the game time it began at, and how far the climber
     * had turned when it began (degrees). Synced: every client plays the ride from these.
     */
    @Persisted
    @DescSynced
    private int climberRide;
    @Persisted
    @DescSynced
    private long climberStart;
    @Persisted
    @DescSynced
    private float climberTurn;
    /** Whether the climber has come down to this tower: it comes down from orbit once, when the tower forms. */
    @Persisted
    private boolean climberDown;
    private TickableSubscription climberSubs;
    /**
     * The Mining Drone slot of the elevator's screen: a recipe input (GT reads the controller's own handlers as it
     * reads an input bus), kept there: a drone is not used up. No pipe access (capability IO NONE), which also makes the
     * handler itself refuse inserts: the screen's slot works on its {@code storage}.
     */
    @Persisted
    public final NotifiableItemStackHandler droneSlot;
    /** Expeditions flown and items of ore brought home since the counters were reset (the screen shows them). */
    @Persisted
    private long flown;
    @Persisted
    private long mined;
    /** The tier of the motors round the shaft (1 to 5), 0 while not formed. */
    private int motorTier;
    /** Mining Modules in the module slots, those of them the motors power, and the expeditions these fly at once. */
    private int modules, poweredModules, expeditions;
    /** The highest tier among the Mining Modules in the slots, 0 without one. */
    private int topModule;
    /** The asteroid drawn last had no room in the output buses: the run waits for room (the screen says so). */
    private boolean outputFull;
    /** The sky above the cable is looked at once a second: when, and what was seen. */
    private long skyChecked = Long.MIN_VALUE / 2;
    private boolean skyClear;

    public SpaceElevatorMachine(IMachineBlockEntity holder) {
        super(holder);
        droneSlot = new NotifiableItemStackHandler(this, 1, IO.IN, IO.NONE).setFilter(SpaceElevatorMachine::isDrone);
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
     * Common setup: the expeditions get their own EMI / JEI page ({@link SpaceMiningRecipeUI}: the drone and the fluids
     * piped into the tower, and the ores the drone's asteroids hold).
     */
    public static void registerRecipeInfo() {
        GTRecipeType type = GTRegistries.RECIPE_TYPES.get(new ResourceLocation("gtceu", RECIPE_TYPE));
        if (type == null) {
            AF9Core.LOGGER.warn("Recipe type gtceu:{} not found - is the AF9 KubeJS startup script loaded?",
                    RECIPE_TYPE);
            return;
        }
        SpaceMiningRecipeUI.install(type);
    }

    /** Re-modify every run: each expedition goes to a new asteroid. */
    @Override
    public boolean alwaysTryModifyRecipe() {
        return true;
    }

    @Override
    public long getAvailableEUt() {
        return energyContainer == null || !isFormed() ? 0 :
                energyContainer.getInputVoltage() * energyContainer.getInputAmperage();
    }

    @Override
    public boolean isElevatorFormed() {
        return isFormed();
    }

    //////////////////////////////////////
    // *********** Climber ************//
    //////////////////////////////////////

    /** Ticks into the ride that is on. */
    private float rideTicks(float partialTick) {
        Level level = getLevel();
        return level == null ? 0F : Math.max(0F, level.getGameTime() - climberStart + partialTick);
    }

    @Override
    public float climberHeight(float partialTick) {
        return ClimberRide.height(climberRide, rideTicks(partialTick));
    }

    @Override
    public float climberTurn(float partialTick) {
        return (climberTurn + ClimberRide.turn(climberRide, rideTicks(partialTick))) % 360F;
    }

    private void startRide(int ride) {
        Level level = getLevel();
        if (level == null) return;
        climberRide = ride;
        climberStart = level.getGameTime();
    }

    /**
     * Server, every tick of a formed tower: ends the ride that is over (the climber keeps the turn it made) and, while
     * the elevator is switched on, sends the climber up on a delivery every {@link ClimberRide#DELIVERY_INTERVAL} ticks.
     */
    private void climberTick() {
        Level level = getLevel();
        if (level == null || !isFormed()) return;
        if (climberRide != ClimberRide.NONE) {
            int duration = ClimberRide.duration(climberRide);
            if (level.getGameTime() - climberStart >= duration) {
                climberTurn = (climberTurn + ClimberRide.turn(climberRide, duration)) % 360F;
                climberRide = ClimberRide.NONE;
            }
        } else if (recipeLogic.isWorkingEnabled() && getOffsetTimer() % ClimberRide.DELIVERY_INTERVAL == 0) {
            startRide(ClimberRide.DELIVERY);
        }
    }

    private void unsubscribeClimber() {
        if (climberSubs != null) {
            climberSubs.unsubscribe();
            climberSubs = null;
        }
    }

    @Override
    public void onUnload() {
        super.onUnload();
        unsubscribeClimber();
    }

    //////////////////////////////////////
    // ********** Structure ***********//
    //////////////////////////////////////

    /** The startup script's: the pattern of the extended structure. */
    public static void setExtendedPattern(BlockPattern pattern) {
        extendedPattern = pattern;
    }

    /** The basic structure's pattern, or the extended one's while that size is switched on. */
    @Override
    public BlockPattern getPattern() {
        // the definition's first: building it is what makes the script hand over the extended one
        BlockPattern basic = super.getPattern();
        BlockPattern big = extendedPattern;
        return extended && big != null ? big : basic;
    }

    /** The two pages of the structure preview: the basic and the extended tower. */
    public static List<MultiblockShapeInfo> previews(MultiblockMachineDefinition definition) {
        List<MultiblockShapeInfo> pages = new ArrayList<>();
        pages.add(preview(definition.getPatternFactory().get()));
        BlockPattern big = extendedPattern;
        if (big != null) pages.add(preview(big));
        return pages;
    }

    private static MultiblockShapeInfo preview(BlockPattern pattern) {
        int[] once = new int[pattern.aisleRepetitions.length];
        Arrays.fill(once, 1);
        return new MultiblockShapeInfo(pattern.getPreview(once));
    }

    public boolean isExtended() {
        return extended;
    }

    /**
     * Switches between the basic and the extended structure (GTNH's size button). A formed tower is taken apart and
     * checked anew for the other size, as when its controller is turned; a run that is on is lost.
     */
    public void setExtended(boolean extended) {
        if (this.extended == extended) return;
        this.extended = extended;
        markDirty();
        if (isFormed() && getLevel() instanceof ServerLevel serverLevel) {
            onStructureInvalid();
            MultiblockWorldSavedData data = MultiblockWorldSavedData.getOrCreate(serverLevel);
            data.removeMapping(getMultiblockState());
            data.addAsyncLogic(this);
        }
    }

    /**
     * Puts the ZPM parts first among a predicate's candidates, then the better ones, then the lesser. The structure
     * preview shows a predicate's first block and the terminal builds with it, and GT lists parts from ULV up: a tower
     * built that way had ULV hatches (8 buckets, 16 EU/t), which hold nothing an expedition needs. What the predicate
     * accepts stays the same.
     */
    public static TraceabilityPredicate zpmFirst(TraceabilityPredicate predicate) {
        for (List<SimplePredicate> kind : List.of(predicate.limited, predicate.common)) {
            for (SimplePredicate simple : kind) {
                Supplier<BlockInfo[]> candidates = simple.candidates;
                if (candidates == null) continue;
                simple.candidates = () -> {
                    BlockInfo[] sorted = candidates.get().clone();
                    Arrays.sort(sorted, Comparator.comparingInt(info -> rank(info.getBlockState().getBlock())));
                    return sorted;
                };
            }
        }
        return predicate;
    }

    /**
     * Takes the Coolant Hatches out of a predicate of fluid input hatches (a Coolant Hatch is a fluid input hatch too):
     * in the elevator they are a part of their own with a maximum of their own, and are not to count twice. To be
     * called before {@link #zpmFirst}: the candidates are made anew here.
     */
    public static TraceabilityPredicate plainFluidHatches(TraceabilityPredicate predicate) {
        for (List<SimplePredicate> kind : List.of(predicate.limited, predicate.common)) {
            for (SimplePredicate simple : kind) {
                if (!(simple instanceof PredicateBlocks hatches)) continue;
                hatches.blocks = Arrays.stream(hatches.blocks)
                        .filter(block -> !CoolantHatchPartMachine.COOLANT_INPUT.isApplicable(block))
                        .toArray(Block[]::new);
                hatches.buildPredicate();
            }
        }
        return predicate;
    }

    /** ZPM first, then up, then down from LuV: a part's place among the candidates. */
    private static int rank(Block block) {
        int tier = block instanceof MetaMachineBlock machine ? machine.getDefinition().getTier() : GTValues.ZPM;
        return tier >= GTValues.ZPM ? tier - GTValues.ZPM : GTValues.MAX + GTValues.ZPM - tier;
    }

    /** A tiered block of the elevator ({@code kubejs:<prefix><tier>}, from the startup script), null when there is none. */
    private static Block tiered(String prefix, int tier) {
        Block block = ForgeRegistries.BLOCKS.getValue(new ResourceLocation("kubejs", prefix + tier));
        return block == null || block == Blocks.AIR ? null : block;
    }

    /** The tier of such a block (1 up), 0 for any other block. */
    private static int tierOf(BlockState state, String prefix) {
        ResourceLocation key = ForgeRegistries.BLOCKS.getKey(state.getBlock());
        if (key == null || !key.getNamespace().equals("kubejs") || !key.getPath().startsWith(prefix)) return 0;
        try {
            return Integer.parseInt(key.getPath().substring(prefix.length()));
        } catch (NumberFormatException exception) {
            return 0;
        }
    }

    private static BlockInfo[] candidates(String prefix, int tiers) {
        return IntStream.rangeClosed(1, tiers).mapToObj(tier -> tiered(prefix, tier)).filter(Objects::nonNull)
                .map(BlockInfo::fromBlock).toArray(BlockInfo[]::new);
    }

    /** The Space Elevator Motors: any tier, but all of the same one (it is the elevator's tier). */
    public static TraceabilityPredicate motors() {
        return new TraceabilityPredicate(state -> {
            int tier = tierOf(state.getBlockState(), MOTOR);
            if (tier < 1 || tier > MOTOR_TIERS) return false;
            int first = state.getMatchContext().getOrPut(MOTOR_KEY, tier);
            if (first != tier) {
                state.setError(new PatternStringError("af9.space_elevator.error.motors"));
                return false;
            }
            return true;
        }, () -> candidates(MOTOR, MOTOR_TIERS))
                .addTooltips(Component.translatable("af9.space_elevator.error.motors"));
    }

    /** A Mining Module in a module slot, any tier; the check notes them all down ({@link #onStructureFormed}). */
    public static TraceabilityPredicate modules() {
        return new TraceabilityPredicate(state -> {
            int tier = tierOf(state.getBlockState(), MODULE);
            if (tier < 1 || tier > MODULE_TIERS) return false;
            state.getMatchContext().getOrCreate(MODULES_KEY, IntArrayList::new).add(tier);
            return true;
        }, () -> candidates(MODULE, MODULE_TIERS))
                .addTooltips(Component.translatable("af9.space_elevator.pattern.module"));
    }

    /** The Space Elevator Cable, with nothing but air above it up to the world's top (GTNH: it must see the sky). */
    public static TraceabilityPredicate cable() {
        return new TraceabilityPredicate(state -> {
            Block cable = ForgeRegistries.BLOCKS.getValue(CABLE);
            if (cable == null || cable == Blocks.AIR || !state.getBlockState().is(cable)) return false;
            if (!seesSky(state.getWorld(), state.getPos())) {
                state.setError(new PatternStringError("af9.space_elevator.error.sky"));
                return false;
            }
            return true;
        }, () -> {
            Block cable = ForgeRegistries.BLOCKS.getValue(CABLE);
            return cable == null || cable == Blocks.AIR ? new BlockInfo[0] :
                    new BlockInfo[] { BlockInfo.fromBlock(cable) };
        }).addTooltips(Component.translatable("af9.space_elevator.error.sky"));
    }

    /**
     * Whether every block above {@code cable} is air. Only reads block states of the cable's own column, so GT's pattern
     * thread can call it (the column's chunk is loaded when the cable block is checked).
     */
    static boolean seesSky(Level level, BlockPos cable) {
        BlockPos.MutableBlockPos pos = cable.mutable();
        for (int y = cable.getY() + 1; y < level.getMaxBuildHeight(); y++) {
            pos.setY(y);
            if (!level.getBlockState(pos).isAir()) return false;
        }
        return true;
    }

    /** Where the cable block of this elevator is. */
    private BlockPos cablePos() {
        return RelativeDirection.offsetPos(getPos(), getFrontFacing(), getUpwardsFacing(), isFlipped(),
                (int) CABLE_UP, 0, -(int) CABLE_BACK);
    }

    /**
     * Whether the cable still has open sky above it (something built over a formed tower stops the expeditions). Looked
     * at once a second: the screen and a run that waits ask every tick.
     */
    public boolean isSkyClear() {
        Level level = getLevel();
        if (level == null || !isFormed()) return false;
        long now = level.getGameTime();
        if (now < skyChecked || now - skyChecked >= 20) {
            skyChecked = now;
            skyClear = seesSky(level, cablePos());
        }
        return skyClear;
    }

    @Override
    public void onStructureFormed() {
        super.onStructureFormed();
        PatternMatchContext context = getMultiblockState().getMatchContext();
        motorTier = context.getOrDefault(MOTOR_KEY, 0);
        IntList found = context.get(MODULES_KEY);
        countModules(found == null ? IntList.of() : found);
        if (getLevel() instanceof ServerLevel) {
            // a tower that forms calls its climber down from orbit; one that was formed before has it already
            if (!climberDown) {
                climberDown = true;
                startRide(ClimberRide.FORMATION);
            }
            climberSubs = subscribeServerTick(climberSubs, this::climberTick);
        }
    }

    @Override
    public void onStructureInvalid() {
        super.onStructureInvalid();
        motorTier = 0;
        modules = poweredModules = expeditions = topModule = 0;
        outputFull = false;
        skyChecked = Long.MIN_VALUE / 2;
        unsubscribeClimber();
        climberDown = false;
        climberRide = ClimberRide.NONE;
        climberTurn = 0F;
    }

    /** Module slots the motors of a tier power, 0 for no motors. */
    public static int moduleSlots(int motorTier) {
        return motorTier < 1 ? 0 : MODULE_SLOTS[Math.min(motorTier, MODULE_SLOTS.length) - 1];
    }

    /**
     * Which of the modules found the motors power: those of the motors' tier or lower, the best first, as many as the
     * motors have slots for (GTNH refuses a tower with more modules than slots; here the rest stands idle).
     */
    private void countModules(IntList tiers) {
        int slots = moduleSlots(motorTier);
        int[] ofTier = new int[MODULE_TIERS + 1];
        for (int i = 0; i < tiers.size(); i++) ofTier[tiers.getInt(i)]++;
        modules = tiers.size();
        poweredModules = 0;
        expeditions = 0;
        topModule = 0;
        for (int tier = MODULE_TIERS; tier >= 1 && topModule == 0; tier--) {
            if (ofTier[tier] > 0) topModule = tier;
        }
        for (int tier = Math.min(motorTier, MODULE_TIERS); tier >= 1 && poweredModules < slots; tier--) {
            int powered = Math.min(ofTier[tier], slots - poweredModules);
            poweredModules += powered;
            expeditions += powered * MODULE_EXPEDITIONS[tier - 1];
        }
    }

    /** A tier as the blocks and the drones are named: MK-I to MK-V ("-" for no tier). */
    public static String mark(int tier) {
        return tier < 1 || tier > ROMAN.length ? "-" : "MK-" + ROMAN[tier - 1];
    }

    /** The tier of the motors (1 to 5), 0 while not formed. */
    public int getMotorTier() {
        return motorTier;
    }

    /** Mining Modules in the module slots. */
    public int getModules() {
        return modules;
    }

    /** Those of them the motors power. */
    public int getPoweredModules() {
        return poweredModules;
    }

    /** Expeditions the powered modules fly at once. */
    public int getExpeditions() {
        return expeditions;
    }

    /** The highest tier among the Mining Modules in the slots (it needs motors of its tier), 0 without one. */
    public int getTopModule() {
        return topModule;
    }

    //////////////////////////////////////
    // ************ Drone *************//
    //////////////////////////////////////

    /** A Mining Drone ({@code kubejs:space_mining_drone_mk<n>}). */
    public static boolean isDrone(ItemStack stack) {
        return droneTierOf(stack) > 0;
    }

    /** The tier of a Mining Drone (1 to 4), 0 for anything else. */
    public static int droneTierOf(ItemStack stack) {
        if (stack.isEmpty()) return 0;
        ResourceLocation key = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (key == null || !key.getNamespace().equals("kubejs") || !key.getPath().startsWith(DRONE)) return 0;
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
    // ************ Status ************//
    //////////////////////////////////////

    /**
     * What the elevator is doing or, while nothing flies, the first thing it lacks ({@link ConsoleWidget}'s status
     * codes): its console shows it and names what to do about it. In the order a run needs things: the structure, the
     * switch, the sky above the cable, a powered module, a drone, the energy and the fluids of one expedition, room for
     * its ore.
     */
    public int getStatus() {
        if (!isFormed()) return ConsoleWidget.STATUS_OFFLINE;
        if (!recipeLogic.isWorkingEnabled()) return ConsoleWidget.STATUS_PAUSED;
        if (recipeLogic.isWorking()) return ConsoleWidget.STATUS_RUNNING;
        // a run that is on and out of energy
        if (recipeLogic.isWaiting()) return ConsoleWidget.STATUS_NO_POWER;
        if (!isSkyClear()) return ConsoleWidget.STATUS_NO_SKY;
        if (expeditions <= 0) return ConsoleWidget.STATUS_NO_MODULE;
        Expedition needs = expedition(chosenDrone());
        if (needs == null) return ConsoleWidget.STATUS_NO_DRONE;
        if (getAvailableEUt() < needs.eut()) return ConsoleWidget.STATUS_NO_POWER;
        if (stockOf(GTMaterials.Hydrogen.getFluid()) < needs.hydrogen()) return ConsoleWidget.STATUS_NO_FUEL;
        if (stockOf(needs.coolant()) < needs.coolantAmount()) return ConsoleWidget.STATUS_NO_COOLANT;
        if (outputFull || getCapabilitiesFlat(IO.OUT, ItemRecipeCapability.CAP).isEmpty()) {
            return ConsoleWidget.STATUS_OUTPUT_FULL;
        }
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

    public void resetCounters() {
        flown = 0;
        mined = 0;
        markDirty();
    }

    /** A run is over: its expeditions and their ore go on the counters. */
    @Override
    public void afterWorking() {
        super.afterWorking();
        GTRecipe run = recipeLogic.getLastRecipe();
        if (run == null) return;
        flown += Math.max(1, run.parallels);
        for (Content content : run.outputs.getOrDefault(ItemRecipeCapability.CAP, List.of())) {
            mined += countOf(ItemRecipeCapability.CAP.of(content.content));
        }
        markDirty();
    }

    //////////////////////////////////////
    // ********** Asteroids ***********//
    //////////////////////////////////////

    /**
     * Starts a run of as many expeditions at once as the powered Mining Modules fly, the hatches can supply in full
     * (EU/t), the hydrogen and the coolant last for and the output buses have room for, while the cable is free, and
     * gives it its asteroid: the ore the run puts out, the same asteroid for every expedition of the run (the recipe's
     * own outputs are dropped). Only the expedition of the {@link #chosenDrone() chosen drone} runs.
     */
    public static final RecipeModifier ASTEROID = (machine, recipe) -> {
        if (!(machine instanceof SpaceElevatorMachine elevator)) {
            return RecipeModifier.nullWrongType(SpaceElevatorMachine.class, machine);
        }
        long eut = RecipeHelper.getRealEUt(recipe).getTotalEU();
        int tier = droneTier(recipe);
        if (tier < 1 || eut < 1 || tier != elevator.chosenDrone() || !elevator.isSkyClear()) {
            return ModifierFunction.NULL;
        }
        int limit = (int) Math.min(elevator.expeditions, elevator.getAvailableEUt() / eut);
        if (limit < 1) return ModifierFunction.NULL;
        int runs = ParallelLogic.getParallelAmountWithoutEU(machine, recipe, limit);
        if (runs < 1) return ModifierFunction.NULL;
        RandomSource random = elevator.getLevel() != null ? elevator.getLevel().getRandom() : RandomSource.create();
        StringBuilder where = new StringBuilder();
        List<ItemStack> ores = asteroid(tier, random, where);
        if (ores.isEmpty()) return ModifierFunction.NULL;
        // as many of them as the output buses can take the ore of; not even one: the run waits for room
        GTRecipe one = recipe.copy();
        one.outputs.put(ItemRecipeCapability.CAP, contents(ores, 1));
        runs = ParallelLogic.limitByOutputMerging(elevator, one, runs, elevator::canVoidRecipeOutputs, List.of());
        elevator.outputFull = runs < 1;
        if (runs < 1) return ModifierFunction.NULL;
        String name = where.toString();
        List<Content> outputs = contents(ores, runs);
        // the inputs and the EU/t of every expedition (the drone is not used up: it is not multiplied)
        ModifierFunction parallel = ModifierFunction.builder()
                .modifyAllContents(ContentModifier.multiplier(runs))
                .eutMultiplier(runs)
                .parallels(runs)
                .build();
        return modified -> {
            GTRecipe result = parallel.apply(modified);
            if (result == null) return null;
            result.outputs.put(ItemRecipeCapability.CAP, outputs);
            // the run's asteroid goes with it (the builder shares the data tag with the recipe it was made from)
            result.data = result.data.copy();
            result.data.putString(ASTEROID_TAG, name);
            return result;
        };
    };

    /** Stacks of ore an expedition of a drone tier brings home at least, and at most. */
    public static int minStacks(int tier) {
        return MIN_STACKS[Math.max(1, Math.min(tier, MIN_STACKS.length)) - 1];
    }

    public static int maxStacks(int tier) {
        return MAX_STACKS[Math.max(1, Math.min(tier, MAX_STACKS.length)) - 1];
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

    /** GT's machine screen with the elevator's page and a panel on each side of the player inventory. */
    @Override
    public ModularUI createUI(Player entityPlayer) {
        return new ModularUI(SidePanelsUIWidget.width(SpaceElevatorConsoleWidget.WIDTH),
                SidePanelsUIWidget.height(SpaceElevatorConsoleWidget.HEIGHT), this, entityPlayer)
                .widget(new SidePanelsUIWidget<>(this, SpaceElevatorConsoleWidget.WIDTH,
                        SpaceElevatorConsoleWidget.HEIGHT, SpaceElevatorConsoleWidget.class,
                        SpaceElevatorConsoleWidget.SidePanel::new));
    }
}
