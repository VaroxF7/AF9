package com.af9.core.elevator;

import com.af9.core.common.IPowerGated;

import com.gregtechceu.gtceu.api.capability.recipe.ItemRecipeCapability;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockDisplayText;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine;
import com.gregtechceu.gtceu.api.pattern.BlockPattern;
import com.gregtechceu.gtceu.api.pattern.MultiblockShapeInfo;
import com.gregtechceu.gtceu.api.pattern.MultiblockWorldSavedData;
import com.gregtechceu.gtceu.api.pattern.TraceabilityPredicate;
import com.gregtechceu.gtceu.api.pattern.error.PatternStringError;
import com.gregtechceu.gtceu.api.pattern.util.PatternMatchContext;
import com.gregtechceu.gtceu.api.pattern.util.RelativeDirection;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.RecipeHelper;
import com.gregtechceu.gtceu.api.recipe.chance.logic.ChanceLogic;
import com.gregtechceu.gtceu.api.recipe.content.Content;
import com.gregtechceu.gtceu.api.recipe.content.ContentModifier;
import com.gregtechceu.gtceu.api.recipe.ingredient.SizedIngredient;
import com.gregtechceu.gtceu.api.recipe.modifier.ModifierFunction;
import com.gregtechceu.gtceu.api.recipe.modifier.ParallelLogic;
import com.gregtechceu.gtceu.api.recipe.modifier.RecipeModifier;

import com.lowdragmc.lowdraglib.gui.util.ClickData;
import com.lowdragmc.lowdraglib.gui.widget.ComponentPanelWidget;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;
import com.lowdragmc.lowdraglib.utils.BlockInfo;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;

/**
 * Space Elevator (GTNH's, docs/space-elevator.md): a tower on a cable that reaches into space. A Mining Drone (not used
 * up) in an input bus, hydrogen and a supercooled coolant in the fluid hatches and a very great deal of energy send an
 * expedition to a random asteroid; when it is back the output buses hold its ore, tens of stacks of raw ore.
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
 * While the structure is formed the cable runs up into the sky and the platform on it turns slowly
 * ({@link com.af9.core.client.render.SpaceElevatorRender}).
 */
@SuppressWarnings("removal") // new ResourceLocation(ns, path) is the only constructor on 1.20.1
public class SpaceElevatorMachine extends WorkableElectricMultiblockMachine implements ISpaceElevatorMachine,
        IPowerGated {

    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(
            SpaceElevatorMachine.class, WorkableElectricMultiblockMachine.MANAGED_FIELD_HOLDER);

    /**
     * Where the cable is (the model's, space_elevator.js reads these): the Space Elevator Cable block sits
     * {@code CABLE_UP} blocks above the controller and {@code CABLE_BACK} behind it, on top of the motor shaft in the
     * middle of the tower; the platform rides the cable {@code PLATFORM_UP} above that block, and the cable runs on
     * {@code CABLE_LENGTH} blocks up from it (as far as the world's top).
     */
    public static final float CABLE_UP = 22, CABLE_BACK = 3, PLATFORM_UP = 64, CABLE_LENGTH = 150;

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

    /**
     * The pattern of the extended structure. The startup script builds it together with the basic one (the machine
     * definition's own pattern) and hands it over, the first time GT asks for the definition's pattern.
     */
    private static volatile BlockPattern extendedPattern;

    /** The size the structure is checked for: GTNH's extended elevator instead of the basic one. */
    @Persisted
    private boolean extended;
    /** The tier of the motors round the shaft (1 to 5), 0 while not formed. */
    private int motorTier;
    /** Mining Modules in the module slots, those of them the motors power, and the expeditions these fly at once. */
    private int modules, poweredModules, expeditions;
    /** The asteroid the elevator drew last, by the vein it is made from (the screen shows it). */
    private String asteroid = "";

    public SpaceElevatorMachine(IMachineBlockEntity holder) {
        super(holder);
    }

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
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

    @Override
    public boolean isElevatorWorking() {
        return isFormed() && getRecipeLogic().isWorking();
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

    /** Whether the cable still has open sky above it (something built over a formed tower stops the expeditions). */
    private boolean cableClear() {
        Level level = getLevel();
        return level != null && seesSky(level, cablePos());
    }

    @Override
    public void onStructureFormed() {
        super.onStructureFormed();
        PatternMatchContext context = getMultiblockState().getMatchContext();
        motorTier = context.getOrDefault(MOTOR_KEY, 0);
        IntList found = context.get(MODULES_KEY);
        countModules(found == null ? IntList.of() : found);
    }

    @Override
    public void onStructureInvalid() {
        super.onStructureInvalid();
        motorTier = 0;
        modules = poweredModules = expeditions = 0;
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
        for (int tier = Math.min(motorTier, MODULE_TIERS); tier >= 1 && poweredModules < slots; tier--) {
            int powered = Math.min(ofTier[tier], slots - poweredModules);
            poweredModules += powered;
            expeditions += powered * MODULE_EXPEDITIONS[tier - 1];
        }
    }

    /** The tier of the motors (1 to 5), 0 while not formed. */
    public int getMotorTier() {
        return motorTier;
    }

    //////////////////////////////////////
    // ********** Asteroids ***********//
    //////////////////////////////////////

    /**
     * Starts a run of as many expeditions at once as the powered Mining Modules fly, the hatches can supply in full
     * (EU/t), the hydrogen and the coolant last for and the output buses have room for, while the cable is free, and
     * gives it its asteroid: the ore the run puts out, the same asteroid for every expedition of the run (the recipe's
     * own outputs are dropped).
     */
    public static final RecipeModifier ASTEROID = (machine, recipe) -> {
        if (!(machine instanceof SpaceElevatorMachine elevator)) {
            return RecipeModifier.nullWrongType(SpaceElevatorMachine.class, machine);
        }
        long eut = RecipeHelper.getRealEUt(recipe).getTotalEU();
        int tier = droneTier(recipe);
        if (tier < 1 || eut < 1 || !elevator.cableClear()) return ModifierFunction.NULL;
        int limit = (int) Math.min(elevator.expeditions, elevator.getAvailableEUt() / eut);
        if (limit < 1) return ModifierFunction.NULL;
        int runs = ParallelLogic.getParallelAmountWithoutEU(machine, recipe, limit);
        if (runs < 1) return ModifierFunction.NULL;
        RandomSource random = elevator.getLevel() != null ? elevator.getLevel().getRandom() : RandomSource.create();
        StringBuilder where = new StringBuilder();
        List<ItemStack> ores = asteroid(tier, random, where);
        if (ores.isEmpty()) return ModifierFunction.NULL;
        if (runs > 1) {
            // fewer of them when the output buses cannot take the ore of all (a single one that does not fit waits)
            GTRecipe one = recipe.copy();
            one.outputs.put(ItemRecipeCapability.CAP, contents(ores, 1));
            runs = ParallelLogic.limitByOutputMerging(elevator, one, runs, elevator::canVoidRecipeOutputs, List.of());
            if (runs < 1) return ModifierFunction.NULL;
        }
        elevator.asteroid = where.toString();
        List<Content> outputs = contents(ores, runs);
        // the inputs and the EU/t of every expedition (the drone is not used up: it is not multiplied)
        ModifierFunction parallel = ModifierFunction.builder()
                .modifyAllContents(ContentModifier.multiplier(runs))
                .eutMultiplier(runs)
                .parallels(runs)
                .build();
        return modified -> {
            GTRecipe result = parallel.apply(modified);
            if (result != null) result.outputs.put(ItemRecipeCapability.CAP, outputs);
            return result;
        };
    };

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
    private static int droneTier(GTRecipe recipe) {
        for (Content content : recipe.inputs.getOrDefault(ItemRecipeCapability.CAP, List.of())) {
            if (!(content.content instanceof Ingredient ingredient)) continue;
            for (ItemStack stack : ingredient.getItems()) {
                ResourceLocation key = ForgeRegistries.ITEMS.getKey(stack.getItem());
                if (key != null && key.getPath().startsWith(DRONE)) {
                    try {
                        return Integer.parseInt(key.getPath().substring(DRONE.length()));
                    } catch (NumberFormatException exception) {
                        return 0;
                    }
                }
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
    public void addDisplayText(List<Component> text) {
        MultiblockDisplayText.builder(text, isFormed())
                .setWorkingStatus(recipeLogic.isWorkingEnabled(), recipeLogic.isActive())
                .addEnergyUsageLine(energyContainer)
                .addWorkingStatusLine()
                .addProgressLine(recipeLogic.getProgress(), recipeLogic.getMaxProgress(),
                        recipeLogic.getProgressPercent())
                .addOutputLines(recipeLogic.getLastRecipe());
        // the size switch: also while the tower is not formed, it says which of the two is to be built
        Component size = Component.translatable(extended ? "af9.space_elevator.size.extended" :
                "af9.space_elevator.size.basic").withStyle(style -> style.withHoverEvent(new HoverEvent(
                        HoverEvent.Action.SHOW_TEXT, Component.translatable("af9.space_elevator.size.hint"))));
        text.add(Component.translatable("af9.space_elevator.size").append(ComponentPanelWidget.withButton(size, "size")));
        if (!isFormed()) return;
        if (motorTier >= 1 && motorTier <= ROMAN.length) {
            text.add(Component.translatable("af9.space_elevator.motors", ROMAN[motorTier - 1],
                    moduleSlots(motorTier)).withStyle(ChatFormatting.AQUA));
        }
        if (expeditions > 0) {
            text.add(Component.translatable("af9.space_elevator.modules", poweredModules, modules, expeditions)
                    .withStyle(ChatFormatting.AQUA));
        } else {
            text.add(Component.translatable("af9.space_elevator.no_modules").withStyle(ChatFormatting.RED));
        }
        if (!cableClear()) {
            text.add(Component.translatable("af9.space_elevator.error.sky").withStyle(ChatFormatting.RED));
        }
        if (!asteroid.isEmpty()) {
            text.add(Component.translatable("af9.space_elevator.asteroid", asteroid).withStyle(ChatFormatting.AQUA));
        }
        text.add(Component.translatable("af9.space_elevator.power", getAvailableEUt()).withStyle(ChatFormatting.GRAY));
    }

    @Override
    public void handleDisplayClick(String componentData, ClickData clickData) {
        if (!clickData.isRemote && componentData.equals("size")) setExtended(!extended);
    }
}
