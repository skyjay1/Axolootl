/**
 * Copyright (c) 2023 Skyler James
 * Permission is granted to use, modify, and redistribute this software, in parts or in whole,
 * under the GNU LGPLv3 license (https://www.gnu.org/licenses/lgpl-3.0.en.html)
 **/

package axolootl.data.aquarium_tab;

import net.minecraft.core.BlockPos;
import net.minecraft.world.MenuProvider;

/**
 * Simple holder for a block position and the menu provider for that position
 * @author skyjay1
 */
public class WorldlyMenuProvider {
    private final BlockPos pos;
    private final MenuProvider provider;

    public WorldlyMenuProvider(BlockPos pos, MenuProvider provider) {
        this.pos = pos;
        this.provider = provider;
    }

    public BlockPos getPos() {
        return pos;
    }

    public MenuProvider getProvider() {
        return provider;
    }
}
