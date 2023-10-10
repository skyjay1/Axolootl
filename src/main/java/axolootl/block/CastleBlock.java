/**
 * Copyright (c) 2023 Skyler James
 * Permission is granted to use, modify, and redistribute this software, in parts or in whole,
 * under the GNU LGPLv3 license (https://www.gnu.org/licenses/lgpl-3.0.en.html)
 **/

package axolootl.block;

import axolootl.util.ShapeUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class CastleBlock extends WaterloggedHorizontalBlock {

    private static final VoxelShape SHAPE = Shapes.or(
            Block.box(0.0D, 0.0D, 0.0D, 4.0D, 12.0D, 4.0D),
            Block.box(12.0D, 0.0D, 0.0D, 16.0D, 12.0D, 4.0D),
            Block.box(12.0D, 0.0D, 12.0D, 16.0D, 12.0D, 16.0D),
            Block.box(0.0D, 0.0D, 12.0D, 4.0D, 12.0D, 16.0D),
            Shapes.join(
                    Block.box(3.0D, 0.0D, 3.0D, 13.0D, 8.0D, 13.0D),
                    Block.box(4.0D, 0.0D, 4.0D, 12.0D, 8.0D, 12.0D),
                    BooleanOp.ONLY_FIRST)
    );


    public CastleBlock(Properties pProperties) {
        super(pProperties);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        // any entity with a small enough bounding box (measured by volume) can pass through this block
        if(pContext instanceof EntityCollisionContext context && ShapeUtils.canEntityPass(context.getEntity())) {
            return Shapes.empty();
        }
        return super.getCollisionShape(pState, pLevel, pPos, pContext);
    }

    @Override
    public VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        return SHAPE;
    }
}
