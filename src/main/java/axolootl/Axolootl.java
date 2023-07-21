/**
 * Copyright (c) 2023 Skyler James
 * Permission is granted to use, modify, and redistribute this software, in parts or in whole,
 * under the GNU LGPLv3 license (https://www.gnu.org/licenses/lgpl-3.0.en.html)
 **/

package axolootl;

import axolootl.network.AxNetwork;
import com.mojang.logging.LogUtils;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import org.slf4j.Logger;
import axolootl.client.ClientEvents;

@Mod(Axolootl.MODID)
public class Axolootl {

    public static final String MODID = "axolootl";

    public static final Logger LOGGER = LogUtils.getLogger();

    private static final ForgeConfigSpec.Builder CONFIG_BUILDER = new ForgeConfigSpec.Builder();
    public static final AxConfig CONFIG = new AxConfig(CONFIG_BUILDER);
    private static final ForgeConfigSpec CONFIG_SPEC = CONFIG_BUILDER.build();

    public Axolootl() {
        // register config
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, CONFIG_SPEC);
        // register registry objects
        AxRegistry.register();
        // register network
        AxNetwork.register();
        // register event handlers
        AxEvents.register();
        // register client events
        DistExecutor.safeRunWhenOn(Dist.CLIENT, () -> ClientEvents::register);
    }


}
