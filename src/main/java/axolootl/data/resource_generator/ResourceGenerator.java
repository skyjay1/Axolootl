package axolootl.data.resource_generator;

import axolootl.AxRegistry;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.resources.RegistryFileCodec;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.RandomSource;
import net.minecraft.util.random.SimpleWeightedRandomList;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.concurrent.Immutable;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

@Immutable
public abstract class ResourceGenerator {

    public static final Codec<ResourceGenerator> DIRECT_CODEC = ExtraCodecs.lazyInitializedCodec(() -> AxRegistry.RESOURCE_GENERATOR_SERIALIZERS_SUPPLIER.get().getCodec())
            .dispatch(ResourceGenerator::getCodec, Function.identity());

    public static final Codec<Holder<ResourceGenerator>> HOLDER_CODEC = RegistryFileCodec.create(AxRegistry.Keys.RESOURCE_GENERATORS, DIRECT_CODEC);
    public static final Codec<HolderSet<ResourceGenerator>> HOLDER_SET_CODEC = RegistryCodecs.homogeneousList(AxRegistry.Keys.RESOURCE_GENERATORS, DIRECT_CODEC);

    public static final Codec<List<ResourceGenerator>> DIRECT_LIST_CODEC = DIRECT_CODEC.listOf();
    public static final Codec<List<ResourceGenerator>> DIRECT_LIST_OR_SINGLE_CODEC = Codec.either(DIRECT_CODEC, DIRECT_LIST_CODEC)
            .xmap(either -> either.map(ImmutableList::of, Function.identity()),
                    list -> list.size() == 1 ? Either.left(list.get(0)) : Either.right(list));

    /**
     * Codec to map between items and item stacks with a single item and no tag
     */
    public static final Codec<ItemStack> ITEM_OR_STACK_CODEC = Codec.either(ForgeRegistries.ITEMS.getCodec(), ItemStack.CODEC)
            .xmap(either -> either.map(ItemStack::new, Function.identity()),
                    stack -> stack.getCount() == 1 && !stack.hasTag()
                            ? Either.left(stack.getItem())
                            : Either.right(stack));

    /**
     * Codec to map between single objects and simple weighted random lists
     */
    public static final Codec<SimpleWeightedRandomList<ResourceGenerator>> WEIGHTED_LIST_CODEC = Codec.either(DIRECT_CODEC, SimpleWeightedRandomList.wrappedCodecAllowingEmpty(ResourceGenerator.DIRECT_CODEC))
            .xmap(either -> either.map(SimpleWeightedRandomList::single, Function.identity()), ResourceGenerator::eitherSimpleList);

    /** The Resource Type of this generator **/
    private final ResourceType resourceType;
    private final Set<ResourceType> resourceTypes;

    public ResourceGenerator(ResourceType resourceType) {
        this.resourceType = resourceType;
        this.resourceTypes = ImmutableSet.of(resourceType);
    }

    /**
     * @return the primary {@link ResourceType} of the generator
     */
    public ResourceType getResourceType() {
        return this.resourceType;
    }

    /**
     * @return any {@link ResourceType}s applicable to the generator
     */
    public Set<ResourceType> getResourceTypes() {
        return this.resourceTypes;
    }

    /**
     * @param type a resource type
     * @return true if the resource type is applicable to this resource generator
     */
    public boolean is(final ResourceType type) {
        return type == this.getResourceType() || this.getResourceTypes().contains(type);
    }

    /**
     * Generates any number of items
     * @param entity the entity
     * @param random the random instance
     * @return a collection of generated items, may be empty
     */
    public abstract Collection<ItemStack> getRandomEntries(final LivingEntity entity, final RandomSource random);

    /**
     * @return the codec for this resource generator, used in the dispatcher
     */
    public abstract Codec<? extends ResourceGenerator> getCodec();

    /**
     * @param access the registry access
     * @return the resource generator registry
     */
    public static Registry<ResourceGenerator> getRegistry(final RegistryAccess access) {
        return access.registryOrThrow(AxRegistry.Keys.RESOURCE_GENERATORS);
    }

    /**
     * Helper method to convert a simple list to a simple weighted random list.
     * Currently unused.
     * @param list the list
     * @param <T> some object type compatible with {@link SimpleWeightedRandomList}
     * @return a simple weighted random list with all weights distributed equally
     */
    public static <T> SimpleWeightedRandomList<T> createSimpleList(final List<T> list) {
        SimpleWeightedRandomList.Builder<T> builder = SimpleWeightedRandomList.builder();
        list.forEach(t -> builder.add(t, 1));
        return builder.build();
    }

    /**
     * Helper method to convert between a simple weighted random list and a single wrapped entry
     * @param list the list
     * @param <T> some object type compatible with {@link SimpleWeightedRandomList}
     * @return an either composing of the simple weighted random list or the unwrapped list
     */
    public static <T> Either<T, SimpleWeightedRandomList<T>> eitherSimpleList(final SimpleWeightedRandomList<T> list) {
        if(list.unwrap().size() == 1) {
            return Either.left(list.unwrap().get(0).getData());
        }
        return Either.right(list);
    }
}
