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
    private final Component description;

    TankStatus(String name, boolean active) {
        this.name = name;
        this.active = active;
        this.description = Component.translatable(Axolootl.MODID + ".tank_status." + name);
    }

    public static TankStatus getByName(final String name) {
        return NAME_TO_TYPE_MAP.getOrDefault(name, INCOMPLETE);
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
