package com.af9.core.registry;

import com.af9.core.AF9Core;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.MissingMappingsEvent;

/**
 * Worlds from before AF9 Core registered the pack's content: their blocks and items were KubeJS's
 * ({@code kubejs:<id>}). What a world misses under such an id and AF9 Core has under {@code af9:<id>} is taken for it,
 * so placed blocks and the items in inventories, chests and networks carry over.
 */
@Mod.EventBusSubscriber(modid = AF9Core.MOD_ID)
@SuppressWarnings("removal") // new ResourceLocation(ns, path) is the only constructor on 1.20.1
public final class AF9Remaps {

    private static final String[] FORMER_NAMESPACES = { "kubejs" };

    private AF9Remaps() {}

    @SubscribeEvent
    public static void onMissingMappings(MissingMappingsEvent event) {
        remap(event, ForgeRegistries.Keys.BLOCKS, ForgeRegistries.BLOCKS);
        remap(event, ForgeRegistries.Keys.ITEMS, ForgeRegistries.ITEMS);
    }

    private static <T> void remap(MissingMappingsEvent event, ResourceKey<? extends Registry<T>> key,
                                  IForgeRegistry<T> registry) {
        for (String namespace : FORMER_NAMESPACES) {
            for (MissingMappingsEvent.Mapping<T> mapping : event.getMappings(key, namespace)) {
                ResourceLocation now = new ResourceLocation(AF9Core.MOD_ID, mapping.getKey().getPath());
                if (registry.containsKey(now)) mapping.remap(registry.getValue(now));
            }
        }
    }
}
