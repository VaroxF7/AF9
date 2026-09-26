package com.af9.core.compat.curios;

import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraftforge.fml.ModList;

import top.theillusivec4.curios.api.CuriosApi;

/**
 * Curios slots (GT's Rubber Gloves go in the hands slot). Only call through {@link #isEquipped}: it checks that
 * Curios is loaded before anything touches the Curios API.
 */
public final class CuriosCompat {

    private static final boolean LOADED = ModList.get().isLoaded("curios");

    private CuriosCompat() {}

    /** An item of the tag in any of the entity's Curios slots; false without Curios. */
    public static boolean isEquipped(LivingEntity entity, TagKey<Item> tag) {
        return LOADED && Api.isEquipped(entity, tag);
    }

    /** Loaded only when Curios is. */
    private static final class Api {

        static boolean isEquipped(LivingEntity entity, TagKey<Item> tag) {
            return CuriosApi.getCuriosInventory(entity)
                    .map(curios -> curios.isEquipped(stack -> stack.is(tag)))
                    .orElse(false);
        }
    }
}
