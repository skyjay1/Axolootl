package axolootl.data.aquarium_modifier;

import axolootl.data.AxolootlVariant;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.LevelAccessor;

import javax.annotation.concurrent.Immutable;
import java.util.List;
import java.util.Map;

@Immutable
public class AquariumModifierContext {
    /*
    -level: LevelAccessor
-pos: BlockPos
-axolootls: List<Axolootl Variant>
-modifiers: Map<BlockPos, ResourceLocation> (map of all other modifier positions and IDs, excluding this one)

     */
    private final LevelAccessor level;
    private final BlockPos pos;
    private final List<AxolootlVariant> axolootls;
    private final Map<BlockPos, ResourceLocation> modifiers;

    public AquariumModifierContext(LevelAccessor level, BlockPos pos, List<AxolootlVariant> axolootls, Map<BlockPos, ResourceLocation> modifiers) {
        this.level = level;
        this.pos = pos;
        this.axolootls = axolootls;
        this.modifiers = modifiers;
    }

    //// GETTERS ////

    public LevelAccessor getLevel() {
        return level;
    }

    public BlockPos getPos() {
        return pos;
    }

    public List<AxolootlVariant> getAxolootls() {
        return axolootls;
    }

    public Map<BlockPos, ResourceLocation> getModifiers() {
        return modifiers;
    }
}
