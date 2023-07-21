package axolootl.block.entity;

import axolootl.AxRegistry;
import axolootl.menu.CyclingInventoryMenu;
import axolootl.menu.TabType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class BreederBlockEntity extends InterfaceBlockEntity {

    public BreederBlockEntity(BlockPos pPos, BlockState pBlockState) {
        this(AxRegistry.BlockEntityReg.BREEDER.get(), pPos, pBlockState);
    }

    public BreederBlockEntity(BlockEntityType<?> pType, BlockPos pPos, BlockState pBlockState) {
        super(pType, pPos, pBlockState, 3);
    }

    @Override
    public boolean isMenuAvailable(Player player, ControllerBlockEntity controller) {
        return controller.hasTank();
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int pContainerId, Inventory pPlayerInventory, Player pPlayer) {
        // verify availability
        if(!isMenuAvailable(pPlayer, this.controller)) {
            return super.createMenu(pContainerId, pPlayerInventory, pPlayer);
        }
        return CyclingInventoryMenu.createBreeder(pContainerId, pPlayerInventory, this.controllerPos, this.controller, this.getBlockPos(), TabType.FOOD_INTERFACE.getIndex(), -1);
    }
}