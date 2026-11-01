package com.af9.core.registry;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A slope: a block cut along its diagonal, a triangle prism that fills half of the cube. {@code facing} is the side of the
 * full-height wall, {@code half} says whether the flat side is the floor ({@code bottom}) or the ceiling ({@code top}); it is
 * placed like a stair: the wall is on the far side of the block you aim at, the half follows the click's height. Used for
 * the chamfer of the Large Chemical Reactor's barrel ({@code af9:inconel_718_slope}).
 */
public class SlopeBlock extends Block {

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final EnumProperty<Half> HALF = BlockStateProperties.HALF;

    /** The wall on the east side, flat floor: 8 steps of two pixels, the first (eastmost) full height. */
    private static final VoxelShape[] SHAPES = new VoxelShape[8]; // [facing index * 2 + half]

    static {
        VoxelShape eastBottom = Shapes.empty();
        for (int i = 0; i < 8; i++) {
            // step i covers x from 2i to 2i+2 (west to east) and is 2(i+1) pixels high
            eastBottom = Shapes.or(eastBottom, Block.box(2 * i, 0, 0, 2 * i + 2, 2 * (i + 1), 16));
        }
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            SHAPES[facing.get2DDataValue() * 2] = rotate(eastBottom, facing);
            SHAPES[facing.get2DDataValue() * 2 + 1] = flip(rotate(eastBottom, facing));
        }
    }

    public SlopeBlock(Properties properties) {
        super(properties.noOcclusion());
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.EAST).setValue(HALF, Half.BOTTOM));
    }

    /** The east-wall shape turned so that its wall is on {@code facing}. */
    private static VoxelShape rotate(VoxelShape east, Direction facing) {
        int turns = switch (facing) {
            case SOUTH -> 1;
            case WEST -> 2;
            case NORTH -> 3;
            default -> 0;
        };
        VoxelShape shape = east;
        for (int i = 0; i < turns; i++) {
            VoxelShape[] turned = { Shapes.empty() };
            // a quarter turn about the y axis: (x, z) -> (1 - z, x)
            shape.forAllBoxes((x1, y1, z1, x2, y2, z2) -> turned[0] = Shapes.or(turned[0],
                    Shapes.box(1 - z2, y1, x1, 1 - z1, y2, x2)));
            shape = turned[0];
        }
        return shape;
    }

    private static VoxelShape flip(VoxelShape shape) {
        VoxelShape[] flipped = { Shapes.empty() };
        shape.forAllBoxes((x1, y1, z1, x2, y2, z2) -> flipped[0] = Shapes.or(flipped[0],
                Shapes.box(x1, 1 - y2, z1, x2, 1 - y1, z2)));
        return flipped[0];
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, HALF);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction clicked = context.getClickedFace();
        boolean top = clicked == Direction.DOWN
                || (clicked != Direction.UP && context.getClickLocation().y - context.getClickedPos().getY() > 0.5);
        // the wall is on the far side: the player looks along the facing, the wall stands ahead of them
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection())
                .setValue(HALF, top ? Half.TOP : Half.BOTTOM);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[state.getValue(FACING).get2DDataValue() * 2 + (state.getValue(HALF) == Half.TOP ? 1 : 0)];
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }
}
