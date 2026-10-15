package com.af9.core.fiber;

import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.TickableSubscription;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiPart;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableMultiblockMachine;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableFluidTank;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableItemStackHandler;
import com.gregtechceu.gtceu.common.machine.multiblock.part.FluidHatchPartMachine;
import com.gregtechceu.gtceu.common.machine.multiblock.part.ItemBusPartMachine;

import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

import java.util.ArrayList;
import java.util.List;

/**
 * The Logistics Hub (4 x 2 x 2, structure in {@code kubejs/startup_scripts/gtceu/logistics_hub.js}): the brain and
 * logistics centre of a machine that is too big to feed from all sides, the Photolithography Scanner Mk2. It stands
 * anywhere and is linked to the machine by a pair of Fiber Link Ports ({@link FiberLinkPortPartMachine}).
 * <p>
 * Its own buses and hatches are the machine's: what is put into the hub's input buses and input hatches is carried
 * over the fibre into the machine's port, and what the machine puts into its port's out side is carried into the hub's
 * output buses and output hatches. Items, fluids and gases (a gas is a fluid) both ways. Only the hub's own parts are
 * handled, so a bus next to the hub that is not part of the structure is left alone.
 */
public class LogisticsHubMachine extends WorkableMultiblockMachine {

    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(
            LogisticsHubMachine.class, WorkableMultiblockMachine.MANAGED_FIELD_HOLDER);

    /** Ticks between two transfers, and the most items and mB one transfer moves per handler. */
    public static final int PERIOD = 4, ITEMS_PER_STEP = 64;
    public static final int FLUID_PER_STEP = 4000;

    private TickableSubscription transferSubs;

    public LogisticsHubMachine(IMachineBlockEntity holder) {
        super(holder);
    }

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    @Override
    public void onStructureFormed() {
        super.onStructureFormed();
        transferSubs = subscribeServerTick(transferSubs, this::transfer);
    }

    @Override
    public void onStructureInvalid() {
        super.onStructureInvalid();
        unsubscribe();
    }

    @Override
    public void onUnload() {
        super.onUnload();
        unsubscribe();
    }

    private void unsubscribe() {
        if (transferSubs != null) {
            transferSubs.unsubscribe();
            transferSubs = null;
        }
    }

    /** The hub's own port, or null. */
    public FiberLinkPortPartMachine port() {
        for (IMultiPart part : getParts()) {
            if (part instanceof FiberLinkPortPartMachine port) return port;
        }
        return null;
    }

    private void transfer() {
        if (!isFormed() || getOffsetTimer() % PERIOD != 0) return;
        FiberLinkPortPartMachine own = port();
        if (own == null) return;
        FiberLinkPortPartMachine far = own.peer();
        if (far == null) return;
        List<NotifiableItemStackHandler> itemsIn = new ArrayList<>(), itemsOut = new ArrayList<>();
        List<NotifiableFluidTank> fluidsIn = new ArrayList<>(), fluidsOut = new ArrayList<>();
        for (IMultiPart part : getParts()) {
            if (part instanceof ItemBusPartMachine bus) {
                (bus.getInventory().getHandlerIO() == IO.OUT ? itemsOut : itemsIn).add(bus.getInventory());
            } else if (part instanceof FluidHatchPartMachine hatch) {
                (hatch.tank.getHandlerIO() == IO.OUT ? fluidsOut : fluidsIn).add(hatch.tank);
            }
        }
        // into the machine: the hub's inputs -> the far port's in side
        for (NotifiableItemStackHandler from : itemsIn) moveItems(from, far.itemsIn);
        for (NotifiableFluidTank from : fluidsIn) moveFluids(from, far.fluidsIn);
        // out of the machine: the far port's out side -> the hub's outputs
        for (NotifiableItemStackHandler to : itemsOut) moveItems(far.itemsOut, to);
        for (NotifiableFluidTank to : fluidsOut) moveFluids(far.fluidsOut, to);
    }

    /** Moves up to {@link #ITEMS_PER_STEP} items out of {@code from} into {@code to}, slot by slot. */
    private static void moveItems(NotifiableItemStackHandler from, NotifiableItemStackHandler to) {
        int budget = ITEMS_PER_STEP;
        for (int slot = 0; slot < from.getSlots() && budget > 0; slot++) {
            ItemStack stack = from.storage.getStackInSlot(slot);
            if (stack.isEmpty()) continue;
            ItemStack offer = stack.copyWithCount(Math.min(stack.getCount(), budget));
            ItemStack rest = offer;
            for (int target = 0; target < to.getSlots() && !rest.isEmpty(); target++) {
                rest = to.storage.insertItem(target, rest, false);
            }
            int moved = offer.getCount() - rest.getCount();
            if (moved > 0) {
                from.storage.extractItem(slot, moved, false);
                budget -= moved;
            }
        }
    }

    /** Moves up to {@link #FLUID_PER_STEP} mB of each fluid of {@code from} into {@code to}. */
    private static void moveFluids(NotifiableFluidTank from, NotifiableFluidTank to) {
        for (int tank = 0; tank < from.getTanks(); tank++) {
            FluidStack stack = from.getFluidInTank(tank);
            if (stack.isEmpty()) continue;
            FluidStack offer = stack.copy();
            offer.setAmount(Math.min(offer.getAmount(), FLUID_PER_STEP));
            int filled = to.fill(offer, IFluidHandler.FluidAction.EXECUTE);
            if (filled > 0) {
                FluidStack drain = offer.copy();
                drain.setAmount(filled);
                from.drain(drain, IFluidHandler.FluidAction.EXECUTE);
            }
        }
    }

    @Override
    public void addDisplayText(List<Component> textList) {
        super.addDisplayText(textList);
        if (!isFormed()) return;
        FiberLinkPortPartMachine own = port();
        if (own == null) return;
        textList.addAll(own.statusLines());
    }
}
