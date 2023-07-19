package axolootl.data;

import axolootl.AxRegistry;
import axolootl.data.resource_generator.EmptyResourceGenerator;
import axolootl.data.resource_generator.ResourceGenerator;
import axolootl.data.resource_generator.ResourceType;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.RegistryFileCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.util.random.SimpleWeightedRandomList;
import net.minecraft.util.random.WeightedEntry;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public class AxolootlVariant {

    public static final AxolootlVariant EMPTY = new AxolootlVariant("empty", 0, 0x0, 0x0, 0, ImmutableList.of(), HolderSet.direct(), SimpleWeightedRandomList.empty());

    private static final Codec<HolderSet<Item>> ITEM_HOLDER_SET_CODEC = RegistryCodecs.homogeneousList(ForgeRegistries.Keys.ITEMS);

    public static final Codec<AxolootlVariant> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.STRING.fieldOf("translation_key").forGetter(AxolootlVariant::getTranslationKey),
        Codec.INT.optionalFieldOf("tier", 1).forGetter(AxolootlVariant::getTier),
        Codec.INT.optionalFieldOf("primary_color", -1).forGetter(AxolootlVariant::getPrimaryColor),
        Codec.INT.optionalFieldOf("secondary_color", -1).forGetter(AxolootlVariant::getSecondaryColor),
        Codec.INT.optionalFieldOf("energy_cost", 0).forGetter(AxolootlVariant::getEnergyCost),
        BonusesProvider.CODEC.listOf().optionalFieldOf("food", BonusesProvider.FISH_BONUS_PROVIDERS).forGetter(AxolootlVariant::getFoods),
        ITEM_HOLDER_SET_CODEC.optionalFieldOf("breed_food", HolderSet.direct()).forGetter(AxolootlVariant::getBreedFood),
        ResourceGenerator.WEIGHTED_LIST_CODEC.optionalFieldOf("resource_generator", SimpleWeightedRandomList.empty()).forGetter(AxolootlVariant::getResourceGenerators)
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
    private final SimpleWeightedRandomList<ResourceGenerator> resourceGenerators;
    /** The resource generator lookup map **/
    private final Set<ResourceType> resourceTypes;

    /** The translation component **/
    private Component description;
    /** The registry object holder **/
    private Holder<AxolootlVariant> holder;

    public AxolootlVariant(String translationKey, int tier, int primaryColor, int secondaryColor, int energyCost,
                           List<BonusesProvider> foods, HolderSet<Item> breedFood, SimpleWeightedRandomList<ResourceGenerator> resourceGenerators) {
        this.translationKey = translationKey;
        this.tier = tier;
        this.primaryColor = primaryColor;
        this.secondaryColor = secondaryColor;
        this.energyCost = energyCost;
        this.foods = ImmutableList.copyOf(foods);
        this.breedFood = breedFood;
        this.resourceGenerators = resourceGenerators;
        // build resource type set
        final ImmutableSet.Builder<ResourceType> resourceTypeBuilder = ImmutableSet.builder();
        for(WeightedEntry.Wrapper<ResourceGenerator> item : resourceGenerators.unwrap()) {
            resourceTypeBuilder.addAll(item.getData().getResourceTypes());
        }
        this.resourceTypes = resourceTypeBuilder.build();
    }

    //// METHODS ////

    /**
     * @param access the registry access
     * @return the axolootl variant registry
     */
    public static Registry<AxolootlVariant> getRegistry(final RegistryAccess access) {
        return access.registryOrThrow(AxRegistry.Keys.AXOLOOTL_VARIANTS);
    }

    /**
     * @param registryAccess the registry access
     * @param tagKey a tag key
     * @return true if the tag key contains this object
     */
    public boolean is(final RegistryAccess registryAccess, final TagKey<AxolootlVariant> tagKey) {
        return getHolder(registryAccess).is(tagKey);
    }

    /**
     * @param registryAccess the registry access
     * @return the resource location ID of the object, if present. Not cached.
     */
    public ResourceLocation getRegistryName(final RegistryAccess registryAccess) {
        return Optional.ofNullable(getRegistry(registryAccess).getKey(this)).orElseThrow(() -> new IllegalStateException("Missing key in Axolootl Variant registry for object " + this.toString()));
    }

    /**
     * @param registryAccess the registry access
     * @return the holder for this registry object
     */
    public Holder<AxolootlVariant> getHolder(final RegistryAccess registryAccess) {
        if(null == this.holder) {
            this.holder =  getRegistry(registryAccess).getOrCreateHolderOrThrow(ResourceKey.create(AxRegistry.Keys.AXOLOOTL_VARIANTS, getRegistryName(registryAccess)));;
        }
        return this.holder;
    }

    public boolean hasResourceGeneratorOfType(final ResourceType type) {
        return this.resourceTypes.contains(type);
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

    /**
     * @param item some item
     * @return the first food bonuses applicable for this item, if any
     * @see #getFoods()
     */
    public Optional<Bonuses> getFoodBonuses(final ItemLike item) {
        // create holder for item
        final Optional<Holder<Item>> holder = ForgeRegistries.ITEMS.getHolder(item.asItem());
        if(holder.isPresent()) {
            // find matching holder
            for(BonusesProvider provider : getFoods()) {
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

    public SimpleWeightedRandomList<ResourceGenerator> getResourceGenerators() {
        return resourceGenerators;
    }

    public ResourceGenerator getResourceGenerator(final RandomSource random) {
        return getResourceGenerators().getRandom(random).orElse(WeightedEntry.wrap(EmptyResourceGenerator.INSTANCE, 1)).getData();
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
