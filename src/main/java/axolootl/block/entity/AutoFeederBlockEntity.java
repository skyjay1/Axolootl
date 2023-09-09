/**
 * Copyright (c) 2023 Skyler James
 * Permission is granted to use, modify, and redistribute this software, in parts or in whole,
 * under the GNU LGPLv3 license (https://www.gnu.org/licenses/lgpl-3.0.en.html)
 **/

package axolootl.block.entity;

import axolootl.AxRegistry;
import axolootl.menu.CyclingContainerMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class AutoFeederBlockEntity extends InterfaceBlockEntity {

    public AutoFeederBlockEntity(BlockPos pPos, BlockState pBlockState) {
        this(AxRegistry.BlockEntityReg.AUTO_FEEDER.get(), pPos, pBlockState);
    }

    public AutoFeederBlockEntity(BlockEntityType<?> pType, BlockPos pPos, BlockState pBlockState) {
        super(pType, pPos, pBlockState, 3);
    }

    //// MENU ////

    @Override
    public boolean isMenuAvailable(Player player, ControllerBlockEntity controller) {
        return controller.hasTank();
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int pContainerId, Inventory pPlayerInventory, Player pPlayer) {
        // verify availability
        if(getController().isEmpty() || !isMenuAvailable(pPlayer, this.controller)) {
            return super.createMenu(pContainerId, pPlayerInventory, pPlayer);
        }
        return CyclingContainerMenu.createFeeder(pContainerId, pPlayerInventory, this.controllerPos, this.controller, this.getBlockPos(), AxRegistry.AquariumTabsReg.FOOD_INTERFACE.get().getSortedIndex(), -1);
    }

    //// CLIENT-SERVER SYNC ////

    @Override
    public void setChanged() {
        super.setChanged();
        if(getLevel() != null && !getLevel().isClientSide()) {
            getLevel().sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    public CompoundTag getUpdateTag() {
        return ContainerHelper.saveAllItems(new CompoundTag(), getInventory());
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        getInventory().clear();
        ContainerHelper.loadAllItems(tag, getInventory());
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}