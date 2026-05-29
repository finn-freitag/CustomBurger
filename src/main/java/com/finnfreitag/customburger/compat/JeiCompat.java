package com.finnfreitag.customburger.compat;

import com.finnfreitag.customburger.Config;
import com.finnfreitag.customburger.item.BurgerContents;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.registration.*;
import mezz.jei.api.registration.ISubtypeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import com.finnfreitag.customburger.recipe.BurgerRecipe;
import com.finnfreitag.customburger.Customburger;

import java.util.List;

@JeiPlugin
public class JeiCompat implements IModPlugin {

    private static final ResourceLocation PLUGIN_ID =
            ResourceLocation.fromNamespaceAndPath("customburger", "jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return PLUGIN_ID;
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        RecipeManager recipeManager = Minecraft.getInstance().level.getRecipeManager();

        List<RecipeHolder<CraftingRecipe>> recipes = recipeManager
                .getAllRecipesFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING)
                .stream()
                .filter(holder -> holder.value() instanceof BurgerRecipe)
                .toList();

        if(Config.enableLogging){
            Customburger.LOGGER.info("JEI: found {} burger recipes", recipes.size());
            for (RecipeHolder<CraftingRecipe> r : recipes) {
                Customburger.LOGGER.info("  - id: {}", r.id());
                Customburger.LOGGER.info("  - class: {}", r.value().getClass().getName());
                Customburger.LOGGER.info("  - ingredients: {}", r.value().getIngredients().size());
                Customburger.LOGGER.info("  - isSpecial: {}", r.value().isSpecial());
                Customburger.LOGGER.info("  - result: {}", r.value().getResultItem(null));
            }
        }

        registration.addRecipes(RecipeTypes.CRAFTING, recipes);

        registration.addIngredientInfo(
                new ItemStack(Customburger.BURGER.get()),
                VanillaTypes.ITEM_STACK,
                Component.translatable("jei.customburger.burger.description")
        );
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(new ItemStack(Items.CRAFTING_TABLE), RecipeTypes.CRAFTING);
    }

    @Override
    public void registerRecipeTransferHandlers(IRecipeTransferRegistration registration) {
        // ensures JEI knows burger recipes can be transferred to crafting table
    }

    @Override
    public void registerIngredients(IModIngredientRegistration registration) {
        // Not needed — burger is a normal ItemStack
    }

    @Override
    public void registerItemSubtypes(ISubtypeRegistration registration) {
        // Tell JEI that burger variants are different based on BURGER_CONTENTS
        registration.registerSubtypeInterpreter(
                VanillaTypes.ITEM_STACK,
                Customburger.BURGER.get(),
                (stack, context) -> {
                    BurgerContents contents = stack.get(Customburger.BURGER_CONTENTS.get());
                    if (contents == null || contents.ingredients().isEmpty()) {
                        return "empty";
                    }
                    return contents.ingredients().stream()
                            .map(s -> s.getItem().toString())
                            .sorted()
                            .collect(java.util.stream.Collectors.joining(","));
                }
        );
    }
}