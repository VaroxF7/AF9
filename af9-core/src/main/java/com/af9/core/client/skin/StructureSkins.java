package com.af9.core.client.skin;

import com.af9.core.AF9Core;
import com.af9.core.machine.OrbitalLithographyMachine;

import com.gregtechceu.gtceu.api.blockentity.MetaMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.MachineDefinition;
import com.gregtechceu.gtceu.api.registry.GTRegistries;
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
                    new int[] { -1, 1, 1 }, new float[] { 0.5F, 2.0F, -0.05F, 0.75F, 0.5F }),
            // the Photolithography Line (Mk1) and Scanner (Mk2): a white paneled cabinet, 3 wide, 3 high with the
            // controller in the middle row of the front, 10-12 long (found from the blocks, see measure)
            StructureSkin.cabinet(() -> machine("photolithography_line"), 1, 2, new StructureSkin.Cabinet(1, 10, 12)),
            StructureSkin.cabinet(() -> machine("photolithography_scanner"), 1, 2,
                    new StructureSkin.Cabinet(1, 10, 12)),
            // the Orbital Array Mk2's tower: its rings turn (only while the Mk2 size is switched on)
            StructureSkin.rings(() -> machine("orbital_lithography_station"),
                    new StructureSkin.Rings(OrbitalLithographyMachine.BANDS_MK2, OrbitalLithographyMachine.TOWER_HALF)));

    /** The blocks a lithography structure is made of (the bottom row of its middle column): the cabinet ends where none is. */
    private static final Set<String> CABINET_BLOCKS = Set.of("gtceu:plascrete", "af9:plascrete_filter_casing",
            "af9:plascrete_pipe_casing", "gtceu:steel_gearbox", "gtceu:stainless_steel_gearbox",
            "gtceu:solid_machine_casing", "gtceu:frostproof_machine_casing", "gtceu:cupronickel_coil_block",
            "gtceu:cleanroom_glass", "gtceu:tempered_glass");
    /** Blocks the cabinet leaves as they are: the windows on the exposure chamber. */
    private static final Set<String> CABINET_WINDOWS = Set.of("gtceu:cleanroom_glass", "gtceu:tempered_glass");
    /** Controller position (packed) -> the cabinet's length. Client thread only. */
    private static final Map<Long, Integer> LENGTHS = new HashMap<>();

    /** How many chunks round the player are searched for controllers, and how often (ticks). */
    private static final int SCAN_RADIUS = 16, SCAN_PERIOD = 10;

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

    private static MachineDefinition machine(String path) {
        return GTRegistries.MACHINES.get(new ResourceLocation("gtceu", path));
    }

    /** How long a formed cabinet machine is (0: not tracked). */
    public static int lengthOf(BlockPos controller) {
        return LENGTHS.getOrDefault(controller.asLong(), 0);
    }

    /**
     * The length of a lithography structure: the blocks of its bottom row, in the middle column, run back from the
     * controller's face for as long as they are blocks it is built of (or machines of its own).
     */
    private static int measure(ClientLevel level, MultiblockControllerMachine controller, StructureSkin skin) {
        StructureSkin.Cabinet cabinet = skin.cabinet();
        int length = 0;
        while (length < cabinet.maxLength()) {
            BlockPos pos = at(controller, 0, -cabinet.below(), length);
            var state = level.getBlockState(pos);
            ResourceLocation id = net.minecraftforge.registries.ForgeRegistries.BLOCKS.getKey(state.getBlock());
            boolean ours = id != null && CABINET_BLOCKS.contains(id.toString());
            if (!ours && MetaMachine.getMachine(level, pos) == null) break;
            length++;
        }
        return length >= cabinet.minLength() ? length : 0;
    }

    private static ResourceLocation model(String path) {
        return new ResourceLocation(AF9Core.MOD_ID, path);
    }

    public static boolean anyHidden() {
        return !hidden.isEmpty();
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
        if (skin.rings() != null) {
            trackRings(level, controller, skin, origin, parts, positions);
            return;
        }
        int firstUp = 0, depth = skin.back();
        if (skin.cabinet() != null) {
            firstUp = -skin.cabinet().below();
            depth = measure(level, controller, skin);
            if (depth == 0) {
                // not a structure this long (a wall behind it, say): keep the blocks
                LENGTHS.remove(origin.asLong());
                untrack(origin.asLong());
                return;
            }
            LENGTHS.put(origin.asLong(), depth);
        }
        for (int right = -skin.right(); right <= skin.right(); right++) {
            for (int up = firstUp; up < skin.up(); up++) {
                for (int back = 0; back < depth; back++) {
                    BlockPos pos = at(controller, right, up, back);
                    if (pos.equals(origin) || parts.contains(pos)) continue;
                    if (level.getBlockState(pos).isAir()) continue;
                    // anything that is a machine of its own (a hatch the controller does not list yet) or any block
                    // entity at all (another mod's part) stays drawn: the player works with it
                    if (MetaMachine.getMachine(level, pos) != null || level.getBlockEntity(pos) != null) continue;
                    // the cabinet leaves the front (controller side) and the top as the original blocks
                    if (skin.cabinet() != null && (back == 0 || up == skin.up() - 1)) continue;
                    if (skin.cabinet() != null && CABINET_WINDOWS.contains(String.valueOf(
                            net.minecraftforge.registries.ForgeRegistries.BLOCKS.getKey(
                                    level.getBlockState(pos).getBlock())))) {
                        continue;
                    }
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

    /**
     * The tower's rings: the blocks of every band and of the spokes to it hide (the rings are drawn turning, the beams
     * take the spokes' place), only on the Mk2 size. The blocks are the pattern's (photolithography.js, orbitalRingCell).
     */
    private static void trackRings(ClientLevel level, MultiblockControllerMachine controller, StructureSkin skin,
                                   BlockPos origin, List<BlockPos> parts, LongArrayList positions) {
        if (!(controller instanceof OrbitalLithographyMachine station) || !station.isExtended()) {
            untrack(origin.asLong());
            return;
        }
        // the rings are drawn in place of their blocks only once the whole tower stands
        Boolean built = ringsBuilt(level, controller, skin);
        if (built == null) {
            // a part of it is out of the loaded chunks: leave things as they are
            return;
        }
        if (!built) {
            untrack(origin.asLong());
            return;
        }
        var bands = skin.rings().bands();
        Direction front = controller.getFrontFacing(), upwards = controller.getUpwardsFacing();
        Direction axisRight = RelativeDirection.RIGHT.getRelative(front, upwards, false);
        Direction axisUp = RelativeDirection.UP.getRelative(front, upwards, false);
        Direction axisBack = RelativeDirection.BACK.getRelative(front, upwards, false);
        for (int bandIndex = 0; bandIndex < bands.size(); bandIndex++) {
            OrbitalLithographyMachine.Band band = bands.get(bandIndex);
            List<RingBlocks.Cell> ring = new ArrayList<>();
            int reach = band.radius() + 1, mid = band.d0() + band.t() / 2;
            for (int d = band.d0(); d < band.d0() + band.t(); d++) {
                for (int right = -reach; right <= reach; right++) {
                    for (int up = -reach; up <= reach; up++) {
                        double r = Math.sqrt(right * right + up * up);
                        boolean inBand = r >= band.radius() - band.width() && r <= band.radius() + 0.3;
                        // the spokes: the beam of shock-proof casing between rails along the axes, on the middle level
                        boolean spoke = d == mid && Math.max(Math.abs(right), Math.abs(up)) > skin.rings().towerHalf() && r < band.radius() - band.width()
                                && (right == 0 || up == 0 || Math.abs(right) == 1 || Math.abs(up) == 1);
                        if (!inBand && !spoke) continue;
                        BlockPos pos = at(controller, right, up, d);
                        if (pos.equals(origin) || parts.contains(pos)) continue;
                        if (level.getBlockState(pos).isAir()) continue;
                        if (MetaMachine.getMachine(level, pos) != null || level.getBlockEntity(pos) != null) continue;
                        positions.add(pos.asLong());
                        // the band's own blocks are drawn turning with their own models (the spokes' beams are plasma)
                        if (inBand) ring.add(new RingBlocks.Cell(right, up, d, pos, level.getBlockState(pos)));
                    }
                }
            }
            RingBlocks.update(origin.asLong(), bands.size(), bandIndex, band.d0() + band.t() / 2F, ring, level, origin,
                    axisRight, axisUp, axisBack);
        }
        trackTower(level, controller, skin, origin, parts, positions);
        long[] now = positions.toLongArray();
        Arrays.sort(now);
        MACHINES.put(origin.asLong(), controller);
        long[] before = TRACKED.put(origin.asLong(), now);
        if (before != null && Arrays.equals(before, now)) return;
        publish();
        dirty(before, now);
    }

    /** Half width of the box searched for the tower's blocks (its fins reach 17 out of the middle). */
    private static final int TOWER_BOX = 17;

    /**
     * The tower's own blocks hide too and are drawn as a white paneled shell ({@link TowerDraw}): every block in the
     * tower's box that is not a machine part or a block entity, bar the beams' spokes (the plasma takes those).
     */
    private static void trackTower(ClientLevel level, MultiblockControllerMachine controller, StructureSkin skin,
                                   BlockPos origin, List<BlockPos> parts, LongArrayList positions) {
        int half = skin.rings().towerHalf();
        int[] mids = skin.rings().bands().stream().mapToInt(band -> band.d0() + band.t() / 2).toArray();
        Map<Long, Byte> cells = new HashMap<>();
        Set<Long> blocked = new HashSet<>();
        for (int back = 0; back < OrbitalLithographyMachine.DEPTH_MK2 - 4; back++) {
            boolean beamLevel = false;
            for (int mid : mids) beamLevel |= mid == back;
            for (int right = -TOWER_BOX; right <= TOWER_BOX; right++) {
                for (int up = -TOWER_BOX; up <= TOWER_BOX; up++) {
                    BlockPos pos = at(controller, right, up, back);
                    var state = level.getBlockState(pos);
                    if (state.isAir()) continue;
                    if (inBand(skin, right, up, back)) continue;
                    long key = TowerDraw.key(right, up, back);
                    if (beamLevel && Math.max(Math.abs(right), Math.abs(up)) > half
                            && (right == 0 || up == 0 || Math.abs(right) == 1 || Math.abs(up) == 1)) {
                        continue;
                    }
                    if (pos.equals(origin) || parts.contains(pos) || MetaMachine.getMachine(level, pos) != null
                            || level.getBlockEntity(pos) != null) {
                        blocked.add(key);
                        continue;
                    }
                    String id = String.valueOf(net.minecraftforge.registries.ForgeRegistries.BLOCKS.getKey(
                            state.getBlock()));
                    cells.put(key, id.contains("coil_block") ? TowerDraw.LIGHT : id.contains("frame") ?
                            TowerDraw.FRAME : TowerDraw.PANEL);
                    positions.add(pos.asLong());
                }
            }
        }
        TowerDraw.update(origin.asLong(), cells, blocked);
    }

    /** Whether the cell is a ring's band (the ring skin draws those, not the tower's shell). */
    private static boolean inBand(StructureSkin skin, int right, int up, int back) {
        for (OrbitalLithographyMachine.Band band : skin.rings().bands()) {
            if (back < band.d0() || back >= band.d0() + band.t()) continue;
            if (Math.sqrt(right * right + up * up) >= band.radius() - band.width() - 0.5) return true;
        }
        return false;
    }

    /**
     * Whether the tower is standing, by samples: the tower's core column, each ring's collar and beam on the four sides and
     * the ring's band at the four points of the compass. {@code null} when a sample lies in a chunk that is not loaded.
     */
    private static Boolean ringsBuilt(ClientLevel level, MultiblockControllerMachine controller, StructureSkin skin) {
        List<BlockPos> samples = new ArrayList<>();
        int half = skin.rings().towerHalf();
        // the core column's frame, in the foot's chamber
        samples.add(at(controller, 0, 0, 90));
        for (OrbitalLithographyMachine.Band band : skin.rings().bands()) {
            int mid = band.d0() + band.t() / 2, mean = band.radius() - band.width() / 2;
            for (int[] side : new int[][] { { 1, 0 }, { -1, 0 }, { 0, 1 }, { 0, -1 } }) {
                samples.add(at(controller, side[0] * half, side[1] * half, mid));
                samples.add(at(controller, side[0] * (half + 1), side[1] * (half + 1), mid));
                samples.add(at(controller, side[0] * mean, side[1] * mean, band.d0()));
            }
        }
        for (BlockPos pos : samples) {
            if (!level.hasChunkAt(pos)) return null;
            if (level.getBlockState(pos).isAir()) return false;
        }
        return true;
    }

    private static void untrack(long controller) {
        RingDraw.forget(controller);
        RingBlocks.forget(controller);
        TowerDraw.forget(controller);
        MACHINES.remove(controller);
        LENGTHS.remove(controller);
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
        RingDraw.clear();
        RingBlocks.clear();
        TowerDraw.clear();
        hidden = new LongOpenHashSet();
    }

    //////////////////////////////////////
    // ************ Events ************//
    //////////////////////////////////////

    /** Mod bus: the skin's models are loaded though no blockstate names them. */
    public static void registerModels(ModelEvent.RegisterAdditional event) {
        for (StructureSkin skin : SKINS) {
            if (skin.cabinet() != null || skin.rings() != null) continue;
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
            if (skin.cabinet() != null || skin.rings() != null) {
                // drawn in code, nothing to bake
                ready.add(skin);
                continue;
            }
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
        RingDraw.carry(Minecraft.getInstance());
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
                    if (skin.rings() != null && !(controller instanceof OrbitalLithographyMachine o && o.isExtended())) {
                        continue;
                    }
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
