/**
 * Copyright (c) 2023 Skyler James
 * Permission is granted to use, modify, and redistribute this software, in parts or in whole,
 * under the GNU LGPLv3 license (https://www.gnu.org/licenses/lgpl-3.0.en.html)
 **/

package axolootl.client.menu;

import axolootl.AxRegistry;
import axolootl.Axolootl;
import axolootl.block.entity.ControllerBlockEntity;
import axolootl.client.menu.widget.ScrollButton;
import axolootl.data.axolootl_variant.AxolootlVariant;
import axolootl.menu.AxolootlInterfaceMenu;
import axolootl.util.TankMultiblock;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.StringUtil;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MobBucketItem;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class AxolootlInterfaceScreen extends AbstractTabScreen<AxolootlInterfaceMenu> implements ScrollButton.IScrollListener {

    // TEXTURES //
    public static final ResourceLocation AXOLOOTL_BG = new ResourceLocation(Axolootl.MODID, "textures/gui/aquarium/axolootl_interface.png");

    // WIDGET CONSTANTS //
    private static final int ENTRY_X = 8;
    private static final int ENTRY_Y = 20;
    private static final int ENTRY_COUNT_X = 2;
    private static final int ENTRY_COUNT_Y = 4;
    private static final int ENTRY_COUNT = ENTRY_COUNT_X * ENTRY_COUNT_Y;

    // WIDGETS //
    private List<EntryButton> entryButtons;
    private InsertButton insertButton;
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
    private Component emptyText;

    public AxolootlInterfaceScreen(AxolootlInterfaceMenu pMenu, Inventory pPlayerInventory, Component pTitle) {
        super(pMenu, pPlayerInventory, pTitle);
        this.variantCountList = new ArrayList<>();
        this.entryButtons = new ArrayList<>();
        this.countText = pTitle;
        this.emptyText = Component.translatable(PREFIX + "no_axolootls");
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
            ChatFormatting color = (entryCount <= maxCapacity) ? ChatFormatting.RESET : ChatFormatting.RED;
            this.countText = Component.translatable(PREFIX + "count", Component.literal("" + entryCount).withStyle(color), maxCapacity);
        });
    }

    @Override
    protected void init() {
        super.init();
        updateVariantList();
        // update title position
        this.titleLabelX = (this.imageWidth - font.width(this.getTitle())) / 2;
        // add scroll button
        this.scrollButton = addRenderableWidget(new ScrollButton(leftPos + 199, topPos + 20, 12, 80, WIDGETS, 244, 0, 12, 15, 15, true, 1.0F / Math.max(1, (variantCountList.size() - ENTRY_COUNT) / ENTRY_COUNT_X), this));
        this.setFocused(this.scrollButton);
        this.scrollButton.active = getMenu().getVariantCountMap().size() > ENTRY_COUNT;
        // add insert button
        final Button.OnPress insertButtonOnPress = b -> {
          getMenu().insert();
          updateVariantList();
          updateEntryButtons();
        };
        this.insertButton = addRenderableWidget(new InsertButton(leftPos + WIDTH - InsertButton.WIDTH - 7, topPos + AxolootlInterfaceMenu.INV_Y - 1, font, insertButtonOnPress, (b, p, mx, my) -> renderTooltip(p, b.getMessage(), mx, my)));
        // add entry buttons
        this.entryButtons.clear();
        for(int i = 0, x, y; i < ENTRY_COUNT; i++) {
            x = leftPos + ENTRY_X + (i % ENTRY_COUNT_X) * EntryButton.WIDTH;
            y = topPos + ENTRY_Y + (i / ENTRY_COUNT_X) * EntryButton.HEIGHT;
            final Button.OnPress onPress = b -> {
                getMenu().extract(((EntryButton) b).getEntry());
                updateVariantList();
                updateEntryButtons();
            };
            this.entryButtons.add(addRenderableWidget(new EntryButton(x, y, font, onPress, (b, p, mx, my) -> renderTooltip(p, b.getMessage(), mx, my))));
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
        RenderSystem.setShaderTexture(0, BACKGROUND);
        blit(poseStack, this.leftPos, this.topPos, 0, 0, WIDTH, HEIGHT);
        RenderSystem.setShaderTexture(0, AXOLOOTL_BG);
        blit(poseStack, this.leftPos, this.topPos, 0, 0, WIDTH, HEIGHT);
        RenderSystem.setShaderTexture(0, SLOTS);
        blit(poseStack, this.leftPos + AxolootlInterfaceMenu.INV_X - 1, this.topPos + AxolootlInterfaceMenu.INV_Y - 1, AxolootlInterfaceMenu.INV_X - 1, AxolootlInterfaceMenu.INV_Y - 1, AxolootlInterfaceMenu.INV_SIZE * 18, 18);
        // bucket icon
        blit(poseStack, this.leftPos + AxolootlInterfaceMenu.INV_X, this.topPos + AxolootlInterfaceMenu.INV_Y - 1, 240, 0, 16, 16);
    }

    @Override
    public void render(PoseStack pPoseStack, int pMouseX, int pMouseY, float pPartialTick) {
        super.render(pPoseStack, pMouseX, pMouseY, pPartialTick);
        // render empty research text
        if(entryCount < 1) {
            int textWidth = font.width(emptyText);
            font.draw(pPoseStack, emptyText, this.leftPos + ENTRY_X + (ENTRY_COUNT_X * EntryButton.WIDTH - textWidth) / 2.0F, this.topPos + ENTRY_Y + (ENTRY_COUNT_Y * EntryButton.HEIGHT - font.lineHeight) / 2.0F, 0);
        }
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
            EntryButton button = entryButtons.get(i);
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
        if(isHovering(ENTRY_X, ENTRY_Y, EntryButton.WIDTH * ENTRY_COUNT_X, EntryButton.HEIGHT * ENTRY_COUNT_Y, pMouseX, pMouseY)) {
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

    //// WIDGETS ////

    private static class InsertButton extends ImageButton {
        private static final int WIDTH = 92;
        private static final int HEIGHT = 18;

        private final Font font;

        public InsertButton(int pX, int pY, Font font, OnPress pOnPress, OnTooltip pOnTooltip) {
            super(pX, pY, WIDTH, HEIGHT, 137, 50, HEIGHT, WIDGETS, 256, 256, pOnPress, pOnTooltip, Component.translatable(PREFIX + "insert"));
            this.font = font;
        }

        @Override
        public void renderButton(PoseStack pPoseStack, int pMouseX, int pMouseY, float pPartialTick) {
            super.renderButton(pPoseStack, pMouseX, pMouseY, pPartialTick);
            font.draw(pPoseStack, getMessage(), this.x + (this.width - font.width(getMessage())) / 2.0F, this.y + (this.height - font.lineHeight) / 2.0F, 0);
        }
    }

    public static class EntryButton extends ImageButton {

        public static final int WIDTH = 92;
        public static final int HEIGHT = 20;

        public static final int INTERACT_WIDTH = 14;
        public static final int INTERACT_HEIGHT = 14;

        private Font font;
        private AxolootlVariant entry;
        private int count;
        private Component text;
        private Component tooltipText;
        private Component tooltipFailText;

        public EntryButton(int pX, int pY, Font font, OnPress pOnPress, OnTooltip pOnTooltip) {
            super(pX, pY, INTERACT_WIDTH, INTERACT_HEIGHT, 242, 143, INTERACT_HEIGHT, WIDGETS, 256, 256,
                    pOnPress, pOnTooltip, Component.empty());
            this.entry = AxolootlVariant.EMPTY;
            this.font = font;
            this.text = Component.empty();
            this.tooltipText = Component.translatable(PREFIX + "extract");
            this.tooltipFailText = Component.translatable(PREFIX + "extract.fail").withStyle(ChatFormatting.RED);
        }

        public AxolootlVariant getEntry() {
            return entry;
        }

        public void update(final Map.Entry<AxolootlVariant, Integer> entry, final boolean isActive) {
            this.entry = entry.getKey();
            this.count = entry.getValue();
            Component axolootlName = Component.translatable("entity.axolootl.axolootl.description", AxRegistry.EntityReg.AXOLOOTL.get().getDescription(), entry.getKey().getDescription());
            this.tooltipText = Component.translatable(PREFIX + "extract_x", axolootlName);
            String sText = Component.translatable(PREFIX + "entry", count, entry.getKey().getDescription()).getString();
            this.text = Component.literal(StringUtil.truncateStringIfNecessary(sText, 15, true));
            updateActive(isActive);
        }

        public void updateActive(final boolean isActive) {
            this.active = isActive && this.count > 0 && this.entry != AxolootlVariant.EMPTY;
            this.setMessage(this.active ? this.tooltipText : this.tooltipFailText);
        }

        @Override
        public void renderButton(PoseStack pPoseStack, int pMouseX, int pMouseY, float pPartialTick) {
            this.font.draw(pPoseStack, text, this.x + INTERACT_WIDTH + 4, this.y + (this.height - font.lineHeight) / 2.0F, 0);
            super.renderButton(pPoseStack, pMouseX, pMouseY, pPartialTick);
        }
    }
}
