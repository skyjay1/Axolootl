/**
 * Copyright (c) 2023 Skyler James
 * Permission is granted to use, modify, and redistribute this software, in parts or in whole,
 * under the GNU LGPLv3 license (https://www.gnu.org/licenses/lgpl-3.0.en.html)
 **/

package axolootl.entity;

import axolootl.AxRegistry;
import axolootl.data.axolootl_variant.AxolootlVariant;
import com.mojang.math.Vector3f;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Bucketable;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.Optional;

public interface IAxolootl {

    /**
     * @return the axolootl as an entity
     */
    LivingEntity getEntity();

    /**
     * @return a representation of the entity as an item stack
     */
    ItemStack asItemStack();

    /**
     * @param level the level
     * @return true if the axolootl can accept food
     */
    boolean isFeedCandidate(ServerLevel level);

    /**
     * @param level the level
     * @return true if the axolootl can generate resources
     */
    boolean isResourceGenerationCandidate(ServerLevel level);

    /**
     * Feeds the entity using the given food
     * @param level the server level
     * @param food a copy of the food item stack
     * @return the interaction result of the feed operation
     */
    InteractionResult feed(ServerLevel level, final ItemStack food);

    /**
     * @param level the server level
     * @param other the other axolootl, may be empty to query general ability to breed
     * @return true if the axolootl can be bred, at all or with the given axolootl
     */
    boolean isBreedCandidate(ServerLevel level, final Optional<IAxolootl> other);

    /**
     * Breeds the entity with another entity
     * @param level the server level
     * @param other the other axolootl
     * @param enableMobBreeding true to enable breeding axolootls that generate mob resources
     * @return an interaction result with the updated item stack
     */
    Optional<IAxolootl> breed(ServerLevel level, final IAxolootl other, final boolean enableMobBreeding);

    /**
     * @param id the axolootl variant ID, can be null
     */
    void setAxolootlVariantId(@Nullable final ResourceLocation id);

    /**
     * @return the axolootl variant ID, if any is defined
     */
    Optional<ResourceLocation> getAxolootlVariantId();

    /**
     * @return true if the entity is playing dead
     */
    boolean isEntityPlayingDead();

    /**
     * @return the generation speed bonus granted by this axolootl
     */
    default double getGenerationSpeed() {
        return 0;
    }

    /**
     * @return the feed speed bonus granted by this axolootl
     */
    default double getFeedSpeed() {
        return 0;
    }

    /**
     * @return the breed speed bonus granted by this axolootl
     */
    default double getBreedSpeed() {
        return 0;
    }

    /**
     * @param registryAccess the registry access
     * @return the axolootl variant from the registry, if any
     */
    default Optional<AxolootlVariant> getAxolootlVariant(final RegistryAccess registryAccess) {
        return getAxolootlVariant(registryAccess, false);
    }

    /**
     * @param registryAccess the registry access
     * @param includeAll true to include disabled axolootl variants
     * @return the axolootl variant from the registry, if any
     */
    default Optional<AxolootlVariant> getAxolootlVariant(final RegistryAccess registryAccess, final boolean includeAll) {
        // load variant ID
        final Optional<ResourceLocation> oId = getAxolootlVariantId();
        if(oId.isEmpty()) {
            return Optional.empty();
        }
        // validate variant ID
        if(!includeAll && !AxRegistry.AxolootlVariantsReg.isValid(oId.get())) {
            return Optional.empty();
        }
        // all checks passed
        return AxolootlVariant.getRegistry(registryAccess).getOptional(oId.get());
    }

    //// NBT ////

    public static final String KEY_VARIANT_ID = "Axolootl";

    default void writeAxolootlVariant(CompoundTag pCompound) {
        getAxolootlVariantId().ifPresent(id -> pCompound.putString(KEY_VARIANT_ID, id.toString()));
    }

    default void readAxolootlVariant(CompoundTag pCompound) {
        if(pCompound.contains(KEY_VARIANT_ID, Tag.TAG_STRING)) {
            setAxolootlVariantId(new ResourceLocation(pCompound.getString(KEY_VARIANT_ID)));
        }
    }
}
