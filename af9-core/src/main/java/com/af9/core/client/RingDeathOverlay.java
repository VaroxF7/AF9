package com.af9.core.client;

import com.af9.core.AF9Core;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import com.mojang.blaze3d.vertex.PoseStack;

/**
 * The death screen of a player the orbital station's light ring burnt ("WASTED", in pixels): over the vanilla death
 * screen a white-hot flash, stepped heat edges, a black band across the title opening from its middle, the word
 * VAPORIZED dropping in, scaled up pixel for pixel and flickering like fire, and embers rising at the sides. The death
 * message ("... touched the light ring ...") and the buttons stay where the vanilla screen draws them.
 * <p>
 * Started by {@link com.af9.core.network.RingDeathPacket}; it ends when the player is alive again.
 */
@Mod.EventBusSubscriber(modid = AF9Core.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class RingDeathOverlay {

    /** Middle and half height of the band: over the vanilla title (y 60 to 78), clear of the death message (y 85). */
    private static final int BAND_MID = 63, BAND_HALF = 19;
    private static final int[] EMBERS = { 0xFFFFD04A, 0xFFFF8A1C, 0xFFE0300A, 0xFFFFF2B0 };

    private static long startedAt = -1;

    private RingDeathOverlay() {}

    /** The ring killed this player (network thread handed over to the main thread). */
    public static void trigger() {
        startedAt = Util.getMillis();
        var sounds = Minecraft.getInstance().getSoundManager();
        sounds.play(SimpleSoundInstance.forUI(SoundEvents.FIRECHARGE_USE, 0.6F, 0.8F));
        sounds.play(SimpleSoundInstance.forUI(SoundEvents.GENERIC_EXPLODE, 0.5F, 0.35F));
    }

    @SubscribeEvent
    public static void onScreenRender(ScreenEvent.Render.Post event) {
        if (startedAt < 0 || !(event.getScreen() instanceof DeathScreen screen)) return;
        draw(event.getGuiGraphics(), screen.width, screen.height, (Util.getMillis() - startedAt) / 1000F);
    }

    /** Ends once the player lives again (respawned, or a totem saved them), not before the death screen had a go. */
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || startedAt < 0) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof DeathScreen) return;
        boolean alive = minecraft.player != null && minecraft.player.isAlive();
        if ((alive && Util.getMillis() - startedAt > 1000) || Util.getMillis() - startedAt > 10000) startedAt = -1;
    }

    private static void draw(GuiGraphics graphics, int width, int height, float time) {
        Font font = Minecraft.getInstance().font;
        // the flash of the burn, white-hot, gone in 0.6 s
        if (time < 0.6F) {
            int alpha = (int) (0xD0 * (1 - time / 0.6F));
            graphics.fill(0, 0, width, height, alpha << 24 | 0xFFE0B0);
        }
        // heat at the edges, in 4 pixel steps getting darker outwards
        int[] steps = { 0x78, 0x54, 0x36, 0x1C };
        for (int i = 0; i < steps.length; i++) {
            int color = steps[i] << 24 | 0x6A1000;
            int a = i * 4, b = a + 4;
            graphics.fill(0, a, width, b, color);
            graphics.fill(0, height - b, width, height - a, color);
            graphics.fill(a, b, b, height - b, color);
            graphics.fill(width - b, b, width - a, height - b, color);
        }
        // the band, opening from its middle line, with dithered embers along its rims
        int half = Math.round(BAND_HALF * Mth.clamp(time / 0.25F, 0, 1));
        if (half > 0) {
            graphics.fill(0, BAND_MID - half, width, BAND_MID + half, 0xE6000000);
            for (int x = 0; x < width; x += 4) {
                int rim = (x / 4 & 1) == 0 ? 0xFFFF6A00 : 0xFFA82000;
                graphics.fill(x, BAND_MID - half - 2, x + 4, BAND_MID - half, rim);
                graphics.fill(x, BAND_MID + half, x + 4, BAND_MID + half + 2, rim);
            }
        }
        // the word: drops in from twice its size, shakes while the flash lasts, flickers like fire
        if (time > 0.15F) {
            String word = Component.translatable("af9.death.vaporized").getString();
            float in = Mth.clamp((time - 0.15F) / 0.3F, 0, 1);
            float base = Math.min(4, (width - 32) / (float) Math.max(1, font.width(word)));
            float scale = base * (1 + (1 - in) * (1 - in));
            int shake = time < 0.6F ? Math.round(Mth.sin(time * 90) * 2) : 0;
            int alpha = Math.max(8, Math.round(255 * in));
            float flicker = 0.5F + 0.5F * Mth.sin(time * 17) * Mth.sin(time * 7.3F);
            int fire = lerpColor(flicker, 0xFF3A12, 0xFFB43A);
            PoseStack pose = graphics.pose();
            pose.pushPose();
            pose.translate(width / 2F + shake, BAND_MID + shake * 0.5F, 0);
            pose.scale(scale, scale, 1);
            int x = -font.width(word) / 2;
            // a deep red shadow one font pixel down-right, then the word
            graphics.drawString(font, word, x + 1, -3, alpha << 24 | 0x3A0000, false);
            graphics.drawString(font, word, x, -4, alpha << 24 | fire, false);
            pose.popPose();
        }
        // embers rising at the sides (clear of the buttons in the middle)
        for (int i = 0; i < 32; i++) {
            float spot = hash(i, 1);
            float x = spot < 0.5F ? spot * 0.5F * width : width - (spot - 0.5F) * 0.5F * width;
            float speed = 18 + 42 * hash(i, 2);
            float y = height - (time * speed + hash(i, 3) * height) % (height + 8);
            x += Mth.sin(time * 2 + i) * 3;
            int size = 2 + (int) (hash(i, 4) * 2);
            int px = Math.round(x), py = Math.round(y);
            graphics.fill(px, py, px + size, py + size, EMBERS[(int) (hash(i, 5) * EMBERS.length)]);
        }
    }

    /** 0 to 1, fixed per ember and salt. */
    private static float hash(int index, int salt) {
        long h = (index * 0x9E3779B97F4A7C15L) ^ (salt * 0xC2B2AE3D27D4EB4FL);
        h ^= h >>> 29;
        h *= 0xBF58476D1CE4E5B9L;
        h ^= h >>> 32;
        return (h >>> 40) / (float) (1L << 24);
    }

    private static int lerpColor(float t, int from, int to) {
        int r = Math.round(Mth.lerp(t, from >> 16 & 0xFF, to >> 16 & 0xFF));
        int g = Math.round(Mth.lerp(t, from >> 8 & 0xFF, to >> 8 & 0xFF));
        int b = Math.round(Mth.lerp(t, from & 0xFF, to & 0xFF));
        return r << 16 | g << 8 | b;
    }
}
