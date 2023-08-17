/**
 * Copyright (c) 2023 Skyler James
 * Permission is granted to use, modify, and redistribute this software, in parts or in whole,
 * under the GNU LGPLv3 license (https://www.gnu.org/licenses/lgpl-3.0.en.html)
 **/

package axolootl.data.resource_generator;

import axolootl.AxRegistry;
import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.tags.ITag;

import java.util.Collection;
import java.util.List;

public class ItemTagResourceGenerator extends ResourceGenerator {

    public static final Codec<ItemTagResourceGenerator> CODEC = TagKey.codec(ForgeRegistries.Keys.ITEMS)
            .xmap(ItemTagResourceGenerator::new, ItemTagResourceGenerator::getTag).fieldOf("tag").codec();

    private final TagKey<Item> tag;

    public ItemTagResourceGenerator(TagKey<Item> tag) {
        super(ResourceType.ITEM);
        this.tag = tag;
    }

    public TagKey<Item> getTag() {
        return tag;
    }

    @Override
    public Collection<ItemStack> getRandomEntries(LivingEntity entity, RandomSource random) {
        // validate server
        final MinecraftServer server = entity.getServer();
        if (null == server) {
            return ImmutableList.of();
        }
        // load item tag
        final ITag<Item> itag = ForgeRegistries.ITEMS.tags().getTag(this.getTag());
        // generate items
        final ImmutableList.Builder<ItemStack> builder = new ImmutableList.Builder<>();
        itag.getRandomElement(random).ifPresent(item -> builder.add(item.getDefaultInstance()));
        return builder.build();
    }

    @Override
    public Codec<? extends ResourceGenerator> getCodec() {
        return AxRegistry.ResourceGeneratorsReg.TAG.get();
    }

    @Override
    public List<ResourceDescriptionGroup> createDescription() {
        return ImmutableList.of(ResourceDescriptionGroup
                .builder()
                .ofTag(this.getTag())
        );
    }

    @Override
    public String toString() {
        return "ItemTag: " + getTag().location().toString();
    }
}
