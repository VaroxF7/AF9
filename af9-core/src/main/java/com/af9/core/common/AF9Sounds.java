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
 * </ul>
 */
@SuppressWarnings("removal") // new ResourceLocation(ns, path) is the only constructor on 1.20.1
public final class AF9Sounds {

    public static final DeferredRegister<SoundEvent> SOUND_EVENTS = DeferredRegister.create(
            ForgeRegistries.SOUND_EVENTS, AF9Core.MOD_ID);

    public static final SoundEvent ORBITAL_STATION_EVENT = event("orbital_station");
    public static final SoundEvent ORBITAL_PULSE = event("orbital_station_pulse");
    public static final SoundEvent ORBITAL_IGNITE = event("orbital_station_ignite");

    /** GT's wrapper of the working sound, for {@code GTRecipeType.setSound}. */
    public static final SoundEntry ORBITAL_STATION = new ExistingSoundEntry(ORBITAL_STATION_EVENT, SoundSource.BLOCKS);

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
