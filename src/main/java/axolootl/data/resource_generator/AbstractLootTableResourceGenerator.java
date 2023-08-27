/**
 * Copyright (c) 2023 Skyler James
 * Permission is granted to use, modify, and redistribute this software, in parts or in whole,
 * under the GNU LGPLv3 license (https://www.gnu.org/licenses/lgpl-3.0.en.html)
 **/

package axolootl.data.resource_generator;

import axolootl.Axolootl;
import axolootl.util.AxCodecUtils;
import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.RandomSource;
import net.minecraft.util.random.SimpleWeightedRandomList;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootTable;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

public abstract class AbstractLootTableResourceGenerator extends ResourceGenerator {

    protected static final Codec<SimpleWeightedRandomList<Wrapper>> WEIGHTED_LIST_CODEC = Codec.either(Wrapper.CODEC, SimpleWeightedRandomList.wrappedCodecAllowingEmpty(Wrapper.CODEC))
            .xmap(either -> either.map(SimpleWeightedRandomList::single, Function.identity()),
                    list -> list.unwrap().size() == 1 ? Either.left(list.unwrap().get(0).getData()) : Either.right(list));

    private final SimpleWeightedRandomList<AbstractLootTableResourceGenerator.Wrapper> list;

    public AbstractLootTableResourceGenerator(SimpleWeightedRandomList<AbstractLootTableResourceGenerator.Wrapper> list) {
        super(ResourceType.MOB);
        this.list = list;
    }

    public SimpleWeightedRandomList<AbstractLootTableResourceGenerator.Wrapper> getList() {
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
        final Optional<Wrapper> oLootTableId = getList().getRandomValue(random);
        if (oLootTableId.isEmpty()) {
            return ImmutableList.of();
        }
        final LootTable lootTable = server.getLootTables().get(oLootTableId.get().getId());
        if (lootTable == LootTable.EMPTY) {
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

    //// HELPER CLASSES ////

    public static class Wrapper {

        protected static final Codec<AbstractLootTableResourceGenerator.Wrapper> DIRECT_CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ResourceLocation.CODEC.fieldOf("id").forGetter(Wrapper::getId),
                AxCodecUtils.ITEM_OR_STACK_CODEC.optionalFieldOf("display", ItemStack.EMPTY).forGetter(Wrapper::getDisplay)
        ).apply(instance, AbstractLootTableResourceGenerator.Wrapper::new));

        protected static final Codec<AbstractLootTableResourceGenerator.Wrapper> CODEC = Codec.either(ResourceLocation.CODEC, DIRECT_CODEC)
                .xmap(either -> either.map(id -> new Wrapper(id, ItemStack.EMPTY), Function.identity()),
                        wrapper -> wrapper.getDisplay().isEmpty() ? Either.left(wrapper.getId()) : Either.right(wrapper));

        private final ResourceLocation id;
        private final ItemStack display;

        public Wrapper(ResourceLocation id, ItemStack display) {
            this.id = id;
            this.display = display;
        }

        //// GETTERS ////

        public ResourceLocation getId() {
            return id;
        }

        public ItemStack getDisplay() {
            return display;
        }

        //// EQUALITY ////

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Wrapper)) return false;
            Wrapper wrapper = (Wrapper) o;
            return Objects.equals(id, wrapper.id) && Objects.equals(display, wrapper.display);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, display);
        }
    }
}
