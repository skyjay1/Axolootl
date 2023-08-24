/**
 * Copyright (c) 2023 Skyler James
 * Permission is granted to use, modify, and redistribute this software, in parts or in whole,
 * under the GNU LGPLv3 license (https://www.gnu.org/licenses/lgpl-3.0.en.html)
 **/

package axolootl.integration;

import axolootl.AxRegistry;
import axolootl.Axolootl;
import com.mojang.blaze3d.vertex.PoseStack;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Map;

public class JeiBreedingCategory implements IRecipeCategory<JeiBreedingRecipe> {

    protected static final ResourceLocation TEXTURE = new ResourceLocation(Axolootl.MODID, "textures/gui/jei/breeding.png");

    protected static final int ITEM_WIDTH = 30;
    protected static final int ITEM_HEIGHT = 16;
    protected static final int INPUTS_WIDTH = 103;
    protected static final int INPUTS_HEIGHT = 45;
    protected static final int OUTPUT_ITEM_HEIGHT = ITEM_HEIGHT + 10;
    protected static final int ITEM_COUNT_PER_ROW = 5;

    protected static final int TEXTURE_WIDTH = ITEM_WIDTH * ITEM_COUNT_PER_ROW;
    protected static final int TEXTURE_HEIGHT = INPUTS_HEIGHT + OUTPUT_ITEM_HEIGHT * 3;

    protected static final int INPUTS_X = (TEXTURE_WIDTH - INPUTS_WIDTH) / 2;
    protected static final int SECOND_INPUTS_X = INPUTS_X + 67;
    protected static final int INPUTS_Y = 1;
    protected static final int OUTPUT_X = 0;
    protected static final int OUTPUT_Y = 45;

    protected final IDrawable background;
    protected final IDrawable inputs;
    protected final IDrawable icon;
    protected final Component title;

    protected final Component monsteriumTooltip;
    protected final Component foodTooltip;
    protected final ItemStack monsteriumItem;

    public JeiBreedingCategory(IGuiHelper guiHelper) {
        this.background = guiHelper.createDrawable(TEXTURE, 0, INPUTS_HEIGHT, TEXTURE_WIDTH, TEXTURE_HEIGHT);
        this.inputs = guiHelper.createDrawable(TEXTURE, 0, 0, INPUTS_WIDTH, INPUTS_HEIGHT);
        this.icon = guiHelper.createDrawableItemStack(Items.AXOLOTL_BUCKET.getDefaultInstance());
        this.title = Component.translatable("jei.recipe_category.breeding");
        this.foodTooltip = Component.translatable("jei.recipe_category.breeding.food");
        final Component monsterium = Component.translatable(AxRegistry.BlockReg.MONSTERIUM.get().getDescriptionId());
        this.monsteriumTooltip = Component.translatable("jei.recipe_category.breeding.monsterium", monsterium);
        this.monsteriumItem = new ItemStack(AxRegistry.BlockReg.MONSTERIUM.get());
    }

    @Override
    public RecipeType<JeiBreedingRecipe> getRecipeType() {
        return JeiAddon.BREEDING_TYPE;
    }

    @Override
    public Component getTitle() {
        return title;
    }

    @Override
    public IDrawable getBackground() {
        return this.background;
    }

    @Override
    public IDrawable getIcon() {
        return this.icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, JeiBreedingRecipe recipe, IFocusGroup focuses) {
        // add input slots
        builder.addSlot(RecipeIngredientRole.INPUT, INPUTS_X, INPUTS_Y)
                .addItemStacks(recipe.getFirst());
        builder.addSlot(RecipeIngredientRole.INPUT, INPUTS_X + 18, INPUTS_Y)
                .addItemStacks(recipe.getFirstFood())
                .addTooltipCallback((recipeSlotView, tooltip) -> tooltip.add(0, foodTooltip));
        builder.addSlot(RecipeIngredientRole.INPUT, SECOND_INPUTS_X, INPUTS_Y)
                .addItemStacks(recipe.getSecond());
        builder.addSlot(RecipeIngredientRole.INPUT, SECOND_INPUTS_X + 18, INPUTS_Y)
                .addItemStacks(recipe.getSecondFood())
                .addTooltipCallback((recipeSlotView, tooltip) -> tooltip.add(0, foodTooltip));
        // prepare to add output slots
        final Minecraft minecraft = Minecraft.getInstance();
        final Font font = minecraft.font;
        int resultCount = 0;
        // add output slots
        final int outputOffsetX = (ITEM_WIDTH * ITEM_COUNT_PER_ROW) / 2 - getOutputOffsetX(recipe) - 2;
        for(Map.Entry<ItemStack, Double> entry : recipe.getSortedResult()) {
            builder.addSlot(RecipeIngredientRole.OUTPUT,
                            OUTPUT_X + (resultCount % ITEM_COUNT_PER_ROW) * ITEM_WIDTH + 16 / 2 + outputOffsetX,
                            OUTPUT_Y + (resultCount / ITEM_COUNT_PER_ROW) * OUTPUT_ITEM_HEIGHT)
                    .addItemStack(entry.getKey());
            resultCount++;
        }
        // add monsterium slot
        if(recipe.requiresMonsterium()) {
            builder.addSlot(RecipeIngredientRole.CATALYST, INPUTS_X - 20, INPUTS_Y)
                    .addItemStack(monsteriumItem)
                    .addTooltipCallback(((recipeSlotView, tooltip) -> {
                        tooltip.clear();
                        tooltip.add(monsteriumTooltip);
                    }));
        }
    }

    @Override
    public void draw(JeiBreedingRecipe recipe, IRecipeSlotsView recipeSlotsView, PoseStack stack, double mouseX, double mouseY) {
        // draw inputs
        this.inputs.draw(stack, INPUTS_X - 1, INPUTS_Y - 1);
        // prepare to draw percentages
        Minecraft minecraft = Minecraft.getInstance();
        Font font = minecraft.font;
        // draw percentages
        final int outputOffsetX = (ITEM_WIDTH * ITEM_COUNT_PER_ROW) / 2 - getOutputOffsetX(recipe);
        for(int i = 0, n = recipeSlotsView.getSlotViews(RecipeIngredientRole.OUTPUT).size(); i < n; i++) {
            // validate entry
            if(recipe.getSortedResult().size() < i) break;
            // determine chance
            double chance = recipe.getSortedResult().get(i).getValue();
            // draw overlay
            PercentChanceOverlay overlay = new PercentChanceOverlay(font, chance);
            overlay.draw(stack,
                    OUTPUT_X + (i % ITEM_COUNT_PER_ROW) * ITEM_WIDTH + ITEM_WIDTH / 2 + outputOffsetX,
                    OUTPUT_Y + (i / ITEM_COUNT_PER_ROW) * OUTPUT_ITEM_HEIGHT + ITEM_HEIGHT + 2);
        }
    }

    public int getOutputOffsetX(JeiBreedingRecipe recipe) {
        return Math.min(5, recipe.getResult().size()) * ITEM_WIDTH / 2;
    }

    /**
     * Draws horizontally centered text at the given position with the given percentage value
     */
    protected static class PercentChanceOverlay implements IDrawable {

        private final Font font;
        private final double value;
        private final Component text;
        private final int width;

        public PercentChanceOverlay(final Font font, final double value) {
            this.font = font;
            this.value = value;
            String formatted = "%.1f".formatted(value * 100).replaceAll("0*$", "").replaceAll("\\.$", "");
            this.text = Component.literal(formatted + "%").withStyle(ChatFormatting.DARK_GRAY);
            this.width = this.font.width(this.text);
        }

        @Override
        public int getWidth() {
            return this.font.width(text);
        }

        @Override
        public int getHeight() {
            return this.font.lineHeight;
        }

        @Override
        public void draw(PoseStack poseStack, int xOffset, int yOffset) {
            this.font.draw(poseStack, this.text, xOffset - width / 2.0F, yOffset, 0);
        }
    }

}
