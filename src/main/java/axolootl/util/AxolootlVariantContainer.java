package axolootl.util;

import axolootl.data.AxolootlVariant;
import com.google.common.collect.ImmutableList;
import net.minecraft.core.NonNullList;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.Collection;
import java.util.Iterator;

public class AxolootlVariantContainer implements Container {

    private static final AxolootlVariant EMPTY = AxolootlVariant.EMPTY;
    private NonNullList<AxolootlVariant> list;

    public AxolootlVariantContainer(final int size) {
        this.list = NonNullList.withSize(size, EMPTY);
    }

    public AxolootlVariantContainer(final Collection<AxolootlVariant> collection) {
        this(collection.size());
        Iterator<AxolootlVariant> iterator = collection.iterator();
        for(int i = 0, n = collection.size(); i < n && iterator.hasNext(); i++) {
            this.list.set(i, iterator.next());
        }
    }

    public AxolootlVariantContainer(final AxolootlVariant... variants) {
        this(ImmutableList.copyOf(variants));
    }

    //// AXOLOOTL METHODS ////

    public AxolootlVariant getEntry(int slot) {
        return this.list.get(slot);
    }

    public AxolootlVariant removeEntry(int slot) {
        AxolootlVariant variant = this.list.set(slot, EMPTY);
        this.setChanged();
        return variant;
    }

    public AxolootlVariant removeEntryNoUpdate(int slot) {
        return this.list.set(slot, EMPTY);
    }

    public void setEntry(int slot, AxolootlVariant variant) {
        this.list.set(slot, variant);
    }

    //// CONTAINER METHODS ////

    @Override
    public int getContainerSize() {
        return this.list.size();
    }

    @Override
    public boolean isEmpty() {
        return this.list.stream().allMatch(AxolootlVariant.EMPTY::equals);
    }

    @Override
    public void clearContent() {
        this.list.clear();
    }

    @Override
    public void setChanged() {}

    //// NO OP ////

    @Override
    public ItemStack getItem(int pSlot) {
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int pSlot, int pAmount) {
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItemNoUpdate(int pSlot) {
        return ItemStack.EMPTY;
    }

    @Override
    public void setItem(int pSlot, ItemStack pStack) {

    }

    @Override
    public boolean stillValid(Player pPlayer) {
        return true;
    }
}
