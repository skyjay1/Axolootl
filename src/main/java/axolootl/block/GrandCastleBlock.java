/**
 * Copyright (c) 2023 Skyler James
 * Permission is granted to use, modify, and redistribute this software, in parts or in whole,
 * under the GNU LGPLv3 license (https://www.gnu.org/licenses/lgpl-3.0.en.html)
 **/

package axolootl.block;

import axolootl.util.ShapeUtils;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;

public class GrandCastleBlock extends WaterloggedHorizontalMultiBlock {

    public static final int MAX_ENCHANTMENT_LEVEL = 3;
    /** The enchantment level **/
    public static final IntegerProperty ENCHANTMENT_LEVEL = IntegerProperty.create("level", 0, MAX_ENCHANTMENT_LEVEL);
    public static final Enchantment ENCHANTMENT = Enchantments.FISHING_LUCK;

    private static final Direction originDirection = Direction.NORTH;

    public GrandCastleBlock(Properties pProperties) {
        super(pProperties, GrandCastleBlock::createShape);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(ENCHANTMENT_LEVEL, 0)
                .setValue(FACING, Direction.NORTH)
                .setValue(WATERLOGGED, false)
                .setValue(WIDTH, 1)
                .setValue(HEIGHT, 1)
                .setValue(DEPTH, 1));
        this.precalculateShapes();
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

    /**
     * Shape data for each block in the default horizontal direction, ordered by index [height][width][depth]
     **/
    private static final ShapeData[][][] SHAPE_DATA = new ShapeData[][][] {
        // height = 0
        {
            // width = 0
            {
                new ShapeData(Block.box(0, 0, 9, 7, 16, 16), true),
                new ShapeData(Shapes.or(
                        Block.box(11, 0, 7, 16, 16, 16),
                        Block.box(0, 15.99D, 15, 11, 16, 16),
                        Block.box(0, 0, 7, 2, 16, 15),
                        Block.box(2, 0, 6, 11, 16, 15)), false),
                new ShapeData(Block.box(6, 0, 6, 16, 16, 16), false)
            },
            // width = 1
            {
                new ShapeData(Shapes.or(
                        Block.box(1, 14, 7, 8, 16, 14),
                        Block.box(0, 13.99D, 0, 7, 14, 16),
                        Shapes.join(
                                Block.box(7, 0, 0, 11, 16, 16),
                                Block.box(7, 0, 4, 11, 12, 12),
                                BooleanOp.ONLY_FIRST)), true),
                new ShapeData(Block.box(0, 14, 0, 16, 16, 16), true),
                new ShapeData(Shapes.join(
                        Block.box(4, 0, 0, 8, 16, 16),
                        Block.box(4, 0, 4, 8, 12, 12),
                        BooleanOp.ONLY_FIRST
                ), true)
            },
            // width = 2
            {
                new ShapeData(Block.box(0, 0, 0, 9, 14, 9), false),
                new ShapeData(Shapes.or(
                        Block.box(8, 0, 4, 15, 16, 11),
                        Block.box(1, 0, 4, 10, 16, 13),
                        Block.box(15, 0, 0, 16, 14, 9),
                        Block.box(10, 14, 0, 12, 16, 2),
                        Block.box(1, 13.99D, 0, 15, 14, 5),
                        Block.box(0, 0, 0, 1, 16, 9)), false),
                new ShapeData(Shapes.or(
                        Block.box(6, 0, 0, 10, 16, 4),
                        Block.box(10, 0, 0, 16, 16, 9)), false)
            }
        },
        // height = 1
        {
            // width = 0
            {
                new ShapeData(Shapes.or(
                        Block.box(0, 2, 8, 8, 6, 16),
                        Block.box(0, 0, 9, 7, 2, 16),
                        Block.box(2.5D, 6.0D, 11.5D, 4.5D, 11.0D, 13.5D)), false),
                new ShapeData(Shapes.or(
                        Block.box(2, 0, 6, 11, 12, 15),
                        Block.box(1, 12, 5, 12, 16, 16),
                        Block.box(15, 2, 8, 16, 6, 16)), false),
                new ShapeData(Shapes.empty(), false)
            },
            // width = 1
            {
                new ShapeData(Shapes.or(
                        Block.box(1, 0, 7, 8, 8, 14),
                        Block.box(0, 8, 6, 9, 12, 15),
                        Block.box(0, 2, 0, 8, 6, 1)), false),
                new ShapeData(Shapes.or(
                        Block.box(3, 0, 3, 13, 16, 13),
                        Block.box(0, 0, 6, 3, 12, 10),
                        Block.box(9, 9, 13, 13, 11, 16),
                        Block.box(15, 2, 0, 16, 6, 1)), false),
                new ShapeData(Block.box(7, 0, 0, 16, 12, 16), false)
            },
            // width = 2
            {
                new ShapeData(Shapes.empty(), false),
                new ShapeData(Shapes.or(
                        Block.box(8, 0, 4, 15, 16, 11),
                        Block.box(9, 9, 0, 13, 11, 4),
                        Block.box(1, 0, 4, 10, 4, 13),
                        Block.box(0, 4, 3, 11, 8, 14),
                        Block.box(10, 0, 0, 12, 9, 2)), false),
                new ShapeData(Block.box(12.5D, 0, 4.5D, 14.5D, 5.0D, 6.5D), true)
            }
        },
        // height = 2
        {
            // width = 0
            {
                new ShapeData(Shapes.empty(), false),
                new ShapeData(Shapes.empty(), false),
                new ShapeData(Shapes.empty(), false)
            },
            // width = 1
            {
                new ShapeData(Shapes.empty(), false),
                new ShapeData(Shapes.or(
                        Block.box(3, 0, 3, 13, 9, 13),
                        Block.box(2, 9, 2, 14, 13, 14)), false),
                new ShapeData(Shapes.empty(), false)
            },
            // width 2
            {
                new ShapeData(Shapes.empty(), false),
                new ShapeData(Block.box(7, 0, 3, 16, 4, 12), false),
                new ShapeData(Shapes.empty(), false)
            }
        }
    };

    private static ShapeData createShape(final BlockState blockState) {
        final int width = blockState.getValue(WIDTH);
        final int depth = blockState.getValue(DEPTH);
        final int height = blockState.getValue(HEIGHT);
        final Direction facing = blockState.getValue(FACING);

        return ShapeUtils.createRotatedIndexedShape(new Vec3i(width, height, depth), originDirection, facing, SHAPE_DATA);
    }
}
