package com.finnfreitag.customburger.recipe;

import com.finnfreitag.customburger.Config;
import com.finnfreitag.customburger.Customburger;
import com.finnfreitag.customburger.item.BurgerContents;
import com.finnfreitag.customburger.item.BurgerItem;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

public class BurgerRecipe extends CustomRecipe implements CraftingRecipe {
    private final CraftingBookCategory category;
    private final IngredientPolicy ingredientPolicy = new IngredientPolicy();

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

        int bunColumn = findTopBunColumn(input, width);
        if (bunColumn == -1) {
            return false;
        }
        if (!hasBottomBun(input, width, bunColumn)) {
            return false;
        }

        return countAllowedFillings(input, width) >= 1;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        ItemStack burgerResult = new ItemStack(Customburger.BURGER.get());

        List<ItemStack> middleRow = getMiddleRowStacks(input);
        List<ItemStack> internalIngredients = IngredientCollector.collectFlattenedIngredients(
                middleRow,
                ingredientPolicy,
                this::getNestedBurgerIngredients
        );

        BurgerContents contents = new BurgerContents(internalIngredients);
        burgerResult.set(Customburger.BURGER_CONTENTS.get(), contents);
        burgerResult.set(DataComponents.FOOD, BurgerItem.buildAggregateFoodProperties(contents));
        if (Config.enableLogging) {
            Customburger.LOGGER.info("Crafted burger with {} ingredients", internalIngredients.size());
        }
        return burgerResult;
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
        return getRemainingItemsInternal(input);
    }

    private NonNullList<ItemStack> getRemainingItemsInternal(CraftingInput input) {
        NonNullList<ItemStack> remainders = NonNullList.withSize(input.size(), ItemStack.EMPTY);
        if (!Config.dropRemainders || Config.dropRemaindersOnEat) {
            return remainders;
        }

        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            ItemStack remainder = RemainderResolver.getRemainder(stack);
            if (!remainder.isEmpty()) {
                remainders.set(i, remainder);
            }
        }

        return remainders;
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

    @Override
    public ItemStack getToastSymbol() {
        return new ItemStack(Customburger.BURGER.get());
    }

    @Override
    public boolean isSpecial() {
        return false;
    }

    @Override
    public boolean showNotification() {
        return true;
    }

    @Override
    public boolean isIncomplete() {
        return false;
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> list = NonNullList.create();

        Ingredient filling = ingredientPolicy.createFillingIngredient();
        Ingredient bread = Ingredient.of(Items.BREAD);

        // Row 0: [empty, bread, empty]  — bun on top, center column
        list.add(Ingredient.EMPTY);
        list.add(bread);
        list.add(Ingredient.EMPTY);
        // Row 1: [filling, filling, filling] — up to 3 fillings across all columns
        list.add(Ingredient.EMPTY);
        list.add(filling);
        list.add(Ingredient.EMPTY);
        // Row 2: [empty, bread, empty]  — bun on bottom, same center column
        list.add(Ingredient.EMPTY);
        list.add(bread);
        list.add(Ingredient.EMPTY);

        return list;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return new ItemStack(Customburger.BURGER.get());
    }

    private int findTopBunColumn(CraftingInput input, int width) {
        int bunColumn = -1;
        for (int col = 0; col < width; col++) {
            ItemStack top = input.getItem(col);
            if (top.isEmpty()) {
                continue;
            }
            if (!top.is(Items.BREAD)) {
                return -1;
            }
            if (bunColumn != -1) {
                return -1;
            }
            bunColumn = col;
        }
        return bunColumn;
    }

    private boolean hasBottomBun(CraftingInput input, int width, int bunColumn) {
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
        return bottomFound;
    }

    private int countAllowedFillings(CraftingInput input, int width) {
        int foodCount = 0;
        for (int col = 0; col < width; col++) {
            ItemStack mid = input.getItem(width + col);
            if (mid.isEmpty()) {
                continue;
            }
            if (!ingredientPolicy.isAllowedIngredient(mid)) {
                return 0;
            }
            foodCount++;
        }
        return foodCount;
    }

    private List<ItemStack> getMiddleRowStacks(CraftingInput input) {
        int width = input.width();
        List<ItemStack> middleRow = new ArrayList<>(width);
        for (int col = 0; col < width; col++) {
            middleRow.add(input.getItem(width + col));
        }
        return middleRow;
    }

    private List<ItemStack> getNestedBurgerIngredients(ItemStack stack) {
        if (!stack.is(Customburger.BURGER.get())) {
            return List.of();
        }
        BurgerContents nestedContents = stack.getOrDefault(Customburger.BURGER_CONTENTS.get(), BurgerContents.EMPTY);
        if (nestedContents.ingredients().isEmpty()) {
            return List.of();
        }
        return nestedContents.ingredients();
    }
}
