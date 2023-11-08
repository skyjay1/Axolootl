/**
 * Copyright (c) 2023 Skyler James
 * Permission is granted to use, modify, and redistribute this software, in parts or in whole,
 * under the GNU LGPLv3 license (https://www.gnu.org/licenses/lgpl-3.0.en.html)
 **/

package axolootl.client.menu.widget;

import axolootl.client.menu.ControllerScreen;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class TabGroupButton extends ImageButton {

    public static final ResourceLocation TEXTURE = ControllerScreen.WIDGETS;
    public static final int WIDTH = 24;
    public static final int HEIGHT = 16;

    private final boolean isLeft;

    public TabGroupButton(int pX, int pY, boolean isLeft, OnPress pOnPress) {
        super(pX, pY, WIDTH, HEIGHT, 232, isLeft ? 79 : 79 + HEIGHT * 2, HEIGHT, TEXTURE, 256, 256,
                pOnPress, Component.translatable("gui.axolootl.tab_group." + (isLeft ? "previous" : "next")));
        this.isLeft = isLeft;
    }
}
