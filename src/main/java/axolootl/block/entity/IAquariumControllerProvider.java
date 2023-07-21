package axolootl.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.Optional;

public interface IAquariumControllerProvider {

    /**
     * @param level the level
     * @param pos the block position
     * @param blockEntity the controller block entity
     * @return true if the block entity at this position is a controller provider and its data was updated
     */
    public static boolean trySetController(final Level level, final BlockPos pos, final ControllerBlockEntity blockEntity) {
        if(level.getBlockEntity(pos) instanceof IAquariumControllerProvider provider) {
            provider.setController(level, blockEntity.getBlockPos(), blockEntity);
            return true;
        }
        return false;
    }

    /**
     * @param entity an entity
     * @param level the level
     * @param pos the block position
     * @param blockEntity the controller block entity
     * @return true if the entity is a controller provider and its data was updated
     */
    public static boolean trySetController(final Entity entity, final Level level, final BlockPos pos, final ControllerBlockEntity blockEntity) {
        if(entity instanceof IAquariumControllerProvider provider) {
            provider.setController(level, blockEntity.getBlockPos(), blockEntity);
            return true;
        }
        return false;
    }

    /**
     * @param level the level
     * @param pos the block position
     * @return true if the block entity at this position is a controller provider and its data was cleared
     */
    public static boolean tryClearController(final Level level, final BlockPos pos) {
        if(level.getBlockEntity(pos) instanceof IAquariumControllerProvider provider) {
            provider.clearController();
            return true;
        }
        return false;
    }

    /**
     * @param entity an entity
     * @return true if the entity is a controller provider and its data was cleared
     */
    public static boolean tryClearController(final Entity entity) {
        if(entity instanceof IAquariumControllerProvider provider) {
            provider.clearController();
            return true;
        }
        return false;
    }

    /**
     * Updates the controller position and reference
     * @param level the level
     * @param pos the block position
     * @param blockEntity the controller block entity
     */
    void setController(final Level level, final BlockPos pos, final ControllerBlockEntity blockEntity);

    /**
     * Resets the controller position and reference
     */
    void clearController();

    /**
     * @return the controller block entity, if any
     */
    Optional<ControllerBlockEntity> getController();

    /**
     * @return true if the controller exists and it has a tank
     * @see ControllerBlockEntity#hasTank()
     */
    default boolean hasTank() {
        final Optional<ControllerBlockEntity> oController = getController();
        return oController.isPresent() && oController.get().hasTank();
    }

    public static final String KEY_CONTROLLER = "Controller";

    default void writeControllerPos(@Nullable final BlockPos pos, final CompoundTag tag) {
        if(pos != null) {
            tag.put(KEY_CONTROLLER, NbtUtils.writeBlockPos(pos));
        }
    }

    @Nullable
    default BlockPos readControllerPos(final CompoundTag tag) {
        if(tag.contains(KEY_CONTROLLER)) {
            return NbtUtils.readBlockPos(tag.getCompound(KEY_CONTROLLER));
        }
        return null;
    }
}
