package axolootl.block.entity;

import axolootl.AxRegistry;
import axolootl.Axolootl;
import axolootl.data.AxolootlVariant;
import axolootl.data.aquarium_modifier.AquariumModifier;
import axolootl.data.aquarium_modifier.AquariumModifierContext;
import axolootl.data.resource_generator.ResourceGenerator;
import axolootl.data.resource_generator.ResourceType;
import axolootl.entity.AxolootlEntity;
import axolootl.util.TankMultiblock;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
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
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
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

public class ControllerBlockEntity extends BlockEntity {

    // CONSTANTS //
    /** The default resource generation speed **/
    public static final double BASE_GENERATION_SPEED = 1.0D;
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
    public static final TagKey<Block> FLUID_INPUTS = ForgeRegistries.BLOCKS.tags().createTagKey(new ResourceLocation(Axolootl.MODID, "fluid_inputs"));
    public static final TagKey<Block> ENERGY_INPUTS = ForgeRegistries.BLOCKS.tags().createTagKey(new ResourceLocation(Axolootl.MODID, "energy_inputs"));
    public static final TagKey<Block> AXOLOOTL_INPUTS = ForgeRegistries.BLOCKS.tags().createTagKey(new ResourceLocation(Axolootl.MODID, "axolootl_inputs"));
    public static final TagKey<Block> RESOURCE_OUTPUTS = ForgeRegistries.BLOCKS.tags().createTagKey(new ResourceLocation(Axolootl.MODID, "resource_outputs"));

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
    private long resourceGenerationTime;
    private long breedTime;
    private long feedTime;
    /** True to force the block entity to recalculate bonuses in the next tick **/
    private boolean forceCalculateBonuses;

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
    public final BiPredicate<BlockPos, AquariumModifier> activePredicate = (p, o) -> this.activeAquariumModifiers.contains(p);
    public final Predicate<IAxolootlVariantProvider> hasMobResourcePredicate = (a) -> {
        final Optional<AxolootlVariant> oVariant = a.getAxolootlVariant(a.getEntity().level.registryAccess());
        return oVariant.isPresent() && oVariant.get().hasMobResources();
    };

    public ControllerBlockEntity(BlockPos pPos, BlockState pBlockState) {
        this(AxRegistry.BlockEntityReg.CONTROLLER.get(), pPos, pBlockState);
        if(null == this.tankStatus) {
            this.tankStatus = TankStatus.INCOMPLETE;
            this.feedStatus = FeedStatus.INACTIVE;
            this.breedStatus = BreedStatus.INACTIVE;
        }
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
            level.getProfiler().push("axolootlTankSize");
            int blocksToScan = Axolootl.CONFIG.TANK_MULTIBLOCK_UPDATE_CAP.get();
            markDirty |= self.iterateOutside(serverLevel, Mth.ceil(blocksToScan * OUTSIDE_ITERATOR_SCAN));
            // search for, validate, and apply modifiers
            level.getProfiler().popPush("axolootlModifiers");
            markDirty |= self.iterateInside(serverLevel, Mth.ceil(blocksToScan * INSIDE_ITERATOR_SCAN));
            if(self.validateUpdateModifiers(serverLevel) || self.forceCalculateBonuses) {
                markDirty |= self.applyActiveModifiers(serverLevel.registryAccess());
            }
            level.getProfiler().pop();
        }
        // active updates after validating tank size and modifiers
        if(self.getTankStatus().isActive()) {
            // validate and search for axolootl entities
            level.getProfiler().push("axolootlEntities");
            markDirty |= self.validateAxolootls(serverLevel);
            markDirty |= self.findAxolootls(serverLevel);
            // update tickers
            level.getProfiler().popPush("axolootlTickers");
            markDirty |= self.updateTickers(serverLevel);
            // feed, breed, and generate resources
            level.getProfiler().popPush("axolootlFeed");
            markDirty |= self.feed(serverLevel);
            level.getProfiler().popPush("axolootlBreed");
            markDirty |= self.breed(serverLevel);
            level.getProfiler().popPush("axolootlResources");
            markDirty |= self.generateResources(serverLevel);
            level.getProfiler().pop();
        }
        // mark changed and send update
        self.forceCalculateBonuses = false;
        if(markDirty) {
            level.blockEntityChanged(pos);
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
     * @param registryAccess the registry access
     */
    private boolean applyActiveModifiers(RegistryAccess registryAccess) {
        // calculate generation, feed, and breed speeds
        double generationSpeed = BASE_GENERATION_SPEED;
        double feedSpeed = BASE_FEED_SPEED;
        double breedSpeed = BASE_BREED_SPEED;
        boolean enableMobResources = false;
        boolean enableMobBreeding = false;
        for(AquariumModifier entry : resolveModifiers(registryAccess, activePredicate).values()) {
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
        final Collection<TagKey<AquariumModifier>> mandatoryModifiers = AxRegistry.getMandatoryAquariumModifiers(registryAccess);
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
        // iterate over axolootl variants to generate resources
        for(Map.Entry<UUID, ResourceLocation> entry : trackedAxolootls.entrySet()) {
            // verify entity exists
            Entity entity = serverLevel.getEntity(entry.getKey());
            if(!(entity instanceof LivingEntity livingEntity)) {
                invalid.add(entry.getKey());
                continue;
            }
            // verify variant exists
            Optional<AxolootlVariant> oVariant = AxolootlVariant.getRegistry(serverLevel.registryAccess()).getOptional(entry.getValue());
            if(oVariant.isEmpty()) {
                invalid.add(entry.getKey());
                continue;
            }
            // iterate all resource generators
            for(ResourceGenerator gen : oVariant.get().getResourceGenerators()) {
                // verify mob resources are enabled
                if(gen.getResourceType() == ResourceType.MOB && !this.enableMobResources) {
                    continue;
                }
                // generate resources
                resources.addAll(gen.getRandomEntries(livingEntity, livingEntity.getRandom()));
            }
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
     * @return true if the axolootl list changed
     * @param serverLevel the server level
     */
    private boolean feed(ServerLevel serverLevel) {
        // TODO
        return false;
    }

    // BREEDING //

    /**
     * @return true if the axolootl list changed
     * @param serverLevel the server level
     */
    private boolean breed(ServerLevel serverLevel) {
        // TODO
        return false;
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
            // validate tank block
            if(!TankMultiblock.AQUARIUM.isTankBlock(serverLevel, pos)) {
                this.setSize(null);
                return true;
            }
            // add to corresponding set, if applicable
            BlockState blockState = serverLevel.getBlockState(pos);
            if(blockState.is(FLUID_INPUTS)) fluidInputs.add(pos.immutable());
            if(blockState.is(ENERGY_INPUTS)) energyInputs.add(pos.immutable());
            if(blockState.is(AXOLOOTL_INPUTS)) axolootlInputs.add(pos.immutable());
            if(blockState.is(RESOURCE_OUTPUTS)) resourceOutputs.add(pos.immutable());
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
     *
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
            }
        }
        // restart iterator after it is finished
        if(!insideIterator.hasNext() && size != null) {
            insideIterator = size.innerPositions().iterator();
        }
        // report changes
        return foundModifiers;
    }

    /**
     * @param serverLevel the server level
     * @return true if the entity list changed
     */
    private boolean findAxolootls(ServerLevel serverLevel) {
        // validate tank exists
        if(null == this.size) {
            return false;
        }
        // validate needs to update this tick
        if(serverLevel.getGameTime() % AXOLOOTL_SEARCH_INTERVAL != 0) {
            return false;
        }
        // query entities that are not already tracked
        final AABB aabb = this.size.aabb();
        final List<AxolootlEntity> list = serverLevel.getEntitiesOfClass(AxolootlEntity.class, aabb,
                entity -> !trackedAxolootls.containsKey(entity.getUUID()) && entity.getAxolootlVariantId().isPresent());
        // add new entities
        list.forEach(e -> this.trackedAxolootls.put(e.getUUID(), e.getAxolootlVariantId().get()));
        return !list.isEmpty();
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
            }
        }
        // remove invalid entities
        invalid.forEach(uuid -> trackedAxolootls.remove(uuid));
        return !invalid.isEmpty();
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
        final Collection<IAxolootlVariantProvider> axolootls = resolveAxolootls(serverLevel);
        final Map<BlockPos, AquariumModifier> modifierMap = ImmutableMap.copyOf(resolveModifiers(serverLevel.registryAccess()));
        for(Map.Entry<BlockPos, AquariumModifier> entry :modifierMap.entrySet()) {
            // validate modifier
            if(entry.getValue().isApplicable(serverLevel, entry.getKey())) {
                // create context
                final Map<BlockPos, AquariumModifier> contextMap = new HashMap<>(modifierMap);
                contextMap.remove(entry.getKey());
                AquariumModifierContext context = new AquariumModifierContext(serverLevel, entry.getKey(), axolootls, contextMap);
                // validate active
                if(entry.getValue().isActive(context)) {
                    active.add(entry.getKey());
                    // attempt to spread
                    final Optional<BlockPos> oPropogated = entry.getValue().checkAndSpread(context);
                    // add new modifier to map
                    oPropogated.ifPresent(b -> {
                        aquariumModifiers.put(b, entry.getValue().getRegistryName(serverLevel.registryAccess()));
                        // create context to check new modifier immediately
                        contextMap.put(entry.getKey(), entry.getValue());
                        // check if the new modifier is active
                        if(entry.getValue().isActive(new AquariumModifierContext(serverLevel, b, axolootls, contextMap))) {
                            active.add(b);
                        }
                    });
                }
            } else {
                invalid.add(entry.getKey());
            }
        }
        // remove invalid modifiers
        invalid.forEach(p -> aquariumModifiers.remove(p));
        boolean isDirty = !invalid.isEmpty();
        // update active modifier set
        if(!this.activeAquariumModifiers.equals(active)) {
            this.activeAquariumModifiers.clear();
            this.activeAquariumModifiers.addAll(active);
            isDirty = true;
        }
        return isDirty;
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
        if(null == this.size) {
            return TankStatus.INCOMPLETE;
        }
        // check missing modifiers
        if(!this.hasMandatoryModifiers(serverLevel.registryAccess(), true)) {
            return TankStatus.MISSING_MODIFIERS;
        }
        // check entity count is above capacity
        if(this.trackedAxolootls.size() > calculateMaxCapacity(this.size)) {
            return TankStatus.OVERCROWDED;
        }
        // check storage is nonexistent or full
        if(this.resourceOutputs.isEmpty() || isOutputFull()) {
            return TankStatus.STORAGE_FULL;
        }
        // TODO check low power
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
            this.insideIterator = size.innerPositions().iterator();
            this.outsideIterator = size.outerPositions().iterator();
            this.forceCalculateBonuses = true;
        } else {
            this.tankStatus = TankStatus.INCOMPLETE;
            this.trackedAxolootls.clear();
            this.aquariumModifiers.clear();
            this.activeAquariumModifiers.clear();
            this.axolootlInputs.clear();
            this.fluidInputs.clear();
            this.energyInputs.clear();
            this.resourceOutputs.clear();
            this.insideIterator = null;
            this.outsideIterator = null;
        }
        // send update
        if(this.level != null) {
            level.blockEntityChanged(this.getBlockPos());
            level.sendBlockUpdated(this.getBlockPos(), this.getBlockState(), this.getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    public Optional<TankMultiblock.Size> getSize() {
        return Optional.ofNullable(size);
    }

    public boolean isOutputFull() {
        return isOutputFull;
    }

    public boolean isInsufficientPower() {
        return isInsufficientPower;
    }

    public boolean isFeedInputEmpty() {
        return isFeedInputEmpty;
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
        return Mth.floor(BASE_BREED_SPEED * breedSpeed);
    }

    /**
     * @return the number of ticks to subtract from the feed ticker based on feed speed
     */
    public long getFeedTickAmount() {
        return Mth.floor(BASE_FEED_SPEED * feedSpeed);
    }

    /**
     * Iterates the tracked axolootl list and attempts to resolve each entity.
     * If the entity cannot be resolved, it is removed from the tracked axolootl list.
     * @param serverLevel the server level
     * @return a collection of axolootl entities
     */
    public Collection<IAxolootlVariantProvider> resolveAxolootls(ServerLevel serverLevel) {
        return resolveAxolootls(serverLevel, o -> true);
    }

    /**
     * Iterates the tracked axolootl list and attempts to resolve each entity.
     * If the entity cannot be resolved, it is removed from the tracked axolootl list.
     * @param serverLevel the server level
     * @param predicate a predicate for axolootls to resolve
     * @return a collection of axolootl entities
     */
    public Collection<IAxolootlVariantProvider> resolveAxolootls(final ServerLevel serverLevel, final Predicate<IAxolootlVariantProvider> predicate) {
        // create list builder
        final ImmutableList.Builder<IAxolootlVariantProvider> builder = ImmutableList.builder();
        // create set of modifiers that need to be removed
        final Set<UUID> invalid = new HashSet<>();
        // iterate each known axolootl and either add it to the list or mark it to be removed
        for(UUID uuid : trackedAxolootls.keySet()) {
            Entity entity = serverLevel.getEntity(uuid);
            if(entity instanceof IAxolootlVariantProvider iprovider && !iprovider.getEntity().isDeadOrDying()) {
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

    // TODO

    //// BLOCK ENTITY METHODS ////

    // TODO

    //// CLIENT SERVER SYNC ////

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
