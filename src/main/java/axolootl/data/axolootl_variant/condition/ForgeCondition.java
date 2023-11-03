/**
 * Copyright (c) 2023 Skyler James
 * Permission is granted to use, modify, and redistribute this software, in parts or in whole,
 * under the GNU LGPLv3 license (https://www.gnu.org/licenses/lgpl-3.0.en.html)
 **/

package axolootl.data.axolootl_variant.condition;

import axolootl.AxRegistry;
import axolootl.util.AxCodecUtils;
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

    public static final Codec<List<ForgeCondition>> LIST_CODEC = AxCodecUtils.listOrElementCodec(DIRECT_CODEC);
    public static final Codec<List<ForgeCondition>> NON_EMPTY_LIST_CODEC = ExtraCodecs.nonEmptyList(LIST_CODEC);

    public abstract Codec<? extends ForgeCondition> getCodec();

    /**
     * @param access the registry access
     * @return the forge condition registry
     */
    public static Registry<ForgeCondition> getRegistry(final RegistryAccess access) {
        return access.registryOrThrow(AxRegistry.Keys.FORGE_CONDITIONS);
    }
}
