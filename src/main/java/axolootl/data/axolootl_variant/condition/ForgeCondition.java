package axolootl.data.axolootl_variant.condition;

import axolootl.AxRegistry;
import axolootl.data.aquarium_modifier.condition.ModifierCondition;
import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.util.ExtraCodecs;

import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

public abstract class ForgeCondition implements Predicate<ForgeConditionContext> {

    public static final Codec<ForgeCondition> DIRECT_CODEC = ExtraCodecs.lazyInitializedCodec(() -> AxRegistry.FORGE_CONDITION_SERIALIZERS_SUPPLIER.get().getCodec())
            .dispatch(ForgeCondition::getCodec, Function.identity());

    public static final Codec<List<ForgeCondition>> LIST_CODEC = DIRECT_CODEC.listOf();
    public static final Codec<List<ForgeCondition>> LIST_OR_SINGLE_CODEC = Codec.either(DIRECT_CODEC, LIST_CODEC)
            .xmap(either -> either.map(ImmutableList::of, Function.identity()),
                    list -> list.size() == 1 ? Either.left(list.get(0)) : Either.right(list));
    public static final Codec<List<ForgeCondition>> NON_EMPTY_LIST_CODEC = ExtraCodecs.nonEmptyList(LIST_OR_SINGLE_CODEC);

    public abstract Codec<? extends ForgeCondition> getCodec();

    /**
     * @param access the registry access
     * @return the forge condition registry
     */
    public static Registry<ForgeCondition> getRegistry(final RegistryAccess access) {
        return access.registryOrThrow(AxRegistry.Keys.FORGE_CONDITIONS);
    }
}
