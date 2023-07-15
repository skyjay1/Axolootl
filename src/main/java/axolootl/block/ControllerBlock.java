package axolootl.block;

import axolootl.AxRegistry;
import axolootl.block.entity.ControllerBlockEntity;
import axolootl.util.TankMultiblock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public class ControllerBlock extends HorizontalDirectionalBlock implements EntityBlock {

    public ControllerBlock(Properties pProperties) {
        super(pProperties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    //// METHODS ////

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext pContext) {
        return this.defaultBlockState().setValue(FACING, pContext.getHorizontalDirection().getOpposite());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
        pBuilder.add(FACING);
    }

    @Override
    public InteractionResult use(BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer, InteractionHand pHand, BlockHitResult pHit) {
        if (pLevel.isClientSide()) {
            return InteractionResult.SUCCESS;
        } else {
            BlockEntity blockentity = pLevel.getBlockEntity(pPos);
            // TODO debug only
            if(blockentity instanceof ControllerBlockEntity be) {
                be.setSize(TankMultiblock.AQUARIUM.hasTankStructure(pLevel, pPos).orElse(null));
                pPlayer.displayClientMessage(Component.literal("Tank: ").append(be.getTankStatus().getDescription()), false);
                pPlayer.displayClientMessage(Component.literal("Breed: ").append(be.getBreedStatus().getDescription()), false);
                pPlayer.displayClientMessage(Component.literal("Feed: ").append(be.getFeedStatus().getDescription()), false);
            }
            // open menu
            if (blockentity instanceof MenuProvider menuProvider) {
                pPlayer.openMenu(menuProvider);
            }
            return InteractionResult.CONSUME;
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            level.updateNeighbourForOutputSignal(pos, this);
        }
        if(level instanceof ServerLevel serverLevel && level.getBlockEntity(pos) instanceof ControllerBlockEntity controller) {
            controller.onRemoved(serverLevel);
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    //// ENTITY BLOCK ////

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pPos, BlockState pState) {
        return AxRegistry.BlockEntityReg.CONTROLLER.get().create(pPos, pState);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level pLevel, BlockState pState, BlockEntityType<T> pBlockEntityType) {
        if(pLevel.isClientSide() || !pState.is(this) || pBlockEntityType != AxRegistry.BlockEntityReg.CONTROLLER.get()) {
            return null;
        }
        return (BlockEntityTicker<T>) (BlockEntityTicker<ControllerBlockEntity>) (ControllerBlockEntity::tick);
    }

    // REDSTONE //

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(BlockState pState, Level pLevel, BlockPos pPos) {
        if (pLevel.getBlockEntity(pPos) instanceof ControllerBlockEntity blockEntity) {
            return blockEntity.getTankStatus().isActive() ? 1 : 0;
        }
        return 0;
    }
}
