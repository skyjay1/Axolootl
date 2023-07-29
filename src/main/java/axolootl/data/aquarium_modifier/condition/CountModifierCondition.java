/**
 * Copyright (c) 2023 Skyler James
 * Permission is granted to use, modify, and redistribute this software, in parts or in whole,
 * under the GNU LGPLv3 license (https://www.gnu.org/licenses/lgpl-3.0.en.html)
 **/

package axolootl.data.aquarium_modifier.condition;

import axolootl.AxRegistry;
import axolootl.data.aquarium_modifier.AquariumModifier;
import axolootl.data.aquarium_modifier.AquariumModifierContext;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderSet;
import net.minecraft.util.valueproviders.IntProvider;

import javax.annotation.concurrent.Immutable;
import java.util.Map;

@Immutable
public class CountModifierCondition extends ModifierCondition {

    public static final Codec<CountModifierCondition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            AquariumModifier.HOLDER_SET_CODEC.fieldOf("modifier").forGetter(CountModifierCondition::getModifiers),
            IntProvider.NON_NEGATIVE_CODEC.fieldOf("count").forGetter(CountModifierCondition::getCount),
            Codec.BOOL.optionalFieldOf("active", false).forGetter(CountModifierCondition::isRequireActive)
    ).apply(instance, CountModifierCondition::new));

    private final HolderSet<AquariumModifier> modifierId;
    private final IntProvider count;
    private final boolean requireActive;

    public CountModifierCondition(HolderSet<AquariumModifier> modifierId, IntProvider count, boolean requireActive) {
        this.modifierId = modifierId;
        this.count = count;
        this.requireActive = requireActive;
    }

    public HolderSet<AquariumModifier> getModifiers() {
        return modifierId;
    }

    public IntProvider getCount() {
        return count;
    }

    public boolean isRequireActive() {
        return requireActive;
    }

    @Override
    public boolean test(AquariumModifierContext context) {
        int count = 0;
        for(Map.Entry<BlockPos, AquariumModifier> entry : context.getModifiers().entrySet()) {
            if(modifierId.contains(entry.getValue().getHolder(context.getRegistryAccess())) && (!isRequireActive() || context.isModifierActive(entry.getKey()))) {
                count++;
            }
        }
        return count >= getCount().getMinValue() && count <= getCount().getMaxValue();
    }

    @Override
    public Codec<? extends ModifierCondition> getCodec() {
        return AxRegistry.ModifierConditionsReg.COUNT.get();
    }

    @Override
    public String toString() {
        return "count {count=(" + getCount().getMinValue() + "," + getCount().getMaxValue() + ") modifier=" + getModifiers() + "}";
    }
}
