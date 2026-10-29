package com.af9.core.mixin.naturesaura;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Nature's Aura keeps every loaded chunk that has drain spots in one per-level
 * {@code LevelData.auraChunksWithSpots} Long2ObjectOpenHashMap, and mutates it while an
 * AuraChunk is deserialized. C2ME loads chunks on several worker threads, so concurrent
 * put/remove calls corrupt the map ({@code ArrayIndexOutOfBoundsException: Index -1} in
 * {@code rehash}) and take the server down. Serialising the two writes on the map itself
 * fixes the race. String target, so this needs no Nature's Aura at compile time and skips
 * itself where it is absent.
 */
@Pseudo
@Mixin(targets = "de.ellpeck.naturesaura.chunk.AuraChunk", remap = false)
public abstract class AuraChunkMixin {

    @Redirect(method = "addOrRemoveAsActive",
            at = @At(value = "INVOKE",
                    target = "Lit/unimi/dsi/fastutil/longs/Long2ObjectOpenHashMap;put(JLjava/lang/Object;)Ljava/lang/Object;"),
            remap = false)
    private Object af9$syncPut(Long2ObjectOpenHashMap<Object> map, long key, Object value) {
        synchronized (map) {
            return map.put(key, value);
        }
    }

    @Redirect(method = "addOrRemoveAsActive",
            at = @At(value = "INVOKE",
                    target = "Lit/unimi/dsi/fastutil/longs/Long2ObjectOpenHashMap;remove(J)Ljava/lang/Object;"),
            remap = false)
    private Object af9$syncRemove(Long2ObjectOpenHashMap<Object> map, long key) {
        synchronized (map) {
            return map.remove(key);
        }
    }
}
