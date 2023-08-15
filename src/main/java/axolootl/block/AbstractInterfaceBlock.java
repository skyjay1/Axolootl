/**
 * Copyright (c) 2023 Skyler James
 * Permission is granted to use, modify, and redistribute this software, in parts or in whole,
 * under the GNU LGPLv3 license (https://www.gnu.org/licenses/lgpl-3.0.en.html)
 **/

package axolootl.block;

import axolootl.Axolootl;
import axolootl.block.entity.InterfaceBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.registries.ForgeRegistries;

public abstract class AbstractInterfaceBlock extends PoweredHorizontalBlock implements EntityBlock {

    public static final TagKey<Block> GLASS = ForgeRegistries.BLOCKS.tags().createTagKey(new ResourceLocation("forge", "glass"));
    public static final TagKey<Block> AQUARIUM = ForgeRegistries.BLOCKS.tags().createTagKey(new ResourceLocation(Axolootl.MODID, "aquarium"));

    public AbstractInterfaceBlock(Properties pProperties) {
        super(pProperties);
    }

    // GLASS //

    @Override
    public VoxelShape getVisualShape(BlockState pState, BlockGetter pReader, BlockPos pPos, CollisionContext pContext) {
        return Shapes.empty();
    }

    @Override
    public float getShadeBrightness(BlockState pState, BlockGetter pLevel, BlockPos pPos) {
        return 1.0F;
    }

    @Override
    public boolean propagatesSkylightDown(BlockState pState, BlockGetter pReader, BlockPos pPos) {
        return true;
    }

    @Override
    public boolean skipRendering(BlockState pState, BlockState pAdjacentBlockState, Direction pSide) {
        return pAdjacentBlockState.is(this) || pAdjacentBlockState.is(GLASS) || pAdjacentBlockState.is(AQUARIUM) || super.skipRendering(pState, pAdjacentBlockState, pSide);
    }

    @Override
    public boolean shouldDisplayFluidOverlay(BlockState state, BlockAndTintGetter level, BlockPos pos, FluidState fluidState) {
        return true;
    }

    //// ENTITY BLOCK ////

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            // drop items
            if(!level.isClientSide() && level.getBlockEntity(pos) instanceof InterfaceBlockEntity blockEntity) {
                blockEntity.dropAllItems();
            }
            // update neighbors
            level.updateNeighbourForOutputSignal(pos, this);
            super.onRemove(state, level, pos, newState, isMoving);
        }
    }
}
