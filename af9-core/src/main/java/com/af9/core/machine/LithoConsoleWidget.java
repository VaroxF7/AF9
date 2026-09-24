package com.af9.core.machine;

import com.af9.core.litho.LithoMode;

import com.lowdragmc.lowdraglib.gui.widget.Widget;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.Arrays;
import java.util.Locale;

/**
 * The Photolithography Line's control console: mode selector, power gauge, process track, output data and counters,
 * drawn on a dark scanline panel. The server samples the machine every tick and syncs only what changed; clicks are
 * handled by invisible {@code ButtonWidget}s placed over the drawn tiles (see
 * {@link PhotolithographyLineMachine#createUIWidget()}).
 */
public class LithoConsoleWidget extends Widget {

    public static final int WIDTH = 190;
    public static final int HEIGHT = 125;
    public static final int TILE_Y = 18;
    public static final int TILE_W = 34;
    public static final int TILE_H = 22;
    public static final int RESET_X = 160;
    public static final int RESET_Y = 98;
    public static final int RESET_W = 24;
    public static final int RESET_H = 10;

    static final int STATUS_OFFLINE = 0, STATUS_IDLE = 1, STATUS_RUNNING = 2, STATUS_NO_POWER = 3, STATUS_PAUSED = 4,
            STATUS_MAINTENANCE = 5;

    private static final int BG = 0xFF0A0E16, PANEL = 0xFF111827, EDGE = 0xFF25324A, TEXT = 0xFFE6EDF7,
            MUTED = 0xFF7C8AA5, GOOD = 0xFF4ADE80, BAD = 0xFFEF4444, WARN = 0xFFFBBF24;
    private static final String[] STATION_LETTERS = { "P", "C", "B", "E", "B", "D", "R", "B" };

    private final PhotolithographyLineMachine machine;

    // last state sent to / received by the client
    private int status = -1;
    private int mode = 0;
    private long available;
    private int progress; // per mille
    private final long[] printed = new long[LithoMode.values().length];

    public LithoConsoleWidget(PhotolithographyLineMachine machine, int x, int y) {
        super(x, y, WIDTH, HEIGHT);
        this.machine = machine;
    }

    public static int tileX(int index) {
        return 6 + index * (TILE_W + 2);
    }

    //////////////////////////////////////
    // *********** Sync ***********//
    //////////////////////////////////////

    private int sampleStatus() {
        if (!machine.isFormed()) return STATUS_OFFLINE;
        if (machine.hasMaintenanceProblems()) return STATUS_MAINTENANCE;
        var logic = machine.getRecipeLogic();
        if (!logic.isWorkingEnabled()) return STATUS_PAUSED;
        if (logic.isWorking()) return STATUS_RUNNING;
        LithoMode active = machine.getActiveMode();
        if (logic.isWaiting() || (active != null && machine.getAvailableEUt() < active.eut())) return STATUS_NO_POWER;
        return STATUS_IDLE;
    }

    @Override
    public void detectAndSendChanges() {
        super.detectAndSendChanges();
        int newStatus = sampleStatus();
        LithoMode active = machine.getActiveMode();
        int newMode = active == null ? 0 : active.ordinal();
        long newAvailable = machine.getAvailableEUt();
        int newProgress = (int) Math.round(machine.getRecipeLogic().getProgressPercent() * 1000);
        long[] newPrinted = machine.getPrintedCounts();
        if (newStatus != status || newMode != mode || newAvailable != available || newProgress != progress ||
                !Arrays.equals(newPrinted, printed)) {
            status = newStatus;
            mode = newMode;
            available = newAvailable;
            progress = newProgress;
            System.arraycopy(newPrinted, 0, printed, 0, printed.length);
            writeUpdateInfo(1, this::writeState);
        }
    }

    @Override
    public void writeInitialData(FriendlyByteBuf buffer) {
        super.writeInitialData(buffer);
        status = sampleStatus();
        LithoMode active = machine.getActiveMode();
        mode = active == null ? 0 : active.ordinal();
        available = machine.getAvailableEUt();
        progress = (int) Math.round(machine.getRecipeLogic().getProgressPercent() * 1000);
        System.arraycopy(machine.getPrintedCounts(), 0, printed, 0, printed.length);
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

    private void writeState(FriendlyByteBuf buffer) {
        buffer.writeVarInt(status);
        buffer.writeVarInt(mode);
        buffer.writeVarLong(available);
        buffer.writeVarInt(progress);
        for (long count : printed) buffer.writeVarLong(count);
    }

    private void readState(FriendlyByteBuf buffer) {
        status = buffer.readVarInt();
        mode = buffer.readVarInt();
        available = buffer.readVarLong();
        progress = buffer.readVarInt();
        for (int i = 0; i < printed.length; i++) printed[i] = buffer.readVarLong();
    }

    //////////////////////////////////////
    // ********** Drawing *********//
    //////////////////////////////////////

    @Override
    @OnlyIn(Dist.CLIENT)
    public void drawInBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        super.drawInBackground(graphics, mouseX, mouseY, partialTicks);
        Font font = Minecraft.getInstance().font;
        int x0 = getPosition().x;
        int y0 = getPosition().y;
        LithoMode active = LithoMode.values()[Math.max(0, Math.min(mode, LithoMode.values().length - 1))];
        long time = System.currentTimeMillis();

        // panel with scanlines and a slow sweep line
        graphics.fill(x0, y0, x0 + WIDTH, y0 + HEIGHT, BG);
        for (int y = 15; y < HEIGHT; y += 3) graphics.fill(x0 + 1, y0 + y, x0 + WIDTH - 1, y0 + y + 1, 0x0AFFFFFF);
        int sweep = (int) (time / 40 % (HEIGHT - 16)) + 15;
        graphics.fill(x0 + 1, y0 + sweep, x0 + WIDTH - 1, y0 + sweep + 1, withAlpha(active.argb, 0x22));
        border(graphics, x0, y0, WIDTH, HEIGHT, EDGE);

        // header: title, status word and LED
        graphics.fill(x0 + 1, y0 + 1, x0 + WIDTH - 1, y0 + 14, PANEL);
        graphics.fill(x0 + 1, y0 + 14, x0 + WIDTH - 1, y0 + 15, withAlpha(active.argb, 0xAA));
        graphics.drawString(font, Component.translatable("af9.litho.console.title").getString(), x0 + 6, y0 + 3, TEXT,
                false);
        int ledColor = statusColor();
        boolean ledOn = status != STATUS_RUNNING || (time / 400) % 2 == 0;
        graphics.fill(x0 + WIDTH - 12, y0 + 4, x0 + WIDTH - 6, y0 + 10, ledOn ? ledColor : withAlpha(ledColor, 0x55));
        String statusText = Component.translatable("af9.litho.console.status." + status).getString();
        graphics.drawString(font, statusText, x0 + WIDTH - 16 - font.width(statusText), y0 + 3, ledColor, false);

        // mode tiles
        for (LithoMode tileMode : LithoMode.values()) {
            int tx = x0 + tileX(tileMode.ordinal());
            int ty = y0 + TILE_Y;
            boolean selected = tileMode == active;
            boolean powered = status != STATUS_OFFLINE && available >= tileMode.eut();
            graphics.fill(tx, ty, tx + TILE_W, ty + TILE_H, selected ? withAlpha(tileMode.argb, 0x55) : PANEL);
            border(graphics, tx, ty, TILE_W, TILE_H, selected ? tileMode.argb : withAlpha(tileMode.argb, 0x66));
            String name = tileMode.name();
            graphics.drawString(font, name, tx + (TILE_W - font.width(name)) / 2, ty + 3,
                    powered || selected ? tileMode.argb : withAlpha(tileMode.argb, 0x88), false);
            drawSmall(graphics, font, tileMode.nodeNm + "nm", tx + TILE_W / 2, ty + 13, powered ? MUTED : 0xFF4B5567,
                    true);
        }

        // left: power gauge, progress and the process track
        int lx = x0 + 6;
        drawSmall(graphics, font, Component.translatable("af9.litho.console.power").getString(), lx, y0 + 48, MUTED,
                false);
        long needed = active.eut();
        boolean enough = available >= needed;
        bar(graphics, lx, y0 + 55, 88, 5, needed == 0 ? 0 : Math.min(1.0, (double) available / needed),
                enough ? GOOD : BAD);
        graphics.drawString(font, compact(available) + "/" + compact(needed) + " EU/t", lx, y0 + 62,
                enough ? TEXT : BAD, false);

        drawSmall(graphics, font, Component.translatable("af9.litho.console.progress").getString(), lx, y0 + 75, MUTED,
                false);
        double fraction = progress / 1000.0;
        bar(graphics, lx, y0 + 82, 88, 5, fraction, active.argb);
        String percent = Math.round(fraction * 100) + "%";
        graphics.drawString(font, percent, lx + 88 - font.width(percent), y0 + 89, TEXT, false);

        int lit = status == STATUS_RUNNING ? Math.min(STATION_LETTERS.length, (int) (fraction * STATION_LETTERS.length) + 1) : 0;
        for (int i = 0; i < STATION_LETTERS.length; i++) {
            int sx = lx + i * 11;
            int sy = y0 + 104;
            boolean current = i == lit - 1;
            int fill = i < lit ? withAlpha(active.argb, current && (time / 250) % 2 == 0 ? 0xEE : 0x88) : PANEL;
            graphics.fill(sx, sy, sx + 9, sy + 9, fill);
            border(graphics, sx, sy, 9, 9, withAlpha(active.argb, 0x66));
            graphics.drawString(font, STATION_LETTERS[i], sx + 5 - font.width(STATION_LETTERS[i]) / 2, sy + 1,
                    i < lit ? 0xFF0A0E16 : MUTED, false);
        }
        drawSmall(graphics, font, Component.translatable("af9.litho.console.track").getString(), lx, y0 + 96, MUTED,
                false);

        // right: output data and counters
        int rx = x0 + 98;
        drawSmall(graphics, font, Component.translatable("af9.litho.console.output").getString(), rx, y0 + 48, MUTED,
                false);
        graphics.drawString(font, active.nodeNm + " nm node", rx, y0 + 55, active.argb, false);
        graphics.drawString(font, "Density " + LithoMode.formatFactor(active.transistorDensity()), rx, y0 + 65, TEXT,
                false);
        graphics.drawString(font, "Dies " + LithoMode.formatFactor(active.dieFactor()), rx, y0 + 75, TEXT, false);
        graphics.drawString(font, Component.translatable("af9.litho.substrate." + active.substrate).getString(), rx,
                y0 + 85, MUTED, false);

        drawSmall(graphics, font, Component.translatable("af9.litho.console.printed").getString(), rx, y0 + 100, MUTED,
                false);
        graphics.fill(x0 + RESET_X, y0 + RESET_Y, x0 + RESET_X + RESET_W, y0 + RESET_Y + RESET_H, PANEL);
        border(graphics, x0 + RESET_X, y0 + RESET_Y, RESET_W, RESET_H, EDGE);
        drawSmall(graphics, font, Component.translatable("af9.litho.console.reset").getString(),
                x0 + RESET_X + RESET_W / 2, y0 + RESET_Y + 2, TEXT, true);
        for (LithoMode counterMode : LithoMode.values()) {
            int cx = rx + counterMode.ordinal() * 17;
            drawSmall(graphics, font, compact(printed[counterMode.ordinal()]), cx + 8, y0 + 111, counterMode.argb, true);
        }
    }

    @OnlyIn(Dist.CLIENT)
    private int statusColor() {
        return switch (status) {
            case STATUS_RUNNING -> GOOD;
            case STATUS_IDLE -> WARN;
            case STATUS_NO_POWER, STATUS_MAINTENANCE -> BAD;
            case STATUS_PAUSED -> 0xFF60A5FA;
            default -> MUTED;
        };
    }

    /** Text at 3/4 scale; x is the left edge, or the centre when {@code centered}. */
    @OnlyIn(Dist.CLIENT)
    private static void drawSmall(GuiGraphics graphics, Font font, String text, int x, int y, int color,
                                  boolean centered) {
        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0);
        graphics.pose().scale(0.75F, 0.75F, 1F);
        int offset = centered ? -font.width(text) / 2 : 0;
        graphics.drawString(font, text, offset, 0, color, false);
        graphics.pose().popPose();
    }

    @OnlyIn(Dist.CLIENT)
    private static void bar(GuiGraphics graphics, int x, int y, int width, int height, double fraction, int color) {
        graphics.fill(x, y, x + width, y + height, 0xFF1E293B);
        int filled = (int) Math.round(width * Math.max(0, Math.min(1, fraction)));
        if (filled > 0) graphics.fill(x, y, x + filled, y + height, color);
    }

    @OnlyIn(Dist.CLIENT)
    private static void border(GuiGraphics graphics, int x, int y, int width, int height, int color) {
        graphics.fill(x, y, x + width, y + 1, color);
        graphics.fill(x, y + height - 1, x + width, y + height, color);
        graphics.fill(x, y, x + 1, y + height, color);
        graphics.fill(x + width - 1, y, x + width, y + height, color);
    }

    private static int withAlpha(int argb, int alpha) {
        return (alpha << 24) | (argb & 0xFFFFFF);
    }

    /** 512, 7.7k, 31k, 123k, 1.2M: short enough for the console columns. */
    static String compact(long value) {
        if (value < 1000) return Long.toString(value);
        if (value < 10_000) return String.format(Locale.ROOT, "%.1fk", value / 1000.0);
        if (value < 1_000_000) return (value / 1000) + "k";
        if (value < 10_000_000) return String.format(Locale.ROOT, "%.1fM", value / 1_000_000.0);
        return (value / 1_000_000) + "M";
    }
}
