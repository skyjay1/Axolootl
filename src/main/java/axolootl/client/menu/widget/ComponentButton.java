/**
 * Copyright (c) 2023 Skyler James
 * Permission is granted to use, modify, and redistribute this software, in parts or in whole,
 * under the GNU LGPLv3 license (https://www.gnu.org/licenses/lgpl-3.0.en.html)
 **/

package axolootl.client.menu.widget;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.Style;
import org.jetbrains.annotations.Nullable;

public class ComponentButton extends Button {

    protected final Font font;
    protected Component hoverMessage;

    public ComponentButton(int pX, int pY, int height, Font font, Component pMessage, OnPress onPress) {
        this(pX, pY, font.width(pMessage), height, font, pMessage, onPress);
    }

    public ComponentButton(int pX, int pY, int width, int height, Font font, Component pMessage, OnPress onPress) {
        super(pX, pY, width, height, pMessage, onPress, Button.DEFAULT_NARRATION);
        this.hoverMessage = Component.empty();
        this.font = font;
        setMessage(getMessage());
    }

    @Override
    public void setMessage(final Component message) {
        super.setMessage(message);
        this.hoverMessage = (message.getStyle().getClickEvent() != null)
                ? ComponentUtils.mergeStyles(message.copy(), Style.EMPTY.withUnderlined(true))
                : message;
        this.setTooltip(Tooltip.create(this.hoverMessage));
    }

    public static int getHeight(Font font) {
        return font.lineHeight + 2;
    }

    @Override
    public void setTooltip(@Nullable Tooltip pTooltip) {
        super.setTooltip(pTooltip);
    }

    @Override
    public void renderButton(PoseStack pPoseStack, int pMouseX, int pMouseY, float pPartialTick) {
        this.font.draw(pPoseStack, isHoveredOrFocused() ? hoverMessage : getMessage(), this.getX(), this.getY() + 1, 0);
    }
}
