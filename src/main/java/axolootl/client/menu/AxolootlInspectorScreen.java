/**
 * Copyright (c) 2023 Skyler James
 * Permission is granted to use, modify, and redistribute this software, in parts or in whole,
 * under the GNU LGPLv3 license (https://www.gnu.org/licenses/lgpl-3.0.en.html)
 **/

package axolootl.client.menu;

import axolootl.Axolootl;
import axolootl.block.entity.ControllerBlockEntity;
import axolootl.menu.AxolootlInspectorMenu;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;

public class AxolootlInspectorScreen extends AbstractCyclingScreen<AxolootlInspectorMenu> {

    // TEXTURES //
    public static final ResourceLocation INSPECTOR_BG = new ResourceLocation(Axolootl.MODID, "textures/gui/aquarium/axolootl_inspector.png");

    // WIDGET CONSTANTS //
    private static final int PROGRESS_X = 101;
    private static final int PROGRESS_Y = 17;
    private static final int PROGRESS_WIDTH = 20;
    private static final int PROGRESS_HEIGHT = 20;

    // DATA //
    private int scaledProgress;

    // COMPONENTS //

    public AxolootlInspectorScreen(AxolootlInspectorMenu pMenu, Inventory pPlayerInventory, Component pTitle) {
        super(pMenu, pPlayerInventory, pTitle);
    }

    @Override
    protected void init() {
        super.init();
        containerTick();
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        scaledProgress = getMenu().hasProgress() ? Mth.ceil(getMenu().getProgress() * PROGRESS_WIDTH) : 0;
    }

    @Override
    protected void renderBgTexture(PoseStack poseStack, float partialTick, int mouseX, int mouseY) {
        super.renderBgTexture(poseStack, partialTick, mouseX, mouseY);
        // render bg
        RenderSystem.setShaderTexture(0, INSPECTOR_BG);
        blit(poseStack, this.leftPos, this.topPos, 0, 0, WIDTH, HEIGHT);
        // render progress
        renderProgress(poseStack, mouseX, mouseY, partialTick);
    }

    @Override
    public void render(PoseStack pPoseStack, int pMouseX, int pMouseY, float pPartialTick) {
        super.render(pPoseStack, pMouseX, pMouseY, pPartialTick);
        renderHoverActions(pPoseStack, pMouseX, pMouseY, pPartialTick);
    }

    private void renderProgress(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        // render progress
        RenderSystem.setShaderTexture(0, WIDGETS);
        blit(poseStack, this.leftPos + PROGRESS_X, this.topPos + PROGRESS_Y, 74, 50, PROGRESS_WIDTH, PROGRESS_HEIGHT);
        if(scaledProgress > 0) {
            blit(poseStack, this.leftPos + PROGRESS_X, this.topPos + PROGRESS_Y, 74, 50 + PROGRESS_HEIGHT, scaledProgress, PROGRESS_HEIGHT);
        }
    }

    private void renderHoverActions(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {

    }
}
