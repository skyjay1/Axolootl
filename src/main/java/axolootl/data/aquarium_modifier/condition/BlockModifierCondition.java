package axolootl.data.aquarium_modifier.condition;

import axolootl.AxRegistry;
import axolootl.data.aquarium_modifier.AquariumModifier;
import axolootl.data.aquarium_modifier.AquariumModifierContext;
import com.mojang.serialization.Codec;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;

import javax.annotation.concurrent.Immutable;
import java.util.Optional;

@Immutable
public class BlockModifierCondition extends ModifierCondition {

    public static final Codec<BlockModifierCondition> CODEC = AquariumModifier.BLOCK_PREDICATE_CODEC
            .xmap(BlockModifierCondition::new, BlockModifierCondition::getPredicate).fieldOf("predicate").codec();

    private final BlockPredicate predicate;

    public BlockModifierCondition(BlockPredicate predicate) {
        this.predicate = predicate;
    }

    public BlockPredicate getPredicate() {
        return predicate;
    }

    @Override
    public boolean test(AquariumModifierContext aquariumModifierContext) {
        final ServerLevel level = (ServerLevel)aquariumModifierContext.getLevel();
        return this.predicate.test(level, aquariumModifierContext.getPos());
    }

    @Override
    public Codec<? extends ModifierCondition> getCodec() {
        return AxRegistry.ModifierConditions.BLOCK.get();
    }

    @Override
    public String toString() {
        final ResourceLocation id = Registry.BLOCK_PREDICATE_TYPES.getKey(predicate.type());
        if(null == id) {
            return "block {ERROR}";
        }
        return "block {" + id.toString() + "}";
    }
}
