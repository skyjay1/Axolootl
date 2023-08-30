/**
 * Copyright (c) 2023 Skyler James
 * Permission is granted to use, modify, and redistribute this software, in parts or in whole,
 * under the GNU LGPLv3 license (https://www.gnu.org/licenses/lgpl-3.0.en.html)
 **/

package axolootl.data.aquarium_modifier.condition;

import axolootl.AxRegistry;
import axolootl.data.aquarium_modifier.AquariumModifier;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.critereon.MinMaxBounds;
import net.minecraft.core.HolderSet;

import javax.annotation.concurrent.Immutable;

@Immutable
public class ExistsModifierCondition extends CountModifierCondition {

    public static final Codec<ExistsModifierCondition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            AquariumModifier.HOLDER_SET_CODEC.fieldOf("modifier").forGetter(CountModifierCondition::getModifiers),
            Codec.BOOL.optionalFieldOf("active", false).forGetter(CountModifierCondition::isRequireActive)
    ).apply(instance, ExistsModifierCondition::new));

    public ExistsModifierCondition(HolderSet<AquariumModifier> modifiers, boolean requireActive) {
        super(modifiers, MinMaxBounds.Ints.atLeast(1), requireActive);
    }

    @Override
    public Codec<? extends ModifierCondition> getCodec() {
        return AxRegistry.ModifierConditionsReg.EXISTS.get();
    }
}
