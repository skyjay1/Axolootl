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
import axolootl.menu.AbstractControllerMenu;
import axolootl.menu.CyclingMenu;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public abstract class AbstractTabScreen<T extends AbstractControllerMenu> extends AbstractContainerScreen<T> implements ITabProvider {

    // TEXTURES //
    public static final ResourceLocation BACKGROUND = new ResourceLocation(Axolootl.MODID, "textures/gui/aquarium/background.png");
    public static final ResourceLocation SLOTS = new ResourceLocation(Axolootl.MODID, "textures/gui/aquarium/slots.png");
    public static final ResourceLocation WIDGETS = new ResourceLocation(Axolootl.MODID, "textures/gui/aquarium/widgets.png");

    // CONSTANTS //
    public static final int WIDTH = 220;
    public static final int HEIGHT = 222;

    // WIDGET CONSTANTS //

    // WIDGETS //
    protected List<TabButton> tabButtons;
    protected List<TabGroupButton> tabGroupButtons;

    // DATA //
    protected int tab;
    protected int tabGroup;

    public AbstractTabScreen(T pMenu, Inventory pPlayerInventory, Component pTitle) {
        super(pMenu, pPlayerInventory, pTitle);
        this.tabButtons = new ArrayList<>();
        this.tabGroupButtons = new ArrayList<>();
        this.imageWidth = WIDTH;
        this.imageHeight = HEIGHT;
        this.inventoryLabelY = this.imageHeight - 94;
        this.inventoryLabelX = CyclingMenu.PLAYER_INV_X;
        this.tab = getMenu().getTab();
    }

    @Override
    protected void init() {
        super.init();
        if(getMenu().getController().isEmpty()) {
            onClose();
            return;
        }
        final ControllerBlockEntity controller = getMenu().getController().get();
        // adjust screen pos
        this.topPos = calculateTopPos(this);
        // add tab buttons
        this.tabButtons.clear();
        this.tabButtons.addAll(initTabs(this));
        // add tab group buttons
        this.tabGroupButtons.clear();
        this.tabGroupButtons.addAll(initTabGroups(this));
        // update tab
        this.setTab(this.getMenu().getTab());
        this.setTabGroup(calculateTabGroup(this.tab));
    }

    protected void renderBgTexture(PoseStack poseStack, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderTexture(0, BACKGROUND);
        blit(poseStack, this.leftPos, this.topPos, 0, 0, WIDTH, HEIGHT);
    }

    protected void renderPlayerSlots(PoseStack poseStack, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderTexture(0, SLOTS);
        blit(poseStack, this.leftPos + CyclingMenu.PLAYER_INV_X - 1, this.topPos + CyclingMenu.PLAYER_INV_Y - 1, 29, 139, 9 * 18, 4 * 18 + 4);
    }

    @Override
    protected void renderBg(PoseStack pPoseStack, float pPartialTick, int pMouseX, int pMouseY) {
        this.renderBackground(pPoseStack);
        RenderSystem.disableBlend();
        renderBgTexture(pPoseStack, pPartialTick, pMouseX, pMouseY);
        if(getMenu().hasPlayerSlots()) {
            renderPlayerSlots(pPoseStack, pPartialTick, pMouseX, pMouseY);
        }
    }

    @Override
    public void render(PoseStack pPoseStack, int pMouseX, int pMouseY, float pPartialTick) {
        super.render(pPoseStack, pMouseX, pMouseY, pPartialTick);
        renderTabTooltip(pPoseStack, pMouseX, pMouseY, pPartialTick);
        // render slot tooltips
        this.renderTooltip(pPoseStack, pMouseX, pMouseY);
    }

    @Override
    protected void renderLabels(PoseStack pPoseStack, int pMouseX, int pMouseY) {
        this.font.draw(pPoseStack, this.getTitle(), this.titleLabelX, this.titleLabelY, 0x404040);
        if(getMenu().hasPlayerSlots()) {
            this.font.draw(pPoseStack, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0x404040);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    /**
     * @param poseStack the pose stack
     * @param component the text to render
     * @param x the text x position
     * @param y the text y position
     * @param width the maximum text width
     * @return the height of the wrapped text
     */
    protected int renderWrappedText(PoseStack poseStack, Component component, int x, int y, int width, int color) {
        font.drawWordWrap(component, x, y, width, color);
        return font.wordWrapHeight(component, width);
    }

    //// TABS ////

    @Override
    public TabButton addTabButton(int x, int y, int index) {
        TabButton button = addRenderableWidget(new TabButton(leftPos + x, topPos + y, index, getMinecraft().getItemRenderer()));
        button.setSelected(index == this.tab);
        return button;
    }

    @Override
    public TabGroupButton addTabGroupButton(int x, int y, boolean isLeft, Button.OnPress onPress) {
        // TODO make sure tooltips are not off the top of the screen
        return addRenderableWidget(new TabGroupButton(leftPos + x, topPos + y, isLeft, onPress));
    }

    @Override
    public void setTab(int tab) {
        // validate tab
        tab = validateTab(tab);
        if(tab == this.tab || getMenu().getController().isEmpty() || !AxRegistry.AquariumTabsReg.getSortedTabs().get(tab).isAvailable(getMenu().getController().get())) {
            return;
        }
        // update tab
        this.tab = tab;
        this.getMenu().setTab(tab);
    }

    @Override
    public int getTab() {
        return this.tab;
    }

    @Override
    public void setTabGroup(int tabGroup) {
        this.tabGroup = validateTabGroup(tabGroup);
        getMenu().getController().ifPresent(c -> onTabGroupUpdated(c));
    }

    @Override
    public int getTabGroup() {
        return this.tabGroup;
    }

    @Override
    public List<TabButton> getTabButtons() {
        return tabButtons;
    }

    @Override
    public List<TabGroupButton> getTabGroupButtons() {
        return tabGroupButtons;
    }

    //// HELPERS ////

    protected static Component addHoverText(final Component message, Component hoverText) {
        return message.copy().withStyle(a -> a.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hoverText)));
    }
}
