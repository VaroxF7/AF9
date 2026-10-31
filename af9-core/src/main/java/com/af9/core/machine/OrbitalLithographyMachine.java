package com.af9.core.machine;

import com.af9.core.AF9Core;
import com.af9.core.common.AF9DamageTypes;
import com.af9.core.litho.Coolant;
import com.af9.core.litho.LithoMode;
import com.af9.core.machine.console.ConsoleWidget;
import com.af9.core.machine.console.OrbitalConsoleWidget;
import com.af9.core.machine.console.OrbitalStationUIWidget;
import com.af9.core.machine.part.CoolantHatchPartMachine;
import com.af9.core.network.AF9Network;
import com.af9.core.registry.AF9Blocks;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.block.ICoilType;
import com.gregtechceu.gtceu.api.capability.IOpticalComputationProvider;
import com.gregtechceu.gtceu.api.capability.recipe.CWURecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.FluidRecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.capability.recipe.IRecipeHandler;
import com.gregtechceu.gtceu.api.capability.recipe.ItemRecipeCapability;
import com.gregtechceu.gtceu.api.gui.fancy.TabsWidget;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.pattern.TraceabilityPredicate;
import com.gregtechceu.gtceu.api.pattern.error.PatternStringError;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import com.gregtechceu.gtceu.api.machine.fancyconfigurator.CombinedDirectionalFancyConfigurator;
import com.gregtechceu.gtceu.api.machine.feature.IMachineLife;
import com.gregtechceu.gtceu.api.machine.feature.IAuxiliaryPowerPart;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiPart;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IRotorHolderMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.PartAbility;
import com.gregtechceu.gtceu.api.pattern.Predicates;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableItemStackHandler;
import com.gregtechceu.gtceu.api.pattern.BlockPattern;
import com.gregtechceu.gtceu.api.pattern.MultiblockShapeInfo;
import com.gregtechceu.gtceu.api.pattern.MultiblockWorldSavedData;
import com.gregtechceu.gtceu.api.pattern.util.RelativeDirection;
import com.gregtechceu.gtceu.api.recipe.RecipeHelper;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.GTRecipeType;
import com.gregtechceu.gtceu.api.recipe.chance.logic.ChanceLogic;
import com.gregtechceu.gtceu.api.recipe.content.Content;
import com.gregtechceu.gtceu.api.recipe.content.ContentModifier;
import com.gregtechceu.gtceu.api.recipe.ingredient.FluidIngredient;
import com.gregtechceu.gtceu.api.recipe.modifier.ModifierFunction;
import com.gregtechceu.gtceu.api.recipe.modifier.ParallelLogic;
import com.gregtechceu.gtceu.api.recipe.modifier.RecipeModifier;
import com.gregtechceu.gtceu.common.machine.multiblock.part.EnergyHatchPartMachine;
import com.gregtechceu.gtceu.common.machine.multiblock.part.OpticalComputationHatchMachine;

import com.lowdragmc.lowdraglib.gui.modular.ModularUI;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;
import com.lowdragmc.lowdraglib.utils.BlockInfo;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Controller logic of the Orbital Lithography Station (structure and recipes in KubeJS): prints the orbital modes,
 * 50 nm (ArF immersion), 20 and 7 nm (EUV) and 1 nm on chromodynium wafers with an X-ray free-electron laser (the 1 nm
 * prints need plain UHV: 1A of UHV from the energy hatches, {@link LithoMode#hatchTier}).
 * <ul>
 * <li>It only prints in orbit (a dimension whose path ends in "orbit", e.g. Ad Astra's ad_astra:earth_orbit): the XFEL
 * needs the vacuum of space, and without gravity the resist goes on dry. Space is the vacuum, so there is nothing to
 * pump: instead the station starts up, {@link #STARTUP_SECONDS} after it is switched on and powered (its systems
 * draw the lines' pump power, 1/8 A); it shuts down when switched off or after 3 s without power. Its prints roll the
 * node's base break chance (times the coolant's factor).</li>
 * <li>Coolant: every print draws a supercooled fluid from the coolant hatches ({@link #COOLANT}): at least the node's
 * minimum grade; each grade above it, up to the node's best, cuts the break chance (x0.8) and the run time (x0.9).</li>
 * <li>7 and 1 nm prints draw computation (CWU/t) from a computation hatch; every 1 nm print needs its own research
 * (like GT's assembly line: the Research Station scans the chip's reticle, the data orb goes in a data hatch).</li>
 * <li>Every print overclocks perfectly (4x faster per voltage tier) up to UXV, the last tier of the pack
 * ({@link #MAX_OC_TIER}); batch mode ({@link #BATCH}) bundles prints into longer runs on both sizes, and the Array Mk2's
 * Parallel Processing Computers (modules) give it parallels (no parallel hatch).</li>
 * <li>The prints use up 10 to 20 times the base amounts of fluids and dry resist ({@link LithoMode#consumption()}).</li>
 * <li>While it is switched on and powered, its magnetic field gives the station itself normal gravity, the deck and
 * 4 blocks above it, nothing around it ({@link OrbitalField}).</li>
 * <li>The controller faces up out of the top deck; the station turns with it (any facing).</li>
 * <li>The EUV Light Source of the 20 and 7 nm prints sits in the controller's own slot ({@link #euvSlot}, a recipe
 * input GT reads like an input bus; it is never used up), or in an input bus.</li>
 * <li>The reticle of the chip to print sits in the controller's reticle slot ({@link #reticleSlot}) and only there: the
 * prints still name their reticle (not consumed, so every chip is its own recipe and EMI shows which reticle), but the
 * station only runs the print whose reticle is in the slot ({@link #canRun}); a reticle in an input bus does not
 * count.</li>
 * <li>Its own screen: {@link OrbitalStationUIWidget} with {@link OrbitalConsoleWidget}.</li>
 * <li>While it prints, a light ring glows inside the rim in the colour of the node
 * ({@link com.af9.core.client.render.LightRingRender}, placed by photolithography.js with {@link #RING_UP} ...).
 * Touching it is deadly: it burns anything living that comes within {@link #RING_BURN} of its core
 * ({@link AF9DamageTypes#ORBITAL_RING}, a death screen of its own for players).</li>
 * </ul>
 */
public class OrbitalLithographyMachine extends LithoMachine implements ILightRingMachine, IMachineLife {

    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(
            OrbitalLithographyMachine.class, LithoMachine.MANAGED_FIELD_HOLDER);

    /** Seconds from switched on and powered to ready: the station starts up instead of pumping a vacuum. */
    public static final int STARTUP_SECONDS = 10;
    public static final int STARTUP_TICKS = STARTUP_SECONDS * 20;
    /**
     * The light ring (the model's, photolithography.js reads these): centre {@code RING_UP} along the controller's up and
     * {@code RING_BACK} behind it (below the deck, as it faces up), radius and tube radius; it lies across the
     * controller's front axis.
     */
    public static final float RING_UP = 0, RING_BACK = 3, RING_RADIUS = 9.6F, RING_THICKNESS = 0.25F;
    /**
     * One ring of the Array Mk2's tower (extended pattern): a band of casing {@code t} levels thick whose top level is
     * {@code d0} levels below the controller, {@code radius} out to its outer edge (measured at the centres of the
     * blocks) and {@code width} wide, turning {@code spin} radians a tick (negative: the other way). Once the station
     * is formed the client hides the ring's blocks and draws them turning (client/skin/RingDraw). A ring is a wheel: the
     * spokes, from {@code hub} blocks out of the axis to the band, turn with it. {@code strip} says which blocks those are:
     * -1 (only for the shield's wheel) everything between its levels, 0 or more only the middle level's blocks within that many
     * blocks of the four axes. Only the spokes' own blocks turn there (frames and atomic casing): the casings that stand near them
     * (the spire's sleeve, the chamber's deck, the crown) do not. Everything else of the tower between levels
     * {@code shieldFrom} and {@code shieldTo} (both -1 for none) that stands {@code hub} or more out of the axis turns too:
     * on the big ring the four shield pillars that hold the plasma field together. A light ring glows just
     * inside the band. The pattern (orbital_mk2_layout.js, tools/orbital_mk2_layout.py) has the same rings: keep them in step.
     */
    public record Band(int d0, int t, int radius, int width, float spin, float hub, int shieldFrom, int shieldTo,
                      int strip) {

        /** Levels below the controller to the middle of the band. */
        public float lightBack() {
            return d0 + (t - 1) / 2F;
        }

        /** The light ring's radius: 1.5 blocks inside the band. */
        public float lightRadius() {
            return radius - width - 1.5F;
        }

        /** The middle level of the band: the spokes run there. */
        public int mid() {
            return d0 + t / 2;
        }
    }

    /** The Mk2's three wheels: the big one in the middle (with the shield), the smaller ones above and below. */
    public static final List<Band> BANDS_MK2 = List.of(new Band(34, 3, 36, 5, 0.02F, 7.5F, 30, 41, -1),
            new Band(16, 3, 26, 4, -0.03F, 11.5F, -1, -1, 1), new Band(52, 2, 18, 3, -0.035F, 10F, -1, -1, 1));
    /** The light rings of the Mk2's bands (the render reads them through {@link #ringSpecs}). */
    public static final List<RingSpec> RINGS_MK2 = BANDS_MK2.stream()
            .map(band -> new RingSpec(band.lightBack(), band.lightRadius(), band.spin())).toList();
    /** Distance from the ring's core line within which it burns: the tube and its hottest glow. */
    public static final double RING_BURN = RING_THICKNESS * 2.5;
    /** Damage of the ring: nothing survives it (totems aside). */
    public static final float RING_DAMAGE = 1.0E6F;
    /** The station's extent: 12 blocks to each side of the controller, 17 behind it (below, when it faces up). */
    public static final int HALF_WIDTH = 12;
    public static final int DEPTH = 17;
    /**
     * The Mk2 extended array's extent: 36 to each side (73 wide, the big ring), 98 + 5 behind (99 levels from the
     * crown with the controller to the foot, and the lance's first blocks: orbital_mk2_layout.js).
     */
    public static final int HALF_WIDTH_MK2 = 36;
    public static final int DEPTH_MK2 = 98 + 5;
    /** Recipe data key of the coolant a run uses (the coolant's id). */
    public static final String COOLANT_TAG = "af9_coolant";

    /** Beam focus (permille): full, the least the Mk2's own recipes start from, the bonus levels. */
    public static final int FOCUS_MAX = 1000, FOCUS_READY = 250, FOCUS_SHARP = 600, FOCUS_LOCKED = 900;
    /**
     * Per {@link #VACUUM_INTERVAL} ticks (half a second): gain, the extra gain from spare computation (full at
     * {@link #FOCUS_SPARE_FULL} CWU/t spare), the drift of a running recipe, the loss while the station is off or
     * unpowered; and the cost of a finished run.
     */
    private static final int FOCUS_GAIN = 5, FOCUS_GAIN_SPARE = 10, FOCUS_SPARE_FULL = 64, FOCUS_DRIFT = 12,
            FOCUS_LOSS_IDLE = 2, FOCUS_PER_RUN = 30;
    /** Run time and break chance factors at sharp and at locked focus. */
    /**
     * The supplemental power connection of the focus lock: an energy hatch (not the laser hatch, which carries the
     * main power) pays {@code SUPPLEMENT_EUT} for every tick, once an interval, out of its own buffer, so it has to
     * be an EV hatch or better on a full amp. Missing for {@code LOCK_GRACE_INTERVALS} intervals in a row (5 s), the
     * lock lets go; the focus stays full and then follows the usual rules.
     */
    public static final int SUPPLEMENT_EUT = 2048;
    private static final int LOCK_GRACE_INTERVALS = 10;
    /**
     * Coil bonuses of the Array Mk2, per coil tier above cupronickel (kanthal 1 ... tritanium 7): run time, EU/t, break
     * chance, and the beam focus (gain faster, drift and run cost smaller). Like the Hearth, a better coil is a
     * better machine.
     */
    public static final double COIL_TIME = 0.96, COIL_ENERGY = 0.04, COIL_BREAK = 0.95, COIL_FOCUS = 0.1;

    /** The highest voltage tier either size overclocks to: UXV, the last tier of the pack. */
    public static final int MAX_OC_TIER = GTValues.UXV;
    /**
     * Batch mode: a print overclocked below {@link #BATCH_TARGET_TICKS} runs several at once, up to
     * {@link #BATCH_MAX}, like GT's batch mode but with a target that fits the station's long prints (GT's 5 s would
     * never be reached by a 500 s print).
     */
    public static final int BATCH_TARGET_TICKS = 20 * 60, BATCH_MAX = 64;

    public static double coilTime(int tier) {
        return Math.pow(COIL_TIME, Math.max(0, tier));
    }

    public static double coilEnergy(int tier) {
        return Math.max(0.5, 1 - COIL_ENERGY * Math.max(0, tier));
    }

    public static double coilBreak(int tier) {
        return Math.pow(COIL_BREAK, Math.max(0, tier));
    }

    /** Focus factor: gain x (1 + this), drift and run cost x (1 - this), at most half. */
    public static double coilFocus(int tier) {
        return Math.min(0.5, COIL_FOCUS * Math.max(0, tier));
    }

    private static final double FOCUS_SHARP_SPEED = 0.9, FOCUS_LOCKED_SPEED = 0.8, FOCUS_SHARP_BREAK = 0.8,
            FOCUS_LOCKED_BREAK = 0.6;

    /**
     * The pattern of the extended Array Mk2. The startup script builds it together with the basic one (the machine
     * definition's own pattern) and hands it over, the first time GT asks for the definition's pattern — like the
     * Space Elevator's extended tower.
     */
    private static volatile BlockPattern extendedPattern;

    /** Tier of the Mk2's coils (0 cupronickel ... 7 tritanium), -1 without coils (basic station or not formed). */
    @Persisted
    @DescSynced
    private int coilTier = -1;

    /** Whether the Array Mk2 (extended) size is on: it is, once the controller is upgraded. */
    @Persisted
    @DescSynced
    private boolean extended;

    /**
     * The controller is an Array Mk2: the Optical Quantum Gravitational Field Generator was put in the upgrade slot and
     * the upgrade button pressed. The item is locked in (kept, not used up) and given back when the controller is broken.
     */
    @Persisted
    @DescSynced
    private boolean upgraded;

    /**
     * Adds the coolant to a print: the best useful supercooled fluid the coolant hatches hold (the node's best grade
     * first, down to its minimum, then colder ones), {@link LithoMode#coolantPerPrint()} per print. Each grade above
     * the minimum (up to the best) shortens the run ({@link Coolant#TIME_FACTOR}); the break roll reads the coolant
     * back from the recipe. No usable coolant: no print. Place it before batch mode, which then multiplies the
     * coolant with the prints.
     */
    public static final RecipeModifier COOLANT = (machine, recipe) -> {
        if (!(machine instanceof OrbitalLithographyMachine station)) {
            return RecipeModifier.nullWrongType(OrbitalLithographyMachine.class, machine);
        }
        LithoMode mode = LithoMode.of(recipe.recipeType);
        if (mode == null || mode.minCoolant() == null) return ModifierFunction.IDENTITY;
        Coolant coolant = station.chooseCoolant(mode);
        if (coolant == null) return ModifierFunction.NULL;
        int steps = coolant.steps(mode);
        return modified -> {
            GTRecipe cooled = modified.copy();
            List<Content> fluids = new ArrayList<>(cooled.inputs.getOrDefault(FluidRecipeCapability.CAP, List.of()));
            fluids.add(new Content(FluidIngredient.of(coolant.fluid(), mode.coolantPerPrint()),
                    ChanceLogic.getMaxChancedValue(), ChanceLogic.getMaxChancedValue(), 0));
            cooled.inputs.put(FluidRecipeCapability.CAP, fluids);
            cooled.duration = Math.max(1, (int) Math.round(cooled.duration * Math.pow(Coolant.TIME_FACTOR, steps)));
            // copy() shares the data tag with the original recipe
            cooled.data = cooled.data.copy();
            cooled.data.putString(COOLANT_TAG, coolant.id);
            return cooled;
        };
    };

    /**
     * The gate of the types only the Array Mk2 runs (plasma soldering, Pico fabrication; the 1 nm prints have the
     * station's own checks): in orbit, on the extended size, aligned ({@link #FOCUS_READY}), with the recipe's full
     * EU/t and a sealed start-up. Litho prints keep their own gate ({@link LithoMachine#LITHO_GATE}); these types
     * have no LithoMode.
     */
    public static final RecipeModifier MK2_GATE = (machine, recipe) -> {
        if (!(machine instanceof OrbitalLithographyMachine station)) {
            return RecipeModifier.nullWrongType(OrbitalLithographyMachine.class, machine);
        }
        if (!isMk2Type(recipe.recipeType)) return ModifierFunction.IDENTITY;
        if (!station.isInOrbit()) return ModifierFunction.NULL;
        if (!station.isExtended()) return ModifierFunction.NULL;
        if (station.focus < FOCUS_READY) return ModifierFunction.NULL;
        if (station.getAvailableEUt() < RecipeHelper.getRealEUt(recipe).getTotalEU()) return ModifierFunction.NULL;
        if (!station.isVacuumSealed()) return ModifierFunction.NULL;
        return ModifierFunction.IDENTITY;
    };

    /**
     * The focus bonus of the Array Mk2: a run is shorter the sharper the beams are ({@link #focusSpeedFactor}). Place
     * it before the overclocks, which then work from the shortened run.
     */
    public static final RecipeModifier FOCUS = (machine, recipe) -> {
        if (!(machine instanceof OrbitalLithographyMachine station)) {
            return RecipeModifier.nullWrongType(OrbitalLithographyMachine.class, machine);
        }
        double factor = station.focusSpeedFactor();
        if (factor >= 1) return ModifierFunction.IDENTITY;
        return modified -> {
            GTRecipe sharpened = modified.copy();
            sharpened.duration = Math.max(1, (int) Math.round(sharpened.duration * factor));
            return sharpened;
        };
    };

    /**
     * The coil bonus of the Array Mk2 ({@link #coilTime}, {@link #coilEnergy}): shorter runs and less EU/t the higher
     * the tier of the heating coils in the collars round the tower's core tube. Place it after {@link #FOCUS} and before the overclocks.
     */
    public static final RecipeModifier COILS = (machine, recipe) -> {
        if (!(machine instanceof OrbitalLithographyMachine station)) {
            return RecipeModifier.nullWrongType(OrbitalLithographyMachine.class, machine);
        }
        int tier = station.coilTier();
        if (tier <= 0) return ModifierFunction.IDENTITY;
        return ModifierFunction.builder().durationMultiplier(coilTime(tier)).eutMultiplier(coilEnergy(tier)).build();
    };

    /**
     * The module bonus of the Array Mk2 ({@link OrbitalModuleMachine.Kind}): faster runs (Evaporation Stacks), less EU/t
     * (Efficiency Modules) and parallels (Parallel Processing Computers, as many as the energy allows: the Array Mk2 has no
     * parallel hatch). Place it after {@link #COILS} and before the overclocks.
     */
    public static final RecipeModifier MODULES = (machine, recipe) -> {
        if (!(machine instanceof OrbitalLithographyMachine station)) {
            return RecipeModifier.nullWrongType(OrbitalLithographyMachine.class, machine);
        }
        if (!station.isExtended() || !station.isFormed()) return ModifierFunction.IDENTITY;
        double time = station.moduleTime();
        double energy = station.moduleEnergy();
        int parallels = 1;
        if (station.moduleParallels() > 1) {
            parallels = ParallelLogic.getParallelAmount(machine, recipe, station.moduleParallels());
            if (parallels == 0) return ModifierFunction.NULL;
        }
        if (time >= 1 && energy >= 1 && parallels == 1) return ModifierFunction.IDENTITY;
        ModifierFunction.FunctionBuilder builder = ModifierFunction.builder().durationMultiplier(time)
                .eutMultiplier(energy * parallels);
        if (parallels > 1) builder.modifyAllContents(ContentModifier.multiplier(parallels)).parallels(parallels);
        return builder.build();
    };

    /**
     * Batch mode (the console's switch, {@link #isBatchEnabled}): a print that the overclocks brought below
     * {@link #BATCH_TARGET_TICKS} runs as a batch of as many prints as fit that time (at most {@link #BATCH_MAX}, and
     * as many as the inputs allow): inputs and outputs times the batch, the run as many times as long. Place it after
     * the overclocks. Every print of the batch rolls its own break chance ({@link LithoMachine#printsIn}).
     */
    public static final RecipeModifier BATCH = (machine, recipe) -> {
        if (!(machine instanceof OrbitalLithographyMachine station)) {
            return RecipeModifier.nullWrongType(OrbitalLithographyMachine.class, machine);
        }
        if (!station.isFormed() || !station.isBatchEnabled() || recipe.duration >= BATCH_TARGET_TICKS) {
            return ModifierFunction.IDENTITY;
        }
        int batch = Math.min(BATCH_MAX, BATCH_TARGET_TICKS / Math.max(1, recipe.duration));
        batch = ParallelLogic.getParallelAmountWithoutEU(machine, recipe, batch);
        if (batch == 0) return ModifierFunction.NULL;
        if (batch == 1) return ModifierFunction.IDENTITY;
        return ModifierFunction.builder()
                .inputModifier(ContentModifier.multiplier(batch))
                .outputModifier(ContentModifier.multiplier(batch))
                .durationMultiplier(batch)
                .batchParallels(batch)
                .build();
    };

    /** The EUV Light Source item the 20 and 7 nm prints keep (not consumed). */
    @SuppressWarnings("removal") // new ResourceLocation(ns, path) is the only constructor on 1.20.1
    public static final ResourceLocation EUV_SOURCE = AF9Blocks.EUV_LIGHT_SOURCE.getId();

    /**
     * The EUV Light Source slot of the station's screen: a recipe input (GT reads the controller's own handlers). No
     * pipe access (capability IO NONE), which also makes the handler itself refuse inserts: the screen's slot works on
     * its {@code storage}.
     */
    @Persisted
    public final NotifiableItemStackHandler euvSlot;

    /** The reticle slot of the station's screen: the photomask of the chip to print, kept. Same access as the EUV slot. */
    @Persisted
    public final NotifiableItemStackHandler reticleSlot;

    /** The upgrade slot of the controller's upgrade menu: takes the Mk2 upgrade item until the upgrade is pressed. */
    @Persisted
    public final NotifiableItemStackHandler upgradeSlot;

    /** The Mk2 upgrade: af9:optical_quantum_gravitational_field_generator. */
    public static final ResourceLocation UPGRADE_ITEM = new ResourceLocation("af9",
            "optical_quantum_gravitational_field_generator");

    /**
     * The Array Mk2's beam focus, 0 to {@link #FOCUS_MAX} (permille). It is the Mk2's setup and its upkeep:
     * <ul>
     * <li>it builds up while the station is formed on the extended size, switched on, started up and in orbit
     * ({@link #FOCUS_GAIN} per {@link #VACUUM_INTERVAL} ticks), faster with computation to spare on the hatches;</li>
     * <li>the Mk2's own work (1 nm prints, plasma soldering, Pico fabrication) starts only from
     * {@link #FOCUS_READY}; the console says ALIGNING until then;</li>
     * <li>a running recipe drifts it down ({@link #FOCUS_DRIFT} per interval) and every finished run costs
     * {@link #FOCUS_PER_RUN}: the drift is covered only by computation beyond what the running recipe draws, so a
     * line that runs without a pause needs an HPCA with room to spare;</li>
     * <li>sharp and locked focus shorten every run and lower every print's break chance
     * ({@link #focusSpeedFactor}, {@link #focusBreakFactor}); it is lost with the structure;</li>
     * <li>the focus lock, an option (sneak + screwdriver on the controller): once the focus is full it latches and
     * stays full, with no drift and no cost per run, until the structure is broken — as long as a supplemental power
     * connection pays for it ({@link #supplementalPaid}).</li>
     * </ul>
     */
    @Persisted
    private int focus;
    /** The focus lock is switched on (an option; sneak + screwdriver). */
    @Persisted
    private boolean focusLockOption;
    /** The focus latched: full and held, until the structure breaks (or the supplemental power stays away). */
    @Persisted
    private boolean focusLocked;
    /** Intervals in a row the supplemental power was missing while latched. */
    private int lockUnpaid;

    /** Magnetic field on (formed, switched on, powered); synced for the client's gravity. */
    @DescSynced
    private boolean fieldActive;

    /**
     * The auxiliary power paid the last interval's start-up draw in full (or a print runs): the Mk2's rings spin up while
     * this holds. Synced for the client's render.
     */
    @DescSynced
    private boolean auxPowered;

    /** Start-up progress, ticks: 0 off, {@link #STARTUP_TICKS} ready. */
    @Persisted
    @DescSynced
    private int startupTicks;
    private int unpoweredTicks;

    /** The parts as of this tick, shared by the per-interval checks so each tick walks them once. */
    private List<IMultiPart> intervalParts;
    private long intervalPartsTick = -1;
    /** The computation of {@link #availableComputation} as of this tick, shared the same way. */
    private int intervalComputation;
    private long intervalComputationTick = -1;

    /** The parts, collected once per tick and shared by the checks that run on them. */
    private List<IMultiPart> partsOnce() {
        long tick = getOffsetTimer();
        if (intervalParts == null || intervalPartsTick != tick) {
            intervalParts = getParts();
            intervalPartsTick = tick;
        }
        return intervalParts;
    }

    private AABB fieldBox;
    private Direction fieldFront;
    private Direction fieldUp;

    public OrbitalLithographyMachine(IMachineBlockEntity holder) {
        super(holder);
        euvSlot = new NotifiableItemStackHandler(this, 1, IO.IN, IO.NONE)
                .setFilter(OrbitalLithographyMachine::isEuvSource);
        reticleSlot = new NotifiableItemStackHandler(this, 1, IO.IN, IO.NONE)
                .setFilter(OrbitalLithographyMachine::isReticle);
        upgradeSlot = new NotifiableItemStackHandler(this, 1, IO.IN, IO.NONE)
                .setFilter(s -> !upgraded && isUpgradeItem(s));
    }

    public static boolean isUpgradeItem(ItemStack stack) {
        return !stack.isEmpty() && UPGRADE_ITEM.equals(ForgeRegistries.ITEMS.getKey(stack.getItem()));
    }

    public boolean isUpgraded() {
        return upgraded;
    }

    /**
     * The upgrade button: the upgrade item in the slot is locked into the controller (the slot empties, the item is
     * kept as the upgraded state) and the controller becomes the Array Mk2. Between runs only; false when it did not
     * happen.
     */
    public boolean tryUpgrade() {
        if (isRemote() || upgraded || getRecipeLogic().isWorking()) return false;
        if (!isUpgradeItem(upgradeSlot.getStackInSlot(0))) return false;
        upgradeSlot.storage.setStackInSlot(0, ItemStack.EMPTY);
        upgraded = true;
        markDirty();
        setExtended(true);
        return true;
    }

    public static boolean isEuvSource(ItemStack stack) {
        return !stack.isEmpty() && EUV_SOURCE.equals(ForgeRegistries.ITEMS.getKey(stack.getItem()));
    }

    /** af9:&lt;chip&gt;_reticle. */
    public static boolean isReticle(ItemStack stack) {
        if (stack.isEmpty()) return false;
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        return id != null && id.getNamespace().equals(AF9Core.MOD_ID) && id.getPath().endsWith("_reticle");
    }

    /** The reticle a print names (its not-consumed input), or null. */
    public static Item reticleOf(GTRecipe recipe) {
        for (Content content : recipe.inputs.getOrDefault(ItemRecipeCapability.CAP, List.of())) {
            for (ItemStack stack : ItemRecipeCapability.CAP.of(content.content).getItems()) {
                if (isReticle(stack)) return stack.getItem();
            }
        }
        return null;
    }

    /** Both sizes overclock to UXV and no further (the energy hatches may be higher). */
    @Override
    public int getOverclockTier() {
        return Math.min(super.getOverclockTier(), MAX_OC_TIER);
    }

    @Override
    public int getMaxOverclockTier() {
        return Math.min(super.getMaxOverclockTier(), MAX_OC_TIER);
    }

    /**
     * A print runs only with its reticle in the reticle slot (GT would also take one from an input bus), and with
     * hatches of the node's voltage (the 1 nm prints: UHV).
     */
    @Override
    public boolean canRun(GTRecipe recipe) {
        LithoMode mode = LithoMode.of(recipe.recipeType);
        if (mode != null && getInputTier() < mode.hatchTier) return false;
        if (mode == LithoMode.N1 && focus < FOCUS_READY) return false;
        Item reticle = reticleOf(recipe);
        return reticle == null || reticleSlot.getStackInSlot(0).is(reticle);
    }

    /** Broken controller: the EUV Light Source and the reticle drop. */
    @Override
    public void onMachineRemoved() {
        clearInventory(euvSlot.storage);
        clearInventory(reticleSlot.storage);
        clearInventory(upgradeSlot.storage);
        // the locked upgrade comes back out with the broken controller
        if (upgraded && getLevel() != null) {
            var item = ForgeRegistries.ITEMS.getValue(UPGRADE_ITEM);
            if (item != null) {
                net.minecraft.world.Containers.dropItemStack(getLevel(), getPos().getX() + 0.5, getPos().getY() + 0.5,
                        getPos().getZ() + 0.5, new ItemStack(item));
            }
        }
    }

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    @Override
    public List<LithoMode> getModes() {
        return LithoMode.ORBITAL_MODES;
    }

    /** The 1 nm prints (chromodynium) are the Array Mk2's alone. */
    @Override
    public boolean canPrint(LithoMode mode) {
        return mode.onOrbitalStation() && isInOrbit() && (!mode.mk2Only() || extended);
    }

    @Override
    public int blockedStatus(LithoMode mode) {
        GTRecipeType type = getRecipeType();
        if (LithoMode.of(type) == null) {
            // plasma soldering, Pico fabrication: no node, reticle or coolant to ask for
            if (!isInOrbit()) return ConsoleWidget.STATUS_NO_ORBIT;
            if (isMk2Type(type)) {
                if (!extended) return ConsoleWidget.STATUS_MK2_ONLY;
                if (focus < FOCUS_READY) return ConsoleWidget.STATUS_ALIGNING;
            }
            return -1;
        }
        if (!mode.onOrbitalStation()) return ConsoleWidget.STATUS_LOCKED;
        if (!isInOrbit()) return ConsoleWidget.STATUS_NO_ORBIT;
        if (mode == LithoMode.N1) {
            if (!extended) return ConsoleWidget.STATUS_MK2_ONLY;
            if (focus < FOCUS_READY) return ConsoleWidget.STATUS_ALIGNING;
        }
        if (getInputTier() < mode.hatchTier) return ConsoleWidget.STATUS_NO_POWER;
        if (reticleSlot.getStackInSlot(0).isEmpty()) return ConsoleWidget.STATUS_NO_RETICLE;
        // a computation hatch alone is not enough: something (an HPCA) has to supply computation through it
        if (mode.computation() > 0 && availableComputation() < 1) {
            return ConsoleWidget.STATUS_NO_COMPUTATION;
        }
        if (mode.needsResearch() && !hasDataHatch()) return ConsoleWidget.STATUS_NO_DATA;
        if (mode.minCoolant() != null && chooseCoolant(mode) == null) return ConsoleWidget.STATUS_NO_COOLANT;
        return -1;
    }

    @Override
    public int surplusFor(LithoMode mode) {
        return 0;
    }

    //////////////////////////////////////
    // ********* Modules *************//
    //////////////////////////////////////

    /** The id prefix of the module blocks ({@code gtceu:orbital_module_<kind>}, {@link OrbitalModuleMachine.Kind}). */
    public static final String MODULE_PREFIX = "orbital_module_";
    private static final String MODULE_POS_KEY = "OrbitalModulePositions", MODULE_COUNT_KEY = "OrbitalModuleCounts";
    /** Where the modules stand that the check found (block positions as longs), as noted by {@link #modules()}. */
    private final LongList modulePositions = new LongArrayList();
    /** Modules of each kind that give their bonus, recounted every {@link #MODULE_RECOUNT} ticks. */
    private final int[] moduleCount = new int[OrbitalModuleMachine.Kind.values().length];
    private long moduleCounted = Long.MIN_VALUE / 2;
    private static final int MODULE_RECOUNT = 20;

    /**
     * A module in a module slot, any kind: a small multiblock of its own ({@link OrbitalModuleMachine}). The check notes
     * where they stand ({@link #onStructureFormed}); the station links them and counts the ones whose own structure
     * stands.
     */
    public static TraceabilityPredicate modules() {
        return new TraceabilityPredicate(state -> {
            OrbitalModuleMachine.Kind kind = moduleKindOf(state.getBlockState());
            if (kind == null) return false;
            // an Array takes only so many of a kind
            int[] counts = state.getMatchContext().getOrCreate(MODULE_COUNT_KEY,
                    () -> new int[OrbitalModuleMachine.Kind.values().length]);
            if (++counts[kind.ordinal()] > kind.max) {
                state.setError(new PatternStringError("af9.orbital_module.error.max"));
                return false;
            }
            state.getMatchContext().getOrCreate(MODULE_POS_KEY, LongArrayList::new).add(state.getPos().asLong());
            return true;
        }, OrbitalLithographyMachine::moduleCandidates)
                .addTooltips(Component.translatable("af9.orbital_lithography_station.pattern.module"));
    }

    /** The lowest tier of the collision chamber's rotor holders. */
    public static final int MIN_HOLDER_TIER = GTValues.UV;

    /**
     * A rotor holder of UV or better, in a spot of the Mk2's collision chamber. The rotors in them are for show: they turn
     * while the Array works and never wear (the mixin {@code RotorHolderCosmeticMixin} drops the wear), a holder with
     * no rotor is as good.
     */
    public static TraceabilityPredicate rotorHolders() {
        int[] tiers = new int[GTValues.MAX - MIN_HOLDER_TIER + 1];
        for (int i = 0; i < tiers.length; i++) tiers[i] = MIN_HOLDER_TIER + i;
        return Predicates.ability(PartAbility.ROTOR_HOLDER, tiers)
                .addTooltips(Component.translatable("af9.orbital_lithography_station.pattern.rotor"));
    }

    private static OrbitalModuleMachine.Kind moduleKindOf(BlockState state) {
        ResourceLocation key = ForgeRegistries.BLOCKS.getKey(state.getBlock());
        if (key == null || !key.getNamespace().equals("gtceu")) return null;
        return OrbitalModuleMachine.Kind.ofBlock(key.getPath());
    }

    private static BlockInfo[] moduleCandidates() {
        return java.util.Arrays.stream(OrbitalModuleMachine.Kind.values())
                .map(kind -> ForgeRegistries.BLOCKS.getValue(new ResourceLocation("gtceu", MODULE_PREFIX + kind.id())))
                .filter(block -> block != null && block != Blocks.AIR)
                .map(BlockInfo::fromBlock).toArray(BlockInfo[]::new);
    }

    private void linkModules(LongList found) {
        unlinkModules();
        if (found == null || getLevel() == null) return;
        modulePositions.addAll(found);
        for (long pos : modulePositions) {
            if (MetaMachine.getMachine(getLevel(), BlockPos.of(pos)) instanceof OrbitalModuleMachine module) {
                module.link(getPos().asLong());
            }
        }
        moduleCounted = Long.MIN_VALUE / 2;
    }

    private void unlinkModules() {
        if (getLevel() != null) {
            for (long pos : modulePositions) {
                if (MetaMachine.getMachine(getLevel(), BlockPos.of(pos)) instanceof OrbitalModuleMachine module) {
                    module.unlink();
                }
            }
        }
        modulePositions.clear();
        java.util.Arrays.fill(moduleCount, 0);
    }

    /** Modules of a kind that give their bonus now (their own structure stands). */
    public int moduleCount(OrbitalModuleMachine.Kind kind) {
        if (getLevel() == null || modulePositions.isEmpty()) return 0;
        long now = getLevel().getGameTime();
        if (now - moduleCounted >= MODULE_RECOUNT || now < moduleCounted) {
            moduleCounted = now;
            java.util.Arrays.fill(moduleCount, 0);
            for (long pos : modulePositions) {
                if (MetaMachine.getMachine(getLevel(), BlockPos.of(pos)) instanceof OrbitalModuleMachine module &&
                        module.isFormed()) {
                    moduleCount[module.getKind().ordinal()]++;
                }
            }
        }
        return moduleCount[kind.ordinal()];
    }

    public int modulesTotal() {
        int sum = 0;
        for (OrbitalModuleMachine.Kind kind : OrbitalModuleMachine.Kind.values()) sum += moduleCount(kind);
        return sum;
    }

    private double moduleFactor(java.util.function.ToDoubleFunction<OrbitalModuleMachine.Kind> per) {
        if (!extended || !isFormed()) return 1;
        double factor = 1;
        for (OrbitalModuleMachine.Kind kind : OrbitalModuleMachine.Kind.values()) {
            factor *= Math.pow(per.applyAsDouble(kind), moduleCount(kind));
        }
        return factor;
    }

    public double moduleTime() {
        return moduleFactor(kind -> kind.time);
    }

    public double moduleEnergy() {
        return moduleFactor(kind -> kind.energy);
    }

    /** Factor of the auxiliary power (start-up and standby draw). */
    public double moduleAux() {
        return moduleFactor(kind -> kind.aux);
    }

    /** Parallels the Multiplex modules add (1 without them). */
    public int moduleParallels() {
        if (!extended || !isFormed()) return 1;
        int parallels = 1;
        for (OrbitalModuleMachine.Kind kind : OrbitalModuleMachine.Kind.values()) {
            parallels += kind.parallels * moduleCount(kind);
        }
        return parallels;
    }

    //////////////////////////////////////
    // ********* Array Mk2 ***********//
    //////////////////////////////////////

    /** The startup script's: the pattern of the extended Array Mk2 (larger ring, same structure). */
    public static void setExtendedPattern(BlockPattern pattern) {
        extendedPattern = pattern;
    }

    /** The basic station's pattern, or the Mk2's while that size is switched on (like the Space Elevator). */
    @Override
    public BlockPattern getPattern() {
        // the definition's first: building it is what makes the script hand over the extended one
        BlockPattern basic = super.getPattern();
        BlockPattern big = extendedPattern;
        return extended && big != null ? big : basic;
    }

    /** The structure previews: the basic station, and the Array Mk2's tower on a second page (its rings are drawn, not built). */
    public static List<MultiblockShapeInfo> previews(MultiblockMachineDefinition definition) {
        List<MultiblockShapeInfo> pages = new ArrayList<>(previewShapes(definition));
        // the page for the Mk2: only the tower itself, cut out of the 73 x 130 x 73 pattern (the full size is far too big
        // for the structure view); the rings and their beams are drawn by the machine's own render once it is formed.
        // previewShapes built the definition's pattern, which is what makes the script hand over the extended one.
        BlockPattern big = extendedPattern;
        if (big != null) {
            try {
                pages.addAll(shapes(definition, big, true));
            } catch (RuntimeException e) {
                AF9Core.LOGGER.warn("Orbital station preview: cannot build the Mk2 page, showing the basic station only", e);
            }
        }
        return pages;
    }

    /**
     * A preview page with no holes: GTCEu's preview widget dereferences every cell with no null check, and a
     * single null cell aborts the whole JEI registration (every GTCEu recipe vanishes). Fill whatever is left
     * empty with GT's own empty cell, which is also what its previews use for air.
     */
    private static BlockInfo[][][] solid(BlockInfo[][][] cells) {
        for (int x = 0; x < cells.length; x++) {
            for (int y = 0; y < cells[x].length; y++) {
                for (int z = 0; z < cells[x][y].length; z++) {
                    if (cells[x][y][z] == null) cells[x][y][z] = BlockInfo.EMPTY;
                }
            }
        }
        return cells;
    }

    public boolean isExtended() {
        return extended;
    }

    /**
     * Switches between the basic station and the Array Mk2. A formed station is taken apart and checked anew for
     * the other size, as when its controller is turned; a run that is on is lost.
     */
    public void setExtended(boolean extended) {
        if (upgraded) extended = true;
        if (this.extended == extended) return;
        this.extended = extended;
        // the basic station does not stay in a mode of the Mk2
        if (!extended && isMk2Only(getRecipeType())) super.setActiveRecipeType(0);
        focus = 0;
        focusLocked = false;
        lockUnpaid = 0;
        fieldBox = null;
        markDirty();
        if (isFormed() && getLevel() instanceof ServerLevel serverLevel) {
            onStructureInvalid();
            MultiblockWorldSavedData data = MultiblockWorldSavedData.getOrCreate(serverLevel);
            data.removeMapping(getMultiblockState());
            data.addAsyncLogic(this);
        }
    }

    /** A screwdriver on the controller (shift: the focus lock option) says which tier it is. */
    @Override
    protected InteractionResult onScrewdriverClick(Player player, InteractionHand hand, Direction side,
                                                   BlockHitResult hit) {
        if (isRemote()) return InteractionResult.SUCCESS;
        if (player.isShiftKeyDown()) {
            // the focus lock option; it only means something on the Mk2, and is kept when the size is switched
            focusLockOption = !focusLockOption;
            markDirty();
            player.displayClientMessage(Component.translatable(focusLockOption ? "af9.orbital_array.lock.on" :
                    "af9.orbital_array.lock.off", SUPPLEMENT_EUT), true);
            return InteractionResult.SUCCESS;
        }
        if (getRecipeLogic().isWorking()) {
            player.displayClientMessage(Component.translatable("af9.orbital_array.mk2.busy"), true);
            return InteractionResult.SUCCESS;
        }
        // the size is the controller's tier: it changes with the upgrade (the upgrade menu), not with a tool
        player.displayClientMessage(Component.translatable(upgraded ? "af9.orbital_array.mk2.on" :
                "af9.orbital_array.mk2.locked"), true);
        return InteractionResult.SUCCESS;
    }

    /** The rings of the size it is formed as (the render reads them through {@link #ringSpecs}). */
    @Override
    public List<RingSpec> ringSpecs(float modelBack, float modelRadius) {
        return extended ? RINGS_MK2 : List.of(new RingSpec(modelBack, RING_RADIUS, 0F));
    }

    /** No vacuum to pump in orbit (see {@link #updateVacuum()}); unused. */
    @Override
    protected int vacuumLevel() {
        return 0;
    }

    @Override
    public String titleKey() {
        return "af9.orbital_litho.console.title";
    }

    /** True in an orbit dimension (path "orbit" or ending in "_orbit", any mod). */
    public boolean isInOrbit() {
        Level level = getLevel();
        return level != null && isOrbit(level.dimension().location());
    }

    public static boolean isOrbit(ResourceLocation dimension) {
        String path = dimension.getPath();
        return path.equals("orbit") || path.endsWith("_orbit");
    }

    /**
     * The most computation the computation hatches can supply, CWU/t: what the optical network behind them (an HPCA)
     * provides at most, 0 without a hatch or with nothing linked to it. GT's own check when a print starts / runs asks
     * the same network, so a print never runs on less.
     */
    public int availableComputation() {
        long tick = getOffsetTimer();
        if (intervalComputationTick != tick) {
            intervalComputationTick = tick;
            intervalComputation = 0;
            if (isFormed()) {
                for (IRecipeHandler<?> handler : getCapabilitiesFlat(IO.IN, CWURecipeCapability.CAP)) {
                    if (handler instanceof IOpticalComputationProvider provider) {
                        intervalComputation += provider.getMaxCWUt();
                    }
                }
            }
        }
        return intervalComputation;
    }

    /** A computation hatch. */
    public boolean hasComputationHatch() {
        return getParts().stream()
                .anyMatch(part -> part instanceof OpticalComputationHatchMachine hatch && !hatch.isTransmitter());
    }

    //////////////////////////////////////
    // ********** Coolant ***********//
    //////////////////////////////////////

    /** Supercooled fluid in the coolant hatches, per coolant. */
    public Map<Coolant, Long> coolantStock() {
        Map<Coolant, Long> stock = new EnumMap<>(Coolant.class);
        for (IMultiPart part : partsOnce()) {
            if (!(part instanceof CoolantHatchPartMachine hatch)) continue;
            for (int i = 0; i < hatch.tank.getTanks(); i++) {
                FluidStack stack = hatch.tank.getFluidInTank(i);
                Coolant coolant = Coolant.of(stack);
                if (coolant != null) stock.merge(coolant, (long) stack.getAmount(), Long::sum);
            }
        }
        return stock;
    }

    /**
     * The coolant a print of the mode would use: enough for one print, the best useful grade first (the node's best
     * down to its minimum), then colder ones than the best (weakest first). Null if none is there.
     */
    public Coolant chooseCoolant(LithoMode mode) {
        Coolant min = mode.minCoolant();
        Coolant best = mode.bestCoolant();
        if (min == null || best == null) return null;
        Map<Coolant, Long> stock = coolantStock();
        long needed = mode.coolantPerPrint();
        Coolant[] all = Coolant.values();
        for (int i = best.ordinal(); i >= min.ordinal(); i--) {
            if (stock.getOrDefault(all[i], 0L) >= needed) return all[i];
        }
        for (int i = best.ordinal() + 1; i < all.length; i++) {
            if (stock.getOrDefault(all[i], 0L) >= needed) return all[i];
        }
        return null;
    }

    /** The coolant of a run (from its recipe data), or null. */
    public static Coolant coolantOf(GTRecipe recipe) {
        if (recipe == null || !recipe.data.contains(COOLANT_TAG)) return null;
        String id = recipe.data.getString(COOLANT_TAG);
        for (Coolant coolant : Coolant.values()) {
            if (coolant.id.equals(id)) return coolant;
        }
        return null;
    }

    /** The coolant of the running print, or the one the next print of the mode would take (null: none). */
    public Coolant currentCoolant(LithoMode mode) {
        GTRecipe running = getRecipeLogic().isActive() ? getRecipeLogic().getLastRecipe() : null;
        Coolant coolant = coolantOf(running);
        return coolant != null ? coolant : chooseCoolant(mode);
    }

    @Override
    protected double machineBreakFactor(LithoMode mode, boolean measured) {
        // a run that ran: the coolant it was started with; the next print: the one it would take now
        GTRecipe run = measured ? getRecipeLogic().getLastRecipe() : null;
        Coolant coolant = run != null ? coolantOf(run) : chooseCoolant(mode);
        double cooled = coolant == null ? 1 : Math.pow(Coolant.BREAK_FACTOR, coolant.steps(mode));
        return cooled * focusBreakFactor() * coilBreak(coilTier);
    }

    //////////////////////////////////////
    // ********* Beam focus **********//
    //////////////////////////////////////

    /** The type only the Array Mk2 runs besides the 1 nm prints: plasma soldering. */
    public static boolean isMk2Type(GTRecipeType type) {
        return type.registryName.getPath().equals("plasma_soldering");
    }

    /** Every mode the basic station cannot run: the 1 nm prints and the Mk2's own types. */
    public static boolean isMk2Only(GTRecipeType type) {
        LithoMode mode = LithoMode.of(type);
        return mode != null ? mode.mk2Only() : isMk2Type(type);
    }

    /** Whether a mode (recipe type index) is shut right now: one of the Array Mk2's, on the basic station. */
    public boolean isModeLocked(int index) {
        GTRecipeType[] types = getRecipeTypes();
        return !extended && index >= 0 && index < types.length && isMk2Only(types[index]);
    }

    /**
     * The mode tab does not offer the Mk2's modes on the basic station ({@link OrbitalModeTab}); this keeps them shut
     * for everything else that sets a mode. The client only follows what the server sends.
     */
    @Override
    public void setActiveRecipeType(int type) {
        if (!isRemote() && isModeLocked(type)) return;
        super.setActiveRecipeType(type);
    }

    @Override
    public void attachSideTabs(TabsWidget sideTabs) {
        sideTabs.setMainTab(this);
        sideTabs.attachSubTab(new OrbitalModeTab(this));
        var directional = CombinedDirectionalFancyConfigurator.of(self(), self());
        if (directional != null) sideTabs.attachSubTab(directional);
        sideTabs.attachSubTab(new com.gregtechceu.gtceu.api.machine.fancyconfigurator.PowerManagementFancyTab(this));
    }

    public int getFocus() {
        return focus;
    }

    @Override
    public int focusPermille() {
        return extended && isFormed() ? focus : -1;
    }

    /** The Mk2's coil tier (0 cupronickel ... 7 tritanium), -1 where there are no coils. */
    @Override
    public int coilTier() {
        return extended && isFormed() ? coilTier : -1;
    }

    /** Run time factor of the Mk2's focus: sharp 0.9, locked 0.8 (1 on the basic station). */
    public double focusSpeedFactor() {
        if (!extended) return 1;
        return focus >= FOCUS_LOCKED ? FOCUS_LOCKED_SPEED : focus >= FOCUS_SHARP ? FOCUS_SHARP_SPEED : 1;
    }

    /** Break chance factor of the Mk2's focus: sharp 0.8, locked 0.6 (1 on the basic station). */
    public double focusBreakFactor() {
        if (!extended) return 1;
        return focus >= FOCUS_LOCKED ? FOCUS_LOCKED_BREAK : focus >= FOCUS_SHARP ? FOCUS_SHARP_BREAK : 1;
    }

    /** CWU/t the running recipe draws from the computation hatches (0 if it draws none). */
    private int runningComputation() {
        GTRecipe run = getRecipeLogic().isWorking() ? getRecipeLogic().getLastRecipe() : null;
        if (run == null) return 0;
        int sum = 0;
        for (Content content : run.tickInputs.getOrDefault(CWURecipeCapability.CAP, List.of())) {
            sum += CWURecipeCapability.CAP.of(content.content);
        }
        return sum;
    }

    /**
     * The focus, every {@link #VACUUM_INTERVAL} ticks: gone off the extended size and with the structure; slowly lost
     * while switched off, unpowered or out of orbit; else gained, and drifted down by a running recipe. The extra
     * gain comes from computation the hatches can supply beyond what the running recipe draws, so keeping a long
     * line focused takes an HPCA bigger than the prints need.
     */
    private void updateFocus() {
        int before = focus;
        boolean lockedBefore = focusLocked;
        if (!extended || !isFormed()) {
            focus = 0;
            focusLocked = false;
            lockUnpaid = 0;
        } else if (focusLocked) {
            // latched: full and held for as long as the supplemental power comes (and the option stays on)
            if (!focusLockOption) {
                focusLocked = false;
            } else if (supplementalPaid()) {
                lockUnpaid = 0;
                focus = FOCUS_MAX;
            } else if (++lockUnpaid >= LOCK_GRACE_INTERVALS) {
                focusLocked = false;
                lockUnpaid = 0;
            }
        } else if (startupTicks < STARTUP_TICKS || !isInOrbit() || !getRecipeLogic().isWorkingEnabled()) {
            focus = Math.max(0, focus - FOCUS_LOSS_IDLE);
        } else {
            boolean running = getRecipeLogic().isWorking();
            int spare = Math.max(0, availableComputation() - runningComputation());
            int gain = FOCUS_GAIN + Math.min(FOCUS_GAIN_SPARE, spare * FOCUS_GAIN_SPARE / FOCUS_SPARE_FULL);
            double coils = coilFocus(coilTier);
            gain = (int) Math.round(gain * (1 + coils));
            int drift = (int) Math.round(FOCUS_DRIFT * (1 - coils));
            focus = Math.max(0, Math.min(FOCUS_MAX, focus + gain - (running ? drift : 0)));
            // full, and the option on: latch, if the supplemental power can be had
            if (focusLockOption && focus >= FOCUS_MAX && supplementalPaid()) {
                focusLocked = true;
                lockUnpaid = 0;
            }
        }
        if (focus != before || focusLocked != lockedBefore) markDirty();
    }

    /**
     * Takes one interval of the lock's supplemental power from the input energy hatches and wireless receivers of EV or
     * better (the laser hatch carries the main power). The hatches pay together, auxiliary ones first, so the start-up
     * power the auxiliary hatches also pay, or a recipe drawing on a primary hatch, no longer starves the lock.
     */
    private boolean supplementalPaid() {
        long need = (long) SUPPLEMENT_EUT * VACUUM_INTERVAL;
        List<IAuxiliaryPowerPart> hatches = new ArrayList<>();
        long stored = 0;
        for (IMultiPart part : partsOnce()) {
            if (part instanceof IAuxiliaryPowerPart hatch &&
                    hatch.getAuxiliaryContainer().getInputVoltage() >= GTValues.V[GTValues.EV]) {
                hatches.add(hatch);
                stored += hatch.getAuxiliaryContainer().getEnergyStored();
            }
        }
        if (stored < need) return false;
        hatches.sort((x, y) -> Boolean.compare(y.isAuxiliaryInput(), x.isAuxiliaryInput()));
        long left = need;
        for (IAuxiliaryPowerPart hatch : hatches) {
            var container = hatch.getAuxiliaryContainer();
            long take = Math.min(left, container.getEnergyStored());
            if (take <= 0) continue;
            container.changeEnergy(-take);
            left -= take;
            if (left <= 0) break;
        }
        return true;
    }

    @Override
    public int focusLockState() {
        return !extended ? 0 : focusLocked ? 2 : focusLockOption ? 1 : 0;
    }

    /** Every finished run knocks the beams a little out of focus (Mk2). */
    @Override
    public void afterWorking() {
        super.afterWorking();
        if (extended && focus > 0 && !focusLocked) {
            focus = Math.max(0, focus - (int) Math.round(FOCUS_PER_RUN * (1 - coilFocus(coilTier))));
            markDirty();
        }
    }

    //////////////////////////////////////
    // ******* Magnetic field ********//
    //////////////////////////////////////

    @Override
    public void onLoad() {
        super.onLoad();
        if (extended && !upgraded) upgraded = true; // stations built as Mk2 before the upgrade item existed keep their size
        OrbitalField.add(this);
    }

    @Override
    public void onUnload() {
        super.onUnload();
        OrbitalField.remove(this);
    }

    @Override
    public void onStructureFormed() {
        super.onStructureFormed();
        // the Mk2's core-tube coil collars are one type all over the tower (GT's coil predicate); the basic station's are fixed
        coilTier = extended && getMultiblockState().getMatchContext().get("CoilType") instanceof ICoilType coil ?
                coil.getTier() : -1;
        linkModules(extended ? getMultiblockState().getMatchContext().get(MODULE_POS_KEY) : null);
        forceAuxiliaryHatches();
        // the chamber's rotors turn in the open, whatever way the holders were placed
        for (IMultiPart part : getParts()) {
            if (part instanceof IRotorHolderMachine && part.self().getFrontFacing() != Direction.UP) {
                part.self().setFrontFacing(Direction.UP);
            }
        }
        markDirty();
    }

    /**
     * The Array Mk2 takes its primary power only from wireless receivers (in Primary mode) and laser hatches: every plain
     * energy input hatch is held in Auxiliary mode (a wireless receiver keeps the mode it is set to) (a screwdriver cannot switch it back; it is set again every tick).
     */
    private void forceAuxiliaryHatches() {
        if (!extended || isRemote()) return;
        for (IMultiPart part : getParts()) {
            if (part instanceof EnergyHatchPartMachine hatch && hatch.energyContainer.getHandlerIO() == IO.IN &&
                    !hatch.isAuxiliaryInput()) {
                hatch.setAuxiliary(true);
            }
        }
    }

    @Override
    public void onStructureInvalid() {
        super.onStructureInvalid();
        unlinkModules();
        coilTier = -1;
        fieldActive = false;
        startupTicks = 0;
        unpoweredTicks = 0;
        focus = 0;
        focusLocked = false;
        lockUnpaid = 0;
        intervalPartsTick = -1;
        intervalComputationTick = -1;
    }

    //////////////////////////////////////
    // ********** Start-up ***********//
    //////////////////////////////////////

    /**
     * Every tick while formed (the lines' vacuum tick): the light ring burns what touches it; every
     * {@link #VACUUM_INTERVAL} ticks the start-up instead of a pump-down. Switched on and powered, the systems draw
     * the lines' pump power and start up in {@link #STARTUP_SECONDS}; switched off, or {@link #POWER_GRACE_TICKS}
     * without power, the station shuts down and starts from 0 again. The magnetic field is on from the first second.
     */
    @Override
    protected void updateVacuum() {
        if (!isFormed()) return;
        forceAuxiliaryHatches();
        if (getOffsetTimer() % 2 == 0 && isRingLit()) burnRingTouchers();
        if (getOffsetTimer() % VACUUM_INTERVAL != 0) return;
        int before = startupTicks;
        boolean on = getRecipeLogic().isWorkingEnabled();
        long drain = pumpDrainPerInterval();
        boolean paid = on && drain > 0 && (getBaseAuxiliaryEUt() > 0 && getRecipeLogic().isWorking() || drawStartupPower(drain));
        if (auxPowered != paid) {
            auxPowered = paid;
            markDirty();
        }
        if (paid) {
            unpoweredTicks = 0;
            startupTicks = Math.min(STARTUP_TICKS, startupTicks + VACUUM_INTERVAL);
        } else {
            unpoweredTicks = Math.min(POWER_GRACE_TICKS, unpoweredTicks + VACUUM_INTERVAL);
            if (!on || unpoweredTicks >= POWER_GRACE_TICKS) startupTicks = 0;
        }
        fieldActive = on && startupTicks > 0;
        if (startupTicks != before) markDirty();
        updateFocus();
    }

    /**
     * The start-up power of one interval: the auxiliary hatches pay it, else the primary ones. A running print pays it
     * through the auxiliary power ({@link #getBaseAuxiliaryEUt()}), so the start-up draws nothing then.
     */
    private boolean drawStartupPower(long drain) {
        if (hasAuxiliary()) return drawAuxiliary(drain);
        if (energyContainer != null && energyContainer.getEnergyStored() >= drain) {
            energyContainer.removeEnergy(drain);
            return true;
        }
        return false;
    }

    /** Voltage that sets the start-up power: the auxiliary hatches', else the primary ones'. */
    @Override
    public long pumpDrainPerInterval() {
        long voltage = 0;
        for (var part : getAuxiliaryParts()) voltage = Math.max(voltage, part.getAuxiliaryContainer().getInputVoltage());
        if (voltage <= 0 && energyContainer != null) voltage = energyContainer.getHighestInputVoltage();
        return (long) (voltage / 8 * VACUUM_INTERVAL * moduleAux());
    }

    /** The auxiliary hatches keep the systems up: the start-up power every tick (0 without auxiliary hatches). */
    @Override
    protected long getBaseAuxiliaryEUt() {
        return isFormed() && hasAuxiliary() ? pumpDrainPerInterval() / VACUUM_INTERVAL : 0;
    }

    /** Start-up progress, 0 to 100. */
    public double getStartupPercent() {
        return isFormed() ? 100.0 * startupTicks / STARTUP_TICKS : 0;
    }

    /** The start-up as the lines' vacuum states: off, starting ({@link #VACUUM_PUMPING}), ready ({@link #VACUUM_SEALED}). */
    @Override
    public int getVacuumState() {
        if (!isFormed() || startupTicks <= 0) return VACUUM_OFF;
        return startupTicks >= STARTUP_TICKS ? VACUUM_SEALED : VACUUM_PUMPING;
    }

    /** Space: always a perfect vacuum, so the break roll uses the node's base chance. */
    @Override
    public double getCleanliness() {
        return 100;
    }

    @Override
    public double getPrintVacuum() {
        return 100;
    }

    @Override
    protected double rollVacuum() {
        return 100;
    }

    @Override
    public int notReadyStatus() {
        return ConsoleWidget.STATUS_STARTING_UP;
    }

    //////////////////////////////////////
    // ******* Magnetic field ********//
    //////////////////////////////////////

    public boolean isFieldActive() {
        return fieldActive && isFormed();
    }

    /** Whether the auxiliary power is paying the start-up in full right now (the Mk2's rings spin then). */
    public boolean isAuxPowered() {
        return auxPowered && isFormed();
    }

    /**
     * The field: the station's own box (basic: {@link #HALF_WIDTH} to the sides, {@link #DEPTH} behind; Mk2:
     * {@link #HALF_WIDTH_MK2}/{@link #DEPTH_MK2}) and 4 blocks in front of it (above the deck, when it faces up), so
     * players stand and hop on the deck; nothing around it.
     */
    public AABB fieldBox() {
        Direction front = getFrontFacing();
        Direction up = RelativeDirection.UP.getRelative(front, getUpwardsFacing(), isFlipped());
        if (fieldBox == null || front != fieldFront || up != fieldUp) {
            Direction left = RelativeDirection.LEFT.getRelative(front, getUpwardsFacing(), isFlipped());
            BlockPos controller = getPos();
            int half = extended ? HALF_WIDTH_MK2 : HALF_WIDTH;
            int depth = extended ? DEPTH_MK2 : DEPTH;
            BlockPos a = controller.relative(left, half).relative(up, half).relative(front, -depth);
            BlockPos b = controller.relative(left, -half).relative(up, -half);
            AABB box = new AABB(a).minmax(new AABB(b));
            fieldBox = box.expandTowards(front.getStepX() * 4, front.getStepY() * 4, front.getStepZ() * 4);
            fieldFront = front;
            fieldUp = up;
        }
        return fieldBox;
    }

    //////////////////////////////////////
    // ********* Light ring *********//
    //////////////////////////////////////

    @Override
    public boolean isRingLit() {
        return getRecipeLogic().isWorking();
    }

    /** The centre of a ring {@code back} levels behind the controller, world coordinates. */
    public Vec3 ringCentre(float back) {
        Direction front = getFrontFacing();
        Direction up = RelativeDirection.UP.getRelative(front, getUpwardsFacing(), isFlipped());
        Direction behind = RelativeDirection.BACK.getRelative(front, getUpwardsFacing(), isFlipped());
        return Vec3.atCenterOf(getPos()).add(
                up.getStepX() * RING_UP + behind.getStepX() * back,
                up.getStepY() * RING_UP + behind.getStepY() * back,
                up.getStepZ() * RING_UP + behind.getStepZ() * back);
    }

    /** The lit rings burn every living thing touching them (players in creative or spectator mode aside). */
    private void burnRingTouchers() {
        if (!(getLevel() instanceof ServerLevel level)) return;
        Vec3 axis = Vec3.atLowerCornerOf(getFrontFacing().getNormal());
        for (RingSpec ring : ringSpecs(RING_BACK, RING_RADIUS)) {
            Vec3 centre = ringCentre(ring.back());
            AABB near = new AABB(centre, centre).inflate(ring.radius() + RING_BURN + 2);
            for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, near,
                    OrbitalLithographyMachine::canBurn)) {
                if (touchesRing(entity.getBoundingBox(), centre, axis, ring.radius())) burn(level, entity);
            }
        }
    }

    private static boolean canBurn(LivingEntity entity) {
        if (!entity.isAlive() || entity.isSpectator()) return false;
        return !(entity instanceof Player player) || !player.getAbilities().invulnerable;
    }

    /**
     * Whether the box comes within {@link #RING_BURN} of the ring's core circle: checked at five heights of the box's
     * middle line, the box's half width added.
     */
    public static boolean touchesRing(AABB box, Vec3 centre, Vec3 axis) {
        return touchesRing(box, centre, axis, RING_RADIUS);
    }

    /** As {@link #touchesRing(AABB, Vec3, Vec3)} for an explicit ring radius (the Array Mk2's larger ring). */
    public static boolean touchesRing(AABB box, Vec3 centre, Vec3 axis, float radius) {
        double reach = RING_BURN + Math.max(box.getXsize(), box.getZsize()) / 2;
        Vec3 middle = box.getCenter();
        for (int i = 0; i <= 4; i++) {
            Vec3 point = new Vec3(middle.x, box.minY + box.getYsize() * i / 4, middle.z);
            Vec3 offset = point.subtract(centre);
            double along = offset.dot(axis);
            double fromCircle = offset.subtract(axis.scale(along)).length() - radius;
            if (fromCircle * fromCircle + along * along < reach * reach) return true;
        }
        return false;
    }

    /** Sets it alight and kills it with the ring's damage; a player it kills gets the ring's death screen. */
    private static void burn(ServerLevel level, LivingEntity entity) {
        double y = entity.getY() + entity.getBbHeight() / 2;
        entity.setSecondsOnFire(10);
        level.sendParticles(ParticleTypes.FLAME, entity.getX(), y, entity.getZ(), 30, 0.3, 0.6, 0.3, 0.03);
        level.sendParticles(ParticleTypes.LAVA, entity.getX(), y, entity.getZ(), 8, 0.3, 0.5, 0.3, 0);
        level.playSound(null, entity.getX(), y, entity.getZ(), SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 1.2F,
                0.6F);
        boolean killed = entity.hurt(AF9DamageTypes.orbitalRing(level), RING_DAMAGE) && entity.isDeadOrDying();
        if (killed && entity instanceof ServerPlayer player) AF9Network.sendRingDeath(player);
    }

    /** The colour of the node being printed (the console's mode colour; plasma violet while soldering). */
    @Override
    public int getRingColor() {
        GTRecipe recipe = getRecipeLogic().getLastRecipe();
        if (recipe != null && "plasma_soldering".equals(recipe.recipeType.registryName.getPath())) {
            return 0xFFB47CFF;
        }
        LithoMode mode = recipe == null ? null : LithoMode.of(recipe.recipeType);
        LithoMode active = mode != null ? mode : getActiveMode();
        return active != null ? active.argb : 0xFFB47CFF;
    }

    /** Plasma soldering glows hotter than lithography: a fatter white-hot core with bloom. */
    @Override
    public float ringGlow() {
        GTRecipe recipe = getRecipeLogic().getLastRecipe();
        if (recipe != null && "plasma_soldering".equals(recipe.recipeType.registryName.getPath())) return 2F;
        return 1F;
    }

    //////////////////////////////////////
    // ********** Preview ***********//
    //////////////////////////////////////

    /**
     * The structure preview with the controller facing up, as it is built. GT draws every preview for a controller
     * facing north, which would stand the platform on its edge; this turns GT's preview into the controller-up
     * orientation (facing up, upwards north) and faces the controller up.
     */
    public static List<MultiblockShapeInfo> previewShapes(MultiblockMachineDefinition definition) {
        return shapes(definition, definition.getPatternFactory().get(), false);
    }

    /** The tower's half width in a cut-out Mk2 page (the plane axes are cut to this many blocks round the middle). */
    private static final int PREVIEW_CUT = 36;

    private static List<MultiblockShapeInfo> shapes(MultiblockMachineDefinition definition, BlockPattern pattern,
                                                    boolean cut) {
        int[] repetition = new int[pattern.aisleRepetitions.length];
        for (int i = 0; i < repetition.length; i++) repetition[i] = pattern.aisleRepetitions[i][0];
        BlockInfo[][][] north = pattern.getPreview(repetition);
        if (cut) north = cutOut(north);
        int[][] turn;
        try {
            turn = previewTurn(pattern);
        } catch (ReflectiveOperationException e) {
            AF9Core.LOGGER.warn("Orbital station preview: cannot turn GT's preview, showing it as GT draws it", e);
            return List.of(new MultiblockShapeInfo(solid(north)));
        }
        int sx = north.length, sy = north[0].length, sz = north[0][0].length;
        int[] min = { Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE };
        int[] max = { Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE };
        int[][][][] target = new int[sx][sy][sz][];
        for (int x = 0; x < sx; x++) {
            for (int y = 0; y < sy; y++) {
                for (int z = 0; z < sz; z++) {
                    int[] p = apply(turn, x, y, z);
                    target[x][y][z] = p;
                    for (int i = 0; i < 3; i++) {
                        min[i] = Math.min(min[i], p[i]);
                        max[i] = Math.max(max[i], p[i]);
                    }
                }
            }
        }
        BlockInfo[][][] up = new BlockInfo[max[0] - min[0] + 1][max[1] - min[1] + 1][max[2] - min[2] + 1];
        for (int x = 0; x < sx; x++) {
            for (int y = 0; y < sy; y++) {
                for (int z = 0; z < sz; z++) {
                    int[] p = target[x][y][z];
                    up[p[0] - min[0]][p[1] - min[1]][p[2] - min[2]] =
                            turnedInfo(north[x][y][z], turn, definition);
                }
            }
        }
        return List.of(new MultiblockShapeInfo(solid(up)));
    }

    /**
     * The tower out of the Mk2's preview: every axis that is the pattern's full 73 wide is cut to the blocks within
     * {@link #PREVIEW_CUT} of its middle, the depth axis stays whole.
     */
    private static BlockInfo[][][] cutOut(BlockInfo[][][] full) {
        int[] size = { full.length, full[0].length, full[0][0].length };
        int[] from = new int[3], to = new int[3];
        for (int i = 0; i < 3; i++) {
            boolean plane = size[i] == 2 * HALF_WIDTH_MK2 + 1;
            from[i] = plane ? HALF_WIDTH_MK2 - PREVIEW_CUT : 0;
            to[i] = plane ? HALF_WIDTH_MK2 + PREVIEW_CUT + 1 : size[i];
        }
        BlockInfo[][][] cut = new BlockInfo[to[0] - from[0]][to[1] - from[1]][to[2] - from[2]];
        for (int x = from[0]; x < to[0]; x++) {
            for (int y = from[1]; y < to[1]; y++) {
                for (int z = from[2]; z < to[2]; z++) {
                    cut[x - from[0]][y - from[1]][z - from[2]] = full[x][y][z];
                }
            }
        }
        return cut;
    }

    /** The linear map from GT's preview (facing north) to the controller-up orientation, as a 3x3 matrix. */
    private static int[][] previewTurn(BlockPattern pattern) throws ReflectiveOperationException {
        Method offset = BlockPattern.class.getDeclaredMethod("setActualRelativeOffset", int.class, int.class,
                int.class, Direction.class, Direction.class, boolean.class);
        offset.setAccessible(true);
        int[][] north = new int[3][3]; // column j: image of pattern axis j
        int[][] up = new int[3][3];
        for (int j = 0; j < 3; j++) {
            int[] e = new int[3];
            e[j] = 1;
            BlockPos n = (BlockPos) offset.invoke(pattern, e[0], e[1], e[2], Direction.NORTH, Direction.UP, false);
            BlockPos u = (BlockPos) offset.invoke(pattern, e[0], e[1], e[2], Direction.UP, Direction.NORTH, false);
            north[0][j] = n.getX();
            north[1][j] = n.getY();
            north[2][j] = n.getZ();
            up[0][j] = u.getX();
            up[1][j] = u.getY();
            up[2][j] = u.getZ();
        }
        // north is a signed permutation: its inverse is its transpose
        int[][] turn = new int[3][3];
        for (int i = 0; i < 3; i++) {
            for (int k = 0; k < 3; k++) {
                int sum = 0;
                for (int j = 0; j < 3; j++) sum += up[i][j] * north[k][j];
                turn[i][k] = sum;
            }
        }
        return turn;
    }

    private static int[] apply(int[][] m, int x, int y, int z) {
        return new int[] { m[0][0] * x + m[0][1] * y + m[0][2] * z, m[1][0] * x + m[1][1] * y + m[1][2] * z,
                m[2][0] * x + m[2][1] * y + m[2][2] * z };
    }

    /** The block turned along: facings follow the turn, the controller faces up. */
    private static BlockInfo turnedInfo(BlockInfo info, int[][] turn, MultiblockMachineDefinition definition) {
        if (info == null) return null;
        BlockState state = info.getBlockState();
        BlockState turned = state;
        if (state.getBlock() == definition.getBlock() && state.hasProperty(BlockStateProperties.FACING)) {
            turned = state.setValue(BlockStateProperties.FACING, Direction.UP);
        } else if (state.hasProperty(BlockStateProperties.FACING)) {
            Direction facing = turn(turn, state.getValue(BlockStateProperties.FACING));
            turned = state.setValue(BlockStateProperties.FACING, facing);
        } else if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            Direction facing = turn(turn, state.getValue(BlockStateProperties.HORIZONTAL_FACING));
            if (facing.getAxis() != Direction.Axis.Y) {
                turned = state.setValue(BlockStateProperties.HORIZONTAL_FACING, facing);
            }
        }
        if (turned == state) return info;
        return BlockInfo.fromBlockState(turned);
    }

    private static Direction turn(int[][] m, Direction direction) {
        int[] v = apply(m, direction.getStepX(), direction.getStepY(), direction.getStepZ());
        Direction turned = Direction.fromDelta(v[0], v[1], v[2]);
        return turned == null ? direction : turned;
    }

    @Override
    public Widget createUIWidget() {
        return OrbitalConsoleWidget.createPage(this);
    }

    /** GT's machine screen with the station's page and a panel on each side of the player inventory. */
    @Override
    public ModularUI createUI(Player entityPlayer) {
        return new ModularUI(OrbitalStationUIWidget.WIDTH, OrbitalStationUIWidget.HEIGHT, this, entityPlayer)
                .widget(new OrbitalStationUIWidget(this));
    }
}
