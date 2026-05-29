package com.finnfreitag.customburger.item;

import com.finnfreitag.customburger.Config;
import com.finnfreitag.customburger.Customburger;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Tuple;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

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

                    ItemStack ingredientCopy = ingredient.copy();
                    ingredientCopy.setCount(1);

                    ItemStack remainder = ingredientCopy.getItem().finishUsingItem(ingredientCopy, level, entity);

                    if (!remainder.isEmpty()
                            && Config.dropRemainders
                            && Config.dropRemaindersOnEat
                            && remainder.getItem() != ingredientCopy.getItem()
                            && entity instanceof Player player) {
                        if (!player.getInventory().add(remainder)) {
                            player.drop(remainder, false);
                        }
                    }
                }
            }
        }
        return super.finishUsingItem(stack, level, entity);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltipLines, TooltipFlag tooltipFlag) {
        BurgerContents contents = stack.getOrDefault(Customburger.BURGER_CONTENTS.get(), BurgerContents.EMPTY);
        if (contents != null && !contents.ingredients().isEmpty()) {
            tooltipLines.add(Component.translatable("item.customburger.burger.tooltip").withStyle(ChatFormatting.GRAY));
            ArrayList<Tuple<String, Integer>> ingredientCounts = new ArrayList<>();
            for (ItemStack ingredient : contents.ingredients()) {
                if (ingredient.isEmpty()) continue;

                String ingredientName = ingredient.getHoverName().getString();
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
}