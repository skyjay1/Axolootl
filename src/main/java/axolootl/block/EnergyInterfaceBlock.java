package axolootl.block;

import axolootl.AxRegistry;
import axolootl.block.entity.EnergyInterfaceBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import org.jetbrains.annotations.Nullable;

public class EnergyInterfaceBlock extends AbstractInterfaceBlock {

    public EnergyInterfaceBlock(Properties pProperties) {
        super(pProperties);
    }

    @Override
    public InteractionResult use(BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer, InteractionHand pHand, BlockHitResult pHit) {
        if (pLevel.isClientSide()) {
            return InteractionResult.SUCCESS;
        } else {
            BlockEntity blockentity = pLevel.getBlockEntity(pPos);
            ItemStack itemStack = pPlayer.getItemInHand(pHand);
            // TODO debug only
            if(blockentity instanceof EnergyInterfaceBlockEntity be) {
                LazyOptional<IEnergyStorage> energyStorage = be.getCapability(ForgeCapabilities.ENERGY, null);
                energyStorage.ifPresent(i -> pPlayer.displayClientMessage(Component.literal("energy: " + i.getEnergyStored()), false));
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
        return AxRegistry.BlockEntityReg.ENERGY_INTERFACE.get().create(pPos, pState);
    }
}
