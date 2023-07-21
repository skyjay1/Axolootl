package axolootl.data.aquarium_tab;

import net.minecraft.core.BlockPos;
import net.minecraft.world.MenuProvider;

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
