package com.af9.core.compat.extremereactors;

import com.af9.core.AF9Core;

import net.minecraftforge.fml.InterModComms;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.event.lifecycle.InterModEnqueueEvent;

/**
 * Supercritical steam in Extreme Reactors' turbines. The FX-1 Reactor makes {@code gtceu:supercritical_steam}; GT's
 * Large Steam Turbines run it through a steam turbine recipe (kubejs/server_scripts/mods/gtceu/asteroid_fission.js),
 * and here Extreme Reactors learns it as a vapor of its own, 320 FE per mB (32 times its steam), made from the fluid
 * tag {@code forge:supercritical_steam}. The API calls go through Extreme Reactors' own inter-mod messages (the two
 * methods it documents, processed in its order: vapors first, then their fluid mappings) by reflection, so AF9 Core
 * needs nothing of the mod to build or to run without it.
 */
public final class ExtremeReactorsCompat {

    public static final String MOD_ID = "bigreactors";
    public static final String VAPOR = "supercritical_steam";
    public static final String VAPOR_LANG_KEY = "vapor.af9.supercritical_steam";
    /** FE per mB: GT's supercritical steam gives 80 EU per mB (4 FE to the EU), Extreme Reactors' steam 10 FE. */
    public static final float ENERGY_DENSITY = 320.0f;
    /** A light, bright steam colour (0xRRGGBB). */
    public static final int COLOUR = 0xE6F4FF;
    public static final String FLUID_TAG = "forge:supercritical_steam";

    private static final String FLUIDS_REGISTRY = "it.zerono.mods.extremereactors.api.coolant.FluidsRegistry";
    private static final String MAPPINGS_REGISTRY = "it.zerono.mods.extremereactors.api.coolant.FluidMappingsRegistry";

    private ExtremeReactorsCompat() {}

    /** Inter-mod enqueue (mod bus). */
    public static void enqueue(InterModEnqueueEvent event) {
        if (!ModList.get().isLoaded(MOD_ID)) return;
        InterModComms.sendTo(MOD_ID, "fluid-register", () -> (Runnable) ExtremeReactorsCompat::registerVapor);
        InterModComms.sendTo(MOD_ID, "fluid-mapping-register", () -> (Runnable) ExtremeReactorsCompat::registerMapping);
    }

    private static void registerVapor() {
        try {
            Class.forName(FLUIDS_REGISTRY)
                    .getMethod("registerVapor", String.class, float.class, String.class, int.class)
                    .invoke(null, VAPOR, ENERGY_DENSITY, VAPOR_LANG_KEY, COLOUR);
        } catch (ReflectiveOperationException | RuntimeException e) {
            AF9Core.LOGGER.warn("Extreme Reactors: could not register the supercritical steam vapor", e);
        }
    }

    private static void registerMapping() {
        try {
            Class.forName(MAPPINGS_REGISTRY)
                    .getMethod("registerVaporMapping", String.class, int.class, String.class)
                    .invoke(null, VAPOR, 1, FLUID_TAG);
        } catch (ReflectiveOperationException | RuntimeException e) {
            AF9Core.LOGGER.warn("Extreme Reactors: could not map supercritical steam to its vapor", e);
        }
    }
}
