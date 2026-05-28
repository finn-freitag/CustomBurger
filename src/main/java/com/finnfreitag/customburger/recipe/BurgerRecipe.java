package com.finnfreitag.customburger.recipe;

import com.finnfreitag.customburger.Config;
import com.finnfreitag.customburger.Customburger;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

public class BurgerRecipe extends CustomRecipe {
    private final CraftingBookCategory category;

    public BurgerRecipe(CraftingBookCategory category) {
        super(category);
        this.category = category;
    }

    public CraftingBookCategory category() {
        return category;
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        Customburger.LOGGER.info("Checking burger recipe match...");
        int width = input.width();
        int height = input.height();

        boolean hasAny = false;
        for (int i = 0; i < width * height; i++) {
            if (!input.getItem(i).isEmpty()) {
                hasAny = true;
                break;
            }
        }

        if (!hasAny || width != 3 || height != 3) {
            return false;
        }

        ItemStack topLeft = input.getItem(0 * width + 0);
        ItemStack topCenter = input.getItem(0 * width + 1);
        ItemStack topRight = input.getItem(0 * width + 2);

        ItemStack midLeft = input.getItem(1 * width + 0);
        ItemStack midCenter = input.getItem(1 * width + 1);
        ItemStack midRight = input.getItem(1 * width + 2);

        ItemStack botLeft = input.getItem(2 * width + 0);
        ItemStack botCenter = input.getItem(2 * width + 1);
        ItemStack botRight = input.getItem(2 * width + 2);

        if (!topLeft.isEmpty() || !topRight.isEmpty()) {
            return false;
        }
        if (!botLeft.isEmpty() || !botRight.isEmpty()) {
            return false;
        }
        if (!topCenter.is(Items.BREAD) || !botCenter.is(Items.BREAD)) {
            return false;
        }

        int foodCount = 0;
        for (ItemStack mid : List.of(midLeft, midCenter, midRight)) {
            if (mid.isEmpty()) continue;
            if (mid.is(Items.BREAD)) {
                return false;
            }
            if (mid.get(DataComponents.FOOD) == null) {
                return false;
            }
            foodCount++;
        }

        return foodCount >= 1;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        ItemStack burgerResult = new ItemStack(Customburger.BURGER.get());
        List<ItemStack> internalIngredients = new ArrayList<>();

        int width = input.width();
        for (int col = 0; col < 3; col++) {
            ItemStack mid = input.getItem(1 * width + col);
            if (!mid.isEmpty() && mid.get(DataComponents.FOOD) != null && !mid.is(Items.BREAD)) {
                ItemStack ingredientCopy = mid.copy();
                ingredientCopy.setCount(1);
                internalIngredients.add(ingredientCopy);
            }
        }

        burgerResult.set(Customburger.BURGER_CONTENTS.get(), internalIngredients);
        if (Config.enableLogging) {
            Customburger.LOGGER.info("Crafted burger with {} ingredients", internalIngredients.size());
        }
        return burgerResult;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width >= 3 && height >= 3;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return Customburger.BURGER_CRAFTING.get();
    }

    @Override
    public net.minecraft.world.item.crafting.RecipeType<?> getType() {
        return net.minecraft.world.item.crafting.RecipeType.CRAFTING;
    }
}
