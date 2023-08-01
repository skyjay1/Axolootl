/**
 * Copyright (c) 2023 Skyler James
 * Permission is granted to use, modify, and redistribute this software, in parts or in whole,
 * under the GNU LGPLv3 license (https://www.gnu.org/licenses/lgpl-3.0.en.html)
 **/

package axolootl.data.resource_generator;

import axolootl.Axolootl;
import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import net.minecraft.Util;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.RandomSource;
import net.minecraft.util.random.SimpleWeightedRandomList;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;

import javax.annotation.concurrent.Immutable;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

@Immutable
public abstract class AbstractLootTableResourceGenerator extends ResourceGenerator {

    protected static final Codec<SimpleWeightedRandomList<ResourceLocation>> WEIGHTED_LIST_CODEC = Codec.either(ResourceLocation.CODEC, SimpleWeightedRandomList.wrappedCodecAllowingEmpty(ResourceLocation.CODEC))
            .xmap(either -> either.map(SimpleWeightedRandomList::single, Function.identity()),
                    list -> list.unwrap().size() == 1 ? Either.left(list.unwrap().get(0).getData()) : Either.right(list));

    private final SimpleWeightedRandomList<ResourceLocation> list;

    public AbstractLootTableResourceGenerator(SimpleWeightedRandomList<ResourceLocation> list) {
        super(ResourceType.MOB);
        this.list = list;
    }

    public SimpleWeightedRandomList<ResourceLocation> getList() {
        return list;
    }

    @Override
    public Collection<ItemStack> getRandomEntries(LivingEntity entity, RandomSource random) {
        // validate server
        final MinecraftServer server = entity.getServer();
        if (null == server) {
            return ImmutableList.of();
        }
        // load loot table
        final Optional<ResourceLocation> oLootTableId = getList().getRandomValue(random);
        if (oLootTableId.isEmpty()) {
            return ImmutableList.of();
        }
        final net.minecraft.world.level.storage.loot.LootTable lootTable = server.getLootTables().get(oLootTableId.get());
        if (lootTable == net.minecraft.world.level.storage.loot.LootTable.EMPTY) {
            Axolootl.LOGGER.warn("[ResourceGenerator#getRandomEntries] Failed to load loot table " + oLootTableId.get());
            return ImmutableList.of();
        }
        // create loot table context
        final LootContext context = createContext(entity, random);
        // generate items
        return lootTable.getRandomItems(context);
    }

    /**
     * @param entity the entity
     * @param random the random instance
     * @return a loot context to generate random items from the selected loot table
     */
    protected abstract LootContext createContext(LivingEntity entity, RandomSource random);

    @Override
    public String toString() {
        return "LootTable: " + list.toString();
    }
}
