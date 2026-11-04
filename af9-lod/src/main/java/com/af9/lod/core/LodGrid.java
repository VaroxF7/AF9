package com.af9.lod.core;

/**
 * A 16x16x16 block of voxel ids (0 = air). At level L one voxel covers 2^L blocks per axis, so a grid covers
 * 16 * 2^L blocks. Pure data, no game types.
 */
public final class LodGrid {
    public static final int SIZE = 16;
    private final int[] ids = new int[SIZE * SIZE * SIZE];

    private static int idx(int x, int y, int z) {
        return (y * SIZE + z) * SIZE + x;
    }

    public int get(int x, int y, int z) {
        if (x < 0 || y < 0 || z < 0 || x >= SIZE || y >= SIZE || z >= SIZE) return 0;
        return ids[idx(x, y, z)];
    }

    public void set(int x, int y, int z, int id) {
        ids[idx(x, y, z)] = id;
    }

    public boolean isEmpty() {
        for (int v : ids) if (v != 0) return false;
        return true;
    }

    /**
     * Merges eight neighbouring grids (children, indexed cx + 2*cz + 4*cy, null = empty) into one grid of the next level.
     * Each output voxel takes the most common non-air id of its 2x2x2 source voxels; air only wins when all eight are air
     * (so thin things like leaves and water surfaces survive at distance).
     */
    public static LodGrid downsample(LodGrid[] children) {
        if (children.length != 8) throw new IllegalArgumentException("need 8 children");
        LodGrid out = new LodGrid();
        int[] ids = new int[8];
        for (int oy = 0; oy < SIZE; oy++) for (int oz = 0; oz < SIZE; oz++) for (int ox = 0; ox < SIZE; ox++) {
            int cx = ox >= SIZE / 2 ? 1 : 0, cz = oz >= SIZE / 2 ? 1 : 0, cy = oy >= SIZE / 2 ? 1 : 0;
            LodGrid child = children[cx + 2 * cz + 4 * cy];
            if (child == null) continue;
            int bx = (ox % (SIZE / 2)) * 2, by = (oy % (SIZE / 2)) * 2, bz = (oz % (SIZE / 2)) * 2;
            int n = 0;
            for (int dy = 0; dy < 2; dy++) for (int dz = 0; dz < 2; dz++) for (int dx = 0; dx < 2; dx++) {
                int v = child.get(bx + dx, by + dy, bz + dz);
                if (v != 0) ids[n++] = v;
            }
            out.set(ox, oy, oz, n == 0 ? 0 : mostCommon(ids, n));
        }
        return out;
    }

    private static int mostCommon(int[] a, int n) {
        int best = a[0], bestCount = 0;
        for (int i = 0; i < n; i++) {
            int c = 0;
            for (int j = 0; j < n; j++) if (a[j] == a[i]) c++;
            if (c > bestCount) { bestCount = c; best = a[i]; }
        }
        return best;
    }
}
