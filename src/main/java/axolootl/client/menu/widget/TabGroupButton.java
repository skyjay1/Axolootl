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

    public TabGroupButton(int pX, int pY, boolean isLeft, OnPress pOnPress, OnTooltip onTooltip) {
        super(pX, pY, WIDTH, HEIGHT, 232, isLeft ? 79 : 79 + HEIGHT * 2, HEIGHT, TEXTURE, 256, 256,
                pOnPress, onTooltip, Component.translatable("gui.axolootl.tab_group." + (isLeft ? "previous" : "next")));
        this.isLeft = isLeft;
    }
}
