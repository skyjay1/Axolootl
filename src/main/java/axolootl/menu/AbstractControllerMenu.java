package axolootl.menu;

import axolootl.block.entity.ControllerBlockEntity;
import axolootl.block.entity.IAquariumControllerProvider;
import axolootl.network.AxNetwork;
import axolootl.network.ServerBoundControllerTabPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.Level;

import java.util.Optional;

public abstract class AbstractControllerMenu extends AbstractContainerMenu implements IAquariumControllerProvider {

    private final Inventory inventory;
    private BlockPos controllerPos;
    private ControllerBlockEntity controller;
    private int tab;

    public AbstractControllerMenu(MenuType<?> menuType, int windowId, Inventory inv, BlockPos pos, ControllerBlockEntity blockEntity, int tab) {
        super(menuType, windowId);
        this.inventory = inv;
        this.controllerPos = pos;
        this.controller = blockEntity;
        this.tab = tab;
        if(!TabType.getByIndex(tab).isAvailable(controller)) {
            this.tab = 0;
        }
    }

    //// TAB ////

    public int getTab() {
        return tab;
    }

    public void setTab(int tab) {
        if(this.tab != tab) {
            AxNetwork.CHANNEL.sendToServer(new ServerBoundControllerTabPacket(tab));
        }
        this.tab = tab;
    }

    public void openMenuForTab() {
        final TabType tabType = TabType.getByIndex(this.tab);
        if(this.controller != null && tabType.isAvailable(this.controller)) {
            // TODO send packet to server to change tab
        }

    }

    //// CONTAINER MENU ////

    public Inventory getInventory() {
        return inventory;
    }

    //// CONTROLLER PROVIDER ////

    @Override
    public void setController(Level level, BlockPos pos, ControllerBlockEntity blockEntity) {
        this.controllerPos = pos;
        this.controller = blockEntity;
    }

    @Override
    public void clearController() {
        this.controllerPos = null;
        this.controller = null;
    }

    @Override
    public Optional<ControllerBlockEntity> getController() {
        return Optional.ofNullable(this.controller);
    }
}
