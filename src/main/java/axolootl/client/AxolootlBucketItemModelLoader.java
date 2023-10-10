/**
 * Copyright (c) 2023 Skyler James
 * Permission is granted to use, modify, and redistribute this software, in parts or in whole,
 * under the GNU LGPLv3 license (https://www.gnu.org/licenses/lgpl-3.0.en.html)
 **/

package axolootl.client;

import axolootl.Axolootl;
import axolootl.client.item.AxolootlBucketItemSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import java.util.Map;

public class AxolootlBucketItemModelLoader {

    public static void register() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(AxolootlBucketItemModelLoader::onRegisterReloadListeners);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(AxolootlBucketItemModelLoader::onRegisterExtraModels);
    }

    /** The item model to use for axolootl bucket items when no other model is specified **/
    private static final ResourceLocation AXOLOOTL_BUCKET_ITEM_FALLBACK = new ResourceLocation(Axolootl.MODID, "item/axolootl_bucket_template");
    /** The instance with the merged values of all registered AxolootlBucketItemSettings **/
    private static AxolootlBucketItemSettings instance = new AxolootlBucketItemSettings();
    private static final String PATH = "models/item/axolootl_bucket/";

    public static AxolootlBucketItemSettings instance() {
        if (!instance.isLoaded()) {
            instance.merge(reload(Minecraft.getInstance().getResourceManager()));
            instance.setLoaded();
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
                return AxolootlBucketItemModelLoader.reload(pResourceManager);
            }
            @Override
            protected void apply(AxolootlBucketItemSettings pObject, ResourceManager pResourceManager, ProfilerFiller pProfiler) {
                instance.clear();
                instance.merge(pObject);
                instance.setLoaded();
            }
        });
    }

    private static void onRegisterExtraModels(final ModelEvent.RegisterAdditional event) {
        // register fallback model
        event.register(AXOLOOTL_BUCKET_ITEM_FALLBACK);
        // iterate models and register each one
        for(ResourceLocation model : instance().getVariantToModelMap().values()) {
            event.register(model);
        }
    }

    /**
     * @param manager the resource manager
     * @return the {@link AxolootlBucketItemSettings} loaded from resources, may be empty
     */
    private static AxolootlBucketItemSettings reload(ResourceManager manager) {
        AxolootlBucketItemSettings settings = new AxolootlBucketItemSettings();
        try {
            // locate all resources in the given folder
            Map<ResourceLocation, Resource> resources = manager.listResources(PATH, id -> id.getPath().endsWith(".json"));
            if(resources.isEmpty()) {
                Axolootl.LOGGER.warn("No axolootl bucket item settings found at " + PATH);
            }
            // iterate each resource
            for(ResourceLocation entry : resources.keySet()) {
                ResourceLocation variantId = new ResourceLocation(entry.getNamespace(), entry.getPath().replaceFirst(PATH, "").replaceFirst("\\.json", ""));
                ResourceLocation modelId = new ResourceLocation(entry.getNamespace(), entry.getPath().replaceFirst("models/", "").replaceFirst("\\.json", ""));
                settings.put(variantId, modelId);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return settings;
    }
}
