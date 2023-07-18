package axolootl.data.aquarium_modifier.condition;

import axolootl.AxRegistry;
import axolootl.data.aquarium_modifier.AquariumModifier;
import com.mojang.serialization.Codec;
import net.minecraft.core.HolderSet;
import net.minecraft.util.valueproviders.UniformInt;

import javax.annotation.concurrent.Immutable;

@Immutable
public class ExistsModifierCondition extends CountModifierCondition {

    public static final Codec<ExistsModifierCondition> CODEC = AquariumModifier.HOLDER_SET_CODEC
            .xmap(ExistsModifierCondition::new, ExistsModifierCondition::getModifiers).fieldOf("modifier").codec();

    public ExistsModifierCondition(HolderSet<AquariumModifier> modifiers) {
        super(modifiers, UniformInt.of(1, Integer.MAX_VALUE));
    }

    @Override
    public Codec<? extends ModifierCondition> getCodec() {
        return AxRegistry.ModifierConditionsReg.EXISTS.get();
    }
}
