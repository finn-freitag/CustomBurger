package com.finnfreitag.customburger;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

@EventBusSubscriber(modid = Customburger.MODID, bus = EventBusSubscriber.Bus.MOD)
public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    private static final ModConfigSpec.BooleanValue ENABLE_LOGGING =
            BUILDER.comment("Enable Customburger logging").define("enableLogging", true);

    static final ModConfigSpec SPEC = BUILDER.build();

    public static boolean enableLogging;

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        enableLogging = ENABLE_LOGGING.get();
    }
}
