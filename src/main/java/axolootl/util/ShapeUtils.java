/**
 * Copyright (c) 2023 Skyler James
 * Permission is granted to use, modify, and redistribute this software, in parts or in whole,
 * under the GNU LGPLv3 license (https://www.gnu.org/licenses/lgpl-3.0.en.html)
 **/

package axolootl.util;

import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * @author XFactHD
 * [https://github.com/XFactHD/FramedBlocks/blob/1.20/src/main/java/xfacthd/framedblocks/api/shapes/ShapeUtils.java]
 * Used with permission under the GNU LGPLv3 license
 */
public final class ShapeUtils {

    private ShapeUtils() { }

    /**
     * @param first the first voxel shape
     * @param second the second voxel shape
     * @return the unoptimized combined shape using the {@link BooleanOp#OR} operation
     */
    public static VoxelShape orUnoptimized(VoxelShape first, VoxelShape second) {
        return Shapes.joinUnoptimized(first, second, BooleanOp.OR);
    }

    /**
     * @param first the first voxel shape
     * @param others any number of additional voxel shapes
     * @return the unoptimized combined shape using the {@link BooleanOp#OR} operation
     */
    public static VoxelShape orUnoptimized(VoxelShape first, VoxelShape... others) {
        for (VoxelShape shape : others) {
            first = ShapeUtils.orUnoptimized(first, shape);
        }
        return first;
    }

    /**
     * @param from the start horizontal direction
     * @param to the target horizontal direction
     * @param shape the original shape
     * @return the rotated and optimized shape
     */
    public static VoxelShape rotateShape(Direction from, Direction to, VoxelShape shape) {
        return rotateShapeUnoptimized(from, to, shape).optimize();
    }

    /**
     * @param from the start horizontal direction
     * @param to the target horizontal direction
     * @param shape the original shape
     * @return the unoptimized rotated shape
     */
    public static VoxelShape rotateShapeUnoptimized(Direction from, Direction to, VoxelShape shape) {
        // validate direction
        if (from.getAxis().isVertical() || to.getAxis().isVertical()) {
            throw new IllegalArgumentException("Invalid Direction!");
        }
        // check start and target directions are the same and skip unnecessary rotations
        if (from == to) {
            return shape;
        }
        // store the shape as AABBs for as long as possible to avoid unnecessary object manipulation
        List<AABB> sourceBoxes = shape.toAabbs();
        VoxelShape rotatedShape = Shapes.empty();
        // determine the number of 90 degree rotations to apply
        int times = (to.get2DDataValue() - from.get2DDataValue() + 4) % 4;
        // iterate each AABB in the shape
        for (AABB box : sourceBoxes) {
            // rotate the AABB to the target direction
            for (int i = 0; i < times; i++) {
                box = new AABB(1 - box.maxZ, box.minY, box.minX, 1 - box.minZ, box.maxY, box.maxX);
            }
            // add the rotated AABB to the result shape without optimizing
            rotatedShape = orUnoptimized(rotatedShape, Shapes.create(box));
        }

        return rotatedShape;
    }

    /**
     * @param from the horizontal start direction
     * @param shape the original unoptimized shape
     * @return an {@link EnumMap} containing rotated and unoptimized shapes for each horizontal direction
     */
    public static Map<Direction, VoxelShape> rotateShapesUnoptimized(Direction from, VoxelShape shape) {
        // validate direction
        if (from.getAxis().isVertical()) {
            throw new IllegalArgumentException("Invalid Direction!");
        }
        // create map to store results
        final Map<Direction, VoxelShape> rotatedShapes = new EnumMap<>(Direction.class);
        // store the original shape at the original direction
        rotatedShapes.put(from, shape);
        // calculate the AABBs in the original shape
        List<AABB> sourceBoxes = shape.toAabbs();
        final Function<Direction, VoxelShape> computeShapeIfAbsent = d -> Shapes.empty();
        // iterate each AABB in the shape
        for (AABB box : sourceBoxes) {
            // iterate each remaining horizontal direction
            for(int i = 0; i < 3; i++) {
                Direction direction = Direction.from2DDataValue(from.get2DDataValue() + i + 1);
                box = new AABB(1 - box.maxZ, box.minY, box.minX, 1 - box.minZ, box.maxY, box.maxX);
                rotatedShapes.put(direction, orUnoptimized(rotatedShapes.computeIfAbsent(direction, computeShapeIfAbsent), Shapes.create(box)));
            }
        }
        return rotatedShapes;
    }

    /**
     * @param from the horizontal start direction
     * @param shape the original unoptimized shape
     * @return an {@link EnumMap} containing rotated and optimized shapes for each horizontal direction
     */
    public static Map<Direction, VoxelShape> rotateShapes(Direction from, VoxelShape shape) {
        final Map<Direction, VoxelShape> rotatedShapes = rotateShapesUnoptimized(from, shape);
        // optimize shapes
        for(Direction direction : Direction.Plane.HORIZONTAL) {
            rotatedShapes.put(direction, rotatedShapes.get(direction).optimize());
        }
        return rotatedShapes;
    }

    //// AXOLOOTL SHAPE UTILS ////

    public static final double MAX_ENTITY_SIZE = 0.62D;
    public static final double MAX_VOLUME = MAX_ENTITY_SIZE * MAX_ENTITY_SIZE * MAX_ENTITY_SIZE;

    /**
     * @param entity an entity
     * @return true if the entity bounding box has a small enough volume to pass through some arbitrary shape
     */
    public static boolean canEntityPass(final Entity entity) {
        return entity != null && !(volume(entity.getBoundingBox()) > MAX_VOLUME);
    }

    public static double volume(final AABB aabb) {
        return aabb.getXsize() * aabb.getYsize() * aabb.getZsize();
    }

}
