/**
 * Copyright (c) 2023 Skyler James
 * Permission is granted to use, modify, and redistribute this software, in parts or in whole,
 * under the GNU LGPLv3 license (https://www.gnu.org/licenses/lgpl-3.0.en.html)
 **/

package axolootl.client.menu;

import axolootl.Axolootl;
import axolootl.client.menu.widget.ScrollButton;
import axolootl.data.axolootl_variant.AxolootlVariant;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.ResourceLocation;

public class AxolootlInspectorDetailsScreen extends Screen implements ScrollButton.IScrollListener {

    // TEXTURES //
    public static final ResourceLocation TEXTURE = new ResourceLocation(Axolootl.MODID, "textures/gui/aquarium/axolootl_details.png");
    public static final ResourceLocation WIDGETS = AbstractTabScreen.WIDGETS;

    // WIDGET CONSTANTS //
    public static final int WIDTH = 240;
    public static final int HEIGHT = 176;

    // WIDGETS //
    private ScrollButton scrollButton;
    private int scrollOffset;

    // DATA //
    private int leftPos;
    private int topPos;
    private final ResourceLocation id;
    private final AxolootlVariant variant;

    // COMPONENTS //
    public static final String PREFIX = "gui.controller_tab.axolootl.axolootl_inspector_details.entry.";

    public AxolootlInspectorDetailsScreen(ResourceLocation id, AxolootlVariant variant) {
        super(variant.getDescription());
        this.id = id;
        this.variant = variant;
    }

    @Override
    protected void init() {
        super.init();
        this.leftPos = (width - WIDTH) / 2;
        this.topPos = (height - HEIGHT) / 2;
        this.scrollButton = addRenderableWidget(new ScrollButton(leftPos + 198, topPos + 19, 12, 82, WIDGETS, 244, 0, 12, 15, 15, true, 1.0F /* TODO */, this));
        this.setFocused(this.scrollButton);
        this.scrollButton.active = false;
        // TODO add details
    }



    @Override
    public void render(PoseStack pPoseStack, int pMouseX, int pMouseY, float pPartialTick) {
        renderBackground(pPoseStack);
        RenderSystem.disableBlend();
        RenderSystem.setShaderTexture(0, TEXTURE);
        blit(pPoseStack, this.leftPos, this.topPos, 0, 0, WIDTH, HEIGHT);
        super.render(pPoseStack, pMouseX, pMouseY, pPartialTick);
    }

    @Override
    public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
        if(pMouseX < this.leftPos || pMouseX > this.leftPos + WIDTH || pMouseY < this.topPos || pMouseY > this.topPos + HEIGHT) {
            onClose();
            return true;
        }
        return super.mouseClicked(pMouseX, pMouseY, pButton);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    //// SCROLL LISTENER ////

    @Override
    public boolean mouseScrolled(double pMouseX, double pMouseY, double pDelta) {
       /* if(isHovering(ENTRY_X, ENTRY_Y, EntryButton.WIDTH * ENTRY_COUNT_X, EntryButton.HEIGHT * ENTRY_COUNT_Y, pMouseX, pMouseY)) {
            return scrollButton.mouseScrolled(pMouseX, pMouseY, pDelta);
        }*/
        return super.mouseScrolled(pMouseX, pMouseY, pDelta);
    }

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
        //this.scrollOffset = Mth.floor(Math.max(0, percent * Math.max(0, variantCountList.size() - ENTRY_COUNT)));
    }
}
