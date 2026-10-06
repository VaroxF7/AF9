package com.af9.core.machine;

import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;

import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;

/**
 * MK2 Void Miner (blue, IV-class): its own machine definition ({@code gtceu:void_miner_mk2}) created by KubeJS
 * ({@code kubejs/startup_scripts/gtceu/void_mining.js}), running its own recipe types ({@link #RECIPE_TYPES}).
 * What sets it apart from the base miner — 2x output and power draw at half the duration — is data, not code: the
 * numbers live in the KubeJS recipes ({@code kubejs/server_scripts/mods/gtceu/miner.js}) and are picked up by
 * {@link VoidMinerMachine#install} / {@link VoidMinerMachine#registerRecipeInfo}, which wire the definition's
 * machine supplier, tooltip lines and recipe pages up. This class only gives the machine its identity: field
 * holder, screen title and the blue mode colours of its areas.
 */
public class VoidMinerMachineMK2 extends VoidMinerMachine {

    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(
            VoidMinerMachineMK2.class, VoidMinerMachine.MANAGED_FIELD_HOLDER);

    /** MK2's areas in blue, one shade per mode, instead of the base miner's colours. */
    private static final int[] COLORS_MK2 = { 0xFF3B82F6, 0xFF60A5FA, 0xFF93C5FD, 0xFFBFDBFE };

    /** The MK2's recipe types, in mode order (gtceu namespace). */
    public static final String[] RECIPE_TYPES = { "void_mining_overworld_mk2", "void_mining_nether_mk2",
            "void_mining_end_mk2", "void_mining_asteroids_mk2" };

    public VoidMinerMachineMK2(IMachineBlockEntity holder) {
        super(holder);
    }

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    @Override
    public String titleKey() {
        return "af9.voidminer.mk2.console.title";
    }

    @Override
    public int modeColor(int index) {
        return COLORS_MK2[Math.max(0, Math.min(COLORS_MK2.length - 1, index))];
    }
}
