package axolootl.data.aquarium_modifier.condition;

import axolootl.AxRegistry;
import com.mojang.serialization.Codec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.valueproviders.UniformInt;

import javax.annotation.concurrent.Immutable;

@Immutable
public class ExistsModifierCondition extends CountModifierCondition {

    public static final Codec<ExistsModifierCondition> CODEC = ResourceLocation.CODEC
            .xmap(ExistsModifierCondition::new, ExistsModifierCondition::getModifierId).fieldOf("modifier").codec();

    public ExistsModifierCondition(ResourceLocation modifierId) {
        super(modifierId, UniformInt.of(1, Integer.MAX_VALUE));
    }

    @Override
    public Codec<? extends ModifierCondition> getCodec() {
        return AxRegistry.ModifierConditions.EXISTS.get();
    }
}
