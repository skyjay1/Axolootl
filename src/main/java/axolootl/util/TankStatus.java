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

public enum TankStatus implements StringRepresentable {
    /** Tank is complete and actively generating resources **/
    ACTIVE("active", true),
    /** Tank is complete but has insufficient power **/
    LOW_ENERGY("low_energy", true),
    /** Tank is complete but missing required modifiers **/
    MISSING_MODIFIERS("missing_modifiers", false),
    /** Tank is complete but any of the generation, feed, or breed speed values are negative **/
    POOR_CONDITIONS("poor_conditions", false),
    /** Tank is complete but entity count exceeds capacity **/
    OVERCROWDED("overcrowded", false),
    /** Tank is complete but output storage is full **/
    STORAGE_FULL("storage_full", false),
    /** Tank is complete but a duplicate controller was found **/
    DUPLICATE_CONTROLLERS("duplicate_controllers", false),
    /** Tank border is incomplete or invalid **/
    INCOMPLETE("incomplete", false);

    public static final Map<String, TankStatus> NAME_TO_TYPE_MAP = ImmutableMap.copyOf(Arrays.stream(values())
            .collect(Collectors.<TankStatus, String, TankStatus>toMap(TankStatus::getSerializedName, Function.identity())));

    public static final Codec<TankStatus> CODEC = Codec.STRING.xmap(TankStatus::getByName, TankStatus::getSerializedName);

    private final String name;
    private final boolean active;
    private final String descriptionKey;
    private final Component description;
    private final Component descriptionSubtext;

    TankStatus(String name, boolean active) {
        this.name = name;
        this.active = active;
        this.descriptionKey = Axolootl.MODID + ".tank_status." + name;
        this.description = Component.translatable(descriptionKey);
        this.descriptionSubtext = Component.translatable(descriptionKey + ".description");
    }

    public static TankStatus getByName(final String name) {
        return NAME_TO_TYPE_MAP.getOrDefault(name, INCOMPLETE);
    }

    /**
     * @return true if the tank can generate resources and perform updates
     */
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
