package com.finnfreitag.customburger.recipe;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.RecipeSerializer;

public class BurgerRecipeSerializer implements RecipeSerializer<BurgerRecipe> {
    public static final MapCodec<BurgerRecipe> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    CraftingBookCategory.CODEC.optionalFieldOf("category", CraftingBookCategory.MISC)
                            .forGetter(BurgerRecipe::category)
            ).apply(instance, BurgerRecipe::new)
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, BurgerRecipe> STREAM_CODEC =
            StreamCodec.composite(
                    CraftingBookCategory.STREAM_CODEC,
                    BurgerRecipe::category,
                    BurgerRecipe::new
            );

    @Override
    public MapCodec<BurgerRecipe> codec() {
        return CODEC;
    }

    @Override
    public StreamCodec<RegistryFriendlyByteBuf, BurgerRecipe> streamCodec() {
        return STREAM_CODEC;
    }
}
