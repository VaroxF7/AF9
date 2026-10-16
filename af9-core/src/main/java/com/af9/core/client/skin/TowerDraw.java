package com.af9.core.client.skin;

import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * The white shell of the Orbital Array Mk2's tower, drawn in place of its blocks (the concept art's paneled tower): every
 * face of a hidden block that looks out gets a white panel in a dark seam, the coil strips glow, the frames stay dark.
 * Built once per change of the tower into a flat vertex list and replayed each frame, in the pattern's own space (cell
 * {@code (right, up, back)} is the unit cube at those coordinates).
 */
final class TowerDraw {

    static final byte PANEL = 0, LIGHT = 1, FRAME = 2;

    private static final int WHITE = 0xEEF1F5, SEAM = 0x474C57, FRAME_RGB = 0x353A44, GLOW = 0x8FD8FF, GLOW_SEAM = 0x2A6FA0;
    private static final float INSET = 0.06F, GLOW_INSET = 0.25F;
    private static final int SKY = LightTexture.pack(10, 15), FULL_BRIGHT = 0xF000F0;

    // neighbour offsets and face frames: normal, then the two edges of the face (u, v) from its origin corner
    private static final int[][] DIRS = { { -1, 0, 0 }, { 1, 0, 0 }, { 0, -1, 0 }, { 0, 1, 0 }, { 0, 0, -1 }, { 0, 0, 1 } };

    /** Per controller: the cells it was built from (sorted keys with their kinds) and the mesh. Client thread only. */
    private static final Map<Long, Mesh> MESHES = new HashMap<>();

    private TowerDraw() {}

    private static final class Mesh {

        long[] cells;
        float[] data = new float[1 << 16];
        int size;
        int quads;

        void quad(float[] p, float nx, float ny, float nz, int rgb, int light) {
            if (size + 4 * 10 > data.length) data = Arrays.copyOf(data, data.length * 2);
            float r = (rgb >> 16 & 255) / 255F, g = (rgb >> 8 & 255) / 255F, b = (rgb & 255) / 255F;
            for (int i = 0; i < 4; i++) {
                data[size++] = p[i * 3];
                data[size++] = p[i * 3 + 1];
                data[size++] = p[i * 3 + 2];
                data[size++] = nx;
                data[size++] = ny;
                data[size++] = nz;
                data[size++] = r;
                data[size++] = g;
                data[size++] = b;
                data[size++] = Float.intBitsToFloat(light);
            }
            quads++;
        }
    }

    static long key(int right, int up, int back) {
        return ((long) (right + 64) << 16) | ((long) (up + 64) << 8) | back;
    }

    static void forget(long controller) {
        MESHES.remove(controller);
    }

    static void clear() {
        MESHES.clear();
    }

    /**
     * Rebuilds the controller's mesh when its cells changed. {@code cells} maps a cell's key to its kind, {@code blocked}
     * holds the keys of visible blocks next to the tower (a face against one is not drawn).
     */
    static void update(long controller, Map<Long, Byte> cells, Set<Long> blocked) {
        long[] packed = new long[cells.size()];
        int n = 0;
        for (Map.Entry<Long, Byte> e : cells.entrySet()) packed[n++] = e.getKey() << 2 | e.getValue();
        Arrays.sort(packed);
        Mesh mesh = MESHES.get(controller);
        if (mesh != null && Arrays.equals(mesh.cells, packed)) return;
        mesh = new Mesh();
        mesh.cells = packed;
        boolean[] outside = outsideAir(cells, blocked);
        for (Map.Entry<Long, Byte> e : cells.entrySet()) {
            long k = e.getKey();
            int right = (int) (k >> 16 & 255) - 64, up = (int) (k >> 8 & 255) - 64, back = (int) (k & 255);
            for (int dir = 0; dir < 6; dir++) {
                // only the faces that look at the open air outside the shell (the hollow inside is never seen)
                if (!isOutside(outside, right + DIRS[dir][0], up + DIRS[dir][1], back + DIRS[dir][2])) continue;
                face(mesh, right, up, back, dir, e.getValue());
            }
        }
        MESHES.put(controller, mesh);
    }

    // the box searched for the open air: right and up from -BOX to BOX, back from -1 to DEPTH - 1
    private static final int BOX = 18, W = 2 * BOX + 1, DEPTH = 138;

    private static int index(int right, int up, int back) {
        return ((right + BOX) * W + (up + BOX)) * DEPTH + back + 1;
    }

    private static boolean isOutside(boolean[] outside, int right, int up, int back) {
        if (right < -BOX || right > BOX || up < -BOX || up > BOX || back < -1 || back >= DEPTH - 1) return true;
        return outside[index(right, up, back)];
    }

    /** Flood fill of the air from the box's border, through everything that is not a cell or a visible block. */
    private static boolean[] outsideAir(Map<Long, Byte> cells, Set<Long> blocked) {
        boolean[] outside = new boolean[W * W * DEPTH];
        boolean[] solid = new boolean[outside.length];
        for (Long k : cells.keySet()) solid[index((int) (k >> 16 & 255) - 64, (int) (k >> 8 & 255) - 64, (int) (k & 255))] = true;
        for (Long k : blocked) solid[index((int) (k >> 16 & 255) - 64, (int) (k >> 8 & 255) - 64, (int) (k & 255))] = true;
        int[] queue = new int[outside.length];
        int head = 0, tail = 0;
        for (int right = -BOX; right <= BOX; right++) {
            for (int up = -BOX; up <= BOX; up++) {
                for (int back = -1; back < DEPTH - 1; back++) {
                    boolean border = Math.abs(right) == BOX || Math.abs(up) == BOX || back == -1 || back == DEPTH - 2;
                    int i = index(right, up, back);
                    if (border && !solid[i]) {
                        outside[i] = true;
                        queue[tail++] = i;
                    }
                }
            }
        }
        while (head < tail) {
            int i = queue[head++];
            int back = i % DEPTH - 1, up = i / DEPTH % W - BOX, right = i / DEPTH / W - BOX;
            for (int[] d : DIRS) {
                int nr = right + d[0], nu = up + d[1], nb = back + d[2];
                if (nr < -BOX || nr > BOX || nu < -BOX || nu > BOX || nb < -1 || nb >= DEPTH - 1) continue;
                int n = index(nr, nu, nb);
                if (solid[n] || outside[n]) continue;
                outside[n] = true;
                queue[tail++] = n;
            }
        }
        return outside;
    }

    static void draw(long controller, VertexConsumer c, PoseStack.Pose pose, int overlay) {
        Mesh mesh = MESHES.get(controller);
        if (mesh == null) return;
        float[] d = mesh.data;
        for (int i = 0; i < mesh.size; i += 10) {
            c.vertex(pose.pose(), d[i], d[i + 1], d[i + 2]).color(d[i + 6], d[i + 7], d[i + 8], 1F).uv(0.5F, 0.5F)
                    .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(Float.floatToRawIntBits(d[i + 9]))
                    .normal(pose.normal(), d[i + 3], d[i + 4], d[i + 5]).endVertex();
        }
    }

    /**
     * One outward face: a panel in a seam (four strips round it, nothing overlapping, so no two surfaces share a depth),
     * a glowing strip for a light block, plain dark for a frame.
     */
    private static void face(Mesh m, int r, int u, int b, int dir, byte kind) {
        float ox, oy, oz, ux, uy, uz, vx, vy, vz;
        switch (dir) {
            case 0 -> { ox = r; oy = u; oz = b; ux = 0; uy = 0; uz = 1; vx = 0; vy = 1; vz = 0; }
            case 1 -> { ox = r + 1; oy = u; oz = b; ux = 0; uy = 0; uz = 1; vx = 0; vy = 1; vz = 0; }
            case 2 -> { ox = r; oy = u; oz = b; ux = 1; uy = 0; uz = 0; vx = 0; vy = 0; vz = 1; }
            case 3 -> { ox = r; oy = u + 1; oz = b; ux = 1; uy = 0; uz = 0; vx = 0; vy = 0; vz = 1; }
            case 4 -> { ox = r; oy = u; oz = b; ux = 1; uy = 0; uz = 0; vx = 0; vy = 1; vz = 0; }
            default -> { ox = r; oy = u; oz = b + 1; ux = 1; uy = 0; uz = 0; vx = 0; vy = 1; vz = 0; }
        }
        float nx = DIRS[dir][0], ny = DIRS[dir][1], nz = DIRS[dir][2];
        if (kind == FRAME) {
            rect(m, ox, oy, oz, ux, uy, uz, vx, vy, vz, 0, 1, 0, 1, nx, ny, nz, FRAME_RGB, SKY);
            return;
        }
        boolean glow = kind == LIGHT;
        float in = glow ? GLOW_INSET : INSET;
        int seam = glow ? GLOW_SEAM : SEAM, panel = glow ? GLOW : WHITE, light = glow ? FULL_BRIGHT : SKY;
        // two seam strips along the lower and the left edge (the neighbour's panel completes the grid), then the panel
        rect(m, ox, oy, oz, ux, uy, uz, vx, vy, vz, 0, 1, 0, in, nx, ny, nz, seam, SKY);
        rect(m, ox, oy, oz, ux, uy, uz, vx, vy, vz, 0, in, in, 1, nx, ny, nz, seam, SKY);
        rect(m, ox, oy, oz, ux, uy, uz, vx, vy, vz, in, 1, in, 1, nx, ny, nz, panel, light);
    }

    private static void rect(Mesh m, float ox, float oy, float oz, float ux, float uy, float uz, float vx, float vy,
                             float vz, float a0, float a1, float b0, float b1, float nx, float ny, float nz, int rgb,
                             int light) {
        float[] p = {
                ox + ux * a0 + vx * b0, oy + uy * a0 + vy * b0, oz + uz * a0 + vz * b0,
                ox + ux * a1 + vx * b0, oy + uy * a1 + vy * b0, oz + uz * a1 + vz * b0,
                ox + ux * a1 + vx * b1, oy + uy * a1 + vy * b1, oz + uz * a1 + vz * b1,
                ox + ux * a0 + vx * b1, oy + uy * a0 + vy * b1, oz + uz * a0 + vz * b1 };
        m.quad(p, nx, ny, nz, rgb, light);
    }
}
