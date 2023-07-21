package axolootl.network;

import axolootl.AxRegistry;
import axolootl.block.entity.ControllerBlockEntity;
import axolootl.menu.AbstractControllerMenu;
import axolootl.menu.TabType;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Tuple;
import net.minecraft.world.MenuProvider;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkHooks;

import java.util.Optional;
import java.util.function.Supplier;

public class ServerBoundControllerCyclePacket {

    private int cycle;

    public ServerBoundControllerCyclePacket(final int cycle) {
        this.cycle = cycle;
    }

    /**
     * Reads the raw packet data from the data stream.
     *
     * @param buf the PacketBuffer
     * @return a new instance of the packet based on the PacketBuffer
     */
    public static ServerBoundControllerCyclePacket fromBytes(final FriendlyByteBuf buf) {
        int tab = buf.readInt();
        return new ServerBoundControllerCyclePacket(tab);
    }

    /**
     * Writes the raw packet data to the data stream.
     *
     * @param msg the packet
     * @param buf the PacketBuffer
     */
    public static void toBytes(final ServerBoundControllerCyclePacket msg, final FriendlyByteBuf buf) {
        buf.writeInt(msg.cycle);
    }

    /**
     * Handles the packet when it is received.
     *
     * @param message the packet
     * @param contextSupplier the NetworkEvent.Context supplier
     */
    public static void handlePacket(final ServerBoundControllerCyclePacket message, final Supplier<NetworkEvent.Context> contextSupplier) {
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
                final BlockPos controllerPos = menu.getController().get().getBlockPos();
                final ControllerBlockEntity controller = (ControllerBlockEntity) player.level.getBlockEntity(controllerPos);
                if(null == controller) {
                    return;
                }
                // validate tab
                final TabType tabType = TabType.getByIndex(menu.getTab());
                if(!tabType.isAvailable(controller)) {
                    return;
                }
                // validate cycle
                if(message.cycle < 0 || message.cycle >= menu.getSortedCycleList().size()) {
                    return;
                }
                BlockPos pos = menu.getSortedCycleList().get(message.cycle);
                // validate menu provider
                final Optional<Tuple<BlockPos, MenuProvider>> oProvider = tabType.getMenuProvider(controller, pos);
                if(oProvider.isEmpty()) {
                    return;
                }
                // open menu
                NetworkHooks.openScreen(player, oProvider.get().getB(), AxRegistry.MenuReg.writeControllerMenu(controllerPos, oProvider.get().getA(), menu.getTab(), message.cycle));
            });
        }
        context.setPacketHandled(true);
    }
}
