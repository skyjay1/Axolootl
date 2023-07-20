package axolootl.client.menu;

import axolootl.block.entity.ControllerBlockEntity;
import axolootl.client.menu.widget.TabButton;
import axolootl.menu.TabType;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public interface ITabProvider {

    default List<TabButton> initTabs(final Screen screen, final ControllerBlockEntity controller) {
        final List<TabButton> list = new ArrayList<>();
        // add tab buttons
        for(int i = 0, y = -TabButton.HEIGHT + 4, n = TabType.values().length; i < n; i++) {
            TabType type = TabType.getByIndex(i);
            Button.OnPress onPress;
            List<Component> messages = new ArrayList<>();
            if (type.isAvailable(controller)) {
                messages.add(type.getTitle());
                onPress = b -> setTab(type);
            } else {
                messages.add(type.getUnavailableTitle());
                messages.add(Component.translatable("gui.axolootl.not_enabled"));
                onPress = b -> {};
            }
            list.add(addTabButton(i * TabButton.WIDTH,  y, i, type.getIcon(), messages, onPress));
        }
        return list;
    }

    /**
     * Renders tooltips for the given tabs
     * @param tabs the tab buttons
     * @param poseStack the pose stack
     * @param mouseX the mouse x position
     * @param mouseY the mouse y position
     * @param partialTick the partial tick
     */
    default void renderTabTooltip(List<TabButton> tabs, PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        for(TabButton tabButton : tabs) {
            if (tabButton.isHoveredOrFocused()) {
                tabButton.renderToolTip(poseStack, mouseX, mouseY);
            }
        }
    }

    /**
     *
     * @param x the relative x position
     * @param y the relative y position
     * @param index the tab index
     * @param icon the item stack icon
     * @param tooltips the tooltip components
     * @param onPress the button OnPress
     * @return the tab button that was added
     */
    TabButton addTabButton(final int x, final int y, final int index, final ItemStack icon, final List<Component> tooltips, final Button.OnPress onPress);

    /**
     * @param type the tab type to apply
     */
    default void setTab(final TabType type) {
        setTab(type.ordinal());
    }

    /**
     * @param tab the tab index to apply
     */
    void setTab(final int tab);

    /**
     * @return the current tab type
     */
    default TabType getTab() {
        return TabType.getByIndex(getTabIndex());
    }

    /**
     * @return the current tab index
     */
    int getTabIndex();
}
