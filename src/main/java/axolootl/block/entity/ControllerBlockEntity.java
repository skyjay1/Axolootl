/**
 * Copyright (c) 2023 Skyler James
 * Permission is granted to use, modify, and redistribute this software, in parts or in whole,
 * under the GNU LGPLv3 license (https://www.gnu.org/licenses/lgpl-3.0.en.html)
 **/

package axolootl.block.entity;

import axolootl.AxRegistry;
import axolootl.Axolootl;
import axolootl.data.axolootl_variant.AxolootlVariant;
import axolootl.data.aquarium_modifier.AquariumModifier;
import axolootl.data.aquarium_modifier.AquariumModifierContext;
import axolootl.data.resource_generator.ResourceGenerator;
import axolootl.data.resource_generator.ResourceType;
import axolootl.entity.IAxolootl;
import axolootl.menu.ControllerMenu;
import axolootl.util.BreedStatus;
import axolootl.util.FeedStatus;
import axolootl.util.TankMultiblock;
import axolootl.util.TankStatus;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterators;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Tuple;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

public class ControllerBlockEntity extends BlockEntity implements MenuProvider {

    // CONSTANTS //
    /** The default resource generation speed **/
    public static final double BASE_GENERATION_SPEED = 0.1D;
    /** The default breeding speed **/
    public static final double BASE_BREED_SPEED = 0.0D;
    /** The default feeding speed **/
    public static final double BASE_FEED_SPEED = 0.0D;
    /** The number to multiply by the corresponding speed and subtract from the corresponding ticker each tick **/
    public static final long BASE_SPEED_DECREMENT = 100;
    /** The number of ticks between axolootl searches **/
    public static final long AXOLOOTL_SEARCH_INTERVAL = 150;
    /** The number of ticks between axolootl validations **/
    public static final long AXOLOOTL_VALIDATE_INTERVAL = 30;
    /** The number of ticks between modifier validations **/
    public static final long MODIFIER_VALIDATE_INTERVAL = 51;
    /** The percentage of allotted blocks to be scanned by the outside iterator in a single tick **/
    public static final double OUTSIDE_ITERATOR_SCAN = 0.4D;
    /** The percentage of allotted blocks to be scanned by the inside iterator in a single tick **/
    public static final double INSIDE_ITERATOR_SCAN = 0.6D;

    // TAGS //
    public static final TagKey<Block> FLUID_INPUTS = ForgeRegistries.BLOCKS.tags().createTagKey(new ResourceLocation(Axolootl.MODID, "aquarium_fluid_interfaces"));
    public static final TagKey<Block> ENERGY_INPUTS = ForgeRegistries.BLOCKS.tags().createTagKey(new ResourceLocation(Axolootl.MODID, "aquarium_energy_interfaces"));
    public static final TagKey<Block> AXOLOOTL_INPUTS = ForgeRegistries.BLOCKS.tags().createTagKey(new ResourceLocation(Axolootl.MODID, "aquarium_axolootl_interfaces"));
    public static final TagKey<Block> RESOURCE_OUTPUTS = ForgeRegistries.BLOCKS.tags().createTagKey(new ResourceLocation(Axolootl.MODID, "aquarium_outputs"));

    // CAPABILITIES //
    private static final Capability<IItemHandler> ITEM_CAPABILITY = CapabilityManager.get(new CapabilityToken<>() {});
    private static final Capability<IFluidHandler> FLUID_CAPABILITY = CapabilityManager.get(new CapabilityToken<>() {});
    private static final Capability<IEnergyStorage> ENERGY_CAPABILITY = CapabilityManager.get(new CapabilityToken<>() {});

    // RESOURCES //
    private double generationSpeed;
    private double breedSpeed;
    private double feedSpeed;
    private boolean enableMobResources;
    private boolean enableMobBreeding;
    private boolean isOutputFull;
    private boolean isInsufficientPower;
    private boolean isFeedInputEmpty;
    private boolean isBreedInputEmpty;
    private boolean isDuplicateFound;
    private long resourceGenerationTime;
    private long breedTime;
    private long feedTime;
    /** True to force the block entity to recalculate bonuses in the next tick **/
    private boolean forceCalculateBonuses;
    /** True to force the block entity to search for axolootls in the next tick **/
    private boolean forceCalculateAxolootls;

    // STATUS //
    private TankStatus tankStatus;
    private BreedStatus breedStatus;
    private FeedStatus feedStatus;

    // ITERATORS //
    /** Used to periodically query blocks on the outside of the tank **/
    @Nullable
    private Iterator<BlockPos> outsideIterator;
    /** Used to periodically query blocks on the inside of the tank **/
    @Nullable
    private Iterator<BlockPos> insideIterator;

    // TANK //
    @Nullable
    private TankMultiblock.Size size;
    private final Set<BlockPos> axolootlInputs = new HashSet<>();
    private final Set<BlockPos> fluidInputs = new HashSet<>();
    private final Set<BlockPos> energyInputs = new HashSet<>();
    private final Set<BlockPos> resourceOutputs = new HashSet<>();
    private final Map<BlockPos, ResourceLocation> aquariumModifiers = new HashMap<>();
    private final Set<BlockPos> activeAquariumModifiers = new HashSet<>();
    private final Map<UUID, ResourceLocation> trackedAxolootls = new HashMap<>();

    // OTHER //

    public final BiPredicate<BlockPos, AquariumModifier> foodInterfacePredicate = (p, a) -> a.getSettings().isEnableMobBreeding() || a.getSettings().getBreedSpeed() > 0 || a.getSettings().getFeedSpeed() > 0;
    public final BiPredicate<BlockPos, AquariumModifier> activePredicate = (p, o) -> this.activeAquariumModifiers.contains(p);
    public final Predicate<IAxolootl> hasMobResourcePredicate = (a) -> {
        final Optional<AxolootlVariant> oVariant = a.getAxolootlVariant(a.getEntity().level.registryAccess());
        return oVariant.isPresent() && oVariant.get().hasMobResources();
    };

    public ControllerBlockEntity(BlockPos pPos, BlockState pBlockState) {
        this(AxRegistry.BlockEntityReg.CONTROLLER.get(), pPos, pBlockState);
        this.tankStatus = TankStatus.INCOMPLETE;
        this.feedStatus = FeedStatus.INACTIVE;
        this.breedStatus = BreedStatus.INACTIVE;
        this.resourceGenerationTime = 1;
        this.feedTime = 1;
        this.breedTime = 1;
        this.forceCalculateBonuses = true;
    }

    public ControllerBlockEntity(BlockEntityType<?> pType, BlockPos pPos, BlockState pBlockState) {
        super(pType, pPos, pBlockState);
    }

    public static void tick(final Level level, final BlockPos pos, final BlockState state, final ControllerBlockEntity self) {
        // verify area loaded
        if(!(level instanceof ServerLevel serverLevel) || !self.hasTank() || !self.size.isAreaLoaded(level)) {
            return;
        }
        boolean markDirty = false;
        // update status
        level.getProfiler().push("axolootlStatus");
        markDirty |= self.updateStatus(serverLevel);
        level.getProfiler().pop();
        // active updates
        if(self.hasTank()) {
            // validate tank size
            level.getProfiler().push("aquariumTankSize");
            int blocksToScan = Axolootl.CONFIG.TANK_MULTIBLOCK_UPDATE_CAP.get();
            markDirty |= self.iterateOutside(serverLevel, Mth.ceil(blocksToScan * OUTSIDE_ITERATOR_SCAN));
            markDirty |= self.validateInputsOutputs(serverLevel);
            // search for, validate, and apply modifiers
            level.getProfiler().popPush("aquariumModifiers");
            markDirty |= self.iterateInside(serverLevel, Mth.ceil(blocksToScan * INSIDE_ITERATOR_SCAN));
            markDirty |= self.validateUpdateModifiers(serverLevel);
            level.getProfiler().pop();
        }
        // active updates after validating tank size and modifiers
        if(self.getTankStatus().isActive()) {
            // distribute energy to modifiers
            level.getProfiler().push("aquariumEnergy");
            markDirty |= self.distributeEnergyToModifiers(serverLevel);
            // validate and search for axolootl entities
            level.getProfiler().popPush("aquariumEntities");
            markDirty |= self.validateAxolootls(serverLevel);
            markDirty |= self.findAxolootls(serverLevel);
            level.getProfiler().popPush("aquariumBonuses");
            if(self.forceCalculateBonuses) {
                markDirty |= self.applyActiveModifiers(serverLevel);
                self.forceCalculateBonuses = false;
            }
            // update tickers
            level.getProfiler().popPush("aquariumTickers");
            markDirty |= self.updateTickers(serverLevel);
            // feed, breed, and generate resources
            level.getProfiler().popPush("aquariumFeed");
            markDirty |= self.feed(serverLevel);
            level.getProfiler().popPush("aquariumBreed");
            markDirty |= self.breed(serverLevel);
            level.getProfiler().popPush("aquariumResources");
            markDirty |= self.generateResources(serverLevel);
            level.getProfiler().pop();
        }
        // mark changed and send update
        if(markDirty) {
            self.setChanged();
            level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
        }
    }

    //// HELPER METHODS ////

    /**
     * @param size the tank multiblock size
     * @return the maximum entity count of a tank with the given size
     */
    public static int calculateMaxCapacity(final TankMultiblock.Size size) {
        // load config value
        final double volumeFactor = Axolootl.CONFIG.TANK_CAPACITY_VOLUME_FACTOR.get();
        // volume factor of zero indicates infinite capacity
        if(!(volumeFactor > 0)) {
            return Integer.MAX_VALUE;
        }
        return Mth.floor(size.getInnerVolume() / volumeFactor);
    }

    /**
     * @param d1 the first double
     * @param d2 the second double
     * @return true if the difference between the two parameters is more than {@code 1.0E-8D}
     */
    private static boolean notEquals(final double d1, final double d2) {
        return Math.abs(d1 - d2) > 1.0E-8D;
    }

    /**
     * @return true if this controller has a valid tank size
     */
    public boolean hasTank() {
        return this.size != null;
    }

    /**
     * Decrements the resource generation, breed, and feed tickers
     * @return true if any ticker was changed
     * @param serverLevel the server level
     */
    private boolean updateTickers(ServerLevel serverLevel) {
        boolean flag = false;
        if(resourceGenerationTime > 0 && generationSpeed > 0) {
            long tickAmount = getResourceGenerationTickAmount();
            resourceGenerationTime = Math.max(0, resourceGenerationTime - tickAmount);
            flag = true;
        }
        if(breedTime > 0 && breedSpeed > 0) {
            long tickAmount = getBreedTickAmount();
            breedTime = Math.max(0, breedTime - tickAmount);
            flag = true;
        }
        if(feedTime > 0 && feedSpeed > 0) {
            long tickAmount = getFeedTickAmount();
            feedTime = Math.max(0, feedTime - tickAmount);
            flag = true;
        }
        return flag;
    }

    //// MODIFIERS ////

    /**
     * @return true if any modifier multipliers or flags changed
     * @param serverLevel the level
     */
    private boolean applyActiveModifiers(final ServerLevel serverLevel) {
        // calculate generation, feed, and breed speeds
        double generationSpeed = BASE_GENERATION_SPEED;
        double feedSpeed = BASE_FEED_SPEED;
        double breedSpeed = BASE_BREED_SPEED;
        boolean enableMobResources = false;
        boolean enableMobBreeding = false;
        // iterate active modifiers
        for(AquariumModifier entry : resolveModifiers(serverLevel.registryAccess(), activePredicate).values()) {
            // add generation speed
            if(tankStatus.isActive()) {
                generationSpeed += entry.getSettings().getGenerationSpeed();
                enableMobResources |= entry.getSettings().isEnableMobResources();
            }
            // add feed speed
            if(feedStatus.isActive()) {
                feedSpeed += entry.getSettings().getFeedSpeed();
            }
            // add breed speed
            if(breedStatus.isActive()) {
                breedSpeed += entry.getSettings().getBreedSpeed();
                enableMobBreeding |= entry.getSettings().isEnableMobBreeding();
            }
        }
        // iterate axolootls
        for(IAxolootl entry : resolveAxolootls(serverLevel)) {
            // add generation speed
            if(tankStatus.isActive()) {
                generationSpeed += entry.getGenerationSpeed();
            }
            // add feed speed
            if(feedStatus.isActive()) {
                feedSpeed += entry.getFeedSpeed();
            }
            // add breed speed
            if(breedStatus.isActive()) {
                breedSpeed += entry.getBreedSpeed();
            }
        }
        // determine results
        boolean isDirty = notEquals(this.generationSpeed, generationSpeed) || notEquals(this.feedSpeed, feedSpeed) || notEquals(this.breedSpeed, breedSpeed)
                || this.enableMobResources != enableMobResources || this.enableMobBreeding != enableMobBreeding ;
        this.generationSpeed = generationSpeed;
        this.feedSpeed = feedSpeed;
        this.breedSpeed = breedSpeed;
        this.enableMobResources = enableMobResources;
        this.enableMobBreeding = enableMobBreeding;
        return isDirty;
    }

    /**
     * @param registryAccess the registry access
     * @param requireActive true to only consider active modifiers
     * @return true if the aquarium has all required modifiers
     * @see #hasAnyModifier(RegistryAccess, TagKey, boolean)
     */
    private boolean hasMandatoryModifiers(final RegistryAccess registryAccess, final boolean requireActive) {
        final Collection<TagKey<AquariumModifier>> mandatoryModifiers = AxRegistry.AquariumModifiersReg.getMandatoryAquariumModifiers(registryAccess);
        // check non-empty
        if(!mandatoryModifiers.isEmpty() && activeAquariumModifiers.isEmpty()) {
            return false;
        }
        // verify each category has a matching aquarium modifier
        for(TagKey<AquariumModifier> tagKey : mandatoryModifiers) {
            if(!hasAnyModifier(registryAccess, tagKey, requireActive)) {
                return false;
            }
        }
        return true;
    }

    /**
     * @param registryAccess the registry access
     * @param modifierTagId the aquarium modifier tag key
     * @param requireActive true to only consider active modifiers
     * @return true if the aquarium has any of the given modifiers
     */
    private boolean hasAnyModifier(final RegistryAccess registryAccess, final TagKey<AquariumModifier> modifierTagId, final boolean requireActive) {
        for(Map.Entry<BlockPos, ResourceLocation> entry : this.aquariumModifiers.entrySet()) {
            // load modifier from ID
            AquariumModifier modifier = AquariumModifier.getRegistry(registryAccess).get(entry.getValue());
            // check if the modifier is acceptable
            if(modifier != null && modifier.is(registryAccess, modifierTagId) && (!requireActive || activeAquariumModifiers.contains(entry.getKey()))) {
                return true;
            }
        }
        return false;
    }

    // RESOURCES //

    /**
     * @param serverLevel the server level
     * @return true if there are any changes, such as tickers resetting or the axolootl variant map changing
     **/
    private boolean generateResources(ServerLevel serverLevel) {
        // validate ticker
        if(resourceGenerationTime > 0) {
            return false;
        }
        // create resource list
        final List<ItemStack> resources = new ArrayList<>();
        final Set<UUID> invalid = new HashSet<>();
        // resolve axolootls
        final Collection<IAxolootl> axolootls = resolveAxolootls(serverLevel, i -> !i.getEntity().isBaby());
        // iterate over axolootl variants to generate resources
        for(IAxolootl entry : axolootls) {
            // verify variant exists
            Optional<AxolootlVariant> oVariant = entry.getAxolootlVariant(serverLevel.registryAccess());
            if(oVariant.isEmpty()) {
                invalid.add(entry.getEntity().getUUID());
                continue;
            }
            // verify energy
            int cost = oVariant.get().getEnergyCost();
            // load generator
            ResourceGenerator gen = oVariant.get().getResourceGenerator(entry.getEntity().getRandom());
            // verify mob resources are enabled
            if(gen.is(ResourceType.MOB) && !this.enableMobResources) {
                continue;
            }
            // generate resources
            Collection<ItemStack> generatedResources = gen.getRandomEntries(entry.getEntity(), entry.getEntity().getRandom());
            // remove energy
            if(!generatedResources.isEmpty() && cost > 0 && transferEnergy(serverLevel, this.getBlockPos(), cost, true) < cost) {
                break;
            }
            // add generated resources to list
            resources.addAll(generatedResources);
        }
        // remove invalid variants
        invalid.forEach(uuid -> trackedAxolootls.remove(uuid));
        // update ticker
        if(resources.isEmpty()) {
            resourceGenerationTime = 100L * BASE_SPEED_DECREMENT;
        } else {
            resourceGenerationTime = Axolootl.CONFIG.BASE_GENERATION_PERIOD.get() * BASE_SPEED_DECREMENT;
        }
        // insert all generated resources (remainder is ignored)
        final Collection<ItemStack> remainder = insertItems(resources, false);
        return !invalid.isEmpty();
    }

    /**
     * Attempts to insert the item stack into any known output
     * @param itemStack the item stack to insert
     * @param simulate true to simulate the operation
     * @return the remainder item stack after attempting to insert into all known outputs
     */
    public ItemStack insertItem(final ItemStack itemStack, final boolean simulate) {
        final Collection<ItemStack> remainder = insertItems(ImmutableList.of(itemStack), simulate);
        if(remainder.isEmpty()) {
            return ItemStack.EMPTY;
        }
        return remainder.iterator().next();
    }

    /**
     * Attempts to insert the item stacks into any known output
     * @param itemStacks the item stacks to insert
     * @param simulate true to simulate the operation
     * @return the remainder item stacks after attempting to insert into all known outputs
     */
    public Collection<ItemStack> insertItems(final Collection<ItemStack> itemStacks, final boolean simulate) {
        final Collection<ItemStack> remainder = insertResources(itemStacks, simulate);
        if(!simulate) {
            this.isOutputFull = !remainder.isEmpty();
        }
        return remainder;
    }

    /**
     * Attempts to insert the item stack into any known output
     * @param itemStack the item stack to insert
     * @param simulate true to simulate the operation
     * @return the remainder item stack after attempting to insert into all known outputs
     */
    private ItemStack insertResource(final ItemStack itemStack, final boolean simulate) {
        final Collection<ItemStack> remainder = insertResources(ImmutableList.of(itemStack), simulate);
        if(remainder.isEmpty()) {
            return ItemStack.EMPTY;
        }
        return remainder.iterator().next();
    }

    /**
     * Attempts to insert the item stacks into any known output
     * @param itemStacks the item stacks to insert
     * @param simulate true to simulate the operation
     * @return the remainder item stacks after attempting to insert into all known outputs
     */
    private Collection<ItemStack> insertResources(final Collection<ItemStack> itemStacks, final boolean simulate) {
        // create modifiable list of item stacks
        final List<ItemStack> itemStackList = new ArrayList<>(itemStacks);
        // prepare to load resource outputs
        BlockEntity blockEntity;
        Optional<IItemHandler> capability;
        // iterate each potential output
        for(BlockPos pos : this.resourceOutputs) {
            blockEntity = this.level.getBlockEntity(pos);
            if(blockEntity != null) {
                // load item handler capability
                capability = blockEntity.getCapability(ITEM_CAPABILITY).resolve();
                // insert items
                capability.ifPresent(c -> {
                    ItemStack itemStack;
                    // iterate itemstacks and attempt to insert each non-empty item
                    for(int i = 0, n = itemStackList.size(); i < n; i++) {
                        itemStack = itemStackList.get(i);
                        if(itemStack.isEmpty()) continue;
                        itemStackList.set(i, ItemHandlerHelper.insertItemStacked(c, itemStack, simulate));
                    }
                });
                // check for successful insertion
                itemStackList.removeIf(ItemStack::isEmpty);
                if(itemStackList.isEmpty()) {
                    return itemStackList;
                }
            }
        }
        // return remaining items
        return itemStackList;
    }


    // FEEDING //

    /**
     * @return true if the axolootl list, feed ticker, or feed status changed
     * @param serverLevel the server level
     */
    private boolean feed(ServerLevel serverLevel) {
        // validate ticker
        if(feedTime > 0 || !(feedSpeed > 0)) {
            return false;
        }
        // validate entity count
        if(this.trackedAxolootls.size() < 1) {
            return false;
        }
        // resolve axolootls
        final Collection<IAxolootl> axolootls = resolveAxolootls(serverLevel);
        // resolve autofeeders
        final Map<BlockPos, AquariumModifier> modifiers = resolveModifiers(serverLevel.registryAccess(),
                activePredicate.and((b, a) -> a.getSettings().getFeedSpeed() > 0));
        // collect inventories
        final ImmutableMap.Builder<BlockPos, IItemHandler> itemHandlerBuilder = ImmutableMap.builder();
        for(BlockPos entry : modifiers.keySet()) {
            BlockEntity blockEntity = serverLevel.getBlockEntity(entry);
            if(blockEntity != null) {
                blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER).ifPresent(handler -> itemHandlerBuilder.put(entry, handler));
            }
        }
        final Map<BlockPos, IItemHandler> itemHandlers = itemHandlerBuilder.build();
        // verify non-empty list
        if(itemHandlers.isEmpty()) {
            this.setFeedInputEmpty(true);
            return true;
        }
        // iterate each axolootl
        boolean hasFed = false; // true when at least one axolootl was fed
        boolean nonEmpty = false; // true when at least one handler has items
        int feedCandidates = 0; // the number of axolootls that need to be fed
        for(IAxolootl axolootl : axolootls) {
            // validate axolootl can accept food
            if(!axolootl.isFeedCandidate(serverLevel)) continue;
            // attempt to feed from each known item handler
            for(IItemHandler handler : itemHandlers.values()) {
                InteractionResultHolder<Boolean> result = feed(serverLevel, handler, axolootl);
                nonEmpty |= result.getObject().booleanValue();
                if(result.getResult().consumesAction()) {
                    hasFed = true;
                    break;
                }
            }
            feedCandidates++;
        }
        // update empty flag
        this.setFeedInputEmpty(feedCandidates > 0 && !nonEmpty);
        // reset ticker
        if(hasFed) {
            this.feedTime = Axolootl.CONFIG.BASE_FEEDING_PERIOD.get() * BASE_SPEED_DECREMENT;
        } else {
            this.feedTime = 200L * BASE_SPEED_DECREMENT;
        }
        return true;
    }

    /**
     * Iterates the item handler inventory and attempts to feed each item to the given axolootl
     * @param serverLevel the server level
     * @param handler the item handler
     * @param axolootl the axolootl
     * @return the result of the operation and a flag that is true when there are at least some items in the handler
     */
    private InteractionResultHolder<Boolean> feed(ServerLevel serverLevel, IItemHandler handler, IAxolootl axolootl) {
        // iterate items in inventory
        int emptySlots = 0;
        for(int i = 0, n = handler.getSlots(); i < n; i++) {
            // validate item can be extracted
            if(handler.extractItem(i, 1, true).isEmpty()) {
                emptySlots++;
                continue;
            }
            // attempt to feed this item
            ItemStack food = handler.getStackInSlot(i).copy().split(1);
            InteractionResult result = axolootl.feed(serverLevel, food);
            if(result.consumesAction()) {
                // remove from item handler
                handler.extractItem(i, 1, false);
                return new InteractionResultHolder<>(result, true);
            }
        }
        return InteractionResultHolder.pass(emptySlots < handler.getSlots());
    }

    // BREEDING //

    /**
     * @return true if the axolootl list or breed ticker changed
     * @param serverLevel the server level
     */
    private boolean breed(ServerLevel serverLevel) {
        // validate ticker
        if(breedTime > 0) {
            return false;
        }
        // validate entity count
        if(this.trackedAxolootls.size() < 2 || this.trackedAxolootls.size() + 1 > calculateMaxCapacity(this.size)) {
            return false;
        }
        // resolve axolootls
        final Collection<IAxolootl> axolootls = resolveAxolootls(serverLevel, a -> enableMobBreeding
                || !a.getAxolootlVariant(serverLevel.registryAccess()).orElse(AxolootlVariant.EMPTY).hasMobResources());
        // resolve breeders
        final Map<BlockPos, AquariumModifier> modifiers = resolveModifiers(serverLevel.registryAccess(),
                activePredicate.and((b, a) -> a.getSettings().getBreedSpeed() > 0 || a.getSettings().isEnableMobBreeding()));
        // collect inventories
        final ImmutableMap.Builder<BlockPos, IItemHandler> itemHandlerBuilder = ImmutableMap.builder();
        for(BlockPos entry : modifiers.keySet()) {
            BlockEntity blockEntity = serverLevel.getBlockEntity(entry);
            if(blockEntity != null) {
                blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER).ifPresent(handler -> itemHandlerBuilder.put(entry, handler));
            }
        }
        final Map<BlockPos, IItemHandler> itemHandlers = itemHandlerBuilder.build();
        // verify non-empty list
        if(itemHandlers.isEmpty()) {
            this.setBreedInputEmpty(true);
            return true;
        }
        // iterate each axolootl
        boolean hasBred = false; // true when at least one pair was bred
        boolean nonEmpty = false; // true when at least one handler has items
        int breedCandidates = 0; // the number of axolootls that can breed
        for(IAxolootl axolootl : axolootls) {
            // validate axolootl can breed
            if (!axolootl.isBreedCandidate(serverLevel, Optional.empty())) continue;
            // iterate each other axolootl
            Optional<IAxolootl> oAxolootl = Optional.of(axolootl);
            for(IAxolootl other : axolootls) {
                // validate other can breed
                if (axolootl == other || !other.isBreedCandidate(serverLevel, oAxolootl)) continue;
                // attempt to breed from each known item handler
                InteractionResultHolder<Boolean> result = breed(serverLevel, itemHandlers, axolootl, other);
                nonEmpty |= result.getObject().booleanValue();
                if(result.getResult().consumesAction()) {
                    hasBred = true;
                    break;
                }
            }
            breedCandidates++;
        }
        // update empty flag
        this.setBreedInputEmpty(breedCandidates > 0 && !nonEmpty);
        // reset ticker
        if(hasBred) {
            this.breedTime = Axolootl.CONFIG.BASE_BREEDING_PERIOD.get() * BASE_SPEED_DECREMENT;
        } else {
            this.breedTime = 200L * BASE_SPEED_DECREMENT;
        }
        return true;
    }

    /**
     * Iterates the item handler inventory and attempts to feed each item to the given axolootls
     * @param serverLevel the server level
     * @param handlers the item handlers
     * @param axolootl the axolootl
     * @return the result of the operation and a flag that is true when there are at least some items in the handler
     */
    private InteractionResultHolder<Boolean> breed(ServerLevel serverLevel, Map<BlockPos, IItemHandler> handlers, IAxolootl axolootl, IAxolootl other) {

        final AxolootlVariant variant1 = axolootl.getAxolootlVariant(serverLevel.registryAccess()).orElse(AxolootlVariant.EMPTY);
        final AxolootlVariant variant2 = other.getAxolootlVariant(serverLevel.registryAccess()).orElse(AxolootlVariant.EMPTY);
        final HolderSet<Item> breedFood1 = variant1.getBreedFood();
        final HolderSet<Item> breedFood2 = variant2.getBreedFood();

        // iterate inventories
        IItemHandler handler1 = null;
        IItemHandler handler2 = null;
        int slot1 = -1;
        int slot2 = -1;
        for(IItemHandler handler : handlers.values()) {
            // find items matching holder sets
            Tuple<Integer, Integer> result = findItems(serverLevel, handler, breedFood1, breedFood2);
            // check first item found
            if(slot1 < 0 && result.getA() >= 0) {
                handler1 = handler;
                slot1 = result.getA();
            }
            // check second item found
            if(slot2 < 0 && result.getB() >= 0) {
                handler2 = handler;
                slot2 = result.getB();
            }
            // check both items found early
            if(slot1 >= 0 && slot2 >= 0) break;
        }
        // validate both items found
        if(null == handler1 || null == handler2) {
            return InteractionResultHolder.pass(true);
        }
        // validate both items extracted
        ItemStack food1;
        ItemStack food2;
        if((food1 = handler1.extractItem(slot1, 1, false)).isEmpty() || (food2 = handler2.extractItem(slot2, 1, false)).isEmpty()) {
            return InteractionResultHolder.fail(false);
        }
        // insert crafting remainders
        ItemStack remainder1 = food1.getCraftingRemainingItem();
        ItemStack remainder2 = food2.getCraftingRemainingItem();
        if(!remainder1.isEmpty()) {
            insertItem(remainder1, false);
        }
        if(!remainder2.isEmpty()) {
            insertItem(remainder2, false);
        }
        // try to breed
        Optional<IAxolootl> oChild = axolootl.breed(serverLevel, other);
        if(oChild.isEmpty() || oChild.get().getAxolootlVariantId().isEmpty()) {
            return InteractionResultHolder.pass(slot1 < 0 && slot2 < 0);
        }
        // add child to tracked axolootls
        this.trackedAxolootls.put(oChild.get().getEntity().getUUID(), oChild.get().getAxolootlVariantId().get());
        this.forceCalculateBonuses();
        return InteractionResultHolder.success(true);
    }

    /**
     * Finds the given item in the given inventory
     * @param serverLevel the server level
     * @param handler the item handler
     * @param left the first item to search for
     * @param right the second item to search for
     * @return the slots of the items that were found, where -1 means the item was not found
     */
    private Tuple<Integer, Integer> findItems(ServerLevel serverLevel, final IItemHandler handler, final HolderSet<Item> left, final HolderSet<Item> right) {
        // iterate items in inventory
        int slotLeft = -1;
        int slotRight = -1;
        for(int i = 0, n = handler.getSlots(); i < n; i++) {
            // validate item can be extracted
            if(handler.extractItem(i, 1, true).isEmpty()) continue;
            // validate item is food
            ItemStack food = handler.getStackInSlot(i).copy().split(1);
            if(slotLeft < 0 && left.contains(food.getItemHolder())) {
                slotLeft = i;
            }
            if(slotRight < 0 && right.contains(food.getItemHolder())) {
                slotRight = i;
            }
            // both items were found
            if(slotLeft >= 0 && slotRight >= 0) {
                break;
            }
        }
        return new Tuple<>(slotLeft, slotRight);
    }

    // ITERATORS //

    /**
     *
     * @param serverLevel the server level
     * @param blocksToCheck the maximum number of blocks to check
     * @return true if the tank size changed or new inputs/outputs were found
     */
    private boolean iterateOutside(ServerLevel serverLevel, final int blocksToCheck) {
        // verify iterator exists
        if(null == outsideIterator || null == size) {
            return false;
        }
        final Set<BlockPos> axolootlInputs = new HashSet<>();
        final Set<BlockPos> fluidInputs = new HashSet<>();
        final Set<BlockPos> energyInputs = new HashSet<>();
        final Set<BlockPos> resourceOutputs = new HashSet<>();
        // iterate each block
        int blocksChecked = 0;
        while(outsideIterator.hasNext() && blocksChecked++ < blocksToCheck) {
            BlockPos pos = outsideIterator.next();
            // validate no duplicate controllers
            if(!getBlockPos().equals(pos) && level.getBlockEntity(pos) instanceof ControllerBlockEntity) {
                this.isDuplicateFound = true;
                this.setSize(null);
                return true;
            }
            // validate tank block
            if(!TankMultiblock.AQUARIUM.isTankBlock(serverLevel, pos)) {
                this.setSize(null);
                return true;
            }
            // add to corresponding set, if applicable
            BlockState blockState = serverLevel.getBlockState(pos);
            // add fluid input
            if(blockState.is(FLUID_INPUTS) && !fluidInputs.contains(pos)) {
                fluidInputs.add(pos.immutable());
                IAquariumControllerProvider.trySetController(serverLevel, pos, this);
            }
            // add energy input
            if(blockState.is(ENERGY_INPUTS) && !energyInputs.contains(pos)) {
                energyInputs.add(pos.immutable());
                IAquariumControllerProvider.trySetController(serverLevel, pos, this);
            }
            // add axolootl input
            if(blockState.is(AXOLOOTL_INPUTS) && !axolootlInputs.contains(pos)) {
                axolootlInputs.add(pos.immutable());
                IAquariumControllerProvider.trySetController(serverLevel, pos, this);
            }
            // add resource outputs
            if(blockState.is(RESOURCE_OUTPUTS) && !resourceOutputs.contains(pos)) {
                resourceOutputs.add(pos.immutable());
                IAquariumControllerProvider.trySetController(serverLevel, pos, this);
            }
        }
        // restart iterator after it is finished
        if(!outsideIterator.hasNext() && size != null) {
            outsideIterator = size.outerPositions().iterator();
        }
        // check any position is not already known
        boolean isDirty = (!this.fluidInputs.containsAll(fluidInputs) || !this.energyInputs.containsAll(energyInputs)
            || !this.axolootlInputs.containsAll(axolootlInputs) || !this.resourceOutputs.containsAll(resourceOutputs));
        // update sets
        this.fluidInputs.addAll(fluidInputs);
        this.energyInputs.addAll(energyInputs);
        this.axolootlInputs.addAll(axolootlInputs);
        this.resourceOutputs.addAll(resourceOutputs);
        return isDirty;
    }

    /**
     * @param serverLevel the server level
     * @param blocksToCheck the maximum number of blocks to iterate
     * @return true if the modifier map was changed
     */
    private boolean iterateInside(ServerLevel serverLevel, final int blocksToCheck) {
        // verify iterator exists
        if(null == insideIterator || null == size) {
            return false;
        }
        // check each block for a valid modifier
        int blocksChecked = 0;
        boolean foundModifiers = false;
        while(insideIterator.hasNext() && blocksChecked++ < blocksToCheck) {
            BlockPos pos = insideIterator.next();
            // determine applicable modifier
            Optional<AquariumModifier> oModifier = AquariumModifier.forBlock(serverLevel, pos);
            if(oModifier.isPresent()) {
                ResourceLocation name = oModifier.get().getRegistryName(serverLevel.registryAccess());
                // determine if modifier was not previously known
                foundModifiers |= !this.aquariumModifiers.containsKey(pos) || !this.aquariumModifiers.get(pos).equals(name);
                // add modifier to map
                this.aquariumModifiers.put(pos.immutable(), name);
                // notify modifier
                IAquariumControllerProvider.trySetController(serverLevel, pos, this);
            }
        }
        // restart iterator after it is finished
        if(!insideIterator.hasNext() && size != null) {
            insideIterator = size.innerPositions().iterator();
        }
        // report changes
        if(foundModifiers) {
            return this.forceCalculateBonuses = true;
        }
        return false;
    }

    /**
     * @param serverLevel the server level
     * @return true if the entity list changed
     */
    public boolean findAxolootls(ServerLevel serverLevel) {
        // validate tank exists
        if(null == this.size) {
            return false;
        }
        // validate needs to update this tick
        if(!forceCalculateAxolootls && serverLevel.getGameTime() % AXOLOOTL_SEARCH_INTERVAL != 0) {
            return false;
        }
        this.forceCalculateAxolootls = false;
        // query entities that are not already tracked
        final AABB aabb = this.size.aabb();
        final List<LivingEntity> list = serverLevel.getEntitiesOfClass(LivingEntity.class, aabb,
                entity -> entity instanceof IAxolootl iAxolootl && !trackedAxolootls.containsKey(entity.getUUID()) && iAxolootl.getAxolootlVariantId().isPresent());
        // add new entities
        list.forEach(e -> {
            this.trackedAxolootls.put(e.getUUID(), ((IAxolootl)e).getAxolootlVariantId().get());
            IAquariumControllerProvider.trySetController(e, serverLevel, this.getBlockPos(), this);
        });
        // report changes
        if(!list.isEmpty()) {
            return this.forceCalculateBonuses = true;
        }
        return false;
    }

    /**
     * @param serverLevel the server level
     * @return true if the entity list changed
     */
    private boolean validateAxolootls(ServerLevel serverLevel) {
        // validate tank exists
        if(null == this.size) {
            return false;
        }
        // validate needs to update this tick
        if(serverLevel.getGameTime() % AXOLOOTL_VALIDATE_INTERVAL != 0) {
            return false;
        }
        final Set<UUID> invalid = new HashSet<>();
        final AABB bounds = this.size.aabb();
        // validate each tracked entity
        for(UUID uuid : trackedAxolootls.keySet()) {
            Entity entity = serverLevel.getEntity(uuid);
            if(null == entity || !bounds.intersects(entity.getBoundingBox())) {
                invalid.add(uuid);
                IAquariumControllerProvider.tryClearController(entity);
            }
        }
        // remove invalid entities
        invalid.forEach(uuid -> trackedAxolootls.remove(uuid));
        // report changes
        if(!invalid.isEmpty()) {
            return this.forceCalculateBonuses = true;
        }
        return false;
    }

    /**
     * Validates that all tracked inputs and outputs are still valid
     * @param serverLevel the server level
     * @return true if any input or output set was changed
     */
    private boolean validateInputsOutputs(ServerLevel serverLevel) {
        // determine which set to validate this tick, if any
        int flag = (int) (serverLevel.getGameTime() % 40);
        Set<BlockPos> invalid;
        // validate all block positions in the set
        switch (flag) {
            case 0:
                // validate resource outputs
                invalid = invalidateBlocks(serverLevel, resourceOutputs, p -> serverLevel.getBlockState(p).is(RESOURCE_OUTPUTS));
                invalid.forEach(p -> {
                    IAquariumControllerProvider.tryClearController(serverLevel, p);
                    resourceOutputs.remove(p);
                });
                return !invalid.isEmpty();
            case 1:
                // validate axolootl inputs
                invalid = invalidateBlocks(serverLevel, axolootlInputs, p -> serverLevel.getBlockState(p).is(AXOLOOTL_INPUTS));
                invalid.forEach(p -> {
                    IAquariumControllerProvider.tryClearController(serverLevel, p);
                    axolootlInputs.remove(p);
                });
                return !invalid.isEmpty();
            case 2:
                // validate fluid inputs
                invalid = invalidateBlocks(serverLevel, fluidInputs, p -> serverLevel.getBlockState(p).is(FLUID_INPUTS));
                invalid.forEach(p -> {
                    IAquariumControllerProvider.tryClearController(serverLevel, p);
                    fluidInputs.remove(p);
                });
                return !invalid.isEmpty();
            case 3:
                // validate energy inputs
                invalid = invalidateBlocks(serverLevel, energyInputs, p -> serverLevel.getBlockState(p).is(ENERGY_INPUTS));
                invalid.forEach(p -> {
                    IAquariumControllerProvider.tryClearController(serverLevel, p);
                    energyInputs.remove(p);
                });
                return !invalid.isEmpty();
            default:
                // do not validate any sets this tick
                return false;
        }
    }

    /**
     * @param serverLevel the server level
     * @param positions the block positions to validate
     * @param predicate the predicate that all valid block positions must pass
     * @return the block positions that were invalid, if any
     */
    private Set<BlockPos> invalidateBlocks(ServerLevel serverLevel, Set<BlockPos> positions, Predicate<BlockPos> predicate) {
        final Set<BlockPos> invalid = new HashSet<>();
        for(BlockPos p : positions) {
            if(!predicate.test(p)) {
                invalid.add(p);
            }
        }
        return invalid;
    }

    /**
     * @param serverLevel the server level
     * @return true if there are any changes to the modifier map or active modifier set
     */
    private boolean validateUpdateModifiers(ServerLevel serverLevel) {
        // validate modifiers can be checked this tick
        if(serverLevel.getGameTime() % MODIFIER_VALIDATE_INTERVAL != 0) {
            return false;
        }
        // iterate modifiers and check if they still exist and whether they are active
        final Set<BlockPos> invalid = new HashSet<>();
        final Set<BlockPos> active = new HashSet<>();
        final Collection<IAxolootl> axolootls = resolveAxolootls(serverLevel);
        final Map<BlockPos, AquariumModifier> modifierMap = ImmutableMap.copyOf(resolveModifiers(serverLevel.registryAccess()));
        for(Map.Entry<BlockPos, AquariumModifier> entry : modifierMap.entrySet()) {
            // validate modifier
            if(entry.getValue().isApplicable(serverLevel, entry.getKey())) {
                // create context
                final Map<BlockPos, AquariumModifier> contextMap = new HashMap<>(modifierMap);
                contextMap.remove(entry.getKey());
                AquariumModifierContext context = new AquariumModifierContext(serverLevel, entry.getKey(), size, axolootls, contextMap);
                // validate modifier is active and either does not require power or the tank has sufficient power
                if(entry.getValue().isActive(context)/* && (!isInsufficientPower() || entry.getValue().getSettings().getEnergyCost() <= 0)*/) {
                    active.add(entry.getKey());
                    // attempt to spread
                    final Optional<BlockPos> oPropogated = entry.getValue().checkAndSpread(context);
                    // add new modifier to map
                    oPropogated.ifPresent(b -> {
                        aquariumModifiers.put(b, entry.getValue().getRegistryName(serverLevel.registryAccess()));
                        // create context to check new modifier immediately
                        contextMap.put(entry.getKey(), entry.getValue());
                        // check if the new modifier is active
                        if(entry.getValue().isActive(new AquariumModifierContext(serverLevel, b, size, axolootls, contextMap))) {
                            active.add(b);
                        }
                    });
                }
            } else {
                invalid.add(entry.getKey());
                IAquariumControllerProvider.tryClearController(serverLevel, entry.getKey());
            }
        }
        // remove invalid modifiers
        invalid.forEach(p -> aquariumModifiers.remove(p));
        boolean isDirty = !invalid.isEmpty();
        // update active modifier set
        if(!this.activeAquariumModifiers.equals(active)) {
            this.activeAquariumModifiers.clear();
            this.activeAquariumModifiers.addAll(active);
            this.forceCalculateBonuses();
            isDirty = true;
        }
        return isDirty;
    }

    /**
     * @param level the server level
     * @return true if there are any changes
     **/
    private boolean distributeEnergyToModifiers(Level level) {
        // collect aquarium modifiers
        final List<Map.Entry<BlockPos, AquariumModifier>> modifiers = new ArrayList<>(resolveModifiers(level.registryAccess(),
                activePredicate.and((b, a) -> a.getSettings().getEnergyCost() > 0)).entrySet());
        // sort from highest energy cost to lowest
        final Comparator<Map.Entry<BlockPos, AquariumModifier>> comparator = Comparator.comparingInt(e -> e.getValue().getSettings().getEnergyCost());
        modifiers.sort(comparator.reversed());
        // verify energy is required
        if(modifiers.isEmpty()) {
            return false;
        }
        // collect energy handlers
        final Map<BlockPos, IEnergyStorage> energyHandlers = resolveEnergyStorage(IEnergyStorage::canExtract);
        // attempt to distribute energy
        boolean hasPowered = false;
        for(Map.Entry<BlockPos, AquariumModifier> entry : modifiers) {
            // determine cost for this modifier
            int cost = entry.getValue().getSettings().getEnergyCost();
            int depleted = 0;
            // whether the destination block is responsible to use up the energy
            boolean isVoid = entry.getValue().getSettings().isGreedyEnergy();
            // iterate each energy storage and attempt to transfer energy
            for(IEnergyStorage energyStorage : energyHandlers.values()) {
                // transfer the amount of energy required for the modifier
                depleted += transferEnergy(level, energyStorage, entry.getKey(), cost, isVoid);
                if(depleted >= cost) {
                    hasPowered = true;
                    break;
                }
            }
            // detect when the energy storage is depleted and notify the controller
            if (depleted < cost) {
                setInsufficientPower(true);
                this.aquariumModifiers.remove(entry.getKey());
                this.activeAquariumModifiers.remove(entry.getKey());
                IAquariumControllerProvider.tryClearController(level, entry.getKey());
                this.forceCalculateBonuses();
                return true;
            }
        }
        // update controller tank state
        setInsufficientPower(false);
        // no internal changes to report here
        return false;
    }

    /**
     * General purpose method to transfer energy from all known energy sources until the given amount is transferred
     * @param level the level
     * @param targetPos the block position to receive the energy
     * @param maxAmount the amount of energy to transfer
     * @param useVoidStorage true to transfer energy into the void, never to be seen again
     * @return the amount of energy that was transferred
     */
    private int transferEnergy(final Level level, final BlockPos targetPos, final int maxAmount, final boolean useVoidStorage) {
        // collect energy handlers
        final Map<BlockPos, IEnergyStorage> energyHandlers = new HashMap<>();
        for(BlockPos entry : energyInputs) {
            IEnergyStorage storage = resolveEnergyStorageOrVoid(level, entry, false);
            if(storage.canExtract()) {
                energyHandlers.put(entry, storage);
            }
        }
        // attempt to transfer energy
        int depleted = 0;
        // iterate each energy storage and attempt to transfer energy
        for(IEnergyStorage energyStorage : energyHandlers.values()) {
            // transfer the amount of energy required for the modifier
            depleted += transferEnergy(level, energyStorage, targetPos, maxAmount - depleted, useVoidStorage);
            if(depleted >= maxAmount) {
                break;
            }
        }
        // detect when the energy storage is depleted and notify the controller
        if (depleted < maxAmount) {
            setInsufficientPower(true);
            this.forceCalculateBonuses();
        }
        return depleted;
    }


    /**
     * Attempts to insert energy into the given position. If the given block entity
     * does not exist or cannot accept energy, the energy is permanently removed into a void storage
     * @param level the level
     * @param energyStorage the source energy storage
     * @param receiverPos the destination position
     * @param maxAmount the maximum amount of energy to transfer
     * @param useVoidStorage true to transfer the energy into the void, never to be seen again
     * @return the energy that was transferred
     */
    private int transferEnergy(final Level level, final IEnergyStorage energyStorage, final BlockPos receiverPos, final int maxAmount, final boolean useVoidStorage) {
        // determine amount to extract
        final int amount = Math.min(maxAmount, energyStorage.extractEnergy(maxAmount, true));
        if(amount <= 0) {
            return 0;
        }
        // load destination energy storage
        final IEnergyStorage receiverStorage = resolveEnergyStorageOrVoid(level, receiverPos, useVoidStorage);
        // attempt to insert energy
        if(!receiverStorage.canReceive() || receiverStorage.receiveEnergy(amount, true) <= 0) {
            return 0;
        }
        return energyStorage.extractEnergy(receiverStorage.receiveEnergy(amount, false), false);
    }

    /**
     * Attempts to resolve an energy storage for each side of the block entity until one is found that can receive energy.
     * If the block entity does not exist or the directional search fails, a void storage is created.
     * @param level the level
     * @param pos the block position
     * @param useVoidStorage true to transfer the energy into the void, never to be seen again
     * @return the non-null energy storage to represent the given position, may be void
     * @see {@link VoidEnergyStorage}
     */
    private IEnergyStorage resolveEnergyStorageOrVoid(final Level level, final BlockPos pos, final boolean useVoidStorage) {
        final BlockEntity blockEntity;
        // verify smart storage and block entity exists
        if(useVoidStorage || null == (blockEntity = level.getBlockEntity(pos))) {
            return VoidEnergyStorage.INSTANCE;
        }
        // iterate each direction until an energy storage capability is found that can receive energy
        for(Direction d : Direction.values()) {
            Optional<IEnergyStorage> oStorage = blockEntity.getCapability(ForgeCapabilities.ENERGY, d).resolve();
            if(oStorage.isPresent() && oStorage.get().canReceive()) {
                return oStorage.get();
            }
        }
        // all checks failed
        return VoidEnergyStorage.INSTANCE;
    }

    /**
     * Called when the block is removed from the world
     * @param level the level
     */
    public void onRemoved(ServerLevel level) {
        clearAllData(level);
    }

    private void clearAllData(final ServerLevel level) {
        // remove self from modifiers
        for(BlockPos p : aquariumModifiers.keySet()) {
            IAquariumControllerProvider.tryClearController(level, p);
        }
        // remove self from inputs and outputs
        Iterator<BlockPos> inputsOutputs = Iterators.concat(axolootlInputs.iterator(), fluidInputs.iterator(), energyInputs.iterator(), resourceOutputs.iterator());
        while(inputsOutputs.hasNext()) {
            IAquariumControllerProvider.tryClearController(level, inputsOutputs.next());
        }
        // remove self from entities
        for(IAxolootl e : resolveAxolootls(level)) {
            IAquariumControllerProvider.tryClearController(e.getEntity());
        }
        // clear data
        this.tankStatus = TankStatus.INCOMPLETE;
        this.trackedAxolootls.clear();
        this.aquariumModifiers.clear();
        this.activeAquariumModifiers.clear();
        this.insideIterator = null;
        this.outsideIterator = null;
        this.setChanged();
    }

    // STATUS //

    /**
     * Updates tank, breed, and feed speeds and statuses
     * @param serverLevel the server level
     * @return true if any status was changed
     */
    private boolean updateStatus(ServerLevel serverLevel) {
        final TankStatus tankStatus = updateTankStatus(serverLevel);
        final FeedStatus feedStatus;
        final BreedStatus breedStatus;
        if(!tankStatus.isActive()) {
            breedStatus = BreedStatus.INACTIVE;
            feedStatus = FeedStatus.INACTIVE;
        } else {
            final Map<BlockPos, AquariumModifier> modifiers = resolveModifiers(serverLevel.registryAccess(), activePredicate);
            breedStatus = updateBreedStatus(serverLevel, modifiers);
            feedStatus = updateFeedStatus(serverLevel, modifiers);
        }
        final boolean isDirty = (tankStatus != this.tankStatus || breedStatus != this.breedStatus || feedStatus != this.feedStatus);
        this.tankStatus = tankStatus;
        this.breedStatus = breedStatus;
        this.feedStatus = feedStatus;
        this.forceCalculateBonuses |= isDirty;
        return isDirty;
    }

    /**
     * Calculates tank status
     * @param serverLevel the server level
     * @return the TankStatus
     */
    private TankStatus updateTankStatus(ServerLevel serverLevel) {
        // check duplicate controllers
        if(this.isDuplicateFound) {
            return TankStatus.DUPLICATE_CONTROLLERS;
        }
        // check missing tank
        if(null == this.size) {
            return TankStatus.INCOMPLETE;
        }
        // check missing modifiers
        if(!this.hasMandatoryModifiers(serverLevel.registryAccess(), true)) {
            return TankStatus.MISSING_MODIFIERS;
        }
        // check low power
        if(this.isInsufficientPower()) {
            return TankStatus.LOW_ENERGY;
        }
        // check entity count is above capacity
        if(this.trackedAxolootls.size() > calculateMaxCapacity(this.size)) {
            return TankStatus.OVERCROWDED;
        }
        // check storage is nonexistent or full
        if(this.resourceOutputs.isEmpty() || isOutputFull()) {
            return TankStatus.STORAGE_FULL;
        }
        // all checks passed
        return TankStatus.ACTIVE;
    }

    /**
     * Updates feed speed and calculates current feed status
     * @param serverLevel the server level
     * @param activeModifiers all active aquarium modifiers
     * @return the FeedStatus
     */
    private FeedStatus updateFeedStatus(ServerLevel serverLevel, final Map<BlockPos, AquariumModifier> activeModifiers) {
        // check missing resources
        if(isFeedInputEmpty()) {
            return FeedStatus.MISSING_RESOURCES;
        }
        // calculate feed settings
        double feedSpeed = BASE_FEED_SPEED;
        for(AquariumModifier modifier : activeModifiers.values()) {
            feedSpeed += modifier.getSettings().getFeedSpeed();
        }
        // TODO check feed paused
        // check feed speed
        if(feedSpeed > 0) {
            // check feed input is not empty
            if(isFeedInputEmpty()) {
                return FeedStatus.MISSING_RESOURCES;
            }
            return FeedStatus.ACTIVE;
        }
        return FeedStatus.INACTIVE;
    }

    /**
     * Updates breed speed and mob breeding flag and calculates current breed status
     *
     * @param serverLevel the server level
     * @param activeModifiers all active aquarium modifiers
     * @return the BreedStatus
     */
    private BreedStatus updateBreedStatus(ServerLevel serverLevel, final Map<BlockPos, AquariumModifier> activeModifiers) {
        // check insufficient resources
        if(isBreedInputEmpty()) {
            return BreedStatus.MISSING_RESOURCES;
        }
        // determine breed settings
        double breedSpeed = BASE_BREED_SPEED;
        boolean mobBreeding = false;
        for(AquariumModifier modifier : activeModifiers.values()) {
            breedSpeed += modifier.getSettings().getBreedSpeed();
            mobBreeding |= modifier.getSettings().isEnableMobBreeding();
        }
        // TODO check breed paused
        // check breed speed
        if(breedSpeed > 0) {
            // check capacity
            if(this.trackedAxolootls.size() >= calculateMaxCapacity(this.size)) {
                return BreedStatus.MAX_COUNT;
            }
            // check mob breeding
            final int mobVariants = (int) this.resolveAxolootlVariants(serverLevel.registryAccess()).values().stream().filter(AxolootlVariant::hasMobResources).count();
            final int resourceVariants = this.trackedAxolootls.size() - mobVariants;
            if(mobBreeding) {
                // check min count of any variant
                if(this.trackedAxolootls.size() < 2) {
                    return BreedStatus.MIN_COUNT;
                }
                return BreedStatus.RESOURCE_MOB_ONLY;
            }
            // check min count of resource variants
            if(resourceVariants < 2) {
                return BreedStatus.MIN_COUNT;
            }
            // all checks passed
            return BreedStatus.ACTIVE;
        }
        // no breeding
        return BreedStatus.INACTIVE;
    }

    //// GETTERS AND SETTERS ////

    /**
     * Updates the tank size and iterators
     * @param size the tank size, may be null
     */
    public void setSize(@Nullable TankMultiblock.Size size) {
        if(Objects.equals(this.size, size)) {
            return;
        }
        this.size = size;
        if(size != null) {
            this.isDuplicateFound = false;
            this.insideIterator = size.innerPositions().iterator();
            this.outsideIterator = size.outerPositions().iterator();
            this.forceCalculateBonuses = true;
        } else if(level instanceof ServerLevel serverLevel) {
            clearAllData(serverLevel);
        }
        // send update
        if(this.level != null) {
            this.setChanged();
            level.sendBlockUpdated(this.getBlockPos(), this.getBlockState(), this.getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    public Optional<TankMultiblock.Size> getSize() {
        return Optional.ofNullable(size);
    }

    public void forceCalculateBonuses() {
        this.forceCalculateBonuses = true;
    }

    public void forceCalculateAxolootls() {
        this.forceCalculateAxolootls = true;
    }

    public boolean isOutputFull() {
        return isOutputFull;
    }

    public boolean isInsufficientPower() {
        return isInsufficientPower;
    }

    public void setInsufficientPower(final boolean insufficientPower) {
        if(this.isInsufficientPower != insufficientPower) {
            setChanged();
        }
        this.isInsufficientPower = insufficientPower;
    }

    public boolean isFeedInputEmpty() {
        return isFeedInputEmpty;
    }

    public boolean isBreedInputEmpty() {
        return isBreedInputEmpty;
    }

    public void setFeedInputEmpty(final boolean feedInputEmpty) {
        if(this.isFeedInputEmpty != feedInputEmpty) {
            setChanged();
        }
        this.isFeedInputEmpty = feedInputEmpty;
    }

    public void setBreedInputEmpty(final boolean breedInputEmpty) {
        if(this.isBreedInputEmpty != breedInputEmpty) {
            setChanged();
        }
        this.isBreedInputEmpty = breedInputEmpty;
    }

    public TankStatus getTankStatus() {
        return tankStatus;
    }

    public BreedStatus getBreedStatus() {
        return breedStatus;
    }

    public FeedStatus getFeedStatus() {
        return feedStatus;
    }

    public double getGenerationSpeed() {
        return generationSpeed;
    }

    public double getBreedSpeed() {
        return breedSpeed;
    }

    public double getFeedSpeed() {
        return feedSpeed;
    }

    public boolean enableMobResources() {
        return enableMobResources;
    }

    public boolean enableMobBreeding() {
        return enableMobBreeding;
    }

    public long getResourceGenerationTime() {
        return resourceGenerationTime;
    }

    public long getBreedTime() {
        return breedTime;
    }

    public long getFeedTime() {
        return feedTime;
    }

    public Set<BlockPos> getAxolootlInputs() {
        return ImmutableSet.copyOf(axolootlInputs);
    }

    public Set<BlockPos> getFluidInputs() {
        return ImmutableSet.copyOf(fluidInputs);
    }

    public Set<BlockPos> getEnergyInputs() {
        return ImmutableSet.copyOf(energyInputs);
    }

    public Set<BlockPos> getResourceOutputs() {
        return ImmutableSet.copyOf(resourceOutputs);
    }

    public Map<BlockPos, ResourceLocation> getAquariumModifiers() {
        return ImmutableMap.copyOf(aquariumModifiers);
    }

    public Set<BlockPos> getActiveAquariumModifiers() {
        return ImmutableSet.copyOf(activeAquariumModifiers);
    }

    public Map<UUID, ResourceLocation> getTrackedAxolootls() {
        return ImmutableMap.copyOf(trackedAxolootls);
    }

    /**
     * @return the number of ticks to subtract from the resource generation ticker based on generation speed
     */
    public long getResourceGenerationTickAmount() {
        return Mth.floor(BASE_SPEED_DECREMENT * generationSpeed);
    }

    /**
     * @return the number of ticks to subtract from the breed ticker based on breed speed
     */
    public long getBreedTickAmount() {
        return Mth.floor(BASE_SPEED_DECREMENT * breedSpeed);
    }

    /**
     * @return the number of ticks to subtract from the feed ticker based on feed speed
     */
    public long getFeedTickAmount() {
        return Mth.floor(BASE_SPEED_DECREMENT * feedSpeed);
    }

    /**
     * @param serverLevel the server level
     * @param uuid the entity ID
     * @return the itemstack representation of the axolootl if it was removed
     */
    public ItemStack removeAxolootl(final ServerLevel serverLevel, final UUID uuid) {
        ResourceLocation id = trackedAxolootls.remove(uuid);
        if(null == id) {
            return ItemStack.EMPTY;
        }
        // resolve axolootl
        Entity entity = serverLevel.getEntity(uuid);
        if(!(entity instanceof IAxolootl iprovider && !iprovider.getEntity().isDeadOrDying())) {
            return ItemStack.EMPTY;
        }
        // resolve item stack
        ItemStack itemStack = iprovider.asItemStack();
        // remove entity
        iprovider.getEntity().discard();
        this.forceCalculateBonuses();
        return itemStack;
    }

    /**
     * @param iaxolootl an axolootl to start tracking
     * @return true if the iaxolootl was added
     */
    public boolean addAxolootl(final IAxolootl iaxolootl) {
        final UUID uuid = iaxolootl.getEntity().getUUID();
        final Optional<ResourceLocation> oId = iaxolootl.getAxolootlVariantId();
        if(oId.isEmpty()) {
            return false;
        }
        this.trackedAxolootls.put(uuid, oId.get());
        this.forceCalculateBonuses();
        return true;
    }

    /**
     * @param serverLevel the server level
     * @param random the random source
     * @return a random position in the tank that has water and no collisions
     */
    public Optional<BlockPos> findSpawnablePosition(ServerLevel serverLevel, RandomSource random) {
        // validate tank
        if(!hasTank()) {
            return Optional.empty();
        }
        final Vec3i dimensions = size.getDimensions();
        final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int attempts = Math.max(1, (int) (size.getInnerVolume() / 10));
        // check random positions until one passes
        while(attempts-- > 0) {
            // randomize position
            pos.setWithOffset(size.getOrigin(), 1 + random.nextInt(dimensions.getX() - 2), 1 + random.nextInt(dimensions.getY() - 2), 1 + random.nextInt(dimensions.getZ() - 2));
            // validate position
            if(serverLevel.getFluidState(pos).is(FluidTags.WATER) && serverLevel.noCollision(AxRegistry.EntityReg.AXOLOOTL.get().getAABB(pos.getX() + 0.5D, pos.getY() + 0.05D, pos.getZ() + 0.5D))) {
                return Optional.of(pos);
            }
        }
        // no checks passed
        return Optional.empty();
    }

    /**
     * Iterates the tracked axolootl list and attempts to resolve each entity.
     * If the entity cannot be resolved, it is removed from the tracked axolootl list.
     * @param serverLevel the server level
     * @return a collection of axolootl entities
     */
    public Collection<IAxolootl> resolveAxolootls(ServerLevel serverLevel) {
        return resolveAxolootls(serverLevel, o -> true);
    }

    /**
     * Iterates the tracked axolootl list and attempts to resolve each entity.
     * If the entity cannot be resolved, it is removed from the tracked axolootl list.
     * @param serverLevel the server level
     * @param predicate a predicate for axolootls to resolve
     * @return a collection of axolootl entities
     */
    public Collection<IAxolootl> resolveAxolootls(final ServerLevel serverLevel, final Predicate<IAxolootl> predicate) {
        // create list builder
        final ImmutableList.Builder<IAxolootl> builder = ImmutableList.builder();
        // create set of modifiers that need to be removed
        final Set<UUID> invalid = new HashSet<>();
        // iterate each known axolootl and either add it to the list or mark it to be removed
        for(UUID uuid : trackedAxolootls.keySet()) {
            Entity entity = serverLevel.getEntity(uuid);
            if(entity instanceof IAxolootl iprovider && !iprovider.getEntity().isDeadOrDying()) {
                // test against predicate before adding
                if(predicate.test(iprovider)) {
                    builder.add(iprovider);
                }
            } else {
                invalid.add(uuid);
            }
        }
        // remove invalid axolootls
        invalid.forEach(o -> this.trackedAxolootls.remove(o));
        return builder.build();
    }

    /**
     * Iterates the tracked axolootl variants and attempts to resolve each one.
     * If the variant cannot be resolved, it is removed from the tracked axolootl list.
     * @param registryAccess the registry access
     * @return a map of axolootl IDs and variants
     */
    public Map<UUID, AxolootlVariant> resolveAxolootlVariants(RegistryAccess registryAccess) {
        // create map builder
        final ImmutableMap.Builder<UUID, AxolootlVariant> builder = ImmutableMap.builder();
        // create set of modifiers that need to be removed
        final Set<UUID> invalid = new HashSet<>();
        // iterate each known axolootl and either add it to the map or mark it to be removed
        final Registry<AxolootlVariant> registry = AxolootlVariant.getRegistry(registryAccess);
        for(Map.Entry<UUID, ResourceLocation> entry : trackedAxolootls.entrySet()) {
            Optional<AxolootlVariant> oModifier = registry.getOptional(entry.getValue());
            oModifier.ifPresentOrElse(m -> builder.put(entry.getKey(), m), () -> invalid.add(entry.getKey()));
        }
        // remove invalid axolootls
        invalid.forEach(o -> this.trackedAxolootls.remove(o));
        return builder.build();
    }

    /**
     * @return a map of block positions and energy storage handlers
     */
    public Map<BlockPos, IEnergyStorage> resolveEnergyStorage() {
        return resolveEnergyStorage(i -> true);
    }

    /**
     * @param predicate the predicate to filter energy storage handlers
     * @return a map of block positions and energy storage handlers that pass the given predicate
     */
    public Map<BlockPos, IEnergyStorage> resolveEnergyStorage(final Predicate<IEnergyStorage> predicate) {
        final ImmutableMap.Builder<BlockPos, IEnergyStorage> builder = ImmutableMap.builder();
        for(BlockPos entry : energyInputs) {
            IEnergyStorage storage = resolveEnergyStorageOrVoid(level, entry, false);
            if(predicate.test(storage)) {
                builder.put(entry, storage);
            }
        }
        return builder.build();
    }

    /**
     * Iterates the known modifiers and attempts to resolve each one.
     * If the modifier cannot be resolved, it is removed from the modifier list.
     * @param registryAccess the registry access
     * @return a map of block positions to aquarium modifiers
     */
    public Map<BlockPos, AquariumModifier> resolveModifiers(final RegistryAccess registryAccess) {
        return resolveModifiers(registryAccess, (p, o) -> true);
    }

    /**
     * Iterates the known modifiers and attempts to resolve each one.
     * If the modifier cannot be resolved, it is removed from the modifier list.
     * @param registryAccess the registry access
     * @param predicate a predicate for modifiers to resolve
     * @return a map of block positions to aquarium modifiers
     */
    public Map<BlockPos, AquariumModifier> resolveModifiers(final RegistryAccess registryAccess, final BiPredicate<BlockPos, AquariumModifier> predicate) {
        // create map builder
        final ImmutableMap.Builder<BlockPos, AquariumModifier> builder = ImmutableMap.builder();
        // create set of modifiers that need to be removed
        final Set<BlockPos> invalid = new HashSet<>();
        // iterate each known modifier and either add it to the map or mark it to be removed
        final Registry<AquariumModifier> registry = AquariumModifier.getRegistry(registryAccess);
        for(Map.Entry<BlockPos, ResourceLocation> entry : aquariumModifiers.entrySet()) {
            Optional<AquariumModifier> oModifier = registry.getOptional(entry.getValue());
            oModifier.ifPresentOrElse(m -> {
                // test against predicate before adding to map
                if(predicate.test(entry.getKey(), m)) {
                    builder.put(entry.getKey(), m);
                }
            }, () -> invalid.add(entry.getKey()));
        }
        // remove invalid modifiers
        for(BlockPos p : invalid) {
            Axolootl.LOGGER.warn("Unknown aquarium modifier " + aquariumModifiers.get(p) + " at (" + p.toShortString() + ")");
            aquariumModifiers.remove(p);
        }
        return builder.build();
    }

    //// MENU PROVIDER METHODS ////

    @Override
    public Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    @Override
    public AbstractContainerMenu createMenu(int pContainerId, Inventory pPlayerInventory, Player pPlayer) {
        if(!AxRegistry.AquariumTabsReg.CONTROLLER.get().isAvailable(this)) {
            return null;
        }
        return new ControllerMenu(pContainerId, pPlayerInventory, getBlockPos(), this, getBlockPos(), AxRegistry.AquariumTabsReg.CONTROLLER.get().getSortedIndex(), 0);
    }


    //// BLOCK ENTITY METHODS ////

    // TODO

    //// CLIENT SERVER SYNC ////

    /**
     * Called when the chunk is saved
     * @return the compound tag to use in #handleUpdateTag
     */
    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag);
        // write speeds
        tag.putDouble(KEY_GENERATION_SPEED, generationSpeed);
        tag.putDouble(KEY_FEED_SPEED, feedSpeed);
        tag.putDouble(KEY_BREED_SPEED, breedSpeed);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    //// NBT ////

    private static final String KEY_TANK_STATUS = "Status";
    private static final String KEY_FEED_STATUS = "Feed";
    private static final String KEY_BREED_STATUS = "Breed";
    private static final String KEY_SIZE = "Size";
    private static final String KEY_MODIFIERS = "Modifiers";
    private static final String KEY_MODIFIER = "Modifier";
    private static final String KEY_POS = "Pos";
    private static final String KEY_ACTIVE = "Active";
    private static final String KEY_FLUID_INPUTS = "FluidInputs";
    private static final String KEY_ENERGY_INPUTS = "EnergyInputs";
    private static final String KEY_AXOLOOTL_INPUTS = "AxolootlInputs";
    private static final String KEY_RESOURCE_OUTPUTS = "ResourceOutputs";
    private static final String KEY_AXOLOOTLS = "Axolootls";
    private static final String KEY_UUID = "UUID";
    private static final String KEY_VARIANT = "Variant";
    private static final String KEY_GENERATION_TIME = "GenerationTime";
    private static final String KEY_BREED_TIME = "BreedTime";
    private static final String KEY_FEED_TIME = "FeedTime";
    private static final String KEY_GENERATION_SPEED = "GenerationSpeed";
    private static final String KEY_FEED_SPEED = "FeedSpeed";
    private static final String KEY_BREED_SPEED = "BreedSpeed";

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        // load tank size
        if(tag.contains(KEY_SIZE)) {
            setSize(readFromTag(tag, KEY_SIZE, TankMultiblock.Size.CODEC));
        } else {
            setSize(null);
        }
        // read statuses
        this.tankStatus = TankStatus.getByName(tag.getString(KEY_TANK_STATUS));
        this.feedStatus = FeedStatus.getByName(tag.getString(KEY_FEED_STATUS));
        this.breedStatus = BreedStatus.getByName(tag.getString(KEY_BREED_STATUS));
        // read speeds
        if(tag.contains(KEY_GENERATION_SPEED) && tag.contains(KEY_FEED_SPEED) && tag.contains(KEY_BREED_SPEED)) {
            this.generationSpeed = tag.getDouble(KEY_GENERATION_SPEED);
            this.feedSpeed = tag.getDouble(KEY_FEED_SPEED);
            this.breedSpeed = tag.getDouble(KEY_BREED_SPEED);
            this.forceCalculateBonuses();
        }
        // read tickers
        this.resourceGenerationTime = tag.getLong(KEY_GENERATION_TIME);
        this.breedTime = tag.getLong(KEY_BREED_TIME);
        this.feedTime = tag.getLong(KEY_FEED_TIME);
        // read axolootl map
        this.trackedAxolootls.clear();
        final ListTag axolootlList = tag.getList(KEY_AXOLOOTLS, Tag.TAG_COMPOUND);
        for(int i = 0, n = axolootlList.size(); i < n; i++) {
            CompoundTag entryTag = axolootlList.getCompound(i);
            UUID uuid = entryTag.getUUID(KEY_UUID);
            ResourceLocation variant = new ResourceLocation(entryTag.getString(KEY_VARIANT));
            this.trackedAxolootls.put(uuid, variant);
        }
        // read modifier map
        this.aquariumModifiers.clear();
        this.activeAquariumModifiers.clear();
        final ListTag modifierList = tag.getList(KEY_MODIFIERS, Tag.TAG_COMPOUND);
        for(int i = 0, n = modifierList.size(); i < n; i++) {
            CompoundTag entryTag = modifierList.getCompound(i);
            BlockPos pos = NbtUtils.readBlockPos(entryTag.getCompound(KEY_POS));
            boolean isActive = entryTag.getBoolean(KEY_ACTIVE);
            ResourceLocation modifier = new ResourceLocation(entryTag.getString(KEY_MODIFIER));
            this.aquariumModifiers.put(pos, modifier);
            if(isActive) {
                this.activeAquariumModifiers.add(pos);
            }
        }
        // read sets
        this.fluidInputs.clear();
        this.energyInputs.clear();
        this.axolootlInputs.clear();
        this.resourceOutputs.clear();
        this.fluidInputs.addAll(readBlockPosSet(tag, KEY_FLUID_INPUTS));
        this.energyInputs.addAll(readBlockPosSet(tag, KEY_ENERGY_INPUTS));
        this.axolootlInputs.addAll(readBlockPosSet(tag, KEY_AXOLOOTL_INPUTS));
        this.resourceOutputs.addAll(readBlockPosSet(tag, KEY_RESOURCE_OUTPUTS));
    }

    @Override
    public void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        // write tank size
        if(this.hasTank()) {
            writeToTag(tag, KEY_SIZE, this.size, TankMultiblock.Size.CODEC);
        }
        // write statuses
        tag.putString(KEY_TANK_STATUS, tankStatus.getSerializedName());
        tag.putString(KEY_FEED_STATUS, feedStatus.getSerializedName());
        tag.putString(KEY_BREED_STATUS, breedStatus.getSerializedName());
        // write tickers
        tag.putLong(KEY_GENERATION_TIME, resourceGenerationTime);
        tag.putLong(KEY_BREED_TIME, breedTime);
        tag.putLong(KEY_FEED_TIME, feedTime);
        // write axolootl list
        final ListTag axolootlList = new ListTag();
        for(Map.Entry<UUID, ResourceLocation> entry : trackedAxolootls.entrySet()) {
            CompoundTag entryTag = new CompoundTag();
            entryTag.putUUID(KEY_UUID, entry.getKey());
            entryTag.putString(KEY_VARIANT, entry.getValue().toString());
            axolootlList.add(entryTag);
        }
        tag.put(KEY_AXOLOOTLS, axolootlList);
        // write modifier map
        final ListTag modifierList = new ListTag();
        for(Map.Entry<BlockPos, ResourceLocation> entry : aquariumModifiers.entrySet()) {
            CompoundTag entryTag = new CompoundTag();
            entryTag.put(KEY_POS, NbtUtils.writeBlockPos(entry.getKey()));
            entryTag.putBoolean(KEY_ACTIVE, this.activeAquariumModifiers.contains(entry.getKey()));
            entryTag.putString(KEY_MODIFIER, entry.getValue().toString());
            modifierList.add(entryTag);
        }
        tag.put(KEY_MODIFIERS, modifierList);
        // write sets
        writeBlockPosSet(tag, KEY_FLUID_INPUTS, fluidInputs);
        writeBlockPosSet(tag, KEY_ENERGY_INPUTS, energyInputs);
        writeBlockPosSet(tag, KEY_AXOLOOTL_INPUTS, axolootlInputs);
        writeBlockPosSet(tag, KEY_RESOURCE_OUTPUTS, resourceOutputs);
    }

    private static Set<BlockPos> readBlockPosSet(CompoundTag tag, String key) {
        final Set<BlockPos> set = new HashSet<>();
        final ListTag listTag = tag.getList(key, Tag.TAG_INT_ARRAY);
        for(int i = 0, n = listTag.size(); i < n; i++) {
            int[] array = listTag.getIntArray(i);
            assert(array.length == 3);
            set.add(new BlockPos(array[0], array[1], array[2]));
        }
        return set;
    }

    private static void writeBlockPosSet(CompoundTag tag, String key, Set<BlockPos> set) {
        final ListTag listTag = new ListTag();
        for(BlockPos pos : set) {
            listTag.add(new IntArrayTag(new int[] { pos.getX(), pos.getY(), pos.getZ() }));
        }
        tag.put(key, listTag);
    }

    private static <T> T readFromTag(CompoundTag tag, String key, Codec<T> codec) {
        return codec.parse(NbtOps.INSTANCE, tag.get(key))
                .resultOrPartial(s -> Axolootl.LOGGER.error("[ControllerBlockEntity#readFromTag] Failed to deserialize " + tag.get(key) + " with key \"" + key + "\"\n" + s))
                .orElseThrow();
    }

    private static <T> void writeToTag(CompoundTag tag, String key, T object, Codec<T> codec) {
        tag.put(key, codec.encodeStart(NbtOps.INSTANCE, object)
                .resultOrPartial(s -> Axolootl.LOGGER.error("[ControllerBlockEntity#writeToTag] Failed to serialize " + object.toString() + " with key \"" + key + "\"\n" + s))
                .orElseThrow());
    }
}
