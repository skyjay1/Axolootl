/**
 * Copyright (c) 2023 Skyler James
 * Permission is granted to use, modify, and redistribute this software, in parts or in whole,
 * under the GNU LGPLv3 license (https://www.gnu.org/licenses/lgpl-3.0.en.html)
 **/

package axolootl.data.aquarium_tab;

import axolootl.AxRegistry;
import axolootl.block.entity.ControllerBlockEntity;
import com.google.common.collect.ImmutableList;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.common.util.NonNullSupplier;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Default instance of {@link IAquariumTab}.
 * Use the {@link AquariumTab.Builder} to construct a tab.
 * @author skyjay1
 */
public class AquariumTab implements IAquariumTab {

    protected final Predicate<ControllerBlockEntity> availablePredicate;
    protected final Function<ControllerBlockEntity, WorldlyMenuProvider> menuProvider;
    protected final LazyOptional<ItemStack> icon;
    protected final LazyOptional<List<IAquariumTab>> before;
    protected final LazyOptional<List<IAquariumTab>> after;

    protected String descriptionId;
    protected Component title;
    protected Component unavailableTitle;

    public AquariumTab(Predicate<ControllerBlockEntity> availablePredicate,
                       Function<ControllerBlockEntity, WorldlyMenuProvider> menuProvider,
                       NonNullSupplier<ItemStack> icon, NonNullSupplier<List<IAquariumTab>> before,
                       NonNullSupplier<List<IAquariumTab>> after) {
        this.availablePredicate = availablePredicate;
        this.menuProvider = menuProvider;
        this.icon = LazyOptional.of(icon);
        this.before = LazyOptional.of(before);
        this.after = LazyOptional.of(after);
    }

    public static AquariumTab.Builder builder() {
        return new Builder();
    }

    @Override
    public boolean isAvailable(@Nullable final ControllerBlockEntity blockEntity) {
        if(null == blockEntity) {
            return false;
        }
        return this.availablePredicate.test(blockEntity);
    }

    @Override
    public Component getTitle(final boolean isAvailable) {
        if(null == title) {
            title = Component.translatable(getDescriptionId());
            unavailableTitle = title.copy().withStyle(ChatFormatting.RED);
        }
        return isAvailable ? title : unavailableTitle;
    }

    public String getDescriptionId() {
        if(null == this.descriptionId) {
            this.descriptionId = Util.makeDescriptionId("gui.controller_tab", getRegistryName());
        }
        return this.descriptionId;
    }

    @Nullable
    public ResourceLocation getRegistryName() {
        return AxRegistry.AQUARIUM_TABS_SUPPLIER.get().getKey(this);
    }

    @Override
    public Optional<WorldlyMenuProvider> getMenuProvider(final ControllerBlockEntity blockEntity, @Nullable final BlockPos pos) {
        if(pos != null) {
            return Optional.ofNullable(IAquariumTab.getMenuProviderAt(blockEntity.getLevel(), pos));
        }
        return Optional.ofNullable(menuProvider.apply(blockEntity));
    }

    @Override
    public ItemStack getIcon() {
        return icon.orElse(ItemStack.EMPTY);
    }

    @Override
    public List<IAquariumTab> getBeforeTabs() {
        return before.orElse(ImmutableList.of());
    }

    @Override
    public List<IAquariumTab> getAfterTabs() {
        return after.orElse(ImmutableList.of());
    }

    public static class Builder {
        private Predicate<ControllerBlockEntity> availablePredicate = c -> true;
        private Function<ControllerBlockEntity, WorldlyMenuProvider> menuProvider = c -> null;
        private NonNullSupplier<ItemStack> icon = () -> ItemStack.EMPTY;
        private NonNullSupplier<List<IAquariumTab>> before = () -> List.of();
        private NonNullSupplier<List<IAquariumTab>> after = () -> List.of();

        private Builder() {}

        public AquariumTab.Builder available(Predicate<ControllerBlockEntity> availablePredicate) {
            this.availablePredicate = availablePredicate;
            return this;
        }

        public AquariumTab.Builder menuProvider(Function<ControllerBlockEntity, WorldlyMenuProvider> menuProvider) {
            this.menuProvider = menuProvider;
            return this;
        }

        public AquariumTab.Builder icon(NonNullSupplier<ItemStack> icon) {
            this.icon = icon;
            return this;
        }

        /**
         * @param before the aquarium tabs that this tab is ordered before
         * @return the builder instance
         */
        public AquariumTab.Builder before(NonNullSupplier<List<IAquariumTab>> before) {
            this.before = before;
            return this;
        }

        /**
         * @param after the aquarium tabs that this tab is ordered after
         * @return the builder instance
         */
        public AquariumTab.Builder after(NonNullSupplier<List<IAquariumTab>> after) {
            this.after = after;
            return this;
        }

        public AquariumTab build() {
            return new AquariumTab(availablePredicate, menuProvider, icon, before, after);
        }
    }
}
