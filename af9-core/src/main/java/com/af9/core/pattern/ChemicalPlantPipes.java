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
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;

/**
 * The real GT pipes of the Chemical Plant (kubejs/startup_scripts/gtceu/chemical_plant.js): the pattern asks for exactly
 * this pipe block with exactly this {@code connections} mask at each spot, as in Janne's structure file, and a controller
 * that is not formed yet sets the masks itself ({@link #repair}), so an auto-built plant (which places plain pipes)
 * ends up connected the way the file has it.
 * <p>
 * The masks are the file's: bit {@code 1 << Direction.ordinal()}, written for a controller facing SOUTH (the file as it
 * stands in the world, x east, z south, the controller in the last aisle); {@link #rotate} turns them for the other
 * three facings. The positions are the file's coordinates; the controller stands at {@link #CX}, {@link #CY}, {@link #CZ}.
 */
public final class ChemicalPlantPipes {

    /** The controller's spot in the file's coordinates. */
    public static final int CX = 3, CY = 1, CZ = 3;

    public record Pipe(int x, int y, int z, String block, int mask) {}

    /** Every pipe of the file. */
    public static final List<Pipe> PIPES = List.of(
            new Pipe(1, 2, 0, "gtceu:bronze_huge_fluid_pipe", 3),
            new Pipe(1, 3, 0, "gtceu:bronze_huge_fluid_pipe", 3),
            new Pipe(1, 4, 0, "gtceu:bronze_huge_fluid_pipe", 33),
            new Pipe(2, 4, 0, "gtceu:bronze_huge_fluid_pipe", 50),
            new Pipe(3, 4, 0, "gtceu:bronze_huge_fluid_pipe", 48),
            new Pipe(4, 4, 0, "gtceu:bronze_huge_fluid_pipe", 24),
            new Pipe(2, 5, 0, "gtceu:stainless_steel_quadruple_fluid_pipe", 9),
            new Pipe(2, 2, 1, "gtceu:niobium_titanium_huge_fluid_pipe", 9),
            new Pipe(4, 4, 1, "gtceu:bronze_huge_fluid_pipe", 12),
            new Pipe(2, 5, 1, "gtceu:stainless_steel_quadruple_fluid_pipe", 12),
            new Pipe(2, 2, 2, "gtceu:stainless_steel_quadruple_fluid_pipe", 7),
            new Pipe(4, 2, 2, "gtceu:bronze_huge_fluid_pipe", 3),
            new Pipe(2, 3, 2, "gtceu:stainless_steel_quadruple_fluid_pipe", 3),
            new Pipe(4, 3, 2, "gtceu:bronze_huge_fluid_pipe", 3),
            new Pipe(2, 4, 2, "gtceu:stainless_steel_quadruple_fluid_pipe", 3),
            new Pipe(4, 4, 2, "gtceu:bronze_huge_fluid_pipe", 5),
            new Pipe(2, 5, 2, "gtceu:stainless_steel_quadruple_fluid_pipe", 5),
    );

    private ChemicalPlantPipes() {}

    /** The mask turned from the file's frame (controller facing SOUTH) to a controller facing {@code facing}. */
    public static int rotate(int mask, Direction facing) {
        int turns = facing.get2DDataValue();
        if (turns == 0) return mask;
        int out = 0;
        for (Direction side : Direction.values()) {
            if ((mask & (1 << side.ordinal())) == 0) continue;
            Direction turned = side;
            if (side.getAxis().isHorizontal()) {
                for (int i = 0; i < turns; i++) turned = turned.getClockWise();
            }
            out |= 1 << turned.ordinal();
        }
        return out;
    }

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
        return new TraceabilityPredicate(state -> {
            if (!state.getBlockState().is(block(blockId))) return false;
            if (!(state.getTileEntity() instanceof PipeBlockEntity<?, ?> pipe)) return false;
            IMultiController controller = state.getController();
            if (controller == null) return false;
            if (pipe.getConnections() != rotate(mask, controller.self().getFrontFacing())) {
                state.setError(new PatternStringError("af9.chemical_plant.pipe_connections"));
                return false;
            }
            return true;
        }, () -> new BlockInfo[] { BlockInfo.fromBlockState(block(blockId).defaultBlockState()) });
    }

    /** Sets the connections of every right pipe that stands where the file has one; true if anything changed. */
    public static boolean repair(Level level, BlockPos controller, Direction facing) {
        boolean changed = false;
        for (Pipe spot : PIPES) {
            BlockPos pos = worldPos(controller, facing, spot);
            if (!level.isLoaded(pos) || !level.getBlockState(pos).is(block(spot.block()))) continue;
            BlockEntity be = level.getBlockEntity(pos);
            if (!(be instanceof PipeBlockEntity<?, ?> pipe)) continue;
            int want = rotate(spot.mask(), facing);
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
