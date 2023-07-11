package axolootl.data.aquarium_modifier.condition;

import axolootl.AxRegistry;
import axolootl.data.aquarium_modifier.AquariumModifierContext;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.valueproviders.IntProvider;

import javax.annotation.concurrent.Immutable;

@Immutable
public class AxolootlCountModifierCondition extends ModifierCondition {

    // TODO introduce some sort of predicate for which axolootls to count
    public static final Codec<AxolootlCountModifierCondition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            IntProvider.NON_NEGATIVE_CODEC.fieldOf("count").forGetter(AxolootlCountModifierCondition::getCount)
    ).apply(instance, AxolootlCountModifierCondition::new));

    private final IntProvider count;

    public AxolootlCountModifierCondition(IntProvider count) {
        this.count = count;
    }

    public IntProvider getCount() {
        return count;
    }

    @Override
    public boolean test(AquariumModifierContext aquariumModifierContext) {
        int count = aquariumModifierContext.getAxolootls().size();
        return count >= getCount().getMinValue() && count <= getCount().getMaxValue();
    }

    @Override
    public Codec<? extends ModifierCondition> getCodec() {
        return AxRegistry.ModifierConditions.AXOLOOTL_COUNT.get();
    }

    @Override
    public String toString() {
        return "axolootl_count {count=(" + getCount().getMinValue() + "," + getCount().getMaxValue() + ")" + "}";
    }
}
