/**
 * Copyright (c) 2023 Skyler James
 * Permission is granted to use, modify, and redistribute this software, in parts or in whole,
 * under the GNU LGPLv3 license (https://www.gnu.org/licenses/lgpl-3.0.en.html)
 **/

package axolootl.data.aquarium_modifier;

import axolootl.entity.IAxolootl;
import axolootl.util.TankMultiblock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.level.LevelAccessor;

import javax.annotation.concurrent.Immutable;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

@Immutable
public class AquariumModifierContext {

    private final LevelAccessor level;
    private final BlockPos pos;
    private final TankMultiblock.Size tankSize;
    private final Collection<IAxolootl> axolootls;
    private final Map<BlockPos, AquariumModifier> modifiers;
    private final Set<BlockPos> activeModifiers;

    /**
     * @param level the level
     * @param pos the block position of the current aquarium modifier
     * @param tankSize the tank size
     * @param axolootls a view of all tracked axolootls
     * @param modifiers a view of all tracked aquarium modifiers, excluding the current one
     * @param activeModifiers the set of block positions that contained active modifiers before the current evaluation
     */
    public AquariumModifierContext(LevelAccessor level, BlockPos pos,
                                   TankMultiblock.Size tankSize, Collection<IAxolootl> axolootls,
                                   Map<BlockPos, AquariumModifier> modifiers, Set<BlockPos> activeModifiers) {
        this.level = level;
        this.pos = pos;
        this.tankSize = tankSize;
        this.axolootls = axolootls;
        this.modifiers = modifiers;
        this.activeModifiers = activeModifiers;
    }

    //// GETTERS ////

    /**
     * @return the level
     */
    public LevelAccessor getLevel() {
        return level;
    }

    /**
     * @return the registry access
     */
    public RegistryAccess getRegistryAccess() {
        return level.registryAccess();
    }

    /**
     * @return the tank size
     */
    public TankMultiblock.Size getTankSize() {
        return tankSize;
    }

    /**
     * @return true if the tank size exists and is not empty
     */
    public boolean hasTank() {
        return tankSize != null && tankSize != TankMultiblock.Size.EMPTY;
    }

    /**
     * @return the modifier block position
     */
    public BlockPos getPos() {
        return pos;
    }

    /**
     * @return a view of all tracked axolootls
     */
    public Collection<IAxolootl> getAxolootls() {
        return axolootls;
    }

    /**
     * @return a view of the aquarium modifiers
     */
    public Map<BlockPos, AquariumModifier> getModifiers() {
        return modifiers;
    }

    /**
     * @return the block positions of modifiers that were active in the previous tick
     */
    public Set<BlockPos> getActiveModifiers() {
        return activeModifiers;
    }

    /**
     * @param pos a block position
     * @return true if the modifier at the given position was active in the previous tick
     */
    public boolean isModifierActive(BlockPos pos) {
        return activeModifiers.contains(pos);
    }
}
