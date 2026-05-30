package com.finnfreitag.customburger.recipe;

import com.finnfreitag.customburger.Config;
import com.mojang.authlib.GameProfile;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

public final class RemainderResolver {
    private static final GameProfile REMAINDER_PROFILE = new GameProfile(
            UUID.nameUUIDFromBytes("customburger_remainders".getBytes(StandardCharsets.UTF_8)),
            "[CustomBurger]"
    );

    private RemainderResolver() {
    }

    public static ItemStack getRemainder(ItemStack stack) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return ItemStack.EMPTY;
        }
        ServerLevel level = server.overworld();
        FakePlayer fakePlayer = FakePlayerFactory.get(level, REMAINDER_PROFILE);
        fakePlayer.setSilent(!Config.enableFakePlayerSounds);
        ItemStack copy = stack.copy();
        copy.setCount(1);
        ItemStack result = copy.getItem().finishUsingItem(copy, level, fakePlayer);
        if (result.isEmpty() || result.getItem() == stack.getItem()) {
            return ItemStack.EMPTY;
        }
        result.setCount(1);
        return result;
    }
}
