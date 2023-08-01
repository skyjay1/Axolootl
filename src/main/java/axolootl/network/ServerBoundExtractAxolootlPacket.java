/**
 * Copyright (c) 2023 Skyler James
 * Permission is granted to use, modify, and redistribute this software, in parts or in whole,
 * under the GNU LGPLv3 license (https://www.gnu.org/licenses/lgpl-3.0.en.html)
 **/

package axolootl.network;

import axolootl.block.entity.AxolootlInterfaceBlockEntity;
import axolootl.block.entity.ControllerBlockEntity;
import axolootl.menu.AxolootlInterfaceMenu;
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

public class ServerBoundExtractAxolootlPacket {

    private ResourceLocation id;

    public ServerBoundExtractAxolootlPacket(ResourceLocation id) {
        this.id = id;
    }

    /**
     * Reads the raw packet data from the data stream.
     *
     * @param buf the PacketBuffer
     * @return a new instance of the packet based on the PacketBuffer
     */
    public static ServerBoundExtractAxolootlPacket fromBytes(final FriendlyByteBuf buf) {
        ResourceLocation id = buf.readResourceLocation();
        return new ServerBoundExtractAxolootlPacket(id);
    }

    /**
     * Writes the raw packet data to the data stream.
     *
     * @param msg the packet
     * @param buf the PacketBuffer
     */
    public static void toBytes(final ServerBoundExtractAxolootlPacket msg, final FriendlyByteBuf buf) {
        buf.writeResourceLocation(msg.id);
    }

    /**
     * Handles the packet when it is received.
     *
     * @param message the packet
     * @param contextSupplier the NetworkEvent.Context supplier
     */
    public static void handlePacket(final ServerBoundExtractAxolootlPacket message, final Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        if (context.getDirection().getReceptionSide() == LogicalSide.SERVER && context.getSender() != null) {
            context.enqueueWork(() -> {
                // validate player
                final ServerPlayer player = context.getSender();
                // validate menu
                if(!(player.containerMenu instanceof AxolootlInterfaceMenu menu) || menu.getController().isEmpty()) {
                    return;
                }
                // validate controller
                final BlockPos controllerPos = menu.getController().get().getBlockPos();
                final ControllerBlockEntity controller = (ControllerBlockEntity) player.level.getBlockEntity(controllerPos);
                if(null == controller) {
                    return;
                }
                // validate axolootl interface
                final BlockPos blockPos = menu.getBlockPos();
                final AxolootlInterfaceBlockEntity axolootlInterface = (AxolootlInterfaceBlockEntity) player.level.getBlockEntity(blockPos);
                if(null == axolootlInterface) {
                    return;
                }
                // validate item handler
                Optional<IItemHandler> oHandler = axolootlInterface.getCapability(ForgeCapabilities.ITEM_HANDLER).resolve();
                if(oHandler.isEmpty()) {
                    return;
                }
                final IItemHandler handler = oHandler.get();
                // find bucket in container
                int bucketSlot = -1;
                for(int i = 0, n = handler.getSlots(); i < n; i++) {
                    if(handler.extractItem(i, 1, true).is(Items.BUCKET)) {
                        bucketSlot = i;
                        break;
                    }
                }
                // validate bucket was found
                if(bucketSlot < 0) {
                    return;
                }
                // validate axolootl exists
                UUID uuid = null;
                for(Map.Entry<UUID, ResourceLocation> entry : controller.getTrackedAxolootls().entrySet()) {
                    if(entry.getValue().equals(message.id)) {
                        uuid = entry.getKey();
                        break;
                    }
                }
                if(null == uuid) {
                    return;
                }
                // extract axolootl
                ItemStack itemStack = controller.removeAxolootl(player.getLevel(), uuid);
                if(itemStack.isEmpty()) {
                    return;
                }
                // extract bucket
                handler.extractItem(bucketSlot, 1, false);
                // insert axolootl item
                for(int i = 0, n = handler.getSlots(); i < n; i++) {
                    itemStack = handler.insertItem(i, itemStack, false);
                    if(itemStack.isEmpty()) {
                        break;
                    }
                }
                // drop remainder, if any
                if(!itemStack.isEmpty() && !player.getInventory().add(itemStack)) {
                    player.drop(itemStack, false);
                }
                // update controller
                controller.setChanged();
                player.level.sendBlockUpdated(controllerPos, controller.getBlockState(), controller.getBlockState(), Block.UPDATE_CLIENTS);
            });
        }
        context.setPacketHandled(true);
    }
}
