package com.af9.core.cpu;

import com.af9.core.machine.console.ScrollingText;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.gui.widget.SlotWidget;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.feature.IMachineLife;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiController;
import com.gregtechceu.gtceu.api.machine.multiblock.PartAbility;
import com.gregtechceu.gtceu.api.machine.multiblock.part.MultiblockPartMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.part.TieredPartMachine;
import com.gregtechceu.gtceu.api.transfer.item.CustomItemStackHandler;

import com.lowdragmc.lowdraglib.gui.widget.ComponentPanelWidget;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * CPU Rack: the component block of the Crafting CPU Array. Four slots of {@link CpuPart}s (HBM Memory Sticks and Stacks,
 * CPU Clusters), up to {@link #SLOT_LIMIT} of one kind in each: the array adds up what its racks hold.
 */
public class CpuRackPartMachine extends TieredPartMachine implements IMachineLife {

    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(
            CpuRackPartMachine.class, MultiblockPartMachine.MANAGED_FIELD_HOLDER);

    /** Registered by the rack definition (KubeJS), taken by the array's pattern. */
    public static final PartAbility CPU_RACK = new PartAbility("af9_cpu_rack");
    public static final int SLOTS = 4;
    public static final int SLOT_LIMIT = 8;

    @Persisted
    public final CustomItemStackHandler parts;

    public CpuRackPartMachine(IMachineBlockEntity holder) {
        super(holder, GTValues.HV);
        parts = new CustomItemStackHandler(SLOTS) {

            @Override
            public int getSlotLimit(int slot) {
                return SLOT_LIMIT;
            }
        };
        parts.setFilter(stack -> CpuPart.of(stack) != null);
        parts.setOnContentsChanged(this::onPartsChanged);
    }

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    /** How many of a part the rack holds. */
    public int count(CpuPart part) {
        int count = 0;
        for (int i = 0; i < SLOTS; i++) {
            ItemStack stack = parts.getStackInSlot(i);
            if (CpuPart.of(stack) == part) count += stack.getCount();
        }
        return count;
    }

    private void onPartsChanged() {
        markDirty();
        for (IMultiController controller : getControllers()) {
            if (controller instanceof CraftingCpuMachine array) array.onRacksChanged();
        }
    }

    @Override
    public boolean canShared() {
        return false;
    }

    @Override
    public void onMachineRemoved() {
        clearInventory(parts);
    }

    //////////////////////////////////////
    // ************ GUI *************//
    //////////////////////////////////////

    /** The four slots on top, what the rack holds under them in a box that scrolls when it runs longer. */
    @Override
    public Widget createUIWidget() {
        var group = new WidgetGroup(0, 0, 176, 96);
        int x0 = (176 - SLOTS * 18) / 2;
        for (int i = 0; i < SLOTS; i++) {
            group.addWidget(new SlotWidget(parts, i, x0 + i * 18, 4, true, true)
                    .setBackgroundTexture(GuiTextures.SLOT));
        }
        boolean client = getLevel() != null && getLevel().isClientSide;
        group.addWidget(ScrollingText.box(0, 26, 176, 70, new ComponentPanelWidget(4, 0,
                this::addDisplayText)
                .textSupplier(client ? null : this::addDisplayText)
                .setMaxWidthLimit(166)));
        return group;
    }

    private void addDisplayText(List<Component> text) {
        long bytes = 0;
        int threads = 0;
        for (CpuPart part : CpuPart.values()) {
            int count = count(part);
            bytes += count * part.bytes;
            threads += count * part.threads;
        }
        text.add(Component.translatable("af9.cpu_rack.holds").withStyle(ChatFormatting.GRAY));
        text.add(Component.translatable("af9.cpu_rack.bytes", CpuPart.formatBytes(bytes))
                .withStyle(ChatFormatting.AQUA));
        text.add(Component.translatable("af9.cpu_rack.threads", threads).withStyle(ChatFormatting.AQUA));
        text.add(Component.translatable("af9.cpu_rack.limit", SLOT_LIMIT).withStyle(ChatFormatting.DARK_GRAY));
    }
}
