/**
 * Copyright (c) 2023 Skyler James
 * Permission is granted to use, modify, and redistribute this software, in parts or in whole,
 * under the GNU LGPLv3 license (https://www.gnu.org/licenses/lgpl-3.0.en.html)
 **/

package axolootl.integration;

import axolootl.AxRegistry;
import axolootl.data.axolootl_variant.AxolootlVariant;
import axolootl.data.breeding.AxolootlBreedingWrapper;
import axolootl.data.resource_generator.ResourceGenerator;
import axolootl.entity.AxolootlEntity;
import axolootl.item.AxolootlBucketItem;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.random.WeightedEntry;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class JeiBreedingRecipe {

    private final AxolootlBreedingWrapper wrapper;
    private final boolean requiresMonsterium;
    private final List<ItemStack> first;
    private final List<ItemStack> firstFood;
    private final List<ItemStack> second;
    private final List<ItemStack> secondFood;
    private final Map<ItemStack, Double> result;
    private final List<Map.Entry<ItemStack, Double>> sortedResult;

    public JeiBreedingRecipe(AxolootlBreedingWrapper wrapper) {
        final RegistryAccess access = JeiAddon.getRegistryAccess();
        final Registry<AxolootlVariant> variants = AxolootlVariant.getRegistry(access);
        this.wrapper = wrapper;
        this.requiresMonsterium = wrapper.getResult().unwrap().stream().anyMatch(w -> variants.get(w.getData().location()).hasMobResources());
        // create inputs
        this.first = ImmutableList.of(getStack(wrapper.getBreeding().getFirst(), false));
        this.second = ImmutableList.of(getStack(wrapper.getBreeding().getSecond(), false));
        // create foods
        this.firstFood = getFood(wrapper.getBreeding().getFirst(access));
        this.secondFood = getFood(wrapper.getBreeding().getSecond(access));
        // create results
        double totalWeight = ResourceGenerator.calculateTotalWeight(wrapper.getResult());
        final ImmutableMap.Builder<ItemStack, Double> builder = ImmutableMap.builder();
        for(WeightedEntry.Wrapper<ResourceKey<AxolootlVariant>> entry : wrapper.getResult().unwrap()) {
            builder.put(getStack(entry.getData(), true), entry.getWeight().asInt() / totalWeight);
        }
        this.result = builder.build();
        // create sorted results
        final List<Map.Entry<ItemStack, Double>> list = new ArrayList<>(getResult().entrySet().stream().toList());
        final Comparator<Map.Entry<ItemStack, Double>> comparator = Map.Entry.comparingByValue();
        list.sort(comparator.reversed());
        this.sortedResult = ImmutableList.copyOf(list);
    }

    public AxolootlBreedingWrapper getWrapper() {
        return wrapper;
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
     * @param variant the axolootl variant
     * @return all breeding items for the given axolootl variant(s)
     */
    private static List<ItemStack> getFood(final AxolootlVariant variant) {
        return ImmutableList.copyOf(variant.getBreedFood().get(BuiltInRegistries.ITEM).stream().map(h -> h.get().getDefaultInstance()).toList());
    }

    /**
     * @param key the axolootl variant resource key
     * @param baby true to create a stack with a baby entity
     * @return item stack representations of the axolootl variants for the holder
     */
    private static ItemStack getStack(final ResourceKey<AxolootlVariant> key, final boolean baby) {
        return getStack(key.location(), baby);
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
