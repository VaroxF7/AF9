package com.af9.core.common;

import com.af9.core.AF9Core;

import com.gregtechceu.gtceu.api.sound.ExistingSoundEntry;
import com.gregtechceu.gtceu.api.sound.SoundEntry;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * AF9's sound events. The sounds are vanilla files pitched down, defined in assets/af9/sounds.json:
 * <ul>
 * <li>{@link #ORBITAL_STATION}: the Orbital Lithography Station's working sound (GT loops it while a print runs;
 * set on its recipe types in kubejs/startup_scripts/gtceu/photolithography.js): a deep beacon hum or portal roar;</li>
 * <li>{@link #ORBITAL_PULSE}: a low throb (warden heartbeat) each time its light ring flashes;</li>
 * <li>{@link #ORBITAL_IGNITE}: the end portal opening, an octave down, when the ring lights up.</li>
 * <li>{@link #PARTICLE_ACCELERATOR}: the Particle Accelerator's working sound (all three modes,
 * kubejs/startup_scripts/gtceu/particle_accelerator.js): the beacon hum pitched up, the magnets' electric whine;</li>
 * <li>{@link #ACCELERATOR_IGNITE}: a respawn anchor charging, pitched down, when its ring lights up;</li>
 * <li>{@link #ACCELERATOR_PASS}: a soft whoosh (a trident's riptide, pitched up) on every white flash of the ring, a
 * bunch of particles going round;</li>
 * <li>{@link #ACCELERATOR_ZAP}: a sharp fizz, pitched up, for every big lightning bolt.</li>
 * </ul>
 */
@SuppressWarnings("removal") // new ResourceLocation(ns, path) is the only constructor on 1.20.1
public final class AF9Sounds {

    public static final DeferredRegister<SoundEvent> SOUND_EVENTS = DeferredRegister.create(
            ForgeRegistries.SOUND_EVENTS, AF9Core.MOD_ID);

    public static final SoundEvent ORBITAL_STATION_EVENT = event("orbital_station");
    public static final SoundEvent ORBITAL_PULSE = event("orbital_station_pulse");
    public static final SoundEvent ORBITAL_IGNITE = event("orbital_station_ignite");
    public static final SoundEvent PARTICLE_ACCELERATOR_EVENT = event("particle_accelerator");
    public static final SoundEvent ACCELERATOR_IGNITE = event("particle_accelerator_ignite");
    public static final SoundEvent ACCELERATOR_PASS = event("particle_accelerator_pass");
    public static final SoundEvent ACCELERATOR_ZAP = event("particle_accelerator_zap");

    /** GT's wrappers of the working sounds, for {@code GTRecipeType.setSound}. */
    public static final SoundEntry ORBITAL_STATION = new ExistingSoundEntry(ORBITAL_STATION_EVENT, SoundSource.BLOCKS);
    public static final SoundEntry PARTICLE_ACCELERATOR = new ExistingSoundEntry(PARTICLE_ACCELERATOR_EVENT,
            SoundSource.BLOCKS);

    private AF9Sounds() {}

    private static SoundEvent event(String name) {
        SoundEvent event = SoundEvent.createVariableRangeEvent(new ResourceLocation(AF9Core.MOD_ID, name));
        SOUND_EVENTS.register(name, () -> event);
        return event;
    }

    public static void register(IEventBus modBus) {
        SOUND_EVENTS.register(modBus);
    }
}
