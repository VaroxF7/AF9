package com.af9.core.space;

import com.af9.core.AF9Core;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
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

    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS,
            AF9Core.MOD_ID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, AF9Core.MOD_ID);

    /** Oil Regolith: the oil of the game, in pockets of the asteroids' rock (docs/oil.md). */
    public static final RegistryObject<Block> OIL_REGOLITH = BLOCKS.register("oil_regolith",
            () -> new OilRegolithBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BROWN)
                    .strength(0.6f)
                    .sound(SoundType.SAND)));
    public static final RegistryObject<Item> OIL_REGOLITH_ITEM = ITEMS.register("oil_regolith",
            () -> new BlockItem(OIL_REGOLITH.get(), new Item.Properties()));

    /** The asteroids; placed by data/af9/worldgen/placed_feature/asteroid_field.json. */
    public static final RegistryObject<AsteroidFieldFeature> ASTEROID_FIELD = FEATURES.register("asteroid_field",
            AsteroidFieldFeature::new);

    private AF9Space() {}

    private static void fillCreativeTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.NATURAL_BLOCKS) event.accept(OIL_REGOLITH_ITEM);
    }

    public static void register(IEventBus modEventBus) {
        FEATURES.register(modEventBus);
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        modEventBus.addListener(AF9Space::fillCreativeTabs);
    }
}
