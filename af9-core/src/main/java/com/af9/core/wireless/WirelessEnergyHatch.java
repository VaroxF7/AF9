package com.af9.core.wireless;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.TickableSubscription;
import com.gregtechceu.gtceu.api.machine.feature.IDataStickInteractable;
import com.gregtechceu.gtceu.api.machine.multiblock.part.TieredIOPartMachine;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableEnergyContainer;
import com.gregtechceu.gtceu.utils.FormattingUtil;
import com.gregtechceu.gtceu.utils.GTUtil;

import com.lowdragmc.lowdraglib.gui.util.ClickData;
import com.lowdragmc.lowdraglib.gui.widget.ComponentPanelWidget;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;

import java.util.List;
import java.util.UUID;

/**
 * What the Wireless Energy Transmitter and Receiver share: an energy buffer whose voltage is set at run time (the
 * hatches have no voltage tier of their own, only a maximum amperage), the channel they belong to, a tick, and a small
 * status screen. The hull tier of each variant is only its look and its crafting tier.
 */
public abstract class WirelessEnergyHatch extends TieredIOPartMachine implements IDataStickInteractable {

    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(
            WirelessEnergyHatch.class, TieredIOPartMachine.MANAGED_FIELD_HOLDER);

    @Persisted
    public final NotifiableEnergyContainer energyContainer;
    /** The channel (a transmitter's own, a receiver's linked one); "" none. */
    @Persisted
    @DescSynced
    protected String channel = "";
    /** Voltage the hatch works at now (0: none). */
    @Persisted
    @DescSynced
    protected long voltage;
    protected final int amperage;
    private TickableSubscription tickSubs;

    protected WirelessEnergyHatch(IMachineBlockEntity holder, int tier, IO io, int amperage) {
        super(holder, tier, io);
        this.amperage = amperage;
        // GT caches a part's handler IO the first time it is asked, and a container reports IO.NONE at 0 V: keep the
        // IO fixed, so an unlinked hatch still counts as an input (output) once it gets a voltage
        this.energyContainer = new NotifiableEnergyContainer(this, 0, 0, 0, 0, 0) {

            @Override
            public IO getHandlerIO() {
                return io;
            }
        };
        // wireless only: no cables in or out
        energyContainer.setSideInputCondition(side -> false);
        energyContainer.setSideOutputCondition(side -> false);
        energyContainer.setCapabilityValidator(side -> side == null);
    }

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    public int getAmperage() {
        return amperage;
    }

    public long getVoltage() {
        return voltage;
    }

    public UUID getChannel() {
        try {
            return channel.isEmpty() ? null : UUID.fromString(channel);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /** The buffer at a voltage: the hatch's capacity for it. */
    protected abstract long capacity(long voltage);

    /** Server, every tick. */
    protected abstract void tick(WirelessChannels channels);

    protected WirelessChannels channels() {
        return getLevel() instanceof ServerLevel level ? WirelessChannels.get(level.getServer()) : null;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        applyVoltage();
        if (getLevel() instanceof ServerLevel) {
            tickSubs = subscribeServerTick(tickSubs, () -> {
                WirelessChannels channels = channels();
                if (channels != null) tick(channels);
            });
        }
    }

    @Override
    public void onUnload() {
        super.onUnload();
        if (tickSubs != null) {
            tickSubs.unsubscribe();
            tickSubs = null;
        }
    }

    /** The energy overlay's tint: cyan receivers, orange transmitters (GT's energy hatches use the tier colour). */
    @Override
    public int tintColor(int index) {
        if (index == 2) return io == IO.OUT ? 0xFFFF9A3C : 0xFF3FC8FF;
        return super.tintColor(index);
    }

    /** Switches the buffer to a new voltage. */
    protected void setVoltage(long newVoltage) {
        if (newVoltage == voltage) return;
        voltage = newVoltage;
        applyVoltage();
        markDirty();
    }

    private void applyVoltage() {
        long capacity = voltage <= 0 ? 0 : capacity(voltage);
        // no voltage, no amps: a dead hatch must not lower the multiblock's average voltage
        long amps = voltage <= 0 ? 0 : amperage;
        if (io == IO.OUT) {
            energyContainer.resetBasicInfo(capacity, 0, 0, Math.max(0, voltage), amps);
        } else {
            energyContainer.resetBasicInfo(capacity, Math.max(0, voltage), amps, 0, 0);
        }
        if (energyContainer.getEnergyStored() > capacity) energyContainer.setEnergyStored(capacity);
    }

    /** "524,288 EU/t (UV)" or "none". */
    protected static Component voltageText(long voltage) {
        if (voltage <= 0) return Component.translatable("af9.wireless.none");
        return Component.literal(FormattingUtil.formatNumbers(voltage) + " EU/t (" +
                GTValues.VNF[GTUtil.getTierByVoltage(voltage)] + "§r)");
    }

    //////////////////////////////////////
    // ************ GUI *************//
    //////////////////////////////////////

    @Override
    public Widget createUIWidget() {
        var group = new WidgetGroup(0, 0, 182, 100);
        group.addWidget(new ComponentPanelWidget(4, 4, this::addDisplayText)
                .textSupplier(getLevel() != null && getLevel().isClientSide ? null : this::addDisplayText)
                .setMaxWidthLimit(174)
                .clickHandler(this::handleDisplayClick));
        return group;
    }

    /** Server: the status lines. */
    protected abstract void addDisplayText(List<Component> text);

    protected void handleDisplayClick(String id, ClickData click) {}
}
