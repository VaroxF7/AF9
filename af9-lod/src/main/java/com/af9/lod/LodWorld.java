package com.af9.lod;

import com.af9.lod.core.LodGrid;
import com.af9.lod.core.LodMesher;
import java.util.ArrayDeque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Client-side store of level-0 LOD sections. Chunk sampling is queued and done within a small per-tick time budget on the
 * client thread; meshing (with neighbour face culling) runs on one background thread. The render hook only reads the
 * finished meshes, so nothing heavy happens on the render thread.
 */
public final class LodWorld {
    public static final LodWorld INSTANCE = new LodWorld();
    private static final long TICK_BUDGET_NANOS = 2_000_000L;

    private final ConcurrentHashMap<Long, LodGrid> grids = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, int[]> meshes = new ConcurrentHashMap<>();
    private final ArrayDeque<ChunkAccess> pending = new ArrayDeque<>();  // client thread only
    private final ExecutorService mesher = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "af9-lod-mesher");
        t.setDaemon(true);
        t.setPriority(Thread.MIN_PRIORITY);
        return t;
    });
    // far store: level 2 (one voxel = 4x4x4 blocks, one grid = 64 blocks per axis), keyed by 64-block cell coords
    private final ConcurrentHashMap<Long, LodGrid> farGrids = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, int[]> farMeshes = new ConcurrentHashMap<>();
    private volatile int version;

    public static long key(int sx, int sy, int sz) {
        return ((long) (sx & 0x3FFFFF) << 42) | ((long) (sz & 0x3FFFFF) << 20) | (sy & 0xFFFFF);
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

    public Map<Long, int[]> farMeshes() {
        return farMeshes;
    }

    /** Server thread: sample one full chunk into the level-2 far store (4x4x4 blocks per voxel, majority non-air). */
    public void sampleFar(ChunkAccess chunk) {
        int cx = chunk.getPos().x, cz = chunk.getPos().z;
        int x2 = Math.floorDiv(cx, 4), z2 = Math.floorDiv(cz, 4);
        int ox = Math.floorMod(cx, 4) * 4, oz = Math.floorMod(cz, 4) * 4;
        java.util.HashSet<Long> touched = new java.util.HashSet<>();
        int[] ids = new int[64];
        LevelChunkSection[] sections = chunk.getSections();
        for (int i = 0; i < sections.length; i++) {
            LevelChunkSection sec = sections[i];
            int sy = chunk.getSectionYFromSectionIndex(i);
            int y2 = Math.floorDiv(sy, 4), oy = Math.floorMod(sy, 4) * 4;
            long k = key(x2, y2, z2);
            boolean empty = sec == null || sec.hasOnlyAir();
            LodGrid g = empty ? farGrids.get(k) : farGrids.computeIfAbsent(k, kk -> new LodGrid());
            if (g == null) continue;
            for (int vy = 0; vy < 4; vy++) for (int vz = 0; vz < 4; vz++) for (int vx = 0; vx < 4; vx++) {
                int id = 0;
                if (!empty) {
                    int n = 0;
                    for (int by = 0; by < 4; by++) for (int bz = 0; bz < 4; bz++) for (int bx = 0; bx < 4; bx++) {
                        BlockState s = sec.getBlockState(vx * 4 + bx, vy * 4 + by, vz * 4 + bz);
                        if (!s.isAir()) ids[n++] = Block.getId(s);
                    }
                    if (n > 0) id = LodGrid.mostCommon(ids, n);
                }
                g.set(ox + vx, oy + vy, oz + vz, id);
            }
            touched.add(k);
        }
        if (touched.isEmpty()) return;
        mesher.execute(() -> {
            for (long k : touched) {
                int kx = keyX(k), ky = keyY(k), kz = keyZ(k);
                meshFar(kx, ky, kz);
                for (int[] d : LodMesher.DIRS) meshFar(kx + d[0], ky + d[1], kz + d[2]);
            }
            version++;
        });
    }

    private void meshFar(int sx, int sy, int sz) {
        long k = key(sx, sy, sz);
        LodGrid g = farGrids.get(k);
        if (g == null || g.isEmpty()) {
            farMeshes.remove(k);
            return;
        }
        LodGrid[] nb = new LodGrid[6];
        for (int f = 0; f < 6; f++) {
            int[] d = LodMesher.DIRS[f];
            nb[f] = farGrids.get(key(sx + d[0], sy + d[1], sz + d[2]));
        }
        int[] mesh = new LodMesher().mesh(g, nb);
        if (mesh.length == 0) farMeshes.remove(k);
        else farMeshes.put(k, mesh);
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
                BlockState s = sec.getBlockState(x, y, z);
                if (!s.isAir()) grid.set(x, y, z, Block.getId(s));
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

    /** Re-mesh a section and its six neighbours (their shared faces changed). */
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
