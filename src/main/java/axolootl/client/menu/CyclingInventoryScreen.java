package axolootl.client.menu;

import axolootl.Axolootl;
import axolootl.block.entity.ControllerBlockEntity;
import axolootl.client.menu.widget.CycleButton;
import axolootl.client.menu.widget.TabButton;
import axolootl.menu.CyclingInventoryMenu;
import axolootl.menu.TabType;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CyclingInventoryScreen extends AbstractContainerScreen<CyclingInventoryMenu> implements ITabProvider, ICycleProvider {

    // TEXTURES //
    public static final ResourceLocation OUTPUT = new ResourceLocation(Axolootl.MODID, "textures/gui/aquarium/output.png");
    public static final ResourceLocation LARGE_OUTPUT = new ResourceLocation(Axolootl.MODID, "textures/gui/aquarium/large_output.png");
    public static final ResourceLocation WIDGETS = ControllerScreen.WIDGETS;

    // CONSTANTS //
    public static final int WIDTH = 220;
    public static final int HEIGHT = 222;

    // WIDGET CONSTANTS //

    // WIDGETS //
    private ResourceLocation texture;
    private List<TabButton> tabButtons;
    private List<CycleButton> cycleButtons;

    // DATA //
    private int tab;

    // COMPONENTS //
    private Component cycledTitle;
    private int cycledTitleWidth;

    public CyclingInventoryScreen(CyclingInventoryMenu pMenu, Inventory pPlayerInventory, Component pTitle) {
        super(pMenu, pPlayerInventory, pTitle);
        this.tabButtons = new ArrayList<>();
        this.cycleButtons = new ArrayList<>();
        this.imageWidth = WIDTH;
        this.imageHeight = HEIGHT;
        this.inventoryLabelY = this.imageHeight - 94;
        this.inventoryLabelX = CyclingInventoryMenu.PLAYER_INV_X;
        this.tab = getMenu().getTab();
        this.texture = getMenu().isLargeOutput() ? LARGE_OUTPUT : OUTPUT;
        this.cycledTitle = createCycledTitle(pTitle, getMenu().getBlockPos(), getMenu().getCycle(), getMenu().getMaxCycle());
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
        // adjust title pos
        this.cycledTitleWidth = this.font.width(this.cycledTitle);
        this.titleLabelX = calculateTitleStartX(this.cycledTitle, this.imageWidth, this.cycledTitleWidth);
        // add tab buttons
        this.tabButtons.clear();
        this.tabButtons.addAll(initTabs(this, controller));
        // add cycle buttons
        this.cycleButtons.clear();
        this.cycleButtons.addAll(initCycleButtons(this, getMenu().getSortedCycleList()));
        // update tab
        this.setTab(this.getMenu().getTab());
    }

    @Override
    protected void renderBg(PoseStack pPoseStack, float pPartialTick, int pMouseX, int pMouseY) {
        this.renderBackground(pPoseStack);
        RenderSystem.setShaderTexture(0, texture);
        blit(pPoseStack, this.leftPos, this.topPos, 0, 0, WIDTH, HEIGHT);
    }

    @Override
    public void render(PoseStack pPoseStack, int pMouseX, int pMouseY, float pPartialTick) {
        super.render(pPoseStack, pMouseX, pMouseY, pPartialTick);
        renderTabTooltip(pPoseStack, pMouseX, pMouseY, pPartialTick);
        renderCycleTooltip(pPoseStack, pMouseX, pMouseY, pPartialTick);
        // render component tooltip
        if(isHovering(this.titleLabelX, this.titleLabelY, this.cycledTitleWidth, font.lineHeight, pMouseX, pMouseY)) {
            renderComponentHoverEffect(pPoseStack, cycledTitle.getStyle(), pMouseX, pMouseY);
        }
    }

    @Override
    protected void renderLabels(PoseStack pPoseStack, int pMouseX, int pMouseY) {
        this.font.draw(pPoseStack, this.cycledTitle, this.titleLabelX, this.titleLabelY, 0x404040);
        this.font.draw(pPoseStack, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0x404040);
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
        if(tab == this.tab || getMenu().getController().isEmpty() || !TabType.getByIndex(tab).isAvailable(getMenu().getController().get())) {
            return;
        }
        // update tab
        this.tab = tab;
        this.getMenu().setTab(tab);
    }

    @Override
    public int getTabIndex() {
        return this.tab;
    }

    @Override
    public List<TabButton> getTabButtons() {
        return tabButtons;
    }

    //// CYCLES ////

    @Override
    public CycleButton addCycleButton(int x, int y, boolean isLeft, Button.OnPress onPress) {
        return addRenderableWidget(new CycleButton(this.leftPos + x, this.topPos + y, isLeft, onPress, (b, p, mx, my) -> renderTooltip(p, b.getMessage(), mx, my)));
    }

    @Override
    public Component getCycledTitle() {
        return cycledTitle;
    }

    @Override
    public void cycle(int amount) {
        this.getMenu().cycle(amount);
        this.cycledTitle = createCycledTitle(getTitle(), getMenu().getBlockPos(), getMenu().getCycle(), getMenu().getMaxCycle());
        this.cycledTitleWidth = this.font.width(this.cycledTitle);
    }

    @Override
    public int getCycle() {
        return this.getMenu().getCycle();
    }

    @Override
    public List<CycleButton> getCycleButtons() {
        return this.cycleButtons;
    }
}
