package axolootl.data.aquarium_modifier.condition;

import axolootl.AxRegistry;
import axolootl.data.aquarium_modifier.AquariumModifierContext;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.server.level.ServerLevel;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import java.util.Optional;

@Immutable
public class WeatherModifierCondition extends ModifierCondition {

    public static final Codec<WeatherModifierCondition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.BOOL.optionalFieldOf("raining").forGetter(WeatherModifierCondition::isRaining),
            Codec.BOOL.optionalFieldOf("thundering").forGetter(WeatherModifierCondition::isThundering)
    ).apply(instance, WeatherModifierCondition::new));

    @Nullable
    private final Boolean isRaining;
    @Nullable
    private final Boolean isThundering;

    public WeatherModifierCondition(Optional<Boolean> raining, Optional<Boolean> thundering) {
        this.isRaining = raining.orElse(null);
        this.isThundering = thundering.orElse(null);
    }


    @Nullable
    public Optional<Boolean> isRaining() {
        return Optional.ofNullable(isRaining);
    }

    @Nullable
    public Optional<Boolean> isThundering() {
        return Optional.ofNullable(isThundering);
    }

    @Override
    public boolean test(AquariumModifierContext aquariumModifierContext) {
        ServerLevel serverlevel = (ServerLevel) aquariumModifierContext.getLevel();
        if (this.isRaining != null && this.isRaining != serverlevel.isRaining()) {
            return false;
        } else {
            return this.isThundering == null || this.isThundering == serverlevel.isThundering();
        }
    }

    @Override
    public Codec<? extends ModifierCondition> getCodec() {
        return AxRegistry.ModifierConditions.WEATHER.get();
    }

    @Override
    public String toString() {
        return "weather {raining=" + isRaining() + ", thundering=" + isThundering() + "}";
    }
}
