package com.af9.core.machine;

import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.TickableSubscription;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;

import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;

import net.minecraft.network.chat.Component;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * The Large Electric Implosion Compressor (structure in {@code kubejs/startup_scripts/gtceu/electric_implosion.js}).
 * It holds a capacitor bank: the energy hatches fill an internal buffer of {@link #CAPACITY} EU while the machine
 * is formed, and a charged implosion recipe (KubeJS data {@code burst_eu}, the explosive route of GT's implosion
 * compressor without its explosive) is fired from that buffer, per parallel, instead of from dynamite.
 * Recipes without {@code burst_eu} (the Lunar Acid Vapor ones) run as usual and do not touch the buffer.
 */
public class LargeElectricImplosionMachine extends WorkableElectricMultiblockMachine {

    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(
            LargeElectricImplosionMachine.class, WorkableElectricMultiblockMachine.MANAGED_FIELD_HOLDER);

    /** Size of the capacitor bank, EU. */
    public static final long CAPACITY = 8_000_000L;

    @Persisted
    @DescSynced
    private long charge;

    private TickableSubscription chargeSubs;

    public LargeElectricImplosionMachine(IMachineBlockEntity holder) {
        super(holder);
    }

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    public long getCharge() {
        return charge;
    }

    @Override
    public void onStructureFormed() {
        super.onStructureFormed();
        chargeSubs = subscribeServerTick(chargeSubs, this::tickCharge);
    }

    @Override
    public void onStructureInvalid() {
        super.onStructureInvalid();
        if (chargeSubs != null) {
            chargeSubs.unsubscribe();
            chargeSubs = null;
        }
    }

    /** The energy hatches fill the bank at their input rate. */
    private void tickCharge() {
        if (!isFormed() || energyContainer == null || charge >= CAPACITY) return;
        long rate = energyContainer.getInputVoltage() * energyContainer.getInputAmperage();
        long want = Math.min(CAPACITY - charge, rate);
        if (want > 0) charge += energyContainer.removeEnergy(want);
    }

    private static long burst(GTRecipe recipe) {
        return recipe.data.getLong("burst_eu") * recipe.parallels;
    }

    /** A charged recipe is only found while the bank holds its burst. */
    @Override
    public @Nullable GTRecipe fullModifyRecipe(@NotNull GTRecipe recipe) {
        GTRecipe modified = super.fullModifyRecipe(recipe);
        if (modified == null) return null;
        return charge >= burst(modified) ? modified : null;
    }

    /** The burst leaves the bank as the recipe starts. */
    @Override
    public boolean beforeWorking(@Nullable GTRecipe recipe) {
        if (recipe != null) {
            long burst = burst(recipe);
            if (charge < burst) return false;
            if (!super.beforeWorking(recipe)) return false;
            charge -= burst;
            return true;
        }
        return super.beforeWorking(recipe);
    }

    @Override
    public void addDisplayText(List<Component> textList) {
        super.addDisplayText(textList);
        if (isFormed()) {
            textList.add(Component.translatable("af9.large_electric_implosion_compressor.charge", charge, CAPACITY));
        }
    }
}
