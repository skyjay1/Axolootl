package axolootl.client.item;

import axolootl.client.AxolootlBucketItemModelLoader;
import axolootl.entity.AxolootlEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MobBucketItem;

public class AxolootlBucketItemRenderer extends BlockEntityWithoutLevelRenderer {

    private final ItemRenderer itemRenderer;

    public AxolootlBucketItemRenderer(BlockEntityRenderDispatcher pBlockEntityRenderDispatcher, EntityModelSet pEntityModelSet, final ItemRenderer itemRenderer) {
        super(pBlockEntityRenderDispatcher, pEntityModelSet);
        this.itemRenderer = itemRenderer;
    }

    @Override
    public void onResourceManagerReload(ResourceManager pResourceManager) {
        super.onResourceManagerReload(pResourceManager);
    }

    @Override
    public void renderByItem(ItemStack pStack, ItemTransforms.TransformType pTransformType, PoseStack pPoseStack, MultiBufferSource pBuffer, int pPackedLight, int pPackedOverlay) {
        if(!renderItemStack(pStack, pTransformType, pPoseStack, pBuffer, pPackedLight, pPackedOverlay)) {
            super.renderByItem(pStack, pTransformType, pPoseStack, pBuffer, pPackedLight, pPackedOverlay);
        }
    }

    public boolean renderItemStack(ItemStack pStack, ItemTransforms.TransformType pTransformType, PoseStack pPoseStack, MultiBufferSource pBuffer, int pPackedLight, int pPackedOverlay) {
        // validate item stack
        if(pStack.isEmpty() || !pStack.hasTag() || !pStack.getTag().contains(AxolootlEntity.KEY_VARIANT_ID, Tag.TAG_STRING) || !(pStack.getItem() instanceof MobBucketItem)) {
            return false;
        }
        // load variant
        final ResourceLocation variantId = new ResourceLocation(pStack.getTag().getString(AxolootlEntity.KEY_VARIANT_ID));
        // load model
        final ResourceLocation modelId = AxolootlBucketItemModelLoader.getModelForVariant(variantId);
        final BakedModel model = Minecraft.getInstance().getModelManager().getModel(modelId);
        // prepare item model
        final VertexConsumer vertexconsumer = ItemRenderer.getFoilBuffer(pBuffer, RenderType.cutout(), true, pStack.hasFoil());
        // render item model
        itemRenderer.renderModelLists(model, pStack, pPackedLight, pPackedOverlay, pPoseStack, vertexconsumer);
        // all checks passed
        return true;
    }
}
