package axolootl;

import com.mojang.logging.LogUtils;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;
import axolootl.client.ClientEvents;

@Mod(Axolootl.MODID)
public class Axolootl {

    public static final String MODID = "axolootl";

    public static final Logger LOGGER = LogUtils.getLogger();

    public Axolootl() {
        AxRegistry.register();
        AxEvents.register();
        // client events
        DistExecutor.safeRunWhenOn(Dist.CLIENT, () -> ClientEvents::register);
    }


}
