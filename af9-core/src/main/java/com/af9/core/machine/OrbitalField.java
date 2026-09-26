package com.af9.core.machine;

import com.af9.core.AF9Config;

import net.minecraft.world.entity.Entity;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The magnetic field of the Orbital Lithography Stations: inside an active station's field (see
 * {@link OrbitalLithographyMachine#fieldBox()}) entities fall with the configured gravity
 * ({@link AF9Config#FIELD_GRAVITY}) instead of floating in orbit, so players stand on the deck and can only hop.
 * Gravity only goes up to that value, never down (a station on a planet changes nothing).
 * <p>
 * Ad Astra decides every entity's gravity and asks its listeners ({@link com.af9.core.compat.adastra.AdAstraCompat}),
 * on the server and on the client (the player moves client-side), so both sides keep their loaded stations here.
 */
public final class OrbitalField {

    private static final Set<OrbitalLithographyMachine> SERVER = ConcurrentHashMap.newKeySet();
    private static final Set<OrbitalLithographyMachine> CLIENT = ConcurrentHashMap.newKeySet();

    private OrbitalField() {}

    static void add(OrbitalLithographyMachine station) {
        (station.isRemote() ? CLIENT : SERVER).add(station);
    }

    static void remove(OrbitalLithographyMachine station) {
        (station.isRemote() ? CLIENT : SERVER).remove(station);
    }

    /** Ad Astra's gravity for the entity (1 = Earth), raised to the field's inside an active station's field. */
    public static float gravity(Entity entity, float gravity) {
        float field = AF9Config.FIELD_GRAVITY.get().floatValue();
        if (gravity >= field) return gravity;
        for (OrbitalLithographyMachine station : entity.level().isClientSide ? CLIENT : SERVER) {
            if (station.getLevel() == entity.level() && station.isFieldActive() &&
                    station.fieldBox().contains(entity.position())) {
                return field;
            }
        }
        return gravity;
    }
}
