package axolootl.data.aquarium_modifier;

import axolootl.AxRegistry;
import axolootl.data.aquarium_modifier.condition.ModifierCondition;
import axolootl.data.aquarium_modifier.condition.ModifierSettings;
import axolootl.data.aquarium_modifier.condition.TrueModifierCondition;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.critereon.StatePropertiesPredicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.concurrent.Immutable;
import java.util.Map;
import java.util.Optional;

@Immutable
public class AquariumModifier {

//    public static final Codec<PropertyM> STATE_PROPERTIES_PREDICATE_CODEC = RecordCodecBuilder.create(instance -> instance.group(
//
//    ).apply(instance, ));

    public static final Codec<AquariumModifier> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ModifierSettings.CODEC.fieldOf("settings").forGetter(AquariumModifier::getSettings),
            Vec3i.CODEC.optionalFieldOf("dimensions", new Vec3i(1, 1, 1)).forGetter(AquariumModifier::getDimensions),
            ModifierCondition.DIRECT_CODEC.optionalFieldOf("condition", TrueModifierCondition.INSTANCE).forGetter(AquariumModifier::getCondition)
    ).apply(instance, AquariumModifier::new));



    private final ModifierSettings settings;
    private final Vec3i dimensions;
    private final ModifierCondition condition;

    public AquariumModifier(ModifierSettings settings, Vec3i dimensions, ModifierCondition condition) {
        this.settings = settings;
        this.dimensions = dimensions;
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

    /*
    +forBlock(level: LevelAccessor, pos: BlockPos, state: BlockState): Optional<Aquarium Modifier>
+isApplicable(block: BlockState): boolean
+isActive(context: Aquarium Modifier Context): boolean (determines if this modifier is allowed to give bonus)
+checkAndSpread(context: Aquarium Modifier Context): boolean
+isMultiblock(): boolean (checks if dimensions are not 1,1,1)

     */

    public static Optional<AquariumModifier> forBlock(final LevelAccessor level, final BlockPos pos, final BlockState state) {
        return getRegistry(level.registryAccess()).stream().filter(o -> o.isApplicable(state)).findFirst();
    }

    public boolean isApplicable(final BlockState blockState) {
        return false; // TODO
    }

    //// GETTERS ////

    public ModifierSettings getSettings() {
        return settings;
    }

    public Vec3i getDimensions() {
        return dimensions;
    }

    public ModifierCondition getCondition() {
        return condition;
    }
}
