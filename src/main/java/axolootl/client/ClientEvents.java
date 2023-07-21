package axolootl.client;

import axolootl.AxRegistry;
import axolootl.client.entity.AxolootlRenderer;
import axolootl.client.menu.ControllerScreen;
import axolootl.client.menu.CyclingInventoryScreen;
import axolootl.data.axolootl_variant.AxolootlVariant;
import axolootl.item.AxolootlBucketItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

public final class ClientEvents {

    public static void register() {
        FMLJavaModLoadingContext.get().getModEventBus().register(ModHandler.class);
        MinecraftForge.EVENT_BUS.register(ForgeHandler.class);
    }

    public static final class ModHandler {
        @SubscribeEvent
        public static void onRegisterRenderers(final EntityRenderersEvent.RegisterRenderers event) {
            event.registerEntityRenderer(AxRegistry.EntityReg.AXOLOOTL.get(), AxolootlRenderer::new);
        }

        @SubscribeEvent
        public static void onCommonSetup(final FMLCommonSetupEvent event) {
            event.enqueueWork(ModHandler::onRegisterScreens);
        }

        @SubscribeEvent
        public static void onRegisterItemColors(final RegisterColorHandlersEvent.Item event) {
            event.register((pStack, pTintIndex) -> {
                // do not color layer 0
                if(pTintIndex == 0) {
                    return -1;
                }
                // validate level
                Level level = Minecraft.getInstance().level;
                if(null == level) {
                    return -1;
                }
                // load variant
                AxolootlVariant variant = AxolootlBucketItem.getVariant(level.registryAccess(), pStack).orElse(AxolootlVariant.EMPTY);
                // colors
                if(pTintIndex == 1) {
                    return variant.getPrimaryColor();
                }
                return variant.getSecondaryColor();
            }, AxRegistry.ItemReg.AXOLOOTL_BUCKET.get());
        }

        private static void onRegisterScreens() {
            MenuScreens.register(AxRegistry.MenuReg.CONTROLLER.get(), ControllerScreen::new);
            MenuScreens.register(AxRegistry.MenuReg.OUTPUT.get(), CyclingInventoryScreen::new);
            MenuScreens.register(AxRegistry.MenuReg.LARGE_OUTPUT.get(), CyclingInventoryScreen::new);
            MenuScreens.register(AxRegistry.MenuReg.AUTOFEEDER.get(), CyclingInventoryScreen::new);
            MenuScreens.register(AxRegistry.MenuReg.BREEDER.get(), CyclingInventoryScreen::new);
            MenuScreens.register(AxRegistry.MenuReg.MONSTERIUM.get(), CyclingInventoryScreen::new);
        }
    }

    public static final class ForgeHandler {

    }
}
