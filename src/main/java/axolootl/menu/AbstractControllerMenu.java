package axolootl.menu;

import axolootl.AxRegistry;
import axolootl.block.entity.ControllerBlockEntity;
import axolootl.block.entity.IAquariumControllerProvider;
import axolootl.network.AxNetwork;
import axolootl.network.ServerBoundControllerCyclePacket;
import axolootl.network.ServerBoundControllerTabPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Optional;

public abstract class AbstractControllerMenu extends AbstractContainerMenu implements IAquariumControllerProvider {

    protected final Inventory inventory;
    protected BlockPos controllerPos;
    protected ControllerBlockEntity controller;
    protected BlockPos blockPos;
    protected int tab;
    protected int cycle;

    public AbstractControllerMenu(MenuType<?> menuType, int windowId, Inventory inv, BlockPos controllerPos, ControllerBlockEntity blockEntity, BlockPos blockPos, int tab, int cycle) {
        super(menuType, windowId);
        this.inventory = inv;
        this.controllerPos = controllerPos;
        this.controller = blockEntity;
        this.blockPos = blockPos;
        this.tab = validateTab(tab);
        if(!AxRegistry.AquariumTabsReg.getSortedTabs().get(tab).isAvailable(controller)) {
            this.tab = AxRegistry.AquariumTabsReg.CONTROLLER.get().getSortedIndex();
        }
        this.cycle = cycle;
    }

    //// TAB ////

    public int validateTab(int tab) {
        return Mth.clamp(tab, 0, AxRegistry.AquariumTabsReg.getTabCount() - 1);
    }

    public int getTab() {
        return tab;
    }

    public void setTab(int tab) {
        tab = validateTab(tab);
        if(this.tab != tab && this.inventory.player.level.isClientSide() && this.controller != null
                && AxRegistry.AquariumTabsReg.getSortedTabs().get(tab).isAvailable(this.controller)) {
            // send packet to server to change tab
            AxNetwork.CHANNEL.sendToServer(new ServerBoundControllerTabPacket(tab));
        }
        this.tab = tab;
    }

    //// CYCLE ////

    public int getCycle() {
        return cycle;
    }

    public void cycle(final int amount) {
        // validate amount
        if(amount == 0) {
            return;
        }
        // increase cycle
        this.cycle = (this.cycle + getMaxCycle() + amount) % getMaxCycle();
        if(this.inventory.player.level.isClientSide() && this.controller != null) {
            // send packet to server to change cycle
            AxNetwork.CHANNEL.sendToServer(new ServerBoundControllerCyclePacket(this.cycle));
        }
    }

    public int getMaxCycle() {
        return 1;
    }

    public List<BlockPos> getSortedCycleList() {
        return List.of(blockPos);
    }

    //// SLOTS ////

    protected void addPlayerSlots(Inventory inv, int startX, int startY) {
        // add player inventory
        for (int inventoryY = 0; inventoryY < 3; inventoryY++) {
            for (int inventoryX = 0; inventoryX < 9; inventoryX++) {
                addSlot(new Slot(inv, inventoryX + inventoryY * 9 + 9, startX + inventoryX * 18, startY + inventoryY * 18));
            }
        }
        // add player hotbar
        for (int hotbarSlot = 0; hotbarSlot < 9; hotbarSlot++) {
            addSlot(new Slot(inv, hotbarSlot, startX + hotbarSlot * 18, startY + 3 * 18 + 4));
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

    //// GETTERS ////

    public BlockPos getBlockPos() {
        return blockPos;
    }

    public BlockPos getControllerPos() {
        return controllerPos;
    }
}
