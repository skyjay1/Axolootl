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
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import javax.annotation.concurrent.Immutable;
import java.util.Collection;
import java.util.List;
import java.util.Set;

@Immutable
public class AndResourceGenerator extends ResourceGenerator {

    public static final Codec<AndResourceGenerator> CODEC = ResourceGenerator.DIRECT_LIST_CODEC
            .xmap(AndResourceGenerator::new, AndResourceGenerator::getChildren).fieldOf("values").codec();

    private final List<ResourceGenerator> children;
    private final Set<ResourceType> resourceTypes;

    public AndResourceGenerator(final List<ResourceGenerator> list) {
        super(ResourceType.MULTIPLE);
        this.children = ImmutableList.copyOf(list);
        final ImmutableSet.Builder<ResourceType> builder = ImmutableSet.builder();
        builder.add(getResourceType());
        list.forEach(gen -> builder.addAll(gen.getResourceTypes()));
        this.resourceTypes = builder.build();
    }

    public List<ResourceGenerator> getChildren() {
        return children;
    }

    @Override
    public Set<ResourceType> getResourceTypes() {
        return this.resourceTypes;
    }

    @Override
    public Collection<ItemStack> getRandomEntries(final LivingEntity entity, RandomSource random) {
        // roll each list
        final ImmutableList.Builder<ItemStack> builder = ImmutableList.builder();
        for(ResourceGenerator generator : children) {
            builder.addAll(generator.getRandomEntries(entity, random));
        }
        return builder.build();
    }

    @Override
    public Codec<? extends ResourceGenerator> getCodec() {
        return AxRegistry.ResourceGeneratorsReg.AND.get();
    }

    @Override
    public String toString() {
        return "And: {" + children.toString() + "}";
    }
}
