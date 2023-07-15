package axolootl.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.data.BuiltinRegistries;
import net.minecraft.resources.RegistryFileCodec;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.concurrent.Immutable;

@Immutable
public class BonusesProvider {

    private static final Codec<HolderSet<Item>> ITEM_HOLDER_SET_CODEC = RegistryCodecs.homogeneousList(ForgeRegistries.Keys.ITEMS);

    public static final Codec<BonusesProvider> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ITEM_HOLDER_SET_CODEC.fieldOf("item").forGetter(BonusesProvider::getFoods),
            Bonuses.CODEC.optionalFieldOf("bonus", Bonuses.EMPTY).forGetter(BonusesProvider::getBonuses)
    ).apply(instance, BonusesProvider::new));

    private final HolderSet<Item> foods;
    private final Bonuses bonuses;

    public BonusesProvider(HolderSet<Item> foods, Bonuses bonuses) {
        this.foods = foods;
        this.bonuses = bonuses;
    }

    //// GETTERS ////

    public HolderSet<Item> getFoods() {
        return foods;
    }

    public Bonuses getBonuses() {
        return bonuses;
    }
}
