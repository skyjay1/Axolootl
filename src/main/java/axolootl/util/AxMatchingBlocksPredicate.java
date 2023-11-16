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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicateType;
import net.minecraft.world.level.levelgen.blockpredicates.StateTestingPredicate;
import net.minecraftforge.registries.ForgeRegistries;

public class AxMatchingBlocksPredicate extends StateTestingPredicate {

    public static final Codec<AxMatchingBlocksPredicate> CODEC = RecordCodecBuilder.create((instance) ->
            stateTestingCodec(instance)
                    .and(RegistryCodecs.homogeneousList(ForgeRegistries.Keys.BLOCKS, ForgeRegistries.BLOCKS.getCodec()).fieldOf("blocks").forGetter(o -> o.blocks))
                    .apply(instance, AxMatchingBlocksPredicate::new));

    private final HolderSet<Block> blocks;

    public AxMatchingBlocksPredicate(Vec3i offset, HolderSet<Block> blocks) {
        super(offset);
        this.blocks = blocks;
    }

    @Override
    protected boolean test(BlockState pState) {
        return pState.is(this.blocks);
    }

    @Override
    public BlockPredicateType<?> type() {
        return AxRegistry.BlockPredicateTypesReg.MATCHING_BLOCKS.get();
    }
}
