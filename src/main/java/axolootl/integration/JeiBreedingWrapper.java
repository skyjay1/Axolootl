/**
 * Copyright (c) 2023 Skyler James
 * Permission is granted to use, modify, and redistribute this software, in parts or in whole,
 * under the GNU LGPLv3 license (https://www.gnu.org/licenses/lgpl-3.0.en.html)
 **/

package axolootl.integration;

import axolootl.AxRegistry;
import axolootl.data.breeding.AxolootlBreeding;
import axolootl.data.axolootl_variant.AxolootlVariant;
import axolootl.data.resource_generator.ResourceGenerator;
import axolootl.entity.AxolootlEntity;
import axolootl.item.AxolootlBucketItem;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.random.WeightedEntry;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

public class JeiBreedingWrapper {

    private final AxolootlBreeding breeding;
    private final boolean requiresMonsterium;
    private final List<ItemStack> first;
    private final List<ItemStack> firstFood;
    private final List<ItemStack> second;
    private final List<ItemStack> secondFood;
    private final Map<ItemStack, Double> result;
    private final List<Map.Entry<ItemStack, Double>> sortedResult;

    public JeiBreedingWrapper(AxolootlBreeding breeding) {
        this.breeding = breeding;
        this.requiresMonsterium = breeding.getResult().unwrap().stream().anyMatch(wrapper -> wrapper.getData().value().hasMobResources());
        // create inputs
        this.first = ImmutableList.of(getStack(breeding.getFirst(), false));
        this.second = ImmutableList.of(getStack(breeding.getSecond(), false));
        // create foods
        this.firstFood = getFood(breeding.getFirst());
        this.secondFood = getFood(breeding.getSecond());
        // create results
        double totalWeight = ResourceGenerator.calculateTotalWeight(breeding.getResult());
        final ImmutableMap.Builder<ItemStack, Double> builder = ImmutableMap.builder();
        for(WeightedEntry.Wrapper<Holder<AxolootlVariant>> entry : breeding.getResult().unwrap()) {
            builder.put(getStack(entry.getData(), true), entry.getWeight().asInt() / totalWeight);
        }
        this.result = builder.build();
        // create sorted results
        final List<Map.Entry<ItemStack, Double>> list = new ArrayList<>(getResult().entrySet().stream().toList());
        final Comparator<Map.Entry<ItemStack, Double>> comparator = Map.Entry.comparingByValue();
        list.sort(comparator.reversed());
        this.sortedResult = ImmutableList.copyOf(list);
    }

    public AxolootlBreeding getBreeding() {
        return breeding;
    }

    public boolean requiresMonsterium() {
        return requiresMonsterium;
    }

    public List<ItemStack> getFirst() {
        return first;
    }

    public List<ItemStack> getSecond() {
        return second;
    }

    public List<ItemStack> getFirstFood() {
        return firstFood;
    }

    public List<ItemStack> getSecondFood() {
        return secondFood;
    }

    public Map<ItemStack, Double> getResult() {
        return result;
    }

    public List<Map.Entry<ItemStack, Double>> getSortedResult() {
        return this.sortedResult;
    }

    /**
     * @param holders a holder set
     * @return all breeding items for the given axolootl variant(s)
     */
    private static List<ItemStack> getFoods(final HolderSet<AxolootlVariant> holders) {
        final ImmutableList.Builder<ItemStack> builder = ImmutableList.builder();
        for(Holder<AxolootlVariant> holder : holders) {
            builder.addAll(holder.get().getBreedFood().stream().map(h -> h.get().getDefaultInstance()).toList());
        }
        return builder.build();
    }

    /**
     * @param holder a holder
     * @return all breeding items for the given axolootl variant(s)
     */
    private static List<ItemStack> getFood(final Holder<AxolootlVariant> holder) {
        return ImmutableList.copyOf(holder.get().getBreedFood().stream().map(h -> h.get().getDefaultInstance()).toList());
    }

    /**
     * @param holder a holder set
     * @return item stack representations of all the axolootl variants in the holder set
     */
    private static List<ItemStack> getStacks(final HolderSet<AxolootlVariant> holder) {
        final List<Holder<AxolootlVariant>> variants = holder.unwrap()
                .map(tagKey -> AxRegistry.AXOLOOTL_VARIANTS_SUPPLIER.get().tags().getTag(tagKey).stream()
                        .map(a -> a.getHolder(JeiAddon.getRegistryAccess())).toList(), Function.identity());
        final ImmutableList.Builder<ItemStack> builder = ImmutableList.builder();
        for(Holder<AxolootlVariant> entry : variants) {
            builder.add(getStack(entry, true));
        }
        return builder.build();
    }

    /**
     * @param holder a holder
     * @param baby true to create a stack with a baby entity
     * @return item stack representations of the axolootl variants for the holder
     */
    private static ItemStack getStack(final Holder<AxolootlVariant> holder, final boolean baby) {
        final Optional<ResourceKey<AxolootlVariant>> oKey = holder.unwrapKey();
        if(oKey.isEmpty()) {
            return getStack(holder.get(), baby);
        }
        return getStack(oKey.get().location(), baby);
    }

    /**
     * @param variant the axolootl variant
     * @param baby true to create a stack with a baby entity
     * @return item stack representations of the axolootl variant
     */
    private static ItemStack getStack(final AxolootlVariant variant, final boolean baby) {
        return getStack(variant.getRegistryName(JeiAddon.getRegistryAccess()), baby);
    }

    /**
     * @param key the axolootl variant ID
     * @param baby true to create a stack with a baby entity
     * @return item stack representations of the axolootl variant with the given ID
     */
    private static ItemStack getStack(final ResourceLocation key, final boolean baby) {
        ItemStack stack = AxolootlBucketItem.getWithVariant(new ItemStack(AxRegistry.ItemReg.AXOLOOTL_BUCKET.get()), key);
        if(baby) {
            stack.getOrCreateTag().putInt(AxolootlEntity.KEY_AGE, AxolootlEntity.BABY_AGE);
        }
        return stack;
    }
}
