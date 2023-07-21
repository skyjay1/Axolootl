/**
 * Copyright (c) 2023 Skyler James
 * Permission is granted to use, modify, and redistribute this software, in parts or in whole,
 * under the GNU LGPLv3 license (https://www.gnu.org/licenses/lgpl-3.0.en.html)
 **/

package axolootl.data.aquarium_modifier.condition;

import axolootl.AxRegistry;
import axolootl.data.aquarium_modifier.AquariumModifierContext;
import com.mojang.serialization.Codec;

import javax.annotation.concurrent.Immutable;

@Immutable
public class RandomChanceModifierCondition extends ModifierCondition {

    public static final Codec<RandomChanceModifierCondition> CODEC = Codec.DOUBLE
            .xmap(RandomChanceModifierCondition::new, RandomChanceModifierCondition::getChance).fieldOf("chance").codec();

    private final double chance;

    public RandomChanceModifierCondition(double chance) {
        this.chance = chance;
    }

    public double getChance() {
        return chance;
    }

    @Override
    public boolean test(AquariumModifierContext aquariumModifierContext) {
        return getChance() > 0 && aquariumModifierContext.getLevel().getRandom().nextDouble() < getChance();
    }

    @Override
    public Codec<? extends ModifierCondition> getCodec() {
        return AxRegistry.ModifierConditionsReg.EXISTS.get();
    }

    @Override
    public String toString() {
        return "chance {" + chance + "}";
    }
}
