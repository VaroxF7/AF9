package com.af9.core.space;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * What the crater fallout of an AE2 meteorite on the Moon or Mars is made of (used by the {@code ae2.Fallout*}
 * mixins): the planet's own sand and stone and Impact Slag, never glass, dirt, grass, gravel or snow.
 */
@SuppressWarnings("removal") // new ResourceLocation(ns, path) is the only constructor on 1.20.1
public final class PlanetFallout {

    public enum Planet { MOON, MARS }

    private PlanetFallout() {}

    /** The planet the biome at the position belongs to (Ad Astra's lunar_* and martian_* biomes), or null. */
    public static Planet planetAt(LevelAccessor level, BlockPos pos) {
        var key = level.getBiome(pos).unwrapKey().orElse(null);
        if (key == null || !"ad_astra".equals(key.location().getNamespace())) return null;
        String path = key.location().getPath();
        if (path.contains("lunar")) return Planet.MOON;
        if (path.contains("martian")) return Planet.MARS;
        return null;
    }

    private static BlockState block(String id, BlockState fallback) {
        var found = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(id));
        return found == null || found == Blocks.AIR ? fallback : found.defaultBlockState();
    }

    private static BlockState sand(Planet planet) {
        return block(planet == Planet.MOON ? "ad_astra:moon_sand" : "ad_astra:mars_sand",
                Blocks.DEEPSLATE.defaultBlockState());
    }

    private static BlockState stone(Planet planet) {
        return block(planet == Planet.MOON ? "ad_astra:moon_stone" : "ad_astra:mars_stone",
                Blocks.DEEPSLATE.defaultBlockState());
    }

    private static BlockState slag() {
        return AF9Space.IMPACT_SLAG.get().defaultBlockState();
    }

    /** Debris thrown onto the ground around the crater: mostly sand, some stone, the odd lump of slag. */
    public static BlockState fall(Planet planet, RandomSource random) {
        float a = random.nextFloat();
        if (a > 0.9f) return slag();
        if (a > 0.7f) return stone(planet);
        return sand(planet);
    }

    /**
     * The blocks of the ground around the meteorite that get changed: air (a crater floor eaten away), sand, stone,
     * slag and sky stone, the same shares AE2's own inset has for air and sky stone.
     */
    public static BlockState inset(Planet planet, RandomSource random, BlockState skyStone) {
        float a = random.nextFloat();
        if (a <= 0.5f) return Blocks.AIR.defaultBlockState();
        if (a <= 0.6f) return sand(planet);
        if (a <= 0.7f) return skyStone;
        if (a <= 0.8f) return slag();
        if (a <= 0.9f) return stone(planet);
        return sand(planet);
    }
}
