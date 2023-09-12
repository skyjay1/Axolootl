/**
 * Copyright (c) 2023 Skyler James
 * Permission is granted to use, modify, and redistribute this software, in parts or in whole,
 * under the GNU LGPLv3 license (https://www.gnu.org/licenses/lgpl-3.0.en.html)
 **/

package axolootl.util;

import axolootl.Axolootl;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;

public enum BreedStatus implements StringRepresentable {
    /** Breeding is enabled for all entities **/
    ACTIVE("active", true),
    /** Breeding is enabled for non-mob generating entities only **/
    RESOURCE_MOB_ONLY("resource_mob_only", true),
    /** Breeding is enabled but not possible because there is no food **/
    MISSING_RESOURCES("missing_resources", true),
    /** Breeding is halted because there are too few entities **/
    MIN_COUNT("min_count", false),
    /** Breeding is halted because the tank is at max capacity **/
    MAX_COUNT("max_count", false),
    /** Breeding is halted for other reasons **/
    PAUSED("paused", false),
    /** Breeding is not enabled **/
    INACTIVE("inactive", false);

    public static final StringRepresentable.EnumCodec<BreedStatus> CODEC = StringRepresentable.fromEnum(BreedStatus::values);

    private final String name;
    private final boolean active;
    private final String descriptionKey;
    private final Component description;
    private final Component descriptionSubtext;

    BreedStatus(String name, boolean active) {
        this.name = name;
        this.active = active;
        this.descriptionKey = Axolootl.MODID + ".breed_status." + name;
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
