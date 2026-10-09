package com.af9.core.machine.console;

import com.lowdragmc.lowdraglib.gui.widget.Widget;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.Locale;

/**
 * Base of the AF9 machine consoles (lithography, accelerator, cryostat, space elevator): a dark panel with a status
 * header, drawn entirely in {@link #drawInBackground}. The server samples the machine every tick ({@link #sample()}) and sends the
 * state only when something changed; hover tooltips and the counter-reset click come from widgets the machine places
 * over the drawn
 * tiles.
 */
public abstract class ConsoleWidget extends Widget {

    public static final int BG = 0xFF0A0E16, PANEL = 0xFF111827, EDGE = 0xFF25324A, TEXT = 0xFFE6EDF7,
            MUTED = 0xFF7C8AA5, DIM = 0xFF4B5567, GOOD = 0xFF4ADE80, BAD = 0xFFEF4444, WARN = 0xFFFBBF24,
            INFO = 0xFF60A5FA, TRACK = 0xFF1E293B;

    /** Status codes shared by all consoles and the Jade tooltip (lang: af9.console.status.&lt;code&gt;). */
    public static final int STATUS_OFFLINE = 0, STATUS_IDLE = 1, STATUS_RUNNING = 2, STATUS_NO_POWER = 3,
            STATUS_PAUSED = 4, STATUS_MAINTENANCE = 5, STATUS_LOCKED = 6, STATUS_NO_ORBIT = 7, STATUS_NO_COOLANT = 8,
            STATUS_PUMPING_DOWN = 9, STATUS_NO_COMPUTATION = 10, STATUS_NO_DATA = 11, STATUS_NO_RETICLE = 12,
            STATUS_STARTING_UP = 13, STATUS_NO_LIGHT = 14, STATUS_NO_COOLING = 15, STATUS_NO_SKY = 16,
            STATUS_NO_MODULE = 17, STATUS_NO_DRONE = 18, STATUS_NO_FUEL = 19, STATUS_OUTPUT_FULL = 20,
            STATUS_NO_DIMENSION = 21, STATUS_ALIGNING = 22, STATUS_MK2_ONLY = 23, STATUS_STAR_TAKEN = 24,
            STATUS_NO_STAR = 25;

    protected ConsoleWidget(int x, int y, int width, int height) {
        super(x, y, width, height);
    }

    //////////////////////////////////////
    // *********** Sync ***********//
    //////////////////////////////////////

    /** Server side: re-reads the machine into the widget's fields; true if anything the client draws changed. */
    protected abstract boolean sample();

    protected abstract void writeState(FriendlyByteBuf buffer);

    protected abstract void readState(FriendlyByteBuf buffer);

    @Override
    public void detectAndSendChanges() {
        super.detectAndSendChanges();
        if (sample()) writeUpdateInfo(1, this::writeState);
    }

    @Override
    public void writeInitialData(FriendlyByteBuf buffer) {
        super.writeInitialData(buffer);
        sample();
        writeState(buffer);
    }

    @Override
    public void readInitialData(FriendlyByteBuf buffer) {
        super.readInitialData(buffer);
        readState(buffer);
    }

    @Override
    public void readUpdateInfo(int id, FriendlyByteBuf buffer) {
        if (id == 1) {
            readState(buffer);
        } else {
            super.readUpdateInfo(id, buffer);
        }
    }

    //////////////////////////////////////
    // ********** Drawing *********//
    //////////////////////////////////////

    @OnlyIn(Dist.CLIENT)
    protected static Font font() {
        return Minecraft.getInstance().font;
    }

    /** Background panel and the header: title, an optional coloured suffix (e.g. "V5"), status word and LED. */
    @OnlyIn(Dist.CLIENT)
    protected void drawFrame(GuiGraphics graphics, String title, String suffix, int suffixColor, int status,
                             int accent) {
        Font font = font();
        int x0 = getPosition().x;
        int y0 = getPosition().y;
        int width = getSize().width;
        int height = getSize().height;
        graphics.fill(x0, y0, x0 + width, y0 + height, BG);
        border(graphics, x0, y0, width, height, EDGE);
        graphics.fill(x0 + 1, y0 + 1, x0 + width - 1, y0 + 14, PANEL);
        graphics.fill(x0 + 1, y0 + 14, x0 + width - 1, y0 + 15, withAlpha(accent, 0xAA));
        graphics.drawString(font, title, x0 + 6, y0 + 3, TEXT, false);
        if (suffix != null && !suffix.isEmpty()) {
            graphics.drawString(font, suffix, x0 + 10 + font.width(title), y0 + 3, suffixColor, false);
        }
        int ledColor = statusColor(status);
        boolean ledOn = status != STATUS_RUNNING || (System.currentTimeMillis() / 400) % 2 == 0;
        graphics.fill(x0 + width - 12, y0 + 4, x0 + width - 6, y0 + 10, ledOn ? ledColor : withAlpha(ledColor, 0x55));
        String statusText = Component.translatable("af9.console.status." + status).getString();
        graphics.drawString(font, statusText, x0 + width - 16 - font.width(statusText), y0 + 3, ledColor, false);
    }

    /**
     * Power in the header, right before the status word: "available/needed EU/t" in small type over a thin bar (green
     * when the hatches can supply it). Drops the unit, then the whole gauge, if the title leaves too little room.
     */
    @OnlyIn(Dist.CLIENT)
    protected void drawHeaderPower(GuiGraphics graphics, String title, String suffix, int status, long available,
                                   long needed) {
        Font font = font();
        int x0 = getPosition().x;
        int y0 = getPosition().y;
        int titleWidth = font.width(title) + (suffix == null || suffix.isEmpty() ? 0 : 4 + font.width(suffix));
        int left = x0 + 6 + titleWidth + 8;
        String statusText = Component.translatable("af9.console.status." + status).getString();
        int right = x0 + getSize().width - 16 - font.width(statusText) - 8;
        String amounts = compact(available) + "/" + compact(needed);
        String text = amounts + " EU/t";
        if (font.width(text) * 3 / 4 > right - left) text = amounts;
        int width = font.width(text) * 3 / 4;
        if (width > right - left) return;
        boolean enough = available >= needed;
        int x = right - width;
        drawSmall(graphics, text, x, y0 + 3, enough ? TEXT : BAD, false);
        bar(graphics, x, y0 + 10, width, 2, needed <= 0 ? 0 : Math.min(1.0, (double) available / needed),
                enough ? GOOD : BAD);
    }

    /** A mode tile: two centred lines (name, detail); selected tiles are filled in the mode colour. */
    @OnlyIn(Dist.CLIENT)
    protected static void drawTile(GuiGraphics graphics, int x, int y, int width, int height, String name,
                                   String detail, int color, boolean selected, boolean available, boolean locked) {
        Font font = font();
        graphics.fill(x, y, x + width, y + height, selected ? withAlpha(color, 0x55) : locked ? 0xFF0B0F17 : PANEL);
        border(graphics, x, y, width, height, selected ? color : withAlpha(color, locked ? 0x33 : 0x66));
        int nameColor = available || selected ? color : withAlpha(color, locked ? 0x55 : 0x88);
        drawSmall(graphics, name, x + width / 2, y + 3, nameColor, true);
        drawSmall(graphics, detail, x + width / 2, y + height - 9, locked ? BAD : available ? MUTED : DIM, true);
    }

    /** Label, run-time bar and "12.3 s / 45.0 s ... 27%" under it. */
    @OnlyIn(Dist.CLIENT)
    protected static void drawRuntime(GuiGraphics graphics, int x, int y, int width, int progress, int duration,
                                      boolean running, int color) {
        Font font = font();
        drawSmall(graphics, Component.translatable("af9.console.runtime").getString(), x, y, MUTED, false);
        double fraction = duration <= 0 ? 0 : Math.min(1.0, (double) progress / duration);
        bar(graphics, x, y + 7, width, 8, running ? fraction : 0, color);
        border(graphics, x - 1, y + 6, width + 2, 10, EDGE);
        String time = running ? seconds(progress) + " / " + seconds(duration) :
                Component.translatable("af9.console.no_run").getString();
        graphics.drawString(font, time, x, y + 18, running ? TEXT : MUTED, false);
        String percent = running ? Math.round(fraction * 100) + "%" : "";
        graphics.drawString(font, percent, x + width - font.width(percent), y + 18, TEXT, false);
    }

    /** Text at 3/4 scale; x is the left edge, or the centre when {@code centered}. */
    @OnlyIn(Dist.CLIENT)
    protected static void drawSmall(GuiGraphics graphics, String text, int x, int y, int color, boolean centered) {
        Font font = font();
        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0);
        graphics.pose().scale(0.75F, 0.75F, 1F);
        int offset = centered ? -font.width(text) / 2 : 0;
        graphics.drawString(font, text, offset, 0, color, false);
        graphics.pose().popPose();
    }

    /** Cuts the text to the width (normal scale), with "..." when it had to be cut. */
    @OnlyIn(Dist.CLIENT)
    protected static String fit(String text, int width) {
        Font font = font();
        if (font.width(text) <= width) return text;
        return font.plainSubstrByWidth(text, Math.max(0, width - font.width("..."))) + "...";
    }

    @OnlyIn(Dist.CLIENT)
    protected static void bar(GuiGraphics graphics, int x, int y, int width, int height, double fraction, int color) {
        graphics.fill(x, y, x + width, y + height, TRACK);
        int filled = (int) Math.round(width * Math.max(0, Math.min(1, fraction)));
        if (filled > 0) graphics.fill(x, y, x + filled, y + height, color);
    }

    @OnlyIn(Dist.CLIENT)
    protected static void border(GuiGraphics graphics, int x, int y, int width, int height, int color) {
        graphics.fill(x, y, x + width, y + 1, color);
        graphics.fill(x, y + height - 1, x + width, y + height, color);
        graphics.fill(x, y, x + 1, y + height, color);
        graphics.fill(x + width - 1, y, x + width, y + height, color);
    }

    /** A small clickable-looking button face (the click itself is a ButtonWidget on top). */
    @OnlyIn(Dist.CLIENT)
    protected static void drawButton(GuiGraphics graphics, int x, int y, int width, int height, String label) {
        graphics.fill(x, y, x + width, y + height, PANEL);
        border(graphics, x, y, width, height, EDGE);
        drawSmall(graphics, label, x + width / 2, y + (height - 6) / 2, TEXT, true);
    }

    public static int statusColor(int status) {
        return switch (status) {
            case STATUS_RUNNING -> GOOD;
            case STATUS_IDLE -> WARN;
            case STATUS_NO_POWER, STATUS_MAINTENANCE, STATUS_LOCKED, STATUS_NO_ORBIT, STATUS_NO_COOLANT,
                    STATUS_NO_COMPUTATION, STATUS_NO_DATA, STATUS_NO_RETICLE, STATUS_NO_LIGHT, STATUS_NO_COOLING,
                    STATUS_NO_SKY, STATUS_NO_MODULE, STATUS_NO_DRONE, STATUS_NO_FUEL, STATUS_OUTPUT_FULL,
                    STATUS_NO_DIMENSION, STATUS_MK2_ONLY, STATUS_STAR_TAKEN, STATUS_NO_STAR ->
                BAD;
            case STATUS_PAUSED, STATUS_PUMPING_DOWN, STATUS_STARTING_UP, STATUS_ALIGNING -> INFO;
            default -> MUTED;
        };
    }

    /** Green from 80, yellow from 50, red below. */
    public static int levelColor(double percent) {
        return percent >= 80 ? GOOD : percent >= 50 ? WARN : BAD;
    }

    public static int withAlpha(int argb, int alpha) {
        return (alpha << 24) | (argb & 0xFFFFFF);
    }

    /** 512, 7.7k, 31k, 123k, 1.2M, 98M, 1.2G: short enough for the console columns. */
    public static String compact(long value) {
        if (value < 1000) return Long.toString(value);
        if (value < 10_000) return String.format(Locale.ROOT, "%.1fk", value / 1000.0);
        if (value < 1_000_000) return (value / 1000) + "k";
        if (value < 10_000_000) return String.format(Locale.ROOT, "%.1fM", value / 1_000_000.0);
        if (value < 1_000_000_000) return (value / 1_000_000) + "M";
        return String.format(Locale.ROOT, "%.1fG", value / 1_000_000_000.0);
    }

    /** Ticks as seconds with one decimal: "12.3 s". */
    public static String seconds(int ticks) {
        return String.format(Locale.ROOT, "%.1f s", ticks / 20.0);
    }
}
