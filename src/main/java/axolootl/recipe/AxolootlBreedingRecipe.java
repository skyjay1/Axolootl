package axolootl.recipe;

import axolootl.AxRegistry;
import axolootl.Axolootl;
import axolootl.data.AxolootlVariant;
import axolootl.util.AxolootlVariantContainer;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.gson.JsonObject;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import net.minecraft.Util;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.util.random.SimpleWeightedRandomList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

public class AxolootlBreedingRecipe implements Recipe<AxolootlVariantContainer> {

    /** The recipe ID **/
    private final ResourceLocation recipeId;
    /** The first axolootl variant **/
    private final Set<ResourceKey<AxolootlVariant>> first;
    /** The second axolootl variant **/
    private final Set<ResourceKey<AxolootlVariant>> second;
    /** A weighted list to determine the result, if it is empty the first variant is used instead **/
    private final SimpleWeightedRandomList<ResourceKey<AxolootlVariant>> result;

    public AxolootlBreedingRecipe(ResourceLocation recipeId, List<ResourceKey<AxolootlVariant>> first, List<ResourceKey<AxolootlVariant>> second,
                                  SimpleWeightedRandomList<ResourceKey<AxolootlVariant>> result) {
        this.recipeId = recipeId;
        this.first = ImmutableSet.copyOf(first);
        this.second = ImmutableSet.copyOf(second);
        this.result = result;
    }

    //// AXOLOOTL RECIPE ////

    public Set<ResourceKey<AxolootlVariant>> getFirst() {
        return first;
    }

    public Set<ResourceKey<AxolootlVariant>> getSecond() {
        return second;
    }

    /**
     * @return A weighted list to determine the result
     * @see #getResult(RandomSource)
     **/
    public SimpleWeightedRandomList<ResourceKey<AxolootlVariant>> getResult() {
        return result;
    }

    /**
     * @return A weighted list to determine the result, if it is empty the first variant is used instead
     **/
    public ResourceKey<AxolootlVariant> getResult(final RandomSource random) {
        // roll result, if that fails, use a random element from the first holder set
        Optional<ResourceKey<AxolootlVariant>> oResult = getResult().getRandomValue(random);
        return oResult.orElse(getFirst().iterator().next());
    }

    /**
     * @param level the level
     * @param container the axolootl container
     * @param random a random source
     * @return the result of {@link #getResult(RandomSource)}
     */
    public ResourceKey<AxolootlVariant> assemble(final Level level, final AxolootlVariantContainer container, final RandomSource random) {
        return getResult(random);
    }

    //// RECIPE /////

    @Override
    public boolean matches(AxolootlVariantContainer pContainer, Level pLevel) {
        // verify container size
        if(pContainer.getContainerSize() != 2 || pContainer.isEmpty()) {
            return false;
        }
        // verify holder set size
        if(getFirst().size() <= 0 || getSecond().size() <= 0) {
            return false;
        }
        // verify objects are registered
        final ResourceLocation idFirst = AxolootlVariant.getRegistry(pLevel.registryAccess()).getKey(pContainer.getEntry(0));
        final ResourceLocation idSecond = AxolootlVariant.getRegistry(pLevel.registryAccess()).getKey(pContainer.getEntry(1));
        if(null == idFirst || null == idSecond) {
            return false;
        }
        // create registry keys
        final ResourceKey<AxolootlVariant> oFirst = ResourceKey.create(AxRegistry.Keys.AXOLOOTL_VARIANTS, idFirst);
        final ResourceKey<AxolootlVariant> oSecond = ResourceKey.create(AxRegistry.Keys.AXOLOOTL_VARIANTS, idSecond);
        // determine if first and second match (order does not matter)
        return (this.getFirst().contains(oFirst) && this.getSecond().contains(oSecond))
            || (this.getFirst().contains(oSecond) && this.getSecond().contains(oFirst));
    }

    @Override
    public ResourceLocation getId() {
        return recipeId;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return AxRegistry.RecipeReg.AXOLOOTL_BREEDING_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return AxRegistry.RecipeReg.AXOLOOTL_BREEDING_TYPE.get();
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public ItemStack getToastSymbol() {
        return new ItemStack(Items.AXOLOTL_BUCKET);
    }

    //// NO OP ////

    @Override
    public ItemStack assemble(AxolootlVariantContainer pContainer) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canCraftInDimensions(int pWidth, int pHeight) {
        return true;
    }

    @Override
    public ItemStack getResultItem() {
        return ItemStack.EMPTY;
    }

    //// SERIALIZER ////

    public static class Serializer implements RecipeSerializer<AxolootlBreedingRecipe> {

        public static final String KEY_FIRST = "first";
        public static final String KEY_SECOND = "second";
        public static final String KEY_RESULT = "result";
        public static final String KEY_VARIANTS = "variants";
        public static final String KEY_POOL = "pool";

        private static final Codec<ResourceKey<AxolootlVariant>> RESOURCE_KEY_DIRECT_CODEC = ResourceLocation.CODEC
                .xmap(id -> ResourceKey.create(AxRegistry.Keys.AXOLOOTL_VARIANTS, id), ResourceKey::location);
        private static final Codec<List<ResourceKey<AxolootlVariant>>> RESOURCE_KEY_LIST_CODEC = RESOURCE_KEY_DIRECT_CODEC.listOf()
                .fieldOf(KEY_VARIANTS).codec();
        private static final Codec<List<ResourceKey<AxolootlVariant>>> RESOURCE_KEY_CODEC = Codec.either(RESOURCE_KEY_DIRECT_CODEC, RESOURCE_KEY_LIST_CODEC)
                .xmap(either -> either.map(ImmutableList::of, Function.identity()),
                        list -> list.size() == 1 ? Either.left(list.get(0)) : Either.right(list));

        private static final Codec<SimpleWeightedRandomList<ResourceKey<AxolootlVariant>>> WEIGHTED_LIST_DIRECT_CODEC =
                SimpleWeightedRandomList.wrappedCodec(RESOURCE_KEY_DIRECT_CODEC).fieldOf(KEY_POOL).codec();

        private static final Codec<SimpleWeightedRandomList<ResourceKey<AxolootlVariant>>> WEIGHTED_LIST_CODEC = Codec.either(RESOURCE_KEY_DIRECT_CODEC, WEIGHTED_LIST_DIRECT_CODEC)
                .xmap(either -> either.map(SimpleWeightedRandomList::single, Function.identity()),
                        list -> list.unwrap().size() == 1 ? Either.left(list.unwrap().get(0).getData()) : Either.right(list));

        private static final Codec<List<ResourceKey<AxolootlVariant>>> FIRST = RESOURCE_KEY_CODEC.fieldOf(KEY_FIRST).codec();
        private static final Codec<List<ResourceKey<AxolootlVariant>>> SECOND = RESOURCE_KEY_CODEC.fieldOf(KEY_SECOND).codec();
        private static final Codec<SimpleWeightedRandomList<ResourceKey<AxolootlVariant>>> RESULT = WEIGHTED_LIST_CODEC.fieldOf(KEY_RESULT).codec();

        @Override
        public AxolootlBreedingRecipe fromJson(ResourceLocation pRecipeId, JsonObject pSerializedRecipe) {

            final List<ResourceKey<AxolootlVariant>> first = FIRST.parse(JsonOps.INSTANCE, pSerializedRecipe)
                    .resultOrPartial(s -> Axolootl.LOGGER.error("Failed to parse '" + KEY_FIRST + "' from axolootl recipe '" + pRecipeId + "': " + s))
                    .orElseThrow();
            final List<ResourceKey<AxolootlVariant>> second = SECOND.parse(JsonOps.INSTANCE, pSerializedRecipe)
                    .resultOrPartial(s -> Axolootl.LOGGER.error("Failed to parse '" + KEY_SECOND + "' from axolootl recipe '" + pRecipeId + "': " + s))
                    .orElseThrow();
            final SimpleWeightedRandomList<ResourceKey<AxolootlVariant>> result = RESULT.parse(JsonOps.INSTANCE, pSerializedRecipe)
                    .resultOrPartial(s -> Axolootl.LOGGER.error("Failed to parse  '" + KEY_RESULT + "'  from axolootl recipe '" + pRecipeId + "': " + s))
                    .orElseThrow();
            return new AxolootlBreedingRecipe(pRecipeId, first, second, result);
        }

        @Override
        public @Nullable AxolootlBreedingRecipe fromNetwork(ResourceLocation pRecipeId, FriendlyByteBuf pBuffer) {
            final List<ResourceKey<AxolootlVariant>> first = pBuffer.readWithCodec(FIRST);
            final List<ResourceKey<AxolootlVariant>> second = pBuffer.readWithCodec(SECOND);
            final SimpleWeightedRandomList<ResourceKey<AxolootlVariant>> result = pBuffer.readWithCodec(RESULT);
            return new AxolootlBreedingRecipe(pRecipeId, first, second, result);
        }

        @Override
        public void toNetwork(FriendlyByteBuf pBuffer, AxolootlBreedingRecipe pRecipe) {
            pBuffer.writeWithCodec(FIRST, ImmutableList.copyOf(pRecipe.getFirst()));
            pBuffer.writeWithCodec(SECOND, ImmutableList.copyOf(pRecipe.getSecond()));
            pBuffer.writeWithCodec(RESULT, pRecipe.getResult());
        }
    }
}
