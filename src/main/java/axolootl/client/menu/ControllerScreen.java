package axolootl.client.menu;

import axolootl.Axolootl;
import axolootl.block.entity.ControllerBlockEntity;
import axolootl.client.menu.widget.ScrollButton;
import axolootl.client.menu.widget.TabButton;
import axolootl.menu.ControllerMenu;
import axolootl.menu.TabType;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

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

    // DATA //
    private int tab;

    // COMPONENTS //
    private final Component NOT_ENABLED_TOOLTIP = Component.translatable("gui.axolootl.not_enabled");

    public ControllerScreen(ControllerMenu pMenu, Inventory pPlayerInventory, Component pTitle) {
        super(pMenu, pPlayerInventory, pTitle);
        this.tabButtons = new ArrayList<>();
        this.width = WIDTH;
        this.height = HEIGHT + TabButton.HEIGHT - 8;
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
        // add scroll button
        this.scrollButton = addRenderableWidget(new ScrollButton(leftPos + 202, topPos + 104, 12, 110, WIDGETS, 244, 0, 12, 15, 15, true, 1.0F, this));
        // add tab buttons
        this.tabButtons.clear();
        this.tabButtons.addAll(initTabs(this, controller));
        // update tab
        this.setTab(this.getMenu().getTab());
    }

    @Override
    protected void renderBg(PoseStack pPoseStack, float pPartialTick, int pMouseX, int pMouseY) {
        this.renderBackground(pPoseStack);
        RenderSystem.setShaderTexture(0, CONTROLLER);
        blit(pPoseStack, this.leftPos, this.topPos, 0, 0, WIDTH, HEIGHT);
    }

    @Override
    public void render(PoseStack pPoseStack, int pMouseX, int pMouseY, float pPartialTick) {
        super.render(pPoseStack, pMouseX, pMouseY, pPartialTick);
        renderTabTooltip(this.tabButtons, pPoseStack, pMouseX, pMouseY, pPartialTick);
    }

    @Override
    protected void renderLabels(PoseStack pPoseStack, int pMouseX, int pMouseY) {
        this.font.draw(pPoseStack, this.title, this.titleLabelX, this.titleLabelY, 0x404040);

    }

    //// TABS ////

    @Override
    public TabButton addTabButton(int x, int y, int index, ItemStack icon, List<Component> tooltips, Button.OnPress onPress) {
        TabButton button = addRenderableWidget(new TabButton(leftPos + x, topPos + y, index, getMinecraft().getItemRenderer(), icon, tooltips.get(0), onPress, (b, p, mx, my) -> renderTooltip(p, tooltips, Optional.empty(), mx, my)));
        button.setSelected(index == this.tab);
        return button;
    }

    @Override
    public void setTab(int tab) {
        // validate tab
        if(getMenu().getController().isEmpty() || !TabType.getByIndex(tab).isAvailable(getMenu().getController().get())) {
            return;
        }
        // update tab
        this.tab = tab;
        this.getMenu().setTab(tab);
        // TODO open menu for corresponding tab
    }

    @Override
    public int getTabIndex() {
        return this.tab;
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
