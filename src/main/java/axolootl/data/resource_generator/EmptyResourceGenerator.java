/**
 * Copyright (c) 2023 Skyler James
 * Permission is granted to use, modify, and redistribute this software, in parts or in whole,
 * under the GNU LGPLv3 license (https://www.gnu.org/licenses/lgpl-3.0.en.html)
 **/

package axolootl.data.resource_generator;

import axolootl.AxRegistry;
import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import javax.annotation.concurrent.Immutable;
import java.util.Collection;
import java.util.List;

public class EmptyResourceGenerator extends SimpleResourceGenerator {

    public static final EmptyResourceGenerator INSTANCE = new EmptyResourceGenerator();

    public static final Codec<EmptyResourceGenerator> CODEC = Codec.unit(INSTANCE);

    public EmptyResourceGenerator() {
        super(ResourceTypes.EMPTY);
    }

    @Override
    public Collection<ItemStack> getRandomEntries(final LivingEntity entity, RandomSource random) {
        return ImmutableList.of();
    }

    @Override
    public Codec<? extends ResourceGenerator> getCodec() {
        return AxRegistry.ResourceGeneratorsReg.EMPTY.get();
    }

    @Override
    protected List<ResourceDescriptionGroup> createDescription() {
        return ImmutableList.of(ResourceDescriptionGroup.builder().ofItem(ItemStack.EMPTY));
    }

    @Override
    public String toString() {
        return "Empty";
    }
}
