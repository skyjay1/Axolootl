/**
 * Copyright (c) 2023 Skyler James
 * Permission is granted to use, modify, and redistribute this software, in parts or in whole,
 * under the GNU LGPLv3 license (https://www.gnu.org/licenses/lgpl-3.0.en.html)
 **/

package axolootl.data.aquarium_modifier.condition;

import axolootl.AxRegistry;
import axolootl.data.aquarium_modifier.AquariumModifier;
import axolootl.data.aquarium_modifier.AquariumModifierContext;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderSet;
import net.minecraft.util.valueproviders.IntProvider;

import javax.annotation.concurrent.Immutable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * checks if the number of modifiers N is less than some value.
 * If it’s not, checks if this modifier has a smaller BlockPos than the other N - [value] modifiers.
 * This allows the first [value] modifiers to be active and any extras will be deactivated
 */
@Immutable
public class CountCappedModifierCondition extends CountModifierCondition {

    public static final Codec<CountCappedModifierCondition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            AquariumModifier.HOLDER_SET_CODEC.fieldOf("modifier").forGetter(CountModifierCondition::getModifiers),
            IntProvider.NON_NEGATIVE_CODEC.fieldOf("count").forGetter(CountModifierCondition::getCount)
    ).apply(instance, CountCappedModifierCondition::new));

    public CountCappedModifierCondition(CountModifierCondition copy) {
        this(copy.getModifiers(), copy.getCount());
    }

    public CountCappedModifierCondition(HolderSet<AquariumModifier> modifierId, IntProvider count) {
        super(modifierId, count);
    }

    @Override
    public boolean test(AquariumModifierContext context) {
        final BlockPos pos = context.getPos();
        final List<BlockPos> modifiers = new LinkedList<>();
        for(Map.Entry<BlockPos, AquariumModifier> entry : context.getModifiers().entrySet()) {
            // count matching modifiers
            if(getModifiers().contains(entry.getValue().getHolder(context.getRegistryAccess()))) {
                insertSorted(modifiers, entry.getKey());
            }
        }
        // check if count is within range
        if(modifiers.size() < getCount().getMaxValue()) {
            return true;
        }
        // check if blockpos is small enough
        return sortedIndexOf(modifiers, pos) < getCount().getMaxValue();
    }

    private void insertSorted(final List<BlockPos> list, final BlockPos pos) {
        list.add(sortedIndexOf(list, pos), pos);
    }

    private int sortedIndexOf(final List<BlockPos> list, final BlockPos pos) {
        if(list.isEmpty()) {
            return 0;
        }
        for(int i = 0, n = list.size(); i < n; i++) {
            if(pos.compareTo(list.get(i)) < 0) {
                return i;
            }
        }
        return list.size();
    }

    @Override
    public Codec<? extends ModifierCondition> getCodec() {
        return AxRegistry.ModifierConditionsReg.COUNT_CAPPED.get();
    }

    @Override
    public String toString() {
        return "count_capped {count=(" + getCount().getMinValue() + "," + getCount().getMaxValue() + ") modifier=" + getModifiers() + "}";
    }
}
