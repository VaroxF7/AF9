package com.af9.lod;

import it.unimi.dsi.fastutil.longs.Long2ByteOpenHashMap;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Singleplayer only (integrated server): walks rings of chunks around the first player, from just outside the view
 * distance out to maxDistanceChunks, loads (or generates) each one once and hands it to LodWorld.sampleFar.
 * Time-boxed per tick so the server keeps its tick rate.
 */
public final class LodFarSampler {
    /** chunk -> far level it was last sampled at (absent = never). */
    private final Long2ByteOpenHashMap done = new Long2ByteOpenHashMap();
    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger("af9_lod");
    private ServerLevel lastLevel;
    private int warmupTicks;

    private int heartbeat;

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        try {
            tick(event.getServer());
        } catch (RuntimeException e) {
            if (heartbeat++ % 200 == 0) LOGGER.warn("far sampler tick failed", e);
        }
    }

    private void tick(MinecraftServer server) {
        if (!LodConfig.ENABLED.get() || !LodConfig.GENERATE_FAR.get()) {
            if (++heartbeat % 200 == 0) LOGGER.info("far sampler idle: enabled={} generateFar={}", LodConfig.ENABLED.get(), LodConfig.GENERATE_FAR.get());
            return;
        }
        if (server.isDedicatedServer() || server.getPlayerList().getPlayers().isEmpty()) return;
        ServerPlayer player = server.getPlayerList().getPlayers().get(0);
        ServerLevel level = player.serverLevel();
        if (level != lastLevel) {
            lastLevel = level;
            done.clear();
            warmupTicks = 0;
        }
        // let the world finish loading first, and back off whenever the server is already struggling
        if (warmupTicks++ < 100) return;
        if (server.getAverageTickTime() > 45f) {
            if (warmupTicks % 100 == 0) LOGGER.info("far sampler paused: server average tick {} ms", server.getAverageTickTime());
            return;
        }
        int pcx = player.chunkPosition().x, pcz = player.chunkPosition().z;
        int view = server.getPlayerList().getViewDistance();
        int max = LodConfig.MAX_DISTANCE_CHUNKS.get();
        long deadline = System.nanoTime() + LodConfig.FAR_BUDGET_MS.get() * 1_000_000L;
        if (++heartbeat % 200 == 0)
            LOGGER.info("far sampler: sampled {}, view {} -> max {} chunks, avg tick {} ms, store {} cells", done.size(), view, max,
                    server.getAverageTickTime(), LodWorld.INSTANCE.farMeshCount());
        int budgetChunks = 6;   // time budget below is what really limits it; chunks already on disk load in ~1 ms

        for (int r = view + 1; r <= max; r++) {
            int want = levelFor(r - view);
            for (int i = 0; i < 8 * r; i++) {
                int[] d = ringOffset(r, i);
                int x = pcx + d[0], z = pcz + d[1];
                long k = ChunkPos.asLong(x, z);
                int have = done.get(k);          // 0 = never sampled
                if (have != 0 && have <= want) continue;   // already at this detail or finer
                if (System.nanoTime() > deadline || budgetChunks <= 0) return;
                budgetChunks--;
                try {
                    ChunkAccess chunk = level.getChunk(x, z, LodConfig.FAST_GENERATION.get() ? ChunkStatus.SURFACE : ChunkStatus.FULL, true);
                    if (chunk != null) {
                        if (have != 0) LodWorld.INSTANCE.clearFar(chunk, have);   // refining: drop the coarse copy first
                        LodWorld.INSTANCE.sampleFar(chunk, want);
                    }
                    done.put(k, (byte) want);
                } catch (RuntimeException e) {
                    done.put(k, (byte) want);
                    LOGGER.warn("far chunk {} {} failed: {}", x, z, e.toString());
                }
                if (done.size() % 200 == 1)
                    LOGGER.info("far sampler: {} chunks sampled, far meshes {}", done.size(), LodWorld.INSTANCE.farMeshCount());
            }
        }
    }

    /** Detail level for a chunk `ring` chunks beyond the view distance: 2-block voxels, then 4, then 8. */
    static int levelFor(int ringBeyondView) {
        if (ringBeyondView <= 16) return 1;
        if (ringBeyondView <= 48) return 2;
        return 3;
    }

    /** i-th offset (0 .. 8r-1) on the square ring of Chebyshev radius r. */
    static int[] ringOffset(int r, int i) {
        int side = 2 * r;
        int s = i / side, t = i % side;
        switch (s) {
            case 0: return new int[] {-r + t, -r};
            case 1: return new int[] {r, -r + t};
            case 2: return new int[] {r - t, r};
            default: return new int[] {-r, r - t};
        }
    }
}
