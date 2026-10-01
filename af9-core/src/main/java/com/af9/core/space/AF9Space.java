package com.af9.core.space;

import com.af9.core.AF9Core;

import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Space: the world generation of the Asteroid Field (data/af9: dimension af9:asteroid_field, the biome of the same
 * name that carries the feature, the planets af9:ceres and af9:asteroid_field for Ad Astra). Spec:
 * docs/asteroid-fission.md.
 */
public final class AF9Space {

    public static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(ForgeRegistries.FEATURES,
            AF9Core.MOD_ID);

    /** The asteroids; placed by data/af9/worldgen/placed_feature/asteroid_field.json. */
    public static final RegistryObject<AsteroidFieldFeature> ASTEROID_FIELD = FEATURES.register("asteroid_field",
            AsteroidFieldFeature::new);

    private AF9Space() {}

    public static void register(IEventBus modEventBus) {
        FEATURES.register(modEventBus);
    }
}
