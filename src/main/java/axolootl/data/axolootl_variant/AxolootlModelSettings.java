package axolootl.data.axolootl_variant;

import axolootl.Axolootl;
import com.mojang.math.Vector3f;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import java.util.Optional;

@Immutable
public class AxolootlModelSettings {

    public static final ResourceLocation ITEM_MODEL = new ResourceLocation(Axolootl.MODID, "todo"); // TODO item model
    public static final ResourceLocation ENTITY_MODEL = new ResourceLocation(Axolootl.MODID, "geo/entity/axolootl.geo.json");
    public static final ResourceLocation ENTITY_TEXTURE = new ResourceLocation("minecraft", "textures/entity/axolotl/axolotl_lucy.png");
    public static final ResourceLocation EMPTY_ENTITY_ANIMATIONS = new ResourceLocation(Axolootl.MODID, "animations/entity/axolootl.animation.json");

    public static final ResourceLocation ENTITY_TEXTURE_BASE = new ResourceLocation(Axolootl.MODID, "textures/entity/axolootl/axolootl_base.png");
    public static final ResourceLocation ENTITY_TEXTURE_PRIMARY = new ResourceLocation(Axolootl.MODID, "textures/entity/axolootl/axolootl_primary.png");
    public static final ResourceLocation ENTITY_TEXTURE_SECONDARY = new ResourceLocation(Axolootl.MODID, "textures/entity/axolootl/axolootl_secondary.png");

    public static final AxolootlModelSettings EMPTY = new AxolootlModelSettings(ITEM_MODEL, true, ENTITY_MODEL, Optional.empty(), ENTITY_TEXTURE, Optional.empty(), Optional.empty(), -1, -1);

    public static final Codec<AxolootlModelSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.optionalFieldOf("item", ITEM_MODEL).forGetter(AxolootlModelSettings::getItemModel),
            Codec.BOOL.optionalFieldOf("item_colors", true).forGetter(AxolootlModelSettings::hasItemModelColors),
            ResourceLocation.CODEC.optionalFieldOf("entity", ENTITY_MODEL).forGetter(AxolootlModelSettings::getEntityGeoModel),
            ResourceLocation.CODEC.optionalFieldOf("animations").forGetter(AxolootlModelSettings::getOptionalEntityGeoAnimations),
            ResourceLocation.CODEC.optionalFieldOf("texture", ENTITY_TEXTURE).forGetter(AxolootlModelSettings::getEntityTexture),
            ResourceLocation.CODEC.optionalFieldOf("primary_texture").forGetter(AxolootlModelSettings::getOptionalEntityPrimaryTexture),
            ResourceLocation.CODEC.optionalFieldOf("secondary_texture").forGetter(AxolootlModelSettings::getOptionalEntitySecondaryTexture),
            Codec.INT.optionalFieldOf("primary_color", -1).forGetter(AxolootlModelSettings::getPrimaryColor),
            Codec.INT.optionalFieldOf("secondary_color", -1).forGetter(AxolootlModelSettings::getSecondaryColor)
    ).apply(instance, AxolootlModelSettings::new));

    /** The GeckoLib item geo model **/
    private final ResourceLocation itemModel;
    /** True if the item model should be recolored **/
    private final boolean hasItemModelColors;
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

    public AxolootlModelSettings(ResourceLocation itemModel, boolean hasItemModelColors, ResourceLocation entityGeoModel,
                                 Optional<ResourceLocation> entityGeoAnimations,
                                 ResourceLocation entityTexture, Optional<ResourceLocation> entityPrimaryTexture, Optional<ResourceLocation> entitySecondaryTexture,
                                 int primaryColor, int secondaryColor) {
        this.itemModel = itemModel;
        this.hasItemModelColors = hasItemModelColors;
        this.entityGeoModel = entityGeoModel;
        this.entityGeoAnimations = entityGeoAnimations.orElse(null);
        this.entityTexture = entityTexture;
        this.entityPrimaryTexture = entityPrimaryTexture.orElse(null);
        this.entitySecondaryTexture = entitySecondaryTexture.orElse(null);
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

    public boolean hasItemModelColors() {
        return hasItemModelColors;
    }

    public ResourceLocation getItemModel() {
        return itemModel;
    }

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
        builder.append(" item=" + getItemModel().toString());
        builder.append(" entity=" + getEntityGeoModel().toString());
        builder.append(" texture=" + getEntityTexture() + ", " + getOptionalEntityPrimaryTexture().toString() + ", " + getOptionalEntitySecondaryTexture().toString());
        builder.append(" }");
        return builder.toString();
    }
}
