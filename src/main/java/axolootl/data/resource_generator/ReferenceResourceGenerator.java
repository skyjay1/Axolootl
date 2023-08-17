/**
 * Copyright (c) 2023 Skyler James
 * Permission is granted to use, modify, and redistribute this software, in parts or in whole,
 * under the GNU LGPLv3 license (https://www.gnu.org/licenses/lgpl-3.0.en.html)
 **/

package axolootl.data.resource_generator;

import axolootl.AxRegistry;
import com.mojang.serialization.Codec;
import net.minecraft.core.Holder;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public class ReferenceResourceGenerator extends ResourceGenerator {

    public static final Codec<ReferenceResourceGenerator> CODEC = HOLDER_CODEC
            .xmap(ReferenceResourceGenerator::new, ReferenceResourceGenerator::getHolder).fieldOf("id").codec();

    private final Holder<ResourceGenerator> holder;

    public ReferenceResourceGenerator(Holder<ResourceGenerator> holder) {
        super(ResourceType.OTHER);
        this.holder = holder;
    }

    public Holder<ResourceGenerator> getHolder() {
        return holder;
    }

    @Override
    public Collection<ItemStack> getRandomEntries(LivingEntity entity, RandomSource random) {
        return getHolder().value().getRandomEntries(entity, random);
    }

    @Override
    public Set<ResourceType> getResourceTypes() {
        return getHolder().value().getResourceTypes();
    }

    @Override
    protected List<ResourceDescriptionGroup> createDescription() {
        return getHolder().value().getDescription();
    }

    @Override
    public Codec<? extends ResourceGenerator> getCodec() {
        return AxRegistry.ResourceGeneratorsReg.REFERENCE.get();
    }


    @Override
    public String toString() {
        return "Reference: " + holder.unwrapKey();
    }
}
