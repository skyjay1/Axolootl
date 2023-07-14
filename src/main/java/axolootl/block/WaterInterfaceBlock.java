package axolootl.block;

import axolootl.AxRegistry;
import axolootl.block.entity.ControllerBlockEntity;
import axolootl.block.entity.WaterInterfaceBlockEntity;
import axolootl.util.TankMultiblock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
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
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;

public class WaterInterfaceBlock extends HorizontalDirectionalBlock implements EntityBlock {

    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

    public WaterInterfaceBlock(Properties pProperties) {
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

    @Override
    public InteractionResult use(BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer, InteractionHand pHand, BlockHitResult pHit) {
        if (pLevel.isClientSide()) {
            return InteractionResult.SUCCESS;
        } else {
            BlockEntity blockentity = pLevel.getBlockEntity(pPos);
            ItemStack itemStack = pPlayer.getItemInHand(pHand);
            // TODO debug only
            if(blockentity instanceof WaterInterfaceBlockEntity be && itemStack.is(Items.WATER_BUCKET)) {
                be.getCapability(ForgeCapabilities.FLUID_HANDLER).ifPresent(c -> {
                    // DEBUG AMOUNT IS MORE THAN ONE BUCKET AT A TIME
                    final FluidStack fluidStack = new FluidStack(Fluids.WATER, FluidType.BUCKET_VOLUME * 2);
                    if(c.fill(fluidStack, IFluidHandler.FluidAction.SIMULATE) >= fluidStack.getAmount()) {
                        c.fill(fluidStack, IFluidHandler.FluidAction.EXECUTE);
                        pPlayer.setItemInHand(pHand, itemStack.getCraftingRemainingItem());
                    }
                });
            }
            // open menu
            if (blockentity instanceof MenuProvider menuProvider) {
                pPlayer.openMenu(menuProvider);
            }
            return InteractionResult.CONSUME;
        }
    }

    //// ENTITY BLOCK ////

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pPos, BlockState pState) {
        return AxRegistry.BlockEntityReg.WATER_INTERFACE.get().create(pPos, pState);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level pLevel, BlockState pState, BlockEntityType<T> pBlockEntityType) {
        if(pLevel.isClientSide() || !pState.is(this) || pBlockEntityType != AxRegistry.BlockEntityReg.WATER_INTERFACE.get()) {
            return null;
        }
        return (BlockEntityTicker<T>) (BlockEntityTicker<WaterInterfaceBlockEntity>) (WaterInterfaceBlockEntity::tick);
    }

    // REDSTONE //

    @Override
    public void onPlace(BlockState blockState, Level level, BlockPos blockPos, BlockState oldState, boolean isMoving) {
        if (!oldState.is(blockState.getBlock())) {
            this.checkPoweredState(level, blockPos, blockState);
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            level.updateNeighbourForOutputSignal(pos, this);
            super.onRemove(state, level, pos, newState, isMoving);
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
        if (pLevel.getBlockEntity(pPos) instanceof WaterInterfaceBlockEntity blockEntity) {
            return blockEntity.hasTank() ? 1 : 0;
        }
        return 0;
    }
}
