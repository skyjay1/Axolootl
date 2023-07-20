package axolootl.menu;

import axolootl.AxRegistry;
import axolootl.block.entity.ControllerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class ControllerMenu extends AbstractControllerMenu {


    public ControllerMenu(int windowId, Inventory inv, BlockPos pos, ControllerBlockEntity blockEntity, int tab) {
        super(AxRegistry.MenuReg.CONTROLLER.get(), windowId, inv, pos, blockEntity, tab);
    }

    @Override
    public ItemStack quickMoveStack(Player pPlayer, int pIndex) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player pPlayer) {
        return true;
    }
}
