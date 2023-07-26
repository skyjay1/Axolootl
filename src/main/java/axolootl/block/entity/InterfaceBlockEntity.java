/**
 * Copyright (c) 2023 Skyler James
 * Permission is granted to use, modify, and redistribute this software, in parts or in whole,
 * under the GNU LGPLv3 license (https://www.gnu.org/licenses/lgpl-3.0.en.html)
 **/

package axolootl.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.wrapper.InvWrapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public abstract class InterfaceBlockEntity extends BlockEntity implements Container, MenuProvider, IAquariumControllerProvider {

    protected final NonNullList<ItemStack> inventory;
    protected LazyOptional<IItemHandler> holder = LazyOptional.of(this::createItemHandler);
    protected final int rows;
    protected final int slots;

    protected BlockPos controllerPos;
    protected ControllerBlockEntity controller;

    protected InterfaceBlockEntity(BlockEntityType<?> pType, BlockPos pPos, BlockState pBlockState, int rows) {
        this(pType, pPos, pBlockState, rows, rows * 9);
    }

    protected InterfaceBlockEntity(BlockEntityType<?> pType, BlockPos pPos, BlockState pBlockState, int rows, int slots) {
        super(pType, pPos, pBlockState);
        this.rows = rows;
        this.slots = slots;
        this.inventory = NonNullList.withSize(slots, ItemStack.EMPTY);
    }

    //// CONTROLLER PROVIDER ////

    @Override
    public void setController(final Level level, final BlockPos pos, final ControllerBlockEntity blockEntity) {
        this.controllerPos = pos;
        this.controller = blockEntity;
        this.setChanged();
    }

    @Override
    public void clearController() {
        this.controllerPos = null;
        this.controller = null;
        this.setChanged();
    }

    @Override
    public Optional<ControllerBlockEntity> getController() {
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

    //// MENU PROVIDER ////

    @Override
    public Component getDisplayName() {
        return this.getBlockState().getBlock().getName();
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int pContainerId, Inventory pPlayerInventory, Player pPlayer) {
        switch (rows) {
            case 1: return new ChestMenu(MenuType.GENERIC_9x1, pContainerId, pPlayerInventory, this, rows);
            case 2: return new ChestMenu(MenuType.GENERIC_9x2, pContainerId, pPlayerInventory, this, rows);
            case 3: return ChestMenu.threeRows(pContainerId, pPlayerInventory, this);
            case 4: return new ChestMenu(MenuType.GENERIC_9x4, pContainerId, pPlayerInventory, this, rows);
            case 5: return new ChestMenu(MenuType.GENERIC_9x5, pContainerId, pPlayerInventory, this, rows);
            case 6: return ChestMenu.sixRows(pContainerId, pPlayerInventory, this);
            default: return null;
        }
    }

    public abstract boolean isMenuAvailable(final Player player, final ControllerBlockEntity controller);

    //// CONTAINER ////

    public NonNullList<ItemStack> getInventory() {
        return inventory;
    }

    public void dropAllItems() {
        if (this.level != null && !this.level.isClientSide()) {
            Containers.dropContents(this.level, this.getBlockPos(), this.getInventory());
            clearContent();
        }
        this.setChanged();
    }

    @Override
    public int getContainerSize() {
        return slots;
    }

    @Override
    public boolean isEmpty() {
        return inventory.stream().allMatch(ItemStack::isEmpty);
    }

    @Override
    public ItemStack getItem(int pSlot) {
        return inventory.get(pSlot);
    }

    @Override
    public ItemStack removeItem(int pSlot, int pAmount) {
        ItemStack itemstack = ContainerHelper.removeItem(inventory, pSlot, pAmount);
        if (!itemstack.isEmpty()) {
            this.setChanged();
        }

        return itemstack;
    }

    @Override
    public ItemStack removeItemNoUpdate(int pSlot) {
        return ContainerHelper.takeItem(inventory, pSlot);
    }

    @Override
    public void setItem(int pSlot, ItemStack pStack) {
        this.inventory.set(pSlot, pStack);
        if (pStack.getCount() > this.getMaxStackSize()) {
            pStack.setCount(this.getMaxStackSize());
        }

        this.setChanged();
    }

    @Override
    public boolean stillValid(Player pPlayer) {
        if (this.level.getBlockEntity(this.worldPosition) != this) {
            return false;
        }
        if(this.controllerPos != null && this.controller != null && this.controller.hasTank()) {
            return true;
        }
        return pPlayer.position().closerThan(Vec3.atCenterOf(this.worldPosition), 8);
    }

    @Override
    public void clearContent() {
        this.inventory.clear();
        this.setChanged();
    }

    //// CAPABILITY ////

    protected IItemHandler createItemHandler() {
        return new InvWrapper(this);
    }

    @Override
    @NotNull
    public <T> LazyOptional<T> getCapability(@NotNull Capability<T> capability, @Nullable Direction facing)
    {
        if (capability == ForgeCapabilities.ITEM_HANDLER) {
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
        holder = LazyOptional.of(this::createItemHandler);
    }

    //// NBT ////

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        ContainerHelper.loadAllItems(tag, inventory);
    }

    @Override
    public void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        ContainerHelper.saveAllItems(tag, inventory);
    }
}
