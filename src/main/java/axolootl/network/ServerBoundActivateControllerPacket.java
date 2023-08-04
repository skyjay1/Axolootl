/**
 * Copyright (c) 2023 Skyler James
 * Permission is granted to use, modify, and redistribute this software, in parts or in whole,
 * under the GNU LGPLv3 license (https://www.gnu.org/licenses/lgpl-3.0.en.html)
 **/

package axolootl.network;

import axolootl.block.entity.AxolootlInterfaceBlockEntity;
import axolootl.block.entity.ControllerBlockEntity;
import axolootl.menu.AxolootlInterfaceMenu;
import axolootl.util.TankMultiblock;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.network.NetworkEvent;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

public class ServerBoundActivateControllerPacket {

    private BlockPos pos;

    public ServerBoundActivateControllerPacket(BlockPos pos) {
        this.pos = pos;
    }

    /**
     * Reads the raw packet data from the data stream.
     *
     * @param buf the PacketBuffer
     * @return a new instance of the packet based on the PacketBuffer
     */
    public static ServerBoundActivateControllerPacket fromBytes(final FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        return new ServerBoundActivateControllerPacket(pos);
    }

    /**
     * Writes the raw packet data to the data stream.
     *
     * @param msg the packet
     * @param buf the PacketBuffer
     */
    public static void toBytes(final ServerBoundActivateControllerPacket msg, final FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.pos);
    }

    /**
     * Handles the packet when it is received.
     *
     * @param message the packet
     * @param contextSupplier the NetworkEvent.Context supplier
     */
    public static void handlePacket(final ServerBoundActivateControllerPacket message, final Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        if (context.getDirection().getReceptionSide() == LogicalSide.SERVER && context.getSender() != null) {
            context.enqueueWork(() -> {
                // validate player
                final ServerPlayer player = context.getSender();
                // validate controller
                final BlockPos controllerPos = message.pos;
                if(!(player.level.getBlockEntity(controllerPos) instanceof ControllerBlockEntity controller)) {
                    return;
                }
                // update controller
                controller.setSize(TankMultiblock.AQUARIUM.hasTankStructure(player.getLevel(), controllerPos).orElse(null));
            });
        }
        context.setPacketHandled(true);
    }
}
