package com.af9.lod;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.OptionsScreen;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.lwjgl.glfw.GLFW;

/** Ways to open the settings screen: a hotkey (F8 by default) and a button on the Options and video settings screens. */
public final class ClientHooks {
    public static final KeyMapping OPEN_SETTINGS = new KeyMapping("key.af9_lod.settings",
            KeyConflictContext.UNIVERSAL, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_F8, "key.categories.misc");

    /** Mod event bus. */
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(OPEN_SETTINGS);
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        while (OPEN_SETTINGS.consumeClick()) {
            if (!(mc.screen instanceof LodConfigScreen)) mc.setScreen(new LodConfigScreen(mc.screen));
        }
    }

    @SubscribeEvent
    public void onScreenInit(ScreenEvent.Init.Post event) {
        Screen screen = event.getScreen();
        String name = screen.getClass().getName().toLowerCase();
        boolean fits = screen instanceof PauseScreen || screen instanceof OptionsScreen || name.contains("videosettings") || name.contains("embeddium")
                || name.contains("sodium");
        if (!fits) return;
        event.addListener(Button.builder(Component.literal("AF9 Horizon"),
                b -> Minecraft.getInstance().setScreen(new LodConfigScreen(screen)))
                .bounds(screen.width - 104, 4, 100, 20).build());
    }
}
