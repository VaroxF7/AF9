package com.af9.core.distillation;

import com.af9.core.AF9Core;

import com.gregtechceu.gtceu.api.registry.GTRegistries;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RecipesUpdatedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Client side: when the recipes arrive, every recipe a Distillation Tower Module can be set to gets its ID written into its data
 * ({@link TowerRecipes#tagIds}), so the recipe viewers' pages (JEI and EMI both show GT's page) can print it.
 */
@Mod.EventBusSubscriber(modid = AF9Core.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class TowerRecipeIdEvents {

    private TowerRecipeIdEvents() {}

    @SubscribeEvent
    @SuppressWarnings("removal") // new ResourceLocation(ns, path) is the only constructor on 1.20.1
    public static void onRecipesUpdated(RecipesUpdatedEvent event) {
        var module = GTRegistries.MACHINES.get(new ResourceLocation("gtceu", "distillation_module"));
        if (module == null) return;
        TowerRecipes.tagIds(event.getRecipeManager(), module.getRecipeTypes());
    }
}
