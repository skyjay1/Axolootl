package axolootl.block.entity;

import axolootl.Axolootl;
import com.google.common.collect.ImmutableMap;
import com.mojang.serialization.Codec;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public enum FeedStatus implements StringRepresentable {
    /** Feeding is enabled and has sufficient resources **/
    ACTIVE("active", true),
    /** Feeding is halted due to insufficient resources **/
    MISSING_RESOURCES("missing_resources", false),
    /** Feeding is halted for other reasons **/
    PAUSED("paused", false),
    /** Feeding is not enabled **/
    INACTIVE("inactive", false);

    public static final Map<String, FeedStatus> NAME_TO_TYPE_MAP = ImmutableMap.copyOf(Arrays.stream(values())
            .collect(Collectors.<FeedStatus, String, FeedStatus>toMap(FeedStatus::getSerializedName, Function.identity())));

    public static final Codec<FeedStatus> CODEC = Codec.STRING.xmap(FeedStatus::getByName, FeedStatus::getSerializedName);

    private final String name;
    private final boolean active;
    private final Component description;

    FeedStatus(String name, boolean active) {
        this.name = name;
        this.active = active;
        this.description = Component.translatable(Axolootl.MODID + ".feed_status." + name);
    }

    public static FeedStatus getByName(final String name) {
        return NAME_TO_TYPE_MAP.getOrDefault(name, INACTIVE);
    }

    public boolean isActive() {
        return active;
    }

    public Component getDescription() {
        return description;
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }
}
