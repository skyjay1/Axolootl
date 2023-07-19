package axolootl.data.resource_generator;

import axolootl.AxRegistry;
import axolootl.Axolootl;
import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

import javax.annotation.concurrent.Immutable;
import java.util.Collection;
import java.util.Optional;

@Immutable
public class BlockDropsResourceGenerator extends ResourceGenerator {

    public static final Codec<BlockDropsResourceGenerator> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ITEM_OR_STACK_CODEC.optionalFieldOf("tool", ItemStack.EMPTY).forGetter(BlockDropsResourceGenerator::getTool),
            BlockStateProvider.CODEC.fieldOf("block_provider").forGetter(BlockDropsResourceGenerator::getBlockProvider)
    ).apply(instance, BlockDropsResourceGenerator::new));

    private final ItemStack tool;
    private final BlockStateProvider blockProvider;

    public BlockDropsResourceGenerator(ItemStack tool, BlockStateProvider blockProvider) {
        super(ResourceType.BLOCK);
        this.tool = tool;
        this.blockProvider = blockProvider;
    }

    public ItemStack getTool() {
        return tool;
    }

    public BlockStateProvider getBlockProvider() {
        return blockProvider;
    }

    @Override
    public Collection<ItemStack> getRandomEntries(LivingEntity entity, RandomSource random) {
        // validate server
        final MinecraftServer server = entity.getServer();
        if (null == server) {
            return ImmutableList.of();
        }
        // load loot table
        final BlockState blockState = getBlockProvider().getState(random, entity.blockPosition());
        final ResourceLocation lootTableId = blockState.getBlock().getLootTable();
        final LootTable lootTable = server.getLootTables().get(lootTableId);
        if (lootTable == LootTable.EMPTY) {
            Axolootl.LOGGER.warn("[ResourceGenerator#getRandomEntries] Failed to load loot table " + lootTableId);
            return ImmutableList.of();
        }
        // create loot table context
        final LootContext context = createContext(entity, blockState, random);
        // generate items
        return lootTable.getRandomItems(context);
    }

    @Override
    public Codec<? extends ResourceGenerator> getCodec() {
        return AxRegistry.ResourceGeneratorsReg.BLOCK.get();
    }

    /**
     * @param entity the entity
     * @param random the random instance
     * @return a loot context to generate random items from the selected loot table
     */
    protected LootContext createContext(LivingEntity entity, BlockState block, RandomSource random) {
        return new LootContext.Builder((ServerLevel) entity.level)
                .withRandom(random)
                .withParameter(LootContextParams.TOOL, this.tool)
                .withParameter(LootContextParams.BLOCK_STATE, block)
                .withParameter(LootContextParams.ORIGIN, entity.position())
                .withParameter(LootContextParams.THIS_ENTITY, entity)
                .create(LootContextParamSets.BLOCK);
    }

    @Override
    public String toString() {
        return "BlockState: {tool=" + tool.getDisplayName() + ", block=" + blockProvider.toString() + "}";
    }
}
