package axolootl.block;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.AbstractGlassBlock;
import net.minecraft.world.level.block.state.BlockState;

public class AquariumGlassBlock extends AbstractGlassBlock {

    public AquariumGlassBlock(Properties pProperties) {
        super(pProperties);
    }

    @Override
    public boolean skipRendering(BlockState pState, BlockState pAdjacentBlockState, Direction pSide) {
        return pAdjacentBlockState.is(AbstractInterfaceBlock.GLASS)
                || pAdjacentBlockState.is(AbstractInterfaceBlock.AQUARIUM)
                || super.skipRendering(pState, pAdjacentBlockState, pSide);
    }
}
