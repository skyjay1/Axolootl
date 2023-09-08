/**
 * Copyright (c) 2023 Skyler James
 * Permission is granted to use, modify, and redistribute this software, in parts or in whole,
 * under the GNU LGPLv3 license (https://www.gnu.org/licenses/lgpl-3.0.en.html)
 **/

package axolootl.block;

import axolootl.block.entity.IAquariumControllerProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

public class PoweredHorizontalBlock extends HorizontalDirectionalBlock {

    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

    public PoweredHorizontalBlock(Properties pProperties) {
        super(pProperties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(POWERED, false));
    }

    //// METHODS ////

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext pContext) {
        return this.defaultBlockState().setValue(FACING, pContext.getHorizontalDirection().getOpposite());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
        pBuilder.add(FACING).add(POWERED);
    }

    // REDSTONE //

    @Override
    public void onPlace(BlockState blockState, Level level, BlockPos blockPos, BlockState oldState, boolean isMoving) {
        if (!oldState.is(blockState.getBlock())) {
            this.checkPoweredState(level, blockPos, blockState);
        }
    }

    @Override
    public void neighborChanged(BlockState blockState, Level level, BlockPos blockPos, Block neighborBlock, BlockPos neighborPos, boolean b) {
        this.checkPoweredState(level, blockPos, blockState);
    }

    private void checkPoweredState(Level level, BlockPos blockPos, BlockState blockState) {
        boolean powered = level.hasNeighborSignal(blockPos);
        if (powered != blockState.getValue(POWERED)) {
            level.setBlock(blockPos, blockState.setValue(POWERED, powered), Block.UPDATE_INVISIBLE | Block.UPDATE_CLIENTS);
        }
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(BlockState pState, Level pLevel, BlockPos pPos) {
        if (pLevel.getBlockEntity(pPos) instanceof IAquariumControllerProvider blockEntity) {
            return blockEntity.hasTank() ? 1 : 0;
        }
        return 0;
    }
}
