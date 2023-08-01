/**
 * Copyright (c) 2023 Skyler James
 * Permission is granted to use, modify, and redistribute this software, in parts or in whole,
 * under the GNU LGPLv3 license (https://www.gnu.org/licenses/lgpl-3.0.en.html)
 **/

package axolootl.network;

import axolootl.block.entity.AxolootlInterfaceBlockEntity;
import axolootl.block.entity.ControllerBlockEntity;
import axolootl.entity.AxolootlEntity;
import axolootl.menu.AxolootlInterfaceMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MobBucketItem;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.network.NetworkEvent;

import java.util.Optional;
import java.util.function.Supplier;

public class ServerBoundInsertAxolootlPacket {

    public ServerBoundInsertAxolootlPacket() {}

    /**
     * Reads the raw packet data from the data stream.
     *
     * @param buf the PacketBuffer
     * @return a new instance of the packet based on the PacketBuffer
     */
    public static ServerBoundInsertAxolootlPacket fromBytes(final FriendlyByteBuf buf) {
        return new ServerBoundInsertAxolootlPacket();
    }

    /**
     * Writes the raw packet data to the data stream.
     *
     * @param msg the packet
     * @param buf the PacketBuffer
     */
    public static void toBytes(final ServerBoundInsertAxolootlPacket msg, final FriendlyByteBuf buf) {
    }

    /**
     * Handles the packet when it is received.
     *
     * @param message the packet
     * @param contextSupplier the NetworkEvent.Context supplier
     */
    public static void handlePacket(final ServerBoundInsertAxolootlPacket message, final Supplier<NetworkEvent.Context> contextSupplier) {
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
                final AxolootlInterfaceBlockEntity axolootlInterface = (AxolootlInterfaceBlockEntity) player.level.getBlockEntity(menu.getBlockPos());
                if(null == axolootlInterface) {
                    return;
                }
                // validate item handler
                Optional<IItemHandler> oHandler = axolootlInterface.getCapability(ForgeCapabilities.ITEM_HANDLER).resolve();
                if(oHandler.isEmpty()) {
                    return;
                }
                final IItemHandler handler = oHandler.get();
                // iterate item handler and insert any axolootls
                for(int i = 0, n = handler.getSlots(); i < n; i++) {
                    ItemStack itemStack = handler.extractItem(i, 1, true);
                    // verify item stack is mob bucket item
                    if(itemStack.getItem() instanceof MobBucketItem mobBucket && itemStack.hasTag() && itemStack.getTag().contains(AxolootlEntity.KEY_VARIANT_ID)) {
                        // determine spawn position
                        Optional<BlockPos> blockPos = controller.findSpawnablePosition(player.getLevel(), player.getRandom());
                        if(blockPos.isEmpty()) {
                            continue;
                        }
                        // spawn the axolootl
                        mobBucket.checkExtraContent(player, player.getLevel(), itemStack, blockPos.get());
                        controller.forceCalculateAxolootls();
                        // remove the item stack
                        ItemStack remainder = handler.extractItem(i, 1, false).getCraftingRemainingItem();
                        if(!remainder.isEmpty() && !(remainder = handler.insertItem(i, remainder, false)).isEmpty() && !player.getInventory().add(remainder)) {
                            player.drop(remainder, false);
                        }
                    }
                }
                // update controller
                // sadly there is no way to directly add the axolootl to the controller, so we tell it to search instead
                controller.findAxolootls(player.getLevel());
                controller.setChanged();
                player.level.sendBlockUpdated(controllerPos, controller.getBlockState(), controller.getBlockState(), Block.UPDATE_CLIENTS);
            });
        }
        context.setPacketHandled(true);
    }
}
