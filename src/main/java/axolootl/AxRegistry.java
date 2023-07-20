package axolootl;

import axolootl.block.AirlockBlock;
import axolootl.block.AutofeederBlock;
import axolootl.block.AxolootlInterfaceBlock;
import axolootl.block.BreederBlock;
import axolootl.block.BubblerBlock;
import axolootl.block.ControllerBlock;
import axolootl.block.EnergyInterfaceBlock;
import axolootl.block.GrandCastleBlock;
import axolootl.block.MonsteriumBlock;
import axolootl.block.OutputBlock;
import axolootl.block.WaterInterfaceBlock;
import axolootl.block.WaterloggedHorizontalBlock;
import axolootl.block.WaterloggedHorizontalDoubleBlock;
import axolootl.block.WaterloggedHorizontalMultiBlock;
import axolootl.block.entity.AutoFeederBlockEntity;
import axolootl.block.entity.AxolootlInterfaceBlockEntity;
import axolootl.block.entity.BreederBlockEntity;
import axolootl.block.entity.ControllerBlockEntity;
import axolootl.block.entity.EnergyInterfaceBlockEntity;
import axolootl.block.entity.MonsteriumBlockEntity;
import axolootl.block.entity.OutputInterfaceBlockEntity;
import axolootl.block.entity.WaterInterfaceBlockEntity;
import axolootl.data.AxolootlBreeding;
import axolootl.data.aquarium_modifier.AquariumModifier;
import axolootl.data.AxolootlVariant;
import axolootl.data.aquarium_modifier.condition.AndModifierCondition;
import axolootl.data.aquarium_modifier.condition.AxolootlCountModifierCondition;
import axolootl.data.aquarium_modifier.condition.BlockModifierCondition;
import axolootl.data.aquarium_modifier.condition.CountCappedModifierCondition;
import axolootl.data.aquarium_modifier.condition.CountModifierCondition;
import axolootl.data.aquarium_modifier.condition.DistanceModifierCondition;
import axolootl.data.aquarium_modifier.condition.EnergyModifierCondition;
import axolootl.data.aquarium_modifier.condition.ExistsModifierCondition;
import axolootl.data.aquarium_modifier.condition.FalseModifierCondition;
import axolootl.data.aquarium_modifier.condition.LocationModifierCondition;
import axolootl.data.aquarium_modifier.condition.ModifierCondition;
import axolootl.data.aquarium_modifier.condition.NotModifierCondition;
import axolootl.data.aquarium_modifier.condition.OrModifierCondition;
import axolootl.data.aquarium_modifier.condition.RandomChanceModifierCondition;
import axolootl.data.aquarium_modifier.condition.TimeModifierCondition;
import axolootl.data.aquarium_modifier.condition.TrueModifierCondition;
import axolootl.data.aquarium_modifier.condition.WeatherModifierCondition;
import axolootl.data.resource_generator.AndResourceGenerator;
import axolootl.data.resource_generator.BlockDropsResourceGenerator;
import axolootl.data.resource_generator.EmptyResourceGenerator;
import axolootl.data.resource_generator.ItemResourceGenerator;
import axolootl.data.resource_generator.ItemTagResourceGenerator;
import axolootl.data.resource_generator.MobDropsResourceGenerator;
import axolootl.data.resource_generator.ResourceGenerator;
import axolootl.entity.AxolootlEntity;
import axolootl.item.AxolootlBucketItem;
import axolootl.item.MultiBlockItem;
import axolootl.menu.ControllerMenu;
import axolootl.util.MatchingStatePredicate;
import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.animal.axolotl.Axolotl;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicateType;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.material.Material;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.entity.SpawnPlacementRegisterEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.RegistryBuilder;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
    private static final DeferredRegister<BlockPredicateType<?>> BLOCK_PREDICATE_TYPES = DeferredRegister.create(Registry.BLOCK_PREDICATE_TYPE_REGISTRY, Axolootl.MODID);

    // RESOURCE GENERATORS //
    private static final DeferredRegister<Codec<? extends ResourceGenerator>> RESOURCE_GENERATOR_SERIALIZERS = DeferredRegister.create(Keys.RESOURCE_GENERATOR_SERIALIZERS, Axolootl.MODID);
    public static final Supplier<IForgeRegistry<Codec<? extends ResourceGenerator>>> RESOURCE_GENERATOR_SERIALIZERS_SUPPLIER =
            RESOURCE_GENERATOR_SERIALIZERS.makeRegistry(() -> new RegistryBuilder<Codec<? extends ResourceGenerator>>().hasTags());

    private static final DeferredRegister<ResourceGenerator> RESOURCE_GENERATORS = DeferredRegister.create(Keys.RESOURCE_GENERATORS, Axolootl.MODID);
    public static final Supplier<IForgeRegistry<ResourceGenerator>> RESOURCE_GENERATORS_SUPPLIER = RESOURCE_GENERATORS.makeRegistry(() -> new RegistryBuilder<ResourceGenerator>()
            .dataPackRegistry(ResourceGenerator.DIRECT_CODEC, ResourceGenerator.DIRECT_CODEC)
            .hasTags());

    // MODIFIER CONDITIONS //
    private static final DeferredRegister<Codec<? extends ModifierCondition>> MODIFIER_CONDITION_SERIALIZERS = DeferredRegister.create(Keys.MODIFIER_CONDITION_SERIALIZERS, Axolootl.MODID);
    public static final Supplier<IForgeRegistry<Codec<? extends ModifierCondition>>> MODIFIER_CONDITION_SERIALIZERS_SUPPLIER =
            MODIFIER_CONDITION_SERIALIZERS.makeRegistry(() -> new RegistryBuilder<Codec<? extends ModifierCondition>>().hasTags());

    private static final DeferredRegister<ModifierCondition> MODIFIER_CONDITIONS = DeferredRegister.create(Keys.MODIFIER_CONDITIONS, Axolootl.MODID);
    public static final Supplier<IForgeRegistry<ModifierCondition>> MODIFIER_CONDITION_SUPPLIER = MODIFIER_CONDITIONS.makeRegistry(() -> new RegistryBuilder<ModifierCondition>()
            .dataPackRegistry(ModifierCondition.DIRECT_CODEC, ModifierCondition.DIRECT_CODEC)
            .hasTags());

    // AXOLOOTL VARIANTS //
    private static final DeferredRegister<AxolootlVariant> AXOLOOTL_VARIANTS = DeferredRegister.create(Keys.AXOLOOTL_VARIANTS, Axolootl.MODID);
    public static final Supplier<IForgeRegistry<AxolootlVariant>> AXOLOOTL_VARIANTS_SUPPLIER = AXOLOOTL_VARIANTS.makeRegistry(() -> new RegistryBuilder<AxolootlVariant>()
            .dataPackRegistry(AxolootlVariant.CODEC, AxolootlVariant.CODEC)
            .hasTags());

    // AXOLOOTL BREEDING //
    private static final DeferredRegister<AxolootlBreeding> AXOLOOTL_BREEDING = DeferredRegister.create(Keys.AXOLOOTL_BREEDING, Axolootl.MODID);
    public static final Supplier<IForgeRegistry<AxolootlBreeding>> AXOLOOTL_BREEDING_SUPPLIER = AXOLOOTL_BREEDING.makeRegistry(() -> new RegistryBuilder<AxolootlBreeding>()
            .dataPackRegistry(AxolootlBreeding.CODEC, AxolootlBreeding.CODEC)
            .hasTags());

    // AQUARIUM MODIFIERS //
    private static final DeferredRegister<AquariumModifier> AQUARIUM_MODIFIERS = DeferredRegister.create(Keys.AQUARIUM_MODIFIERS, Axolootl.MODID);
    public static final Supplier<IForgeRegistry<AquariumModifier>> AQUARIUM_MODIFIERS_SUPPLIER = AQUARIUM_MODIFIERS.makeRegistry(() -> new RegistryBuilder<AquariumModifier>()
            .dataPackRegistry(AquariumModifier.CODEC, AquariumModifier.CODEC)
            .onClear((owner, stage) -> AxRegistry.MANDATORY_AQUARIUM_MODIFIERS.clear())
            .hasTags());

    public static void register() {
        BlockReg.register();
        ItemReg.register();
        BlockEntityReg.register();
        EntityReg.register();
        MenuReg.register();
        RecipeReg.register();
        BlockPredicateTypesReg.register();
        ModifierConditionsReg.register();
        ResourceGeneratorsReg.register();
        AxolootlVariantsReg.register();
        AquariumModifiersReg.register();
        AxolootlBreedingReg.register();
        FMLJavaModLoadingContext.get().getModEventBus().addListener(AxRegistry::onCommonSetup);
    }

    //// HELPERS ////

    private static final Set<TagKey<AquariumModifier>> MANDATORY_AQUARIUM_MODIFIERS = new HashSet<>();

    public static Set<TagKey<AquariumModifier>> getMandatoryAquariumModifiers(final RegistryAccess registryAccess) {
        if(MANDATORY_AQUARIUM_MODIFIERS.isEmpty()) {
            // attempt to load mandatory aquarium modifiers
            for(AquariumModifier entry : AquariumModifier.getRegistry(registryAccess)) {
                entry.getSettings().getCategory().ifPresent(tagKey -> AxRegistry.MANDATORY_AQUARIUM_MODIFIERS.add(tagKey));
            }
        }
        return MANDATORY_AQUARIUM_MODIFIERS;
    }

    private static void onCommonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> EntityDataSerializers.registerSerializer(AxolootlEntity.OPTIONAL_RESOURCE_LOCATION));
    }

    //// REGISTRY CLASSES ////

    public static final class ItemReg {

        public static final CreativeModeTab TAB = new CreativeModeTab(Axolootl.MODID) {
            @Override
            public ItemStack makeIcon() {
                return Items.AXOLOTL_BUCKET.getDefaultInstance();
            }
        };

        private static final List<RegistryObject<Item>> SPAWN_EGGS = new ArrayList<>();

        public static void register() {
            ITEMS.register(FMLJavaModLoadingContext.get().getModEventBus());
        }

        public static final RegistryObject<Item> AXOLOOTL_BUCKET = ITEMS.register("axolootl_bucket", () ->
                new AxolootlBucketItem(EntityReg.AXOLOOTL, () -> Fluids.WATER, () -> SoundEvents.BUCKET_EMPTY_AXOLOTL, new Item.Properties().stacksTo(1).craftRemainder(Items.BUCKET).tab(TAB)));

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

        public static final RegistryObject<Block> AQUARIUM_CONTROLLER = registerWithItem("aquarium_controller", () -> new ControllerBlock(BlockBehaviour.Properties.of(Material.METAL).requiresCorrectToolForDrops().strength(3.5F, 8.0F)));
        public static final RegistryObject<Block> AQUARIUM_WATER_INTERFACE = registerWithItem("aquarium_water_interface", () -> new WaterInterfaceBlock(BlockBehaviour.Properties.of(Material.METAL).requiresCorrectToolForDrops().strength(3.5F, 8.0F)));
        public static final RegistryObject<Block> AQUARIUM_ENERGY_INTERFACE = registerWithItem("aquarium_energy_interface", () -> new EnergyInterfaceBlock(BlockBehaviour.Properties.of(Material.METAL).requiresCorrectToolForDrops().strength(3.5F, 8.0F)));
        public static final RegistryObject<Block> AQUARIUM_AXOLOOTL_INTERFACE = registerWithItem("aquarium_axolootl_interface", () -> new AxolootlInterfaceBlock(BlockBehaviour.Properties.of(Material.METAL).requiresCorrectToolForDrops().strength(3.5F, 8.0F)));
        public static final RegistryObject<Block> AQUARIUM_OUTPUT = registerWithItem("aquarium_output", () -> new OutputBlock(3, BlockBehaviour.Properties.of(Material.METAL).requiresCorrectToolForDrops().strength(3.5F, 8.0F)));
        public static final RegistryObject<Block> LARGE_AQUARIUM_OUTPUT = registerWithItem("large_aquarium_output", () -> new OutputBlock(6, BlockBehaviour.Properties.of(Material.METAL).requiresCorrectToolForDrops().strength(3.5F, 8.0F)));
        public static final RegistryObject<Block> AQUARIUM_AIRLOCK = registerWithItem("aquarium_airlock", () -> new AirlockBlock(BlockBehaviour.Properties.of(Material.METAL).noOcclusion().isSuffocating((s, l, p) -> false).requiresCorrectToolForDrops().strength(3.5F, 6.0F)));
        public static final RegistryObject<Block> BUBBLER = registerWithItem("bubbler", () -> new BubblerBlock(BlockBehaviour.Properties.of(Material.METAL).requiresCorrectToolForDrops().strength(3.5F)));
        public static final RegistryObject<Block> POWERED_BUBBLER = registerWithItem("powered_bubbler", () -> new BubblerBlock(BlockBehaviour.Properties.of(Material.METAL).requiresCorrectToolForDrops().strength(3.5F)));
        public static final RegistryObject<Block> PUMP = registerWithItem("pump", () -> new WaterloggedHorizontalDoubleBlock(BlockBehaviour.Properties.of(Material.METAL).requiresCorrectToolForDrops().strength(3.5F)));
        public static final RegistryObject<Block> POWERED_PUMP = registerWithItem("powered_pump", () -> new WaterloggedHorizontalDoubleBlock(BlockBehaviour.Properties.of(Material.METAL).requiresCorrectToolForDrops().strength(3.5F)));
        public static final RegistryObject<Block> BREEDER = registerWithItem("breeder", () -> new BreederBlock(BlockBehaviour.Properties.of(Material.METAL).requiresCorrectToolForDrops().strength(3.5F)));
        public static final RegistryObject<Block> NURSERY = registerWithItem("nursery", () -> new WaterloggedHorizontalBlock(BlockBehaviour.Properties.of(Material.METAL).requiresCorrectToolForDrops().strength(3.5F)));
        public static final RegistryObject<Block> MONSTERIUM = registerWithItem("monsterium", () -> new MonsteriumBlock(BlockBehaviour.Properties.of(Material.METAL).requiresCorrectToolForDrops().strength(3.5F)));
        public static final RegistryObject<Block> AUTOFEEDER = registerWithItem("autofeeder", () -> new AutofeederBlock(BlockBehaviour.Properties.of(Material.METAL).requiresCorrectToolForDrops().strength(3.5F)));
        public static final RegistryObject<Block> CASTLE = registerWithItem("castle", () -> new WaterloggedHorizontalBlock(BlockBehaviour.Properties.of(Material.STONE).requiresCorrectToolForDrops().strength(3.5F, 5.0F)));
        public static final RegistryObject<Block> GRAND_CASTLE = registerWithMultiBlockItem("grand_castle", () -> new GrandCastleBlock(BlockBehaviour.Properties.of(Material.STONE).noOcclusion().requiresCorrectToolForDrops().strength(5.0F, 20.0F)));
        public static final RegistryObject<Block> BASTION = registerWithMultiBlockItem("bastion", () -> new WaterloggedHorizontalMultiBlock(BlockBehaviour.Properties.of(Material.STONE).noOcclusion().requiresCorrectToolForDrops().strength(5.0F, 60.0F)));
        public static final RegistryObject<Block> PRISTINE_BASTION = registerWithMultiBlockItem("pristine_bastion", () -> new WaterloggedHorizontalMultiBlock(BlockBehaviour.Properties.of(Material.STONE).noOcclusion().requiresCorrectToolForDrops().strength(5.0F, 60.0F)));
        // TODO end city and end ship blocks

        private static RegistryObject<Block> registerWithItem(final String name, final Supplier<Block> supplier) {
            return registerWithItem(name, supplier, ItemReg::registerBlockItem);
        }

        private static RegistryObject<Block> registerWithItem(final String name, final Supplier<Block> blockSupplier, final Function<RegistryObject<Block>, RegistryObject<Item>> itemSupplier) {
            final RegistryObject<Block> block = BLOCKS.register(name, blockSupplier);
            final RegistryObject<Item> item = itemSupplier.apply(block);
            return block;
        }

        private static RegistryObject<Block> registerWithMultiBlockItem(final String name, final Supplier<Block> supplier) {
            return registerWithItem(name, supplier, block -> ItemReg.register(block.getId().getPath(), () -> new MultiBlockItem(block.get(), new Item.Properties().stacksTo(1).tab(ItemReg.TAB))));
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

        public static final RegistryObject<MenuType<ControllerMenu>> CONTROLLER = MENU_TYPES.register("controller", () ->
                IForgeMenuType.create((windowId, inv, data) -> {
                    BlockPos pos = data.readBlockPos();
                    int tab = data.readInt();
                    return new ControllerMenu(windowId, inv, pos, (ControllerBlockEntity)inv.player.level.getBlockEntity(pos), tab);
                })
        );

    }

    public static final class RecipeReg {

        public static void register() {
            RECIPE_TYPES.register(FMLJavaModLoadingContext.get().getModEventBus());
            RECIPE_SERIALIZERS.register(FMLJavaModLoadingContext.get().getModEventBus());
        }

    }

    public static final class BlockPredicateTypesReg {

        public static void register() {
            BLOCK_PREDICATE_TYPES.register(FMLJavaModLoadingContext.get().getModEventBus());
        }

        public static final RegistryObject<BlockPredicateType<MatchingStatePredicate>> MATCHING_PROPERTY = BLOCK_PREDICATE_TYPES.register("matching_state", () -> () -> MatchingStatePredicate.CODEC);

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

    public static final class AquariumModifiersReg {

        public static void register() {
            AQUARIUM_MODIFIERS.register(FMLJavaModLoadingContext.get().getModEventBus());
        }

    }

    public static final class AxolootlVariantsReg {

        public static void register() {
            AXOLOOTL_VARIANTS.register(FMLJavaModLoadingContext.get().getModEventBus());
        }

    }

    public static final class AxolootlBreedingReg {

        public static void register() {
            AXOLOOTL_BREEDING.register(FMLJavaModLoadingContext.get().getModEventBus());
        }

    }

    public static final class Keys {
        public static final ResourceKey<Registry<AxolootlVariant>> AXOLOOTL_VARIANTS = ResourceKey.createRegistryKey(new ResourceLocation(Axolootl.MODID, "axolootl_variants"));
        public static final ResourceKey<Registry<AxolootlBreeding>> AXOLOOTL_BREEDING = ResourceKey.createRegistryKey(new ResourceLocation(Axolootl.MODID, "breeding"));
        public static final ResourceKey<Registry<AquariumModifier>> AQUARIUM_MODIFIERS = ResourceKey.createRegistryKey(new ResourceLocation(Axolootl.MODID, "aquarium_modifiers"));
        public static final ResourceKey<Registry<Codec<? extends ResourceGenerator>>> RESOURCE_GENERATOR_SERIALIZERS = ResourceKey.createRegistryKey(new ResourceLocation(Axolootl.MODID, "resource_generator_serializers"));
        public static final ResourceKey<? extends Registry<ResourceGenerator>> RESOURCE_GENERATORS = ResourceKey.createRegistryKey(new ResourceLocation(Axolootl.MODID, "resource_generators"));
        public static final ResourceKey<Registry<Codec<? extends ModifierCondition>>> MODIFIER_CONDITION_SERIALIZERS = ResourceKey.createRegistryKey(new ResourceLocation(Axolootl.MODID, "modifier_condition_serializers"));
        public static final ResourceKey<? extends Registry<ModifierCondition>> MODIFIER_CONDITIONS = ResourceKey.createRegistryKey(new ResourceLocation(Axolootl.MODID, "modifier_conditions"));

    }
}
