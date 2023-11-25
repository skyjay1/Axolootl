/**
 * Copyright (c) 2023 Skyler James
 * Permission is granted to use, modify, and redistribute this software, in parts or in whole,
 * under the GNU LGPLv3 license (https://www.gnu.org/licenses/lgpl-3.0.en.html)
 **/

package axolootl.data.aquarium_modifier.condition;

import axolootl.AxRegistry;
import axolootl.data.aquarium_modifier.AquariumModifier;
import axolootl.data.aquarium_modifier.AquariumModifierContext;
import axolootl.util.AxCodecUtils;
import axolootl.util.DeferredHolderSet;
import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.critereon.MinMaxBounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;

import javax.annotation.concurrent.Immutable;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Immutable
public class CountModifierCondition extends ModifierCondition {

    public static final Codec<CountModifierCondition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            DeferredHolderSet.codec(AxRegistry.Keys.AQUARIUM_MODIFIERS).fieldOf("modifier").forGetter(CountModifierCondition::getModifiers),
            AxCodecUtils.NON_NEGATIVE_INTS_CODEC.fieldOf("count").forGetter(CountModifierCondition::getCount),
            Codec.BOOL.optionalFieldOf("active", false).forGetter(CountModifierCondition::isRequireActive)
    ).apply(instance, CountModifierCondition::new));

    private final DeferredHolderSet<AquariumModifier> modifierId;
    private final MinMaxBounds.Ints count;
    private final boolean requireActive;

    public CountModifierCondition(DeferredHolderSet<AquariumModifier> modifierId, MinMaxBounds.Ints count, boolean requireActive) {
        this.modifierId = modifierId;
        this.count = count;
        this.requireActive = requireActive;
    }

    public DeferredHolderSet<AquariumModifier> getModifiers() {
        return modifierId;
    }

    public MinMaxBounds.Ints getCount() {
        return count;
    }

    public boolean isRequireActive() {
        return requireActive;
    }

    @Override
    public boolean test(AquariumModifierContext context) {
        int count = 0;
        final Registry<AquariumModifier> registry = context.getRegistryAccess().registryOrThrow(AxRegistry.Keys.AQUARIUM_MODIFIERS);
        final HolderSet<AquariumModifier> holderSet = this.modifierId.get(registry);
        for(Map.Entry<BlockPos, AquariumModifier> entry : context.getModifiers().entrySet()) {
            // skip this entry
            if(entry.getKey().equals(context.getPos())) continue;
            // count matching modifiers
            if(holderSet.contains(entry.getValue().getHolder(context.getRegistryAccess())) && (!isRequireActive() || context.isModifierActive(entry.getKey()))) {
                count++;
            }
        }
        return getCount().matches(count);
    }

    @Override
    public Codec<? extends ModifierCondition> getCodec() {
        return AxRegistry.ModifierConditionsReg.COUNT.get();
    }

    @Override
    public List<Component> createDescription(final RegistryAccess registryAccess) {
        final Registry<AquariumModifier> registry = registryAccess.registryOrThrow(AxRegistry.Keys.AQUARIUM_MODIFIERS);
        final List<Component> modifierDescription = createHolderSetDescription(registry, modifierId.get(registry), AquariumModifier::getDescription);
        // single entry list
        if(modifierDescription.size() == 1) {
            return ImmutableList.of(Component.translatable("axolootl.modifier_condition.count.single", createIntDescription(this.count), modifierDescription.get(0)));
        }
        // multiple entry list
        ImmutableList.Builder<Component> builder = ImmutableList.builder();
        builder.add(Component.translatable("axolootl.modifier_condition.count.single", createIntDescription(this.count)));
        for(Component c : modifierDescription) {
            builder.add(Component.literal("  ").append(c));
        }
        return builder.build();
    }

    @Override
    public String toString() {
        return "count {count=(" + Optional.ofNullable(getCount().getMin()) + "," + Optional.ofNullable(getCount().getMax()) + ") modifier=" + getModifiers() + "}";
    }
}
