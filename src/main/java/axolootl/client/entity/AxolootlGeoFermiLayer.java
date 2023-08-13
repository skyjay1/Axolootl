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
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.LerpingModel;
import net.minecraft.world.entity.LivingEntity;
import software.bernie.geckolib3.core.IAnimatable;
import software.bernie.geckolib3.renderers.geo.GeoLayerRenderer;
import software.bernie.geckolib3.renderers.geo.IGeoRenderer;

import java.util.Locale;
import java.util.Optional;

/**
 * Fermi is a real-life axolotl that belongs to my sibling. He comes from a genetically modified stock of axolotls
 * and his eyes glow in the dark. Fluorescent proteins are commonly used when inserting genes because they easily indicate
 * which experiments led to successfully modified DNA.
 * @author skyjay1
 * @param <T> the axolootl entity
 */
public class AxolootlGeoFermiLayer<T extends LivingEntity & LerpingModel & IAxolootl & IAnimatable> extends GeoLayerRenderer<T> {

    private static final ResourceLocation TEXTURE = new ResourceLocation(Axolootl.MODID, "textures/entity/axolootl/fermi.png");
    private static final TagKey<AxolootlVariant> BLACKLIST = TagKey.create(AxRegistry.Keys.AXOLOOTL_VARIANTS, new ResourceLocation(Axolootl.MODID, "special_blacklist"));

    public AxolootlGeoFermiLayer(IGeoRenderer<T> parent) {
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
        final RegistryAccess access = minecraft.level.registryAccess();
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
        renderModel(getEntityModel(), TEXTURE, poseStack, multiBufferSource, LightTexture.FULL_BRIGHT, entity, partialTick, 1.0F, 1.0F, 1.0F);
    }

    @Override
    public RenderType getRenderType(ResourceLocation textureLocation) {
        return RenderType.eyes(textureLocation);
    }
}
