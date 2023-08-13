/**
 * Copyright (c) 2023 Skyler James
 * Permission is granted to use, modify, and redistribute this software, in parts or in whole,
 * under the GNU LGPLv3 license (https://www.gnu.org/licenses/lgpl-3.0.en.html)
 **/

package axolootl.client.menu;

import axolootl.AxRegistry;
import axolootl.Axolootl;
import axolootl.capability.AxolootlResearchCapability;
import axolootl.client.menu.widget.ScrollButton;
import axolootl.data.axolootl_variant.AxolootlVariant;
import axolootl.data.axolootl_variant.Bonuses;
import axolootl.data.axolootl_variant.BonusesProvider;
import axolootl.data.breeding.AxolootlBreeding;
import axolootl.data.resource_generator.ResourceGenerator;
import axolootl.item.AxolootlBucketItem;
import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.datafixers.util.Either;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.util.StringUtil;
import net.minecraft.util.random.WeightedEntry;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

public class AxolootlInspectorDetailsScreen extends Screen implements ScrollButton.IScrollListener {

    // TEXTURES //
    public static final ResourceLocation TEXTURE = new ResourceLocation(Axolootl.MODID, "textures/gui/aquarium/axolootl_details.png");
    public static final ResourceLocation WIDGETS = AbstractTabScreen.WIDGETS;

    // WIDGET CONSTANTS //
    public static final int WIDTH = 250;
    public static final int HEIGHT = 176;
    public static final int GENERATOR_X = 128;
    public static final int GENERATOR_Y = 21;
    public static final int DETAILS_X = 8;
    public static final int DETAILS_Y = 8;
    public static final int DETAILS_LINE_SPACING = 4;
    public static final int GENERATOR_WIDTH = 100;
    public static final int GENERATOR_HEIGHT = 147;
    public static final int FOOD_MAX_COUNT = 6;
    public static final int BREED_FOOD_MAX_COUNT = 6;

    // WIDGETS //
    private final List<ComponentButton> componentButtons;
    private final List<ComponentButton> generatorButtons;
    private final List<CyclingItemButton> foodButtons;
    private final List<CyclingItemButton> breedFoodButtons;
    private ScrollButton scrollButton;
    private int scrollOffset;
    private int totalHeight;
    private int generatorCountY;

    // DATA //
    private int leftPos;
    private int topPos;
    private long ticksOpen;
    private final ResourceLocation id;
    private final AxolootlVariant variant;
    private final ItemStack itemStack;
    private final List<List<ItemStack>> foods;
    private final List<List<ItemStack>> breedFoods;
    private final List<ParentData> parentData;

    // COMPONENTS //
    public static final String PREFIX = "gui.controller_tab.axolootl.axolootl_details.";
    private final List<Component> generatorText;
    private final Component generatorTitleText;
    private final Component tierText;
    private final Component foodText;
    private final Component breedFoodText;
    private final Component parentsTitleText;
    private final List<Component> parentsText;

    public AxolootlInspectorDetailsScreen(final ResourceLocation id, final AxolootlVariant variant, final ItemStack icon, final RegistryAccess access) {
        super(createAxolootlName(variant, id));
        this.id = id;
        this.variant = variant;
        this.itemStack = icon;
        this.componentButtons = new ArrayList<>();
        this.generatorButtons = new ArrayList<>();
        this.foodButtons = new ArrayList<>();
        this.breedFoodButtons = new ArrayList<>();
        this.generatorText = new ArrayList<>();
        this.parentsText = new ArrayList<>();
        this.parentData = calculateParentData(this.variant, access);
        this.ticksOpen = 0;
        // determine food and breed food item stacks
        final List<HolderSet<Item>> foods = new ArrayList<>();
        for(BonusesProvider provider : this.variant.getFoods()) {
            foods.add(provider.getFoods());
        }
        this.foods = distributeEqually(resolveHolderSets(foods), FOOD_MAX_COUNT);
        this.breedFoods = distributeEqually(resolveHolderSet(this.variant.getBreedFood()), BREED_FOOD_MAX_COUNT);
        // create text components
        this.generatorTitleText = Component.translatable(PREFIX + "loot")
                .withStyle(a -> a.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable(PREFIX + "loot.description"))));
        this.foodText = Component.translatable(PREFIX + "food")
                .withStyle(a -> a.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable(PREFIX + "food.description"))));
        this.breedFoodText = Component.translatable(PREFIX + "breed_food")
                .withStyle(a -> a.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable(PREFIX + "breed_food.description"))));
        this.parentsTitleText = Component.translatable(PREFIX + "parents")
                .withStyle(a -> a.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable(PREFIX + "parents.description"))));
        this.tierText = Component.translatable("entity.axolootl.axolootl.tier", variant.getTierDescription());
        // create parents data text
        if(this.parentData.isEmpty()) {
            this.parentsText.add(Component.translatable(PREFIX + "parents.entry.none"));
        }
        for(ParentData entry : this.parentData) {
            Component parentA = createTieredAxolootlName(entry.parentA(), entry.parentA().getRegistryName(access));
            Component parentB = createTieredAxolootlName(entry.parentB(), entry.parentB().getRegistryName(access));
            Component chance = Component.literal(String.format("%.2f", entry.chance() * 100.0D).replaceAll("0*$", "").replaceAll("\\.$", ""));
            this.parentsText.add(Component.translatable(PREFIX + "parents.entry.chance", chance).withStyle(ChatFormatting.DARK_BLUE));
            this.parentsText.add(Component.literal("  ").append(parentA).withStyle(parentA.getStyle()).withStyle(a -> a.withClickEvent(new ClickEvent(ClickEvent.Action.CHANGE_PAGE, entry.parentA().getRegistryName(access).toString()))));
            this.parentsText.add(Component.literal("  ").append(parentB).withStyle(parentB.getStyle()).withStyle(a -> a.withClickEvent(new ClickEvent(ClickEvent.Action.CHANGE_PAGE, entry.parentB().getRegistryName(access).toString()))));
        }
        // add energy cost to generator description list, if any
        if(this.variant.getEnergyCost() > 0) {
            this.generatorText.add(Component.translatable(PREFIX + "energy_cost", this.variant.getEnergyCost())
                    .withStyle(ChatFormatting.DARK_RED)
                    .withStyle(a -> a.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable(PREFIX + "energy_cost.description", this.variant.getEnergyCost())))));
        }
        // create generator description components
        final int maxLength = 19;
        for(Component c : this.variant.getResourceGeneratorDescription()) {
            // truncate length
            final String raw = c.getString();
            final String truncated = StringUtil.truncateStringIfNecessary(raw, maxLength, true);
            final String stripped = raw.stripLeading();
            // add truncated text with hover action
            this.generatorText.add(Component.literal(truncated).withStyle(c.getStyle().withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(stripped)))));
        }
    }

    private static Component createAxolootlName(final AxolootlVariant variant, final ResourceLocation id) {
        return Component.translatable("entity.axolootl.axolootl.description", Component.translatable("entity.axolootl.axolootl"), variant.getDescription()).withStyle(a -> a.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, variant.getDescription().copy().append("\n").append(Component.literal("" + id).withStyle(ChatFormatting.GRAY)))));
    }

    private static Component createTieredAxolootlName(final AxolootlVariant variant, final ResourceLocation id) {
        return Component.translatable(PREFIX + "parents.entry.axolootl_tier", variant.getDescription(), variant.getTierDescription()).withStyle(a -> a.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, variant.getDescription().copy().append("\n").append(Component.literal("" + id).withStyle(ChatFormatting.GRAY)))));
    }

    private static List<List<ItemStack>> distributeEqually(final Collection<ItemStack> collection, final int maxCount) {
        // determine number of lists required
        int listCount = Math.min(collection.size(), maxCount);
        // create lists
        final List<List<ItemStack>> list = new ArrayList<>();
        for(int i = 0; i < listCount; i++) {
            list.add(new ArrayList<>());
        }
        // populate lists
        final Iterator<ItemStack> iterator = collection.iterator();
        for(int i = 0, n = collection.size(); i < n && iterator.hasNext(); i++) {
            list.get((i % listCount)).add(iterator.next());
        }
        // convert to lists
        return list;
    }

    @Override
    protected void init() {
        super.init();
        this.leftPos = (width - WIDTH) / 2;
        this.topPos = (height - HEIGHT) / 2;
        final int componentHeight = ComponentButton.getHeight(font);
        this.totalHeight = generatorText.size() * componentHeight;
        this.generatorCountY = GENERATOR_HEIGHT / componentHeight;
        // add scroll button
        this.scrollButton = addRenderableWidget(new ScrollButton(leftPos + 229, topPos + 22, 12, GENERATOR_HEIGHT - 2, WIDGETS, 244, 0, 12, 15, 15, true, 1.0F / Math.max(1, generatorText.size() - generatorCountY), this));
        this.setFocused(this.scrollButton);
        this.scrollButton.active = this.totalHeight > GENERATOR_HEIGHT;
        // create on tooltip
        final Button.OnTooltip componentButtonOnTooltip = (b, p, mx, my) -> renderComponentHoverEffect(p, b.getMessage().getStyle(), mx, my);
        // add static component buttons
        int x = this.leftPos + DETAILS_X;
        int y = this.topPos + DETAILS_Y + (16 - font.lineHeight) / 2;
        int deltaY = DETAILS_LINE_SPACING + font.lineHeight;
        this.componentButtons.add(addRenderableWidget(new ComponentButton(x + 16 + 2, y, font.lineHeight, font, getTitle(), componentButtonOnTooltip)));
        this.componentButtons.add(addRenderableWidget(new ComponentButton(this.leftPos + GENERATOR_X, y, font.lineHeight, font, generatorTitleText, componentButtonOnTooltip)));
        y += deltaY;
        this.componentButtons.add(addRenderableWidget(new ComponentButton(x + 16 + 2, y, font.lineHeight, font, tierText, componentButtonOnTooltip)));
        y += deltaY + 4;
        // add food buttons
        this.componentButtons.add(addRenderableWidget(new ComponentButton(x, y, font.lineHeight, font, foodText, componentButtonOnTooltip)));
        final Button.OnTooltip cyclingItemButtonOnTooltip = (b, p, mx, my) -> renderTooltip(p, ((CyclingItemButton)b).getTooltips(), Optional.empty(), mx, my);
        y += deltaY;
        for(int i = 0, n = foods.size(); i < n; i++) {
            this.foodButtons.add(addRenderableWidget(new CyclingItemButton(x + i * (CyclingItemButton.WIDTH + 3), y, itemRenderer, foods.get(i), itemStack -> getFoodTooltip(this.variant, itemStack), cyclingItemButtonOnTooltip)));
        }
        y += CyclingItemButton.HEIGHT + DETAILS_LINE_SPACING;
        // add breed buttons
        this.componentButtons.add(addRenderableWidget(new ComponentButton(x, y, font.lineHeight, font, breedFoodText, componentButtonOnTooltip)));
        y += deltaY;
        for(int i = 0, n = breedFoods.size(); i < n; i++) {
            this.breedFoodButtons.add(addRenderableWidget(new CyclingItemButton(x + i * (CyclingItemButton.WIDTH + 3), y, itemRenderer, breedFoods.get(i), this::getTooltipFromItem, cyclingItemButtonOnTooltip)));
        }
        y += CyclingItemButton.HEIGHT + deltaY;
        // add parent info
        this.componentButtons.add(addRenderableWidget(new ComponentButton(x, y, font.lineHeight, font, parentsTitleText, componentButtonOnTooltip)));
        y += deltaY;
        for(Component entry : parentsText) {
            this.componentButtons.add(addRenderableWidget(new ComponentButton(x, y, font.lineHeight, font, entry, componentButtonOnTooltip)));
            y += font.lineHeight + 1;
        }
        // add generator component buttons
        this.generatorButtons.clear();
        x = this.leftPos + GENERATOR_X + 1;
        y = this.topPos + GENERATOR_Y + 1;
        for(int i = 0; i < generatorCountY; i++) {
            generatorButtons.add(addRenderableWidget(new ComponentButton(x, y, GENERATOR_WIDTH - 2, componentHeight, font, Component.empty(), componentButtonOnTooltip)));
            y += componentHeight;
        }
        updateComponentButtons();
    }

    @Override
    public void tick() {
        super.tick();
        this.ticksOpen++;
        updateCyclingItemButtons();
    }

    @Override
    public void render(PoseStack pPoseStack, int pMouseX, int pMouseY, float pPartialTick) {
        // render background gradient
        renderBackground(pPoseStack);
        RenderSystem.disableBlend();
        // prepare to render textures and components
        setBlitOffset(0);
        // render background texture
        RenderSystem.setShaderTexture(0, TEXTURE);
        blit(pPoseStack, this.leftPos, this.topPos, 0, 0, WIDTH, HEIGHT);
        // render item
        final int deltaY = DETAILS_LINE_SPACING + font.lineHeight;
        final int x = this.leftPos + DETAILS_X;
        final int y = this.topPos + DETAILS_Y + (font.lineHeight + deltaY - 16);
        PoseStack modelViewStack = RenderSystem.getModelViewStack();
        modelViewStack.pushPose();
        modelViewStack.translate(x, y, 2_000);
        RenderSystem.applyModelViewMatrix();
        this.itemRenderer.renderAndDecorateItem(itemStack, 0, 0);
        modelViewStack.popPose();
        RenderSystem.applyModelViewMatrix();
        // render widgets
        super.render(pPoseStack, pMouseX, pMouseY, pPartialTick);
        // render tooltips
        renderHoverActions(pPoseStack, pMouseX, pMouseY, pPartialTick);
    }

    private void renderHoverActions(final PoseStack poseStack, int pMouseX, int pMouseY, float pPartialTick) {

    }

    @Override
    public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
        if(pMouseX < this.leftPos || pMouseX > this.leftPos + WIDTH || pMouseY < this.topPos || pMouseY > this.topPos + HEIGHT) {
            onClose();
            return true;
        }
        return super.mouseClicked(pMouseX, pMouseY, pButton);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void updateCyclingItemButtons() {
        for(CyclingItemButton button : foodButtons) {
            button.tick(ticksOpen);
        }
        for(CyclingItemButton button : breedFoodButtons) {
            button.tick(ticksOpen);
        }
    }

    private void updateComponentButtons() {
        for(int i = 0, n = generatorButtons.size(); i < n; i++) {
            ComponentButton button = generatorButtons.get(i);
            int index = i + scrollOffset;
            if(index < 0 || index >= generatorText.size()) {
                button.visible = button.active = false;
                continue;
            }
            button.visible = true;
            button.update(generatorText.get(index));
        }
    }

    public static boolean canOpenDetails(final Player player, final ResourceLocation id) {
        // validate variant exists and is valid
        final Optional<AxolootlVariant> oVariant = AxolootlVariant.getRegistry(player.level.registryAccess()).getOptional(id);
        if(oVariant.isEmpty() || !AxRegistry.AxolootlVariantsReg.isValid(id)) {
            return false;
        }
        // validate player capability
        if(!player.getCapability(Axolootl.AXOLOOTL_RESEARCH_CAPABILITY).orElse(AxolootlResearchCapability.EMPTY).containsAxolootl(id)) {
            return false;
        }
        // all checks passed
        return true;
    }

    public static void checkAndOpenDetails(final Minecraft minecraft, final ResourceLocation id) {
        // validate level and player
        if(null == minecraft.level || null == minecraft.player) {
            return;
        }
        // validate axolootl research
        if(!canOpenDetails(minecraft.player, id)) {
            return;
        }
        final RegistryAccess access = minecraft.level.registryAccess();
        openDetails(minecraft, access, id);
    }

    public static void openDetails(final Minecraft minecraft, final RegistryAccess access, final ResourceLocation id) {
        final AxolootlVariant variant = AxolootlVariant.getRegistry(access).getOptional(id).orElse(AxolootlVariant.EMPTY);
        final ItemStack icon = AxolootlBucketItem.getWithVariant(new ItemStack(AxRegistry.ItemReg.AXOLOOTL_BUCKET.get()), id);
        minecraft.pushGuiLayer(new AxolootlInspectorDetailsScreen(id, variant, icon, access));
    }

    public static void openDetails(final Minecraft minecraft, final RegistryAccess access, final ResourceLocation id, final AxolootlVariant variant, final ItemStack icon) {
        minecraft.pushGuiLayer(new AxolootlInspectorDetailsScreen(id, variant, icon, access));
    }

    private static Collection<ItemStack> resolveHolderSets(final Collection<HolderSet<Item>> holderSets) {
        final Set<Item> collection = new HashSet<>();
        for(HolderSet<Item> holderSet : holderSets) {
            collection.addAll(resolveHolderSetItems(holderSet));
        }
        // convert to item stacks
        final List<ItemStack> itemStacks = new ArrayList<>(collection.size());
        for(Item item : collection) {
            itemStacks.add(item.getDefaultInstance());
        }
        return itemStacks;
    }

    private static Collection<ItemStack> resolveHolderSet(final HolderSet<Item> holderSet) {
        // convert to item stacks
        final List<ItemStack> itemStacks = new ArrayList<>();
        for(Item item : resolveHolderSetItems(holderSet)) {
            itemStacks.add(item.getDefaultInstance());
        }
        return itemStacks;
    }

    private static Collection<Item> resolveHolderSetItems(final HolderSet<Item> holderSet) {
        final Set<Item> collection = new HashSet<>();
        final Either<TagKey<Item>, List<Holder<Item>>> either = holderSet.unwrap();
        // add tag key elements
        either.ifLeft(key -> {
            for(Item item : ForgeRegistries.ITEMS.tags().getTag(key))  {
                collection.add(item);
            }
        });
        // add holder elements
        either.ifRight(items -> {
            for(Holder<Item> item : items) {
                collection.add(item.value());
            }
        });
        return collection;
    }

    //// SCROLL LISTENER ////

    @Override
    public boolean mouseScrolled(double pMouseX, double pMouseY, double pDelta) {
        if(pMouseX >= (this.leftPos + GENERATOR_X - 1) && pMouseX < (this.leftPos + GENERATOR_X + GENERATOR_WIDTH + 1) && pMouseY >= (this.topPos + GENERATOR_Y - 1) && pMouseY < (this.topPos + GENERATOR_Y + GENERATOR_HEIGHT + 1)) {
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
        this.scrollOffset = Mth.floor(Math.max(0, percent * Math.max(0, generatorText.size() - generatorCountY)));
        updateComponentButtons();
    }

    //// DATA ////

    private static record ParentData(AxolootlVariant parentA, AxolootlVariant parentB, double chance) {}

    private static List<ParentData> calculateParentData(final AxolootlVariant variant, final RegistryAccess access) {
        final ResourceLocation variantId = variant.getRegistryName(access);
        final Registry<AxolootlBreeding> breedingRegistry = AxolootlBreeding.getRegistry(access);
        final List<ParentData> list = new ArrayList<>();
        // iterate breeding recipes
        for(AxolootlBreeding breeding : breedingRegistry) {
            // iterate results of each recipe to check for the given variant
            for(WeightedEntry.Wrapper<Holder<AxolootlVariant>> wrapper : breeding.getResult().unwrap()) {
                if(wrapper.getData().is(variantId)) {
                    // result variant matches, calculate parents and percentage chance
                    double totalWeight = ResourceGenerator.calculateTotalWeight(breeding.getResult());
                    list.add(new ParentData(breeding.getFirst().value(), breeding.getSecond().value(), wrapper.getWeight().asInt() / totalWeight));
                }
            }
        }
        return list;
    }

    private List<Component> getFoodTooltip(final AxolootlVariant variant, final ItemStack itemStack) {
        // load tooltips
        final List<Component> list = new ArrayList<>(getTooltipFromItem(itemStack));
        // validate list not empty
        if(list.isEmpty()) {
            return list;
        }
        // validate bonuses
        final Optional<Bonuses> oBonuses = variant.getFoodBonuses(itemStack.getItem());
        if(oBonuses.isEmpty()) {
            return list;
        }
        final Bonuses bonuses = oBonuses.get();
        // insert applicable bonus tooltips
        double bonus = bonuses.getBreedBonus();
        if(Math.abs(bonus) > 1.0E-7) {
            list.add(1, Component.translatable("axolootl.modifier_settings.breed_speed.single", ControllerScreen.toAdditivePercentage(bonus, bonus < 0 ? ChatFormatting.RED : ChatFormatting.LIGHT_PURPLE)));
        }
        bonus = bonuses.getFeedBonus();
        if(Math.abs(bonus) > 1.0E-7) {
            list.add(1, Component.translatable("axolootl.modifier_settings.feed_speed.single", ControllerScreen.toAdditivePercentage(bonus, bonus < 0 ? ChatFormatting.RED : ChatFormatting.YELLOW)));
        }
        bonus = bonuses.getGenerationBonus();
        if(Math.abs(bonus) > 1.0E-7) {
            list.add(1, Component.translatable("axolootl.modifier_settings.generation_speed.single", ControllerScreen.toAdditivePercentage(bonus, bonus < 0 ? ChatFormatting.RED : ChatFormatting.GREEN)));
        }
        return list;
    }

    //// WIDGETS ////

    private static class ComponentButton extends Button {

        private final Font font;
        private Component hoverMessage;

        public ComponentButton(int pX, int pY, int height, Font font, Component pMessage, OnTooltip onTooltip) {
            this(pX, pY, font.width(pMessage), height, font, pMessage, onTooltip);
        }

        public ComponentButton(int pX, int pY, int width, int height, Font font, Component pMessage, OnTooltip onTooltip) {
            super(pX, pY, width, height, pMessage, b -> ((ComponentButton)b).onPressComponent(), onTooltip);
            this.hoverMessage = Component.empty();
            this.font = font;
            update(getMessage());
        }

        public void update(final Component message) {
            this.setMessage(message);
            this.hoverMessage = (message.getStyle().getClickEvent() != null) ? message.copy().withStyle(message.getStyle()).withStyle(ChatFormatting.UNDERLINE) : message;
        }

        public static int getHeight(Font font) {
            return font.lineHeight + 2;
        }

        @Override
        public void renderButton(PoseStack pPoseStack, int pMouseX, int pMouseY, float pPartialTick) {
            this.font.draw(pPoseStack, isHoveredOrFocused() ? hoverMessage : getMessage(), this.x, this.y + 1, 0);
            if(this.isHoveredOrFocused()) {
                this.renderToolTip(pPoseStack, pMouseX, pMouseY);
            }
        }

        private void onPressComponent() {
            // validate click event
            final ClickEvent event = getMessage().getStyle().getClickEvent();
            if(null == event || event.getAction() != ClickEvent.Action.CHANGE_PAGE) {
                return;
            }
            ResourceLocation id = new ResourceLocation(event.getValue());
            Minecraft minecraft = Minecraft.getInstance();
            // validate level and player
            if(null == minecraft.level || null == minecraft.player) {
                return;
            }
            // validate axolootl research
            if(!canOpenDetails(minecraft.player, id)) {
                return;
            }
            // open details
            final RegistryAccess access = minecraft.level.registryAccess();
            minecraft.popGuiLayer();
            openDetails(minecraft, access, id);
        }
    }

    private static class CyclingItemButton extends ImageButton {

        public static final int WIDTH = 16;
        public static final int HEIGHT = 16;

        private final ItemRenderer itemRenderer;
        private final Function<ItemStack, List<Component>> getTooltipFromItem;
        private final List<ItemStack> items;
        private final List<Component> tooltips;
        private int index;

        public CyclingItemButton(int pX, int pY, ItemRenderer itemRenderer, List<ItemStack> items, Function<ItemStack, List<Component>> getTooltipFromItem, OnTooltip onTooltip) {
            super(pX, pY, WIDTH, HEIGHT, 30, 18, 0, AbstractTabScreen.SLOTS, 256, 256, b -> {}, onTooltip, Component.empty());
            this.itemRenderer = itemRenderer;
            this.items = items;
            this.getTooltipFromItem = getTooltipFromItem;
            this.tooltips = new ArrayList<>();
            this.visible = !items.isEmpty();
            this.index = -1;
            tick(0);
        }

        public void tick(long ticksOpen) {
            // validate visible
            if(!this.visible) {
                return;
            }
            // validate items
            if(items.isEmpty()) {
                return;
            }
            // update index
            int indexOld = index;
            index = (int) ((ticksOpen / 20) % items.size());
            // validate index changed
            if(indexOld == index) {
                return;
            }
            // update tooltips
            ItemStack itemStack = items.get(index);
            this.tooltips.clear();
            this.tooltips.addAll(getTooltipFromItem.apply(itemStack));
        }

        public List<Component> getTooltips() {
            return tooltips;
        }

        @Override
        public void renderButton(PoseStack pPoseStack, int pMouseX, int pMouseY, float pPartialTick) {
            super.renderButton(pPoseStack, pMouseX, pMouseY, pPartialTick);
            PoseStack modelViewStack = RenderSystem.getModelViewStack();
            modelViewStack.pushPose();
            modelViewStack.translate(0, 0, 2_000);
            RenderSystem.applyModelViewMatrix();
            this.itemRenderer.renderAndDecorateItem(items.get(index), this.x, this.y);
            modelViewStack.popPose();
            RenderSystem.applyModelViewMatrix();
        }

    }
}
