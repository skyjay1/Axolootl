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
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.critereon.MinMaxBounds;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import java.util.List;
import java.util.Optional;

@Immutable
public class TimeModifierCondition extends ModifierCondition {

    public static final Codec<TimeModifierCondition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.LONG.optionalFieldOf("period").forGetter(TimeModifierCondition::getPeriod),
            AxCodecUtils.NON_NEGATIVE_INTS_CODEC.fieldOf("range").forGetter(TimeModifierCondition::getRange)
    ).apply(instance, TimeModifierCondition::new));

    @Nullable
    private final Long period;
    private final MinMaxBounds.Ints range;

    public TimeModifierCondition(Optional<Long> period, MinMaxBounds.Ints range) {
        this.period = period.orElse(null);
        this.range = range;
    }

    public Optional<Long> getPeriod() {
        return Optional.ofNullable(period);
    }

    public MinMaxBounds.Ints getRange() {
        return range;
    }

    @Override
    public boolean test(AquariumModifierContext aquariumModifierContext) {
        ServerLevel serverlevel = (ServerLevel) aquariumModifierContext.getLevel();
        long i = serverlevel.getDayTime();
        if (this.period != null) {
            i %= this.period;
        }
        return this.getRange().matches((int) i);

    }

    @Override
    public Codec<? extends ModifierCondition> getCodec() {
        return AxRegistry.ModifierConditionsReg.TIME.get();
    }

    @Override
    public List<Component> createDescription(final RegistryAccess registryAccess) {
        int min = Optional.ofNullable(range.getMin()).orElse(0);
        int max = Optional.ofNullable(range.getMax()).orElse(0);
        if(period != null) {
            return ImmutableList.of(Component.translatable("axolootl.modifier_condition.time.period", min, max, period));
        }
        return ImmutableList.of(Component.translatable("axolootl.modifier_condition.time", min, max));
    }

    @Override
    public String toString() {
        return "time {range=(" + Optional.ofNullable(getRange().getMin()) + "," + Optional.ofNullable(getRange().getMax()) + ") period=" + Optional.ofNullable(getPeriod()) + "}";
    }
}
