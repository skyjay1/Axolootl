/**
 * Copyright (c) 2023 Skyler James
 * Permission is granted to use, modify, and redistribute this software, in parts or in whole,
 * under the GNU LGPLv3 license (https://www.gnu.org/licenses/lgpl-3.0.en.html)
 **/

package axolootl.data.aquarium_modifier;

import axolootl.AxRegistry;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Vec3i;
import net.minecraft.tags.TagKey;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import java.util.Optional;

@Immutable
public class ModifierSettings {

    public static final Codec<ModifierSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            TagKey.codec(AxRegistry.Keys.AQUARIUM_MODIFIERS).optionalFieldOf("category").forGetter(ModifierSettings::getCategory),
            Codec.DOUBLE.optionalFieldOf("generation", 0.0D).forGetter(ModifierSettings::getGenerationSpeed),
            Codec.DOUBLE.optionalFieldOf("breed", 0.0D).forGetter(ModifierSettings::getBreedSpeed),
            Codec.DOUBLE.optionalFieldOf("feed", 0.0D).forGetter(ModifierSettings::getFeedSpeed),
            Codec.DOUBLE.optionalFieldOf("spread", 0.0D).forGetter(ModifierSettings::getSpreadSpeed),
            Vec3i.CODEC.optionalFieldOf("spread_distance", Vec3i.ZERO).forGetter(ModifierSettings::getSpreadSearchDistance),
            Codec.BOOL.optionalFieldOf("enable_mob_generators", false).forGetter(ModifierSettings::isEnableMobResources),
            Codec.BOOL.optionalFieldOf("enable_mob_breeding", false).forGetter(ModifierSettings::isEnableMobBreeding),
            Codec.INT.optionalFieldOf("energy_cost", 0).forGetter(ModifierSettings::getEnergyCost),
            Codec.BOOL.optionalFieldOf("greedy_energy", false).forGetter(ModifierSettings::isGreedyEnergy)
    ).apply(instance, ModifierSettings::new));

    /** The modifier category. At least one modifier from each category must be present for the aquarium to function **/
    @Nullable
    private final TagKey<AquariumModifier> category;
    /** The generation speed multiplier to add **/
    private final double generationSpeed;
    /** The breeding speed multiplier to add **/
    private final double breedSpeed;
    /** The feeding speed multiplier to add **/
    private final double feedSpeed;
    /** The spread speed of the modifier **/
    private final double spreadSpeed;
    /** The maximum spread distance of the modifier **/
    private final Vec3i spreadSearchDistance;
    /** True if the modifier enables mob resource generators **/
    private final boolean enableMobResources;
    /** True if the modifier enables mob breeding **/
    private final boolean enableMobBreeding;
    /** The energy cost of the modifier **/
    private final int energyCost;
    /** True to deplete energy into the void, false to transfer energy to the block entity to be handled in some other way **/
    private final boolean greedyEnergy;

    public ModifierSettings(Optional<TagKey<AquariumModifier>> category, double generationSpeed, double breedSpeed, double feedSpeed, double spreadSpeed,
                            Vec3i spreadSearchDistance, boolean enableMobResources, boolean enableMobBreeding, int energyCost, boolean greedyEnergy) {
        this.category = category.orElse(null);
        this.generationSpeed = generationSpeed;
        this.breedSpeed = breedSpeed;
        this.feedSpeed = feedSpeed;
        this.spreadSpeed = spreadSpeed;
        this.spreadSearchDistance = spreadSearchDistance;
        this.enableMobResources = enableMobResources;
        this.enableMobBreeding = enableMobBreeding;
        this.energyCost = energyCost;
        this.greedyEnergy = greedyEnergy;
    }

    //// GETTERS ////

    public Optional<TagKey<AquariumModifier>> getCategory() {
        return Optional.ofNullable(category);
    }

    public double getGenerationSpeed() {
        return generationSpeed;
    }

    public double getBreedSpeed() {
        return breedSpeed;
    }

    public double getFeedSpeed() {
        return feedSpeed;
    }

    public double getSpreadSpeed() {
        return spreadSpeed;
    }

    public Vec3i getSpreadSearchDistance() {
        return new Vec3i(spreadSearchDistance.getX(), spreadSearchDistance.getY(), spreadSearchDistance.getZ());
    }

    public boolean isEnableMobResources() {
        return enableMobResources;
    }

    public boolean isEnableMobBreeding() {
        return enableMobBreeding;
    }

    public int getEnergyCost() {
        return energyCost;
    }

    public boolean isGreedyEnergy() {
        return greedyEnergy;
    }
}
