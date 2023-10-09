package axolootl.block;

import net.minecraft.world.phys.shapes.VoxelShape;

public class ShapeData {

    private final VoxelShape collisionShape;
    private final boolean passable;

    /**
     * @param collisionShape the collision voxel shape
     * @param passable       true if small entities can pass through the block without collisions
     */
    public ShapeData(final VoxelShape collisionShape, final boolean passable) {
        this.collisionShape = collisionShape;
        this.passable = passable;
    }

    //// GETTERS ////

    public VoxelShape getCollisionShape() {
        return collisionShape;
    }

    public boolean isPassable() {
        return passable;
    }
}
