/**
 * Copyright (c) 2023 Skyler James
 * Permission is granted to use, modify, and redistribute this software, in parts or in whole,
 * under the GNU LGPLv3 license (https://www.gnu.org/licenses/lgpl-3.0.en.html)
 **/

package axolootl.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Accepts an ItemStack and returns the BlockState to replace, if any
 */
@FunctionalInterface
public interface BlockConverter extends Function<ItemStack, Optional<BlockState>> {

    /**
     * Helper method to create a function for registration
     * @param item the itemstack supplier to trigger this function
     * @param block the block whose default state will be placed for the given item
     * @return a function that can be easily registered where needed
     */
    public static BlockConverter itemStackConverter(final Supplier<ItemStack> item, final Supplier<Block> block) {
        return itemStack -> {
            if(!ItemStack.isSame(itemStack, item.get())) {
                return Optional.empty();
            }
            return Optional.of(block.get().defaultBlockState());
        };
    }

    /**
     * Helper method to create a function for registration
     * @param item the item to trigger this function
     * @param block the block whose default state will be placed for the given item
     * @return a {@link BlockConverter}
     */
    public static BlockConverter itemConverter(final Item item, final Supplier<Block> block) {
        return itemStack -> {
            if(!itemStack.is(item)) {
                return Optional.empty();
            }
            return Optional.of(block.get().defaultBlockState());
        };
    }

    /**
     * Helper method to create a function for registration
     * @param item the item to trigger this function
     * @param block the block whose default state will be placed for the given item
     * @return a {@link BlockConverter}
     */
    public static BlockConverter itemConverter(final Supplier<Item> item, final Supplier<Block> block) {
        return itemStack -> {
            if(!itemStack.is(item.get())) {
                return Optional.empty();
            }
            return Optional.of(block.get().defaultBlockState());
        };
    }

    /**
     * Places the given block and shrinks the item stack count
     * @param level the level
     * @param pos the block position
     * @param player the player
     * @param hand the interaction hand
     * @param itemStack the item stack
     * @param blockState the block state to place
     * @param flags the block flag constants (can be ORed)
     * @return true if the level is client-side or the block was placed
     */
    public static boolean convert(Level level, BlockPos pos, Player player, InteractionHand hand, ItemStack itemStack, BlockState blockState, final int flags) {
        // validate server side
        if(level.isClientSide()) {
            return true;
        }
        // replace the block
        if(!level.setBlock(pos, blockState, flags)) {
            return false;
        }
        // consume the item
        if(!player.getAbilities().instabuild) {
            itemStack.shrink(1);
        }
        return true;
    }
}
