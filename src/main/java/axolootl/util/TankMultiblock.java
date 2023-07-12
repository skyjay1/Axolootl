package axolootl.util;

import axolootl.Axolootl;
import com.google.common.collect.Iterables;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.concurrent.Immutable;
import java.util.Optional;

@Immutable
public class TankMultiblock {

    public static final TagKey<Block> AQUARIUM_BLOCKS = ForgeRegistries.BLOCKS.tags().createTagKey(new ResourceLocation(Axolootl.MODID, "aquarium"));

    /** Default parameters for the multiblock instance **/
    public static TankMultiblock AQUARIUM = new TankMultiblock(new Vec3i(3, 3, 3), new Vec3i(64, 128, 64), AQUARIUM_BLOCKS);

    /** The minimum XYZ sizes of the multiblock **/
    public final BlockPos minSize;
    /** The maximum XYZ sizes of the multiblock **/
    public final BlockPos maxSize;
    /** The block tag to validate multiblock bounds **/
    public final TagKey<Block> blocks;
    /** The minimum volume of the multiblock **/
    public final long minVolume;
    /** The maximum volume of the multiblock **/
    public final long maxVolume;

    /**
     * @param minSize The minimum XYZ sizes of the multiblock
     * @param maxSize The maximum XYZ sizes of the multiblock
     * @param blocks The block tag to validate multiblock bounds
     */
    public TankMultiblock(final Vec3i minSize, final Vec3i maxSize, TagKey<Block> blocks) {
        // validate min and max sizes
        if(minSize.getX() < 1 || minSize.getY() < 1 || minSize.getZ() < 1
                || maxSize.getX() < 1 || maxSize.getY() < 1 || maxSize.getZ() < 1) {
            throw new IllegalArgumentException("[TankMultiblock] Invalid size bounds! All values must be greater than zero. min=" + minSize + " max=" + maxSize);
        }
        // shrink size bounds by 1 to accommodate offset
        this.minSize = new BlockPos(Math.max(0, minSize.getX() - 1), Math.max(0, minSize.getY() - 1), Math.max(0, minSize.getZ() - 1));
        this.maxSize = new BlockPos(Math.max(this.minSize.getX(), maxSize.getX() - 1), Math.max(this.minSize.getX(), maxSize.getY() - 1), Math.max(this.minSize.getZ(), maxSize.getZ() - 1));
        this.blocks = blocks;
        this.minVolume = (long) minSize.getX() * minSize.getY() * minSize.getZ();
        this.maxVolume = (long) maxSize.getX() * maxSize.getY() * maxSize.getZ();
    }

    /**
     * Checks if a tank exists and returns the size if found
     * @param level the level
     * @param pos a position on the outer bounds of the multiblock
     * @return the multiblock size, if a valid multiblock was found
     * @see #isInsideTankStructure(LevelAccessor, BlockPos)
     */
    public Optional<TankMultiblock.Size> hasTankStructure(final LevelAccessor level, final BlockPos pos) {
        // determine multiblock size
        final BlockPos origin = calculateOrigin(level, pos);
        final BlockPos end = calculateEnd(level, pos);
        final Vec3i dimensions = end.subtract(origin);
        // validate dimensions
        final int volume = dimensions.getX() * dimensions.getY() * dimensions.getZ();
        if(volume < minVolume || volume > maxVolume
                || dimensions.getX() < minSize.getX() || dimensions.getY() < minSize.getY() || dimensions.getZ() < minSize.getZ()
                || dimensions.getX() > maxSize.getX() || dimensions.getY() > maxSize.getY() || dimensions.getZ() > maxSize.getZ()) {
            return Optional.empty();
        }
        // create size object
        final TankMultiblock.Size size = new TankMultiblock.Size(origin, dimensions);
        // validate blocks
        for(BlockPos p : size.outerPositions()) {
            if(!isTankBlock(level, p)) {
                return Optional.empty();
            }
        }
        // all checks passed
        return Optional.of(size);
    }

    /**
     * Checks if a multiblock exists with the given size
     * @param level the level
     * @param size the multiblock size
     * @return true if there is a multiblock with the given size, false if there is no structure, and empty if the area is not loaded
     */
    public Optional<Boolean> hasTankStructure(final LevelAccessor level, final TankMultiblock.Size size) {
        // validate area loaded
        final BlockPos minPos = size.getMinChunk().getMiddleBlockPosition(size.getOrigin().getY());
        final BlockPos maxPos = size.getMaxChunk().getMiddleBlockPosition(size.getOrigin().getY());
        if(!level.hasChunksAt(minPos, maxPos)) {
            return Optional.empty();
        }
        // validate blocks
        for(BlockPos p : size.outerPositions()) {
            if(!isTankBlock(level, p)) {
                return Optional.of(false);
            }
        }
        // all checks passed
        return Optional.of(true);
    }

    /**
     * @param level the level
     * @param pos the block position
     * @return true if the block at the given position is a multiblock building block
     */
    public boolean isTankBlock(final LevelAccessor level, final BlockPos pos) {
        return level.getBlockState(pos).is(blocks);
    }

    /**
     * Locates the first multiblock building block at or below the given position in order to calculate the tank origin
     * @param level the level
     * @param pos the position
     * @return the result of {@link #hasTankStructure(LevelAccessor, BlockPos)} for the lowest block at or below this position
     */
    public Optional<TankMultiblock.Size> isInsideTankStructure(final LevelAccessor level, final BlockPos pos) {
        BlockPos.MutableBlockPos cursor = pos.mutable();
        // move down until cursor is on a valid block
        int offsetY = 0;
        while(!level.isOutsideBuildHeight(cursor) && !isTankBlock(level, cursor) && (offsetY++) < maxSize.getY()) {
            cursor.move(Direction.DOWN);
        }
        return hasTankStructure(level, cursor);
    }

    /**
     * Locates the smallest coordinate after tracing aquarium blocks in all negative directions
     * @param level the level
     * @param pos a position on the outer bounds of the multiblock
     * @return the coordinate of the tank origin, may be the same as {@code pos}
     * @see #trace(LevelAccessor, BlockPos, Direction.AxisDirection)
     */
    public BlockPos calculateOrigin(final LevelAccessor level, final BlockPos pos) {
        return trace(level, pos, Direction.AxisDirection.NEGATIVE);
    }

    /**
     * Locates the largest coordinate after tracing aquarium blocks in all positive directions
     * @param level the level
     * @param pos a position on the outer bounds of the multiblock
     * @return the coordinate of the tank end, may be the same as {@code pos}
     * @see #trace(LevelAccessor, BlockPos, Direction.AxisDirection)
     */
    public BlockPos calculateEnd(final LevelAccessor level, final BlockPos pos) {
        return trace(level, pos, Direction.AxisDirection.POSITIVE);
    }

    /**
     * Locates the smallest or largest coordinate after tracing multiblock blocks in the given directions
     * @param level the level
     * @param pos a position on the outer bounds of the multiblock
     * @param axis the direction to search
     * @return the coordinate of the tank end, may be the same as {@code pos}
     */
    public BlockPos trace(final LevelAccessor level, final BlockPos pos, final Direction.AxisDirection axis) {
        // determine directions
        final Direction axisX, axisY, axisZ;
        if(axis == Direction.AxisDirection.NEGATIVE) {
            axisX = Direction.WEST;
            axisY = Direction.DOWN;
            axisZ = Direction.NORTH;
        } else {
            axisX = Direction.EAST;
            axisY = Direction.UP;
            axisZ = Direction.SOUTH;
        }
        // prepare to move in each direction
        BlockPos.MutableBlockPos cursor = pos.mutable();
        BlockPos.MutableBlockPos next = pos.mutable();
        // move down until cursor is on the lowest valid block
        int offsetY = 0;
        while(!level.isOutsideBuildHeight(next) && isTankBlock(level, next.move(axisY)) && (offsetY++) < maxSize.getY()) {
            cursor.set(next);
        }
        next.set(cursor);
        // move west until cursor is on the west-most valid block
        int offsetX = 0;
        while(isTankBlock(level, next.move(axisX)) && (offsetX++) < maxSize.getX()) {
            cursor.set(next);
        }
        next.set(cursor);
        // move north until cursor is on the north-most valid block
        int offsetZ = 0;
        while(isTankBlock(level, next.move(axisZ)) && (offsetZ++) < maxSize.getZ()) {
            cursor.set(next);
        }
        next.set(cursor);
        // move down again to compensate for block positions that started on the far upper face
        while(!level.isOutsideBuildHeight(next) && isTankBlock(level, next.move(axisY)) && (offsetY++) < maxSize.getY()) {
            cursor.set(next);
        }
        // if a valid multiblock exists, the cursor will be in the corner
        // for positive axis, this is the upper-south-east corner
        // for negative axis, this is the bottom-north-west corner
        return cursor.immutable();
    }

    @Immutable
    public static class Size {

        public static final Codec<TankMultiblock.Size> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                BlockPos.CODEC.fieldOf("origin").forGetter(TankMultiblock.Size::getOrigin),
                Vec3i.CODEC.fieldOf("dimensions").forGetter(TankMultiblock.Size::getDimensions)
        ).apply(instance, TankMultiblock.Size::new));

        /** The bottom-north-west corner of the bounds **/
        private final BlockPos origin;
        /** The XYZ size of the bounds **/
        private final Vec3i dimensions;
        /** A bounding box to represent this object **/
        private final BoundingBox boundingBox;
        /** An axis-aligned bounding box to represent this object **/
        private final AABB aabb;
        /** The chunk at the origin position **/
        private final ChunkPos minChunk;
        /** The chunk at the end position **/
        private final ChunkPos maxChunk;

        public Size(BlockPos origin, Vec3i dimensions) {
            this.origin = origin.immutable();
            this.dimensions = new Vec3i(Math.abs(dimensions.getX()), Math.abs(dimensions.getY()), Math.abs(dimensions.getZ()));
            this.boundingBox = BoundingBox.fromCorners(this.origin, this.origin.offset(this.dimensions));
            this.aabb = AABB.of(this.boundingBox);
            this.minChunk = new ChunkPos(this.origin);
            this.maxChunk = new ChunkPos(this.origin.offset(this.dimensions));
        }

        //// METHODS ////

        /**
         * @return an iterable for the block positions that make up the multiblock border
         * @see BlockPos#betweenClosed(BlockPos, BlockPos)
         */
        public Iterable<BlockPos> outerPositions() {
            // create iterables for each face
            final Iterable<BlockPos> floor = BlockPos.betweenClosed(this.origin, this.origin.offset(this.dimensions.getX(), 0, this.dimensions.getZ()));
            final Iterable<BlockPos> roof = BlockPos.betweenClosed(this.origin.offset(0, this.dimensions.getY(), 0), this.origin.offset(this.dimensions.getX(), this.dimensions.getY(), this.dimensions.getZ()));
            final Iterable<BlockPos> west = BlockPos.betweenClosed(this.origin.offset(1, 1, 0), this.origin.offset(this.dimensions.getX() - 1, this.dimensions.getY() - 1, 0));
            final Iterable<BlockPos> east = BlockPos.betweenClosed(this.origin.offset(1, 1, this.dimensions.getZ()), this.origin.offset(this.dimensions.getX() - 1, this.dimensions.getY() - 1, this.dimensions.getZ()));
            final Iterable<BlockPos> north = BlockPos.betweenClosed(this.origin.offset(0, 1, 0), this.origin.offset(0, this.dimensions.getY() - 1, this.dimensions.getZ()));
            final Iterable<BlockPos> south = BlockPos.betweenClosed(this.origin.offset(this.dimensions.getX(), 1, 0), this.origin.offset(this.dimensions.getX(), this.dimensions.getY() - 1, this.dimensions.getZ()));
            return Iterables.concat(floor, roof, west, east, north, south);
        }

        /**
         * @return an iterable for the block positions inside but not including the multiblock border
         * @see BlockPos#betweenClosed(BlockPos, BlockPos)
         */
        public Iterable<BlockPos> innerPositions() {
            final BlockPos start = this.origin.offset(1, 1, 1);
            final BlockPos end = this.origin.offset(this.dimensions).offset(-1, -1, -1);
            return BlockPos.betweenClosed(start, end);
        }

        @Override
        public String toString() {
            return "Size {origin=" + origin + " dimensions=" + dimensions + "}";
        }

        //// GETTERS ////

        public BlockPos getOrigin() {
            return origin;
        }

        public Vec3i getDimensions() {
            return new Vec3i(dimensions.getX(), dimensions.getY(), dimensions.getZ());
        }

        public BoundingBox boundingBox() {
            return boundingBox;
        }

        public AABB aabb() {
            return aabb;
        }

        public ChunkPos getMinChunk() {
            return minChunk;
        }

        public ChunkPos getMaxChunk() {
            return maxChunk;
        }
    }
}
