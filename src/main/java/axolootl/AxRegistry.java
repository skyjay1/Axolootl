/**
 * Copyright (c) 2023 Skyler James
 * Permission is granted to use, modify, and redistribute this software, in parts or in whole,
 * under the GNU LGPLv3 license (https://www.gnu.org/licenses/lgpl-3.0.en.html)
 **/

package axolootl;

import axolootl.block.*;
import axolootl.block.entity.*;
import axolootl.capability.AxolootlResearchCapability;
import axolootl.data.aquarium_tab.AquariumTab;
import axolootl.data.aquarium_tab.IAquariumTab;
import axolootl.data.aquarium_tab.WorldlyMenuProvider;
import axolootl.data.axolootl_variant.condition.*;
import axolootl.data.breeding.AxolootlBreeding;
import axolootl.data.aquarium_modifier.AquariumModifier;
import axolootl.data.axolootl_variant.AxolootlVariant;
import axolootl.data.aquarium_modifier.condition.*;
import axolootl.data.breeding.AxolootlBreedingWrapper;
import axolootl.data.breeding_modifier.*;
import axolootl.data.resource_generator.*;
import axolootl.entity.AxolootlEntity;
import axolootl.item.AxolootlBucketItem;
import axolootl.item.GrandCastleMultiBlockItem;
import axolootl.item.MultiBlockItem;
import axolootl.menu.AxolootlInspectorMenu;
import axolootl.menu.AxolootlInterfaceMenu;
import axolootl.menu.ControllerMenu;
import axolootl.menu.CyclingContainerMenu;
import axolootl.menu.CyclingMenu;
import axolootl.util.AxMatchingBlocksPredicate;
import axolootl.util.AxMatchingFluidsPredicate;
import axolootl.util.ControllerTabSorter;
import axolootl.util.MatchingStatePredicate;
import axolootl.util.NbtPredicate;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.animal.axolotl.Axolotl;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicateType;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.level.material.MaterialColor;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.entity.SpawnPlacementRegisterEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.util.thread.EffectiveSide;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.RegistryBuilder;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiPredicate;
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
    private static final DeferredRegister<RecipeType<?>> RECIPE_TYPES = DeferredRegister.create(ForgeRegistries.RECIPE_TYPES, Axolootl.MODID);
    private static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS = DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, Axolootl.MODID);
    private static final DeferredRegister<BlockPredicateType<?>> BLOCK_PREDICATE_TYPES = DeferredRegister.create(BuiltInRegistries.BLOCK_PREDICATE_TYPE.key(), Axolootl.MODID);

    // RESOURCE GENERATORS //
    private static final DeferredRegister<Codec<? extends ResourceGenerator>> RESOURCE_GENERATOR_SERIALIZERS = DeferredRegister.create(Keys.RESOURCE_GENERATOR_SERIALIZERS, Axolootl.MODID);
    public static final Supplier<IForgeRegistry<Codec<? extends ResourceGenerator>>> RESOURCE_GENERATOR_SERIALIZERS_SUPPLIER = RESOURCE_GENERATOR_SERIALIZERS.makeRegistry(() -> new RegistryBuilder<>());
    private static final DeferredRegister<ResourceGenerator> RESOURCE_GENERATORS = DeferredRegister.create(Keys.RESOURCE_GENERATORS, Axolootl.MODID);
    public static final Function<RegistryAccess, Registry<ResourceGenerator>> RESOURCE_GENERATORS_REGISTRY_SUPPLIER = access -> access.registryOrThrow(Keys.RESOURCE_GENERATORS);

    // MODIFIER CONDITIONS //
    private static final DeferredRegister<Codec<? extends ModifierCondition>> MODIFIER_CONDITION_SERIALIZERS = DeferredRegister.create(Keys.MODIFIER_CONDITION_SERIALIZERS, Axolootl.MODID);
    public static final Supplier<IForgeRegistry<Codec<? extends ModifierCondition>>> MODIFIER_CONDITION_SERIALIZERS_SUPPLIER = MODIFIER_CONDITION_SERIALIZERS.makeRegistry(() -> new RegistryBuilder<>());
    private static final DeferredRegister<ModifierCondition> MODIFIER_CONDITIONS = DeferredRegister.create(Keys.MODIFIER_CONDITIONS, Axolootl.MODID);
    public static final Supplier<IForgeRegistry<ModifierCondition>> MODIFIER_CONDITIONS_SUPPLIER = MODIFIER_CONDITIONS.makeRegistry(() -> new RegistryBuilder<>());

    // FORGE CONDITIONS //
    private static final DeferredRegister<Codec<? extends ForgeCondition>> FORGE_CONDITION_SERIALIZERS = DeferredRegister.create(Keys.FORGE_CONDITION_SERIALIZERS, "forge");
    public static final Supplier<IForgeRegistry<Codec<? extends ForgeCondition>>> FORGE_CONDITION_SERIALIZERS_SUPPLIER = FORGE_CONDITION_SERIALIZERS.makeRegistry(() -> new RegistryBuilder<>());
    private static final DeferredRegister<ForgeCondition> FORGE_CONDITIONS = DeferredRegister.create(Keys.FORGE_CONDITIONS, "forge");
    public static final Supplier<IForgeRegistry<ForgeCondition>> FORGE_CONDITIONS_SUPPLIER = FORGE_CONDITIONS.makeRegistry(() -> new RegistryBuilder<>());

    // AXOLOOTL VARIANTS //
    private static final DeferredRegister<AxolootlVariant> AXOLOOTL_VARIANTS = DeferredRegister.create(Keys.AXOLOOTL_VARIANTS, Axolootl.MODID);
    public static final Function<RegistryAccess, Registry<AxolootlVariant>> AXOLOOTL_VARIANTS_SUPPLIER = access -> access.registryOrThrow(Keys.AXOLOOTL_VARIANTS);

    // AXOLOOTL BREEDING //
    private static final DeferredRegister<AxolootlBreeding> AXOLOOTL_BREEDING = DeferredRegister.create(Keys.AXOLOOTL_BREEDING, Axolootl.MODID);
    public static final Function<RegistryAccess, Registry<AxolootlBreeding>> AXOLOOTL_BREEDING_SUPPLIER = access -> access.registryOrThrow(Keys.AXOLOOTL_BREEDING);

    // AXOLOOTL BREEDING MODIFIERS //
    private static final DeferredRegister<Codec<? extends AxolootlBreedingModifier>> AXOLOOTL_BREEDING_MODIFIERS_SERIALIZERS = DeferredRegister.create(Keys.AXOLOOTL_BREEDING_MODIFIER_SERIALIZERS, Axolootl.MODID);
    public static final Supplier<IForgeRegistry<Codec<? extends AxolootlBreedingModifier>>> AXOLOOTL_BREEDING_MODIFIERS_SERIALIZERS_SUPPLIER = AXOLOOTL_BREEDING_MODIFIERS_SERIALIZERS.makeRegistry(() -> new RegistryBuilder<>());
    private static final DeferredRegister<AxolootlBreedingModifier> AXOLOOTL_BREEDING_MODIFIERS = DeferredRegister.create(Keys.AXOLOOTL_BREEDING_MODIFIERS, Axolootl.MODID);
    public static final Function<RegistryAccess, Registry<AxolootlBreedingModifier>> AXOLOOTL_BREEDING_MODIFIERS_SUPPLIER = access -> access.registryOrThrow(Keys.AXOLOOTL_BREEDING_MODIFIERS);

    // AQUARIUM MODIFIERS //
    private static final DeferredRegister<AquariumModifier> AQUARIUM_MODIFIERS = DeferredRegister.create(Keys.AQUARIUM_MODIFIERS, Axolootl.MODID);
    public static final Function<RegistryAccess, Registry<AquariumModifier>> AQUARIUM_MODIFIERS_SUPPLIER = access -> access.registryOrThrow(Keys.AQUARIUM_MODIFIERS);

    // AQUARIUM TABS //
    private static final DeferredRegister<IAquariumTab> AQUARIUM_TABS = DeferredRegister.create(Keys.AQUARIUM_TABS, Axolootl.MODID);
    public static final Supplier<IForgeRegistry<IAquariumTab>> AQUARIUM_TABS_SUPPLIER = AQUARIUM_TABS.makeRegistry(() -> new RegistryBuilder<>());

    public static void register() {
        BlockReg.register();
        ItemReg.register();
        BlockEntityReg.register();
        EntityReg.register();
        MenuReg.register();
        RecipeReg.register();
        CapabilityReg.register();
        BlockPredicateTypesReg.register();
        ModifierConditionsReg.register();
        ResourceGeneratorsReg.register();
        ForgeConditionsReg.register();
        AxolootlVariantsReg.register();
        AquariumModifiersReg.register();
        AquariumTabsReg.register();
        AxolootlBreedingReg.register();
        AxolootlBreedingModifierReg.register();
        FMLJavaModLoadingContext.get().getModEventBus().addListener(AxRegistry::onCommonSetup);
    }

    //// HELPERS ////

    /**
     * Clears the cached datapack-derived data
     */
    public static void clearCaches() {
        AxRegistry.AquariumTabsReg.clearCache();
        AxRegistry.AxolootlVariantsReg.clearCache();
        AxRegistry.AquariumModifiersReg.clearCache();
        AxRegistry.AxolootlBreedingReg.clearCache();
    }

    /**
     * Clears and re-populates cached datapack-derived data
     * @param registryAccess the registry access
     */
    public static void refreshCaches(final RegistryAccess registryAccess) {
        clearCaches();
        AxRegistry.AquariumTabsReg.getSortedTabs();
        AxRegistry.AxolootlVariantsReg.validate(registryAccess);
        AxRegistry.AquariumModifiersReg.getMandatoryAquariumModifiers(registryAccess);
    }

    private static void onCommonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> EntityDataSerializers.registerSerializer(AxolootlEntity.OPTIONAL_RESOURCE_LOCATION));
    }

    //// REGISTRY CLASSES ////

    public static final class ItemReg {

        public static void register() {
            ITEMS.register(FMLJavaModLoadingContext.get().getModEventBus());
        }

        public static final RegistryObject<Item> BASTION_RAMPART = register("bastion_rampart", () -> new Item(new Item.Properties().stacksTo(16)));
        public static final RegistryObject<Item> CASTLE_RAMPART = register("castle_rampart", () -> new Item(new Item.Properties().stacksTo(16)));
        public static final RegistryObject<Item> END_CITY_RAMPART = register("end_city_rampart", () -> new Item(new Item.Properties().stacksTo(16)));
        public static final RegistryObject<Item> AXOLOOTL_BUCKET = registerWithSubtypes("axolootl_bucket", () ->
                new AxolootlBucketItem(EntityReg.AXOLOOTL, () -> Fluids.WATER, () -> SoundEvents.BUCKET_EMPTY_AXOLOTL, new Item.Properties().stacksTo(1).craftRemainder(Items.BUCKET)),
                AxolootlBucketItem::createSubtypes);

        /**
         * Creates a registry object for a block item and adds it to the mod creative tab
         * @param block the block
         * @return the registry object
         */
        private static RegistryObject<Item> registerBlockItem(final RegistryObject<Block> block) {
            return register(block.getId().getPath(), () -> new BlockItem(block.get(), new Item.Properties()));
        }

        /**
         * Creates a registry object for the given item and adds it to the mod creative tab
         * @param name the registry name
         * @param supplier the item supplier
         * @return the item registry object
         */
        private static RegistryObject<Item> register(final String name, final Supplier<Item> supplier) {
            return AxTabs.add(ITEMS.register(name, supplier));
        }

        /**
         * Creates a registry object for the given item and adds it to the mod creative tab
         * @param name the registry name
         * @param supplier the item supplier
         * @return the item registry object
         */
        private static RegistryObject<Item> registerWithSubtypes(final String name, final Supplier<Item> supplier, final Supplier<List<ItemStack>> subtypes) {
            return AxTabs.addAll(ITEMS.register(name, supplier), subtypes);
        }
    }

    public static final class BlockReg {

        public static void register() {
            BLOCKS.register(FMLJavaModLoadingContext.get().getModEventBus());
        }

        public static final RegistryObject<Block> AQUARIUM_FLOOR = registerWithItem("aquarium_floor", () -> new AquariumFloorBlock(BlockBehaviour.Properties.of(Material.METAL).strength(1.5F, 2.0F)));
        public static final RegistryObject<Block> SAND_AQUARIUM_FLOOR = registerWithItem("sand_aquarium_floor", () -> new FilledAquariumFloorBlock(() -> new ItemStack(Items.SAND), BlockBehaviour.Properties.of(Material.METAL, MaterialColor.SAND).sound(SoundType.SAND).strength(1.5F, 2.0F)));
        public static final RegistryObject<Block> GRAVEL_AQUARIUM_FLOOR = registerWithItem("gravel_aquarium_floor", () -> new FilledAquariumFloorBlock(() -> new ItemStack(Items.GRAVEL), BlockBehaviour.Properties.of(Material.METAL, MaterialColor.COLOR_LIGHT_GRAY).sound(SoundType.GRAVEL).strength(1.5F, 2.0F)));
        public static final RegistryObject<Block> AQUARIUM_ROOF = registerWithItem("aquarium_roof", () -> new AquariumGlassBlock(BlockBehaviour.Properties.of(Material.BUILDABLE_GLASS, MaterialColor.NONE).sound(SoundType.GLASS).noOcclusion().isValidSpawn(BlockReg::never).isRedstoneConductor(BlockReg::never).isSuffocating(BlockReg::never).isViewBlocking(BlockReg::never).strength(1.5F, 2.0F)));
        public static final RegistryObject<Block> AQUARIUM_GLASS = registerWithItem("aquarium_glass", () -> new AquariumGlassBlock(BlockBehaviour.Properties.of(Material.BUILDABLE_GLASS, MaterialColor.NONE).sound(SoundType.GLASS).noOcclusion().isValidSpawn(BlockReg::never).isRedstoneConductor(BlockReg::never).isSuffocating(BlockReg::never).isViewBlocking(BlockReg::never).strength(1.5F, 2.0F)));
        public static final RegistryObject<Block> AQUARIUM_CONTROLLER = registerWithItem("aquarium_controller", () -> new ControllerBlock(BlockBehaviour.Properties.of(Material.METAL, MaterialColor.NONE).requiresCorrectToolForDrops().sound(SoundType.GLASS).noOcclusion().isValidSpawn(BlockReg::never).isRedstoneConductor(BlockReg::never).isSuffocating(BlockReg::never).isViewBlocking(BlockReg::never).strength(3.5F, 8.0F)));
        public static final RegistryObject<Block> AQUARIUM_WATER_INTERFACE = registerWithItem("aquarium_water_interface", () -> new WaterInterfaceBlock(BlockBehaviour.Properties.of(Material.METAL, MaterialColor.NONE).requiresCorrectToolForDrops().sound(SoundType.GLASS).noOcclusion().isValidSpawn(BlockReg::never).isRedstoneConductor(BlockReg::never).isSuffocating(BlockReg::never).isViewBlocking(BlockReg::never).strength(3.5F, 8.0F)));
        public static final RegistryObject<Block> AQUARIUM_ENERGY_INTERFACE = registerWithItem("aquarium_energy_interface", () -> new EnergyInterfaceBlock(BlockBehaviour.Properties.of(Material.METAL, MaterialColor.NONE).requiresCorrectToolForDrops().sound(SoundType.GLASS).noOcclusion().isValidSpawn(BlockReg::never).isRedstoneConductor(BlockReg::never).isSuffocating(BlockReg::never).isViewBlocking(BlockReg::never).strength(3.5F, 8.0F)));
        public static final RegistryObject<Block> AQUARIUM_AXOLOOTL_INTERFACE = registerWithItem("aquarium_axolootl_interface", () -> new AxolootlInterfaceBlock(BlockBehaviour.Properties.of(Material.METAL, MaterialColor.NONE).requiresCorrectToolForDrops().sound(SoundType.GLASS).noOcclusion().isValidSpawn(BlockReg::never).isRedstoneConductor(BlockReg::never).isSuffocating(BlockReg::never).isViewBlocking(BlockReg::never).strength(3.5F, 8.0F)));
        public static final RegistryObject<Block> AQUARIUM_AXOLOOTL_INSPECTOR = registerWithItem("aquarium_axolootl_inspector", () -> new AxolootlInspectorBlock(BlockBehaviour.Properties.of(Material.METAL, MaterialColor.NONE).requiresCorrectToolForDrops().sound(SoundType.GLASS).noOcclusion().isValidSpawn(BlockReg::never).isRedstoneConductor(BlockReg::never).isSuffocating(BlockReg::never).isViewBlocking(BlockReg::never).strength(3.5F, 8.0F)));
        public static final RegistryObject<Block> AQUARIUM_OUTPUT = registerWithItem("aquarium_output", () -> new OutputBlock(3, BlockBehaviour.Properties.of(Material.METAL, MaterialColor.NONE).requiresCorrectToolForDrops().sound(SoundType.GLASS).noOcclusion().isValidSpawn(BlockReg::never).isRedstoneConductor(BlockReg::never).isSuffocating(BlockReg::never).isViewBlocking(BlockReg::never).strength(3.5F, 8.0F)));
        public static final RegistryObject<Block> LARGE_AQUARIUM_OUTPUT = registerWithItem("large_aquarium_output", () -> new OutputBlock(6, BlockBehaviour.Properties.of(Material.METAL, MaterialColor.NONE).requiresCorrectToolForDrops().sound(SoundType.GLASS).noOcclusion().isValidSpawn(BlockReg::never).isRedstoneConductor(BlockReg::never).isSuffocating(BlockReg::never).isViewBlocking(BlockReg::never).strength(3.5F, 8.0F)));
        public static final RegistryObject<Block> AQUARIUM_AIRLOCK = registerWithItem("aquarium_airlock", () -> new AquariumAirlockBlock(BlockBehaviour.Properties.of(Material.METAL).noOcclusion().dynamicShape().isSuffocating(BlockReg::never).requiresCorrectToolForDrops().strength(3.5F, 6.0F)));
        public static final RegistryObject<Block> BUBBLER = registerWithItem("bubbler", () -> new BubblerBlock(BlockBehaviour.Properties.of(Material.METAL)/*.randomTicks()*/.requiresCorrectToolForDrops().strength(3.5F)));
        public static final RegistryObject<Block> POWERED_BUBBLER = registerWithItem("powered_bubbler", () -> new BubblerBlock(BlockBehaviour.Properties.of(Material.METAL)/*.randomTicks()*/.requiresCorrectToolForDrops().strength(3.5F)));
        public static final RegistryObject<Block> PUMP = registerWithItem("pump", () -> new PumpBlock(BlockBehaviour.Properties.of(Material.METAL).noOcclusion().requiresCorrectToolForDrops().strength(3.5F)));
        public static final RegistryObject<Block> POWERED_PUMP = registerWithItem("powered_pump", () -> new PumpBlock(BlockBehaviour.Properties.of(Material.METAL).noOcclusion().requiresCorrectToolForDrops().strength(3.5F)));
        public static final RegistryObject<Block> BREEDER = registerWithItem("breeder", () -> new BreederBlock(BlockBehaviour.Properties.of(Material.METAL, MaterialColor.COLOR_CYAN).requiresCorrectToolForDrops().strength(3.5F)));
        public static final RegistryObject<Block> NURSERY = registerWithItem("nursery", () -> new NurseryBlock(BlockBehaviour.Properties.of(Material.METAL, MaterialColor.COLOR_CYAN).noOcclusion().requiresCorrectToolForDrops().strength(3.5F)));
        public static final RegistryObject<Block> MONSTERIUM = registerWithItem("monsterium", () -> new MonsteriumBlock(BlockBehaviour.Properties.of(Material.METAL, MaterialColor.COLOR_CYAN).noOcclusion().requiresCorrectToolForDrops().isValidSpawn(BlockReg::never).isRedstoneConductor(BlockReg::never).strength(3.5F)));
        public static final RegistryObject<Block> AUTOFEEDER = registerWithItem("autofeeder", () -> new AutofeederBlock(BlockBehaviour.Properties.of(Material.METAL, MaterialColor.COLOR_CYAN).noOcclusion().requiresCorrectToolForDrops().strength(3.5F)));
        public static final RegistryObject<Block> BASTION = registerWithMultiBlockItem("bastion", () -> new BastionMultiBlock(BlockBehaviour.Properties.of(Material.STONE, MaterialColor.TERRACOTTA_BLACK).noOcclusion().strength(5.0F, 120.0F)));
        public static final RegistryObject<Block> CASTLE = registerWithItem("castle", () -> new CastleBlock(BlockBehaviour.Properties.of(Material.STONE).noOcclusion().dynamicShape().isSuffocating(BlockReg::never).strength(3.5F, 60.0F)));
        public static final RegistryObject<Block> GRAND_CASTLE = registerWithGrandCastleMultiBlockItem("grand_castle", () -> new GrandCastleMultiBlock(BlockBehaviour.Properties.of(Material.STONE, state -> state.getValue(GrandCastleMultiBlock.ENCHANTMENT_LEVEL) > 0 ? MaterialColor.COLOR_BLUE : MaterialColor.STONE).noOcclusion().strength(5.0F, 120.0F)));
        public static final RegistryObject<Block> END_CITY = registerWithItem("end_city", () -> new EndCityBlock(BlockBehaviour.Properties.of(Material.STONE, MaterialColor.COLOR_PURPLE).noOcclusion().isSuffocating(BlockReg::never).strength(3.5F, 60.0F)));
        public static final RegistryObject<Block> END_SHIP = registerWithItem("end_ship", () -> new EndShipBlock(BlockBehaviour.Properties.of(Material.STONE, MaterialColor.COLOR_PURPLE).noOcclusion().isSuffocating(BlockReg::never).strength(3.5F, 60.0F)));


        private static RegistryObject<Block> registerWithItem(final String name, final Supplier<Block> supplier) {
            return registerWithItem(name, supplier, ItemReg::registerBlockItem);
        }

        private static RegistryObject<Block> registerWithItem(final String name, final Supplier<Block> blockSupplier, final Function<RegistryObject<Block>, RegistryObject<Item>> itemSupplier) {
            final RegistryObject<Block> block = BLOCKS.register(name, blockSupplier);
            final RegistryObject<Item> item = itemSupplier.apply(block);
            return block;
        }

        private static RegistryObject<Block> registerWithMultiBlockItem(final String name, final Supplier<Block> supplier) {
            return registerWithItem(name, supplier, block -> ItemReg.register(block.getId().getPath(), () -> new MultiBlockItem(block.get(), new Item.Properties().stacksTo(1))));
        }

        private static RegistryObject<Block> registerWithGrandCastleMultiBlockItem(final String name, final Supplier<Block> supplier) {
            return registerWithItem(name, supplier, block -> ItemReg.register(block.getId().getPath(), () -> new GrandCastleMultiBlockItem(block.get(), new Item.Properties().stacksTo(1))));
        }

        private static boolean never(final BlockState blockState, final BlockGetter blockGetter, final BlockPos blockPos, final EntityType<?> entityType) {
            return false;
        }

        private static boolean never(final BlockState state, final BlockGetter level, final BlockPos pos) {
            return false;
        }

    }

    public static final class BlockEntityReg {

        public static void register() {
            BLOCK_ENTITY_TYPES.register(FMLJavaModLoadingContext.get().getModEventBus());
        }

        public static final RegistryObject<BlockEntityType<ControllerBlockEntity>> CONTROLLER = BLOCK_ENTITY_TYPES.register("controller", () ->
                BlockEntityType.Builder.of(ControllerBlockEntity::new, BlockReg.AQUARIUM_CONTROLLER.get())
                        .build(null));

        public static final RegistryObject<BlockEntityType<AxolootlInterfaceBlockEntity>> AXOLOOTL_INTERFACE = BLOCK_ENTITY_TYPES.register("axolootl_interface", () ->
                BlockEntityType.Builder.of(AxolootlInterfaceBlockEntity::new, BlockReg.AQUARIUM_AXOLOOTL_INTERFACE.get())
                        .build(null));

        public static final RegistryObject<BlockEntityType<WaterInterfaceBlockEntity>> WATER_INTERFACE = BLOCK_ENTITY_TYPES.register("water_interface", () ->
                BlockEntityType.Builder.of(WaterInterfaceBlockEntity::new, BlockReg.AQUARIUM_WATER_INTERFACE.get())
                        .build(null));

        public static final RegistryObject<BlockEntityType<EnergyInterfaceBlockEntity>> ENERGY_INTERFACE = BLOCK_ENTITY_TYPES.register("energy_interface", () ->
                BlockEntityType.Builder.of(EnergyInterfaceBlockEntity::new, BlockReg.AQUARIUM_ENERGY_INTERFACE.get())
                        .build(null));

        public static final RegistryObject<BlockEntityType<OutputInterfaceBlockEntity>> OUTPUT_INTERFACE = BLOCK_ENTITY_TYPES.register("output_interface", () ->
                BlockEntityType.Builder.of(OutputInterfaceBlockEntity::new, BlockReg.AQUARIUM_OUTPUT.get(), BlockReg.LARGE_AQUARIUM_OUTPUT.get())
                        .build(null));

        public static final RegistryObject<BlockEntityType<AutoFeederBlockEntity>> AUTO_FEEDER = BLOCK_ENTITY_TYPES.register("auto_feeder", () ->
                BlockEntityType.Builder.of(AutoFeederBlockEntity::new, BlockReg.AUTOFEEDER.get())
                        .build(null));

        public static final RegistryObject<BlockEntityType<BreederBlockEntity>> BREEDER = BLOCK_ENTITY_TYPES.register("breeder", () ->
                BlockEntityType.Builder.of(BreederBlockEntity::new, BlockReg.BREEDER.get())
                        .build(null));

        public static final RegistryObject<BlockEntityType<MonsteriumBlockEntity>> MONSTERIUM = BLOCK_ENTITY_TYPES.register("monsterium", () ->
                BlockEntityType.Builder.of(MonsteriumBlockEntity::new, BlockReg.MONSTERIUM.get())
                        .build(null));

        public static final RegistryObject<BlockEntityType<AxolootlInspectorBlockEntity>> AXOLOOTL_INSPECTOR = BLOCK_ENTITY_TYPES.register("axolootl_inspector", () ->
                BlockEntityType.Builder.of(AxolootlInspectorBlockEntity::new, BlockReg.AQUARIUM_AXOLOOTL_INSPECTOR.get())
                        .build(null));

    }

    public static final class EntityReg {

        public static void register() {
            ENTITY_TYPES.register(FMLJavaModLoadingContext.get().getModEventBus());
            FMLJavaModLoadingContext.get().getModEventBus().addListener(EntityReg::onEntityAttributeCreation);
            FMLJavaModLoadingContext.get().getModEventBus().addListener(EntityReg::onRegisterSpawnPlacement);
        }

        public static void onEntityAttributeCreation(final EntityAttributeCreationEvent event) {
            event.put(AXOLOOTL.get(), AxolootlEntity.createAttributes().build());
        }

        public static void onRegisterSpawnPlacement(final SpawnPlacementRegisterEvent event) {
            event.register(AXOLOOTL.get(), SpawnPlacements.Type.IN_WATER, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Axolotl::checkAxolotlSpawnRules, SpawnPlacementRegisterEvent.Operation.REPLACE);
        }

        public static final RegistryObject<EntityType<AxolootlEntity>> AXOLOOTL = ENTITY_TYPES.register("axolootl", () ->
                EntityType.Builder.of(AxolootlEntity::new, MobCategory.AXOLOTLS)
                        .sized(0.75F, 0.42F).clientTrackingRange(10)
                        .build("axolootl"));

    }

    public static final class MenuReg {

        public static void register() {
            MENU_TYPES.register(FMLJavaModLoadingContext.get().getModEventBus());
        }

        public static final RegistryObject<MenuType<ControllerMenu>> CONTROLLER = MENU_TYPES.register("controller", () -> createForgeMenu(ControllerMenu::new));
        public static final RegistryObject<MenuType<AxolootlInterfaceMenu>> AXOLOOTL = MENU_TYPES.register("axolootl", () -> createForgeMenu(AxolootlInterfaceMenu::new));
        public static final RegistryObject<MenuType<AxolootlInspectorMenu>> INSPECTOR = MENU_TYPES.register("inspector", () -> createForgeMenu(AxolootlInspectorMenu::new));
        public static final RegistryObject<MenuType<CyclingContainerMenu>> OUTPUT = MENU_TYPES.register("output", () -> createForgeMenu(CyclingContainerMenu::createOutput));
        public static final RegistryObject<MenuType<CyclingMenu>> ENERGY = MENU_TYPES.register("energy", () -> createForgeMenu(CyclingMenu::createEnergy));
        public static final RegistryObject<MenuType<CyclingContainerMenu>> FLUID = MENU_TYPES.register("fluid", () -> createForgeMenu(CyclingContainerMenu::createFluid));
        public static final RegistryObject<MenuType<CyclingContainerMenu>> LARGE_OUTPUT = MENU_TYPES.register("large_output", () -> createForgeMenu(CyclingContainerMenu::createLargeOutput));
        public static final RegistryObject<MenuType<CyclingContainerMenu>> AUTOFEEDER = MENU_TYPES.register("autofeeder", () -> createForgeMenu(CyclingContainerMenu::createFeeder));
        public static final RegistryObject<MenuType<CyclingContainerMenu>> BREEDER = MENU_TYPES.register("breeder", () -> createForgeMenu(CyclingContainerMenu::createBreeder));
        public static final RegistryObject<MenuType<CyclingContainerMenu>> MONSTERIUM = MENU_TYPES.register("monsterium", () -> createForgeMenu(CyclingContainerMenu::createMonsterium));


        public static Consumer<FriendlyByteBuf> writeControllerMenu(final BlockPos controller, final BlockPos block, final int tab, final int cycle) {
            return buf -> {
              buf.writeBlockPos(controller);
              buf.writeBlockPos(block);
              buf.writeInt(tab);
              buf.writeInt(cycle);
            };
        }

        private static <T extends AbstractContainerMenu> MenuType<T> createForgeMenu(final IControllerMenuConstructor<T> constructor) {
            return IForgeMenuType.create(((windowId, inv, data) -> {
                BlockPos controllerPos = data.readBlockPos();
                BlockPos blockPos = data.readBlockPos();
                int tab = data.readInt();
                int cycle = data.readInt();
                return constructor.create(windowId, inv, controllerPos, (ControllerBlockEntity)inv.player.level.getBlockEntity(controllerPos), blockPos, tab, cycle);
            }));
        }


        @FunctionalInterface
        private static interface IControllerMenuConstructor<T extends AbstractContainerMenu> {
            T create(int windowId, Inventory inv, BlockPos controllerPos, ControllerBlockEntity controller, BlockPos blockPos, int tab, int cycle);
        }

    }

    public static final class RecipeReg {

        public static void register() {
            RECIPE_TYPES.register(FMLJavaModLoadingContext.get().getModEventBus());
            RECIPE_SERIALIZERS.register(FMLJavaModLoadingContext.get().getModEventBus());
        }

    }

    public static final class CapabilityReg {
        public static void register() {
            FMLJavaModLoadingContext.get().getModEventBus().addListener(CapabilityReg::onRegisterCapabilities);
            MinecraftForge.EVENT_BUS.addGenericListener(Entity.class, CapabilityReg::onAttachEntityCapabilities);
            MinecraftForge.EVENT_BUS.addListener(CapabilityReg::onClonePlayer);
            MinecraftForge.EVENT_BUS.addListener(CapabilityReg::onPlayerLoggedIn);
        }

        private static void onRegisterCapabilities(final RegisterCapabilitiesEvent event) {
            event.register(AxolootlResearchCapability.class);
        }


        private static void onAttachEntityCapabilities(final AttachCapabilitiesEvent<Entity> event) {
            if(event.getObject() instanceof Player) {
                event.addCapability(AxolootlResearchCapability.REGISTRY_NAME, AxolootlResearchCapability.provider());
            }
        }

        private static void onClonePlayer(final PlayerEvent.Clone event) {
            cloneCapability(event.getOriginal(), event.getEntity(), Axolootl.AXOLOOTL_RESEARCH_CAPABILITY);
        }

        private static void onPlayerLoggedIn(final PlayerEvent.PlayerLoggedInEvent event) {
            if(event.getEntity() instanceof ServerPlayer serverPlayer) {
                serverPlayer.getCapability(Axolootl.AXOLOOTL_RESEARCH_CAPABILITY).ifPresent(c -> c.syncToClient(serverPlayer));
            }
        }

        private static <C extends Tag, T extends INBTSerializable<C>> void cloneCapability(final Entity original, final Entity clone, final Capability<T> capability) {
            // revive caps
            original.reviveCaps();
            // load capabilities
            Optional<T> originalCap = original.getCapability(capability).resolve();
            Optional<T> cloneCap = clone.getCapability(capability).resolve();
            // clone capability
            if(originalCap.isPresent() && cloneCap.isPresent()) {
                cloneCap.get().deserializeNBT(originalCap.get().serializeNBT());
            }
            // invalidate caps
            original.invalidateCaps();
        }
    }

    public static final class BlockPredicateTypesReg {

        public static void register() {
            BLOCK_PREDICATE_TYPES.register(FMLJavaModLoadingContext.get().getModEventBus());
        }

        public static final RegistryObject<BlockPredicateType<AxMatchingBlocksPredicate>> MATCHING_BLOCKS = BLOCK_PREDICATE_TYPES.register("matching_blocks", () -> () -> AxMatchingBlocksPredicate.CODEC);
        public static final RegistryObject<BlockPredicateType<AxMatchingFluidsPredicate>> MATCHING_FLUIDS = BLOCK_PREDICATE_TYPES.register("matching_fluids", () -> () -> AxMatchingFluidsPredicate.CODEC);
        public static final RegistryObject<BlockPredicateType<MatchingStatePredicate>> MATCHING_STATE = BLOCK_PREDICATE_TYPES.register("matching_state", () -> () -> MatchingStatePredicate.CODEC);
        public static final RegistryObject<BlockPredicateType<NbtPredicate>> MATCHING_TAG = BLOCK_PREDICATE_TYPES.register("matching_tag", () -> () -> NbtPredicate.CODEC);

    }

    public static final class ResourceGeneratorsReg {

        public static void register() {
            RESOURCE_GENERATORS.register(FMLJavaModLoadingContext.get().getModEventBus());
            RESOURCE_GENERATOR_SERIALIZERS.register(FMLJavaModLoadingContext.get().getModEventBus());
        }

        public static final RegistryObject<Codec<? extends ResourceGenerator>> EMPTY = RESOURCE_GENERATOR_SERIALIZERS.register("empty", () -> EmptyResourceGenerator.CODEC);
        public static final RegistryObject<Codec<? extends ResourceGenerator>> ITEM = RESOURCE_GENERATOR_SERIALIZERS.register("item", () -> ItemResourceGenerator.CODEC);
        public static final RegistryObject<Codec<? extends ResourceGenerator>> MOB = RESOURCE_GENERATOR_SERIALIZERS.register("mob", () -> MobDropsResourceGenerator.CODEC);
        public static final RegistryObject<Codec<? extends ResourceGenerator>> BLOCK = RESOURCE_GENERATOR_SERIALIZERS.register("block", () -> BlockDropsResourceGenerator.CODEC);
        public static final RegistryObject<Codec<? extends ResourceGenerator>> TAG = RESOURCE_GENERATOR_SERIALIZERS.register("tag", () -> ItemTagResourceGenerator.CODEC);
        public static final RegistryObject<Codec<? extends ResourceGenerator>> AND = RESOURCE_GENERATOR_SERIALIZERS.register("and", () -> AndResourceGenerator.CODEC);
        public static final RegistryObject<Codec<? extends ResourceGenerator>> SELECT = RESOURCE_GENERATOR_SERIALIZERS.register("select", () -> SelectResourceGenerator.CODEC);
        public static final RegistryObject<Codec<? extends ResourceGenerator>> REFERENCE = RESOURCE_GENERATOR_SERIALIZERS.register("reference", () -> ReferenceResourceGenerator.CODEC);
    }

    public static final class ModifierConditionsReg {

        public static void register() {
            MODIFIER_CONDITIONS.register(FMLJavaModLoadingContext.get().getModEventBus());
            MODIFIER_CONDITION_SERIALIZERS.register(FMLJavaModLoadingContext.get().getModEventBus());
        }

        public static final RegistryObject<Codec<? extends ModifierCondition>> TRUE = MODIFIER_CONDITION_SERIALIZERS.register("true", () -> TrueModifierCondition.CODEC);
        public static final RegistryObject<Codec<? extends ModifierCondition>> FALSE = MODIFIER_CONDITION_SERIALIZERS.register("false", () -> FalseModifierCondition.CODEC);
        public static final RegistryObject<Codec<? extends ModifierCondition>> AND = MODIFIER_CONDITION_SERIALIZERS.register("and", () -> AndModifierCondition.CODEC);
        public static final RegistryObject<Codec<? extends ModifierCondition>> OR = MODIFIER_CONDITION_SERIALIZERS.register("or", () -> OrModifierCondition.CODEC);
        public static final RegistryObject<Codec<? extends ModifierCondition>> NOT = MODIFIER_CONDITION_SERIALIZERS.register("not", () -> NotModifierCondition.CODEC);
        public static final RegistryObject<Codec<? extends ModifierCondition>> COUNT = MODIFIER_CONDITION_SERIALIZERS.register("count", () -> CountModifierCondition.CODEC);
        public static final RegistryObject<Codec<? extends ModifierCondition>> COUNT_CAPPED = MODIFIER_CONDITION_SERIALIZERS.register("count_capped", () -> CountCappedModifierCondition.CODEC);
        public static final RegistryObject<Codec<? extends ModifierCondition>> EXISTS = MODIFIER_CONDITION_SERIALIZERS.register("exists", () -> ExistsModifierCondition.CODEC);
        public static final RegistryObject<Codec<? extends ModifierCondition>> DISTANCE = MODIFIER_CONDITION_SERIALIZERS.register("distance", () -> DistanceModifierCondition.CODEC);
        public static final RegistryObject<Codec<? extends ModifierCondition>> TIME = MODIFIER_CONDITION_SERIALIZERS.register("time", () -> TimeModifierCondition.CODEC);
        public static final RegistryObject<Codec<? extends ModifierCondition>> WEATHER = MODIFIER_CONDITION_SERIALIZERS.register("weather", () -> WeatherModifierCondition.CODEC);
        public static final RegistryObject<Codec<? extends ModifierCondition>> AXOLOOTL_COUNT = MODIFIER_CONDITION_SERIALIZERS.register("axolootl_count", () -> AxolootlCountModifierCondition.CODEC);
        public static final RegistryObject<Codec<? extends ModifierCondition>> LOCATION = MODIFIER_CONDITION_SERIALIZERS.register("location", () -> LocationModifierCondition.CODEC);
        public static final RegistryObject<Codec<? extends ModifierCondition>> CHANCE = MODIFIER_CONDITION_SERIALIZERS.register("chance", () -> RandomChanceModifierCondition.CODEC);
        public static final RegistryObject<Codec<? extends ModifierCondition>> BLOCK = MODIFIER_CONDITION_SERIALIZERS.register("block", () -> BlockModifierCondition.CODEC);
        public static final RegistryObject<Codec<? extends ModifierCondition>> ENERGY = MODIFIER_CONDITION_SERIALIZERS.register("energy", () -> EnergyModifierCondition.CODEC);

    }


    public static final class ForgeConditionsReg {

        public static void register() {
            FORGE_CONDITIONS.register(FMLJavaModLoadingContext.get().getModEventBus());
            FORGE_CONDITION_SERIALIZERS.register(FMLJavaModLoadingContext.get().getModEventBus());
        }

        public static final RegistryObject<Codec<? extends ForgeCondition>> TRUE = FORGE_CONDITION_SERIALIZERS.register("true", () -> TrueForgeCondition.CODEC);
        public static final RegistryObject<Codec<? extends ForgeCondition>> FALSE = FORGE_CONDITION_SERIALIZERS.register("false", () -> FalseForgeCondition.CODEC);
        public static final RegistryObject<Codec<? extends ForgeCondition>> AND = FORGE_CONDITION_SERIALIZERS.register("and", () -> AndForgeCondition.CODEC);
        public static final RegistryObject<Codec<? extends ForgeCondition>> OR = FORGE_CONDITION_SERIALIZERS.register("or", () -> OrForgeCondition.CODEC);
        public static final RegistryObject<Codec<? extends ForgeCondition>> NOT = FORGE_CONDITION_SERIALIZERS.register("not", () -> NotForgeCondition.CODEC);
        public static final RegistryObject<Codec<? extends ForgeCondition>> MOD_LOADED = FORGE_CONDITION_SERIALIZERS.register("mod_loaded", () -> ModLoadedForgeCondition.CODEC);
        public static final RegistryObject<Codec<? extends ForgeCondition>> TAG_EMPTY = FORGE_CONDITION_SERIALIZERS.register("tag_empty", () -> TagEmptyForgeCondition.CODEC);
        public static final RegistryObject<Codec<? extends ForgeCondition>> ITEM_EXISTS = FORGE_CONDITION_SERIALIZERS.register("item_exists", () -> ItemExistsForgeCondition.CODEC);

    }

    public static final class AquariumModifiersReg {

        private static final Set<TagKey<AquariumModifier>> MANDATORY_AQUARIUM_MODIFIERS = new HashSet<>();
        private static final Set<TagKey<AquariumModifier>> MANDATORY_AQUARIUM_MODIFIERS_CLIENT = new HashSet<>();
        private static final String MANDATORY_PREFIX = "mandatory";

        public static void register() {
            AQUARIUM_MODIFIERS.register(FMLJavaModLoadingContext.get().getModEventBus());
        }

        public static Set<TagKey<AquariumModifier>> getMandatoryAquariumModifiers(final RegistryAccess registryAccess) {
            final Set<TagKey<AquariumModifier>> set = EffectiveSide.get().isClient() ? MANDATORY_AQUARIUM_MODIFIERS_CLIENT : MANDATORY_AQUARIUM_MODIFIERS;
            if(set.isEmpty()) {
                // collect all aquarium modifiers that are in the mandatory folder
                set.addAll(AquariumModifier.getRegistry(registryAccess).getTags()
                            .filter(pair -> pair.getFirst().location().getPath().startsWith(MANDATORY_PREFIX + "/") && pair.getSecond().size() > 0)
                            .map(Pair::getFirst)
                            .toList()
                );
            }
            return Collections.unmodifiableSet(set);
        }

        private static void clearCache() {
            if(EffectiveSide.get().isClient()) {
                MANDATORY_AQUARIUM_MODIFIERS_CLIENT.clear();
            } else {
                MANDATORY_AQUARIUM_MODIFIERS.clear();
            }
        }
    }

    public static final class AquariumTabsReg {

        private static final List<IAquariumTab> SORTED_TABS = new ArrayList<>();
        private static final List<IAquariumTab> SORTED_TABS_CLIENT = new ArrayList<>();

        public static void register() {
            AQUARIUM_TABS.register(FMLJavaModLoadingContext.get().getModEventBus());
        }

        public static final RegistryObject<IAquariumTab> CONTROLLER = AQUARIUM_TABS.register("controller", () ->
                AquariumTab.builder()
                        .available(c -> true)
                        .accepts(b -> b.is(BlockReg.AQUARIUM_CONTROLLER.get()))
                        .menuProvider(c -> new WorldlyMenuProvider(c.getBlockPos(), c))
                        .icon(() -> Items.CONDUIT.getDefaultInstance())
                        .build());

        public static final RegistryObject<IAquariumTab> AXOLOOTL_INTERFACE = AQUARIUM_TABS.register("axolootl_interface", () ->
                AquariumTab.builder()
                        .available(c -> !c.getAxolootlInputs().isEmpty())
                        .accepts(ForgeRegistries.BLOCKS.tags().createTagKey(new ResourceLocation(Axolootl.MODID, "aquarium_axolootl_interface")))
                        .menuProvider(c -> IAquariumTab.getFirstMenuProvider(c.getLevel(), c.getAxolootlInputs()))
                        .icon(() -> Items.AXOLOTL_BUCKET.getDefaultInstance())
                        .before(() -> List.of(AquariumTabsReg.CONTROLLER.get()))
                        .after(() -> List.of(AquariumTabsReg.OUTPUT.get()))
                        .build());

        public static final RegistryObject<IAquariumTab> OUTPUT = AQUARIUM_TABS.register("output", () ->
                AquariumTab.builder()
                        .available(c -> !c.getResourceOutputs().isEmpty())
                        .accepts(ForgeRegistries.BLOCKS.tags().createTagKey(new ResourceLocation(Axolootl.MODID, "aquarium_output")))
                        .menuProvider(c -> IAquariumTab.getFirstMenuProvider(c.getLevel(), c.getResourceOutputs()))
                        .icon(() -> Items.CHEST.getDefaultInstance())
                        .before(() -> List.of(AquariumTabsReg.AXOLOOTL_INTERFACE.get()))
                        .after(() -> List.of(AquariumTabsReg.FOOD_INTERFACE.get()))
                        .build());

        private static final TagKey<Block> FOOD_INTERFACE_TAG_KEY = ForgeRegistries.BLOCKS.tags().createTagKey(new ResourceLocation(Axolootl.MODID, "aquarium_food_interface"));
        public static final RegistryObject<IAquariumTab> FOOD_INTERFACE = AQUARIUM_TABS.register("food_interface", () ->
                AquariumTab.builder()
                        .available(c -> !c.resolveModifiers(c.getLevel().registryAccess(), createFoodInterfacePredicate(c)).isEmpty())
                        .accepts(FOOD_INTERFACE_TAG_KEY)
                        .menuProvider(c -> IAquariumTab.getFirstMenuProvider(c.getLevel(), c.resolveModifiers(c.getLevel().registryAccess(), createFoodInterfacePredicate(c)).keySet()))
                        .icon(() -> Items.TROPICAL_FISH.getDefaultInstance())
                        .before(() -> List.of(AquariumTabsReg.OUTPUT.get()))
                        .after(() -> List.of(AquariumTabsReg.FLUID_INTERFACE.get()))
                        .build());

        public static final RegistryObject<IAquariumTab> FLUID_INTERFACE = AQUARIUM_TABS.register("fluid_interface", () ->
                AquariumTab.builder()
                        .available(c -> !c.getFluidInputs().isEmpty())
                        .accepts(ForgeRegistries.BLOCKS.tags().createTagKey(new ResourceLocation(Axolootl.MODID, "aquarium_fluid_interface")))
                        .menuProvider(c -> IAquariumTab.getFirstMenuProvider(c.getLevel(), c.getFluidInputs()))
                        .icon(() -> Items.WATER_BUCKET.getDefaultInstance())
                        .before(() -> List.of(AquariumTabsReg.FOOD_INTERFACE.get()))
                        .after(() -> List.of(AquariumTabsReg.ENERGY_INTERFACE.get()))
                        .build());

        public static final RegistryObject<IAquariumTab> ENERGY_INTERFACE = AQUARIUM_TABS.register("energy_interface", () ->
                AquariumTab.builder()
                        .available(c -> !c.getEnergyInputs().isEmpty())
                        .accepts(ForgeRegistries.BLOCKS.tags().createTagKey(new ResourceLocation(Axolootl.MODID, "aquarium_energy_interface")))
                        .menuProvider(c -> IAquariumTab.getFirstMenuProvider(c.getLevel(), c.getEnergyInputs()))
                        .icon(() -> Items.REDSTONE.getDefaultInstance())
                        .before(() -> List.of(AquariumTabsReg.FLUID_INTERFACE.get()))
                        .after(() -> List.of(AquariumTabsReg.AXOLOOTL_INSPECTOR.get()))
                        .build());

        public static final RegistryObject<IAquariumTab> AXOLOOTL_INSPECTOR = AQUARIUM_TABS.register("axolootl_inspector", () ->
                AquariumTab.builder()
                        .available(c -> !c.getTrackedBlocks(AquariumTabsReg.AXOLOOTL_INSPECTOR.getId()).isEmpty())
                        .accepts(ForgeRegistries.BLOCKS.tags().createTagKey(new ResourceLocation(Axolootl.MODID, "aquarium_axolootl_inspector")))
                        .menuProvider(c -> IAquariumTab.getFirstMenuProvider(c.getLevel(), c.getTrackedBlocks(AquariumTabsReg.AXOLOOTL_INSPECTOR.getId())))
                        .icon(() -> Items.SPYGLASS.getDefaultInstance())
                        .before(() -> List.of(AquariumTabsReg.ENERGY_INTERFACE.get()))
                        .build());

        /**
         * @return an immutable view of the sorted tab list
         */
        public static List<IAquariumTab> getSortedTabs() {
            final List<IAquariumTab> list = EffectiveSide.get().isClient() ? SORTED_TABS_CLIENT : SORTED_TABS;
            if(list.isEmpty()) {
                list.addAll(ControllerTabSorter.recalculateSortedTabs());
            }
            return Collections.unmodifiableList(list);
        }

        /**
         * @return the total number of registered tabs
         */
        public static int getTabCount() {
            return AxRegistry.AQUARIUM_TABS.getEntries().size();
        }

        private static void clearCache() {
            if(EffectiveSide.get().isClient()) {
                SORTED_TABS_CLIENT.clear();
            } else {
                SORTED_TABS.clear();
            }
        }

        private static BiPredicate<BlockPos, AquariumModifier> createFoodInterfacePredicate(final ControllerBlockEntity blockEntity) {
            return blockEntity.activePredicate.and((b, a) -> blockEntity.getLevel().getBlockState(b).is(FOOD_INTERFACE_TAG_KEY));
        }
    }

    public static final class AxolootlVariantsReg {

        public static void register() {
            AXOLOOTL_VARIANTS.register(FMLJavaModLoadingContext.get().getModEventBus());
        }

        private static final Set<ResourceLocation> INVALID = new HashSet<>();
        private static final Set<ResourceLocation> INVALID_CLIENT = new HashSet<>();

        public static void validate(final RegistryAccess access) {
            final Set<ResourceLocation> set = EffectiveSide.get().isClient() ? INVALID_CLIENT : INVALID;
            set.clear();
            // create context
            final ForgeConditionContext context = new ForgeConditionContext(access);
            // iterate registry
            for(Map.Entry<ResourceKey<AxolootlVariant>, AxolootlVariant> entry : AxolootlVariant.getRegistry(access).entrySet()) {
                // test each entry
                if(!entry.getValue().getCondition().test(context)) {
                    set.add(entry.getKey().location());
                }
            }
        }

        public static boolean isValid(final ResourceLocation id) {
            return !getInvalidEntries().contains(id);
        }

        public static boolean isValid(final RegistryAccess access, final AxolootlVariant variant) {
            return variant != AxolootlVariant.EMPTY && isValid(variant.getRegistryName(access));
        }

        public static Set<ResourceLocation> getInvalidEntries() {
            if(EffectiveSide.get().isClient()) {
                return Collections.unmodifiableSet(INVALID_CLIENT);
            } else {
                return Collections.unmodifiableSet(INVALID);
            }
        }

        private static void clearCache() {
            if(EffectiveSide.get().isClient()) {
                INVALID_CLIENT.clear();
            } else {
                INVALID.clear();
            }
        }
    }

    public static final class AxolootlBreedingReg {

        private static final Map<ResourceLocation, AxolootlBreedingWrapper> WRAPPERS = new HashMap<>();
        private static final Map<ResourceLocation, AxolootlBreedingWrapper> WRAPPERS_CLIENT = new HashMap<>();

        public static void register() {
            AXOLOOTL_BREEDING.register(FMLJavaModLoadingContext.get().getModEventBus());
        }

        public static AxolootlBreedingWrapper getWrapper(final RegistryAccess access, final AxolootlBreeding entry) {
            // load registry
            final Registry<AxolootlBreeding> breedingRegistry = AxolootlBreeding.getRegistry(access);
            final ResourceLocation key = breedingRegistry.getKey(entry);
            if(null == key) {
                throw new IllegalArgumentException("Attempted to create AxolootlBreedingWrapper for unregistered AxolootlBreeding object " + entry.toString());
            }
            final Map<ResourceLocation, AxolootlBreedingWrapper> map = EffectiveSide.get().isClient() ? WRAPPERS_CLIENT : WRAPPERS;
            // create value if it does not exist
            if(!map.containsKey(key)) {
                // collect modifiers
                final List<AxolootlBreedingModifier> modifiers = AxolootlBreedingModifier.getRegistry(access)
                        .stream()
                        .filter(modifier -> key.equals(modifier.getTarget()))
                        .toList();
                // add wrapper to map
                map.put(key, new AxolootlBreedingWrapper(access, entry, modifiers));
            }
            return map.get(key);
        }

        private static void clearCache() {
            if(EffectiveSide.get().isClient()) {
                WRAPPERS_CLIENT.clear();
            } else {
                WRAPPERS.clear();
            }
        }
    }

    public static final class AxolootlBreedingModifierReg {

        public static void register() {
            AXOLOOTL_BREEDING_MODIFIERS_SERIALIZERS.register(FMLJavaModLoadingContext.get().getModEventBus());
            AXOLOOTL_BREEDING_MODIFIERS.register(FMLJavaModLoadingContext.get().getModEventBus());
        }

        public static final RegistryObject<Codec<? extends AxolootlBreedingModifier>> ADD = AXOLOOTL_BREEDING_MODIFIERS_SERIALIZERS.register("add", () -> AddAxolootlBreedingModifier.CODEC);
        public static final RegistryObject<Codec<? extends AxolootlBreedingModifier>> REMOVE = AXOLOOTL_BREEDING_MODIFIERS_SERIALIZERS.register("remove", () -> RemoveAxolootlBreedingModifier.CODEC);
        public static final RegistryObject<Codec<? extends AxolootlBreedingModifier>> SEQUENCE = AXOLOOTL_BREEDING_MODIFIERS_SERIALIZERS.register("sequence", () -> SequenceAxolootlBreedingModifier.CODEC);
    }

    public static final class Keys {
        public static final ResourceKey<Registry<AxolootlVariant>> AXOLOOTL_VARIANTS = ResourceKey.createRegistryKey(new ResourceLocation(Axolootl.MODID, "axolootl_variants"));
        public static final ResourceKey<Registry<AxolootlBreeding>> AXOLOOTL_BREEDING = ResourceKey.createRegistryKey(new ResourceLocation(Axolootl.MODID, "breeding"));
        public static final ResourceKey<Registry<Codec<? extends AxolootlBreedingModifier>>> AXOLOOTL_BREEDING_MODIFIER_SERIALIZERS = ResourceKey.createRegistryKey(new ResourceLocation(Axolootl.MODID, "breeding_modifier_serializers"));
        public static final ResourceKey<Registry<AxolootlBreedingModifier>> AXOLOOTL_BREEDING_MODIFIERS = ResourceKey.createRegistryKey(new ResourceLocation(Axolootl.MODID, "breeding_modifiers"));
        public static final ResourceKey<Registry<AquariumModifier>> AQUARIUM_MODIFIERS = ResourceKey.createRegistryKey(new ResourceLocation(Axolootl.MODID, "aquarium_modifiers"));
        public static final ResourceKey<Registry<IAquariumTab>> AQUARIUM_TABS = ResourceKey.createRegistryKey(new ResourceLocation(Axolootl.MODID, "aquarium_tabs"));
        public static final ResourceKey<Registry<Codec<? extends ResourceGenerator>>> RESOURCE_GENERATOR_SERIALIZERS = ResourceKey.createRegistryKey(new ResourceLocation(Axolootl.MODID, "resource_generator_serializers"));
        public static final ResourceKey<Registry<ResourceGenerator>> RESOURCE_GENERATORS = ResourceKey.createRegistryKey(new ResourceLocation(Axolootl.MODID, "resource_generators"));
        public static final ResourceKey<Registry<Codec<? extends ModifierCondition>>> MODIFIER_CONDITION_SERIALIZERS = ResourceKey.createRegistryKey(new ResourceLocation(Axolootl.MODID, "modifier_condition_serializers"));
        public static final ResourceKey<Registry<ModifierCondition>> MODIFIER_CONDITIONS = ResourceKey.createRegistryKey(new ResourceLocation(Axolootl.MODID, "modifier_conditions"));
        public static final ResourceKey<Registry<Codec<? extends ForgeCondition>>> FORGE_CONDITION_SERIALIZERS = ResourceKey.createRegistryKey(new ResourceLocation(Axolootl.MODID, "variant_condition_serializers"));
        public static final ResourceKey<Registry<ForgeCondition>> FORGE_CONDITIONS = ResourceKey.createRegistryKey(new ResourceLocation(Axolootl.MODID, "variant_conditions"));

    }
}
