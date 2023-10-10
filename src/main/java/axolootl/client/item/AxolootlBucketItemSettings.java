/**
 * Copyright (c) 2023 Skyler James
 * Permission is granted to use, modify, and redistribute this software, in parts or in whole,
 * under the GNU LGPLv3 license (https://www.gnu.org/licenses/lgpl-3.0.en.html)
 **/

package axolootl.client.item;

import com.mojang.serialization.Codec;
import net.minecraft.resources.ResourceLocation;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class AxolootlBucketItemSettings {

    public static final Codec<AxolootlBucketItemSettings> CODEC = Codec.unboundedMap(ResourceLocation.CODEC, ResourceLocation.CODEC)
            .xmap(AxolootlBucketItemSettings::new, AxolootlBucketItemSettings::getVariantToModelMap).fieldOf("values").codec();

    private boolean isLoaded;
    private final Map<ResourceLocation, ResourceLocation> variantToModelMap;
    private final Map<ResourceLocation, ResourceLocation> variantToModelMapView;

    public AxolootlBucketItemSettings() {
        this(Map.of());
        this.isLoaded = false;
    }

    public AxolootlBucketItemSettings(Map<ResourceLocation, ResourceLocation> variantToModelMap) {
        this.variantToModelMap = new HashMap<>(variantToModelMap);
        this.variantToModelMapView = Collections.unmodifiableMap(this.variantToModelMap);
    }

    public AxolootlBucketItemSettings merge(final AxolootlBucketItemSettings other) {
        if(!other.isEmpty()) {
            this.putAll(other.getVariantToModelMap());
        }
        return this;
    }

    //// METHODS ////

    public void clear() {
        this.variantToModelMap.clear();
        this.isLoaded = false;
    }

    public void put(final ResourceLocation variant, final ResourceLocation model) {
        this.variantToModelMap.put(variant, model);
    }

    public void putAll(final Map<ResourceLocation, ResourceLocation> map) {
        this.variantToModelMap.putAll(map);
    }

    public boolean isEmpty() {
        return this.variantToModelMap.isEmpty();
    }

    public boolean isLoaded() {
        return this.isLoaded;
    }

    public void setLoaded() {
        this.isLoaded = true;
    }

    //// GETTERS ////

    /**
     * @return an unmodifiable view of the variant to model map
     */
    public Map<ResourceLocation, ResourceLocation> getVariantToModelMap() {
        return variantToModelMapView;
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
