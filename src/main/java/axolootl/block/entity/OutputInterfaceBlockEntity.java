/**
 * Copyright (c) 2023 Skyler James
 * Permission is granted to use, modify, and redistribute this software, in parts or in whole,
 * under the GNU LGPLv3 license (https://www.gnu.org/licenses/lgpl-3.0.en.html)
 **/

package axolootl.block.entity;

import axolootl.AxRegistry;
import axolootl.block.OutputBlock;
import axolootl.menu.CyclingContainerMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class OutputInterfaceBlockEntity extends InterfaceBlockEntity {

    public OutputInterfaceBlockEntity(BlockPos pPos, BlockState pBlockState) {
        this(AxRegistry.BlockEntityReg.OUTPUT_INTERFACE.get(), pPos, pBlockState);
    }

    public OutputInterfaceBlockEntity(BlockEntityType<?> pType, BlockPos pPos, BlockState pBlockState) {
        super(pType, pPos, pBlockState, (pBlockState.getBlock() instanceof OutputBlock b) ? b.inventoryRows : 0);
    }

    @Override
    public boolean isMenuAvailable(Player player, ControllerBlockEntity controller) {
        return AxRegistry.AquariumTabsReg.OUTPUT.get().isAvailable(controller);
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int pContainerId, Inventory pPlayerInventory, Player pPlayer) {
        // verify availability
        if(getController().isEmpty() || !isMenuAvailable(pPlayer, this.controller)) {
            return super.createMenu(pContainerId, pPlayerInventory, pPlayer);
        }
        switch (this.rows) {
            case 3: return CyclingContainerMenu.createOutput(pContainerId, pPlayerInventory, this.controllerPos, this.controller, this.getBlockPos(), AxRegistry.AquariumTabsReg.OUTPUT.get().getSortedIndex(), -1);
            case 6: return CyclingContainerMenu.createLargeOutput(pContainerId, pPlayerInventory, this.controllerPos, this.controller, this.getBlockPos(), AxRegistry.AquariumTabsReg.OUTPUT.get().getSortedIndex(), -1);
            default: return null;
        }
    }
}
