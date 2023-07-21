package axolootl.data.aquarium_tab;

import axolootl.AxRegistry;
import axolootl.block.entity.ControllerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface IAquariumTab {

    /**
     * @param blockEntity the controller block entity
     * @return true if this controller exists and the tab is enabled
     */
    boolean isAvailable(@Nullable final ControllerBlockEntity blockEntity);

    /**
     * @param isAvailable true if the tab is available
     * @return the tab title
     */
    Component getTitle(boolean isAvailable);

    /**
     * @param blockEntity the controller block entity
     * @param pos the block position to search, can be null
     * @return the menu provider for this tab type, if any was found
     */
    Optional<WorldlyMenuProvider> getMenuProvider(final ControllerBlockEntity blockEntity, @Nullable final BlockPos pos);

    /**
     * @return the item stack icon for this tab
     */
    ItemStack getIcon();

    /**
     * @return the tabs that this tab is ordered before
     */
    List<IAquariumTab> getBeforeTabs();

    /**
     * @return the tabs that this tab is ordered after
     */
    List<IAquariumTab> getAfterTabs();

    /**
     * @return the integer index of this tab
     */
    default int getSortedIndex() {
        return AxRegistry.AquariumTabsReg.getSortedTabs().indexOf(this);
    }

    /**
     * Iterates a set of block positions and returns the first one that is a menu provider
     * @param level the level
     * @param positions the block positions
     * @return the first valid menu provider, may be null
     * @see #getMenuProviderAt(Level, BlockPos)
     */
    @Nullable
    public static WorldlyMenuProvider getFirstMenuProvider(final Level level, final Set<BlockPos> positions) {
        for (BlockPos p : positions) {
            WorldlyMenuProvider provider = getMenuProviderAt(level, p);
            if (provider != null) {
                return provider;
            }
        }
        return null;
    }

    /**
     * @param level the level
     * @param pos   the block position
     * @return the menu provider at this position, may be null
     */
    @Nullable
    public static WorldlyMenuProvider getMenuProviderAt(final Level level, final BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof MenuProvider provider) {
            return new WorldlyMenuProvider(pos, provider);
        }
        return null;
    }



}
