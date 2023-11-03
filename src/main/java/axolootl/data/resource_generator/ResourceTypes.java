/**
 * Copyright (c) 2023 Skyler James
 * Permission is granted to use, modify, and redistribute this software, in parts or in whole,
 * under the GNU LGPLv3 license (https://www.gnu.org/licenses/lgpl-3.0.en.html)
 **/

package axolootl.data.resource_generator;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public enum ResourceTypes implements ResourceType {
    /** Indicates resources that are simple items **/
    ITEM("item"),
    /** Indicates resources that come from mobs **/
    MOB("mob"),
    /** Indicates resources that come from blocks **/
    BLOCK("block"),
    /** Indicates no resources **/
    EMPTY("empty"),
    /** Indicates resources that come from multiple sources **/
    MULTIPLE("multiple"),
    /** Indicates a redirect to some other resource generator **/
    REFERENCE("reference");

    private static final Map<String, ResourceType> RESOURCE_TYPES = new HashMap<>();
    private static final Map<String, ResourceType> RESOURCE_TYPES_VIEW = Collections.unmodifiableMap(RESOURCE_TYPES);
    public static final Codec<ResourceType> CODEC = Codec.STRING
            .flatXmap(ResourceTypes::parse, ResourceTypes::parse)
            .xmap(ResourceTypes::getByName, ResourceType::getSerializedName);

    public static void bootstrap() {
        // register built-in values
        for(ResourceType type : ResourceTypes.values()) {
            ResourceTypes.register(type);
        }
    }

    /**
     * @param resourceType the resource type to register
     * @return the resource type that was registered
     */
    public static ResourceType register(final ResourceType resourceType) {
        if(RESOURCE_TYPES.containsKey(resourceType.getSerializedName())) {
            throw new IllegalArgumentException("ResourceType failed to register with duplicate key \"" + resourceType.getSerializedName() + "\"");
        }
        RESOURCE_TYPES.put(resourceType.getSerializedName(), resourceType);
        return resourceType;
    }

    /**
     * @return an unmodifiable view of the registered resource types
     */
    public static Map<String, ResourceType> getResourceTypesView() {
        return RESOURCE_TYPES_VIEW;
    }

    private final String name;

    ResourceTypes(String name) {
        this.name = name;
    }

    @Nullable
    public static ResourceType getByName(final String name) {
        return RESOURCE_TYPES.get(name);
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }

    private static DataResult<String> parse(final String value) {
        if(!RESOURCE_TYPES.containsKey(value)) {
            return DataResult.error("Unknown resource type \"" + value + "\"");
        }
        return DataResult.success(value);
    }
}
