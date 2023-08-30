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
import net.minecraft.advancements.critereon.MinMaxBounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderSet;

import javax.annotation.concurrent.Immutable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * checks if the number of modifiers N is less than some value.
 * If it’s not, checks if this modifier has a smaller BlockPos than the other N - [value] modifiers.
 * This allows the first [value] modifiers to be active and any extras will be deactivated
 */
@Immutable
public class CountCappedModifierCondition extends CountModifierCondition {

    public static final Codec<CountCappedModifierCondition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            AquariumModifier.HOLDER_SET_CODEC.fieldOf("modifier").forGetter(CountCappedModifierCondition::getModifiers),
            Codec.intRange(0, Integer.MAX_VALUE).fieldOf("count").forGetter(CountCappedModifierCondition::getMaxCount),
            Codec.BOOL.optionalFieldOf("active", false).forGetter(CountCappedModifierCondition::isRequireActive)
    ).apply(instance, CountCappedModifierCondition::new));

    private final int maxCount;

    public CountCappedModifierCondition(CountModifierCondition copy) {
        this(copy.getModifiers(), Optional.ofNullable(copy.getCount().getMax()).orElse(Optional.ofNullable(copy.getCount().getMin()).orElse(0)), copy.isRequireActive());
    }

    public CountCappedModifierCondition(HolderSet<AquariumModifier> modifierId, final int count, boolean requireActive) {
        super(modifierId, MinMaxBounds.Ints.atMost(count), requireActive);
        this.maxCount = count;
    }

    public int getMaxCount() {
        return this.maxCount;
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
        if(modifiers.size() < getMaxCount()) {
            return true;
        }
        // check if blockpos is small enough
        return sortedIndexOf(modifiers, pos) < getMaxCount();
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
        return "count_capped {count=(" + getMaxCount() + ") modifier=" + getModifiers() + "}";
    }
}
