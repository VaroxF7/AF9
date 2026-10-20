package com.af9.core.mixin.zeta;

import java.util.Map;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Quark's event bus (Zeta) keeps the handlers it has converted in a plain fastutil map, and it writes to it from
 * two threads: the thread that constructs Quark's modules and the thread that loads Zeta's config (a config load
 * re-subscribes the modules that the config enables). Two threads rehashing the same map at once leave it with the
 * array of one size and the length of the other, and the next write dies with
 * "Index 512 out of bounds for length 257" while Quark is still loading: "Quark (quark) has failed to load
 * correctly".
 * <p>
 * The map is only written in {@code subscribeMethod} (a put) and {@code unsubscribeMethod} (a remove), so locking
 * those two calls on the map itself makes it safe. Zeta does the same thing upstream since 1.0-31.
 */
@Pseudo
@Mixin(targets = "org.violetmoon.zetaimplforge.event.ForgeZetaEventBus", remap = false)
public abstract class ForgeZetaEventBusMixin {

    @Redirect(method = "subscribeMethod", at = @At(value = "INVOKE",
            target = "Ljava/util/Map;put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", remap = false),
            remap = false, require = 0)
    private Object af9$lockedPut(Map<Object, Object> handlers, Object key, Object handler) {
        synchronized (handlers) {
            return handlers.put(key, handler);
        }
    }

    @Redirect(method = "unsubscribeMethod", at = @At(value = "INVOKE",
            target = "Ljava/util/Map;remove(Ljava/lang/Object;)Ljava/lang/Object;", remap = false),
            remap = false, require = 0)
    private Object af9$lockedRemove(Map<Object, Object> handlers, Object key) {
        synchronized (handlers) {
            return handlers.remove(key);
        }
    }
}
