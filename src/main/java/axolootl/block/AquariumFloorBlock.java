package axolootl.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

public class AquariumFloorBlock extends Block {

    private static final List<Converter> ITEM_CONVERTERS = new ArrayList<>();
    public static final List<Converter> ITEM_CONVERTERS_VIEW = Collections.unmodifiableList(ITEM_CONVERTERS);

    public AquariumFloorBlock(Properties pProperties) {
        super(pProperties);
    }

    /**
     * Registers an item stack function and its resulting block state when the item is used on this block.
     * For best results, call during the {@link net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent} event
     * @param function a function that accepts an itemstack and returns either a block state or an empty optional
     * @see #itemConverter(Item, Supplier)
     * @see FilledAquariumFloorBlock#FilledAquariumFloorBlock(Supplier, Properties)
     * @see FilledAquariumFloorBlock#itemConverter(Supplier, Supplier)
     */
    public static void registerItemConverter(final Converter function) {
        ITEM_CONVERTERS.add(function);
    }

    @Override
    public InteractionResult use(BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer, InteractionHand pHand, BlockHitResult pHit) {
        ItemStack itemStack = pPlayer.getItemInHand(pHand);
        // iterate each conversion function until one is successful
        for(Converter f : ITEM_CONVERTERS) {
            Optional<BlockState> oState = f.apply(itemStack);
            if(oState.isPresent() && convert(pLevel, pPos, pPlayer, pHand, itemStack, oState.get())) {
                return InteractionResult.SUCCESS;
            }
        }
        return super.use(pState, pLevel, pPos, pPlayer, pHand, pHit);
    }

    /**
     * Helper method to create a function for use in {@link #registerItemConverter(Converter)}
     * @param item the item to trigger this function
     * @param block the block whose default state will be placed for the given item
     * @return an {@link Converter}
     */
    public static Converter itemConverter(final Item item, final Supplier<Block> block) {
        return itemStack -> {
            if(!itemStack.is(item)) {
                return Optional.empty();
            }
            return Optional.of(block.get().defaultBlockState());
        };
    }

    /**
     * Places the given block and shrinks the item stack count
     * @param level the level
     * @param pos the block position
     * @param player the player
     * @param hand the interaction hand
     * @param itemStack the item stack
     * @param blockState the block state to place
     * @return true if the level is client-side or the block was placed
     */
    private boolean convert(Level level, BlockPos pos, Player player, InteractionHand hand, ItemStack itemStack, BlockState blockState) {
        // validate server side
        if(level.isClientSide()) {
            return true;
        }
        // replace the block
        if(!level.setBlock(pos, blockState, Block.UPDATE_CLIENTS)) {
            return false;
        }
        // consume the item
        if(!player.getAbilities().instabuild) {
            itemStack.shrink(1);
        }
        return true;
    }

    /**
     * Accepts an ItemStack and returns the BlockState to replace the current BlockState, if any
     */
    @FunctionalInterface
    public static interface Converter extends Function<ItemStack, Optional<BlockState>> {}
}
