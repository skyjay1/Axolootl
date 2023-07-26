/**
 * Copyright (c) 2023 Skyler James
 * Permission is granted to use, modify, and redistribute this software, in parts or in whole,
 * under the GNU LGPLv3 license (https://www.gnu.org/licenses/lgpl-3.0.en.html)
 **/

package axolootl.menu;

import axolootl.AxRegistry;
import axolootl.block.entity.AxolootlInterfaceBlockEntity;
import axolootl.block.entity.ControllerBlockEntity;
import axolootl.data.axolootl_variant.AxolootlVariant;
import axolootl.network.AxNetwork;
import axolootl.network.ServerBoundExtractAxolootlPacket;
import axolootl.network.ServerBoundInsertAxolootlPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AxolootlMenu extends AbstractControllerMenu {

    public static final int INV_X = 30;
    public static final int INV_Y = 108;
    public static int INV_SIZE = 6;

    public static final int PLAYER_INV_X = 30;
    public static final int PLAYER_INV_Y = 140;

    private Container container;
    private Map<AxolootlVariant, Integer> variantCountMap;
    private int totalCount;

    public AxolootlMenu(int windowId, Inventory inv, BlockPos controllerPos, ControllerBlockEntity blockEntity, BlockPos blockPos, int tab, int cycle) {
        super(AxRegistry.MenuReg.AXOLOOTL.get(), windowId, inv, controllerPos, blockEntity, blockPos, tab, cycle);
        this.variantCountMap = new HashMap<>();
        if(blockEntity.getLevel().getBlockEntity(blockPos) instanceof Container c && c.getContainerSize() == INV_SIZE) {
            this.container = c;
        } else {
            this.container = new SimpleContainer(INV_SIZE);
        }
        // add container slots
        for(int i = 0; i < INV_SIZE; i++) {
            addSlot(new AxolootlSlot(this.container, i, INV_X + i * 18, INV_Y));
        }
        addPlayerSlots(inv, PLAYER_INV_X, PLAYER_INV_Y);
        // update variant maps
        updateVariantMaps();
    }

    public void updateVariantMaps() {
        if(null == controller) {
            return;
        }
        // resolve UUID to variant map
        final Map<UUID, AxolootlVariant> variantMap = new HashMap<>();
        variantMap.putAll(controller.resolveAxolootlVariants(getInventory().player.level.registryAccess()));
        this.totalCount = variantMap.size();
        // build variant to UUID map
        variantCountMap.clear();
        for(Map.Entry<UUID, AxolootlVariant> entry : variantMap.entrySet()) {
            int count = this.variantCountMap.getOrDefault(entry.getValue(), 0);
            this.variantCountMap.put(entry.getValue(), count + 1);
        }
    }

    public int getTotalCount() {
        return totalCount;
    }

    public Map<AxolootlVariant, Integer> getVariantCountMap() {
        return variantCountMap;
    }

    public void extract(AxolootlVariant variant) {
        // validate variant
        if(variant == AxolootlVariant.EMPTY) {
            return;
        }
        // validate client side
        if(getInventory().player.level.isClientSide()) {
            // send packet to server
            AxNetwork.CHANNEL.sendToServer(new ServerBoundExtractAxolootlPacket(variant.getRegistryName(getInventory().player.level.registryAccess())));
            // remove from local maps
            int count = variantCountMap.getOrDefault(variant, 0);
            if(count > 1) {
                variantCountMap.put(variant, Math.max(0, count - 1));
            } else {
                variantCountMap.remove(variant);
            }
        }
    }

    public void insert() {
        // validate client side
        if(getInventory().player.level.isClientSide()) {
            // send packet to server
            AxNetwork.CHANNEL.sendToServer(new ServerBoundInsertAxolootlPacket());
        }
    }

    @Override
    public boolean hasPlayerSlots() {
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player pPlayer, int pIndex) {
       return quickMoveStack(pPlayer, pIndex, INV_SIZE);
    }

    @Override
    public boolean stillValid(Player pPlayer) {
        return this.container.stillValid(pPlayer);
    }

    public Container getContainer() {
        return container;
    }

    //// SLOTS ////

    private static class AxolootlSlot extends Slot {

        public AxolootlSlot(Container pContainer, int pSlot, int pX, int pY) {
            super(pContainer, pSlot, pX, pY);
        }

        @Override
        public boolean mayPlace(ItemStack pStack) {
            return pStack.is(AxolootlInterfaceBlockEntity.ITEM_WHITELIST);
        }
    }
}
