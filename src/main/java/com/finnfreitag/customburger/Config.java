package com.finnfreitag.customburger;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

@EventBusSubscriber(modid = Customburger.MODID, bus = EventBusSubscriber.Bus.MOD)
public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    private static final ModConfigSpec.BooleanValue ENABLE_LOGGING =
            BUILDER.comment("Enable Customburger logging").define("enableLogging", false);

    private static final ModConfigSpec.BooleanValue ALLOW_POTION_INGREDIENTS =
            BUILDER.comment("Allow potions to be used as burger ingredients")
                    .define("allowPotionIngredients", false);

    private static final ModConfigSpec.BooleanValue DROP_REMAINDERS =
            BUILDER.comment("Drop container remainders like bowls or bottles")
                    .define("dropRemainders", true);

    private static final ModConfigSpec.BooleanValue DROP_REMAINDERS_ON_EAT =
            BUILDER.comment("Drop container remainders when eating (if false, remainders stay in the crafting grid)")
                    .define("dropRemaindersOnEat", false);

    static final ModConfigSpec SPEC = BUILDER.build();

    public static boolean enableLogging;
    public static boolean allowPotionIngredients;
    public static boolean dropRemainders;
    public static boolean dropRemaindersOnEat;

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        enableLogging = ENABLE_LOGGING.get();
        allowPotionIngredients = ALLOW_POTION_INGREDIENTS.get();
        dropRemainders = DROP_REMAINDERS.get();
        dropRemaindersOnEat = DROP_REMAINDERS_ON_EAT.get();
    }
}
