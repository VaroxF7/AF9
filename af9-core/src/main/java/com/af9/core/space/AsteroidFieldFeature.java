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
 * The asteroids of the Asteroid Field (af9:asteroid_field), a void dimension: lumpy rocks of five size classes, from
 * pebbles to rocks a hundred blocks across, and in the bigger ones the ruins of an ancient temple.
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
 * <p>
 * Temples ({@link TempleLayout}): an asteroid of the medium, large and huge classes holds a shrine, a temple or a grand
 * temple with the chance of its class, the biggest that fits inside it. The hall is carved out of the rock and lined with
 * polished blackstone brick, the corridor from it runs out to a gate on the surface, and the chests (loot tables
 * {@code af9:chests/ancient_shrine} and {@code af9:chests/ancient_temple}) stand at the altar. The temple's blocks are not
 * the stones of the ore layer, so no ore vein grows into the walls.
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
    /** Blocks past the rock the work of a chunk reaches: the gate of a temple and its opening stand at the surface. */
    private static final int MARGIN = 4;

    /**
     * A size class: cells of {@code cell} x {@code cell} blocks, {@code minCount}..{@code maxCount} asteroids in each,
     * radii {@code minR}..{@code maxR}; {@code spread} is how far the centre's height reaches from the middle of the
     * band (0 = all in the middle, 1 = the whole band); {@code templeChance} is the share of its asteroids that hold a
     * temple (when one fits).
     */
    private record SizeClass(int salt, int cell, int minCount, int maxCount, int minR, int maxR, double spread,
                             double templeChance) {

        /** Blocks around the centre of an asteroid its shape and its temple can reach. */
        int reach() {
            return (int) Math.ceil(maxR * MAX_EXTENT) + MARGIN + 1;
        }
    }

    // About a third of the band is rock before the big ones overlap. Pebbles, small, medium, large and huge rocks.
    private static final SizeClass[] CLASSES = {
            new SizeClass(1, 14, 1, 3, 2, 4, 1.0, 0.0),
            new SizeClass(2, 27, 1, 3, 4, 8, 1.0, 0.0),
            new SizeClass(3, 54, 1, 3, 9, 16, 1.0, 0.25),
            new SizeClass(4, 82, 1, 2, 18, 28, 0.8, 0.6),
            new SizeClass(5, 136, 1, 1, 32, 46, 0.5, 1.0),
    };

    private static final Direction[] ENTRANCES = { Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST };

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
            double templeRoll = random.nextDouble();
            int templeTurns = random.nextInt(4);
            if (!reaches(centerX, centerZ, radiusX, radiusZ, chunkMinX, chunkMinZ)) continue;
            placed |= fill(level, new Rock(noiseSeed, centerX, centerY, centerZ, radiusX, radiusY, radiusZ),
                    sizeClass.templeChance, templeRoll, templeTurns, chunkMinX, chunkMinZ);
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

    /** One asteroid: its shape (an ellipsoid with a lumpy surface) and its stone. */
    private static final class Rock {

        final long noiseSeed;
        final double centerX, centerY, centerZ, radiusX, radiusY, radiusZ;
        private final SimplexNoise shape, detail, stone;
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
            RandomSource random = new XoroshiroRandomSource(noiseSeed);
            this.shape = new SimplexNoise(random);
            this.detail = new SimplexNoise(random);
            this.stone = new SimplexNoise(random);
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
            return rockAt(stone.getValue(x * 0.09, y * 0.09, z * 0.09));
        }
    }

    /** Sets the blocks of one asteroid (and its temple) inside the chunk. */
    private static boolean fill(WorldGenLevel level, Rock rock, double templeChance, double templeRoll,
                                int templeTurns, int chunkMinX, int chunkMinZ) {
        // the corridor of a temple may run out to the far side of the rock
        int maxLength = (int) Math.ceil(Math.max(rock.radiusX, rock.radiusZ) * MAX_BULGE) + 2;
        TempleLayout temple = TempleLayout.create(templeChance, templeRoll, templeTurns, rock.centerX, rock.centerY,
                rock.centerZ, rock.radiusX, rock.radiusY, rock.radiusZ, maxLength, rock.noiseSeed, rock::solid);

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
