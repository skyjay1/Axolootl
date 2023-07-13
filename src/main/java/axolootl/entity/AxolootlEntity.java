package axolootl.entity;

import axolootl.Axolootl;
import axolootl.block.entity.IAxolootlVariantProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.animal.axolotl.Axolotl;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.Optional;

public class AxolootlEntity extends Axolotl implements IAxolootlVariantProvider {

    public static final TagKey<Item> AXOLOOTL_FOOD = ForgeRegistries.ITEMS.tags().createTagKey(new ResourceLocation(Axolootl.MODID, "axolootl_food"));
    public static final EntityDataSerializer<Optional<ResourceLocation>> OPTIONAL_RESOURCE_LOCATION = EntityDataSerializer.optional(FriendlyByteBuf::writeResourceLocation, FriendlyByteBuf::readResourceLocation);

    // DATA //
    private static final EntityDataAccessor<Optional<ResourceLocation>> DATA_VARIANT_ID = SynchedEntityData.defineId(AxolootlEntity.class, OPTIONAL_RESOURCE_LOCATION);
    @Nullable
    private BlockPos controllerPos;

    public AxolootlEntity(EntityType<? extends Axolotl> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Axolotl.createAttributes();
    }

    //// METHODS ////

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
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

    //// ANIMAL ////

    @Override
    public boolean isFood(ItemStack pStack) {
        return pStack.is(AXOLOOTL_FOOD) || super.isFood(pStack);
    }

    //// BUCKET ////

    @Override
    public boolean fromBucket() {
        return true;
    }

    @Override
    public void setFromBucket(boolean pFromBucket) {}

    //// CONTROLLER ////

    public Optional<BlockPos> getControllerPos() {
        return Optional.ofNullable(controllerPos);
    }

    public void setControllerPos(@Nullable BlockPos controllerPos) {
        this.controllerPos = controllerPos;
    }


    //// AXOLOOTL VARIANT PROVIDER ////

    @Override
    public LivingEntity getEntity() {
        return this;
    }

    @Override
    public void setAxolootlVariantId(@Nullable final ResourceLocation id) {
        getEntityData().set(DATA_VARIANT_ID, Optional.ofNullable(id));
    }

    @Override
    public Optional<ResourceLocation> getAxolootlVariantId() {
        return getEntityData().get(DATA_VARIANT_ID);
    }


    //// NBT ////

    @Override
    public void addAdditionalSaveData(CompoundTag pCompound) {
        super.addAdditionalSaveData(pCompound);
        writeAxolootlVariant(pCompound);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag pCompound) {
        super.readAdditionalSaveData(pCompound);
        readAxolootlVariant(pCompound);
    }
}
