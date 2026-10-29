package com.af9.core.pattern;

import com.gregtechceu.gtceu.api.blockentity.PipeBlockEntity;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiController;
import com.gregtechceu.gtceu.api.pattern.TraceabilityPredicate;
import com.gregtechceu.gtceu.api.pattern.error.PatternStringError;

import com.lowdragmc.lowdraglib.utils.BlockInfo;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

/**
 * The real GT pipes of the Distillation Facility (kubejs/startup_scripts/gtceu/distillation_facility.js), the way the
 * Chemical Plant has them ({@link ChemicalPlantPipes}): the pattern asks for exactly this pipe block with exactly this
 * {@code connections} mask at each spot, as in Janne's structure file (v3.nbt), and the controller sets the masks itself
 * while the facility is not formed, so an auto-built facility (which places plain pipes) ends up connected the way the file
 * has it. The file's 9 framed pipes are plain pipes here: the frame is not asked for.
 * <p>
 * The masks are written for a controller facing SOUTH (the file as it stands in the world); {@link ChemicalPlantPipes#rotate}
 * turns them for the other facings. The positions are the file's coordinates, the controller stands at {@link #CX},
 * {@link #CY}, {@link #CZ}. The pipes are marked with {@link ChemicalPlantPipes#LOCK_TAG}, so {@code PipeBlockMixin} keeps their
 * connections as they are.
 */
public final class DistillationFacilityPipes {

    /** The controller's spot in the file's coordinates (where the green wool stood). */
    public static final int CX = 4, CY = 2, CZ = 13;

    /** {@code frame}: the file has the pipe in a HSLA Steel Frame (not asked for, kept as a note of the file). */
    public record Pipe(int x, int y, int z, String block, int mask, boolean frame) {}

    /** Every pipe of the file. */
    public static final List<Pipe> PIPES = List.of(
            new Pipe(6, 3, 2, "gtceu:tungsten_small_fluid_pipe", 3, false),
            new Pipe(3, 4, 2, "gtceu:tungsten_small_fluid_pipe", 48, false),
            new Pipe(4, 4, 2, "gtceu:tungsten_small_fluid_pipe", 48, true),
            new Pipe(5, 4, 2, "gtceu:tungsten_small_fluid_pipe", 48, false),
            new Pipe(6, 4, 2, "gtceu:tungsten_small_fluid_pipe", 17, false),
            new Pipe(6, 3, 3, "gtceu:stainless_steel_small_fluid_pipe", 3, false),
            new Pipe(3, 4, 3, "gtceu:stainless_steel_small_fluid_pipe", 48, false),
            new Pipe(4, 4, 3, "gtceu:stainless_steel_small_fluid_pipe", 48, true),
            new Pipe(5, 4, 3, "gtceu:stainless_steel_small_fluid_pipe", 48, false),
            new Pipe(6, 4, 3, "gtceu:stainless_steel_small_fluid_pipe", 17, false),
            new Pipe(6, 3, 4, "gtceu:stainless_steel_small_fluid_pipe", 3, false),
            new Pipe(3, 4, 4, "gtceu:stainless_steel_small_fluid_pipe", 48, false),
            new Pipe(4, 4, 4, "gtceu:stainless_steel_small_fluid_pipe", 48, true),
            new Pipe(5, 4, 4, "gtceu:stainless_steel_small_fluid_pipe", 48, false),
            new Pipe(6, 4, 4, "gtceu:stainless_steel_small_fluid_pipe", 17, false),
            new Pipe(6, 3, 6, "gtceu:stainless_steel_small_fluid_pipe", 3, false),
            new Pipe(3, 4, 6, "gtceu:stainless_steel_small_fluid_pipe", 34, false),
            new Pipe(4, 4, 6, "gtceu:stainless_steel_small_fluid_pipe", 48, true),
            new Pipe(5, 4, 6, "gtceu:stainless_steel_small_fluid_pipe", 48, false),
            new Pipe(6, 4, 6, "gtceu:stainless_steel_small_fluid_pipe", 17, false),
            new Pipe(3, 5, 6, "gtceu:stainless_steel_small_fluid_pipe", 17, false),
            new Pipe(6, 3, 7, "gtceu:tungsten_small_fluid_pipe", 3, false),
            new Pipe(3, 4, 7, "gtceu:tungsten_small_fluid_pipe", 48, false),
            new Pipe(4, 4, 7, "gtceu:tungsten_small_fluid_pipe", 48, true),
            new Pipe(5, 4, 7, "gtceu:tungsten_small_fluid_pipe", 48, false),
            new Pipe(6, 4, 7, "gtceu:tungsten_small_fluid_pipe", 17, false),
            new Pipe(6, 3, 8, "gtceu:stainless_steel_small_fluid_pipe", 3, false),
            new Pipe(3, 4, 8, "gtceu:stainless_steel_small_fluid_pipe", 48, false),
            new Pipe(4, 4, 8, "gtceu:stainless_steel_small_fluid_pipe", 48, true),
            new Pipe(5, 4, 8, "gtceu:stainless_steel_small_fluid_pipe", 48, false),
            new Pipe(6, 4, 8, "gtceu:stainless_steel_small_fluid_pipe", 17, false),
            new Pipe(6, 3, 10, "gtceu:stainless_steel_small_fluid_pipe", 3, false),
            new Pipe(3, 4, 10, "gtceu:stainless_steel_small_fluid_pipe", 48, false),
            new Pipe(4, 4, 10, "gtceu:stainless_steel_small_fluid_pipe", 48, true),
            new Pipe(5, 4, 10, "gtceu:stainless_steel_small_fluid_pipe", 48, false),
            new Pipe(6, 4, 10, "gtceu:stainless_steel_small_fluid_pipe", 17, false),
            new Pipe(6, 3, 11, "gtceu:tungsten_small_fluid_pipe", 3, false),
            new Pipe(6, 4, 11, "gtceu:tungsten_small_fluid_pipe", 3, false),
            new Pipe(3, 5, 11, "gtceu:tungsten_small_fluid_pipe", 48, false),
            new Pipe(4, 5, 11, "gtceu:tungsten_small_fluid_pipe", 48, true),
            new Pipe(5, 5, 11, "gtceu:tungsten_small_fluid_pipe", 48, false),
            new Pipe(6, 5, 11, "gtceu:tungsten_small_fluid_pipe", 17, false),
            new Pipe(6, 3, 12, "gtceu:tungsten_small_fluid_pipe", 3, false),
            new Pipe(3, 4, 12, "gtceu:tungsten_small_fluid_pipe", 48, false),
            new Pipe(4, 4, 12, "gtceu:tungsten_small_fluid_pipe", 48, true),
            new Pipe(5, 4, 12, "gtceu:tungsten_small_fluid_pipe", 48, false),
            new Pipe(6, 4, 12, "gtceu:tungsten_small_fluid_pipe", 17, false)
    );

    private DistillationFacilityPipes() {}

    /** Where a pipe of the file stands in the world for a controller at {@code controller} facing {@code facing}. */
    public static BlockPos worldPos(BlockPos controller, Direction facing, Pipe pipe) {
        int dx = pipe.x() - CX, dz = pipe.z() - CZ;
        for (int i = 0; i < facing.get2DDataValue(); i++) {
            int tmp = dx;
            dx = -dz;
            dz = tmp;
        }
        return controller.offset(dx, pipe.y() - CY, dz);
    }

    @SuppressWarnings("removal") // new ResourceLocation(String) is the only constructor on 1.20.1
    private static Block block(String id) {
        return ForgeRegistries.BLOCKS.getValue(new ResourceLocation(id));
    }

    /** This pipe block with exactly this connection mask (file frame), for the pattern. */
    public static TraceabilityPredicate pipe(String blockId, int mask) {
        return pipe(blockId, mask, false);
    }

    /** The same; {@code frame} is the file's note of a framed pipe and is not checked (a framed or a plain pipe both form). */
    public static TraceabilityPredicate pipe(String blockId, int mask, boolean frame) {
        return new TraceabilityPredicate(state -> {
            if (!state.getBlockState().is(block(blockId))) return false;
            if (!(state.getTileEntity() instanceof PipeBlockEntity<?, ?> pipe)) return false;
            IMultiController controller = state.getController();
            if (controller == null) return false;
            if (pipe.getConnections() != ChemicalPlantPipes.rotate(mask, controller.self().getFrontFacing())) {
                state.setError(new PatternStringError("af9.distillation_facility.pipe_connections"));
                return false;
            }
            // (a frame box on the pipe is not asked for: the file's 9 framed pipes are plain pipes here)
            return true;
        }, () -> new BlockInfo[] { BlockInfo.fromBlockState(block(blockId).defaultBlockState()) });
    }

    /**
     * The structure preview's touch-up ({@code PatternPreviewWidget.PREVIEW_HOOKS}, fork patch 0028): the preview's blocks carry no
     * block entity data, so the pipes get their connections here, before the preview checks the structure.
     */
    public static void previewHook(IMultiController controller, net.minecraft.world.level.Level level) {
        if (!(controller instanceof com.af9.core.distillation.DistillationFacilityMachine)) return;
        BlockPos at = controller.self().getPos();
        Direction facing = controller.self().getFrontFacing();
        for (Pipe spot : PIPES) {
            BlockPos pos = worldPos(at, facing, spot);
            if (!(level.getBlockEntity(pos) instanceof PipeBlockEntity<?, ?> pipe)) continue;
            // setConnection does nothing on a client level (the preview world is one), so the mask is set as it is
            pipe.setConnections(ChemicalPlantPipes.rotate(spot.mask(), facing));
        }
    }

    /**
     * What is wrong at the pipe spots of the file (at most {@code limit} lines, as "x y z: what"): a controller shows these while the
     * facility is not formed, so a pattern that does not form says which pipe it is.
     */
    public static List<String> problems(Level level, BlockPos controller, Direction facing, int limit) {
        List<String> out = new ArrayList<>();
        for (Pipe spot : PIPES) {
            if (out.size() >= limit) break;
            BlockPos pos = worldPos(controller, facing, spot);
            String where = pos.getX() + " " + pos.getY() + " " + pos.getZ() + ": ";
            if (!level.isLoaded(pos)) {
                out.add(where + "not loaded");
                continue;
            }
            Block want = block(spot.block());
            if (!level.getBlockState(pos).is(want)) {
                out.add(where + "needs " + spot.block() + ", has " + ForgeRegistries.BLOCKS.getKey(level.getBlockState(pos).getBlock()));
            } else if (!(level.getBlockEntity(pos) instanceof PipeBlockEntity<?, ?> pipe)) {
                out.add(where + "no pipe block entity");
            } else if (pipe.getConnections() != ChemicalPlantPipes.rotate(spot.mask(), facing)) {
                out.add(where + "connections " + pipe.getConnections() + ", want " + ChemicalPlantPipes.rotate(spot.mask(), facing));
            }
        }
        return out;
    }

    /** Marks every right pipe that stands where the file has one, then sets their connections; true if anything changed. */
    public static boolean repair(Level level, BlockPos controller, Direction facing) {
        boolean changed = false;
        // mark them all first, so no neighbour update caused by the first pipe's change trims the next one
        List<PipeBlockEntity<?, ?>> found = new ArrayList<>();
        List<Pipe> spots = new ArrayList<>();
        for (Pipe spot : PIPES) {
            BlockPos pos = worldPos(controller, facing, spot);
            if (!level.isLoaded(pos) || !level.getBlockState(pos).is(block(spot.block()))) continue;
            if (!(level.getBlockEntity(pos) instanceof PipeBlockEntity<?, ?> pipe)) continue;
            if (!pipe.getPersistentData().getBoolean(ChemicalPlantPipes.LOCK_TAG)) {
                pipe.getPersistentData().putBoolean(ChemicalPlantPipes.LOCK_TAG, true);
                pipe.setChanged();
            }
            found.add(pipe);
            spots.add(spot);
        }
        for (int i = 0; i < found.size(); i++) {
            PipeBlockEntity<?, ?> pipe = found.get(i);
            int want = ChemicalPlantPipes.rotate(spots.get(i).mask(), facing);
            for (Direction side : Direction.values()) {
                boolean open = (want & (1 << side.ordinal())) != 0;
                if (pipe.isConnected(side) != open) {
                    pipe.setConnection(side, open, false);
                    changed = true;
                }
            }
        }
        return changed;
    }
}
