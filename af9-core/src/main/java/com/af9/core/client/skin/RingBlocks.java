package com.af9.core.client.skin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.ModelData;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.Arrays;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The rings of the Orbital Array Mk2 as the real blocks: every block of a band keeps its own model (the casing's quads,
 * texture and all) and is drawn turning with the ring, the way GTNH's Godforge turns its ring. The blocks are baked once
 * (when the band's blocks change) into a flat vertex list in the ring's own frame, centre at the origin and lying in
 * the x/y plane, and replayed each frame under the ring's turning pose. A face against another block of the band is left
 * out, so the ring is a skin of its outer faces, the quads of the models exactly as the world draws them.
 */
final class RingBlocks {

    /** A block of a band: its place in the pattern's space (right, up, back from the controller) and in the world. */
    record Cell(int right, int up, int back, BlockPos pos, BlockState state) {}

    private static final int FLOATS = 9;

    /** The mesh is split by the angle round the ring's axis, so a frame draws only the stretches of the ring in view. */
    private static final int SECTORS = 64;

    private static final class Mesh {

        long[] signature;
        final float[][] data = new float[SECTORS][];
        final int[] size = new int[SECTORS];
        /** Per sector: the bounding sphere (centre and radius), in the ring's frame. */
        final float[] bounds = new float[SECTORS * 4];

        void quad(float[] q) {
            float cx = (q[0] + q[FLOATS] + q[2 * FLOATS] + q[3 * FLOATS]) / 4F;
            float cy = (q[1] + q[FLOATS + 1] + q[2 * FLOATS + 1] + q[3 * FLOATS + 1]) / 4F;
            int sector = Math.min(SECTORS - 1,
                    (int) ((Math.atan2(cy, cx) + Math.PI) / (2 * Math.PI) * SECTORS));
            float[] d = data[sector];
            int n = size[sector];
            if (d == null) data[sector] = d = new float[1 << 10];
            if (n + 4 * FLOATS > d.length) data[sector] = d = Arrays.copyOf(d, d.length * 2);
            System.arraycopy(q, 0, d, n, 4 * FLOATS);
            size[sector] = n + 4 * FLOATS;
        }

        /** Computes each sector's bounding sphere once all the quads are in. */
        void finish() {
            for (int sector = 0; sector < SECTORS; sector++) {
                float[] d = data[sector];
                if (d == null) continue;
                float minX = Float.MAX_VALUE, minY = minX, minZ = minX, maxX = -minX, maxY = -minX, maxZ = -minX;
                for (int i = 0; i < size[sector]; i += FLOATS) {
                    minX = Math.min(minX, d[i]);
                    maxX = Math.max(maxX, d[i]);
                    minY = Math.min(minY, d[i + 1]);
                    maxY = Math.max(maxY, d[i + 1]);
                    minZ = Math.min(minZ, d[i + 2]);
                    maxZ = Math.max(maxZ, d[i + 2]);
                }
                bounds[sector * 4] = (minX + maxX) / 2F;
                bounds[sector * 4 + 1] = (minY + maxY) / 2F;
                bounds[sector * 4 + 2] = (minZ + maxZ) / 2F;
                float hx = (maxX - minX) / 2F, hy = (maxY - minY) / 2F, hz = (maxZ - minZ) / 2F;
                bounds[sector * 4 + 3] = (float) Math.sqrt(hx * hx + hy * hy + hz * hz);
            }
        }
    }

    /** Per controller, per band: the baked blocks. Client thread only. */
    private static final Map<Long, Mesh[]> MESHES = new HashMap<>();

    private RingBlocks() {}

    static void forget(long controller) {
        MESHES.remove(controller);
    }

    static void clear() {
        MESHES.clear();
    }

    /**
     * Bakes one band's blocks when they changed. {@code zc} is the ring's centre along the back axis, {@code origin} the
     * controller block, and {@code right}, {@code up}, {@code back} the world directions of the pattern's axes.
     */
    static void update(long controller, int bands, int band, float zc, List<Cell> cells, ClientLevel level,
                       BlockPos origin, Direction right, Direction up, Direction back) {
        Mesh[] meshes = MESHES.computeIfAbsent(controller, key -> new Mesh[bands]);
        if (meshes.length != bands) {
            meshes = new Mesh[bands];
            MESHES.put(controller, meshes);
        }
        long[] signature = new long[cells.size()];
        for (int i = 0; i < signature.length; i++) {
            Cell cell = cells.get(i);
            signature[i] = cell.pos().asLong() * 31 + Block.getId(cell.state());
        }
        Arrays.sort(signature);
        if (meshes[band] != null && Arrays.equals(meshes[band].signature, signature)) return;
        Mesh mesh = new Mesh();
        mesh.signature = signature;

        Set<Long> present = new HashSet<>();
        for (Cell cell : cells) present.add(TowerDraw.key(cell.right(), cell.up(), cell.back()));
        var renderer = Minecraft.getInstance().getBlockRenderer();
        for (Cell cell : cells) {
            BlockState state = cell.state();
            if (state.getRenderShape() != RenderShape.MODEL) continue;
            BakedModel model = renderer.getBlockModel(state);
            // the model data of the block in its place, so connected-texture models pick their neighbours' variants
            ModelData data = model.getModelData(level, cell.pos(), state, ModelData.EMPTY);
            for (Direction face : FACES) {
                if (face != null) {
                    // a face against another block of the band is never seen
                    int dr = step(face, right), du = step(face, up), db = step(face, back);
                    if (present.contains(TowerDraw.key(cell.right() + dr, cell.up() + du, cell.back() + db))) continue;
                }
                RandomSource random = RandomSource.create(state.getSeed(cell.pos()));
                for (BakedQuad quad : model.getQuads(state, face, random, data, null)) {
                    emit(mesh, quad, cell, level, origin, right, up, back, zc);
                }
            }
        }
        mesh.finish();
        meshes[band] = mesh;
    }

    private static final Direction[] FACES = { null, Direction.DOWN, Direction.UP, Direction.NORTH, Direction.SOUTH,
            Direction.WEST, Direction.EAST };

    /** The step of a world direction along a pattern axis: 1, -1 or 0. */
    private static int step(Direction world, Direction axis) {
        return world == axis ? 1 : world == axis.getOpposite() ? -1 : 0;
    }

    private static void emit(Mesh mesh, BakedQuad quad, Cell cell, ClientLevel level, BlockPos origin, Direction right,
                             Direction up, Direction back, float zc) {
        int[] v = quad.getVertices();
        Direction dir = quad.getDirection();
        // the light of the air the face looks into; a solid block there (a spoke the beam replaces) would be black
        BlockPos lit = cell.pos().relative(dir);
        int light = level.getBlockState(lit).isSolidRender(level, lit) ? LightTexture.pack(0, 15)
                : LevelRenderer.getLightColor(level, lit);
        float nx = dot(dir, right), ny = dot(dir, up), nz = dot(dir, back);
        float[] q = new float[4 * FLOATS];
        for (int i = 0; i < 4; i++) {
            int o = i * 8;
            // the vertex in the world, relative to the controller block's middle, then onto the pattern's axes
            float wx = cell.pos().getX() + Float.intBitsToFloat(v[o]) - origin.getX() - 0.5F;
            float wy = cell.pos().getY() + Float.intBitsToFloat(v[o + 1]) - origin.getY() - 0.5F;
            float wz = cell.pos().getZ() + Float.intBitsToFloat(v[o + 2]) - origin.getZ() - 0.5F;
            float x = wx * right.getStepX() + wy * right.getStepY() + wz * right.getStepZ();
            float y = wx * up.getStepX() + wy * up.getStepY() + wz * up.getStepZ();
            float z = wx * back.getStepX() + wy * back.getStepY() + wz * back.getStepZ() + 0.5F;
            // about the ring's centre (the middle of the controller's column in x and y, zc along the axis)
            int k = i * FLOATS;
            q[k] = x;
            q[k + 1] = y;
            q[k + 2] = z - zc;
            q[k + 3] = nx;
            q[k + 4] = ny;
            q[k + 5] = nz;
            q[k + 6] = Float.intBitsToFloat(v[o + 4]);
            q[k + 7] = Float.intBitsToFloat(v[o + 5]);
            q[k + 8] = Float.intBitsToFloat(light);
        }
        mesh.quad(q);
    }

    private static float dot(Direction world, Direction axis) {
        return world.getStepX() * axis.getStepX() + world.getStepY() * axis.getStepY()
                + world.getStepZ() * axis.getStepZ();
    }

    /**
     * Draws one band's blocks under the pose (the ring's turning pose, its centre at the origin). The pose maps the ring's
     * frame to the view (camera) space, so a stretch of the ring that is behind the camera or well out of the view is
     * left out whole.
     */
    static void draw(long controller, int band, VertexConsumer c, PoseStack.Pose pose) {
        Mesh[] meshes = MESHES.get(controller);
        if (meshes == null || band >= meshes.length || meshes[band] == null) return;
        Mesh mesh = meshes[band];
        Vector3f view = new Vector3f();
        // the camera in the ring's frame (the origin of the view space), for leaving out the quads that face away
        Vector3f cam = new Matrix4f(pose.pose()).invert().transformPosition(new Vector3f());
        for (int sector = 0; sector < SECTORS; sector++) {
            float[] d = mesh.data[sector];
            if (d == null) continue;
            float r = mesh.bounds[sector * 4 + 3] + 1F;
            pose.pose().transformPosition(mesh.bounds[sector * 4], mesh.bounds[sector * 4 + 1],
                    mesh.bounds[sector * 4 + 2], view);
            // the camera looks down -z in view space: behind it, or beyond a wide view cone, nothing of it can show
            float ahead = -view.z + r;
            if (ahead < 0 || Math.abs(view.x) > ahead * 3.4F + r || Math.abs(view.y) > ahead * 2.2F + r) continue;
            for (int q = 0; q < mesh.size[sector]; q += 4 * FLOATS) {
                // a quad facing away from the camera is hidden by the ring's own blocks: a closed skin of outer faces
                if (d[q + 3] * (cam.x - d[q]) + d[q + 4] * (cam.y - d[q + 1]) + d[q + 5] * (cam.z - d[q + 2]) <= 0F) {
                    continue;
                }
                for (int i = q; i < q + 4 * FLOATS; i += FLOATS) {
                    c.vertex(pose.pose(), d[i], d[i + 1], d[i + 2]).color(1F, 1F, 1F, 1F).uv(d[i + 6], d[i + 7])
                            .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(Float.floatToRawIntBits(d[i + 8]))
                            .normal(pose.normal(), d[i + 3], d[i + 4], d[i + 5]).endVertex();
                }
            }
        }
    }
}
