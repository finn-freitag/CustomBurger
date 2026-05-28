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
        int width = input.width();
        int height = input.height();

        if (height != 3 || width < 1) {
            return false;
        }

        int bunColumn = -1;
        for (int col = 0; col < width; col++) {
            ItemStack top = input.getItem(col);
            if (top.isEmpty()) {
                continue;
            }
            if (!top.is(Items.BREAD)) {
                return false;
            }
            if (bunColumn != -1) {
                return false;
            }
            bunColumn = col;
        }

        if (bunColumn == -1) {
            return false;
        }

        boolean bottomFound = false;
        for (int col = 0; col < width; col++) {
            ItemStack bottom = input.getItem(2 * width + col);
            if (bottom.isEmpty()) {
                continue;
            }
            if (!bottom.is(Items.BREAD) || col != bunColumn) {
                return false;
            }
            if (bottomFound) {
                return false;
            }
            bottomFound = true;
        }

        if (!bottomFound) {
            return false;
        }

        int foodCount = 0;
        for (int col = 0; col < width; col++) {
            ItemStack mid = input.getItem(1 * width + col);
            if (mid.isEmpty()) {
                continue;
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
        for (int col = 0; col < width; col++) {
            ItemStack mid = input.getItem(1 * width + col);
            if (mid.isEmpty()) {
                continue;
            }
            if (mid.is(Customburger.BURGER.get())) {
                List<ItemStack> nestedContents = mid.get(Customburger.BURGER_CONTENTS.get());
                if (nestedContents != null && !nestedContents.isEmpty()) {
                    for (ItemStack nested : nestedContents) {
                        if (nested.isEmpty()) {
                            continue;
                        }
                        ItemStack ingredientCopy = nested.copy();
                        ingredientCopy.setCount(1);
                        internalIngredients.add(ingredientCopy);
                    }
                    continue;
                }
            }
            if (mid.get(DataComponents.FOOD) != null) {
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
