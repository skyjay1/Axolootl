/**
 * Copyright (c) 2023 Skyler James
 * Permission is granted to use, modify, and redistribute this software, in parts or in whole,
 * under the GNU LGPLv3 license (https://www.gnu.org/licenses/lgpl-3.0.en.html)
 **/

package axolootl.data.aquarium_modifier.condition;

import axolootl.AxRegistry;
import axolootl.entity.IAxolootl;
import axolootl.data.axolootl_variant.AxolootlVariant;
import axolootl.data.aquarium_modifier.AquariumModifierContext;
import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.network.chat.Component;
import net.minecraft.util.valueproviders.IntProvider;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import java.util.List;
import java.util.Optional;

@Immutable
public class AxolootlCountModifierCondition extends ModifierCondition {

    public static final Codec<AxolootlCountModifierCondition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            AxolootlVariant.HOLDER_SET_CODEC.optionalFieldOf("variants").forGetter(AxolootlCountModifierCondition::getVariant),
            IntProvider.NON_NEGATIVE_CODEC.fieldOf("count").forGetter(AxolootlCountModifierCondition::getCount)
    ).apply(instance, AxolootlCountModifierCondition::new));

    @Nullable
    private final HolderSet<AxolootlVariant> variant;
    private final IntProvider count;
    private final List<Component> description;

    public AxolootlCountModifierCondition(Optional<HolderSet<AxolootlVariant>> variant, IntProvider count) {
        this.variant = variant.orElse(null);
        this.count = count;
        if(variant.isPresent()) {
            this.description = createCountedDescription("axolootl.modifier_condition.axolootl_count", count, variant.get(), AxolootlVariant::getDescription);
        } else if(count.getMinValue() == count.getMaxValue() && count.getMaxValue() == 1) {
            this.description = ImmutableList.of(Component.translatable("axolootl.modifier_condition.axolootl_count.any.single", createIntDescription(count)));
        } else {
            this.description = ImmutableList.of(Component.translatable("axolootl.modifier_condition.axolootl_count.any.multiple", createIntDescription(count)));
        }
    }

    public IntProvider getCount() {
        return count;
    }

    public Optional<HolderSet<AxolootlVariant>> getVariant() {
        return Optional.ofNullable(variant);
    }

    @Override
    public boolean test(AquariumModifierContext context) {
        int count = 0;
        // count all axolootls of the defined variant, or count all of them if no variant is defined
        if(this.variant != null) {
            for(IAxolootl entry : context.getAxolootls()) {
                Optional<AxolootlVariant> oVariant = entry.getAxolootlVariant(context.getRegistryAccess());
                if(oVariant.isPresent() && variant.contains(new Holder.Direct<>(oVariant.get()))) {
                    count++;
                }
            }
        } else {
            count = context.getAxolootls().size();
        }
        // verify count is within range
        return count >= getCount().getMinValue() && count <= getCount().getMaxValue();
    }

    @Override
    public Codec<? extends ModifierCondition> getCodec() {
        return AxRegistry.ModifierConditionsReg.AXOLOOTL_COUNT.get();
    }

    @Override
    public List<Component> getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return "axolootl_count {count=(" + getCount().getMinValue() + "," + getCount().getMaxValue() + ") variant=" + getVariant().toString() + "}";
    }
}
