/**
 * Copyright (c) 2023 Skyler James
 * Permission is granted to use, modify, and redistribute this software, in parts or in whole,
 * under the GNU LGPLv3 license (https://www.gnu.org/licenses/lgpl-3.0.en.html)
 **/

package axolootl.data.resource_generator;

import axolootl.AxRegistry;
import axolootl.util.AxCodecUtils;
import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.mojang.serialization.Codec;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import javax.annotation.concurrent.Immutable;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

@Immutable
public class AndResourceGenerator extends ResourceGenerator {

    public static final Codec<AndResourceGenerator> CODEC = AxCodecUtils.listOrElementCodec(ResourceGenerator.HOLDER_CODEC)
            .xmap(AndResourceGenerator::new, AndResourceGenerator::getChildren).fieldOf("values").codec();

    private final List<Holder<ResourceGenerator>> children;
    private final Supplier<Set<ResourceType>> resourceTypes;

    public AndResourceGenerator(final List<Holder<ResourceGenerator>> list) {
        super();
        this.children = list;
        this.resourceTypes = Suppliers.memoize(() -> {
            // prepare to build resource type set
            final ImmutableSet.Builder<ResourceType> typeBuilder = ImmutableSet.builder();
            typeBuilder.add(ResourceTypes.MULTIPLE);
            getChildren().forEach(entry -> typeBuilder.addAll(entry.value().getResourceTypes()));
            // build resource type set
            return typeBuilder.build();
        });
    }

    public List<Holder<ResourceGenerator>> getChildren() {
        return children;
    }

    @Override
    public Set<ResourceType> getResourceTypes() {
        return this.resourceTypes.get();
    }

    @Override
    public Collection<ItemStack> getRandomEntries(final LivingEntity entity, RandomSource random) {
        // roll each list
        final ImmutableList.Builder<ItemStack> builder = ImmutableList.builder();
        getChildren().forEach(entry -> builder.addAll(entry.value().getRandomEntries(entity, random)));
        return builder.build();
    }

    @Override
    public Codec<? extends ResourceGenerator> getCodec() {
        return AxRegistry.ResourceGeneratorsReg.AND.get();
    }

    @Override
    protected List<ResourceDescriptionGroup> createDescription() {
        final ImmutableList.Builder<ResourceDescriptionGroup> builder = ImmutableList.builder();
        // iterate all children
        for(Holder<ResourceGenerator> entry : this.getChildren()) {
            // iterate descriptions
            for(ResourceDescriptionGroup description : entry.value().getDescription()) {
                // add a copy of the description but always show the weight of the group
                builder.add(ResourceDescriptionGroup
                        .builder(description, 1, 1)
                        .forceShowWeight().build()
                );
            }
        }
        return builder.build();
    }

    @Override
    public String toString() {
        return "And: {" + children.toString() + "}";
    }
}
