package com.af9.core.wireless;

import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiController;
import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine;
import com.gregtechceu.gtceu.utils.FormattingUtil;

import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.UUID;

/**
 * Wireless Energy Receiver: an energy input hatch for any multiblock, fed by the transmitter it is linked to (right-
 * click with a data stick that holds a transmitter's link; shift-right-click copies this receiver's link onto the
 * stick). Any distance, any dimension ({@link WirelessChannels}).
 * <p>
 * It works at the transmitter's voltage and draws up to its own amperage per tick. When that voltage changes (the
 * first link, a new substation input) the multiblock re-forms, since GT reads its hatches' voltage when it forms.
 */
public class WirelessReceiverHatch extends WirelessEnergyHatch {

    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(
            WirelessReceiverHatch.class, WirelessEnergyHatch.MANAGED_FIELD_HOLDER);

    public WirelessReceiverHatch(IMachineBlockEntity holder, int tier, int amperage) {
        super(holder, tier, IO.IN, amperage);
    }

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    /** Like GT's energy hatches: 16 ticks of full input. */
    @Override
    protected long capacity(long voltage) {
        return voltage * amperage * 16L;
    }

    @Override
    protected void tick(WirelessChannels channels) {
        WirelessChannels.Channel link = channels.get(getChannel());
        if (link == null) {
            // not linked, or the transmitter is gone
            if (voltage != 0) changeVoltage(0);
            return;
        }
        if (link.voltage != voltage && (getOffsetTimer() % 20 == 0 || voltage == 0)) changeVoltage(link.voltage);
        if (voltage <= 0 || link.voltage != voltage) return;
        long room = energyContainer.getEnergyCapacity() - energyContainer.getEnergyStored();
        long taken = link.take(Math.min(room, voltage * amperage));
        if (taken > 0) {
            energyContainer.changeEnergy(taken);
            channels.setDirty();
        }
    }

    /** New voltage: the multiblock re-forms so it reads it. */
    private void changeVoltage(long newVoltage) {
        setVoltage(newVoltage);
        for (IMultiController controller : getControllers()) {
            if (controller instanceof MultiblockControllerMachine machine && machine.isFormed()) {
                machine.onPartUnload();
            }
        }
    }

    //////////////////////////////////////
    // ********* Data stick *********//
    //////////////////////////////////////

    /** Right-click with a linked data stick: link to its transmitter. */
    @Override
    public InteractionResult onDataStickUse(Player player, ItemStack dataStick) {
        if (!(getLevel() instanceof ServerLevel level)) return InteractionResult.SUCCESS;
        WirelessLink link = WirelessLink.read(dataStick);
        if (link == null) {
            player.sendSystemMessage(Component.translatable("af9.wireless.stick_empty").withStyle(ChatFormatting.RED));
            return InteractionResult.SUCCESS;
        }
        channel = link.channel().toString();
        markDirty();
        WirelessChannels.Channel target = WirelessChannels.get(level.getServer()).get(link.channel());
        changeVoltage(target == null ? 0 : target.voltage);
        player.sendSystemMessage(Component.translatable("af9.wireless.linked", link.where())
                .withStyle(ChatFormatting.AQUA));
        if (target == null) {
            player.sendSystemMessage(Component.translatable("af9.wireless.no_transmitter")
                    .withStyle(ChatFormatting.YELLOW));
        }
        return InteractionResult.SUCCESS;
    }

    /** Shift-right-click: copy this receiver's link onto the stick, for more receivers. */
    @Override
    public InteractionResult onDataStickShiftUse(Player player, ItemStack dataStick) {
        if (!(getLevel() instanceof ServerLevel level)) return InteractionResult.SUCCESS;
        UUID id = getChannel();
        WirelessChannels.Channel target = WirelessChannels.get(level.getServer()).get(id);
        if (target == null || target.dimension == null) {
            player.sendSystemMessage(Component.translatable("af9.wireless.no_transmitter")
                    .withStyle(ChatFormatting.YELLOW));
            return InteractionResult.SUCCESS;
        }
        WirelessLink link = new WirelessLink(id, target.dimension, target.pos, (int) target.amperage);
        link.write(dataStick);
        player.sendSystemMessage(Component.translatable("af9.wireless.stick_written", link.where())
                .withStyle(ChatFormatting.AQUA));
        return InteractionResult.SUCCESS;
    }

    //////////////////////////////////////
    // ************ GUI *************//
    //////////////////////////////////////

    @Override
    protected void addDisplayText(List<Component> text) {
        text.add(Component.translatable("af9.wireless.receiver.title", amperage).withStyle(ChatFormatting.AQUA));
        WirelessChannels channels = channels();
        WirelessChannels.Channel link = channels == null ? null : channels.get(getChannel());
        if (getChannel() == null) {
            text.add(Component.translatable("af9.wireless.unlinked").withStyle(ChatFormatting.YELLOW));
        } else if (link == null) {
            text.add(Component.translatable("af9.wireless.no_transmitter").withStyle(ChatFormatting.RED));
        } else {
            text.add(Component.translatable("af9.wireless.channel", WirelessLink.shortId(getChannel())));
            if (link.dimension != null) {
                text.add(Component.translatable("af9.wireless.from",
                        new WirelessLink(link.id, link.dimension, link.pos, (int) link.amperage).where()));
            }
            text.add(Component.translatable("af9.wireless.channel_buffer", FormattingUtil.formatNumbers(link.stored),
                    FormattingUtil.formatNumbers(link.capacity)));
        }
        text.add(Component.translatable("af9.wireless.voltage", voltageText(voltage)));
        text.add(Component.translatable("af9.wireless.throughput", amperage,
                FormattingUtil.formatNumbers(Math.max(0, voltage) * amperage)));
        text.add(Component.translatable("af9.wireless.buffer",
                FormattingUtil.formatNumbers(energyContainer.getEnergyStored()),
                FormattingUtil.formatNumbers(energyContainer.getEnergyCapacity())));
        text.add(Component.translatable("af9.wireless.receiver.hint").withStyle(ChatFormatting.DARK_GRAY));
    }
}
