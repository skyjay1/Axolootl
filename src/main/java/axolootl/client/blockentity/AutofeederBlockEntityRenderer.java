/**
 * Copyright (c) 2023 Skyler James
 * Permission is granted to use, modify, and redistribute this software, in parts or in whole,
 * under the GNU LGPLv3 license (https://www.gnu.org/licenses/lgpl-3.0.en.html)
 **/

package axolootl.client.blockentity;

import axolootl.block.entity.AutoFeederBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Vector3f;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;

public class AutofeederBlockEntityRenderer implements BlockEntityRenderer<AutoFeederBlockEntity> {

    private final ItemRenderer itemRenderer;
    private final RandomSource random;

    public AutofeederBlockEntityRenderer(final BlockEntityRendererProvider.Context context) {
        this.itemRenderer = context.getItemRenderer();
        this.random = RandomSource.create();
    }

    @Override
    public void render(AutoFeederBlockEntity pBlockEntity, float pPartialTick, PoseStack pPoseStack, MultiBufferSource pBufferSource, int pPackedLight, int pPackedOverlay) {
        // locate first non-empty item stack in container
        ItemStack itemStack = ItemStack.EMPTY;
        for(int i = 0, n = pBlockEntity.getContainerSize(); i < n; i++) {
            ItemStack stack = pBlockEntity.getItem(i);
            if(!stack.isEmpty()) {
                itemStack = stack;
                break;
            }
        }
        // verify item stack is not empty
        if(itemStack.isEmpty()) {
            return;
        }
        // prepare to render
        pPoseStack.pushPose();
        pPoseStack.translate(0.5D, 0.96D, 0.375D);
        pPoseStack.mulPose(Vector3f.XP.rotationDegrees(90));

        // render item stack
        random.setSeed(pBlockEntity.getBlockPos().asLong());
        renderItemStack(itemStack, pPoseStack, pBufferSource, pPackedLight, pPartialTick, random);

        // finish render
        pPoseStack.popPose();
    }

    protected void renderItemStack(ItemStack itemStack, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, float partialTick, RandomSource random) {
        final BakedModel bakedmodel = this.itemRenderer.getModel(itemStack, null, null, 0);
        boolean isGui3d = bakedmodel.isGui3d();
        for(int i = 0, n = getRenderAmount(itemStack); i < n; ++i) {
            poseStack.pushPose();
            if (i > 0) {
                if (isGui3d) {
                    float dx = (random.nextFloat() * 2.0F - 1.0F) * 0.15F;
                    float dy = (random.nextFloat() * 2.0F - 1.0F) * 0.15F;
                    float dz = (random.nextFloat() * 2.0F - 1.0F) * 0.15F;
                    poseStack.translate(dx, dy, dz);
                } else {
                    float dx = (random.nextFloat() * 2.0F - 1.0F) * 0.15F * 0.5F;
                    float dy = (random.nextFloat() * 2.0F - 1.0F) * 0.15F * 0.5F;
                    poseStack.translate(dx, dy, 0.0D);
                }
            }

            this.itemRenderer.render(itemStack, ItemTransforms.TransformType.GROUND, false, poseStack, bufferSource, packedLight, OverlayTexture.NO_OVERLAY, bakedmodel);
            poseStack.popPose();
            if (!isGui3d) {
                poseStack.translate(0.0, 0.0, 0.09375F);
            }
        }
    }

    protected int getRenderAmount(ItemStack pStack) {
        return Math.min(pStack.getCount(), (int) Math.ceil(5.0F * (float) pStack.getCount() / (float) pStack.getMaxStackSize()));
    }

}
