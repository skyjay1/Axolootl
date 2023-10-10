/**
 * Copyright (c) 2023 Skyler James
 * Permission is granted to use, modify, and redistribute this software, in parts or in whole,
 * under the GNU LGPLv3 license (https://www.gnu.org/licenses/lgpl-3.0.en.html)
 **/

package axolootl.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;

public class BastionMultiBlock extends WaterloggedHorizontalMultiBlock {

    public BastionMultiBlock(Properties pProperties) {
        super(pProperties, BastionMultiBlock::createShape);
    }

    //// SHAPE ////

    /**
     * Shape data for each block in the default horizontal direction, ordered by index [height][width][depth]
     **/
    private static final ShapeData[][][] SHAPE_DATA = new ShapeData[][][] {
        // height = 0
        {
            // width = 0
            {
                new ShapeData(Block.box(4, 0, 12, 16, 16, 16)),
                new ShapeData(Shapes.or(
                        Block.box(6, 0, 10, 10, 16, 13),
                        Block.box(6, 0, 3, 10, 16, 6))),
                new ShapeData(Shapes.empty())
            },
            // width = 1
            {
                new ShapeData(Shapes.block()),
                new ShapeData(Shapes.block()),
                new ShapeData(Block.box(0, 0, 2, 16, 16, 16))
            },
            // width 2
            {
                new ShapeData(Shapes.or(
                        Block.box(0, 0, 0, 12, 16, 8),
                        Block.box(12, 0, 0, 16, 16, 4))),
                new ShapeData(Block.box(0, 0, 0, 16, 16, 14)),
                new ShapeData(Block.box(0, 0, 0, 16, 16, 8))
            }
        },
        // height = 1
        {
            // width = 0
            {
                new ShapeData(Block.box(4, 0, 12, 16, 16, 16)),
                new ShapeData(Block.box(4, 0, 1, 12, 4, 16), true),
                new ShapeData(Shapes.empty())
            },
            // width = 1
            {
                new ShapeData(Shapes.block()),
                new ShapeData(Shapes.join(
                        Shapes.block(),
                        Block.box(2, 2, 0, 14, 16, 14),
                        BooleanOp.ONLY_FIRST), true),
                new ShapeData(Shapes.or(
                        Block.box(0, 0, 2, 16, 14, 16),
                        Block.box(8, 14, 2, 16, 16, 16)))
            },
            // width 2
            {
                new ShapeData(Shapes.or(
                        Block.box(0, 0, 0, 12, 16, 8),
                        Block.box(12, 0, 0, 16, 16, 4))),
                new ShapeData(Shapes.join(
                                Block.box(0, 0, 0, 16, 16, 8),
                                Block.box(2, 0, 0, 14, 12, 8),
                                BooleanOp.ONLY_FIRST), true),
                new ShapeData(Block.box(0, 0, 0, 16, 16, 8))
            }
        },
        // height = 2
        {
            // width = 0
            {
                new ShapeData(Shapes.empty()),
                new ShapeData(Shapes.empty()),
                new ShapeData(Shapes.empty())
            },
            // width = 1
            {
                new ShapeData(Shapes.or(
                        Block.box(0, 0, 0, 16, 5, 16),
                        Block.box(0, 5, 0, 4, 12, 16))),
                new ShapeData(Block.box(0, 0, 0, 16, 14, 16)),
                new ShapeData(Block.box(8, 0, 2, 16, 8, 16))
            },
            // width 2
            {
                new ShapeData(Shapes.or(
                        Block.box(0, 0, 0, 12, 5, 8),
                        Block.box(12, 0, 0, 16, 5, 4),
                        Block.box(0, 5, 0, 4, 12, 8))),
                new ShapeData(Block.box(0, 0, 0, 16, 12, 8)),
                new ShapeData(Shapes.empty())
            }
        }
    };

    private static ShapeData createShape(final BlockState blockState) {
        return createRotatedShapeData(blockState, SHAPE_DATA);
    }
}
