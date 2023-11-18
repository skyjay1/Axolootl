/**
 * Copyright (c) 2023 Skyler James
 * Permission is granted to use, modify, and redistribute this software, in parts or in whole,
 * under the GNU LGPLv3 license (https://www.gnu.org/licenses/lgpl-3.0.en.html)
 **/

package axolootl.data.axolootl_variant;

import axolootl.util.AxCodecUtils;
import axolootl.util.DeferredHolderSet;
import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.concurrent.Immutable;
import java.util.List;

@Immutable
public class BonusesProvider {

    @SuppressWarnings("unchecked")
    public static final List<BonusesProvider> FISH_BONUS_PROVIDERS = ImmutableList.<BonusesProvider>builder()
            .add(new BonusesProvider(new DeferredHolderSet<>(ForgeRegistries.ITEMS.getResourceKey(Items.TROPICAL_FISH).get()), new Bonuses(0.05)))
            .add(new BonusesProvider(new DeferredHolderSet<>(ForgeRegistries.ITEMS.getResourceKey(Items.PUFFERFISH).get()), new Bonuses(-0.1)))
            .add(new BonusesProvider(new DeferredHolderSet<>(ItemTags.FISHES), new Bonuses(0.02)))
            .build();

    public static final Codec<BonusesProvider> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            DeferredHolderSet.codec(ForgeRegistries.ITEMS.getRegistryKey()).fieldOf("item").forGetter(BonusesProvider::getFoods),
            Bonuses.CODEC.optionalFieldOf("bonus", Bonuses.EMPTY).forGetter(BonusesProvider::getBonuses)
    ).apply(instance, BonusesProvider::new));

    private final DeferredHolderSet<Item> foods;
    private final Bonuses bonuses;

    public BonusesProvider(DeferredHolderSet<Item> foods, Bonuses bonuses) {
        this.foods = foods;
        this.bonuses = bonuses;
    }

    //// GETTERS ////

    public DeferredHolderSet<Item> getFoods() {
        return foods;
    }

    public Bonuses getBonuses() {
        return bonuses;
    }
}
