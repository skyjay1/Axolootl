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
        return AxRegistry.ModifierConditions.NOT.get();
    }

    @Override
    public String toString() {
        return "not {" + child.toString() + "}";
    }
}
