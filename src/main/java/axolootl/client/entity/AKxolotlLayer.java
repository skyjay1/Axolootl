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
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.LerpingModel;
import net.minecraft.world.entity.LivingEntity;
import software.bernie.geckolib3.core.IAnimatable;
import software.bernie.geckolib3.model.AnimatedGeoModel;
import software.bernie.geckolib3.renderers.geo.GeoLayerRenderer;
import software.bernie.geckolib3.renderers.geo.IGeoRenderer;

import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

public class AKxolotlLayer<T extends LivingEntity & LerpingModel & IAxolootl & IAnimatable> extends GeoLayerRenderer<T> {

    private static final TagKey<AxolootlVariant> BLACKLIST = TagKey.create(AxRegistry.Keys.AXOLOOTL_VARIANTS, new ResourceLocation(Axolootl.MODID, "special_blacklist"));
    private final AnimatedGeoModel<T> model;

    private final Pattern namePattern = Pattern.compile("(?i)ak(?:-| )?xolo(?:o)?tl");

    public AKxolotlLayer(IGeoRenderer<T> parent, AnimatedGeoModel<T> model) {
        super(parent);
        this.model = model;
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource multiBufferSource, int packedLight, T entity,
                       float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        // validate visibility
        final Minecraft minecraft = Minecraft.getInstance();
        if(null == minecraft.level || null == minecraft.player || entity.isInvisibleTo(minecraft.player)) {
            return;
        }
        final RegistryAccess access = minecraft.level.registryAccess();
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
        renderModel(model, model.getTextureResource(entity), poseStack, multiBufferSource, LightTexture.FULL_BRIGHT, entity, partialTick, 1.0F, 1.0F, 1.0F);
    }

    @Override
    public RenderType getRenderType(ResourceLocation textureLocation) {
        return RenderType.entityCutoutNoCull(textureLocation);
    }
}
