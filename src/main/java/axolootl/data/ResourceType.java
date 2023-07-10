package axolootl.data;

import com.google.common.collect.ImmutableMap;
import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public enum ResourceType implements StringRepresentable {
    RESOURCE("resource"), MOB("mob");

    public static final Map<String, ResourceType> NAME_TO_TYPE_MAP = ImmutableMap.copyOf(Arrays.stream(values())
            .collect(Collectors.<ResourceType, String, ResourceType>toMap(ResourceType::getSerializedName, Function.identity())));

    public static final Codec<ResourceType> CODEC = Codec.STRING.xmap(ResourceType::getByName, ResourceType::getSerializedName);

    private final String name;

    ResourceType(String name) {
        this.name = name;
    }

    public static ResourceType getByName(final String name) {
        return NAME_TO_TYPE_MAP.getOrDefault(name, RESOURCE);
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }
}
