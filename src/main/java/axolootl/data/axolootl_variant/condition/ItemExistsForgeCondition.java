/**
 * Copyright (c) 2023 Skyler James
 * Permission is granted to use, modify, and redistribute this software, in parts or in whole,
 * under the GNU LGPLv3 license (https://www.gnu.org/licenses/lgpl-3.0.en.html)
 **/

package axolootl.data.axolootl_variant.condition;

import axolootl.AxRegistry;
import com.mojang.serialization.Codec;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;

public class ItemExistsForgeCondition extends ForgeCondition {

    public static final Codec<ItemExistsForgeCondition> CODEC = ResourceLocation.CODEC
            .xmap(ItemExistsForgeCondition::new, ItemExistsForgeCondition::getItem).fieldOf("item").codec();

    private final ResourceLocation item;

    public ItemExistsForgeCondition(ResourceLocation item) {
        this.item = item;
    }

    public ResourceLocation getItem() {
        return item;
    }

    @Override
    public boolean test(ForgeConditionContext context) {
        return ForgeRegistries.ITEMS.getValue(getItem()) != null;
    }

    @Override
    public Codec<? extends ForgeCondition> getCodec() {
        return AxRegistry.ForgeConditionsReg.ITEM_EXISTS.get();
    }
}
