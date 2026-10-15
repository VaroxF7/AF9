package com.af9.core.space;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
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
 * Field): clumps of tall white spires of AE2's quartz that stand on the rock, a few thick giants in the middle of
 * a clump and a ring of small ones around it. Spires stay within one chunk's neighbours (anchor 4..11 in the chunk,
 * clump spread and lean under 12 blocks) so they come out whole in any generation order. Blocks are looked up by
 * name, so a missing AE2 falls back to vanilla quartz and glass.
 */
@SuppressWarnings("removal") // new ResourceLocation(ns, path) is the only constructor on 1.20.1
public class CertusCrystalFeature extends Feature<NoneFeatureConfiguration> {

    /** Where the formations stand: on the surface of the Moon, or on any rock of the asteroids. */
    public enum Surface { MOON, ASTEROIDS }

    private static final int CLUMPS_MOON = 4;
    private static final int CLUMPS_ASTEROIDS = 8;
    /** Chance (per clump attempt) that a clump grows at all. */
    private static final double CLUMP_CHANCE = 0.7;
    private static final int SCAN_TOP = 285;
    private static final int SCAN_BOTTOM = 0;
    /** One in this many base blocks of a giant spire is flawless budding quartz (it regrows clusters). */
    private static final int BUDDING_ONE_IN = 25;

    private final Surface surface;

    public CertusCrystalFeature(Surface surface) {
        super(NoneFeatureConfiguration.CODEC);
        this.surface = surface;
    }

    private static Block block(String id, Block fallback) {
        Block found = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(id));
        return found == null || found == Blocks.AIR ? fallback : found;
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
        Block solid = block("ae2:quartz_block", Blocks.QUARTZ_BLOCK);
        Block glass = block("ae2:quartz_glass", Blocks.GLASS);
        Block cluster = block("ae2:certus_quartz_cluster", Blocks.AMETHYST_CLUSTER);
        Block budding = block("ae2:flawless_budding_quartz", solid);

        boolean placed = false;
        int attempts = surface == Surface.MOON ? CLUMPS_MOON : CLUMPS_ASTEROIDS;
        for (int i = 0; i < attempts; i++) {
            if (random.nextDouble() >= CLUMP_CHANCE) continue;
            int x = chunkMinX + 4 + random.nextInt(8);
            int z = chunkMinZ + 4 + random.nextInt(8);
            int y = surfaceY(level, x, z, random);
            if (y < 0) continue;
            placed |= growClump(level, random, new BlockPos(x, y, z), solid, glass, cluster, budding);
        }
        return placed;
    }

    /** The y of a free-standing block face (solid below, air above) of the column, or -1; on the asteroids one of them. */
    private int surfaceY(WorldGenLevel level, int x, int z, RandomSource random) {
        if (surface == Surface.MOON) {
            int top = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z);
            return top > level.getMinBuildHeight() ? top : -1;
        }
        List<Integer> faces = new ArrayList<>();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
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

    /** One clump: 1 to 2 giants in the middle, 5 to 11 smaller spires around, cluster buds in the gaps at the foot. */
    private boolean growClump(WorldGenLevel level, RandomSource random, BlockPos center, Block solid, Block glass,
                              Block cluster, Block budding) {
        boolean placed = false;
        int giants = 1 + random.nextInt(2);
        for (int i = 0; i < giants; i++) {
            int dx = random.nextInt(5) - 2;
            int dz = random.nextInt(5) - 2;
            placed |= spire(level, random, center.offset(dx, 0, dz), 3 + random.nextInt(2), 18 + random.nextInt(14),
                    solid, glass, budding);
        }
        int smalls = 5 + random.nextInt(7);
        for (int i = 0; i < smalls; i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double dist = 3 + random.nextDouble() * 6;
            int dx = (int) Math.round(Math.cos(angle) * dist);
            int dz = (int) Math.round(Math.sin(angle) * dist);
            BlockPos base = footing(level, center.offset(dx, 0, dz));
            if (base == null) continue;
            placed |= spire(level, random, base, 1 + random.nextInt(2), 6 + random.nextInt(12), solid, glass,
                    budding);
        }
        BlockState bud = cluster.defaultBlockState();
        if (bud.hasProperty(BlockStateProperties.FACING)) bud = bud.setValue(BlockStateProperties.FACING, Direction.UP);
        for (int i = 0; i < 12; i++) {
            BlockPos at = footing(level, center.offset(random.nextInt(15) - 7, 0, random.nextInt(15) - 7));
            if (at != null && level.getBlockState(at).isAir()) {
                level.setBlock(at, bud, 2);
                placed = true;
            }
        }
        return placed;
    }

    /** The free face near a position (within 3 blocks up or down), or null if there is none (a spire needs ground). */
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
     * One spire: from the base block upward, a square column with cut corners that narrows to a point and leans a
     * little, quartz block in the lower third (the base, with the odd budding block) and glass above.
     */
    private boolean spire(WorldGenLevel level, RandomSource random, BlockPos base, int radius, int height, Block solid,
                          Block glass, Block budding) {
        double leanX = (random.nextDouble() - 0.5) * 0.5;
        double leanZ = (random.nextDouble() - 0.5) * 0.5;
        boolean placed = false;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int h = -1; h < height; h++) {
            double t = Math.max(0, h) / (double) height;
            double width = radius * Math.pow(1.0 - t, 0.65);
            int w = (int) Math.floor(width + 0.35);
            int cx = base.getX() + (int) Math.round(leanX * h);
            int cz = base.getZ() + (int) Math.round(leanZ * h);
            int y = base.getY() + h;
            if (y >= level.getMaxBuildHeight()) break;
            for (int dx = -w; dx <= w; dx++) {
                for (int dz = -w; dz <= w; dz++) {
                    if (Math.abs(dx) + Math.abs(dz) > w * 1.5 + 0.5) continue;
                    pos.set(cx + dx, y, cz + dz);
                    // the base block (-1) digs into the ground so the spire never floats
                    BlockState old = level.getBlockState(pos);
                    if (h >= 0 && !old.isAir() && old.canOcclude() && h > 2) continue;
                    boolean lower = t < 0.33;
                    Block chosen = lower && random.nextInt(BUDDING_ONE_IN) == 0 && w >= 2 ? budding
                            : lower || (h < 3) ? solid : glass;
                    level.setBlock(pos, chosen.defaultBlockState(), 2);
                    placed = true;
                }
            }
        }
        return placed;
    }
}
