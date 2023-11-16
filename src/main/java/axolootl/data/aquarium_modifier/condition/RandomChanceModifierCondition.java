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
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;

import javax.annotation.concurrent.Immutable;
import java.util.List;

@Immutable
public class RandomChanceModifierCondition extends ModifierCondition {

    public static final Codec<RandomChanceModifierCondition> CODEC = Codec.doubleRange(0.0D, 1.0D)
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
    public List<Component> createDescription(final RegistryAccess registryAccess) {
        final String sChance = String.format("%.4f", chance * 100.0D).replaceAll("0*$", "").replaceAll("\\.$", "");
        return ImmutableList.of(Component.translatable("axolootl.modifier_condition.random_chance", sChance));
    }

    @Override
    public String toString() {
        return "chance {" + chance + "}";
    }
}
