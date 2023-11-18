/**
 * Copyright (c) 2023 Skyler James
 * Permission is granted to use, modify, and redistribute this software, in parts or in whole,
 * under the GNU LGPLv3 license (https://www.gnu.org/licenses/lgpl-3.0.en.html)
 **/

package axolootl.client.menu;

import axolootl.AxRegistry;
import axolootl.Axolootl;
import axolootl.client.menu.widget.ScrollButton;
import axolootl.data.axolootl_variant.AxolootlVariant;
import axolootl.entity.AxolootlEntity;
import axolootl.menu.AxolootlInspectorMenu;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.StringUtil;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class AxolootlInspectorScreen extends AbstractCyclingScreen<AxolootlInspectorMenu> implements ScrollButton.IScrollListener {

    // TEXTURES //
    public static final ResourceLocation INSPECTOR_BG = new ResourceLocation(Axolootl.MODID, "textures/gui/aquarium/axolootl_inspector.png");

    // WIDGET CONSTANTS //
    private static final int PROGRESS_X = 101;
    private static final int PROGRESS_Y = 17;
    private static final int PROGRESS_WIDTH = 20;
    private static final int PROGRESS_HEIGHT = 20;

    private static final int ENTRY_X = 8;
    private static final int ENTRY_Y = 42;
    private static final int ENTRY_COUNT_X = 2;
    private static final int ENTRY_COUNT_Y = 4;
    private static final int ENTRY_COUNT = ENTRY_COUNT_X * ENTRY_COUNT_Y;

    // DATA //
    private final List<Map.Entry<ResourceKey<AxolootlVariant>, AxolootlVariant>> variants;
    private int scaledProgress;

    // WIDGETS //
    private final List<EntryButton> entryButtons;
    private ScrollButton scrollButton;
    private int scrollOffset;

    // COMPONENTS //
    public static final String PREFIX = "gui.controller_tab.axolootl.axolootl_inspector.";
    private Component progressText;
    private Component emptyResearchText;

    public AxolootlInspectorScreen(AxolootlInspectorMenu pMenu, Inventory pPlayerInventory, Component pTitle) {
        super(pMenu, pPlayerInventory, pTitle);
        this.progressText = Component.empty();
        this.emptyResearchText = Component.translatable(PREFIX + "no_research");
        this.variants = new ArrayList<>();
        this.entryButtons = new ArrayList<>();
    }

    private void calculateSortedVariants() {
        this.variants.clear();
        final RegistryAccess access = getMenu().getInventory().player.level.registryAccess();
        // determine the variants to show
        Set<ResourceLocation> tracked = new HashSet<>();
        getMenu().getInventory().player.getCapability(Axolootl.AXOLOOTL_RESEARCH_CAPABILITY).ifPresent(c -> tracked.addAll(c.getAxolootls()));
        // resolve variants with cross reference to the tracked variants
        final Registry<AxolootlVariant> registry = AxolootlVariant.getRegistry(access);
        for(Map.Entry<ResourceKey<AxolootlVariant>, AxolootlVariant> entry : registry.entrySet()) {
            if(tracked.contains(entry.getKey().location()) && entry.getValue().isEnabled(access)) {
                this.variants.add(entry);
            }
        }
        // sort variants by tier
        final Comparator<Map.Entry<ResourceKey<AxolootlVariant>, AxolootlVariant>> comparator = Comparator.comparing(a -> a.getValue().getDescription().getString());
        this.variants.sort(comparator);
    }

    @Override
    protected void init() {
        super.init();
        calculateSortedVariants();
        // add scroll button
        this.scrollButton = addRenderableWidget(new ScrollButton(leftPos + 199, topPos + ENTRY_Y + 1, 12, ENTRY_COUNT_Y * EntryButton.HEIGHT, WIDGETS, 244, 0, 12, 15, 15, true, 1.0F / Math.max(1.0F, (float) (variants.size() - ENTRY_COUNT) / ENTRY_COUNT_X), this));
        this.setFocused(this.scrollButton);
        this.scrollButton.active = variants.size() > ENTRY_COUNT;
        // add entry buttons
        this.entryButtons.clear();
        for(int i = 0, x, y; i < ENTRY_COUNT; i++) {
            x = this.leftPos + ENTRY_X + (i % ENTRY_COUNT_X) * EntryButton.WIDTH + 1;
            y = this.topPos + ENTRY_Y + (i / ENTRY_COUNT_X) * EntryButton.HEIGHT + 1;
            Button.OnPress onPress = b -> {
                ((EntryButton)b).openDetails(getMinecraft());
            };
            this.entryButtons.add(this.addRenderableWidget(new EntryButton(x, y, this.font, this.itemRenderer, onPress)));
        }
        updateEntryButtons();
        containerTick();
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        // update progress
        if(getMenu().hasProgress()) {
            scaledProgress = Mth.ceil(getMenu().getProgress() * PROGRESS_WIDTH);
            final int progressPercent = Mth.ceil(getMenu().getProgress() * 100.0F);
            progressText = Component.translatable(PREFIX + "progress", progressPercent);
        } else if(scaledProgress > 0) {
            scaledProgress = 0;
            progressText = Component.empty();
        }
        // update variant list
        if(getMenu().isChanged()) {
            getMenu().setChanged(false);
            init(getMinecraft(), width, height);
        }
    }

    @Override
    protected void renderBgTexture(PoseStack poseStack, float partialTick, int mouseX, int mouseY) {
        super.renderBgTexture(poseStack, partialTick, mouseX, mouseY);
        // render bg
        RenderSystem.setShaderTexture(0, INSPECTOR_BG);
        blit(poseStack, this.leftPos, this.topPos, 0, 0, WIDTH, HEIGHT);
        // render progress
        renderProgress(poseStack, mouseX, mouseY, partialTick);
    }

    @Override
    public void render(PoseStack pPoseStack, int pMouseX, int pMouseY, float pPartialTick) {
        super.render(pPoseStack, pMouseX, pMouseY, pPartialTick);
        // render empty research text
        if(variants.isEmpty()) {
            int textWidth = font.width(emptyResearchText);
            font.draw(pPoseStack, emptyResearchText, this.leftPos + ENTRY_X + (ENTRY_COUNT_X * EntryButton.WIDTH - textWidth) / 2.0F, this.topPos + ENTRY_Y + (ENTRY_COUNT_Y * EntryButton.HEIGHT - font.lineHeight) / 2.0F, 0);
        }
        // render hover actions
        renderHoverActions(pPoseStack, pMouseX, pMouseY, pPartialTick);
    }

    private void renderProgress(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        // render progress
        RenderSystem.setShaderTexture(0, WIDGETS);
        blit(poseStack, this.leftPos + PROGRESS_X, this.topPos + PROGRESS_Y, 74, 50, PROGRESS_WIDTH, PROGRESS_HEIGHT);
        if(scaledProgress > 0) {
            blit(poseStack, this.leftPos + PROGRESS_X, this.topPos + PROGRESS_Y, 74, 50 + PROGRESS_HEIGHT, scaledProgress, PROGRESS_HEIGHT);
        }
    }

    private void renderHoverActions(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        // render progress tooltip
        if(getMenu().hasProgress() && isHovering(PROGRESS_X, PROGRESS_Y, PROGRESS_WIDTH, PROGRESS_HEIGHT, mouseX, mouseY)) {
            renderTooltip(poseStack, progressText, mouseX, mouseY);
        }
    }

    private void updateEntryButtons() {
        final RegistryAccess access = getMenu().getInventory().player.level.registryAccess();
        for(int i = 0, n = entryButtons.size(); i < n; i++) {
            AxolootlInspectorScreen.EntryButton button = entryButtons.get(i);
            int index = i + scrollOffset * ENTRY_COUNT_X;
            if(index < 0 || index >= variants.size()) {
                button.visible = false;
                continue;
            }
            button.visible = true;
            button.update(variants.get(index), access);
        }
    }

    //// SCROLL LISTENER ////

    @Override
    public boolean mouseScrolled(double pMouseX, double pMouseY, double pDelta) {
        if(isHovering(ENTRY_X, ENTRY_Y, AxolootlInterfaceScreen.EntryButton.WIDTH * ENTRY_COUNT_X, AxolootlInterfaceScreen.EntryButton.HEIGHT * ENTRY_COUNT_Y, pMouseX, pMouseY)) {
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
        this.scrollOffset = Math.round(Math.max(0.0F, percent * Math.max(0.0F, Mth.ceil((float) (variants.size() - ENTRY_COUNT) / (float) ENTRY_COUNT_X))));
        updateEntryButtons();
    }

    //// WIDGETS ////

    public static class EntryButton extends ImageButton {

        public static final int WIDTH = 92;
        public static final int HEIGHT = 20;

        private Font font;
        private ItemRenderer itemRenderer;
        private ResourceLocation key;
        private AxolootlVariant entry;
        private ItemStack icon;
        private List<Component> tooltips;

        public EntryButton(int pX, int pY, Font font, ItemRenderer itemRenderer, OnPress pOnPress) {
            super(pX, pY, WIDTH, HEIGHT, 137, 104, HEIGHT, WIDGETS, 256, 256,
                    pOnPress, Component.empty());
            this.tooltips =new ArrayList<>();
            this.key = new ResourceLocation("empty");
            this.entry = AxolootlVariant.EMPTY;
            this.font = font;
            this.itemRenderer = itemRenderer;
            this.icon = new ItemStack(AxRegistry.ItemReg.AXOLOOTL_BUCKET.get());
        }

        public ResourceLocation getKey() {
            return key;
        }

        public AxolootlVariant getEntry() {
            return entry;
        }

        public List<Component> getTooltips() {
            return tooltips;
        }

        public void update(final Map.Entry<ResourceKey<AxolootlVariant>, AxolootlVariant> entry, final RegistryAccess access) {
            this.key = entry.getKey().location();
            this.entry = entry.getValue();
            String axolootlName = StringUtil.truncateStringIfNecessary(entry.getValue().getDescription().getString(), 12, true);
            this.setMessage(Component.literal(axolootlName));
            ResourceLocation id = entry.getValue().getRegistryName(access);
            this.icon.getOrCreateTag().putString(AxolootlEntity.KEY_VARIANT_ID, id.toString());
            this.tooltips.clear();
            this.tooltips.add(entry.getValue().getDescription());
            this.tooltips.add(Component.translatable("entity.axolootl.axolootl.tier", entry.getValue().getTierDescription()).withStyle(ChatFormatting.GRAY));
            this.setTooltip(Tooltip.create(AbstractTabScreen.concat(this.tooltips)));
        }

        @Override
        public void renderButton(PoseStack pPoseStack, int pMouseX, int pMouseY, float pPartialTick) {
            super.renderButton(pPoseStack, pMouseX, pMouseY, pPartialTick);
            this.itemRenderer.renderGuiItem(this.icon, this.getX() + 1, this.getY() + (this.height - 16) / 2);
            this.font.draw(pPoseStack, getMessage(), this.getX() + 16 + 3, this.getY() + (this.height - font.lineHeight) / 2.0F, 0);
        }

        public void openDetails(final Minecraft minecraft) {
            // validate entry
            if(AxolootlVariant.EMPTY.equals(entry)) {
                return;
            }
            // validate level
            if(null == minecraft.level || null == minecraft.player) {
                return;
            }
            // validate can open
            if(!AxolootlDetailsScreen.canOpenDetails(minecraft.player, key)) {
                return;
            }
            // open details screen
            AxolootlDetailsScreen.openDetails(minecraft, minecraft.level.registryAccess(), key, entry, icon);
        }
    }
}
