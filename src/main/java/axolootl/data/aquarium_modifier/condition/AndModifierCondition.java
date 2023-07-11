package axolootl.data.aquarium_modifier.condition;

import axolootl.AxRegistry;
import axolootl.data.aquarium_modifier.AquariumModifierContext;
import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;

import javax.annotation.concurrent.Immutable;
import java.util.List;

@Immutable
public class AndModifierCondition extends ModifierCondition {

    public static final Codec<AndModifierCondition> CODEC = LIST_OR_SINGLE_CODEC
            .xmap(AndModifierCondition::new, AndModifierCondition::getChildren).fieldOf("children").codec();

    private final List<ModifierCondition> children;

    public AndModifierCondition(List<ModifierCondition> children) {
        this.children = ImmutableList.copyOf(children);
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
        return AxRegistry.ModifierConditions.AND.get();
    }

    @Override
    public String toString() {
        return "and {" + children.toString() + "}";
    }
}
