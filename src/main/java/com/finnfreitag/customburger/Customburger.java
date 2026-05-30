package com.finnfreitag.customburger;

import com.finnfreitag.customburger.item.BurgerContents;
import com.finnfreitag.customburger.item.BurgerItem;
import com.finnfreitag.customburger.recipe.BurgerRecipe;
import com.finnfreitag.customburger.recipe.BurgerRecipeSerializer;
import com.mojang.logging.LogUtils;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.slf4j.Logger;

import java.util.List;
import java.util.function.Supplier;

@Mod(Customburger.MODID)
public class Customburger {
    public static final String MODID = "customburger";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    public static final DeferredRegister<DataComponentType<?>> DATA_COMPONENT_TYPES =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, MODID);

    public static final Supplier<DataComponentType<BurgerContents>> BURGER_CONTENTS =
            DATA_COMPONENT_TYPES.register("burger_contents", () ->
                    DataComponentType.<BurgerContents>builder()
                            .persistent(BurgerContents.CODEC)
                            .networkSynchronized(BurgerContents.STREAM_CODEC)
                            .build());

    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, MODID);

    public static final Supplier<RecipeSerializer<BurgerRecipe>> BURGER_CRAFTING =
            RECIPE_SERIALIZERS.register("burger_crafting", BurgerRecipeSerializer::new);

    public static final Supplier<Item> BURGER = ITEMS.register("burger",
            () -> new BurgerItem(new Item.Properties()));

    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    public static final Supplier<CreativeModeTab> BURGER_TAB =
            CREATIVE_TABS.register("burger_tab", () ->
                    CreativeModeTab.builder()
                            .title(Component.literal("CustomBurger"))
                            .icon(() -> new ItemStack(BURGER.get()))
                            .displayItems((params, output) -> {
                                output.accept(BURGER.get());
                            })
                            .build()
            );

    public Customburger(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::commonSetup);

        ITEMS.register(modEventBus);
        DATA_COMPONENT_TYPES.register(modEventBus);
        RECIPE_SERIALIZERS.register(modEventBus);
        CREATIVE_TABS.register(modEventBus);

        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        if (Config.enableLogging) {
            LOGGER.info("Customburger setup complete");
        }
    }
}
