package axolootl.entity;

import axolootl.AxRegistry;
import axolootl.Axolootl;
import axolootl.block.entity.ControllerBlockEntity;
import axolootl.block.entity.IAquariumControllerProvider;
import axolootl.data.AxolootlBreeding;
import axolootl.data.AxolootlVariant;
import axolootl.data.Bonuses;
import com.mojang.math.Vector3f;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.axolotl.Axolotl;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.BabyEntitySpawnEvent;

import javax.annotation.Nullable;
import java.util.Optional;

public class AxolootlEntity extends Axolotl implements IAxolootl, IAquariumControllerProvider {

    public static final EntityDataSerializer<Optional<ResourceLocation>> OPTIONAL_RESOURCE_LOCATION = EntityDataSerializer.optional(FriendlyByteBuf::writeResourceLocation, FriendlyByteBuf::readResourceLocation);

    // DATA //
    private static final EntityDataAccessor<Optional<ResourceLocation>> DATA_VARIANT_ID = SynchedEntityData.defineId(AxolootlEntity.class, OPTIONAL_RESOURCE_LOCATION);
    private static final EntityDataAccessor<Boolean> DATA_ACTIVE_BONUS = SynchedEntityData.defineId(AxolootlEntity.class, EntityDataSerializers.BOOLEAN);

    // CONTROLLER PROVIDER //
    @Nullable
    private BlockPos controllerPos;
    @Nullable
    private ControllerBlockEntity controller;

    // BONUSES //
    private Bonuses bonuses = Bonuses.EMPTY;
    private long bonusDuration;

    // COLORS //
    private static final Vector3f VEC3F_ONE = new Vector3f(1, 1, 1);
    private Vector3f primaryColors;
    private Vector3f secondaryColors;

    // TEXT //
    private Component displayName;

    public AxolootlEntity(EntityType<? extends Axolotl> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
        this.primaryColors = VEC3F_ONE;
        this.secondaryColors = VEC3F_ONE;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Axolotl.createAttributes();
    }

    //// METHODS ////

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_ACTIVE_BONUS, false);
        this.entityData.define(DATA_VARIANT_ID, Optional.empty());
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor pLevel, DifficultyInstance pDifficulty, MobSpawnType pReason, @Nullable SpawnGroupData pSpawnData, @Nullable CompoundTag pDataTag) {
        final SpawnGroupData data = super.finalizeSpawn(pLevel, pDifficulty, pReason, pSpawnData, pDataTag);
        // read variant
        if(pDataTag != null && pDataTag.contains(KEY_VARIANT_ID, Tag.TAG_STRING)) {
            setAxolootlVariantId(new ResourceLocation(pDataTag.getString(KEY_VARIANT_ID)));
        }
        return data;
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        // update bonus duration
        if(bonusDuration > 0 && --bonusDuration <= 0) {
            setBonuses(null);
        }
        // lazy load controller
        this.getController();
    }

    @Override
    public void tick() {
        super.tick();
        if(this.level.isClientSide() && this.isActiveBonus() && this.getRandom().nextInt(10) == 0) {
            final Vec3 vec = position().add(0, this.getBbHeight() * 0.5D, 0);
            final double radius = 0.25D;
            this.level.addParticle(ParticleTypes.ELECTRIC_SPARK,
                    vec.x() + (getRandom().nextDouble() - 0.5D) * 2.0D * radius,
                    vec.y() + (getRandom().nextDouble() - 0.5D) * 2.0D * radius,
                    vec.z() + (getRandom().nextDouble() - 0.5D) * 2.0D * radius,
                    0, 0, 0);
        }
    }

    @Override
    protected Component getTypeName() {
        if(null == displayName) {
            Optional<AxolootlVariant> oVariant = getAxolootlVariant(level.registryAccess());
            if(oVariant.isPresent()) {
                this.displayName = Component.translatable(getType().getDescriptionId() + ".description", super.getTypeName(), oVariant.get().getDescription());
            } else {
                this.displayName = super.getTypeName();
            }
        }
        return displayName;
    }

    @Override
    protected void usePlayerItem(Player pPlayer, InteractionHand pHand, ItemStack pStack) {
        final Optional<Bonuses> oBonuses = getUseFoodResult(pStack);
        oBonuses.ifPresent(bonuses -> setBonuses(bonuses));
        super.usePlayerItem(pPlayer, pHand, pStack);
    }

    /**
     * @param pStack the item stack
     * @return the food bonus, if any
     */
    private Optional<Bonuses> getUseFoodResult(ItemStack pStack) {
        final AxolootlVariant variant = getAxolootlVariant(level.registryAccess()).orElse(AxolootlVariant.EMPTY);
        // check for normal food and no active bonus
        if(this.bonusDuration <= 0) {
            return variant.getFoodBonuses(pStack.getItem());
        }
        return Optional.empty();
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> pKey) {
        super.onSyncedDataUpdated(pKey);
        if(pKey == DATA_VARIANT_ID) {
            this.displayName = null;
            getAxolootlVariant(level.registryAccess()).ifPresentOrElse(a -> {
                // set primary color
                if(a.getPrimaryColor() >= 0) {
                    this.primaryColors = IAxolootl.unpackColor(a.getPrimaryColor());
                } else {
                    this.primaryColors = VEC3F_ONE;
                }
                // set secondary color
                if(a.getSecondaryColor() >= 0) {
                    this.secondaryColors = IAxolootl.unpackColor(a.getSecondaryColor());
                } else {
                    this.secondaryColors = VEC3F_ONE;
                }
            }, () -> {
                // variant is not defined, use fallback colors
                this.primaryColors = VEC3F_ONE;
                this.secondaryColors = VEC3F_ONE;
            });
        }
    }

    //// ANIMAL ////

    @Override
    public boolean canFallInLove() {
        return this.controller != null && super.canFallInLove();
    }

    @Override
    public boolean canBreed() {
        // validate adult
        if(this.isBaby()) {
            return false;
        }
        // validate variant
        final AxolootlVariant variant = getAxolootlVariant(level.registryAccess()).orElse(AxolootlVariant.EMPTY);
        if(null == controller || (variant.hasMobResources() && !controller.enableMobBreeding())) {
            return false;
        }
        return true;
    }

    @Override
    public boolean isFood(ItemStack pStack) {
        final AxolootlVariant variant = getAxolootlVariant(level.registryAccess()).orElse(AxolootlVariant.EMPTY);
        // check for breed food
        /*if(canFallInLove() && variant.getBreedFood().contains(pStack.getItemHolder())) {
            return true;
        }*/
        // check for normal food
        return variant.getFoodBonuses(pStack.getItem()).isPresent();
    }

    @Nullable
    public AgeableMob getBreedOffspring(ServerLevel pLevel, AgeableMob pOtherParent) {
        final AxolootlEntity baby = AxRegistry.EntityReg.AXOLOOTL.get().create(pLevel);
        if(baby != null) {
            baby.setAxolootlVariantId(this.getAxolootlVariantId().orElse(null));
        }
        return baby;
    }

    public Optional<IAxolootl> spawnAxolootlFromBreeding(ServerLevel pLevel, Animal pMate, Holder<AxolootlVariant> variant) {
        // determine variant ID
        final Optional<ResourceKey<AxolootlVariant>> oVariantId = variant.unwrapKey();
        if(oVariantId.isEmpty()) {
            return Optional.empty();
        }
        // create offspring
        AgeableMob ageablemob = this.getBreedOffspring(pLevel, pMate);
        if(!(ageablemob instanceof IAxolootl iaxolootl)) {
            return Optional.empty();
        }
        // update variant ID
        iaxolootl.setAxolootlVariantId(oVariantId.get().location());
        // prepare entity
        ageablemob.setBaby(true);
        ageablemob.moveTo(this.getX(), this.getY(), this.getZ(), 0.0F, 0.0F);
        // fire event
        final BabyEntitySpawnEvent event = new net.minecraftforge.event.entity.living.BabyEntitySpawnEvent(this, pMate, ageablemob);
        final boolean cancelled = MinecraftForge.EVENT_BUS.post(event);
        // reset age and love
        this.setAge(6000);
        pMate.setAge(6000);
        this.resetLove();
        pMate.resetLove();
        // handle event canceled (ignore the proposed entity)
        if (cancelled) {
            return Optional.empty();
        }
        // add the entity to the level
        pLevel.addFreshEntityWithPassengers(ageablemob);
        ageablemob.finalizeSpawn(pLevel, pLevel.getCurrentDifficultyAt(this.blockPosition()), MobSpawnType.BREEDING, null, null);
        pLevel.broadcastEntityEvent(this, EntityEvent.IN_LOVE_HEARTS);
        pLevel.broadcastEntityEvent(pMate, EntityEvent.IN_LOVE_HEARTS);
        return Optional.of(iaxolootl);
    }

    //// BUCKET ////

    @Override
    public boolean fromBucket() {
        return true;
    }

    @Override
    public void setFromBucket(boolean pFromBucket) {}

    @Override
    public boolean requiresCustomPersistence() {
        return true;
    }

    @Override
    public void saveToBucketTag(ItemStack pStack) {
        super.saveToBucketTag(pStack);
        getAxolootlVariantId().ifPresent(id -> pStack.getTag().putString(KEY_VARIANT_ID, id.toString()));
    }

    @Override
    public void loadFromBucketTag(CompoundTag pTag) {
        super.loadFromBucketTag(pTag);
        if(pTag.contains(KEY_VARIANT_ID, Tag.TAG_STRING)) {
            final ResourceLocation id = new ResourceLocation(pTag.getString(KEY_VARIANT_ID));
            this.setAxolootlVariantId(id);
        }
    }

    @Override
    public ItemStack getBucketItemStack() {
        return new ItemStack(AxRegistry.ItemReg.AXOLOOTL_BUCKET.get());
    }

    //// CONTROLLER PROVIDER ////

    @Override
    public void setController(Level level, BlockPos pos, ControllerBlockEntity blockEntity) {
        this.controllerPos = pos;
        this.controller = blockEntity;
    }

    @Override
    public void clearController() {
        this.controllerPos = null;
        this.controller = null;
    }

    @Override
    public Optional<ControllerBlockEntity> getController() {
        // lazy load controller from position
        if(controllerPos != null && null == controller) {
            if(level.getBlockEntity(controllerPos) instanceof ControllerBlockEntity controller) {
                setController(level, controllerPos, controller);
            } else {
                clearController();
            }
        }
        return Optional.ofNullable(this.controller);
    }

    //// IAXOLOOTL ////

    @Override
    public LivingEntity getEntity() {
        return this;
    }

    @Override
    public ItemStack asItemStack() {
        ItemStack stack = getBucketItemStack();
        this.saveToBucketTag(stack);
        return stack;
    }

    @Override
    public void setAxolootlVariantId(@Nullable final ResourceLocation id) {
        getEntityData().set(DATA_VARIANT_ID, Optional.ofNullable(id));
    }

    @Override
    public Optional<ResourceLocation> getAxolootlVariantId() {
        return getEntityData().get(DATA_VARIANT_ID);
    }

    @Override
    public boolean isFeedCandidate(ServerLevel level) {
        return this.bonusDuration <= 0;
    }

    @Override
    public InteractionResult feed(ServerLevel level, ItemStack food) {
        final Optional<Bonuses> oBonuses = this.getUseFoodResult(food);
        if(oBonuses.isPresent()) {
            this.setBonuses(oBonuses.get());
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    @Override
    public boolean isBreedCandidate(ServerLevel level, Optional<IAxolootl> other) {
        // check love and breed status
        if(!(this.canFallInLove() && this.canBreed())) {
            return false;
        }
        // check the specific axolootl, if any
        if(other.isPresent()) {
            final AxolootlVariant selfVariant = this.getAxolootlVariant(level.registryAccess()).orElse(AxolootlVariant.EMPTY);
            final AxolootlVariant otherVariant = other.get().getAxolootlVariant(level.registryAccess()).orElse(AxolootlVariant.EMPTY);
            return AxolootlBreeding.getBreedingRecipe(level, selfVariant, otherVariant).isPresent();
        }
        // all checks passed
        return true;
    }

    @Override
    public Optional<IAxolootl> breed(ServerLevel level, IAxolootl other) {
        // load recipe
        final AxolootlVariant selfVariant = this.getAxolootlVariant(level.registryAccess()).orElse(AxolootlVariant.EMPTY);
        final AxolootlVariant otherVariant = other.getAxolootlVariant(level.registryAccess()).orElse(AxolootlVariant.EMPTY);
        final Optional<AxolootlBreeding> oRecipe = AxolootlBreeding.getBreedingRecipe(level, selfVariant, otherVariant);
        // verify recipe
        if(oRecipe.isEmpty()) {
            return Optional.empty();
        }
        // load result
        Holder<AxolootlVariant> oResult = oRecipe.get().getBreedResult(level, selfVariant, otherVariant, this.getRandom());
        // create axolootl
        Animal parent = (other.getEntity() instanceof Animal animal) ? animal : this;
        return spawnAxolootlFromBreeding(level, parent, oResult);
    }

    @Override
    public double getGenerationSpeed() {
        return bonuses.getGenerationBonus();
    }

    @Override
    public double getFeedSpeed() {
        return bonuses.getFeedBonus();
    }

    @Override
    public double getBreedSpeed() {
        return bonuses.getBreedBonus();
    }

    public void setBonuses(@Nullable final Bonuses bonuses) {
        if(this.bonuses.equals(bonuses)) {
            return;
        }
        if(null == bonuses) {
            this.bonuses = Bonuses.EMPTY;
            this.bonusDuration = 0;
            this.getEntityData().set(DATA_ACTIVE_BONUS, false);
            return;
        }
        this.bonuses = bonuses;
        this.bonusDuration = bonuses.getDuration();
        this.getEntityData().set(DATA_ACTIVE_BONUS, true);
        this.getController().ifPresent(c -> c.forceCalculateBonuses());
    }

    public Bonuses getBonuses() {
        return this.bonuses;
    }

    public boolean isActiveBonus() {
        return this.getEntityData().get(DATA_ACTIVE_BONUS);
    }

    //// COLORS ////

    public Vector3f getPrimaryColors() {
        return primaryColors;
    }

    public Vector3f getSecondaryColors() {
        return secondaryColors;
    }


    //// NBT ////

    private static final String KEY_BONUSES = "Bonuses";
    private static final String KEY_BONUS_DURATION = "BonusDuration";

    @Override
    public void addAdditionalSaveData(CompoundTag pCompound) {
        super.addAdditionalSaveData(pCompound);
        writeAxolootlVariant(pCompound);
        writeControllerPos(controllerPos, pCompound);
        pCompound.putLong(KEY_BONUS_DURATION, bonusDuration);
        pCompound.put(KEY_BONUSES, Bonuses.DIRECT_CODEC.encodeStart(NbtOps.INSTANCE, bonuses)
                .resultOrPartial(s -> Axolootl.LOGGER.error("Failed to write entity bonuses! " + s))
                .orElse(new CompoundTag()));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag pCompound) {
        super.readAdditionalSaveData(pCompound);
        readAxolootlVariant(pCompound);
        this.controllerPos = readControllerPos(pCompound);
        this.bonusDuration = pCompound.getLong(KEY_BONUS_DURATION);
        this.bonuses = Bonuses.DIRECT_CODEC.parse(NbtOps.INSTANCE, pCompound.getCompound(KEY_BONUSES))
                .resultOrPartial(s -> Axolootl.LOGGER.error("Failed to read entity bonuses! " + s))
                .orElse(Bonuses.EMPTY);
        this.getEntityData().set(DATA_ACTIVE_BONUS, this.bonusDuration > 0);
    }
}
