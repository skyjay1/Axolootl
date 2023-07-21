package axolootl.client.menu.widget;

import axolootl.client.menu.ControllerScreen;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public class TabButton extends Button {

    public static final ResourceLocation TEXTURE = ControllerScreen.WIDGETS;
    public static final int WIDTH = 44;
    public static final int HEIGHT = 21;

    private final int index;
    private final ItemRenderer itemRenderer;
    private final ItemStack icon;
    private boolean selected;

    public TabButton(int x, int y, int index, final ItemRenderer itemRenderer, final ItemStack icon, final Component title, OnPress onPress, OnTooltip onTooltip) {
        super(x, y, WIDTH, HEIGHT, title, onPress, onTooltip);
        this.index = index;
        this.itemRenderer = itemRenderer;
        this.icon = icon;
    }

    public int getIndex() {
        return index;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    @Override
    public void renderButton(PoseStack pPoseStack, int pMouseX, int pMouseY, float pPartialTick) {
        // prepare to render image
        int u = index * WIDTH;
        int v = isSelected() ? HEIGHT : 0;
        // render image
        RenderSystem.setShaderTexture(0, TEXTURE);
        blit(pPoseStack, this.x, this.y, u, v, WIDTH, HEIGHT);
        // render icon
        this.itemRenderer.renderGuiItem(this.icon, this.x + (this.width - 16) / 2, this.y + this.height - 16 - 2);
    }
}
