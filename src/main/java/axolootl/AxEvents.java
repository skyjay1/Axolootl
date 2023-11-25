/**
 * Copyright (c) 2023 Skyler James
 * Permission is granted to use, modify, and redistribute this software, in parts or in whole,
 * under the GNU LGPLv3 license (https://www.gnu.org/licenses/lgpl-3.0.en.html)
 **/

package axolootl;

import axolootl.block.AquariumGlassBlock;
import axolootl.block.BlockConverter;
import axolootl.command.AxolootlResearchCommand;
import axolootl.data.aquarium_modifier.AquariumModifier;
import axolootl.data.axolootl_variant.AxolootlVariant;
import axolootl.data.breeding.AxolootlBreeding;
import axolootl.data.resource_generator.ResourceGenerator;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.dispenser.DispenseItemBehavior;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TagsUpdatedEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class AxEvents {

    public static void register() {
        FMLJavaModLoadingContext.get().getModEventBus().register(ModHandler.class);
        MinecraftForge.EVENT_BUS.register(ForgeHandler.class);
    }

    public static final class ForgeHandler {

        @SubscribeEvent
        public static void onRegisterCommands(final RegisterCommandsEvent event) {
            AxolootlResearchCommand.register(event.getDispatcher());
        }

        @SubscribeEvent
        public static void onDatapackSync(final OnDatapackSyncEvent event) {
            final RegistryAccess registryAccess = event.getPlayerList().getServer().registryAccess();
            AxRegistry.refreshCaches(registryAccess);
            Axolootl.LOGGER.debug("Axolootl loaded " + (AxolootlVariant.getRegistry(registryAccess).size()) + " axolootl variants");
            Axolootl.LOGGER.debug("Axolootl loaded " + ResourceGenerator.getRegistry(registryAccess).size() + " resource generators");
            Axolootl.LOGGER.debug("Axolootl loaded " + AxolootlBreeding.getRegistry(registryAccess).size() + " axolootl breeding recipes");
            Axolootl.LOGGER.debug("Axolootl loaded " + AquariumModifier.getRegistry(registryAccess).size() + " aquarium modifiers");
        }

        @SubscribeEvent
        public static void onPlayerLoggedOut(final PlayerEvent.PlayerLoggedOutEvent event) {
            if(event.getEntity().level.isClientSide()) {
                AxRegistry.clearCaches();
            }
        }

        @SubscribeEvent(priority = EventPriority.HIGH)
        public static void onTagsUpdated(final TagsUpdatedEvent event) {
            // validate axolootl variants
            final RegistryAccess registryAccess = event.getRegistryAccess();
            AxRegistry.AxolootlVariantsReg.validate(registryAccess);
            Axolootl.LOGGER.debug("Axolootl validated " + (AxolootlVariant.getRegistry(registryAccess).size() - AxRegistry.AxolootlVariantsReg.getInvalidEntries().size()) + " axolootl variants");
        }

    }

    public static final class ModHandler {

        @SubscribeEvent
        public static void onCommonSetup(final FMLCommonSetupEvent event) {
            event.enqueueWork(ModHandler::registerDispenserBehavior);
            event.enqueueWork(ModHandler::registerAquariumGlassConverters);
        }

        private static void registerDispenserBehavior() {
            final DispenseItemBehavior axolotlBucketBehavior = DispenserBlock.DISPENSER_REGISTRY.get(Items.AXOLOTL_BUCKET);
            if(axolotlBucketBehavior != null) {
                DispenserBlock.registerBehavior(AxRegistry.ItemReg.AXOLOOTL_BUCKET.get(), axolotlBucketBehavior);
            }
        }

        private static void registerAquariumGlassConverters() {
            registerAquariumGlassConverter(AxRegistry.BlockReg.AQUARIUM_CONTROLLER);
            registerAquariumGlassConverter(AxRegistry.BlockReg.AQUARIUM_AXOLOOTL_INSPECTOR);
            registerAquariumGlassConverter(AxRegistry.BlockReg.AQUARIUM_AXOLOOTL_INTERFACE);
            registerAquariumGlassConverter(AxRegistry.BlockReg.AQUARIUM_ENERGY_INTERFACE);
            registerAquariumGlassConverter(AxRegistry.BlockReg.AQUARIUM_WATER_INTERFACE);
            registerAquariumGlassConverter(AxRegistry.BlockReg.AQUARIUM_OUTPUT);
            registerAquariumGlassConverter(AxRegistry.BlockReg.LARGE_AQUARIUM_OUTPUT);
        }

        private static void registerAquariumGlassConverter(final RegistryObject<Block> block) {
            AquariumGlassBlock.registerItemConverter(BlockConverter.itemConverter(RegistryObject.create(block.getId(), ForgeRegistries.ITEMS), block));
        }
    }
}
