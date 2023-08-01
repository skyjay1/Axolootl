/**
 * Copyright (c) 2023 Skyler James
 * Permission is granted to use, modify, and redistribute this software, in parts or in whole,
 * under the GNU LGPLv3 license (https://www.gnu.org/licenses/lgpl-3.0.en.html)
 **/

package axolootl.data.axolootl_variant;

import axolootl.AxRegistry;
import axolootl.data.axolootl_variant.condition.FalseForgeCondition;
import axolootl.data.axolootl_variant.condition.ForgeCondition;
import axolootl.data.axolootl_variant.condition.TrueForgeCondition;
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
import net.minecraft.util.ExtraCodecs;
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

    public static final AxolootlVariant EMPTY = new AxolootlVariant(FalseForgeCondition.INSTANCE, "empty", 0, AxolootlModelSettings.EMPTY, 0, ImmutableList.of(), HolderSet.direct(), SimpleWeightedRandomList.empty());

    private static final Codec<HolderSet<Item>> ITEM_HOLDER_SET_CODEC = RegistryCodecs.homogeneousList(ForgeRegistries.Keys.ITEMS);

    public static final Codec<AxolootlVariant> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        ForgeCondition.DIRECT_CODEC.optionalFieldOf("condition", TrueForgeCondition.INSTANCE).forGetter(AxolootlVariant::getCondition),
        Codec.STRING.fieldOf("translation_key").forGetter(AxolootlVariant::getTranslationKey),
        ExtraCodecs.NON_NEGATIVE_INT.optionalFieldOf("tier", 1).forGetter(AxolootlVariant::getTier),
        AxolootlModelSettings.CODEC.optionalFieldOf("model", AxolootlModelSettings.EMPTY).forGetter(AxolootlVariant::getModelSettings),
        ExtraCodecs.NON_NEGATIVE_INT.optionalFieldOf("energy_cost", 0).forGetter(AxolootlVariant::getEnergyCost),
        BonusesProvider.CODEC.listOf().optionalFieldOf("food", BonusesProvider.FISH_BONUS_PROVIDERS).forGetter(AxolootlVariant::getFoods),
        ITEM_HOLDER_SET_CODEC.optionalFieldOf("breed_food", HolderSet.direct()).forGetter(AxolootlVariant::getBreedFood),
        ResourceGenerator.WEIGHTED_LIST_CODEC.optionalFieldOf("resource_generator", SimpleWeightedRandomList.empty()).forGetter(AxolootlVariant::getResourceGenerators)
    ).apply(instance, AxolootlVariant::new));

    public static final Codec<Holder<AxolootlVariant>> HOLDER_CODEC = RegistryFileCodec.create(AxRegistry.Keys.AXOLOOTL_VARIANTS, CODEC);
    public static final Codec<HolderSet<AxolootlVariant>> HOLDER_SET_CODEC = RegistryCodecs.homogeneousList(AxRegistry.Keys.AXOLOOTL_VARIANTS, CODEC);

    /** The requirements for this object to be enabled **/
    private final ForgeCondition condition;
    /** The translation key of the object **/
    private final String translationKey;
    /** The axolootl tier **/
    private final int tier;
    /** The primary packed color **/
    private final AxolootlModelSettings axolootlModelSettings;
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
    /** The resource generator translation component **/
    private List<Component> resourceGeneratorDescription;
    /** The registry object holder **/
    private Holder<AxolootlVariant> holder;

    public AxolootlVariant(ForgeCondition condition, String translationKey, int tier, AxolootlModelSettings axolootlModelSettings, int energyCost,
                           List<BonusesProvider> foods, HolderSet<Item> breedFood, SimpleWeightedRandomList<ResourceGenerator> resourceGenerators) {
        this.condition = condition;
        this.translationKey = translationKey;
        this.tier = tier;
        this.axolootlModelSettings = axolootlModelSettings;
        this.energyCost = energyCost;
        this.foods = ImmutableList.copyOf(foods);
        this.breedFood = breedFood;
        this.resourceGenerators = resourceGenerators;
        this.resourceGeneratorDescription = ImmutableList.copyOf(ResourceGenerator.createDescription(resourceGenerators));
        // build resource type set
        final ImmutableSet.Builder<ResourceType> resourceTypeBuilder = ImmutableSet.builder();
        for(WeightedEntry.Wrapper<ResourceGenerator> item : resourceGenerators.unwrap()) {
            resourceTypeBuilder.addAll(item.getData().getResourceTypes());
        }
        this.resourceTypes = resourceTypeBuilder.build();
    }

    //// METHODS ////

    /**
     * Loads the axolootl variant registry.
     * If you are going to iterate the registry, make sure to check
     * {@link axolootl.AxRegistry.AxolootlVariantsReg#isValid(ResourceLocation)} or
     * {@link axolootl.AxRegistry.AxolootlVariantsReg#isValid(RegistryAccess, AxolootlVariant)}
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
            this.holder =  getRegistry(registryAccess).getOrCreateHolderOrThrow(ResourceKey.create(AxRegistry.Keys.AXOLOOTL_VARIANTS, getRegistryName(registryAccess)));
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

    public ForgeCondition getCondition() {
        return condition;
    }

    public int getTier() {
        return tier;
    }

    public AxolootlModelSettings getModelSettings() {
        return axolootlModelSettings;
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

    public List<Component> getResourceGeneratorDescription() {
        return resourceGeneratorDescription;
    }

    public boolean isEnabled(RegistryAccess registryAccess) {
        return AxRegistry.AxolootlVariantsReg.isValid(registryAccess, this);
    }

    //// OVERRIDES ////

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder("AxolootlVariant{");
        builder.append(" name=" + translationKey);
        builder.append(" model=" + axolootlModelSettings);
        builder.append(" food_bonuses=" + foods.toString());
        builder.append(" breed_food=" + breedFood.toString());
        builder.append(" generators=" + resourceGenerators.toString());
        builder.append("}");
        return super.toString();
    }
}
