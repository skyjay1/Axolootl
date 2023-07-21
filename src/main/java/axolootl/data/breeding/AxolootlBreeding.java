/**
 * Copyright (c) 2023 Skyler James
 * Permission is granted to use, modify, and redistribute this software, in parts or in whole,
 * under the GNU LGPLv3 license (https://www.gnu.org/licenses/lgpl-3.0.en.html)
 **/

package axolootl.data.breeding;

import axolootl.AxRegistry;
import axolootl.data.axolootl_variant.AxolootlVariant;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.util.RandomSource;
import net.minecraft.util.random.SimpleWeightedRandomList;
import net.minecraft.util.random.WeightedEntry;
import net.minecraft.world.level.Level;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

public class AxolootlBreeding {

    private static final Codec<SimpleWeightedRandomList<Holder<AxolootlVariant>>> WEIGHTED_LIST_DIRECT_CODEC =
            SimpleWeightedRandomList.wrappedCodec(AxolootlVariant.HOLDER_CODEC);

    private static final Codec<SimpleWeightedRandomList<Holder<AxolootlVariant>>> WEIGHTED_LIST_CODEC = Codec.either(AxolootlVariant.HOLDER_CODEC, WEIGHTED_LIST_DIRECT_CODEC)
            .xmap(either -> either.map(SimpleWeightedRandomList::single, Function.identity()),
                    list -> list.unwrap().size() == 1 ? Either.left(list.unwrap().get(0).getData()) : Either.right(list));


    public static final Codec<AxolootlBreeding> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            AxolootlVariant.HOLDER_SET_CODEC.fieldOf("first").forGetter(AxolootlBreeding::getFirst),
            AxolootlVariant.HOLDER_SET_CODEC.fieldOf("second").forGetter(AxolootlBreeding::getSecond),
            WEIGHTED_LIST_CODEC.fieldOf("result").forGetter(AxolootlBreeding::getResult)
    ).apply(instance, AxolootlBreeding::new));


    /** The first axolootl variant **/
    private final HolderSet<AxolootlVariant> first;
    /** The second axolootl variant **/
    private final HolderSet<AxolootlVariant> second;
    /** A weighted list to determine the result, if it is empty the first variant is used instead **/
    private final SimpleWeightedRandomList<Holder<AxolootlVariant>> result;

    public AxolootlBreeding(HolderSet<AxolootlVariant> first, HolderSet<AxolootlVariant> second,
                                  SimpleWeightedRandomList<Holder<AxolootlVariant>> result) {
        this.first = first;
        this.second = second;
        this.result = result;
    }

    //// METHODS ////

    /**
     * @param access the registry access
     * @return the axolootl variant registry
     */
    public static Registry<AxolootlBreeding> getRegistry(final RegistryAccess access) {
        return access.registryOrThrow(AxRegistry.Keys.AXOLOOTL_BREEDING);
    }

    /**
     * @param level the level
     * @param first the first variant
     * @param second the second variant
     * @return the axolotl breeding recipe for the given variants, if any
     */
    public static Optional<AxolootlBreeding> getBreedingRecipe(final Level level, final AxolootlVariant first, final AxolootlVariant second) {
        return getRegistry(level.registryAccess()).entrySet()
                .stream()
                .filter(e -> e.getValue().matches(level, first, second))
                .map(Map.Entry::getValue)
                .findFirst();
    }

    /**
     * @param level the level
     * @param aFirst the first variant
     * @param aSecond the second variant
     * @return true if both variants match the variants defined in this object
     * @see #getFirst()
     * @see #getSecond()
     */
    public boolean matches(final Level level, final AxolootlVariant aFirst, final AxolootlVariant aSecond) {
        // create holders
        final Holder<AxolootlVariant> hFirst = aFirst.getHolder(level.registryAccess());
        final Holder<AxolootlVariant> hSecond = aSecond.getHolder(level.registryAccess());
        // check holder sets (order does not matter)
        return (this.first.contains(hFirst) && this.second.contains(hSecond)
            || (this.first.contains(hSecond) && this.second.contains(hFirst)));
    }

    /**
     * @param level the level
     * @param aFirst the first variant
     * @param aSecond the second variant
     * @param random a random source
     * @return the axolootl variant according to {@link #sampleResult(RandomSource)}, or {@link #getFirst()} if it failed
     */
    public Holder<AxolootlVariant> getBreedResult(final Level level, final AxolootlVariant aFirst, final AxolootlVariant aSecond, final RandomSource random) {
         return sampleResult(random).orElse(first.getRandomElement(random).orElseThrow());
    }

    /**
     * @param random a random source
     * @return a random element from the result set
     */
    public Optional<Holder<AxolootlVariant>> sampleResult(final RandomSource random) {
        final Optional<WeightedEntry.Wrapper<Holder<AxolootlVariant>>> oVariant = getResult().getRandom(random);
        if(oVariant.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(oVariant.get().getData());
    }

    //// GETTERS ////

    public HolderSet<AxolootlVariant> getFirst() {
        return first;
    }

    public HolderSet<AxolootlVariant> getSecond() {
        return second;
    }

    public SimpleWeightedRandomList<Holder<AxolootlVariant>> getResult() {
        return result;
    }
}
