package axolootl.client.item;

import com.google.common.collect.ImmutableMap;
import com.mojang.serialization.Codec;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.concurrent.Immutable;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Immutable
public class AxolootlBucketItemSettings {

    public static final AxolootlBucketItemSettings EMPTY = new AxolootlBucketItemSettings(Map.of());

    public static final Codec<AxolootlBucketItemSettings> CODEC = Codec.unboundedMap(ResourceLocation.CODEC, ResourceLocation.CODEC)
            .xmap(AxolootlBucketItemSettings::new, AxolootlBucketItemSettings::getVariantToModelMap).fieldOf("values").codec();

    private final Map<ResourceLocation, ResourceLocation> variantToModelMap;

    public AxolootlBucketItemSettings(Map<ResourceLocation, ResourceLocation> variantToModelMap) {
        this.variantToModelMap = ImmutableMap.copyOf(variantToModelMap);
    }

    public static AxolootlBucketItemSettings merge(final List<AxolootlBucketItemSettings> list) {
        final ImmutableMap.Builder<ResourceLocation, ResourceLocation> builder = ImmutableMap.builder();
        list.forEach(e -> builder.putAll(e.getVariantToModelMap()));
        return new AxolootlBucketItemSettings(builder.build());
    }

    //// METHODS ////

    public boolean isEmpty() {
        return this.variantToModelMap.isEmpty();
    }

    //// GETTERS ////

    /**
     * @return an unmodifiable view of the variant to model map
     */
    public Map<ResourceLocation, ResourceLocation> getVariantToModelMap() {
        return variantToModelMap;
    }

    //// EQUALITY ////

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AxolootlBucketItemSettings)) return false;
        AxolootlBucketItemSettings that = (AxolootlBucketItemSettings) o;
        return variantToModelMap.equals(that.variantToModelMap);
    }

    @Override
    public int hashCode() {
        return Objects.hash(variantToModelMap);
    }

    @Override
    public String toString() {
        return variantToModelMap.entrySet().toString();
    }
}
