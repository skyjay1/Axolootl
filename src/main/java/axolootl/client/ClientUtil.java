package axolootl.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.Optional;

public class ClientUtil {

    public static Optional<Level> getClientLevel() {
        return Optional.ofNullable(Minecraft.getInstance().level);
    }

    public static Player getClientPlayer() {
        return Minecraft.getInstance().player;
    }
}
