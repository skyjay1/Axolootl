package axolootl.client.item;

import axolootl.Axolootl;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import software.bernie.geckolib3.core.IAnimatable;
import software.bernie.geckolib3.model.AnimatedGeoModel;

public class AxolootlBucketItemModel<T extends Item & IAnimatable> extends AnimatedGeoModel<T> {

    public static final ResourceLocation ITEM_MODEL = new ResourceLocation(Axolootl.MODID, "geo/item/axolootl_bucket.geo.json");

    public static final ResourceLocation TEXTURE_BASE = new ResourceLocation(Axolootl.MODID, "textures/item/axolootl_bucket_base.png");
    public static final ResourceLocation TEXTURE_PRIMARY = new ResourceLocation(Axolootl.MODID, "textures/item/axolootl_bucket_layer_1.png");
    public static final ResourceLocation TEXTURE_SECONDARY = new ResourceLocation(Axolootl.MODID, "textures/item/axolootl_bucket_layer_2.png");

    public static final ResourceLocation ITEM_ANIMATIONS = new ResourceLocation(Axolootl.MODID, "animations/item/axolootl_bucket.animation.json");

    @Override
    public ResourceLocation getModelResource(T object) {
        return ITEM_MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(T object) {
        return TEXTURE_BASE;
    }

    @Override
    public ResourceLocation getAnimationResource(T animatable) {
        return ITEM_ANIMATIONS;
    }


}
