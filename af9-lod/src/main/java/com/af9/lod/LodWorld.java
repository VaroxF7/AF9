package com.af9.lod;

import com.af9.lod.core.LodGrid;
import com.af9.lod.core.LodMesher;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Client-side LOD stores.
 *  - level 0: one grid per loaded 16^3 section (only used by the debug draw), filled from client chunk events.
 *  - levels 1..4: far terrain, one voxel = 2 / 4 / 8 / 16 blocks, one grid = 32 / 64 / 128 / 256 blocks per axis. Filled from the
 *    integrated server by LodFarSampler.
 * Meshing (with neighbour face culling) runs on one background thread; the render hook only reads finished meshes.
 */
public final class LodWorld {
    public static final LodWorld INSTANCE = new LodWorld();
    public static final int MAX_FAR_LEVEL = 4;
    private static final long TICK_BUDGET_NANOS = 2_000_000L;

    private final ConcurrentHashMap<Long, LodGrid> grids = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, int[]> meshes = new ConcurrentHashMap<>();
    @SuppressWarnings("unchecked")
    private final ConcurrentHashMap<Long, LodGrid>[] farGrids = new ConcurrentHashMap[MAX_FAR_LEVEL + 1];
    @SuppressWarnings("unchecked")
    private final ConcurrentHashMap<Long, int[]>[] farMeshes = new ConcurrentHashMap[MAX_FAR_LEVEL + 1];
    private final ArrayDeque<ChunkAccess> pending = new ArrayDeque<>();  // client thread only
    private final ExecutorService mesher = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "af9-lod-mesher");
        t.setDaemon(true);
        t.setPriority(Thread.MIN_PRIORITY);
        return t;
    });
    private volatile int version;

    private LodWorld() {
        for (int l = 0; l <= MAX_FAR_LEVEL; l++) {
            farGrids[l] = new ConcurrentHashMap<>();
            farMeshes[l] = new ConcurrentHashMap<>();
        }
    }

    public static long key(int sx, int sy, int sz) {
        return ((long) (sx & 0x3FFFFF) << 42) | ((long) (sz & 0x3FFFFF) << 20) | (sy & 0xFFFFF);
    }

    /** Voxel value = opaque ARGB (never 0, so never "air"): water gets a water blue, everything else its map colour. */
    static int colorId(BlockState s) {
        int rgb = s.getFluidState().is(FluidTags.WATER) ? 0x3F76E4 : s.getBlock().defaultMapColor().col;
        if (rgb == 0) rgb = 0x707070;
        else if (rgb == 0xA0A0FF) rgb = 0x8DB6E8;   // ICE map colour is lavender; real ice is pale blue
        return 0xFF000000 | rgb;
    }

    public static int keyX(long k) { return (int) (k >> 42); }
    public static int keyZ(long k) { return (int) ((k << 22) >> 42); }
    public static int keyY(long k) { return (int) ((k << 44) >> 44); }

    /** Bumped when any mesh changes so the renderer knows to rebuild its GPU buffers. */
    public int version() {
        return version;
    }

    public Map<Long, int[]> meshes() {
        return meshes;
    }

    /** Meshes of far level 1..MAX_FAR_LEVEL. */
    public Map<Long, int[]> farMeshes(int level) {
        return farMeshes[level];
    }

    public int farMeshCount() {
        int n = 0;
        for (int l = 1; l <= MAX_FAR_LEVEL; l++) n += farMeshes[l].size();
        return n;
    }

    /**
     * Server thread: sample one chunk into far level `level` (voxel = 2^level blocks, majority non-air colour).
     * A grid covers 2^level chunks horizontally and 2^level sections vertically.
     */
    public void sampleFar(ChunkAccess chunk, int level) {
        final int s = 1 << level, vpc = 16 >> level, b = 1 << level;   // chunks per grid, voxels per chunk axis, blocks per voxel
        int cx = chunk.getPos().x, cz = chunk.getPos().z;
        int gx = Math.floorDiv(cx, s), gz = Math.floorDiv(cz, s);
        int ox = Math.floorMod(cx, s) * vpc, oz = Math.floorMod(cz, s) * vpc;
        HashSet<Long> touched = new HashSet<>();
        int[] ids = new int[b * b * b];
        LevelChunkSection[] sections = chunk.getSections();
        for (int i = 0; i < sections.length; i++) {
            LevelChunkSection sec = sections[i];
            int sy = chunk.getSectionYFromSectionIndex(i);
            int gy = Math.floorDiv(sy, s), oy = Math.floorMod(sy, s) * vpc;
            long k = key(gx, gy, gz);
            boolean empty = sec == null || sec.hasOnlyAir();
            LodGrid g = empty ? farGrids[level].get(k) : farGrids[level].computeIfAbsent(k, kk -> new LodGrid());
            if (g == null) continue;
            for (int vy = 0; vy < vpc; vy++) for (int vz = 0; vz < vpc; vz++) for (int vx = 0; vx < vpc; vx++) {
                int id = 0;
                if (!empty) {
                    // topmost solid block of the voxel wins: the thin grass/snow skin must not be outvoted by the stone below it
                    outer:
                    for (int by = b - 1; by >= 0; by--) for (int bz = 0; bz < b; bz++) for (int bx = 0; bx < b; bx++) {
                        BlockState st = sec.getBlockState(vx * b + bx, vy * b + by, vz * b + bz);
                        if (!st.isAir()) { id = colorId(st); break outer; }
                    }
                }
                g.set(ox + vx, oy + vy, oz + vz, id);
            }
            touched.add(k);
        }
        scheduleFarMesh(level, touched);
    }

    /** Server thread: drop a chunk's footprint from far level `level` (before it is re-sampled at a finer level). */
    public void clearFar(ChunkAccess chunk, int level) {
        final int s = 1 << level, vpc = 16 >> level;
        int cx = chunk.getPos().x, cz = chunk.getPos().z;
        int gx = Math.floorDiv(cx, s), gz = Math.floorDiv(cz, s);
        int ox = Math.floorMod(cx, s) * vpc, oz = Math.floorMod(cz, s) * vpc;
        HashSet<Long> touched = new HashSet<>();
        for (int sy = chunk.getMinSection(); sy < chunk.getMaxSection(); sy++) {
            int gy = Math.floorDiv(sy, s), oy = Math.floorMod(sy, s) * vpc;
            long k = key(gx, gy, gz);
            LodGrid g = farGrids[level].get(k);
            if (g == null) continue;
            for (int vy = 0; vy < vpc; vy++) for (int vz = 0; vz < vpc; vz++) for (int vx = 0; vx < vpc; vx++)
                g.set(ox + vx, oy + vy, oz + vz, 0);
            touched.add(k);
        }
        scheduleFarMesh(level, touched);
    }

    // ---- persistence (per world, inside the save folder) ----

    /** Snapshot of the far grids for saving: level -> (key -> grid). */
    ConcurrentHashMap<Long, LodGrid> farGrids(int level) {
        return farGrids[level];
    }

    /** Re-mesh every loaded far grid (after loading the cache). */
    void remeshAllFar() {
        mesher.execute(() -> {
            for (int l = 1; l <= MAX_FAR_LEVEL; l++)
                for (long k : farGrids[l].keySet()) meshFar(l, keyX(k), keyY(k), keyZ(k));
            version++;
        });
    }

    void clearAllFar() {
        for (int l = 1; l <= MAX_FAR_LEVEL; l++) {
            farGrids[l].clear();
            farMeshes[l].clear();
        }
        version++;
    }

    private void scheduleFarMesh(int level, HashSet<Long> touched) {
        if (touched.isEmpty()) return;
        mesher.execute(() -> {
            for (long k : touched) {
                int kx = keyX(k), ky = keyY(k), kz = keyZ(k);
                meshFar(level, kx, ky, kz);
                for (int[] d : LodMesher.DIRS) meshFar(level, kx + d[0], ky + d[1], kz + d[2]);
            }
            version++;
        });
    }

    private void meshFar(int level, int sx, int sy, int sz) {
        long k = key(sx, sy, sz);
        LodGrid g = farGrids[level].get(k);
        if (g == null || g.isEmpty()) {
            farMeshes[level].remove(k);
            return;
        }
        LodGrid[] nb = new LodGrid[6];
        for (int f = 0; f < 6; f++) {
            int[] d = LodMesher.DIRS[f];
            nb[f] = farGrids[level].get(key(sx + d[0], sy + d[1], sz + d[2]));
        }
        int[] mesh = new LodMesher().mesh(g, nb);
        if (mesh.length == 0) farMeshes[level].remove(k);
        else farMeshes[level].put(k, mesh);
    }

    @SubscribeEvent
    public void onChunkLoad(ChunkEvent.Load event) {
        if (!event.getLevel().isClientSide()) return;
        pending.add(event.getChunk());
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        long deadline = System.nanoTime() + TICK_BUDGET_NANOS;
        while (!pending.isEmpty() && System.nanoTime() < deadline) sample(pending.poll());
    }

    private void sample(ChunkAccess chunk) {
        int cx = chunk.getPos().x, cz = chunk.getPos().z;
        LevelChunkSection[] sections = chunk.getSections();
        for (int i = 0; i < sections.length; i++) {
            LevelChunkSection sec = sections[i];
            int sy = chunk.getSectionYFromSectionIndex(i);
            long k = key(cx, sy, cz);
            if (sec == null || sec.hasOnlyAir()) {
                if (grids.remove(k) != null) remeshAround(cx, sy, cz);
                continue;
            }
            LodGrid grid = new LodGrid();
            for (int y = 0; y < 16; y++) for (int z = 0; z < 16; z++) for (int x = 0; x < 16; x++) {
                BlockState st = sec.getBlockState(x, y, z);
                if (!st.isAir()) grid.set(x, y, z, colorId(st));
            }
            if (grid.isEmpty()) grids.remove(k);
            else grids.put(k, grid);
            remeshAround(cx, sy, cz);
        }
    }

    @SubscribeEvent
    public void onChunkUnload(ChunkEvent.Unload event) {
        if (!event.getLevel().isClientSide()) return;
        ChunkAccess chunk = event.getChunk();
        pending.remove(chunk);
        int cx = chunk.getPos().x, cz = chunk.getPos().z;
        for (int i = 0; i < chunk.getSections().length; i++) {
            long k = key(cx, chunk.getSectionYFromSectionIndex(i), cz);
            grids.remove(k);
            meshes.remove(k);
        }
        version++;
    }

    /** Re-mesh a level-0 section and its six neighbours (their shared faces changed). */
    private void remeshAround(int sx, int sy, int sz) {
        mesher.execute(() -> {
            meshOne(sx, sy, sz);
            for (int[] d : LodMesher.DIRS) meshOne(sx + d[0], sy + d[1], sz + d[2]);
            version++;
        });
    }

    private void meshOne(int sx, int sy, int sz) {
        long k = key(sx, sy, sz);
        LodGrid g = grids.get(k);
        if (g == null) {
            meshes.remove(k);
            return;
        }
        LodGrid[] nb = new LodGrid[6];
        for (int f = 0; f < 6; f++) {
            int[] d = LodMesher.DIRS[f];
            nb[f] = grids.get(key(sx + d[0], sy + d[1], sz + d[2]));
        }
        int[] mesh = new LodMesher().mesh(g, nb);
        if (mesh.length == 0) meshes.remove(k);
        else meshes.put(k, mesh);
    }
}
