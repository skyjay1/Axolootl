/**
 * Copyright (c) 2023 Skyler James
 * Permission is granted to use, modify, and redistribute this software, in parts or in whole,
 * under the GNU LGPLv3 license (https://www.gnu.org/licenses/lgpl-3.0.en.html)
 **/

package axolootl.data.aquarium_modifier.condition;

import axolootl.AxRegistry;
import axolootl.data.aquarium_modifier.AquariumModifierContext;
import axolootl.util.AxCodecUtils;
import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.critereon.FluidPredicate;
import net.minecraft.advancements.critereon.LightPredicate;
import net.minecraft.advancements.critereon.MinMaxBounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;
import net.minecraft.world.level.levelgen.structure.Structure;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

@Immutable
public class LocationModifierCondition extends ModifierCondition {

    public static final Codec<LocationModifierCondition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            DoublesPosition.CODEC.optionalFieldOf("position", DoublesPosition.ANY).forGetter(LocationModifierCondition::getPosition),
            Vec3i.CODEC.optionalFieldOf("offset", Vec3i.ZERO).forGetter(LocationModifierCondition::getOffset),
            ResourceKey.codec(Registries.BIOME).optionalFieldOf("biome").forGetter(LocationModifierCondition::getBiome),
            ResourceKey.codec(Registries.STRUCTURE).optionalFieldOf("structure").forGetter(LocationModifierCondition::getStructure),
            ResourceKey.codec(Registries.DIMENSION).optionalFieldOf("dimension").forGetter(LocationModifierCondition::getDimension),
            Codec.BOOL.optionalFieldOf("smokey").forGetter(LocationModifierCondition::getSmokey),
            AxCodecUtils.LIGHT_PREDICATE_CODEC.optionalFieldOf("light", LightPredicate.ANY).forGetter(LocationModifierCondition::getLight),
            AxCodecUtils.FLUID_PREDICATE_CODEC.optionalFieldOf("fluid", FluidPredicate.ANY).forGetter(LocationModifierCondition::getFluid),
            BlockPredicate.CODEC.optionalFieldOf("block", BlockPredicate.alwaysTrue()).forGetter(LocationModifierCondition::getBlock)
    ).apply(instance, LocationModifierCondition::new));

    private final DoublesPosition position;
    private final Vec3i offset;
    @Nullable
    private final ResourceKey<Biome> biome;
    @Nullable
    private final ResourceKey<Structure> structure;
    @Nullable
    private final ResourceKey<Level> dimension;
    @Nullable
    private final Boolean smokey;
    private final LightPredicate light;
    private final FluidPredicate fluid;
    private final BlockPredicate block;

    public LocationModifierCondition(DoublesPosition position, Vec3i offset,
                                     Optional<ResourceKey<Biome>> biome, Optional<ResourceKey<Structure>> structure,
                                     Optional<ResourceKey<Level>> dimension, Optional<Boolean> smokey,
                                     LightPredicate light, FluidPredicate fluid, BlockPredicate block) {
        this.position = position;
        this.offset = offset;
        this.biome = biome.orElse(null);
        this.structure = structure.orElse(null);
        this.dimension = dimension.orElse(null);
        this.smokey = smokey.orElse(null);
        this.light = light;
        this.fluid = fluid;
        this.block = block;
    }


    @Override
    public boolean test(AquariumModifierContext context) {
        final BlockPos blockpos = context.getPos().offset(offset);
        final ServerLevel level = (ServerLevel) context.getLevel();
        // validate position
        if (!position.x.matches(blockpos.getX())) {
            return false;
        }
        if (!position.y.matches(blockpos.getY())) {
            return false;
        }
        if (!position.z.matches(blockpos.getZ())) {
            return false;
        }
        // validate dimension
        if (this.dimension != null && !this.dimension.equals(level.dimension())) {
            return false;
        }
        // verify area is loaded
        boolean isLoaded = level.isLoaded(blockpos);
        if(!isLoaded) {
            return false;
        }
        // validate biome
        if(this.biome != null && !level.getBiome(blockpos).is(this.biome)) {
            return false;
        }
        // validate structure
        if(this.structure != null && !hasStructure(context.getRegistryAccess(), this.structure, level.structureManager().getAllStructuresAt(blockpos).keySet())) {
            return false;
        }
        // validate light
        if(!getLight().matches(level, blockpos)) {
            return false;
        }
        // validate block
        if(!getBlock().test(level, blockpos)) {
            return false;
        }
        // validate fluid
        if(!getFluid().matches(level, blockpos)) {
            return false;
        }
        // validate smokey
        if(smokey != null && smokey != CampfireBlock.isSmokeyPos(level, blockpos)) {
            return false;
        }
        // all checks passed
        return true;
    }

    @Override
    public Codec<? extends ModifierCondition> getCodec() {
        return AxRegistry.ModifierConditionsReg.LOCATION.get();
    }

    @Override
    public List<Component> createDescription(final RegistryAccess registryAccess) {
        // create description
        final ImmutableList.Builder<Component> builder = ImmutableList.builder();
        // offset
        if(!Vec3i.ZERO.equals(this.offset)) {
            builder.add(Component.translatable("axolootl.modifier_condition.location.with_offset", this.offset.getX(), this.offset.getY(), this.offset.getZ()).withStyle(ChatFormatting.ITALIC));
        }
        // position
        if(this.position != DoublesPosition.ANY) {
            builder.addAll(this.position.getDescription());
        }
        // biome
        if(this.biome != null) {
            Component text = createResourceKeyDescription(this.biome);
            builder.add(Component.translatable("axolootl.modifier_condition.location.biome", text));
        }
        // structure
        if(this.structure != null) {
            Component text = createResourceKeyDescription(this.structure);
            builder.add(Component.translatable("axolootl.modifier_condition.location.structure", text));
        }
        // dimension
        if(this.dimension != null) {
            Component text = createResourceKeyDescription(this.dimension);
            builder.add(Component.translatable("axolootl.modifier_condition.location.dimension", text));
        }
        // smokey
        if(this.smokey != null) {
            builder.add(Component.translatable("axolootl.modifier_condition.location." + (this.smokey ? "smokey" : "not_smokey")));
        }
        // light
        if(this.light != LightPredicate.ANY) {
            builder.add(createLightDescription(this.light.composite));
        }
        return builder.build();
    }

    private static boolean hasStructure(final RegistryAccess registryAccess, final ResourceKey<Structure> key, final Set<Structure> structures) {
        final Registry<Structure> registry = registryAccess.registryOrThrow(Registries.STRUCTURE);
        for(Structure structure : structures) {
            // load structure resource key
            Optional<ResourceKey<Structure>> resourceKey = registry.getResourceKey(structure);
            if(resourceKey.isEmpty()) continue;
            // check equality
            if(resourceKey.get().equals(key)) {
                return true;
            }
        }
        // all checks failed
        return false;
    }

    private static Component createLightDescription(final MinMaxBounds.Ints bounds) {
        // check range
        if(bounds.getMin() != null && bounds.getMax() != null) {
            return Component.translatable("axolootl.modifier_condition.location.light.range", bounds.getMin(), bounds.getMax());
        }
        // check min only
        if(bounds.getMin() != null) {
            return Component.translatable("axolootl.modifier_condition.location.light.min", bounds.getMin());
        }
        // check max only
        if(bounds.getMax() != null) {
            return Component.translatable("axolootl.modifier_condition.location.light.max", bounds.getMax());
        }
        // fallback
        return Component.empty();
    }

    //// GETTERS ////

    public DoublesPosition getPosition() {
        return position;
    }

    public Vec3i getOffset() {
        return new Vec3i(offset.getX(), offset.getY(), offset.getZ());
    }

    public Optional<ResourceKey<Biome>> getBiome() {
        return Optional.ofNullable(biome);
    }

    public Optional<ResourceKey<Structure>> getStructure() {
        return Optional.ofNullable(structure);
    }

    public Optional<ResourceKey<Level>> getDimension() {
        return Optional.ofNullable(dimension);
    }

    public Optional<Boolean> getSmokey() {
        return Optional.ofNullable(smokey);
    }

    public LightPredicate getLight() {
        return light;
    }

    public FluidPredicate getFluid() {
        return fluid;
    }

    public BlockPredicate getBlock() {
        return block;
    }

    //// UTILITY CLASSES ////

    /**
     * Contains information about X, Y, and Z {@link MinMaxBounds.Doubles}
     **/
    public static final class DoublesPosition {

        public static final Codec<MinMaxBounds.Doubles> DOUBLES_DIRECT_CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.DOUBLE.optionalFieldOf("min").forGetter(o -> Optional.ofNullable(o.getMin())),
                Codec.DOUBLE.optionalFieldOf("max").forGetter(o -> Optional.ofNullable(o.getMax()))
        ).apply(instance, (p1, p2) -> {
            if(p1.isPresent() && p2.isEmpty()) return MinMaxBounds.Doubles.atLeast(p1.get());
            if(p1.isEmpty() && p2.isPresent()) return MinMaxBounds.Doubles.atMost(p2.get());
            if(p1.isEmpty() && p2.isEmpty()) return MinMaxBounds.Doubles.ANY;
            return MinMaxBounds.Doubles.between(p1.get(), p2.get());
        }));

        public static final Codec<MinMaxBounds.Doubles> DOUBLES_CODEC = Codec.either(Codec.DOUBLE, DOUBLES_DIRECT_CODEC)
                .xmap(either -> either.map(MinMaxBounds.Doubles::exactly, Function.identity()),
                        o -> (o.getMin() != null && o.getMax() != null && o.getMin().equals(o.getMax())) ? Either.left(o.getMin()) : Either.right(o));

        public static final Codec<DoublesPosition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                DOUBLES_CODEC.optionalFieldOf("x").forGetter(o -> Optional.of(o.x)),
                DOUBLES_CODEC.optionalFieldOf("y").forGetter(o -> Optional.of(o.y)),
                DOUBLES_CODEC.optionalFieldOf("z").forGetter(o -> Optional.of(o.z))
        ).apply(instance, DoublesPosition::new));

        public static final DoublesPosition ANY = new DoublesPosition(Optional.empty(), Optional.empty(), Optional.empty());

        private final MinMaxBounds.Doubles x;
        private final MinMaxBounds.Doubles y;
        private final MinMaxBounds.Doubles z;

        private final List<Component> description;

        public DoublesPosition(Optional<MinMaxBounds.Doubles> x, Optional<MinMaxBounds.Doubles> y, Optional<MinMaxBounds.Doubles> z) {
            this.x = x.orElse(MinMaxBounds.Doubles.ANY);
            this.y = y.orElse(MinMaxBounds.Doubles.ANY);
            this.z = z.orElse(MinMaxBounds.Doubles.ANY);
            // create description
            final ImmutableList.Builder<Component> builder = ImmutableList.builder();
            if(!this.x.isAny()) {
                builder.add(createDescription("x", this.x));
            }
            if(!this.y.isAny()) {
                builder.add(createDescription("y", this.y));
            }
            if(!this.z.isAny()) {
                builder.add(createDescription("z", this.z));
            }
            this.description = builder.build();
        }

        private static Component createDescription(final String axis, final MinMaxBounds.Doubles bounds) {
            final Component axisText = Component.translatable("axolootl.modifier_condition.location.position." + axis);
            // check range
            if(bounds.getMin() != null && bounds.getMax() != null) {
                return Component.translatable("axolootl.modifier_condition.location.position.range", axisText, bounds.getMin().longValue(), bounds.getMax().longValue());
            }
            // check min only
            if(bounds.getMin() != null) {
                return Component.translatable("axolootl.modifier_condition.location.position.min", axisText, bounds.getMin().longValue());
            }
            // check max only
            if(bounds.getMax() != null) {
                return Component.translatable("axolootl.modifier_condition.location.position.max", axisText, bounds.getMax().longValue());
            }
            // fallback
            return Component.empty();
        }

        public MinMaxBounds.Doubles getX() {
            return x;
        }

        public MinMaxBounds.Doubles getY() {
            return y;
        }

        public MinMaxBounds.Doubles getZ() {
            return z;
        }

        public List<Component> getDescription() {
            return description;
        }
    }
}
