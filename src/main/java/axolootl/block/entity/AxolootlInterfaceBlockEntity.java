/**
 * Copyright (c) 2023 Skyler James
 * Permission is granted to use, modify, and redistribute this software, in parts or in whole,
 * under the GNU LGPLv3 license (https://www.gnu.org/licenses/lgpl-3.0.en.html)
 **/

package axolootl.block.entity;

import axolootl.AxRegistry;
import axolootl.Axolootl;
import axolootl.menu.AxolootlInterfaceMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.wrapper.InvWrapper;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AxolootlInterfaceBlockEntity extends InterfaceBlockEntity {

    public static final TagKey<Item> ITEM_WHITELIST = ForgeRegistries.ITEMS.tags().createTagKey(new ResourceLocation(Axolootl.MODID, "axolootl_interface_whitelist"));

    public AxolootlInterfaceBlockEntity(BlockPos pPos, BlockState pBlockState) {
        this(AxRegistry.BlockEntityReg.AXOLOOTL_INTERFACE.get(), pPos, pBlockState);
    }

    public AxolootlInterfaceBlockEntity(BlockEntityType<?> pType, BlockPos pPos, BlockState pBlockState) {
        super(pType, pPos, pBlockState, 1, 5);
    }

    @Override
    public boolean isMenuAvailable(Player player, ControllerBlockEntity controller) {
        return getController().isPresent() && AxRegistry.AquariumTabsReg.AXOLOOTL_INTERFACE.get().isAvailable(controller);
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int pContainerId, Inventory pPlayerInventory, Player pPlayer) {
        return new AxolootlInterfaceMenu(pContainerId, pPlayerInventory, this.controllerPos, this.getController().get(), this.getBlockPos(), AxRegistry.AquariumTabsReg.AXOLOOTL_INTERFACE.get().getSortedIndex(), 0);

    }

    @Override
    protected IItemHandler createItemHandler() {
        return new InvWrapper(this) {
            @Override
            public boolean isItemValid(int slot, @NotNull ItemStack stack) {
                return stack.is(ITEM_WHITELIST);
            }
        };
    }

    @Override
    protected boolean exposeItemCapability() {
        return false;
    }
}
