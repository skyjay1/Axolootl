/**
 * Copyright (c) 2023 Skyler James
 * Permission is granted to use, modify, and redistribute this software, in parts or in whole,
 * under the GNU LGPLv3 license (https://www.gnu.org/licenses/lgpl-3.0.en.html)
 **/

package axolootl.client.menu;

import axolootl.Axolootl;
import axolootl.block.entity.ControllerBlockEntity;
import axolootl.client.menu.widget.AxolootlEntryButton;
import axolootl.client.menu.widget.ScrollButton;
import axolootl.data.axolootl_variant.AxolootlVariant;
import axolootl.menu.AxolootlMenu;
import axolootl.util.TankMultiblock;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MobBucketItem;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class AxolootlInterfaceScreen extends AbstractTabScreen<AxolootlMenu> implements ScrollButton.IScrollListener {

    // TEXTURES //
    public static final ResourceLocation AXOLOOTL_BG = new ResourceLocation(Axolootl.MODID, "textures/gui/aquarium/axolootl_interface.png");

    // WIDGET CONSTANTS //
    private static final int ENTRY_X = 8;
    private static final int ENTRY_Y = 20;
    private static final int ENTRY_COUNT_X = 2;
    private static final int ENTRY_COUNT_Y = 4;
    private static final int ENTRY_COUNT = ENTRY_COUNT_X * ENTRY_COUNT_Y;

    // WIDGETS //
    private List<AxolootlEntryButton> entryButtons;
    private Button insertButton;
    private ScrollButton scrollButton;
    private int scrollOffset;

    // DATA //
    private List<Map.Entry<AxolootlVariant, Integer>> variantCountList;
    private int entryCount;
    private boolean hasBucket;
    private boolean hasAxolootl;

    // COMPONENTS //
    public static final String PREFIX = "gui.controller_tab.axolootl.axolootl_interface.";
    private Component countText;

    public AxolootlInterfaceScreen(AxolootlMenu pMenu, Inventory pPlayerInventory, Component pTitle) {
        super(pMenu, pPlayerInventory, pTitle);
        this.variantCountList = new ArrayList<>();
        this.entryButtons = new ArrayList<>();
        this.countText = pTitle;
    }

    private void updateVariantList() {
        Optional<ControllerBlockEntity> oController = getMenu().getController();
        // build sorted list
        this.variantCountList.clear();
        this.variantCountList.addAll(getMenu().getVariantCountMap().entrySet());
        this.variantCountList.sort(Comparator.comparingInt(e -> e.getKey().getTier()));
        this.entryCount = getMenu().getTotalCount();
        // update components
        oController.ifPresent(c -> {
            final int maxCapacity = ControllerBlockEntity.calculateMaxCapacity(c.getSize().orElse(TankMultiblock.Size.EMPTY));
            ChatFormatting color = (entryCount <= maxCapacity) ? ChatFormatting.BLACK : ChatFormatting.RED;
            this.countText = Component.translatable(PREFIX + "count", Component.literal("" + entryCount).withStyle(color), maxCapacity);
        });
    }

    @Override
    protected void init() {
        super.init();
        updateVariantList();
        // add scroll button
        this.scrollButton = addRenderableWidget(new ScrollButton(leftPos + 198, topPos + 19, 12, 82, WIDGETS, 244, 0, 12, 15, 15, true, 1.0F / Math.max(1, (variantCountList.size() - ENTRY_COUNT) / ENTRY_COUNT_X), this));
        this.setFocused(this.scrollButton);
        this.scrollButton.active = getMenu().getVariantCountMap().size() > ENTRY_COUNT;
        // add insert button
        this.insertButton = addRenderableWidget(new ImageButton(leftPos + 143, topPos + 107, 69, 18, 160, 50, 18, WIDGETS, 256, 256, b -> getMenu().insert(), (b, p, mx, my) -> renderTooltip(p, b.getMessage(), mx, my), Component.translatable(PREFIX + "insert")));
        // add entry buttons
        this.entryButtons.clear();
        for(int i = 0, x, y; i < ENTRY_COUNT; i++) {
            x = leftPos + ENTRY_X + (i % ENTRY_COUNT_X) * AxolootlEntryButton.WIDTH;
            y = topPos + ENTRY_Y + (i / ENTRY_COUNT_X) * AxolootlEntryButton.HEIGHT;
            final Button.OnPress onPress = b -> {
                getMenu().extract(((AxolootlEntryButton) b).getEntry());
                updateVariantList();
                updateEntryButtons();
            };
            this.entryButtons.add(addRenderableWidget(new AxolootlEntryButton(x, y, font, onPress, (b, p, mx, my) -> renderTooltip(p, b.getMessage(), mx, my))));
        }
        updateEntryButtons();
        containerTick();
    }

    @Override
    public Component getTitle() {
        return countText;
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        boolean hadBucket = hasBucket;
        // check container contents to update flags
        updateContainerFlags(getMenu().getContainer());
        // update entry buttons
        if(hasBucket != hadBucket) {
            updateEntryButtons();
        }
        // update insert button
        insertButton.active = hasAxolootl;
    }

    @Override
    protected void renderBgTexture(PoseStack poseStack, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderTexture(0, AXOLOOTL_BG);
        blit(poseStack, this.leftPos, this.topPos, 0, 0, WIDTH, HEIGHT);
    }

    @Override
    public void render(PoseStack pPoseStack, int pMouseX, int pMouseY, float pPartialTick) {
        super.render(pPoseStack, pMouseX, pMouseY, pPartialTick);
    }

    private void updateContainerFlags(final Container container) {
        this.hasBucket = false;
        this.hasAxolootl = false;
        for(int i = 0, n = container.getContainerSize(); i < n; i++) {
            ItemStack itemStack = container.getItem(i);
            if(itemStack.isEmpty()) continue;
            // check for bucket
            if(itemStack.is(Items.BUCKET)) {
                this.hasBucket = true;
            }
            // check for mob bucket
            if(itemStack.getItem() instanceof MobBucketItem) {
                this.hasAxolootl = true;
            }
        }
    }

    private void updateEntryButtons() {
        for(int i = 0, n = entryButtons.size(); i < n; i++) {
            AxolootlEntryButton button = entryButtons.get(i);
            int index = i + scrollOffset * ENTRY_COUNT_X;
            if(index < 0 || index >= variantCountList.size()) {
                button.visible = button.active = false;
                continue;
            }
            button.visible = true;
            button.update(variantCountList.get(index), hasBucket);
        }
    }

    //// SCROLL LISTENER ////

    @Override
    public boolean mouseScrolled(double pMouseX, double pMouseY, double pDelta) {
        if(isHovering(ENTRY_X, ENTRY_Y, AxolootlEntryButton.WIDTH * ENTRY_COUNT_X, AxolootlEntryButton.HEIGHT * ENTRY_COUNT_Y, pMouseX, pMouseY)) {
            return scrollButton.mouseScrolled(pMouseX, pMouseY, pDelta);
        }
        return super.mouseScrolled(pMouseX, pMouseY, pDelta);
    }

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
        this.scrollOffset = Mth.floor(Math.max(0, percent * Math.max(0, variantCountList.size() - ENTRY_COUNT)));
        updateEntryButtons();
    }
}
