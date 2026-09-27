package com.af9.core.client.render;

import java.lang.reflect.Method;

/**
 * Oculus (Iris for Forge), when installed, through its public API by reflection (no compile dependency): whether a
 * shader pack draws the world, and whether it is drawing the shadows. Without Oculus both are false.
 */
final class IrisCompat {

    private static final Object API;
    private static final Method SHADER_PACK_IN_USE;
    private static final Method RENDERING_SHADOW_PASS;

    static {
        Object api = null;
        Method inUse = null, shadow = null;
        try {
            Class<?> type = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
            api = type.getMethod("getInstance").invoke(null);
            inUse = type.getMethod("isShaderPackInUse");
            shadow = type.getMethod("isRenderingShadowPass");
        } catch (ReflectiveOperationException | LinkageError | RuntimeException ignored) {
            // no Oculus
            api = null;
        }
        API = api;
        SHADER_PACK_IN_USE = inUse;
        RENDERING_SHADOW_PASS = shadow;
    }

    private IrisCompat() {}

    /** A shader pack is active: it draws the world, and whatever is drawn outside its passes is lost. */
    static boolean shaderPackInUse() {
        return call(SHADER_PACK_IN_USE);
    }

    /** The shader pack is drawing its shadow map (block entities are rendered again for it). */
    static boolean renderingShadowPass() {
        return call(RENDERING_SHADOW_PASS);
    }

    private static boolean call(Method method) {
        if (API == null || method == null) return false;
        try {
            return (Boolean) method.invoke(API);
        } catch (ReflectiveOperationException | RuntimeException e) {
            return false;
        }
    }
}
