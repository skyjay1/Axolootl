/**
 * Copyright (c) 2023 Skyler James
 * Permission is granted to use, modify, and redistribute this software, in parts or in whole,
 * under the GNU LGPLv3 license (https://www.gnu.org/licenses/lgpl-3.0.en.html)
 **/

package axolootl.client.entity;

import axolootl.data.axolootl_variant.AxolootlModelSettings;
import axolootl.data.axolootl_variant.AxolootlVariant;
import axolootl.entity.IAxolootl;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LerpingModel;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Vector3f;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

public class PrimaryLayer<T extends LivingEntity & LerpingModel & IAxolootl & GeoAnimatable> extends GeoRenderLayer<T> {

    public PrimaryLayer(GeoRenderer<T> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack poseStack, T entity, BakedGeoModel bakedModel, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {
        // load model settings
        final AxolootlModelSettings settings = entity.getAxolootlVariant(entity.level.registryAccess()).orElse(AxolootlVariant.EMPTY).getModelSettings();
        // validate layer
        if(settings.getOptionalEntityPrimaryTexture().isEmpty()) {
            return;
        }
        // load colors
        final Vector3f colors = settings.getPrimaryColors();
        getRenderer().reRender(bakedModel, poseStack, bufferSource, entity, renderType, buffer, partialTick, packedLight, packedOverlay, colors.x(), colors.y(), colors.z(), 1.0F);
    }

    @Override
    protected ResourceLocation getTextureResource(T entity) {
        final AxolootlModelSettings settings = entity.getAxolootlVariant(entity.level.registryAccess()).orElse(AxolootlVariant.EMPTY).getModelSettings();
        return settings.getOptionalEntityPrimaryTexture().orElse(AxolootlModelSettings.ENTITY_TEXTURE);
    }
}
