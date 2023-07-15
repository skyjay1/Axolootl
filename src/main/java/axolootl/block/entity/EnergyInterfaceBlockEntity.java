package axolootl.block.entity;

import axolootl.AxRegistry;
import axolootl.block.EnergyInterfaceBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.EnergyStorage;
import net.minecraftforge.energy.IEnergyStorage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class EnergyInterfaceBlockEntity extends BlockEntity implements IAquariumControllerProvider {

    protected IEnergyStorage energy = new EnergyStorage(25_000, 10_000) {
        @Override
        public boolean canExtract() {
            return !getBlockState().getValue(EnergyInterfaceBlock.POWERED);
        }
    };
    private final LazyOptional<IEnergyStorage> holder = LazyOptional.of(() -> energy);

    private BlockPos controllerPos;
    private ControllerBlockEntity controller;

    public EnergyInterfaceBlockEntity(BlockPos pPos, BlockState pBlockState) {
        this(AxRegistry.BlockEntityReg.ENERGY_INTERFACE.get(), pPos, pBlockState);
    }

    public EnergyInterfaceBlockEntity(BlockEntityType<?> pType, BlockPos pPos, BlockState pBlockState) {
        super(pType, pPos, pBlockState);
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
        this.setChanged();
    }

    @Override
    public Optional<ControllerBlockEntity> getController() {
        return Optional.ofNullable(controller);
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

    //// CAPABILITY ////

    @Override
    @NotNull
    public <T> LazyOptional<T> getCapability(@NotNull Capability<T> capability, @Nullable Direction facing)
    {
        if (capability == ForgeCapabilities.ENERGY) {
            return holder.cast();
        }
        return super.getCapability(capability, facing);
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

}
