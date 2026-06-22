package com.finnfreitag.customburger.item;

import com.finnfreitag.customburger.Config;
import com.finnfreitag.customburger.Customburger;
import com.finnfreitag.customburger.recipe.RemainderResolver;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Tuple;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.alchemy.PotionContents;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class BurgerItem extends Item {
    public BurgerItem(Properties properties) {
        super(properties.stacksTo(64).food(
                new FoodProperties.Builder()
                        .nutrition(0)
                        .saturationModifier(0.0f)
                        .alwaysEdible()
                        .build()
        ));
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!level.isClientSide) {
            BurgerContents contents = stack.getOrDefault(Customburger.BURGER_CONTENTS.get(), BurgerContents.EMPTY);
            if (contents != null) {
                // Apply each ingredient's consumption effects and return any container items.
                for (ItemStack ingredient : contents.ingredients()) {
                    if (ingredient.isEmpty()) continue;

                    ItemStack effectsCopy = ingredient.copy();
                    effectsCopy.setCount(1);
                    effectsCopy.getItem().finishUsingItem(effectsCopy, level, entity);

                    ItemStack remainder = RemainderResolver.getRemainder(ingredient);
                    if (!remainder.isEmpty()
                            && !ingredient.getOrDefault(Customburger.NO_DROP.get(), false)
                            && Config.dropRemainders
                            && Config.dropRemaindersOnEat
                            && entity instanceof Player player) {
                        if (!player.getInventory().add(remainder)) {
                            player.drop(remainder, false);
                        }
                    }
                }
            }
        }
        // Don't call super — vanilla would apply FoodProperties-based hunger/effects,
        // which we've already handled ingredient by ingredient above.
        // Just shrink the stack and return the remainder as super does:
        stack.consume(1, entity);
        return stack;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltipLines, TooltipFlag tooltipFlag) {
        BurgerContents contents = stack.getOrDefault(Customburger.BURGER_CONTENTS.get(), BurgerContents.EMPTY);
        if (contents != null && !contents.ingredients().isEmpty()) {
            tooltipLines.add(Component.translatable("item.customburger.burger.tooltip").withStyle(ChatFormatting.GRAY));
            ArrayList<Tuple<String, Integer>> ingredientCounts = new ArrayList<>();
            for (ItemStack ingredient : contents.ingredients()) {
                if (ingredient.isEmpty()) continue;

                String ingredientName = getIngredientDisplayName(ingredient);
                boolean found = false;
                for (Tuple<String, Integer> ingredientCount : ingredientCounts) {
                    if (ingredientCount.getA().equals(ingredientName)) {
                        ingredientCount.setB(ingredientCount.getB() + 1);
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    ingredientCounts.add(new Tuple<>(ingredientName, 1));
                }
            }
            for (Tuple<String, Integer> ingredientCount : ingredientCounts) {
                tooltipLines.add(Component.literal("- " + (ingredientCount.getB() > 1 ? ingredientCount.getB() + "x " : "") + ingredientCount.getA()).withStyle(ChatFormatting.GREEN));
            }
        } else {
            //tooltipLines.add(Component.literal("Plain Bun").withStyle(ChatFormatting.RED));
        }
        super.appendHoverText(stack, context, tooltipLines, tooltipFlag);
    }

    @Override
    @Nullable
    public FoodProperties getFoodProperties(ItemStack stack, @Nullable LivingEntity entity) {
        BurgerContents contents = stack.getOrDefault(Customburger.BURGER_CONTENTS.get(), BurgerContents.EMPTY);
        if (contents.ingredients().isEmpty()) {
            // Plain burger — return the base zero-nutrition food so it's still edible
            return super.getFoodProperties(stack, entity);
        }
        return buildAggregateFoodProperties(contents);
    }

    public static FoodProperties buildAggregateFoodProperties(BurgerContents contents) {
        int totalNutrition = 0;
        float totalSaturation = 0.0f;
        //List<FoodProperties.PossibleEffect> allEffects = new ArrayList<>();

        for (ItemStack ingredient : contents.ingredients()) {
            if (ingredient.isEmpty()) continue;
            FoodProperties fp = ingredient.get(DataComponents.FOOD);
            if (fp == null) continue;
            totalNutrition += fp.nutrition();
            // Vanilla saturation is stored as a modifier; effective saturation = nutrition * modifier * 2
            // To combine correctly, accumulate the *effective* saturation points then back-convert later.
            // Simplest correct approach: sum effective saturation (hunger * satMod * 2), convert at end.
            totalSaturation += fp.nutrition() * fp.saturation() * 2.0f;
            //allEffects.addAll(fp.effects());
        }

        // Back-convert total effective saturation to a modifier relative to the combined nutrition.
        // If totalNutrition is 0 (all non-food ingredients), avoid division by zero.
        float saturationModifier = (totalNutrition > 0)
                ? (totalSaturation / (totalNutrition * 2.0f))
                : 0.0f;

        return new FoodProperties(
                totalNutrition,
                saturationModifier,
                true,           // alwaysEdible — keep consistent with original burger
                1.6f,           // eatSeconds — standard value
                Optional.empty(),
                List.of() //allEffects
        );
    }

    private static String getRomanNumeral(int value) {
        return switch (value) {
            case 0 -> "I";
            case 1 -> "II";
            case 2 -> "III";
            case 3 -> "IV";
            case 4 -> "V";
            default -> String.valueOf(value + 1);
        };
    }

    public static String getIngredientDisplayName(ItemStack ingredient) {
        String baseName = ingredient.getHoverName().getString();
        PotionContents potionContents = ingredient.get(DataComponents.POTION_CONTENTS);
        if (potionContents != null) {
            int maxAmplifier = 0;
            int maxDuration = 0;
            boolean hasEffects = false;
            boolean isInfinite = false;
            for (MobEffectInstance effect : potionContents.getAllEffects()) {
                maxAmplifier = Math.max(maxAmplifier, effect.getAmplifier());
                maxDuration = Math.max(maxDuration, effect.getDuration());
                hasEffects = true;
                if (effect.isInfiniteDuration()) {
                    isInfinite = true;
                }
            }
            if (hasEffects) {
                String levelStr = maxAmplifier > 0 ? " " + getRomanNumeral(maxAmplifier) : "";
                String durationStr;
                if (isInfinite) {
                    durationStr = "∞";
                } else {
                    int seconds = maxDuration / 20;
                    int minutes = seconds / 60;
                    seconds = seconds % 60;
                    durationStr = String.format("%d:%02d", minutes, seconds);
                }
                return baseName + levelStr + " (" + durationStr + ")";
            }
        }
        return baseName;
    }
}