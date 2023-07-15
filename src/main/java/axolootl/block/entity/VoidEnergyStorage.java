package axolootl.block.entity;

import net.minecraftforge.energy.IEnergyStorage;

/**
 * Implementation of {@link IEnergyStorage} that can only receive and has no storage.
 * Energy received by this storage is lost to the void, never to be seen again.
 */
public class VoidEnergyStorage implements IEnergyStorage {

    public VoidEnergyStorage() {}

    @Override
    public boolean canExtract() {
        return false;
    }

    @Override
    public boolean canReceive() {
        return true;
    }

    @Override
    public int receiveEnergy(int maxReceive, boolean simulate) {
        return maxReceive;
    }

    @Override
    public int extractEnergy(int maxExtract, boolean simulate) {
        return 0;
    }

    @Override
    public int getEnergyStored() {
        return 0;
    }

    @Override
    public int getMaxEnergyStored() {
        return Integer.MAX_VALUE;
    }
}
