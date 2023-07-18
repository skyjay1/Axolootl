package axolootl.client.entity;

import axolootl.Axolootl;
import axolootl.entity.AxolootlEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Vector3f;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;

public class AxolootlSecondaryLayer<T extends AxolootlEntity, M extends AxolootlModel<T>> extends RenderLayer<T, M> {

    public static final ResourceLocation TEXTURE = new ResourceLocation(Axolootl.MODID, "textures/entity/axolootl/axolootl_secondary.png");

    public AxolootlSecondaryLayer(RenderLayerParent<T, M> pRenderer) {
        super(pRenderer);
    }

    @Override
    public void render(PoseStack pPoseStack, MultiBufferSource pBuffer, int pPackedLight, T pLivingEntity, float pLimbSwing, float pLimbSwingAmount, float pPartialTick, float pAgeInTicks, float pNetHeadYaw, float pHeadPitch) {
        final Vector3f colors = getParentModel().secondaryColor;
        renderColoredCutoutModel(getParentModel(), TEXTURE, pPoseStack, pBuffer, pPackedLight, pLivingEntity, colors.x(), colors.y(), colors.z());
    }
}
