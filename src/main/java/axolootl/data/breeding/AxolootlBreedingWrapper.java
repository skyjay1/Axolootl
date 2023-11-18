/**
 * Copyright (c) 2023 Skyler James
 * Permission is granted to use, modify, and redistribute this software, in parts or in whole,
 * under the GNU LGPLv3 license (https://www.gnu.org/licenses/lgpl-3.0.en.html)
 **/

package axolootl.data.breeding;

import axolootl.AxRegistry;
import axolootl.data.axolootl_variant.AxolootlVariant;
import axolootl.data.breeding_modifier.AxolootlBreedingModifier;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.util.random.SimpleWeightedRandomList;
import net.minecraft.util.random.WeightedEntry;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class AxolootlBreedingWrapper {

    private final AxolootlBreeding breeding;
    private final SimpleWeightedRandomList<ResourceKey<AxolootlVariant>> result;

    public AxolootlBreedingWrapper(final RegistryAccess access, AxolootlBreeding breeding, List<AxolootlBreedingModifier> modifiers) {
        this.breeding = breeding;
        // initialize wrapped result
        final List<WeightedEntry.Wrapper<ResourceKey<AxolootlVariant>>> builder = new ArrayList<>(breeding.getResult().unwrap());
        // before everything
        for(AxolootlBreedingModifier modifier : modifiers) {
            modifier.apply(builder, AxolootlBreedingModifier.Phase.PRE);
        }
        // add
        for(AxolootlBreedingModifier modifier : modifiers) {
            modifier.apply(builder, AxolootlBreedingModifier.Phase.ADD);
        }
        // remove
        for(AxolootlBreedingModifier modifier : modifiers) {
            modifier.apply(builder, AxolootlBreedingModifier.Phase.REMOVE);
        }
        // after everything
        for(AxolootlBreedingModifier modifier : modifiers) {
            modifier.apply(builder, AxolootlBreedingModifier.Phase.POST);
        }
        // remove invalid results
        builder.removeIf(wrapper -> !AxRegistry.AxolootlVariantsReg.isValid(wrapper.getData().location()));
        // build the list
        this.result = new SimpleWeightedRandomList<>(builder);
    }

    /**
     * @param level the level
     * @param first the first variant
     * @param second the second variant
     * @return the axolotl breeding recipe for the given variants, if any
     */
    public static Optional<AxolootlBreedingWrapper> getBreedingRecipe(final Level level, final AxolootlVariant first, final AxolootlVariant second) {
        if(AxolootlVariant.EMPTY.equals(first) || AxolootlVariant.EMPTY.equals(second)) {
            return Optional.empty();
        }
        // find first matching recipe
        final Optional<AxolootlBreeding> match = AxolootlBreeding.getRegistry(level.registryAccess()).entrySet()
                .stream()
                .filter(e -> e.getValue().matches(level, first, second))
                .findFirst()
                .map(Map.Entry::getValue);
        if(match.isEmpty()) {
            return Optional.empty();
        }
        // get or create wrapper
        final AxolootlBreedingWrapper wrapper = AxRegistry.AxolootlBreedingReg.getWrapper(level.registryAccess(), match.get());
        // verify post-processed recipe results are not empty
        if(wrapper.getResult().unwrap().isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(wrapper);
    }

    /**
     * @param level the level
     * @param aFirst the first variant
     * @param aSecond the second variant
     * @param random a random source
     * @return the axolootl variant according to {@link #sampleResult(RandomSource)}, or {@link AxolootlBreeding#getFirst()} if it failed
     */
    public AxolootlVariant getBreedResult(final Level level, final AxolootlVariant aFirst, final AxolootlVariant aSecond, final RandomSource random) {
        return AxolootlVariant.getRegistry(level.registryAccess()).get(sampleResult(random).orElse(breeding.getFirst()).location());
    }

    /**
     * @param random a random source
     * @return a random element from the result set
     */
    public Optional<ResourceKey<AxolootlVariant>> sampleResult(final RandomSource random) {
        final Optional<WeightedEntry.Wrapper<ResourceKey<AxolootlVariant>>> oVariant = getResult().getRandom(random);
        if(oVariant.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(oVariant.get().getData());
    }

    //// GETTERS ////

    public AxolootlBreeding getBreeding() {
        return breeding;
    }

    public SimpleWeightedRandomList<ResourceKey<AxolootlVariant>> getResult() {
        return result;
    }
}
