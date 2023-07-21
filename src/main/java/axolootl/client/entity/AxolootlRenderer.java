package axolootl.client.entity;

import axolootl.Axolootl;
import axolootl.entity.AxolootlEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class AxolootlRenderer extends MobRenderer<AxolootlEntity, AxolootlModel<AxolootlEntity>> {

    public static final ResourceLocation TEXTURE_BASE = new ResourceLocation(Axolootl.MODID, "textures/entity/axolootl/axolootl_base.png");

    public AxolootlRenderer(EntityRendererProvider.Context context) {
        super(context, new AxolootlModel<>(context.bakeLayer(ModelLayers.AXOLOTL)), 0.5F);
        this.addLayer(new AxolootlPrimaryLayer<>(this));
        this.addLayer(new AxolootlSecondaryLayer<>(this));
    }

    @Override
    public void render(AxolootlEntity pEntity, float pEntityYaw, float pPartialTicks, PoseStack pMatrixStack, MultiBufferSource pBuffer, int pPackedLight) {
        // prepare to color
        getModel().setColors(pEntity.getPrimaryColors(), pEntity.getSecondaryColors());
        // render entity
        super.render(pEntity, pEntityYaw, pPartialTicks, pMatrixStack, pBuffer, pPackedLight);
        // reset colors
        getModel().resetColors();
    }

    @Override
    public ResourceLocation getTextureLocation(AxolootlEntity pEntity) {
        return TEXTURE_BASE;
    }


}
