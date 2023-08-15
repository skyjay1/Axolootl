package axolootl.block;

import axolootl.AxRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.common.ToolActions;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.function.Supplier;

public class FilledAquariumFloorBlock extends Block {

    private final Supplier<ItemStack> item;

    /**
     * Creates an aquarium floor block and registers it to {@link AquariumFloorBlock#registerItemConverter(AquariumFloorBlock.Converter)}
     * @param itemSupplier the item that is used to create this block and that is given to the player upon destroying this block
     * @param pProperties the block properties
     */
    public FilledAquariumFloorBlock(Supplier<ItemStack> itemSupplier, Properties pProperties) {
        super(pProperties);
        this.item = itemSupplier;
        AquariumFloorBlock.registerItemConverter(itemConverter(this.item, () -> this));
    }

    /**
     * Helper method to create a function for use in {@link AquariumFloorBlock#registerItemConverter(AquariumFloorBlock.Converter)}
     * @param item the itemstack supplier to trigger this function
     * @param block the block whose default state will be placed for the given item
     * @return a function that can be easily registered in {@link AquariumFloorBlock#registerItemConverter(AquariumFloorBlock.Converter)}
     */
    public static AquariumFloorBlock.Converter itemConverter(final Supplier<ItemStack> item, final Supplier<Block> block) {
        return itemStack -> {
            if(!ItemStack.isSame(itemStack, item.get())) {
                return Optional.empty();
            }
            return Optional.of(block.get().defaultBlockState());
        };
    }

    @Override
    public void playerWillDestroy(Level pLevel, BlockPos pPos, BlockState pState, Player pPlayer) {
        super.playerWillDestroy(pLevel, pPos, pState, pPlayer);
    }

    @Override
    public void playerDestroy(Level pLevel, Player pPlayer, BlockPos pPos, BlockState pState, @Nullable BlockEntity pBlockEntity, ItemStack pTool) {
        if(pTool.canPerformAction(ToolActions.SHOVEL_DIG)) {
            // verify server side
            if(!pLevel.isClientSide()) {
                // update block
                pLevel.setBlock(pPos, AxRegistry.BlockReg.AQUARIUM_FLOOR.get().defaultBlockState(), Block.UPDATE_ALL);
                // drop item
                popResourceFromFace(pLevel, pPos, Direction.UP, item.get());
            }
        } else {
            super.playerDestroy(pLevel, pPlayer, pPos, pState, pBlockEntity, pTool);
        }
    }

    @Override
    public InteractionResult use(BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer, InteractionHand pHand, BlockHitResult pHit) {
        ItemStack itemstack = pPlayer.getItemInHand(pHand);
        // verify dig action
        if (itemstack.canPerformAction(ToolActions.SHOVEL_DIG)) {
            // verify server side
            if(!pLevel.isClientSide()) {
                // update block
                pLevel.setBlock(pPos, AxRegistry.BlockReg.AQUARIUM_FLOOR.get().defaultBlockState(), Block.UPDATE_CLIENTS);
                // drop item
                popResourceFromFace(pLevel, pPos, Direction.UP, item.get());
                itemstack.hurtAndBreak(1, pPlayer, (entity) -> entity.broadcastBreakEvent(pHand));
                pLevel.gameEvent(pPlayer, GameEvent.BLOCK_CHANGE, pPos);
                pPlayer.awardStat(Stats.ITEM_USED.get(itemstack.getItem()));
            }
            return InteractionResult.SUCCESS;
        }
        return super.use(pState, pLevel, pPos, pPlayer, pHand, pHit);
    }
}