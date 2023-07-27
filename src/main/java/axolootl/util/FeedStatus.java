/**
 * Copyright (c) 2023 Skyler James
 * Permission is granted to use, modify, and redistribute this software, in parts or in whole,
 * under the GNU LGPLv3 license (https://www.gnu.org/licenses/lgpl-3.0.en.html)
 **/

package axolootl.util;

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
    /** Feeding is enabled but not possible because there is no food **/
    MISSING_RESOURCES("missing_resources", true),
    /** Feeding is halted for other reasons **/
    PAUSED("paused", false),
    /** Feeding is not enabled **/
    INACTIVE("inactive", false);

    public static final Map<String, FeedStatus> NAME_TO_TYPE_MAP = ImmutableMap.copyOf(Arrays.stream(values())
            .collect(Collectors.<FeedStatus, String, FeedStatus>toMap(FeedStatus::getSerializedName, Function.identity())));

    public static final Codec<FeedStatus> CODEC = Codec.STRING.xmap(FeedStatus::getByName, FeedStatus::getSerializedName);

    private final String name;
    private final boolean active;
    private final String descriptionKey;
    private final Component description;
    private final Component descriptionSubtext;

    FeedStatus(String name, boolean active) {
        this.name = name;
        this.active = active;
        this.descriptionKey = Axolootl.MODID + ".feed_status." + name;
        this.description = Component.translatable(descriptionKey);
        this.descriptionSubtext = Component.translatable(descriptionKey + ".description");
    }

    public static FeedStatus getByName(final String name) {
        return NAME_TO_TYPE_MAP.getOrDefault(name, INACTIVE);
    }

    public boolean isActive() {
        return active;
    }

    public String getTranslationKey() {
        return descriptionKey;
    }

    public Component getDescription() {
        return description;
    }

    public Component getDescriptionSubtext() {
        return descriptionSubtext;
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }
}
