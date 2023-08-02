/**
 * Copyright (c) 2023 Skyler James
 * Permission is granted to use, modify, and redistribute this software, in parts or in whole,
 * under the GNU LGPLv3 license (https://www.gnu.org/licenses/lgpl-3.0.en.html)
 **/

package axolootl.network;

import axolootl.Axolootl;
import axolootl.capability.AxolootlResearchCapability;
import axolootl.client.ClientUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.network.NetworkEvent;

import java.util.Optional;
import java.util.function.Supplier;

public class ClientBoundSyncAxolootlResearchCapabilityPacket {

    private static final String KEY_CAPABILITY = "Capability";

    private CompoundTag tag;

    public ClientBoundSyncAxolootlResearchCapabilityPacket(final AxolootlResearchCapability capability) {
        this(capability.serializeNBT());
    }

    private ClientBoundSyncAxolootlResearchCapabilityPacket(final CompoundTag tag) {
        this.tag = tag;
    }

    private ClientBoundSyncAxolootlResearchCapabilityPacket(final ListTag listTag) {
        this.tag = new CompoundTag();
        this.tag.put(KEY_CAPABILITY, listTag);
    }

    public ListTag getListTag() {
        return this.tag.getList(KEY_CAPABILITY, Tag.TAG_STRING);
    }

    /**
     * Reads the raw packet data from the data stream.
     *
     * @param buf the PacketBuffer
     * @return a new instance of the packet based on the PacketBuffer
     */
    public static ClientBoundSyncAxolootlResearchCapabilityPacket fromBytes(final FriendlyByteBuf buf) {
        CompoundTag tag = buf.readNbt();
        return new ClientBoundSyncAxolootlResearchCapabilityPacket(tag);
    }

    /**
     * Writes the raw packet data to the data stream.
     *
     * @param msg the packet
     * @param buf the PacketBuffer
     */
    public static void toBytes(final ClientBoundSyncAxolootlResearchCapabilityPacket msg, final FriendlyByteBuf buf) {
        buf.writeNbt(msg.tag);
    }

    /**
     * Handles the packet when it is received.
     *
     * @param message the packet
     * @param contextSupplier the NetworkEvent.Context supplier
     */
    public static void handlePacket(final ClientBoundSyncAxolootlResearchCapabilityPacket message, final Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        if (context.getDirection().getReceptionSide() == LogicalSide.CLIENT) {
            context.enqueueWork(() -> {
                // validate player
                final Player player = ClientUtil.getClientPlayer();
                if(null == player) {
                    return;
                }
                // load capability
                final Optional<AxolootlResearchCapability> oCap = player.getCapability(Axolootl.AXOLOOTL_RESEARCH_CAPABILITY).resolve();
                // deserialize capability from packet
                oCap.ifPresent(c -> c.deserializeNBT(message.getListTag()));
            });
        }
        context.setPacketHandled(true);
    }
}
