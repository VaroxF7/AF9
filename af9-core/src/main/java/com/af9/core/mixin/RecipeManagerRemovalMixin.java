package com.af9.core.mixin;

import com.af9.core.recipes.Removals;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.crafting.RecipeManager;

import com.google.gson.JsonElement;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

/**
 * AF9's recipe removals ({@link Removals}) run at the head of the recipe load, after KubeJS's recipe event and before
 * GregTech builds its recipe lookup at the tail. The high priority puts this callback behind KubeJS's and GregTech's.
 */
@Mixin(value = RecipeManager.class, priority = 5000)
public abstract class RecipeManagerRemovalMixin {

    @Inject(method = "apply(Ljava/util/Map;Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/util/profiling/ProfilerFiller;)V", at = @At("HEAD"))
    private void af9$removals(Map<ResourceLocation, JsonElement> map, ResourceManager resourceManager,
            ProfilerFiller profiler, CallbackInfo ci) {
        Removals.apply(map);
    }
}
