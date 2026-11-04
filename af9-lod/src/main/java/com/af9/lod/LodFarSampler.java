package com.af9.lod;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
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
    private final LongOpenHashSet done = new LongOpenHashSet();
    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger("af9_lod");
    private ServerLevel lastLevel;
    private int warmupTicks;

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !LodConfig.ENABLED.get() || !LodConfig.GENERATE_FAR.get()) return;
        MinecraftServer server = event.getServer();
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
        int budgetChunks = 6;   // time budget below is what really limits it; chunks already on disk load in ~1 ms

        for (int r = view + 1; r <= max; r++) {
            for (int i = 0; i < 8 * r; i++) {
                int[] d = ringOffset(r, i);
                int x = pcx + d[0], z = pcz + d[1];
                long k = ChunkPos.asLong(x, z);
                if (done.contains(k)) continue;
                if (System.nanoTime() > deadline || budgetChunks <= 0) return;
                budgetChunks--;
                done.add(k);
                if (done.size() % 200 == 1)
                    LOGGER.info("far sampler: {} chunks sampled, far meshes {}", done.size(), LodWorld.INSTANCE.farMeshes().size());
                try {
                    ChunkAccess chunk = level.getChunk(x, z, ChunkStatus.FULL, true);
                    if (chunk != null) LodWorld.INSTANCE.sampleFar(chunk);
                } catch (RuntimeException e) {
                    org.slf4j.LoggerFactory.getLogger("af9_lod").warn("far chunk {} {} failed: {}", x, z, e.toString());
                }
            }
        }
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
