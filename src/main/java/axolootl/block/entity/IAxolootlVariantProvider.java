package axolootl.block.entity;

import axolootl.data.AxolootlVariant;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

import javax.annotation.Nullable;
import java.util.Optional;

public interface IAxolootlVariantProvider {

    /**
     * @return the axolootl variant provider as an entity
     */
    LivingEntity getEntity();

    /**
     * @param id the axolootl variant ID, can be null
     */
    void setAxolootlVariantId(@Nullable final ResourceLocation id);

    /**
     * @return the axolootl variant ID, if any is defined
     */
    Optional<ResourceLocation> getAxolootlVariantId();

    /**
     * @return the axolootl variant from the registry, if any
     */
    default Optional<AxolootlVariant> getAxolootlVariant(final RegistryAccess registryAccess) {
        final Optional<ResourceLocation> oId = getAxolootlVariantId();
        if(oId.isEmpty()) {
            return Optional.empty();
        }
        return AxolootlVariant.getRegistry(registryAccess).getOptional(oId.get());
    }

    //// NBT ////

    public static final String KEY_VARIANT_ID = "VariantId";

    default void writeAxolootlVariant(CompoundTag pCompound) {
        getAxolootlVariantId().ifPresent(id -> pCompound.putString(KEY_VARIANT_ID, id.toString()));
    }

    default void readAxolootlVariant(CompoundTag pCompound) {
        if(pCompound.contains(KEY_VARIANT_ID, Tag.TAG_STRING)) {
            setAxolootlVariantId(new ResourceLocation(pCompound.getString(KEY_VARIANT_ID)));
        }
    }
}
