package com.af9.core.distillation;

import com.af9.core.pattern.DistillationFacilityPipes;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.fluids.store.FluidStorageKeys;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.TickableSubscription;
import com.gregtechceu.gtceu.api.machine.feature.ICleanroomProvider;
import com.gregtechceu.gtceu.api.machine.feature.IRecipeLogicMachine;
import com.gregtechceu.gtceu.api.machine.feature.ITieredMachine;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMaintenanceMachine;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiPart;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IRotorHolderMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.CleanroomType;
import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockDisplayText;
import com.gregtechceu.gtceu.api.machine.multiblock.PartAbility;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine;
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic;
import com.gregtechceu.gtceu.api.pattern.Predicates;
import com.gregtechceu.gtceu.api.pattern.TraceabilityPredicate;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.RecipeHelper;
import com.gregtechceu.gtceu.common.data.GTBlocks;
import com.gregtechceu.gtceu.common.data.GTMaterials;
import com.gregtechceu.gtceu.common.machine.multiblock.part.CleaningMaintenanceHatchPartMachine;
import com.gregtechceu.gtceu.data.recipe.builder.GTRecipeBuilder;

import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;

import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;
import com.lowdragmc.lowdraglib.utils.BlockInfo;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.ForgeRegistries;

import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * The Distillation Facility: the IV Distillation Tower of the pack, a walk-through of real GT fluid pipes (see
 * {@link DistillationFacilityPipes}) round three Distillation Tower Modules ({@link DistillationModuleMachine}), each a small
 * multiblock of its own with its own recipe. The facility is what the modules run on: its hatches, on the Turbine Casing spine
 * (up to six energy hatches, one maintenance hatch, fluid and item hatches), are the modules' ({@link
 * DistillationModuleMachine#collectRecipeHandlers}), it is the clean room of the clean-room recipes, and the three rotor holders
 * (EV or better) behind the towers give each tower its rotor ({@link #rotorOf}).
 * <p>
 * It runs no recipe itself: its recipe logic only shows whether a tower works (so the rotors keep spinning, the muffler smokes
 * and the model is active while any does).
 * <p>
 * Made in KubeJS ({@code startup_scripts/gtceu/distillation_facility.js}, {@code gtceu:distillation_facility}).
 */
public class DistillationFacilityMachine extends WorkableElectricMultiblockMachine implements ICleanroomProvider {

    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(
            DistillationFacilityMachine.class, WorkableElectricMultiblockMachine.MANAGED_FIELD_HOLDER);

    /** Where the pattern notes the module controllers it finds (block positions as longs). */
    private static final String MODULE_POS_KEY = "DistillationModulePositions";
    /** The registry id of the module controller. */
    public static final String MODULE = "distillation_module";

    /** The rotor holders must be of this tier or better (EV). */
    public static final int MIN_HOLDER_TIER = GTValues.EV;
    /** A rotor holder's speed goes up by this much a tick while its tower works (GT's turbines: 1). */
    public static final int SPIN_UP = 4;
    /** Of a rotor's efficiency above 100 % (rotor stats, 105 to 250), this share is the tower's speed bonus at full spin. */
    public static final double SPEED_PER_GRADE = 0.5;
    /** The speed bonus of each tier of the rotor holder above EV, at full spin. */
    public static final double SPEED_PER_TIER = 0.04;
    /** Of the same grade, this share is the energy saved at full spin, at most {@link #MAX_SAVING}. */
    public static final double SAVING_PER_GRADE = 0.2;
    public static final double MAX_SAVING = 0.30;

    /** What a tower gets from its rotor: why it cannot run, or the speed and energy factors of this moment. */
    public record RotorState(@Nullable Component problem, Component material, int holderTier, int efficiency, double spin,
                             double speed, double energy) {

        public boolean ok() {
            return problem == null;
        }

        static RotorState broken(String key) {
            return new RotorState(Component.translatable(key), Component.empty(), 0, 0, 0, 1, 1);
        }
    }

    /** Filtered Distillation: liquid oxygen a second the facility needs while its cleanliness is built up and held. */
    public static final int OXYGEN_PER_SECOND = 100;
    /** Cleanliness (0-100) gained a second while the oxygen comes: 30 seconds from 0 to 100. */
    public static final double CLEAN_PER_SECOND = 100.0 / 30;
    /** Cleanliness lost a second without oxygen: 60 seconds from 100 to 0. */
    public static final double DIRT_PER_SECOND = 100.0 / 60;
    /** The facility is ready (filtered) at 100 % and stays so until the cleanliness falls below this. */
    public static final double READY_LOST_BELOW = 80;

    /** The cleanliness score, 0-100, like the clean room's. */
    @Persisted
    @DescSynced
    private double cleanliness;
    /** Filtered Distillation is up: the fab recipes may run. */
    @Persisted
    @DescSynced
    private boolean filtered;
    /** The oxygen of the last second came (for the screen). */
    private boolean oxygenFed;
    /** A Cleaning Maintenance Hatch is in the structure: always 100 %, no oxygen. */
    @DescSynced
    private boolean hatchClean;

    private TickableSubscription repairSubs;
    /** Where the modules of the structure stand (the pattern's find), empty while unformed. */
    private LongList modulePositions = new LongArrayList();
    private final List<IRotorHolderMachine> holders = new ArrayList<>();

    public DistillationFacilityMachine(IMachineBlockEntity holder) {
        super(holder);
    }

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    @Override
    protected RecipeLogic createRecipeLogic(Object... args) {
        return new FacilityLogic(this);
    }

    //////////////////////////////////////
    // ***** The pattern's parts ******//
    //////////////////////////////////////

    /**
     * What a part of the facility looks like: a part in a tower (the module is one of its controllers) wears the clean stainless
     * casing, every other part (the spine's hatches) the Steel Turbine Casing, as the facility's controller does.
     */
    public static BlockState partAppearance(IMultiPart part) {
        for (var controller : part.getControllers()) {
            if (controller instanceof DistillationModuleMachine) return GTBlocks.CASING_STAINLESS_CLEAN.getDefaultState();
        }
        return GTBlocks.CASING_STEEL_TURBINE.getDefaultState();
    }

    /** A Distillation Tower Module controller (any facing: the facility turns it), noted down with its place. */
    public static TraceabilityPredicate modules() {
        return new TraceabilityPredicate(state -> {
            ResourceLocation key = ForgeRegistries.BLOCKS.getKey(state.getBlockState().getBlock());
            if (key == null || !key.getNamespace().equals("gtceu") || !key.getPath().equals(MODULE)) return false;
            state.getMatchContext().getOrCreate(MODULE_POS_KEY, LongArrayList::new).add(state.getPos().asLong());
            return true;
        }, DistillationFacilityMachine::moduleCandidates)
                .addTooltips(Component.translatable("af9.distillation.pattern.module"));
    }

    @SuppressWarnings("removal") // new ResourceLocation(ns, path) is the only constructor on 1.20.1
    private static BlockInfo[] moduleCandidates() {
        Block block = ForgeRegistries.BLOCKS.getValue(new ResourceLocation("gtceu", MODULE));
        return block == null || block == net.minecraft.world.level.block.Blocks.AIR ? new BlockInfo[0] :
                new BlockInfo[] { BlockInfo.fromBlock(block) };
    }

    /** A rotor holder of EV or better. */
    public static TraceabilityPredicate rotorHolders() {
        int[] tiers = new int[GTValues.MAX - MIN_HOLDER_TIER + 1];
        for (int i = 0; i < tiers.length; i++) tiers[i] = MIN_HOLDER_TIER + i;
        return Predicates.ability(PartAbility.ROTOR_HOLDER, tiers)
                .addTooltips(Component.translatable("af9.distillation.pattern.rotor"));
    }

    //////////////////////////////////////
    // ******** Life cycle *********//
    //////////////////////////////////////

    @Override
    public void onLoad() {
        super.onLoad();
        if (!isRemote()) repairSubs = subscribeServerTick(repairSubs, this::repairTick);
    }

    @Override
    public void onUnload() {
        super.onUnload();
        if (repairSubs != null) {
            repairSubs.unsubscribe();
            repairSubs = null;
        }
    }

    private void repairTick() {
        if (getOffsetTimer() % 20 != 0 || getLevel() == null) return;
        // formed or not: marks the pipes (GT would trim the connections toward casings and air) and sets their connections
        if (DistillationFacilityPipes.repair(getLevel(), getPos(), getFrontFacing()) && !isFormed()) checkPattern();
    }

    @Override
    public void onStructureFormed() {
        super.onStructureFormed();
        holders.clear();
        for (IMultiPart part : getParts()) {
            if (part instanceof IRotorHolderMachine holder) {
                holders.add(holder);
                // the rotors turn in the open air above the casings, whatever way the holder was placed or built
                if (part.self().getFrontFacing() != Direction.UP) part.self().setFrontFacing(Direction.UP);
            }
        }
        Direction towers = getFrontFacing().getClockWise();
        for (long pos : modulePositions) {
            DistillationModuleMachine module = moduleAt(pos);
            if (module != null && module.getFrontFacing() != towers) module.setFrontFacing(towers);
        }
    }

    @Override
    public void onStructureInvalid() {
        unlinkModules();
        holders.clear();
        super.onStructureInvalid();
    }

    /** The modules take the hatches of the facility again whenever the facility has collected its own. */
    @Override
    protected void collectRecipeHandlers() {
        super.collectRecipeHandlers();
        LongList found = isFormed() ? getMultiblockState().getMatchContext().get(MODULE_POS_KEY) : null;
        modulePositions = found == null ? new LongArrayList() : new LongArrayList(found);
        for (long pos : modulePositions) {
            DistillationModuleMachine module = moduleAt(pos);
            if (module == null) continue;
            module.connect(getPos().asLong());
            module.setCleanroom(this);
            module.rebuildRecipeHandlers();
        }
    }

    private void unlinkModules() {
        for (long pos : modulePositions) {
            DistillationModuleMachine module = moduleAt(pos);
            if (module == null) continue;
            module.disconnect();
            module.rebuildRecipeHandlers();
        }
        modulePositions = new LongArrayList();
    }

    private @Nullable DistillationModuleMachine moduleAt(long pos) {
        return getLevel() != null && getLevel().isLoaded(BlockPos.of(pos)) &&
                MetaMachine.getMachine(getLevel(), BlockPos.of(pos)) instanceof DistillationModuleMachine module ? module :
                        null;
    }

    /** The towers that stand in this facility, formed or not. */
    public List<DistillationModuleMachine> modules(boolean formedOnly) {
        List<DistillationModuleMachine> out = new ArrayList<>();
        for (long pos : modulePositions) {
            DistillationModuleMachine module = moduleAt(pos);
            if (module != null && (!formedOnly || module.isFormed())) out.add(module);
        }
        return out;
    }

    /** Once a tick: the facility works while a tower does, and a tower that missed the facility (or lost it) is linked once a second. */
    void tickFacility(RecipeLogic logic) {
        boolean any = false;
        boolean relink = getOffsetTimer() % 20 == 0;
        for (long pos : modulePositions) {
            DistillationModuleMachine module = moduleAt(pos);
            if (module == null) continue;
            if (relink && (!module.isLinked() || !module.isConnected()) && module.isFormed()) {
                module.connect(getPos().asLong());
                module.setCleanroom(this);
                module.rebuildRecipeHandlers();
            }
            if (module.getRecipeLogic().isWorking()) any = true;
        }
        if (relink && isFormed()) tickCleanliness();
        if (!logic.isSuspend()) logic.setStatus(any ? RecipeLogic.Status.WORKING : RecipeLogic.Status.IDLE);
        // the maintenance hatch counts the facility's working time, once a tick however many towers work (as in any GT multiblock)
        if (any) {
            for (IMultiPart part : getParts()) {
                if (part instanceof IMaintenanceMachine) part.onWorking(this);
            }
        }
    }

    /** Whether the maintenance hatch has problems: a tower does not start until they are fixed, as in any GT multiblock. */
    public boolean needsMaintenance() {
        for (IMultiPart part : getParts()) {
            if (part instanceof IMaintenanceMachine maintenance && maintenance.hasMaintenanceProblems()) return true;
        }
        return false;
    }

    /** What the maintenance hatch does to a run (a configurable hatch's duration), for the towers' runs. */
    public @Nullable GTRecipe maintain(GTRecipe recipe) {
        for (IMultiPart part : getParts()) {
            if (part instanceof IMaintenanceMachine) {
                recipe = part.modifyRecipe(recipe);
                if (recipe == null) return null;
            }
        }
        return recipe;
    }

    /** The facility runs no recipe itself: it shows the work of its towers (see {@link #tickFacility}). */
    private static class FacilityLogic extends RecipeLogic {

        FacilityLogic(IRecipeLogicMachine machine) {
            super(machine);
        }

        @Override
        public void serverTick() {
            if (getMachine() instanceof DistillationFacilityMachine facility) facility.tickFacility(this);
        }

        @Override
        public void findAndHandleRecipe() {}
    }

    //////////////////////////////////////
    // ******* The clean room *******//
    //////////////////////////////////////

    /**
     * Filtered Distillation: the facility is its own clean room, once it is filtered. A Cleaning Maintenance Hatch (UV) in the
     * maintenance place makes that always so (100 %, no oxygen); without one, the facility needs liquid oxygen from a fluid
     * input hatch, {@link #OXYGEN_PER_SECOND} mB a second, constantly: the cleanliness climbs to 100 % while it comes, the
     * facility is filtered from there, and when the oxygen stops the cleanliness falls and the fab recipes stop below
     * {@link #READY_LOST_BELOW} %. The plain distillation recipes do not ask for any of this.
     */
    @Override
    public Set<CleanroomType> getTypes() {
        return Set.of(CleanroomType.CLEANROOM);
    }

    @Override
    public boolean isClean() {
        return isFormed() && filtered;
    }

    /** Whether the fab recipes may run now. */
    public boolean isFiltered() {
        return isFormed() && filtered;
    }

    public double getCleanliness() {
        return isFormed() ? cleanliness : 0;
    }

    private boolean hasCleaningHatch() {
        for (IMultiPart part : getParts()) {
            if (part instanceof CleaningMaintenanceHatchPartMachine) return true;
        }
        return false;
    }

    /** Once a second, server side: the oxygen (or the hatch) sets the cleanliness. */
    private void tickCleanliness() {
        double before = cleanliness;
        boolean wasFiltered = filtered;
        hatchClean = hasCleaningHatch();
        if (hatchClean) {
            oxygenFed = false;
            cleanliness = 100;
            filtered = true;
        } else {
            oxygenFed = drawOxygen();
            cleanliness = oxygenFed ? Math.min(100, cleanliness + CLEAN_PER_SECOND) :
                    Math.max(0, cleanliness - DIRT_PER_SECOND);
            if (cleanliness >= 100) filtered = true;
            else if (cleanliness < READY_LOST_BELOW) filtered = false;
        }
        if (before != cleanliness || wasFiltered != filtered) {
            markDirty();
            // a fab recipe that was running when the filtering fell out stops now
            if (wasFiltered && !filtered) {
                for (DistillationModuleMachine module : modules(true)) module.getRecipeLogic().resetRecipeLogic();
            }
        }
    }

    /** Takes this second's liquid oxygen from the fluid input hatches; false if the hatches did not have it all. */
    private boolean drawOxygen() {
        GTRecipe oxygen = GTRecipeBuilder.ofRaw()
                .inputFluids(GTMaterials.Oxygen.getFluid(FluidStorageKeys.LIQUID, OXYGEN_PER_SECOND)).buildRawRecipe();
        return RecipeHelper.matchRecipe(this, oxygen).isSuccess() &&
                RecipeHelper.handleRecipeIO(this, oxygen, IO.IN, recipeLogic.getChanceCaches()).isSuccess();
    }

    //////////////////////////////////////
    // ********* The rotors *********//
    //////////////////////////////////////

    /** The rotor holder in a tower's row: the one at the same place along the facility's length. */
    private @Nullable IRotorHolderMachine holderFor(DistillationModuleMachine module) {
        Direction.Axis axis = getFrontFacing().getAxis();
        BlockPos at = module.getPos();
        for (IRotorHolderMachine holder : holders) {
            BlockPos pos = holder.self().getPos();
            if (axis.choose(pos.getX(), pos.getY(), pos.getZ()) == axis.choose(at.getX(), at.getY(), at.getZ())) {
                return holder;
            }
        }
        return null;
    }

    /**
     * What the rotor in a tower's row gives the tower. A tower needs its rotor (in the holder, with its front face free). At full
     * spin the tower is faster by {@code 0.5 x} the rotor's efficiency above 100 % plus 4 % a holder tier above EV, and uses up to
     * 30 % less energy ({@code 0.2 x} the same excess): steel 105 to 130 % rotors give 3 to 15 %, the best rotors 50 to 75 %.
     * The bonus is the spin's share of it: the holder's speed against its top speed, which goes up while the tower works and
     * down when the whole facility stands still.
     */
    public RotorState rotorOf(DistillationModuleMachine module) {
        IRotorHolderMachine holder = holderFor(module);
        if (holder == null) return RotorState.broken("af9.distillation.rotor.none");
        if (!holder.hasRotor() || holder.getRotorEfficiency() < 0) return RotorState.broken("af9.distillation.rotor.missing");
        if (!holder.isFrontFaceFree()) return RotorState.broken("af9.distillation.rotor.blocked");
        int tier = holder instanceof ITieredMachine tiered ? tiered.getTier() : MIN_HOLDER_TIER;
        int efficiency = holder.getRotorEfficiency();
        double grade = Math.max(0, (efficiency - 100) / 100.0);
        int max = Math.max(1, holder.getMaxRotorHolderSpeed());
        double spin = Math.min(1.0, Math.max(0.0, holder.getRotorSpeed() / (double) max));
        double speed = 1 + spin * (SPEED_PER_GRADE * grade + SPEED_PER_TIER * Math.max(0, tier - MIN_HOLDER_TIER));
        double energy = 1 - Math.min(MAX_SAVING, SAVING_PER_GRADE * grade * spin);
        return new RotorState(null, holder.getRotorMaterial().getLocalizedName(), tier, efficiency, spin, speed, energy);
    }

    /** A tower works: its rotor spins up faster than GT's turbines' and wears the same way (one a second). */
    void spinRotor(DistillationModuleMachine module) {
        IRotorHolderMachine holder = holderFor(module);
        if (holder == null || !holder.hasRotor()) return;
        holder.onWorking(module);
        int max = holder.getMaxRotorHolderSpeed();
        if (holder.getRotorSpeed() < max) holder.setRotorSpeed(Math.min(max, holder.getRotorSpeed() + SPIN_UP - 1));
    }

    //////////////////////////////////////
    // *********** Screen ***********//
    //////////////////////////////////////

    @Override
    public void addDisplayText(List<Component> textList) {
        MultiblockDisplayText.builder(textList, isFormed())
                .setWorkingStatus(recipeLogic.isWorkingEnabled(), recipeLogic.isActive())
                .addEnergyUsageLine(energyContainer)
                .addEnergyTierLine(tier)
                .addWorkingStatusLine();
        if (!isFormed()) return;
        List<DistillationModuleMachine> towers = modules(false);
        int running = 0;
        for (DistillationModuleMachine tower : towers) if (tower.getRecipeLogic().isWorking()) running++;
        if (hatchClean) {
            textList.add(Component.translatable("af9.distillation.display.filtered_hatch")
                    .withStyle(ChatFormatting.GREEN));
        } else {
            textList.add(Component.translatable("af9.distillation.display.filtered", String.format(Locale.ROOT, "%.0f%%",
                    cleanliness), Component.translatable(filtered ? "af9.distillation.display.filtered_on" :
                            oxygenFed ? "af9.distillation.display.filtered_rising" : "af9.distillation.display.filtered_off"))
                    .withStyle(filtered ? ChatFormatting.GREEN : ChatFormatting.YELLOW));
            textList.add(Component.translatable("af9.distillation.display.oxygen", OXYGEN_PER_SECOND));
        }
        textList.add(Component.translatable("af9.distillation.display.towers", running, towers.size()));
        int number = 0;
        for (DistillationModuleMachine tower : towers) {
            number++;
            RotorState rotor = rotorOf(tower);
            if (!tower.isFormed()) {
                textList.add(Component.translatable("af9.distillation.display.tower_unformed", number)
                        .withStyle(ChatFormatting.RED));
            } else if (!rotor.ok()) {
                textList.add(Component.translatable("af9.distillation.display.tower", number, rotor.problem())
                        .withStyle(ChatFormatting.RED));
            } else {
                textList.add(Component.translatable("af9.distillation.display.tower_rotor", number, rotor.material(),
                        String.format(Locale.ROOT, "%.0f%%", rotor.spin() * 100),
                        String.format(Locale.ROOT, "+%.0f%%", (rotor.speed() - 1) * 100),
                        String.format(Locale.ROOT, "-%.0f%%", (1 - rotor.energy()) * 100)));
            }
        }
    }
}
