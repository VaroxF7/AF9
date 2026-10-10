package com.af9.core.droppod.client;

import com.af9.core.droppod.DropPodEntity;
import com.af9.core.network.AF9Network;
import com.af9.core.network.DropPodReleasePacket;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import org.lwjgl.glfw.GLFW;

import java.util.Locale;

/**
 * The wait in the pod: "Welcome to AF9!" letter by letter (dropping in, colours running through it, a glint sweeping
 * along), a light line opening under it, sparks rising, and a pixel button to launch (a click or SPACE) with the 20 s
 * countdown of the server under it. It shows the world behind it and does not pause; the pod closes it when it lets go.
 */
@OnlyIn(Dist.CLIENT)
public class DropPodScreen extends Screen {

    private static final int BUTTON_W = 220, BUTTON_H = 26;
    private static final int[] SPARKS = { 0xFF4AD8FF, 0xFFFFB43A, 0xFFB8F0FF, 0xFFFF7A1C };

    private final long openedAt = Util.getMillis();
    private boolean sent;
    private LaunchButton button;

    public DropPodScreen() {
        super(Component.translatable("af9.drop_pod.welcome"));
    }

    @Override
    protected void init() {
        button = new LaunchButton(width / 2 - BUTTON_W / 2, Math.round(height * 0.64F), BUTTON_W, BUTTON_H,
                Component.translatable("af9.drop_pod.launch"), b -> launch());
        addRenderableWidget(button);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    /** The server does the launching; this only asks (once). */
    private void launch() {
        if (sent) return;
        sent = true;
        AF9Network.CHANNEL.sendToServer(new DropPodReleasePacket(true));
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    @Override
    public boolean keyPressed(int key, int scanCode, int modifiers) {
        if (key == GLFW.GLFW_KEY_SPACE || key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER
                || minecraft != null && minecraft.options.keyJump.matches(key, scanCode)) {
            launch();
            return true;
        }
        return super.keyPressed(key, scanCode, modifiers);
    }

    @Override
    public void tick() {
        Minecraft mc = Minecraft.getInstance();
        // the pod let go (or is gone): the screen's job is done
        if (mc.player == null || !(mc.player.getVehicle() instanceof DropPodEntity pod) || pod.isReleased()) {
            mc.setScreen(null);
        }
    }

    //////////////////////////////////////
    // ********** Drawing *********//
    //////////////////////////////////////

    @Override
    public void renderBackground(GuiGraphics graphics) {
        // the world stays visible; only a soft veil at the top and bottom edges
        graphics.fillGradient(0, 0, width, height / 3, 0x80000814, 0x00000814);
        graphics.fillGradient(0, height * 2 / 3, width, height, 0x00000814, 0x90000814);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        float time = (Util.getMillis() - openedAt) / 1000F;
        drawSparks(graphics, time);
        drawTitle(graphics, time);
        drawCountdown(graphics, time);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void drawTitle(GuiGraphics graphics, float time) {
        String text = getTitle().getString();
        int count = text.length();
        float scale = Math.min(4.5F, (width - 40) / (float) Math.max(1, font.width(text)));
        int total = font.width(text);
        int baseY = Math.round(height * 0.30F);
        PoseStack pose = graphics.pose();
        int x = 0;
        // the glint: one letter lights up white, running along the word and starting again
        float glint = (time * 11F) % (count + 14);
        for (int i = 0; i < count; i++) {
            char c = text.charAt(i);
            String s = String.valueOf(c);
            int w = font.width(s);
            float in = Mth.clamp((time - 0.15F - 0.06F * i) / 0.35F, 0F, 1F);
            if (in > 0 && c != ' ') {
                float ease = 1F - (1F - in) * (1F - in);
                float drop = (1F - ease) * -26F;
                float pulse = 0.5F + 0.5F * Mth.sin(time * 2.2F - i * 0.45F);
                int color = lerpColor(pulse, 0x39C8FF, 0xFFB43A);
                float near = 1F - Mth.clamp(Math.abs(glint - i) / 2.2F, 0F, 1F);
                color = lerpColor(near * 0.9F, color, 0xFFFFFF);
                int alpha = Math.max(8, Math.round(255 * in));
                float bob = Mth.sin(time * 3F + i * 0.6F) * 1.2F * in;
                pose.pushPose();
                pose.translate(width / 2F - total * scale / 2F + x * scale, baseY + (drop + bob) * scale / 2F, 0);
                pose.scale(scale, scale, 1);
                graphics.drawString(font, s, 1, 1, alpha << 24 | 0x04101C, false);
                pose.translate(0, 0, 1);
                graphics.drawString(font, s, 0, 0, alpha << 24 | color, false);
                pose.popPose();
            }
            x += w;
        }
        // the lines opening from the middle under it, and the subtitle fading in
        float line = Mth.clamp((time - 0.9F) / 0.6F, 0F, 1F);
        int half = Math.round((width * 0.32F) * line);
        int ly = baseY + Math.round(10 * scale) + 8;
        if (half > 0) {
            graphics.fill(width / 2 - half, ly, width / 2 + half, ly + 2, 0xFF39C8FF);
            graphics.fill(width / 2 - half / 2, ly + 4, width / 2 + half / 2, ly + 5, 0xFFFFB43A);
        }
        float sub = Mth.clamp((time - 1.3F) / 0.5F, 0F, 1F);
        if (sub > 0) {
            graphics.drawCenteredString(font, Component.translatable("af9.drop_pod.subtitle"), width / 2, ly + 12,
                    Math.max(8, Math.round(255 * sub)) << 24 | 0xDDEFFF);
        }
    }

    /** The countdown the server runs: a bar and the seconds under the button. */
    private void drawCountdown(GuiGraphics graphics, float time) {
        Minecraft mc = Minecraft.getInstance();
        int left = DropPodEntity.AUTO_TICKS;
        if (mc.player != null && mc.player.getVehicle() instanceof DropPodEntity pod && pod.countdownTicks() >= 0) {
            left = pod.countdownTicks();
        }
        float fraction = Mth.clamp(left / (float) DropPodEntity.AUTO_TICKS, 0F, 1F);
        int x = width / 2 - BUTTON_W / 2;
        int y = button.getY() + BUTTON_H + 8;
        graphics.fill(x, y, x + BUTTON_W, y + 4, 0xA0000814);
        graphics.fill(x, y, x + Math.round(BUTTON_W * fraction), y + 4, fraction > 0.25F ? 0xFF39C8FF : 0xFFFF7A1C);
        int seconds = Math.max(1, (left + 19) / 20);
        graphics.drawCenteredString(font, Component.translatable("af9.drop_pod.auto", seconds), width / 2, y + 9, 0xFFDDEFFF);
    }

    /** Sparks rising on both sides of the screen, clear of the middle. */
    private void drawSparks(GuiGraphics graphics, float time) {
        for (int i = 0; i < 40; i++) {
            float spot = hash(i, 1);
            float x = spot < 0.5F ? spot * 0.6F * width : width - (spot - 0.5F) * 0.6F * width;
            float speed = 14 + 40 * hash(i, 2);
            float y = height - (time * speed + hash(i, 3) * height) % (height + 8);
            x += Mth.sin(time * 1.6F + i) * 4;
            int size = 2 + (int) (hash(i, 4) * 2);
            int px = Math.round(x), py = Math.round(y);
            graphics.fill(px, py, px + size, py + size, SPARKS[(int) (hash(i, 5) * SPARKS.length)]);
        }
    }

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

    /** The launch button: a notched pixel frame pulsing blue to orange, the label with the key in brackets. */
    private static final class LaunchButton extends Button {

        LaunchButton(int x, int y, int width, int height, Component message, OnPress onPress) {
            super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            Font font = Minecraft.getInstance().font;
            int x = getX(), y = getY(), w = getWidth(), h = getHeight();
            boolean hot = isHoveredOrFocused();
            float pulse = 0.5F + 0.5F * Mth.sin(Util.getMillis() / 260F);
            int edge = 0xFF000000 | lerpColor(hot ? 1F : pulse * 0.6F, 0x39C8FF, 0xFFB43A);
            int fill = hot ? 0xF0102838 : 0xE0081420;
            graphics.fill(x + 2, y, x + w - 2, y + h, fill);
            graphics.fill(x, y + 2, x + w, y + h - 2, fill);
            graphics.fill(x + 2, y, x + w - 2, y + 2, edge);
            graphics.fill(x + 2, y + h - 2, x + w - 2, y + h, edge);
            graphics.fill(x, y + 2, x + 2, y + h - 2, edge);
            graphics.fill(x + w - 2, y + 2, x + w, y + h - 2, edge);
            String label = getMessage().getString().toUpperCase(Locale.ROOT) + "  [SPACE]";
            int color = hot ? 0xFFFFD27A : 0xFFEAF6FF;
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
