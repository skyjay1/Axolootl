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
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.valueproviders.IntProvider;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import java.util.List;
import java.util.Optional;

@Immutable
public class TimeModifierCondition extends ModifierCondition {

    public static final Codec<TimeModifierCondition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.LONG.optionalFieldOf("period").forGetter(TimeModifierCondition::getPeriod),
            IntProvider.NON_NEGATIVE_CODEC.fieldOf("range").forGetter(TimeModifierCondition::getRange)
    ).apply(instance, TimeModifierCondition::new));

    @Nullable
    private final Long period;
    private final IntProvider range;
    private final List<Component> description;

    public TimeModifierCondition(Optional<Long> period, IntProvider range) {
        this.period = period.orElse(null);
        this.range = range;
        if(period.isPresent()) {
            this.description = ImmutableList.of(Component.translatable("axolootl.modifier_condition.time.period", range.getMinValue(), range.getMaxValue(), period.get()));
        } else {
            this.description = ImmutableList.of(Component.translatable("axolootl.modifier_condition.time", range.getMinValue(), range.getMaxValue()));
        }
    }

    public Optional<Long> getPeriod() {
        return Optional.ofNullable(period);
    }

    public IntProvider getRange() {
        return range;
    }

    @Override
    public boolean test(AquariumModifierContext aquariumModifierContext) {
        ServerLevel serverlevel = (ServerLevel) aquariumModifierContext.getLevel();
        long i = serverlevel.getDayTime();
        if (this.period != null) {
            i %= this.period;
        }
        return i >= this.range.getMinValue() && i <= this.range.getMaxValue();

    }

    @Override
    public Codec<? extends ModifierCondition> getCodec() {
        return AxRegistry.ModifierConditionsReg.TIME.get();
    }

    @Override
    public List<Component> getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return "time {range=(" + getRange().getMinValue() + "," + getRange().getMaxValue() + ") period=" + Optional.ofNullable(getPeriod()) + "}";
    }
}
