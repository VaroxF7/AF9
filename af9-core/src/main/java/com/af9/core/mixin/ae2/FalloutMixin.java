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
 * AE2 meteorites spawn on the Moon and Mars (biome tag {@code ae2:has_meteorites},
 * {@code kubejs/data}): their crater fallout is moon sand / mars sand instead of the
 * overworld's stone, cobble, dirt, gravel and grass. String target and vanilla-only
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
        // AE2's inset: cobble / stone / grass / skystone / gravel / air by descending
        // roll. The planet keeps the skystone and air slots, sand fills the rest.
        float a = random.nextFloat();
        if (a > 0.6f && a <= 0.7f) {
            af9$put(level, pos, this.skyStone);
        } else if (a <= 0.5f) {
            af9$put(level, pos, Blocks.AIR.defaultBlockState());
        } else {
            af9$put(level, pos, sand);
        }
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
