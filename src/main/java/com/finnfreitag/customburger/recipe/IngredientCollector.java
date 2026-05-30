package com.finnfreitag.customburger.recipe;

import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public final class IngredientCollector {
    private IngredientCollector() {
    }

    public static List<ItemStack> collectFlattenedIngredients(
            Iterable<ItemStack> stacks,
            IngredientPolicy policy,
            NestedIngredientResolver nestedResolver
    ) {
        List<ItemStack> collected = new ArrayList<>();
        for (ItemStack stack : stacks) {
            if (stack.isEmpty()) {
                continue;
            }
            List<ItemStack> nested = nestedResolver.getNestedIngredients(stack);
            if (nested != null && !nested.isEmpty()) {
                for (ItemStack nestedStack : nested) {
                    if (nestedStack.isEmpty()) {
                        continue;
                    }
                    ItemStack ingredientCopy = nestedStack.copy();
                    ingredientCopy.setCount(1);
                    collected.add(ingredientCopy);
                }
                continue;
            }
            if (policy.isAllowedIngredient(stack)) {
                ItemStack ingredientCopy = stack.copy();
                ingredientCopy.setCount(1);
                collected.add(ingredientCopy);
            }
        }
        return collected;
    }

    @FunctionalInterface
    public interface NestedIngredientResolver {
        List<ItemStack> getNestedIngredients(ItemStack stack);
    }
}
