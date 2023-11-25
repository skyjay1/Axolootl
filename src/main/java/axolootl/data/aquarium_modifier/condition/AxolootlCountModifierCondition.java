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
import axolootl.util.AxCodecUtils;
import axolootl.util.DeferredHolderSet;
import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.critereon.MinMaxBounds;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import java.util.List;
import java.util.Optional;

@Immutable
public class AxolootlCountModifierCondition extends ModifierCondition {

    public static final Codec<AxolootlCountModifierCondition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            DeferredHolderSet.codec(AxRegistry.Keys.AXOLOOTL_VARIANTS).optionalFieldOf("variant").forGetter(AxolootlCountModifierCondition::getVariant),
            AxCodecUtils.NON_NEGATIVE_INTS_CODEC.fieldOf("count").forGetter(AxolootlCountModifierCondition::getCount)
    ).apply(instance, AxolootlCountModifierCondition::new));

    @Nullable
    private final DeferredHolderSet<AxolootlVariant> variant;
    private final MinMaxBounds.Ints count;

    public AxolootlCountModifierCondition(Optional<DeferredHolderSet<AxolootlVariant>> variant, MinMaxBounds.Ints count) {
        this.variant = variant.orElse(null);
        this.count = count;
    }

    public MinMaxBounds.Ints getCount() {
        return count;
    }

    public Optional<DeferredHolderSet<AxolootlVariant>> getVariant() {
        return Optional.ofNullable(variant);
    }

    @Override
    public boolean test(AquariumModifierContext context) {
        int count = 0;
        // count all axolootls of the defined variant, or count all of them if no variant is defined
        if(this.variant != null) {
            final Registry<AxolootlVariant> registry = context.getRegistryAccess().registryOrThrow(AxRegistry.Keys.AXOLOOTL_VARIANTS);
            final HolderSet<AxolootlVariant> holderSet = variant.get(registry);
            for(IAxolootl entry : context.getAxolootls()) {
                Optional<AxolootlVariant> oVariant = entry.getAxolootlVariant(context.getRegistryAccess());
                if(oVariant.isPresent() && holderSet.contains(oVariant.get().getHolder(context.getRegistryAccess()))) {
                    count++;
                }
            }
        } else {
            count = context.getAxolootls().size();
        }
        // verify count is within range
        return getCount().matches(count);
    }

    @Override
    public Codec<? extends ModifierCondition> getCodec() {
        return AxRegistry.ModifierConditionsReg.AXOLOOTL_COUNT.get();
    }

    @Override
    public List<Component> createDescription(final RegistryAccess registryAccess) {
        final Registry<AxolootlVariant> registry = registryAccess.registryOrThrow(AxRegistry.Keys.AXOLOOTL_VARIANTS);
        if(variant != null) {
            return createCountedDescription("axolootl.modifier_condition.axolootl_count", count, registry, variant.get(registry), AxolootlVariant::getDescription);
        } else if(count.getMin() != null && count.getMax() != null && count.getMin().equals(count.getMax()) && count.getMin() == 1) {
            return ImmutableList.of(Component.translatable("axolootl.modifier_condition.axolootl_count.any.single", createIntDescription(count)));
        } else {
            return ImmutableList.of(Component.translatable("axolootl.modifier_condition.axolootl_count.any.multiple", createIntDescription(count)));
        }
    }

    @Override
    public String toString() {
        return "axolootl_count {count=(" + Optional.ofNullable(getCount().getMin()) + "," + Optional.ofNullable(getCount().getMax()) + ") variant=" + getVariant().toString() + "}";
    }
}
