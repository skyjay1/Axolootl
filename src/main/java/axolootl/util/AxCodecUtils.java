/**
 * Copyright (c) 2023 Skyler James
 * Permission is granted to use, modify, and redistribute this software, in parts or in whole,
 * under the GNU LGPLv3 license (https://www.gnu.org/licenses/lgpl-3.0.en.html)
 **/

package axolootl.util;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.minecraft.core.Registry;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicateType;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;
import java.util.function.Function;

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

    /** Block predicate dispatch codec **/
    public static final Codec<BlockPredicate> BLOCK_PREDICATE_CODEC = Registry.BLOCK_PREDICATE_TYPES.byNameCodec().dispatch(BlockPredicate::type, BlockPredicateType::codec);
    /** Block predicate list or single element codec **/
    public static final Codec<List<BlockPredicate>> BLOCK_PREDICATE_LIST_CODEC = listOrElementCodec(BLOCK_PREDICATE_CODEC);

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

    private static Codec<Integer> hexIntCodec() {
        Function<String, DataResult<String>> function = (s) -> {
            if(s.isEmpty()) {
                return DataResult.error("Failed to parse hex int from empty string");
            }
            return DataResult.success(s);
        };
        return Codec.STRING.flatXmap(function, function).xmap(s -> Integer.valueOf(s, 16), Integer::toHexString);
    }
}
