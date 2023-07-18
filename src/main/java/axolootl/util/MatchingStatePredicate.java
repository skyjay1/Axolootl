package axolootl.util;

import axolootl.AxRegistry;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.critereon.StatePropertiesPredicate;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicateType;
import net.minecraft.world.level.levelgen.blockpredicates.StateTestingPredicate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MatchingStatePredicate extends StateTestingPredicate {

    // TODO ranged property matcher is currently unsupported
    public static final Codec<List<StatePropertiesPredicate.PropertyMatcher>> PROPERTY_MATCHER_LIST_CODEC =
            Codec.unboundedMap(Codec.STRING, Codec.STRING)
                    .xmap(map -> {
                        final List<StatePropertiesPredicate.PropertyMatcher> list = new ArrayList<>();
                        map.forEach((key, value) -> list.add(new StatePropertiesPredicate.ExactPropertyMatcher(key, value)));
                        return list;
                    }, list -> {
                        final Map<String, String> map = new HashMap<>();
                        list.forEach(matcher -> map.put(matcher.getName(), ((StatePropertiesPredicate.ExactPropertyMatcher)matcher).value));
                        return map;
                    });

    public static final Codec<StatePropertiesPredicate> STATE_PROPERTIES_PREDICATE_CODEC = PROPERTY_MATCHER_LIST_CODEC.xmap(StatePropertiesPredicate::new, o -> o.properties);

    public static final Codec<MatchingStatePredicate> CODEC = RecordCodecBuilder.create((instance) ->
            stateTestingCodec(instance)
                .and(RegistryCodecs.homogeneousList(Registry.BLOCK_REGISTRY).fieldOf("blocks").forGetter(o -> o.blocks))
                .and(STATE_PROPERTIES_PREDICATE_CODEC.fieldOf("state").forGetter(o -> o.state))
                .apply(instance, MatchingStatePredicate::new));

    private final HolderSet<Block> blocks;
    private final StatePropertiesPredicate state;

    public MatchingStatePredicate(Vec3i offset, HolderSet<Block> blocks, StatePropertiesPredicate state) {
        super(offset);
        this.blocks = blocks;
        this.state = state;
    }

    @Override
    protected boolean test(BlockState pState) {
        if(!pState.is(this.blocks)) {
            return false;
        }
        return this.state.matches(pState);
    }

    @Override
    public BlockPredicateType<?> type() {
        return AxRegistry.BlockPredicateTypesReg.MATCHING_PROPERTY.get();
    }
}
