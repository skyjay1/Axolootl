/**
 * Copyright (c) 2023 Skyler James
 * Permission is granted to use, modify, and redistribute this software, in parts or in whole,
 * under the GNU LGPLv3 license (https://www.gnu.org/licenses/lgpl-3.0.en.html)
 **/

package axolootl.data.resource_generator;

import axolootl.AxRegistry;
import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.util.random.SimpleWeightedRandomList;
import net.minecraft.util.random.WeightedEntry;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;

public class MobDropsResourceGenerator extends LootContextResourceGenerator {

    public static final Codec<MobDropsResourceGenerator> CODEC = WEIGHTED_LIST_CODEC
            .xmap(MobDropsResourceGenerator::new, LootContextResourceGenerator::getList).fieldOf("loot_table").codec();

    public MobDropsResourceGenerator(SimpleWeightedRandomList<Wrapper> list) {
        super(ResourceTypes.MOB, list);
    }

    @Override
    public Codec<? extends ResourceGenerator> getCodec() {
        return AxRegistry.ResourceGeneratorsReg.MOB.get();
    }

    @Override
    protected LootContext createContext(LivingEntity entity, RandomSource random) {
        return new LootContext.Builder((ServerLevel) entity.level)
                .withRandom(random)
                .withParameter(LootContextParams.THIS_ENTITY, entity)
                .withParameter(LootContextParams.ORIGIN, entity.position())
                .withParameter(LootContextParams.DAMAGE_SOURCE, DamageSource.mobAttack(entity))
                .withParameter(LootContextParams.KILLER_ENTITY, entity)
                .withParameter(LootContextParams.DIRECT_KILLER_ENTITY, entity)
                .create(LootContextParamSets.ENTITY);
    }

    @Override
    public List<ResourceDescriptionGroup> createDescription() {
        final ResourceDescriptionGroup.Builder builder = ResourceDescriptionGroup.builder();
        final int totalWeight = calculateTotalWeight(getList());
        // iterate each entry
        for(WeightedEntry.Wrapper<LootContextResourceGenerator.Wrapper> wrapper : getList().unwrap()) {
            // check for display item
            ItemStack itemStack = wrapper.getData().getDisplay();
            Component description = Component.translatable("axolootl.resource_generator.mob", getDescriptionForLootTable(wrapper.getData().getId()));
            builder.with(new ResourceDescription(itemStack, wrapper.getWeight().asInt(), totalWeight, ImmutableList.of(description)));
        }
        return ImmutableList.of(builder.build());
    }

    private Component getDescriptionForLootTable(final ResourceLocation lootTableId) {
        String path = lootTableId.getPath();
        path = path.substring(Math.max(0, path.lastIndexOf("/") + 1));
        final ResourceLocation entityId = new ResourceLocation(lootTableId.getNamespace(), path);
        // query the entity type from the registry (may be null)
        final EntityType<?> entityType = ForgeRegistries.ENTITY_TYPES.getValue(entityId);
        if(null == entityType) {
            return Component.literal("#" + lootTableId.toString()).withStyle(ChatFormatting.GRAY);
        }
        // create description using the name of the registered entity type
        return entityType.getDescription();
    }

    @Override
    public String toString() {
        return "Mob: " + getList().toString();
    }
}
