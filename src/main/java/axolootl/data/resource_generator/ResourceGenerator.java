package axolootl.data.resource_generator;

import axolootl.AxRegistry;
import axolootl.data.AxolootlVariant;
import com.google.common.collect.ImmutableList;
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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.concurrent.Immutable;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

@Immutable
public abstract class ResourceGenerator {

    public static final Codec<ResourceGenerator> DIRECT_CODEC = ExtraCodecs.lazyInitializedCodec(() -> AxRegistry.RESOURCE_GENERATOR_SERIALIZERS_SUPPLIER.get().getCodec())
            .dispatch(ResourceGenerator::getCodec, Function.identity());

    public static final Codec<Holder<ResourceGenerator>> HOLDER_CODEC = RegistryFileCodec.create(AxRegistry.Keys.RESOURCE_GENERATORS, DIRECT_CODEC);
    public static final Codec<HolderSet<ResourceGenerator>> HOLDER_SET_CODEC = RegistryCodecs.homogeneousList(AxRegistry.Keys.RESOURCE_GENERATORS, DIRECT_CODEC);

    public static final Codec<List<ResourceGenerator>> LIST_CODEC = DIRECT_CODEC.listOf();
    public static final Codec<List<ResourceGenerator>> LIST_OR_SINGLE_CODEC = Codec.either(DIRECT_CODEC, LIST_CODEC)
            .xmap(either -> either.map(ImmutableList::of, Function.identity()),
                    list -> list.size() == 1 ? Either.left(list.get(0)) : Either.right(list));

    public static final Codec<ItemStack> ITEM_OR_STACK_CODEC = Codec.either(ForgeRegistries.ITEMS.getCodec(), ItemStack.CODEC)
            .xmap(either -> either.map(ItemStack::new, Function.identity()),
                    stack -> stack.getCount() == 1 && !stack.hasTag()
                            ? Either.left(stack.getItem())
                            : Either.right(stack));

    /** The Resource Type of this generator **/
    private final ResourceType resourceType;

    public ResourceGenerator(ResourceType resourceType) {
        this.resourceType = resourceType;
    }

    /**
     * @return the {@link ResourceType} of the generator
     */
    public ResourceType getResourceType() {
        return resourceType;
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
}
