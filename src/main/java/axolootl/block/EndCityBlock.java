/**
 * Copyright (c) 2023 Skyler James
 * Permission is granted to use, modify, and redistribute this software, in parts or in whole,
 * under the GNU LGPLv3 license (https://www.gnu.org/licenses/lgpl-3.0.en.html)
 **/

package axolootl.block;

import axolootl.util.ShapeUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.EnumMap;
import java.util.Map;

public class EndCityBlock extends WaterloggedHorizontalDoubleBlock {

    private static final VoxelShape SHAPE_LOWER = ShapeUtils.orUnoptimized(
            Block.box(4, 0, 4, 12, 3, 12),
            Block.box(5, 3, 5, 11, 4, 11),
            Block.box(4, 4, 4, 12, 6, 12),
            Block.box(7, 6, 7, 9, 10, 9),
            Block.box(6, 10, 6, 10, 14, 10),
            Block.box(5, 14, 5, 11, 16, 11)
    );

    private static final VoxelShape SHAPE_UPPER = ShapeUtils.orUnoptimized(
            Block.box(5, 0, 5, 11, 6, 11),
            Block.box(5.5D, 6.0D, 5.5D, 10.5D, 10.0D, 10.5D),
            Block.box(5.5D, 1.0D, 0.0D, 8.5D, 4.0D, 3.0D),
            Block.box(6.0D, 0.0D, 0.5D, 8.0D, 1.0D, 5.5D),
            Block.box(10.5D, 3.0D, 6.0D, 13.5D, 4.0D, 8.0D),
            Block.box(12.5D, 4.0D, 6.0D, 15.5D, 5.0D, 8.0D),
            Block.box(13.0D, 5.0D, 5.5D, 16.0D, 8.0D, 8.5D),
            Block.box(8.0D, 1.0D, 10.5D, 10.0D, 2.0D, 13.5D),
            Block.box(8.0D, 2.0D, 12.5D, 10.0D, 3.0D, 15.5D),
            Block.box(7.5D, 3.0D, 13.0D, 10.5D, 6.0D, 16.0D),
            Block.box(8.0D, 6.0D, 13.5D, 10.0D, 8.0D, 15.5D),
            Block.box(7.5D, 8.0D, 13.0D, 10.5D, 11.0D, 16.0D),
            Block.box(1.0D, 2.0D, 7.5D, 4.0D, 5.0D, 10.5D),
            Block.box(1.0D, 11.0D, 7.5D, 4.0D, 14.0D, 10.5D),
            Block.box(1.5D, 1.0D, 8.0D, 5.5D, 2.0D, 10.0D),
            Block.box(1.5D, 5.0D, 8.0D, 3.5D, 7.0D, 10.0D),
            Block.box(0.5D, 7.0D, 7.0D, 4.5D, 11.0D, 11.0D)
    );

    private static final Map<Direction, VoxelShape> SHAPES_LOWER = ShapeUtils.rotateShapes(Direction.NORTH, SHAPE_LOWER);
    private static final Map<Direction, VoxelShape> SHAPES_UPPER = ShapeUtils.rotateShapes(Direction.NORTH, SHAPE_UPPER);

    public EndCityBlock(Properties pProperties) {
        super(pProperties);
    }

    @Override
    public VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        final Direction direction = pState.getValue(FACING);
        if(pState.getValue(HALF) == DoubleBlockHalf.LOWER) {
            return SHAPES_LOWER.get(direction);
        }
        return SHAPES_UPPER.get(direction);
    }
}
