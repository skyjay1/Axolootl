package axolootl.block.entity;

import axolootl.AxRegistry;
import axolootl.block.OutputBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class OutputInterfaceBlockEntity extends InterfaceBlockEntity {

    public OutputInterfaceBlockEntity(BlockPos pPos, BlockState pBlockState) {
        this(AxRegistry.BlockEntityReg.OUTPUT_INTERFACE.get(), pPos, pBlockState);
    }

    public OutputInterfaceBlockEntity(BlockEntityType<?> pType, BlockPos pPos, BlockState pBlockState) {
        super(pType, pPos, pBlockState, (pBlockState.getBlock() instanceof OutputBlock b) ? b.inventoryRows : 0);
    }
}
