package com.af9.lod.core;

/**
 * Pure LOD math (no Minecraft or GL types). Level 0 is a 16-block section column, each level doubles the edge length.
 * Kept free of game types so it is unit-tested without the game.
 */
public final class LodLevel {
    public static final int MAX_LEVEL = 5;

    private LodLevel() {}

    /** Edge length in blocks of one cell at the given level (16 << level). */
    public static int cellSize(int level) {
        if (level < 0 || level > MAX_LEVEL) throw new IllegalArgumentException("level " + level);
        return 16 << level;
    }

    /**
     * Picks the level for a cell at distance (blocks): the smallest level whose cell is at most distance / ratio blocks wide,
     * clamped to [0, MAX_LEVEL]. A bigger ratio keeps detail further out.
     */
    public static int forDistance(double distance, double ratio) {
        if (ratio <= 0) throw new IllegalArgumentException("ratio");
        int level = 0;
        while (level < MAX_LEVEL && cellSize(level) * ratio < distance) level++;
        return level;
    }

    /** Cell index along one axis for a block coordinate at the given level (floor division, correct for negatives). */
    public static int cellIndex(int blockCoord, int level) {
        return Math.floorDiv(blockCoord, cellSize(level));
    }
}
