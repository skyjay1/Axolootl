package axolootl.data;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import javax.annotation.concurrent.Immutable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Immutable
public class AxolootlVariant {

    public static final AxolootlVariant EMPTY = new AxolootlVariant("empty", 0x0, 0x0, 0, new ArrayList<>());

    public static final Codec<AxolootlVariant> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.STRING.fieldOf("translation_key").forGetter(AxolootlVariant::getTranslationKey),
        Codec.INT.optionalFieldOf("primary_color", 0xFFFFFF).forGetter(AxolootlVariant::getPrimaryColor),
        Codec.INT.optionalFieldOf("secondary_color", 0xFFFFFF).forGetter(AxolootlVariant::getSecondaryColor),
        Codec.INT.optionalFieldOf("energy_cost", 0).forGetter(AxolootlVariant::getEnergyCost),
        ResourceGenerator.LIST_CODEC.fieldOf("resource_generators").forGetter(o -> o.getResourceGenerators())
    ).apply(instance, AxolootlVariant::new));


    private final String translationKey;
    private final int primaryColor;
    private final int secondaryColor;
    private final int energyCost;
    private final List<? extends ResourceGenerator> resourceGenerators;
    private final Map<ResourceType, List<ResourceGenerator>> resourceTypeMap;

    public AxolootlVariant(String translationKey, int primaryColor, int secondaryColor, int energyCost, List<? extends ResourceGenerator> resourceGenerators) {
        this.translationKey = translationKey;
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

    public int getPrimaryColor() {
        return primaryColor;
    }

    public int getSecondaryColor() {
        return secondaryColor;
    }

    public int getEnergyCost() {
        return energyCost;
    }

    /*public <T extends List<? extends ResourceGenerator>> T getResourceGenerators() {
        return (T) resourceGenerators;
    }*/

    public List<? extends ResourceGenerator> getResourceGenerators() {
        return resourceGenerators;
    }
}
