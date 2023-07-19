package axolootl.data;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.data.BuiltinRegistries;
import net.minecraft.resources.RegistryFileCodec;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.concurrent.Immutable;
import java.util.List;

@Immutable
public class BonusesProvider {

    public static final List<BonusesProvider> FISH_BONUS_PROVIDERS = ImmutableList.<BonusesProvider>builder()
            .add(new BonusesProvider(HolderSet.direct(ForgeRegistries.ITEMS.getHolder(Items.TROPICAL_FISH).orElseThrow()), new Bonuses(0.05)))
            .add(new BonusesProvider(HolderSet.direct(ForgeRegistries.ITEMS.getHolder(Items.PUFFERFISH).orElseThrow()), new Bonuses(-0.1)))
            .add(new BonusesProvider(new HolderSet.Named<>(Registry.ITEM, ItemTags.FISHES), new Bonuses(0.02)))
            .build();

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
