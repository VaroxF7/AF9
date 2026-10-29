package com.af9.core.space;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.XoroshiroRandomSource;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

/**
 * The certus crystal formations of af9:certus_crystal (the Moon) and af9:certus_crystal_asteroids (the Asteroid
 * Field): an outcrop is one tall central spire of AE2's certus quartz with a handful of slanted shards leaning out
 * of its foot, the lower blocks of the spire partly budding quartz (the damaged to flawless varieties) and AE2's
 * own quartz buds and clusters growing out of the faces. All solid quartz, no glass, and sparse: about 200 blocks
 * an outcrop. An outcrop stays within one chunk's neighbours (anchor 4..11 in the chunk, shards reach under 9
 * blocks sideways) so it comes out whole in any generation order. Blocks are looked up by name, so a missing AE2
 * falls back to vanilla quartz and amethyst.
 */
@SuppressWarnings("removal") // new ResourceLocation(ns, path) is the only constructor on 1.20.1
public class CertusCrystalFeature extends Feature<NoneFeatureConfiguration> {

    /** Where the formations stand: on the surface of the Moon, or on any rock of the asteroids. */
    public enum Surface { MOON, ASTEROIDS }

    private static final int OUTCROPS_MOON = 2;
    private static final int OUTCROPS_ASTEROIDS = 6;
    /** Chance (per outcrop attempt) that an outcrop grows at all. */
    private static final double OUTCROP_CHANCE = 0.45;
    private static final int SCAN_TOP = 285;
    private static final int SCAN_BOTTOM = 0;
    /** Chance that an air block touching the crystal grows a bud or cluster. */
    private static final double BUD_CHANCE = 0.07;

    private final Surface surface;

    public CertusCrystalFeature(Surface surface) {
        super(NoneFeatureConfiguration.CODEC);
        this.surface = surface;
    }

    private static Block block(String id, Block fallback) {
        Block found = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(id));
        return found == null || found == Blocks.AIR ? fallback : found;
    }

    /** The blocks an outcrop is built of; budding varieties and buds are looked up once per feature call. */
    private record Palette(Block solid, Block[] budding, Block[] buds) {}

    private static Palette palette() {
        Block solid = block("ae2:quartz_block", Blocks.QUARTZ_BLOCK);
        Block[] budding = {
                block("ae2:damaged_budding_quartz", solid),
                block("ae2:chipped_budding_quartz", solid),
                block("ae2:flawed_budding_quartz", solid),
                block("ae2:flawless_budding_quartz", solid)};
        Block[] buds = {
                block("ae2:small_quartz_bud", Blocks.SMALL_AMETHYST_BUD),
                block("ae2:medium_quartz_bud", Blocks.MEDIUM_AMETHYST_BUD),
                block("ae2:large_quartz_bud", Blocks.LARGE_AMETHYST_BUD),
                block("ae2:quartz_cluster", Blocks.AMETHYST_CLUSTER)};
        return new Palette(solid, budding, buds);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        int chunkMinX = origin.getX() & ~15;
        int chunkMinZ = origin.getZ() & ~15;
        RandomSource random = new XoroshiroRandomSource(
                level.getSeed() ^ (chunkMinX * 0x9E3779B97F4A7C15L) ^ (chunkMinZ * 0xC2B2AE3D27D4EB4FL)
                        ^ (surface == Surface.MOON ? 0x3A17C0L : 0x3A17C1L));
        Palette palette = palette();

        boolean placed = false;
        int attempts = surface == Surface.MOON ? OUTCROPS_MOON : OUTCROPS_ASTEROIDS;
        for (int i = 0; i < attempts; i++) {
            if (random.nextDouble() >= OUTCROP_CHANCE) continue;
            int x = chunkMinX + 4 + random.nextInt(8);
            int z = chunkMinZ + 4 + random.nextInt(8);
            int y = surfaceY(level, x, z, random);
            if (y < 0) continue;
            placed |= outcrop(level, random, new BlockPos(x, y, z), palette);
        }
        return placed;
    }

    /** The y of a free-standing block face (solid below, air above) of the column, or -1; on the asteroids one of them. */
    private int surfaceY(WorldGenLevel level, int x, int z, RandomSource random) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        if (surface == Surface.MOON) {
            int top = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z);
            if (top <= level.getMinBuildHeight()) return -1;
            pos.set(x, top - 1, z);
            return isRock(level.getBlockState(pos)) ? top : -1;
        }
        List<Integer> faces = new ArrayList<>();
        int top = Math.min(SCAN_TOP, level.getMaxBuildHeight() - 2);
        for (int y = top; y > Math.max(SCAN_BOTTOM, level.getMinBuildHeight()); y--) {
            pos.set(x, y, z);
            if (level.getBlockState(pos).isAir() && isRock(level.getBlockState(pos.move(Direction.DOWN)))) {
                faces.add(y);
            }
        }
        return faces.isEmpty() ? -1 : faces.get(random.nextInt(faces.size()));
    }

    private static boolean isRock(BlockState state) {
        return !state.isAir() && state.getFluidState().isEmpty() && state.canOcclude();
    }

    /** One outcrop: the central spire, the shards at its foot, then the buds on every free face. */
    private boolean outcrop(WorldGenLevel level, RandomSource random, BlockPos base, Palette palette) {
        List<BlockPos> placed = new ArrayList<>();
        int height = 12 + random.nextInt(13);
        double leanX = (random.nextDouble() - 0.5) * 0.4;
        double leanZ = (random.nextDouble() - 0.5) * 0.4;
        line(level, random, palette, placed, base, leanX, leanZ, height, 1 + random.nextInt(2), true);

        int shards = 4 + random.nextInt(5);
        for (int i = 0; i < shards; i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double reach = 0.35 + random.nextDouble() * 0.45;
            BlockPos foot = footing(level, base.offset((int) Math.round(Math.cos(angle) * (1 + random.nextInt(4))),
                    0, (int) Math.round(Math.sin(angle) * (1 + random.nextInt(4)))));
            if (foot == null) continue;
            line(level, random, palette, placed, foot, Math.cos(angle) * reach, Math.sin(angle) * reach,
                    5 + random.nextInt(8), random.nextInt(2), false);
        }
        if (placed.isEmpty()) return false;
        grow(level, random, palette, placed);
        return true;
    }

    /** The free face near a position (within 3 blocks up or down), or null if there is none (a shard needs ground). */
    private BlockPos footing(WorldGenLevel level, BlockPos near) {
        BlockPos.MutableBlockPos pos = near.mutable();
        for (int d = 0; d <= 3; d++) {
            for (int sign = -1; sign <= 1; sign += 2) {
                pos.set(near.getX(), near.getY() + d * sign, near.getZ());
                if (level.getBlockState(pos).isAir() && isRock(level.getBlockState(pos.below()))) {
                    return pos.immutable();
                }
            }
        }
        return null;
    }

    /**
     * One spire or shard: from the base block (dug one block into the ground) up {@code length} blocks, a square
     * column with cut corners that narrows to a point and leans by (leanX, leanZ) blocks per block of height. The
     * lowest blocks of a {@code spire} are partly budding quartz.
     */
    private void line(WorldGenLevel level, RandomSource random, Palette palette, List<BlockPos> placed, BlockPos base,
                      double leanX, double leanZ, int length, int radius, boolean spire) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int h = -1; h < length; h++) {
            double t = Math.max(0, h) / (double) length;
            int w = (int) Math.floor(radius * Math.pow(1.0 - t, 0.7) + 0.35);
            int cx = base.getX() + (int) Math.round(leanX * h);
            int cz = base.getZ() + (int) Math.round(leanZ * h);
            int y = base.getY() + h;
            if (y >= level.getMaxBuildHeight() - 1) break;
            for (int dx = -w; dx <= w; dx++) {
                for (int dz = -w; dz <= w; dz++) {
                    if (Math.abs(dx) + Math.abs(dz) > w * 1.5 + 0.5) continue;
                    pos.set(cx + dx, y, cz + dz);
                    BlockState old = level.getBlockState(pos);
                    // above the ground a spire never eats rock, and the base block digs in so nothing floats
                    if (h >= 0 && !old.isAir() && !old.canBeReplaced()) continue;
                    Block chosen = palette.solid();
                    if (spire && h <= 3 && w >= 1 && random.nextInt(4) == 0) chosen = budding(random, palette);
                    level.setBlock(pos, chosen.defaultBlockState(), 2);
                    placed.add(pos.immutable());
                }
            }
        }
    }

    /** Damaged budding quartz mostly, flawless rarely. */
    private static Block budding(RandomSource random, Palette palette) {
        int roll = random.nextInt(20);
        Block[] budding = palette.budding();
        return roll == 0 ? budding[3] : roll < 3 ? budding[2] : roll < 8 ? budding[1] : budding[0];
    }

    /** AE2's buds and clusters on the free faces of the crystal, taller clusters higher up. */
    private void grow(WorldGenLevel level, RandomSource random, Palette palette, List<BlockPos> placed) {
        Block[] buds = palette.buds();
        for (BlockPos at : placed) {
            if (random.nextDouble() >= BUD_CHANCE) continue;
            Direction face = Direction.getRandom(random);
            BlockPos target = at.relative(face);
            if (!level.getBlockState(target).isAir()) continue;
            Block bud = buds[Math.min(3, random.nextInt(2) + random.nextInt(3))];
            BlockState state = bud.defaultBlockState();
            if (state.hasProperty(BlockStateProperties.FACING)) state = state.setValue(BlockStateProperties.FACING, face);
            if (state.hasProperty(BlockStateProperties.WATERLOGGED)) {
                state = state.setValue(BlockStateProperties.WATERLOGGED, false);
            }
            level.setBlock(target, state, 2);
        }
    }
}
