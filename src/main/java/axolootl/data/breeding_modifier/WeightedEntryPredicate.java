/**
 * Copyright (c) 2023 Skyler James
 * Permission is granted to use, modify, and redistribute this software, in parts or in whole,
 * under the GNU LGPLv3 license (https://www.gnu.org/licenses/lgpl-3.0.en.html)
 **/

package axolootl.data.breeding_modifier;

import axolootl.data.axolootl_variant.AxolootlVariant;
import axolootl.util.AxCodecUtils;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.critereon.MinMaxBounds;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.random.WeightedEntry;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

@Immutable
public class WeightedEntryPredicate implements Predicate<WeightedEntry.Wrapper<ResourceKey<AxolootlVariant>>> {

    public static final Codec<WeightedEntryPredicate> DIRECT_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.optionalFieldOf("variant").forGetter(o -> Optional.ofNullable(o.variantId)),
            AxCodecUtils.POSITIVE_INTS_CODEC.optionalFieldOf("weight").forGetter(o -> Optional.ofNullable(o.weight))
    ).apply(instance, WeightedEntryPredicate::new));

    /**
     * Codec to convert between string and full predicate object
     **/
    public static final Codec<WeightedEntryPredicate> CODEC = Codec.either(Codec.STRING, DIRECT_CODEC)
            .xmap(either -> either.map(s -> new WeightedEntryPredicate(Optional.of(s), Optional.empty()), Function.identity()),
                    o -> o.weight == null && o.variantId != null ? Either.left(o.variantId) : Either.right(o));

    @Nullable
    private final String variantId;
    @Nullable
    private final String namespace;
    @Nullable
    private final String path;
    @Nullable
    private final MinMaxBounds.Ints weight;

    private WeightedEntryPredicate(Optional<String> variantId, Optional<MinMaxBounds.Ints> weight) {
        this.variantId = variantId.orElse(null);
        this.weight = weight.orElse(null);
        if (variantId.isPresent()) {
            // parse variant and mod id
            final String s = variantId.get();
            final int index = s.indexOf(":");
            if (index > 0 && index < s.length() - 1) {
                String sModId = s.substring(0, index);
                String sVariantId = s.substring(index + 1);
                // allow for wildcard in the namespace or path
                this.namespace = sModId.equals("*") ? null : sModId;
                this.path = sVariantId.equals("*") ? null : sVariantId;
            } else {
                this.namespace = null;
                this.path = null;
            }
        } else {
            this.namespace = null;
            this.path = null;
        }
    }

    /**
     * @param entry a weighted entry for an axolootl variant holder
     * @return true if the predicate matches the given axolootl variant
     */
    public boolean test(final WeightedEntry.Wrapper<ResourceKey<AxolootlVariant>> entry) {
        ResourceLocation id = entry.getData().location();
        boolean matches;
        matches = (null == namespace) || id.getNamespace().equals(namespace);
        matches &= (null == path) || id.getPath().equals(path);
        matches &= (null == weight) || this.weight.matches(entry.getWeight().asInt());
        return matches;
    }
}
