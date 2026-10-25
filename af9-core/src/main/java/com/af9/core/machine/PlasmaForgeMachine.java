package com.af9.core.machine;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.TickableSubscription;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiPart;
import com.gregtechceu.gtceu.api.pattern.TraceabilityPredicate;
import com.gregtechceu.gtceu.api.pattern.error.PatternStringError;
import com.gregtechceu.gtceu.api.recipe.RecipeHelper;
import com.gregtechceu.gtceu.api.recipe.content.ContentModifier;
import com.gregtechceu.gtceu.api.recipe.modifier.ModifierFunction;
import com.gregtechceu.gtceu.api.recipe.modifier.ParallelLogic;
import com.gregtechceu.gtceu.api.recipe.modifier.RecipeModifier;
import com.gregtechceu.gtceu.common.machine.multiblock.part.EnergyHatchPartMachine;
import com.gregtechceu.gtceu.common.machine.multiblock.part.LaserHatchPartMachine;

import com.gregtechceu.gtceu.api.pattern.util.RelativeDirection;

import com.af9.core.common.AF9DamageTypes;
import com.af9.core.machine.console.ConsoleWidget;
import com.af9.core.machine.console.PlasmaForgeConsoleWidget;
import com.af9.core.machine.part.PlasmaHatchPartMachine;
import com.af9.core.network.AF9Network;
import com.af9.core.wireless.WirelessReceiverHatch;

import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.utils.BlockInfo;
import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

/**
 * The Dimensionally Transcendent Plasma Forge (structure and recipes in KubeJS: {@code startup_scripts/gtceu/dtpf.js},
 * {@code server_scripts/mods/gtceu/dtpf.js}), after GTNH's: a vast hall that forges plasma into matter. What it keeps
 * from the original is the running-time ramp: the longer it runs without a pause, the cheaper and the faster it forges
 * ({@link #RAMP}: up to {@value #ENERGY_PERCENT} % less EU/t and {@value #TIME_PERCENT} % shorter runs after
 * {@value #RAMP_MINUTES} minutes), and a pause costs it that warmth ({@value #COOLING_FACTOR} times as fast as it
 * built). It runs only a recipe its hatches can fully supply ({@link #FORGE_GATE}).
 * <p>
 * <b>The magnetic field.</b> The hall holds a field round the beam that is made of osmium plasma: it is built up out of the
 * plasma hatches ({@value #PLASMA_FILL} mB a second at most, up to {@value #FIELD_MAX} mB for a full field) and burns
 * {@value #PLASMA_BURN} mB a second to stay up, while {@value #AUX_AMPS} A of LuV auxiliary power
 * ({@link #AUX_EUT} EU/t, every tick; the energy hatches are held in Auxiliary mode, a wireless receiver keeps the mode
 * it is set to) stabilise it. A freshly built hall has no field at all: it is there only once the stabilisers have their
 * power and the plasma flows. Without the power it falls {@value #FIELD_FALL_FACTOR} times as fast and takes in nothing;
 * with too little plasma it falls slowly. A run starts at {@link #FIELD_READY} (50 %) and is lost below
 * {@link #FIELD_HOLD} (25 %). The weaker the field when a run starts, the longer the run and the dearer its power: up to
 * twice both at {@link #FIELD_HOLD}, normal at a full field ({@link #getFieldPenalty()}). A broken structure or a
 * switched-off controller lets it go.
 * <p>
 * The recipes' own power comes from a single input only: one laser hatch, or one wireless receiver in Primary mode
 * ({@link #isPowerLayoutValid()}). No maintenance, no coolant.
 * <p>
 * A force field burns in the middle of the hall ({@link com.af9.core.client.render.ForceFieldRender}): it is there while
 * the field has any strength, faint while it builds and hard while a recipe runs, and every injection casing feeds it
 * with a beam. <b>It kills</b>: whatever living thing comes within {@value #FIELD_RADIUS} blocks of the beam between the
 * end rings is set alight and takes {@value #FIELD_DAMAGE} damage that no armour, effect or enchantment turns, and a
 * player it kills gets the Orbital Station's ring death screen with the field's own message
 * ({@link AF9DamageTypes#dtpfField}).
 */
public class PlasmaForgeMachine extends ProcessMachine implements IForceFieldMachine {

    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(PlasmaForgeMachine.class,
            ProcessMachine.MANAGED_FIELD_HOLDER);

    /** Minutes of running for the full ramp; the discounts at the full ramp, percent; how much faster it cools. */
    public static final int RAMP_MINUTES = 30, ENERGY_PERCENT = 50, TIME_PERCENT = 25, COOLING_FACTOR = 3;
    public static final int RAMP_TICKS = RAMP_MINUTES * 60 * 20;
    /** Ticks between two updates of the ramp and the field. */
    private static final int RAMP_INTERVAL = 20;

    /**
     * Field strength is the osmium plasma held in the field, mB: a run starts at {@link #FIELD_READY}, the field is
     * strongest at {@link #FIELD_MAX}, a run is lost below {@link #FIELD_HOLD}.
     */
    public static final int FIELD_MAX = 250_000, FIELD_READY = FIELD_MAX / 2, FIELD_HOLD = FIELD_MAX / 4;
    /** An unpowered second loses this many times the burn. */
    private static final int FIELD_FALL_FACTOR = 3;
    /** Osmium plasma a second the hall takes in at most, and burns to stay up. */
    public static final int PLASMA_FILL = 2500, PLASMA_BURN = 576;
    /** The field's heat in Kelvin: what the recipes' {@code forge_heat} is measured against (ten times that with a Superheating Element). */
    public static final int BASE_HEAT = 1000;
    /** The stabilisers: 10 A of LuV, every tick, from the auxiliary hatches. */
    public static final int AUX_AMPS = 10;
    public static final long AUX_EUT = AUX_AMPS * GTValues.V[GTValues.LuV];
    /** Ticks of a second the stabilisers may miss their power before the second counts as unpowered. */
    private static final int AUX_GRACE_TICKS = 3;
    /** The field state the console and Jade show. */
    public static final int FIELD_COLD = 0, FIELD_BUILDING = 1, FIELD_HOLDING = 2, FIELD_FALLING = 3;

    /** The force field: its radius round the beam, and from where to where it runs, blocks behind the controller's centre. */
    public static final float FIELD_RADIUS = 2.6F, FIELD_START = 1.6F, FIELD_END = 40.4F;
    /** What the field does to what touches it (the Orbital Station ring's), and how long it sets it alight, seconds. */
    public static final float FIELD_DAMAGE = 1.0E6F;
    private static final int FIELD_FIRE_SECONDS = 10;

    /** The hall's colour in its console: forge orange. */
    private static final int COLOR = 0xFFFF9A3C;

    /** The recipe data that says how hot the field must be for it, Kelvin ({@link #getFieldHeat()}). */
    public static final String FORGE_HEAT_KEY = "forge_heat";

    /**
     * Only starts a recipe the hatches can fully supply: GT's own voltage check would let a hatch of the tier below
     * start it and then starve it. GT keeps retrying a gated recipe. The field must also be ready (burning osmium
     * plasma and its stabilisers powered), and the recipes' power must come from one input only.
     */
    public static final RecipeModifier FORGE_GATE = (machine, recipe) -> {
        if (!(machine instanceof PlasmaForgeMachine forge)) {
            return RecipeModifier.nullWrongType(PlasmaForgeMachine.class, machine);
        }
        if (!forge.isPowerLayoutValid()) {
            return ModifierFunction.cancel(Component.translatable("af9.dtpf.too_many_inputs"));
        }
        long need = RecipeHelper.getRealEUt(recipe).getTotalEU();
        double penalty = forge.getFieldPenalty();
        need = (long) Math.ceil(need * penalty);
        need = (long) Math.ceil(need * forge.moduleEnergy());
        if (forge.getAvailableEUt() < need) return ModifierFunction.NULL;
        if (!forge.isReady()) return ModifierFunction.cancel(Component.translatable("af9.dtpf.field_low"));
        int heat = recipe.data.contains(FORGE_HEAT_KEY) ? recipe.data.getInt(FORGE_HEAT_KEY) : 0;
        if (heat > forge.getFieldHeat()) {
            return ModifierFunction.cancel(Component.translatable("af9.dtpf.heat_low", heat, forge.getFieldHeat()));
        }
        if (penalty <= 1.0001) return ModifierFunction.IDENTITY;
        return ModifierFunction.builder().eutMultiplier(penalty).durationMultiplier(penalty).build();
    };

    /**
     * The module bonus ({@link DtpfModuleMachine.Kind}): faster runs (Manipulated Celestial Forges), twice the EU/t
     * (the Superheating Element) and parallels (Parallel Processing Assemblies, as many as the energy allows; the hall has
     * no parallel hatch and no batch mode). Place it after {@link #RAMP} and before the overclocks.
     */
    public static final RecipeModifier MODULES = (machine, recipe) -> {
        if (!(machine instanceof PlasmaForgeMachine forge)) {
            return RecipeModifier.nullWrongType(PlasmaForgeMachine.class, machine);
        }
        if (!forge.isFormed()) return ModifierFunction.IDENTITY;
        double time = forge.moduleTime();
        double energy = forge.moduleEnergy();
        int parallels = 1;
        if (forge.moduleParallels() > 1) {
            parallels = ParallelLogic.getParallelAmount(machine, recipe, forge.moduleParallels());
            if (parallels == 0) return ModifierFunction.NULL;
        }
        if (time >= 1 && energy <= 1 && parallels == 1) return ModifierFunction.IDENTITY;
        ModifierFunction.FunctionBuilder builder = ModifierFunction.builder().durationMultiplier(time)
                .eutMultiplier(energy * parallels);
        if (parallels > 1) builder.modifyAllContents(ContentModifier.multiplier(parallels)).parallels(parallels);
        return builder.build();
    };

    /** The running-time ramp: the recipe's EU/t and run time, scaled by how warm the forge has run. */
    public static final RecipeModifier RAMP = (machine, recipe) -> {
        if (!(machine instanceof PlasmaForgeMachine forge)) {
            return RecipeModifier.nullWrongType(PlasmaForgeMachine.class, machine);
        }
        double ramp = forge.getRamp();
        if (ramp <= 0) return ModifierFunction.IDENTITY;
        return ModifierFunction.builder()
                .eutMultiplier(1 - ENERGY_PERCENT / 100.0 * ramp)
                .durationMultiplier(1 - TIME_PERCENT / 100.0 * ramp)
                .build();
    };

    /** Ticks of warmth, 0 to {@link #RAMP_TICKS}. */
    @Persisted
    private int warmth;
    /** Field strength: mB of osmium plasma in the field, 0 to {@link #FIELD_MAX}. */
    @Persisted
    @DescSynced
    private int fieldStrength;
    /** Ticks of the current second the stabilisers did not get their power. */
    private int auxMissed;
    /** What the last full second did to the field: one of {@code FIELD_*}. */
    private int fieldState = FIELD_COLD;
    /** Whether the last second's plasma and stabiliser power were paid. */
    @DescSynced
    private boolean plasmaFed, auxPaid;

    private TickableSubscription rampSubs;

    public PlasmaForgeMachine(IMachineBlockEntity holder) {
        super(holder);
    }

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    @Override
    public String titleKey() {
        return "af9.dtpf.console.title";
    }

    @Override
    public int modeColor(int index) {
        return COLOR;
    }

    /** The forge's own console: the field, the plasma, the stabilisers, the primary power, the ramp. */
    @Override
    public Widget createUIWidget() {
        return PlasmaForgeConsoleWidget.create(this);
    }

    //////////////////////////////////////
    // ************ Ramp *************//
    //////////////////////////////////////

    @Override
    public void onStructureFormed() {
        super.onStructureFormed();
        forceAuxiliaryHatches();
        scanModules();
        rampSubs = subscribeServerTick(rampSubs, this::tickForge);
    }

    /** A broken hall loses its warmth and its field: the next time it forms it starts cold. */
    @Override
    public void onStructureInvalid() {
        super.onStructureInvalid();
        unsubscribeRamp();
        unlinkModules();
        warmth = 0;
        fieldStrength = 0;
        fieldState = FIELD_COLD;
        plasmaFed = false;
        auxPaid = false;
        auxMissed = 0;
        markDirty();
    }

    @Override
    public void onUnload() {
        super.onUnload();
        unsubscribeRamp();
    }

    private void unsubscribeRamp() {
        if (rampSubs != null) {
            rampSubs.unsubscribe();
            rampSubs = null;
        }
    }

    /**
     * Every tick while formed: the stabilisers take their power (a running recipe pays it through the auxiliary power
     * of {@link #getBaseAuxiliaryEUt()}); every second the ramp and the field step.
     */
    private void tickForge() {
        if (!isFormed()) return;
        var logic = getRecipeLogic();
        if (getOffsetTimer() % 2 == 0 && fieldStrength > 0) burnFieldTouchers();
        if (logic.isWorkingEnabled() && !logic.isWorking()) drawAuxiliary(AUX_EUT);
        if (getOffsetTimer() % RAMP_INTERVAL != 0) return;
        forceAuxiliaryHatches();
        scanModules();
        int before = warmth;
        if (logic.isWorking()) {
            warmth = Math.min(RAMP_TICKS, warmth + RAMP_INTERVAL);
        } else {
            warmth = Math.max(0, warmth - RAMP_INTERVAL * COOLING_FACTOR);
        }
        if (warmth != before) markDirty();
        tickField();
    }

    /**
     * Takes osmium plasma out of the plasma hatches into the field and checks the stabilisers' power of the last second.
     * The field burns {@link #getPlasmaPull()} mB a second; powered, the hall draws what the field lacks (up to
     * {@link #PLASMA_FILL}) and the field gains what it drew less the burn. Unpowered it takes in nothing and loses three
     * times the burn. A switched-off controller lets the field go the same way.
     */
    private void tickField() {
        int before = fieldStrength;
        boolean enabled = getRecipeLogic().isWorkingEnabled();
        int burn = getPlasmaPull();
        boolean sequenced = enabled && moduleCount(DtpfModuleMachine.Kind.SEQUENCER) > 0;
        auxPaid = enabled && auxMissed <= AUX_GRACE_TICKS;
        auxMissed = 0;
        if (auxPaid) {
            long want = Math.min(PLASMA_FILL, FIELD_MAX - fieldStrength + burn);
            long got = drainPlasma(want);
            plasmaFed = got >= burn;
            fieldStrength = (int) Math.max(0, Math.min(FIELD_MAX, fieldStrength + got - burn));
            if (sequenced && fieldStrength > 0) fieldStrength = Math.max(fieldStrength, sequencerFloor());
            if (fieldStrength <= 0) fieldState = FIELD_COLD;
            else if (got < burn) fieldState = FIELD_FALLING;
            else fieldState = fieldStrength >= FIELD_MAX - burn ? FIELD_HOLDING : FIELD_BUILDING;
        } else {
            plasmaFed = false;
            fieldStrength = Math.max(0, fieldStrength - burn * FIELD_FALL_FACTOR);
            if (sequenced && before > 0) fieldStrength = Math.max(fieldStrength, sequencerFloor());
            fieldState = fieldStrength > 0 ? FIELD_FALLING : FIELD_COLD;
        }
        if (fieldStrength != before) markDirty();
    }

    /** mB of osmium plasma drawn from the plasma hatches this second. */
    private long drainPlasma(long need) {
        long left = need;
        for (var part : getParts()) {
            if (!(part instanceof PlasmaHatchPartMachine hatch) || left <= 0) continue;
            for (int i = 0; i < hatch.tank.getTanks() && left > 0; i++) {
                FluidStack stack = hatch.tank.getFluidInTank(i);
                if (!PlasmaHatchPartMachine.isPlasma(stack)) continue;
                FluidStack take = new FluidStack(stack, (int) Math.min(left, stack.getAmount()));
                left -= hatch.tank.drain(take, FluidAction.EXECUTE).getAmount();
            }
        }
        return need - left;
    }

    /** mB of osmium plasma in the plasma hatches. */
    public long getPlasmaStored() {
        long amount = 0;
        for (var part : getParts()) {
            if (!(part instanceof PlasmaHatchPartMachine hatch)) continue;
            for (int i = 0; i < hatch.tank.getTanks(); i++) {
                FluidStack stack = hatch.tank.getFluidInTank(i);
                if (PlasmaHatchPartMachine.isPlasma(stack)) amount += stack.getAmount();
            }
        }
        return amount;
    }

    /** mB of osmium plasma a second the field burns (a Stabilisation Sequencer halves it). */
    public int getPlasmaPull() {
        double burn = PLASMA_BURN;
        if (moduleCount(DtpfModuleMachine.Kind.SEQUENCER) > 0) burn *= DtpfModuleMachine.SEQUENCER_BURN;
        return (int) Math.round(burn);
    }

    /** The least strength the Sequencer keeps the field at. */
    private static int sequencerFloor() {
        return (int) Math.round(FIELD_MAX * DtpfModuleMachine.SEQUENCER_FLOOR);
    }

    /**
     * How hot the field is, Kelvin: {@link #BASE_HEAT}, ten times that with a Superheating Element. A recipe needs the
     * field at least as hot as its {@link #FORGE_HEAT_KEY}.
     */
    public int getFieldHeat() {
        return moduleCount(DtpfModuleMachine.Kind.SUPERHEATER) > 0 ? BASE_HEAT * DtpfModuleMachine.HEAT_FACTOR : BASE_HEAT;
    }

    /**
     * How good the field is for a run: 0 at {@link #FIELD_HOLD} (25 %), 1 at a full field.
     */
    public double getFieldQuality() {
        double q = (double) (fieldStrength - FIELD_HOLD) / (FIELD_MAX - FIELD_HOLD);
        return q < 0 ? 0 : Math.min(q, 1);
    }

    /**
     * What a weak field costs a run: its time and its EU/t are multiplied by this, 1 at a full field and 2 at
     * {@link #FIELD_HOLD}. Set when the run starts.
     */
    public double getFieldPenalty() {
        return isFormed() ? 2 - getFieldQuality() : 1;
    }

    //////////////////////////////////////
    // ******** Auxiliary power *******//
    //////////////////////////////////////

    /** The stabilisers' power: 10 A of LuV, whatever the hall is doing. */
    @Override
    protected long getBaseAuxiliaryEUt() {
        return isFormed() ? AUX_EUT : 0;
    }

    /**
     * The stabilisers' draw, whether the hall's tick or a running recipe makes it. A miss is counted for the second's
     * field step; the run is only lost when the field has fallen below {@link #FIELD_HOLD}, so a flicker in the power
     * does not cost a twenty minute run.
     */
    @Override
    public boolean drawAuxiliary(long eut) {
        boolean paid = super.drawAuxiliary(eut);
        if (eut != AUX_EUT) return paid;
        if (!paid) auxMissed++;
        return paid || fieldStrength >= FIELD_HOLD;
    }

    /** A run on a collapsed field is lost. */
    @Override
    public boolean onWorking() {
        // a Stabilisation Sequencer's hall never loses a run to a weak field
        if (fieldStrength < FIELD_HOLD && moduleCount(DtpfModuleMachine.Kind.SEQUENCER) == 0) return false;
        return super.onWorking();
    }

    /**
     * The hall's primary power comes from one input only: every plain energy input hatch is held in Auxiliary mode (a
     * screwdriver cannot switch it back); a wireless receiver keeps the mode it is set to.
     */
    private void forceAuxiliaryHatches() {
        if (isRemote()) return;
        for (IMultiPart part : getParts()) {
            if (part instanceof EnergyHatchPartMachine hatch && hatch.energyContainer.getHandlerIO() == IO.IN &&
                    !hatch.isAuxiliaryInput()) {
                hatch.setAuxiliary(true);
            }
        }
    }

    /** The inputs the recipes' power comes through: the laser hatch and the wireless receivers in Primary mode. */
    public int countPrimaryInputs() {
        int count = 0;
        for (IMultiPart part : getParts()) {
            if (part instanceof LaserHatchPartMachine) {
                count++;
            } else if (part instanceof WirelessReceiverHatch receiver) {
                if (!receiver.isAuxiliaryInput()) count++;
            } else if (part instanceof EnergyHatchPartMachine hatch && hatch.energyContainer.getHandlerIO() == IO.IN &&
                    !hatch.isAuxiliaryInput()) {
                count++;
            }
        }
        return count;
    }

    /** One primary input at most: a laser hatch and a primary wireless receiver together hold the forge. */
    public boolean isPowerLayoutValid() {
        return countPrimaryInputs() <= 1;
    }

    //////////////////////////////////////
    // ********* Modules *************//
    //////////////////////////////////////

    /** The id prefix of the module controllers ({@code gtceu:dtpf_module_<kind>}, {@link DtpfModuleMachine.Kind}). */
    public static final String MODULE_PREFIX = "dtpf_module_";
    private static final String MODULE_COUNT_KEY = "DtpfModuleCounts";
    /**
     * The six slots: behind the middle injection casing at the top and the bottom of the three big rings, {@link #SLOT_BACK}
     * blocks behind the controller and {@link #SLOT_UP} up or down. The controller of a module stands in the slot's middle,
     * facing the field; the {@link #SLOT_CASINGS} (sideways, outwards) round it are its structure.
     */
    private static final int[] SLOT_BACK = { 31, 21, 11 };
    private static final int SLOT_UP = 5;
    private static final int[][] SLOT_CASINGS = { { -3, 0 }, { -2, 0 }, { -1, 0 }, { 1, 0 }, { 2, 0 }, { 3, 0 },
            { -3, 1 }, { -2, 1 }, { 2, 1 }, { 3, 1 }, { -1, -1 }, { 1, -1 } };
    /** Modules of each kind that give their bonus, recounted every {@link #RAMP_INTERVAL} ticks. */
    private final int[] moduleCount = new int[DtpfModuleMachine.Kind.values().length];
    /** Set while {@link #scanModules()} runs (it turns modules, which makes GT form this hall again, which scans again). */
    private boolean scanningModules;

    /**
     * A module controller in a slot, any kind (or nothing): the hall takes only so many of a kind. Its facing is the
     * hall's business ({@link #scanModules()} turns it outwards).
     */
    public static TraceabilityPredicate modules() {
        return new TraceabilityPredicate(state -> {
            DtpfModuleMachine.Kind kind = moduleKindOf(state.getBlockState());
            if (kind == null) return false;
            int[] counts = state.getMatchContext().getOrCreate(MODULE_COUNT_KEY,
                    () -> new int[DtpfModuleMachine.Kind.values().length]);
            if (++counts[kind.ordinal()] > kind.max) {
                state.setError(new PatternStringError("af9.dtpf_module.error.max"));
                return false;
            }
            return true;
        }, PlasmaForgeMachine::moduleCandidates)
                .addTooltips(Component.translatable("af9.dtpf.pattern.module"));
    }

    private static DtpfModuleMachine.Kind moduleKindOf(BlockState state) {
        ResourceLocation key = ForgeRegistries.BLOCKS.getKey(state.getBlock());
        if (key == null || !key.getNamespace().equals("gtceu")) return null;
        return DtpfModuleMachine.Kind.ofBlock(key.getPath());
    }

    private static BlockInfo[] moduleCandidates() {
        return java.util.Arrays.stream(DtpfModuleMachine.Kind.values())
                .map(kind -> ForgeRegistries.BLOCKS.getValue(new ResourceLocation("gtceu", MODULE_PREFIX + kind.id())))
                .filter(block -> block != null && block != net.minecraft.world.level.block.Blocks.AIR)
                .map(BlockInfo::fromBlock).toArray(BlockInfo[]::new);
    }

    /** Where the controller of a slot (0 to 5: ring, then top before bottom) stands. */
    private BlockPos slotCenter(int slot) {
        Direction back = RelativeDirection.BACK.getRelative(getFrontFacing(), getUpwardsFacing(), isFlipped());
        return getPos().relative(back, SLOT_BACK[slot / 2]).relative(slotOutward(slot), SLOT_UP);
    }

    /** Away from the beam: up for the top slots, down for the bottom ones. */
    private Direction slotOutward(int slot) {
        Direction up = RelativeDirection.UP.getRelative(getFrontFacing(), getUpwardsFacing(), isFlipped());
        return slot % 2 == 0 ? up : up.getOpposite();
    }

    /** Whether all {@value DtpfModuleMachine#CASINGS} casings round the slot's controller are Module Casings. */
    private boolean slotComplete(int slot) {
        if (getLevel() == null) return false;
        Direction left = RelativeDirection.LEFT.getRelative(getFrontFacing(), getUpwardsFacing(), isFlipped());
        Direction out = slotOutward(slot);
        BlockPos center = slotCenter(slot);
        for (int[] casing : SLOT_CASINGS) {
            BlockPos pos = center.relative(left, casing[0]).relative(out, casing[1]);
            ResourceLocation key = ForgeRegistries.BLOCKS.getKey(getLevel().getBlockState(pos).getBlock());
            if (key == null || !key.getNamespace().equals("af9") || !key.getPath().equals("dtpf_module_casing")) {
                return false;
            }
        }
        return true;
    }

    /**
     * Looks at the six slots: a module controller in one is turned to face outwards (away from the field, to the player) and linked, and counts when all its
     * casings stand (a hall takes only so many of a kind). Runs when the hall forms and every second, so a module set up
     * or broken later is noticed.
     */
    private void scanModules() {
        if (getLevel() == null || getLevel().isClientSide || scanningModules) return;
        // turning a module changes its block state, and GT then checks this hall's structure again, which forms it and
        // scans again: only the outermost scan may turn anything
        scanningModules = true;
        try {
            java.util.Arrays.fill(moduleCount, 0);
            for (int slot = 0; slot < 6; slot++) {
                if (!(MetaMachine.getMachine(getLevel(), slotCenter(slot)) instanceof DtpfModuleMachine module)) continue;
                Direction outwards = slotOutward(slot);
                if (module.getFrontFacing() != outwards) module.setFrontFacing(outwards);
                DtpfModuleMachine.Kind kind = module.getKind();
                boolean counted = slotComplete(slot) && moduleCount[kind.ordinal()] < kind.max;
                if (counted) moduleCount[kind.ordinal()]++;
                module.link(getPos().asLong(), counted);
            }
        } finally {
            scanningModules = false;
        }
    }

    private void unlinkModules() {
        java.util.Arrays.fill(moduleCount, 0);
        if (getLevel() == null || getLevel().isClientSide) return;
        for (int slot = 0; slot < 6; slot++) {
            if (MetaMachine.getMachine(getLevel(), slotCenter(slot)) instanceof DtpfModuleMachine module) {
                module.unlink();
            }
        }
    }

    /** Modules of a kind that give their bonus now (a client sees the last sync of its own count: always 0). */
    public int moduleCount(DtpfModuleMachine.Kind kind) {
        return isFormed() ? moduleCount[kind.ordinal()] : 0;
    }

    public int modulesTotal() {
        int sum = 0;
        for (DtpfModuleMachine.Kind kind : DtpfModuleMachine.Kind.values()) sum += moduleCount(kind);
        return sum;
    }

    /** The factor of the run time: every Celestial Forge makes the runs 1.5 times as fast. */
    public double moduleTime() {
        return Math.pow(1 / DtpfModuleMachine.FORGE_SPEED, moduleCount(DtpfModuleMachine.Kind.FORGE));
    }

    /** The factor of the recipes' EU/t: a Superheating Element doubles it. */
    public double moduleEnergy() {
        return Math.pow(DtpfModuleMachine.HEAT_ENERGY, moduleCount(DtpfModuleMachine.Kind.SUPERHEATER));
    }

    /** The parallels the Parallel Processing Assemblies allow (1 without them; six make 61, under the pack's 64). */
    public int moduleParallels() {
        return 1 + DtpfModuleMachine.PARALLEL_STEP * moduleCount(DtpfModuleMachine.Kind.PARALLEL);
    }

    //////////////////////////////////////
    // ********* The deadly field *******//
    //////////////////////////////////////

    /** The field burns every living thing that touches it (players in creative or spectator mode aside). */
    private void burnFieldTouchers() {
        if (!(getLevel() instanceof ServerLevel level)) return;
        Direction back = RelativeDirection.BACK.getRelative(getFrontFacing(), getUpwardsFacing(), isFlipped());
        Vec3 axis = new Vec3(back.getStepX(), back.getStepY(), back.getStepZ());
        Vec3 origin = Vec3.atCenterOf(getPos());
        AABB near = new AABB(origin.add(axis.scale(FIELD_START)), origin.add(axis.scale(FIELD_END)))
                .inflate(FIELD_RADIUS + 1);
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, near,
                PlasmaForgeMachine::canBurn)) {
            if (touchesField(entity.getBoundingBox(), origin, axis)) burn(level, entity);
        }
    }

    private static boolean canBurn(LivingEntity entity) {
        if (!entity.isAlive() || entity.isSpectator()) return false;
        return !(entity instanceof Player player) || !player.getAbilities().invulnerable;
    }

    /**
     * Whether the box reaches into the field: the tube of {@link #FIELD_RADIUS} round the axis between
     * {@link #FIELD_START} and {@link #FIELD_END}, the box's half width added. Checked at five heights of the box's
     * middle line.
     */
    public static boolean touchesField(AABB box, Vec3 origin, Vec3 axis) {
        double half = Math.max(box.getXsize(), box.getZsize()) / 2;
        Vec3 middle = box.getCenter();
        for (int i = 0; i <= 4; i++) {
            Vec3 point = new Vec3(middle.x, box.minY + box.getYsize() * i / 4, middle.z);
            Vec3 offset = point.subtract(origin);
            double along = offset.dot(axis);
            if (along < FIELD_START - half || along > FIELD_END + half) continue;
            if (offset.subtract(axis.scale(along)).length() < FIELD_RADIUS + half) return true;
        }
        return false;
    }

    /** Sets it alight and kills it with the field's damage; a player it kills gets the vaporised death screen. */
    private static void burn(ServerLevel level, LivingEntity entity) {
        double y = entity.getY() + entity.getBbHeight() / 2;
        entity.setSecondsOnFire(FIELD_FIRE_SECONDS);
        level.sendParticles(ParticleTypes.FLAME, entity.getX(), y, entity.getZ(), 30, 0.3, 0.6, 0.3, 0.03);
        level.sendParticles(ParticleTypes.LAVA, entity.getX(), y, entity.getZ(), 8, 0.3, 0.5, 0.3, 0);
        level.playSound(null, entity.getX(), y, entity.getZ(), SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 1.2F,
                0.6F);
        boolean killed = entity.hurt(AF9DamageTypes.dtpfField(level), FIELD_DAMAGE) && entity.isDeadOrDying();
        if (killed && entity instanceof ServerPlayer player) AF9Network.sendRingDeath(player);
    }

    // ---- the renderer's view ----

    /** The field is there while it has any strength: it is what the renderer draws, and what kills. */
    @Override
    public boolean isFieldFormed() {
        return isFormed() && fieldStrength > 0;
    }

    @Override
    public float getFieldLevel() {
        return (float) getFieldStrength() / FIELD_MAX;
    }

    @Override
    public boolean isFieldInjecting() {
        return isFormed() && plasmaFed && auxPaid;
    }

    @Override
    public boolean isFieldBurning() {
        return isFormed() && getRecipeLogic().isWorking();
    }

    /** The field is strong enough to forge. */
    public boolean isReady() {
        return isFormed() && fieldStrength >= FIELD_READY;
    }

    /** Field strength, 0 to {@link #FIELD_MAX}. */
    public int getFieldStrength() {
        return isFormed() ? fieldStrength : 0;
    }

    /** One of {@code FIELD_*}. */
    public int getFieldState() {
        return isFormed() ? fieldState : FIELD_COLD;
    }

    public boolean isPlasmaFed() {
        return isFormed() && plasmaFed;
    }

    public boolean isAuxPaid() {
        return isFormed() && auxPaid;
    }

    /** How warm the forge has run, 0 to 1. */
    public double getRamp() {
        return isFormed() ? (double) warmth / RAMP_TICKS : 0;
    }

    @Override
    public int getStatus() {
        if (!isFormed()) return ConsoleWidget.STATUS_OFFLINE;
        var logic = getRecipeLogic();
        if (!logic.isWorkingEnabled()) return ConsoleWidget.STATUS_PAUSED;
        if (!isPowerLayoutValid()) return ConsoleWidget.STATUS_TOO_MANY_INPUTS;
        if (logic.isWorking()) return ConsoleWidget.STATUS_RUNNING;
        if (!isReady()) {
            return fieldState == FIELD_BUILDING ? ConsoleWidget.STATUS_STARTING_UP : ConsoleWidget.STATUS_NO_FIELD;
        }
        if (logic.isWaiting() || getAvailableEUt() < minimumEUt()) return ConsoleWidget.STATUS_NO_POWER;
        return ConsoleWidget.STATUS_IDLE;
    }

    @Override
    public List<Component> infoLines() {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable("af9.dtpf.console.field", Math.round(100.0 * getFieldStrength() / FIELD_MAX),
                Component.translatable("af9.dtpf.console.field." + getFieldState())));
        lines.add(Component.translatable(isPlasmaFed() ? "af9.dtpf.console.plasma_fed" : "af9.dtpf.console.plasma_dry",
                getPlasmaPull()));
        lines.add(Component.translatable(isAuxPaid() ? "af9.dtpf.console.aux_paid" : "af9.dtpf.console.aux_short",
                ConsoleWidget.compact(AUX_EUT)));
        double ramp = getRamp();
        lines.add(Component.translatable("af9.dtpf.console.discount", Math.round(ENERGY_PERCENT * ramp),
                Math.round(TIME_PERCENT * ramp)));
        return lines;
    }
}
