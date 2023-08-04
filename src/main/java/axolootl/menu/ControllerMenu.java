/**
 * Copyright (c) 2023 Skyler James
 * Permission is granted to use, modify, and redistribute this software, in parts or in whole,
 * under the GNU LGPLv3 license (https://www.gnu.org/licenses/lgpl-3.0.en.html)
 **/

package axolootl.menu;

import axolootl.AxRegistry;
import axolootl.block.entity.ControllerBlockEntity;
import axolootl.network.AxNetwork;
import axolootl.network.ServerBoundActivateControllerPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class ControllerMenu extends AbstractControllerMenu {

    public ControllerMenu(int windowId, Inventory inv, BlockPos controllerPos, ControllerBlockEntity blockEntity, BlockPos blockPos, int tab, int cycle) {
        super(AxRegistry.MenuReg.CONTROLLER.get(), windowId, inv, controllerPos, blockEntity, blockPos, tab, cycle);
    }

    @Override
    public ItemStack quickMoveStack(Player pPlayer, int pIndex) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player pPlayer) {
        return controller != null;
    }

    public void activate() {
        if(getInventory().player.level.isClientSide()) {
            // send packet to server
            AxNetwork.CHANNEL.sendToServer(new ServerBoundActivateControllerPacket(getControllerPos()));
        }
    }
}
