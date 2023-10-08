/**
 * Copyright (c) 2023 Skyler James
 * Permission is granted to use, modify, and redistribute this software, in parts or in whole,
 * under the GNU LGPLv3 license (https://www.gnu.org/licenses/lgpl-3.0.en.html)
 **/

package axolootl.block;

import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class GrandCastleBlock extends WaterloggedHorizontalMultiBlock {

    public static final int MAX_ENCHANTMENT_LEVEL = 3;
    /** The enchantment level **/
    public static final IntegerProperty ENCHANTMENT_LEVEL = IntegerProperty.create("level", 0, MAX_ENCHANTMENT_LEVEL);
    public static final Enchantment ENCHANTMENT = Enchantments.FISHING_LUCK;

    public GrandCastleBlock(Properties pProperties) {
        super(pProperties, GrandCastleBlock::createShape);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(ENCHANTMENT_LEVEL, 0)
                .setValue(FACING, Direction.NORTH)
                .setValue(WATERLOGGED, false)
                .setValue(WIDTH, 1)
                .setValue(HEIGHT, 1)
                .setValue(DEPTH, 1));
    }

    //// METHODS ////

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext pContext) {
        // determine blockstate from parent class
        BlockState blockState = super.getStateForPlacement(pContext);
        if(null == blockState) {
            return null;
        }
        // determine enchantment level
        int enchantmentLevel = pContext.getItemInHand().getEnchantmentLevel(ENCHANTMENT);
        return blockState.setValue(ENCHANTMENT_LEVEL, enchantmentLevel);
    }


    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
        super.createBlockStateDefinition(pBuilder.add(ENCHANTMENT_LEVEL));
    }

    private static VoxelShape createShape(final BlockState blockState) {
        // TODO grand castle block shapes
        return Shapes.block();
    }
}
