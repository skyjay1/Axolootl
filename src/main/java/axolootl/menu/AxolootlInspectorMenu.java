/**
 * Copyright (c) 2023 Skyler James
 * Permission is granted to use, modify, and redistribute this software, in parts or in whole,
 * under the GNU LGPLv3 license (https://www.gnu.org/licenses/lgpl-3.0.en.html)
 **/

package axolootl.menu;

import axolootl.AxRegistry;
import axolootl.Axolootl;
import axolootl.block.entity.AxolootlInspectorBlockEntity;
import axolootl.block.entity.ControllerBlockEntity;
import axolootl.data.axolootl_variant.AxolootlVariant;
import axolootl.entity.AxolootlEntity;
import axolootl.item.AxolootlBucketItem;
import axolootl.network.AxNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.function.Consumer;

public class AxolootlInspectorMenu extends CyclingContainerMenu {

    public static final int INV_X = 79;
    public static final int INV_Y = 19;

    private AxolootlInspectorBlockEntity blockEntity;
    private ContainerData data;
    private boolean isChanged;

    public AxolootlInspectorMenu(int windowId, Inventory inv, BlockPos controllerPos,
                                 ControllerBlockEntity controller, BlockPos blockPos,
                                 int tab, int cycle) {
        super(AxRegistry.MenuReg.INSPECTOR.get(), windowId, inv, controllerPos, controller, blockPos, tab, cycle, controller.getTrackedBlocks(AxRegistry.AquariumTabsReg.AXOLOOTL_INSPECTOR.getId()));
    }

    @Override
    protected void addBlockSlots(BlockPos blockPos) {
        this.data = new SimpleContainerData(2);
        // load block entity
        if(!(controller.getLevel().getBlockEntity(blockPos) instanceof AxolootlInspectorBlockEntity blockEntity)) {
            return;
        }
        // load container
        this.blockEntity = blockEntity;
        this.container = blockEntity;
        this.containerSize = this.container.getContainerSize();
        this.containerRows = 1;
        // add container data
        this.data = blockEntity.getDataAccess();
        addDataSlots(this.data);
        // add container slots
        addSlot(new AxolootlSlot(this.container, 0, INV_X, INV_Y));
        addSlot(new ResultSlot(this.container, 1, INV_X + 48, INV_Y, this::setChanged));
    }

    @Nullable
    public AxolootlInspectorBlockEntity getBlockEntity() {
        return blockEntity;
    }

    /**
     * @return true to show the progress overlay
     */
    public boolean hasProgress() {
        return this.data.get(0) > 0;
    }

    /**
     * @return a number between 0 and 1.0 to describe the percentage toward completion
     */
    public float getProgress() {
        return (float) this.data.get(0) / (float) Math.max(1, this.data.get(1));
    }

    public boolean isChanged() {
        return isChanged;
    }

    public void setChanged(boolean changed) {
        isChanged = changed;
    }

    //// SLOTS ////

    protected static class AxolootlSlot extends Slot {

        public AxolootlSlot(Container pContainer, int pSlot, int pX, int pY) {
            super(pContainer, pSlot, pX, pY);
        }

        @Override
        public boolean mayPlace(ItemStack pStack) {
            return pStack.hasTag() && pStack.getTag().contains(AxolootlEntity.KEY_VARIANT_ID, Tag.TAG_STRING);
        }
    }

    protected static class ResultSlot extends Slot {

        private final Consumer<Boolean> setChanged;

        public ResultSlot(Container pContainer, int pSlot, int pX, int pY, Consumer<Boolean> setChanged) {
            super(pContainer, pSlot, pX, pY);
            this.setChanged = setChanged;
        }

        @Override
        public void onTake(Player pPlayer, ItemStack pStack) {
            Optional<AxolootlVariant> oVariant = AxolootlBucketItem.getVariant(pPlayer.level.registryAccess(), pStack);
            if(oVariant.isPresent()) {
                // update capability
                addAxolootlResearch(pPlayer, oVariant.get().getRegistryName(pPlayer.level.registryAccess()));
            }
            super.onTake(pPlayer, pStack);
        }

        @Override
        public boolean mayPlace(ItemStack pStack) {
            return false;
        }

        protected void addAxolootlResearch(Player player, ResourceLocation id) {
            // load capability
            player.getCapability(Axolootl.AXOLOOTL_RESEARCH_CAPABILITY).ifPresent(c -> {
                // add variant (on both sides) and send sync (on server)
                if(c.addAxolootl(id) && player instanceof ServerPlayer serverPlayer) {
                    player.playSound(SoundEvents.PLAYER_LEVELUP);
                    c.syncToClient(serverPlayer);
                }
            });
            // set changed
            this.setChanged.accept(true);
        }
    }
}
