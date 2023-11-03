/**
 * Copyright (c) 2023 Skyler James
 * Permission is granted to use, modify, and redistribute this software, in parts or in whole,
 * under the GNU LGPLv3 license (https://www.gnu.org/licenses/lgpl-3.0.en.html)
 **/

package axolootl;

import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableList;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.event.CreativeModeTabEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public final class AxTabs {
    public static final ResourceLocation TAB_ID = new ResourceLocation(Axolootl.MODID, "tab");
    public static CreativeModeTab tab;

    private static final List<Supplier<List<ItemStack>>> SORTED_ITEMS = new ArrayList<>();

    public static void register() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(AxTabs::onCreativeTabRegister);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(AxTabs::onCreativeTabBuild);
    }

    public static <T extends Item> RegistryObject<T> add(final RegistryObject<T> item) {
        SORTED_ITEMS.add(() -> ImmutableList.of(item.get().getDefaultInstance()));
        return item;
    }

    public static <T extends Item> RegistryObject<T> addAll(final RegistryObject<T> item, Supplier<List<ItemStack>> items) {
        SORTED_ITEMS.add(items);
        return item;
    }

    private static void onCreativeTabRegister(final CreativeModeTabEvent.Register event) {
        tab = event.registerCreativeModeTab(TAB_ID, builder -> builder
                .icon(Suppliers.memoize(() -> new ItemStack(Items.AXOLOTL_BUCKET)))
                .title(Component.translatable(TAB_ID.toLanguageKey("creativemodetab"))));
    }

    private static void onCreativeTabBuild(final CreativeModeTabEvent.BuildContents event) {
        if(event.getTab() == tab) {
            SORTED_ITEMS.forEach(supplier -> supplier.get().forEach(itemStack -> event.accept(itemStack)));
        }
    }
}
