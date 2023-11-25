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
import com.mojang.math.Vector3f;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LerpingModel;
import net.minecraft.world.entity.LivingEntity;
import software.bernie.geckolib3.core.IAnimatable;
import software.bernie.geckolib3.renderers.geo.GeoLayerRenderer;
import software.bernie.geckolib3.renderers.geo.IGeoRenderer;

import java.util.Optional;

public class PrimaryLayer<T extends LivingEntity & LerpingModel & IAxolootl & IAnimatable> extends GeoLayerRenderer<T> {


    public PrimaryLayer(IGeoRenderer<T> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource multiBufferSource, int packedLight, T entity,
                       float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        // validate visibility
        final Minecraft minecraft = Minecraft.getInstance();
        if(null == minecraft.level || null == minecraft.player || entity.isInvisibleTo(minecraft.player)) {
            return;
        }
        // load model settings
        final AxolootlModelSettings settings = entity.getAxolootlVariant(minecraft.level.registryAccess()).orElse(AxolootlVariant.EMPTY).getModelSettings();
        final Optional<ResourceLocation> oPrimary = settings.getOptionalEntityPrimaryTexture();
        // validate layer
        if(oPrimary.isEmpty()) {
            return;
        }
        // load colors
        final Vector3f colors = settings.getPrimaryColors();
        renderModel(getEntityModel(), oPrimary.get(), poseStack, multiBufferSource, packedLight, entity, partialTick, colors.x(), colors.y(), colors.z());
    }

    @Override
    public RenderType getRenderType(ResourceLocation textureLocation) {
        return RenderType.entityCutoutNoCull(textureLocation);
    }
}
