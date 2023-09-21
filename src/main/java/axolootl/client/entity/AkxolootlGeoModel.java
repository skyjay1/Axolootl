/**
 * Copyright (c) 2023 Skyler James
 * Permission is granted to use, modify, and redistribute this software, in parts or in whole,
 * under the GNU LGPLv3 license (https://www.gnu.org/licenses/lgpl-3.0.en.html)
 **/

package axolootl.client.entity;

import axolootl.Axolootl;
import axolootl.data.axolootl_variant.AxolootlVariant;
import axolootl.entity.IAxolootl;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LerpingModel;
import net.minecraft.world.entity.LivingEntity;
import software.bernie.geckolib3.core.IAnimatable;
import software.bernie.geckolib3.core.event.predicate.AnimationEvent;
import software.bernie.geckolib3.model.AnimatedGeoModel;

public class AkxolootlGeoModel<T extends LivingEntity & LerpingModel & IAxolootl & IAnimatable> extends AnimatedGeoModel<T> {

    private static final ResourceLocation EMPTY_ANIMATIONS = new ResourceLocation(Axolootl.MODID, "animations/entity/axolootl/axolootl.animation.json");
    private static final ResourceLocation MODEL = new ResourceLocation(Axolootl.MODID, "geo/entity/axolootl/akxolootl.geo.json");
    private static final ResourceLocation TEXTURE = new ResourceLocation(Axolootl.MODID, "textures/entity/axolootl/akxolootl.png");

    @Override
    public ResourceLocation getModelResource(T entity) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(T entity) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(T entity) {
        return entity.getAxolootlVariant(entity.level.registryAccess()).orElse(AxolootlVariant.EMPTY).getModelSettings().getOptionalEntityGeoAnimations().orElse(EMPTY_ANIMATIONS);
    }

    @Override
    public void setCustomAnimations(T entity, int instanceId, AnimationEvent animationEvent) {
        super.setCustomAnimations(entity, instanceId, animationEvent);
    }
}
