package axolootl.data;

import axolootl.AxRegistry;
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
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class AxolootlVariant {

    public static final AxolootlVariant EMPTY = new AxolootlVariant("empty", 0, 0x0, 0x0, 0, ImmutableList.of(), HolderSet.direct(), new ArrayList<>());

    private static final Codec<HolderSet<Item>> ITEM_HOLDER_SET_CODEC = RegistryCodecs.homogeneousList(ForgeRegistries.Keys.ITEMS);

    public static final Codec<AxolootlVariant> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.STRING.fieldOf("translation_key").forGetter(AxolootlVariant::getTranslationKey),
        Codec.INT.optionalFieldOf("tier", 0).forGetter(AxolootlVariant::getTier),
        Codec.INT.optionalFieldOf("primary_color", 0xFFFFFF).forGetter(AxolootlVariant::getPrimaryColor),
        Codec.INT.optionalFieldOf("secondary_color", 0xFFFFFF).forGetter(AxolootlVariant::getSecondaryColor),
        Codec.INT.optionalFieldOf("energy_cost", 0).forGetter(AxolootlVariant::getEnergyCost),
        BonusesProvider.CODEC.listOf().optionalFieldOf("food", ImmutableList.of()).forGetter(AxolootlVariant::getFoods),
        ITEM_HOLDER_SET_CODEC.optionalFieldOf("breed_food", HolderSet.direct()).forGetter(AxolootlVariant::getBreedFood),
        ResourceGenerator.LIST_OR_SINGLE_CODEC.optionalFieldOf("resource_generator", ImmutableList.of()).forGetter(AxolootlVariant::getResourceGenerators)
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
    /** The map of food to bonuses **/
    private final List<BonusesProvider> foods;
    /** The set of foods that enable breeding **/
    private final HolderSet<Item> breedFood;
    /** Any number of resource generators **/
    private final List<ResourceGenerator> resourceGenerators;
    /** The resource generator lookup map **/
    private final Map<ResourceType, List<ResourceGenerator>> resourceTypeMap;

    /** The translation component **/
    private Component description;

    public AxolootlVariant(String translationKey, int tier, int primaryColor, int secondaryColor, int energyCost,
                           List<BonusesProvider> foods, HolderSet<Item> breedFood, List<ResourceGenerator> resourceGenerators) {
        this.translationKey = translationKey;
        this.tier = tier;
        this.primaryColor = primaryColor;
        this.secondaryColor = secondaryColor;
        this.energyCost = energyCost;
        this.foods = ImmutableList.copyOf(foods);
        this.breedFood = breedFood;
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

    public List<BonusesProvider> getFoods() {
        return foods;
    }

    public Optional<Bonuses> getFoodBonuses(final ItemLike item) {
        // create holder for item
        final Optional<Holder<Item>> holder = ForgeRegistries.ITEMS.getHolder(item.asItem());
        if(!holder.isEmpty()) {
            // find matching holder
            for(BonusesProvider provider : foods) {
                if(provider.getFoods().contains(holder.get())) {
                    return Optional.of(provider.getBonuses());
                }
            }
        }
        return Optional.empty();
    }

    public HolderSet<Item> getBreedFood() {
        return breedFood;
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
        builder.append(" food_bonuses=" + foods.toString());
        builder.append(" breed_food=" + breedFood.toString());
        builder.append(" generators=" + resourceGenerators.toString());
        builder.append("}");
        return super.toString();
    }
}
