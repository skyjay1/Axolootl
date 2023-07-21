/**
 * Copyright (c) 2023 Skyler James
 * Permission is granted to use, modify, and redistribute this software, in parts or in whole,
 * under the GNU LGPLv3 license (https://www.gnu.org/licenses/lgpl-3.0.en.html)
 **/

package axolootl.data.resource_generator;

import com.google.common.collect.ImmutableMap;
import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public enum ResourceType implements StringRepresentable {
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
    /** Indicates some other resource **/
    OTHER("other");

    public static final Map<String, ResourceType> NAME_TO_TYPE_MAP = ImmutableMap.copyOf(Arrays.stream(values())
            .collect(Collectors.<ResourceType, String, ResourceType>toMap(ResourceType::getSerializedName, Function.identity())));

    public static final Codec<ResourceType> CODEC = Codec.STRING.xmap(ResourceType::getByName, ResourceType::getSerializedName);

    private final String name;

    ResourceType(String name) {
        this.name = name;
    }

    public static ResourceType getByName(final String name) {
        return NAME_TO_TYPE_MAP.getOrDefault(name, ITEM);
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }
}
