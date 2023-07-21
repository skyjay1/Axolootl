package axolootl.menu;

import axolootl.AxRegistry;
import axolootl.block.entity.ControllerBlockEntity;
import com.google.common.collect.ImmutableList;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class CyclingInventoryMenu extends AbstractControllerMenu {

    public static final int INV_X = 30;
    public static final int INV_Y = 18;

    public static final int PLAYER_INV_X = 30;
    public static final int PLAYER_INV_Y = 140;

    public final int cycleCount;
    private final List<BlockPos> sortedCycleList;
    private int containerSize;
    private Container container;

    public CyclingInventoryMenu(MenuType<?> menuType, int windowId, Inventory inv,
                                BlockPos controllerPos, ControllerBlockEntity controller, BlockPos blockPos,
                                int tab, int cycle, Collection<BlockPos> positions) {
        super(menuType, windowId, inv, controllerPos, controller, blockPos, tab, cycle);
        // calculate sorted cycle list
        final ArrayList<BlockPos> builder = new ArrayList<>(positions);
        builder.sort(BlockPos::compareTo);
        this.sortedCycleList = ImmutableList.copyOf(builder);
        // calculate cycle max
        this.cycleCount = sortedCycleList.size();
        // calculate cycle
        if(cycle < 0) {
            this.cycle = sortedCycleList.indexOf(blockPos);
        }
        // add slots
        addBlockSlots(blockPos);
        // add player slots
        addPlayerSlots(inv, PLAYER_INV_X, PLAYER_INV_Y);
    }

    public static CyclingInventoryMenu createOutput(int windowId, Inventory inv, BlockPos controllerPos, ControllerBlockEntity controller, BlockPos blockPos, int tab, int cycle) {
        return new CyclingInventoryMenu(AxRegistry.MenuReg.OUTPUT.get(), windowId, inv, controllerPos, controller, blockPos, tab, cycle, controller.getResourceOutputs());
    }

    public static CyclingInventoryMenu createLargeOutput(int windowId, Inventory inv, BlockPos controllerPos, ControllerBlockEntity controller, BlockPos blockPos, int tab, int cycle) {
        return new CyclingInventoryMenu(AxRegistry.MenuReg.LARGE_OUTPUT.get(), windowId, inv, controllerPos, controller, blockPos, tab, cycle, controller.getResourceOutputs());
    }

    public static CyclingInventoryMenu createFeeder(int windowId, Inventory inv, BlockPos controllerPos, ControllerBlockEntity controller, BlockPos blockPos, int tab, int cycle) {
        return new CyclingInventoryMenu(AxRegistry.MenuReg.AUTOFEEDER.get(), windowId, inv, controllerPos, controller, blockPos, tab, cycle, controller.resolveModifiers(controller.getLevel().registryAccess(), controller.activePredicate.and(controller.foodInterfacePredicate)).keySet());
    }

    public static CyclingInventoryMenu createBreeder(int windowId, Inventory inv, BlockPos controllerPos, ControllerBlockEntity controller, BlockPos blockPos, int tab, int cycle) {
        return new CyclingInventoryMenu(AxRegistry.MenuReg.BREEDER.get(), windowId, inv, controllerPos, controller, blockPos, tab, cycle, controller.resolveModifiers(controller.getLevel().registryAccess(), controller.activePredicate.and(controller.foodInterfacePredicate)).keySet());
    }

    public static CyclingInventoryMenu createMonsterium(int windowId, Inventory inv, BlockPos controllerPos, ControllerBlockEntity controller, BlockPos blockPos, int tab, int cycle) {
        return new CyclingInventoryMenu(AxRegistry.MenuReg.MONSTERIUM.get(), windowId, inv, controllerPos, controller, blockPos, tab, cycle, controller.resolveModifiers(controller.getLevel().registryAccess(), controller.activePredicate.and(controller.foodInterfacePredicate)).keySet());
    }

    protected void addBlockSlots(BlockPos blockPos) {
        // load block entity
        final BlockEntity blockEntity = controller.getLevel().getBlockEntity(blockPos);
        if(!(blockEntity instanceof Container container)) {
            return;
        }
        this.container = container;
        this.containerSize = container.getContainerSize();
        // add item slots
        for(int i = 0, x = 0, y = 0; i < this.containerSize; i++) {
            x = INV_X + (i % 9) * 18;
            y = INV_Y + (i / 9) * 18;
            this.addSlot(new Slot(container, i, x, y));
        }
    }

    public boolean isLargeOutput() {
        return this.getType() == AxRegistry.MenuReg.LARGE_OUTPUT.get();
    }

    @Override
    public int getMaxCycle() {
        return cycleCount;
    }

    @Override
    public List<BlockPos> getSortedCycleList() {
        return this.sortedCycleList;
    }

    @Override
    public ItemStack quickMoveStack(Player pPlayer, int pIndex) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(pIndex);
        if (slot != null && slot.hasItem()) {
            ItemStack itemstack1 = slot.getItem();
            itemstack = itemstack1.copy();
            if (pIndex < this.containerSize) {
                if (!this.moveItemStackTo(itemstack1, this.containerSize, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(itemstack1, 0, this.containerSize, false)) {
                return ItemStack.EMPTY;
            }

            if (itemstack1.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }

        return itemstack;
    }

    @Override
    public boolean stillValid(Player pPlayer) {
        if(this.container != null) {
            return this.container.stillValid(pPlayer);
        }
        return false;
    }
}
