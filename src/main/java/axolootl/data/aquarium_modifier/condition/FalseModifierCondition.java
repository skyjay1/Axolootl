/**
 * Copyright (c) 2023 Skyler James
 * Permission is granted to use, modify, and redistribute this software, in parts or in whole,
 * under the GNU LGPLv3 license (https://www.gnu.org/licenses/lgpl-3.0.en.html)
 **/

package axolootl.data.aquarium_modifier.condition;

import axolootl.AxRegistry;
import axolootl.data.aquarium_modifier.AquariumModifierContext;
import com.mojang.serialization.Codec;

import javax.annotation.concurrent.Immutable;

@Immutable
public class FalseModifierCondition extends ModifierCondition {

    public static final FalseModifierCondition INSTANCE = new FalseModifierCondition();

    public static final Codec<FalseModifierCondition> CODEC = Codec.unit(INSTANCE);

    @Override
    public boolean test(AquariumModifierContext aquariumModifierContext) {
        return false;
    }

    @Override
    public Codec<? extends ModifierCondition> getCodec() {
        return AxRegistry.ModifierConditionsReg.FALSE.get();
    }

    @Override
    public String toString() {
        return "false";
    }
}
