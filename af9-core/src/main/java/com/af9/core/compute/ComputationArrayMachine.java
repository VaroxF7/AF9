package com.af9.core.compute;

import com.af9.core.bus.BusConnectorPartMachine;
import com.af9.core.machine.part.CoolantHatchPartMachine;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.capability.IOpticalComputationProvider;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiPart;
import com.gregtechceu.gtceu.api.machine.multiblock.PartAbility;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine;
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic;
import com.gregtechceu.gtceu.utils.FormattingUtil;
import com.gregtechceu.gtceu.utils.GTUtil;

import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

import java.util.Collection;
import java.util.List;
import java.util.Locale;

/**
 * The N1 computation arrays: computers built from Computer Racks ({@link ComputerRackPartMachine}) whose cards
 * ({@link ComputeCard}) make computation. No recipes: switched on and formed, the array draws its energy every tick
 * (the cards', the racks', its own) and its coolant every second (the heat of all that, through Coolant Hatches:
 * {@link ComputeCoolant}), and puts out the cards' computation, scaled down by the share of the heat the coolant took.
 * Out of energy it puts out nothing; nothing burns, nothing breaks.
 * <p>
 * It is a GT computation source ({@link IOpticalComputationProvider}, as GT's HPCA): each tick it gives what is asked
 * of it up to its output. A Bus Connector in it puts it on the machine bus
 * ({@link BusConnectorPartMachine#getComputationSource}); a Computation Transmitter Hatch in it feeds GT's Optical
 * Fiber Cable or an ME Computation Link.
 * <p>
 * Two sizes ({@link Spec}): the N1 Computation Array (MV, 3x3x6, eight MV racks) and the N1 Supercomputer Array (LuV,
 * 2x4 across, 7 to 30 long, two racks a slice). Structures in KubeJS ({@code startup_scripts/gtceu/computation.js}).
 */
public class ComputationArrayMachine extends WorkableElectricMultiblockMachine implements IOpticalComputationProvider {

    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(
            ComputationArrayMachine.class, WorkableElectricMultiblockMachine.MANAGED_FIELD_HOLDER);

    /** A size of array: its own draw and heat (controller, fans), on top of the racks. */
    public record Spec(long baseEut, int baseHeat) {}

    public static final Spec ARRAY = new Spec(32, 2);
    public static final Spec SUPERCOMPUTER = new Spec(512, 8);

    private final Spec spec;

    // what the racks add up to (recomputed every second and when a rack changes)
    private ComputeCard.Load load = ComputeCard.Load.EMPTY;
    private int racks;
    private int cards;
    private long eut;
    private int heat;
    private boolean dirty = true;
    // running state
    private boolean powered;
    /** Share of the heat the coolant took last second, 0-1: the output's factor. */
    private double cooling;
    private ComputeCoolant coolant;
    private int coolantPerSecond;
    private int output;
    private long outputTime = -1;
    /** CWU/t given this tick, and last tick's. */
    private int given, lastGiven;
    private long givenTick = -1;

    public ComputationArrayMachine(IMachineBlockEntity holder, Spec spec) {
        super(holder);
        this.spec = spec;
    }

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    @Override
    protected RecipeLogic createRecipeLogic(Object... args) {
        return new ComputeLogic(this);
    }

    @Override
    public void onStructureFormed() {
        super.onStructureFormed();
        dirty = true;
    }

    @Override
    public void onStructureInvalid() {
        super.onStructureInvalid();
        output = 0;
        cooling = 0;
        powered = false;
    }

    /** A rack's cards changed. */
    public void onRacksChanged() {
        dirty = true;
    }

    private void evaluate() {
        ComputeCard.Load total = ComputeCard.Load.EMPTY;
        int rackCount = 0;
        int cardCount = 0;
        for (IMultiPart part : getParts()) {
            if (part instanceof ComputerRackPartMachine rack) {
                rackCount++;
                cardCount += rack.getCards().size();
                total = total.plus(rack.load());
            }
        }
        load = total;
        racks = rackCount;
        cards = cardCount;
        eut = spec.baseEut() + total.eut();
        heat = spec.baseHeat() + total.heat();
    }

    /** The cards' computation at full cooling, CWU/t. */
    public int getRawCWUt() {
        return load.quarterCwut() / 4;
    }

    /**
     * Computation this array puts on the bus now, CWU/t (0 when it did not run this tick or the last: off, unformed,
     * unloaded).
     */
    public int getOutputCWUt() {
        Level level = getLevel();
        if (level == null || !isFormed() || !getRecipeLogic().isWorkingEnabled()) return 0;
        return outputTime >= level.getGameTime() - 1 ? output : 0;
    }

    /** One tick while switched on and formed; the computation it puts out. */
    int computeTick() {
        Level level = getLevel();
        long now = level == null ? 0 : level.getGameTime();
        boolean second = now % 20 == 0;
        if (dirty || second) {
            evaluate();
            dirty = false;
        }
        powered = energyContainer != null && energyContainer.getEnergyStored() >= eut;
        if (powered) energyContainer.removeEnergy(eut);
        // every second, and at once while it has no coolant yet (formed, switched on, power back)
        if (second || coolant == null) cool();
        output = powered ? (int) (load.quarterCwut() * cooling / 4) : 0;
        outputTime = now;
        return output;
    }

    /** Switched off or unformed: nothing out. */
    void idle() {
        output = 0;
        powered = false;
        cooling = 0;
        coolant = null;
        Level level = getLevel();
        outputTime = level == null ? 0 : level.getGameTime();
    }

    /**
     * Drains a second's worth of coolant for the heat (none without power: nothing runs) from the Coolant Hatches,
     * each hatch's fluid taking its share; the output runs at the share of the heat taken until the next second.
     */
    private void cool() {
        int needed = powered ? heat * 20 : 0;
        if (needed <= 0) {
            cooling = 0;
            coolant = null;
            coolantPerSecond = 0;
            return;
        }
        long absorbed = 0;
        int drained = 0;
        ComputeCoolant used = null;
        for (IMultiPart part : getParts()) {
            if (absorbed >= needed) break;
            if (!(part instanceof CoolantHatchPartMachine hatch)) continue;
            FluidStack inside = hatch.tank.getFluidInTank(0);
            ComputeCoolant kind = ComputeCoolant.of(inside);
            if (kind == null) continue;
            int mb = (int) Math.ceil((needed - absorbed) / (double) kind.heatPerMb);
            FluidStack got = hatch.tank.drainInternal(mb, IFluidHandler.FluidAction.EXECUTE);
            absorbed += (long) got.getAmount() * kind.heatPerMb;
            drained += got.getAmount();
            if (got.getAmount() > 0) used = kind;
        }
        cooling = Math.min(1, absorbed / (double) needed);
        coolant = used;
        coolantPerSecond = drained;
    }

    //////////////////////////////////////
    // ********** Computation **********//
    //////////////////////////////////////

    @Override
    public int requestCWUt(int cwut, boolean simulate, Collection<IOpticalComputationProvider> seen) {
        seen.add(this);
        if (cwut <= 0 || getLevel() == null) return 0;
        roll();
        int give = Math.max(0, Math.min(cwut, getOutputCWUt() - given));
        if (!simulate) given += give;
        return give;
    }

    @Override
    public int getMaxCWUt(Collection<IOpticalComputationProvider> seen) {
        seen.add(this);
        return getOutputCWUt();
    }

    @Override
    public boolean canBridge(Collection<IOpticalComputationProvider> seen) {
        seen.add(this);
        return true;
    }

    /** CWU/t it gave last tick. */
    public int getLastGiven() {
        roll();
        return lastGiven;
    }

    private void roll() {
        Level level = getLevel();
        long now = level == null ? 0 : level.getGameTime();
        if (now != givenTick) {
            lastGiven = givenTick == now - 1 ? given : 0;
            given = 0;
            givenTick = now;
        }
    }

    /** Whether its computation leaves it: a Bus Connector or a Computation Transmitter Hatch in it. */
    public boolean hasOutlet() {
        for (IMultiPart part : getParts()) {
            if (part instanceof BusConnectorPartMachine ||
                    PartAbility.COMPUTATION_DATA_TRANSMISSION.isApplicable(part.self().getBlockState().getBlock())) {
                return true;
            }
        }
        return false;
    }

    public int getRacks() {
        return racks;
    }

    public int getHeat() {
        return heat;
    }

    public long getEUt() {
        return eut;
    }

    public double getCooling() {
        return cooling;
    }

    //////////////////////////////////////
    // ************ GUI *************//
    //////////////////////////////////////

    @Override
    public void addDisplayText(List<Component> text) {
        if (!isFormed()) {
            text.add(Component.translatable("gtceu.multiblock.invalid_structure").withStyle(ChatFormatting.RED));
            return;
        }
        boolean on = getRecipeLogic().isWorkingEnabled();
        text.add(Component.translatable(!on ? "af9.compute.array.off" : getOutputCWUt() > 0 ?
                "af9.compute.array.running" : "af9.compute.array.stalled")
                .withStyle(!on ? ChatFormatting.GRAY : getOutputCWUt() > 0 ? ChatFormatting.GREEN :
                        ChatFormatting.RED));
        text.add(Component.translatable("af9.compute.array.racks", racks, cards, load.processors()));
        text.add(Component.translatable("af9.compute.array.cwut", getOutputCWUt(), getRawCWUt())
                .withStyle(ChatFormatting.AQUA));
        if (load.unfed() > 0) {
            text.add(Component.translatable("af9.compute.array.unfed", load.unfed()).withStyle(ChatFormatting.YELLOW));
        }
        text.add(Component.translatable("af9.compute.array.eut", FormattingUtil.formatNumbers(eut),
                GTValues.VNF[GTUtil.getTierByVoltage(eut)]).withStyle(powered || !on ? ChatFormatting.GRAY :
                        ChatFormatting.RED));
        if (on && !powered) {
            text.add(Component.translatable("af9.compute.array.no_power").withStyle(ChatFormatting.RED));
        }
        text.add(Component.translatable("af9.compute.array.heat", heat, heat * 20).withStyle(ChatFormatting.GRAY));
        if (on && powered) {
            if (coolant == null) {
                text.add(Component.translatable("af9.compute.array.no_coolant").withStyle(ChatFormatting.RED));
            } else {
                text.add(Component.translatable("af9.compute.array.cooling",
                        String.format(Locale.ROOT, "%.0f", cooling * 100), Component.translatable(coolant.langKey()),
                        coolantPerSecond).withStyle(cooling >= 1 ? ChatFormatting.GREEN : ChatFormatting.YELLOW));
            }
        }
        text.add(hasOutlet() ?
                Component.translatable("af9.compute.array.given", getLastGiven(), getOutputCWUt())
                        .withStyle(ChatFormatting.GRAY) :
                Component.translatable("af9.compute.array.no_outlet").withStyle(ChatFormatting.DARK_GRAY));
    }

    /** Runs the array every tick instead of looking for recipes (as GT's Central Monitor does). */
    public static class ComputeLogic extends RecipeLogic {

        public ComputeLogic(ComputationArrayMachine machine) {
            super(machine);
        }

        @Override
        public void serverTick() {
            ComputationArrayMachine array = (ComputationArrayMachine) machine;
            if (!array.isFormed() || !isWorkingEnabled()) {
                setStatus(Status.IDLE);
                isActive = false;
                array.idle();
                return;
            }
            boolean running = array.computeTick() > 0;
            setStatus(running ? Status.WORKING : Status.WAITING);
            isActive = running;
        }
    }
}
