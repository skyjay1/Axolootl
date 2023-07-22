/**
 * Copyright (c) 2023 Skyler James
 * Permission is granted to use, modify, and redistribute this software, in parts or in whole,
 * under the GNU LGPLv3 license (https://www.gnu.org/licenses/lgpl-3.0.en.html)
 **/

package axolootl.block.entity;

import axolootl.AxRegistry;
import axolootl.menu.CyclingContainerMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class BreederBlockEntity extends InterfaceBlockEntity {

    public BreederBlockEntity(BlockPos pPos, BlockState pBlockState) {
        this(AxRegistry.BlockEntityReg.BREEDER.get(), pPos, pBlockState);
    }

    public BreederBlockEntity(BlockEntityType<?> pType, BlockPos pPos, BlockState pBlockState) {
        super(pType, pPos, pBlockState, 3);
    }

    @Override
    public boolean isMenuAvailable(Player player, ControllerBlockEntity controller) {
        return controller.hasTank();
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int pContainerId, Inventory pPlayerInventory, Player pPlayer) {
        // verify availability
        if(!isMenuAvailable(pPlayer, this.controller)) {
            return super.createMenu(pContainerId, pPlayerInventory, pPlayer);
        }
        return CyclingContainerMenu.createBreeder(pContainerId, pPlayerInventory, this.controllerPos, this.controller, this.getBlockPos(), AxRegistry.AquariumTabsReg.FOOD_INTERFACE.get().getSortedIndex(), -1);
    }
}