package com.finnfreitag.customburger.recipe;

import com.finnfreitag.customburger.Config;
import com.finnfreitag.customburger.Customburger;
import com.finnfreitag.customburger.item.BurgerContents;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.*;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import net.minecraft.world.level.Level;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class BurgerRecipe extends CustomRecipe implements CraftingRecipe {
    private static final GameProfile REMAINDER_PROFILE = new GameProfile(
            UUID.nameUUIDFromBytes("customburger_remainders".getBytes(StandardCharsets.UTF_8)),
            "[CustomBurger]"
    );
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
            if (!isAllowedIngredient(mid)) {
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
                BurgerContents nestedContents = mid.getOrDefault(Customburger.BURGER_CONTENTS.get(), BurgerContents.EMPTY);
                if (nestedContents != null && !nestedContents.ingredients().isEmpty()) {
                    for (ItemStack nested : nestedContents.ingredients()) {
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
            if (isAllowedIngredient(mid)) {
                ItemStack ingredientCopy = mid.copy();
                ingredientCopy.setCount(1);
                internalIngredients.add(ingredientCopy);
            }
        }

        burgerResult.set(Customburger.BURGER_CONTENTS.get(), new BurgerContents(internalIngredients));
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
            ItemStack remainder = getRemainderForCrafting(stack);
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
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> list = NonNullList.create();

        List<ItemStack> foodItems = BuiltInRegistries.ITEM.stream()
                .filter(item -> new ItemStack(item).get(DataComponents.FOOD) != null)
                .map(ItemStack::new)
                .toList();

        List<ItemStack> middleItems = new ArrayList<>(foodItems);
        if (Config.allowPotionIngredients) {
            middleItems.add(new ItemStack(Items.POTION));
            middleItems.add(new ItemStack(Items.SPLASH_POTION));
            middleItems.add(new ItemStack(Items.LINGERING_POTION));
            middleItems.add(new ItemStack(Items.MILK_BUCKET));
        }

        Ingredient filling = Ingredient.of(middleItems.stream());
        Ingredient bread = Ingredient.of(Items.BREAD);

        // Row 0: [empty, bread, empty]  — bun on top, center column
        list.add(Ingredient.EMPTY);
        list.add(bread);
        list.add(Ingredient.EMPTY);
        // Row 1: [filling, filling, filling] — up to 3 fillings across all columns
        list.add(filling);
        list.add(filling);
        list.add(filling);
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

    private ItemStack getRemainderForCrafting(ItemStack stack) {
        if (stack.is(Items.POTION)
                || stack.is(Items.SPLASH_POTION)
                || stack.is(Items.LINGERING_POTION)) {
            return new ItemStack(Items.GLASS_BOTTLE);
        }
        if (stack.is(Items.MILK_BUCKET)) {
            return new ItemStack(Items.BUCKET);
        }
        ItemStack craftingRemainder = stack.getCraftingRemainingItem();
        if (!craftingRemainder.isEmpty()) {
            return craftingRemainder;
        }
        if (stack.get(DataComponents.FOOD) != null) {
            return getEatRemainder(stack);
        }
        return ItemStack.EMPTY;
    }

    private ItemStack getEatRemainder(ItemStack stack) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return ItemStack.EMPTY;
        }
        ServerLevel level = server.overworld();
        if (level == null) {
            return ItemStack.EMPTY;
        }
        FakePlayer fakePlayer = FakePlayerFactory.get(level, REMAINDER_PROFILE);
        ItemStack copy = stack.copy();
        copy.setCount(1);
        ItemStack result = copy.getItem().finishUsingItem(copy, level, fakePlayer);
        if (result.isEmpty() || result.getItem() == stack.getItem()) {
            return ItemStack.EMPTY;
        }
        result.setCount(1);
        return result;
    }

    private boolean isAllowedIngredient(ItemStack stack) {
        if (stack.get(DataComponents.FOOD) != null) {
            return true;
        }
        if (!Config.allowPotionIngredients) {
            return false;
        }
        return stack.is(Items.POTION)
                || stack.is(Items.SPLASH_POTION)
                || stack.is(Items.LINGERING_POTION)
                || stack.is(Items.MILK_BUCKET);
    }
}
