/**
 * Copyright (c) 2023 Skyler James
 * Permission is granted to use, modify, and redistribute this software, in parts or in whole,
 * under the GNU LGPLv3 license (https://www.gnu.org/licenses/lgpl-3.0.en.html)
 **/

package axolootl;

import net.minecraftforge.common.ForgeConfigSpec;

public class AxConfig {

    public final ForgeConfigSpec.IntValue TANK_MULTIBLOCK_UPDATE_CAP;
    public final ForgeConfigSpec.DoubleValue TANK_CAPACITY_VOLUME_FACTOR;
    public final ForgeConfigSpec.LongValue BASE_GENERATION_PERIOD;
    public final ForgeConfigSpec.LongValue BASE_BREEDING_PERIOD;
    public final ForgeConfigSpec.LongValue BASE_FEEDING_PERIOD;

    public AxConfig(ForgeConfigSpec.Builder builder) {
        builder.push("options");
        TANK_MULTIBLOCK_UPDATE_CAP = builder
                .comment("The maximum number of blocks for a single multiblock tank to validate each tick")
                .defineInRange("tank_multiblock_update_cap", 36, 2, 256);
        TANK_CAPACITY_VOLUME_FACTOR = builder
                .comment("The minimum cubic volume per axolootl, used to determine the maximum capacity")
                .defineInRange("tank_capacity_volume_factor", 15.0D, 0.0D, 512.0D);
        BASE_GENERATION_PERIOD = builder
                .comment("The base number of ticks in a resource generation cycle")
                .defineInRange("base_generation_period", 8000L, 1L, 96000L);
        BASE_BREEDING_PERIOD = builder
                .comment("The base number of ticks in a mob breeding cycle")
                .defineInRange("base_breeding_period", 8000L, 1L, 96000L);
        BASE_FEEDING_PERIOD = builder
                .comment("The base number of ticks in a mob feeding cycle")
                .defineInRange("base_feeding_period", 6000L, 1L, 96000L);
        builder.pop();
    }
}
