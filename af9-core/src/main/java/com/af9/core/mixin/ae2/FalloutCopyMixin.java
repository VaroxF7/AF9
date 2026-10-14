package com.af9.core.mixin.ae2;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import net.minecraftforge.registries.ForgeRegistries;

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
        BlockState sand = af9$planetSand(level, pos);
        if (sand == null) return;
        random.nextFloat();
        af9$put(level, pos, sand);
        ci.cancel();
    }

    @Inject(method = "getRandomInset", at = @At("HEAD"), cancellable = true, remap = false)
    private void af9$planetInset(LevelAccessor level, BlockPos pos, CallbackInfo ci) {
        BlockState sand = af9$planetSand(level, pos);
        if (sand == null) return;
        random.nextFloat();
        af9$put(level, pos, sand);
        ci.cancel();
    }

    /** AE2's MeteoriteBlockPutter guard, without needing AE2 at compile time. */
    @Unique
    private static void af9$put(LevelAccessor level, BlockPos pos, BlockState next) {
        var current = level.getBlockState(pos);
        if (current.getBlock() == Blocks.BEDROCK || current == next) return;
        level.setBlock(pos, next, 3);
    }

    /** Moon sand on lunar biomes, mars sand on martian ones, else null (AE2 runs). */
    @Unique
    private static BlockState af9$planetSand(LevelAccessor level, BlockPos pos) {
        var key = level.getBiome(pos).unwrapKey().orElse(null);
        if (key == null || !"ad_astra".equals(key.location().getNamespace())) return null;
        String path = key.location().getPath();
        String sand;
        if (path.contains("lunar")) {
            sand = "moon_sand";
        } else if (path.contains("martian")) {
            sand = "mars_sand";
        } else {
            return null;
        }
        var block = ForgeRegistries.BLOCKS.getValue(new ResourceLocation("ad_astra", sand));
        return block == null ? null : block.defaultBlockState();
    }
}
