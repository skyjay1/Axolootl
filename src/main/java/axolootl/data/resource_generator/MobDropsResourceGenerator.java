/**
 * Copyright (c) 2023 Skyler James
 * Permission is granted to use, modify, and redistribute this software, in parts or in whole,
 * under the GNU LGPLv3 license (https://www.gnu.org/licenses/lgpl-3.0.en.html)
 **/

package axolootl.data.resource_generator;

import axolootl.AxRegistry;
import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import net.minecraft.Util;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.util.random.SimpleWeightedRandomList;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

import javax.annotation.concurrent.Immutable;
import java.util.List;

public class MobDropsResourceGenerator extends AbstractLootTableResourceGenerator {

    public static final Codec<MobDropsResourceGenerator> CODEC = WEIGHTED_LIST_CODEC
            .xmap(MobDropsResourceGenerator::new, AbstractLootTableResourceGenerator::getList).fieldOf("loot_table").codec();

    public MobDropsResourceGenerator(SimpleWeightedRandomList<ResourceLocation> list) {
        super(list);
    }

    private static Component createMobLootTableDescription(final ResourceLocation id) {
        String path = id.getPath();
        path = path.substring(Math.max(0, path.lastIndexOf("/") + 1));
        final Component entity = Component.translatable(Util.makeDescriptionId("entity", new ResourceLocation(id.getNamespace(), path)));
        return Component.translatable("axolootl.resource_generator.mob", entity);
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
    public List<Component> createDescription() {
        return createDescription(getList(), MobDropsResourceGenerator::createMobLootTableDescription);
    }

    @Override
    public String toString() {
        return "Mob: " + getList().toString();
    }
}
