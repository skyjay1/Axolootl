package axolootl.client.menu.widget;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

public class ComponentButton extends Button {

    protected final Font font;
    protected Component hoverMessage;
    protected boolean drawTooltip;

    public ComponentButton(int pX, int pY, int height, Font font, Component pMessage, OnPress onPress, OnTooltip onTooltip) {
        this(pX, pY, font.width(pMessage), height, font, pMessage, onPress, onTooltip);
    }

    public ComponentButton(int pX, int pY, int width, int height, Font font, Component pMessage, OnPress onPress, OnTooltip onTooltip) {
        super(pX, pY, width, height, pMessage, onPress, onTooltip);
        this.hoverMessage = Component.empty();
        this.font = font;
        this.drawTooltip = true;
        setMessage(getMessage());
    }

    @Override
    public void setMessage(final Component message) {
        super.setMessage(message);
        this.hoverMessage = (message.getStyle().getClickEvent() != null) ? message.copy().withStyle(message.getStyle()).withStyle(ChatFormatting.UNDERLINE) : message;
    }

    public static int getHeight(Font font) {
        return font.lineHeight + 2;
    }

    @Override
    public void renderButton(PoseStack pPoseStack, int pMouseX, int pMouseY, float pPartialTick) {
        this.font.draw(pPoseStack, isHoveredOrFocused() ? hoverMessage : getMessage(), this.x, this.y + 1, 0);
        if(drawTooltip && this.isHoveredOrFocused()) {
            this.renderToolTip(pPoseStack, pMouseX, pMouseY);
        }
    }
}
