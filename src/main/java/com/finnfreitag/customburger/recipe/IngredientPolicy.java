package com.finnfreitag.customburger.recipe;

import com.finnfreitag.customburger.Config;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.ArrayList;
import java.util.List;

public class IngredientPolicy {
    public boolean isAllowedIngredient(ItemStack stack) {
        if (stack.get(DataComponents.FOOD) != null) {
            return true;
            //return !stack.is(Items.BREAD); // Only for recipe viewer to prevent display bugs (see createFillingItemStacks), but allow bread as an ingredient in actual crafting since it doesn't cause any issues there
        }
        if (!Config.allowPotionIngredients) {
            return false;
        }
        return stack.is(Items.POTION)
                || stack.is(Items.SPLASH_POTION)
                || stack.is(Items.LINGERING_POTION)
                || stack.is(Items.MILK_BUCKET);
    }

    public Ingredient createFillingIngredient() {
        return Ingredient.of(createFillingItemStacks().stream());
    }

    public List<ItemStack> createFillingItemStacks() {
        List<ItemStack> foodItems = BuiltInRegistries.ITEM.stream()
                .filter(item -> item != Items.BREAD && new ItemStack(item).get(DataComponents.FOOD) != null)
                .map(ItemStack::new)
                .toList();

        List<ItemStack> middleItems = new ArrayList<>(foodItems);
        if (Config.allowPotionIngredients) {
            middleItems.add(new ItemStack(Items.POTION));
            middleItems.add(new ItemStack(Items.SPLASH_POTION));
            middleItems.add(new ItemStack(Items.LINGERING_POTION));
            middleItems.add(new ItemStack(Items.MILK_BUCKET));
        }

        return middleItems;
    }
}
