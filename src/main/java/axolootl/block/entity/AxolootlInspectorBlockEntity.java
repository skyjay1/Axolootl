/**
 * Copyright (c) 2023 Skyler James
 * Permission is granted to use, modify, and redistribute this software, in parts or in whole,
 * under the GNU LGPLv3 license (https://www.gnu.org/licenses/lgpl-3.0.en.html)
 **/

package axolootl.block.entity;

import axolootl.AxRegistry;
import axolootl.entity.AxolootlEntity;
import axolootl.item.AxolootlBucketItem;
import axolootl.menu.AxolootlInspectorMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.wrapper.InvWrapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class AxolootlInspectorBlockEntity extends InterfaceBlockEntity {

    protected static final int MAX_PROGRESS = 310;

    protected int progress;
    protected int maxProgress;

    protected final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int pIndex) {
            switch (pIndex) {
                case 0: return AxolootlInspectorBlockEntity.this.progress;
                case 1: return AxolootlInspectorBlockEntity.this.maxProgress;
                default: return 0;
            }
        }

        @Override
        public void set(int pIndex, int pValue) {
            switch (pIndex) {
                case 0:
                    AxolootlInspectorBlockEntity.this.progress = pValue;
                    break;
                case 1:
                    AxolootlInspectorBlockEntity.this.maxProgress = pValue;
                    break;
                default:
                    break;
            }
        }

        @Override
        public int getCount() {
            return 2;
        }
    };

    public AxolootlInspectorBlockEntity(BlockPos pPos, BlockState pBlockState) {
        this(AxRegistry.BlockEntityReg.AXOLOOTL_INSPECTOR.get(), pPos, pBlockState);
    }

    public AxolootlInspectorBlockEntity(BlockEntityType<?> pType, BlockPos pPos, BlockState pBlockState) {
        super(pType, pPos, pBlockState, 1, 2);
        this.maxProgress = MAX_PROGRESS;
    }

    public static void tick(final Level level, final BlockPos pos, final BlockState state, final AxolootlInspectorBlockEntity self) {
        // verify tank exists
        final Optional<ControllerBlockEntity> oController = self.getController();
        if (oController.isEmpty() || !oController.get().hasTank() || !oController.get().getTankStatus().isActive()) {
            return;
        }
        boolean markDirty = false;
        markDirty |= self.validateController(level);
        markDirty |= self.tickInspector(level);
        // mark changed and send update
        if(markDirty) {
            self.setChanged();
            level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
        }
    }

    private boolean tickInspector(final Level level) {
        int oldProgress = progress;
        int oldMaxProgress = maxProgress;
        boolean hasItems = hasInspectorItems(level);
        if(!hasItems) {
            // stop progress
            progress = 0;
        } else if(progress <= 0) {
            // start progress
            progress = 1;
        } else if(++progress >= maxProgress) {
            // increment progress and check for completion
            progress = 0;
            ItemStack input = removeItemNoUpdate(0);
            setItem(1, input);
        }
        // report changes
        return progress != oldProgress || maxProgress != oldMaxProgress;
    }

    public boolean hasInspectorItems(final Level level) {
        // validate input slot
        ItemStack input = getItem(0);
        if(input.isEmpty() || AxolootlBucketItem.getVariant(level.registryAccess(), input).isEmpty()) {
            return false;
        }
        // validate empty result slot
        if(!getItem(1).isEmpty()) {
            return false;
        }
        // all checks passed
        return true;
    }

    public boolean isInspectorComplete() {
        return !getItem(1).isEmpty();
    }

    //// MENU PROVIDER ////

    public ContainerData getDataAccess() {
        return this.dataAccess;
    }

    @Override
    public boolean isMenuAvailable(Player player, ControllerBlockEntity controller) {
        return AxRegistry.AquariumTabsReg.AXOLOOTL_INSPECTOR.get().isAvailable(controller);
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int pContainerId, Inventory pPlayerInventory, Player pPlayer) {
        return new AxolootlInspectorMenu(pContainerId, pPlayerInventory, this.controllerPos, this.getController().get(), this.getBlockPos(), AxRegistry.AquariumTabsReg.AXOLOOTL_INSPECTOR.get().getSortedIndex(), -1);
    }

    //// CONTAINER ////

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    //// CAPABILITY ////

    @Override
    protected IItemHandler createItemHandler() {
        return new InvWrapper(this) {
            @Override
            public boolean isItemValid(int slot, @NotNull ItemStack stack) {
                return stack.hasTag() && stack.getTag().contains(AxolootlEntity.KEY_VARIANT_ID, Tag.TAG_STRING);
            }
        };
    }
}
