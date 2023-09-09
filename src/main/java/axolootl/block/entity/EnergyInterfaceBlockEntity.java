/**
 * Copyright (c) 2023 Skyler James
 * Permission is granted to use, modify, and redistribute this software, in parts or in whole,
 * under the GNU LGPLv3 license (https://www.gnu.org/licenses/lgpl-3.0.en.html)
 **/

package axolootl.block.entity;

import axolootl.AxRegistry;
import axolootl.block.EnergyInterfaceBlock;
import axolootl.menu.CyclingMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.EnergyStorage;
import net.minecraftforge.energy.IEnergyStorage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class EnergyInterfaceBlockEntity extends BlockEntity implements IAquariumControllerProvider, MenuProvider {

    protected EnergyStorage energy = new InternalEnergyStorage();
    private LazyOptional<IEnergyStorage> holder = LazyOptional.of(() -> energy);

    private BlockPos controllerPos;
    private ControllerBlockEntity controller;

    public EnergyInterfaceBlockEntity(BlockPos pPos, BlockState pBlockState) {
        this(AxRegistry.BlockEntityReg.ENERGY_INTERFACE.get(), pPos, pBlockState);
    }

    public EnergyInterfaceBlockEntity(BlockEntityType<?> pType, BlockPos pPos, BlockState pBlockState) {
        super(pType, pPos, pBlockState);
    }

    //// CONTROLLER PROVIDER ////

    @Override
    public void setController(final Level level, final BlockPos pos, final ControllerBlockEntity blockEntity) {
        boolean isChanged = this.controllerPos != pos || this.controller != blockEntity;
        this.controllerPos = pos;
        this.controller = blockEntity;
        if(isChanged) {
            this.setChanged();
        }
    }

    @Override
    public void clearController() {
        this.controllerPos = null;
        this.controller = null;
        this.setChanged();
    }

    @Override
    public Optional<ControllerBlockEntity> getController() {
        // lazy load controller from position
        if(controllerPos != null && null == controller && level != null) {
            if(level.getBlockEntity(controllerPos) instanceof ControllerBlockEntity controller) {
                setController(level, controllerPos, controller);
            } else {
                clearController();
            }
        }
        return Optional.ofNullable(controller);
    }

    //// SETTERS AND GETTERS ////

    /**
     * @param level the level
     * @return true if the controller changed
     */
    public boolean validateController(final Level level) {
        // validate position
        if(null == controllerPos) {
            this.controller = null;
            return true;
        }
        // validate block entity
        BlockEntity blockEntity = level.getBlockEntity(controllerPos);
        if(blockEntity instanceof ControllerBlockEntity controllerBlockEntity && controllerBlockEntity != this.controller) {
            this.controller = controllerBlockEntity;
            return true;
        }
        // no changes
        return false;
    }

    //// CAPABILITY ////

    @Override
    @NotNull
    public <T> LazyOptional<T> getCapability(@NotNull Capability<T> capability, @Nullable Direction facing)
    {
        if (capability == ForgeCapabilities.ENERGY) {
            return holder.cast();
        }
        return super.getCapability(capability, facing);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        holder.invalidate();
    }

    @Override
    public void reviveCaps() {
        super.reviveCaps();
        holder = LazyOptional.of(() -> energy);
    }

    //// MENU PROVIDER ////

    @Override
    public Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    public boolean isMenuAvailable(Player player, ControllerBlockEntity controller) {
        return getController().isPresent() && AxRegistry.AquariumTabsReg.ENERGY_INTERFACE.get().isAvailable(controller);
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int pContainerId, Inventory pPlayerInventory, Player pPlayer) {
        return CyclingMenu.createEnergy(pContainerId, pPlayerInventory, controllerPos, getController().get(), getBlockPos(), AxRegistry.AquariumTabsReg.ENERGY_INTERFACE.get().getSortedIndex(), -1);
    }

    //// CLIENT SERVER SYNC ////

    /**
     * Called when the chunk is saved
     * @return the compound tag to use in #handleUpdateTag
     */
    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag);
        tag.put(KEY_ENERGY, this.energy.serializeNBT());
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    //// NBT ////

    private static final String KEY_ENERGY = "Energy";

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        controllerPos = readControllerPos(tag);
        if(tag.contains(KEY_ENERGY, Tag.TAG_INT)) {
            this.energy.deserializeNBT(tag.get(KEY_ENERGY));
        }
    }

    @Override
    public void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        writeControllerPos(controllerPos, tag);
    }

    //// ENERGY STORAGE ////

    private class InternalEnergyStorage extends EnergyStorage {

        private static final int CAPACITY = 10_000;
        private static final int TRANSFER = 10_000;

        public InternalEnergyStorage() {
            super(CAPACITY, TRANSFER);
        }

        @Override
        public boolean canExtract() {
            return !EnergyInterfaceBlockEntity.this.getBlockState().getValue(EnergyInterfaceBlock.POWERED);
        }

        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            int value = super.receiveEnergy(maxReceive, simulate);
            if (!simulate && value != 0) {
                EnergyInterfaceBlockEntity.this.setChanged();
                EnergyInterfaceBlockEntity.this.level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
            }
            return value;
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            int value = super.extractEnergy(maxExtract, simulate);
            if (!simulate && value != 0) {
                EnergyInterfaceBlockEntity.this.setChanged();
                EnergyInterfaceBlockEntity.this.level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
            }
            return value;
        }
    }

}
