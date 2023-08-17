/**
 * Copyright (c) 2023 Skyler James
 * Permission is granted to use, modify, and redistribute this software, in parts or in whole,
 * under the GNU LGPLv3 license (https://www.gnu.org/licenses/lgpl-3.0.en.html)
 **/

package axolootl.data.resource_generator;

import axolootl.AxRegistry;
import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import net.minecraft.util.RandomSource;
import net.minecraft.util.random.SimpleWeightedRandomList;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public class ItemResourceGenerator extends ResourceGenerator {

    private static final Codec<SimpleWeightedRandomList<ItemStack>> WEIGHTED_LIST_CODEC = Codec.either(ITEM_OR_STACK_CODEC, SimpleWeightedRandomList.wrappedCodecAllowingEmpty(ITEM_OR_STACK_CODEC))
            .xmap(either -> either.map(SimpleWeightedRandomList::single, Function.identity()),
                    list -> list.unwrap().size() == 1 ? Either.left(list.unwrap().get(0).getData()) : Either.right(list));

    public static final Codec<ItemResourceGenerator> CODEC = WEIGHTED_LIST_CODEC
            .xmap(ItemResourceGenerator::new, ItemResourceGenerator::getList).fieldOf("item").codec();

    private final SimpleWeightedRandomList<ItemStack> list;

    public ItemResourceGenerator(final SimpleWeightedRandomList<ItemStack> list) {
        super(ResourceType.ITEM);
        this.list = list;
    }

    public SimpleWeightedRandomList<ItemStack> getList() {
        return list;
    }

    @Override
    public Collection<ItemStack> getRandomEntries(final LivingEntity entity, RandomSource random) {
        // sample the item list
        final Optional<ItemStack> sample = list.getRandomValue(random);
        if (sample.isEmpty()) {
            return ImmutableList.of();
        }
        return ImmutableList.of(sample.get().copy());
    }

    @Override
    public Codec<? extends ResourceGenerator> getCodec() {
        return AxRegistry.ResourceGeneratorsReg.ITEM.get();
    }

    @Override
    public List<ResourceDescriptionGroup> createDescription() {
        return ImmutableList.of(ResourceDescriptionGroup
                .builder()
                .ofWeightedList(list)
        );
    }

    @Override
    public String toString() {
        return "Item: " + list.toString();
    }
}
