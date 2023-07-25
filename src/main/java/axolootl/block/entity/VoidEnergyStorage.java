/**
 * Copyright (c) 2023 Skyler James
 * Permission is granted to use, modify, and redistribute this software, in parts or in whole,
 * under the GNU LGPLv3 license (https://www.gnu.org/licenses/lgpl-3.0.en.html)
 **/

package axolootl.block.entity;

import net.minecraftforge.energy.IEnergyStorage;

/**
 * Implementation of {@link IEnergyStorage} that can only receive and has no storage.
 * Energy received by this storage is lost to the void, never to be seen again.
 */
public class VoidEnergyStorage implements IEnergyStorage {

    public static final VoidEnergyStorage INSTANCE = new VoidEnergyStorage();

    protected VoidEnergyStorage() {}

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
        return 0;
    }
}
