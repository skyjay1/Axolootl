/**
 * Copyright (c) 2023 Skyler James
 * Permission is granted to use, modify, and redistribute this software, in parts or in whole,
 * under the GNU LGPLv3 license (https://www.gnu.org/licenses/lgpl-3.0.en.html)
 **/

package axolootl.data.aquarium_modifier.condition;

import axolootl.AxRegistry;
import axolootl.data.aquarium_modifier.AquariumModifierContext;
import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;

import javax.annotation.concurrent.Immutable;
import java.util.List;

@Immutable
public class OrModifierCondition extends ModifierCondition {

    public static final Codec<OrModifierCondition> CODEC = LIST_OR_SINGLE_CODEC
            .xmap(OrModifierCondition::new, OrModifierCondition::getChildren).fieldOf("children").codec();

    private final List<ModifierCondition> children;

    public OrModifierCondition(List<ModifierCondition> children) {
        this.children = ImmutableList.copyOf(children);
    }

    public List<ModifierCondition> getChildren() {
        return children;
    }

    @Override
    public boolean test(AquariumModifierContext aquariumModifierContext) {
        for(ModifierCondition child : children) {
            if(child.test(aquariumModifierContext)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Codec<? extends ModifierCondition> getCodec() {
        return AxRegistry.ModifierConditionsReg.OR.get();
    }

    @Override
    public String toString() {
        return "or {" + children.toString() + "}";
    }
}
