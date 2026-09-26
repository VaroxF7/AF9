package com.af9.core.compat.adastra;

import com.af9.core.machine.OrbitalField;

import net.minecraftforge.fml.ModList;

import earth.terrarium.adastra.api.events.AdAstraEvents;

/**
 * Ad Astra's gravity: the Orbital Lithography Station's magnetic field ({@link OrbitalField}) raises the gravity of
 * everything inside it. Does nothing without Ad Astra.
 */
public final class AdAstraCompat {

    private AdAstraCompat() {}

    /** Common setup (both sides). */
    public static void init() {
        if (ModList.get().isLoaded("ad_astra")) Api.register();
    }

    /** Loaded only when Ad Astra is. */
    private static final class Api {

        static void register() {
            AdAstraEvents.EntityGravityEvent.register(OrbitalField::gravity);
        }
    }
}
