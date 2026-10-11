package com.af9.core.client.skin;

import com.af9.core.AF9Core;

import com.gregtechceu.gtceu.api.blockentity.MetaMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.MachineDefinition;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine;
import com.gregtechceu.gtceu.api.pattern.util.RelativeDirection;
import com.gregtechceu.gtceu.client.model.machine.MachineModel;
import com.gregtechceu.gtceu.common.data.machines.GTMultiMachines;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.event.TickEvent;

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
 * The formed multiblocks that are drawn as a model ({@link StructureSkin}): which they are, which blocks of them are not
 * drawn while formed, and the events that put the model in place.
 * <p>
 * A controller that is formed (the client knows it from GT's synced state) hides its structure's plain blocks: their
 * positions go to {@link #isHidden}, which Embeddium's public per-block render hook asks while it builds a chunk, and the
 * chunk is rebuilt when the set changes. The machine blocks stay (the controller and the hatches). The model itself is
 * a dynamic render of the controller ({@link StructureSkinRender}), added to the machine's baked model when models are
 * baked. The blocks are only hidden when the model is there to take their place: the controller must have a block
 * entity renderer and the model must have been attached.
 */
@SuppressWarnings("removal") // new ResourceLocation(ns, path) is the only constructor on 1.20.1
public final class StructureSkins {

    public static final List<StructureSkin> SKINS = List.of(
            // the Electric Blast Furnace: 3 wide, 4 high, 3 deep, the controller in the middle of the front, at the bottom
            new StructureSkin(() -> GTMultiMachines.ELECTRIC_BLAST_FURNACE, 1, 4, 3,
                    model("multiblock/ebf_lower"), model("multiblock/ebf_upper"),
                    model("multiblock/ebf_glow_lower"), model("multiblock/ebf_glow_upper"),
                    new int[] { -1, 1, 1 }));

    /** How many chunks round the player are searched for controllers, and how often (ticks). */
    private static final int SCAN_RADIUS = 8, SCAN_PERIOD = 10;

    /** Controller position (packed) -> the positions it hides. Client thread only. */
    private static final Map<Long, long[]> TRACKED = new HashMap<>();
    /** Every hidden position, replaced as a whole when it changes (the chunk builders read it from their threads). */
    private static volatile LongSet hidden = new LongOpenHashSet();
    private static ClientLevel trackedLevel;
    private static int ticks;
    /** Machine models that got the render: when none did, nothing is hidden. */
    private static int attached;

    private StructureSkins() {}

    private static ResourceLocation model(String path) {
        return new ResourceLocation(AF9Core.MOD_ID, path);
    }

    public static boolean isHidden(long packedPos) {
        return hidden.contains(packedPos);
    }

    public static boolean isTracked(BlockPos controller) {
        return TRACKED.containsKey(controller.asLong());
    }

    public static StructureSkin skinOf(MachineDefinition definition) {
        for (StructureSkin skin : SKINS) {
            if (skin.definition().get() == definition) return skin;
        }
        return null;
    }

    //////////////////////////////////////
    // ******** Structure space *******//
    //////////////////////////////////////

    /** The block of the pattern that is {@code right}, {@code up}, {@code back} from the controller. */
    public static BlockPos at(MetaMachine controller, int right, int up, int back) {
        Direction front = controller.getFrontFacing(), upwards = controller.getUpwardsFacing();
        Direction r = RelativeDirection.RIGHT.getRelative(front, upwards, false);
        Direction u = RelativeDirection.UP.getRelative(front, upwards, false);
        Direction b = RelativeDirection.BACK.getRelative(front, upwards, false);
        return controller.getPos().offset(
                r.getStepX() * right + u.getStepX() * up + b.getStepX() * back,
                r.getStepY() * right + u.getStepY() * up + b.getStepY() * back,
                r.getStepZ() * right + u.getStepZ() * up + b.getStepZ() * back);
    }

    //////////////////////////////////////
    // ********* Tracking *************//
    //////////////////////////////////////

    /** A formed controller with a skin: hide its plain blocks. */
    private static void track(ClientLevel level, MultiblockControllerMachine controller, StructureSkin skin) {
        BlockPos origin = controller.getPos();
        LongArrayList positions = new LongArrayList();
        List<BlockPos> parts = Arrays.asList(controller.getPartPositions());
        for (int right = -skin.right(); right <= skin.right(); right++) {
            for (int up = 0; up < skin.up(); up++) {
                for (int back = 0; back < skin.back(); back++) {
                    BlockPos pos = at(controller, right, up, back);
                    if (pos.equals(origin) || parts.contains(pos)) continue;
                    if (level.getBlockState(pos).isAir()) continue;
                    // anything that is a machine of its own (a hatch the controller does not list yet) stays
                    if (MetaMachine.getMachine(level, pos) != null) continue;
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

    /** Called by the skin's render when it sees the controller not formed any more. */
    static void untrack(BlockPos controller) {
        untrack(controller.asLong());
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

    //////////////////////////////////////
    // ************ Events ************//
    //////////////////////////////////////

    /** Mod bus: the skin's models are loaded though no blockstate names them. */
    public static void registerModels(ModelEvent.RegisterAdditional event) {
        for (StructureSkin skin : SKINS) {
            event.register(skin.lower());
            event.register(skin.upper());
            event.register(skin.glowLower());
            event.register(skin.glowUpper());
        }
    }

    /** Mod bus: the controller's machine model gets the dynamic render that draws the skin. */
    public static void attachRenders(ModelEvent.ModifyBakingResult event) {
        attached = 0;
        StructureSkinRender.clearCaches();
        for (BakedModel baked : event.getModels().values()) {
            if (!(baked instanceof MachineModel machineModel)) continue;
            if (skinOf(machineModel.getDefinition()) == null) continue;
            boolean has = false;
            for (var render : machineModel.getDynamicRenders()) {
                if (render instanceof StructureSkinRender) {
                    has = true;
                    break;
                }
            }
            if (!has) {
                StructureSkinRender render = new StructureSkinRender();
                render.setParent(machineModel);
                machineModel.getDynamicRenders().add(render);
            }
            attached++;
        }
    }

    /** Forge bus, every client tick: looks for formed controllers round the player and keeps what they hide current. */
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || ++ticks % SCAN_PERIOD != 0) return;
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
        if (attached == 0) return;

        Set<Long> seen = new HashSet<>();
        ChunkPos centre = mc.player.chunkPosition();
        for (int cx = centre.x - SCAN_RADIUS; cx <= centre.x + SCAN_RADIUS; cx++) {
            for (int cz = centre.z - SCAN_RADIUS; cz <= centre.z + SCAN_RADIUS; cz++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
                if (chunk == null) continue;
                for (BlockEntity be : chunk.getBlockEntities().values()) {
                    if (!(be instanceof MetaMachineBlockEntity holder)) continue;
                    if (!(holder.getMetaMachine() instanceof MultiblockControllerMachine controller)) continue;
                    StructureSkin skin = skinOf(controller.getDefinition());
                    if (skin == null || !controller.isFormed()) continue;
                    // without a block entity renderer there is nothing to draw the model: keep the blocks
                    if (mc.getBlockEntityRenderDispatcher().getRenderer(be) == null) continue;
                    seen.add(be.getBlockPos().asLong());
                    track(level, controller, skin);
                }
            }
        }
        for (long controller : new ArrayList<>(TRACKED.keySet())) {
            if (!seen.contains(controller)) untrack(controller);
        }
    }

    //////////////////////////////////////
    // ********** Embeddium ***********//
    //////////////////////////////////////

    /**
     * Embeddium's {@code BlockRendererRegistry}: for each block it builds a chunk mesh of, a populator may add a renderer
     * that takes the block over. The hidden blocks get one that draws nothing. The classes are named by reflection (no
     * compile dependency); without Embeddium the blocks stay drawn under the model.
     */
    public static void registerEmbeddium() {
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
            Object renderer = Proxy.newProxyInstance(StructureSkins.class.getClassLoader(),
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
            Object populator = Proxy.newProxyInstance(StructureSkins.class.getClassLoader(),
                    new Class<?>[] { populatorType }, populates);
            Object instance = registry.getMethod("instance").invoke(null);
            registry.getMethod("registerRenderPopulator", populatorType).invoke(instance, populator);
            AF9Core.LOGGER.info("Structure skins: hooked Embeddium's block render registry");
        } catch (ReflectiveOperationException | LinkageError | RuntimeException e) {
            AF9Core.LOGGER.info("Structure skins: no Embeddium block render registry ({}), blocks stay drawn",
                    e.toString());
        }
    }

    private static Object objectMethod(Object proxy, Method method, Object[] args) {
        return switch (method.getName()) {
            case "hashCode" -> System.identityHashCode(proxy);
            case "equals" -> proxy == args[0];
            case "toString" -> "AF9 structure skin hook";
            default -> null;
        };
    }
}
