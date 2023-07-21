package axolootl.block;

import axolootl.AxRegistry;
import axolootl.block.entity.OutputInterfaceBlockEntity;
import axolootl.menu.TabType;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;

public class OutputBlock extends AbstractInterfaceBlock {

    public final int inventoryRows;

    public OutputBlock(int inventoryRows, Properties pProperties) {
        super(pProperties);
        this.inventoryRows = Mth.clamp(inventoryRows, 0, 6);
    }

    @Override
    public InteractionResult use(BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer, InteractionHand pHand, BlockHitResult pHit) {
        if (pLevel.isClientSide()) {
            return InteractionResult.SUCCESS;
        } else {
            // open menu
            if (pPlayer instanceof ServerPlayer serverPlayer && pLevel.getBlockEntity(pPos) instanceof OutputInterfaceBlockEntity blockEntity) {
                // validate controller
                if(blockEntity.hasTank() && blockEntity.validateController(pLevel)) {
                    blockEntity.setChanged();
                }
                // open menu
                BlockPos controllerPos = blockEntity.getController().isPresent() ? blockEntity.getController().get().getBlockPos() : pPos;
                NetworkHooks.openScreen(serverPlayer, blockEntity, AxRegistry.MenuReg.writeControllerMenu(controllerPos, pPos, TabType.OUTPUT.getIndex(), -1));
            }
            return InteractionResult.CONSUME;
        }
    }


    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!level.isClientSide() && !state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof OutputInterfaceBlockEntity blockEntity) {
            // drop items from inventory
            blockEntity.dropAllItems();
            level.updateNeighbourForOutputSignal(pos, this);
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    //// ENTITY BLOCK ////

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pPos, BlockState pState) {
        return AxRegistry.BlockEntityReg.OUTPUT_INTERFACE.get().create(pPos, pState);
    }
}
