package com.finnfreitag.customburger.compat.create;

import com.finnfreitag.customburger.Customburger;
import com.finnfreitag.customburger.item.BurgerContents;
import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.content.fluids.transfer.FillingRecipe;
import com.simibubi.create.content.processing.recipe.StandardProcessingRecipe;
import com.google.common.collect.Multimap;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;

import java.util.Map;

public class CreateCompat {

    public static void injectRecipes(RecipeManager manager, HolderLookup.Provider registries, Map<ResourceLocation, RecipeHolder<?>> byName, Multimap<RecipeType<?>, RecipeHolder<?>> byType) {
        RecipeType<?> fillingType = AllRecipeTypes.FILLING.getType();
        java.util.Collection<RecipeHolder<?>> fillingRecipes = byType.get(fillingType);
        
        java.util.List<RecipeHolder<?>> newRecipes = new java.util.ArrayList<>();
        
        for (RecipeHolder<?> holder : fillingRecipes) {
            if (holder.value() instanceof FillingRecipe recipe) {
                if (recipe.getIngredients().isEmpty()) continue;
                Ingredient itemIng = recipe.getIngredients().get(0);
                if (itemIng.test(new ItemStack(Items.GLASS_BOTTLE))) {
                    if (recipe.getFluidIngredients().isEmpty()) continue;
                    
                    ResourceLocation originalId = holder.id();
                    ResourceLocation newId = ResourceLocation.fromNamespaceAndPath(
                            "customburger", 
                            "spout_filling/" + originalId.getNamespace() + "/" + originalId.getPath()
                    );
                    
                    ItemStack bottleOutput = recipe.getResultItem(registries);
                    if (bottleOutput.isEmpty()) continue;
                    if (!new com.finnfreitag.customburger.recipe.IngredientPolicy().isAllowedIngredient(bottleOutput)) continue;
                    
                    ItemStack outputBurger = new ItemStack(Customburger.BURGER.get());
                    
                    ItemStack bottleIng = bottleOutput.copy();
                    bottleIng.setCount(1);
                    bottleIng.set(Customburger.NO_DROP.get(), true);
                    
                    BurgerContents newContents = new BurgerContents(java.util.List.of(bottleIng));
                    outputBurger.set(Customburger.BURGER_CONTENTS.get(), newContents);
                    outputBurger.set(net.minecraft.core.component.DataComponents.FOOD, 
                            com.finnfreitag.customburger.item.BurgerItem.buildAggregateFoodProperties(newContents));
                    
                    SizedFluidIngredient fluidIng = recipe.getRequiredFluid();
                    
                    try {
                        FillingRecipe burgerRecipe = new StandardProcessingRecipe.Builder<>(FillingRecipe::new, newId)
                                .withItemIngredients(Ingredient.of(Customburger.BURGER.get()))
                                .withFluidIngredients(fluidIng)
                                .withSingleItemOutput(outputBurger)
                                .build();
                        
                        RecipeHolder<FillingRecipe> newHolder = new RecipeHolder<>(newId, burgerRecipe);
                        newRecipes.add(newHolder);
                    } catch (Exception e) {
                        Customburger.LOGGER.error("Failed to build Spout recipe for CustomBurger compatibility: ", e);
                    }
                }
            }
        }
        
        for (RecipeHolder<?> newHolder : newRecipes) {
            byName.put(newHolder.id(), newHolder);
            byType.put(fillingType, newHolder);
        }
        
        Customburger.LOGGER.info("CustomBurger compat: Injected {} Spout recipes", newRecipes.size());
    }
}
