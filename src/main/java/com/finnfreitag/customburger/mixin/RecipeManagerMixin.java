package com.finnfreitag.customburger.mixin;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.fml.loading.LoadingModList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
import com.google.common.collect.Multimap;

@Mixin(RecipeManager.class)
public class RecipeManagerMixin {
    @Shadow
    private net.minecraft.core.HolderLookup.Provider registries;

    @Shadow
    private Map<ResourceLocation, RecipeHolder<?>> byName;

    @Shadow
    private Multimap<RecipeType<?>, RecipeHolder<?>> byType;

    @Inject(method = "apply", at = @At("TAIL"))
    private void onApply(Map<ResourceLocation, com.google.gson.JsonElement> object, ResourceManager resourceManager, ProfilerFiller profiler, CallbackInfo ci) {
        if (LoadingModList.get().getModFileById("create") != null) {
            Map<ResourceLocation, RecipeHolder<?>> mutableByName = new java.util.HashMap<>(this.byName);
            Multimap<RecipeType<?>, RecipeHolder<?>> mutableByType = com.google.common.collect.ArrayListMultimap.create(this.byType);
            
            com.finnfreitag.customburger.compat.create.CreateCompat.injectRecipes((RecipeManager) (Object) this, this.registries, mutableByName, mutableByType);
            
            this.byName = mutableByName;
            this.byType = mutableByType;
        }
    }
}
