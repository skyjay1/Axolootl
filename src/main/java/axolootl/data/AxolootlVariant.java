package axolootl.data;

import axolootl.AxRegistry;
import axolootl.data.aquarium_modifier.AquariumModifier;
import axolootl.data.resource_generator.ResourceGenerator;
import axolootl.data.resource_generator.ResourceType;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.RegistryFileCodec;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AxolootlVariant {

    public static final AxolootlVariant EMPTY = new AxolootlVariant("empty", 0, 0x0, 0x0, 0, new ArrayList<>());

    public static final Codec<AxolootlVariant> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.STRING.fieldOf("translation_key").forGetter(AxolootlVariant::getTranslationKey),
        Codec.INT.optionalFieldOf("tier", 0).forGetter(AxolootlVariant::getTier),
        Codec.INT.optionalFieldOf("primary_color", 0xFFFFFF).forGetter(AxolootlVariant::getPrimaryColor),
        Codec.INT.optionalFieldOf("secondary_color", 0xFFFFFF).forGetter(AxolootlVariant::getSecondaryColor),
        Codec.INT.optionalFieldOf("energy_cost", 0).forGetter(AxolootlVariant::getEnergyCost),
        ResourceGenerator.LIST_OR_SINGLE_CODEC.fieldOf("resource_generator").forGetter(AxolootlVariant::getResourceGenerators)
    ).apply(instance, AxolootlVariant::new));

    public static final Codec<Holder<AxolootlVariant>> HOLDER_CODEC = RegistryFileCodec.create(AxRegistry.Keys.AXOLOOTL_VARIANTS, CODEC);
    public static final Codec<HolderSet<AxolootlVariant>> HOLDER_SET_CODEC = RegistryCodecs.homogeneousList(AxRegistry.Keys.AXOLOOTL_VARIANTS, CODEC);

    /** The translation key of the object **/
    private final String translationKey;
    /** The axolootl tier **/
    private final int tier;
    /** The primary packed color **/
    private final int primaryColor;
    /** The secondary packed color **/
    private final int secondaryColor;
    /** The amount of energy that is consumed each time a resource is generated **/
    private final int energyCost;
    /** Any number of resource generators **/
    private final List<ResourceGenerator> resourceGenerators;
    /** The resource generator lookup map **/
    private final Map<ResourceType, List<ResourceGenerator>> resourceTypeMap;

    /** The translation component **/
    private Component description;

    public AxolootlVariant(String translationKey, int tier, int primaryColor, int secondaryColor, int energyCost, List<ResourceGenerator> resourceGenerators) {
        this.translationKey = translationKey;
        this.tier = tier;
        this.primaryColor = primaryColor;
        this.secondaryColor = secondaryColor;
        this.energyCost = energyCost;
        this.resourceGenerators = ImmutableList.copyOf(resourceGenerators);
        // prepare to build resource type to generator map
        final Map<ResourceType, List<ResourceGenerator>> tempMap = new HashMap<>();
        for(ResourceGenerator item : resourceGenerators) {
            tempMap.computeIfAbsent(item.getResourceType(), type -> new ArrayList<>()).add(item);
        }
        // convert lists to immutable lists
        final ImmutableMap.Builder<ResourceType, List<ResourceGenerator>> builder = new ImmutableMap.Builder<>();
        for(Map.Entry<ResourceType, List<ResourceGenerator>> entry : tempMap.entrySet()) {
            builder.put(entry.getKey(), ImmutableList.copyOf(entry.getValue()));
        }
        // build immutable resource type to generator map
        this.resourceTypeMap = builder.build();
    }

    //// METHODS ////

    /**
     * @param access the registry access
     * @return the axolootl variant registry
     */
    public static Registry<AxolootlVariant> getRegistry(final RegistryAccess access) {
        return access.registryOrThrow(AxRegistry.Keys.AXOLOOTL_VARIANTS);
    }

    public boolean hasResourceGeneratorOfType(final ResourceType type) {
        return this.resourceTypeMap.containsKey(type);
    }

    public List<ResourceGenerator> getResourceGenerators(final ResourceType type) {
        return this.resourceTypeMap.getOrDefault(type, ImmutableList.of());
    }

    public boolean hasMobResources() {
        return hasResourceGeneratorOfType(ResourceType.MOB);
    }

    //// GETTERS ////

    public String getTranslationKey() {
        return translationKey;
    }

    public int getTier() {
        return tier;
    }

    public int getPrimaryColor() {
        return primaryColor;
    }

    public int getSecondaryColor() {
        return secondaryColor;
    }

    public int getEnergyCost() {
        return energyCost;
    }

    public List<ResourceGenerator> getResourceGenerators() {
        return resourceGenerators;
    }

    public Component getDescription() {
        if(null == this.description) {
            this.description = Component.translatable(getTranslationKey());
        }
        return this.description;
    }

    //// OVERRIDES ////

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder("AxolootlVariant{");
        builder.append(" name=" + translationKey);
        builder.append(" colors=(" + primaryColor + ", " + secondaryColor + ")");
        builder.append(" generators=" + resourceGenerators.toString());
        builder.append("}");
        return super.toString();
    }
}
