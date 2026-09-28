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
 * Optical Bus Cable: the machine bus's cable (multimode glass fibre in an aqua jacket, a data center's OM4 fibre). A
 * thin pipe-shaped block that joins the cables next to it, the Bus Connectors and Interconnect Hatches whose port
 * (front face) points at it, GT's optical transmitter hatches (an HPCA's or Network Switch's computation, a Data
 * Bank's research) that face it and CWU Servers on any side but their front; {@link BusNetwork} walks it. Unlike GT's Optical Fiber Cable it branches. No block
 * entity: the six connection properties are all it has.
 */
public class OpticalBusCableBlock extends PipeBlock {

    /** Half the cable's thickness: 4 px. */
    private static final float APOTHEM = 2 / 16f;

    public OpticalBusCableBlock(Properties properties) {
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

    /**
     * Another cable, a Bus Connector, Interconnect Hatch or {@link BusConsumer} whose port faces this cable, a GT
     * transmitter hatch facing it, or a CWU Server on any side but its front.
     */
    public static boolean connectsTo(BlockGetter level, BlockPos pos, Direction direction) {
        BlockPos other = pos.relative(direction);
        if (level.getBlockState(other).getBlock() instanceof OpticalBusCableBlock) return true;
        if (level.getBlockEntity(other) instanceof BusConsumer consumer) {
            return consumer.getPortSide() == direction.getOpposite();
        }
        MetaMachine machine = MetaMachine.getMachine(level, other);
        if (machine == null || !BusNetwork.facesBus(machine, direction.getOpposite())) return false;
        return machine instanceof BusConnectorPartMachine || machine instanceof BusInterconnectPartMachine ||
                BusNetwork.isTransmitter(machine);
    }
}
