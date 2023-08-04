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
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import javax.annotation.concurrent.Immutable;
import java.util.ArrayList;
import java.util.List;

@Immutable
public class AndModifierCondition extends ModifierCondition {

    public static final Codec<AndModifierCondition> CODEC = LIST_OR_SINGLE_CODEC
            .xmap(AndModifierCondition::new, AndModifierCondition::getChildren).fieldOf("children").codec();

    private final List<ModifierCondition> children;
    private final List<Component> description;

    public AndModifierCondition(List<ModifierCondition> children) {
        this.children = ImmutableList.copyOf(children);
        final List<Component> builder = new ArrayList<>();
        for(ModifierCondition child : children) {
            for(Component c : child.getDescription()) {
                builder.add(Component.literal("  ").append(c));
                builder.add(Component.translatable("axolootl.modifier_condition.and").withStyle(ChatFormatting.GOLD));
            }
        }
        // remove trailing entry
        if(!builder.isEmpty()) {
            builder.remove(builder.size() - 1);
        }
        this.description = ImmutableList.copyOf(builder);
    }

    public List<ModifierCondition> getChildren() {
        return children;
    }

    @Override
    public boolean test(AquariumModifierContext aquariumModifierContext) {
        for(ModifierCondition child : children) {
            if(!child.test(aquariumModifierContext)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public Codec<? extends ModifierCondition> getCodec() {
        return AxRegistry.ModifierConditionsReg.AND.get();
    }

    @Override
    public List<Component> getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return "and {" + children.toString() + "}";
    }
}
