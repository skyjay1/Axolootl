package axolootl.data.aquarium_modifier.condition;

import axolootl.AxRegistry;
import axolootl.data.aquarium_modifier.AquariumModifierContext;
import com.mojang.serialization.Codec;

import javax.annotation.concurrent.Immutable;

@Immutable
public class TrueModifierCondition extends ModifierCondition {

    public static final TrueModifierCondition INSTANCE = new TrueModifierCondition();

    public static final Codec<TrueModifierCondition> CODEC = Codec.unit(INSTANCE);

    @Override
    public boolean test(AquariumModifierContext aquariumModifierContext) {
        return true;
    }

    @Override
    public Codec<? extends ModifierCondition> getCodec() {
        return AxRegistry.ModifierConditions.TRUE.get();
    }

    @Override
    public String toString() {
        return "true";
    }
}
