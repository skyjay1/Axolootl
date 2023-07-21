/**
 * Copyright (c) 2023 Skyler James
 * Permission is granted to use, modify, and redistribute this software, in parts or in whole,
 * under the GNU LGPLv3 license (https://www.gnu.org/licenses/lgpl-3.0.en.html)
 **/

package axolootl.block.entity;

import axolootl.AxRegistry;
import axolootl.block.WaterInterfaceBlock;
import axolootl.util.TankMultiblock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlockContainer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.fluids.capability.FluidHandlerBlockEntity;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.EmptyFluidHandler;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

public class WaterInterfaceBlockEntity extends FluidHandlerBlockEntity implements IAquariumControllerProvider, MenuProvider {

    /** The number to multiply by the corresponding speed and subtract from the corresponding ticker each tick **/
    public static final long BASE_SPEED_DECREMENT = 100;
    public static final int FLUIDS_SCANNED_PER_TICK = 20;

    private BlockPos controllerPos;
    private ControllerBlockEntity controller;

    private double placeFluidSpeed = 1.0D;
    private long placeFluidTime;
    private Iterator<BlockPos> placeFluidIterator;

    public WaterInterfaceBlockEntity(BlockPos pPos, BlockState pBlockState) {
        this(AxRegistry.BlockEntityReg.WATER_INTERFACE.get(), pPos, pBlockState);
    }

    public WaterInterfaceBlockEntity(BlockEntityType<?> pType, BlockPos pPos, BlockState pBlockState) {
        super(pType, pPos, pBlockState);
        this.tank.setCapacity(FluidType.BUCKET_VOLUME * 12);
        this.tank.setValidator(f -> f.getFluid().getFluidType() == ForgeMod.WATER_TYPE.get());
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
        // verify not powered
        if(state.getValue(WaterInterfaceBlock.POWERED)) {
            return;
        }
        // verify has fluid
        final IFluidHandler fluidHandler = self.getCapability(ForgeCapabilities.FLUID_HANDLER).orElse(EmptyFluidHandler.INSTANCE);
        final FluidStack fluidStack = fluidHandler.getFluidInTank(0);
        if(!fluidStack.getFluid().isSame(Fluids.WATER) || fluidStack.getAmount() < FluidType.BUCKET_VOLUME) {
            return;
        }
        // attempt to place fluid
        markDirty |= self.tickPlaceFluid(level, fluidHandler);
        // mark changed and send update
        if(markDirty) {
            self.setChanged();
            level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
        }
    }

    //// MENU PROVIDER ////

    public boolean isMenuAvailable(Player player, ControllerBlockEntity controller) {
        return AxRegistry.AquariumTabsReg.FLUID_INTERFACE.get().isAvailable(controller);
    }

    @Override
    public Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int pContainerId, Inventory pPlayerInventory, Player pPlayer) {
        if(!isMenuAvailable(pPlayer, controller)) {
            return null;
        }
        // TODO create fluid interface menu
        return null;
    }

    //// CONTROLLER PROVIDER ////

    @Override
    public void setController(final Level level, final BlockPos pos, final ControllerBlockEntity blockEntity) {
        this.controllerPos = pos;
        this.controller = blockEntity;
        this.setChanged();
    }

    @Override
    public void clearController() {
        this.controllerPos = null;
        this.controller = null;
        this.placeFluidIterator = null;
        this.setChanged();
    }

    @Override
    public Optional<ControllerBlockEntity> getController() {
        return Optional.ofNullable(controller);
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

    //// PLACE FLUID ////

    /**
     * @param level the server level
     * @param fluidHandler the fluid handler
     * @return true if there are any changes, such as tickers resetting or fluids changing
     **/
    private boolean tickPlaceFluid(Level level, IFluidHandler fluidHandler) {
        // validate ticker
        if (placeFluidTime > 0) {
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
        if(oPos.isEmpty() || !placeFluidBlock(level, oPos.get(), Fluids.WATER, Fluids.FLOWING_WATER, false)) {
            return false;
        }
        // fluid block was placed, remove fluid from handler
        fluidHandler.drain(fluidStack, IFluidHandler.FluidAction.EXECUTE);
        // reset ticker
        placeFluidTime = 80L * BASE_SPEED_DECREMENT; // TODO balance
        // all checks passed
        return true;
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
        if(null == placeFluidIterator || !placeFluidIterator.hasNext()) {
            final TankMultiblock.Size size = controller.getSize().orElse(TankMultiblock.Size.EMPTY);
            final BoundingBox bounds = size.boundingBox().inflatedBy(-1);
            // determine a valid starting position inside bounds
            final Optional<BlockPos> oPos = findNeighborPositionInsideBounds(level, origin, fluid, flowing, bounds, false);
            if(oPos.isEmpty()) {
                return Optional.empty();
            }
            // create iterator
            this.placeFluidIterator = new PlaceFluidIterator(origin, oPos.get().immutable(), bounds);
            // attempt to place at start position for aesthetic reasons
            if(placeFluidBlock(level, oPos.get(), fluid, flowing, true)) {
                return oPos;
            }
        }
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
     * Finds an adjacent block that is within the given bounds and can accept the given fluid
     * @param level the level
     * @param origin the block position
     * @param fluid the fluid
     * @param flowing the flowing fluid
     * @param bounds the bounds
     * @param sourceOnly true to only consider source blocks
     * @return the first block position that was found, if any
     */
    private Optional<BlockPos> findNeighborPositionInsideBounds(final Level level, final BlockPos origin, final Fluid fluid, final Fluid flowing, BoundingBox bounds, boolean sourceOnly) {
        // brute force to determine first position inside tank bounds
        BlockPos.MutableBlockPos pos = origin.mutable();
        for(Direction d : Direction.values()) {
            if(bounds.isInside(pos.setWithOffset(origin, d)) && (isFluidSource(level, pos, fluid) || (!sourceOnly && placeFluidBlock(level, pos, fluid, flowing, true)))) {
                return Optional.of(pos);
            }
        }
        return Optional.empty();
    }

    private boolean isFluidSource(Level level, BlockPos pos, Fluid fluid) {
        return level.getFluidState(pos).is(fluid);
    }

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

    //// SETTERS AND GETTERS ////

    /**
     * @param level the level
     * @return true if the controller changed
     */
    private boolean validateController(final Level level) {
        // validate position
        if(null == controllerPos) {
            this.controller = null;
            return true;
        }
        // validate block entity
        BlockEntity blockEntity = level.getBlockEntity(controllerPos);
        if(blockEntity instanceof ControllerBlockEntity controllerBlockEntity && controllerBlockEntity != this.controller) {
            this.controller = controllerBlockEntity;
            return true;
        }
        // no changes
        return false;
    }

    //// NBT ////

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
    }

    @Override
    public void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
    }

    //// CLASSES ////

    public static class PlaceFluidIterator implements Iterator<BlockPos> {

        private int layer;
        private final Set<Long> invalid;
        private final BlockPos start;
        private final BlockPos.MutableBlockPos min;
        private final BlockPos.MutableBlockPos max;
        private final BlockPos origin;
        private final BoundingBox bounds;
        private Iterator<BlockPos> betweenClosed;

        public PlaceFluidIterator(final BlockPos origin, final BlockPos start, final BoundingBox bounds) {
            this.origin = origin;
            this.start = start;
            this.bounds = bounds;
            this.min = start.mutable();
            this.max = start.mutable();
            this.layer = 0;
            this.invalid = new HashSet<>();
            this.betweenClosed = BlockPos.betweenClosed(min, max).iterator();
        }

        private boolean isReachedMaxLayer() {
            return layer >= bounds.getYSpan();
        }

        private boolean isReachedMaxBounds() {
            return (max.getX() - min.getX() + 1) >= bounds.getXSpan() && (max.getZ() - min.getZ() + 1) >= bounds.getZSpan();
        }

        private boolean betweenClosedDone() {
            return !betweenClosed.hasNext();
        }

        private boolean isLayerDone() {
            return isReachedMaxBounds() && betweenClosedDone();
        }

        @Override
        public boolean hasNext() {
            return !isReachedMaxLayer() || !isLayerDone();
        }

        @Override
        public BlockPos next() {
            // scan each layer
            while(!isReachedMaxLayer() || !isLayerDone()) {
                // calculate progressively larger bounding box
                while (!isLayerDone()) {
                    // iterate all positions in bounding box
                    while(!betweenClosedDone()) {
                        // load next position
                        BlockPos p = betweenClosed.next();
                        // verify position was not already checked
                        if (invalid.contains(p.asLong())) continue;
                        // add to set of checked positions
                        invalid.add(p.asLong());
                        // return the first position that has not been visited already
                        return p;
                    }
                    // increase min and max
                    min.set(Math.max(bounds.minX(), min.getX() - 1), min.getY(), Math.max(bounds.minZ(), min.getZ() - 1));
                    max.set(Math.min(bounds.maxX(), max.getX() + 1), max.getY(), Math.min(bounds.maxZ(), max.getZ() + 1));
                    // update iterator
                    betweenClosed = BlockPos.betweenClosed(min, max).iterator();
                }
                // increase layer and reset values
                layer++;
                min.setWithOffset(start, 0, layer, 0);
                max.setWithOffset(start, 0, layer, 0);
                invalid.clear();
            }
            return start;
        }

        @Override
        public void remove() {
            Iterator.super.remove();
        }

        @Override
        public void forEachRemaining(Consumer<? super BlockPos> action) {
            while(hasNext()) {
                action.accept(next());
            }
        }
    }
}
