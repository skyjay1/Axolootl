package axolootl.data.resource_generator;

import axolootl.AxRegistry;
import com.mojang.serialization.Codec;
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

@Immutable
public class MobDropsResourceGenerator extends AbstractLootTableResourceGenerator {

    public static final Codec<MobDropsResourceGenerator> CODEC = WEIGHTED_LIST_CODEC
            .xmap(MobDropsResourceGenerator::new, AbstractLootTableResourceGenerator::getList).fieldOf("loot_table").codec();

    public MobDropsResourceGenerator(SimpleWeightedRandomList<ResourceLocation> list) {
        super(list);
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
    public String toString() {
        return "Mob: " + getList().toString();
    }
}
