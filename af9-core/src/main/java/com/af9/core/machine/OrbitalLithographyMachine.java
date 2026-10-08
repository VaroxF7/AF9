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

import com.gregtechceu.gtceu.api.capability.IOpticalComputationProvider;
import com.gregtechceu.gtceu.api.capability.recipe.CWURecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.FluidRecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.capability.recipe.IRecipeHandler;
import com.gregtechceu.gtceu.api.capability.recipe.ItemRecipeCapability;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import com.gregtechceu.gtceu.api.machine.feature.IMachineLife;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiPart;
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
import com.gregtechceu.gtceu.api.recipe.ingredient.FluidIngredient;
import com.gregtechceu.gtceu.api.recipe.modifier.ModifierFunction;
import com.gregtechceu.gtceu.api.recipe.modifier.RecipeModifier;
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
 * 50 nm (ArF immersion), 20 and 7 nm (EUV) and 1 nm on chromodynium wafers with an X-ray free-electron laser (50A of
 * UHV from a laser hatch and energy hatches).
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
     * The Array Mk2 (extended pattern): the same station with a larger ring, same structure scaled out. The light
     * ring grows with the rim so it stays just inside it; the burn distance and tube stay the same.
     */
    public static final float RING_RADIUS_MK2 = 13.6F;
    /** Distance from the ring's core line within which it burns: the tube and its hottest glow. */
    public static final double RING_BURN = RING_THICKNESS * 2.5;
    /** Damage of the ring: nothing survives it (totems aside). */
    public static final float RING_DAMAGE = 1.0E6F;
    /** The station's extent: 12 blocks to each side of the controller, 17 behind it (below, when it faces up). */
    public static final int HALF_WIDTH = 12;
    public static final int DEPTH = 17;
    /** The Mk2 extended array's extent: 17 to each side, 17 behind (a 35x35 platform, like the Elevator's 35x35). */
    public static final int HALF_WIDTH_MK2 = 17;
    public static final int DEPTH_MK2 = 17;
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
    private static final double FOCUS_SHARP_SPEED = 0.9, FOCUS_LOCKED_SPEED = 0.8, FOCUS_SHARP_BREAK = 0.8,
            FOCUS_LOCKED_BREAK = 0.6;

    /**
     * The pattern of the extended Array Mk2. The startup script builds it together with the basic one (the machine
     * definition's own pattern) and hands it over, the first time GT asks for the definition's pattern — like the
     * Space Elevator's extended tower.
     */
    private static volatile BlockPattern extendedPattern;

    /** Whether the Array Mk2 (extended) size is switched on: persisted like the Elevator's size switch. */
    @Persisted
    private boolean extended;

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
     * ({@link #focusSpeedFactor}, {@link #focusBreakFactor}); it is lost with the structure.</li>
     * </ul>
     */
    @Persisted
    private int focus;

    /** Magnetic field on (formed, switched on, powered); synced for the client's gravity. */
    @DescSynced
    private boolean fieldActive;

    /** Start-up progress, ticks: 0 off, {@link #STARTUP_TICKS} ready. */
    @Persisted
    private int startupTicks;
    private int unpoweredTicks;

    private AABB fieldBox;
    private Direction fieldFront;
    private Direction fieldUp;

    public OrbitalLithographyMachine(IMachineBlockEntity holder) {
        super(holder);
        euvSlot = new NotifiableItemStackHandler(this, 1, IO.IN, IO.NONE)
                .setFilter(OrbitalLithographyMachine::isEuvSource);
        reticleSlot = new NotifiableItemStackHandler(this, 1, IO.IN, IO.NONE)
                .setFilter(OrbitalLithographyMachine::isReticle);
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

    /** A print runs only with its reticle in the reticle slot (GT would also take one from an input bus). */
    @Override
    public boolean canRun(GTRecipe recipe) {
        if (LithoMode.of(recipe.recipeType) == LithoMode.N1 && focus < FOCUS_READY) return false;
        Item reticle = reticleOf(recipe);
        return reticle == null || reticleSlot.getStackInSlot(0).is(reticle);
    }

    /** Broken controller: the EUV Light Source and the reticle drop. */
    @Override
    public void onMachineRemoved() {
        clearInventory(euvSlot.storage);
        clearInventory(reticleSlot.storage);
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
        return mode.onOrbitalStation() && isInOrbit() && (mode != LithoMode.N1 || extended);
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
        if (reticleSlot.getStackInSlot(0).isEmpty()) return ConsoleWidget.STATUS_NO_RETICLE;
        // a computation hatch alone is not enough: something (an HPCA) has to supply the node's CWU/t through it
        if (mode.computation() > 0 && availableComputation() < mode.computation()) {
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

    /** The two pages of the structure preview: the basic station and the Array Mk2. */
    public static List<MultiblockShapeInfo> previews(MultiblockMachineDefinition definition) {
        List<MultiblockShapeInfo> pages = new ArrayList<>(previewShapes(definition));
        BlockPattern big = extendedPattern;
        if (big != null) pages.add(turnedPreview(big, definition));
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

    /** The preview of an arbitrary pattern, turned into the controller-up orientation like previewShapes does. */
    private static MultiblockShapeInfo turnedPreview(BlockPattern pattern,
                                                     MultiblockMachineDefinition definition) {
        int[] repetition = new int[pattern.aisleRepetitions.length];
        for (int i = 0; i < repetition.length; i++) repetition[i] = pattern.aisleRepetitions[i][0];
        BlockInfo[][][] north = pattern.getPreview(repetition);
        int[][] turn;
        try {
            turn = previewTurn(pattern);
        } catch (ReflectiveOperationException e) {
            AF9Core.LOGGER.warn("Orbital array preview: cannot turn GT's preview, showing it as GT draws it", e);
            return new MultiblockShapeInfo(solid(north));
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
        return new MultiblockShapeInfo(solid(up));
    }

    public boolean isExtended() {
        return extended;
    }

    /**
     * Switches between the basic station and the Array Mk2. A formed station is taken apart and checked anew for
     * the other size, as when its controller is turned; a run that is on is lost.
     */
    public void setExtended(boolean extended) {
        if (this.extended == extended) return;
        this.extended = extended;
        focus = 0;
        fieldBox = null;
        markDirty();
        if (isFormed() && getLevel() instanceof ServerLevel serverLevel) {
            onStructureInvalid();
            MultiblockWorldSavedData data = MultiblockWorldSavedData.getOrCreate(serverLevel);
            data.removeMapping(getMultiblockState());
            data.addAsyncLogic(this);
        }
    }

    /** A screwdriver on the controller switches between the basic station and the Array Mk2, between runs. */
    @Override
    protected InteractionResult onScrewdriverClick(Player player, InteractionHand hand, Direction side,
                                                   BlockHitResult hit) {
        if (isRemote()) return InteractionResult.SUCCESS;
        if (getRecipeLogic().isWorking()) {
            player.displayClientMessage(Component.translatable("af9.orbital_array.mk2.busy"), true);
            return InteractionResult.SUCCESS;
        }
        setExtended(!extended);
        player.displayClientMessage(Component.translatable(extended ? "af9.orbital_array.mk2.on" :
                "af9.orbital_array.mk2.off"), true);
        return InteractionResult.SUCCESS;
    }

    /** Ring radius of the size it is formed as (the render reads this through {@link #ringRadius(float)}). */
    public float currentRingRadius() {
        return extended ? RING_RADIUS_MK2 : RING_RADIUS;
    }

    @Override
    public float ringRadius(float modelRadius) {
        return currentRingRadius();
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
        if (!isFormed()) return 0;
        int max = 0;
        for (IRecipeHandler<?> handler : getCapabilitiesFlat(IO.IN, CWURecipeCapability.CAP)) {
            if (handler instanceof IOpticalComputationProvider provider) max += provider.getMaxCWUt();
        }
        return max;
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
        for (IMultiPart part : getParts()) {
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
        return cooled * focusBreakFactor();
    }

    //////////////////////////////////////
    // ********* Beam focus **********//
    //////////////////////////////////////

    /** Types only the Array Mk2 runs besides the 1 nm prints: plasma soldering and Pico fabrication. */
    public static boolean isMk2Type(GTRecipeType type) {
        String path = type.registryName.getPath();
        return path.equals("plasma_soldering") || path.equals("pico_fabrication");
    }

    public int getFocus() {
        return focus;
    }

    @Override
    public int focusPermille() {
        return extended && isFormed() ? focus : -1;
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
        if (!extended || !isFormed()) {
            focus = 0;
        } else if (startupTicks < STARTUP_TICKS || !isInOrbit() || !getRecipeLogic().isWorkingEnabled()) {
            focus = Math.max(0, focus - FOCUS_LOSS_IDLE);
        } else {
            boolean running = getRecipeLogic().isWorking();
            int spare = Math.max(0, availableComputation() - runningComputation());
            int gain = FOCUS_GAIN + Math.min(FOCUS_GAIN_SPARE, spare * FOCUS_GAIN_SPARE / FOCUS_SPARE_FULL);
            focus = Math.max(0, Math.min(FOCUS_MAX, focus + gain - (running ? FOCUS_DRIFT : 0)));
        }
        if (focus != before) markDirty();
    }

    /** Every finished run knocks the beams a little out of focus (Mk2). */
    @Override
    public void afterWorking() {
        super.afterWorking();
        if (extended && focus > 0) {
            focus = Math.max(0, focus - FOCUS_PER_RUN);
            markDirty();
        }
    }

    //////////////////////////////////////
    // ******* Magnetic field ********//
    //////////////////////////////////////

    @Override
    public void onLoad() {
        super.onLoad();
        OrbitalField.add(this);
    }

    @Override
    public void onUnload() {
        super.onUnload();
        OrbitalField.remove(this);
    }

    @Override
    public void onStructureInvalid() {
        super.onStructureInvalid();
        fieldActive = false;
        startupTicks = 0;
        unpoweredTicks = 0;
        focus = 0;
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
        if (getOffsetTimer() % 2 == 0 && isRingLit()) burnRingTouchers();
        if (getOffsetTimer() % VACUUM_INTERVAL != 0) return;
        int before = startupTicks;
        boolean on = getRecipeLogic().isWorkingEnabled();
        long drain = pumpDrainPerInterval();
        if (on && energyContainer != null && drain > 0 && energyContainer.getEnergyStored() >= drain) {
            energyContainer.removeEnergy(drain);
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

    /** The ring's centre, world coordinates. */
    public Vec3 ringCentre() {
        Direction front = getFrontFacing();
        Direction up = RelativeDirection.UP.getRelative(front, getUpwardsFacing(), isFlipped());
        Direction back = RelativeDirection.BACK.getRelative(front, getUpwardsFacing(), isFlipped());
        return Vec3.atCenterOf(getPos()).add(
                up.getStepX() * RING_UP + back.getStepX() * RING_BACK,
                up.getStepY() * RING_UP + back.getStepY() * RING_BACK,
                up.getStepZ() * RING_UP + back.getStepZ() * RING_BACK);
    }

    /** The lit ring burns every living thing touching it (players in creative or spectator mode aside). */
    private void burnRingTouchers() {
        if (!(getLevel() instanceof ServerLevel level)) return;
        Vec3 centre = ringCentre();
        Vec3 axis = Vec3.atLowerCornerOf(getFrontFacing().getNormal());
        float radius = currentRingRadius();
        AABB near = new AABB(centre, centre).inflate(radius + RING_BURN + 2);
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, near,
                OrbitalLithographyMachine::canBurn)) {
            if (touchesRing(entity.getBoundingBox(), centre, axis, radius)) burn(level, entity);
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
        BlockPattern pattern = definition.getPatternFactory().get();
        int[] repetition = new int[pattern.aisleRepetitions.length];
        for (int i = 0; i < repetition.length; i++) repetition[i] = pattern.aisleRepetitions[i][0];
        BlockInfo[][][] north = pattern.getPreview(repetition);
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
