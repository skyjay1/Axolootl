package axolootl.data.resource_generator;

import com.google.common.collect.ImmutableList;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.util.random.WeightedEntry;
import net.minecraft.util.random.WeightedRandomList;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.feature.stateproviders.WeightedStateProvider;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.tags.ITag;

import javax.annotation.concurrent.Immutable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

@Immutable
public class WrappedDescription {

    private final List<ItemStack> icons;
    private final Component description;

    public WrappedDescription(List<ItemStack> icons, Component description) {
        this.icons = ImmutableList.copyOf(icons);
        this.description = description;
    }

    //// GETTERS ////

    public List<ItemStack> getIcons() {
        return icons;
    }

    public Component getDescription() {
        return description;
    }

    //// HELPER METHODS ////

    /**
     * @param itemStack an item stack
     * @return a wrapped description for the given item stack
     */
    public static WrappedDescription of(final ItemStack itemStack) {
        return new WrappedDescription(ImmutableList.of(itemStack), ResourceGenerator.getItemDisplayName(itemStack));
    }

    /**
     * @param itemStack the item stack
     * @param weight the entry weight
     * @param totalWeight the total weight of all entries
     * @return a wrapped description for the given item stack and weights
     */
    public static WrappedDescription of(final ItemStack itemStack, final double weight, final double totalWeight) {
        return new WrappedDescription(ImmutableList.of(itemStack), ResourceGenerator.createChanceDescription(itemStack, weight, totalWeight, ResourceGenerator::getItemDisplayName));
    }

    /**
     * Creates a wrapped description for each entry in the list
     * @param list a weighted random list of item stacks
     * @return a list of wrapped descriptions for each entry in the given list
     */
    public static List<WrappedDescription> of(final WeightedRandomList<WeightedEntry.Wrapper<ItemStack>> list) {
        return of(list, Function.identity());
    }

    /**
     * Creates a wrapped description for each entry in the list
     * @param list a weighted random list of item stacks
     * @param toItemStack a function to convert the list entries to item stacks
     * @param <T> an object that can be converted to an item stack
     * @return a list of wrapped descriptions for each entry in the given list
     */
    public static <T> List<WrappedDescription> of(final WeightedRandomList<WeightedEntry.Wrapper<T>> list, Function<T, ItemStack> toItemStack) {
        final List<WrappedDescription> builder = new ArrayList<>();
        final double totalWeight = ResourceGenerator.calculateTotalWeight(list);
        for (WeightedEntry.Wrapper<T> entry : list.unwrap()) {
            final ItemStack itemStack = toItemStack.apply(entry.getData());
            builder.add(new WrappedDescription(ImmutableList.of(itemStack), ResourceGenerator.createChanceDescription(itemStack, entry.getWeight().asInt(), totalWeight, ResourceGenerator::getItemDisplayName)));
        }
        return builder;
    }

    /**
     * Creates a wrapped description for each value in the tag
     * @param tagKey the tag key
     * @return a list of wrapped descriptions for each value in the given item tag
     */
    public static List<WrappedDescription> of(final TagKey<Item> tagKey) {
        final List<WrappedDescription> builder = new ArrayList<>();
        final ITag<Item> tag = ForgeRegistries.ITEMS.tags().getTag(tagKey);
        final double size = tag.size();
        // create singleton list
        if (size == 1) {
            return ImmutableList.of(of(new ItemStack(tag.iterator().next())));
        }
        // create weighted chance list
        for (Item item : tag) {
            builder.add(of(new ItemStack(item), 1.0D, size));
        }
        return builder;
    }

    public static List<WrappedDescription> of(BlockStateProvider blockStateProvider) {
        // convert weighted list of block states
        if(blockStateProvider instanceof WeightedStateProvider p) {
            return of(p.weightedList, b -> new ItemStack(b.getBlock().asItem()));
        }
        // convert the first randomly selected state in the provider
        return ImmutableList.of(of(new ItemStack(blockStateProvider.getState(RandomSource.create(), BlockPos.ZERO).getBlock().asItem())));
    }
}
