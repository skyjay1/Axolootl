/**
 * Copyright (c) 2023 Skyler James
 * Permission is granted to use, modify, and redistribute this software, in parts or in whole,
 * under the GNU LGPLv3 license (https://www.gnu.org/licenses/lgpl-3.0.en.html)
 **/

package axolootl.client.menu;

import axolootl.Axolootl;
import axolootl.block.entity.ControllerBlockEntity;
import axolootl.client.menu.widget.ScrollButton;
import axolootl.data.aquarium_modifier.AquariumModifier;
import axolootl.data.aquarium_modifier.ModifierSettings;
import axolootl.menu.ControllerMenu;
import axolootl.util.BreedStatus;
import axolootl.util.FeedStatus;
import axolootl.util.TankMultiblock;
import axolootl.util.TankStatus;
import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.StringUtil;
import net.minecraft.world.entity.player.Inventory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ControllerScreen extends AbstractTabScreen<ControllerMenu> implements ScrollButton.IScrollListener {

    // TEXTURES //
    public static final ResourceLocation CONTROLLER_BG = new ResourceLocation(Axolootl.MODID, "textures/gui/aquarium/controller.png");

    // WIDGET CONSTANTS //
    private static final int TEXT_X = 9;
    private static final int TEXT_Y = 17;
    private static final int TEXT_LINE_SPACING = 4;

    private static final int ENTRY_X = 7;
    private static final int ENTRY_Y = 105;
    private static final int ENTRY_COUNT_X = 1;
    private static final int ENTRY_COUNT_Y = 6;
    private static final int ENTRY_COUNT = ENTRY_COUNT_X * ENTRY_COUNT_Y;

    // WIDGETS //
    private final List<EntryButton> entryButtons;
    private ScrollButton scrollButton;
    private ActivateButton activateButton;
    private int scrollOffset;

    // DATA //
    private static final int PROCESSING_TIMER = 80;
    private List<ModifierData> modifierDataList;
    private boolean hasTank;
    private int processingTimer;

    // COMPONENTS //
    public static final String PREFIX = "gui.controller_tab.axolootl.controller.";
    private Component tankSizeText;
    private Component tankStatusText;
    private Component feedStatusText;
    private Component breedStatusText;
    private Component axolootlCapacityText;
    private Component modifierCountText;

    public ControllerScreen(ControllerMenu pMenu, Inventory pPlayerInventory, Component pTitle) {
        super(pMenu, pPlayerInventory, addHoverText(pTitle, Component.literal("(" + pMenu.getBlockPos().toShortString() + ")")));
        this.modifierDataList = new ArrayList<>();
        this.entryButtons = new ArrayList<>();
        this.tankSizeText = Component.empty();
        this.tankStatusText = Component.empty();
        this.feedStatusText = Component.empty();
        this.breedStatusText = Component.empty();
        this.axolootlCapacityText = Component.empty();
        this.modifierCountText = Component.empty();
        this.hasTank = pMenu.getController().isPresent() && pMenu.getController().get().hasTank();
    }

    public static Component toAdditivePercentage(final double value, final ChatFormatting color) {
        return Component.literal(toAdditivePercentage(value)).withStyle(color);
    }

    public static String toAdditivePercentage(final double value) {
        String sign = (value < 0) ? "-" : "+";
        String sValue = String.format("%.8f", Math.abs(value) * 100.0D).replaceAll("0*$", "").replaceAll("\\.$", "");
        return sign + sValue + "%";
    }

    private void updateModifierCountMap() {
        if(getMenu().getController().isEmpty() || !hasTank) {
            return;
        }
        // prepare to create data
        final RegistryAccess access = getMenu().getInventory().player.level.registryAccess();
        final ControllerBlockEntity controller = getMenu().getController().get();
        final Map<BlockPos, AquariumModifier> modifierMap = controller.resolveModifiers(getMenu().getInventory().player.level.registryAccess());
        final Map<ResourceLocation, ModifierData> dataMap = new HashMap<>();
        // build data
        for(Map.Entry<BlockPos, AquariumModifier> entry : modifierMap.entrySet()) {
            ResourceLocation id = entry.getValue().getRegistryName(access);
            boolean active = controller.activePredicate.test(entry.getKey(), entry.getValue());
            dataMap.computeIfAbsent(id, r -> new ModifierData(r, entry.getValue())).addCount(active);
        }
        // populate and sort list
        this.modifierDataList.clear();
        this.modifierDataList.addAll(dataMap.values());
        this.modifierDataList.sort(Comparator.comparing(o -> o.getModifier().getDescription().getString()));
    }

    @Override
    protected void init() {
        super.init();
        updateModifierCountMap();
        // update title position
        this.titleLabelX = (this.imageWidth - font.width(this.getTitle())) / 2;
        // add scroll button
        this.scrollButton = addRenderableWidget(new ScrollButton(leftPos + 202, topPos + 104, 12, 110, WIDGETS, 244, 0, 12, 15, 15, true, 1.0F / Math.max(1, modifierDataList.size() / ENTRY_COUNT_X), this));
        this.setFocused(this.scrollButton);
        this.scrollButton.active = modifierDataList.size() > ENTRY_COUNT;
        // add activate button
        Button.OnPress activateOnPress = b -> {
          ((ActivateButton)b).setActive(false);
          ControllerScreen.this.getMenu().activate();
          ControllerScreen.this.processingTimer = PROCESSING_TIMER;
        };
        this.activateButton = addRenderableWidget(new ActivateButton((width - ActivateButton.WIDTH) / 2, 90, this.font, activateOnPress, (b, p, mx, my) -> renderTooltip(p, b.getMessage(), mx, my)));
        // add entry buttons
        this.entryButtons.clear();
        Button.OnTooltip onTooltip = (b, p, mx, my) -> {
            renderTooltip(p, ((EntryButton)b).getTooltips(p, mx, my), Optional.empty(), mx, my);
        };
        for(int i = 0, x, y; i < ENTRY_COUNT; i++) {
            x = this.leftPos + 1 + ENTRY_X + (i % ENTRY_COUNT_X) * EntryButton.WIDTH;
            y = this.topPos + 1 + ENTRY_Y + (i / ENTRY_COUNT_X) * EntryButton.HEIGHT;
            this.entryButtons.add(this.addRenderableWidget(new EntryButton(x, y, this.font, b -> {}, onTooltip)));
        }
        updateEntryButtons();
        containerTick();
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if(getMenu().getController().isEmpty()) {
            return;
        }
        final ControllerBlockEntity controller = getMenu().getController().get();
        // update processing timer
        if(processingTimer > 0) {
            if(--processingTimer <= 0) {
                this.activateButton.setActive(true);
                this.hasTank = controller.hasTank();
                init(getMinecraft(), width, height);
            }
            return;
        }
        // load values
        // determine if controller is activated
        this.hasTank = controller.hasTank();
        this.activateButton.visible = !hasTank;
        this.scrollButton.visible = hasTank;
        // load statuses
        final TankStatus tankStatus = controller.getTankStatus();
        final BreedStatus breedStatus = controller.getBreedStatus();
        final FeedStatus feedStatus = controller.getFeedStatus();
        // create status text and tooltips
        tankStatusText = Component.translatable(PREFIX + "tank_status", tankStatus.getDescription(), Math.round(controller.getGenerationSpeed() * 100.0D))
                .withStyle(a -> a.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, tankStatus.getDescriptionSubtext().copy().withStyle(tankStatus.isActive() ? ChatFormatting.RESET : ChatFormatting.RED))));
        feedStatusText = Component.translatable(PREFIX + "feed_status", feedStatus.getDescription(), Math.round(controller.getFeedSpeed() * 100.0D))
                .withStyle(a -> a.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, feedStatus.getDescriptionSubtext().copy().withStyle(feedStatus.isActive() ? ChatFormatting.RESET : ChatFormatting.RED))));
        breedStatusText = Component.translatable(PREFIX + "breed_status", breedStatus.getDescription(), Math.round(controller.getBreedSpeed() * 100.0D))
                .withStyle(a -> a.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, breedStatus.getDescriptionSubtext().copy().withStyle(breedStatus.isActive() ? ChatFormatting.RESET : ChatFormatting.RED))));
        // verify size
        Optional<TankMultiblock.Size> size = controller.getSize();
        if(size.isEmpty()) {
            this.tankSizeText = Component.translatable(PREFIX + "size.invalid");
            this.axolootlCapacityText = Component.translatable(PREFIX + "capacity.multiple", 0);
            this.modifierCountText = Component.translatable(PREFIX + "modifier_count.invalid");
        } else {
            // create size component
            Vec3i dim = size.get().getDimensions();
            this.tankSizeText = Component.translatable(PREFIX + "size", dim.getX(), dim.getY(), dim.getZ());
            // create capacity component
            int capacity = ControllerBlockEntity.calculateMaxCapacity(size.get());
            this.axolootlCapacityText = Component.translatable(PREFIX + "capacity." + (capacity == 1 ? "single" : "multiple"), capacity);
            // create modifier tooltip
            int modifierCount = controller.getAquariumModifiers().size();
            int activeCount = controller.getActiveAquariumModifiers().size();
            MutableComponent modifierCountTooltip = Component.translatable(PREFIX + "modifier_count.description", activeCount, modifierCount);
            // create modifier component
            this.modifierCountText = Component.translatable(PREFIX + "modifier_count", Component.literal("" + activeCount).withStyle((modifierCount == activeCount ? ChatFormatting.RESET : ChatFormatting.DARK_RED)), modifierCount)
                    .withStyle(a -> a.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, modifierCountTooltip)));
        }
        updateEntryButtons();
    }

    @Override
    protected void renderBgTexture(PoseStack poseStack, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderTexture(0, BACKGROUND);
        blit(poseStack, this.leftPos, this.topPos, 0, 0, WIDTH, HEIGHT);
        if(hasTank) {
            RenderSystem.setShaderTexture(0, CONTROLLER_BG);
            blit(poseStack, this.leftPos, this.topPos, 0, 0, WIDTH, HEIGHT);
        }
    }

    @Override
    public void render(PoseStack pPoseStack, int pMouseX, int pMouseY, float pPartialTick) {
        super.render(pPoseStack, pMouseX, pMouseY, pPartialTick);
        renderDetails(pPoseStack, pMouseX, pMouseY, pPartialTick);
        renderHoverActions(pPoseStack, pMouseX, pMouseY, pPartialTick);
    }

    private void renderDetails(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        int x = leftPos + TEXT_X;
        int y = topPos + TEXT_Y;
        int textDeltaY = font.lineHeight + TEXT_LINE_SPACING;
        // statuses
        font.draw(poseStack, tankStatusText, x, y, 0);
        y += textDeltaY;
        font.draw(poseStack, feedStatusText, x, y, 0);
        y += textDeltaY;
        font.draw(poseStack, breedStatusText, x, y, 0);
        if(!hasTank) {
            return;
        }
        // tank details
        y += textDeltaY * 2;
        font.draw(poseStack, tankSizeText, x, y, 0);
        y += textDeltaY;
        font.draw(poseStack, axolootlCapacityText, x, y, 0);
        y += textDeltaY;
        font.draw(poseStack, modifierCountText, x, y, 0);

    }

    private void renderHoverActions(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        // title
        if(isHovering(this.titleLabelX, this.titleLabelY, this.font.width(this.getTitle()), font.lineHeight, mouseX, mouseY)) {
            renderComponentHoverEffect(poseStack, this.getTitle().getStyle(), mouseX, mouseY);
        }
        // statuses
        int x = TEXT_X;
        int y = TEXT_Y;
        int textDeltaY = font.lineHeight + TEXT_LINE_SPACING;
        if(isHovering(x, y, this.font.width(tankStatusText), font.lineHeight, mouseX, mouseY)) {
            renderComponentHoverEffect(poseStack, tankStatusText.getStyle(), mouseX, mouseY);
        }
        y += textDeltaY;
        if(isHovering(x, y, this.font.width(feedStatusText), font.lineHeight, mouseX, mouseY)) {
            renderComponentHoverEffect(poseStack, feedStatusText.getStyle(), mouseX, mouseY);
        }
        y += textDeltaY;
        if(isHovering(x, y, this.font.width(breedStatusText), font.lineHeight, mouseX, mouseY)) {
            renderComponentHoverEffect(poseStack, breedStatusText.getStyle(), mouseX, mouseY);
        }
        if(!hasTank) {
            return;
        }
        // modifier count
        y += textDeltaY * 4;
        if(isHovering(x, y, this.font.width(modifierCountText), font.lineHeight, mouseX, mouseY)) {
            renderComponentHoverEffect(poseStack, modifierCountText.getStyle(), mouseX, mouseY);
        }
    }

    private void updateEntryButtons() {
        for(int i = 0, n = entryButtons.size(); i < n; i++) {
            EntryButton button = entryButtons.get(i);
            int index = i + scrollOffset * ENTRY_COUNT_X;
            if(!hasTank || index < 0 || index >= modifierDataList.size()) {
                button.visible = button.active = false;
                continue;
            }
            button.visible = true;
            button.update(modifierDataList.get(index));
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
        this.scrollOffset = Mth.floor(Math.max(0, percent * Math.max(0, modifierDataList.size() - ENTRY_COUNT)));
        updateEntryButtons();
    }

    //// DATA ////

    private static class ModifierData {
        private final ResourceLocation id;
        private final AquariumModifier modifier;
        private int activeCount;
        private int count;

        private ModifierData(ResourceLocation id, AquariumModifier modifier) {
            this.id = id;
            this.modifier = modifier;
        }

        private void addCount(final boolean active) {
            if(active) {
                activeCount++;
            }
            count++;
        }

        public ResourceLocation getId() {
            return id;
        }

        public AquariumModifier getModifier() {
            return modifier;
        }

        public int getActiveCount() {
            return activeCount;
        }

        public int getCount() {
            return count;
        }
    }

    //// WIDGETS ////

    private static class EntryButton extends ImageButton {
        private static final int WIDTH = 186;
        private static final int HEIGHT = 18;

        private final Font font;
        private ModifierData entry;
        private boolean hasInactive;
        private Component showBonuses;
        private Component showConditions;
        private Component description;
        private Component countText;
        private List<Component> nameTooltips;
        private List<Component> countTooltips;
        private List<Component> bonusesTooltips;
        private List<Component> conditionsTooltips;

        public EntryButton(int pX, int pY, Font font, OnPress pOnPress, OnTooltip pOnTooltip) {
            super(pX, pY, WIDTH, HEIGHT, 0, 234, 0, WIDGETS, 256, 256, pOnPress, pOnTooltip, Component.empty());
            this.nameTooltips = new ArrayList<>();
            this.countTooltips = new ArrayList<>();
            this.bonusesTooltips = new ArrayList<>();
            this.conditionsTooltips = new ArrayList<>();
            this.showBonuses = Component.translatable(PREFIX + "entry.bonuses").withStyle(ChatFormatting.GOLD).append(" ").append(Component.translatable(PREFIX + "entry.show_bonuses").withStyle(ChatFormatting.YELLOW));
            this.showConditions = Component.translatable(PREFIX + "entry.condition").withStyle(ChatFormatting.GOLD).append(" ").append(Component.translatable(PREFIX + "entry.show_condition").withStyle(ChatFormatting.YELLOW));
            this.font = font;
            this.entry = new ModifierData(new ResourceLocation("empty"), AquariumModifier.EMPTY);
            this.description = Component.empty();
        }

        public void update(final ModifierData entry) {
            this.entry = entry;
            Component activeText = Component.literal("" + entry.getActiveCount()).withStyle(hasInactive ? ChatFormatting.DARK_RED : ChatFormatting.RESET);
            this.countText = Component.translatable(PREFIX + "entry.count", activeText, entry.getCount());
            final String sDescription = entry.getModifier().getDescription().getString();
            this.description = Component.literal(StringUtil.truncateStringIfNecessary(sDescription, 38 - font.width(countText) / 6, true)).withStyle(entry.getModifier().getDescription().getStyle());
            this.hasInactive = entry.getCount() > entry.getActiveCount();
            // add name tooltips
            this.nameTooltips.clear();
            this.nameTooltips.add(entry.getModifier().getDescription());
            this.nameTooltips.add(Component.literal(entry.getId().toString()).withStyle(ChatFormatting.GRAY));
            // add count tooltips
            this.countTooltips.clear();
            final Component activeCountText = Component.literal("" + entry.getActiveCount()).withStyle(hasInactive ? ChatFormatting.RED : ChatFormatting.RESET);
            if(entry.getActiveCount() == 1) {
                countTooltips.add(Component.translatable(PREFIX + "entry.active.single", activeCountText));
            } else {
                countTooltips.add(Component.translatable(PREFIX + "entry.active.multiple", activeCountText));
            }
            // add bonuses tooltips
            this.bonusesTooltips.clear();
            this.bonusesTooltips.addAll(createBonusesTooltips(entry.getModifier().getSettings(), entry.getActiveCount()));
            // add conditions tooltips
            this.conditionsTooltips.clear();
            conditionsTooltips.add(Component.translatable(PREFIX + "entry.condition").withStyle(ChatFormatting.GOLD));
            // add energy cost tooltip
            if(entry.getModifier().getSettings().getEnergyCost() > 0) {
                conditionsTooltips.add(createBonusTooltip("axolootl.modifier_settings.energy_cost", entry.getActiveCount(), Component.literal("" + entry.getModifier().getSettings().getEnergyCost()).withStyle(ChatFormatting.RED), Component.literal("" + (entry.getModifier().getSettings().getEnergyCost() * entry.getActiveCount())).withStyle(ChatFormatting.RED)));
            }
            conditionsTooltips.addAll(entry.getModifier().getCondition().getDescription());
        }

        public List<Component> getTooltips(final PoseStack poseStack, final int mouseX, final int mouseY) {
            // name tooltips
            if(mouseX < this.x + this.width - font.width(countText) - 4) {
                return this.nameTooltips;
            }
            final List<Component> list = new ArrayList<>(countTooltips);
            // bonuses tooltips
            if(Screen.hasShiftDown()) {
                list.addAll(bonusesTooltips);
            } else {
                list.add(showBonuses);
            }
            // conditions tooltips
            if(hasInactive || Screen.hasControlDown()) {
                list.addAll(conditionsTooltips);
            } else {
                list.add(showConditions);
            }
            return list;
        }

        @Override
        public void renderButton(PoseStack pPoseStack, int pMouseX, int pMouseY, float pPartialTick) {
            int messageWidth = font.width(countText);
            super.renderButton(pPoseStack, pMouseX, pMouseY, pPartialTick);
            final float drawY = this.y + (this.height - font.lineHeight) / 2.0F;
            font.draw(pPoseStack, description, this.x + 4, drawY, 0);
            font.draw(pPoseStack, countText, this.x + (this.width - messageWidth) - 4, drawY, 0);
        }

        private static List<Component> createBonusesTooltips(final ModifierSettings modifier, final int activeCount) {
            final ImmutableList.Builder<Component> builder = ImmutableList.builder();
            final String PREFIX = "axolootl.modifier_settings.";
            if(Math.abs(modifier.getGenerationSpeed()) > 1.0E-8) {
                builder.add(createBonusTooltip(PREFIX + "generation_speed", activeCount, toAdditivePercentage(modifier.getGenerationSpeed(), ChatFormatting.GREEN), toAdditivePercentage(modifier.getGenerationSpeed() * activeCount, ChatFormatting.GREEN)));
            }
            if(Math.abs(modifier.getBreedSpeed()) > 1.0E-8) {
                builder.add(createBonusTooltip(PREFIX + "breed_speed", activeCount, toAdditivePercentage(modifier.getBreedSpeed(), ChatFormatting.LIGHT_PURPLE), toAdditivePercentage(modifier.getBreedSpeed() * activeCount, ChatFormatting.LIGHT_PURPLE)));
            }
            if(Math.abs(modifier.getFeedSpeed()) > 1.0E-8) {
                builder.add(createBonusTooltip(PREFIX + "feed_speed", activeCount, toAdditivePercentage(modifier.getFeedSpeed(), ChatFormatting.YELLOW), toAdditivePercentage(modifier.getFeedSpeed() * activeCount, ChatFormatting.YELLOW)));
            }
            if(modifier.getSpreadSpeed() > 1.0E-8) {
                builder.add(createBonusTooltip(PREFIX + "spread_speed", activeCount, toAdditivePercentage(modifier.getSpreadSpeed(), ChatFormatting.BLUE), toAdditivePercentage(modifier.getSpreadSpeed() * activeCount, ChatFormatting.BLUE)));
            }
            if(modifier.isEnableMobResources()) {
                builder.add(Component.translatable(PREFIX + "enable_mob_resources").withStyle(ChatFormatting.AQUA, ChatFormatting.ITALIC));
            }
            if(modifier.isEnableMobBreeding()) {
                builder.add(Component.translatable(PREFIX + "enable_mob_breeding").withStyle(ChatFormatting.AQUA, ChatFormatting.ITALIC));
            }
            return builder.build();
        }

        private static MutableComponent createBonusTooltip(final String key, final int activeCount, final Object single, final Object multiple) {
            if(activeCount > 1) {
                return Component.translatable(key + ".multiple", multiple, single);
            }
            return Component.translatable(key + ".single", single);
        }

    }

    private static class ActivateButton extends ImageButton {
        private static final int WIDTH = 92;
        private static final int HEIGHT = 20;

        private final Font font;
        private final Component message;
        private final Component messagePending;

        public ActivateButton(int pX, int pY, Font font, OnPress pOnPress, OnTooltip pOnTooltip) {
            super(pX, pY, WIDTH, HEIGHT, 137, 144, HEIGHT, WIDGETS, 256, 256, pOnPress, pOnTooltip, Component.translatable(PREFIX + "activate"));
            this.font = font;
            this.message = getMessage();
            this.messagePending = Component.translatable(PREFIX + "activate.pending");
        }

        public void setActive(final boolean active) {
            this.active = active;
            if(active) {
                setMessage(message);
            } else {
                setMessage(messagePending);
            }
        }

        @Override
        public void renderButton(PoseStack pPoseStack, int pMouseX, int pMouseY, float pPartialTick) {
            super.renderButton(pPoseStack, pMouseX, pMouseY, pPartialTick);
            font.draw(pPoseStack, getMessage(), this.x + (this.width - font.width(getMessage())) / 2.0F, this.y + (this.height - font.lineHeight) / 2.0F, 0);
        }
    }
}
