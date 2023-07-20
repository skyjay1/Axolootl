package axolootl.block.entity;

import axolootl.AxRegistry;
import axolootl.block.OutputBlock;
import axolootl.menu.TabType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class OutputInterfaceBlockEntity extends InterfaceBlockEntity {

    public OutputInterfaceBlockEntity(BlockPos pPos, BlockState pBlockState) {
        this(AxRegistry.BlockEntityReg.OUTPUT_INTERFACE.get(), pPos, pBlockState);
    }

    public OutputInterfaceBlockEntity(BlockEntityType<?> pType, BlockPos pPos, BlockState pBlockState) {
        super(pType, pPos, pBlockState, (pBlockState.getBlock() instanceof OutputBlock b) ? b.inventoryRows : 0);
    }

    @Override
    public boolean isMenuAvailable(Player player, ControllerBlockEntity controller) {
        return TabType.OUTPUT.isAvailable(controller);
    }
}
