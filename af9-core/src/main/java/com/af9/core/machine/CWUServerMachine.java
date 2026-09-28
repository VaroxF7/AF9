package com.af9.core.machine;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.capability.IControllable;
import com.gregtechceu.gtceu.api.capability.IOpticalComputationProvider;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.TieredEnergyMachine;
import com.gregtechceu.gtceu.api.machine.feature.IFancyUIMachine;

import com.lowdragmc.lowdraglib.gui.widget.ComponentPanelWidget;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;

import java.util.Collection;
import java.util.List;
import java.util.Locale;

/**
 * CWU Server: a single-block machine that turns EU into computation, LV to IV ({@link #cwutFor}: LV 4, MV 8, HV 16, EV
 * 32, IV 64 CWU/t). It gives what is asked of it each tick up to that, and pays for it from its buffer: at full output
 * one amp of its tier ({@code VA[tier]} EU/t), less when less is drawn. It is a GT computation source
 * ({@link IOpticalComputationProvider}, on every side): an ME Computation Link against it takes it directly, GT's
 * Optical Fiber Cable leads it to a reception hatch, and with its front on Optical Bus Cable it is a source on the
 * machine bus. Power goes in on any side but the front. A soft mallet (or the screen) switches it off.
 */
public class CWUServerMachine extends TieredEnergyMachine implements IOpticalComputationProvider, IControllable,
                              IFancyUIMachine {

    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(CWUServerMachine.class,
            TieredEnergyMachine.MANAGED_FIELD_HOLDER);

    @Persisted
    @DescSynced
    private boolean workingEnabled = true;
    /** CWU/t given this tick, and last tick's. */
    private int given, lastGiven;
    private long givenTick = -1;

    public CWUServerMachine(IMachineBlockEntity holder, int tier) {
        super(holder, tier);
        energyContainer.setSideInputCondition(side -> side != getFrontFacing());
    }

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    /** The CWU/t a server of a tier gives at most: 4 at LV, doubling each tier. */
    public static int cwutFor(int tier) {
        return 4 << Math.max(0, tier - GTValues.LV);
    }

    public int getMaxOutput() {
        return cwutFor(tier);
    }

    /** EU per CWU: at full output one amp of its tier. */
    public double euPerCwu() {
        return (double) GTValues.VA[tier] / getMaxOutput();
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

    //////////////////////////////////////
    // ********** Computation **********//
    //////////////////////////////////////

    @Override
    public int requestCWUt(int cwut, boolean simulate, Collection<IOpticalComputationProvider> seen) {
        seen.add(this);
        if (!workingEnabled || cwut <= 0 || getLevel() == null) return 0;
        roll();
        int room = getMaxOutput() - given;
        int affordable = (int) Math.floor(energyContainer.getEnergyStored() / euPerCwu());
        int give = Math.max(0, Math.min(cwut, Math.min(room, affordable)));
        if (!simulate && give > 0) {
            given += give;
            energyContainer.removeEnergy((long) Math.ceil(give * euPerCwu()));
        }
        return give;
    }

    @Override
    public int getMaxCWUt(Collection<IOpticalComputationProvider> seen) {
        seen.add(this);
        return workingEnabled && energyContainer.getEnergyStored() >= euPerCwu() ? getMaxOutput() : 0;
    }

    @Override
    public boolean canBridge(Collection<IOpticalComputationProvider> seen) {
        return true;
    }

    @Override
    public boolean isWorkingEnabled() {
        return workingEnabled;
    }

    @Override
    public void setWorkingEnabled(boolean enabled) {
        workingEnabled = enabled;
    }

    //////////////////////////////////////
    // ************ Screen *************//
    //////////////////////////////////////

    @Override
    public Widget createUIWidget() {
        var group = new WidgetGroup(0, 0, 182, 80);
        boolean client = getLevel() != null && getLevel().isClientSide;
        group.addWidget(new ComponentPanelWidget(4, 5, this::addDisplayText)
                .textSupplier(client ? null : this::addDisplayText)
                .setMaxWidthLimit(174)
                .clickHandler((id, click) -> {
                    if (!click.isRemote && id.equals("power")) setWorkingEnabled(!workingEnabled);
                }));
        return group;
    }

    private void addDisplayText(List<Component> text) {
        text.add(ComponentPanelWidget.withButton(Component.translatable(workingEnabled ? "af9.cwu_server.on" :
                "af9.cwu_server.off").withStyle(workingEnabled ? ChatFormatting.GREEN : ChatFormatting.RED), "power"));
        text.add(Component.translatable("af9.cwu_server.giving", getLastGiven(), getMaxOutput())
                .withStyle(ChatFormatting.AQUA));
        text.add(Component.translatable("af9.cwu_server.power", String.format(Locale.ROOT, "%.1f", euPerCwu()),
                GTValues.VA[tier]).withStyle(ChatFormatting.GRAY));
        text.add(Component.translatable("af9.cwu_server.stored", energyContainer.getEnergyStored(),
                energyContainer.getEnergyCapacity()).withStyle(ChatFormatting.GRAY));
        text.add(Component.translatable("af9.cwu_server.hint").withStyle(ChatFormatting.DARK_GRAY));
    }
}
