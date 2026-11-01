package com.af9.core.mixin.gtceu;

import com.af9.core.worldgen.AF9Veins;

import com.gregtechceu.gtceu.data.loader.GTOreLoader;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * GT's ore loader builds the vein generators once every vein is in: the addons', KubeJS's and the data packs'. AF9's
 * veins join here, after the pack's scripts (which move every vein they find to the Mining Dimension).
 */
@Mixin(value = GTOreLoader.class, remap = false)
public abstract class OreLoaderMixin {

    @Inject(method = "buildVeinGenerator", at = @At("HEAD"), remap = false)
    private static void af9$oreVeins(CallbackInfo ci) {
        AF9Veins.oreVeins();
    }
}
