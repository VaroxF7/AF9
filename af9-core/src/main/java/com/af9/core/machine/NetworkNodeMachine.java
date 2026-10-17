package com.af9.core.machine;

import com.gregtechceu.gtceu.api.capability.IOpticalComputationProvider;
import com.gregtechceu.gtceu.api.capability.recipe.CWURecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableComputationContainer;
import com.gregtechceu.gtceu.common.machine.multiblock.electric.research.DataBankMachine;
import com.gregtechceu.gtceu.common.machine.multiblock.part.DataAccessHatchMachine;
import com.gregtechceu.gtceu.utils.FormattingUtil;

import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Network Node: the Data Bank's structure and power use (it is a Data Bank underneath, so its data hatches take sticks,
 * orbs and modules the same way), but the data items store computation. Every Data Stick holds
 * {@link #STICK_CWU} CWU, a Data Orb {@link #ORB_CWU}, a Data Module {@link #MODULE_CWU}, in up to nine data hatches.
 * <p>
 * It is a computation source (a GT {@link IOpticalComputationProvider}) that, unlike the Network Switch, keeps what it
 * is given: computation reception hatches (optical or wireless) fill the store, up to {@link #FILL_RATE} CWU/t, from
 * whatever sits behind them; computation transmission hatches give it out again, up to {@link #OUT_RATE} CWU/t, from
 * the store. The stored amount is saved with the controller. It only works while the bank is powered. Taking items
 * out of the hatches lowers the capacity, and the store with it.
 */
public class NetworkNodeMachine extends DataBankMachine implements IOpticalComputationProvider {

    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(
            NetworkNodeMachine.class, WorkableElectricMultiblockMachine.MANAGED_FIELD_HOLDER);

    public static final long STICK_CWU = 1_000;
    public static final long ORB_CWU = 10_000;
    public static final long MODULE_CWU = 100_000;
    /** CWU/t the node takes in from its reception hatches / gives out through its transmission hatches. */
    public static final int FILL_RATE = 256;
    public static final int OUT_RATE = 1024;
    /** Ticks between two capacity counts. */
    private static final int COUNT_INTERVAL = 10;

    @Persisted
    @DescSynced
    private long stored;
    @DescSynced
    private long capacity;

    public NetworkNodeMachine(IMachineBlockEntity holder) {
        super(holder);
    }

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    public long getStored() {
        return stored;
    }

    public long getCapacity() {
        return capacity;
    }

    /** CWU of the data items in the data hatches. */
    private long countCapacity() {
        long total = 0;
        for (var part : getParts()) {
            if (!(part instanceof DataAccessHatchMachine hatch)) continue;
            for (int i = 0; i < hatch.importItems.getSlots(); i++) {
                total += valueOf(hatch.importItems.getStackInSlot(i));
            }
        }
        return total;
    }

    private static long valueOf(ItemStack stack) {
        if (stack.isEmpty()) return 0;
        var key = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (!"gtceu".equals(key.getNamespace())) return 0;
        return switch (key.getPath()) {
            case "data_stick" -> STICK_CWU * stack.getCount();
            case "data_orb" -> ORB_CWU * stack.getCount();
            case "data_module" -> MODULE_CWU * stack.getCount();
            default -> 0;
        };
    }

    @Override
    public void onStructureFormed() {
        super.onStructureFormed();
        capacity = countCapacity();
        stored = Math.min(stored, capacity);
    }

    @Override
    public void onStructureInvalid() {
        super.onStructureInvalid();
        // the stored computation stays with the controller: the items are still in the hatches
        capacity = 0;
    }

    @Override
    public void tick() {
        super.tick();
        if (!isFormed()) return;
        if (getOffsetTimer() % COUNT_INTERVAL == 0) {
            long counted = countCapacity();
            if (counted != capacity || stored > counted) {
                capacity = counted;
                stored = Math.min(stored, capacity);
                markDirty();
            }
        }
        if (!working() || stored >= capacity) return;
        // fill: from the reception hatches (not the transmission ones, which would ask this node itself)
        long before = stored;
        for (var handler : getCapabilitiesFlat(IO.IN, CWURecipeCapability.CAP)) {
            if (stored >= capacity) break;
            if (handler instanceof NotifiableComputationContainer container && !container.isTransmitter()) {
                int want = (int) Math.min(FILL_RATE, capacity - stored);
                Collection<IOpticalComputationProvider> seen = new ArrayList<>();
                seen.add(this);
                stored += Math.max(0, container.requestCWUt(want, false, seen));
            }
        }
        if (stored != before) markDirty();
    }

    /** Powered and switched on. */
    private boolean working() {
        return isFormed() && isWorkingEnabled() && getRecipeLogic().isActive();
    }

    //////////////////////////////////////
    // ****** Computation source ******//
    //////////////////////////////////////

    @Override
    public int requestCWUt(int cwut, boolean simulate, Collection<IOpticalComputationProvider> seen) {
        seen.add(this);
        if (!working() || cwut <= 0) return 0;
        int give = (int) Math.max(0, Math.min(Math.min(cwut, OUT_RATE), stored));
        if (!simulate && give > 0) {
            stored -= give;
            markDirty();
        }
        return give;
    }

    @Override
    public int getMaxCWUt(Collection<IOpticalComputationProvider> seen) {
        seen.add(this);
        return working() ? (int) Math.min(OUT_RATE, stored) : 0;
    }

    @Override
    public boolean canBridge(Collection<IOpticalComputationProvider> seen) {
        seen.add(this);
        return true;
    }

    @Override
    public void addDisplayText(List<Component> textList) {
        super.addDisplayText(textList);
        if (!isFormed()) return;
        textList.add(Component.translatable("af9.network_node.stored", FormattingUtil.formatNumbers(stored),
                FormattingUtil.formatNumbers(capacity)).withStyle(ChatFormatting.AQUA));
        textList.add(Component.translatable("af9.network_node.rates", FILL_RATE, OUT_RATE)
                .withStyle(ChatFormatting.GRAY));
    }
}
