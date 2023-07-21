/**
 * Copyright (c) 2023 Skyler James
 * Permission is granted to use, modify, and redistribute this software, in parts or in whole,
 * under the GNU LGPLv3 license (https://www.gnu.org/licenses/lgpl-3.0.en.html)
 **/

package axolootl.data.aquarium_modifier.condition;

import axolootl.AxRegistry;
import axolootl.data.aquarium_modifier.AquariumModifier;
import axolootl.data.aquarium_modifier.AquariumModifierContext;
import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.resources.RegistryFileCodec;
import net.minecraft.util.ExtraCodecs;

import javax.annotation.concurrent.Immutable;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

@Immutable
public abstract class ModifierCondition implements Predicate<AquariumModifierContext> {

    public static final Codec<ModifierCondition> DIRECT_CODEC = ExtraCodecs.lazyInitializedCodec(() -> AxRegistry.MODIFIER_CONDITION_SERIALIZERS_SUPPLIER.get().getCodec())
            .dispatch(ModifierCondition::getCodec, Function.identity());

    public static final Codec<Holder<ModifierCondition>> HOLDER_CODEC = RegistryFileCodec.create(AxRegistry.Keys.MODIFIER_CONDITIONS, DIRECT_CODEC);
    public static final Codec<HolderSet<ModifierCondition>> HOLDER_SET_CODEC = RegistryCodecs.homogeneousList(AxRegistry.Keys.MODIFIER_CONDITIONS, DIRECT_CODEC);

    public static final Codec<List<ModifierCondition>> LIST_CODEC = DIRECT_CODEC.listOf();
    public static final Codec<List<ModifierCondition>> LIST_OR_SINGLE_CODEC = Codec.either(DIRECT_CODEC, LIST_CODEC)
            .xmap(either -> either.map(ImmutableList::of, Function.identity()),
                    list -> list.size() == 1 ? Either.left(list.get(0)) : Either.right(list));

    public abstract Codec<? extends ModifierCondition> getCodec();

    /**
     * @param access the registry access
     * @return the axolootl variant registry
     */
    public static Registry<ModifierCondition> getRegistry(final RegistryAccess access) {
        return access.registryOrThrow(AxRegistry.Keys.MODIFIER_CONDITIONS);
    }
}
