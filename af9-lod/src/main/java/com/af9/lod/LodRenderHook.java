package com.af9.lod;

import com.af9.lod.core.LodGrid;
import com.af9.lod.core.LodMesher;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Draft render hook: draws every loaded level-0 section through LodRenderer. Off unless -Daf9lod.debugdraw=true,
 * because it re-draws what vanilla already shows. It proves the GPU pipeline (mesh -> SSBO -> compute cull -> indirect
 * draw); far terrain beyond the server's view distance needs persisted LOD data (next milestone).
 */
public final class LodRenderHook {
    static final boolean ENABLED = Boolean.getBoolean("af9lod.debugdraw");

    private LodRenderer renderer;
    private boolean failed;
    private int builtVersion = -1;
    private int cellCount;
    private long[] cellKeys = new long[0];
    private int[] cellSection = new int[0];   // sx, sy, sz per cell (3 ints each)

    @SubscribeEvent
    public void onStage(RenderLevelStageEvent event) {
        if (!ENABLED || failed || event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
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
            int renderDistBlocks = Minecraft.getInstance().options.getEffectiveRenderDistance() * 16;
            renderer.render(planes, v, p, 0f, renderDistBlocks * 4f);
        } catch (RuntimeException e) {
            failed = true;
            org.slf4j.LoggerFactory.getLogger("af9_lod").error("LOD render disabled after error", e);
        }
    }

    private int[] firstQuad = new int[0], quadCount = new int[0];

    private void rebuild() {
        builtVersion = LodWorld.INSTANCE.version();
        Map<Long, LodGrid> grids = LodWorld.INSTANCE.grids();
        int n = grids.size();
        cellKeys = new long[n];
        cellSection = new int[3 * n];
        firstQuad = new int[n];
        quadCount = new int[n];
        int[] all = new int[1024];
        int len = 0, i = 0;
        LodMesher mesher = new LodMesher();
        for (Map.Entry<Long, LodGrid> e : grids.entrySet()) {
            long k = e.getKey();
            int sx = (int) (k << 0 >> 42), sz = (int) ((k << 22) >> 42), sy = (int) ((k << 44) >> 44);
            int[] q = mesher.mesh(e.getValue(), null);
            if (len + q.length > all.length) all = java.util.Arrays.copyOf(all, Math.max(all.length * 2, len + q.length));
            System.arraycopy(q, 0, all, len, q.length);
            cellKeys[i] = k;
            cellSection[3 * i] = sx;
            cellSection[3 * i + 1] = sy;
            cellSection[3 * i + 2] = sz;
            firstQuad[i] = len / 2;
            quadCount[i] = q.length / 2;
            len += q.length;
            i++;
        }
        cellCount = i;
        ByteBuffer quads = ByteBuffer.allocateDirect(len * 4).order(ByteOrder.nativeOrder());
        quads.asIntBuffer().put(all, 0, len);
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
