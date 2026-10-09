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
import com.lowdragmc.lowdraglib.gui.util.ClickData;
import com.lowdragmc.lowdraglib.gui.widget.ComponentPanelWidget;
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
import java.util.stream.IntStream;

/**
 * Space Elevator (GTNH's, docs/space-elevator.md): a tower on a cable that reaches into space. As in GTNH it does no work
 * itself: it is the structure that holds the Mining Modules ({@link SpaceModuleMachine}, small multiblocks in its module slots),
 * powers them from its energy hatches and lets them fly expeditions. The modules have the drone, the hydrogen and coolant
 * hatches and the output of the missions ({@link SpaceMissionMachine}).
 * <p>
 * The structure is GTNH's (the startup script {@code startup_scripts/gtceu/space_elevator.js} holds it): its blocks
 * with rules of their own are checked here. The {@link #motors() motors} round the shaft are all of one tier, the
 * elevator's; the {@link #cable() cable} block on top of the shaft needs open sky above it; the module slots hold
 * {@link #modules() Mining Modules}. It has GTNH's two sizes: the basic tower, and the extended one with a ring round
 * its foot and twelve more module slots, switched on its screen ({@link #setExtended}; {@link #getPattern()}).
 * <p>
 * The motors' tier powers 6, 12, 15, 18 or 24 module slots, and only modules of its own tier or lower. The elevator finds its
 * modules when it forms, connects them ({@link SpaceModuleMachine#connect}) and, every tick, moves the energy its hatches hold
 * into the buffers of the powered ones ({@link #powerTick}).
 * <p>
 * While the structure is formed the cable runs up into the sky and the climber rides it as GTNH's does
 * ({@link ClimberRide}; drawn by {@link com.af9.core.client.render.SpaceElevatorRender}).
 */
@SuppressWarnings("removal") // new ResourceLocation(ns, path) is the only constructor on 1.20.1
public class SpaceElevatorMachine extends WorkableElectricMultiblockMachine implements ISpaceElevatorMachine,
        IPowerGated {

    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(
            SpaceElevatorMachine.class, WorkableElectricMultiblockMachine.MANAGED_FIELD_HOLDER);

    /**
     * Where the cable is (the model's, space_elevator.js reads these): the Space Elevator Cable block sits
     * {@code CABLE_UP} blocks above the controller and {@code CABLE_BACK} behind it, on top of the motor shaft in the
     * middle of the tower.
     */
    public static final float CABLE_UP = 22, CABLE_BACK = 3;

    /** The motor tiers there are: {@code af9:space_elevator_motor_mk1} to {@code mk5}. */
    public static final int MOTOR_TIERS = AF9Blocks.MOTORS;
    private static final String[] ROMAN = { "I", "II", "III", "IV", "V" };
    private static final String MOTOR = AF9Blocks.MOTOR;
    /** The Mining Module tiers there are: {@code gtceu:space_mining_module_mk1} to {@code mk3} (machines: {@link SpaceModuleMachine}). */
    public static final int MODULE_TIERS = AF9Blocks.MODULES;
    private static final String MODULE = AF9Blocks.MODULE;
    /** The pattern check's notes (GT's match context): the motor tier, the tiers of the modules found. */
    private static final String MOTOR_KEY = "SpaceElevatorMotor";
    private static final String MODULES_KEY = "SpaceElevatorModules";
    /** Where the modules found stand (block positions as longs, in the order of the tiers noted in {@link #MODULES_KEY}). */
    private static final String MODULE_POS_KEY = "SpaceElevatorModulePositions";
    /** Module slots the motors of each tier power (GTNH's). */
    private static final int[] MODULE_SLOTS = { 6, 12, 15, 18, 24 };
    /** Expeditions a Mining Module of each tier flies at once (GTNH's parallels). */
    static final int[] MODULE_EXPEDITIONS = { 2, 4, 8 };


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
    /** The tier of the motors round the shaft (1 to 5), 0 while not formed. */
    private int motorTier;
    /** Mining Modules in the module slots, those of them the motors power, and the expeditions these fly at once. */
    private int modules, poweredModules, expeditions;
    /** The highest tier among the Mining Modules in the slots, 0 without one. */
    private int topModule;
    /** Where the modules stand that this tower connected (disconnected when it breaks). */
    private final LongList connectedModules = new LongArrayList();
    private TickableSubscription powerSubs;
    /** The sky above the cable is looked at once a second: when, and what was seen. */
    private long skyChecked = Long.MIN_VALUE / 2;
    private boolean skyClear;


    public SpaceElevatorMachine(IMachineBlockEntity holder) {
        super(holder);
    }

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
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

    /** A tiered block of the elevator ({@code af9:<prefix><tier>}, {@link AF9Blocks}), null when there is none. */
    private static Block tiered(String prefix, int tier) {
        Block block = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(AF9Core.MOD_ID, prefix + tier));
        return block == null || block == Blocks.AIR ? null : block;
    }

    /** The tier of such a block (1 up), 0 for any other block. */
    private static int tierOf(BlockState state, String prefix) {
        ResourceLocation key = ForgeRegistries.BLOCKS.getKey(state.getBlock());
        if (key == null || !key.getNamespace().equals(AF9Core.MOD_ID) || !key.getPath().startsWith(prefix)) return 0;
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

    /** The tier of a Mining Module controller ({@code gtceu:space_mining_module_mk<tier>}), 0 for any other block. */
    private static int moduleTierOf(BlockState state) {
        ResourceLocation key = ForgeRegistries.BLOCKS.getKey(state.getBlock());
        if (key == null || !key.getNamespace().equals("gtceu") || !key.getPath().startsWith(MODULE)) return 0;
        try {
            return Integer.parseInt(key.getPath().substring(MODULE.length()));
        } catch (NumberFormatException exception) {
            return 0;
        }
    }

    private static BlockInfo[] moduleCandidates() {
        return IntStream.rangeClosed(1, MODULE_TIERS)
                .mapToObj(tier -> ForgeRegistries.BLOCKS.getValue(new ResourceLocation("gtceu", MODULE + tier)))
                .filter(block -> block != null && block != Blocks.AIR)
                .map(BlockInfo::fromBlock).toArray(BlockInfo[]::new);
    }

    /**
     * A Mining Module in a module slot, any tier: a small multiblock of its own ({@link SpaceModuleMachine}), as in GTNH. The
     * check notes them all down, tier and place ({@link #onStructureFormed}), and connects them.
     */
    public static TraceabilityPredicate modules() {
        return new TraceabilityPredicate(state -> {
            int tier = moduleTierOf(state.getBlockState());
            if (tier < 1 || tier > MODULE_TIERS) return false;
            state.getMatchContext().getOrCreate(MODULES_KEY, IntArrayList::new).add(tier);
            state.getMatchContext().getOrCreate(MODULE_POS_KEY, LongArrayList::new).add(state.getPos().asLong());
            return true;
        }, SpaceElevatorMachine::moduleCandidates)
                .addTooltips(Component.translatable("af9.space_elevator.pattern.module"));
    }

    /** The Space Elevator Cable, with nothing but air above it up to the world's top (GTNH: it must see the sky). */
    public static TraceabilityPredicate cable() {
        return new TraceabilityPredicate(state -> {
            if (!state.getBlockState().is(AF9Blocks.SPACE_ELEVATOR_CABLE.get())) return false;
            if (!seesSky(state.getWorld(), state.getPos())) {
                state.setError(new PatternStringError("af9.space_elevator.error.sky"));
                return false;
            }
            return true;
        }, () -> new BlockInfo[] { BlockInfo.fromBlock(AF9Blocks.SPACE_ELEVATOR_CABLE.get()) })
                .addTooltips(Component.translatable("af9.space_elevator.error.sky"));
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
        connectModules(found == null ? IntList.of() : found, context.get(MODULE_POS_KEY));
        if (getLevel() instanceof ServerLevel) {
            // a tower that forms calls its climber down from orbit; one that was formed before has it already
            if (!climberDown) {
                climberDown = true;
                startRide(ClimberRide.FORMATION);
            }
            climberSubs = subscribeServerTick(climberSubs, this::climberTick);
            powerSubs = subscribeServerTick(powerSubs, this::powerTick);
        }
    }

    @Override
    public void onStructureInvalid() {
        super.onStructureInvalid();
        disconnectModules();
        motorTier = 0;
        modules = poweredModules = expeditions = topModule = 0;
        skyChecked = Long.MIN_VALUE / 2;
        unsubscribeClimber();
        if (powerSubs != null) {
            powerSubs.unsubscribe();
            powerSubs = null;
        }
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

    /**
     * Tells every module the tower found that it is part of it: the motors' tier, the slots they power, and whether this module
     * is one of the powered ones (the same choice as {@link #countModules}: the best tiers first, the motors' tier at most).
     */
    private void connectModules(IntList tiers, LongList positions) {
        disconnectModules();
        if (positions == null || positions.size() != tiers.size() || getLevel() == null) return;
        int slots = moduleSlots(motorTier);
        boolean[] powered = new boolean[tiers.size()];
        int count = 0;
        for (int tier = Math.min(motorTier, MODULE_TIERS); tier >= 1 && count < slots; tier--) {
            for (int i = 0; i < tiers.size() && count < slots; i++) {
                if (tiers.getInt(i) == tier) {
                    powered[i] = true;
                    count++;
                }
            }
        }
        for (int i = 0; i < positions.size(); i++) {
            if (MetaMachine.getMachine(getLevel(), BlockPos.of(positions.getLong(i))) instanceof SpaceModuleMachine module) {
                module.connect(getPos().asLong(), motorTier, slots, powered[i]);
                connectedModules.add(positions.getLong(i));
            }
        }
    }

    private void disconnectModules() {
        Level level = getLevel();
        if (level != null) {
            for (long pos : connectedModules) {
                if (MetaMachine.getMachine(level, BlockPos.of(pos)) instanceof SpaceModuleMachine module) {
                    module.disconnect();
                }
            }
        }
        connectedModules.clear();
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
    // ************* Power ************//
    //////////////////////////////////////

    /**
     * Server, every tick of a formed tower: the energy its hatches hold goes into the buffers of the powered modules (as much
     * as each has room for), as GTNH's elevator charges its modules.
     */
    private void powerTick() {
        Level level = getLevel();
        if (level == null || !isFormed() || energyContainer == null) return;
        for (long pos : connectedModules) {
            if (!(MetaMachine.getMachine(level, BlockPos.of(pos)) instanceof SpaceModuleMachine module) ||
                    !module.isPowered()) {
                continue;
            }
            long take = Math.min(module.energyRoom(), energyContainer.getEnergyStored());
            if (take <= 0) continue;
            long removed = -energyContainer.changeEnergy(-take);
            if (removed > 0) module.receiveEnergy(removed);
        }
    }

    //////////////////////////////////////
    // ************ Screen ************//
    //////////////////////////////////////

    @Override
    public void addDisplayText(List<Component> textList) {
        textList.add(Component.translatable("af9.space_elevator.display.title"));
        if (!isFormed()) {
            textList.add(Component.translatable("af9.space_elevator.display.not_formed"));
            return;
        }
        textList.add(Component.translatable("af9.space_elevator.display.motors", mark(motorTier),
                moduleSlots(motorTier)));
        textList.add(Component.translatable("af9.space_elevator.display.modules", poweredModules, modules, expeditions));
        textList.add(isSkyClear() ? Component.translatable("af9.space_elevator.display.sky") :
                Component.translatable("af9.space_elevator.display.no_sky"));
        // the size switch: a click turns the structure to the other size and has it checked again
        textList.add(ComponentPanelWidget.withButton(Component.translatable(extended ?
                "af9.space_elevator.display.extended" : "af9.space_elevator.display.basic"), "size"));
        super.addDisplayText(textList);
    }

    @Override
    public void handleDisplayClick(String componentData, ClickData clickData) {
        if ("size".equals(componentData) && !clickData.isRemote) setExtended(!extended);
    }
}
