package com.af9.core.client;

import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.gui.screens.GenericDirtMessageScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import com.mojang.blaze3d.vertex.PoseStack;

import java.util.List;
import java.util.Locale;

/**
 * The death screen of a player the orbital station's light ring burnt ("WASTED", in pixels), in place of the vanilla
 * one: a white-hot flash, stepped heat edges, a black band across the middle of the screen opening from its centre
 * line, VAPORIZED dropping in, scaled up pixel for pixel and flickering like fire, the death message (wrapped) and the
 * score under it, then its own pixel buttons: respawn (spectate in hardcore) and back to the title screen, which do
 * what the vanilla ones do (the title button asks first, and goes through the chat-report check). Embers rise at the
 * sides. A {@link DeathScreen} still, so everything that closes the death screen on respawn closes this one.
 */
@OnlyIn(Dist.CLIENT)
public class VaporizedScreen extends DeathScreen {

    /** Half height of the band; gaps below it. */
    private static final int BAND_HALF = 22, TEXT_GAP = 12, BUTTON_GAP = 20, BUTTON_W = 200, BUTTON_H = 20;
    /** Ticks before the buttons work (vanilla: 20). */
    private static final int BUTTON_DELAY = 20;
    private static final int[] EMBERS = { 0xFFFFD04A, 0xFFFF8A1C, 0xFFE0300A, 0xFFFFF2B0 };

    private final Component causeOfDeath;
    private final boolean hardcore;
    private final long openedAt = Util.getMillis();
    private int ticks;
    private int bandMid;
    private List<FormattedCharSequence> message = List.of();
    private Component score = Component.empty();
    private PixelButton respawnButton;
    private PixelButton titleButton;

    public VaporizedScreen(Component causeOfDeath, boolean hardcore) {
        super(causeOfDeath, hardcore);
        this.causeOfDeath = causeOfDeath;
        this.hardcore = hardcore;
    }

    /** Our own layout and buttons; none of the vanilla screen's. */
    @Override
    protected void init() {
        Minecraft mc = Minecraft.getInstance();
        message = causeOfDeath == null ? List.of() : font.split(causeOfDeath, Math.min(width - 40, 380));
        score = Component.translatable("deathScreen.score").append(": ").append(
                Component.literal(Integer.toString(mc.player == null ? 0 : mc.player.getScore()))
                        .withStyle(ChatFormatting.YELLOW));
        // the band in the middle of the screen, as long as the text and buttons still fit below it
        int below = TEXT_GAP + message.size() * 10 + 4 + 10 + BUTTON_GAP + BUTTON_H * 2 + 6;
        bandMid = Mth.clamp(height / 2, BAND_HALF + 10, Math.max(BAND_HALF + 10, height - below - BAND_HALF - 8));
        int y = bandMid + BAND_HALF + TEXT_GAP + message.size() * 10 + 4 + 10 + BUTTON_GAP;
        respawnButton = new PixelButton(width / 2 - BUTTON_W / 2, y, BUTTON_W, BUTTON_H,
                Component.translatable(hardcore ? "deathScreen.spectate" : "deathScreen.respawn"), button -> {
                    if (mc.player != null) mc.player.respawn();
                    button.active = false;
                });
        titleButton = new PixelButton(width / 2 - BUTTON_W / 2, y + BUTTON_H + 6, BUTTON_W, BUTTON_H,
                Component.translatable("deathScreen.titleScreen"),
                button -> mc.getReportingContext().draftReportHandled(mc, this, this::exitToTitle, true));
        addWidget(respawnButton);
        addWidget(titleButton);
        boolean ready = ticks >= BUTTON_DELAY;
        respawnButton.active = ready;
        titleButton.active = ready;
    }

    @Override
    public void tick() {
        super.tick();
        ticks++;
        if (ticks == BUTTON_DELAY && respawnButton != null) {
            respawnButton.active = true;
            titleButton.active = true;
        }
    }

    /** As the vanilla screen: hardcore leaves at once, else it asks (respawning instead is the other answer). */
    private void exitToTitle() {
        Minecraft mc = Minecraft.getInstance();
        if (hardcore) {
            quitToTitle(mc);
            return;
        }
        ConfirmScreen confirm = new ConfirmScreen(quit -> {
            if (quit) {
                quitToTitle(mc);
            } else {
                if (mc.player != null) mc.player.respawn();
                mc.setScreen(null);
            }
        }, Component.translatable("deathScreen.quit.confirm"), CommonComponents.EMPTY,
                Component.translatable("deathScreen.titleScreen"), Component.translatable("deathScreen.respawn"));
        mc.setScreen(confirm);
        confirm.setDelay(20);
    }

    private static void quitToTitle(Minecraft mc) {
        if (mc.level != null) mc.level.disconnect();
        mc.clearLevel(new GenericDirtMessageScreen(Component.translatable("menu.savingLevel")));
        mc.setScreen(new TitleScreen());
    }

    //////////////////////////////////////
    // ********** Drawing *********//
    //////////////////////////////////////

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        float time = (Util.getMillis() - openedAt) / 1000F;
        // the vanilla death screen's red veil
        graphics.fillGradient(0, 0, width, height, 0x60500000, 0xA0803030);
        // the flash of the burn, white-hot, gone in 0.6 s
        if (time < 0.6F) {
            graphics.fill(0, 0, width, height, (int) (0xD0 * (1 - time / 0.6F)) << 24 | 0xFFE0B0);
        }
        drawHeatEdges(graphics);
        drawBand(graphics, time);
        // the death message and the score under the band
        int y = bandMid + BAND_HALF + TEXT_GAP;
        float in = Mth.clamp((time - 0.35F) / 0.3F, 0, 1);
        int textAlpha = Math.max(8, Math.round(255 * in));
        for (FormattedCharSequence line : message) {
            graphics.drawString(font, line, width / 2 - font.width(line) / 2, y, textAlpha << 24 | 0xF3E3D8, true);
            y += 10;
        }
        y += 4;
        graphics.drawCenteredString(font, score, width / 2, y, textAlpha << 24 | 0xFFFFFF);
        // the buttons, once the band is open
        if (time > 0.5F) {
            respawnButton.render(graphics, mouseX, mouseY, partialTick);
            titleButton.render(graphics, mouseX, mouseY, partialTick);
        }
        drawEmbers(graphics, time);
    }

    /** Heat at the edges, in 4 pixel steps getting darker outwards. */
    private void drawHeatEdges(GuiGraphics graphics) {
        int[] steps = { 0x78, 0x54, 0x36, 0x1C };
        for (int i = 0; i < steps.length; i++) {
            int color = steps[i] << 24 | 0x6A1000;
            int a = i * 4, b = a + 4;
            graphics.fill(0, a, width, b, color);
            graphics.fill(0, height - b, width, height - a, color);
            graphics.fill(a, b, b, height - b, color);
            graphics.fill(width - b, b, width - a, height - b, color);
        }
    }

    /** The band, opening from its centre line with dithered embers along its rims, and the word in it. */
    private void drawBand(GuiGraphics graphics, float time) {
        int half = Math.round(BAND_HALF * Mth.clamp(time / 0.25F, 0, 1));
        if (half > 0) {
            graphics.fill(0, bandMid - half, width, bandMid + half, 0xEE000000);
            for (int x = 0; x < width; x += 4) {
                int rim = (x / 4 & 1) == 0 ? 0xFFFF6A00 : 0xFFA82000;
                graphics.fill(x, bandMid - half - 2, x + 4, bandMid - half, rim);
                graphics.fill(x, bandMid + half, x + 4, bandMid + half + 2, rim);
            }
        }
        if (time <= 0.15F) return;
        // drops in from twice its size, shakes while the flash lasts, flickers like fire
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
        pose.translate(width / 2F + shake, bandMid + shake * 0.5F, 0);
        pose.scale(scale, scale, 1);
        int x = -font.width(word) / 2;
        // a deep red shadow one font pixel down-right, then the word
        graphics.drawString(font, word, x + 1, -3, alpha << 24 | 0x3A0000, false);
        pose.translate(0, 0, 1);
        graphics.drawString(font, word, x, -4, alpha << 24 | fire, false);
        pose.popPose();
    }

    /** Embers rising at the sides, clear of the text and the buttons in the middle. */
    private void drawEmbers(GuiGraphics graphics, float time) {
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

    /**
     * A button in the screen's style: a notched pixel frame, dark red, the frame and the label glowing orange when
     * hovered, dim until the screen lets it work; the label in capitals with a pointer on each side when hovered.
     */
    static class PixelButton extends Button {

        PixelButton(int x, int y, int width, int height, Component message, OnPress onPress) {
            super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            Font font = Minecraft.getInstance().font;
            int x = getX(), y = getY(), w = getWidth(), h = getHeight();
            boolean hot = active && isHoveredOrFocused();
            int edge = !active ? 0xFF4A1A10 : hot ? 0xFFFF7A1C : 0xFFA83A14;
            int fill = hot ? 0xF02A0A04 : 0xE0140606;
            // body and frame, the corners notched
            graphics.fill(x + 2, y, x + w - 2, y + h, fill);
            graphics.fill(x, y + 2, x + w, y + h - 2, fill);
            graphics.fill(x + 2, y, x + w - 2, y + 2, edge);
            graphics.fill(x + 2, y + h - 2, x + w - 2, y + h, edge);
            graphics.fill(x, y + 2, x + 2, y + h - 2, edge);
            graphics.fill(x + w - 2, y + 2, x + w, y + h - 2, edge);
            String label = getMessage().getString().toUpperCase(Locale.ROOT);
            int color = !active ? 0xFF7A5A50 : hot ? 0xFFFFC04A : 0xFFF3E3D8;
            int textY = y + (h - 8) / 2;
            graphics.drawString(font, label, x + w / 2 - font.width(label) / 2, textY, color, true);
            if (hot) {
                int reach = font.width(label) / 2 + 10;
                graphics.drawString(font, ">", x + w / 2 - reach - font.width(">"), textY, color, true);
                graphics.drawString(font, "<", x + w / 2 + reach, textY, color, true);
            }
        }
    }
}
