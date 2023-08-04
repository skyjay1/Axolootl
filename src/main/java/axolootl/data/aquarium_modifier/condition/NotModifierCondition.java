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
public class NotModifierCondition extends ModifierCondition {

    public static final Codec<NotModifierCondition> CODEC = DIRECT_CODEC
            .xmap(NotModifierCondition::new, NotModifierCondition::getChild).fieldOf("child").codec();

    private final ModifierCondition child;
    private final List<Component> description;

    public NotModifierCondition(ModifierCondition child) {
        this.child = child;
        final List<Component> builder = new ArrayList<>();
        builder.add(Component.translatable("axolootl.modifier_condition.not").withStyle(ChatFormatting.BOLD, ChatFormatting.GOLD));
        for(Component c : child.getDescription()) {
            builder.add(Component.literal("  ").append(c));
        }
        this.description = ImmutableList.copyOf(builder);
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
    public List<Component> getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return "not {" + child.toString() + "}";
    }
}
