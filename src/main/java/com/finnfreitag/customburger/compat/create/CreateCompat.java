package com.finnfreitag.customburger.compat.create;

import com.finnfreitag.customburger.Customburger;
import com.finnfreitag.customburger.item.BurgerContents;
import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.content.fluids.transfer.FillingRecipe;
import com.simibubi.create.content.processing.recipe.StandardProcessingRecipe;
import com.google.common.collect.Multimap;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;

import java.util.Map;

public class CreateCompat {

    public static void injectRecipes(RecipeManager manager, HolderLookup.Provider registries, Map<ResourceLocation, RecipeHolder<?>> byName, Multimap<RecipeType<?>, RecipeHolder<?>> byType) {
        RecipeType<?> fillingType = AllRecipeTypes.FILLING.getType();
        java.util.Collection<RecipeHolder<?>> fillingRecipes = byType.get(fillingType);
        
        java.util.List<net.neoforged.neoforge.fluids.FluidStack> collectedFluids = new java.util.ArrayList<>();
        Customburger.LOGGER.info("CreateCompat: injectRecipes entered. Doing direct fluid lookup.");

        net.minecraft.world.level.material.Fluid honeyFluid = null;
        net.minecraft.world.level.material.Fluid milkFluid = null;
        net.minecraft.world.level.material.Fluid potionFluid = null;

        for (java.util.Map.Entry<net.minecraft.resources.ResourceKey<net.minecraft.world.level.material.Fluid>, net.minecraft.world.level.material.Fluid> entry : net.minecraft.core.registries.BuiltInRegistries.FLUID.entrySet()) {
            ResourceLocation id = entry.getKey().location();
            String path = id.getPath();
            String namespace = id.getNamespace();
            if (namespace.equals("create")) {
                if (path.contains("flowing")) {
                    continue;
                }
                if (path.contains("honey")) {
                    honeyFluid = entry.getValue();
                } else if (path.contains("milk")) {
                    milkFluid = entry.getValue();
                } else if (path.contains("potion")) {
                    potionFluid = entry.getValue();
                }
            }
        }

        Customburger.LOGGER.info("CreateCompat: Direct fluids found: honey={}, milk={}, potion={}", 
                honeyFluid != null ? net.minecraft.core.registries.BuiltInRegistries.FLUID.getKey(honeyFluid) : "null", 
                milkFluid != null ? net.minecraft.core.registries.BuiltInRegistries.FLUID.getKey(milkFluid) : "null", 
                potionFluid != null ? net.minecraft.core.registries.BuiltInRegistries.FLUID.getKey(potionFluid) : "null");

        if (com.finnfreitag.customburger.Config.allowPotionIngredients) {
            collectedFluids.add(new net.neoforged.neoforge.fluids.FluidStack(net.minecraft.world.level.material.Fluids.WATER, 250));
            if (milkFluid != null) {
                collectedFluids.add(new net.neoforged.neoforge.fluids.FluidStack(milkFluid, 250));
            }
            if (potionFluid != null) {
                com.finnfreitag.customburger.recipe.IngredientPolicy policy = new com.finnfreitag.customburger.recipe.IngredientPolicy();
                for (net.minecraft.world.item.alchemy.Potion potion : net.minecraft.core.registries.BuiltInRegistries.POTION) {
                    net.minecraft.core.Holder<net.minecraft.world.item.alchemy.Potion> holder =
                            net.minecraft.core.registries.BuiltInRegistries.POTION.wrapAsHolder(potion);

                    ItemStack potionStack = net.minecraft.world.item.alchemy.PotionContents.createItemStack(Items.POTION, holder);
                    if (policy.isAllowedSpoutIngredient(potionStack)) {
                        net.minecraft.world.item.alchemy.PotionContents contents = potionStack.get(net.minecraft.core.component.DataComponents.POTION_CONTENTS);
                        if (contents != null) {
                            net.neoforged.neoforge.fluids.FluidStack stack = new net.neoforged.neoforge.fluids.FluidStack(potionFluid, 250);
                            stack.set(net.minecraft.core.component.DataComponents.POTION_CONTENTS, contents);
                            collectedFluids.add(stack);
                        }
                    }
                }
            }
        }
        if (honeyFluid != null) {
            collectedFluids.add(new net.neoforged.neoforge.fluids.FluidStack(honeyFluid, 250));
        }
        
        if (!collectedFluids.isEmpty()) {
            ResourceLocation newId = ResourceLocation.fromNamespaceAndPath("customburger", "spout_filling/burger");
            
            try {
                net.neoforged.neoforge.fluids.crafting.FluidIngredient unionIngredient = 
                        net.neoforged.neoforge.fluids.crafting.FluidIngredient.of(collectedFluids.toArray(new net.neoforged.neoforge.fluids.FluidStack[0]));
                SizedFluidIngredient fluidIng = new SizedFluidIngredient(unionIngredient, 250);
                
                ItemStack outputBurger = new ItemStack(Customburger.BURGER.get());
                
                FillingRecipe burgerRecipe = new StandardProcessingRecipe.Builder<>(FillingRecipe::new, newId)
                        .withItemIngredients(Ingredient.of(Customburger.BURGER.get()))
                        .withFluidIngredients(fluidIng)
                        .withSingleItemOutput(outputBurger)
                        .build();
                
                RecipeHolder<FillingRecipe> newHolder = new RecipeHolder<>(newId, burgerRecipe);
                
                byName.put(newHolder.id(), newHolder);
                byType.put(fillingType, newHolder);
                
                Customburger.LOGGER.info("CustomBurger compat: Injected Spout recipe for Burger with {} fluids", collectedFluids.size());
            } catch (Exception e) {
                Customburger.LOGGER.error("Failed to build Spout recipe for CustomBurger compatibility: ", e);
            }
        }
    }

    public static java.util.List<ItemStack> getSpoutItems() {
        java.util.List<ItemStack> items = new java.util.ArrayList<>();
        com.finnfreitag.customburger.recipe.IngredientPolicy policy = new com.finnfreitag.customburger.recipe.IngredientPolicy();

        // 1. Honey bottle
        ItemStack honey = new ItemStack(Items.HONEY_BOTTLE);
        if (policy.isAllowedSpoutIngredient(honey)) {
            items.add(honey);
        }

        // 2. Milk bucket
        ItemStack milk = new ItemStack(Items.MILK_BUCKET);
        if (policy.isAllowedSpoutIngredient(milk)) {
            items.add(milk);
        }

        // 3. Potions
        if (com.finnfreitag.customburger.Config.allowPotionIngredients) {
            for (net.minecraft.world.item.alchemy.Potion potion : net.minecraft.core.registries.BuiltInRegistries.POTION) {
                net.minecraft.core.Holder<net.minecraft.world.item.alchemy.Potion> holder =
                        net.minecraft.core.registries.BuiltInRegistries.POTION.wrapAsHolder(potion);

                ItemStack potionStack = net.minecraft.world.item.alchemy.PotionContents.createItemStack(Items.POTION, holder);
                if (policy.isAllowedSpoutIngredient(potionStack)) {
                    items.add(potionStack);
                }

                ItemStack splashStack = net.minecraft.world.item.alchemy.PotionContents.createItemStack(Items.SPLASH_POTION, holder);
                if (policy.isAllowedSpoutIngredient(splashStack)) {
                    items.add(splashStack);
                }

                ItemStack lingeringStack = net.minecraft.world.item.alchemy.PotionContents.createItemStack(Items.LINGERING_POTION, holder);
                if (policy.isAllowedSpoutIngredient(lingeringStack)) {
                    items.add(lingeringStack);
                }
            }
        }

        return items;
    }

    public static class SpoutFillingResult {
        public final int amount;
        public final ItemStack output;

        public SpoutFillingResult(int amount, ItemStack output) {
            this.amount = amount;
            this.output = output;
        }
    }

    public static SpoutFillingResult findFillingResult(net.minecraft.world.level.Level level, net.neoforged.neoforge.fluids.FluidStack fluid) {
        ResourceLocation fluidId = net.minecraft.core.registries.BuiltInRegistries.FLUID.getKey(fluid.getFluid());
        Customburger.LOGGER.info("FillingBySpoutMixin: findFillingResult called for fluid: {}, id: {}, amount: {}", 
                fluid.getHoverName().getString(), fluidId, fluid.getAmount());
        
        // 1. Try Glass Bottle
        int amount = com.simibubi.create.content.fluids.transfer.GenericItemFilling.getRequiredAmountForItem(level, new ItemStack(Items.GLASS_BOTTLE), fluid);
        Customburger.LOGGER.info("  Glass Bottle amount: {}", amount);
        if (amount > 0) {
            ItemStack output = com.simibubi.create.content.fluids.transfer.GenericItemFilling.fillItem(level, amount, new ItemStack(Items.GLASS_BOTTLE), fluid.copy());
            Customburger.LOGGER.info("  Glass Bottle output: {}", output);
            if (!output.isEmpty()) {
                return new SpoutFillingResult(amount, output);
            }
        }

        // Honey manual fallback (e.g. if the Spout recipe query fails)
        if (fluidId != null && fluidId.getPath().contains("honey")) {
            Customburger.LOGGER.info("  Honey manual fallback triggered!");
            return new SpoutFillingResult(250, new ItemStack(Items.HONEY_BOTTLE));
        }

        // 2. Try Bucket
        amount = com.simibubi.create.content.fluids.transfer.GenericItemFilling.getRequiredAmountForItem(level, new ItemStack(Items.BUCKET), fluid);
        Customburger.LOGGER.info("  Bucket amount: {}", amount);
        if (amount > 0) {
            ItemStack output = com.simibubi.create.content.fluids.transfer.GenericItemFilling.fillItem(level, amount, new ItemStack(Items.BUCKET), fluid.copy());
            Customburger.LOGGER.info("  Bucket output: {}", output);
            if (!output.isEmpty()) {
                return new SpoutFillingResult(amount, output);
            }
        }

        return null;
    }
}

