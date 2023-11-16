/**
 * Copyright (c) 2023 Skyler James
 * Permission is granted to use, modify, and redistribute this software, in parts or in whole,
 * under the GNU LGPLv3 license (https://www.gnu.org/licenses/lgpl-3.0.en.html)
 **/

package axolootl.integration;

import axolootl.AxRegistry;
import axolootl.Axolootl;
import axolootl.data.breeding.AxolootlBreeding;
import axolootl.data.breeding.AxolootlBreedingWrapper;
import axolootl.entity.AxolootlEntity;
import com.google.common.collect.ImmutableList;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.ISubtypeRegistration;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.List;

@JeiPlugin
public class JeiAddon implements IModPlugin {

    public static final ResourceLocation UID = new ResourceLocation(Axolootl.MODID, "jei");

    public static final RecipeType<JeiBreedingRecipe> BREEDING_TYPE = RecipeType.create(Axolootl.MODID, "breeding", JeiBreedingRecipe.class);

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
        // load registry
        final RegistryAccess registryAccess = getRegistryAccess();
        final Registry<AxolootlBreeding> registry = AxolootlBreeding.getRegistry(registryAccess);
        final ImmutableList.Builder<JeiBreedingRecipe> builder = ImmutableList.builder();
        // collect non-empty breeding wrappers
        for(AxolootlBreeding entry : registry) {
            // verify
            if(!AxRegistry.AxolootlVariantsReg.isValid(entry.getFirst().unwrapKey().get().location())) {
                continue;
            }
            AxolootlBreedingWrapper wrapper = AxRegistry.AxolootlBreedingReg.getWrapper(registryAccess, entry);
            if(!wrapper.getResult().isEmpty()) {
                // create jei recipe from wrapper
                builder.add(new JeiBreedingRecipe(wrapper));
            }
        }
        // add recipes
        final List<JeiBreedingRecipe> list = builder.build();
        registration.addRecipes(BREEDING_TYPE, builder.build());
        Axolootl.LOGGER.debug("Axolootl sent " + list.size() + " axolootl breeding recipes to JEI");
        // TODO debug JEI recipes not being registered?
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
