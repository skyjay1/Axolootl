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
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.EnumMap;
import java.util.Map;

public class EndShipBlock extends WaterloggedHorizontalBlock {

    private static final VoxelShape SHAPE = ShapeUtils.orUnoptimized(
            Block.box(13, 3, 7, 16, 5, 9),
            Block.box(2, 0, 5, 9, 1, 11),
            Block.box(1, 1, 5, 11, 2, 11),
            Block.box(0, 2, 5, 13, 5, 11),
            Block.box(0, 5, 5, 4, 6, 11),
            Block.box(13, 3, 6, 14, 5, 10),
            Block.box(6, 5, 7, 8, 13, 9),
            Block.box(5, 10, 6, 9, 12, 10),
            Block.box(10, 5, 7, 12, 7, 9)
    );

    private static final Map<Direction, VoxelShape> SHAPES = ShapeUtils.rotateShapes(Direction.NORTH, SHAPE);

    public EndShipBlock(Properties pProperties) {
        super(pProperties);
    }

    @Override
    public VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        return SHAPES.get(pState.getValue(FACING));
    }
}
