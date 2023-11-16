/**
 * Copyright (c) 2023 Skyler James
 * Permission is granted to use, modify, and redistribute this software, in parts or in whole,
 * under the GNU LGPLv3 license (https://www.gnu.org/licenses/lgpl-3.0.en.html)
 **/

package axolootl.data.aquarium_modifier.condition;

import axolootl.AxRegistry;
import axolootl.data.aquarium_modifier.AquariumModifierContext;
import axolootl.util.AxCodecUtils;
import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import net.minecraft.Util;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;

import javax.annotation.concurrent.Immutable;
import java.util.ArrayList;
import java.util.List;

@Immutable
public class BlockModifierCondition extends ModifierCondition {

    public static final Codec<BlockModifierCondition> CODEC = BlockPredicate.CODEC
            .xmap(BlockModifierCondition::new, BlockModifierCondition::getPredicate).fieldOf("predicate").codec();

    private final BlockPredicate predicate;

    public BlockModifierCondition(BlockPredicate predicate) {
        this.predicate = predicate;
    }

    public BlockPredicate getPredicate() {
        return predicate;
    }

    @Override
    public boolean test(AquariumModifierContext aquariumModifierContext) {
        final ServerLevel level = (ServerLevel)aquariumModifierContext.getLevel();
        return this.predicate.test(level, aquariumModifierContext.getPos());
    }

    @Override
    public Codec<? extends ModifierCondition> getCodec() {
        return AxRegistry.ModifierConditionsReg.BLOCK.get();
    }

    @Override
    public List<Component> createDescription(final RegistryAccess registryAccess) {
        return createDescription(predicate);
    }

    @Override
    public String toString() {
        final ResourceLocation id = BuiltInRegistries.BLOCK_PREDICATE_TYPE.getKey(predicate.type());
        if(null == id) {
            return "block {ERROR}";
        }
        return "block {" + id.toString() + "}";
    }

    protected static List<Component> createDescription(final BlockPredicate blockPredicate) {
        final List<Component> list = new ArrayList<>();
        final ResourceLocation id = BuiltInRegistries.BLOCK_PREDICATE_TYPE.getKey(blockPredicate.type());
        // TODO improve by adding support for each block predicate type
        if(id != null) {
            final String key = Util.makeDescriptionId("block_predicate_type", id);
            list.add(Component.translatable(key));
        }
        return list;
    }
}
