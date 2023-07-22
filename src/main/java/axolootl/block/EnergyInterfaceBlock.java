/**
 * Copyright (c) 2023 Skyler James
 * Permission is granted to use, modify, and redistribute this software, in parts or in whole,
 * under the GNU LGPLv3 license (https://www.gnu.org/licenses/lgpl-3.0.en.html)
 **/

package axolootl.block;

import axolootl.AxRegistry;
import axolootl.block.entity.EnergyInterfaceBlockEntity;
import axolootl.block.entity.MonsteriumBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
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
import net.minecraftforge.network.NetworkHooks;
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
            if (pPlayer instanceof ServerPlayer serverPlayer && pLevel.getBlockEntity(pPos) instanceof EnergyInterfaceBlockEntity blockEntity) {
                // validate controller
                if (blockEntity.hasTank() && blockEntity.validateController(pLevel)) {
                    blockEntity.setChanged();
                }
                // open menu
                BlockPos controllerPos = blockEntity.getController().isPresent() ? blockEntity.getController().get().getBlockPos() : pPos;
                NetworkHooks.openScreen(serverPlayer, blockEntity, AxRegistry.MenuReg.writeControllerMenu(controllerPos, pPos, AxRegistry.AquariumTabsReg.ENERGY_INTERFACE.get().getSortedIndex(), -1));
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
