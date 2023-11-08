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
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.LerpingModel;
import net.minecraft.world.entity.LivingEntity;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

import java.util.Locale;
import java.util.Optional;

/**
 * Fermi is a real-life axolotl that belongs to my sibling. He comes from a genetically modified stock of axolotls
 * and his eyes glow in the dark. Fluorescent proteins are commonly used when inserting genes because they easily indicate
 * which experiments led to successfully modified DNA.
 * @author skyjay1
 * @param <T> the axolootl entity
 */
public class FermiLayer<T extends LivingEntity & LerpingModel & IAxolootl & GeoAnimatable> extends GeoRenderLayer<T> {

    private static final ResourceLocation TEXTURE = new ResourceLocation(Axolootl.MODID, "textures/entity/axolootl/fermi.png");
    private static final RenderType RENDER_TYPE = RenderType.eyes(TEXTURE);
    private static final TagKey<AxolootlVariant> BLACKLIST = TagKey.create(AxRegistry.Keys.AXOLOOTL_VARIANTS, new ResourceLocation(Axolootl.MODID, "special_blacklist"));

    public FermiLayer(GeoRenderer<T> parent) {
        super(parent);
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
        if(!"fermi".equals(entity.getName().getString().toLowerCase(Locale.ENGLISH))) {
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
        getRenderer().reRender(bakedModel, poseStack, bufferSource, entity, RENDER_TYPE, buffer, partialTick, LightTexture.FULL_BRIGHT, packedOverlay, 1.0F, 1.0F, 1.0F, 1.0F);
    }
}
