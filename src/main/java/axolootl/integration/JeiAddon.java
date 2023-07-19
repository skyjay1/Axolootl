package axolootl.integration;

import axolootl.AxRegistry;
import axolootl.Axolootl;
import axolootl.data.AxolootlBreeding;
import axolootl.entity.AxolootlEntity;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IAdvancedRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.ISubtypeRegistration;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

@JeiPlugin
public class JeiAddon implements IModPlugin {

    public static final ResourceLocation UID = new ResourceLocation(Axolootl.MODID, "jei");

    public static final RecipeType<JeiBreedingWrapper> BREEDING_TYPE = RecipeType.create(Axolootl.MODID, "breeding", JeiBreedingWrapper.class);

    @Override
    public ResourceLocation getPluginUid() {
        return UID;
    }

    @Override
    public void registerItemSubtypes(ISubtypeRegistration registration) {
        registration.registerSubtypeInterpreter(AxRegistry.ItemReg.AXOLOOTL_BUCKET.get(), ((ingredient, context) -> ingredient.getOrCreateTag().getString(AxolootlEntity.KEY_VARIANT_ID)));
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new JeiBreedingCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        final RegistryAccess registryAccess = getRegistryAccess();
        final Registry<AxolootlBreeding> registry = AxolootlBreeding.getRegistry(registryAccess);
        registration.addRecipes(BREEDING_TYPE, registry.stream().map(JeiBreedingWrapper::new).toList());
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(new ItemStack(AxRegistry.BlockReg.BREEDER.get()), BREEDING_TYPE);
    }

    public static RegistryAccess getRegistryAccess() {
        final net.minecraft.client.Minecraft minecraft = net.minecraft.client.Minecraft.getInstance();
        return minecraft.level.registryAccess();
    }
}
