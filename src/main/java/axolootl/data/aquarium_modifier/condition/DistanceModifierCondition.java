package axolootl.data.aquarium_modifier.condition;

import axolootl.AxRegistry;
import axolootl.data.aquarium_modifier.AquariumModifier;
import axolootl.data.aquarium_modifier.AquariumModifierContext;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Vec3i;
import net.minecraft.util.valueproviders.IntProvider;

import javax.annotation.concurrent.Immutable;
import java.util.Map;

@Immutable
public class DistanceModifierCondition extends ModifierCondition {

    public static final Codec<DistanceModifierCondition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            AquariumModifier.HOLDER_SET_CODEC.fieldOf("modifier").forGetter(DistanceModifierCondition::getModifier),
            IntProvider.NON_NEGATIVE_CODEC.fieldOf("distance").forGetter(DistanceModifierCondition::getDistance),
            Vec3i.CODEC.optionalFieldOf("offset", Vec3i.ZERO).forGetter(DistanceModifierCondition::getOffset)
    ).apply(instance, DistanceModifierCondition::new));

    private final HolderSet<AquariumModifier> modifier;
    private final IntProvider distance;
    private final Vec3i offset;

    public DistanceModifierCondition(HolderSet<AquariumModifier> modifier, IntProvider distance, Vec3i offset) {
        this.modifier = modifier;
        this.distance = distance;
        this.offset = offset;
    }

    public HolderSet<AquariumModifier> getModifier() {
        return modifier;
    }

    public IntProvider getDistance() {
        return distance;
    }

    public Vec3i getOffset() {
        return new Vec3i(offset.getX(), offset.getY(), offset.getZ());
    }

    @Override
    public boolean test(AquariumModifierContext context) {
        final BlockPos pos = context.getPos().offset(offset);
        // check distance to each matching modifier
        for(Map.Entry<BlockPos, AquariumModifier> entry : context.getModifiers().entrySet()) {
            if(getModifier().contains(entry.getValue().getHolder(context.getRegistryAccess()))) {
                final int manhattanDistance = pos.distManhattan(entry.getKey());
                // verify distance is within range
                if(manhattanDistance >= getDistance().getMinValue() && manhattanDistance <= getDistance().getMaxValue()) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public Codec<? extends ModifierCondition> getCodec() {
        return AxRegistry.ModifierConditionsReg.DISTANCE.get();
    }

    @Override
    public String toString() {
        return "distance {distance=(" + getDistance().getMinValue() + "," + getDistance().getMaxValue() + ") modifier=" + getModifier() + "}";
    }
}
