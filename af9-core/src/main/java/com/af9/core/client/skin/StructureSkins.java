package com.af9.core.client.skin;

import com.af9.core.AF9Core;

import com.gregtechceu.gtceu.api.blockentity.MetaMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.MachineDefinition;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.feature.IRecipeLogicMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine;
import com.gregtechceu.gtceu.api.pattern.util.RelativeDirection;
import com.gregtechceu.gtceu.client.model.machine.MachineModel;
import com.gregtechceu.gtceu.common.data.machines.GTMultiMachines;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
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
                    new int[] { -1, 1, 1 }, new float[] { 0.5F, 2.0F, -0.05F, 0.75F, 0.5F }));

    /** How many chunks round the player are searched for controllers, and how often (ticks). */
    private static final int SCAN_RADIUS = 8, SCAN_PERIOD = 10;

    /** Controller position (packed) -> the positions it hides. Client thread only. */
    private static final Map<Long, long[]> TRACKED = new HashMap<>();
    /** The tracked controllers, for the flames at the window. */
    private static final Map<Long, MultiblockControllerMachine> MACHINES = new HashMap<>();
    /** Every hidden position, replaced as a whole when it changes (the chunk builders read it from their threads). */
    private static volatile LongSet hidden = new LongOpenHashSet();
    private static ClientLevel trackedLevel;
    private static int ticks;
    /** Machine models that got the render: when none did, nothing is hidden. */
    private static int attached;
    /** The skins whose models baked. */
    private static volatile Set<StructureSkin> READY = Set.of();

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
                    // anything that is a machine of its own (a hatch the controller does not list yet) or any block
                    // entity at all (another mod's part) stays drawn: the player works with it
                    if (MetaMachine.getMachine(level, pos) != null || level.getBlockEntity(pos) != null) continue;
                    positions.add(pos.asLong());
                }
            }
        }
        long[] now = positions.toLongArray();
        Arrays.sort(now);
        MACHINES.put(origin.asLong(), controller);
        long[] before = TRACKED.put(origin.asLong(), now);
        if (before != null && Arrays.equals(before, now)) return;
        publish();
        dirty(before, now);
    }

    private static void untrack(long controller) {
        MACHINES.remove(controller);
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
        MACHINES.clear();
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
        // the skin's models must have baked (a model that did not load is the missing-model cube: hiding the blocks
        // under it would leave a hole)
        BakedModel missing = event.getModels().get(ModelBakery.MISSING_MODEL_LOCATION);
        Set<StructureSkin> ready = new HashSet<>();
        for (StructureSkin skin : SKINS) {
            boolean ok = true;
            for (ResourceLocation location : new ResourceLocation[] { skin.lower(), skin.upper(), skin.glowLower(),
                    skin.glowUpper() }) {
                BakedModel model = event.getModels().get(location);
                if (model == null || model == missing) {
                    AF9Core.LOGGER.warn("Structure skins: model {} did not load, {} keeps its blocks", location,
                            skin.definition().get().getId());
                    ok = false;
                }
            }
            if (ok) ready.add(skin);
        }
        READY = ready;
        for (BakedModel baked : event.getModels().values()) {
            if (baked instanceof MachineModel machineModel && attach(machineModel)) attached++;
        }
        AF9Core.LOGGER.info("Structure skins: {} of {} skin(s) ready, the render attached to {} machine model(s)",
                ready.size(), SKINS.size(), attached);
    }

    /**
     * Puts the skin's render on a controller's machine model (once). With dynamic model loading (ModernFix) the machine
     * models are not in the baking result, so the tick does this for the models it meets.
     */
    private static boolean attach(MachineModel machineModel) {
        StructureSkin skin = skinOf(machineModel.getDefinition());
        if (skin == null || !READY.contains(skin)) return false;
        for (var render : machineModel.getDynamicRenders()) {
            if (render instanceof StructureSkinRender) return true;
        }
        StructureSkinRender render = new StructureSkinRender();
        render.setParent(machineModel);
        machineModel.getDynamicRenders().add(render);
        return true;
    }

    /** Forge bus, every client tick: looks for formed controllers round the player and keeps what they hide current. */
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        flames(Minecraft.getInstance());
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
        if (READY.isEmpty()) return;

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
                    // the model the block draws with gets the render now (it may have been loaded after the baking)
                    if (!(mc.getBlockRenderer().getBlockModel(be.getBlockState()) instanceof MachineModel model)
                            || !attach(model)) {
                        continue;
                    }
                    seen.add(be.getBlockPos().asLong());
                    track(level, controller, skin);
                }
            }
        }
        for (long controller : new ArrayList<>(TRACKED.keySet())) {
            if (!seen.contains(controller)) untrack(controller);
        }
    }

    /**
     * While a tracked machine works, flames and sparks come out of its furnace window (the muffler hatch, a real block,
     * gives its own smoke). Only near the player.
     */
    private static void flames(Minecraft mc) {
        ClientLevel level = mc.level;
        if (level == null || mc.player == null || MACHINES.isEmpty()) return;
        RandomSource random = level.random;
        for (MultiblockControllerMachine controller : MACHINES.values()) {
            StructureSkin skin = skinOf(controller.getDefinition());
            if (skin == null || skin.window() == null || controller.isInValid()) continue;
            if (!(controller instanceof IRecipeLogicMachine logic) || !logic.isActive()) continue;
            BlockPos origin = controller.getPos();
            if (origin.distSqr(mc.player.blockPosition()) > 32 * 32) continue;
            Direction front = controller.getFrontFacing(), upwards = controller.getUpwardsFacing();
            Direction r = RelativeDirection.RIGHT.getRelative(front, upwards, false);
            Direction u = RelativeDirection.UP.getRelative(front, upwards, false);
            Direction b = RelativeDirection.BACK.getRelative(front, upwards, false);
            float[] w = skin.window();
            // model space -> world: the model's origin is the controller block's corner, turned about its middle
            for (int n = 0; n < 2; n++) {
                if (random.nextFloat() > 0.55F) continue;
                float mx = w[0] + (random.nextFloat() * 2F - 1F) * w[3];
                float my = w[1] + (random.nextFloat() * 2F - 1F) * w[4];
                float mz = w[2];
                double x = origin.getX() + 0.5 + r.getStepX() * (mx - 0.5) + u.getStepX() * (my - 0.5)
                        + b.getStepX() * (mz - 0.5);
                double y = origin.getY() + 0.5 + r.getStepY() * (mx - 0.5) + u.getStepY() * (my - 0.5)
                        + b.getStepY() * (mz - 0.5);
                double z = origin.getZ() + 0.5 + r.getStepZ() * (mx - 0.5) + u.getStepZ() * (my - 0.5)
                        + b.getStepZ() * (mz - 0.5);
                // out of the front, a little up
                double vx = -b.getStepX() * 0.03 + u.getStepX() * 0.01;
                double vy = -b.getStepY() * 0.03 + u.getStepY() * 0.01;
                double vz = -b.getStepZ() * 0.03 + u.getStepZ() * 0.01;
                float pick = random.nextFloat();
                if (pick < 0.7F) {
                    level.addParticle(ParticleTypes.FLAME, x, y, z, vx, vy + 0.01, vz);
                } else if (pick < 0.92F) {
                    level.addParticle(ParticleTypes.SMOKE, x, y, z, vx, vy + 0.02, vz);
                } else {
                    level.addParticle(ParticleTypes.LAVA, x, y, z, 0, 0, 0);
                }
            }
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
