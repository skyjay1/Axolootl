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
        CHANNEL.registerMessage(messageId++, ServerBoundControllerTabPacket.class, ServerBoundControllerTabPacket::toBytes, ServerBoundControllerTabPacket::fromBytes, ServerBoundControllerTabPacket::handlePacket, Optional.of(NetworkDirection.PLAY_TO_SERVER));
    }
}
