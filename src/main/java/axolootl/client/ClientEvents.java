/**
 * Copyright (c) 2023 Skyler James
 * Permission is granted to use, modify, and redistribute this software, in parts or in whole,
 * under the GNU LGPLv3 license (https://www.gnu.org/licenses/lgpl-3.0.en.html)
 **/

package axolootl.client;

import axolootl.AxRegistry;
import axolootl.Axolootl;
import axolootl.block.GrandCastleMultiBlock;
import axolootl.client.blockentity.AutofeederBlockEntityRenderer;
import axolootl.client.entity.AxolootlGeoRenderer;
import axolootl.client.menu.AxolootlInspectorScreen;
import axolootl.client.menu.AxolootlInterfaceScreen;
import axolootl.client.menu.ControllerScreen;
import axolootl.client.menu.CyclingContainerScreen;
import axolootl.client.menu.EnergyInterfaceScreen;
import axolootl.client.menu.FluidInterfaceScreen;
import axolootl.data.axolootl_variant.AxolootlVariant;
import axolootl.item.AxolootlBucketItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ClientEvents {

    public static void register() {
        FMLJavaModLoadingContext.get().getModEventBus().register(ModHandler.class);
        MinecraftForge.EVENT_BUS.register(ForgeHandler.class);
        AxolootlBucketItemModelLoader.register();
    }

    public static final class ModHandler {

        @SubscribeEvent
        public static void onCommonSetup(final FMLCommonSetupEvent event) {
            event.enqueueWork(ModHandler::onRegisterScreens);
            event.enqueueWork(ModHandler::onRegisterItemPropertyOverrides);
        }

        @SubscribeEvent
        public static void onRegisterRenderers(final EntityRenderersEvent.RegisterRenderers event) {
            event.registerEntityRenderer(AxRegistry.EntityReg.AXOLOOTL.get(), AxolootlGeoRenderer::new);
            event.registerBlockEntityRenderer(AxRegistry.BlockEntityReg.AUTO_FEEDER.get(), AutofeederBlockEntityRenderer::new);
        }

        @SubscribeEvent
        public static void onRegisterItemColors(final RegisterColorHandlersEvent.Item event) {
            event.register((pStack, pTintIndex) -> {
                // do not color layer 0
                if(pTintIndex < 1 || pTintIndex > 2) {
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
                    return variant.getModelSettings().getPrimaryColor();
                }
                return variant.getModelSettings().getSecondaryColor();
            }, AxRegistry.ItemReg.AXOLOOTL_BUCKET.get());
        }

        private static void onRegisterScreens() {
            MenuScreens.register(AxRegistry.MenuReg.CONTROLLER.get(), ControllerScreen::new);
            MenuScreens.register(AxRegistry.MenuReg.AXOLOOTL.get(), AxolootlInterfaceScreen::new);
            MenuScreens.register(AxRegistry.MenuReg.INSPECTOR.get(), AxolootlInspectorScreen::new);
            MenuScreens.register(AxRegistry.MenuReg.OUTPUT.get(), CyclingContainerScreen::new);
            MenuScreens.register(AxRegistry.MenuReg.LARGE_OUTPUT.get(), CyclingContainerScreen::new);
            MenuScreens.register(AxRegistry.MenuReg.AUTOFEEDER.get(), CyclingContainerScreen::new);
            MenuScreens.register(AxRegistry.MenuReg.BREEDER.get(), CyclingContainerScreen::new);
            MenuScreens.register(AxRegistry.MenuReg.MONSTERIUM.get(), CyclingContainerScreen::new);
            MenuScreens.register(AxRegistry.MenuReg.ENERGY.get(), EnergyInterfaceScreen::new);
            MenuScreens.register(AxRegistry.MenuReg.FLUID.get(), FluidInterfaceScreen::new);
        }

        private static void onRegisterItemPropertyOverrides() {
            // Grand Castle
            ItemProperties.register(
                    RegistryObject.create(new ResourceLocation(Axolootl.MODID, "grand_castle"), ForgeRegistries.ITEMS).get(),
                    new ResourceLocation(Axolootl.MODID, "enchantment"),
                    (pStack, pLevel, pEntity, pSeed) -> {
                        return (float) pStack.getEnchantmentLevel(GrandCastleMultiBlock.ENCHANTMENT) / (float) GrandCastleMultiBlock.MAX_ENCHANTMENT_LEVEL;
                    });
            // Axolootl Bucket
            ItemProperties.register(AxRegistry.ItemReg.AXOLOOTL_BUCKET.get(), new ResourceLocation(Axolootl.MODID, "baby"),
                    ((pStack, pLevel, pEntity, pSeed) -> AxolootlBucketItem.isBaby(pStack) ? 1.0F : 0.0F)
            );
        }
    }

    public static final class ForgeHandler {

    }
}
