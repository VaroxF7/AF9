package com.af9.core.space;

/**
 * An ancient temple inside an asteroid: a hall with an altar, behind a corridor that leads out through the rock to a gate
 * on the surface. Pure geometry, no Minecraft classes ({@link AsteroidFieldFeature} turns a {@link Part} into a block):
 * the layout answers "what is at this world position", so every chunk can ask for its own blocks and the temple comes out
 * whole in any order of chunk generation.
 * <p>
 * Local coordinates: x across, z along the hall (the entrance side at -z, the altar at +z), y up with the floor layer at
 * 0, the hall's air at 1..ih and the ceiling at ih + 1. The hall is {@code 2 * ix + 1} wide and {@code 2 * iz + 1} long
 * inside, its walls one block thick. The corridor runs on from the middle of the front wall along -z, {@code 2 * cw + 1}
 * wide and {@code ch} high, lined with brick; where the rock ends (found by walking along its axis through the asteroid's
 * own shape) the gate stands: two pillars, a lintel and two lights. The whole thing is turned by {@code rotation} quarter
 * turns around its middle.
 * <p>
 * Three sizes, the biggest that fits inside the asteroid ({@link #fits}): a shrine (one chest), a temple (an altar with a
 * chest between two rows of pillars) and a grand temple (the same, longer, with two more chests in the back corners).
 */
public final class TempleLayout {

    /** What a position of the temple is. {@code KEEP}: not the temple's, leave what the asteroid has there. */
    public enum Part {
        KEEP, AIR, WALL, WALL_CRACKED, WALL_CHISELED, FLOOR, FLOOR_GILDED, PILLAR, ALTAR, LIGHT, CHEST
    }

    /**
     * A size: the hall's inside is {@code 2 * ix + 1} wide, {@code 2 * iz + 1} long and {@code ih} high; the corridor is
     * {@code 2 * cw + 1} wide and {@code ch} high.
     */
    public enum Kind {
        SHRINE("ancient_shrine", 1, 2, 3, 0, 2),
        TEMPLE("ancient_temple", 3, 5, 4, 1, 3),
        GRAND_TEMPLE("ancient_temple", 5, 8, 6, 2, 4);

        public final String lootTable;
        public final int ix, iz, ih, cw, ch;

        Kind(String lootTable, int ix, int iz, int ih, int cw, int ch) {
            this.lootTable = lootTable;
            this.ix = ix;
            this.iz = iz;
            this.ih = ih;
            this.cw = cw;
            this.ch = ch;
        }

        /** Pillars stand this far from the middle of the hall (none in a shrine). */
        int pillarX() {
            return ix >= 3 ? Math.max(2, ix - 2) : 0;
        }
    }

    /** Whether a position of the asteroid's own shape is rock (the layout walks through it to find the surface). */
    @FunctionalInterface
    public interface Solid {
        boolean at(int x, int y, int z);
    }

    /**
     * How much of the asteroid the temple may take: the corner of the hall's outer wall, as a share of the radii, squared
     * and added up, stays below this. 1 is the surface of an ellipsoid, the surface's lumps take up to a third of the
     * radius away, so 0.36 keeps the walls inside the rock nearly everywhere.
     */
    private static final double FIT = 0.36;
    /** Blocks past the last rock the gate's opening is carved. */
    private static final int OPENING = 3;

    public final Kind kind;
    public final int rotation;
    /** World position of the hall's middle and the world y of its floor layer. */
    public final int originX, originZ, floorY;
    /** Local z of the gate, the corridor's end (at most {@code -(iz + 2)}). */
    public final int exitZ;
    /** World bounds of everything the temple sets or carves. */
    public final int minX, maxX, minY, maxY, minZ, maxZ;
    private final int seed;

    private TempleLayout(Kind kind, int rotation, int originX, int originZ, int floorY, int exitZ, int seed) {
        this.kind = kind;
        this.rotation = rotation;
        this.originX = originX;
        this.originZ = originZ;
        this.floorY = floorY;
        this.exitZ = exitZ;
        this.seed = seed;
        int halfX = Math.max(kind.ix + 1, kind.cw + 1);
        int lowZ = exitZ - OPENING;
        int highZ = kind.iz + 1;
        int top = Math.max(kind.ih + 1, kind.ch + 3);
        int[] xs = new int[4];
        int[] zs = new int[4];
        int[][] corners = { { -halfX, lowZ }, { halfX, lowZ }, { -halfX, highZ }, { halfX, highZ } };
        for (int i = 0; i < 4; i++) {
            xs[i] = originX + worldX(rotation, corners[i][0], corners[i][1]);
            zs[i] = originZ + worldZ(rotation, corners[i][0], corners[i][1]);
        }
        this.minX = Math.min(Math.min(xs[0], xs[1]), Math.min(xs[2], xs[3]));
        this.maxX = Math.max(Math.max(xs[0], xs[1]), Math.max(xs[2], xs[3]));
        this.minZ = Math.min(Math.min(zs[0], zs[1]), Math.min(zs[2], zs[3]));
        this.maxZ = Math.max(Math.max(zs[0], zs[1]), Math.max(zs[2], zs[3]));
        this.minY = floorY;
        this.maxY = floorY + top;
    }

    /** Whether a hall of this size, with a shell, fits inside an ellipsoid with these radii. */
    public static boolean fits(Kind kind, double radiusX, double radiusY, double radiusZ) {
        double x = (kind.ix + 1.5) / radiusX;
        double z = (kind.iz + 1.5) / radiusZ;
        double y = ((kind.ih + 2) / 2.0) / radiusY;
        return x * x + y * y + z * z <= FIT;
    }

    /**
     * The temple of an asteroid, or null for none: with the chance {@code chance} ({@code roll}, a random number in 0..1,
     * decides) and only if the smallest size fits. The biggest size that fits is built, its middle at the asteroid's.
     *
     * @param maxLength how far the corridor may run at most (beyond the asteroid's largest reach)
     * @param solid     the asteroid's own shape
     */
    public static TempleLayout create(double chance, double roll, int rotation, double centerX, double centerY,
                                      double centerZ, double radiusX, double radiusY, double radiusZ, int maxLength,
                                      long hashSeed, Solid solid) {
        if (roll >= chance) return null;
        Kind kind = null;
        for (Kind candidate : new Kind[] { Kind.GRAND_TEMPLE, Kind.TEMPLE, Kind.SHRINE }) {
            if (fits(candidate, radiusX, radiusY, radiusZ)) {
                kind = candidate;
                break;
            }
        }
        if (kind == null) return null;
        int originX = (int) Math.floor(centerX);
        int originZ = (int) Math.floor(centerZ);
        int floorY = (int) Math.floor(centerY) - (kind.ih + 2) / 2;
        // walk out along the corridor's axis, at the height of its middle, to the last rock (a gap of up to two blocks
        // is a lump in the surface, not its end)
        int start = -(kind.iz + 2);
        int last = start;
        int gap = 0;
        for (int z = start; z >= start - maxLength && gap < 3; z--) {
            int x = originX + worldX(rotation, 0, z);
            int zz = originZ + worldZ(rotation, 0, z);
            if (solid.at(x, floorY + 2, zz)) {
                last = z;
                gap = 0;
            } else {
                gap++;
            }
        }
        return new TempleLayout(kind, rotation & 3, originX, originZ, floorY, last, (int) (hashSeed ^ (hashSeed >>> 32)));
    }

    /** The direction of local -z (where the corridor leaves) as an index into {north, east, south, west}. */
    public int entranceDirection() {
        return rotation;
    }

    /** What this world position is. */
    public Part at(int x, int y, int z) {
        if (x < minX || x > maxX || y < minY || y > maxY || z < minZ || z > maxZ) return Part.KEEP;
        int dx = x - originX;
        int dz = z - originZ;
        int lx;
        int lz;
        switch (rotation) {
            case 0 -> {
                lx = dx;
                lz = dz;
            }
            case 1 -> {
                lx = dz;
                lz = -dx;
            }
            case 2 -> {
                lx = -dx;
                lz = -dz;
            }
            default -> {
                lx = -dz;
                lz = dx;
            }
        }
        return local(lx, y - floorY, lz);
    }

    /** What the position at local coordinates is. */
    public Part local(int x, int y, int z) {
        int ix = kind.ix;
        int iz = kind.iz;
        int ih = kind.ih;
        int ax = Math.abs(x);
        if (z >= -(iz + 1) && z <= iz + 1 && ax <= ix + 1 && y >= 0 && y <= ih + 1) return hall(x, y, z, ax);
        if (z < -(iz + 1) && z >= exitZ - OPENING) return corridor(x, y, z, ax);
        return Part.KEEP;
    }

    private Part hall(int x, int y, int z, int ax) {
        int ix = kind.ix;
        int iz = kind.iz;
        int ih = kind.ih;
        int px = kind.pillarX();
        if (y == 0) {
            // the floor: a gilded stripe down the middle
            if (ax == 0 && Math.floorMod(z, 3) == 0 && px > 0) return Part.FLOOR_GILDED;
            if (kind == Kind.SHRINE && x == 0 && z == iz) return Part.ALTAR;
            return Part.FLOOR;
        }
        if (y == ih + 1) {
            // the ceiling: a chiselled beam over each row of pillars
            if (px > 0 && ax == px) return Part.WALL_CHISELED;
            return wall(x, y, z);
        }
        boolean side = ax == ix + 1;
        boolean end = z == iz + 1 || z == -(iz + 1);
        if (side || end) {
            // the opening of the front wall
            if (z == -(iz + 1) && ax <= kind.cw && y <= kind.ch) return Part.AIR;
            // a band of carvings on the back wall
            if (z == iz + 1 && px > 0 && (y == 2 || y == ih - 1) && ax % 2 == 0 && ax <= ix) return Part.WALL_CHISELED;
            return wall(x, y, z);
        }
        // inside: |x| <= ix, |z| <= iz, 1 <= y <= ih
        if (kind == Kind.SHRINE) {
            if (z == iz && x == 0 && y == 1) return Part.CHEST;
            if (z == iz && ax == 1 && y == 1) return Part.LIGHT;
            return Part.AIR;
        }
        // the altar: a platform two blocks deep and five wide, a pedestal in the middle, the chest on it, a light each side
        if (z >= iz - 2 && ax <= 2) {
            if (y == 1) return (x == 0 && z == iz) ? Part.ALTAR : Part.FLOOR;
            if (y == 2 && z == iz) {
                if (x == 0) return Part.CHEST;
                if (ax == 2) return Part.LIGHT;
            }
        }
        // two chests in the back corners of the grand temple
        if (kind == Kind.GRAND_TEMPLE && z == iz && ax == ix && y == 1) return Part.CHEST;
        // the rows of pillars, a light at the foot of each on the nave's side
        if (z >= -iz + 2 && z <= iz - 4 && Math.floorMod(z + iz - 2, 3) == 0) {
            if (ax == px) return y == ih ? Part.WALL_CHISELED : Part.PILLAR;
            if (ax == px - 1 && y == 1) return Part.LIGHT;
        }
        return Part.AIR;
    }

    private Part corridor(int x, int y, int z, int ax) {
        int cw = kind.cw;
        int ch = kind.ch;
        // the opening goes on past the gate, so that no lump of rock closes it
        if (z < exitZ) return (ax <= cw && y >= 1 && y <= ch) ? Part.AIR : Part.KEEP;
        if (z == exitZ) {
            // the gate: a pillar each side, a chiselled lintel over the opening, a light on each end of it
            if (ax <= cw) {
                if (y == 0) return Part.FLOOR;
                if (y <= ch) return Part.AIR;
                if (y == ch + 1) return Part.WALL_CHISELED;
                return Part.KEEP;
            }
            if (ax == cw + 1) {
                if (y == 0) return Part.FLOOR;
                if (y <= ch) return Part.PILLAR;
                if (y == ch + 1) return Part.WALL_CHISELED;
                if (y == ch + 2) return Part.LIGHT;
            }
            return Part.KEEP;
        }
        if (ax > cw + 1 || y > ch + 1) return Part.KEEP;
        if (ax <= cw && y >= 1 && y <= ch) return Part.AIR;
        if (y == 0) return Part.FLOOR;
        return wall(x, y, z);
    }

    /** Brick, now and then cracked: by a hash of the position, so a chunk and its neighbour agree. */
    private Part wall(int x, int y, int z) {
        return (hash(x, y, z) & 7) == 0 ? Part.WALL_CRACKED : Part.WALL;
    }

    private int hash(int x, int y, int z) {
        int h = x * 73856093 ^ y * 19349663 ^ z * 83492791 ^ seed;
        h ^= h >>> 13;
        h *= 0x5bd1e995;
        h ^= h >>> 15;
        return h & 0x7fffffff;
    }

    /** The world x offset of local (x, z) after {@code rotation} quarter turns. */
    private static int worldX(int rotation, int x, int z) {
        return switch (rotation & 3) {
            case 0 -> x;
            case 1 -> -z;
            case 2 -> -x;
            default -> z;
        };
    }

    /** The world z offset of local (x, z) after {@code rotation} quarter turns. */
    private static int worldZ(int rotation, int x, int z) {
        return switch (rotation & 3) {
            case 0 -> z;
            case 1 -> x;
            case 2 -> -z;
            default -> -x;
        };
    }
}
