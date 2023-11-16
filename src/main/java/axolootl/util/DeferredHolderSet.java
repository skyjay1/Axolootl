/**
 * Copyright (c) 2023 Skyler James
 * Permission is granted to use, modify, and redistribute this software, in parts or in whole,
 * under the GNU LGPLv3 license (https://www.gnu.org/licenses/lgpl-3.0.en.html)
 **/

package axolootl.util;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Datapack object holder sets are not synced, so this class exists to sync the underlying values instead.
 * @param <T> the holder set type
 */
public class DeferredHolderSet<T> {

    private final Either<TagKey<T>, List<ResourceKey<T>>> either;
    private HolderSet<T> holderSet;

    public DeferredHolderSet(TagKey<T> tagKey) {
        this(Either.left(tagKey));
    }

    public DeferredHolderSet(List<ResourceKey<T>> list) {
        this(Either.right(list));
    }

    public DeferredHolderSet(Either<TagKey<T>, List<ResourceKey<T>>> either) {
        this.either = either;
    }

    public Either<TagKey<T>, List<ResourceKey<T>>> unwrap() {
        return either;
    }

    /**
     * Creates and caches a holder set.
     * Do not call this method until datapacks have finished syncing.
     * @param registry the underlying registry
     * @return the holder set
     */
    public HolderSet<T> get(final Registry<T> registry) {
        if(null == this.holderSet) {
            this.holderSet = build(registry, unwrap());
        }
        return this.holderSet;
    }

    public static <T> Codec<DeferredHolderSet<T>> codec(final ResourceKey<? extends Registry<T>> registry) {
        return eitherCodec(registry).xmap(DeferredHolderSet::new, DeferredHolderSet::unwrap);
    }

    /**
     * Datapack object holder sets are not synced, so this method provides a codec for the underlying objects instead
     * @param registry the registry
     * @param <T> the element type
     * @return a codec that allows a single resource key, list of resource keys, or tag key
     */
    public static <T> Codec<Either<TagKey<T>, List<ResourceKey<T>>>> eitherCodec(final ResourceKey<? extends Registry<T>> registry) {
        return Codec.either(TagKey.hashedCodec(registry), AxCodecUtils.listOrElementCodec(ResourceKey.codec(registry)));
    }

    /**
     * Builds a holder set using the tag key or resource key values. Do not call until datapacks have finished syncing.
     * @param registry the registry
     * @param either either the tag key or the resource key list
     * @param <T> the element type
     * @return a new holder set with the given values
     */
    private static <T> HolderSet<T> build(final Registry<T> registry, final Either<TagKey<T>, List<ResourceKey<T>>> either) {
        if(either.left().isPresent()) {
            // TODO all single element values are being parsed as tags when they should be parsed as resource keys
            // TODO make a codec that only parses to tag key if the string begins with #
            return registry.getOrCreateTag(either.left().get());
        }
        if(either.right().isPresent()) {
            List<Holder<T>> list = new ArrayList<>();
            for(ResourceKey<T> key : either.right().get()) {
                Optional<? extends Holder<T>> oHolder = registry.getHolder(key);
                // only add valid holders (invalid holders are skipped)
                if(oHolder.isPresent()) {
                    list.add(oHolder.get());
                }
            }
            return HolderSet.direct(list);
        }
        throw new IllegalStateException("[ModifierCondition#asHolderSet] Either has neither left nor right! " + either);
    }
}
