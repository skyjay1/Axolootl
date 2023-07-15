package axolootl.block;

import axolootl.AxRegistry;
import axolootl.block.entity.WaterInterfaceBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;

public class WaterInterfaceBlock extends AbstractInterfaceBlock {

    public WaterInterfaceBlock(Properties pProperties) {
        super(pProperties);
    }

    //// METHODS ////

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
}
