package com.af9.core.client.render;

import com.af9.core.AF9Core;
import com.af9.core.machine.EvaporationWellMachine;

import com.gregtechceu.gtceu.api.blockentity.MetaMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine;
import com.gregtechceu.gtceu.api.pattern.util.RelativeDirection;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The ECRW's custom models: while a well is formed, the stepped blocks of its wall ring, its rim and its pier and the
 * casing of its substation are not drawn and {@link EcrwArmRender} draws a smooth round wall, a round pier and a
 * distribution substation in their place (the casings' own textures).
 * <p>
 * A formed controller that the client knows from GT's synced state hides those blocks of its structure: their positions
 * go to {@link #isHidden}, which Embeddium's public per-block render hook asks while it builds a chunk, and the chunk is
 * rebuilt when the set changes. The machines stay (the controller, every hatch: the player works with them, they stand
 * out of the models) and so do the floor blocks. (Hatches are only in the substation against the pond, so the
 * ring is plain casing.) The blocks are only hidden when something takes their place: the
 * hook must be registered ({@link #registerEmbeddium}; without Embeddium the stepped blocks stay and the render draws
 * the pond without its round wall) and the controller must have a block entity renderer.
 */
public final class PondWalls {

    /**
     * The structure (see {@code kubejs/startup_scripts/gtceu/ecrw.js}): 11 wide, 13 deep with the substation (2 deep)
     * in front, the pond's centre 7 behind the controller.
     */
    private static final int HALF = 5, DEPTH = 13, CENTRE_BACK = 7;
    /** How many chunks round the player are searched for controllers, and how often (ticks). */
    private static final int SCAN_RADIUS = 8, SCAN_PERIOD = 10;

    /** Controller position (packed) -> the positions it hides. Client thread only. */
    private static final Map<Long, long[]> TRACKED = new HashMap<>();
    /** Every hidden position, replaced as a whole when it changes (the chunk builders read it from their threads). */
    private static volatile LongSet hidden = new LongOpenHashSet();
    private static volatile boolean hooked;
    private static ClientLevel trackedLevel;
    private static int ticks;

    private PondWalls() {}

    /** Client, at mod construction. */
    @SuppressWarnings("removal") // FMLJavaModLoadingContext.get() is the only way on 47.x
    public static void register() {
        FMLJavaModLoadingContext.get().getModEventBus()
                .addListener((FMLClientSetupEvent event) -> event.enqueueWork(PondWalls::registerEmbeddium));
        MinecraftForge.EVENT_BUS.addListener((TickEvent.ClientTickEvent event) -> onClientTick(event));
    }

    public static boolean anyHidden() {
        return !hidden.isEmpty();
    }

    public static boolean isHidden(long packedPos) {
        return hidden.contains(packedPos);
    }

    /** Whether this controller's round wall is the one being drawn (its blocks are hidden). */
    public static boolean isTracked(BlockPos controller) {
        return TRACKED.containsKey(controller.asLong());
    }

    /**
     * Whether the block {@code right} to the controller's right, {@code up} above it and {@code back} behind it is one
     * the custom models replace: the substation (5 wide, 2 deep, 2 high), the wall ring at the foot (outside
     * the floor disc), the wall and rim ring above it, the plus-shaped pier. The pattern's own shapes.
     */
    static boolean replaced(int right, int up, int back) {
        if (back <= 1 && Math.abs(right) <= 2 && up <= 1) return true;
        double r = Math.hypot(right, back - CENTRE_BACK);
        if (up == 0) return r > 4.5 && r <= 5.6;
        if (up <= 2 && r >= 4.4 && r <= 5.6) return true;
        int dz = back - CENTRE_BACK;
        return (right == 0 || dz == 0) && Math.abs(right) <= 1 && Math.abs(dz) <= 1;
    }

    private static BlockPos at(MetaMachine controller, int right, int up, int back) {
        Direction front = controller.getFrontFacing(), upwards = controller.getUpwardsFacing();
        Direction r = RelativeDirection.RIGHT.getRelative(front, upwards, false);
        Direction u = RelativeDirection.UP.getRelative(front, upwards, false);
        Direction b = RelativeDirection.BACK.getRelative(front, upwards, false);
        return controller.getPos().offset(
                r.getStepX() * right + u.getStepX() * up + b.getStepX() * back,
                r.getStepY() * right + u.getStepY() * up + b.getStepY() * back,
                r.getStepZ() * right + u.getStepZ() * up + b.getStepZ() * back);
    }

    // ---------------------------------------------------------------------------------------------------- tracking

    /** A formed well: hide its replaced blocks. */
    private static void track(ClientLevel level, MultiblockControllerMachine controller) {
        BlockPos origin = controller.getPos();
        LongArrayList positions = new LongArrayList();
        List<BlockPos> parts = Arrays.asList(controller.getPartPositions());
        for (int right = -HALF; right <= HALF; right++) {
            for (int up = 0; up <= 3; up++) {
                for (int back = 0; back < DEPTH; back++) {
                    if (!replaced(right, up, back)) continue;
                    BlockPos pos = at(controller, right, up, back);
                    if (pos.equals(origin) || parts.contains(pos)) continue;
                    if (level.getBlockState(pos).isAir()) continue;
                    // a machine of its own (a hatch the controller does not list yet) or any block entity stays drawn
                    if (MetaMachine.getMachine(level, pos) != null || level.getBlockEntity(pos) != null) continue;
                    positions.add(pos.asLong());
                }
            }
        }
        long[] now = positions.toLongArray();
        Arrays.sort(now);
        long[] before = TRACKED.put(origin.asLong(), now);
        if (before != null && Arrays.equals(before, now)) return;
        publish();
        dirty(before, now);
    }

    private static void untrack(long controller) {
        long[] before = TRACKED.remove(controller);
        if (before == null) return;
        publish();
        dirty(before, null);
    }

    private static void publish() {
        LongSet all = new LongOpenHashSet();
        for (long[] positions : TRACKED.values()) {
            for (long position : positions) all.add(position);
        }
        hidden = all;
    }

    /** The chunk sections holding the positions that changed are drawn again. */
    private static void dirty(long[] a, long[] b) {
        int[] box = { Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE,
                Integer.MIN_VALUE };
        for (long[] positions : new long[][] { a == null ? new long[0] : a, b == null ? new long[0] : b }) {
            for (long packed : positions) {
                BlockPos pos = BlockPos.of(packed);
                box[0] = Math.min(box[0], pos.getX());
                box[1] = Math.min(box[1], pos.getY());
                box[2] = Math.min(box[2], pos.getZ());
                box[3] = Math.max(box[3], pos.getX());
                box[4] = Math.max(box[4], pos.getY());
                box[5] = Math.max(box[5], pos.getZ());
            }
        }
        if (box[0] == Integer.MAX_VALUE) return;
        Minecraft.getInstance().levelRenderer.setBlocksDirty(box[0], box[1], box[2], box[3], box[4], box[5]);
    }

    private static void clear() {
        if (TRACKED.isEmpty()) return;
        TRACKED.clear();
        hidden = new LongOpenHashSet();
    }

    /** Forge bus, every client tick: looks for formed wells round the player and keeps what they hide current. */
    private static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (++ticks % SCAN_PERIOD != 0) return;
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null || mc.player == null) {
            clear();
            trackedLevel = null;
            return;
        }
        if (level != trackedLevel) {
            clear();
            trackedLevel = level;
        }
        if (!hooked) return;

        Set<Long> seen = new HashSet<>();
        ChunkPos centre = mc.player.chunkPosition();
        for (int cx = centre.x - SCAN_RADIUS; cx <= centre.x + SCAN_RADIUS; cx++) {
            for (int cz = centre.z - SCAN_RADIUS; cz <= centre.z + SCAN_RADIUS; cz++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
                if (chunk == null) continue;
                for (BlockEntity be : chunk.getBlockEntities().values()) {
                    if (!(be instanceof MetaMachineBlockEntity holder)) continue;
                    if (!(holder.getMetaMachine() instanceof EvaporationWellMachine well) || !well.isFormed()) continue;
                    // without a block entity renderer there is nothing to draw the round wall: keep the blocks
                    if (mc.getBlockEntityRenderDispatcher().getRenderer(be) == null) continue;
                    seen.add(be.getBlockPos().asLong());
                    track(level, well);
                }
            }
        }
        for (long controller : new ArrayList<>(TRACKED.keySet())) {
            if (!seen.contains(controller)) untrack(controller);
        }
    }

    // -------------------------------------------------------------------------------------------------- Embeddium

    /**
     * Embeddium's {@code BlockRendererRegistry}: for each block it builds a chunk mesh of, a populator may add a renderer
     * that takes the block over. The hidden blocks get one that draws nothing. The classes are named by reflection (no
     * compile dependency); without Embeddium the blocks stay drawn and no round wall is drawn.
     */
    static void registerEmbeddium() {
        try {
            Class<?> registry = Class.forName("org.embeddedt.embeddium.api.BlockRendererRegistry");
            Class<?> populatorType = Class.forName("org.embeddedt.embeddium.api.BlockRendererRegistry$RenderPopulator");
            Class<?> rendererType = Class.forName("org.embeddedt.embeddium.api.BlockRendererRegistry$Renderer");
            Class<?> resultType = Class.forName("org.embeddedt.embeddium.api.BlockRendererRegistry$RenderResult");
            Class<?> contextType = Class.forName(
                    "me.jellysquid.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderContext");
            @SuppressWarnings({ "unchecked", "rawtypes" })
            Object override = Enum.valueOf((Class<Enum>) resultType, "OVERRIDE");
            MethodHandle pos = MethodHandles.publicLookup().unreflect(contextType.getMethod("pos"));

            InvocationHandler drawsNothing = (proxy, method, args) -> {
                if (method.getName().equals("renderBlock")) return override;
                return objectMethod(proxy, method, args);
            };
            Object renderer = Proxy.newProxyInstance(PondWalls.class.getClassLoader(),
                    new Class<?>[] { rendererType }, drawsNothing);
            InvocationHandler populates = (proxy, method, args) -> {
                if (method.getName().equals("fillCustomRenderers")) {
                    if (hidden.isEmpty()) return null;
                    BlockPos at = (BlockPos) pos.invoke(args[1]);
                    if (hidden.contains(at.asLong())) {
                        @SuppressWarnings("unchecked")
                        List<Object> list = (List<Object>) args[0];
                        list.add(renderer);
                    }
                    return null;
                }
                return objectMethod(proxy, method, args);
            };
            Object populator = Proxy.newProxyInstance(PondWalls.class.getClassLoader(),
                    new Class<?>[] { populatorType }, populates);
            Object instance = registry.getMethod("instance").invoke(null);
            registry.getMethod("registerRenderPopulator", populatorType).invoke(instance, populator);
            hooked = true;
            AF9Core.LOGGER.info("Pond walls: hooked Embeddium's block render registry");
        } catch (ReflectiveOperationException | LinkageError | RuntimeException e) {
            AF9Core.LOGGER.info("Pond walls: no Embeddium block render registry ({}), the wall stays stepped",
                    e.toString());
        }
    }

    private static Object objectMethod(Object proxy, Method method, Object[] args) {
        return switch (method.getName()) {
            case "hashCode" -> System.identityHashCode(proxy);
            case "equals" -> proxy == args[0];
            case "toString" -> "AF9 pond walls hook";
            default -> null;
        };
    }
}
