package axolootl.data.resource_generator;

import axolootl.AxRegistry;
import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import javax.annotation.concurrent.Immutable;
import java.util.Collection;

@Immutable
public class EmptyResourceGenerator extends ResourceGenerator {

    public static final EmptyResourceGenerator INSTANCE = new EmptyResourceGenerator();

    public static final Codec<EmptyResourceGenerator> CODEC = Codec.unit(INSTANCE);

    public EmptyResourceGenerator() {
        super(ResourceType.EMPTY);
    }

    @Override
    public Collection<ItemStack> getRandomEntries(final LivingEntity entity, RandomSource random) {
        return ImmutableList.of();
    }

    @Override
    public Codec<? extends ResourceGenerator> getCodec() {
        return AxRegistry.ResourceGeneratorsReg.EMPTY.get();
    }

    @Override
    public String toString() {
        return "Empty";
    }
}
