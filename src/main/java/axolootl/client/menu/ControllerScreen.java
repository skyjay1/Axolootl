package axolootl.client.menu;

import axolootl.AxRegistry;
import axolootl.Axolootl;
import axolootl.block.entity.ControllerBlockEntity;
import axolootl.client.menu.widget.ScrollButton;
import axolootl.client.menu.widget.TabButton;
import axolootl.client.menu.widget.TabGroupButton;
import axolootl.menu.ControllerMenu;
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

public class ControllerScreen extends AbstractContainerScreen<ControllerMenu> implements ITabProvider, ScrollButton.IScrollListener {

    // TEXTURES //
    public static final ResourceLocation CONTROLLER = new ResourceLocation(Axolootl.MODID, "textures/gui/aquarium/controller.png");
    public static final ResourceLocation WIDGETS = new ResourceLocation(Axolootl.MODID, "textures/gui/aquarium/widgets.png");

    // CONSTANTS //
    public static final int WIDTH = 220;
    public static final int HEIGHT = 222;

    // WIDGET CONSTANTS //

    // WIDGETS //
    private ScrollButton scrollButton;
    private List<TabButton> tabButtons;
    private List<TabGroupButton> tabGroupButtons;

    // DATA //
    private int tab;
    private int tabGroup;

    // COMPONENTS //

    public ControllerScreen(ControllerMenu pMenu, Inventory pPlayerInventory, Component pTitle) {
        super(pMenu, pPlayerInventory, addHoverText(pTitle, Component.literal("(" + pMenu.getBlockPos().toShortString() + ")")));
        this.tabButtons = new ArrayList<>();
        this.tabGroupButtons = new ArrayList<>();
        this.imageWidth = WIDTH;
        this.imageHeight = HEIGHT;
        this.tab = getMenu().getTab();
        this.tabGroup = 0;
    }

    protected static Component addHoverText(final Component message, Component hoverText) {
        return message.copy().withStyle(a -> a.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hoverText)));
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
        // add scroll button
        this.scrollButton = addRenderableWidget(new ScrollButton(leftPos + 202, topPos + 104, 12, 110, WIDGETS, 244, 0, 12, 15, 15, true, 1.0F, this));
        // add tab buttons
        this.tabButtons.clear();
        this.tabButtons.addAll(initTabs(this, controller));
        // add tab group buttons
        this.tabGroupButtons.clear();
        this.tabGroupButtons.addAll(initTabGroups(this));
        // update tab
        this.setTab(this.getMenu().getTab());
        this.setTabGroup(calculateTabGroup(this.tab));
    }

    @Override
    protected void renderBg(PoseStack pPoseStack, float pPartialTick, int pMouseX, int pMouseY) {
        this.renderBackground(pPoseStack);
        RenderSystem.disableBlend();
        RenderSystem.setShaderTexture(0, CONTROLLER);
        blit(pPoseStack, this.leftPos, this.topPos, 0, 0, WIDTH, HEIGHT);
    }

    @Override
    public void render(PoseStack pPoseStack, int pMouseX, int pMouseY, float pPartialTick) {
        super.render(pPoseStack, pMouseX, pMouseY, pPartialTick);
        renderTabTooltip(pPoseStack, pMouseX, pMouseY, pPartialTick);
        // render component tooltip
        if(isHovering(this.titleLabelX, this.titleLabelY, this.font.width(this.title), font.lineHeight, pMouseX, pMouseY)) {
            renderComponentHoverEffect(pPoseStack, this.title.getStyle(), pMouseX, pMouseY);
        }
        // render slot tooltips
        this.renderTooltip(pPoseStack, pMouseX, pMouseY);
    }

    @Override
    protected void renderLabels(PoseStack pPoseStack, int pMouseX, int pMouseY) {
        this.font.draw(pPoseStack, this.title, this.titleLabelX, this.titleLabelY, 0x404040);
    }

    //// TABS ////

    @Override
    public TabButton addTabButton(int x, int y, int index) {
        TabButton button = addRenderableWidget(new TabButton(leftPos + x, topPos + y, index, getMinecraft().getItemRenderer(), (b, p, mx, my) -> renderTooltip(p, ((TabButton)b).getTooltips(), Optional.empty(), mx, my)));
        button.setSelected(index == this.tab);
        return button;
    }

    @Override
    public TabGroupButton addTabGroupButton(int x, int y, boolean isLeft, Button.OnPress onPress) {
        return addRenderableWidget(new TabGroupButton(leftPos + x, topPos + y, isLeft, onPress, (b, p, mx, my) -> renderTooltip(p, b.getMessage(), mx, Math.max(MIN_TOOLTIP_Y, my))));
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
    public int getTabGroup() {
        return this.tabGroup;
    }

    @Override
    public void setTabGroup(int tabGroup) {
        this.tabGroup = validateTabGroup(tabGroup);
        getMenu().getController().ifPresent(c -> onTabGroupUpdated(c));
    }

    @Override
    public List<TabButton> getTabButtons() {
        return tabButtons;
    }

    @Override
    public List<TabGroupButton> getTabGroupButtons() {
        return tabGroupButtons;
    }

    //// SCROLL LISTENER ////

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if(button == 0 && scrollButton != null && scrollButton.isDragging()) {
            scrollButton.onDrag(mouseX, mouseY, dragX, dragY);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public void onScroll(ScrollButton button, float percent) {
        // TODO
    }
}
