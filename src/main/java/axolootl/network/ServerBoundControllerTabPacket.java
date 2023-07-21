/**
 * Copyright (c) 2023 Skyler James
 * Permission is granted to use, modify, and redistribute this software, in parts or in whole,
 * under the GNU LGPLv3 license (https://www.gnu.org/licenses/lgpl-3.0.en.html)
 **/

package axolootl.network;

import axolootl.AxRegistry;
import axolootl.block.entity.ControllerBlockEntity;
import axolootl.data.aquarium_tab.IAquariumTab;
import axolootl.data.aquarium_tab.WorldlyMenuProvider;
import axolootl.menu.AbstractControllerMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkHooks;

import java.util.Optional;
import java.util.function.Supplier;

public class ServerBoundControllerTabPacket {

    private int tab;

    public ServerBoundControllerTabPacket(final int tab) {
        this.tab = tab;
    }

    /**
     * Reads the raw packet data from the data stream.
     *
     * @param buf the PacketBuffer
     * @return a new instance of the packet based on the PacketBuffer
     */
    public static ServerBoundControllerTabPacket fromBytes(final FriendlyByteBuf buf) {
        int tab = buf.readInt();
        return new ServerBoundControllerTabPacket(tab);
    }

    /**
     * Writes the raw packet data to the data stream.
     *
     * @param msg the packet
     * @param buf the PacketBuffer
     */
    public static void toBytes(final ServerBoundControllerTabPacket msg, final FriendlyByteBuf buf) {
        buf.writeInt(msg.tab);
    }

    /**
     * Handles the packet when it is received.
     *
     * @param message the packet
     * @param contextSupplier the NetworkEvent.Context supplier
     */
    public static void handlePacket(final ServerBoundControllerTabPacket message, final Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        if (context.getDirection().getReceptionSide() == LogicalSide.SERVER && context.getSender() != null) {
            context.enqueueWork(() -> {
                // validate player
                final ServerPlayer player = context.getSender();
                // validate menu
                if(!(player.containerMenu instanceof AbstractControllerMenu menu) || menu.getController().isEmpty()) {
                    return;
                }
                // validate controller
                final BlockPos pos = menu.getControllerPos();
                final ControllerBlockEntity controller = (ControllerBlockEntity) player.level.getBlockEntity(pos);
                if(null == controller) {
                    return;
                }
                // validate tab
                final IAquariumTab tab = AxRegistry.AquariumTabsReg.getSortedTabs().get(menu.validateTab(message.tab));
                if(!tab.isAvailable(controller)) {
                    return;
                }
                // validate menu provider
                final Optional<WorldlyMenuProvider> oProvider = tab.getMenuProvider(controller, null);
                if(oProvider.isEmpty()) {
                    return;
                }
                // open menu
                NetworkHooks.openScreen(player, oProvider.get().getProvider(), AxRegistry.MenuReg.writeControllerMenu(pos, oProvider.get().getPos(), message.tab, -1));
            });
        }
        context.setPacketHandled(true);
    }
}
