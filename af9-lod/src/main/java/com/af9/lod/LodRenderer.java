package com.af9.lod;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import org.lwjgl.opengl.GL11C;
import org.lwjgl.opengl.GL15C;
import org.lwjgl.opengl.GL20C;
import org.lwjgl.opengl.GL30C;
import org.lwjgl.opengl.GL42C;
import org.lwjgl.opengl.GL43C;

/**
 * GPU-driven draw of LOD cells (draft, H3). Render thread only.
 * Buffers: cells (SSBO 0, 32 bytes each), indirect commands (16 bytes each), quads (SSBO 2, 8 bytes each).
 * Per frame: a compute pass culls every cell and writes one DrawArraysIndirect command per cell, then one
 * glMultiDrawArraysIndirect draws all visible cells. The CPU never loops over cells.
 */
public final class LodRenderer implements AutoCloseable {
    public static final int CELL_BYTES = 32;
    public static final int CMD_BYTES = 16;

    private final int cullProgram, drawProgram, cellBuf, cmdBuf, quadBuf, vao;
    private int cellCount;

    public LodRenderer() {
        cullProgram = link(compile(GL43C.GL_COMPUTE_SHADER, "cull.comp"));
        drawProgram = link(compile(GL20C.GL_VERTEX_SHADER, "lod.vert"), compile(GL20C.GL_FRAGMENT_SHADER, "lod.frag"));
        cellBuf = GL15C.glGenBuffers();
        cmdBuf = GL15C.glGenBuffers();
        quadBuf = GL15C.glGenBuffers();
        vao = GL30C.glGenVertexArrays();
    }

    /** Quads change rarely: uploaded when the set of sections changes. */
    public void uploadQuads(ByteBuffer quads, int cellCount) {
        GL15C.glBindBuffer(GL43C.GL_SHADER_STORAGE_BUFFER, quadBuf);
        GL15C.glBufferData(GL43C.GL_SHADER_STORAGE_BUFFER, quads, GL15C.GL_DYNAMIC_DRAW);
        GL15C.glBindBuffer(GL43C.GL_DRAW_INDIRECT_BUFFER, cmdBuf);
        GL15C.glBufferData(GL43C.GL_DRAW_INDIRECT_BUFFER, (long) cellCount * CMD_BYTES, GL15C.GL_DYNAMIC_DRAW);
    }

    /** Cells are re-uploaded every frame (camera-relative origins). CELL_BYTES per cell as in cull.comp. */
    public void updateCells(ByteBuffer cells, int cellCount) {
        this.cellCount = cellCount;
        GL15C.glBindBuffer(GL43C.GL_SHADER_STORAGE_BUFFER, cellBuf);
        GL15C.glBufferData(GL43C.GL_SHADER_STORAGE_BUFFER, cells, GL15C.GL_STREAM_DRAW);
    }

    /** planes: 6 vec4 (camera-relative, inward normals); view/proj: column-major 4x4 float arrays. */
    public void render(float[] planes, float[] view, float[] proj, float nearBand, float farBand) {
        if (cellCount == 0) return;
        GL20C.glUseProgram(cullProgram);
        GL20C.glUniform4fv(GL20C.glGetUniformLocation(cullProgram, "uPlanes"), planes);
        GL30C.glUniform1ui(GL20C.glGetUniformLocation(cullProgram, "uCellCount"), cellCount);
        GL20C.glUniform1f(GL20C.glGetUniformLocation(cullProgram, "uNearBand"), nearBand);
        GL20C.glUniform1f(GL20C.glGetUniformLocation(cullProgram, "uFarBand"), farBand);
        GL30C.glBindBufferBase(GL43C.GL_SHADER_STORAGE_BUFFER, 0, cellBuf);
        GL30C.glBindBufferBase(GL43C.GL_SHADER_STORAGE_BUFFER, 1, cmdBuf);
        GL43C.glDispatchCompute((cellCount + 63) / 64, 1, 1);
        logGlError("after compute dispatch");
        GL42C.glMemoryBarrier(GL43C.GL_SHADER_STORAGE_BARRIER_BIT | GL42C.GL_COMMAND_BARRIER_BIT);

        GL20C.glUseProgram(drawProgram);
        GL20C.glUniformMatrix4fv(GL20C.glGetUniformLocation(drawProgram, "uView"), false, view);
        GL20C.glUniformMatrix4fv(GL20C.glGetUniformLocation(drawProgram, "uProj"), false, proj);
        GL30C.glBindBufferBase(GL43C.GL_SHADER_STORAGE_BUFFER, 0, cellBuf);
        GL30C.glBindBufferBase(GL43C.GL_SHADER_STORAGE_BUFFER, 2, quadBuf);
        GL30C.glBindVertexArray(vao);
        GL15C.glBindBuffer(GL43C.GL_DRAW_INDIRECT_BUFFER, cmdBuf);
        // Push LOD depth behind real chunks so it never z-fights with terrain Embeddium already drew:
        // LOD only shows where nothing nearer was drawn (that is the far-terrain case).
        boolean offsetWas = GL11C.glIsEnabled(GL11C.GL_POLYGON_OFFSET_FILL);
        GL11C.glEnable(GL11C.GL_POLYGON_OFFSET_FILL);
        GL11C.glPolygonOffset(4f, 8f);
        GL11C.glDisable(GL11C.GL_CULL_FACE);
        GL43C.glMultiDrawArraysIndirect(GL15C.GL_TRIANGLES, 0L, cellCount, CMD_BYTES);
        if (!offsetWas) GL11C.glDisable(GL11C.GL_POLYGON_OFFSET_FILL);
        GL11C.glEnable(GL11C.GL_CULL_FACE);
        GL30C.glBindVertexArray(0);
        logGlError("after multi draw");
        GL20C.glUseProgram(0);
    }

    private int errorsLogged;

    /** Logs the first few GL errors with the step that raised them (the vanilla debug message has no detail). */
    private void logGlError(String where) {
        int err;
        while ((err = GL11C.glGetError()) != GL11C.GL_NO_ERROR && errorsLogged < 8) {
            errorsLogged++;
            org.slf4j.LoggerFactory.getLogger("af9_lod").warn("GL error 0x{} {}", Integer.toHexString(err), where);
        }
    }

    public static ByteBuffer cellBuffer(int cells) {
        return ByteBuffer.allocateDirect(cells * CELL_BYTES).order(ByteOrder.nativeOrder());
    }

    private static int compile(int type, String file) {
        String src;
        try (InputStream in = LodRenderer.class.getResourceAsStream("/assets/" + Horizon.MOD_ID + "/shaders/" + file)) {
            if (in == null) throw new IllegalStateException("missing shader " + file);
            src = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        int s = GL20C.glCreateShader(type);
        GL20C.glShaderSource(s, src);
        GL20C.glCompileShader(s);
        if (GL20C.glGetShaderi(s, GL20C.GL_COMPILE_STATUS) == 0)
            throw new IllegalStateException(file + ": " + GL20C.glGetShaderInfoLog(s));
        return s;
    }

    private static int link(int... shaders) {
        int p = GL20C.glCreateProgram();
        for (int s : shaders) GL20C.glAttachShader(p, s);
        GL20C.glLinkProgram(p);
        if (GL20C.glGetProgrami(p, GL20C.GL_LINK_STATUS) == 0)
            throw new IllegalStateException("link: " + GL20C.glGetProgramInfoLog(p));
        for (int s : shaders) GL20C.glDeleteShader(s);
        return p;
    }

    @Override
    public void close() {
        GL20C.glDeleteProgram(cullProgram);
        GL20C.glDeleteProgram(drawProgram);
        GL15C.glDeleteBuffers(new int[] {cellBuf, cmdBuf, quadBuf});
        GL30C.glDeleteVertexArrays(vao);
    }
}
