/**
 * Copyright (c) 2023 Skyler James
 * Permission is granted to use, modify, and redistribute this software, in parts or in whole,
 * under the GNU LGPLv3 license (https://www.gnu.org/licenses/lgpl-3.0.en.html)
 **/

package axolootl.block.entity;

import axolootl.AxRegistry;
import axolootl.Axolootl;
import axolootl.item.AxolootlBucketItem;
import axolootl.menu.AxolootlInterfaceMenu;
import axolootl.menu.CyclingContainerMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.wrapper.InvWrapper;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.function.Supplier;

public class AxolootlInspectorBlockEntity extends InterfaceBlockEntity {

    public static final TagKey<Item> ITEM_WHITELIST = ForgeRegistries.ITEMS.tags().createTagKey(new ResourceLocation(Axolootl.MODID, "axolootl_interface_whitelist"));
    public static final TagKey<Item> BOOK_WHITELIST = ForgeRegistries.ITEMS.tags().createTagKey(new ResourceLocation(Axolootl.MODID, "axolootl_inspector_books"));
    public static final Supplier<Item> RESULT_ITEM = () -> Items.WRITTEN_BOOK;

    private int progress;
    private int maxProgress;
    // TODO add data slots

    public AxolootlInspectorBlockEntity(BlockPos pPos, BlockState pBlockState) {
        this(AxRegistry.BlockEntityReg.AXOLOOTL_INSPECTOR.get(), pPos, pBlockState);
    }

    public AxolootlInspectorBlockEntity(BlockEntityType<?> pType, BlockPos pPos, BlockState pBlockState) {
        super(pType, pPos, pBlockState, 1, 2);
    }

    public static void tick(final Level level, final BlockPos pos, final BlockState state, final AxolootlInspectorBlockEntity self) {
        // verify tank exists
        final Optional<ControllerBlockEntity> oController = self.getController();
        if (oController.isEmpty() || !oController.get().hasTank() || !oController.get().getTankStatus().isActive()) {
            return;
        }
        boolean markDirty = false;
        markDirty |= self.validateController(level);
        markDirty |= self.tickDecoding(level);
        // TODO update progress
        // mark changed and send update
        if(markDirty) {
            self.setChanged();
            level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
        }
    }

    private boolean tickDecoding(final Level level) {
        int oldProgress = progress;
        int oldMaxProgress = maxProgress;
        // verify decoding
        if(isDecodingComplete() || !hasDecodingItems(level)) {
            progress = 0;
            maxProgress = 0;
        }
        // increment decoding
        if(++progress >= maxProgress) {
            progress = 0;
            maxProgress = 0;
            ItemStack result = createResult(level);
            setItem(1, result);
        }
        // report changes
        return progress != oldProgress || maxProgress != oldMaxProgress;
    }

    private ItemStack createResult(final Level level) {
        ItemStack result = new ItemStack(RESULT_ITEM.get());
        // TODO write to NBT
        return result;
    }

    public boolean hasDecodingItems(final Level level) {
        // validate axolootl
        ItemStack axolootl = getItem(0);
        if(axolootl.isEmpty() || AxolootlBucketItem.getVariant(level.registryAccess(), axolootl).isEmpty()) {
            return false;
        }
        // validate book
        return getItem(1).is(BOOK_WHITELIST);
    }

    public boolean isDecodingComplete() {
        return getItem(1).is(RESULT_ITEM.get());
    }

    //// MENU PROVIDER ////

    @Override
    public boolean isMenuAvailable(Player player, ControllerBlockEntity controller) {
        return AxRegistry.AquariumTabsReg.AXOLOOTL_INSPECTOR.get().isAvailable(controller);
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int pContainerId, Inventory pPlayerInventory, Player pPlayer) {
        return CyclingContainerMenu.createInspector(pContainerId, pPlayerInventory, this.controllerPos, this.controller, this.getBlockPos(), AxRegistry.AquariumTabsReg.AXOLOOTL_INSPECTOR.get().getSortedIndex(), -1);
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
                if(slot == 0) {
                    return stack.is(ITEM_WHITELIST);
                }
                return stack.is(BOOK_WHITELIST) || stack.is(RESULT_ITEM.get());
            }
        };
    }
}
