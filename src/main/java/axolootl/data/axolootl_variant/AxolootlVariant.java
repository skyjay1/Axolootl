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
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.RegistryFileCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.ItemLike;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

public class AxolootlVariant {

    public static final AxolootlVariant EMPTY = new AxolootlVariant(FalseForgeCondition.INSTANCE, "empty", 0, false, Rarity.COMMON, AxolootlModelSettings.EMPTY, 0, ImmutableList.of(), HolderSet.direct(), Holder.direct(EmptyResourceGenerator.INSTANCE));

    private static final Codec<HolderSet<Item>> ITEM_HOLDER_SET_CODEC = RegistryCodecs.homogeneousList(ForgeRegistries.Keys.ITEMS);

    private static final Codec<Rarity> RARITY_CODEC = Codec.STRING.xmap(AxolootlVariant::getRarityByName, r -> r.toString().toLowerCase(Locale.ENGLISH));

    public static final Codec<AxolootlVariant> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        ForgeCondition.DIRECT_CODEC.optionalFieldOf("condition", TrueForgeCondition.INSTANCE).forGetter(AxolootlVariant::getCondition),
        Codec.STRING.fieldOf("translation_key").forGetter(AxolootlVariant::getTranslationKey),
        ExtraCodecs.NON_NEGATIVE_INT.optionalFieldOf("tier", 1).forGetter(AxolootlVariant::getTier),
        Codec.BOOL.optionalFieldOf("fire_immune", false).forGetter(AxolootlVariant::isFireImmune),
        RARITY_CODEC.optionalFieldOf("rarity", Rarity.COMMON).forGetter(AxolootlVariant::getRarity),
        AxolootlModelSettings.CODEC.optionalFieldOf("model", AxolootlModelSettings.EMPTY).forGetter(AxolootlVariant::getModelSettings),
        ExtraCodecs.NON_NEGATIVE_INT.optionalFieldOf("energy_cost", 0).forGetter(AxolootlVariant::getEnergyCost),
        BonusesProvider.CODEC.listOf().optionalFieldOf("food", BonusesProvider.FISH_BONUS_PROVIDERS).forGetter(AxolootlVariant::getFoods),
        ITEM_HOLDER_SET_CODEC.optionalFieldOf("breed_food", HolderSet.direct()).forGetter(AxolootlVariant::getBreedFood),
        ResourceGenerator.HOLDER_CODEC.optionalFieldOf("resource_generator", Holder.direct(EmptyResourceGenerator.INSTANCE)).forGetter(AxolootlVariant::getResourceGenerator)
    ).apply(instance, AxolootlVariant::new));

    public static final Codec<Holder<AxolootlVariant>> HOLDER_CODEC = RegistryFileCodec.create(AxRegistry.Keys.AXOLOOTL_VARIANTS, CODEC);
    public static final Codec<HolderSet<AxolootlVariant>> HOLDER_SET_CODEC = RegistryCodecs.homogeneousList(AxRegistry.Keys.AXOLOOTL_VARIANTS, CODEC);

    /** The requirements for this object to be enabled **/
    private final ForgeCondition condition;
    /** The translation key of the object **/
    private final String translationKey;
    /** The axolootl tier **/
    private final int tier;
    /** True if the entity is immune to fire **/
    private final boolean isFireImmune;
    /** The axolootl rarity **/
    private final Rarity rarity;
    /** The primary packed color **/
    private final AxolootlModelSettings axolootlModelSettings;
    /** The amount of energy that is consumed each time a resource is generated **/
    private final int energyCost;
    /** The map of food to bonuses **/
    private final List<BonusesProvider> foods;
    /** The set of foods that enable breeding **/
    private final HolderSet<Item> breedFood;
    /** The resource generator **/
    private final Holder<ResourceGenerator> resourceGenerator;
    /** The resource generator lookup map **/
    private final Set<ResourceType> resourceTypes;

    /** The translation component **/
    private Component description;
    /** The tier text component **/
    private Component tierDescription;
    /** The registry object holder **/
    private Holder<AxolootlVariant> holder;

    public AxolootlVariant(ForgeCondition condition, String translationKey, int tier, boolean isFireImmune,
                           Rarity rarity, AxolootlModelSettings axolootlModelSettings, int energyCost,
                           List<BonusesProvider> foods, HolderSet<Item> breedFood, Holder<ResourceGenerator> resourceGenerator) {
        this.condition = condition;
        this.translationKey = translationKey;
        this.tier = tier;
        this.isFireImmune = isFireImmune;
        this.rarity = rarity;
        this.axolootlModelSettings = axolootlModelSettings;
        this.energyCost = energyCost;
        this.foods = ImmutableList.copyOf(foods);
        this.breedFood = breedFood;
        this.resourceGenerator = resourceGenerator;
        this.resourceTypes = ImmutableSet.copyOf(resourceGenerator.value().getResourceTypes());
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

    public boolean isFireImmune() {
        return isFireImmune;
    }

    public Rarity getRarity() {
        return rarity;
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

    public Holder<ResourceGenerator> getResourceGenerator() {
        return resourceGenerator;
    }

    public Component getDescription() {
        if(null == this.description) {
            this.description = Component.translatable(getTranslationKey());
        }
        return this.description;
    }

    public Component getTierDescription() {
        if(null == this.tierDescription) {
            this.tierDescription = createTierDescription(this.getTier());
        }
        return this.tierDescription;
    }

    public List<Component> getResourceGeneratorDescription() {
        return this.resourceGenerator.value().getDescriptionText();
    }

    public boolean isEnabled(RegistryAccess registryAccess) {
        return AxRegistry.AxolootlVariantsReg.isValid(registryAccess, this);
    }

    private static Rarity getRarityByName(final String name) {
        for(Rarity rarity : Rarity.values()) {
            if(rarity.toString().toLowerCase(Locale.ENGLISH).equals(name)) {
                return rarity;
            }
        }
        return Rarity.COMMON;
    }

    private static Component createTierDescription(int tier) {
        int value = Math.abs(tier);
        MutableComponent builder = Component.empty();
        while(value >= 100) {
            builder.append(Component.translatable("axolootl.tier." + 100));
            value -= 100;
        }
        while(value >= 50) {
            builder.append(Component.translatable("axolootl.tier." + 50));
            value -= 50;
        }
        while(value >= 40) {
            builder.append(Component.translatable("axolootl.tier." + 40));
            value -= 40;
        }
        while(value >= 10) {
            builder.append(Component.translatable("axolootl.tier." + 10));
            value -= 10;
        }
        if(value > 0 || tier <= 0) {
            builder.append(Component.translatable("axolootl.tier." + value));
        }
        return builder;
    }

    //// OVERRIDES ////

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder("AxolootlVariant{");
        builder.append(" name=" + translationKey);
        builder.append(" model=" + axolootlModelSettings);
        builder.append(" food_bonuses=" + foods.toString());
        builder.append(" breed_food=" + breedFood.toString());
        builder.append(" generators=" + resourceGenerator.toString());
        builder.append("}");
        return super.toString();
    }
}
