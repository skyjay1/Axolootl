/**
 * Copyright (c) 2023 Skyler James
 * Permission is granted to use, modify, and redistribute this software, in parts or in whole,
 * under the GNU LGPLv3 license (https://www.gnu.org/licenses/lgpl-3.0.en.html)
 **/

package axolootl.client.menu;

import axolootl.AxRegistry;
import axolootl.Axolootl;
import axolootl.capability.AxolootlResearchCapability;
import axolootl.client.menu.widget.ComponentButton;
import axolootl.client.menu.widget.ItemButton;
import axolootl.client.menu.widget.ScrollButton;
import axolootl.data.axolootl_variant.AxolootlVariant;
import axolootl.data.axolootl_variant.Bonuses;
import axolootl.data.axolootl_variant.BonusesProvider;
import axolootl.data.breeding.AxolootlBreeding;
import axolootl.data.resource_generator.ResourceDescription;
import axolootl.data.resource_generator.ResourceDescriptionGroup;
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
import net.minecraft.util.random.WeightedEntry;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

public class AxolootlDetailsScreen extends Screen implements ScrollButton.IScrollListener {

    // TEXTURES //
    public static final ResourceLocation TEXTURE = new ResourceLocation(Axolootl.MODID, "textures/gui/aquarium/axolootl_details.png");
    public static final ResourceLocation WIDGETS = AbstractTabScreen.WIDGETS;

    // WIDGET CONSTANTS //
    public static final int WIDTH = 250;
    public static final int HEIGHT = 176;
    public static final int DETAILS_X = 8;
    public static final int DETAILS_Y = 8;
    public static final int DETAILS_LINE_SPACING = 4;
    public static final int RESOURCE_DESCRIPTION_X = 128;
    public static final int RESOURCE_DESCRIPTION_Y = 32;
    public static final int RESOURCE_DESCRIPTION_WIDTH = 100;
    public static final int RESOURCE_DESCRIPTION_HEIGHT = 135;
    public static final int RESOURCE_DESCRIPTION_MARGIN_X = 6;
    public static final int RESOURCE_DESCRIPTION_MARGIN_Y = 6;
    public static final int FOOD_MAX_COUNT = 6;
    public static final int BREED_FOOD_MAX_COUNT = 6;

    // WIDGETS //
    private final List<DetailsComponentButton> componentButtons;
    private final List<CyclingItemButton> foodButtons;
    private final List<CyclingItemButton> breedFoodButtons;
    private final List<ResourceDescriptionGroupButton> resourceDescriptionGroupButtons;
    private final List<ResourceDescriptionButton> resourceDescriptionButtons;
    private ScrollButton scrollButton;
    private int scrollOffset;
    private int resourceDescriptionHeight;

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
    private final List<ResourceDescriptionGroup> resourceDescriptionGroups;

    // COMPONENTS //
    public static final String PREFIX = "gui.controller_tab.axolootl.axolootl_details.";
    private final Component resourceTitleText;
    private final Component tierText;
    private final Component foodText;
    private final Component breedFoodText;
    private final Component parentsTitleText;
    private final List<Component> parentsText;

    public AxolootlDetailsScreen(final ResourceLocation id, final AxolootlVariant variant, final ItemStack icon, final RegistryAccess access) {
        super(createAxolootlName(variant, id));
        this.id = id;
        this.variant = variant;
        this.itemStack = icon;
        this.componentButtons = new ArrayList<>();
        this.foodButtons = new ArrayList<>();
        this.breedFoodButtons = new ArrayList<>();
        this.resourceDescriptionGroupButtons = new ArrayList<>();
        this.resourceDescriptionButtons = new ArrayList<>();
        this.resourceDescriptionGroups = new ArrayList<>(variant.getResourceGenerator().value().getDescription());
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
        this.resourceTitleText = Component.translatable(PREFIX + "loot")
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
    }

    private static Component createAxolootlName(final AxolootlVariant variant, final ResourceLocation id) {
        return Component.translatable("entity.axolootl.axolootl.description", Component.translatable("entity.axolootl.axolootl"), variant.getDescription()).withStyle(a -> a.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, variant.getDescription().copy().append("\n").append(Component.literal("" + id).withStyle(ChatFormatting.GRAY)))));
    }

    private static Component createTieredAxolootlName(final AxolootlVariant variant, final ResourceLocation id) {
        return Component.translatable(PREFIX + "parents.entry.axolootl_tier", variant.getDescription(), variant.getTierDescription()).withStyle(a -> a.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, variant.getDescription().copy().append("\n").append(Component.literal("" + id).withStyle(ChatFormatting.GRAY)))));
    }

    /**
     * @param collection the collection to distribute
     * @param maxCount the maximum number of lists to create
     * @return a list of item stack lists where the difference in size between each list is no greater than 1
     */
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
        final int resourceMaxWidth = RESOURCE_DESCRIPTION_WIDTH - RESOURCE_DESCRIPTION_MARGIN_X;
        this.resourceDescriptionHeight = ResourceDescriptionGroupButton.calculateTotalHeight(this.resourceDescriptionGroups, font, resourceMaxWidth);
        // add scroll button
        this.scrollButton = addRenderableWidget(new ScrollButton(leftPos + RESOURCE_DESCRIPTION_X + RESOURCE_DESCRIPTION_WIDTH + 1, topPos + RESOURCE_DESCRIPTION_Y + 2, 12, RESOURCE_DESCRIPTION_HEIGHT - 2, WIDGETS, 244, 0, 12, 15, 15, true,  (float) ResourceDescriptionGroupButton.getHeight(font) / (float) Math.max(1, resourceDescriptionHeight - RESOURCE_DESCRIPTION_HEIGHT), this));
        this.setFocused(this.scrollButton);
        this.scrollButton.active = this.resourceDescriptionHeight > RESOURCE_DESCRIPTION_HEIGHT;
        // create on tooltip
        final Button.OnTooltip componentButtonOnTooltip = (b, p, mx, my) -> renderComponentHoverEffect(p, b.getMessage().getStyle(), mx, my);
        // prepare to add components
        int x = this.leftPos + DETAILS_X;
        int y = this.topPos + DETAILS_Y + (16 - font.lineHeight) / 2;
        int deltaY = DETAILS_LINE_SPACING + font.lineHeight;
        // add tier text
        this.componentButtons.add(addRenderableWidget(new DetailsComponentButton(x + 16 + 2, y, font.lineHeight, font, getTitle(), componentButtonOnTooltip)));
        y += deltaY;
        // add loot title text
        int resourceTitleX = this.leftPos + RESOURCE_DESCRIPTION_X;this.componentButtons.add(addRenderableWidget(new DetailsComponentButton(x + 16 + 2, y, font.lineHeight, font, tierText, componentButtonOnTooltip)));
        this.componentButtons.add(addRenderableWidget(new DetailsComponentButton(resourceTitleX, y, font.lineHeight, font, resourceTitleText, componentButtonOnTooltip)));
        // add energy cost icon, if any
        if(this.variant.getEnergyCost() > 0) {
            final Component energyCostText = Component.translatable(PREFIX + "energy_cost.description", this.variant.getEnergyCost())
                    .withStyle(ChatFormatting.RED);
            addRenderableWidget(new ItemButton(resourceTitleX + font.width(resourceTitleText) + 4, y - (16 - DetailsComponentButton.getHeight(font)), false, font, itemRenderer, new ItemStack(Items.REDSTONE), item -> ImmutableList.of(energyCostText), b -> {}, (b, p, mx, my) -> renderTooltip(p, energyCostText, mx, my)));
        }
        y += deltaY + 4;
        // add food buttons
        this.componentButtons.add(addRenderableWidget(new DetailsComponentButton(x, y, font.lineHeight, font, foodText, componentButtonOnTooltip)));
        final Button.OnTooltip cyclingItemButtonOnTooltip = (b, p, mx, my) -> renderTooltip(p, ((CyclingItemButton)b).getTooltips(), Optional.empty(), mx, my);
        y += deltaY;
        for(int i = 0, n = foods.size(); i < n; i++) {
            this.foodButtons.add(addRenderableWidget(new CyclingItemButton(x + i * (CyclingItemButton.WIDTH + 3), y, font, itemRenderer, foods.get(i), itemStack -> getFoodTooltip(this.variant, itemStack), cyclingItemButtonOnTooltip)));
        }
        y += CyclingItemButton.HEIGHT + DETAILS_LINE_SPACING;
        // add breed buttons
        this.componentButtons.add(addRenderableWidget(new DetailsComponentButton(x, y, font.lineHeight, font, breedFoodText, componentButtonOnTooltip)));
        y += deltaY;
        for(int i = 0, n = breedFoods.size(); i < n; i++) {
            this.breedFoodButtons.add(addRenderableWidget(new CyclingItemButton(x + i * (CyclingItemButton.WIDTH + 3), y, font, itemRenderer, breedFoods.get(i), this::getTooltipFromItem, cyclingItemButtonOnTooltip)));
        }
        y += CyclingItemButton.HEIGHT + deltaY;
        // add parent info
        // TODO change the layout to be (chance%) [item1] [item2] where the parents are represented by item stack buttons
        this.componentButtons.add(addRenderableWidget(new DetailsComponentButton(x, y, font.lineHeight, font, parentsTitleText, componentButtonOnTooltip)));
        y += deltaY;
        for(Component entry : parentsText) {
            this.componentButtons.add(addRenderableWidget(new DetailsComponentButton(x, y, font.lineHeight, font, entry, componentButtonOnTooltip)));
            y += font.lineHeight + 1;
        }
        // add resource description buttons
        x = this.leftPos + RESOURCE_DESCRIPTION_X + RESOURCE_DESCRIPTION_MARGIN_X;
        y = this.topPos + RESOURCE_DESCRIPTION_Y + 1;
        final Button.OnTooltip descriptionButtonOnTooltip = (b, p, mx, my) -> renderTooltip(p, ((ResourceDescriptionButton)b).getTooltips(), Optional.empty(), mx, my);
        for(int i = 0, n = resourceDescriptionGroups.size(), h = ResourceDescriptionGroupButton.getHeight(font); i < n; i++) {
            ResourceDescriptionGroup group = resourceDescriptionGroups.get(i);
            ResourceDescriptionGroupButton button = new ResourceDescriptionGroupButton(x, y, resourceMaxWidth, h, itemRenderer, font, group, this::getTooltipFromItem, componentButtonOnTooltip, descriptionButtonOnTooltip, resourceDescriptionButtons::add);
            this.resourceDescriptionGroupButtons.add(button);
            y += button.getTotalHeight() + RESOURCE_DESCRIPTION_MARGIN_Y;
        }
        updateResourceDescriptionButtons();
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
        // render resource descriptions
        final int scissorStartX = this.leftPos + RESOURCE_DESCRIPTION_X;
        final int scissorStartY = this.topPos + RESOURCE_DESCRIPTION_Y;
        enableScissor(scissorStartX, scissorStartY, scissorStartX + RESOURCE_DESCRIPTION_WIDTH, scissorStartY + RESOURCE_DESCRIPTION_HEIGHT);
        for(ResourceDescriptionGroupButton button : resourceDescriptionGroupButtons) {
            button.render(pPoseStack, pMouseX, pMouseY, pPartialTick);
        }
        for(ResourceDescriptionButton button : resourceDescriptionButtons) {
            button.render(pPoseStack, pMouseX, pMouseY, pPartialTick);
        }
        disableScissor();
        // render tooltips
        renderHoverActions(pPoseStack, pMouseX, pMouseY, pPartialTick);
    }

    private void renderHoverActions(final PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        for(ResourceDescriptionGroupButton button : resourceDescriptionGroupButtons) {
            if(button.visible && button.isHoveredOrFocused()) {
                button.renderToolTip(poseStack, mouseX, mouseY);
            }
        }
        for(ResourceDescriptionButton button : resourceDescriptionButtons) {
            if(button.visible && button.isHoveredOrFocused()) {
                button.renderToolTip(poseStack, mouseX, mouseY);
            }
        }
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

    private void updateResourceDescriptionButtons() {
        final int scissorStartY = this.topPos + RESOURCE_DESCRIPTION_Y;
        final int deltaY = -scrollOffset;
        for(ResourceDescriptionGroupButton button : resourceDescriptionGroupButtons) {
            button.move(0, deltaY);
            button.visible = button.group.showChance() && (button.y + button.getHeight() > scissorStartY || button.y < scissorStartY + RESOURCE_DESCRIPTION_HEIGHT);
        }
        for(ResourceDescriptionButton button : resourceDescriptionButtons) {
            button.move(0, deltaY);
            button.visible = button.y + button.getHeight() > scissorStartY || button.y < scissorStartY + RESOURCE_DESCRIPTION_HEIGHT;
        }
    }

    /**
     * @param player the player
     * @param id the {@link AxolootlVariant} ID
     * @return true if the player has permission to open details for the given {@link AxolootlVariant}
     */
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

    /**
     * Opens the details GUI for the given {@link AxolootlVariant} ID
     * @param minecraft the minecraft instance
     * @param access the registry access
     * @param id the {@link AxolootlVariant} ID
     */
    public static void openDetails(final Minecraft minecraft, final RegistryAccess access, final ResourceLocation id) {
        final AxolootlVariant variant = AxolootlVariant.getRegistry(access).getOptional(id).orElse(AxolootlVariant.EMPTY);
        final ItemStack icon = AxolootlBucketItem.getWithVariant(new ItemStack(AxRegistry.ItemReg.AXOLOOTL_BUCKET.get()), id);
        openDetails(minecraft, access, id, variant, icon);
    }

    /**
     * Opens the details GUI for the given {@link AxolootlVariant} ID
     * @param minecraft the minecraft instance
     * @param access the registry access
     * @param id the {@link AxolootlVariant} ID
     * @param variant the variant instance
     * @param icon the itemstack to display
     */
    public static void openDetails(final Minecraft minecraft, final RegistryAccess access, final ResourceLocation id, final AxolootlVariant variant, final ItemStack icon) {
        minecraft.pushGuiLayer(new AxolootlDetailsScreen(id, variant, icon, access));
    }

    /**
     * @param holderSets a collection of item holder sets
     * @return all items in the holder sets as item stacks
     */
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

    /**
     * @param holderSet an item holder set
     * @return all items in the holder set as item stacks
     */
    private static Collection<ItemStack> resolveHolderSet(final HolderSet<Item> holderSet) {
        // convert to item stacks
        final List<ItemStack> itemStacks = new ArrayList<>();
        for(Item item : resolveHolderSetItems(holderSet)) {
            itemStacks.add(item.getDefaultInstance());
        }
        return itemStacks;
    }

    /**
     * @param holderSet an item holder set
     * @return all unique items in the holder set
     */
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
        if(pMouseX >= (this.leftPos + RESOURCE_DESCRIPTION_X - 1) && pMouseX < (this.leftPos + RESOURCE_DESCRIPTION_X + RESOURCE_DESCRIPTION_WIDTH + 1) && pMouseY >= (this.topPos + RESOURCE_DESCRIPTION_Y - 1) && pMouseY < (this.topPos + RESOURCE_DESCRIPTION_Y + RESOURCE_DESCRIPTION_HEIGHT + 1)) {
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
        this.scrollOffset = Mth.floor(Math.max(0, percent * Math.max(0, resourceDescriptionHeight - RESOURCE_DESCRIPTION_HEIGHT)));
        updateResourceDescriptionButtons();
    }

    //// PARENT DATA ////

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

    private static class DetailsComponentButton extends ComponentButton {

        public DetailsComponentButton(int pX, int pY, int height, Font font, Component pMessage, OnTooltip onTooltip) {
            this(pX, pY, font.width(pMessage), height, font, pMessage, onTooltip);
        }

        public DetailsComponentButton(int pX, int pY, int width, int height, Font font, Component pMessage, OnTooltip onTooltip) {
            super(pX, pY, width, height, font, pMessage, b -> ((DetailsComponentButton)b).onPressComponent(), onTooltip);
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

    private static class ResourceDescriptionGroupButton extends DetailsComponentButton {

        private final ResourceDescriptionGroup group;
        private int totalWidth;
        private int totalHeight;
        private int xo;
        private int yo;

        public ResourceDescriptionGroupButton(int pX, int pY, int width, int height, ItemRenderer itemRenderer, Font font, ResourceDescriptionGroup group,
                                              Function<ItemStack, List<Component>> getTooltipFromItem, OnTooltip onTooltip,
                                              OnTooltip descriptionButtonOnTooltip,
                                              Consumer<ResourceDescriptionButton> onAddDescriptionButton) {
            super(pX, pY, height, font, group.getChanceDescription(), onTooltip);
            this.group = group;
            this.totalWidth = width;
            this.totalHeight = group.showChance() ? height : 0;
            this.xo = pX;
            this.yo = pY;
            this.drawTooltip = false;
            init(onAddDescriptionButton, getTooltipFromItem, descriptionButtonOnTooltip, itemRenderer, font);
        }

        private void init(Consumer<ResourceDescriptionButton> onAddDescriptionButton, Function<ItemStack, List<Component>> getTooltipFromItem, OnTooltip descriptionButtonOnTooltip, ItemRenderer itemRenderer, Font font) {
            // add each description button
            for(int i = 0, n = group.getDescriptions().size(), countX = (this.totalWidth / ResourceDescriptionButton.WIDTH), startY = y + (group.showChance() ? height : 0); i < n; i++) {
                // create the button
                ResourceDescription description = group.getDescriptions().get(i);
                ResourceDescriptionButton button = new ResourceDescriptionButton(x + (i % countX) * ResourceDescriptionButton.WIDTH, startY + (i / countX) * ResourceDescriptionButton.getHeight(font), font, itemRenderer, description, getTooltipFromItem, descriptionButtonOnTooltip);
                // add the button
                onAddDescriptionButton.accept(button);
                if(i % countX == 0) {
                    this.totalHeight += button.getTotalHeight();
                }
            }
        }

        public void move(int deltaX, int deltaY) {
            this.x = this.xo + deltaX;
            this.y = this.yo + deltaY;
        }

        public int getTotalWidth() {
            return totalWidth;
        }

        public int getTotalHeight() {
            return totalHeight;
        }

        public static int calculateTotalHeight(final List<ResourceDescriptionGroup> groups, final Font font, final int maxWidth) {
            int height = 0;
            float countX = (float) (maxWidth / ResourceDescriptionButton.WIDTH);
            // iterate each group
            for(ResourceDescriptionGroup group : groups) {
                // add chance height
                if(group.showChance()) {
                    height += ResourceDescriptionGroupButton.getHeight(font);
                }
                // add description height
                height += Mth.ceil((float) group.getDescriptions().size() / countX) * ResourceDescriptionButton.getHeight(font);
                height += RESOURCE_DESCRIPTION_MARGIN_Y;
            }
            return height;
        }
    }

    private static class ResourceDescriptionButton extends ItemButton {

        public static final int WIDTH = 30;
        private static final List<Component> EMPTY_TOOLTIP = ImmutableList.of(ResourceGenerator.getItemDisplayName(ItemStack.EMPTY));
        private static final ItemStack EMPTY_ITEMSTACK = new ItemStack(Items.BARRIER);

        private final ResourceDescription description;
        private final Font font;
        private int xo;
        private int yo;

        public ResourceDescriptionButton(int pX, int pY, Font font, ItemRenderer itemRenderer, ResourceDescription description, Function<ItemStack, List<Component>> getTooltipFromItem, OnTooltip onTooltip) {
            super(pX, pY, false, font, itemRenderer, description.getItem().isEmpty() ? EMPTY_ITEMSTACK : description.getItem(), getTooltipFromItem, b -> {}, onTooltip);
            this.font = font;
            this.description = description;
            this.xo = pX;
            this.yo = pY;
            this.drawTooltip = false;
        }

        public void move(int deltaX, int deltaY) {
            this.x = this.xo + deltaX;
            this.y = this.yo + deltaY;
        }

        public static int getHeight(final Font font) {
            return HEIGHT + font.lineHeight + 1;
        }

        @Override
        public List<Component> getTooltips() {
            // check for empty
            if(this.description.getItem().isEmpty()) {
                return new ArrayList<>(EMPTY_TOOLTIP);
            }
            if(this.description.getDescriptions().isEmpty()) {
                return getTooltipFromItem.apply(this.description.getItem());
            }
            return this.description.getDescriptions();
        }

        public int getTotalHeight() {
            return this.height + this.font.lineHeight + 1;
        }

        @Override
        public void renderButton(PoseStack pPoseStack, int pMouseX, int pMouseY, float pPartialTick) {
            // render text
            if(description.showChance()) {
                final int textWidth = font.width(this.description.getChanceDescription());
                font.draw(pPoseStack, this.description.getChanceDescription(), this.x + (this.width - textWidth) / 2.0F, this.y + this.height + 1, 0);
            }
            // render button
            super.renderButton(pPoseStack, pMouseX, pMouseY, pPartialTick);
        }

    }

    private static class CyclingItemButton extends ItemButton {

        private final List<ItemStack> items;
        private int index;

        public CyclingItemButton(int pX, int pY, Font font, ItemRenderer itemRenderer, List<ItemStack> items, Function<ItemStack, List<Component>> getTooltipFromItem, OnTooltip onTooltip) {
            super(pX, pY, true, font, itemRenderer, ItemStack.EMPTY, getTooltipFromItem, b -> {}, onTooltip);
            this.items = items;
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
            setItem(items.get(index));
        }
    }
}
