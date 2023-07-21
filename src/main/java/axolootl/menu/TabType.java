package axolootl.menu;

import axolootl.block.entity.ControllerBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.util.StringRepresentable;
import net.minecraft.util.Tuple;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.common.util.NonNullSupplier;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

public enum TabType implements StringRepresentable {
    CONTROLLER("controller",
            c -> true,
            c -> new Tuple<>(c.getBlockPos(), c),
            () -> Items.CONDUIT.getDefaultInstance()),
    AXOLOOTL_INTERFACE("axolootl_interface",
            c -> !c.getAxolootlInputs().isEmpty(),
            c -> getFirstMenuProvider(c.getLevel(), c.getAxolootlInputs()),
            () -> Items.AXOLOTL_BUCKET.getDefaultInstance()),
    FLUID_INTERFACE("fluid_interface",
            c -> !c.getFluidInputs().isEmpty(),
            c -> getFirstMenuProvider(c.getLevel(), c.getFluidInputs()),
            () -> Items.WATER_BUCKET.getDefaultInstance()),
    ENERGY_INTERFACE("energy_interface",
            c -> !c.getEnergyInputs().isEmpty(),
            c -> getFirstMenuProvider(c.getLevel(), c.getEnergyInputs()),
            () -> Items.REDSTONE.getDefaultInstance()),
    FOOD_INTERFACE("food_interface",
            c -> !c.resolveModifiers(c.getLevel().registryAccess(), c.activePredicate.and(c.foodInterfacePredicate)).isEmpty(),
            c -> getFirstMenuProvider(c.getLevel(), c.resolveModifiers(c.getLevel().registryAccess(), c.activePredicate.and(c.foodInterfacePredicate)).keySet()),
            () -> Items.TROPICAL_FISH.getDefaultInstance()),
    OUTPUT("output",
            c -> !c.getResourceOutputs().isEmpty(),
            c -> getFirstMenuProvider(c.getLevel(), c.getResourceOutputs()),
            () -> Items.CHEST.getDefaultInstance());

    private final String name;
    private final Component title;
    private final Component unavailableTitle;
    private final Predicate<ControllerBlockEntity> availablePredicate;
    private final Function<ControllerBlockEntity, Tuple<BlockPos, MenuProvider>> menuProvider;
    private final LazyOptional<ItemStack> icon;

    TabType(final String name, Predicate<ControllerBlockEntity> availablePredicate,
            Function<ControllerBlockEntity, Tuple<BlockPos, MenuProvider>> menuProvider, NonNullSupplier<ItemStack> icon) {
        this.name = name;
        this.title = Component.translatable("gui.axolootl.tab." + name);
        this.unavailableTitle = this.title.copy().withStyle(ChatFormatting.RED);
        this.availablePredicate = availablePredicate;
        this.menuProvider = menuProvider;
        this.icon = LazyOptional.of(icon);
    }

    /**
     * @param blockEntity the controller block entity
     * @return true if this controller exists and the tab is enabled
     */
    public boolean isAvailable(@Nullable final ControllerBlockEntity blockEntity) {
        if(null == blockEntity) {
            return false;
        }
        return this.availablePredicate.test(blockEntity);
    }

    /**
     * @return the tab title
     */
    public Component getTitle() {
        return title;
    }

    /**
     * @return the tab title when it the tab is not available
     */
    public Component getUnavailableTitle() {
        return unavailableTitle;
    }

    /**
     * @param blockEntity the controller block entity
     * @param pos the block position to search, can be null
     * @return the menu provider for this tab type, if any was found
     */
    public Optional<Tuple<BlockPos, MenuProvider>> getMenuProvider(final ControllerBlockEntity blockEntity, @Nullable final BlockPos pos) {
        if(pos != null) {
            return Optional.ofNullable(getMenuProviderAt(blockEntity.getLevel(), pos));
        }
        return Optional.ofNullable(menuProvider.apply(blockEntity));
    }

    public ItemStack getIcon() {
        return icon.orElse(ItemStack.EMPTY);
    }

    public int getIndex() {
        return this.ordinal();
    }

    /**
     * @param index the tab index
     * @return the tab type for the given index (clamped)
     */
    public static TabType getByIndex(final int index) {
        return TabType.values()[Mth.clamp(index, 0, values().length - 1)];
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }

    /**
     * Iterates a set of block positions and returns the first one that is a menu provider
     *
     * @param level     the level
     * @param positions the block positions
     * @return the first valid menu provider, may be null
     * @see #getMenuProviderAt(Level, BlockPos)
     */
    @Nullable
    private static Tuple<BlockPos, MenuProvider> getFirstMenuProvider(final Level level, final Set<BlockPos> positions) {
        for (BlockPos p : positions) {
            Tuple<BlockPos, MenuProvider> tuple = getMenuProviderAt(level, p);
            if (tuple != null) {
                return tuple;
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
    private static Tuple<BlockPos, MenuProvider> getMenuProviderAt(final Level level, final BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof MenuProvider provider) {
            return new Tuple<>(pos, provider);
        }
        return null;
    }
}
