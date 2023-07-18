package axolootl.data.aquarium_modifier.condition;

import axolootl.AxRegistry;
import axolootl.data.aquarium_modifier.AquariumModifier;
import axolootl.data.aquarium_modifier.AquariumModifierContext;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderSet;
import net.minecraft.util.valueproviders.IntProvider;

import javax.annotation.concurrent.Immutable;

@Immutable
public class CountModifierCondition extends ModifierCondition {

    public static final Codec<CountModifierCondition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            AquariumModifier.HOLDER_SET_CODEC.fieldOf("modifier").forGetter(CountModifierCondition::getModifiers),
            IntProvider.NON_NEGATIVE_CODEC.fieldOf("count").forGetter(CountModifierCondition::getCount)
    ).apply(instance, CountModifierCondition::new));

    private final HolderSet<AquariumModifier> modifierId;
    private final IntProvider count;

    public CountModifierCondition(HolderSet<AquariumModifier> modifierId, IntProvider count) {
        this.modifierId = modifierId;
        this.count = count;
    }

    public HolderSet<AquariumModifier> getModifiers() {
        return modifierId;
    }

    public IntProvider getCount() {
        return count;
    }

    @Override
    public boolean test(AquariumModifierContext context) {
        int count = 0;
        for(AquariumModifier entry : context.getModifiers().values()) {
            if(modifierId.contains(entry.getHolder(context.getRegistryAccess()))) {
                count++;
            }
        }
        return count >= getCount().getMinValue() && count <= getCount().getMaxValue();
    }

    @Override
    public Codec<? extends ModifierCondition> getCodec() {
        return AxRegistry.ModifierConditionsReg.COUNT.get();
    }

    @Override
    public String toString() {
        return "count {count=(" + getCount().getMinValue() + "," + getCount().getMaxValue() + ") modifier=" + getModifiers() + "}";
    }
}
