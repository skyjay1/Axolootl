/**
 * Copyright (c) 2023 Skyler James
 * Permission is granted to use, modify, and redistribute this software, in parts or in whole,
 * under the GNU LGPLv3 license (https://www.gnu.org/licenses/lgpl-3.0.en.html)
 **/

package axolootl.client.menu.widget;

import axolootl.client.menu.ControllerScreen;
import axolootl.data.aquarium_tab.IAquariumTab;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class TabButton extends Button {

    public static final ResourceLocation TEXTURE = ControllerScreen.WIDGETS;
    public static final int WIDTH = 44;
    public static final int HEIGHT = 21;
    public static final int DELTA_SELECTED = 3;

    private final ItemRenderer itemRenderer;
    private final List<Component> tooltips;
    private ItemStack icon;
    private int index;
    private boolean selected;
    private OnPress mutableOnPress;

    public TabButton(int x, int y, int index, final ItemRenderer itemRenderer, OnTooltip onTooltip) {
        super(x, y, WIDTH, HEIGHT, Component.empty(), b -> {}, onTooltip);
        this.index = index;
        this.itemRenderer = itemRenderer;
        this.tooltips = new ArrayList<>();
        this.icon = ItemStack.EMPTY;
        this.mutableOnPress = b -> {};
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(final int index, final ItemStack icon, final List<Component> messages, final OnPress onPress) {
        this.index = index;
        this.icon = icon;
        this.tooltips.clear();
        this.tooltips.addAll(messages);
        this.mutableOnPress = onPress;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public List<Component> getTooltips() {
        return this.tooltips;
    }

    @Override
    public void onPress() {
        this.mutableOnPress.onPress(this);
    }

    @Override
    public void renderButton(PoseStack pPoseStack, int pMouseX, int pMouseY, float pPartialTick) {
        // prepare to render image
        int u = index * WIDTH;
        int v = isSelected() ? HEIGHT : 0;
        int y = this.y + (isSelected() ? DELTA_SELECTED : 0);
        // render image
        RenderSystem.setShaderTexture(0, TEXTURE);
        blit(pPoseStack, this.x, y, u, v, WIDTH, HEIGHT);
        // render icon
        this.itemRenderer.renderGuiItem(this.icon, this.x + (this.width - 16) / 2, this.y + DELTA_SELECTED + (this.height - 16) / 2);
    }
}
