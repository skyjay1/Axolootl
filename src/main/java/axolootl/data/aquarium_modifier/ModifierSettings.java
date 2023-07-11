package axolootl.data.aquarium_modifier;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Vec3i;

import javax.annotation.concurrent.Immutable;

@Immutable
public class ModifierSettings {

    public static final Codec<ModifierSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.BOOL.optionalFieldOf("mandatory", false).forGetter(ModifierSettings::isMandatory),
            Codec.DOUBLE.optionalFieldOf("generation", 0.0D).forGetter(ModifierSettings::getGenerationSpeed),
            Codec.DOUBLE.optionalFieldOf("breed", 0.0D).forGetter(ModifierSettings::getBreedSpeed),
            Codec.DOUBLE.optionalFieldOf("feed", 0.0D).forGetter(ModifierSettings::getFeedSpeed),
            Codec.DOUBLE.optionalFieldOf("spread", 0.0D).forGetter(ModifierSettings::getSpreadSpeed),
            Vec3i.CODEC.optionalFieldOf("spread_distance", Vec3i.ZERO).forGetter(ModifierSettings::getSpreadSearchDistance),
            Codec.BOOL.optionalFieldOf("enable_mob_generators", false).forGetter(ModifierSettings::isEnableMobResources),
            Codec.BOOL.optionalFieldOf("enable_mob_breeding", false).forGetter(ModifierSettings::isEnableMobBreeding),
            Codec.INT.optionalFieldOf("energy_cost", 0).forGetter(ModifierSettings::getEnergyCost)
    ).apply(instance, ModifierSettings::new));

    // TODO find some way of specifying certain groups of modifiers are mandatory (but any modifier in the group fulfills the condition)
    // Use case: bubbler OR powered bubbler is required, but not both
    /** True if the aquarium must have this modifier to function **/
    private final boolean mandatory;
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

    public ModifierSettings(boolean mandatory, double generationSpeed, double breedSpeed, double feedSpeed, double spreadSpeed,
                            Vec3i spreadSearchDistance, boolean enableMobResources, boolean enableMobBreeding, int energyCost) {
        this.mandatory = mandatory;
        this.generationSpeed = generationSpeed;
        this.breedSpeed = breedSpeed;
        this.feedSpeed = feedSpeed;
        this.spreadSpeed = spreadSpeed;
        this.spreadSearchDistance = spreadSearchDistance;
        this.enableMobResources = enableMobResources;
        this.enableMobBreeding = enableMobBreeding;
        this.energyCost = energyCost;
    }

    //// GETTERS ////

    public boolean isMandatory() {
        return mandatory;
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
}
