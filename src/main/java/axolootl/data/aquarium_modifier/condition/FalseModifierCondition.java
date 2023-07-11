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
        return AxRegistry.ModifierConditions.FALSE.get();
    }
}
