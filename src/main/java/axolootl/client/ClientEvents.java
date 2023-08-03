/**
 * Copyright (c) 2023 Skyler James
 * Permission is granted to use, modify, and redistribute this software, in parts or in whole,
 * under the GNU LGPLv3 license (https://www.gnu.org/licenses/lgpl-3.0.en.html)
 **/

package axolootl.client;

import axolootl.AxRegistry;
import axolootl.Axolootl;
import axolootl.client.entity.AxolootlGeoRenderer;
import axolootl.client.item.AxolootlBucketItemSettings;
import axolootl.client.menu.AxolootlInspectorScreen;
import axolootl.client.menu.AxolootlInterfaceScreen;
import axolootl.client.menu.ControllerScreen;
import axolootl.client.menu.CyclingContainerScreen;
import axolootl.client.menu.EnergyInterfaceScreen;
import axolootl.client.menu.FluidInterfaceScreen;
import axolootl.data.axolootl_variant.AxolootlVariant;
import axolootl.item.AxolootlBucketItem;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class ClientEvents {

    public static void register() {
        FMLJavaModLoadingContext.get().getModEventBus().register(ModHandler.class);
        MinecraftForge.EVENT_BUS.register(ForgeHandler.class);
        ClientEvents.ModelHandler.register();
    }

    public static final class ModHandler {

        @SubscribeEvent
        public static void onCommonSetup(final FMLCommonSetupEvent event) {
            event.enqueueWork(ModHandler::onRegisterScreens);
        }

        @SubscribeEvent
        public static void onRegisterRenderers(final EntityRenderersEvent.RegisterRenderers event) {
            event.registerEntityRenderer(AxRegistry.EntityReg.AXOLOOTL.get(), AxolootlGeoRenderer::new);
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
    }

    public static final class ForgeHandler {

    }



    public static final class ModelHandler {

        public static void register() {
            FMLJavaModLoadingContext.get().getModEventBus().addListener(ModelHandler::onRegisterReloadListeners);
            FMLJavaModLoadingContext.get().getModEventBus().addListener(ModelHandler::onRegisterExtraModels);
        }

        /** The item model to use for axolootl bucket items when no other model is specified **/
        private static final ResourceLocation AXOLOOTL_BUCKET_ITEM_FALLBACK = new ResourceLocation(Axolootl.MODID, "item/axolootl_bucket_fallback");
        /** The instance with the merged values of all registered AxolootlBucketItemSettings **/
        private static AxolootlBucketItemSettings instance = AxolootlBucketItemSettings.EMPTY;
        private static final String PATH = "axolootl_bucket";

        public static AxolootlBucketItemSettings instance() {
            if (instance.isEmpty()) {
                instance = reload(Minecraft.getInstance().getResourceManager());
            }
            return instance;
        }

        /**
         * @param variant the axolootl variant ID
         * @return the model resource location for the given variant
         */
        public static ResourceLocation getModelForVariant(ResourceLocation variant) {
            return instance().getVariantToModelMap().getOrDefault(variant, AXOLOOTL_BUCKET_ITEM_FALLBACK);
        }

        private static void onRegisterReloadListeners(final RegisterClientReloadListenersEvent event) {
            event.registerReloadListener(new SimplePreparableReloadListener<AxolootlBucketItemSettings>() {
                @Override
                protected AxolootlBucketItemSettings prepare(ResourceManager pResourceManager, ProfilerFiller pProfiler) {
                    ClientEvents.ModelHandler.instance = AxolootlBucketItemSettings.EMPTY;
                    return ClientEvents.ModelHandler.reload(pResourceManager);
                }
                @Override
                protected void apply(AxolootlBucketItemSettings pObject, ResourceManager pResourceManager, ProfilerFiller pProfiler) {
                    ClientEvents.ModelHandler.instance = pObject;
                }
            });
        }

        private static void onRegisterExtraModels(final ModelEvent.RegisterAdditional event) {
            // register fallback model
            event.register(ModelHandler.AXOLOOTL_BUCKET_ITEM_FALLBACK);
            // iterate models and register each one
            for(ResourceLocation model : ModelHandler.instance().getVariantToModelMap().values()) {
                event.register(model);
            }
        }

        /**
         * @param manager the resource manager
         * @return the {@link AxolootlBucketItemSettings} loaded from resources, may be empty
         */
        private static AxolootlBucketItemSettings reload(ResourceManager manager) {
            Gson gson = new Gson();
            try {
                // locate the resource
                Map<ResourceLocation, Resource> resources = manager.listResources(PATH, id -> id.getPath().endsWith(".json"));
                if(resources.isEmpty()) {
                    Axolootl.LOGGER.error("Failed to locate AxolootlBucketItemSettings at " + PATH);
                }
                final List<AxolootlBucketItemSettings> results = new ArrayList<>();
                // iterate each resource
                for(Map.Entry<ResourceLocation, Resource> entry : resources.entrySet()) {
                    // open the file
                    try(Reader reader = entry.getValue().openAsReader()) {
                        // parse from JSON
                        JsonElement jsonElement = JsonParser.parseReader(reader);
                        AxolootlBucketItemSettings.CODEC.parse(JsonOps.INSTANCE, jsonElement)
                                .resultOrPartial(errorMsg -> Axolootl.LOGGER.error("Error deserializing json {} in folder {} from pack {}: {}", entry.getKey(), PATH, entry.getValue().sourcePackId(), errorMsg))
                                .ifPresent(results::add);
                    }
                    catch(Exception e) {
                        Axolootl.LOGGER.error(String.format(Locale.ENGLISH, "Error reading resource %s in folder %s from pack %s: ", entry.getKey(), PATH, entry.getValue().sourcePackId()), e);
                    }
                }
                // populate instance
                final AxolootlBucketItemSettings merged = AxolootlBucketItemSettings.merge(results);
                Axolootl.LOGGER.debug("Parsed AxolootlBucketItemSettings from file: " + merged.toString());
                return merged;
            } catch (Exception e) {
                e.printStackTrace();
            }
            return AxolootlBucketItemSettings.EMPTY;
        }
    }
}
