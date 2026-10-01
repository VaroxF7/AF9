package com.af9.core.machine;

import com.af9.core.bus.BusConnectorPartMachine;
import com.af9.core.bus.BusNetwork;

import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiController;
import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockDisplayText;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine;
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic;

import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Metrology Station (structure and recipe type in KubeJS): the fab's measuring tool. It sits on the machine bus; a run
 * (a calibration wafer and some computation) measures the critical dimensions and the overlay of every lithography
 * machine on its bus network, and calibrates them all ({@link LithoMachine#calibrate}). For {@link #FEEDBACK_TICKS}
 * after a run, and while one is running, the station feeds its measurements back into the machines' alignment and
 * dose: their prints break {@link com.af9.core.litho.LithoMode#METROLOGY_FACTOR} as often ({@link #isFeedbackActive}).
 */
public class MetrologyStationMachine extends WorkableElectricMultiblockMachine {

    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(
            MetrologyStationMachine.class, WorkableElectricMultiblockMachine.MANAGED_FIELD_HOLDER);

    /** Ticks the feedback lasts after a finished run: ten minutes. */
    public static final long FEEDBACK_TICKS = 12000;

    /** Game time the feedback lasts until (-1: none yet). */
    @Persisted
    private long feedbackUntil = -1;
    /** Machines the last run calibrated. */
    @Persisted
    private int lastCalibrated;

    public MetrologyStationMachine(IMachineBlockEntity holder) {
        super(holder);
    }

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    @Override
    protected RecipeLogic createRecipeLogic(Object... args) {
        return new MetrologyLogic(this);
    }

    /** A run finished: calibrate the lithography machines on the bus network and start the feedback. */
    private void runFinished() {
        Level level = getLevel();
        if (level == null || level.isClientSide) return;
        int count = 0;
        for (LithoMachine litho : getLithoMachines()) {
            litho.calibrate();
            count++;
        }
        lastCalibrated = count;
        feedbackUntil = level.getGameTime() + FEEDBACK_TICKS;
        markDirty();
    }

    /** The lithography machines on the bus network of this station (empty off the bus). */
    public List<LithoMachine> getLithoMachines() {
        List<LithoMachine> machines = new ArrayList<>();
        BusConnectorPartMachine own = BusConnectorPartMachine.of(this);
        if (own == null) return machines;
        for (BusNetwork.Bus bus : own.getNetwork().buses()) {
            for (BusConnectorPartMachine connector : bus.connectors()) {
                if (connector.isInValid()) continue;
                IMultiController controller = connector.getMachineController();
                if (controller instanceof LithoMachine litho && !machines.contains(litho)) machines.add(litho);
            }
        }
        return machines;
    }

    /** A run is measuring or finished lately: the machines on the bus get their feedback. */
    public boolean isFeedbackActive() {
        Level level = getLevel();
        return isFormed() && level != null && (getRecipeLogic().isWorking() || level.getGameTime() < feedbackUntil);
    }

    /** Ticks of feedback left after the last run (0: none, or a run is measuring). */
    public long getFeedbackTicksLeft() {
        Level level = getLevel();
        return level == null ? 0 : Math.max(0, feedbackUntil - level.getGameTime());
    }

    @Override
    public void addDisplayText(List<Component> text) {
        MultiblockDisplayText.builder(text, isFormed())
                .setWorkingStatus(recipeLogic.isWorkingEnabled(), recipeLogic.isActive())
                .addEnergyUsageLine(energyContainer)
                .addWorkingStatusLine();
        if (!isFormed()) return;
        BusConnectorPartMachine connector = BusConnectorPartMachine.of(this);
        if (connector == null || !connector.isOnBus()) {
            text.add(Component.translatable("af9.metrology.no_bus").withStyle(ChatFormatting.RED));
            return;
        }
        List<LithoMachine> machines = getLithoMachines();
        text.add(Component.translatable("af9.metrology.machines", machines.size()).withStyle(ChatFormatting.GRAY));
        for (LithoMachine litho : machines) {
            text.add(Component.translatable("af9.metrology.machine", litho.getBlockState().getBlock().getName(),
                    String.format(Locale.ROOT, "%.0f", litho.getCalibration()))
                    .withStyle(litho.getCalibration() < 70 ? ChatFormatting.YELLOW : ChatFormatting.DARK_GRAY));
        }
        if (isFeedbackActive()) {
            long left = getFeedbackTicksLeft() / 20;
            text.add(Component.translatable(getRecipeLogic().isWorking() ? "af9.metrology.measuring" :
                    "af9.metrology.feedback", left / 60, String.format("%02d", left % 60))
                    .withStyle(ChatFormatting.GREEN));
        } else {
            text.add(Component.translatable("af9.metrology.idle").withStyle(ChatFormatting.GRAY));
        }
    }

    /** The recipe logic that tells the station when a run finishes. */
    private static final class MetrologyLogic extends RecipeLogic {

        protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(
                MetrologyLogic.class, RecipeLogic.MANAGED_FIELD_HOLDER);

        private final MetrologyStationMachine station;

        MetrologyLogic(MetrologyStationMachine station) {
            super(station);
            this.station = station;
        }

        @Override
        public ManagedFieldHolder getFieldHolder() {
            return MANAGED_FIELD_HOLDER;
        }

        @Override
        public void onRecipeFinish() {
            super.onRecipeFinish();
            station.runFinished();
        }
    }
}
