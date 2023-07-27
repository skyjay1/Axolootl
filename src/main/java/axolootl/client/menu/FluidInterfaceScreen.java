/**
 * Copyright (c) 2023 Skyler James
 * Permission is granted to use, modify, and redistribute this software, in parts or in whole,
 * under the GNU LGPLv3 license (https://www.gnu.org/licenses/lgpl-3.0.en.html)
 **/

package axolootl.client.menu;

import axolootl.block.AbstractInterfaceBlock;
import axolootl.menu.CyclingContainerMenu;
import axolootl.util.TankMultiblock;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.EmptyFluidHandler;

public class FluidInterfaceScreen extends CyclingContainerScreen {

    // WIDGET CONSTANTS //
    private static final int FLUID_X = 29;
    private static final int FLUID_Y = 17;
    private static final int FLUID_WIDTH = 18;
    private static final int FLUID_HEIGHT = 72;
    private static final int FLUID_U = 55;
    private static final int FLUID_V = 50;
    private static final int FLUID_MARGIN = 2;

    private static final int TEXT_X = 63;
    private static final int TEXT_Y = 17;
    private static final int TEXT_LINE_SPACING = 8;
    private int textDeltaY;

    // DATA //
    private IFluidHandler storage;
    private FluidStack fluidStack;
    private long volume;
    private float totalFluidPercent;
    private int totalFluidHeight;
    private int totalCapacity;
    private int textureWidth;

    // COMPONENTS //
    public static final String PREFIX = "gui.controller_tab.axolootl.fluid_interface.";
    private Component volumetext;
    private Component capacityText;
    private Component storageText;
    private Component statusText;
    private Component statusDescriptionText;

    public FluidInterfaceScreen(CyclingContainerMenu pMenu, Inventory pPlayerInventory, Component pTitle) {
        super(pMenu, pPlayerInventory, pTitle);
        this.textureWidth = Math.min(9, getMenu().getContainerSize()) * 18;
        final BlockEntity blockEntity = pPlayerInventory.player.level.getBlockEntity(getMenu().getBlockPos());
        if(blockEntity != null) {
            this.storage = blockEntity.getCapability(ForgeCapabilities.FLUID_HANDLER).orElse(EmptyFluidHandler.INSTANCE);
        } else {
            this.storage = EmptyFluidHandler.INSTANCE;
        }
        this.fluidStack = this.storage.getFluidInTank(0);
        if(getMenu().hasTank()) {
            volume = getMenu().getController().get().getSize().orElse(TankMultiblock.Size.EMPTY).getInnerVolume();
        }
        this.volumetext = Component.translatable(PREFIX + "volume", volume);
    }

    @Override
    protected void renderContainerSlots(PoseStack pPoseStack, float pPartialTick, int pMouseX, int pMouseY) {
        RenderSystem.setShaderTexture(0, SLOTS);
        blit(pPoseStack, this.leftPos + CyclingContainerMenu.INV_X, this.topPos + CyclingContainerMenu.INV_Y + (5 * 18), CyclingContainerMenu.INV_X - 1, CyclingContainerMenu.INV_Y - 1, textureWidth, textureHeight);
    }

    @Override
    protected void init() {
        super.init();
        this.textDeltaY = font.lineHeight + TEXT_LINE_SPACING;
        this.containerTick();
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        // update values
        this.fluidStack = this.storage.getFluidInTank(0);
        this.totalCapacity = this.storage.getTankCapacity(0);
        this.totalFluidPercent = (float) this.fluidStack.getAmount() / (float) Math.max(1, this.totalCapacity);
        this.totalFluidHeight = Mth.clamp(Mth.floor(totalFluidPercent * FLUID_HEIGHT), 1, FLUID_HEIGHT - FLUID_MARGIN * 2);
        // create components
        this.capacityText = Component.translatable(PREFIX + "capacity", totalCapacity);
        this.storageText = Component.translatable(PREFIX + "storage", fluidStack.getAmount(), totalCapacity);
        final String status = getStatus();
        this.statusDescriptionText = Component.translatable(PREFIX + "status." + status + ".description");
        this.statusText = Component.translatable(PREFIX + "status", Component.translatable(PREFIX + "status." + status))
                .withStyle(a -> a.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, statusDescriptionText)));
    }

    @Override
    protected void renderBgTexture(PoseStack poseStack, float partialTick, int mouseX, int mouseY) {
        super.renderBgTexture(poseStack, partialTick, mouseX, mouseY);
        // render indicator
        RenderSystem.setShaderTexture(0, WIDGETS);
        blit(poseStack, this.leftPos + 29, this.topPos + 93, 226, 0, 18, 10);
    }

    @Override
    public void render(PoseStack pPoseStack, int pMouseX, int pMouseY, float pPartialTick) {
        super.render(pPoseStack, pMouseX, pMouseY, pPartialTick);
        renderFluidBar(pPoseStack, pMouseX, pMouseY, pPartialTick);
        renderDetails(pPoseStack, pMouseX, pMouseY, pPartialTick);
        renderHoverActions(pPoseStack, pMouseX, pMouseY, pPartialTick);
    }

    private String getStatus() {
        // check for empty
        if(this.fluidStack.isEmpty() || this.fluidStack.getAmount() < FluidType.BUCKET_VOLUME) {
            return "empty";
        }
        // check for paused
        final BlockState blockState = getMenu().getInventory().player.level.getBlockState(getMenu().getBlockPos());
        if(blockState.hasProperty(AbstractInterfaceBlock.POWERED) && blockState.getValue(AbstractInterfaceBlock.POWERED)) {
            return "paused";
        }
        // fallback
        return "active";
    }

    private void renderDetails(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        int x = this.leftPos + TEXT_X;
        int y = this.topPos + TEXT_Y;
        this.font.draw(poseStack, volumetext, x, y, 0);
        y += textDeltaY;
        this.font.draw(poseStack, storageText, x, y, 0);
        y += textDeltaY;
        this.font.draw(poseStack, statusText, x, y, 0);
    }

    private void renderFluidBar(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        int x = this.leftPos + FLUID_X;
        int y = this.topPos + FLUID_Y;
        // draw overlaid energy bars
        RenderSystem.setShaderTexture(0, WIDGETS);
        // draw background energy bar
        blit(poseStack, x, y, FLUID_U, FLUID_V, FLUID_WIDTH, FLUID_HEIGHT);
        // draw fluid stack
        renderFluid(poseStack, leftPos + FLUID_X + FLUID_MARGIN, topPos + FLUID_Y + (FLUID_HEIGHT - totalFluidHeight) - FLUID_MARGIN, FLUID_WIDTH - FLUID_MARGIN * 2, totalFluidHeight, fluidStack);
    }


    private void renderFluid(PoseStack poseStack, final int x, final int y, final int width, final int height, final FluidStack fluidStack) {
        // determine fluid
        final Fluid fluid = fluidStack.getFluid();
        if (fluid.isSame(Fluids.EMPTY) || fluidStack.getAmount() <= 0) {
            return;
        }
        // load client fluid extensions
        final IClientFluidTypeExtensions fluidExtensions = IClientFluidTypeExtensions.of(fluid);
        // load texture, atlas, and color
        final ResourceLocation stillTexture = fluidExtensions.getStillTexture(fluidStack);
        final TextureAtlasSprite stillTextureSprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(stillTexture);
        final int textureWidth = stillTextureSprite.getWidth();
        final int textureHeight = stillTextureSprite.getHeight();
        final int fluidColor = fluidExtensions.getTintColor(fluidStack);
        final float red = FastColor.ARGB32.red(fluidColor) / 255.0F;
        final float green = FastColor.ARGB32.green(fluidColor) / 255.0F;
        final float blue = FastColor.ARGB32.blue(fluidColor) / 255.0F;
        final float alpha = FastColor.ARGB32.alpha(fluidColor) / 255.0F;

        // prepare to render fluid
        enableScissor(x, y, x + width, y + height);
        RenderSystem.enableBlend();
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, InventoryMenu.BLOCK_ATLAS);
        RenderSystem.setShaderColor(red, green, blue, alpha);

        // iterate renderable area to render tiled texture
        for(int i = 0, xCount = Mth.ceil((float) width / textureWidth); i < xCount; i++) {
            for(int j = 0, yCount = Mth.ceil((float) height / textureHeight); j < yCount; j++) {
                blit(poseStack, x + i * textureWidth, y + j * textureHeight, getBlitOffset() + 100, textureWidth, textureHeight, stillTextureSprite);
            }
        }

        // reset render settings
        RenderSystem.setShaderColor(1, 1, 1, 1);
        RenderSystem.disableBlend();
        RenderSystem.disableScissor();
    }

    private void renderHoverActions(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        int x = TEXT_X;
        int y = TEXT_Y + textDeltaY * 2;
        // render detail hover actions
        if(isHovering(x, y, font.width(statusText), font.lineHeight, mouseX, mouseY)) {
            renderComponentHoverEffect(poseStack, statusText.getStyle(), mouseX, mouseY);
        }
        // render energy bar tooltips
        x = FLUID_X;
        y = FLUID_Y;
        if(isHovering(x, y, FLUID_WIDTH, FLUID_HEIGHT - totalFluidHeight - 1, mouseX, mouseY)) {
            renderTooltip(poseStack, capacityText, mouseX, mouseY);
        }
        y += FLUID_HEIGHT - totalFluidHeight + 1;
        if(isHovering(x, y, FLUID_WIDTH, totalFluidHeight, mouseX, mouseY)) {
            renderTooltip(poseStack, storageText, mouseX, mouseY);
        }
    }

}
