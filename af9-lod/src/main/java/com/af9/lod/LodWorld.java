package com.af9.lod;

import com.af9.lod.core.LodGrid;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Client-side store of level-0 LOD grids (one per 16x16x16 section), filled as chunks load and dropped as they unload.
 * Higher levels are built from these with LodGrid.downsample. Draft: no persistence yet, and only chunks the server sent.
 */
public final class LodWorld {
    public static final LodWorld INSTANCE = new LodWorld();

    /** key = packed section coords, see key(). */
    private final ConcurrentHashMap<Long, LodGrid> level0 = new ConcurrentHashMap<>();

    public static long key(int sx, int sy, int sz) {
        return ((long) (sx & 0x3FFFFF) << 42) | ((long) (sz & 0x3FFFFF) << 20) | (sy & 0xFFFFF);
    }

    private volatile int version;

    /** Bumped on every change so the renderer knows when to rebuild its quad buffer. */
    public int version() {
        return version;
    }

    public java.util.Map<Long, LodGrid> grids() {
        return level0;
    }

    public int size() {
        return level0.size();
    }

    public LodGrid get(int sx, int sy, int sz) {
        return level0.get(key(sx, sy, sz));
    }

    @SubscribeEvent
    public void onChunkLoad(ChunkEvent.Load event) {
        LevelAccessor level = event.getLevel();
        if (!level.isClientSide()) return;
        version++;
        ChunkAccess chunk = event.getChunk();
        int cx = chunk.getPos().x, cz = chunk.getPos().z;
        LevelChunkSection[] sections = chunk.getSections();
        for (int i = 0; i < sections.length; i++) {
            LevelChunkSection sec = sections[i];
            int sy = chunk.getSectionYFromSectionIndex(i);
            if (sec == null || sec.hasOnlyAir()) {
                level0.remove(key(cx, sy, cz));
                continue;
            }
            LodGrid grid = new LodGrid();
            for (int y = 0; y < 16; y++) for (int z = 0; z < 16; z++) for (int x = 0; x < 16; x++) {
                BlockState s = sec.getBlockState(x, y, z);
                if (!s.isAir()) grid.set(x, y, z, Block.getId(s));
            }
            if (grid.isEmpty()) level0.remove(key(cx, sy, cz));
            else level0.put(key(cx, sy, cz), grid);
        }
    }

    @SubscribeEvent
    public void onChunkUnload(ChunkEvent.Unload event) {
        if (!event.getLevel().isClientSide()) return;
        version++;
        ChunkAccess chunk = event.getChunk();
        int cx = chunk.getPos().x, cz = chunk.getPos().z;
        for (int i = 0; i < chunk.getSections().length; i++)
            level0.remove(key(cx, chunk.getSectionYFromSectionIndex(i), cz));
    }
}
