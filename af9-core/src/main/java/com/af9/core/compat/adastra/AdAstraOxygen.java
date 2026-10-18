package com.af9.core.compat.adastra;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraftforge.fml.ModList;

import earth.terrarium.adastra.api.systems.OxygenApi;

/**
 * Ad Astra's oxygen: whether a position has breathable air (its oxygen distributors, the Earth, a sealed room).
 * Without Ad Astra every position has oxygen.
 */
public final class AdAstraOxygen {

    private AdAstraOxygen() {}

    public static boolean hasOxygen(Level level, BlockPos pos) {
        return !ModList.get().isLoaded("ad_astra") || Api.hasOxygen(level, pos);
    }

    /** Holds the Ad Astra classes, so they load only with the mod. */
    private static final class Api {

        static boolean hasOxygen(Level level, BlockPos pos) {
            return OxygenApi.API.hasOxygen(level, pos);
        }
    }
}
