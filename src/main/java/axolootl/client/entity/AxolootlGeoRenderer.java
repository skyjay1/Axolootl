/**
 * Copyright (c) 2023 Skyler James
 * Permission is granted to use, modify, and redistribute this software, in parts or in whole,
 * under the GNU LGPLv3 license (https://www.gnu.org/licenses/lgpl-3.0.en.html)
 **/

package axolootl.client.entity;

import axolootl.entity.IAxolootl;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.entity.LerpingModel;
import net.minecraft.world.entity.LivingEntity;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.GeoObjectRenderer;

public class AxolootlGeoRenderer<T extends LivingEntity & LerpingModel & IAxolootl & GeoAnimatable> extends GeoEntityRenderer<T> {

    public AxolootlGeoRenderer(EntityRendererProvider.Context context) {
        super(context, new AxolootlGeoModel<>());
        this.addRenderLayer(new PrimaryLayer<>(this));
        this.addRenderLayer(new SecondaryLayer<>(this));
        this.addRenderLayer(new FermiLayer<>(this));
        this.addRenderLayer(new AKxolootlLayer<>(this, new GeoObjectRenderer<>(new AKxolootlGeoModel<>())));
    }

    @Override
    public void preRender(PoseStack poseStack, T animatable, BakedGeoModel model, MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        // TODO remove the call to withScale after 1.19.3 (the bug in GeckoLib is not fixed until later versions)
        this.withScale(animatable.getScale());
        super.preRender(poseStack, animatable, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, red, green, blue, alpha);
    }

    @Override
    public void scaleModelForRender(float widthScale, float heightScale, PoseStack poseStack, T animatable, BakedGeoModel model, boolean isReRender, float partialTick, int packedLight, int packedOverlay) {
        final float scale = animatable.getScale();
        // call the super method
        super.scaleModelForRender(widthScale * scale, heightScale * scale, poseStack, animatable, model, isReRender, partialTick, packedLight, packedOverlay);
    }
}
