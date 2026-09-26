package com.af9.core.common;

import com.af9.core.AF9Core;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.level.Level;

/**
 * AF9's damage types (data: data/af9/damage_type, tags in data/minecraft/tags/damage_types).
 */
public final class AF9DamageTypes {

    /**
     * The light ring of a running Orbital Lithography Station: burns (fire effects), bypasses armour, enchantments and
     * potion effects; death message death.attack.af9.orbital_ring.
     */
    @SuppressWarnings("removal") // new ResourceLocation(ns, path) is the only constructor on 1.20.1
    public static final ResourceKey<DamageType> ORBITAL_RING = ResourceKey.create(Registries.DAMAGE_TYPE,
            new ResourceLocation(AF9Core.MOD_ID, "orbital_ring"));

    private AF9DamageTypes() {}

    /** The ring's damage source (plain fire if the damage type is missing from the data). */
    public static DamageSource orbitalRing(Level level) {
        return level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolder(ORBITAL_RING)
                .map(DamageSource::new)
                .orElseGet(() -> level.damageSources().inFire());
    }
}
