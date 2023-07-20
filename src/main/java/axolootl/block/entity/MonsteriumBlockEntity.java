package axolootl.block.entity;

import axolootl.AxRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class MonsteriumBlockEntity extends InterfaceBlockEntity {

    public MonsteriumBlockEntity(BlockPos pPos, BlockState pBlockState) {
        this(AxRegistry.BlockEntityReg.MONSTERIUM.get(), pPos, pBlockState);
    }

    public MonsteriumBlockEntity(BlockEntityType<?> pType, BlockPos pPos, BlockState pBlockState) {
        super(pType, pPos, pBlockState, 3);
    }


    @Override
    public boolean isMenuAvailable(Player player, ControllerBlockEntity controller) {
        return true;
    }
}