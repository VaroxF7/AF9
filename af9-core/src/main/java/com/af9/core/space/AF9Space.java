package com.af9.core.space;

import com.af9.core.AF9Core;

import com.gregtechceu.gtceu.api.data.worldgen.SimpleWorldGenLayer;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.structure.templatesystem.TagMatchTest;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.Set;

/**
 * Space: the world generation of the Asteroid Field (data/af9: dimension af9:asteroid_field, the biome of the same
 * name that carries the feature, the planets af9:ceres and af9:asteroid_field for Ad Astra). Spec:
 * docs/asteroid-fission.md.
 */
@SuppressWarnings("removal") // new ResourceLocation(ns, path) is the only constructor on 1.20.1
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

    /** The dimension of the Asteroid Field (data/af9/dimension). */
    public static final ResourceLocation ASTEROID_FIELD_DIMENSION = new ResourceLocation(AF9Core.MOD_ID,
            "asteroid_field");
    /**
     * The rock of the asteroids: the four stones GT has ore blocks for, which {@link AsteroidFieldFeature} builds them
     * of (data/af9/tags/blocks/asteroid_rock.json).
     */
    public static final TagKey<Block> ASTEROID_ROCK = BlockTags.create(new ResourceLocation(AF9Core.MOD_ID,
            "asteroid_rock"));
    /** The layer GT's ore veins grow into in the Asteroid Field (the veins: KubeJS, asteroid_fission.js). */
    public static final String ORE_LAYER = "af9_asteroid";

    private AF9Space() {}

    /** GregTech collects its addons' ore layers ({@link com.af9.core.AF9Addon}); a layer registers itself. */
    public static void registerWorldgenLayers() {
        new SimpleWorldGenLayer(ORE_LAYER, () -> new TagMatchTest(ASTEROID_ROCK), Set.of(ASTEROID_FIELD_DIMENSION));
    }

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
