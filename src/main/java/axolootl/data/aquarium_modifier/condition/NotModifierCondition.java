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
public class NotModifierCondition extends ModifierCondition {

    public static final Codec<NotModifierCondition> CODEC = DIRECT_CODEC
            .xmap(NotModifierCondition::new, NotModifierCondition::getChild).fieldOf("child").codec();

    private final ModifierCondition child;

    public NotModifierCondition(ModifierCondition child) {
        this.child = child;
    }

    public ModifierCondition getChild() {
        return child;
    }

    @Override
    public boolean test(AquariumModifierContext aquariumModifierContext) {
        return !child.test(aquariumModifierContext);
    }

    @Override
    public Codec<? extends ModifierCondition> getCodec() {
        return AxRegistry.ModifierConditionsReg.NOT.get();
    }

    @Override
    public String toString() {
        return "not {" + child.toString() + "}";
    }
}
