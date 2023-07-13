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

public enum BreedStatus implements StringRepresentable {
    /** Breeding is enabled for all entities **/
    ACTIVE("active", true),
    /** Breeding is enabled for non-mob generating entities only **/
    RESOURCE_MOB_ONLY("resource_mob_only", true),
    /** Breeding is halted because there are too few entities **/
    MIN_COUNT("min_count", false),
    /** Breeding is halted because the tank is at max capacity **/
    MAX_COUNT("max_count", false),
    /** Breeding is halted for other reasons **/
    PAUSED("paused", false),
    /** Breeding is not enabled **/
    INACTIVE("inactive", false);

    public static final Map<String, BreedStatus> NAME_TO_TYPE_MAP = ImmutableMap.copyOf(Arrays.stream(values())
            .collect(Collectors.<BreedStatus, String, BreedStatus>toMap(BreedStatus::getSerializedName, Function.identity())));

    public static final Codec<BreedStatus> CODEC = Codec.STRING.xmap(BreedStatus::getByName, BreedStatus::getSerializedName);

    private final String name;
    private final boolean active;
    private final Component description;

    BreedStatus(String name, boolean active) {
        this.name = name;
        this.active = active;
        this.description = Component.translatable(Axolootl.MODID + ".breed_status." + name);
    }

    public static BreedStatus getByName(final String name) {
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
