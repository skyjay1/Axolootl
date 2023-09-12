/**
 * Copyright (c) 2023 Skyler James
 * Permission is granted to use, modify, and redistribute this software, in parts or in whole,
 * under the GNU LGPLv3 license (https://www.gnu.org/licenses/lgpl-3.0.en.html)
 **/

package axolootl.util;

import axolootl.Axolootl;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;

public enum FeedStatus implements StringRepresentable {
    /** Feeding is enabled and has sufficient resources **/
    ACTIVE("active", true),
    /** Feeding is enabled but not possible because there is no food **/
    MISSING_RESOURCES("missing_resources", true),
    /** Feeding is halted for other reasons **/
    PAUSED("paused", false),
    /** Feeding is not enabled **/
    INACTIVE("inactive", false);

    public static final StringRepresentable.EnumCodec<FeedStatus> CODEC = StringRepresentable.fromEnum(FeedStatus::values);

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
