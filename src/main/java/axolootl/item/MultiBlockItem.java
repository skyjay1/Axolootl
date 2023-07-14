package axolootl.item;

import axolootl.block.WaterloggedHorizontalMultiBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

public class MultiBlockItem extends BlockItem {

    public MultiBlockItem(Block pBlock, Properties pProperties) {
        super(pBlock, pProperties);
    }

    @Nullable
    public BlockPlaceContext updatePlacementContext(BlockPlaceContext pContext) {
        final BlockPos blockpos = pContext.getClickedPos();
        final Direction direction = pContext.getHorizontalDirection();
        // determine the center position of a multiblock placed with the given position and rotation
        final Vec3i index = new Vec3i(-(direction.getStepX() - 1), 0, -(direction.getStepZ() - 1));
        final BlockPos center = WaterloggedHorizontalMultiBlock.getCenter(blockpos, index);
        // create a block place context at the center position
        return BlockPlaceContext.at(pContext, center, direction);
    }

    @Override
    protected boolean mustSurvive() {
        return false;
    }
}
