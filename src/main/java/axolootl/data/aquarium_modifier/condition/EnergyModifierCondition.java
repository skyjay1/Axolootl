/**
 * Copyright (c) 2023 Skyler James
 * Permission is granted to use, modify, and redistribute this software, in parts or in whole,
 * under the GNU LGPLv3 license (https://www.gnu.org/licenses/lgpl-3.0.en.html)
 **/

package axolootl.data.aquarium_modifier.condition;

import axolootl.AxRegistry;
import axolootl.data.aquarium_modifier.AquariumModifierContext;
import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.energy.IEnergyStorage;

import javax.annotation.concurrent.Immutable;
import java.util.List;
import java.util.Optional;

@Immutable
public class EnergyModifierCondition extends ModifierCondition {

    public static final Codec<EnergyModifierCondition> CODEC = Codec.LONG
            .xmap(EnergyModifierCondition::new, EnergyModifierCondition::getEnergy).fieldOf("energy").codec();

    private static final Capability<IEnergyStorage> ENERGY_STORAGE_CAPABILITY = CapabilityManager.get(new CapabilityToken<>() {});

    private final long energy;

    public EnergyModifierCondition(long energy) {
        this.energy = Math.max(0, energy);
    }

    public long getEnergy() {
        return energy;
    }

    @Override
    public boolean test(AquariumModifierContext aquariumModifierContext) {
        final BlockPos pos = aquariumModifierContext.getPos();
        final BlockEntity blockEntity = aquariumModifierContext.getLevel().getBlockEntity(pos);
        if(null == blockEntity) {
            return false;
        }
        final Optional<IEnergyStorage> oEnergy = blockEntity.getCapability(ENERGY_STORAGE_CAPABILITY).resolve();
        if(oEnergy.isEmpty()) {
            return false;
        }
        return oEnergy.get().getEnergyStored() >= getEnergy();
    }

    @Override
    public Codec<? extends ModifierCondition> getCodec() {
        return AxRegistry.ModifierConditionsReg.ENERGY.get();
    }

    @Override
    public List<Component> createDescription(final RegistryAccess registryAccess) {
        return ImmutableList.of(Component.translatable("axolootl.modifier_condition.energy", energy));
    }

    @Override
    public String toString() {
        return "energy {" + getEnergy() + "}";
    }
}
