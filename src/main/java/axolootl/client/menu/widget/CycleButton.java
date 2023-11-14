/**
 * Copyright (c) 2023 Skyler James
 * Permission is granted to use, modify, and redistribute this software, in parts or in whole,
 * under the GNU LGPLv3 license (https://www.gnu.org/licenses/lgpl-3.0.en.html)
 **/

package axolootl.client.menu.widget;

import axolootl.client.menu.ControllerScreen;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class CycleButton extends ImageButton {

    public static final ResourceLocation TEXTURE = ControllerScreen.WIDGETS;
    public static final int WIDTH = 27;
    public static final int HEIGHT = 12;

    private final boolean isLeft;

    public CycleButton(int pX, int pY, boolean isLeft, OnPress pOnPress) {
        super(pX, pY, WIDTH, HEIGHT, 229, isLeft ? 30 : 30 + HEIGHT * 2, HEIGHT, TEXTURE, 256, 256,
                pOnPress, Component.translatable("gui.axolootl.cycle." + (isLeft ? "previous" : "next")));
        this.isLeft = isLeft;
        this.setTooltip(Tooltip.create(getMessage()));
    }
}
