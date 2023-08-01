/**
 * Copyright (c) 2023 Skyler James
 * Permission is granted to use, modify, and redistribute this software, in parts or in whole,
 * under the GNU LGPLv3 license (https://www.gnu.org/licenses/lgpl-3.0.en.html)
 **/

package axolootl.menu;

import axolootl.AxRegistry;
import axolootl.block.entity.AxolootlInspectorBlockEntity;
import axolootl.block.entity.AxolootlInterfaceBlockEntity;
import axolootl.block.entity.ControllerBlockEntity;
import com.google.common.collect.ImmutableList;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class CyclingContainerMenu extends AbstractControllerMenu {

    public static final int INV_X = 30;
    public static final int INV_Y = 18;

    public final int cycleCount;
    protected final List<BlockPos> sortedCycleList;
    protected int containerRows;
    protected int containerSize;
    protected Container container;

    public CyclingContainerMenu(MenuType<?> menuType, int windowId, Inventory inv,
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

    public static CyclingContainerMenu createOutput(int windowId, Inventory inv, BlockPos controllerPos, ControllerBlockEntity controller, BlockPos blockPos, int tab, int cycle) {
        return new CyclingContainerMenu(AxRegistry.MenuReg.OUTPUT.get(), windowId, inv, controllerPos, controller, blockPos, tab, cycle, controller.getResourceOutputs());
    }

    public static CyclingContainerMenu createLargeOutput(int windowId, Inventory inv, BlockPos controllerPos, ControllerBlockEntity controller, BlockPos blockPos, int tab, int cycle) {
        return new CyclingContainerMenu(AxRegistry.MenuReg.LARGE_OUTPUT.get(), windowId, inv, controllerPos, controller, blockPos, tab, cycle, controller.getResourceOutputs());
    }

    public static CyclingContainerMenu createFeeder(int windowId, Inventory inv, BlockPos controllerPos, ControllerBlockEntity controller, BlockPos blockPos, int tab, int cycle) {
        return new CyclingContainerMenu(AxRegistry.MenuReg.AUTOFEEDER.get(), windowId, inv, controllerPos, controller, blockPos, tab, cycle, controller.resolveModifiers(controller.getLevel().registryAccess(), controller.activePredicate.and(controller.foodInterfacePredicate)).keySet());
    }

    public static CyclingContainerMenu createBreeder(int windowId, Inventory inv, BlockPos controllerPos, ControllerBlockEntity controller, BlockPos blockPos, int tab, int cycle) {
        return new CyclingContainerMenu(AxRegistry.MenuReg.BREEDER.get(), windowId, inv, controllerPos, controller, blockPos, tab, cycle, controller.resolveModifiers(controller.getLevel().registryAccess(), controller.activePredicate.and(controller.foodInterfacePredicate)).keySet());
    }

    public static CyclingContainerMenu createMonsterium(int windowId, Inventory inv, BlockPos controllerPos, ControllerBlockEntity controller, BlockPos blockPos, int tab, int cycle) {
        return new CyclingContainerMenu(AxRegistry.MenuReg.MONSTERIUM.get(), windowId, inv, controllerPos, controller, blockPos, tab, cycle, controller.resolveModifiers(controller.getLevel().registryAccess(), controller.activePredicate.and(controller.foodInterfacePredicate)).keySet());
    }

    public static CyclingContainerMenu createFluid(int windowId, Inventory inv, BlockPos controllerPos, ControllerBlockEntity controller, BlockPos blockPos, int tab, int cycle) {
        return new CyclingContainerMenu(AxRegistry.MenuReg.FLUID.get(), windowId, inv, controllerPos, controller, blockPos, tab, cycle, controller.getFluidInputs()) {
            @Override
            protected void addBlockSlots(BlockPos blockPos) {
                // load block entity
                final BlockEntity blockEntity = controller.getLevel().getBlockEntity(blockPos);
                if(!(blockEntity instanceof Container container)) {
                    return;
                }
                // load container
                this.container = container;
                this.containerSize = container.getContainerSize();
                this.containerRows = 1;
                this.addSlot(new FluidItemSlot(container, 0, INV_X, 108, Fluids.WATER));
            }
        };
    }

    public static CyclingContainerMenu createInspector(int windowId, Inventory inv, BlockPos controllerPos, ControllerBlockEntity controller, BlockPos blockPos, int tab, int cycle) {
        return new CyclingContainerMenu(AxRegistry.MenuReg.INSPECTOR.get(), windowId, inv, controllerPos, controller, blockPos, tab, cycle, controller.getTrackedBlocks(AxRegistry.AquariumTabsReg.AXOLOOTL_INSPECTOR.getId())) {
            @Override
            protected void addBlockSlots(BlockPos blockPos) {
                // load block entity
                if(!(controller.getLevel().getBlockEntity(blockPos) instanceof AxolootlInspectorBlockEntity blockEntity)) {
                    return;
                }
                // load container
                this.container = blockEntity;
                this.containerSize = this.container.getContainerSize();
                this.containerRows = 1;
                // TODO add data slots
                // add container slots
                addSlot(new AxolootlSlot(this.container, 0, INV_X, 90));
                addSlot(new BookSlot(this.container, 1, INV_X, 47));
            }
        };
    }

    protected void addBlockSlots(BlockPos blockPos) {
        // load block entity
        final BlockEntity blockEntity = controller.getLevel().getBlockEntity(blockPos);
        if(!(blockEntity instanceof Container container)) {
            return;
        }
        this.container = container;
        this.containerSize = container.getContainerSize();
        this.containerRows = Math.min(6, Mth.ceil(this.containerSize / 9.0F));
        // add item slots
        for(int i = 0, x = 0, y = 0; i < this.containerSize; i++) {
            x = INV_X + (i % 9) * 18;
            y = INV_Y + (i / 9) * 18;
            this.addSlot(new Slot(container, i, x, y));
        }
    }

    public int getRows() {
        return this.containerRows;
    }

    @Nullable
    public Container getContainer() {
        return this.container;
    }

    public int getContainerSize() {
        return this.containerSize;
    }

    @Override
    public boolean hasPlayerSlots() {
        return true;
    }

    @Override
    public int getCycleCount() {
        return cycleCount;
    }

    @Override
    public List<BlockPos> getSortedCycleList() {
        return this.sortedCycleList;
    }

    @Override
    public ItemStack quickMoveStack(Player pPlayer, int pIndex) {
        return quickMoveStack(pPlayer, pIndex, this.containerSize);
    }

    @Override
    public boolean stillValid(Player pPlayer) {
        if(this.container != null) {
            return this.container.stillValid(pPlayer);
        }
        return false;
    }

    //// SLOTS ////

    protected static class FluidItemSlot extends Slot {

        private final Set<Fluid> fluids;

        public FluidItemSlot(Container pContainer, int pSlot, int pX, int pY, Fluid... fluids) {
            super(pContainer, pSlot, pX, pY);
            this.fluids = Set.of(fluids);
        }

        @Override
        public boolean mayPlace(ItemStack pStack) {
            // load capability
            LazyOptional<IFluidHandlerItem> oHandler = pStack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM);
            if(!oHandler.isPresent()) {
                return false;
            }
            // resolve capability
            IFluidHandlerItem handler = oHandler.resolve().get();
            FluidStack fluidStack = handler.getFluidInTank(0);
            // validate fluid
            if(!fluidStack.isEmpty() && !this.fluids.contains(fluidStack.getFluid())) {
                return false;
            }
            // all checks passed
            return true;
        }
    }

    protected static class AxolootlSlot extends Slot {

        public AxolootlSlot(Container pContainer, int pSlot, int pX, int pY) {
            super(pContainer, pSlot, pX, pY);
        }

        @Override
        public boolean mayPlace(ItemStack pStack) {
            return pStack.is(AxolootlInterfaceBlockEntity.ITEM_WHITELIST);
        }
    }

    protected static class BookSlot extends Slot {

        public BookSlot(Container pContainer, int pSlot, int pX, int pY) {
            super(pContainer, pSlot, pX, pY);
        }

        @Override
        public boolean mayPlace(ItemStack pStack) {
            return pStack.is(AxolootlInspectorBlockEntity.BOOK_WHITELIST);
        }
    }
}
