package com.af9.lod;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Map;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Draft render hook: draws every loaded level-0 section through LodRenderer. Off unless the config enables it (or -Daf9lod.debugdraw=true with the master switch on),
 * because it re-draws what vanilla already shows. It proves the GPU pipeline (mesh -> SSBO -> compute cull -> indirect
 * draw); far terrain beyond the server's view distance needs persisted LOD data (next milestone).
 */
public final class LodRenderHook {
    private LodRenderer renderer;
    private boolean failed;
    private int builtVersion = -1;
    private int cellCount;
    private int[] cellSection = new int[0];   // sx, sy, sz per cell (3 ints each)

    @SubscribeEvent
    public void onStage(RenderLevelStageEvent event) {
        if (failed || !LodConfig.drawEnabled() || event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
        try {
            if (renderer == null) {
                if (!GlCaps.query().usable()) { failed = true; return; }
                renderer = new LodRenderer();
            }
            if (builtVersion != LodWorld.INSTANCE.version()) rebuild();
            if (cellCount == 0) return;

            Vector3f cam = event.getCamera().getPosition().toVector3f();
            ByteBuffer cells = LodRenderer.cellBuffer(cellCount).order(ByteOrder.nativeOrder());
            // cell layout (32 bytes): vec4(minPos.xyz camera-relative, edge), firstQuad, quadCount, level, pad
            for (int i = 0; i < cellCount; i++) {
                int base = i * LodRenderer.CELL_BYTES;
                cells.putFloat(base, cellSection[3 * i] * 16f - cam.x);
                cells.putFloat(base + 4, cellSection[3 * i + 1] * 16f - cam.y);
                cells.putFloat(base + 8, cellSection[3 * i + 2] * 16f - cam.z);
                cells.putFloat(base + 12, 16f);
                cells.putInt(base + 16, firstQuad[i]);
                cells.putInt(base + 20, quadCount[i]);
                cells.putInt(base + 24, 0);
            }
            renderer.updateCells(cells, cellCount);

            Matrix4f view = new Matrix4f(event.getPoseStack().last().pose());
            Matrix4f proj = new Matrix4f(event.getProjectionMatrix());
            float[] planes = frustumPlanes(new Matrix4f(proj).mul(view));
            float[] v = new float[16], p = new float[16];
            view.get(v);
            proj.get(p);
            renderer.render(planes, v, p, LodConfig.NEAR_BAND_CHUNKS.get() * 16f, LodConfig.MAX_DISTANCE_CHUNKS.get() * 16f);
        } catch (RuntimeException e) {
            failed = true;
            org.slf4j.LoggerFactory.getLogger("af9_lod").error("LOD render disabled after error", e);
        }
    }

    private int[] firstQuad = new int[0], quadCount = new int[0];

    private long lastRebuildNanos;

    private void rebuild() {
        // throttle: copying all meshes into one buffer is cheap, but not worth doing more than twice a second
        long now = System.nanoTime();
        if (builtVersion >= 0 && now - lastRebuildNanos < 500_000_000L) return;
        lastRebuildNanos = now;
        builtVersion = LodWorld.INSTANCE.version();
        Map<Long, int[]> meshes = LodWorld.INSTANCE.meshes();
        int n = meshes.size();
        cellSection = new int[3 * n];
        firstQuad = new int[n];
        quadCount = new int[n];
        int total = 0;
        for (int[] m : meshes.values()) total += m.length;
        ByteBuffer quads = ByteBuffer.allocateDirect(Math.max(total, 2) * 4).order(ByteOrder.nativeOrder());
        java.nio.IntBuffer ib = quads.asIntBuffer();
        int i = 0;
        for (Map.Entry<Long, int[]> e : meshes.entrySet()) {
            if (i >= n) break;   // map may have grown since size()
            long k = e.getKey();
            int[] q = e.getValue();
            if (ib.remaining() < q.length) break;
            cellSection[3 * i] = LodWorld.keyX(k);
            cellSection[3 * i + 1] = LodWorld.keyY(k);
            cellSection[3 * i + 2] = LodWorld.keyZ(k);
            firstQuad[i] = ib.position() / 2;
            quadCount[i] = q.length / 2;
            ib.put(q);
            i++;
        }
        cellCount = i;
        renderer.uploadQuads(quads, cellCount);
    }

    /** Gribb-Hartmann: 6 inward-pointing planes (left, right, bottom, top, near, far) from a combined matrix. */
    static float[] frustumPlanes(Matrix4f m) {
        float[] r0 = {m.m00(), m.m10(), m.m20(), m.m30()};
        float[] r1 = {m.m01(), m.m11(), m.m21(), m.m31()};
        float[] r2 = {m.m02(), m.m12(), m.m22(), m.m32()};
        float[] r3 = {m.m03(), m.m13(), m.m23(), m.m33()};
        float[] out = new float[24];
        for (int j = 0; j < 4; j++) {
            out[j] = r3[j] + r0[j];
            out[4 + j] = r3[j] - r0[j];
            out[8 + j] = r3[j] + r1[j];
            out[12 + j] = r3[j] - r1[j];
            out[16 + j] = r3[j] + r2[j];
            out[20 + j] = r3[j] - r2[j];
        }
        return out;
    }
}
