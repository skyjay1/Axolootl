/**
 * Copyright (c) 2023 Skyler James
 * Permission is granted to use, modify, and redistribute this software, in parts or in whole,
 * under the GNU LGPLv3 license (https://www.gnu.org/licenses/lgpl-3.0.en.html)
 **/

package axolootl.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractGlassBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

public class AquariumGlassBlock extends AbstractGlassBlock {

    private static final List<BlockConverter> BLOCK_CONVERTERS = new ArrayList<>();
    public static final List<BlockConverter> BLOCK_CONVERTERS_VIEW = Collections.unmodifiableList(BLOCK_CONVERTERS);

    public AquariumGlassBlock(Properties pProperties) {
        super(pProperties);
    }

    @Override
    public boolean skipRendering(BlockState pState, BlockState pAdjacentBlockState, Direction pSide) {
        return pAdjacentBlockState.is(AbstractInterfaceBlock.GLASS)
                || pAdjacentBlockState.is(AbstractInterfaceBlock.AQUARIUM)
                || super.skipRendering(pState, pAdjacentBlockState, pSide);
    }

    @Override
    public InteractionResult use(BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer, InteractionHand pHand, BlockHitResult pHit) {
        ItemStack itemStack = pPlayer.getItemInHand(pHand);
        Direction direction = pHit.getDirection();
        if(!pPlayer.isShiftKeyDown()) {
            // iterate each conversion function until one is successful
            for (BlockConverter f : BLOCK_CONVERTERS) {
                Optional<BlockState> oState = f.apply(itemStack);
                if (oState.isPresent()) {
                    BlockState state = oState.get();
                    // detect and apply horizontal facing based on hit result
                    if (state.hasProperty(HorizontalDirectionalBlock.FACING)) {
                        state = state.setValue(HorizontalDirectionalBlock.FACING, direction);
                    }
                    // convert the block
                    convert(pLevel, pPos, pPlayer, pHand, itemStack, state);
                    return InteractionResult.SUCCESS;
                }
            }
        }
        return super.use(pState, pLevel, pPos, pPlayer, pHand, pHit);
    }

    /**
     * Places the given block and shrinks the item stack count
     * @param level the level
     * @param pos the block position
     * @param player the player
     * @param hand the interaction hand
     * @param itemStack the item stack
     * @param blockState the block state to place
     * @return true if the level is client-side or the block was placed
     */
    private boolean convert(Level level, BlockPos pos, Player player, InteractionHand hand, ItemStack itemStack, BlockState blockState) {
        final boolean result = BlockConverter.convert(level, pos, player, hand, itemStack, blockState, Block.UPDATE_ALL);
        return result;
    }

    /**
     * Registers an item stack function and its resulting block state when the item is used on this block.
     * For best results, call during the {@link net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent} event
     * @param function a function that accepts an itemstack and returns either a block state or an empty optional
     * @see BlockConverter#itemConverter(Item, Supplier)
     * @see BlockConverter#itemConverter(Supplier, Supplier)
     */
    public static void registerItemConverter(final BlockConverter function) {
        BLOCK_CONVERTERS.add(function);
    }
}
