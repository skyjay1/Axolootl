package axolootl.data.aquarium_modifier;

import axolootl.entity.IAxolootl;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.level.LevelAccessor;

import javax.annotation.concurrent.Immutable;
import java.util.Collection;
import java.util.Map;

@Immutable
public class AquariumModifierContext {

    private final LevelAccessor level;
    private final BlockPos pos;
    private final Collection<IAxolootl> axolootls;
    private final Map<BlockPos, AquariumModifier> modifiers;

    public AquariumModifierContext(LevelAccessor level, BlockPos pos, Collection<IAxolootl> axolootls, Map<BlockPos, AquariumModifier> modifiers) {
        this.level = level;
        this.pos = pos;
        this.axolootls = axolootls;
        this.modifiers = modifiers;
    }

    //// GETTERS ////

    public LevelAccessor getLevel() {
        return level;
    }

    public RegistryAccess getRegistryAccess() {
        return level.registryAccess();
    }

    public BlockPos getPos() {
        return pos;
    }

    public Collection<IAxolootl> getAxolootls() {
        return axolootls;
    }

    public Map<BlockPos, AquariumModifier> getModifiers() {
        return modifiers;
    }
}
