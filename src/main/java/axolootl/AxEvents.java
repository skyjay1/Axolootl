package axolootl;

import axolootl.data.aquarium_modifier.AquariumModifier;
import axolootl.data.AxolootlVariant;
import axolootl.data.aquarium_modifier.condition.ModifierCondition;
import axolootl.data.resource_generator.ResourceGenerator;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
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
            // TODO debug
            if(event.getEntity().level instanceof ServerLevel serverLevel) {
                Axolootl.LOGGER.debug(serverLevel.registryAccess().registryOrThrow(AxRegistry.Keys.RESOURCE_GENERATOR_SERIALIZERS).size() + " resource generator serializers");
                Axolootl.LOGGER.debug(ResourceGenerator.getRegistry(serverLevel.registryAccess()).size() + " resource generators");
                Axolootl.LOGGER.debug(AxolootlVariant.getRegistry(serverLevel.registryAccess()).size() + " axolootl variants");
                Axolootl.LOGGER.debug(serverLevel.registryAccess().registryOrThrow(AxRegistry.Keys.MODIFIER_CONDITION_SERIALIZERS).size() + " modifier condition serializers");
                Axolootl.LOGGER.debug(ModifierCondition.getRegistry(serverLevel.registryAccess()).size() + " modifier conditions");
                Axolootl.LOGGER.debug(AquariumModifier.getRegistry(serverLevel.registryAccess()).size() + " modifiers");
            }
            // TODO remove for release
            event.getEntity().displayClientMessage(Component.literal("You are using a beta version of Axolootl, do not distribute").withStyle(ChatFormatting.AQUA), false);
        }
    }

    public static final class ModHandler {

    }
}
