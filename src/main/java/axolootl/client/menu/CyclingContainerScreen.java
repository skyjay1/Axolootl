/**
 * Copyright (c) 2023 Skyler James
 * Permission is granted to use, modify, and redistribute this software, in parts or in whole,
 * under the GNU LGPLv3 license (https://www.gnu.org/licenses/lgpl-3.0.en.html)
 **/

package axolootl.client.menu;

import axolootl.menu.CyclingContainerMenu;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class CyclingContainerScreen extends AbstractCyclingScreen<CyclingContainerMenu> {

    // DATA //
    protected int textureHeight;

    public CyclingContainerScreen(CyclingContainerMenu pMenu, Inventory pPlayerInventory, Component pTitle) {
        super(pMenu, pPlayerInventory, pTitle);
        this.inventoryLabelY = this.imageHeight - 94;
        this.inventoryLabelX = CyclingContainerMenu.PLAYER_INV_X;
        this.textureHeight = getMenu().getRows() * 18;
    }

    protected void renderContainerSlots(PoseStack pPoseStack, float pPartialTick, int pMouseX, int pMouseY) {
        RenderSystem.setShaderTexture(0, SLOTS);
        blit(pPoseStack, this.leftPos + CyclingContainerMenu.INV_X - 1, this.topPos + CyclingContainerMenu.INV_Y - 1, CyclingContainerMenu.INV_X - 1, CyclingContainerMenu.INV_Y - 1, 9 * 18, textureHeight);
    }

    @Override
    protected void init() {
        super.init();
    }

    @Override
    protected void renderBg(PoseStack pPoseStack, float pPartialTick, int pMouseX, int pMouseY) {
        super.renderBg(pPoseStack, pPartialTick, pMouseX, pMouseY);
        renderContainerSlots(pPoseStack, pPartialTick, pMouseX, pMouseY);
    }

    @Override
    protected void renderLabels(PoseStack pPoseStack, int pMouseX, int pMouseY) {
        this.font.draw(pPoseStack, this.cycledTitle, this.titleLabelX, this.titleLabelY, 0x404040);
        if(getMenu().hasPlayerSlots()) {
            this.font.draw(pPoseStack, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0x404040);
        }
    }
}
