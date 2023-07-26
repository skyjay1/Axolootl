/**
 * Copyright (c) 2023 Skyler James
 * Permission is granted to use, modify, and redistribute this software, in parts or in whole,
 * under the GNU LGPLv3 license (https://www.gnu.org/licenses/lgpl-3.0.en.html)
 **/

package axolootl.client.menu;

import axolootl.Axolootl;
import axolootl.client.menu.widget.ScrollButton;
import axolootl.menu.ControllerMenu;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class ControllerScreen extends AbstractTabScreen<ControllerMenu> implements ScrollButton.IScrollListener {

    // TEXTURES //
    public static final ResourceLocation CONTROLLER_BG = new ResourceLocation(Axolootl.MODID, "textures/gui/aquarium/controller.png");

    // WIDGETS //
    private ScrollButton scrollButton;

    // DATA //

    // COMPONENTS //

    public ControllerScreen(ControllerMenu pMenu, Inventory pPlayerInventory, Component pTitle) {
        super(pMenu, pPlayerInventory, addHoverText(pTitle, Component.literal("(" + pMenu.getBlockPos().toShortString() + ")")));
    }

    @Override
    protected void init() {
        super.init();
        // add scroll button
        this.scrollButton = addRenderableWidget(new ScrollButton(leftPos + 202, topPos + 104, 12, 110, WIDGETS, 244, 0, 12, 15, 15, true, 1.0F, this));
        this.setFocused(this.scrollButton);
    }

    @Override
    protected void renderBgTexture(PoseStack poseStack, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderTexture(0, CONTROLLER_BG);
        blit(poseStack, this.leftPos, this.topPos, 0, 0, WIDTH, HEIGHT);
    }

    @Override
    public void render(PoseStack pPoseStack, int pMouseX, int pMouseY, float pPartialTick) {
        super.render(pPoseStack, pMouseX, pMouseY, pPartialTick);
        // render component tooltip
        if(isHovering(this.titleLabelX, this.titleLabelY, this.font.width(this.getTitle()), font.lineHeight, pMouseX, pMouseY)) {
            renderComponentHoverEffect(pPoseStack, this.getTitle().getStyle(), pMouseX, pMouseY);
        }
    }

    @Override
    protected void renderLabels(PoseStack pPoseStack, int pMouseX, int pMouseY) {
        this.font.draw(pPoseStack, this.getTitle(), this.titleLabelX, this.titleLabelY, 0x404040);
    }

    //// SCROLL LISTENER ////

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if(button == 0 && scrollButton != null && scrollButton.isDragging()) {
            scrollButton.onDrag(mouseX, mouseY, dragX, dragY);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public void onScroll(ScrollButton button, float percent) {
        // TODO
    }
}
