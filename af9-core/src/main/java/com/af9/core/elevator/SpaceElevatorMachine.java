package com.af9.core.elevator;

import com.af9.core.common.IPowerGated;

import com.gregtechceu.gtceu.api.capability.recipe.ItemRecipeCapability;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockDisplayText;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine;
import com.gregtechceu.gtceu.api.pattern.TraceabilityPredicate;
import com.gregtechceu.gtceu.api.pattern.error.PatternStringError;
import com.gregtechceu.gtceu.api.pattern.util.RelativeDirection;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.RecipeHelper;
import com.gregtechceu.gtceu.api.recipe.chance.logic.ChanceLogic;
import com.gregtechceu.gtceu.api.recipe.content.Content;
import com.gregtechceu.gtceu.api.recipe.ingredient.SizedIngredient;
import com.gregtechceu.gtceu.api.recipe.modifier.ModifierFunction;
import com.gregtechceu.gtceu.api.recipe.modifier.RecipeModifier;

import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;
import com.lowdragmc.lowdraglib.utils.BlockInfo;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;

/**
 * Space Elevator (GTNH's, docs/space-elevator.md): a tower on a cable that reaches into space. A Mining Drone (not used
 * up) in an input bus, hydrogen and a supercooled coolant in the fluid hatches and a very great deal of energy send an
 * expedition to a random asteroid; when it is back the output buses hold its ore, tens of stacks of raw ore.
 * <p>
 * The structure is GTNH's (the startup script {@code startup_scripts/gtceu/space_elevator.js} holds it): its two blocks
 * with rules of their own are checked here. The {@link #motors() motors} round the shaft are all of one tier, the
 * elevator's; the {@link #cable() cable} block on top of the shaft needs open sky above it.
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
    /** The pattern check's note of the motor tier (GT's match context). */
    private static final String MOTOR_KEY = "SpaceElevatorMotor";

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

    /** The tier of the motors round the shaft (1 to 5), 0 while not formed. */
    private int motorTier;
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
            if (tier < 1) return false;
            int first = state.getMatchContext().getOrPut(MOTOR_KEY, tier);
            if (first != tier) {
                state.setError(new PatternStringError("af9.space_elevator.error.motors"));
                return false;
            }
            return true;
        }, () -> candidates(MOTOR, MOTOR_TIERS))
                .addTooltips(Component.translatable("af9.space_elevator.error.motors"));
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
        motorTier = getMultiblockState().getMatchContext().getOrDefault(MOTOR_KEY, 0);
    }

    @Override
    public void onStructureInvalid() {
        super.onStructureInvalid();
        motorTier = 0;
    }

    /** The tier of the motors (1 to 5), 0 while not formed. */
    public int getMotorTier() {
        return motorTier;
    }

    //////////////////////////////////////
    // ********** Asteroids ***********//
    //////////////////////////////////////

    /**
     * Only starts a run the hatches can supply with its full EU/t (and while the cable is free), and gives it its
     * asteroid: the ore the run puts out (the recipe's own outputs are dropped).
     */
    public static final RecipeModifier ASTEROID = (machine, recipe) -> {
        if (!(machine instanceof SpaceElevatorMachine elevator)) {
            return RecipeModifier.nullWrongType(SpaceElevatorMachine.class, machine);
        }
        if (elevator.getAvailableEUt() < RecipeHelper.getRealEUt(recipe).getTotalEU()) return ModifierFunction.NULL;
        int tier = droneTier(recipe);
        if (tier < 1 || !elevator.cableClear()) return ModifierFunction.NULL;
        RandomSource random = elevator.getLevel() != null ? elevator.getLevel().getRandom() : RandomSource.create();
        StringBuilder where = new StringBuilder();
        List<ItemStack> ores = asteroid(tier, random, where);
        if (ores.isEmpty()) return ModifierFunction.NULL;
        elevator.asteroid = where.toString();
        return modified -> {
            GTRecipe result = modified.copy();
            List<Content> outputs = new ArrayList<>();
            for (ItemStack stack : ores) {
                outputs.add(new Content(SizedIngredient.create(stack), ChanceLogic.getMaxChancedValue(),
                        ChanceLogic.getMaxChancedValue(), 0));
            }
            result.outputs.put(ItemRecipeCapability.CAP, outputs);
            return result;
        };
    };

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
        if (!isFormed()) return;
        if (motorTier >= 1 && motorTier <= ROMAN.length) {
            text.add(Component.translatable("af9.space_elevator.motors", ROMAN[motorTier - 1])
                    .withStyle(ChatFormatting.AQUA));
        }
        if (!cableClear()) {
            text.add(Component.translatable("af9.space_elevator.error.sky").withStyle(ChatFormatting.RED));
        }
        if (!asteroid.isEmpty()) {
            text.add(Component.translatable("af9.space_elevator.asteroid", asteroid).withStyle(ChatFormatting.AQUA));
        }
        text.add(Component.translatable("af9.space_elevator.power", getAvailableEUt()).withStyle(ChatFormatting.GRAY));
    }
}
