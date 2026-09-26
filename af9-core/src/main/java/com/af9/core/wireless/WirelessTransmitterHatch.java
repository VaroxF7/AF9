package com.af9.core.wireless;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.capability.IEnergyContainer;
import com.gregtechceu.gtceu.api.capability.recipe.EURecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.capability.recipe.IRecipeHandler;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.feature.IMachineLife;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiController;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiPart;
import com.gregtechceu.gtceu.api.machine.trait.RecipeHandlerList;
import com.gregtechceu.gtceu.utils.FormattingUtil;

import com.lowdragmc.lowdraglib.gui.util.ClickData;
import com.lowdragmc.lowdraglib.gui.widget.ComponentPanelWidget;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
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
 * Wireless Energy Transmitter: an output (dynamo) hatch for the Power Substation, or any multiblock that puts out
 * energy. What the multiblock pushes into it goes into its own channel ({@link WirelessChannels}), up to its amperage
 * at its voltage per tick; linked receivers draw from there.
 * <p>
 * Voltage: the highest voltage among its multiblock's energy inputs (a substation fed with UV hatches sends UV). A
 * multiblock without energy inputs (a generator) uses the tier set on the hatch's screen. A data stick (shift- or
 * normal right-click) takes the link for the receivers. Breaking it deletes the channel.
 */
public class WirelessTransmitterHatch extends WirelessEnergyHatch implements IMachineLife {

    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(
            WirelessTransmitterHatch.class, WirelessEnergyHatch.MANAGED_FIELD_HOLDER);

    /** Voltage tier used when the multiblock has no energy inputs. */
    @Persisted
    private int manualTier = GTValues.EV;
    /** Whether the voltage comes from the multiblock's inputs (else the manual tier). */
    private boolean auto;

    public WirelessTransmitterHatch(IMachineBlockEntity holder, int tier, int amperage) {
        super(holder, tier, IO.OUT, amperage);
    }

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    /** One second of full throughput. */
    @Override
    protected long capacity(long voltage) {
        return voltage * amperage * 20L;
    }

    @Override
    protected void tick(WirelessChannels channels) {
        UUID id = getChannel();
        if (id == null) {
            id = UUID.randomUUID();
            channel = id.toString();
            markDirty();
        }
        if (getOffsetTimer() % 20 == 0 || voltage == 0) updateVoltage();
        WirelessChannels.Channel link = channels.getOrCreate(id);
        link.voltage = voltage;
        link.amperage = amperage;
        link.capacity = capacity(Math.max(0, voltage));
        link.pos = getPos();
        link.dimension = getLevel().dimension().location();
        if (voltage <= 0) return;
        long moved = link.put(Math.min(energyContainer.getEnergyStored(), voltage * amperage));
        if (moved > 0) {
            energyContainer.changeEnergy(-moved);
            channels.setDirty();
        }
    }

    private void updateVoltage() {
        long source = sourceVoltage();
        auto = source > 0;
        setVoltage(auto ? source : GTValues.V[manualTier]);
    }

    /** Highest input voltage among the energy inputs of the multiblocks this hatch is part of (0: none). */
    private long sourceVoltage() {
        long highest = 0;
        for (IMultiController controller : getControllers()) {
            for (IMultiPart part : controller.getParts()) {
                if (part == this) continue;
                for (RecipeHandlerList handlers : part.getRecipeHandlers()) {
                    if (!handlers.getHandlerIO().support(IO.IN)) continue;
                    for (IRecipeHandler<?> handler : handlers.getCapability(EURecipeCapability.CAP)) {
                        if (handler instanceof IEnergyContainer container) {
                            highest = Math.max(highest, container.getInputVoltage());
                        }
                    }
                }
            }
        }
        return highest;
    }

    /** Broken: the channel goes (its receivers lose their link). */
    @Override
    public void onMachineRemoved() {
        WirelessChannels channels = channels();
        if (channels != null) channels.remove(getChannel());
    }

    //////////////////////////////////////
    // ********* Data stick *********//
    //////////////////////////////////////

    @Override
    public InteractionResult onDataStickUse(Player player, ItemStack dataStick) {
        return writeLink(player, dataStick);
    }

    @Override
    public InteractionResult onDataStickShiftUse(Player player, ItemStack dataStick) {
        return writeLink(player, dataStick);
    }

    private InteractionResult writeLink(Player player, ItemStack dataStick) {
        if (!(getLevel() instanceof ServerLevel level)) return InteractionResult.SUCCESS;
        UUID id = getChannel();
        if (id == null) {
            id = UUID.randomUUID();
            channel = id.toString();
            markDirty();
        }
        WirelessLink link = new WirelessLink(id, level.dimension().location(), getPos(), amperage);
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
        text.add(Component.translatable("af9.wireless.transmitter.title", amperage).withStyle(ChatFormatting.AQUA));
        text.add(Component.translatable("af9.wireless.channel", WirelessLink.shortId(getChannel())));
        text.add(Component.translatable("af9.wireless.voltage", voltageText(voltage)));
        if (auto) {
            text.add(Component.translatable("af9.wireless.voltage_auto").withStyle(ChatFormatting.GRAY));
        } else {
            text.add(Component.translatable("af9.wireless.voltage_manual", GTValues.VNF[manualTier])
                    .append(" ").append(ComponentPanelWidget.withButton(Component.literal("[-]"), "down"))
                    .append(" ").append(ComponentPanelWidget.withButton(Component.literal("[+]"), "up")));
        }
        text.add(Component.translatable("af9.wireless.throughput", amperage,
                FormattingUtil.formatNumbers(Math.max(0, voltage) * amperage)));
        text.add(Component.translatable("af9.wireless.buffer",
                FormattingUtil.formatNumbers(energyContainer.getEnergyStored()),
                FormattingUtil.formatNumbers(energyContainer.getEnergyCapacity())));
        WirelessChannels channels = channels();
        WirelessChannels.Channel link = channels == null ? null : channels.get(getChannel());
        if (link != null) {
            text.add(Component.translatable("af9.wireless.channel_buffer", FormattingUtil.formatNumbers(link.stored),
                    FormattingUtil.formatNumbers(link.capacity)));
        }
        text.add(Component.translatable("af9.wireless.transmitter.hint").withStyle(ChatFormatting.DARK_GRAY));
    }

    @Override
    protected void handleDisplayClick(String id, ClickData click) {
        if (click.isRemote) return;
        if (id.equals("up")) manualTier = Math.min(GTValues.MAX, manualTier + 1);
        else if (id.equals("down")) manualTier = Math.max(GTValues.ULV, manualTier - 1);
        else return;
        markDirty();
        updateVoltage();
    }
}
