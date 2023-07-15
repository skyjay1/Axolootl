package axolootl.entity;

import axolootl.AxRegistry;
import axolootl.data.AxolootlVariant;
import axolootl.recipe.AxolootlBreedingRecipe;
import axolootl.util.AxolootlVariantContainer;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.Optional;

public interface IAxolootl {

    /**
     * @return the axolootl as an entity
     */
    LivingEntity getEntity();

    /**
     * @param level the level
     * @return true if the axolootl can accept food
     */
    boolean isFeedCandidate(ServerLevel level);

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
     * @return an interaction result with the updated item stack
     */
    Optional<IAxolootl> breed(ServerLevel level, final IAxolootl other);

    /**
     * @param id the axolootl variant ID, can be null
     */
    void setAxolootlVariantId(@Nullable final ResourceLocation id);

    /**
     * @return the axolootl variant ID, if any is defined
     */
    Optional<ResourceLocation> getAxolootlVariantId();

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
     * @return the axolootl variant from the registry, if any
     */
    default Optional<AxolootlVariant> getAxolootlVariant(final RegistryAccess registryAccess) {
        final Optional<ResourceLocation> oId = getAxolootlVariantId();
        if(oId.isEmpty()) {
            return Optional.empty();
        }
        return AxolootlVariant.getRegistry(registryAccess).getOptional(oId.get());
    }

    /**
     * @param level the level
     * @param container an axolootl variant container with exactly 2 entries
     * @return the axolotl breeding recipe for the given container, if any
     */
    default Optional<AxolootlBreedingRecipe> getBreedingRecipe(final Level level, final AxolootlVariantContainer container) {
        if(container.getContainerSize() != 2 || container.isEmpty()) {
            return Optional.empty();
        }
        return level.getRecipeManager().getRecipeFor(AxRegistry.RecipeReg.AXOLOOTL_BREEDING_TYPE.get(), container, level);
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
