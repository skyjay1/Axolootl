package axolootl.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import java.util.Optional;

public interface IAquariumControllerProvider {

    public static boolean trySetController(final Level level, final BlockPos pos, final ControllerBlockEntity blockEntity) {
        if(level.getBlockEntity(pos) instanceof IAquariumControllerProvider provider) {
            provider.setController(level, pos.immutable(), blockEntity);
            return true;
        }
        return false;
    }

    void setController(final Level level, final BlockPos pos, final ControllerBlockEntity blockEntity);

    void clearController();

    Optional<ControllerBlockEntity> getController();

    default boolean hasTank() {
        final Optional<ControllerBlockEntity> oController = getController();
        return oController.isPresent() && oController.get().hasTank();
    }
}
