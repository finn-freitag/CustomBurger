package com.finnfreitag.customburger.item;

import com.mojang.serialization.Codec;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public record BurgerContents(List<ItemStack> ingredients) {

    // Canonical constructor — defensive copy + immutability
    public BurgerContents(List<ItemStack> ingredients) {
        this.ingredients = ingredients.stream()
                .sorted(Comparator.comparing((ItemStack stack) -> BuiltInRegistries.ITEM.getKey(stack.getItem()).toString())
                        .thenComparing(stack -> stack.getComponentsPatch().toString()))
                .collect(Collectors.toUnmodifiableList());
    }

    public static final BurgerContents EMPTY = new BurgerContents(List.of());

    public static final Codec<BurgerContents> CODEC =
            ItemStack.CODEC.listOf().xmap(BurgerContents::new, BurgerContents::ingredients);

    public static final StreamCodec<RegistryFriendlyByteBuf, BurgerContents> STREAM_CODEC =
            ItemStack.LIST_STREAM_CODEC.map(BurgerContents::new, BurgerContents::ingredients);

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BurgerContents other)) return false;
        if (ingredients.size() != other.ingredients.size()) return false;
        for (int i = 0; i < ingredients.size(); i++) {
            // Use Minecraft's own item+component equality check
            if (!ItemStack.isSameItemSameComponents(ingredients.get(i), other.ingredients.get(i)))
                return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int result = 1;
        for (ItemStack stack : ingredients) {
            result = 31 * result + BuiltInRegistries.ITEM.getId(stack.getItem());
            result = 31 * result + stack.getComponentsPatch().hashCode();
        }
        return result;
    }
}