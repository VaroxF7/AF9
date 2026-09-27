package com.af9.core.bus;

import com.gregtechceu.gtceu.api.machine.MetaMachine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;

/**
 * Polycat Cable: the machine bus's cable (a Cat cable in a polyethylene jacket). A thin pipe-shaped block that joins
 * the cables next to it and the Bus Connectors whose port (front face) points at it; {@link BusNetwork} walks it. No
 * block entity: the six connection properties are all it has.
 */
public class PolycatCableBlock extends PipeBlock {

    /** Half the cable's thickness: 4 px. */
    private static final float APOTHEM = 2 / 16f;

    public PolycatCableBlock(Properties properties) {
        super(APOTHEM, properties);
        BlockState state = stateDefinition.any();
        for (Direction direction : Direction.values()) {
            state = state.setValue(PROPERTY_BY_DIRECTION.get(direction), false);
        }
        registerDefaultState(state);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NORTH, EAST, SOUTH, WEST, UP, DOWN);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return connections(defaultBlockState(), context.getLevel(), context.getClickedPos());
    }

    @Override
    @SuppressWarnings("deprecation")
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level,
                                  BlockPos pos, BlockPos neighborPos) {
        return state.setValue(PROPERTY_BY_DIRECTION.get(direction), connectsTo(level, pos, direction));
    }

    private BlockState connections(BlockState state, BlockGetter level, BlockPos pos) {
        for (Direction direction : Direction.values()) {
            state = state.setValue(PROPERTY_BY_DIRECTION.get(direction), connectsTo(level, pos, direction));
        }
        return state;
    }

    /** Another cable, or a Bus Connector with its port towards this cable. */
    public static boolean connectsTo(BlockGetter level, BlockPos pos, Direction direction) {
        BlockPos other = pos.relative(direction);
        if (level.getBlockState(other).getBlock() instanceof PolycatCableBlock) return true;
        return MetaMachine.getMachine(level, other) instanceof BusConnectorPartMachine connector &&
                connector.getFrontFacing() == direction.getOpposite();
    }
}
