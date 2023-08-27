/**
 * Copyright (c) 2023 Skyler James
 * Permission is granted to use, modify, and redistribute this software, in parts or in whole,
 * under the GNU LGPLv3 license (https://www.gnu.org/licenses/lgpl-3.0.en.html)
 **/

package axolootl.data.resource_generator;

import axolootl.AxRegistry;
import axolootl.util.AxCodecUtils;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.RegistryFileCodec;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.RandomSource;
import net.minecraft.util.random.SimpleWeightedRandomList;
import net.minecraft.util.random.WeightedEntry;
import net.minecraft.util.random.WeightedRandomList;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

public abstract class ResourceGenerator {

    public static final Codec<ResourceGenerator> DIRECT_CODEC = ExtraCodecs.lazyInitializedCodec(() -> AxRegistry.RESOURCE_GENERATOR_SERIALIZERS_SUPPLIER.get().getCodec())
            .dispatch(ResourceGenerator::getCodec, Function.identity());

    public static final Codec<Holder<ResourceGenerator>> HOLDER_CODEC = RegistryFileCodec.create(AxRegistry.Keys.RESOURCE_GENERATORS, DIRECT_CODEC);
    public static final Codec<HolderSet<ResourceGenerator>> HOLDER_SET_CODEC = RegistryCodecs.homogeneousList(AxRegistry.Keys.RESOURCE_GENERATORS, DIRECT_CODEC);

    public static final Codec<List<ResourceGenerator>> DIRECT_LIST_CODEC = AxCodecUtils.listOrElementCodec(DIRECT_CODEC);

    /** Codec to map between single objects and simple weighted random lists **/
    public static final Codec<SimpleWeightedRandomList<ResourceGenerator>> WEIGHTED_LIST_CODEC = Codec.either(ResourceGenerator.DIRECT_CODEC, SimpleWeightedRandomList.wrappedCodecAllowingEmpty(ResourceGenerator.DIRECT_CODEC))
            .xmap(either -> either.map(SimpleWeightedRandomList::single, Function.identity()), ResourceGenerator::eitherSimpleList);

    /** The Resource Type of this generator **/
    private final ResourceType resourceType;
    private final Set<ResourceType> resourceTypes;

    private final List<ResourceDescriptionGroup> description;
    private final List<ResourceDescriptionGroup> descriptionView;

    public ResourceGenerator(ResourceType resourceType) {
        this.resourceType = resourceType;
        this.resourceTypes = ImmutableSet.of(resourceType);
        this.description = new ArrayList<>();
        this.descriptionView = Collections.unmodifiableList(this.description);
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
     * Gets or creates the description for this resource generator
     * @return the list of descriptions
     */
    public List<ResourceDescriptionGroup> getDescription() {
        if(this.description.isEmpty()) {
            this.description.addAll(createDescription());
        }
        return this.descriptionView;
    }

    public List<Component> getDescriptionText() {
        return createDescriptionText(getDescription());
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
     * @return a list of resource description groups that describe this resource generator
     */
    protected abstract List<ResourceDescriptionGroup> createDescription();

    //// HELPER METHODS ////

    public static List<Component> createDescriptionText(final List<ResourceDescriptionGroup> descriptions) {
        final ImmutableList.Builder<Component> builder = ImmutableList.builder();
        final boolean showGroupChance = descriptions.size() > 1;
        for(ResourceDescriptionGroup group : descriptions) {
            MutableComponent chanceDescription = null;
            // add percent chance text
            if(showGroupChance || group.showChance()) {
                chanceDescription = group.getChanceDescription().copy();
                builder.add(chanceDescription);
            }
            // create single-line component if possible
            if(group.getDescriptions().size() == 1 && group.getDescriptions().get(0).getDescriptions().size() <= 1) {
                ResourceDescription description = group.getDescriptions().get(0);
                MutableComponent componentBuilder = (chanceDescription != null) ? chanceDescription.append(" ") : Component.empty();
                // add description percent chance text
                if(description.showChance()) {
                    componentBuilder.append(description.getChanceDescription()).append(" ");
                }
                // add description text
                if(description.getDescriptions().isEmpty()) {
                    componentBuilder.append(getItemDisplayName(description.getItem()));
                } else {
                    Component c = description.getDescriptions().get(0);
                    componentBuilder.append(c).withStyle(c.getStyle());
                }
                // add to builder if necessary
                if(chanceDescription == null) {
                    builder.add(componentBuilder);
                }
            } else {
                // iterate descriptions in group
                boolean showDescriptionChance = group.getDescriptions().size() > 1;
                for(ResourceDescription description : group.getDescriptions()) {
                    // add description percent chance text
                    if(showDescriptionChance || description.showChance()) {
                        builder.add(description.getChanceDescription());
                    }
                    // add description text
                    if(description.getDescriptions().isEmpty()) {
                        builder.add(Component.literal("  ").append(getItemDisplayName(description.getItem())));
                    }
                    for(Component c : description.getDescriptions()) {
                        builder.add(Component.literal("  ").append(c).withStyle(c.getStyle()));
                    }
                }
            }
        }

        return builder.build();
    }

    /**
     * @param list a weighted random list
     * @return the total weight of the list entries
     */
    public static int calculateTotalWeight(final WeightedRandomList<?> list) {
        return list.unwrap()
                .stream()
                .map(e -> e.getWeight().asInt())
                .reduce(Integer::sum)
                .orElse(1).intValue();
    }

    /**
     * @param itemStack an item stack
     * @return the item display name or an indicator that the stack is empty
     */
    public static Component getItemDisplayName(final ItemStack itemStack) {
        if (itemStack.isEmpty()) {
            return Component.translatable("axolootl.resource_generator.nothing");
        }
        return itemStack.getHoverName();
    }

    /**
     * @param list a weighted random list
     * @return a list of weighted entries sorted from highest to lowest
     */
    public static <T> List<WeightedEntry.Wrapper<T>> createSortedWeightedList(final WeightedRandomList<WeightedEntry.Wrapper<T>> list) {
        final Comparator<WeightedEntry.Wrapper<T>> comparator = Comparator.comparingInt(e -> e.getWeight().asInt());
        return createSortedWeightedList(list, comparator.reversed());
    }

    /**
     * @param list a weighted random list
     * @param comparator the comparator to sort the list
     * @param <T> the weighted list wrapped type
     * @return a list of weighted entries sorted from highest to lowest
     */
    public static <T> List<WeightedEntry.Wrapper<T>> createSortedWeightedList(final WeightedRandomList<WeightedEntry.Wrapper<T>> list, Comparator<WeightedEntry.Wrapper<T>> comparator) {
        List<WeightedEntry.Wrapper<T>> entries = new ArrayList<>(list.unwrap());
        entries.sort(comparator);
        return entries;
    }

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
