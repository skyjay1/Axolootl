/**
 * Copyright (c) 2023 Skyler James
 * Permission is granted to use, modify, and redistribute this software, in parts or in whole,
 * under the GNU LGPLv3 license (https://www.gnu.org/licenses/lgpl-3.0.en.html)
 **/

package axolootl.data.aquarium_tab;

import axolootl.AxRegistry;
import axolootl.block.entity.ControllerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
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
     * @param level the level
     * @param pos the block position
     * @param blockState the block state
     * @return true if this tab is applicable to the block at the given position
     */
    boolean isFor(final LevelAccessor level, final BlockPos pos, final BlockState blockState);

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

    public static Optional<IAquariumTab> forBlock(final LevelAccessor level, final BlockPos pos, final BlockState blockState) {
        for(Map.Entry<ResourceKey<IAquariumTab>, IAquariumTab> entry : AxRegistry.AQUARIUM_TABS_SUPPLIER.get().getEntries()) {
            if(entry.getValue().isFor(level, pos, blockState)) {
                return Optional.of(entry.getValue());
            }
        }
        return Optional.empty();
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
