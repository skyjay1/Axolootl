package axolootl;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

public final class AxEvents {

    public static void register() {
        FMLJavaModLoadingContext.get().getModEventBus().register(ModHandler.class);
        MinecraftForge.EVENT_BUS.register(ForgeHandler.class);
    }

    public static final class ForgeHandler {

        @SubscribeEvent
        public static void onPlayerLogin(final PlayerEvent.PlayerLoggedInEvent event) {
            // remove for release
            event.getEntity().displayClientMessage(Component.literal("You are using a beta version of Axolootl, do not distribute").withStyle(ChatFormatting.AQUA), false);
        }
    }

    public static final class ModHandler {

    }
}
