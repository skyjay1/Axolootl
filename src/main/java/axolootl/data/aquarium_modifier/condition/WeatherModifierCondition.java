/**
 * Copyright (c) 2023 Skyler James
 * Permission is granted to use, modify, and redistribute this software, in parts or in whole,
 * under the GNU LGPLv3 license (https://www.gnu.org/licenses/lgpl-3.0.en.html)
 **/

package axolootl.data.aquarium_modifier.condition;

import axolootl.AxRegistry;
import axolootl.data.aquarium_modifier.AquariumModifierContext;
import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import java.util.List;
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
        return AxRegistry.ModifierConditionsReg.WEATHER.get();
    }

    @Override
    public List<Component> createDescription(final RegistryAccess registryAccess) {
        // create components
        final Component cRaining = Component.translatable("axolootl.modifier_condition.weather.raining");
        final Component cThundering = Component.translatable("axolootl.modifier_condition.weather.thundering");
        Component cRainingText = null;
        Component cThunderingText = null;
        if(isRaining != null) {
            cRainingText = isRaining ? cRaining : Component.translatable("axolootl.modifier_condition.weather.not", cRaining);
        }
        if(isThundering != null) {
            cThunderingText = isThundering ? cThundering : Component.translatable("axolootl.modifier_condition.weather.not", cThundering);
        }
        // create description
        if(cRainingText != null && cThunderingText != null) {
            return ImmutableList.of(Component.translatable("axolootl.modifier_condition.weather.multiple", cRainingText, cThunderingText));
        } else if(cRainingText != null) {
            return ImmutableList.of(Component.translatable("axolootl.modifier_condition.weather.single", cRainingText));
        } else if(cThunderingText != null) {
            return ImmutableList.of(Component.translatable("axolootl.modifier_condition.weather.single", cThunderingText));
        } else {
            return ImmutableList.of(Component.translatable("axolootl.modifier_condition.weather.never"));
        }
    }

    @Override
    public String toString() {
        return "weather {raining=" + isRaining() + ", thundering=" + isThundering() + "}";
    }
}
