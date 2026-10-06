package com.af9.core.machine;

import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;

import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;

/**
 * MK3 Void Miner (white/silver, LuV-class): its own machine definition ({@code gtceu:void_miner_mk3}) created by
 * KubeJS ({@code kubejs/startup_scripts/gtceu/void_mining.js}), running its own recipe types
 * ({@link #RECIPE_TYPES}). What sets it apart from the base miner — 3x output and power draw at a third of the
 * duration — is data, not code: the numbers live in the KubeJS recipes ({@code kubejs/server_scripts/mods/gtceu/
 * miner.js}) and are picked up by {@link VoidMinerMachine#install} / {@link VoidMinerMachine#registerRecipeInfo},
 * which wire the definition's machine supplier, tooltip lines and recipe pages up. This class only gives the
 * machine its identity: field holder, screen title and the silver mode colours of its areas.
 */
public class VoidMinerMachineMK3 extends VoidMinerMachine {

    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(
            VoidMinerMachineMK3.class, VoidMinerMachine.MANAGED_FIELD_HOLDER);

    /** MK3's areas in white/silver, one shade per mode, instead of the base miner's colours. */
    private static final int[] COLORS_MK3 = { 0xFFF8FAFC, 0xFFE2E8F0, 0xFFCBD5E1, 0xFF94A3B8 };

    /** The MK3's recipe types, in mode order (gtceu namespace). */
    public static final String[] RECIPE_TYPES = { "void_mining_overworld_mk3", "void_mining_nether_mk3",
            "void_mining_end_mk3", "void_mining_asteroids_mk3" };

    public VoidMinerMachineMK3(IMachineBlockEntity holder) {
        super(holder);
    }

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    @Override
    public String titleKey() {
        return "af9.voidminer.mk3.console.title";
    }

    @Override
    public int modeColor(int index) {
        return COLORS_MK3[Math.max(0, Math.min(COLORS_MK3.length - 1, index))];
    }
}
