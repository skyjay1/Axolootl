/**
 * Copyright (c) 2023 Skyler James
 * Permission is granted to use, modify, and redistribute this software, in parts or in whole,
 * under the GNU LGPLv3 license (https://www.gnu.org/licenses/lgpl-3.0.en.html)
 **/

package axolootl.data.resource_generator;

import com.google.common.collect.ImmutableList;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.util.random.WeightedEntry;
import net.minecraft.util.random.WeightedRandomList;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.feature.stateproviders.SimpleStateProvider;
import net.minecraft.world.level.levelgen.feature.stateproviders.WeightedStateProvider;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.tags.ITag;

import javax.annotation.concurrent.Immutable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;

@Immutable
public class ResourceDescriptionGroup {

    private final List<ResourceDescription> descriptions;
    private final int weight;
    private final int totalWeight;
    private final boolean forceShowWeight;
    private final double percentChance;
    private final Component chanceDescription;

    /**
     * @param descriptions the list of resource descriptions
     * @param weight the weight of this description group
     * @param totalWeight the total weight of all description groups
     * @param forceShowWeight true to always display the weight percent chance
     * @see #builder(int, int)
     * @see #builder()
     */
    public ResourceDescriptionGroup(List<ResourceDescription> descriptions, int weight, int totalWeight, boolean forceShowWeight) {
        this.descriptions = ImmutableList.sortedCopyOf(Comparator.comparingDouble(ResourceDescription::getPercentChance).reversed(), descriptions);
        this.weight = weight;
        this.totalWeight = totalWeight;
        this.forceShowWeight = forceShowWeight;
        this.percentChance = weight / Math.max(1.0D, totalWeight);
        this.chanceDescription = ResourceDescription.createChanceDescription(this.percentChance);
    }

    /**
     * @param weight the weight of the description group
     * @param totalWeight the total weight of all description groups
     * @return a new builder to begin creating a resource description group
     */
    public static ResourceDescriptionGroup.Builder builder(final int weight, final int totalWeight) {
        return new Builder(weight, totalWeight);
    }

    /**
     * @return a new builder to begin creating a resource description group
     */
    public static ResourceDescriptionGroup.Builder builder() {
        return new Builder(1, 1);
    }

    /**
     * @param group a resource description group to copy
     * @param weight the weight of the description group
     * @param totalWeight the total weight of all description groups
     * @return a new builder to begin creating a resource description group
     */
    public static ResourceDescriptionGroup.Builder builder(final ResourceDescriptionGroup group, final int weight, final int totalWeight) {
        return new Builder(group, weight, totalWeight);
    }

    //// GETTERS ////

    public boolean showChance() {
        return forceShowWeight || weight < totalWeight;
    }

    public int getCount() {
        return descriptions.size();
    }

    public List<ResourceDescription> getDescriptions() {
        return descriptions;
    }

    public int getWeight() {
        return weight;
    }

    public int getTotalWeight() {
        return totalWeight;
    }

    public double getPercentChance() {
        return percentChance;
    }

    public Component getChanceDescription() {
        return chanceDescription;
    }

    //// BUILDER CLASS ////

    public static class Builder {
        private final List<ResourceDescription> builder;
        private final List<Component> descriptions;
        private final int groupWeight;
        private final int groupTotalWeight;

        private boolean forceShowWeight;

        public Builder(int groupWeight, int groupTotalWeight) {
            this.builder = new LinkedList<>();
            this.descriptions = new ArrayList<>();
            this.groupWeight = Math.max(1, groupWeight);
            this.groupTotalWeight = Math.max(1, groupTotalWeight);
            this.forceShowWeight = false;
        }

        public Builder(final ResourceDescriptionGroup group, int groupWeight, int groupTotalWeight) {
            this.builder = new ArrayList<>(group.descriptions);
            this.descriptions = new ArrayList<>();
            this.groupWeight = groupWeight;
            this.groupTotalWeight = groupTotalWeight;
            this.forceShowWeight = group.forceShowWeight;
        }

        public Builder withDescriptions(final Component... descriptions) {
            return withDescriptions(ImmutableList.copyOf(descriptions));
        }

        public Builder withDescriptions(final List<Component> descriptions) {
            this.descriptions.clear();
            this.descriptions.addAll(descriptions);
            return this;
        }

        public Builder clearDescriptions() {
            this.descriptions.clear();
            return this;
        }

        /**
         * Adds the resource description directly to the resource generator group
         * @param description the resource description
         * @return the builder instance
         */
        public Builder with(final ResourceDescription description) {
            this.builder.add(description);
            return this;
        }

        /**
         * Adds the resource descriptions directly to the resource generator group
         * @param descriptions the resource descriptions
         * @return the builder instance
         */
        public Builder with(final List<ResourceDescription> descriptions) {
            this.builder.addAll(descriptions);
            return this;
        }

        /**
         * Forces the resource description group to always display its weight
         * @return the builder instance
         */
        public Builder forceShowWeight() {
            this.forceShowWeight = true;
            return this;
        }

        /**
         * @param itemStack a single item stack
         * @return the resource description group
         */
        public ResourceDescriptionGroup ofItem(final ItemStack itemStack) {
            this.builder.add(new ResourceDescription(itemStack, this.groupWeight, this.groupTotalWeight, this.descriptions));
            return build();
        }

        /**
         * @param tagKey a tag key for a collection of equally weighted items
         * @return the resource description group
         */
        public ResourceDescriptionGroup ofTag(final TagKey<Item> tagKey) {
            final Component tagName = Component.literal("#" + tagKey.location().toString()).withStyle(ChatFormatting.GRAY);
            final Component tagDescription = Component.translatable("axolootl.resource_description.tag", tagName);
            this.descriptions.add(tagDescription);
            final ITag<Item> tag = ForgeRegistries.ITEMS.tags().getTag(tagKey);
            final int size = tag.size();
            // create singleton description
            if (size == 1) {
                this.builder.add(new ResourceDescription(new ItemStack(tag.iterator().next()), 1, 1, this.descriptions));
                return build();
            }
            // create equally weighted chance descriptions
            for (Item item : tag) {
                builder.add(new ResourceDescription(new ItemStack(item), 1, size, this.descriptions));
            }
            return build();
        }

        /**
         * @param items a collection of equally weighted itemstacks
         * @return the resource description group
         */
        public ResourceDescriptionGroup ofList(final Collection<ItemStack> items) {
            final int size = items.size();
            for(ItemStack item : items) {
                this.builder.add(new ResourceDescription(item, 1, size, this.descriptions));
            }
            return build();
        }

        /**
         * Creates a wrapped description for each entry in the list
         * @param list a weighted list of item stacks
         * @return the resource description group
         */
        public ResourceDescriptionGroup ofWeightedList(final WeightedRandomList<WeightedEntry.Wrapper<ItemStack>> list) {
            return ofWeightedList(list, Function.identity());
        }

        /**
         * Creates a wrapped description for each entry in the list
         * @param list a weighted list of objects
         * @param toItemStack a function to convert the list entries to item stacks
         * @param <T> an object that can be converted to an item stack
         * @return the resource description group
         */
        public <T> ResourceDescriptionGroup ofWeightedList(final WeightedRandomList<WeightedEntry.Wrapper<T>> list, Function<T, ItemStack> toItemStack) {
            final int totalWeight = ResourceGenerator.calculateTotalWeight(list);
            for (WeightedEntry.Wrapper<T> entry : ResourceGenerator.createSortedWeightedList(list)) {
                final ItemStack itemStack = toItemStack.apply(entry.getData());
                this.builder.add(new ResourceDescription(itemStack, entry.getWeight().asInt(), totalWeight, this.descriptions));
            }
            return build();
        }

        /**
         * @param blockStateProvider a block state provider. Only supports {@link SimpleStateProvider} and {@link WeightedStateProvider}
         * @return the resource description group
         */
        public ResourceDescriptionGroup ofBlockProvider(final BlockStateProvider blockStateProvider) {
            // convert weighted list of block states
            if(blockStateProvider instanceof WeightedStateProvider p) {
                return ofWeightedList(p.weightedList, b -> new ItemStack(b.getBlock().asItem()));
            }
            // convert simple block state
            if(blockStateProvider instanceof SimpleStateProvider p) {
                return ofItem(new ItemStack(p.getState(RandomSource.create(), BlockPos.ZERO).getBlock().asItem()));
            }
            throw new IllegalArgumentException("#ofBlockProvider only supports SimpleStateProvider and WeightedStateProvider, provided " + blockStateProvider.getClass());
        }

        /**
         * @return the resource description group
         */
        public ResourceDescriptionGroup build() {
            return new ResourceDescriptionGroup(builder, groupWeight, groupTotalWeight, forceShowWeight);
        }
    }
}
