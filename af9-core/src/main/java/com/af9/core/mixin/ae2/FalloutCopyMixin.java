package com.af9.core.mixin.ae2;

import com.af9.core.space.PlanetFallout;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Companion to {@code FalloutMixin}: AE2 picks the fallout class from the biome
 * ({@code FalloutMode.fromBiome}), so a lunar/martian biome carrying a vanilla tag
 * (sandy beach, cold polar caps) would run {@code FalloutCopy} (and its
 * {@code FalloutSand}/{@code FalloutSnow} subclasses) instead of the base
 * {@code Fallout} — whose overrides never call {@code super}. Same planet-sand
 * replacement, same bedrock guard, same string target so no AE2 is needed at
 * compile time.
 */
@Pseudo
@Mixin(targets = "appeng.worldgen.meteorite.fallout.FalloutCopy", remap = false)
public abstract class FalloutCopyMixin {

    @Shadow
    @Final
    protected RandomSource random;

    @Inject(method = "getRandomFall", at = @At("HEAD"), cancellable = true, remap = false)
    private void af9$planetFall(LevelAccessor level, BlockPos pos, CallbackInfo ci) {
        PlanetFallout.Planet planet = PlanetFallout.planetAt(level, pos);
        if (planet == null) return;
        af9$put(level, pos, PlanetFallout.fall(planet, random));
        ci.cancel();
    }

    @Inject(method = "getRandomInset", at = @At("HEAD"), cancellable = true, remap = false)
    private void af9$planetInset(LevelAccessor level, BlockPos pos, CallbackInfo ci) {
        PlanetFallout.Planet planet = PlanetFallout.planetAt(level, pos);
        if (planet == null) return;
        af9$put(level, pos, PlanetFallout.inset(planet, random, Blocks.AIR.defaultBlockState()));
        ci.cancel();
    }

    /** AE2's MeteoriteBlockPutter guard, without needing AE2 at compile time. */
    @Unique
    private static void af9$put(LevelAccessor level, BlockPos pos, BlockState next) {
        var current = level.getBlockState(pos);
        if (current.getBlock() == Blocks.BEDROCK || current == next) return;
        level.setBlock(pos, next, 3);
    }
}
