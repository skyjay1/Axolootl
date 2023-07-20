package axolootl.block.entity;

import axolootl.AxRegistry;
import axolootl.menu.TabType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class AutoFeederBlockEntity extends InterfaceBlockEntity {

    public AutoFeederBlockEntity(BlockPos pPos, BlockState pBlockState) {
        this(AxRegistry.BlockEntityReg.AUTO_FEEDER.get(), pPos, pBlockState);
    }

    public AutoFeederBlockEntity(BlockEntityType<?> pType, BlockPos pPos, BlockState pBlockState) {
        super(pType, pPos, pBlockState, 3);
    }


    @Override
    public boolean isMenuAvailable(Player player, ControllerBlockEntity controller) {
        return true;
    }
}