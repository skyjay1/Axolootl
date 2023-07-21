/**
 * Copyright (c) 2023 Skyler James
 * Permission is granted to use, modify, and redistribute this software, in parts or in whole,
 * under the GNU LGPLv3 license (https://www.gnu.org/licenses/lgpl-3.0.en.html)
 **/

package axolootl.data.aquarium_modifier.condition;

import axolootl.AxRegistry;
import axolootl.data.aquarium_modifier.AquariumModifierContext;
import axolootl.util.MatchingStatePredicate;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.critereon.FluidPredicate;
import net.minecraft.advancements.critereon.LightPredicate;
import net.minecraft.advancements.critereon.MinMaxBounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import java.util.Optional;

@Immutable
public class LocationModifierCondition extends ModifierCondition {

    public static final Codec<MinMaxBounds.Ints> INTS_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("min").forGetter(o -> o.getMin().describeConstable()),
            Codec.INT.optionalFieldOf("max").forGetter(o -> o.getMax().describeConstable())
    ).apply(instance, (p1, p2) -> {
        if(p1.isPresent() && p2.isEmpty()) return MinMaxBounds.Ints.atLeast(p1.get());
        if(p1.isEmpty() && p2.isPresent()) return MinMaxBounds.Ints.atMost(p2.get());
        if(p1.isEmpty() && p2.isEmpty()) return MinMaxBounds.Ints.ANY;
        return MinMaxBounds.Ints.between(p1.get(), p2.get());
    }));

    public static final Codec<LightPredicate> LIGHT_PREDICATE_CODEC = INTS_CODEC.xmap(composite -> new LightPredicate.Builder().setComposite(composite).build(), o -> o.composite);

    public static final Codec<FluidPredicate> FLUID_PREDICATE_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            TagKey.codec(ForgeRegistries.Keys.FLUIDS).optionalFieldOf("tag").forGetter(o -> Optional.ofNullable(o.tag)),
            ForgeRegistries.FLUIDS.getCodec().optionalFieldOf("fluid").forGetter(o -> Optional.ofNullable(o.fluid)),
            // TODO state properties predicate is only partly supported
            MatchingStatePredicate.STATE_PROPERTIES_PREDICATE_CODEC.optionalFieldOf("state").forGetter(o -> Optional.of(o.properties))
    ).apply(instance, (tag, fluid, state) -> {
        final FluidPredicate.Builder builder = FluidPredicate.Builder.fluid();
        tag.ifPresent(builder::of);
        fluid.ifPresent(builder::of);
        state.ifPresent(builder::setProperties);
        return builder.build();
    }));

    public static final Codec<LocationModifierCondition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            DoublesPosition.CODEC.optionalFieldOf("position", DoublesPosition.ANY).forGetter(LocationModifierCondition::getPosition),
            Vec3i.CODEC.optionalFieldOf("offset", Vec3i.ZERO).forGetter(LocationModifierCondition::getOffset),
            ResourceKey.codec(Registry.BIOME_REGISTRY).optionalFieldOf("biome").forGetter(LocationModifierCondition::getBiome),
            ResourceKey.codec(Registry.STRUCTURE_REGISTRY).optionalFieldOf("structure").forGetter(LocationModifierCondition::getStructure),
            ResourceKey.codec(Registry.DIMENSION_REGISTRY).optionalFieldOf("dimension").forGetter(LocationModifierCondition::getDimension),
            Codec.BOOL.optionalFieldOf("smokey").forGetter(LocationModifierCondition::getSmokey),
            LIGHT_PREDICATE_CODEC.optionalFieldOf("light", LightPredicate.ANY).forGetter(LocationModifierCondition::getLight),
            FLUID_PREDICATE_CODEC.optionalFieldOf("fluid", FluidPredicate.ANY).forGetter(LocationModifierCondition::getFluid),
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
    public boolean test(AquariumModifierContext aquariumModifierContext) {
        final BlockPos blockpos = aquariumModifierContext.getPos().offset(offset);
        final ServerLevel level = (ServerLevel) aquariumModifierContext.getLevel();
        // validate position
        if (!getPosition().x.matches(blockpos.getX())) {
            return false;
        }
        if (!getPosition().y.matches(blockpos.getY())) {
            return false;
        }
        if (!getPosition().z.matches(blockpos.getZ())) {
            return false;
        }
        // validate dimension
        if (this.dimension != null && this.dimension != level.dimension()) {
            return false;
        }
        // verify loaded
        boolean isLoaded = level.isLoaded(blockpos);
        if(!isLoaded) {
            return false;
        }
        // validate biome
        if(this.biome != null && !level.getBiome(blockpos).is(this.biome)) {
            return false;
        }
        // validate structure
        if(this.structure != null && !level.structureManager().getStructureWithPieceAt(blockpos, this.structure).isValid()) {
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
        // all checks passed
        return true;
    }

    @Override
    public Codec<? extends ModifierCondition> getCodec() {
        return AxRegistry.ModifierConditionsReg.LOCATION.get();
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

    public static final class DoublesPosition {

        public static final Codec<MinMaxBounds.Doubles> DOUBLES_CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.DOUBLE.optionalFieldOf("min").forGetter(o -> o.getMin().describeConstable()),
                Codec.DOUBLE.optionalFieldOf("max").forGetter(o -> o.getMax().describeConstable())
        ).apply(instance, (p1, p2) -> {
            if(p1.isPresent() && p2.isEmpty()) return MinMaxBounds.Doubles.atLeast(p1.get());
            if(p1.isEmpty() && p2.isPresent()) return MinMaxBounds.Doubles.atMost(p2.get());
            if(p1.isEmpty() && p2.isEmpty()) return MinMaxBounds.Doubles.ANY;
            return MinMaxBounds.Doubles.between(p1.get(), p2.get());
        }));

        public static final Codec<DoublesPosition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                DOUBLES_CODEC.optionalFieldOf("x").forGetter(o -> Optional.of(o.x)),
                DOUBLES_CODEC.optionalFieldOf("y").forGetter(o -> Optional.of(o.y)),
                DOUBLES_CODEC.optionalFieldOf("z").forGetter(o -> Optional.of(o.z))
        ).apply(instance, DoublesPosition::new));

        public static final DoublesPosition ANY = new DoublesPosition(Optional.empty(), Optional.empty(), Optional.empty());

        private final MinMaxBounds.Doubles x;
        private final MinMaxBounds.Doubles y;
        private final MinMaxBounds.Doubles z;

        public DoublesPosition(Optional<MinMaxBounds.Doubles> x, Optional<MinMaxBounds.Doubles> y, Optional<MinMaxBounds.Doubles> z) {
            this.x = x.orElse(MinMaxBounds.Doubles.ANY);
            this.y = y.orElse(MinMaxBounds.Doubles.ANY);
            this.z = z.orElse(MinMaxBounds.Doubles.ANY);
        }
    }
}
