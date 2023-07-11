package axolootl.data.aquarium_modifier.condition;

import axolootl.AxRegistry;
import axolootl.data.aquarium_modifier.AquariumModifierContext;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.valueproviders.IntProvider;

import javax.annotation.concurrent.Immutable;

@Immutable
public class CountModifierCondition extends ModifierCondition {

    public static final Codec<CountModifierCondition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("modifier").forGetter(CountModifierCondition::getModifierId),
            IntProvider.NON_NEGATIVE_CODEC.fieldOf("count").forGetter(CountModifierCondition::getCount)
    ).apply(instance, CountModifierCondition::new));

    private final ResourceLocation modifierId;
    private final IntProvider count;

    public CountModifierCondition(ResourceLocation modifierId, IntProvider count) {
        this.modifierId = modifierId;
        this.count = count;
    }

    public ResourceLocation getModifierId() {
        return modifierId;
    }

    public IntProvider getCount() {
        return count;
    }

    @Override
    public boolean test(AquariumModifierContext aquariumModifierContext) {
        int count = 0;
        for(ResourceLocation id : aquariumModifierContext.getModifiers().values()) {
            if(id.equals(getModifierId())) {
                count++;
            }
        }
        return count >= getCount().getMinValue() && count <= getCount().getMaxValue();
    }

    @Override
    public Codec<? extends ModifierCondition> getCodec() {
        return AxRegistry.ModifierConditions.COUNT.get();
    }

    @Override
    public String toString() {
        return "count {count=(" + getCount().getMinValue() + "," + getCount().getMaxValue() + ") modifier=" + getModifierId() + "}";
    }
}
