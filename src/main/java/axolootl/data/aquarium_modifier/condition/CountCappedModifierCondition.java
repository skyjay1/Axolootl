package axolootl.data.aquarium_modifier.condition;

import axolootl.AxRegistry;
import axolootl.data.aquarium_modifier.AquariumModifierContext;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.valueproviders.IntProvider;

import javax.annotation.concurrent.Immutable;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * checks if the number of modifiers N is less than some value.
 * If it’s not, checks if this modifier has a smaller BlockPos than the other N - [value] modifiers.
 * This allows the first [value] modifiers to be active and any extras will be deactivated
 */
@Immutable
public class CountCappedModifierCondition extends CountModifierCondition {

    public CountCappedModifierCondition(ResourceLocation modifierId, IntProvider count) {
        super(modifierId, count);
    }

    @Override
    public boolean test(AquariumModifierContext aquariumModifierContext) {
        final BlockPos pos = aquariumModifierContext.getPos();
        final List<BlockPos> modifiers = new LinkedList<>();
        for(Map.Entry<BlockPos, ResourceLocation> entry : aquariumModifierContext.getModifiers().entrySet()) {
            // count matching modifiers
            if(entry.getValue().equals(getModifierId())) {
                insertSorted(modifiers, entry.getKey());
            }
        }
        // check if count is within range
        if(modifiers.size() <= getCount().getMaxValue()) {
            return true;
        }
        // check if count is above max and blockpos is not small enough
        return sortedIndexOf(modifiers, pos) > getCount().getMaxValue();
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
        return 0;
    }

    @Override
    public Codec<? extends ModifierCondition> getCodec() {
        return AxRegistry.ModifierConditions.COUNT_CAPPED.get();
    }

    @Override
    public String toString() {
        return "count_capped {count=(" + getCount().getMinValue() + "," + getCount().getMaxValue() + ") modifier=" + getModifierId() + "}";
    }
}
