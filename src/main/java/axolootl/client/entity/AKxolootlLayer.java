/**
 * Copyright (c) 2023 Skyler James
 * Permission is granted to use, modify, and redistribute this software, in parts or in whole,
 * under the GNU LGPLv3 license (https://www.gnu.org/licenses/lgpl-3.0.en.html)
 **/

package axolootl.client.entity;

import axolootl.AxRegistry;
import axolootl.Axolootl;
import axolootl.data.axolootl_variant.AxolootlVariant;
import axolootl.entity.IAxolootl;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LerpingModel;
import net.minecraft.world.entity.LivingEntity;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

public class AKxolootlLayer<T extends LivingEntity & LerpingModel & IAxolootl & GeoAnimatable> extends GeoRenderLayer<T> {

    private static final TagKey<AxolootlVariant> BLACKLIST = TagKey.create(AxRegistry.Keys.AXOLOOTL_VARIANTS, new ResourceLocation(Axolootl.MODID, "special_blacklist"));
    private final GeoRenderer<T> renderer;

    private final Pattern namePattern = Pattern.compile("(?i)ak(?:-| )?xolo(?:o)?tl");

    public AKxolootlLayer(GeoRenderer<T> parent, GeoRenderer<T> renderer) {
        super(parent);
        this.renderer = renderer;
    }

    @Override
    public void render(PoseStack poseStack, T entity, BakedGeoModel bakedModel, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {
        // nothing
    }

    @Override
    public void renderForBone(PoseStack poseStack, T entity, GeoBone bone, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {
        // validate bone
        if(!"body".equals(bone.getName())) {
            return;
        }
        // validate visibility
        final Minecraft minecraft = Minecraft.getInstance();
        if(null == minecraft.player || entity.isInvisibleTo(minecraft.player)) {
            return;
        }
        final RegistryAccess access = entity.level.registryAccess();
        // validate name
        if(!namePattern.matcher(entity.getName().getString().toLowerCase(Locale.ENGLISH)).matches()) {
            return;
        }
        // validate variant
        final Optional<AxolootlVariant> oVariant = entity.getAxolootlVariant(access);
        if(oVariant.isEmpty()) {
            return;
        }
        // validate not on blacklist
        if(oVariant.get().is(access, BLACKLIST)) {
            return;
        }
        // render model
        final RenderType layerRenderType = RenderType.entityCutoutNoCull(getTextureResource(entity));
        poseStack.pushPose();
        //RenderUtils.translateAndRotateMatrixForBone(poseStack, bone.get());
        poseStack.translate(-0.5D, -0.5D, -0.5D);
        final float yaw = Mth.lerp(partialTick, entity.yRotO, entity.getYRot());
        this.renderer.defaultRender(poseStack, entity, bufferSource, layerRenderType, bufferSource.getBuffer(layerRenderType), yaw, partialTick, packedLight);
        poseStack.popPose();
        // reset buffer source
        bufferSource.getBuffer(renderType);
    }

    @Override
    public BakedGeoModel getDefaultBakedModel(T animatable) {
        return this.renderer.getGeoModel().getBakedModel(this.renderer.getGeoModel().getModelResource(animatable));
    }

    @Override
    protected ResourceLocation getTextureResource(T animatable) {
        return this.renderer.getGeoModel().getTextureResource(animatable);
    }
}
