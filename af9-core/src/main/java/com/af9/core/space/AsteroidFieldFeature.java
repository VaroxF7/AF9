package com.af9.core.space;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.XoroshiroRandomSource;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;

/**
 * The asteroids of the Asteroid Field (af9:asteroid_field), a void dimension: lumpy rocks of five size classes, from
 * pebbles to rocks a hundred blocks across.
 * <p>
 * Every chunk draws every asteroid that reaches into it and fills only its own part, so the rocks come out whole
 * whatever order the chunks are generated in. An asteroid is fixed by the world seed and the cell of its size class
 * it belongs to: the cells of a class are squares of {@link SizeClass#cell} blocks, each holds
 * {@link SizeClass#minCount} to {@link SizeClass#maxCount} asteroids at random places, and the radius of each is
 * random between the class's limits (small ones more often than large ones). The shape is an ellipsoid with
 * different radii on each axis whose surface is pushed in and out by two layers of simplex noise.
 * <p>
 * The rock is a mix of andesite, tuff, basalt and blackstone (by a slow noise, so it comes in patches): the stones
 * GregTech has ore blocks for, which is what its ore veins (the {@code af9_asteroid} layer, KubeJS) grow into. Ad
 * Astra builds a space station at y = 100, so the rocks hang below it.
 */
public class AsteroidFieldFeature extends Feature<NoneFeatureConfiguration> {

    /** Lowest and highest y a rock's centre can have. */
    public static final int CENTER_MIN_Y = 8;
    public static final int CENTER_MAX_Y = 92;

    /** Largest stretch of a class's radius along one axis. */
    private static final double MAX_STRETCH = 1.25;
    /** Largest bulge of the surface: 1 + the two noise layers' amplitudes (0.25 and 0.10). */
    private static final double MAX_BULGE = 1.35;
    /** Blocks from the centre per block of a class's radius that an asteroid of that class can reach. */
    private static final double MAX_EXTENT = MAX_STRETCH * MAX_BULGE;

    /**
     * A size class: cells of {@code cell} x {@code cell} blocks, {@code minCount}..{@code maxCount} asteroids in each,
     * radii {@code minR}..{@code maxR}; {@code spread} is how far the centre's height reaches from the middle of the
     * band (0 = all in the middle, 1 = the whole band).
     */
    private record SizeClass(int salt, int cell, int minCount, int maxCount, int minR, int maxR, double spread) {

        /** Blocks around the centre of an asteroid its shape can reach. */
        int reach() {
            return (int) Math.ceil(maxR * MAX_EXTENT) + 1;
        }
    }

    // About 12 % of the band is rock. Pebbles, small, medium, large and huge rocks.
    private static final SizeClass[] CLASSES = {
            new SizeClass(1, 16, 1, 3, 2, 4, 1.0),
            new SizeClass(2, 32, 1, 3, 4, 8, 1.0),
            new SizeClass(3, 64, 1, 3, 9, 16, 1.0),
            new SizeClass(4, 96, 1, 2, 18, 28, 0.8),
            new SizeClass(5, 160, 1, 1, 32, 46, 0.5),
    };

    public AsteroidFieldFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        int chunkMinX = origin.getX() & ~15;
        int chunkMinZ = origin.getZ() & ~15;
        long seed = level.getSeed();
        boolean placed = false;
        for (SizeClass sizeClass : CLASSES) {
            int reach = sizeClass.reach();
            int cellMinX = Mth.floorDiv(chunkMinX - reach, sizeClass.cell);
            int cellMaxX = Mth.floorDiv(chunkMinX + 15 + reach, sizeClass.cell);
            int cellMinZ = Mth.floorDiv(chunkMinZ - reach, sizeClass.cell);
            int cellMaxZ = Mth.floorDiv(chunkMinZ + 15 + reach, sizeClass.cell);
            for (int cellX = cellMinX; cellX <= cellMaxX; cellX++) {
                for (int cellZ = cellMinZ; cellZ <= cellMaxZ; cellZ++) {
                    placed |= fillCell(level, seed, sizeClass, cellX, cellZ, chunkMinX, chunkMinZ);
                }
            }
        }
        return placed;
    }

    /** The asteroids of one cell of a class, as far as they reach into the chunk. */
    private static boolean fillCell(WorldGenLevel level, long seed, SizeClass sizeClass, int cellX, int cellZ,
                                    int chunkMinX, int chunkMinZ) {
        RandomSource random = new XoroshiroRandomSource(cellSeed(seed, sizeClass.salt, cellX, cellZ));
        int count = sizeClass.minCount + random.nextInt(sizeClass.maxCount - sizeClass.minCount + 1);
        boolean placed = false;
        // always draw every parameter of every asteroid, so the next one does not depend on whether this one is in range
        for (int i = 0; i < count; i++) {
            double centerX = cellX * sizeClass.cell + random.nextInt(sizeClass.cell) + 0.5;
            double centerZ = cellZ * sizeClass.cell + random.nextInt(sizeClass.cell) + 0.5;
            double band = (random.nextDouble() + random.nextDouble()) - 1.0; // -1..1, most near 0
            double middle = (CENTER_MIN_Y + CENTER_MAX_Y) / 2.0;
            double centerY = middle + band * sizeClass.spread * (CENTER_MAX_Y - CENTER_MIN_Y) / 2.0;
            double size = Math.pow(random.nextDouble(), 1.6);
            double radius = sizeClass.minR + size * (sizeClass.maxR - sizeClass.minR);
            double radiusX = radius * (0.75 + random.nextDouble() * 0.5);
            double radiusY = radius * (0.60 + random.nextDouble() * 0.5);
            double radiusZ = radius * (0.75 + random.nextDouble() * 0.5);
            long noiseSeed = random.nextLong();
            if (!reaches(centerX, centerZ, radiusX, radiusZ, chunkMinX, chunkMinZ)) continue;
            placed |= fill(level, centerX, centerY, centerZ, radiusX, radiusY, radiusZ, noiseSeed, chunkMinX,
                    chunkMinZ);
        }
        return placed;
    }

    private static boolean reaches(double centerX, double centerZ, double radiusX, double radiusZ, int chunkMinX,
                                   int chunkMinZ) {
        double reachX = radiusX * MAX_BULGE;
        double reachZ = radiusZ * MAX_BULGE;
        return centerX + reachX >= chunkMinX && centerX - reachX < chunkMinX + 16 &&
                centerZ + reachZ >= chunkMinZ && centerZ - reachZ < chunkMinZ + 16;
    }

    /** Sets the blocks of one asteroid inside the chunk. */
    private static boolean fill(WorldGenLevel level, double centerX, double centerY, double centerZ, double radiusX,
                                double radiusY, double radiusZ, long noiseSeed, int chunkMinX, int chunkMinZ) {
        RandomSource random = new XoroshiroRandomSource(noiseSeed);
        SimplexNoise shape = new SimplexNoise(random);
        SimplexNoise detail = new SimplexNoise(random);
        SimplexNoise rock = new SimplexNoise(random);
        double radius = Math.max(radiusX, Math.max(radiusY, radiusZ));
        double shapeScale = 1.0 / Math.max(6.0, radius * 0.9);
        double detailScale = 1.0 / Math.max(3.0, radius * 0.3);

        int minX = Math.max(chunkMinX, Mth.floor(centerX - radiusX * MAX_BULGE));
        int maxX = Math.min(chunkMinX + 15, Mth.ceil(centerX + radiusX * MAX_BULGE));
        int minZ = Math.max(chunkMinZ, Mth.floor(centerZ - radiusZ * MAX_BULGE));
        int maxZ = Math.min(chunkMinZ + 15, Mth.ceil(centerZ + radiusZ * MAX_BULGE));
        int minY = Math.max(level.getMinBuildHeight(), Mth.floor(centerY - radiusY * MAX_BULGE));
        int maxY = Math.min(level.getMaxBuildHeight() - 1, Mth.ceil(centerY + radiusY * MAX_BULGE));
        boolean placed = false;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = minX; x <= maxX; x++) {
            double dx = (x + 0.5 - centerX) / radiusX;
            for (int z = minZ; z <= maxZ; z++) {
                double dz = (z + 0.5 - centerZ) / radiusZ;
                for (int y = minY; y <= maxY; y++) {
                    double dy = (y + 0.5 - centerY) / radiusY;
                    double distanceSquared = dx * dx + dy * dy + dz * dz;
                    if (distanceSquared > MAX_BULGE * MAX_BULGE) continue;
                    double bulge = 1.0 + 0.25 * shape.getValue(x * shapeScale, y * shapeScale, z * shapeScale) +
                            0.10 * detail.getValue(x * detailScale, y * detailScale, z * detailScale);
                    if (distanceSquared >= bulge * bulge) continue;
                    pos.set(x, y, z);
                    level.setBlock(pos, rockAt(rock.getValue(x * 0.09, y * 0.09, z * 0.09)), 2);
                    placed = true;
                }
            }
        }
        return placed;
    }

    /** The stone of a patch: darker blackstone and basalt, lighter andesite and tuff. */
    private static BlockState rockAt(double noise) {
        if (noise < -0.35) return Blocks.BLACKSTONE.defaultBlockState();
        if (noise < 0.15) return Blocks.ANDESITE.defaultBlockState();
        if (noise < 0.55) return Blocks.TUFF.defaultBlockState();
        return Blocks.BASALT.defaultBlockState();
    }

    /** A well-mixed seed of one cell (the finalizer of MurmurHash3 over the world seed, class and cell). */
    private static long cellSeed(long seed, int salt, int cellX, int cellZ) {
        long hash = seed ^ (salt * 0x9E3779B97F4A7C15L);
        hash ^= cellX * 0xC2B2AE3D27D4EB4FL;
        hash = mix(hash);
        hash ^= cellZ * 0x165667B19E3779F9L;
        return mix(hash);
    }

    private static long mix(long value) {
        value ^= value >>> 33;
        value *= 0xFF51AFD7ED558CCDL;
        value ^= value >>> 33;
        value *= 0xC4CEB9FE1A85EC53L;
        value ^= value >>> 33;
        return value;
    }
}
