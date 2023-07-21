package axolootl.client.menu;

import axolootl.client.menu.widget.CycleButton;
import axolootl.client.menu.widget.TabButton;
import axolootl.menu.AbstractControllerMenu;
import axolootl.menu.TabType;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;

import java.util.ArrayList;
import java.util.List;

public interface ICycleProvider {

    default int calculateTitleStartX(final Component component, final int imageWidth, final int textWidth) {
        return (imageWidth - textWidth) / 2;
    }

    default List<CycleButton> initCycleButtons(final AbstractContainerScreen<? extends AbstractControllerMenu> screen, final List<BlockPos> positions) {
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

    default Component createCycledTitle(final Component title, final BlockPos pos, final int cycle, final int maxCycle) {
        return Component.empty()
                .append(Component.translatable("gui.axolootl.cycle.tooltip", cycle + 1, maxCycle).withStyle(ChatFormatting.BLUE))
                .append(" ").append(title.copy().withStyle(ChatFormatting.DARK_GRAY))
                .withStyle(a -> a.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("(" + pos.toShortString() + ")"))));
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
     * @param enabled whether the cycle buttons should be enabled
     */
    default void setCycleButtonsEnabled(final boolean enabled) {
        for(CycleButton button : getCycleButtons()) {
            button.active = enabled;
        }
    }
}
