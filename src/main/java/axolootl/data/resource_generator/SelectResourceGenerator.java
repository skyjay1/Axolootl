/**
 * Copyright (c) 2023 Skyler James
 * Permission is granted to use, modify, and redistribute this software, in parts or in whole,
 * under the GNU LGPLv3 license (https://www.gnu.org/licenses/lgpl-3.0.en.html)
 **/

package axolootl.data.resource_generator;

import axolootl.AxRegistry;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.util.random.SimpleWeightedRandomList;
import net.minecraft.util.random.WeightedEntry;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import javax.annotation.concurrent.Immutable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class SelectResourceGenerator extends ResourceGenerator {

    public static final Codec<SelectResourceGenerator> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            SimpleWeightedRandomList.wrappedCodec(ResourceGenerator.HOLDER_CODEC).fieldOf("pool").forGetter(SelectResourceGenerator::getChildren),
            IntProvider.NON_NEGATIVE_CODEC.optionalFieldOf("rolls", ConstantInt.of(1)).forGetter(SelectResourceGenerator::getRolls)
    ).apply(instance, SelectResourceGenerator::new));

    private final SimpleWeightedRandomList<Holder<ResourceGenerator>> children;
    private final IntProvider rolls;
    private final Set<ResourceType> resourceTypes;

    public SelectResourceGenerator(final SimpleWeightedRandomList<Holder<ResourceGenerator>> list, final IntProvider rolls) {
        super(ResourceType.MULTIPLE);
        this.children = list;
        this.rolls = rolls;
        // prepare to build resource type set
        final ImmutableSet.Builder<ResourceType> typeBuilder = ImmutableSet.builder();
        typeBuilder.add(ResourceType.MULTIPLE);
        list.unwrap().forEach(entry -> typeBuilder.addAll(entry.getData().value().getResourceTypes()));
        // build resource type set
        this.resourceTypes = typeBuilder.build();
    }

    public SimpleWeightedRandomList<Holder<ResourceGenerator>> getChildren() {
        return children;
    }

    public IntProvider getRolls() {
        return rolls;
    }

    @Override
    public Set<ResourceType> getResourceTypes() {
        return this.resourceTypes;
    }

    @Override
    public Collection<ItemStack> getRandomEntries(final LivingEntity entity, RandomSource random) {
        // roll each list
        final ImmutableList.Builder<ItemStack> builder = ImmutableList.builder();
        for(int i = 0, n = getRolls().sample(random); i < n; i++) {
            final Optional<WeightedEntry.Wrapper<Holder<ResourceGenerator>>> oGenerator = children.getRandom(random);
            if (oGenerator.isEmpty()) continue;
            builder.addAll(oGenerator.get().getData().value().getRandomEntries(entity, random));
        }
        return builder.build();
    }

    @Override
    public Codec<? extends ResourceGenerator> getCodec() {
        return AxRegistry.ResourceGeneratorsReg.SELECT.get();
    }

    @Override
    public List<Component> createDescription() {
        return createDescription(getChildren());
    }

    @Override
    public String toString() {
        return "Select: {" + children.toString() + "}";
    }
}
