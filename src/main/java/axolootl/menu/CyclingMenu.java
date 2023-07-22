/**
 * Copyright (c) 2023 Skyler James
 * Permission is granted to use, modify, and redistribute this software, in parts or in whole,
 * under the GNU LGPLv3 license (https://www.gnu.org/licenses/lgpl-3.0.en.html)
 **/

package axolootl.menu;

import axolootl.AxRegistry;
import axolootl.block.entity.ControllerBlockEntity;
import com.google.common.collect.ImmutableList;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class CyclingMenu extends AbstractControllerMenu {

    public static final int PLAYER_INV_X = 30;
    public static final int PLAYER_INV_Y = 140;

    public final int cycleCount;
    private final List<BlockPos> sortedCycleList;
    private final boolean hasPlayerSlots;

    public CyclingMenu(MenuType<?> menuType, int windowId, Inventory inv,
                       BlockPos controllerPos, ControllerBlockEntity controller, BlockPos blockPos,
                       int tab, int cycle, Collection<BlockPos> positions, boolean hasPlayerSlots) {
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
        // add player inventory
        this.hasPlayerSlots = hasPlayerSlots;
        if(hasPlayerSlots) {
            addPlayerSlots(inv, PLAYER_INV_X, PLAYER_INV_Y);
        }
    }

    public static CyclingMenu createEnergy(int windowId, Inventory inv, BlockPos controllerPos, ControllerBlockEntity controller, BlockPos blockPos, int tab, int cycle) {
        return new CyclingMenu(AxRegistry.MenuReg.ENERGY.get(), windowId, inv, controllerPos, controller, blockPos, tab, cycle, controller.getEnergyInputs(), true);
    }

    public boolean hasPlayerSlots() {
        return hasPlayerSlots;
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
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player pPlayer) {
        if(this.cycle < 0 || this.cycle >= this.getSortedCycleList().size()) {
            return false;
        }
        if(this.getController().isPresent()) {
            return true;
        }
        BlockPos pos = this.getSortedCycleList().get(this.cycle);
        return pPlayer.position().closerThan(Vec3.atCenterOf(pos), 8.0D);
    }
}
