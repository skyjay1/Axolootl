package axolootl.client.menu.widget;

import axolootl.client.menu.AbstractTabScreen;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.function.Function;

public class ItemButton extends Button {

    public static final int WIDTH = 16;
    public static final int HEIGHT = 16;

    protected final Font font;
    protected final ItemRenderer itemRenderer;
    protected final Function<ItemStack, List<Component>> getTooltipFromItem;
    protected final ResourceLocation texture;
    protected final int textureU;
    protected final int textureV;
    protected final int textureWidth;
    protected final int textureHeight;
    protected boolean drawBackground;
    protected boolean drawTooltip;
    protected ItemStack item;

    public ItemButton(int pX, int pY, boolean drawBackground, Font font, ItemRenderer itemRenderer, ItemStack item, Function<ItemStack, List<Component>> getTooltipFromItem, OnPress onPress, OnTooltip onTooltip) {
        this(pX, pY, drawBackground, font, itemRenderer, item, getTooltipFromItem, onPress, onTooltip, AbstractTabScreen.SLOTS, 30, 18, 256, 256);
    }

    public ItemButton(int pX, int pY, boolean drawBackground, Font font, ItemRenderer itemRenderer, ItemStack item,
                      Function<ItemStack, List<Component>> getTooltipFromItem, OnPress onPress, OnTooltip onTooltip,
                      ResourceLocation texture, int u, int v, int textureWidth, int textureHeight) {
        super(pX, pY, WIDTH, HEIGHT, item.getHoverName(), onPress, onTooltip);
        this.drawBackground = drawBackground;
        this.font = font;
        this.itemRenderer = itemRenderer;
        this.item = item;
        this.getTooltipFromItem = getTooltipFromItem;
        this.texture = texture;
        this.textureU = u;
        this.textureV = v;
        this.textureWidth = textureWidth;
        this.textureHeight = textureHeight;
        this.drawTooltip = true;
    }

    public List<Component> getTooltips() {
        return getTooltipFromItem.apply(this.item);
    }

    public void setItem(ItemStack item) {
        this.item = item;
    }

    public void setDrawBackground(boolean drawBackground) {
        this.drawBackground = drawBackground;
    }

    @Override
    public void renderButton(PoseStack pPoseStack, int pMouseX, int pMouseY, float pPartialTick) {
        // draw slot
        if (drawBackground) {
            renderBackground(pPoseStack, this.x, this.y);
        }
        // render item
        if(!this.item.isEmpty()) {
            renderItem(this.item, this.x, this.y);
        }
        // render tooltip
        if (drawTooltip && this.isHoveredOrFocused()) {
            this.renderToolTip(pPoseStack, pMouseX, pMouseY);
        }
    }

    protected void renderBackground(final PoseStack poseStack, final int x, final int y) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, this.texture);
        RenderSystem.enableDepthTest();
        blit(poseStack, x, y, this.textureU, this.textureV, WIDTH, HEIGHT, this.textureWidth, this.textureHeight);
    }

    protected void renderItem(final ItemStack itemStack, final int x, final int y) {
        // render item
        PoseStack modelViewStack = RenderSystem.getModelViewStack();
        modelViewStack.pushPose();
        modelViewStack.translate(0, 0, 2_000);
        RenderSystem.applyModelViewMatrix();
        this.itemRenderer.renderAndDecorateItem(itemStack, x, y);
        this.itemRenderer.renderGuiItemDecorations(font, itemStack, x, y);
        modelViewStack.popPose();
        RenderSystem.applyModelViewMatrix();
    }

}
