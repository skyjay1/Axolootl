/**
 * Copyright (c) 2023 Skyler James
 * Permission is granted to use, modify, and redistribute this software, in parts or in whole,
 * under the GNU LGPLv3 license (https://www.gnu.org/licenses/lgpl-3.0.en.html)
 **/

package axolootl.client.menu;

import axolootl.AxRegistry;
import axolootl.Axolootl;
import axolootl.block.entity.ControllerBlockEntity;
import axolootl.client.menu.widget.TabButton;
import axolootl.client.menu.widget.TabGroupButton;
import axolootl.data.aquarium_tab.IAquariumTab;
import axolootl.menu.AbstractControllerMenu;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public interface ITabProvider {

    public static final int MIN_TOOLTIP_Y = 18;

    /**
     * @param screen the screen instance
     * @return the gui top position, adjusted to allow space for the tab buttons
     */
    default int calculateTopPos(final AbstractContainerScreen<?> screen) {
        final int tabHeight = TabButton.HEIGHT - TabButton.DELTA_SELECTED;
        return Math.max(screen.getGuiTop(), tabHeight);
    }

    /**
     * Call from the Screen {@code init()} method
     * @param screen the screen instance
     * @return the list of tab buttons that were added
     */
    default List<TabButton> initTabs(final AbstractContainerScreen<? extends AbstractControllerMenu> screen) {
        final List<TabButton> list = new ArrayList<>();
        // add tab buttons
        for(int i = 0, y = -TabButton.HEIGHT, n = getTabsPerGroup(); i < n; i++) {
            final int index = i;
            list.add(addTabButton(i * TabButton.WIDTH, y, i));
        }
        return list;
    }

    /**
     * Call from the Screen {@code init()} method
     * @param screen the screen instance
     * @return the list of tab group buttons that were added
     */
    default List<TabGroupButton> initTabGroups(final AbstractContainerScreen<? extends AbstractControllerMenu> screen) {
        final List<TabGroupButton> list = new ArrayList<>();
        // add previous button
        int y = -(TabButton.HEIGHT - TabButton.DELTA_SELECTED) + (TabButton.HEIGHT - TabGroupButton.HEIGHT) / 2;
        list.add(addTabGroupButton(0, y, true, b -> {
            setTabGroup(getTabGroup() - 1);
        }));
        // add next button
        list.add(addTabGroupButton(screen.getXSize() - TabGroupButton.WIDTH, y, false, b -> {
            setTabGroup(getTabGroup() + 1);
        }));
        return list;
    }

    /**
     * Renders tooltips for the given tabs
     * @param poseStack the pose stack
     * @param mouseX the mouse x position
     * @param mouseY the mouse y position
     * @param partialTick the partial tick
     */
    default void renderTabTooltip(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        // TODO tab tooltip
        /*for(TabButton tabButton : getTabButtons()) {
            if (tabButton.isHoveredOrFocused()) {
                tabButton.renderToolTip(poseStack, mouseX, Math.max(mouseY, MIN_TOOLTIP_Y));
            }
        }*/
    }

    /**
     * @param x the relative x position
     * @param y the relative y position
     * @param index the tab index
     * @return the tab button that was added
     */
    TabButton addTabButton(final int x, final int y, final int index);

    /**
     * @param x the relative x position
     * @param y the relative y position
     * @param isLeft true if the button is facing left
     * @param onPress the button OnPress
     * @return the tab button that was added
     */
    TabGroupButton addTabGroupButton(final int x, final int y, final boolean isLeft, final Button.OnPress onPress);

    /**
     * @param tab the tab index to apply
     */
    void setTab(final int tab);

    /**
     * @return the current tab index
     */
    int getTab();

    /**
     * @param tabGroup the tab group index to apply
     */
    void setTabGroup(final int tabGroup);

    /**
     * @return the current tab group index
     */
    int getTabGroup();

    /**
     * @return the tab button list
     */
    List<TabButton> getTabButtons();

    /**
     * @return the tab group button list
     */
    List<TabGroupButton> getTabGroupButtons();

    /**
     * @return the maximum number of tab buttons per tab group. Recommended 3 or greater.
     */
    default int getTabsPerGroup() {
        return 5;
    }

    /**
     * @param tab the tab index
     * @return the tab group index for the given tab
     */
    default int calculateTabGroup(final int tab) {
        int maxPerGroup = getTabsPerGroup();
        int groupCount = calculateGroupCount();
        // these tabs are always in the first group
        if(tab < maxPerGroup - 1) {
            return 0;
        }
        // these tabs could be at the end of one group or the start of another
        // depending on how many groups there are
        if(tab % Math.max(1, maxPerGroup - 2) == 1) {
            int groupIndex = (tab - 1) / Math.max(1, maxPerGroup - 2);
            return groupCount > groupIndex ? groupIndex : groupIndex - 1;
        }
        // these tabs are always in the middle of a group
        return ((tab + 1) / Math.max(1, maxPerGroup - 2)) - 1;
    }

    /**
     * @return the total number of groups
     */
    default int calculateGroupCount() {
        int tabCount = AxRegistry.AquariumTabsReg.getTabCount();
        int maxPerGroup = getTabsPerGroup();
        if(tabCount <= maxPerGroup) {
            return 1;
        }
        return tabCount / Math.max(1, maxPerGroup - 2);
    }

    default int validateTab(int tab) {
        return Mth.clamp(tab, 0, AxRegistry.AquariumTabsReg.getTabCount() - 1);
    }

    /**
     * @param tabGroup the tab group index
     * @return the tab group clamped between 0 and the maximum number of groups
     */
    default int validateTabGroup(int tabGroup) {
        return Mth.clamp(tabGroup, 0, calculateGroupCount() - 1);
    }

    default void onTabGroupUpdated(ControllerBlockEntity controller) {
        // collect values
        int group = getTabGroup();
        int maxGroup = calculateGroupCount() - 1;
        final List<IAquariumTab> sortedTabs = AxRegistry.AquariumTabsReg.getSortedTabs();
        // collect buttons
        final List<TabButton> tabButtons = getTabButtons();
        final List<TabGroupButton> tabGroupButtons = getTabGroupButtons();
        // iterate tab buttons
        for(int i = 0, n = tabButtons.size(), index = group * (getTabsPerGroup() - 1); i < n; i++) {
            TabButton button = getTabButtons().get(i);
            // show or hide first tab button
            if(i == 0) {
                boolean active = !(group > 0);
                button.active = button.visible = active;
                if(!active) continue;
            }
            // show or hide last tab button
            if(i == n - 1) {
                boolean active = !(group <= maxGroup);
                button.active = button.visible = active;
                if(!active) continue;
            }
            // hide unused tab buttons
            if(index >= sortedTabs.size()) {
                button.active = button.visible = false;
                continue;
            }
            // show all other tab buttons
            button.active = button.visible = true;
            // determine components and onPress
            IAquariumTab tab = sortedTabs.get(index);
            final List<Component> messages = new ArrayList<>();
            Button.OnPress onPress;
            if (tab.isAvailable(controller)) {
                messages.add(tab.getTitle(true));
                final int onPressIndex = index;
                onPress = b -> {
                    setTab(onPressIndex);
                    this.setTabsEnabled(false);
                };
            } else {
                messages.add(tab.getTitle(false));
                messages.add(Component.translatable("gui.controller_tab.not_enabled"));
                onPress = b -> {};
            }
            // update tab index and icon
            button.setIndex(i, tab.getIcon(), messages, onPress);
            button.setSelected(i == getTab());
            button.setSelected(index == getTab());
            index++;
        }
        if(tabGroupButtons.size() != 2) {
            Axolootl.LOGGER.warn("[ITabProvider#onTabGroupUpdated] Invalid tab group button size; expected 2 but got " + tabGroupButtons.size());
            return;
        }
        TabGroupButton b = tabGroupButtons.get(0);
        b.visible = b.active = group > 0;
        b = tabGroupButtons.get(1);
        b.visible = b.active = group < maxGroup;
    }

    /**
     * @param enabled whether the tab buttons are enabled
     */
    default void setTabsEnabled(final boolean enabled) {
        for(TabButton tabButton : getTabButtons()) {
            tabButton.active = enabled;
        }
    }
}
