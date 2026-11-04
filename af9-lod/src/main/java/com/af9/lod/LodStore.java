package com.af9.lod;

import com.af9.lod.core.LodGrid;
import it.unimi.dsi.fastutil.longs.Long2ByteMap;
import it.unimi.dsi.fastutil.longs.Long2ByteOpenHashMap;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Saves/loads the far LOD grids and the sampler's "done" map next to the world (saves/&lt;world&gt;/af9_lod/far.bin.gz), so
 * far terrain is there right after joining instead of being re-sampled every launch. Singleplayer only.
 */
public final class LodStore {
    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger("af9_lod");
    private static final int MAGIC = 0xAF9D0004;

    private final LodFarSampler sampler;
    private int ticks;

    public LodStore(LodFarSampler sampler) {
        this.sampler = sampler;
    }

    private static Path file(MinecraftServer server) {
        return server.getWorldPath(LevelResource.ROOT).resolve("af9_lod").resolve("far.bin.gz");
    }

    @SubscribeEvent
    public void onStarted(ServerStartedEvent event) {
        MinecraftServer server = event.getServer();
        if (server.isDedicatedServer()) return;
        LodWorld.INSTANCE.clearAllFar();
        sampler.doneMap().clear();
        Path f = file(server);
        if (!Files.exists(f)) return;
        try (DataInputStream in = new DataInputStream(new BufferedInputStream(new GZIPInputStream(Files.newInputStream(f)), 1 << 16))) {
            if (in.readInt() != MAGIC) return;
            int levels = in.readInt();
            int grids = 0;
            for (int l = 1; l <= levels; l++) {
                int n = in.readInt();
                for (int i = 0; i < n; i++) {
                    long key = in.readLong();
                    LodGrid g = new LodGrid();
                    for (int y = 0; y < 16; y++) for (int z = 0; z < 16; z++) for (int x = 0; x < 16; x++) g.set(x, y, z, in.readInt());
                    if (l <= LodWorld.MAX_FAR_LEVEL) LodWorld.INSTANCE.farGrids(l).put(key, g);
                    grids++;
                }
            }
            int done = in.readInt();
            for (int i = 0; i < done; i++) sampler.doneMap().put(in.readLong(), in.readByte());
            LodWorld.INSTANCE.remeshAllFar();
            LOGGER.info("LOD cache loaded: {} grids, {} chunks done", grids, done);
        } catch (IOException | RuntimeException e) {
            LOGGER.warn("LOD cache unreadable, starting empty: {}", e.toString());
            LodWorld.INSTANCE.clearAllFar();
            sampler.doneMap().clear();
        }
    }

    @SubscribeEvent
    public void onStopping(ServerStoppingEvent event) {
        if (!event.getServer().isDedicatedServer()) save(event.getServer());
    }

    /** Autosave every ~3 minutes of server time. */
    @SubscribeEvent
    public void onTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.getServer().isDedicatedServer()) return;
        if (++ticks % 3600 == 0) save(event.getServer());
    }

    private void save(MinecraftServer server) {
        Path f = file(server);
        Path tmp = f.resolveSibling("far.bin.gz.tmp");
        try {
            Files.createDirectories(f.getParent());
            try (DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new GZIPOutputStream(Files.newOutputStream(tmp)), 1 << 16))) {
                out.writeInt(MAGIC);
                out.writeInt(LodWorld.MAX_FAR_LEVEL);
                for (int l = 1; l <= LodWorld.MAX_FAR_LEVEL; l++) {
                    Map<Long, LodGrid> m = LodWorld.INSTANCE.farGrids(l);
                    out.writeInt(m.size());
                    int written = 0;
                    for (Map.Entry<Long, LodGrid> e : m.entrySet()) {
                        if (written++ >= m.size()) break;
                        out.writeLong(e.getKey());
                        LodGrid g = e.getValue();
                        for (int y = 0; y < 16; y++) for (int z = 0; z < 16; z++) for (int x = 0; x < 16; x++) out.writeInt(g.get(x, y, z));
                    }
                }
                Long2ByteOpenHashMap done = sampler.doneMap();
                out.writeInt(done.size());
                for (Long2ByteMap.Entry e : done.long2ByteEntrySet()) {
                    out.writeLong(e.getLongKey());
                    out.writeByte(e.getByteValue());
                }
            }
            Files.move(tmp, f, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException | RuntimeException e) {
            LOGGER.warn("LOD cache save failed: {}", e.toString());
        }
    }
}
