package com.af9.core.space;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.XoroshiroRandomSource;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;

/**
 * The asteroids of the Asteroid Field (af9:asteroid_field), a void dimension: <b>clusters</b>, each a large island with
 * a swarm of smaller rocks around it at every height of a range of about 100 blocks, and a lot of empty space between
 * the clusters (about 2 blocks of rock in a column of the whole 300-block height).
 * <p>
 * Every chunk draws every cluster that reaches into it and fills only its own part, so the rocks come out whole
 * whatever order the chunks are generated in. A cluster is fixed by the world seed and the square cell of
 * {@link #CELL} blocks it belongs to (a cell holds one with a chance of {@link #CLUSTER_CHANCE}, at a random place in
 * it). Its height is the middle of the band plus a slow noise over the plane ({@link #DRIFT}: whole regions lie higher
 * or lower) and a random lift, so clusters hang at all heights, not in one flat band like the End's islands.
 * <ul>
 * <li>the island: a flattened ellipsoid with radii of {@link #ISLAND_MIN_R} to {@link #ISLAND_MAX_R} blocks, big enough
 * for a whole ore vein;</li>
 * <li>the satellites ({@link #SATELLITES} classes, {@code MIN_SATELLITES} to {@code MAX_SATELLITES} of them): rocks
 * from pebbles to 28 blocks of radius, around the island at a distance of its edge to 100 blocks beyond, anywhere in
 * {@link #SATELLITE_SPREAD_Y} blocks above and below the island.</li>
 * </ul>
 * Every rock is an ellipsoid whose surface is pushed in and out by two layers of simplex noise.
 * <p>
 * The rock is a mix of andesite, tuff, basalt and blackstone (by a slow noise, so it comes in patches): the stones
 * GregTech has ore blocks for, which is what its ore veins (the {@code af9_asteroid} layer, KubeJS) grow into. In
 * pockets of it (a second noise, about 7 % of the rock) the stone is {@link AF9Space#OIL_REGOLITH}, the sand-like,
 * oil-soaked rock all of the game's oil comes from. Ad Astra builds a space station at y = 100; rocks hang around it
 * at any height.
 * <p>
 * Temples ({@link TempleLayout}): the island of a cluster holds a temple (the biggest size that fits, a grand temple in all
 * but the smallest islands), a large satellite most of the time (60 %), a medium one now and then (25 %, a shrine); pebbles
 * and small rocks never. The hall is carved out of the rock and lined with polished blackstone brick, the corridor from it
 * runs out to a gate on the surface, and the chests (loot tables {@code af9:chests/ancient_shrine} and
 * {@code af9:chests/ancient_temple}) stand at the altar. The temple's blocks are not stones of the ore layer, so no ore
 * vein grows into the walls. Whether a rock holds one is a function of the rock's own seed, so it does not change which
 * rocks there are or where.
 */
public class AsteroidFieldFeature extends Feature<NoneFeatureConfiguration> {

    /** Lowest and highest y a rock's centre can have. */
    public static final int CENTER_MIN_Y = 5;
    public static final int CENTER_MAX_Y = 270;
    /** Cells of clusters (blocks), the chance of a cluster in a cell. */
    private static final int CELL = 420;
    private static final double CLUSTER_CHANCE = 0.55;
    /** How far the slow noise lifts or sinks a region's clusters (blocks), and how wide its features are (blocks). */
    private static final double DRIFT = 90;
    private static final double DRIFT_SCALE = 1.0 / 420.0;
    /** The random lift of a cluster on top of that (+- blocks). */
    private static final double LIFT = 55;
    /** Radii of the island (blocks) and how much flatter than wide it is (vertical radius / horizontal). */
    private static final int ISLAND_MIN_R = 45;
    private static final int ISLAND_MAX_R = 75;
    private static final double ISLAND_FLAT_MIN = 0.45;
    private static final double ISLAND_FLAT_MAX = 0.70;
    /**
     * Satellites per cluster; how far they lie from the island's edge; how far above and below the island's centre. (The
     * last two were 100 and 50: the rocks of a cluster lie about 1.5 times closer.)
     */
    private static final int MIN_SATELLITES = 12;
    private static final int MAX_SATELLITES = 24;
    private static final double SATELLITE_DISTANCE = 85;
    private static final double SATELLITE_SPREAD_Y = 42;

    /** Largest stretch of a rock's radius along one axis. */
    private static final double MAX_STRETCH = 1.25;
    /** Largest bulge of the surface: 1 + the two noise layers' amplitudes (0.25 and 0.10). */
    private static final double MAX_BULGE = 1.35;
    /** Blocks past a rock the work of a chunk reaches: the gate of a temple and its opening stand at the surface. */
    private static final int MARGIN = 4;
    /** The share of the islands, of the large and of the medium satellites that hold a temple (if one fits). */
    private static final double ISLAND_TEMPLE_CHANCE = 1.0;
    /**
     * Blocks from a cluster's centre that any of its rocks can reach: the island's radius, a satellite at the end of
     * its range and its own radius, each with the stretch and the bulge. (Plus a margin.)
     */
    private static final int CLUSTER_REACH = (int) Math.ceil(
            (ISLAND_MAX_R * 1.2 + SATELLITE_DISTANCE + 28 * MAX_STRETCH) * MAX_BULGE) + 8;

    /**
     * Above this value of the pocket noise the rock is Oil Regolith: about 7 % of it, in separate deposits of some hundreds of
     * blocks. (It was 0.38 at a scale of 0.07: a fifth of the rock, and the pockets ran into each other.)
     */
    private static final double OIL_POCKET = 0.65;
    /** The pocket noise's scale: its features are about 1 / this many blocks wide. */
    private static final double OIL_POCKET_SCALE = 0.05;

    /** A class of satellite: radii, its share of the satellites (relative) and the share of it that holds a temple. */
    private record Satellite(int minR, int maxR, int weight, double templeChance) {}

    private static final Satellite[] SATELLITES = {
            new Satellite(2, 4, 28, 0.0),     // pebbles
            new Satellite(4, 8, 40, 0.0),     // small
            new Satellite(9, 16, 24, 0.25),   // medium
            new Satellite(18, 28, 8, 0.6),    // large
    };

    private static final Direction[] ENTRANCES = { Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST };
    private static final int SATELLITE_WEIGHT = 28 + 40 + 24 + 8;

    /** The slow noise that lifts and sinks regions: made once per world seed. */
    private static volatile Drift drift;

    private record Drift(long seed, SimplexNoise noise) {}

    private static SimplexNoise driftNoise(long seed) {
        Drift current = drift;
        if (current == null || current.seed != seed) {
            current = new Drift(seed, new SimplexNoise(new XoroshiroRandomSource(seed ^ 0x5DEECE66DL)));
            drift = current;
        }
        return current.noise;
    }

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
        int cellMinX = Mth.floorDiv(chunkMinX - CLUSTER_REACH, CELL);
        int cellMaxX = Mth.floorDiv(chunkMinX + 15 + CLUSTER_REACH, CELL);
        int cellMinZ = Mth.floorDiv(chunkMinZ - CLUSTER_REACH, CELL);
        int cellMaxZ = Mth.floorDiv(chunkMinZ + 15 + CLUSTER_REACH, CELL);
        boolean placed = false;
        for (int cellX = cellMinX; cellX <= cellMaxX; cellX++) {
            for (int cellZ = cellMinZ; cellZ <= cellMaxZ; cellZ++) {
                placed |= fillCluster(level, seed, cellX, cellZ, chunkMinX, chunkMinZ);
            }
        }
        return placed;
    }

    /** The cluster of one cell (if it has one), as far as its rocks reach into the chunk. */
    private static boolean fillCluster(WorldGenLevel level, long seed, int cellX, int cellZ, int chunkMinX,
                                       int chunkMinZ) {
        RandomSource random = new XoroshiroRandomSource(cellSeed(seed, cellX, cellZ));
        if (random.nextDouble() >= CLUSTER_CHANCE) return false;
        double centerX = cellX * (double) CELL + random.nextInt(CELL) + 0.5;
        double centerZ = cellZ * (double) CELL + random.nextInt(CELL) + 0.5;
        // the band's middle, lifted or sunk by the region and by this cluster; the satellites need room both ways
        double middle = (CENTER_MIN_Y + CENTER_MAX_Y) / 2.0;
        double lift = driftNoise(seed).getValue(centerX * DRIFT_SCALE, centerZ * DRIFT_SCALE) * DRIFT +
                (random.nextDouble() * 2.0 - 1.0) * LIFT;
        double centerY = Mth.clamp(middle + lift, CENTER_MIN_Y + SATELLITE_SPREAD_Y,
                CENTER_MAX_Y - SATELLITE_SPREAD_Y);

        boolean placed = false;
        // the island
        double islandR = ISLAND_MIN_R + random.nextDouble() * (ISLAND_MAX_R - ISLAND_MIN_R);
        double islandX = islandR * (0.8 + random.nextDouble() * 0.4);
        double islandZ = islandR * (0.8 + random.nextDouble() * 0.4);
        double islandY = islandR * (ISLAND_FLAT_MIN + random.nextDouble() * (ISLAND_FLAT_MAX - ISLAND_FLAT_MIN));
        long islandNoise = random.nextLong();
        if (reaches(centerX, centerZ, islandX, islandZ, chunkMinX, chunkMinZ)) {
            placed |= fill(level, new Rock(islandNoise, centerX, centerY, centerZ, islandX, islandY, islandZ),
                    ISLAND_TEMPLE_CHANCE, chunkMinX, chunkMinZ);
        }

        // the satellites: always draw every parameter of every one, so the next does not depend on whether this one is in range
        int count = MIN_SATELLITES + random.nextInt(MAX_SATELLITES - MIN_SATELLITES + 1);
        for (int i = 0; i < count; i++) {
            Satellite kind = SATELLITES[0];
            int roll = random.nextInt(SATELLITE_WEIGHT);
            for (Satellite candidate : SATELLITES) {
                if (roll < candidate.weight) {
                    kind = candidate;
                    break;
                }
                roll -= candidate.weight;
            }
            double angle = random.nextDouble() * Math.PI * 2;
            double distance = islandR * 1.05 + random.nextDouble() * SATELLITE_DISTANCE;
            double dy = (random.nextDouble() * 2.0 - 1.0) * SATELLITE_SPREAD_Y;
            double size = Math.pow(random.nextDouble(), 1.6);
            double radius = kind.minR + size * (kind.maxR - kind.minR);
            double radiusX = radius * (0.75 + random.nextDouble() * 0.5);
            double radiusY = radius * (0.60 + random.nextDouble() * 0.5);
            double radiusZ = radius * (0.75 + random.nextDouble() * 0.5);
            long noiseSeed = random.nextLong();
            double x = centerX + Math.cos(angle) * distance;
            double z = centerZ + Math.sin(angle) * distance;
            double y = Mth.clamp(centerY + dy, CENTER_MIN_Y, CENTER_MAX_Y);
            if (!reaches(x, z, radiusX, radiusZ, chunkMinX, chunkMinZ)) continue;
            placed |= fill(level, new Rock(noiseSeed, x, y, z, radiusX, radiusY, radiusZ), kind.templeChance,
                    chunkMinX, chunkMinZ);
        }
        return placed;
    }

    private static boolean reaches(double centerX, double centerZ, double radiusX, double radiusZ, int chunkMinX,
                                   int chunkMinZ) {
        double reachX = radiusX * MAX_BULGE + MARGIN;
        double reachZ = radiusZ * MAX_BULGE + MARGIN;
        return centerX + reachX >= chunkMinX && centerX - reachX < chunkMinX + 16 &&
                centerZ + reachZ >= chunkMinZ && centerZ - reachZ < chunkMinZ + 16;
    }

    /** One rock: its shape (an ellipsoid with a lumpy surface) and its stone (with pockets of Oil Regolith). */
    private static final class Rock {

        final long noiseSeed;
        final double centerX, centerY, centerZ, radiusX, radiusY, radiusZ;
        private final SimplexNoise shape, detail, stone, pocket;
        private final BlockState regolith;
        private final double shapeScale, detailScale;

        Rock(long noiseSeed, double centerX, double centerY, double centerZ, double radiusX, double radiusY,
             double radiusZ) {
            this.noiseSeed = noiseSeed;
            this.centerX = centerX;
            this.centerY = centerY;
            this.centerZ = centerZ;
            this.radiusX = radiusX;
            this.radiusY = radiusY;
            this.radiusZ = radiusZ;
            // the order of the noises is the order the rocks were made in: it fixes their shape and their stone
            RandomSource random = new XoroshiroRandomSource(noiseSeed);
            this.shape = new SimplexNoise(random);
            this.detail = new SimplexNoise(random);
            this.stone = new SimplexNoise(random);
            this.pocket = new SimplexNoise(random);
            this.regolith = AF9Space.OIL_REGOLITH.get().defaultBlockState();
            double radius = Math.max(radiusX, Math.max(radiusY, radiusZ));
            this.shapeScale = 1.0 / Math.max(6.0, radius * 0.9);
            this.detailScale = 1.0 / Math.max(3.0, radius * 0.3);
        }

        boolean solid(int x, int y, int z) {
            double dx = (x + 0.5 - centerX) / radiusX;
            double dy = (y + 0.5 - centerY) / radiusY;
            double dz = (z + 0.5 - centerZ) / radiusZ;
            double distanceSquared = dx * dx + dy * dy + dz * dz;
            if (distanceSquared > MAX_BULGE * MAX_BULGE) return false;
            double bulge = 1.0 + 0.25 * shape.getValue(x * shapeScale, y * shapeScale, z * shapeScale) +
                    0.10 * detail.getValue(x * detailScale, y * detailScale, z * detailScale);
            return distanceSquared < bulge * bulge;
        }

        BlockState stoneAt(int x, int y, int z) {
            boolean oily = pocket.getValue(x * OIL_POCKET_SCALE, y * OIL_POCKET_SCALE, z * OIL_POCKET_SCALE) > OIL_POCKET;
            return oily ? regolith : rockAt(stone.getValue(x * 0.09, y * 0.09, z * 0.09));
        }
    }

    /** Sets the blocks of one rock (and its temple) inside the chunk. */
    private static boolean fill(WorldGenLevel level, Rock rock, double templeChance, int chunkMinX, int chunkMinZ) {
        // whether the rock holds a temple, and which way it faces, come from the rock's own seed: a chunk and its neighbour agree
        TempleLayout temple = null;
        if (templeChance > 0) {
            RandomSource templeRandom = new XoroshiroRandomSource(rock.noiseSeed ^ 0x2545F4914F6CDD1DL);
            double roll = templeRandom.nextDouble();
            int turns = templeRandom.nextInt(4);
            // the corridor of a temple may run out to the far side of the rock
            int maxLength = (int) Math.ceil(Math.max(rock.radiusX, rock.radiusZ) * MAX_BULGE) + 2;
            temple = TempleLayout.create(templeChance, roll, turns, rock.centerX, rock.centerY, rock.centerZ,
                    rock.radiusX, rock.radiusY, rock.radiusZ, maxLength, rock.noiseSeed, rock::solid);
        }

        int minX = Math.max(chunkMinX, Mth.floor(rock.centerX - rock.radiusX * MAX_BULGE) - MARGIN);
        int maxX = Math.min(chunkMinX + 15, Mth.ceil(rock.centerX + rock.radiusX * MAX_BULGE) + MARGIN);
        int minZ = Math.max(chunkMinZ, Mth.floor(rock.centerZ - rock.radiusZ * MAX_BULGE) - MARGIN);
        int maxZ = Math.min(chunkMinZ + 15, Mth.ceil(rock.centerZ + rock.radiusZ * MAX_BULGE) + MARGIN);
        int minY = Math.max(level.getMinBuildHeight(), Mth.floor(rock.centerY - rock.radiusY * MAX_BULGE) - MARGIN);
        int maxY = Math.min(level.getMaxBuildHeight() - 1, Mth.ceil(rock.centerY + rock.radiusY * MAX_BULGE) + MARGIN);
        boolean placed = false;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                for (int y = minY; y <= maxY; y++) {
                    TempleLayout.Part part = temple == null ? TempleLayout.Part.KEEP : temple.at(x, y, z);
                    boolean solid = rock.solid(x, y, z);
                    if (part == TempleLayout.Part.KEEP) {
                        if (!solid) continue;
                        pos.set(x, y, z);
                        level.setBlock(pos, rock.stoneAt(x, y, z), 2);
                        placed = true;
                        continue;
                    }
                    if (part == TempleLayout.Part.AIR) {
                        // carving air out of the void is nothing
                        if (!solid) continue;
                        pos.set(x, y, z);
                        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
                        placed = true;
                        continue;
                    }
                    pos.set(x, y, z);
                    if (part == TempleLayout.Part.CHEST) {
                        placeChest(level, pos, temple, rock.noiseSeed);
                    } else {
                        level.setBlock(pos, blockOf(part), 2);
                    }
                    placed = true;
                }
            }
        }
        return placed;
    }

    /** A chest facing the temple's entrance, with the loot of its size. */
    private static void placeChest(WorldGenLevel level, BlockPos pos, TempleLayout temple, long seed) {
        Direction facing = ENTRANCES[temple.entranceDirection()];
        level.setBlock(pos, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, facing), 2);
        RandomSource random = new XoroshiroRandomSource(seed ^ pos.asLong());
        RandomizableContainerBlockEntity.setLootTable(level, random, pos,
                new ResourceLocation("af9", "chests/" + temple.kind.lootTable));
    }

    /** The block of a part of the temple: black brick, purpur pillars, crying obsidian altars, end rods for light. */
    private static BlockState blockOf(TempleLayout.Part part) {
        return switch (part) {
            case WALL -> Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState();
            case WALL_CRACKED -> Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS.defaultBlockState();
            case WALL_CHISELED -> Blocks.CHISELED_POLISHED_BLACKSTONE.defaultBlockState();
            case FLOOR -> Blocks.POLISHED_BLACKSTONE.defaultBlockState();
            case FLOOR_GILDED -> Blocks.GILDED_BLACKSTONE.defaultBlockState();
            case PILLAR -> Blocks.PURPUR_PILLAR.defaultBlockState();
            case ALTAR -> Blocks.CRYING_OBSIDIAN.defaultBlockState();
            case LIGHT -> Blocks.END_ROD.defaultBlockState();
            default -> Blocks.AIR.defaultBlockState();
        };
    }

    /** The stone of a patch: darker blackstone and basalt, lighter andesite and tuff. */
    private static BlockState rockAt(double noise) {
        if (noise < -0.35) return Blocks.BLACKSTONE.defaultBlockState();
        if (noise < 0.15) return Blocks.ANDESITE.defaultBlockState();
        if (noise < 0.55) return Blocks.TUFF.defaultBlockState();
        return Blocks.BASALT.defaultBlockState();
    }

    /** A well-mixed seed of one cell (the finalizer of MurmurHash3 over the world seed and the cell). */
    private static long cellSeed(long seed, int cellX, int cellZ) {
        long hash = seed ^ 0x9E3779B97F4A7C15L;
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
