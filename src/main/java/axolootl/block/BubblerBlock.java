/**
 * Copyright (c) 2023 Skyler James
 * Permission is granted to use, modify, and redistribute this software, in parts or in whole,
 * under the GNU LGPLv3 license (https://www.gnu.org/licenses/lgpl-3.0.en.html)
 **/

package axolootl.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BubbleColumnBlock;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class BubblerBlock extends Block implements SimpleWaterloggedBlock {

    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    protected static final VoxelShape SHAPE = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 8.0D, 16.0D);
    protected static final int BUBBLE_COLUMN_CHECK_DELAY = 20;
    private static final Vec3 BUBBLE_RADIUS = new Vec3(0.375D, 0.125D, 0.375D);

    public BubblerBlock(Properties pProperties) {
        super(pProperties);
    }

    //// METHODS ////

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext pContext) {
        FluidState fluidstate = pContext.getLevel().getFluidState(pContext.getClickedPos());
        boolean waterlogged = fluidstate.getType() == Fluids.WATER;
        return this.defaultBlockState().setValue(WATERLOGGED, waterlogged);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
        pBuilder.add(WATERLOGGED);
    }

    @Override
    public VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        return SHAPE;
    }

    @Override
    public BlockState updateShape(BlockState pState, Direction pFacing, BlockState pFacingState, LevelAccessor pLevel, BlockPos pCurrentPos, BlockPos pFacingPos) {
        if (pState.getValue(WATERLOGGED)) {
            pLevel.scheduleTick(pCurrentPos, Fluids.WATER, Fluids.WATER.getTickDelay(pLevel));
        }
        return super.updateShape(pState, pFacing, pFacingState, pLevel, pCurrentPos, pFacingPos);
    }

    @Override
    public FluidState getFluidState(BlockState pState) {
        return pState.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(pState);
    }

    /*@Override
    public void tick(BlockState pState, ServerLevel pLevel, BlockPos pPos, RandomSource pRandom) {
        final BlockPos above = pPos.above();
        BubbleColumnBlock.updateColumn(pLevel, above, pLevel.getBlockState(above), Blocks.SOUL_SAND.defaultBlockState());
    }*/

    /*@Override
    public void onPlace(BlockState pState, Level pLevel, BlockPos pPos, BlockState pOldState, boolean pIsMoving) {
        pLevel.scheduleTick(pPos, this, BUBBLE_COLUMN_CHECK_DELAY);
    }*/

    protected boolean canAnimateParticles(BlockState state, Level level, BlockPos pos, RandomSource random) {
        return state.getValue(WATERLOGGED);
    }

    @Override
    public void animateTick(BlockState pState, Level pLevel, BlockPos pPos, RandomSource pRandom) {
        if(!canAnimateParticles(pState, pLevel, pPos, pRandom)) {
            return;
        }
        // add bubble particles
        Vec3 vec = Vec3.atBottomCenterOf(pPos).add(0, 0.625D, 0);
        addParticles(pLevel, vec, BUBBLE_RADIUS, ParticleTypes.BUBBLE, 2 + pRandom.nextInt(3), 0.09D, pRandom);
        addParticles(pLevel, vec, BUBBLE_RADIUS, ParticleTypes.BUBBLE_COLUMN_UP, 6, 0.09D, pRandom);
    }

    public static void addParticles(final Level level, final Vec3 pos, final Vec3 radius, final ParticleOptions particle,
                                       final int count, final double motion, final RandomSource random) {
        for(int i = 0; i < count; i++) {
            double dx = (random.nextDouble() - 0.5D) * 2.0D * radius.x();
            double dy = (random.nextDouble() - 0.5D) * 2.0D * radius.y();
            double dz = (random.nextDouble() - 0.5D) * 2.0D * radius.z();
            double vx = (random.nextDouble() - 0.5D) * 2.0D * motion;
            double vy = (random.nextDouble()) * 2.0D * motion;
            double vz = (random.nextDouble() - 0.5D) * 2.0D * motion;
            level.addParticle(particle, pos.x() + dx, pos.y() + dy, pos.z() + dz, vx, vy, vz);
        }
    }
}
