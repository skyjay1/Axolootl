package axolootl.data.aquarium_modifier;

import axolootl.block.entity.IAxolootlVariantProvider;
import axolootl.data.AxolootlVariant;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.LevelAccessor;

import javax.annotation.concurrent.Immutable;
import java.util.Collection;
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
    private final Collection<IAxolootlVariantProvider> axolootls;
    private final Map<BlockPos, AquariumModifier> modifiers;

    public AquariumModifierContext(LevelAccessor level, BlockPos pos, Collection<IAxolootlVariantProvider> axolootls, Map<BlockPos, AquariumModifier> modifiers) {
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

    public Collection<IAxolootlVariantProvider> getAxolootls() {
        return axolootls;
    }

    public Map<BlockPos, AquariumModifier> getModifiers() {
        return modifiers;
    }
}
