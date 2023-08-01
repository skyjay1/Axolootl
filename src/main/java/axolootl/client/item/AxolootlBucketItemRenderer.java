package axolootl.client.item;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib3.core.IAnimatable;
import software.bernie.geckolib3.model.AnimatedGeoModel;
import software.bernie.geckolib3.renderers.geo.GeoItemRenderer;

public class AxolootlBucketItemRenderer<T extends Item & IAnimatable> extends GeoItemRenderer<T> {

    public AxolootlBucketItemRenderer(AnimatedGeoModel<T> modelProvider) {
        super(modelProvider);
    }

    @Override
    public void render(T animatable, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, ItemStack stack) {


    }

}
