package axolootl.data.axolootl_variant;

import axolootl.Axolootl;
import axolootl.util.AxCodecUtils;
import com.mojang.math.Vector3f;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import java.util.Optional;

@Immutable
public class AxolootlModelSettings {

    public static final ResourceLocation ENTITY_MODEL = new ResourceLocation(Axolootl.MODID, "axolootl");
    public static final ResourceLocation ENTITY_TEXTURE = new ResourceLocation(Axolootl.MODID, "axolotl_lucy");
    public static final ResourceLocation EMPTY_ENTITY_ANIMATIONS = new ResourceLocation(Axolootl.MODID, "axolootl");

    public static final AxolootlModelSettings EMPTY = new AxolootlModelSettings(ENTITY_MODEL, Optional.empty(), ENTITY_TEXTURE, Optional.empty(), Optional.empty(), -1, -1);

    public static final Codec<AxolootlModelSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.optionalFieldOf("entity", ENTITY_MODEL).forGetter(AxolootlModelSettings::getEntityGeoModel),
            ResourceLocation.CODEC.optionalFieldOf("animations").forGetter(AxolootlModelSettings::getOptionalEntityGeoAnimations),
            ResourceLocation.CODEC.optionalFieldOf("texture", ENTITY_TEXTURE).forGetter(AxolootlModelSettings::getEntityTexture),
            ResourceLocation.CODEC.optionalFieldOf("primary_texture").forGetter(AxolootlModelSettings::getOptionalEntityPrimaryTexture),
            ResourceLocation.CODEC.optionalFieldOf("secondary_texture").forGetter(AxolootlModelSettings::getOptionalEntitySecondaryTexture),
            AxCodecUtils.HEX_OR_INT_CODEC.optionalFieldOf("primary_color", -1).forGetter(AxolootlModelSettings::getPrimaryColor),
            AxCodecUtils.HEX_OR_INT_CODEC.optionalFieldOf("secondary_color", -1).forGetter(AxolootlModelSettings::getSecondaryColor)
    ).apply(instance, AxolootlModelSettings::new));

    /** The GeckoLib entity geo model **/
    private final ResourceLocation entityGeoModel;
    /** The entity animations, if any **/
    @Nullable
    private final ResourceLocation entityGeoAnimations;
    /** The entity texture location, will not be recolored **/
    private final ResourceLocation entityTexture;
    /** The entity texture location to be recolored using the primary color **/
    @Nullable
    private final ResourceLocation entityPrimaryTexture;
    /** The entity texture location to be recolored using the secondary color **/
    @Nullable
    private final ResourceLocation entitySecondaryTexture;
    /** The primary packed color **/
    private final int primaryColor;
    /** The unpacked primary colors **/
    private final Vector3f primaryColors;
    /** The secondary packed color **/
    private final int secondaryColor;
    /** The unpacked secondary colors **/
    private final Vector3f secondaryColors;

    public AxolootlModelSettings(ResourceLocation entityGeoModel, Optional<ResourceLocation> entityGeoAnimations,
                                 ResourceLocation entityTexture, Optional<ResourceLocation> entityPrimaryTexture, Optional<ResourceLocation> entitySecondaryTexture,
                                 int primaryColor, int secondaryColor) {
        this.entityGeoModel = new ResourceLocation(entityGeoModel.getNamespace(), "geo/entity/axolootl/" + entityGeoModel.getPath() + ".geo.json");
        this.entityGeoAnimations = entityGeoAnimations.map(animations -> new ResourceLocation(animations.getNamespace(), "animations/entity/axolootl/" + animations.getPath() + ".animation.json")).orElse(null);
        this.entityTexture = new ResourceLocation(entityTexture.getNamespace(), "textures/entity/axolootl/" + entityTexture.getPath() + ".png");
        this.entityPrimaryTexture = entityPrimaryTexture.map(texture -> new ResourceLocation(texture.getNamespace(), "textures/entity/axolootl/" + texture.getPath() + ".png")).orElse(null);
        this.entitySecondaryTexture = entitySecondaryTexture.map(texture -> new ResourceLocation(texture.getNamespace(), "textures/entity/axolootl/" + texture.getPath() + ".png")).orElse(null);
        this.primaryColor = primaryColor;
        this.secondaryColor = secondaryColor;
        // unpack primary colors
        if(primaryColor < 0) {
            this.primaryColors = new Vector3f(1, 1, 1);
        } else {
            this.primaryColors = unpackColor(primaryColor);
        }
        // unpack secondary colors
        if(primaryColor < 0) {
            this.secondaryColors = new Vector3f(1, 1, 1);
        } else {
            this.secondaryColors = unpackColor(secondaryColor);
        }
    }

    //// GETTERS ////

    public ResourceLocation getEntityGeoModel() {
        return entityGeoModel;
    }

    public ResourceLocation getEntityTexture() {
        return entityTexture;
    }

    public ResourceLocation getEntityGeoAnimations() {
        return entityGeoAnimations;
    }

    public Optional<ResourceLocation> getOptionalEntityGeoAnimations() {
        return Optional.ofNullable(entityGeoAnimations);
    }

    public Optional<ResourceLocation> getOptionalEntityPrimaryTexture() {
        return Optional.ofNullable(entityPrimaryTexture);
    }

    public Optional<ResourceLocation> getOptionalEntitySecondaryTexture() {
        return Optional.ofNullable(entitySecondaryTexture);
    }

    public int getPrimaryColor() {
        return primaryColor;
    }

    public int getSecondaryColor() {
        return secondaryColor;
    }

    /**
     * @return the red, green, blue components of the primary color from 0 to 1.0
     */
    public Vector3f getPrimaryColors() {
        return primaryColors;
    }

    /**
     * @return the red, green, blue components of the secondary color from 0 to 1.0
     */
    public Vector3f getSecondaryColors() {
        return secondaryColors;
    }

    //// METHODS ////

    /**
     * @param color a packed color
     * @return the unpacked red, green, blue components of the color from 0 to 1.0
     */
    public static Vector3f unpackColor(final int color) {
        final float red = (color >> 16) & 0xFF;
        final float green = (color >> 8) & 0xFF;
        final float blue = color & 0xFF;
        return new Vector3f(red / 255.0F, green / 255.0F, blue / 255.0F);
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder("ModelSettings{");
        builder.append(" colors=(" + getPrimaryColor() + ", " + getSecondaryColor() + ")");
        builder.append(" entity=" + getEntityGeoModel().toString());
        builder.append(" texture=" + getEntityTexture().toString() + ", " + getOptionalEntityPrimaryTexture().toString() + ", " + getOptionalEntitySecondaryTexture().toString());
        builder.append(" }");
        return builder.toString();
    }
}
