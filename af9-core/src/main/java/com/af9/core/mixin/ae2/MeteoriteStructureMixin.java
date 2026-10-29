package com.af9.core.mixin.ae2;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.structure.Structure;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * AE2 picks a crater type from the biome's temperature and rain (lava lakes, water, snow, ice, obsidian) and a crater
 * lake from the sea level. The Moon and Mars have none of that: their meteorites always get the plain dry crater and
 * never a lake. String target and a raw {@link CallbackInfoReturnable} (the return type is AE2's enum), so this needs
 * no AE2 at compile time and skips itself where AE2 is absent. Every other biome runs AE2's code.
 */
@Pseudo
@Mixin(targets = "appeng.worldgen.meteorite.MeteoriteStructure", remap = false)
public abstract class MeteoriteStructureMixin {

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Inject(method = "determineCraterType", at = @At("HEAD"), cancellable = true, remap = false)
    private static void af9$dryCrater(BlockPos pos, Holder<Biome> biome, WorldgenRandom random,
                                      CallbackInfoReturnable cir) {
        if (!af9$offWorld(biome)) return;
        try {
            Class type = Class.forName("appeng.worldgen.meteorite.CraterType");
            cir.setReturnValue(Enum.valueOf(type, "NORMAL"));
        } catch (ReflectiveOperationException | RuntimeException e) {
            // AE2 changed: leave its craters alone
        }
    }

    @Inject(method = "locateWaterAroundTheCrater", at = @At("HEAD"), cancellable = true, remap = false)
    private static void af9$noLake(BlockPos pos, float radius, Structure.GenerationContext context,
                                   CallbackInfoReturnable<Boolean> cir) {
        var biome = context.biomeSource().getNoiseBiome(pos.getX() >> 2, pos.getY() >> 2, pos.getZ() >> 2,
                context.randomState().sampler());
        if (af9$offWorld(biome)) cir.setReturnValue(false);
    }

    private static boolean af9$offWorld(Holder<Biome> biome) {
        var key = biome.unwrapKey().orElse(null);
        return key != null && "ad_astra".equals(key.location().getNamespace());
    }
}
