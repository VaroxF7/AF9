package com.af9.lod;

import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11C;
import org.lwjgl.opengl.GLCapabilities;

/** What the GPU/driver offers for the GPU-driven LOD path. Must be called on the render thread with a current GL context. */
public record GlCaps(String renderer, String version, boolean gl46, boolean compute, boolean multiDrawIndirect,
                     boolean indirectCount, boolean shaderStorage, boolean persistentMapping) {

    public static GlCaps query() {
        GLCapabilities c = GL.getCapabilities();
        return new GlCaps(
                GL11C.glGetString(GL11C.GL_RENDERER), GL11C.glGetString(GL11C.GL_VERSION),
                c.OpenGL46, c.OpenGL43 || c.GL_ARB_compute_shader,
                c.OpenGL43 || c.GL_ARB_multi_draw_indirect,
                c.OpenGL46 || c.GL_ARB_indirect_parameters,
                c.OpenGL43 || c.GL_ARB_shader_storage_buffer_object,
                c.OpenGL44 || c.GL_ARB_buffer_storage);
    }

    /** The GPU-driven path needs compute, multi-draw-indirect, SSBOs and persistent mapping (all core in GL 4.4). */
    public boolean usable() {
        return compute && multiDrawIndirect && shaderStorage && persistentMapping;
    }

    @Override
    public String toString() {
        return renderer + " / GL " + version + " usable=" + usable() + " indirectCount=" + indirectCount;
    }
}
