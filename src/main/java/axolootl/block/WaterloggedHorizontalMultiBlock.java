/**
 * Copyright (c) 2023 Skyler James
 * Permission is granted to use, modify, and redistribute this software, in parts or in whole,
 * under the GNU LGPLv3 license (https://www.gnu.org/licenses/lgpl-3.0.en.html)
 **/

package axolootl.block;

import axolootl.util.ShapeUtils;
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
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
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

    private final Map<BlockState, ShapeData> SHAPES = new HashMap<>();
    private final Map<BlockState, VoxelShape> VISUAL_SHAPES = new HashMap<>();
    private final Function<BlockState, ShapeData> shapes;

    /**
     * @param pProperties the block properties
     * @param shapes a function that creates {@link ShapeData} to be cached for later use
     */
    public WaterloggedHorizontalMultiBlock(Properties pProperties, Function<BlockState, ShapeData> shapes) {
        super(pProperties.dynamicShape());
        this.shapes = shapes;
        this.registerDefaultState(this.stateDefinition.any()
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

    protected void precalculateShapes() {
        SHAPES.clear();
        VISUAL_SHAPES.clear();
        for(BlockState blockState : this.stateDefinition.getPossibleStates()) {
            // no need to cache individual shapes, #createVisualShape will handle that too
            VISUAL_SHAPES.put(blockState, createVisualShape(blockState));
        }
    }

    /**
     * @param blockState the block state
     * @return the cached visual outline for the given block state
     */
    public VoxelShape getOrCreateVisualShape(final BlockState blockState) {
        if(!VISUAL_SHAPES.containsKey(blockState)) {
            VISUAL_SHAPES.put(blockState, createVisualShape(blockState));
        }
        return VISUAL_SHAPES.get(blockState);
    }

    private VoxelShape createVisualShape(final BlockState blockState) {
        final Vec3i offset = getIndex(blockState);
        VoxelShape shape = Shapes.empty();
        for(int x = 0; x < 3; x++) {
            for(int y = 0; y < 3; y++) {
                for(int z = 0; z < 3; z++) {
                    BlockState b = blockState.setValue(WIDTH, x).setValue(HEIGHT, y).setValue(DEPTH, z);
                    shape = ShapeUtils.orUnoptimized(shape, getOrCreateShapeData(b).getCollisionShape().move(x - offset.getX(), y - offset.getY(), z - offset.getZ()));
                }
            }
        }
        return shape.optimize();
    }

    /**
     * @param blockState the block state
     * @return the cached shape data for the given block state
     */
    public ShapeData getOrCreateShapeData(final BlockState blockState) {
        if(!SHAPES.containsKey(blockState)) {
            SHAPES.put(blockState, createShapeData(blockState));
        }
        return SHAPES.get(blockState);
    }

    private ShapeData createShapeData(final BlockState blockState) {
        return this.shapes.apply(blockState);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
        pBuilder.add(FACING).add(WATERLOGGED).add(WIDTH).add(HEIGHT).add(DEPTH);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        final ShapeData shapeData = getOrCreateShapeData(pState);
        if(shapeData.getCollisionShape().isEmpty() || (shapeData.isPassable() && pContext instanceof EntityCollisionContext context && ShapeUtils.canEntityPass(context.getEntity()))) {
            return Shapes.empty();
        }
        return shapeData.getCollisionShape();
    }

    @Override
    public VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        return getOrCreateVisualShape(pState);
        //return getOrCreateShapeData(pState).getCollisionShape();
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
        // assume if the block at the given position is not this one, this is a preemptive check
        if(!pState.is(this)) {
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

    /**
     * @param blockState the block state of a multiblock part
     * @return the [width, height, depth] values of the block state
     */
    public static Vec3i getIndex(final BlockState blockState) {
        return new Vec3i(blockState.getValue(WIDTH), blockState.getValue(HEIGHT), blockState.getValue(DEPTH));
    }

    /**
     * @param pos the block position
     * @param indices the [width, height, depth] values of the block at the given position
     * @return the block position of the center of the 3x3x3 multiblock
     * @see #getIndex(BlockState)
     * @see #getCenter(BlockPos, BlockState)
     */
    public static BlockPos getCenter(final BlockPos pos, final Vec3i indices) {
        return pos.offset(-(indices.getX() - 1), -(indices.getY() - 1), -(indices.getZ() - 1));
    }

    /**
     * @param pos the block position
     * @param blockState the block state of a multiblock part
     * @return the block position of the center of the 3x3x3 multiblock
     * @see #getIndex(BlockState)
     * @see #getCenter(BlockPos, BlockState)
     */
    public static BlockPos getCenter(final BlockPos pos, final BlockState blockState) {
        return getCenter(pos, getIndex(blockState));
    }

    /**
     * @param center the block position of the center of the 3x3x3 multiblock
     * @return an iterable containing block positions for all blocks in the 3x3x3 multiblock
     * @see #getCenter(BlockPos, BlockState)
     * @see #getCenter(BlockPos, Vec3i)
     */
    public static Iterable<BlockPos> getPositions(final BlockPos center) {
        return BlockPos.betweenClosed(center.getX() - 1, center.getY() - 1, center.getZ() - 1, center.getX() + 1, center.getY() + 1, center.getZ() + 1);
    }

    /**
     * @param center the block position of the center of the 3x3x3 multiblock
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
     * @param center the block position of the center of the 3x3x3 multiblock
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
    public static interface PositionIterator {
        /**
         * @param p the block position
         * @param x the x index in the range [0,3)
         * @param y the y index in the range [0,3)
         * @param z the z index in the range [0,3)
         */
        void accept(BlockPos p, int x, int y, int z);

        /**
         * Iterates all blocks in the 3x3x3 multiblock with the given {@link PositionIterator}
         * @param center the block position of the center of the 3x3x3 multiblock
         * @param iterator the position iterator
         */
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

    //// SHAPE HELPERS ////

    public static class ShapeData {

        private final VoxelShape collisionShape;
        private final boolean passable;

        /**
         * Constructs a {@link ShapeData} with the passable set to true only if the given shape is empty
         * @param collisionShape the collision voxel shape
         * @see #ShapeData(VoxelShape, boolean)  ShapeData
         */
        public ShapeData(final VoxelShape collisionShape) {
            this(collisionShape, collisionShape.isEmpty());
        }

        /**
         * @param collisionShape the collision voxel shape
         * @param passable true if small entities can pass through the block without collisions
         */
        public ShapeData(final VoxelShape collisionShape, final boolean passable) {
            this.collisionShape = collisionShape;
            this.passable = passable;
        }

        //// GETTERS ////

        /**
         * @return the collision voxel shape
         */
        public VoxelShape getCollisionShape() {
            return collisionShape;
        }

        /**
         * @return true if small entities can pass through the block without collisions
         */
        public boolean isPassable() {
            return passable;
        }
    }

    /**
     * Creates a new {@link ShapeData} with rotated shapes, using the shape data in a 3d array as a reference
     * @param index the [width, height, depth] index of the blockstate as calculated by {@link #getIndex(BlockState)}
     * @param originDirection the horizontal direction of the shapes in the 3d array
     * @param direction the target direction to rotate towards
     * @param template a 3d array containing base values for all indices of the multiblock
     * @return a new {@link ShapeData} object containing a correctly rotated shape
     */
    public static ShapeData createRotatedIndexedShape(final Vec3i index,
                                                      final Direction originDirection, final Direction direction,
                                                      final ShapeData[][][] template) {
        final Vec3i rotatedIndex = rotateIndex(index, Direction.from2DDataValue(direction.get2DDataValue() - originDirection.get2DDataValue() + 4));
        final ShapeData shapeData = template[rotatedIndex.getY()][rotatedIndex.getX()][rotatedIndex.getZ()];
        return new ShapeData(
                ShapeUtils.rotateShape(originDirection, direction, shapeData.getCollisionShape()),
                shapeData.isPassable());
    }

    /**
     * @param index the original index as calculated by {@link #getIndex(BlockState)}
     * @param direction the direction to rotate
     * @return the index after rotating in the given direction
     * @see #createRotatedIndexedShape(Vec3i, Direction, Direction, ShapeData[][][])
     */
    public static Vec3i rotateIndex(final Vec3i index, final Direction direction) {
        switch (direction) {
            default:
            case WEST: return new Vec3i(-index.getX() + 2, index.getY(), -index.getZ() + 2);
            case NORTH: return new Vec3i(-index.getZ() + 2, index.getY(), index.getX());
            case EAST: return index;
            case SOUTH: return new Vec3i(index.getZ(), index.getY(), -index.getX() + 2);
        }
    }

}
