package axolootl.data.aquarium_modifier.condition;

import axolootl.AxRegistry;
import axolootl.data.aquarium_modifier.AquariumModifierContext;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.valueproviders.IntProvider;

import javax.annotation.concurrent.Immutable;
import java.util.Map;

@Immutable
public class DistanceModifierCondition extends ModifierCondition {

    public static final Codec<DistanceModifierCondition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("modifier").forGetter(DistanceModifierCondition::getModifierId),
            IntProvider.NON_NEGATIVE_CODEC.fieldOf("distance").forGetter(DistanceModifierCondition::getCount),
            Vec3i.CODEC.optionalFieldOf("offset", Vec3i.ZERO).forGetter(DistanceModifierCondition::getOffset)
    ).apply(instance, DistanceModifierCondition::new));

    private final ResourceLocation modifierId;
    private final IntProvider count;
    private final Vec3i offset;

    public DistanceModifierCondition(ResourceLocation modifierId, IntProvider count, Vec3i offset) {
        this.modifierId = modifierId;
        this.count = count;
        this.offset = offset;
    }

    public ResourceLocation getModifierId() {
        return modifierId;
    }

    public IntProvider getCount() {
        return count;
    }

    public Vec3i getOffset() {
        return offset;
    }

    @Override
    public boolean test(AquariumModifierContext aquariumModifierContext) {
        final BlockPos pos = aquariumModifierContext.getPos().offset(getOffset());
        // check distance to each matching modifier
        for(Map.Entry<BlockPos, ResourceLocation> entry : aquariumModifierContext.getModifiers().entrySet()) {
            if(entry.getValue().equals(getModifierId())) {
                final int manhattanDistance = pos.distManhattan(entry.getKey());
                // verify distance is within range
                if(manhattanDistance >= getCount().getMinValue() && manhattanDistance <= getCount().getMaxValue()) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public Codec<? extends ModifierCondition> getCodec() {
        return AxRegistry.ModifierConditions.DISTANCE.get();
    }

    @Override
    public String toString() {
        return "distance {distance=(" + getCount().getMinValue() + "," + getCount().getMaxValue() + ") modifier=" + getModifierId() + "}";
    }
}
