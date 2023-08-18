/**
 * Copyright (c) 2023 Skyler James
 * Permission is granted to use, modify, and redistribute this software, in parts or in whole,
 * under the GNU LGPLv3 license (https://www.gnu.org/licenses/lgpl-3.0.en.html)
 **/

package axolootl.block.entity;

import axolootl.AxRegistry;
import axolootl.block.WaterInterfaceBlock;
import axolootl.menu.CyclingContainerMenu;
import axolootl.util.TankMultiblock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.LiquidBlockContainer;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import net.minecraftforge.fluids.capability.templates.EmptyFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import net.minecraftforge.items.IItemHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

public class WaterInterfaceBlockEntity extends InterfaceBlockEntity {

    /** The number to multiply by the speed and subtract from the ticker each tick **/
    public static final long BASE_SPEED_DECREMENT = 100;
    /** The maximum number of blocks to check each tick **/
    public static final int FLUIDS_SCANNED_PER_TICK = 20;
    /** The number of ticks between fluid searches **/
    public static final int FLUID_UPDATE_INTERVAL = 80;

    private double placeFluidSpeed = 1.0D;
    private long placeFluidTime;
    private boolean fillTopLayer;
    private boolean isObstructed;
    private boolean forceUpdateObstructed;
    private Iterator<BlockPos> placeFluidIterator;

    protected FluidTank tank = new FluidTank(FluidType.BUCKET_VOLUME * 12)
            .setValidator(f -> f.getFluid().getFluidType() == ForgeMod.WATER_TYPE.get());
    private LazyOptional<IFluidHandler> holder = createFluidHolder();

    public WaterInterfaceBlockEntity(BlockPos pPos, BlockState pBlockState) {
        this(AxRegistry.BlockEntityReg.WATER_INTERFACE.get(), pPos, pBlockState);
    }

    public WaterInterfaceBlockEntity(BlockEntityType<?> pType, BlockPos pPos, BlockState pBlockState) {
        super(pType, pPos, pBlockState, 1, 1);
        forceUpdateObstructed = true;
    }

    public static void tick(final Level level, final BlockPos pos, final BlockState state, final WaterInterfaceBlockEntity self) {
        // verify tank exists
        final Optional<ControllerBlockEntity> oController = self.getController();
        if(oController.isEmpty() || !oController.get().hasTank() || !oController.get().getTankStatus().isActive()) {
            return;
        }
        boolean markDirty = false;
        markDirty |= self.validateController(level);
        markDirty |= self.updateTickers(level);
        // verify has fluid
        final IFluidHandler fluidHandler = self.getCapability(ForgeCapabilities.FLUID_HANDLER).orElse(EmptyFluidHandler.INSTANCE);
        final Optional<IItemHandler> itemHandler = self.getCapability(ForgeCapabilities.ITEM_HANDLER).resolve();
        markDirty |= self.consumeFluid(level, itemHandler, fluidHandler);
        // update obstructed status
        if(self.forceUpdateObstructed) {
            self.updateObstructed(level, pos, state, Fluids.WATER, Fluids.FLOWING_WATER);
            self.forceUpdateObstructed = false;
            markDirty = true;
        }
        // verify not powered
        if(!state.getValue(WaterInterfaceBlock.POWERED) && !self.isObstructed()) {
            // validate fluid and amount
            final FluidStack fluidStack = fluidHandler.getFluidInTank(0);
            if(fluidStack.getFluid().isSame(Fluids.WATER) && fluidStack.getAmount() >= FluidType.BUCKET_VOLUME) {
                // attempt to place fluid
                markDirty |= self.tickPlaceFluid(level, fluidHandler);
            }
        }
        // mark changed and send update
        if(markDirty) {
            self.setChanged();
            level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
        }
    }

    //// MENU PROVIDER ////

    @Override
    public boolean isMenuAvailable(Player player, ControllerBlockEntity controller) {
        return AxRegistry.AquariumTabsReg.FLUID_INTERFACE.get().isAvailable(controller);
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int pContainerId, Inventory pPlayerInventory, Player pPlayer) {
        return CyclingContainerMenu.createFluid(pContainerId, pPlayerInventory, this.controllerPos, this.getController().get(), this.getBlockPos(), AxRegistry.AquariumTabsReg.FLUID_INTERFACE.get().getSortedIndex(), -1);
    }

    //// CONTROLLER PROVIDER ////


    @Override
    public void clearController() {
        this.placeFluidIterator = null;
        super.clearController();
    }

    //// CAPABILITY ////

    private LazyOptional<IFluidHandler> createFluidHolder() {
        return LazyOptional.of(() -> tank);
    }

    @Override
    @NotNull
    public <T> LazyOptional<T> getCapability(@NotNull Capability<T> capability, @Nullable Direction facing) {
        if (capability == ForgeCapabilities.FLUID_HANDLER) {
            return holder.cast();
        }
        return super.getCapability(capability, facing);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        holder.invalidate();
    }

    @Override
    public void reviveCaps() {
        super.reviveCaps();
        holder = createFluidHolder();
    }

    //// TICKER ////

    /**
     * Decrements the place fluid ticker
     * @return true if any ticker was changed
     * @param level the level
     */
    private boolean updateTickers(Level level) {
        if(placeFluidTime > 0) {
            long tickAmount = getPlaceFluidTickAmount();
            placeFluidTime = Math.max(0, placeFluidTime - tickAmount);
            return true;
        }
        return false;
    }

    /**
     * @return the number of ticks to subtract from the resource generation ticker based on generation speed
     */
    public long getPlaceFluidTickAmount() {
        return Mth.floor(BASE_SPEED_DECREMENT * placeFluidSpeed);
    }

    //// SLOTS ////

    private boolean consumeFluid(final Level level, final Optional<IItemHandler> oHandler, final IFluidHandler fluidHandler) {
        // validate item handler
        if(oHandler.isEmpty()) {
            return false;
        }
        final IItemHandler itemHandler = oHandler.get();
        // iterate item slots
        boolean hasConsumed = false;
        for(int i = 0, n = itemHandler.getSlots(); i < n; i++) {
            // validate non-empty item
            ItemStack itemStack = itemHandler.extractItem(i, 1, true);
            if(itemStack.isEmpty()) {
                continue;
            }
            // resolve capability
            Optional<IFluidHandlerItem> oFluidItem = itemStack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).resolve();
            if(oFluidItem.isEmpty()) {
                continue;
            }
            IFluidHandlerItem fluidItem = oFluidItem.get();
            // validate fluid
            FluidStack fluidStack = fluidItem.getFluidInTank(0);
            if(fluidHandler.fill(fluidStack, IFluidHandler.FluidAction.SIMULATE) <= 0) {
                continue;
            }
            // extract item
            itemHandler.extractItem(i, 1, false);
            // transfer fluid
            if(fluidHandler.fill(fluidItem.drain(fluidStack.getAmount(), IFluidHandler.FluidAction.EXECUTE), IFluidHandler.FluidAction.EXECUTE) > 0) {
                hasConsumed = true;
                // insert remainder item
                ItemStack remainder = itemHandler.insertItem(i, fluidItem.getContainer(), false);
                if(!remainder.isEmpty()) {
                    Direction facing = getBlockState().getValue(HorizontalDirectionalBlock.FACING);
                    Vec3 pos = Vec3.atCenterOf(getBlockPos()).relative(facing, 0.6D);
                    level.addFreshEntity(new ItemEntity(level, pos.x(), pos.y(), pos.z(), remainder));
                }
            }
        }
        return hasConsumed;
    }

    //// PLACE FLUID ////

    public boolean fillTopLayer() {
        return fillTopLayer;
    }

    public boolean isObstructed() {
        return isObstructed;
    }

    public void forceUpdateObstructed() {
        this.forceUpdateObstructed = true;
        this.setChanged();
    }

    /**
     * Checks if the back of the block is obstructed and cannot place fluids
     * @param level the level
     * @param origin the block position
     * @param fluid the fluid source
     * @param flowing the flowing fluid
     * @return true if the obstructed status changed
     */
    public boolean updateObstructed(Level level, BlockPos origin, BlockState blockState, Fluid fluid, Fluid flowing) {
        if(!hasTank()) {
            return false;
        }
        // create bounding box of the area to fill
        final BoundingBox b = controller.getSize().orElse(TankMultiblock.Size.EMPTY).boundingBox();
        final BoundingBox bounds = new BoundingBox(b.minX() + 1, b.minY() + 1, b.minZ() + 1, b.maxX() - 1, b.maxY() - (fillTopLayer() ? 1 : 2), b.maxZ() - 1);
        // determine the position to check
        BlockPos neighbor = origin.relative(blockState.getValue(HorizontalDirectionalBlock.FACING).getOpposite());
        // determine if the position is inside the bounds and is either a fluid source or can accept fluid
        boolean wasObstructed = isObstructed;
        this.isObstructed = !bounds.isInside(neighbor) || !(isFluidSource(level, neighbor, fluid) || placeFluidBlock(level, neighbor, fluid, flowing, true));
        return wasObstructed != isObstructed;
    }

    /**
     * @param level the server level
     * @param fluidHandler the fluid handler
     * @return true if there are any changes, such as tickers resetting or fluids changing
     **/
    private boolean tickPlaceFluid(Level level, IFluidHandler fluidHandler) {
        // validate not obstructed
        if(isObstructed) {
            return false;
        }
        // validate ticker
        if (placeFluidTime > 0) {
            return false;
        }
        // validate controller has tank
        if(!hasTank()) {
            return false;
        }
        // validate fluid and amount
        final int amount = FluidType.BUCKET_VOLUME;
        final FluidStack fluidStack = new FluidStack(Fluids.WATER, amount);
        if(fluidHandler.drain(fluidStack, IFluidHandler.FluidAction.SIMULATE).getAmount() < amount) {
            return false;
        }
        // find and validate fluid position
        final Optional<BlockPos> oPos = findPlaceablePosition(level, getBlockPos(), Fluids.WATER, Fluids.FLOWING_WATER);
        // attempt to place fluid
        if(oPos.isEmpty() || !placeFluidBlock(level, oPos.get(), Fluids.WATER, Fluids.FLOWING_WATER, false)) {
            return false;
        }
        // fluid block was placed, remove fluid from handler
        fluidHandler.drain(fluidStack, IFluidHandler.FluidAction.EXECUTE);
        // reset ticker
        placeFluidTime = FLUID_UPDATE_INTERVAL * BASE_SPEED_DECREMENT;
        // all checks passed
        return true;
    }

    private Iterator<BlockPos> createIterator(final Level level, BlockPos origin) {
        // create bounding box of the area to fill
        final BoundingBox b = controller.getSize().orElse(TankMultiblock.Size.EMPTY).boundingBox();
        final BoundingBox bounds = new BoundingBox(b.minX() + 1, b.minY() + 1, b.minZ() + 1, b.maxX() - 1, b.maxY() - (fillTopLayer() ? 1 : 2), b.maxZ() - 1);
        // determine a valid starting position inside bounds
        // determine the position to check
        BlockPos neighbor = origin.relative(level.getBlockState(origin).getValue(HorizontalDirectionalBlock.FACING).getOpposite());
        // create iterator
        return new PlaceFluidIterator(origin, neighbor, bounds);
    }

    /**
     * Searches for the first available position to place a fluid
     * @param level the level
     * @param origin the origin position
     * @param fluid the fluid
     * @param flowing the flowing fluid
     * @return the position to place a fluid, if any
     */
    private Optional<BlockPos> findPlaceablePosition(final Level level, BlockPos origin, Fluid fluid, Fluid flowing) {
        // create or recreate iterator
        if((null == placeFluidIterator || !placeFluidIterator.hasNext())) {
            placeFluidIterator = createIterator(level, origin);
        }
        // check a specified number of positions for fluids
        int steps = FLUIDS_SCANNED_PER_TICK;
        while(placeFluidIterator.hasNext() && steps-- > 0) {
            BlockPos p = placeFluidIterator.next();
            if(placeFluidBlock(level, p, fluid, flowing, true)) {
                return Optional.of(p);
            }
        }
        return Optional.empty();
    }

    /**
     * @param level the level
     * @param pos the block position
     * @param fluid the fluid source
     * @return true if the fluid state at the given position matches the given fluid
     */
    private boolean isFluidSource(Level level, BlockPos pos, Fluid fluid) {
        return level.getFluidState(pos).is(fluid);
    }

    /**
     * Places a fluid block, either by replacing the position with fluid, or filling a {@link LiquidBlockContainer}
     * @param level the level
     * @param pos the block position
     * @param fluid the fluid source
     * @param flowing the flowing fluid
     * @param simulate true to simulate the action
     * @return true if the fluid was placed or can be placed at this position
     */
    private boolean placeFluidBlock(Level level, BlockPos pos, Fluid fluid, Fluid flowing, boolean simulate) {
        BlockState blockState = level.getBlockState(pos);
        // check for liquid block container
        if(blockState.getBlock() instanceof LiquidBlockContainer container) {
            return simulate ? container.canPlaceLiquid(level, pos, blockState, fluid) : container.placeLiquid(level, pos, blockState, fluid.defaultFluidState());
        }
        // check for air or flowing water
        if(blockState.isAir() || blockState.getFluidState().is(flowing)) {
            return simulate || level.setBlock(pos, fluid.defaultFluidState().createLegacyBlock(), Block.UPDATE_ALL);
        }
        // no checks passed
        return false;
    }

    //// CLIENT SERVER SYNC ////

    /**
     * Called when the chunk is saved
     * @return the compound tag to use in #handleUpdateTag
     */
    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    //// NBT ////

    private static final String KEY_FILL_TOP_LAYER = "FillTopLayer";
    private static final String KEY_FORCE_OBSTRUCTED_UPDATE = "UpdateObstructed";
    private static final String KEY_OBSTRUCTED = "Obstructed";

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        this.tank.readFromNBT(tag);
        this.fillTopLayer = tag.getBoolean(KEY_FILL_TOP_LAYER);
        this.isObstructed = tag.getBoolean(KEY_OBSTRUCTED);
        this.forceUpdateObstructed = tag.getBoolean(KEY_FORCE_OBSTRUCTED_UPDATE);
    }

    @Override
    public void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        this.tank.writeToNBT(tag);
        tag.putBoolean(KEY_FILL_TOP_LAYER, fillTopLayer);
        tag.putBoolean(KEY_OBSTRUCTED, isObstructed);
        tag.putBoolean(KEY_FORCE_OBSTRUCTED_UPDATE, forceUpdateObstructed);
    }

    //// CLASSES ////

    /**
     * Custom iterator to scan block positions in increasingly larger areas
     * around a given position and within a given bounds. The result is an iterator
     * that can be used to place fluids adjacent to other fluids without the need
     * for pathfinding or heavy assumptions about where the next fluid should be placed.
     */
    public static class PlaceFluidIterator implements Iterator<BlockPos> {

        /** Used to always skip block positions that were removed with {@link #remove()} **/
        private final Set<Long> invalidSuperSet;
        /** Used to track which block positions were already checked in a single layer **/
        private final Set<Long> invalid;
        /** The start position of the iterator, assumed to be within the bounds **/
        private final BlockPos start;
        /** The minimum position of the iterator for use in {@link BlockPos#betweenClosed(BlockPos, BlockPos)} **/
        private final BlockPos.MutableBlockPos min;
        /** The maximum position of the iterator for use in {@link BlockPos#betweenClosed(BlockPos, BlockPos)} **/
        private final BlockPos.MutableBlockPos max;
        /** The origin block position, assumed to be next to start **/
        private final BlockPos origin;
        /** The maximum bounds to iterate **/
        private final BoundingBox bounds;

        /** The most recently calculated position **/
        private final BlockPos.MutableBlockPos cursor;
        /** The next position, if any **/
        private BlockPos.MutableBlockPos next;

        /** The current layer index of the iterator **/
        private int layer;
        /** The current BlockPos iterator as created by {@link BlockPos#betweenClosed(BlockPos, BlockPos)}, can be empty **/
        private Iterator<BlockPos> iterator;

        public PlaceFluidIterator(final BlockPos origin, final BlockPos start, final BoundingBox bounds) {
            this.origin = origin.immutable();
            this.start = start.immutable();
            this.bounds = bounds;
            this.min = start.mutable();
            this.max = start.mutable();
            this.cursor = start.mutable();
            this.next = start.mutable();
            this.layer = bounds.minY() - start.getY();
            this.invalidSuperSet = new HashSet<>();
            this.invalid = new HashSet<>();
            this.iterator = calculateNextIterator();
        }

        private boolean isReachedMaxLayer() {
            int maxLayer = (bounds.maxY()  - start.getY());
            return layer > maxLayer;
        }

        private boolean isReachedMaxBounds() {
            return (max.getX() - min.getX()) >= (bounds.maxX() - bounds.minX()) && (max.getZ() - min.getZ()) >= (bounds.maxZ() - bounds.minZ());
        }

        private Iterator<BlockPos> calculateNextIterator() {
            // check for max bounds
            if(isReachedMaxBounds()) {
                // update layer and invalid set
                layer++;
                invalid.clear();
                // reset min and max
                min.setWithOffset(start, 0, layer, 0);
                max.setWithOffset(start, 0, layer, 0);
                // check for max layer and max bounds
                if(isReachedMaxLayer()) {
                    return Collections.emptyIterator();
                }
            } else {
                // increase min and max
                min.set(Math.max(bounds.minX(), min.getX() - 1), min.getY(), Math.max(bounds.minZ(), min.getZ() - 1));
                max.set(Math.min(bounds.maxX(), max.getX() + 1), max.getY(), Math.min(bounds.maxZ(), max.getZ() + 1));
            }
            // create iterator
            return BlockPos.betweenClosed(min, max).iterator();
        }

        private Optional<BlockPos> calculateNext() {
            while(iterator.hasNext()) {
                // load next position
                BlockPos p = iterator.next();
                // reset iterator if necessary
                if(!iterator.hasNext()) {
                    iterator = calculateNextIterator();
                }
                // verify position was not already checked
                long key = p.asLong();
                if (invalid.contains(key) || invalidSuperSet.contains(key)) continue;
                // add to set of checked positions
                invalid.add(key);
                // return the first position that has not been visited already
                return Optional.of(p);
            }
            // all iterators completed, there is no next element
            return Optional.empty();
        }

        @Override
        public boolean hasNext() {
            return next != null;
        }

        @Override
        public BlockPos next() {
            cursor.set(next);
            calculateNext().ifPresentOrElse(p -> next.set(p), () -> next = null);
            return cursor;
        }

        @Override
        public void remove() {
            invalidSuperSet.add(cursor.asLong());
        }

        @Override
        public void forEachRemaining(Consumer<? super BlockPos> action) {
            while(hasNext()) {
                action.accept(next());
            }
        }
    }
}
