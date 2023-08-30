/**
 * Copyright (c) 2023 Skyler James
 * Permission is granted to use, modify, and redistribute this software, in parts or in whole,
 * under the GNU LGPLv3 license (https://www.gnu.org/licenses/lgpl-3.0.en.html)
 **/

package axolootl.data.aquarium_modifier;

import axolootl.AxRegistry;
import axolootl.data.aquarium_modifier.condition.FalseModifierCondition;
import axolootl.data.aquarium_modifier.condition.ModifierCondition;
import axolootl.data.aquarium_modifier.condition.TrueModifierCondition;
import axolootl.util.AxCodecUtils;
import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.RegistryFileCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import java.util.List;
import java.util.Optional;

public class AquariumModifier {

    public static final AquariumModifier EMPTY = new AquariumModifier("empty", ModifierSettings.EMPTY, new Vec3i(1, 1, 1), BlockPredicate.not(BlockPredicate.alwaysTrue()), FalseModifierCondition.INSTANCE);

    public static final Codec<AquariumModifier> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("translation_key").forGetter(AquariumModifier::getTranslationKey),
            ModifierSettings.CODEC.fieldOf("settings").forGetter(AquariumModifier::getSettings),
            AxCodecUtils.POSITIVE_VEC3I_CODEC.optionalFieldOf("dimensions", new Vec3i(1, 1, 1)).forGetter(AquariumModifier::getDimensions),
            AxCodecUtils.BLOCK_PREDICATE_CODEC.optionalFieldOf("block", BlockPredicate.not(BlockPredicate.alwaysTrue())).forGetter(AquariumModifier::getBlockStatePredicate),
            ModifierCondition.DIRECT_CODEC.optionalFieldOf("condition", TrueModifierCondition.INSTANCE).forGetter(AquariumModifier::getCondition)
    ).apply(instance, AquariumModifier::new));

    public static final Codec<Holder<AquariumModifier>> HOLDER_CODEC = RegistryFileCodec.create(AxRegistry.Keys.AQUARIUM_MODIFIERS, CODEC);
    public static final Codec<HolderSet<AquariumModifier>> HOLDER_SET_CODEC = RegistryCodecs.homogeneousList(AxRegistry.Keys.AQUARIUM_MODIFIERS, CODEC);

    /** The translation key of the object **/
    private final String translationKey;
    /** The aquarium modifier settings, such as generation speed and boolean flags **/
    private final ModifierSettings settings;
    /** The width, length, and height of the modifier in the world, used for multiblocks **/
    private final Vec3i dimensions;
    /** The predicate to determine which block states are valid candidates for this aquarium modifier **/
    private final BlockPredicate blockStatePredicate;
    /** The condition to check each generation cycle to determine if the modifier is active **/
    private final ModifierCondition condition;

    /** The translation component **/
    private Component description;
    private Holder<AquariumModifier> holder;

    public AquariumModifier(String translationKey, ModifierSettings settings, Vec3i dimensions, BlockPredicate blockStatePredicate, ModifierCondition condition) {
        this.translationKey = translationKey;
        this.settings = settings;
        this.dimensions = dimensions;
        this.blockStatePredicate = blockStatePredicate;
        this.condition = condition;
    }

    //// METHODS ////

    /**
     * @param access the registry access
     * @return the axolootl variant registry
     */
    public static Registry<AquariumModifier> getRegistry(final RegistryAccess access) {
        return access.registryOrThrow(AxRegistry.Keys.AQUARIUM_MODIFIERS);
    }

    /**
     * @param level the level
     * @param pos the block position
     * @return the first aquarium modifier suitable for the given position, if any
     */
    public static Optional<AquariumModifier> forBlock(final LevelAccessor level, final BlockPos pos) {
        final ServerLevel serverLevel = (ServerLevel) level;
        for(AquariumModifier entry : getRegistry(level.registryAccess())) {
            if(entry.isApplicable(serverLevel, pos)) {
                return Optional.of(entry);
            }
        }
        return Optional.empty();
    }

    /**
     * @param registryAccess the registry access
     * @param tagKey a tag key
     * @return true if the tag key contains this object
     */
    public boolean is(final RegistryAccess registryAccess, final TagKey<AquariumModifier> tagKey) {
        return getHolder(registryAccess).is(tagKey);
    }

    public ResourceLocation getRegistryName(final RegistryAccess registryAccess) {
        return Optional.ofNullable(getRegistry(registryAccess).getKey(this)).orElseThrow(() -> new IllegalStateException("Missing key in AquariumModifier registry for object " + this.toString()));
    }

    public List<TagKey<AquariumModifier>> getReverseTags(final RegistryAccess access) {
        final Holder<AquariumModifier> self = getHolder(access);
        return getRegistry(access).getTags().filter(pair -> pair.getSecond().contains(self)).map(Pair::getFirst).toList();
    }

    /**
     * @param level the level
     * @param pos the block position
     * @return true if the block at the given position is applicable to this modifier
     */
    public boolean isApplicable(final ServerLevel level, final BlockPos pos) {
        return blockStatePredicate.test(level, pos);
    }

    /**
     * @param context the aquarium modifier context
     * @return true if the aquarium modifier meets all conditions to be active
     */
    public boolean isActive(final AquariumModifierContext context) {
        return condition.test(context);
    }

    /** @return true if the block dimensions are greater than 1x1x1 **/
    public boolean isMultiblock() {
        return dimensions.getX() > 1 || dimensions.getY() > 1 || dimensions.getZ() > 1;
    }

    /**
     * Checks if the modifier can spread and attempts to replicate
     * @param context the aquarium modifier context
     * @return the block position for the block that was placed, if any
     */
    public Optional<BlockPos> checkAndSpread(AquariumModifierContext context) {
        final double spreadSpeed = this.settings.getSpreadSpeed();
        // verify can spread
        if(!(spreadSpeed > 0) || context.getLevel().getRandom().nextDouble() > spreadSpeed) {
            return Optional.empty();
        }
        // find position to spread to
        final BlockState blockState = context.getLevel().getBlockState(context.getPos());
        final Optional<BlockPos> oPos = findSpreadablePosition(context);
        if(oPos.isEmpty()) {
            return Optional.empty();
        }
        final BlockPos pos = oPos.get().immutable();
        // spread to the calculated position
        if(!context.getLevel().setBlock(pos, blockState, Block.UPDATE_ALL)) {
            return Optional.empty();
        }
        context.getLevel().playSound(null, pos, blockState.getSoundType().getPlaceSound(), SoundSource.BLOCKS, 1.0F, 0.8F + context.getLevel().getRandom().nextFloat() * 0.4F);
        context.getLevel().levelEvent(LevelEvent.PARTICLES_PLANT_GROWTH, pos, 0);
        return Optional.of(pos);
    }

    /**
     * @param context the aquarium modifier context
     * @return the block pos to duplicate this block state, if any
     */
    protected Optional<BlockPos> findSpreadablePosition(final AquariumModifierContext context) {
        if (!context.hasTank()) {
            return Optional.empty();
        }
        // prepare to search block positions
        final BoundingBox bounds = context.getTankSize().boundingBox();
        final Vec3i spreadDistance = settings.getSpreadSearchDistance();
        final BlockPos origin = context.getPos();
        final BlockState blockState = context.getLevel().getBlockState(origin);
        // clamp from and to positions within the tank size bounds
        BlockPos fromPos = new BlockPos(
                Math.max(bounds.minX(), origin.getX() - spreadDistance.getX()),
                Math.max(bounds.minY(), origin.getY() - spreadDistance.getY()),
                Math.max(bounds.minZ(), origin.getZ() - spreadDistance.getZ()));
        BlockPos toPos = new BlockPos(
                Math.min(bounds.maxX(), origin.getX() + spreadDistance.getX()),
                Math.min(bounds.maxY(), origin.getY() + spreadDistance.getY()),
                Math.min(bounds.maxZ(), origin.getZ() + spreadDistance.getZ()));
        // iterate block positions until one is found that can support the modifier block state
        int volume = (spreadDistance.getX() * 2 + 1) * (spreadDistance.getY() * 2 + 1) * (spreadDistance.getZ() * 2 + 1);
        int chance = context.getLevel().getRandom().nextInt(volume);
        for(BlockPos pos : BlockPos.betweenClosed(fromPos, toPos)) {
            // validate random check
            if(--chance > 0 && context.getLevel().getRandom().nextInt(chance) != 0) {
                continue;
            }
            // validate replaceable
            BlockState replacing = context.getLevel().getBlockState(pos);
            if(!replacing.getMaterial().isReplaceable()) {
                continue;
            }
            // validate can survive
            if(!blockState.canSurvive(context.getLevel(), pos)) {
                continue;
            }
            // all checks passed
            return Optional.of(pos);
        }
        return Optional.empty();
    }

    //// GETTERS ////

    public String getTranslationKey() {
        return translationKey;
    }

    public ModifierSettings getSettings() {
        return settings;
    }

    public Vec3i getDimensions() {
        return new Vec3i(dimensions.getX(), dimensions.getY(), dimensions.getZ());
    }

    public BlockPredicate getBlockStatePredicate() {
        return blockStatePredicate;
    }

    public ModifierCondition getCondition() {
        return condition;
    }

    public Component getDescription() {
        if(null == this.description) {
            this.description = Component.translatable(getTranslationKey());
        }
        return this.description;
    }

    public Holder<AquariumModifier> getHolder(final RegistryAccess registryAccess) {
        if(null == this.holder) {
            final Registry<AquariumModifier> registry = getRegistry(registryAccess);
            this.holder = registry.getOrCreateHolderOrThrow(ResourceKey.create(AxRegistry.Keys.AQUARIUM_MODIFIERS, getRegistryName(registryAccess)));
        }
        return this.holder;
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder("AquariumModifier{");
        builder.append(" name=" + translationKey);
        builder.append(" settings=" + settings);
        builder.append(" predicate=" + blockStatePredicate);
        builder.append(" condition=" + condition);
        builder.append(" }");
        return builder.toString();
    }
}
