package com.af9.core.space;

import com.af9.core.AF9Core;

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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The asteroids of the Asteroid Field (af9:asteroid_field), a void dimension: <b>clusters</b>, each a large island with
 * a swarm of smaller rocks around it at every height of a range of about 85 blocks, and empty space between the
 * clusters (the nearest island is about 100 blocks from an island's edge; about 5 blocks of rock in a column of the whole
 * 300-block height).
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
 * from pebbles to 28 blocks of radius, around the island at a distance of its edge to 85 blocks beyond, anywhere in
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
 * runs out to a gate on the surface, in front of the gate a forecourt with pillars and two glowing towers stands out into
 * the void (the sign of a temple from far away), and the chests (loot tables {@code af9:chests/ancient_shrine} and
 * {@code af9:chests/ancient_temple}) stand at the altar. The temple's blocks are not stones of the ore layer, so no ore
 * vein grows into the walls. Whether a rock holds one is a function of the rock's own seed, so it does not change which
 * rocks there are or where. A chunk sets the stone of all the rocks that reach it first and the temples after, so a
 * rock that overlaps a temple's rock never closes its corridor.
 */
public class AsteroidFieldFeature extends Feature<NoneFeatureConfiguration> {

    /** Lowest and highest y a rock's centre can have. */
    public static final int CENTER_MIN_Y = 5;
    public static final int CENTER_MAX_Y = 270;
    /** Cells of clusters (blocks), the chance of a cluster in a cell. */
    private static final int CELL = 300;
    private static final double CLUSTER_CHANCE = 0.65;
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
    /**
     * Blocks past a rock the work of a chunk reaches: a temple's forecourt stands out in front of its gate (up to 10 blocks
     * past the surface, 5 to each side).
     */
    private static final int MARGIN = 14;
    /**
     * The temples sit on a grid like the End cities' (minecraft:end_city: spacing 20, separation 11 chunks): every square of
     * {@code TEMPLE_SPACING} x {@code TEMPLE_SPACING} chunks has one candidate point at random in its first
     * {@code TEMPLE_SPACING - TEMPLE_SEPARATION} chunks, so two candidates are at least {@code TEMPLE_SEPARATION} chunks apart.
     * The temple goes into the cluster whose centre is nearest to the point (within {@link #TEMPLE_RANGE} blocks; none if there is
     * no cluster), and a cluster holds at most one: in its island (a grand temple) {@link #ISLAND_HOST_SHARE} of the time, else
     * in its biggest satellite that a temple fits (a temple or a shrine).
     */
    private static final int TEMPLE_SPACING = 20;
    private static final int TEMPLE_SEPARATION = 11;
    private static final double TEMPLE_RANGE = 192;
    /** The share of the clusters whose temple is in the island (the rest: in the biggest satellite that holds one). */
    private static final double ISLAND_HOST_SHARE = 0.7;
    private static final long TEMPLE_SALT = 0x3C6EF372FE94F82AL;
    /**
     * Blocks from a cluster's centre that any of its rocks can reach: the island's radius, a satellite at the end of
     * its range and its own radius, each with the stretch and the bulge. (Plus a margin.)
     */
    private static final int CLUSTER_REACH = (int) Math.ceil(
            (ISLAND_MAX_R * 1.2 + SATELLITE_DISTANCE + 28 * MAX_STRETCH) * MAX_BULGE) + MARGIN + 4;

    /**
     * Above this value of the pocket noise the rock is Oil Regolith: about 7 % of it, in separate deposits of some hundreds of
     * blocks. (It was 0.38 at a scale of 0.07: a fifth of the rock, and the pockets ran into each other.)
     */
    private static final double OIL_POCKET = 0.65;
    /** The pocket noise's scale: its features are about 1 / this many blocks wide. */
    private static final double OIL_POCKET_SCALE = 0.05;

    /** A class of satellite: radii, its share of the satellites (relative) and its rank as a host of a temple (0 never). */
    private record Satellite(int minR, int maxR, int weight, int rank) {}

    private static final Satellite[] SATELLITES = {
            new Satellite(2, 4, 28, 0),     // pebbles
            new Satellite(4, 8, 40, 0),     // small
            new Satellite(9, 16, 24, 1),    // medium
            new Satellite(18, 28, 8, 2),    // large
    };
    /** The island's rank as a host of a temple (a satellite's is 2 for the large ones, 1 for the medium ones, 0 never). */
    private static final int ISLAND_RANK = 3;

    /** A rock before it is made: where it is, how big, its noise seed, its rank as a host of a temple. */
    private record Spec(long noiseSeed, double x, double y, double z, double radiusX, double radiusY, double radiusZ,
                        int rank) {

        /** Whether a temple may go into it: it ranks, and the smallest one fits. */
        boolean hostsTemple() {
            return rank > 0 && TempleLayout.fits(TempleLayout.Kind.SHRINE, radiusX, radiusY, radiusZ);
        }
    }

    /** A cluster: its centre, its rocks (the island first) and the rock that would hold its temple. */
    private record Cluster(double x, double z, List<Spec> specs, Spec host) {}

    /** The square of a grid of the temples, in a world. */
    private record Region(long seed, int x, int z) {}

    private static final long NO_ROCK = Long.MIN_VALUE;
    /** The rock that holds the temple of a region (its noise seed, or {@link #NO_ROCK}): a pure function, kept for speed. */
    private static final Map<Region, Long> TEMPLE_ROCKS = new ConcurrentHashMap<>();
    private static final AtomicBoolean LOGGED = new AtomicBoolean();

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
        if (LOGGED.compareAndSet(false, true)) {
            AF9Core.LOGGER.info("AF9 Asteroid Field: a cluster in {} % of the cells of {} blocks, temples on a grid of {} "
                    + "chunks (separation {}, within {} blocks of the point), oil regolith above noise {}",
                    (int) (CLUSTER_CHANCE * 100), CELL, TEMPLE_SPACING, TEMPLE_SEPARATION, (int) TEMPLE_RANGE, OIL_POCKET);
        }
        int cellMinX = Mth.floorDiv(chunkMinX - CLUSTER_REACH, CELL);
        int cellMaxX = Mth.floorDiv(chunkMinX + 15 + CLUSTER_REACH, CELL);
        int cellMinZ = Mth.floorDiv(chunkMinZ - CLUSTER_REACH, CELL);
        int cellMaxZ = Mth.floorDiv(chunkMinZ + 15 + CLUSTER_REACH, CELL);
        List<Rock> rocks = new ArrayList<>();
        for (int cellX = cellMinX; cellX <= cellMaxX; cellX++) {
            for (int cellZ = cellMinZ; cellZ <= cellMaxZ; cellZ++) {
                collectCluster(rocks, seed, cellX, cellZ, chunkMinX, chunkMinZ);
            }
        }
        boolean placed = false;
        // the stone of every rock first, the temples over all of it: a rock that overlaps a temple's rock does not close
        // its corridor or its forecourt, whatever order they are drawn in
        for (Rock rock : rocks) {
            placed |= fillStone(level, rock, chunkMinX, chunkMinZ);
        }
        for (Rock rock : rocks) {
            if (rock.temple != null) placed |= buildTemple(level, rock, chunkMinX, chunkMinZ);
        }
        return placed;
    }

    /** The cluster of one cell (if it has one): the rocks of it that reach into the chunk, each with its temple if it has one. */
    private static void collectCluster(List<Rock> rocks, long seed, int cellX, int cellZ, int chunkMinX,
                                       int chunkMinZ) {
        Cluster cluster = clusterOf(seed, cellX, cellZ);
        if (cluster == null) return;
        for (Spec spec : cluster.specs) {
            if (reaches(spec.x, spec.z, spec.radiusX, spec.radiusZ, chunkMinX, chunkMinZ)) {
                rocks.add(new Rock(spec, spec == cluster.host && clusterHasTemple(seed, cluster)));
            }
        }
    }

    /**
     * The cluster of one cell, or null: all of it from the world seed and the cell, a pure function (every parameter of every
     * satellite is drawn, so the next does not depend on the one before).
     */
    private static Cluster clusterOf(long seed, int cellX, int cellZ) {
        RandomSource random = new XoroshiroRandomSource(cellSeed(seed, cellX, cellZ));
        if (random.nextDouble() >= CLUSTER_CHANCE) return null;
        double centerX = cellX * (double) CELL + random.nextInt(CELL) + 0.5;
        double centerZ = cellZ * (double) CELL + random.nextInt(CELL) + 0.5;
        // the band's middle, lifted or sunk by the region and by this cluster; the satellites need room both ways
        double middle = (CENTER_MIN_Y + CENTER_MAX_Y) / 2.0;
        double lift = driftNoise(seed).getValue(centerX * DRIFT_SCALE, centerZ * DRIFT_SCALE) * DRIFT +
                (random.nextDouble() * 2.0 - 1.0) * LIFT;
        double centerY = Mth.clamp(middle + lift, CENTER_MIN_Y + SATELLITE_SPREAD_Y,
                CENTER_MAX_Y - SATELLITE_SPREAD_Y);

        List<Spec> specs = new ArrayList<>();
        // the island
        double islandR = ISLAND_MIN_R + random.nextDouble() * (ISLAND_MAX_R - ISLAND_MIN_R);
        double islandX = islandR * (0.8 + random.nextDouble() * 0.4);
        double islandZ = islandR * (0.8 + random.nextDouble() * 0.4);
        double islandY = islandR * (ISLAND_FLAT_MIN + random.nextDouble() * (ISLAND_FLAT_MAX - ISLAND_FLAT_MIN));
        long islandNoise = random.nextLong();
        specs.add(new Spec(islandNoise, centerX, centerY, centerZ, islandX, islandY, islandZ, ISLAND_RANK));

        // the satellites
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
            specs.add(new Spec(noiseSeed, x, y, z, radiusX, radiusY, radiusZ, kind.rank));
        }

        // the rock that would hold the cluster's temple: the island, or the biggest satellite that one fits
        Spec host = specs.get(0);
        if (random.nextDouble() >= ISLAND_HOST_SHARE) {
            for (Spec spec : specs.subList(1, specs.size())) {
                if (spec.hostsTemple() && (host == specs.get(0) ||
                        Math.max(spec.radiusX, spec.radiusZ) > Math.max(host.radiusX, host.radiusZ))) {
                    host = spec;
                }
            }
        }
        return new Cluster(centerX, centerZ, specs, host);
    }

    /** Whether the cluster holds the temple of one of the grid's regions whose candidate point is within reach of its centre. */
    private static boolean clusterHasTemple(long seed, Cluster cluster) {
        int size = TEMPLE_SPACING * 16;
        // a candidate point lies in the first (spacing - separation) chunks of its region: at most this far from its corner
        int offset = (TEMPLE_SPACING - TEMPLE_SEPARATION) * 16;
        int fromX = Mth.floorDiv(Mth.floor(cluster.x - TEMPLE_RANGE) - offset, size);
        int toX = Mth.floorDiv(Mth.floor(cluster.x + TEMPLE_RANGE), size);
        int fromZ = Mth.floorDiv(Mth.floor(cluster.z - TEMPLE_RANGE) - offset, size);
        int toZ = Mth.floorDiv(Mth.floor(cluster.z + TEMPLE_RANGE), size);
        for (int regionX = fromX; regionX <= toX; regionX++) {
            for (int regionZ = fromZ; regionZ <= toZ; regionZ++) {
                if (templeRock(seed, regionX, regionZ) == cluster.host.noiseSeed) return true;
            }
        }
        return false;
    }

    private static long templeRock(long seed, int regionX, int regionZ) {
        Region key = new Region(seed, regionX, regionZ);
        Long known = TEMPLE_ROCKS.get(key);
        if (known != null) return known;
        long found = searchTempleRock(seed, regionX, regionZ);
        if (TEMPLE_ROCKS.size() > 20000) TEMPLE_ROCKS.clear();
        TEMPLE_ROCKS.put(key, found);
        return found;
    }

    /** The rock of the region's temple: the host of the cluster whose centre is nearest to the candidate point. */
    private static long searchTempleRock(long seed, int regionX, int regionZ) {
        RandomSource random = new XoroshiroRandomSource(cellSeed(seed ^ TEMPLE_SALT, regionX, regionZ));
        int spread = TEMPLE_SPACING - TEMPLE_SEPARATION;
        double pointX = (regionX * (double) TEMPLE_SPACING + random.nextInt(spread)) * 16 + 8;
        double pointZ = (regionZ * (double) TEMPLE_SPACING + random.nextInt(spread)) * 16 + 8;
        int range = (int) Math.ceil(TEMPLE_RANGE);
        Cluster best = null;
        double bestDistance = 0;
        for (int cellX = Mth.floorDiv(Mth.floor(pointX) - range, CELL); cellX <= Mth.floorDiv(Mth.floor(pointX) + range, CELL); cellX++) {
            for (int cellZ = Mth.floorDiv(Mth.floor(pointZ) - range, CELL); cellZ <= Mth.floorDiv(Mth.floor(pointZ) + range, CELL); cellZ++) {
                Cluster cluster = clusterOf(seed, cellX, cellZ);
                if (cluster == null) continue;
                double distance = Math.hypot(cluster.x - pointX, cluster.z - pointZ);
                if (distance <= TEMPLE_RANGE && (best == null || distance < bestDistance)) {
                    best = cluster;
                    bestDistance = distance;
                }
            }
        }
        return best == null ? NO_ROCK : best.host.noiseSeed;
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
        /** The temple inside the rock, or null. */
        final TempleLayout temple;
        private final SimplexNoise shape, detail, stone, pocket;
        private final BlockState regolith;
        private final double shapeScale, detailScale;

        Rock(Spec spec, boolean hasTemple) {
            this.noiseSeed = spec.noiseSeed;
            this.centerX = spec.x;
            this.centerY = spec.y;
            this.centerZ = spec.z;
            this.radiusX = spec.radiusX;
            this.radiusY = spec.radiusY;
            this.radiusZ = spec.radiusZ;
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
            this.temple = hasTemple ? templeOf() : null;
        }

        /**
         * Whether the rock holds a temple, and which way it faces, come from the rock's own seed: a chunk and its
         * neighbour agree.
         */
        private TempleLayout templeOf() {
            int turns = new XoroshiroRandomSource(noiseSeed ^ 0x2545F4914F6CDD1DL).nextInt(4);
            // the corridor of a temple may run out to the far side of the rock
            int maxLength = (int) Math.ceil(Math.max(radiusX, radiusZ) * MAX_BULGE) + 2;
            return TempleLayout.create(1.0, 0.0, turns, centerX, centerY, centerZ, radiusX, radiusY, radiusZ,
                    maxLength, noiseSeed, this::solid);
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

    /** Sets the stone of one rock inside the chunk. */
    private static boolean fillStone(WorldGenLevel level, Rock rock, int chunkMinX, int chunkMinZ) {
        int minX = Math.max(chunkMinX, Mth.floor(rock.centerX - rock.radiusX * MAX_BULGE) - 1);
        int maxX = Math.min(chunkMinX + 15, Mth.ceil(rock.centerX + rock.radiusX * MAX_BULGE) + 1);
        int minZ = Math.max(chunkMinZ, Mth.floor(rock.centerZ - rock.radiusZ * MAX_BULGE) - 1);
        int maxZ = Math.min(chunkMinZ + 15, Mth.ceil(rock.centerZ + rock.radiusZ * MAX_BULGE) + 1);
        int minY = Math.max(level.getMinBuildHeight(), Mth.floor(rock.centerY - rock.radiusY * MAX_BULGE) - 1);
        int maxY = Math.min(level.getMaxBuildHeight() - 1, Mth.ceil(rock.centerY + rock.radiusY * MAX_BULGE) + 1);
        boolean placed = false;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                for (int y = minY; y <= maxY; y++) {
                    if (!rock.solid(x, y, z)) continue;
                    pos.set(x, y, z);
                    level.setBlock(pos, rock.stoneAt(x, y, z), 2);
                    placed = true;
                }
            }
        }
        return placed;
    }

    /** Sets the blocks of a rock's temple inside the chunk: its walls and furnishings, and the air of its halls. */
    private static boolean buildTemple(WorldGenLevel level, Rock rock, int chunkMinX, int chunkMinZ) {
        TempleLayout temple = rock.temple;
        int minX = Math.max(chunkMinX, temple.minX);
        int maxX = Math.min(chunkMinX + 15, temple.maxX);
        int minZ = Math.max(chunkMinZ, temple.minZ);
        int maxZ = Math.min(chunkMinZ + 15, temple.maxZ);
        int minY = Math.max(level.getMinBuildHeight(), temple.minY);
        int maxY = Math.min(level.getMaxBuildHeight() - 1, temple.maxY);
        boolean placed = false;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                for (int y = minY; y <= maxY; y++) {
                    TempleLayout.Part part = temple.at(x, y, z);
                    if (part == TempleLayout.Part.KEEP) continue;
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
