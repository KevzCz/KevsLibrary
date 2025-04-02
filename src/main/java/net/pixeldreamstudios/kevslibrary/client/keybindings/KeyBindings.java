package net.pixeldreamstudios.kevslibrary.client;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.pixeldreamstudios.kevslibrary.client.screen.PlayerStatsScreen;
import org.lwjgl.glfw.GLFW;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
public class KeyBindings {
    public static KeyBinding STATS_KEY;

    public static void register() {
        STATS_KEY = KeyBindingHelper.registerKeyBinding(
                new KeyBinding(
                        "key.kevslibrary.stats",                         // Translation key (for localization)
                        InputUtil.Type.KEYSYM,                           // Input type (KEYSYM = keyboard)
                        GLFW.GLFW_KEY_P,                                 // Default key
                        "key.categories.kevslibrary"                     // Category shown in controls menu
                )
        );

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (STATS_KEY.wasPressed()) {
                if (client.player != null) {
                    client.setScreen(new PlayerStatsScreen());
                }
            }
        });
    }
}
