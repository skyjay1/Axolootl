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
import axolootl.menu.ControllerMenu;
import axolootl.util.BreedStatus;
import axolootl.util.FeedStatus;
import axolootl.util.TankMultiblock;
import axolootl.util.TankStatus;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.util.ArrayList;
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
    private int textDeltaY;

    // WIDGETS //
    private ScrollButton scrollButton;

    // DATA //
    private Map<AquariumModifier, Integer> modifierCountMap;

    // COMPONENTS //
    public static final String PREFIX = "gui.controller_tab.axolootl.controller.";
    private Component tankSizeText;
    private Component tankStatusText;
    private Component feedStatusText;
    private Component breedStatusText;
    private Component axolootlCapacityText;
    private Component modifierCountText;
    private List<Component> modifierCountListText;

    public ControllerScreen(ControllerMenu pMenu, Inventory pPlayerInventory, Component pTitle) {
        super(pMenu, pPlayerInventory, addHoverText(pTitle, Component.literal("(" + pMenu.getBlockPos().toShortString() + ")")));
        this.modifierCountMap = new HashMap<>();
        this.modifierCountListText = new ArrayList<>();
        this.tankSizeText = Component.empty();
        this.tankStatusText = Component.empty();
        this.feedStatusText = Component.empty();
        this.breedStatusText = Component.empty();
        this.axolootlCapacityText = Component.empty();
        this.modifierCountText = Component.empty();
    }

    private void updateModifierCountMap() {
        if(getMenu().getController().isEmpty()) {
            return;
        }
        // prepare to create map
        final ControllerBlockEntity controller = getMenu().getController().get();
        final Map<BlockPos, AquariumModifier> modifierMap = controller.resolveModifiers(getMenu().getInventory().player.level.registryAccess());
        // build map
        this.modifierCountMap.clear();
        for(AquariumModifier modifier : modifierMap.values()) {
            int count = modifierCountMap.getOrDefault(modifier, 0);
            modifierCountMap.put(modifier, count + 1);
        }
        // populate list
        modifierCountListText.clear();
        for(Map.Entry<AquariumModifier, Integer> entry : modifierCountMap.entrySet()) {
            modifierCountListText.add(Component.translatable(PREFIX + "modifier_entry", entry.getValue(), entry.getKey().getDescription()));
        }
    }

    @Override
    protected void init() {
        super.init();
        updateModifierCountMap();
        this.textDeltaY = font.lineHeight + TEXT_LINE_SPACING;
        // update title position
        this.titleLabelX = (this.imageWidth - font.width(this.getTitle())) / 2;
        // add scroll button
        this.scrollButton = addRenderableWidget(new ScrollButton(leftPos + 202, topPos + 104, 12, 110, WIDGETS, 244, 0, 12, 15, 15, true, 1.0F, this));
        this.setFocused(this.scrollButton);
        containerTick();
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if(getMenu().getController().isEmpty()) {
            return;
        }
        // load values
        final ControllerBlockEntity controller = getMenu().getController().get();
        Optional<TankMultiblock.Size> size = controller.getSize();
        final TankStatus tankStatus = controller.getTankStatus();
        final BreedStatus breedStatus = controller.getBreedStatus();
        final FeedStatus feedStatus = controller.getFeedStatus();
        // crate status text and tooltips
        tankStatusText = Component.translatable(PREFIX + "tank_status", tankStatus.getDescription(), Math.round(controller.getGenerationSpeed() * 100.0D))
                .withStyle(a -> a.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, tankStatus.getDescriptionSubtext().copy().withStyle(tankStatus.isActive() ? ChatFormatting.RESET : ChatFormatting.RED))));
        feedStatusText = Component.translatable(PREFIX + "feed_status", feedStatus.getDescription(), Math.round(controller.getFeedSpeed() * 100.0D))
                .withStyle(a -> a.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, feedStatus.getDescriptionSubtext().copy().withStyle(feedStatus.isActive() ? ChatFormatting.RESET : ChatFormatting.RED))));
        breedStatusText = Component.translatable(PREFIX + "breed_status", breedStatus.getDescription(), Math.round(controller.getBreedSpeed() * 100.0D))
                .withStyle(a -> a.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, breedStatus.getDescriptionSubtext().copy().withStyle(breedStatus.isActive() ? ChatFormatting.RESET : ChatFormatting.RED))));
        // verify size
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
            modifierCountListText.forEach(c -> modifierCountTooltip.append("\n").append(c));
            // create modifier component
            this.modifierCountText = Component.translatable(PREFIX + "modifier_count", Component.literal("" + activeCount).withStyle((modifierCount == activeCount ? ChatFormatting.RESET : ChatFormatting.RED)), modifierCount)
                    .withStyle(a -> a.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, modifierCountTooltip)));
        }


    }

    @Override
    protected void renderBgTexture(PoseStack poseStack, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderTexture(0, BACKGROUND);
        blit(poseStack, this.leftPos, this.topPos, 0, 0, WIDTH, HEIGHT);
        RenderSystem.setShaderTexture(0, CONTROLLER_BG);
        blit(poseStack, this.leftPos, this.topPos, 0, 0, WIDTH, HEIGHT);
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
        // statuses
        font.draw(poseStack, tankStatusText, x, y, 0);
        y += textDeltaY;
        font.draw(poseStack, feedStatusText, x, y, 0);
        y += textDeltaY;
        font.draw(poseStack, breedStatusText, x, y, 0);
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
        // modifier count
        y += textDeltaY * 4;
        if(isHovering(x, y, this.font.width(modifierCountText), font.lineHeight, mouseX, mouseY)) {
            renderComponentHoverEffect(poseStack, modifierCountText.getStyle(), mouseX, mouseY);
        }
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
