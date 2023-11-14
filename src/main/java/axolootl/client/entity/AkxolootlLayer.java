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
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.LerpingModel;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Vector3f;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;
import software.bernie.geckolib.util.RenderUtils;

import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

public class AkxolootlLayer<T extends LivingEntity & LerpingModel & IAxolootl & GeoAnimatable> extends GeoRenderLayer<T> {

    private static final TagKey<AxolootlVariant> BLACKLIST = TagKey.create(AxRegistry.Keys.AXOLOOTL_VARIANTS, new ResourceLocation(Axolootl.MODID, "special_blacklist"));
    private final GeoModel<T> model;

    private final Pattern namePattern = Pattern.compile("(?i)ak( -)?xolotl");

    public AkxolootlLayer(GeoRenderer<T> parent, GeoModel<T> model) {
        super(parent);
        this.model = model;
    }

    @Override
    public void render(PoseStack poseStack, T entity, BakedGeoModel bakedModel, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {
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
        // validate bone exists
        final Optional<GeoBone> bone = bakedModel.getBone("body");
        if(bone.isEmpty()) {
            return;
        }
        // render model
        final RenderType layerRenderType = RenderType.entityCutoutNoCull(getTextureResource(entity));
        poseStack.pushPose();
        RenderUtils.translateAndRotateMatrixForBone(poseStack, bone.get());
        poseStack.translate(0, -5.0D / 16.0D, -4.0D / 16.0D);
        getRenderer().reRender(getDefaultBakedModel(entity), poseStack, bufferSource, entity, layerRenderType, bufferSource.getBuffer(layerRenderType), partialTick, packedLight, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
        poseStack.popPose();
    }

    @Override
    public BakedGeoModel getDefaultBakedModel(T animatable) {
        return this.model.getBakedModel(this.model.getModelResource(animatable));
    }

    @Override
    protected ResourceLocation getTextureResource(T animatable) {
        return this.model.getTextureResource(animatable);
    }
}
