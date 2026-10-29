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
 * AE2 meteorites spawn on the Moon and Mars (biome tag {@code ae2:has_meteorites},
 * {@code kubejs/data}): their crater fallout is the planet's sand and stone and Impact Slag
 * ({@link PlanetFallout}) instead of the overworld's stone, cobble, dirt, gravel and grass. String target and vanilla-only
 * members, so this needs no AE2 at compile time and skips itself where AE2 is absent.
 * Covers the base {@code Fallout} (the DEFAULT mode the Moon/Mars biomes fall into);
 * biome-tagged modes (sandy/cold) run through {@code FalloutCopy} and are covered by
 * {@code FalloutCopyMixin}. Like AE2's own putter, bedrock and the identical block
 * are left alone.
 */
@Pseudo
@Mixin(targets = "appeng.worldgen.meteorite.fallout.Fallout", remap = false)
public abstract class FalloutMixin {

    @Shadow
    @Final
    protected RandomSource random;

    @Shadow
    @Final
    private BlockState skyStone;

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
        af9$put(level, pos, PlanetFallout.inset(planet, random, this.skyStone));
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
