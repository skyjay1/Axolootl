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
import net.minecraft.ChatFormatting;
import net.minecraft.core.HolderSet;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import javax.annotation.concurrent.Immutable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

@Immutable
public class AndResourceGenerator extends ResourceGenerator {

    public static final Codec<AndResourceGenerator> CODEC = ResourceGenerator.HOLDER_SET_CODEC
            .xmap(AndResourceGenerator::new, AndResourceGenerator::getChildren).fieldOf("values").codec();

    private final HolderSet<ResourceGenerator> children;
    private final Set<ResourceType> resourceTypes;
    private final List<Component> description;

    public AndResourceGenerator(final HolderSet<ResourceGenerator> list) {
        super(ResourceType.MULTIPLE);
        this.children = list;
        // prepare to build resource type set
        final ImmutableSet.Builder<ResourceType> typeBuilder = ImmutableSet.builder();
        typeBuilder.add(ResourceType.MULTIPLE);
        list.forEach(entry -> typeBuilder.addAll(entry.value().getResourceTypes()));
        // build resource type set
        this.resourceTypes = typeBuilder.build();
        // prepare to build description
        final List<Component> descriptionBuilder = new ArrayList<>();
        list.forEach(entry -> {
            entry.value().getDescription().forEach(c -> descriptionBuilder.add(Component.literal("  ").append(c)));
            descriptionBuilder.add(Component.translatable("axolootl.resource_generator.and.header").withStyle(ChatFormatting.GOLD));
        });
        // remove trailing component
        if(descriptionBuilder.size() > 1) {
            descriptionBuilder.remove(descriptionBuilder.size() - 1);
        }
        // build description
        this.description = ImmutableList.copyOf(descriptionBuilder);
    }

    public HolderSet<ResourceGenerator> getChildren() {
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
        getChildren().forEach(entry -> builder.addAll(entry.value().getRandomEntries(entity, random)));
        return builder.build();
    }

    @Override
    public Codec<? extends ResourceGenerator> getCodec() {
        return AxRegistry.ResourceGeneratorsReg.AND.get();
    }

    @Override
    public List<Component> getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return "And: {" + children.toString() + "}";
    }
}
