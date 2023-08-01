/**
 * Copyright (c) 2023 Skyler James
 * Permission is granted to use, modify, and redistribute this software, in parts or in whole,
 * under the GNU LGPLv3 license (https://www.gnu.org/licenses/lgpl-3.0.en.html)
 **/

package axolootl.client.menu;

import axolootl.client.menu.widget.CycleButton;
import axolootl.menu.AbstractControllerMenu;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.util.StringUtil;

import java.util.ArrayList;
import java.util.List;

public interface ICycleProvider {

    /**
     * @param imageWidth the image width
     * @param textWidth the width of the title component
     * @return the adjusted x position of the title component
     */
    default int calculateTitleStartX(final int imageWidth, final int textWidth) {
        return (imageWidth - textWidth) / 2;
    }

    /**
     * Call from the Screen {@code init()} method
     * @param screen the screen instance
     * @return the cycle buttons that were added
     */
    default List<CycleButton> initCycleButtons(final AbstractContainerScreen<? extends AbstractControllerMenu> screen) {
        final List<CycleButton> list = new ArrayList<>();
        // add left button
        addCycleButton(7, 4, true, b -> {
            cycle(-1);
            setCycleButtonsEnabled(false);
        });
        addCycleButton(screen.getXSize() - CycleButton.WIDTH - 7, 4, false, b -> {
            cycle(1);
            setCycleButtonsEnabled(false);
        });
        return list;
    }

    /**
     * Renders tooltips for the cycle buttons
     * @param poseStack the pose stack
     * @param mouseX the mouse x position
     * @param mouseY the mouse y position
     * @param partialTick the partial tick
     */
    default void renderCycleTooltip(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        for(CycleButton button : getCycleButtons()) {
            if (button.isHoveredOrFocused()) {
                button.renderToolTip(poseStack, mouseX, mouseY);
            }
        }
    }

    /**
     * @param title the original title
     * @param pos the block position for the hover text
     * @param cycle the current cycle index
     * @param maxCycle the maximum cycle index
     * @return the title with cycle and block pos information
     */
    default Component createCycledTitle(final Component title, final BlockPos pos, final int cycle, final int maxCycle) {
        String sTitle = StringUtil.truncateStringIfNecessary(title.getString(), 23, true);
        Component tooltip = Component.empty().append(title).append("\n").append("(" + pos.toShortString() + ")");
        return Component.empty()
                .append(Component.translatable("gui.axolootl.cycle.tooltip", cycle + 1, maxCycle).withStyle(ChatFormatting.DARK_BLUE))
                .append(" ").append(Component.literal(sTitle).withStyle(ChatFormatting.BLACK))
                .withStyle(a -> a.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, tooltip)));
    }

    /**
     * @param x the relative x position
     * @param y the relative y position
     * @param isLeft true if the button is left facing
     * @param onPress the button OnPress
     * @return the cycle button that was added
     */
    CycleButton addCycleButton(final int x, final int y, final boolean isLeft, final Button.OnPress onPress);

    /**
     * @return the title of the screen including cycle information
     */
    Component getCycledTitle();

    /**
     * @param amount the amount to cycle
     */
    void cycle(final int amount);

    /**
     * @return the current cycle index
     */
    int getCycle();

    /**
     * @return the cycle button list
     */
    List<CycleButton> getCycleButtons();

    /**
     * @param visible whether the cycle buttons are visible
     */
    default void setCycleButtonsVisible(final boolean visible) {
        for(CycleButton button : getCycleButtons()) {
            button.visible = visible;
        }
    }

    /**
     * @param enabled whether the cycle buttons should be enabled
     */
    default void setCycleButtonsEnabled(final boolean enabled) {
        for(CycleButton button : getCycleButtons()) {
            button.active = enabled;
        }
    }
}
