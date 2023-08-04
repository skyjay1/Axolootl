/**
 * Copyright (c) 2023 Skyler James
 * Permission is granted to use, modify, and redistribute this software, in parts or in whole,
 * under the GNU LGPLv3 license (https://www.gnu.org/licenses/lgpl-3.0.en.html)
 **/

package axolootl.network;

import axolootl.Axolootl;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Optional;

public final class AxNetwork {
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(new ResourceLocation(Axolootl.MODID, "channel"), () -> PROTOCOL_VERSION, PROTOCOL_VERSION::equals, PROTOCOL_VERSION::equals);

    public static void register() {
        int messageId = 0;
        CHANNEL.registerMessage(messageId++, ServerBoundActivateControllerPacket.class, ServerBoundActivateControllerPacket::toBytes, ServerBoundActivateControllerPacket::fromBytes, ServerBoundActivateControllerPacket::handlePacket, Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(messageId++, ServerBoundControllerTabPacket.class, ServerBoundControllerTabPacket::toBytes, ServerBoundControllerTabPacket::fromBytes, ServerBoundControllerTabPacket::handlePacket, Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(messageId++, ServerBoundControllerCyclePacket.class, ServerBoundControllerCyclePacket::toBytes, ServerBoundControllerCyclePacket::fromBytes, ServerBoundControllerCyclePacket::handlePacket, Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(messageId++, ServerBoundExtractAxolootlPacket.class, ServerBoundExtractAxolootlPacket::toBytes, ServerBoundExtractAxolootlPacket::fromBytes, ServerBoundExtractAxolootlPacket::handlePacket, Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(messageId++, ServerBoundInsertAxolootlPacket.class, ServerBoundInsertAxolootlPacket::toBytes, ServerBoundInsertAxolootlPacket::fromBytes, ServerBoundInsertAxolootlPacket::handlePacket, Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(messageId++, ClientBoundSyncAxolootlResearchCapabilityPacket.class, ClientBoundSyncAxolootlResearchCapabilityPacket::toBytes, ClientBoundSyncAxolootlResearchCapabilityPacket::fromBytes, ClientBoundSyncAxolootlResearchCapabilityPacket::handlePacket, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    }
}
