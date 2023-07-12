package axolootl.data.aquarium_modifier;

import axolootl.AxRegistry;
import axolootl.data.aquarium_modifier.condition.ModifierCondition;
import axolootl.data.aquarium_modifier.condition.TrueModifierCondition;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicateType;

import javax.annotation.concurrent.Immutable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

@Immutable
public class AquariumModifier {

    public static final Codec<BlockPredicate> BLOCK_PREDICATE_CODEC = Registry.BLOCK_PREDICATE_TYPES.byNameCodec().dispatch(BlockPredicate::type, BlockPredicateType::codec);
    public static final Codec<List<BlockPredicate>> BLOCK_PREDICATE_LIST_CODEC = BLOCK_PREDICATE_CODEC.listOf();
    public static final Codec<List<BlockPredicate>> BLOCK_PREDICATE_LIST_OR_SINGLE_CODEC = Codec.either(BLOCK_PREDICATE_CODEC, BLOCK_PREDICATE_LIST_CODEC)
            .xmap(either -> either.map(ImmutableList::of, Function.identity()),
                    list -> list.size() == 1 ? Either.left(list.get(0)) : Either.right(list));

    public static final Codec<AquariumModifier> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ModifierSettings.CODEC.fieldOf("settings").forGetter(AquariumModifier::getSettings),
            Vec3i.CODEC.optionalFieldOf("dimensions", new Vec3i(1, 1, 1)).forGetter(AquariumModifier::getDimensions),
            BLOCK_PREDICATE_LIST_OR_SINGLE_CODEC.optionalFieldOf("block", ImmutableList.of(BlockPredicate.not(BlockPredicate.alwaysTrue()))).forGetter(AquariumModifier::getBlockStatePredicate),
            ModifierCondition.DIRECT_CODEC.optionalFieldOf("condition", TrueModifierCondition.INSTANCE).forGetter(AquariumModifier::getCondition)
    ).apply(instance, AquariumModifier::new));

    /** The aquarium modifier settings, such as generation speed and boolean flags **/
    private final ModifierSettings settings;
    /** The width, length, and height of the modifier in the world, used for multiblocks **/
    private final Vec3i dimensions;
    /** The width, length, and height of the modifier in the world, used for multiblocks **/
    private final List<BlockPredicate> blockStatePredicate;
    /** The condition to check each generation cycle to determine if the modifier is active **/
    private final ModifierCondition condition;

    public AquariumModifier(ModifierSettings settings, Vec3i dimensions, List<BlockPredicate> blockStatePredicate, ModifierCondition condition) {
        this.settings = settings;
        this.dimensions = dimensions;
        this.blockStatePredicate = ImmutableList.copyOf(blockStatePredicate);
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
     * @param level the level
     * @param pos the block position
     * @return true if the block at the given position is applicable to this modifier
     */
    public boolean isApplicable(final ServerLevel level, final BlockPos pos) {
        for(BlockPredicate predicate : blockStatePredicate) {
            if(predicate.test(level, pos)) {
                return true;
            }
        }
        return false;
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
     * @return true if a block was placed
     */
    public boolean checkAndSpread(AquariumModifierContext context) {
        final double spreadSpeed = this.settings.getSpreadSpeed();
        // verify can spread
        if(!(spreadSpeed > 0) || context.getLevel().getRandom().nextDouble() > spreadSpeed) {
            return false;
        }
        // find position to spread to
        final BlockState blockState = context.getLevel().getBlockState(context.getPos());
        final Optional<BlockPos> oPos = findSpreadablePosition(context);
        if(oPos.isEmpty()) {
            return false;
        }
        // spread to the calculated position
        if(!context.getLevel().setBlock(oPos.get(), blockState, Block.UPDATE_ALL)) {
            return false;
        }
        context.getLevel().playSound(null, oPos.get(), blockState.getSoundType().getPlaceSound(), SoundSource.BLOCKS, 1.0F, 0.8F + context.getLevel().getRandom().nextFloat() * 0.4F);
        context.getLevel().levelEvent(LevelEvent.PARTICLES_PLANT_GROWTH, oPos.get(), 0);
        return true;
    }

    /**
     * @param context the aquarium modifier context
     * @return the block pos to duplicate this block state, if any
     */
    protected Optional<BlockPos> findSpreadablePosition(final AquariumModifierContext context) {
        final BlockPos origin = context.getPos();
        final BlockState blockState = context.getLevel().getBlockState(origin);
        final BlockPos fromPos = origin.offset(settings.getSpreadSearchDistance().multiply(-1));
        final BlockPos toPos = origin.offset(settings.getSpreadSearchDistance());
        for(BlockPos pos : BlockPos.betweenClosed(fromPos, toPos)) {
            if(blockState.canSurvive(context.getLevel(), pos)) {
                return Optional.of(pos);
            }
        }
        return Optional.empty();
    }

    //// GETTERS ////

    public ModifierSettings getSettings() {
        return settings;
    }

    public Vec3i getDimensions() {
        return new Vec3i(dimensions.getX(), dimensions.getY(), dimensions.getZ());
    }

    public List<BlockPredicate> getBlockStatePredicate() {
        return blockStatePredicate;
    }

    public ModifierCondition getCondition() {
        return condition;
    }
}
