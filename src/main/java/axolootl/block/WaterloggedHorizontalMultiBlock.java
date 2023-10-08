/**
 * Copyright (c) 2023 Skyler James
 * Permission is granted to use, modify, and redistribute this software, in parts or in whole,
 * under the GNU LGPLv3 license (https://www.gnu.org/licenses/lgpl-3.0.en.html)
 **/

package axolootl.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

public class WaterloggedHorizontalMultiBlock extends WaterloggedHorizontalBlock {

    /** The X index property **/
    public static final IntegerProperty WIDTH = IntegerProperty.create("width", 0, 3);
    /** The Y index property **/
    public static final IntegerProperty HEIGHT = IntegerProperty.create("height", 0, 3);
    /** The Z index property **/
    public static final IntegerProperty DEPTH = IntegerProperty.create("depth", 0, 3);

    private static final Vec3i CENTER = new Vec3i(1, 1, 1);

    private final Map<BlockState, VoxelShape> SHAPES = new HashMap<>();
    private final Function<BlockState, VoxelShape> shapes;

    /**
     * @param pProperties the block properties
     * @param shapes a function that creates a voxel shape to be cached for later use
     */
    public WaterloggedHorizontalMultiBlock(Properties pProperties, Function<BlockState, VoxelShape> shapes) {
        super(pProperties.dynamicShape());
        this.shapes = shapes;
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(WATERLOGGED, false)
                .setValue(WIDTH, 1)
                .setValue(HEIGHT, 1)
                .setValue(DEPTH, 1));
    }

    //// METHODS ////

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext pContext) {
        /*
         * The BlockPlaceContext is adjusted in the block item to contain the center position
         */
        // prepare to scan area for valid block placement
        final Level level = pContext.getLevel();
        final Direction direction = pContext.getHorizontalDirection().getOpposite();
        // determine center
        final BlockPos center = pContext.getClickedPos();
        // verify below world height
        if(center.getY() + 1 >= level.getMaxBuildHeight()) {
            return null;
        }
        // validate blocks
        if(!allPositions(center, p -> level.getBlockState(p).canBeReplaced(pContext))) {
            return null;
        }
        // place block
        final boolean waterlogged = pContext.getLevel().getFluidState(center).getType() == Fluids.WATER;
        return this.defaultBlockState()
                .setValue(FACING, direction)
                .setValue(WATERLOGGED, waterlogged)
                .setValue(WIDTH, 1)
                .setValue(HEIGHT, 1)
                .setValue(DEPTH, 1);
    }


    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
        pBuilder.add(FACING).add(WATERLOGGED).add(WIDTH).add(HEIGHT).add(DEPTH);
    }

    @Override
    public VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        if(!SHAPES.containsKey(pState)) {
            SHAPES.put(pState, this.shapes.apply(pState));
        }
        return SHAPES.get(pState);
    }

    @Override
    public BlockState updateShape(BlockState pState, Direction pFacing, BlockState pFacingState, LevelAccessor pLevel, BlockPos pCurrentPos, BlockPos pFacingPos) {
        // update waterlogged
        if (pState.getValue(WATERLOGGED)) {
            pLevel.scheduleTick(pCurrentPos, Fluids.WATER, Fluids.WATER.getTickDelay(pLevel));
        }
        // validate block can stay
        final BlockPos center = getCenter(pCurrentPos, pState);
        if(!allPositions(center, p -> pLevel.getBlockState(p).is(this))) {
            return getFluidState(pState).createLegacyBlock();
        }
        return super.updateShape(pState, pFacing, pFacingState, pLevel, pCurrentPos, pFacingPos);
    }

    @Override
    public FluidState getFluidState(BlockState pState) {
        return pState.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(pState);
    }

    @Override
    public void setPlacedBy(Level pLevel, BlockPos pPos, BlockState pState, LivingEntity pPlacer, ItemStack pStack) {
        // determine center
        final Direction direction = pState.getValue(FACING);
        final BlockPos center = getCenter(pPos, pState);
        // place multiblock
        PositionIterator.accept(center, (p, x, y, z) -> {
            // skip this block
            if(pPos.equals(p)) return;
            // determine block to place
            boolean waterlogged = pLevel.getFluidState(p).getType() == Fluids.WATER;
            BlockState state = pState
                    .setValue(WATERLOGGED, waterlogged)
                    .setValue(FACING, direction)
                    .setValue(WIDTH, x)
                    .setValue(HEIGHT, y)
                    .setValue(DEPTH, z);
            // place block
            pLevel.setBlock(p, state, Block.UPDATE_ALL);
        });
    }

    @Override
    public boolean canSurvive(BlockState pState, LevelReader pLevel, BlockPos pPos) {
        if(!pState.getBlock().equals(this)) {
            return true;
        }
        // validate positions
        return allPositions(getCenter(pPos, pState), p -> pLevel.getBlockState(p).is(this));
    }

    @Override
    public void playerWillDestroy(Level pLevel, BlockPos pPos, BlockState pState, Player pPlayer) {
        if (!pLevel.isClientSide && pPlayer.isCreative()) {
            preventCreativeDropFromCenterPart(pLevel, pPos, pState, pPlayer);
        }
        super.playerWillDestroy(pLevel, pPos, pState, pPlayer);
    }

    @Override
    public PushReaction getPistonPushReaction(BlockState pState) {
        return PushReaction.BLOCK;
    }

    public static void preventCreativeDropFromCenterPart(Level pLevel, BlockPos pPos, BlockState pState, Player pPlayer) {
        final BlockPos center = getCenter(pPos, pState);
        final BlockState blockState = pLevel.getBlockState(center);
        if(blockState.is(pState.getBlock()) && getIndex(blockState).equals(CENTER)) {
            pLevel.setBlock(center, blockState.getFluidState().createLegacyBlock(), Block.UPDATE_SUPPRESS_DROPS | Block.UPDATE_ALL);
            pLevel.levelEvent(pPlayer, LevelEvent.PARTICLES_DESTROY_BLOCK, center, Block.getId(blockState));
        }
    }

    //// POSITION HELPERS ////

    public static Vec3i getIndex(final BlockState blockState) {
        return new Vec3i(blockState.getValue(WIDTH), blockState.getValue(HEIGHT), blockState.getValue(DEPTH));
    }

    public static BlockPos getCenter(final BlockPos pos, final Vec3i indices) {
        return pos.offset(-(indices.getX() - 1), -(indices.getY() - 1), -(indices.getZ() - 1));
    }
    public static BlockPos getCenter(final BlockPos pos, final BlockState blockState) {
        return getCenter(pos, getIndex(blockState));
    }

    public static Iterable<BlockPos> getPositions(final BlockPos center) {
        return BlockPos.betweenClosed(center.getX() - 1, center.getY() - 1, center.getZ() - 1, center.getX() + 1, center.getY() + 1, center.getZ() + 1);
    }

    /**
     * @param center the center position
     * @param predicate a predicate to test
     * @return true if all positions passed.
     */
    public static boolean allPositions(final BlockPos center, final Predicate<BlockPos> predicate) {
        for(BlockPos p : getPositions(center)) {
            if(!predicate.test(p)) {
                return false;
            }
        }
        return true;
    }

    /**
     * @param center the center position
     * @param predicate a predicate to test
     * @return true if any position passed.
     */
    public static boolean anyPositions(final BlockPos center, final Predicate<BlockPos> predicate) {
        for(BlockPos p : getPositions(center)) {
            if(predicate.test(p)) {
                return true;
            }
        }
        return false;
    }

    @FunctionalInterface
    private static interface PositionIterator {
        /**
         * @param p the block position
         * @param x the x index in the range [0,3)
         * @param y the y index in the range [0,3)
         * @param z the z index in the range [0,3)
         */
        void accept(BlockPos p, int x, int y, int z);

        public static void accept(BlockPos center, PositionIterator iterator) {
            BlockPos.MutableBlockPos pos = center.mutable();
            for(int x = 0; x < 3; x++) {
                for(int y = 0; y < 3; y++) {
                    for(int z = 0; z < 3; z++) {
                        pos.setWithOffset(center, x - 1, y - 1, z - 1);
                        iterator.accept(pos, x, y, z);
                    }
                }
            }
        }
    }
}
