/**
 * Copyright (c) 2023 Skyler James
 * Permission is granted to use, modify, and redistribute this software, in parts or in whole,
 * under the GNU LGPLv3 license (https://www.gnu.org/licenses/lgpl-3.0.en.html)
 **/

package axolootl.block;

import com.google.common.collect.ImmutableMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.EnumMap;
import java.util.Map;

public class PumpBlock extends WaterloggedHorizontalBlock {

    protected static final Map<Direction, VoxelShape> SHAPES = new EnumMap<>(
            ImmutableMap.<Direction, VoxelShape>builder()
                    .put(Direction.NORTH, Shapes.or(
                            Block.box(2.0D, 0.0D, 0.0D, 14.0D, 12.0D, 12.0D),
                            Block.box(3.0D, 1.0D, 12.0D, 13.0D, 11.0D, 16.0D))
                    )
                    .put(Direction.SOUTH, Shapes.or(
                            Block.box(2.0D, 0.0D, 4.0D, 14.0D, 12.0D, 16.0D),
                            Block.box(3.0D, 1.0D, 0.0D, 13.0D, 11.0D, 4.0D))
                    )
                    .put(Direction.EAST, Shapes.or(
                            Block.box(4.0D, 0.0D, 2.0D, 16.0D, 12.0D, 14.0D),
                            Block.box(0.0D, 1.0D, 3.0D, 4.0D, 11.0D, 13.0D))
                    )
                    .put(Direction.WEST, Shapes.or(
                            Block.box(0.0D, 0.0D, 2.0D, 12.0D, 12.0D, 14.0D),
                            Block.box(12.0D, 1.0D, 3.0D, 16.0D, 11.0D, 13.0D))
                    )
                    .build()
    );

    protected static final Vec3 BUBBLE_RADIUS_X = new Vec3(0.0625D, 0.3125D, 0.3125D);
    protected static final Vec3 BUBBLE_RADIUS_Z = new Vec3(0.3125D, 0.3125D, 0.0625D);

    public PumpBlock(Properties pProperties) {
        super(pProperties);
    }

    @Override
    public VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        return SHAPES.getOrDefault(pState.getValue(FACING), Shapes.block());
    }

    @Override
    public void animateTick(BlockState pState, Level pLevel, BlockPos pPos, RandomSource pRandom) {
        if(!pState.getValue(WATERLOGGED) || pRandom.nextInt(3) != 0) {
            return;
        }
        // determine direction
        final Direction direction = pState.getValue(FACING);
        // determine location and radius
        final Vec3 vec = Vec3.atBottomCenterOf(pPos).add(direction.getStepX() * -0.5625D, 0.375D, direction.getStepZ() * -0.5625D);
        final Vec3 radius = direction.getAxis() == Direction.Axis.X ? BUBBLE_RADIUS_X : BUBBLE_RADIUS_Z;
        // add bubble particles
        BubblerBlock.addParticles(pLevel, vec, radius, ParticleTypes.BUBBLE, 1 + pRandom.nextInt(3), 0.09D, pRandom);
        BubblerBlock.addParticles(pLevel, vec, radius, ParticleTypes.BUBBLE_COLUMN_UP, 1, 0.09D, pRandom);
    }


}
