package com.finnfreitag.customburger.mixin;

import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.compat.jei.category.SpoutCategory;
import com.simibubi.create.content.fluids.transfer.FillingRecipe;
import mezz.jei.api.runtime.IIngredientManager;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Consumer;

@Mixin(value = SpoutCategory.class, remap = false)
public class SpoutCategoryMixin {

    @Inject(method = "consumeRecipes", at = @At("TAIL"))
    private static void onConsumeRecipes(Consumer<RecipeHolder<FillingRecipe>> consumer, IIngredientManager ingredientManager, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        com.finnfreitag.customburger.Customburger.LOGGER.info("CustomBurger JEI Spout Mixin: consumeRecipes called. level={}, connection={}", mc.level, mc.getConnection());
        if (mc.level != null && mc.getConnection() != null) {
            RecipeManager recipeManager = mc.getConnection().getRecipeManager();
            int count = 0;
            for (RecipeHolder<?> holder : recipeManager.getAllRecipesFor(AllRecipeTypes.FILLING.getType())) {
                if (holder.id().getNamespace().equals("customburger")) {
                    consumer.accept((RecipeHolder<FillingRecipe>) holder);
                    count++;
                }
            }
            com.finnfreitag.customburger.Customburger.LOGGER.info("CustomBurger JEI Spout Mixin: Injected {} recipes into JEI Spout Category", count);
        }
    }

    @Inject(method = "setRecipe(Lmezz/jei/api/gui/builder/IRecipeLayoutBuilder;Lcom/simibubi/create/content/fluids/transfer/FillingRecipe;Lmezz/jei/api/recipe/IFocusGroup;)V", at = @At("HEAD"), cancellable = true)
    public void onSetRecipe(mezz.jei.api.gui.builder.IRecipeLayoutBuilder builder, FillingRecipe recipe, mezz.jei.api.recipe.IFocusGroup focuses, CallbackInfo ci) {
        if (!recipe.getIngredients().isEmpty() && recipe.getIngredients().get(0).test(new net.minecraft.world.item.ItemStack(com.finnfreitag.customburger.Customburger.BURGER.get()))) {
            ci.cancel();

            // 1. Input slot: plain burger
            builder.addSlot(mezz.jei.api.recipe.RecipeIngredientRole.INPUT, 27, 51)
                    .setBackground(com.simibubi.create.compat.jei.category.CreateRecipeCategory.getRenderedSlot(), -1, -1)
                    .addIngredients(recipe.getIngredients().get(0));

            // 2. Rotating container items slot (instead of the default fluid slot at X=27, Y=32)
            java.util.List<net.minecraft.world.item.ItemStack> rotatingItems = com.finnfreitag.customburger.compat.create.CreateCompat.getSpoutItems();
            builder.addSlot(mezz.jei.api.recipe.RecipeIngredientRole.INPUT, 27, 32)
                    .setBackground(com.simibubi.create.compat.jei.category.CreateRecipeCategory.getRenderedSlot(), -1, -1)
                    .addItemStacks(rotatingItems);

            // 3. Output slot: plain burger
            builder.addSlot(mezz.jei.api.recipe.RecipeIngredientRole.OUTPUT, 132, 51)
                    .setBackground(com.simibubi.create.compat.jei.category.CreateRecipeCategory.getRenderedSlot(), -1, -1)
                    .addItemStack(com.simibubi.create.compat.jei.category.CreateRecipeCategory.getResultItem(recipe));
        }
    }
}

