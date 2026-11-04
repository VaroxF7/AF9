package com.af9.lod;

import java.lang.reflect.Method;

/** Reflection-only link to Oculus/Iris (no compile dependency): is a shader pack currently active? */
public final class IrisCompat {
    private static Method inUse;
    private static Object api;
    private static boolean resolved;

    private IrisCompat() {}

    public static boolean shaderPackInUse() {
        if (!resolved) {
            resolved = true;
            try {
                Class<?> c = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
                api = c.getMethod("getInstance").invoke(null);
                inUse = c.getMethod("isShaderPackInUse");
            } catch (ReflectiveOperationException | LinkageError e) {
                org.slf4j.LoggerFactory.getLogger("af9_lod").info("Iris/Oculus API not found, assuming no shader packs");
            }
        }
        if (inUse == null) return false;
        try {
            return (boolean) inUse.invoke(api);
        } catch (ReflectiveOperationException | RuntimeException e) {
            return false;
        }
    }
}
