package axolootl;

import axolootl.data.AquariumModifier;
import axolootl.data.AxolootlVariant;
import axolootl.data.ResourceGenerator;
import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.common.world.BiomeModifier;
import net.minecraftforge.common.world.ForgeBiomeModifiers;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.entity.SpawnPlacementRegisterEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.NewRegistryEvent;
import net.minecraftforge.registries.RegistryBuilder;
import net.minecraftforge.registries.RegistryObject;
import org.checkerframework.checker.units.qual.A;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;


@SuppressWarnings("unused")
public final class AxRegistry {

    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, Axolootl.MODID);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, Axolootl.MODID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, Axolootl.MODID);
    private static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, Axolootl.MODID);
    private static final DeferredRegister<MenuType<?>> MENU_TYPES = DeferredRegister.create(ForgeRegistries.MENU_TYPES, Axolootl.MODID);

    public static final RegistryDispatcher<? extends ResourceGenerator> RESOURCE_GENERATORS_DISPATCHER = RegistryDispatcher.makeDispatchForgeRegistry(
            FMLJavaModLoadingContext.get().getModEventBus(), Keys.RESOURCE_GENERATORS, ResourceGenerator::getCodec, builder -> {});

    private static final DeferredRegister<Codec<? extends ResourceGenerator>> RESOURCE_GENERATORS = DeferredRegister.create(Keys.RESOURCE_GENERATORS, Axolootl.MODID);
    private static final DeferredRegister<AxolootlVariant> AXOLOOTL_VARIANTS = DeferredRegister.create(Keys.AXOLOOTL_VARIANTS, Axolootl.MODID);
    private static final DeferredRegister<AquariumModifier> AQUARIUM_MODIFIERS = DeferredRegister.create(Keys.AQUARIUM_MODIFIERS, Axolootl.MODID);

    private static final Supplier<IForgeRegistry<AxolootlVariant>> AXOLOOTL_VARIANTS_SUPPLIER = AXOLOOTL_VARIANTS.makeRegistry(() -> new RegistryBuilder<AxolootlVariant>()
            .dataPackRegistry(AxolootlVariant.CODEC, AxolootlVariant.CODEC)
            .hasTags());
    private static final Supplier<IForgeRegistry<AquariumModifier>> AQUARIUM_MODIFIERS_SUPPLIER = AQUARIUM_MODIFIERS.makeRegistry(() -> new RegistryBuilder<AquariumModifier>()
            .dataPackRegistry(AquariumModifier.CODEC, AquariumModifier.CODEC)
            .hasTags());

    public static void register() {
        BlockReg.register();
        ItemReg.register();
        BlockEntityReg.register();
        EntityReg.register();
        MenuReg.register();
        ResourceGenerators.register();
        AxolootlVariants.register();
        AquariumModifiers.register();
    }

    /**
     * @author Commoble
     * @param <T> an object
     */
    public record RegistryDispatcher<T>(Codec<Codec<? extends T>> dispatcherCodec, Codec<T> dispatchedCodec, DeferredRegister<Codec<? extends T>> registry, Supplier<IForgeRegistry<Codec<? extends T>>> registryGetter)
    {
        public static <T> RegistryDispatcher<T> makeDispatchForgeRegistry(
                final IEventBus modBus,
                final ResourceKey<? extends Registry<Codec<? extends T>>> registryId,
                final Function<T,? extends Codec<? extends T>> typeLookup,
                final Consumer<RegistryBuilder<Codec<? extends T>>> extraSettings)
        {
            DeferredRegister<Codec<? extends T>> deferredRegister = DeferredRegister.create(registryId, registryId.location().getNamespace());
            Supplier<RegistryBuilder<Codec<? extends T>>> builderFactory = () ->
            {
                RegistryBuilder<Codec<? extends T>> builder = new RegistryBuilder<>();
                extraSettings.accept(builder);
                return builder;
            };
            Supplier<IForgeRegistry<Codec<? extends T>>> registryGetter = deferredRegister.makeRegistry(builderFactory);
            Codec<Codec<? extends T>> dispatcherCodec = ResourceLocation.CODEC.xmap(
                    id -> registryGetter.get().getValue(id),
                    codec -> registryGetter.get().getKey(codec));
            Codec<T> dispatchedCodec = dispatcherCodec.dispatch(typeLookup, Function.identity());
            deferredRegister.register(modBus);

            return new RegistryDispatcher<>(dispatcherCodec, dispatchedCodec, deferredRegister, registryGetter);
        }
    }

    public static final class ItemReg {

        public static final CreativeModeTab TAB = new CreativeModeTab(Axolootl.MODID) {
            @Override
            public ItemStack makeIcon() {
                return new ItemStack(Items.EGG);
            }
        };

        private static final List<RegistryObject<Item>> SPAWN_EGGS = new ArrayList<>();

        public static void register() {
            ITEMS.register(FMLJavaModLoadingContext.get().getModEventBus());
        }

        /**
         * Creates a registry object for a block item and adds it to the mod creative tab
         * @param block the block
         * @return the registry object
         */
        private static RegistryObject<Item> registerBlockItem(final RegistryObject<Block> block) {
            return register(block.getId().getPath(), () -> new BlockItem(block.get(), new Item.Properties().tab(TAB)));
        }

        /**
         * Creates a registry object for the given vial item and adds it to the mod creative tab
         * @param name the registry name
         * @param entityType the entity type supplier
         * @param bgColor the background color
         * @param fgColor the foreground color
         * @return the item registry object
         */
        private static <T extends Mob> RegistryObject<Item> registerSpawnEgg(final String name, final RegistryObject<EntityType<T>> entityType, final int bgColor, final int fgColor) {
            final RegistryObject<Item> spawnEgg = ITEMS.register(name + "_spawn_egg", () -> new ForgeSpawnEggItem(entityType, bgColor, fgColor, new Item.Properties().tab(TAB)));
            SPAWN_EGGS.add(spawnEgg);
            return spawnEgg;
        }

        /**
         * Creates a registry object for the given item and adds it to the mod creative tab
         * @param name the registry name
         * @param supplier the item supplier
         * @return the item registry object
         */
        private static RegistryObject<Item> register(final String name, final Supplier<Item> supplier) {
            return ITEMS.register(name, supplier);
        }

        public static List<RegistryObject<Item>> getSpawnEggs() {
            return ImmutableList.copyOf(SPAWN_EGGS);
        }

    }

    public static final class BlockReg {

        public static void register() {
            BLOCKS.register(FMLJavaModLoadingContext.get().getModEventBus());
        }

        private static RegistryObject<Block> registerWithItem(final String name, final Supplier<Block> supplier) {
            return registerWithItem(name, supplier, ItemReg::registerBlockItem);
        }

        private static RegistryObject<Block> registerWithItem(final String name, final Supplier<Block> blockSupplier, final Function<RegistryObject<Block>, RegistryObject<Item>> itemSupplier) {
            final RegistryObject<Block> block = BLOCKS.register(name, blockSupplier);
            final RegistryObject<Item> item = itemSupplier.apply(block);
            return block;
        }

    }

    public static final class BlockEntityReg {

        public static void register() {
            BLOCK_ENTITY_TYPES.register(FMLJavaModLoadingContext.get().getModEventBus());
        }

    }

    public static final class EntityReg {

        public static void register() {
            ENTITY_TYPES.register(FMLJavaModLoadingContext.get().getModEventBus());
            FMLJavaModLoadingContext.get().getModEventBus().addListener(EntityReg::onEntityAttributeCreation);
            FMLJavaModLoadingContext.get().getModEventBus().addListener(EntityReg::onRegisterSpawnPlacement);
        }

        public static void onEntityAttributeCreation(final EntityAttributeCreationEvent event) {

        }

        public static void onRegisterSpawnPlacement(final SpawnPlacementRegisterEvent event) {

        }

    }

    public static final class MenuReg {

        public static void register() {
            MENU_TYPES.register(FMLJavaModLoadingContext.get().getModEventBus());
        }

    }

    public static final class ResourceGenerators {

        public static void register() {
            RESOURCE_GENERATORS.register(FMLJavaModLoadingContext.get().getModEventBus());
        }

        public static final RegistryObject<Codec<ResourceGenerator.Resource>> RESOURCE = RESOURCE_GENERATORS.register("resource", () -> ResourceGenerator.Resource.CODEC);
        public static final RegistryObject<Codec<ResourceGenerator.Mob>> MOB = RESOURCE_GENERATORS.register("mob", () -> ResourceGenerator.Mob.CODEC);
    }

    public static final class AquariumModifiers {

        public static void register() {
            AQUARIUM_MODIFIERS.register(FMLJavaModLoadingContext.get().getModEventBus());
        }

    }

    public static final class AxolootlVariants {

        public static void register() {
            AXOLOOTL_VARIANTS.register(FMLJavaModLoadingContext.get().getModEventBus());
        }

    }

    public static final class Keys {
        public static final ResourceKey<Registry<AxolootlVariant>> AXOLOOTL_VARIANTS = ResourceKey.createRegistryKey(new ResourceLocation(Axolootl.MODID, "axolootl_variants"));
        public static final ResourceKey<Registry<AquariumModifier>> AQUARIUM_MODIFIERS = ResourceKey.createRegistryKey(new ResourceLocation(Axolootl.MODID, "aquarium_modifiers"));
        public static final ResourceKey<Registry<Codec<? extends ResourceGenerator>>> RESOURCE_GENERATORS = ResourceKey.createRegistryKey(new ResourceLocation(Axolootl.MODID, "resource_generators"));
    }
}
