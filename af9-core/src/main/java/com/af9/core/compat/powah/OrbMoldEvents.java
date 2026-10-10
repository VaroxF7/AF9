package com.af9.core.compat.powah;

import com.af9.core.AF9Core;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RecipesUpdatedEvent;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** The mold cache of {@link OrbRecipes} is dropped when the recipes change (a reload, a join, a server start). */
public final class OrbMoldEvents {

    private OrbMoldEvents() {}

    @Mod.EventBusSubscriber(modid = AF9Core.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static final class Common {

        @SubscribeEvent
        public static void onSync(OnDatapackSyncEvent event) {
            OrbRecipes.invalidateMolds();
        }

        @SubscribeEvent
        public static void onStarted(ServerStartedEvent event) {
            OrbRecipes.invalidateMolds();
        }
    }

    @Mod.EventBusSubscriber(modid = AF9Core.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
    public static final class Client {

        @SubscribeEvent
        public static void onRecipes(RecipesUpdatedEvent event) {
            OrbRecipes.invalidateMolds();
        }
    }
}
