package com.af9.lod.core;

import java.util.Arrays;

/**
 * Face-culling mesher for a LodGrid. Output is a flat int array, 2 ints per quad:
 *   a = x | y << 5 | z << 10 | face << 15 (x,y,z in 0..15, face 0..5 = -X +X -Y +Y -Z +Z)
 *   b = voxel id
 * This packing is what the GPU side unpacks into corners, so the vertex buffer stays small (8 bytes per face).
 * A face is emitted when its voxel is solid and the neighbour is air; the grid border counts as air unless a neighbour
 * grid is passed for that direction (same order as faces, null = air) so seams between grids are not drawn twice.
 */
public final class LodMesher {
    private int[] out = new int[256];
    private int len;

    public static final int[][] DIRS = {{-1, 0, 0}, {1, 0, 0}, {0, -1, 0}, {0, 1, 0}, {0, 0, -1}, {0, 0, 1}};

    public int[] mesh(LodGrid g, LodGrid[] neighbours) {
        len = 0;
        int n = LodGrid.SIZE;
        for (int y = 0; y < n; y++) for (int z = 0; z < n; z++) for (int x = 0; x < n; x++) {
            int id = g.get(x, y, z);
            if (id == 0) continue;
            for (int f = 0; f < 6; f++) {
                int nx = x + DIRS[f][0], ny = y + DIRS[f][1], nz = z + DIRS[f][2];
                int other;
                if (nx < 0 || ny < 0 || nz < 0 || nx >= n || ny >= n || nz >= n) {
                    LodGrid nb = neighbours == null ? null : neighbours[f];
                    other = nb == null ? 0 : nb.get(Math.floorMod(nx, n), Math.floorMod(ny, n), Math.floorMod(nz, n));
                } else {
                    other = g.get(nx, ny, nz);
                }
                if (other == 0) add(x | y << 5 | z << 10 | f << 15, id);
            }
        }
        return Arrays.copyOf(out, len);
    }

    private void add(int a, int b) {
        if (len + 2 > out.length) out = Arrays.copyOf(out, out.length * 2);
        out[len++] = a;
        out[len++] = b;
    }

    public static int quadCount(int[] mesh) {
        return mesh.length / 2;
    }
}
