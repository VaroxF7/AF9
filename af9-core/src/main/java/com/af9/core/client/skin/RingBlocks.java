package com.af9.core.client.skin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
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

    private static final class Mesh {

        long[] signature;
        float[] data = new float[1 << 14];
        int size;

        void vertex(float x, float y, float z, float nx, float ny, float nz, float u, float v, int light) {
            if (size + FLOATS > data.length) data = Arrays.copyOf(data, data.length * 2);
            data[size++] = x;
            data[size++] = y;
            data[size++] = z;
            data[size++] = nx;
            data[size++] = ny;
            data[size++] = nz;
            data[size++] = u;
            data[size++] = v;
            data[size++] = Float.intBitsToFloat(light);
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
            for (Direction face : FACES) {
                if (face != null) {
                    // a face against another block of the band is never seen
                    int dr = step(face, right), du = step(face, up), db = step(face, back);
                    if (present.contains(TowerDraw.key(cell.right() + dr, cell.up() + du, cell.back() + db))) continue;
                }
                RandomSource random = RandomSource.create(state.getSeed(cell.pos()));
                for (BakedQuad quad : model.getQuads(state, face, random, ModelData.EMPTY, null)) {
                    emit(mesh, quad, cell, level, origin, right, up, back, zc);
                }
            }
        }
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
        // the light of the air the face looks into (the block itself is opaque and dark)
        int light = LevelRenderer.getLightColor(level, cell.pos().relative(dir));
        float nx = dot(dir, right), ny = dot(dir, up), nz = dot(dir, back);
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
            mesh.vertex(x, y, z - zc, nx, ny, nz, Float.intBitsToFloat(v[o + 4]), Float.intBitsToFloat(v[o + 5]), light);
        }
    }

    private static float dot(Direction world, Direction axis) {
        return world.getStepX() * axis.getStepX() + world.getStepY() * axis.getStepY()
                + world.getStepZ() * axis.getStepZ();
    }

    /** Draws one band's blocks under the pose (the ring's turning pose, its centre at the origin). */
    static void draw(long controller, int band, VertexConsumer c, PoseStack.Pose pose) {
        Mesh[] meshes = MESHES.get(controller);
        if (meshes == null || band >= meshes.length || meshes[band] == null) return;
        Mesh mesh = meshes[band];
        float[] d = mesh.data;
        for (int i = 0; i < mesh.size; i += FLOATS) {
            c.vertex(pose.pose(), d[i], d[i + 1], d[i + 2]).color(1F, 1F, 1F, 1F).uv(d[i + 6], d[i + 7])
                    .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(Float.floatToRawIntBits(d[i + 8]))
                    .normal(pose.normal(), d[i + 3], d[i + 4], d[i + 5]).endVertex();
        }
    }
}
