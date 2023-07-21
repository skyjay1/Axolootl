package axolootl.block.entity;

import axolootl.AxRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class AxolootlInterfaceBlockEntity extends InterfaceBlockEntity {

    public AxolootlInterfaceBlockEntity(BlockPos pPos, BlockState pBlockState) {
        this(AxRegistry.BlockEntityReg.AXOLOOTL_INTERFACE.get(), pPos, pBlockState);
    }

    public AxolootlInterfaceBlockEntity(BlockEntityType<?> pType, BlockPos pPos, BlockState pBlockState) {
        super(pType, pPos, pBlockState, 1, 6);
    }

    @Override
    public boolean isMenuAvailable(Player player, ControllerBlockEntity controller) {
        return AxRegistry.AquariumTabsReg.AXOLOOTL_INTERFACE.get().isAvailable(controller);
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int pContainerId, Inventory pPlayerInventory, Player pPlayer) {
        // TODO
        return null;
    }
}
