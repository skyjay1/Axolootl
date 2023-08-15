/**
 * Copyright (c) 2023 Skyler James
 * Permission is granted to use, modify, and redistribute this software, in parts or in whole,
 * under the GNU LGPLv3 license (https://www.gnu.org/licenses/lgpl-3.0.en.html)
 **/

package axolootl.block;

import axolootl.AxRegistry;
import axolootl.block.entity.AxolootlInterfaceBlockEntity;
import axolootl.block.entity.InterfaceBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;

public class AxolootlInterfaceBlock extends HorizontalDirectionalBlock implements EntityBlock {

    public AxolootlInterfaceBlock(Properties pProperties) {
        super(pProperties);        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext pContext) {
        return this.defaultBlockState().setValue(FACING, pContext.getHorizontalDirection().getOpposite());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
        pBuilder.add(FACING);
    }

    //// GLASS ////

    @Override
    public VoxelShape getVisualShape(BlockState pState, BlockGetter pReader, BlockPos pPos, CollisionContext pContext) {
        return Shapes.empty();
    }

    @Override
    public float getShadeBrightness(BlockState pState, BlockGetter pLevel, BlockPos pPos) {
        return 1.0F;
    }

    @Override
    public boolean propagatesSkylightDown(BlockState pState, BlockGetter pReader, BlockPos pPos) {
        return true;
    }

    @Override
    public boolean skipRendering(BlockState pState, BlockState pAdjacentBlockState, Direction pSide) {
        return pAdjacentBlockState.is(this) || pAdjacentBlockState.is(AbstractInterfaceBlock.GLASS) || pAdjacentBlockState.is(AbstractInterfaceBlock.AQUARIUM) || super.skipRendering(pState, pAdjacentBlockState, pSide);
    }

    @Override
    public boolean shouldDisplayFluidOverlay(BlockState state, BlockAndTintGetter level, BlockPos pos, FluidState fluidState) {
        return true;
    }

    //// ENTITY BLOCK ////

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pPos, BlockState pState) {
        return AxRegistry.BlockEntityReg.AXOLOOTL_INTERFACE.get().create(pPos, pState);
    }

    @Override
    public InteractionResult use(BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer, InteractionHand pHand, BlockHitResult pHit) {
        if (pLevel.isClientSide()) {
            return InteractionResult.SUCCESS;
        } else {
            if (pPlayer instanceof ServerPlayer serverPlayer && pLevel.getBlockEntity(pPos) instanceof AxolootlInterfaceBlockEntity blockEntity) {
                // validate controller
                if (blockEntity.hasTank() && blockEntity.validateController(pLevel)) {
                    blockEntity.setChanged();
                }
                // open menu
                if (blockEntity.isMenuAvailable(serverPlayer, blockEntity.getController().orElse(null))) {
                    BlockPos controllerPos = blockEntity.getController().isPresent() ? blockEntity.getController().get().getBlockPos() : pPos;
                    NetworkHooks.openScreen(serverPlayer, blockEntity, AxRegistry.MenuReg.writeControllerMenu(controllerPos, pPos, AxRegistry.AquariumTabsReg.AXOLOOTL_INTERFACE.get().getSortedIndex(), 0));
                }
            }
            return InteractionResult.CONSUME;
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            // drop items
            if(!level.isClientSide() && level.getBlockEntity(pos) instanceof InterfaceBlockEntity blockEntity) {
                blockEntity.dropAllItems();
            }
            // update neighbors
            level.updateNeighbourForOutputSignal(pos, this);
            super.onRemove(state, level, pos, newState, isMoving);
        }
    }

    // REDSTONE //

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(BlockState pState, Level pLevel, BlockPos pPos) {
        if (pLevel.getBlockEntity(pPos) instanceof Container container) {
            return AbstractContainerMenu.getRedstoneSignalFromContainer(container);
        }
        return 0;
    }
}
