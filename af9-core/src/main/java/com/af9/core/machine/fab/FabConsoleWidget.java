package com.af9.core.machine.fab;

import com.af9.core.fab.FabFamily;
import com.af9.core.fab.IFabMachine;
import com.af9.core.machine.console.BusConsole;

import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.GTRecipeType;
import com.gregtechceu.gtceu.api.recipe.RecipeHelper;

import com.lowdragmc.lowdraglib.gui.widget.Widget;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import io.netty.buffer.Unpooled;

import java.util.Arrays;
import java.util.Locale;

/**
 * Console of the AF9 fab machines. The full layout (multiblocks) has mode tiles, power and progress gauges, the
 * process stage track, clean-room class, parallels, heat, changeover status and counters; the compact strip (single
 * blocks) sits above GT's slot layout. The server samples the machine every tick and syncs only what changed; mode
 * tiles are made clickable by invisible buttons (see {@link FabMultiblockMachine#createUIWidget()}).
 */
public class FabConsoleWidget extends Widget implements BusConsole {

    public static final int WIDTH = 190;
    public static final int HEIGHT = 125;
    public static final int STRIP_WIDTH = 176;
    public static final int STRIP_HEIGHT = 40;
    public static final int TILE_Y = 18;
    public static final int TILE_H = 20;
    // the changeover counter, clickable to reset all counters
    public static final int RESET_W = 64;
    public static final int RESET_H = 10;
    public static final int RESET_X = WIDTH - 4 - RESET_W;
    public static final int RESET_Y = 94;

    static final int STATUS_OFFLINE = 0, STATUS_IDLE = 1, STATUS_RUNNING = 2, STATUS_NO_POWER = 3, STATUS_PAUSED = 4,
            STATUS_MAINTENANCE = 5, STATUS_PURGING = 6;

    private static final int BG = 0xFF0A0E16, PANEL = 0xFF111827, EDGE = 0xFF25324A, TEXT = 0xFFE6EDF7,
            MUTED = 0xFF7C8AA5, GOOD = 0xFF4ADE80, BAD = 0xFFEF4444, WARN = 0xFFFBBF24, PURGE = 0xFFC084FC;

    private final IFabMachine machine;
    private final boolean compact;

    // last state sent to / received by the client
    private int status = -1;
    private int mode;
    private long available;
    private long needed;
    private int progress; // per mille
    private int purgeShare; // per mille of the run spent purging, 0 if not a changeover
    private int parallel = 1;
    private int cleanClass;
    private int heat;
    private long purges;
    private final long[] counters = new long[IFabMachine.MAX_MODES];

    public FabConsoleWidget(IFabMachine machine, int x, int y, boolean compact) {
        super(x, y, compact ? STRIP_WIDTH : WIDTH, compact ? STRIP_HEIGHT : HEIGHT);
        this.machine = machine;
        this.compact = compact;
    }

    /** Left edge of mode tile {@code index} of {@code count}, relative to the console. */
    public static int tileX(int index, int count) {
        return 6 + index * (tileW(count) + 2);
    }

    public static int tileW(int count) {
        int n = Math.max(1, count);
        return (WIDTH - 12 - 2 * (n - 1)) / n;
    }

    //////////////////////////////////////
    // *********** Sync ***********//
    //////////////////////////////////////

    private int sampleStatus() {
        if (!machine.isFabFormed()) return STATUS_OFFLINE;
        if (machine.hasFabMaintenanceProblems()) return STATUS_MAINTENANCE;
        var logic = machine.getRecipeLogic();
        if (!logic.isWorkingEnabled()) return STATUS_PAUSED;
        if (logic.isWaiting()) return STATUS_NO_POWER;
        if (logic.isWorking()) {
            return machine.isPurgeRun() && logic.getProgressPercent() * 1000 < samplePurgeShare() ? STATUS_PURGING :
                    STATUS_RUNNING;
        }
        return STATUS_IDLE;
    }

    private int samplePurgeShare() {
        if (!machine.isPurgeRun()) return 0;
        GTRecipe recipe = machine.getRecipeLogic().getLastRecipe();
        if (recipe == null || recipe.duration <= 0) return 0;
        return Math.min(1000, machine.getFabFamily().purgeTicks * 1000 / recipe.duration);
    }

    private long sampleNeeded() {
        GTRecipe recipe = machine.getRecipeLogic().getLastRecipe();
        if (recipe == null || !machine.getRecipeLogic().isActive()) return 0;
        return RecipeHelper.getRealEUt(recipe).getTotalEU();
    }

    private int sampleMode() {
        return Math.max(0, Math.min(machine.getActiveRecipeType(), IFabMachine.MAX_MODES - 1));
    }

    private boolean sample() {
        int newStatus = sampleStatus();
        int newMode = sampleMode();
        long newAvailable = machine.getFabAvailableEUt();
        long newNeeded = sampleNeeded();
        int newProgress = (int) Math.round(machine.getRecipeLogic().getProgressPercent() * 1000);
        int newPurgeShare = samplePurgeShare();
        int newParallel = machine.getFabParallel();
        int newClean = machine.getFabCleanClass();
        int newHeat = machine.getFabHeat();
        long newPurges = machine.getFabPurges();
        long[] newCounters = machine.getFabCounters();
        boolean changed = newStatus != status || newMode != mode || newAvailable != available || newNeeded != needed ||
                newProgress != progress || newPurgeShare != purgeShare || newParallel != parallel ||
                newClean != cleanClass || newHeat != heat || newPurges != purges ||
                !Arrays.equals(newCounters, counters);
        status = newStatus;
        mode = newMode;
        available = newAvailable;
        needed = newNeeded;
        progress = newProgress;
        purgeShare = newPurgeShare;
        parallel = newParallel;
        cleanClass = newClean;
        heat = newHeat;
        purges = newPurges;
        System.arraycopy(newCounters, 0, counters, 0, Math.min(newCounters.length, counters.length));
        return changed;
    }

    @Override
    public void detectAndSendChanges() {
        super.detectAndSendChanges();
        if (sample()) {
            writeUpdateInfo(1, this::writeState);
        }
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

    @Override
    public byte[] snapshot() {
        sample();
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            writeState(buffer);
            byte[] bytes = new byte[buffer.readableBytes()];
            buffer.readBytes(bytes);
            return bytes;
        } finally {
            buffer.release();
        }
    }

    @Override
    public void applySnapshot(byte[] bytes) {
        readState(new FriendlyByteBuf(Unpooled.wrappedBuffer(bytes)));
    }

    private void writeState(FriendlyByteBuf buffer) {
        buffer.writeVarInt(status);
        buffer.writeVarInt(mode);
        buffer.writeVarLong(available);
        buffer.writeVarLong(needed);
        buffer.writeVarInt(progress);
        buffer.writeVarInt(purgeShare);
        buffer.writeVarInt(parallel);
        buffer.writeVarInt(cleanClass);
        buffer.writeVarInt(heat);
        buffer.writeVarLong(purges);
        for (long count : counters) buffer.writeVarLong(count);
    }

    private void readState(FriendlyByteBuf buffer) {
        status = buffer.readVarInt();
        mode = buffer.readVarInt();
        available = buffer.readVarLong();
        needed = buffer.readVarLong();
        progress = buffer.readVarInt();
        purgeShare = buffer.readVarInt();
        parallel = buffer.readVarInt();
        cleanClass = buffer.readVarInt();
        heat = buffer.readVarInt();
        purges = buffer.readVarLong();
        for (int i = 0; i < counters.length; i++) counters[i] = buffer.readVarLong();
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
        int w = getSize().width;
        int h = getSize().height;
        FabFamily family = machine.getFabFamily();
        int accent = family.argb;
        GTRecipeType[] types = machine.getRecipeTypes();
        GTRecipeType active = types.length == 0 ? null : types[Math.min(mode, types.length - 1)];
        long time = System.currentTimeMillis();

        // panel with scanlines and a slow sweep line
        graphics.fill(x0, y0, x0 + w, y0 + h, BG);
        for (int y = 15; y < h; y += 3) graphics.fill(x0 + 1, y0 + y, x0 + w - 1, y0 + y + 1, 0x0AFFFFFF);
        int sweep = (int) (time / 40 % Math.max(1, h - 16)) + 15;
        graphics.fill(x0 + 1, y0 + sweep, x0 + w - 1, y0 + sweep + 1, withAlpha(accent, 0x22));
        border(graphics, x0, y0, w, h, EDGE);

        // header: title (or the active mode in the strip), status word and LED
        graphics.fill(x0 + 1, y0 + 1, x0 + w - 1, y0 + 14, PANEL);
        graphics.fill(x0 + 1, y0 + 14, x0 + w - 1, y0 + 15, withAlpha(accent, 0xAA));
        String title = compact && active != null ? modeName(active) :
                Component.translatable("af9.fab.console." + family.id).getString();
        graphics.drawString(font, title, x0 + 6, y0 + 3, compact ? accent : TEXT, false);
        int ledColor = statusColor();
        boolean ledOn = (status != STATUS_RUNNING && status != STATUS_PURGING) || (time / 400) % 2 == 0;
        graphics.fill(x0 + w - 12, y0 + 4, x0 + w - 6, y0 + 10, ledOn ? ledColor : withAlpha(ledColor, 0x55));
        String statusText = Component.translatable("af9.fab.console.status." + status).getString();
        graphics.drawString(font, statusText, x0 + w - 16 - font.width(statusText), y0 + 3, ledColor, false);

        if (compact) {
            drawStrip(graphics, font, x0, y0, w, active, accent, time);
        } else {
            drawFull(graphics, font, x0, y0, types, active, accent, time);
        }
    }

    @OnlyIn(Dist.CLIENT)
    private void drawStrip(GuiGraphics graphics, Font font, int x0, int y0, int w, GTRecipeType active, int accent,
                           long time) {
        drawStages(graphics, font, x0 + 4, y0 + 18, w - 8, 9, active, accent, time);
        bar(graphics, x0 + 4, y0 + 29, w - 8, 2, progress / 1000.0, accent);
        drawSmall(graphics, font, infoLine(), x0 + 5, y0 + 33, MUTED, false);
    }

    @OnlyIn(Dist.CLIENT)
    private void drawFull(GuiGraphics graphics, Font font, int x0, int y0, GTRecipeType[] types,
                          GTRecipeType active, int accent, long time) {
        // mode tiles
        int count = Math.min(types.length, IFabMachine.MAX_MODES);
        for (int i = 0; i < count; i++) {
            int tx = x0 + tileX(i, count);
            int ty = y0 + TILE_Y;
            int tw = tileW(count);
            boolean selected = i == mode;
            graphics.fill(tx, ty, tx + tw, ty + TILE_H, selected ? withAlpha(accent, 0x55) : PANEL);
            border(graphics, tx, ty, tw, TILE_H, selected ? accent : withAlpha(accent, 0x66));
            drawSmall(graphics, font, modeName(types[i]), tx + tw / 2, ty + 4, selected ? TEXT : MUTED, true);
            drawSmall(graphics, font, compact(counters[i]), tx + tw / 2, ty + 12, withAlpha(accent, 0xCC), true);
        }

        // left: power and progress
        int lx = x0 + 6;
        drawSmall(graphics, font, Component.translatable("af9.fab.console.power").getString(), lx, y0 + 44, MUTED,
                false);
        boolean enough = needed == 0 || available >= needed;
        bar(graphics, lx, y0 + 51, 88, 5, needed == 0 ? (available > 0 ? 1 : 0) : Math.min(1.0,
                (double) available / needed), enough ? GOOD : BAD);
        graphics.drawString(font, compact(needed) + "/" + compact(available) + " EU/t", lx, y0 + 58,
                enough ? TEXT : BAD, false);
        drawSmall(graphics, font, Component.translatable("af9.fab.console.progress").getString(), lx, y0 + 70,
                MUTED, false);
        bar(graphics, lx, y0 + 77, 88, 5, progress / 1000.0, accent);
        if (purgeShare > 0) {
            // the changeover part of the run, in purge colour
            graphics.fill(lx, y0 + 82, lx + (int) Math.round(88 * purgeShare / 1000.0), y0 + 83, PURGE);
        }
        String percent = Math.round(progress / 10.0) + "%";
        graphics.drawString(font, percent, lx + 88 - font.width(percent), y0 + 84, TEXT, false);

        // right: clean class, parallels, heat, changeover
        int rx = x0 + 100;
        drawSmall(graphics, font, Component.translatable("af9.fab.console.plant").getString(), rx, y0 + 44, MUTED,
                false);
        graphics.drawString(font, Component.translatable("af9.fab.console.clean." + cleanClass).getString(), rx,
                y0 + 51, cleanClass > 0 ? GOOD : MUTED, false);
        graphics.drawString(font, Component.translatable("af9.fab.console.parallel", parallel).getString(), rx,
                y0 + 60, TEXT, false);
        if (heat > 0) {
            graphics.drawString(font, Component.translatable("af9.fab.console.heat", heat).getString(), rx, y0 + 69,
                    WARN, false);
        }
        drawSmall(graphics, font, Component.translatable("af9.fab.console.purge." + machine.getFabFamily().id)
                .getString(), rx, y0 + 80, PURGE, false);

        // process stages across the bottom
        drawSmall(graphics, font, Component.translatable("af9.fab.console.process").getString(), lx, y0 + 96, MUTED,
                false);
        drawSmall(graphics, font, Component.translatable("af9.fab.console.purges", compact(purges)).getString(),
                x0 + WIDTH - 6, y0 + 96, PURGE, false, true);
        drawStages(graphics, font, lx, y0 + 103, WIDTH - 12, 16, active, accent, time);
    }

    /** One cell per process step; cells light up with progress, a changeover run starts with a purge cell. */
    @OnlyIn(Dist.CLIENT)
    private void drawStages(GuiGraphics graphics, Font font, int x, int y, int width, int height,
                            GTRecipeType active, int accent, long time) {
        String[] stages = FabFamily.stagesOf(active);
        boolean purging = purgeShare > 0;
        int cells = stages.length + (purging ? 1 : 0);
        int cellW = (width - 2 * (cells - 1)) / cells;
        boolean running = status == STATUS_RUNNING || status == STATUS_PURGING;
        double fraction = progress / 1000.0;
        // which cell is lit: the purge cell covers its share of the run, the stages split the rest
        int current = -1;
        if (running) {
            if (purging && progress < purgeShare) {
                current = 0;
            } else {
                double rest = purging ? (fraction - purgeShare / 1000.0) / Math.max(1e-6, 1 - purgeShare / 1000.0) :
                        fraction;
                current = (purging ? 1 : 0) + Math.min(stages.length - 1, (int) (rest * stages.length));
            }
        }
        for (int i = 0; i < cells; i++) {
            int cx = x + i * (cellW + 2);
            boolean isPurgeCell = purging && i == 0;
            int color = isPurgeCell ? PURGE : accent;
            boolean lit = running && i <= current;
            boolean blink = i == current && (time / 250) % 2 == 0;
            graphics.fill(cx, y, cx + cellW, y + height, lit ? withAlpha(color, blink ? 0xEE : 0x88) : PANEL);
            border(graphics, cx, y, cellW, height, withAlpha(color, 0x66));
            String label = isPurgeCell ? "PURGE" : stages[i - (purging ? 1 : 0)];
            drawSmall(graphics, font, label, cx + cellW / 2, y + (height - 6) / 2, lit ? BG : MUTED, true);
        }
    }

    private String infoLine() {
        String clean = Component.translatable("af9.fab.console.clean." + cleanClass).getString();
        String info = heat > 0 ? clean + "  " + Component.translatable("af9.fab.console.heat", heat).getString() :
                clean;
        return info + "  " + Component.translatable("af9.fab.console.purge." + machine.getFabFamily().id).getString();
    }

    private static String modeName(GTRecipeType type) {
        return Component.translatable("af9.fab.mode." + type.registryName.getPath()).getString();
    }

    @OnlyIn(Dist.CLIENT)
    private int statusColor() {
        return switch (status) {
            case STATUS_RUNNING -> GOOD;
            case STATUS_PURGING -> PURGE;
            case STATUS_IDLE -> WARN;
            case STATUS_NO_POWER, STATUS_MAINTENANCE -> BAD;
            case STATUS_PAUSED -> 0xFF60A5FA;
            default -> MUTED;
        };
    }

    @OnlyIn(Dist.CLIENT)
    private static void drawSmall(GuiGraphics graphics, Font font, String text, int x, int y, int color,
                                  boolean centered) {
        drawSmall(graphics, font, text, x, y, color, centered, false);
    }

    /** Text at 3/4 scale; x is the left edge, the centre when {@code centered} or the right edge when {@code right}. */
    @OnlyIn(Dist.CLIENT)
    private static void drawSmall(GuiGraphics graphics, Font font, String text, int x, int y, int color,
                                  boolean centered, boolean right) {
        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0);
        graphics.pose().scale(0.75F, 0.75F, 1F);
        int offset = centered ? -font.width(text) / 2 : right ? -font.width(text) : 0;
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
