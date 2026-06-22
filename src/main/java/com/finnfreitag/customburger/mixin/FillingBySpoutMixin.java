package com.finnfreitag.customburger.mixin;

import com.finnfreitag.customburger.Customburger;
import com.finnfreitag.customburger.item.BurgerContents;
import com.simibubi.create.content.fluids.spout.FillingBySpout;
import com.simibubi.create.content.fluids.transfer.GenericItemFilling;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = FillingBySpout.class, remap = false)
public class FillingBySpoutMixin {

    @Inject(method = "canItemBeFilled", at = @At("HEAD"), cancellable = true)
    private static void onCanItemBeFilled(Level level, ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (stack.is(Customburger.BURGER.get())) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "getRequiredAmountForItem", at = @At("HEAD"), cancellable = true)
    private static void onGetRequiredAmountForItem(Level level, ItemStack stack, FluidStack fluid, CallbackInfoReturnable<Integer> cir) {
        if (stack.is(Customburger.BURGER.get())) {
            int bottleAmount = GenericItemFilling.getRequiredAmountForItem(level, new ItemStack(Items.GLASS_BOTTLE), fluid);
            if (bottleAmount > 0) {
                ItemStack bottleOutput = GenericItemFilling.fillItem(level, bottleAmount, new ItemStack(Items.GLASS_BOTTLE), fluid.copy());
                if (!bottleOutput.isEmpty()) {
                    if (!new com.finnfreitag.customburger.recipe.IngredientPolicy().isAllowedIngredient(bottleOutput)) {
                        cir.setReturnValue(-1);
                        return;
                    }
                    ItemStack checkStack = bottleOutput.copy();
                    checkStack.setCount(1);
                    checkStack.set(Customburger.NO_DROP.get(), true);

                    BurgerContents contents = stack.getOrDefault(Customburger.BURGER_CONTENTS.get(), BurgerContents.EMPTY);
                    boolean alreadyContains = false;
                    for (ItemStack ingredient : contents.ingredients()) {
                        if (ItemStack.isSameItemSameComponents(ingredient, checkStack)) {
                            alreadyContains = true;
                            break;
                        }
                    }
                    if (!alreadyContains) {
                        cir.setReturnValue(bottleAmount);
                        return;
                    }
                }
            }
            cir.setReturnValue(-1);
        }
    }

    @Inject(method = "fillItem", at = @At("HEAD"), cancellable = true)
    private static void onFillItem(Level level, int amount, ItemStack stack, FluidStack fluid, CallbackInfoReturnable<ItemStack> cir) {
        if (stack.is(Customburger.BURGER.get())) {
            int bottleAmount = GenericItemFilling.getRequiredAmountForItem(level, new ItemStack(Items.GLASS_BOTTLE), fluid);
            if (bottleAmount > 0) {
                ItemStack bottleOutput = GenericItemFilling.fillItem(level, bottleAmount, new ItemStack(Items.GLASS_BOTTLE), fluid.copy());
                if (!bottleOutput.isEmpty()) {
                    if (!new com.finnfreitag.customburger.recipe.IngredientPolicy().isAllowedIngredient(bottleOutput)) {
                        cir.setReturnValue(stack);
                        return;
                    }
                    ItemStack checkStack = bottleOutput.copy();
                    checkStack.setCount(1);
                    checkStack.set(Customburger.NO_DROP.get(), true);

                    BurgerContents contents = stack.getOrDefault(Customburger.BURGER_CONTENTS.get(), BurgerContents.EMPTY);
                    
                    boolean alreadyContains = false;
                    for (ItemStack ingredient : contents.ingredients()) {
                        if (ItemStack.isSameItemSameComponents(ingredient, checkStack)) {
                            alreadyContains = true;
                            break;
                        }
                    }
                    
                    if (!alreadyContains) {
                        ItemStack bottleIng = bottleOutput.copy();
                        bottleIng.setCount(1);
                        bottleIng.set(Customburger.NO_DROP.get(), true);
                        
                        java.util.List<ItemStack> newIngredients = new java.util.ArrayList<>(contents.ingredients());
                        newIngredients.add(bottleIng);
                        
                        BurgerContents newContents = new BurgerContents(newIngredients);
                        ItemStack newBurger = new ItemStack(Customburger.BURGER.get());
                        newBurger.set(Customburger.BURGER_CONTENTS.get(), newContents);
                        
                        newBurger.applyComponents(stack.getComponentsPatch());
                        
                        newBurger.set(Customburger.BURGER_CONTENTS.get(), newContents);
                        newBurger.set(net.minecraft.core.component.DataComponents.FOOD, 
                                com.finnfreitag.customburger.item.BurgerItem.buildAggregateFoodProperties(newContents));
                        
                        stack.shrink(1);
                        fluid.shrink(bottleAmount);
                        
                        cir.setReturnValue(newBurger);
                        return;
                    }
                }
            }
            cir.setReturnValue(stack);
        }
    }
}
