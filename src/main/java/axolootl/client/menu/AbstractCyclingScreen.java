/**
 * Copyright (c) 2023 Skyler James
 * Permission is granted to use, modify, and redistribute this software, in parts or in whole,
 * under the GNU LGPLv3 license (https://www.gnu.org/licenses/lgpl-3.0.en.html)
 **/

package axolootl.client.menu;

import axolootl.client.menu.widget.CycleButton;
import axolootl.menu.AbstractControllerMenu;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractCyclingScreen<T extends AbstractControllerMenu> extends AbstractTabScreen<T> implements ICycleProvider {

    // WIDGETS //
    protected List<CycleButton> cycleButtons;

    // COMPONENTS //
    protected Component cycledTitle;
    protected int cycledTitleWidth;

    public AbstractCyclingScreen(T pMenu, Inventory pPlayerInventory, Component pTitle) {
        super(pMenu, pPlayerInventory, pTitle);
        this.cycleButtons = new ArrayList<>();
        this.cycledTitle = createCycledTitle(pTitle, getMenu().getBlockPos(), getMenu().getCycle(), getMenu().getCycleCount());
    }

    @Override
    protected void init() {
        super.init();
        // adjust screen pos
        this.topPos = calculateTopPos(this);
        // adjust title pos
        this.cycledTitleWidth = this.font.width(this.cycledTitle);
        this.titleLabelX = calculateTitleStartX(this.imageWidth, this.cycledTitleWidth);
        // add cycle buttons
        this.cycleButtons.clear();
        this.cycleButtons.addAll(initCycleButtons(this));
        this.setCycleButtonsVisible(getMenu().getCycleCount() > 1);
    }

    @Override
    public void render(PoseStack pPoseStack, int pMouseX, int pMouseY, float pPartialTick) {
        super.render(pPoseStack, pMouseX, pMouseY, pPartialTick);
        renderCycleTooltip(pPoseStack, pMouseX, pMouseY, pPartialTick);
        // render component tooltip
        if(isHovering(this.titleLabelX, this.titleLabelY, this.cycledTitleWidth, font.lineHeight, pMouseX, pMouseY)) {
            renderComponentHoverEffect(pPoseStack, cycledTitle.getStyle(), pMouseX, pMouseY);
        }
    }

    @Override
    public Component getTitle() {
        return cycledTitle;
    }

    //// CYCLES ////

    @Override
    public CycleButton addCycleButton(int x, int y, boolean isLeft, Button.OnPress onPress) {
        return addRenderableWidget(new CycleButton(this.leftPos + x, this.topPos + y, isLeft, onPress));
    }

    @Override
    public Component getCycledTitle() {
        return cycledTitle;
    }

    @Override
    public void cycle(int amount) {
        if(amount == 0 || getMenu().getCycleCount() <= 1) {
            return;
        }
        this.getMenu().cycle(amount);
        this.cycledTitle = createCycledTitle(getTitle(), getMenu().getBlockPos(), getMenu().getCycle(), getMenu().getCycleCount());
        this.cycledTitleWidth = this.font.width(this.cycledTitle);
    }

    @Override
    public int getCycle() {
        return this.getMenu().getCycle();
    }

    @Override
    public List<CycleButton> getCycleButtons() {
        return this.cycleButtons;
    }
}
