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
import axolootl.util.DeferredHolderSet;
import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.critereon.MinMaxBounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import javax.annotation.concurrent.Immutable;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Immutable
public class DistanceModifierCondition extends ModifierCondition {

    public static final Codec<DistanceModifierCondition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            DeferredHolderSet.codec(AxRegistry.Keys.AQUARIUM_MODIFIERS).fieldOf("modifier").forGetter(DistanceModifierCondition::getModifier),
            AxCodecUtils.NON_NEGATIVE_INTS_CODEC.fieldOf("distance").forGetter(DistanceModifierCondition::getDistance),
            Vec3i.CODEC.optionalFieldOf("offset", Vec3i.ZERO).forGetter(DistanceModifierCondition::getOffset)
    ).apply(instance, DistanceModifierCondition::new));

    private final DeferredHolderSet<AquariumModifier> modifier;
    private final MinMaxBounds.Ints distance;
    private final Vec3i offset;

    public DistanceModifierCondition(DeferredHolderSet<AquariumModifier> modifier, MinMaxBounds.Ints distance, Vec3i offset) {
        this.modifier = modifier;
        this.distance = distance;
        this.offset = offset;
    }

    public DeferredHolderSet<AquariumModifier> getModifier() {
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
        final Registry<AquariumModifier> registry = context.getRegistryAccess().registryOrThrow(AxRegistry.Keys.AQUARIUM_MODIFIERS);
        final HolderSet<AquariumModifier> holderSet = getModifier().get(registry);
        // check distance to each matching modifier
        for(Map.Entry<BlockPos, AquariumModifier> entry : context.getModifiers().entrySet()) {
            // skip this entry
            if(entry.getKey().equals(context.getPos())) continue;
            // calculate distance for matching modifiers
            if(holderSet.contains(entry.getValue().getHolder(context.getRegistryAccess()))) {
                if(isWithinDistance(pos, entry.getKey(), entry.getValue())) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isWithinDistance(final BlockPos origin, final BlockPos pos, final AquariumModifier modifier) {
        // check distance allows any value
        if(distance.isAny()) {
            return true;
        }
        // check modifier is not multiblock
        if(!modifier.isMultiblock()) {
            return distance.matches(origin.distManhattan(pos));
        }
        // prepare to check distance between origin and multiblock
        final Vec3i dimensions = modifier.getDimensions();
        final Vec3i offset = new Vec3i(dimensions.getX() - 1, dimensions.getY() - 1, dimensions.getZ() - 1);
        final BlockPos modifierStart = pos.offset(Math.max(0, offset.getX() - 1), Math.max(0, offset.getY() - 1), Math.max(0, offset.getZ() - 1));
        final BlockPos modifierEnd = pos.offset(offset);
        // create a bounding box that contains the origin and all modifier positions
        final Optional<BoundingBox> oBox = BoundingBox.encapsulatingPositions(ImmutableList.of(
                origin,
                pos,
                modifierStart,
                modifierEnd
        ));
        // verify non-empty
        if(oBox.isEmpty()) {
            return false;
        }
        // calculate distance from the origin to the modifier in each dimension
        final Vec3i length = oBox.get().getLength();
        final int dx = Math.max(0, length.getX() - dimensions.getX() + 1);
        final int dy = Math.max(0, length.getY() - dimensions.getY() + 1);
        final int dz = Math.max(0, length.getZ() - dimensions.getZ() + 1);
        // calculate total distance using manhattan formula
        final int totalDistance = (dx + dy + dz);
        // verify distance is in acceptable range
        return distance.matches(totalDistance);
    }

    @Override
    public Codec<? extends ModifierCondition> getCodec() {
        return AxRegistry.ModifierConditionsReg.DISTANCE.get();
    }

    @Override
    public List<Component> createDescription(final RegistryAccess registryAccess) {
        final Registry<AquariumModifier> registry = registryAccess.registryOrThrow(AxRegistry.Keys.AQUARIUM_MODIFIERS);
        final HolderSet<AquariumModifier> holderSet = getModifier().get(registry);
        return createCountedDescription("axolootl.modifier_condition.distance", distance, registry, holderSet, AquariumModifier::getDescription);
    }

    @Override
    public String toString() {
        return "distance {distance=(" + Optional.ofNullable(getDistance().getMin()) + "," + Optional.ofNullable(getDistance().getMax()) + ") modifier=" + getModifier() + "}";
    }
}
