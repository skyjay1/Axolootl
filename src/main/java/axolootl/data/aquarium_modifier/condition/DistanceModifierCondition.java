/**
 * Copyright (c) 2023 Skyler James
 * Permission is granted to use, modify, and redistribute this software, in parts or in whole,
 * under the GNU LGPLv3 license (https://www.gnu.org/licenses/lgpl-3.0.en.html)
 **/

package axolootl.data.aquarium_modifier.condition;

import axolootl.AxRegistry;
import axolootl.data.aquarium_modifier.AquariumModifier;
import axolootl.data.aquarium_modifier.AquariumModifierContext;
import axolootl.util.AxCodecUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.critereon.MinMaxBounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;

import javax.annotation.concurrent.Immutable;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Immutable
public class DistanceModifierCondition extends ModifierCondition {

    public static final Codec<DistanceModifierCondition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            AquariumModifier.HOLDER_SET_CODEC.fieldOf("modifier").forGetter(DistanceModifierCondition::getModifier),
            AxCodecUtils.NON_NEGATIVE_INTS_CODEC.fieldOf("distance").forGetter(DistanceModifierCondition::getDistance),
            Vec3i.CODEC.optionalFieldOf("offset", Vec3i.ZERO).forGetter(DistanceModifierCondition::getOffset)
    ).apply(instance, DistanceModifierCondition::new));

    private final HolderSet<AquariumModifier> modifier;
    private final MinMaxBounds.Ints distance;
    private final Vec3i offset;
    private final List<Component> description;

    public DistanceModifierCondition(HolderSet<AquariumModifier> modifier, MinMaxBounds.Ints distance, Vec3i offset) {
        this.modifier = modifier;
        this.distance = distance;
        this.offset = offset;
        this.description = createCountedDescription("axolootl.modifier_condition.count", distance, modifier, AquariumModifier::getDescription);
    }

    public HolderSet<AquariumModifier> getModifier() {
        return modifier;
    }

    public MinMaxBounds.Ints getDistance() {
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
                if(getDistance().matches(manhattanDistance)) {
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
    public List<Component> getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return "distance {distance=(" + Optional.ofNullable(getDistance().getMin()) + "," + Optional.ofNullable(getDistance().getMax()) + ") modifier=" + getModifier() + "}";
    }
}
