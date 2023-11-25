/**
 * Copyright (c) 2023 Skyler James
 * Permission is granted to use, modify, and redistribute this software, in parts or in whole,
 * under the GNU LGPLv3 license (https://www.gnu.org/licenses/lgpl-3.0.en.html)
 **/

package axolootl.util;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.critereon.FluidPredicate;
import net.minecraft.advancements.critereon.LightPredicate;
import net.minecraft.advancements.critereon.MinMaxBounds;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.Vec3i;
import net.minecraft.tags.TagKey;
import net.minecraft.util.random.SimpleWeightedRandomList;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;

public final class AxCodecUtils {

    /** Codec to map between items and item stacks with a single item and no tag **/
    public static final Codec<ItemStack> ITEM_OR_STACK_CODEC = Codec.either(ForgeRegistries.ITEMS.getCodec(), ItemStack.CODEC)
            .xmap(either -> either.map(ItemStack::new, Function.identity()),
                    stack -> stack.getCount() == 1 && !stack.hasTag()
                            ? Either.left(stack.getItem())
                            : Either.right(stack));

    /** Codec to validate and read an integer as a hex string **/
    public static final Codec<Integer> HEX_INT_CODEC = hexIntCodec();
    /** Codec to accept either a hex string or a raw integer **/
    public static final Codec<Integer> HEX_OR_INT_CODEC = Codec.either(HEX_INT_CODEC, Codec.INT)
            .xmap(either -> either.map(Function.identity(), Function.identity()),
                    i -> Either.right(i));

    /** {@link Rarity} codec that uses lowercase names to look up rarity enums **/
    public static final Codec<Rarity> RARITY_CODEC = Codec.STRING.xmap(AxCodecUtils::getRarityByName, r -> r.toString().toLowerCase(Locale.ENGLISH));
    /** {@link Vec3i} codec that requires all values to be 0 or greater **/
    public static final Codec<Vec3i> NON_NEGATIVE_VEC3I_CODEC = vec3Codec(0);
    /** {@link Vec3i} codec that requires all values to be 1 or greater **/
    public static final Codec<Vec3i> POSITIVE_VEC3I_CODEC = vec3Codec(1);
    /** {@link MinMaxBounds.Ints} codec **/
    public static final Codec<MinMaxBounds.Ints> INTS_DIRECT_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("min").forGetter(o -> Optional.ofNullable(o.getMin())),
            Codec.INT.optionalFieldOf("max").forGetter(o -> Optional.ofNullable(o.getMax()))
    ).apply(instance, (p1, p2) -> {
        if(p1.isPresent() && p2.isEmpty()) return MinMaxBounds.Ints.atLeast(p1.get());
        if(p1.isEmpty() && p2.isPresent()) return MinMaxBounds.Ints.atMost(p2.get());
        if(p1.isEmpty() && p2.isEmpty()) return MinMaxBounds.Ints.ANY;
        return MinMaxBounds.Ints.between(p1.get(), p2.get());
    }));
    /** {@link MinMaxBounds.Ints} or {@link Codec#INT} codec **/
    public static final Codec<MinMaxBounds.Ints> INTS_CODEC = Codec.either(Codec.INT, INTS_DIRECT_CODEC)
            .xmap(either -> either.map(MinMaxBounds.Ints::exactly, Function.identity()),
                    o -> (o.getMin() != null && o.getMax() != null && o.getMin().equals(o.getMax())) ? Either.left(o.getMin()) : Either.right(o));
    /** {@link MinMaxBounds.Ints} or {@link Codec#INT} codec that requires the value to be 0 or greater **/
    public static final Codec<MinMaxBounds.Ints> NON_NEGATIVE_INTS_CODEC = boundedIntCodec(0, Integer.MAX_VALUE);
    /** {@link MinMaxBounds.Ints} or {@link Codec#INT} codec that requires the value to be 1 or greater **/
    public static final Codec<MinMaxBounds.Ints> POSITIVE_INTS_CODEC = boundedIntCodec(1, Integer.MAX_VALUE);
    /** {@link LightPredicate} codec **/
    public static final Codec<LightPredicate> LIGHT_PREDICATE_CODEC = INTS_CODEC.xmap(composite -> new LightPredicate.Builder().setComposite(composite).build(), o -> o.composite);
    /** {@link FluidPredicate} codec (partly supported) **/
    public static final Codec<FluidPredicate> FLUID_PREDICATE_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            TagKey.codec(ForgeRegistries.Keys.FLUIDS).optionalFieldOf("tag").forGetter(o -> Optional.ofNullable(o.tag)),
            ForgeRegistries.FLUIDS.getCodec().optionalFieldOf("fluid").forGetter(o -> Optional.ofNullable(o.fluid)),
            // TODO state properties predicate is only partly supported
            MatchingStatePredicate.STATE_PROPERTIES_PREDICATE_CODEC.optionalFieldOf("state").forGetter(o -> Optional.of(o.properties))
    ).apply(instance, (tag, fluid, state) -> {
        final FluidPredicate.Builder builder = FluidPredicate.Builder.fluid();
        tag.ifPresent(builder::of);
        fluid.ifPresent(builder::of);
        state.ifPresent(builder::setProperties);
        return builder.build();
    }));

    public static final Pattern RESOURCE_LOCATION_PATTERN = Pattern.compile("(?:[a-z0-9_.]+:)?[a-z0-9_./-]+");

    /**
     * @param codec an element codec
     * @param <T> the element type
     * @return a codec that allows either a single element or a list of elements
     */
    public static <T> Codec<List<T>> listOrElementCodec(final Codec<T> codec) {
       return Codec.either(codec, codec.listOf())
                .xmap(either -> either.map(ImmutableList::of, Function.identity()),
                        list -> list.size() == 1 ? Either.left(list.get(0)) : Either.right(list));
    }

    /**
     * @param codec an element codec
     * @param <T> the element type
     * @return a codec that allows either a single element or a list of elements
     */
    public static <T> Codec<Set<T>> setOrElementCodec(final Codec<T> codec) {
        return Codec.either(codec, codec.listOf().xmap(o -> (Set<T>) ImmutableSet.copyOf(o), ImmutableList::copyOf))
                .xmap(either -> either.map(ImmutableSet::of, Function.identity()),
                        set -> set.size() == 1 ? Either.left(set.iterator().next()) : Either.right(set));
    }


    /**
     * @param codec an element codec
     * @param <T> the element type
     * @return a codec that allows either a single element or a weighted list of elements
     */
    public static <T> Codec<SimpleWeightedRandomList<T>> weightedListOrElementCodec(final Codec<T> codec) {
        return Codec.either(codec, SimpleWeightedRandomList.wrappedCodec(codec))
                .xmap(either -> either.map(SimpleWeightedRandomList::single, Function.identity()),
                        list -> list.unwrap().size() == 1 ? Either.left(list.unwrap().get(0).getData()) : Either.right(list));
    }

    /**
     * @return a codec that converts between hex formatted strings and packed decimal integers
     */
    private static Codec<Integer> hexIntCodec() {
        final Pattern pattern = Pattern.compile("[0-9a-fA-F]+");
        Function<String, DataResult<String>> function = (s) -> {
            if(s.isEmpty()) {
                return DataResult.error("Failed to parse hex int from empty string");
            }
            if(!pattern.matcher(s).matches()) {
                return DataResult.error("Invalid hex int " + s);
            }
            return DataResult.success(s);
        };
        return Codec.STRING.flatXmap(function, function).xmap(s -> Integer.valueOf(s, 16), Integer::toHexString);
    }

    /**
     * @param name the lowercase rarity name
     * @return the rarity with the given name, or {@link Rarity#COMMON} if no matches were found
     */
    private static Rarity getRarityByName(final String name) {
        for(Rarity rarity : Rarity.values()) {
            if(rarity.toString().toLowerCase(Locale.ENGLISH).equals(name)) {
                return rarity;
            }
        }
        return Rarity.COMMON;
    }

    /**
     * @param min the minimum XYZ value
     * @return a codec that fails when the provided vec has any values below the given minimum
     */
    public static Codec<Vec3i> vec3Codec(final int min) {
        Function<Vec3i, DataResult<Vec3i>> function = (instance) -> {
            if (instance.getX() < min || instance.getY() < min || instance.getZ() < min) {
                return DataResult.error("Vec3i too low. minimum " + min + "; provided [" + instance.toShortString() + "]");
            } else {
                return DataResult.success(instance);
            }
        };
        return Vec3i.CODEC.flatXmap(function, function);
    }

    /**
     * @param min the minimum value, inclusive
     * @param max the maximum value, inclusive
     * @return a codec that fails when the min or max of the int is outside the given range
     */
    public static Codec<MinMaxBounds.Ints> boundedIntCodec(final int min, final int max) {
        Function<MinMaxBounds.Ints, DataResult<MinMaxBounds.Ints>> function = (instance) -> {
            if (instance.getMin() != null && instance.getMin() < min) {
                return DataResult.error("Value too low. minimum " + min + "; provided [" + instance + "]");
            } else if(instance.getMax() != null && instance.getMax() > max) {
                return DataResult.error("Value too high. maximum " + max + "; provided [" + instance + "]");
            } else {
                return DataResult.success(instance);
            }
        };
        return INTS_CODEC.flatXmap(function, function);
    }
}
