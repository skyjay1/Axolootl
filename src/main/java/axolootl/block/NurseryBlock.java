/**
 * Copyright (c) 2023 Skyler James
 * Permission is granted to use, modify, and redistribute this software, in parts or in whole,
 * under the GNU LGPLv3 license (https://www.gnu.org/licenses/lgpl-3.0.en.html)
 **/

package axolootl.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class NurseryBlock extends WaterloggedHorizontalBlock {

    protected static final VoxelShape SHAPE = Shapes.or(
            Block.box(0, 0, 0, 2, 12, 2),
            Block.box(14, 0, 0, 16, 12, 2),
            Block.box(14, 0, 14, 16, 12, 16),
            Block.box(0, 0, 14, 2, 12, 16),
            Block.box(1, 0, 1, 15, 0.02D, 15),
            Block.box(2, 0, 0.99D, 14, 10, 1.01D),
            Block.box(14.99D, 0, 2, 15.01D, 10, 14),
            Block.box(2, 0, 14.99D, 14, 10, 15.01D),
            Block.box(0.99D, 0, 2, 1.01D, 10, 14)
    );

    public NurseryBlock(Properties pProperties) {
        super(pProperties);
    }

    @Override
    public VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        return SHAPE;
    }

    @Override
    public boolean isPathfindable(BlockState pState, BlockGetter pLevel, BlockPos pPos, PathComputationType pType) {
        return false;
    }
}
