package axolootl.data.axolootl_variant.condition;

import axolootl.AxRegistry;
import com.mojang.serialization.Codec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Set;

public class TagEmptyForgeCondition extends ForgeCondition {

    public static final Codec<TagEmptyForgeCondition> CODEC = ResourceLocation.CODEC
            .xmap(TagEmptyForgeCondition::new, TagEmptyForgeCondition::getTag).fieldOf("tag").codec();

    private final ResourceLocation tag;
    private final TagKey<Item> tagKey;

    public TagEmptyForgeCondition(ResourceLocation tag) {
        this.tag = tag;
        this.tagKey = ForgeRegistries.ITEMS.tags().createOptionalTagKey(tag, Set.of());
    }

    public ResourceLocation getTag() {
        return tag;
    }

    @Override
    public boolean test(ForgeConditionContext context) {
        return ForgeRegistries.ITEMS.tags().getTag(tagKey).isEmpty();
    }

    @Override
    public Codec<? extends ForgeCondition> getCodec() {
        return AxRegistry.ForgeConditionsReg.TAG_EMPTY.get();
    }
}
