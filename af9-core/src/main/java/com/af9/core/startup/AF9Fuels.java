package com.af9.core.startup;

import com.af9.core.AF9Core;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.event.furnace.FurnaceFuelBurnTimeEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * AF9 - furnace fuels (was the startup script af9_fuels.js): spent filter cake (the filtration plant's waste dust) burns
 * like coal, asphalt bottoms burn like coke.
 */
@Mod.EventBusSubscriber(modid = AF9Core.MOD_ID)
public final class AF9Fuels {

    private AF9Fuels() {}

    @SubscribeEvent
    public static void onBurnTime(FurnaceFuelBurnTimeEvent event) {
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(event.getItemStack().getItem());
        if (id == null || !"gtceu".equals(id.getNamespace())) return;
        switch (id.getPath()) {
            case "filter_cake_dust" -> event.setBurnTime(1600);
            case "asphalt_dust" -> event.setBurnTime(3200);
            default -> {
            }
        }
    }
}
