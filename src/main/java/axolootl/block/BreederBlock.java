/**
 * Copyright (c) 2023 Skyler James
 * Permission is granted to use, modify, and redistribute this software, in parts or in whole,
 * under the GNU LGPLv3 license (https://www.gnu.org/licenses/lgpl-3.0.en.html)
 **/

package axolootl.block;

import axolootl.AxRegistry;
import axolootl.block.entity.BreederBlockEntity;
import axolootl.block.entity.InterfaceBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;

public class BreederBlock extends WaterloggedHorizontalBlock implements EntityBlock {

    public BreederBlock(Properties pProperties) {
        super(pProperties);
    }

    //// ENTITY BLOCK ////

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pPos, BlockState pState) {
        return AxRegistry.BlockEntityReg.BREEDER.get().create(pPos, pState);
    }

    @Override
    public InteractionResult use(BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer, InteractionHand pHand, BlockHitResult pHit) {
        if (pLevel.isClientSide()) {
            return InteractionResult.SUCCESS;
        } else {
            if (pPlayer instanceof ServerPlayer serverPlayer && pLevel.getBlockEntity(pPos) instanceof BreederBlockEntity blockEntity) {
                // validate controller
                if(blockEntity.hasTank() && blockEntity.validateController(pLevel)) {
                    blockEntity.setChanged();
                }
                // open menu
                BlockPos controllerPos = blockEntity.getController().isPresent() ? blockEntity.getController().get().getBlockPos() : pPos;
                NetworkHooks.openScreen(serverPlayer, blockEntity, AxRegistry.MenuReg.writeControllerMenu(controllerPos, pPos, AxRegistry.AquariumTabsReg.FOOD_INTERFACE.get().getSortedIndex(), -1));
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
