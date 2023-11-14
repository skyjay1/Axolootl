/**
 * Copyright (c) 2023 Skyler James
 * Permission is granted to use, modify, and redistribute this software, in parts or in whole,
 * under the GNU LGPLv3 license (https://www.gnu.org/licenses/lgpl-3.0.en.html)
 **/

package axolootl.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import software.bernie.shadowed.eliotlash.mclib.math.functions.limit.Min;

import java.util.Optional;

public class ClientUtil {

    public static Optional<Level> getClientLevel() {
        return Optional.ofNullable(Minecraft.getInstance().level);
    }

    public static Player getClientPlayer() {
        return Minecraft.getInstance().player;
    }

    public static RegistryAccess getClientRegistryAccess() {
        return Minecraft.getInstance().level.registryAccess();
    }
}
