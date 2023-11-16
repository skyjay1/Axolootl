/**
 * Copyright (c) 2023 Skyler James
 * Permission is granted to use, modify, and redistribute this software, in parts or in whole,
 * under the GNU LGPLv3 license (https://www.gnu.org/licenses/lgpl-3.0.en.html)
 **/

package axolootl.util;

import axolootl.AxRegistry;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicateType;
import net.minecraft.world.level.levelgen.blockpredicates.StateTestingPredicate;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.registries.ForgeRegistries;

public class AxMatchingFluidsPredicate extends StateTestingPredicate {

    public static final Codec<AxMatchingFluidsPredicate> CODEC = RecordCodecBuilder.create((instance) ->
            stateTestingCodec(instance)
                    .and(RegistryCodecs.homogeneousList(ForgeRegistries.Keys.FLUIDS, ForgeRegistries.FLUIDS.getCodec()).fieldOf("fluids").forGetter(o -> o.fluids))
                    .apply(instance, AxMatchingFluidsPredicate::new));

    private final HolderSet<Fluid> fluids;

    public AxMatchingFluidsPredicate(Vec3i offset, HolderSet<Fluid> fluids) {
        super(offset);
        this.fluids = fluids;
    }

    @Override
    protected boolean test(BlockState pState) {
        return pState.getFluidState().is(this.fluids);
    }

    @Override
    public BlockPredicateType<?> type() {
        return AxRegistry.BlockPredicateTypesReg.MATCHING_FLUIDS.get();
    }
}
