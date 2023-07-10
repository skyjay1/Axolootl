package axolootl.data;

import axolootl.AxRegistry;
import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.ListCodec;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.RandomSource;
import net.minecraft.util.random.SimpleWeightedRandomList;
import net.minecraft.util.random.WeightedEntry;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraftforge.common.world.BiomeModifier;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;

public abstract class ResourceGenerator {

    public static final Codec<? extends ResourceGenerator> CODEC = AxRegistry.RESOURCE_GENERATORS_DISPATCHER.dispatchedCodec();

    public static final Codec<ResourceGenerator> DIRECT_CODEC = ExtraCodecs.lazyInitializedCodec(() -> AxRegistry.RESOURCE_GENERATORS_DISPATCHER.registry().getCodec())
            .dispatch(ResourceGenerator::codec, Function.identity());

    public static final Codec<? extends List<? extends ResourceGenerator>> LIST_CODEC = CODEC.listOf();
    private final ResourceType resourceType;

    public ResourceGenerator(ResourceType resourceType) {
        this.resourceType = resourceType;
    }

    /**
     * @return the {@link ResourceType} of the generator
     */
    public ResourceType getResourceType() {
        return resourceType;
    }

    /**
     * Generates any number of items
     * @param entity the entity
     * @param random the random instance
     * @return a collection of generated items, may be empty
     */
    public abstract Collection<ItemStack> getRandomEntries(final LivingEntity entity, final RandomSource random);

    /**
     * @return the codec for this resource generator, used in the dispatcher
     */
    public abstract Codec<? extends ResourceGenerator> getCodec();

    // SIMPLE WEIGHTED LIST GENERATOR //

    public static class Resource extends ResourceGenerator {

        // TODO rework this to accept Items, ItemStacks, or Item Tag Keys

        public static final Codec<ItemStack> ITEM_OR_STACK_CODEC = Codec.either(ForgeRegistries.ITEMS.getCodec(), ItemStack.CODEC)
                .xmap(either -> either.map(ItemStack::new, Function.identity()),
                        stack -> stack.getCount() == 1 && !stack.hasTag()
                                ? Either.left(stack.getItem())
                                : Either.right(stack));

        public static final Codec<ResourceGenerator.Resource> CODEC = SimpleWeightedRandomList.wrappedCodecAllowingEmpty(ITEM_OR_STACK_CODEC)
                .xmap(Resource::new, instance -> instance.list);

        private static final WeightedEntry.Wrapper<ItemStack> EMPTY_WRAPPER = WeightedEntry.wrap(ItemStack.EMPTY, 1);
        private final SimpleWeightedRandomList<ItemStack> list;

        public Resource(final SimpleWeightedRandomList<ItemStack> list) {
            super(ResourceType.RESOURCE);
            this.list = list;
        }

        @Override
        public Collection<ItemStack> getRandomEntries(final LivingEntity entity, RandomSource random) {
            return ImmutableList.of(list.getRandom(random).orElse(EMPTY_WRAPPER).getData());
        }

        @Override
        public Codec<? extends ResourceGenerator> getCodec() {
            return AxRegistry.ResourceGenerators.RESOURCE.get();
        }
    }

    // MOB LOOT TABLE GENERATOR //

    public static class Mob extends ResourceGenerator {

        public static final Codec<ResourceGenerator.Mob> CODEC = ResourceLocation.CODEC.xmap(Mob::new, instance -> instance.lootTableId);

        private final ResourceLocation lootTableId;

        public Mob(final ResourceLocation lootTableId) {
            super(ResourceType.MOB);
            this.lootTableId = lootTableId;
        }

        @Override
        public Collection<ItemStack> getRandomEntries(LivingEntity entity, RandomSource random) {
            final MinecraftServer server = entity.getServer();
            if(null == server) {
                return ImmutableList.of(ItemStack.EMPTY);
            }
            final LootContext context = new LootContext.Builder((ServerLevel) entity.level)
                    .withRandom(random)
                    .withParameter(LootContextParams.THIS_ENTITY, entity)
                    .withParameter(LootContextParams.ORIGIN, entity.position())
                    .withParameter(LootContextParams.DAMAGE_SOURCE, DamageSource.mobAttack(entity))
                    .withParameter(LootContextParams.KILLER_ENTITY, entity)
                    .withParameter(LootContextParams.DIRECT_KILLER_ENTITY, entity)
                    .create(LootContextParamSets.ENTITY);
            return server.getLootTables().get(lootTableId).getRandomItems(context);
        }

        @Override
        public Codec<? extends ResourceGenerator> getCodec() {
            return AxRegistry.ResourceGenerators.MOB.get();
        }
    }
}
