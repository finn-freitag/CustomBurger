package com.finnfreitag.customburger.item;

import com.finnfreitag.customburger.Customburger;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

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
            List<ItemStack> contents = stack.get(Customburger.BURGER_CONTENTS.get());
            if (contents != null) {
                // Apply each ingredient's consumption effects and return any container items.
                for (ItemStack ingredient : contents) {
                    if (ingredient.isEmpty()) continue;

                    ItemStack ingredientCopy = ingredient.copy();
                    ingredientCopy.setCount(1);

                    ItemStack remainder = ingredientCopy.getItem().finishUsingItem(ingredientCopy, level, entity);

                    if (!remainder.isEmpty() && entity instanceof Player player) {
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
        List<ItemStack> contents = stack.get(Customburger.BURGER_CONTENTS.get());
        if (contents != null && !contents.isEmpty()) {
            tooltipLines.add(Component.literal("Ingredients:").withStyle(ChatFormatting.GRAY));
            for (ItemStack ingredient : contents) {
                tooltipLines.add(Component.literal("- ")
                        .append(ingredient.getHoverName())
                        .withStyle(ChatFormatting.GREEN));
            }
        } else {
            tooltipLines.add(Component.literal("Plain Bun").withStyle(ChatFormatting.RED));
        }
        super.appendHoverText(stack, context, tooltipLines, tooltipFlag);
    }
}