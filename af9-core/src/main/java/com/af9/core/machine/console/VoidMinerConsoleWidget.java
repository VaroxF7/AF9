package com.af9.core.machine.console;

import com.af9.core.machine.ProcessMachine;
import com.af9.core.machine.VoidMinerMachine;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.recipe.GTRecipeType;
import com.gregtechceu.gtceu.utils.FormattingUtil;

import com.lowdragmc.lowdraglib.gui.texture.ColorBorderTexture;
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.widget.ButtonWidget;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;

import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringJoiner;

/**
 * The Void Miner's screen (all three miners), the Orbital Lithography Station's layout (see
 * {@link SidePanelsUIWidget}):
 * <ul>
 * <li>left, the ore chart of the area the miner stands in: a cell per programmed circuit with the ores a run of it
 * brings (the stack sizes on them, the chance on hover), the circuit in the buses and the one being mined marked;</li>
 * <li>right, the run: the area, the state with the run-time bar, the circuit and the drilling fluid the miner has
 * against what a run takes, the energy of a run, the on/off switch, the run counter and what to do now;</li>
 * <li>beside the player inventory ({@link SidePanel}): left the process (area, circuit, fluid, state), right the
 * system (status, power, tier, switch, runs).</li>
 * </ul>
 * The server samples the machine every tick and sends the state only when something changed; the chart goes over on
 * its own, when it changed.
 */
public class VoidMinerConsoleWidget extends ConsoleWidget {

    public static final int WIDTH = 384;
    public static final int HEIGHT = 148;
    /** Left field and right panel of the page. */
    public static final int FIELD_X = 4, FIELD_Y = 4, FIELD_W = 240, FIELD_H = 140;
    public static final int PANEL_X = 250, PANEL_Y = 4, PANEL_W = 130, PANEL_H = 140;
    public static final int SWITCH_X = PANEL_X + 5, SWITCH_Y = PANEL_Y + 76, SWITCH_W = PANEL_W - 10, SWITCH_H = 16;
    public static final int RESET_W = 30, RESET_H = 9, RESET_X = PANEL_X + PANEL_W - 5 - RESET_W,
            RESET_Y = PANEL_Y + 95;
    /** The chart's grid in the field: so many rows a column, a cell with its circuit number in front. */
    private static final int GRID_X = FIELD_X + 4, GRID_Y = FIELD_Y + 16, GRID_W = FIELD_W - 8, ROWS = 7,
            ROW_H = 17, CELL_H = 16, BADGE_W = 11;
    /** Update id of the chart (the state is 1, {@link ConsoleWidget}). */
    private static final int CHART_UPDATE = 2;

    private final VoidMinerMachine machine;

    // last state sent to / received by the client
    private int status = -1;
    /** Index of the active recipe type among the miner's. */
    private int mode;
    private int tier;
    private int progress;
    private int duration;
    private boolean workingEnabled;
    /** The programmed circuit in the buses and the one of the running recipe, -1 for none. */
    private int circuit = -1;
    private int runCircuit = -1;
    /** The voltage tier the circuit's recipe runs at, -1 when the circuit picks none. */
    private int needTier = -1;
    private long available;
    private long needed;
    private long energyPerRun;
    private long runs;
    /** Drilling fluid in the hatches and what a run takes, mB. */
    private long fluidHave;
    private long fluidNeed;
    /** The ore chart as the machine writes it ({@link VoidMinerMachine#oreChart}), and as it was last sent. */
    private String chart = "";
    private String sentChart = "";
    /** Client: the chart, read. */
    private List<Entry> entries = List.of();

    /** A cell of the chart: a circuit, the stacks a run of it can bring and the chance of each in percent. */
    private record Entry(int circuit, List<ItemStack> ores, List<Integer> chances) {}

    public VoidMinerConsoleWidget(VoidMinerMachine machine, int x, int y) {
        super(x, y, WIDTH, HEIGHT);
        this.machine = machine;
    }

    /** The page: this console, the on/off switch and the counter reset. */
    public static WidgetGroup createPage(VoidMinerMachine machine) {
        var page = new WidgetGroup(0, 0, WIDTH, HEIGHT);
        page.addWidget(new VoidMinerConsoleWidget(machine, 0, 0));
        var power = new ButtonWidget(SWITCH_X, SWITCH_Y, SWITCH_W, SWITCH_H, IGuiTexture.EMPTY, click -> {
            if (!click.isRemote) machine.setWorkingEnabled(!machine.isWorkingEnabled());
        });
        power.setHoverTexture(new ColorBorderTexture(1, 0xFFFFFFFF));
        power.setHoverTooltips(Component.translatable("af9.voidminer.console.switch_tooltip"));
        page.addWidget(power);
        var reset = new ButtonWidget(RESET_X, RESET_Y, RESET_W, RESET_H, IGuiTexture.EMPTY, click -> {
            if (!click.isRemote) machine.resetRuns();
        });
        reset.setHoverTexture(new ColorBorderTexture(1, 0xFFFFFFFF));
        reset.setHoverTooltips(Component.translatable("af9.voidminer.console.reset_tooltip"));
        page.addWidget(reset);
        return page;
    }

    //////////////////////////////////////
    // *********** Sync ***********//
    //////////////////////////////////////

    @Override
    protected boolean sample() {
        var logic = machine.getRecipeLogic();
        int[] now = { machine.getStatus(), machine.getActiveRecipeType(), machine.isFormed() ? machine.getTier() : -1,
                logic.isWorking() ? logic.getProgress() : 0, logic.isWorking() ? logic.getDuration() : 0,
                machine.isWorkingEnabled() ? 1 : 0, machine.circuitSet(), machine.circuitRunning(),
                machine.tierNeeded() };
        int[] before = { status, mode, tier, progress, duration, workingEnabled ? 1 : 0, circuit, runCircuit,
                needTier };
        long[] nowLong = { machine.getAvailableEUt(), machine.getNeededEUt(), machine.getEnergyPerRun(),
                machine.getRuns(), machine.fluidAvailable(), machine.fluidNeeded() };
        long[] beforeLong = { available, needed, energyPerRun, runs, fluidHave, fluidNeed };
        boolean changed = !Arrays.equals(now, before) || !Arrays.equals(nowLong, beforeLong);
        status = now[0];
        mode = now[1];
        tier = now[2];
        progress = now[3];
        duration = now[4];
        workingEnabled = now[5] != 0;
        circuit = now[6];
        runCircuit = now[7];
        needTier = now[8];
        available = nowLong[0];
        needed = nowLong[1];
        energyPerRun = nowLong[2];
        runs = nowLong[3];
        fluidHave = nowLong[4];
        fluidNeed = nowLong[5];
        chart = machine.oreChart();
        return changed;
    }

    @Override
    protected void writeState(FriendlyByteBuf buffer) {
        for (int value : new int[] { status, mode, tier, progress, duration, circuit, runCircuit, needTier }) {
            buffer.writeVarInt(value);
        }
        buffer.writeBoolean(workingEnabled);
        for (long value : new long[] { available, needed, energyPerRun, runs, fluidHave, fluidNeed }) {
            buffer.writeVarLong(value);
        }
    }

    @Override
    protected void readState(FriendlyByteBuf buffer) {
        status = buffer.readVarInt();
        mode = buffer.readVarInt();
        tier = buffer.readVarInt();
        progress = buffer.readVarInt();
        duration = buffer.readVarInt();
        circuit = buffer.readVarInt();
        runCircuit = buffer.readVarInt();
        needTier = buffer.readVarInt();
        workingEnabled = buffer.readBoolean();
        available = buffer.readVarLong();
        needed = buffer.readVarLong();
        energyPerRun = buffer.readVarLong();
        runs = buffer.readVarLong();
        fluidHave = buffer.readVarLong();
        fluidNeed = buffer.readVarLong();
    }

    /** The state as every console sends it, and the chart when it is another than the one last sent. */
    @Override
    public void detectAndSendChanges() {
        super.detectAndSendChanges();
        if (!chart.equals(sentChart)) {
            sentChart = chart;
            String sent = chart;
            writeUpdateInfo(CHART_UPDATE, buffer -> buffer.writeUtf(sent));
        }
    }

    @Override
    public void writeInitialData(FriendlyByteBuf buffer) {
        super.writeInitialData(buffer);
        sentChart = chart;
        buffer.writeUtf(chart);
    }

    @Override
    public void readInitialData(FriendlyByteBuf buffer) {
        super.readInitialData(buffer);
        readChart(buffer.readUtf());
    }

    @Override
    public void readUpdateInfo(int id, FriendlyByteBuf buffer) {
        if (id == CHART_UPDATE) {
            readChart(buffer.readUtf());
        } else {
            super.readUpdateInfo(id, buffer);
        }
    }

    /** "circuit=id*count*percent,...;circuit=..." to the chart's cells. */
    private void readChart(String encoded) {
        List<Entry> read = new ArrayList<>();
        if (!encoded.isEmpty()) {
            for (String part : encoded.split(";")) {
                int equals = part.indexOf('=');
                if (equals <= 0) continue;
                List<ItemStack> ores = new ArrayList<>();
                List<Integer> chances = new ArrayList<>();
                for (String ore : part.substring(equals + 1).split(",")) {
                    String[] fields = ore.split("\\*");
                    if (fields.length < 3) continue;
                    ResourceLocation id = ResourceLocation.tryParse(fields[0]);
                    Item item = id == null ? null : ForgeRegistries.ITEMS.getValue(id);
                    if (item == null || item == Items.AIR) continue;
                    ores.add(new ItemStack(item, number(fields[1], 1)));
                    chances.add(number(fields[2], 100));
                }
                read.add(new Entry(number(part.substring(0, equals), 0), ores, chances));
            }
        }
        entries = read;
    }

    private static int number(String text, int fallback) {
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    /** The active recipe type (the area the miner works in). */
    private GTRecipeType activeType() {
        GTRecipeType[] types = machine.getRecipeTypes();
        return types[Math.max(0, Math.min(types.length - 1, mode))];
    }

    private int modeColor() {
        return machine.modeColor(Math.max(0, VoidMinerMachine.areaOf(activeType())));
    }

    private String modeKey() {
        return ProcessMachine.modeKey(activeType());
    }

    /** Whether the miner stands formed in an area with ores: only then the chart, the area's name and colour hold. */
    private boolean charted() {
        return status != STATUS_OFFLINE && status != STATUS_NO_DIMENSION && !entries.isEmpty() &&
                VoidMinerMachine.areaOf(activeType()) >= 0;
    }

    /** The area's short name, "-" while the miner is in none. */
    private String areaShort() {
        return charted() ? Component.translatable(modeKey() + ".short").getString() : "-";
    }

    private boolean inChart(int number) {
        for (Entry entry : entries) {
            if (entry.circuit() == number) return true;
        }
        return false;
    }

    /** "none" or the number of the circuit in the buses. */
    private String circuitText() {
        return circuit < 0 ? Component.translatable("af9.voidminer.console.circuit_none").getString() :
                Integer.toString(circuit);
    }

    private int circuitColor() {
        if (!charted()) return MUTED;
        return circuit < 0 ? BAD : inChart(circuit) ? TEXT : WARN;
    }

    /** "1.2k/2.0k" of drilling fluid, "-" where no run is known. */
    private String fluidText() {
        return fluidNeed <= 0 ? "-" : compact(fluidHave) + "/" + compact(fluidNeed);
    }

    private int fluidColor() {
        return fluidNeed <= 0 || !charted() ? MUTED : fluidHave >= fluidNeed ? TEXT : BAD;
    }

    /** What to do now: for an idle miner the first thing a run still waits for. */
    private Component hint() {
        if (status == STATUS_IDLE) {
            if (entries.isEmpty()) return Component.translatable("af9.voidminer.hint.no_ores");
            if (circuit < 0) return Component.translatable("af9.voidminer.hint.no_circuit");
            if (!inChart(circuit)) return Component.translatable("af9.voidminer.hint.circuit_unknown", circuit);
            if (needTier > tier && tier >= 0) {
                return Component.translatable("af9.voidminer.hint.low_tier", tierName(needTier), tierName(tier));
            }
            if (fluidHave < fluidNeed) {
                return Component.translatable("af9.voidminer.hint.no_fluid", FormattingUtil.formatNumbers(fluidHave),
                        FormattingUtil.formatNumbers(fluidNeed));
            }
        }
        return Component.translatable("af9.voidminer.hint." + status);
    }

    private static String tierName(int tier) {
        return GTValues.VN[Math.max(0, Math.min(GTValues.VN.length - 1, tier))];
    }

    //////////////////////////////////////
    // ********** The chart *********//
    //////////////////////////////////////

    /** One column up to {@link #ROWS} cells, two up to twice that, else three. */
    private int columns() {
        int count = entries.size();
        return count <= ROWS ? 1 : count <= 2 * ROWS ? 2 : 3;
    }

    /** The cells that fit; an area with more says so under the chart. */
    private int shown() {
        return Math.min(entries.size(), columns() * ROWS);
    }

    /** Rows of a column: the cells run down the first column, then the next. */
    private int rows() {
        int columns = columns();
        return Math.max(1, (shown() + columns - 1) / columns);
    }

    private int cellX(int index) {
        return getPosition().x + GRID_X + (index / rows()) * (GRID_W / columns());
    }

    private int cellY(int index) {
        return getPosition().y + GRID_Y + (index % rows()) * ROW_H;
    }

    /** The cell under the mouse, null for none. */
    private Entry entryAt(double mouseX, double mouseY) {
        if (!charted()) return null;
        int width = GRID_W / columns() - 1;
        for (int i = 0; i < shown(); i++) {
            if (isMouseOver(cellX(i), cellY(i), width, CELL_H, mouseX, mouseY)) return entries.get(i);
        }
        return null;
    }

    //////////////////////////////////////
    // ********** Drawing *********//
    //////////////////////////////////////

    @Override
    @OnlyIn(Dist.CLIENT)
    public void drawInBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        int x0 = getPosition().x;
        int y0 = getPosition().y;
        graphics.fill(x0, y0, x0 + WIDTH, y0 + HEIGHT, BG);
        border(graphics, x0, y0, WIDTH, HEIGHT, EDGE);
        drawChart(graphics, x0 + FIELD_X, y0 + FIELD_Y, mouseX, mouseY);
        drawPanel(graphics, x0 + PANEL_X, y0 + PANEL_Y, partialTicks);
        super.drawInBackground(graphics, mouseX, mouseY, partialTicks);
    }

    /** The ore chart: its heading, a cell per circuit; where there is no chart, why. */
    @OnlyIn(Dist.CLIENT)
    private void drawChart(GuiGraphics graphics, int x, int y, int mouseX, int mouseY) {
        Font font = font();
        graphics.fill(x, y, x + FIELD_W, y + FIELD_H, 0xFF070A11);
        border(graphics, x, y, FIELD_W, FIELD_H, EDGE);
        boolean charted = charted();
        String title = Component.translatable("af9.voidminer.console.chart").getString();
        drawSmall(graphics, title, x + 5, y + 5, TEXT, false);
        int used = x + 5 + font.width(title) * 3 / 4;
        if (charted) {
            String area = areaShort();
            drawSmall(graphics, area, used + 5, y + 5, modeColor(), false);
            used += 5 + font.width(area) * 3 / 4;
        }
        String legend = Component.translatable("af9.voidminer.console.chart_legend").getString();
        int legendX = x + FIELD_W - 5 - font.width(legend) * 3 / 4;
        if (legendX >= used + 6) drawSmall(graphics, legend, legendX, y + 5, MUTED, false);
        graphics.fill(x + 1, y + 13, x + FIELD_W - 1, y + 14, EDGE);

        if (!charted) {
            // no chart: the structure is not formed, the dimension has no ores, or the area has no recipes
            Component why = Component.translatable(status == STATUS_OFFLINE ? "af9.voidminer.hint.0" :
                    status == STATUS_NO_DIMENSION ? "af9.voidminer.hint.21" : "af9.voidminer.hint.no_ores");
            List<FormattedCharSequence> lines = font.split(why, FIELD_W - 40);
            int ty = y + 14 + (FIELD_H - 14 - lines.size() * 10) / 2;
            for (FormattedCharSequence line : lines) {
                graphics.drawString(font, line, x + (FIELD_W - font.width(line)) / 2, ty,
                        status == STATUS_OFFLINE ? MUTED : statusColor(status), false);
                ty += 10;
            }
            return;
        }

        int columns = columns();
        int width = GRID_W / columns - 1;
        int pitch = columns == 3 ? 16 : 18;
        int most = 0;
        for (Entry entry : entries) most = Math.max(most, entry.ores().size());
        Entry hovered = entryAt(mouseX, mouseY);
        int shown = shown();
        for (int i = 0; i < shown; i++) {
            Entry entry = entries.get(i);
            drawCell(graphics, font, entry, cellX(i), cellY(i), width, pitch, most, entry == hovered);
        }
        if (shown < entries.size()) {
            String more = Component.translatable("af9.voidminer.chart.more", entries.size() - shown).getString();
            drawSmall(graphics, more, x + FIELD_W - 5 - font.width(more) * 3 / 4, y + FIELD_H - 8, MUTED, false);
        }
    }

    /**
     * A cell: the circuit number, the ores of a run with their stack sizes and, where the cell is wide enough, their
     * names. The circuit in the buses is tinted, the one being mined tinted and framed by a pulse.
     */
    @OnlyIn(Dist.CLIENT)
    private void drawCell(GuiGraphics graphics, Font font, Entry entry, int cx, int cy, int width, int pitch,
                          int most, boolean hovered) {
        int color = modeColor();
        boolean mining = status == STATUS_RUNNING && entry.circuit() == runCircuit;
        boolean set = entry.circuit() == circuit;
        boolean marked = mining || set;
        graphics.fill(cx, cy, cx + width, cy + CELL_H, 0xFF0B0F17);
        if (marked) graphics.fill(cx, cy, cx + width, cy + CELL_H, withAlpha(color, mining ? 0x38 : 0x20));
        graphics.fill(cx, cy, cx + BADGE_W, cy + CELL_H, marked ? withAlpha(color, 0x80) : TRACK);
        drawSmall(graphics, Integer.toString(entry.circuit()), cx + BADGE_W / 2 + 1, cy + 5, marked ? TEXT : MUTED,
                true);
        int ix = cx + BADGE_W + 1;
        for (ItemStack ore : entry.ores()) {
            graphics.renderItem(ore, ix, cy);
            drawCount(graphics, font, compact(ore.getCount()), ix, cy);
            ix += pitch;
        }
        int textX = cx + BADGE_W + 1 + most * pitch + 3;
        int textWidth = cx + width - 2 - textX;
        if (textWidth >= 40) {
            StringJoiner names = new StringJoiner(", ");
            for (ItemStack ore : entry.ores()) names.add(ore.getHoverName().getString());
            drawSmall(graphics, fit(names.toString(), textWidth * 4 / 3), textX, cy + 5, marked ? TEXT : MUTED,
                    false);
        }
        if (mining) {
            double pulse = 0.5 + 0.5 * Math.sin(Util.getMillis() / 200.0);
            border(graphics, cx, cy, width, CELL_H, withAlpha(color, 0x70 + (int) (0x8F * pulse)));
        } else if (set) {
            border(graphics, cx, cy, width, CELL_H, color);
        } else if (hovered) {
            border(graphics, cx, cy, width, CELL_H, MUTED);
        }
    }

    /** A stack size at the foot of its item, small, over the item. */
    @OnlyIn(Dist.CLIENT)
    private static void drawCount(GuiGraphics graphics, Font font, String text, int x, int y) {
        graphics.pose().pushPose();
        graphics.pose().translate(x + 17 - font.width(text) * 0.75F, y + 10, 200);
        graphics.pose().scale(0.75F, 0.75F, 1F);
        graphics.drawString(font, text, 0, 0, 0xFFFFFFFF, true);
        graphics.pose().popPose();
    }

    /** The run: area, state and run time, circuit, drilling fluid, energy, switch, counter, hint. */
    @OnlyIn(Dist.CLIENT)
    private void drawPanel(GuiGraphics graphics, int x, int y, float partialTicks) {
        Font font = font();
        int x0 = getPosition().x;
        int y0 = getPosition().y;
        boolean charted = charted();
        boolean running = status == STATUS_RUNNING;
        int color = charted ? modeColor() : MUTED;
        int inner = PANEL_W - 10;
        graphics.fill(x, y, x + PANEL_W, y + PANEL_H, PANEL);
        border(graphics, x, y, PANEL_W, PANEL_H, EDGE);
        graphics.fill(x + 1, y + 1, x + PANEL_W - 1, y + 2, withAlpha(color, 0xAA));

        // the area by its full name (small where that is too long for the panel); the miner's own name while it is in none
        String title = Component.translatable(charted ? modeKey() : machine.titleKey()).getString();
        if (font.width(title) <= inner) {
            graphics.drawString(font, title, x + 5, y + 5, color, false);
        } else {
            drawSmall(graphics, fit(title, inner * 4 / 3), x + 5, y + 6, color, false);
        }

        // the state, and the run under it
        double fraction = duration <= 0 ? 0 : Math.min(1.0, (progress + (running ? partialTicks : 0)) / duration);
        String percent = running ? Math.round(fraction * 100) + "%" : "";
        String state = running ? Component.translatable("af9.voidminer.console.mining").getString() :
                Component.translatable("af9.console.status." + status).getString();
        graphics.drawString(font, fit(state, inner - font.width(percent) - 4), x + 5, y + 17,
                running ? color : statusColor(status), false);
        graphics.drawString(font, percent, x + PANEL_W - 5 - font.width(percent), y + 17, TEXT, false);
        border(graphics, x + 4, y + 27, inner + 2, 8, EDGE);
        bar(graphics, x + 5, y + 28, inner, 6, running ? fraction : 0, color);
        String time = running ? seconds(progress) + " / " + seconds(duration) :
                Component.translatable("af9.console.no_run").getString();
        drawSmall(graphics, time, x + 5, y + 38, running ? TEXT : MUTED, false);

        // what the miner has against what a run takes
        row(graphics, x + 5, y + 48, inner, "af9.voidminer.console.circuit", circuitText(), circuitColor());
        row(graphics, x + 5, y + 57, inner, "af9.voidminer.console.fluid",
                fluidNeed <= 0 ? "-" : fluidText() + " mB", fluidColor());
        row(graphics, x + 5, y + 66, inner, "af9.voidminer.console.energy",
                energyPerRun > 0 ? compact(energyPerRun) + " EU" : "-", energyPerRun > 0 ? TEXT : MUTED);

        // the on/off switch
        int sx = x0 + SWITCH_X, sy = y0 + SWITCH_Y;
        graphics.fill(sx, sy, sx + SWITCH_W, sy + SWITCH_H, workingEnabled ? withAlpha(GOOD, 0x28) : 0xFF0B0F17);
        border(graphics, sx, sy, SWITCH_W, SWITCH_H, workingEnabled ? GOOD : DIM);
        graphics.fill(sx + 5, sy + 5, sx + 11, sy + 11, workingEnabled ? GOOD : DIM);
        String switchText = Component.translatable(workingEnabled ? "af9.orbital.console.online" :
                "af9.orbital.console.offline").getString();
        graphics.drawString(font, switchText, sx + 7 + (SWITCH_W - 7 - font.width(switchText)) / 2, sy + 4,
                workingEnabled ? GOOD : MUTED, false);

        // the run counter and its reset
        String counter = Component.translatable("af9.voidminer.console.runs", compact(runs)).getString();
        drawSmall(graphics, fit(counter, (x0 + RESET_X - 3 - (x + 5)) * 4 / 3), x + 5, y0 + RESET_Y + 2, MUTED,
                false);
        drawButton(graphics, x0 + RESET_X, y0 + RESET_Y, RESET_W, RESET_H,
                Component.translatable("af9.litho.console.reset").getString());

        // what to do now
        int line = 0;
        for (FormattedCharSequence part : font.split(hint(), inner * 4 / 3)) {
            if (line >= 4) break;
            graphics.pose().pushPose();
            graphics.pose().translate(x + 5, y + 110 + line * 7, 0);
            graphics.pose().scale(0.75F, 0.75F, 1F);
            graphics.drawString(font, part, 0, 0, running ? MUTED : statusColor(status), false);
            graphics.pose().popPose();
            line++;
        }
    }

    /** A chart cell under the mouse says the whole of it: the circuit, each ore with its stack and its chance. */
    @Override
    @OnlyIn(Dist.CLIENT)
    public void drawInForeground(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        super.drawInForeground(graphics, mouseX, mouseY, partialTicks);
        Entry entry = entryAt(mouseX, mouseY);
        if (entry == null || gui == null || gui.getModularUIGui() == null) return;
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable("af9.voidminer.chart.circuit", entry.circuit()));
        for (int i = 0; i < entry.ores().size(); i++) {
            ItemStack ore = entry.ores().get(i);
            lines.add(Component.translatable("af9.voidminer.chart.ore", ore.getCount(), ore.getHoverName(),
                    entry.chances().get(i)).withStyle(ChatFormatting.GRAY));
        }
        boolean mining = status == STATUS_RUNNING && entry.circuit() == runCircuit;
        boolean set = entry.circuit() == circuit;
        lines.add(Component.translatable(mining ? "af9.voidminer.chart.mining" : set ? "af9.voidminer.chart.set" :
                "af9.voidminer.chart.pick", entry.circuit())
                .withStyle(mining || set ? ChatFormatting.GREEN : ChatFormatting.DARK_GRAY));
        gui.getModularUIGui().setHoverTooltip(lines, ItemStack.EMPTY, null, null);
    }

    /** Label left, value right, both small. */
    @OnlyIn(Dist.CLIENT)
    private static void row(GuiGraphics graphics, int x, int y, int w, String labelKey, String value, int color) {
        Font font = font();
        drawSmall(graphics, Component.translatable(labelKey).getString(), x, y, MUTED, false);
        drawSmall(graphics, value, x + w - font.width(value) * 3 / 4, y, color, false);
    }

    //////////////////////////////////////
    // ******** Side panels *********//
    //////////////////////////////////////

    /**
     * A panel beside the player inventory, drawn from the console's synced state: left the process (area, circuit,
     * fluid, state), right the system (status, power, tier, switch, runs).
     */
    public static class SidePanel extends Widget {

        private final VoidMinerConsoleWidget console;
        private final boolean system;

        public SidePanel(VoidMinerConsoleWidget console, boolean system, int x, int y, int width, int height) {
            super(x, y, width, height);
            this.console = console;
            this.system = system;
        }

        @Override
        @OnlyIn(Dist.CLIENT)
        public void drawInBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
            int x = getPosition().x;
            int y = getPosition().y;
            int w = getSize().width;
            int h = getSize().height;
            graphics.fill(x, y, x + w, y + h, PANEL);
            border(graphics, x, y, w, h, EDGE);
            graphics.fill(x + 1, y + 1, x + w - 1, y + 2,
                    withAlpha(console.charted() ? console.modeColor() : MUTED, 0xAA));
            drawSmall(graphics, Component.translatable(system ? "af9.orbital.console.system" :
                    "af9.orbital.console.process").getString(), x + 4, y + 5, TEXT, false);
            if (system) drawSystem(graphics, x + 4, y + 16, w - 8);
            else drawProcess(graphics, x + 4, y + 16, w - 8);
            super.drawInBackground(graphics, mouseX, mouseY, partialTicks);
        }

        @OnlyIn(Dist.CLIENT)
        private void drawProcess(GuiGraphics graphics, int x, int y, int w) {
            boolean running = console.status == STATUS_RUNNING;
            int color = console.charted() ? console.modeColor() : MUTED;
            row(graphics, x, y, w, "af9.voidminer.console.mode", console.areaShort(), color);
            row(graphics, x, y + 10, w, "af9.voidminer.console.circuit", console.circuitText(),
                    console.circuitColor());
            row(graphics, x, y + 20, w, "af9.voidminer.console.fluid_short", console.fluidText(),
                    console.fluidColor());
            row(graphics, x, y + 30, w, "af9.voidminer.console.state",
                    Component.translatable("af9.console.status." + console.status).getString(),
                    statusColor(console.status));
            // the shaft in the panel: drill light running down while it mines
            long now = Util.getMillis();
            for (int i = 0; i < w; i += 3) {
                boolean lit = running && ((i + now / 40) % 30 < 8);
                graphics.fill(x + i, y + 42, x + i + 2, y + 43, lit ? color : TRACK);
            }
        }

        @OnlyIn(Dist.CLIENT)
        private void drawSystem(GuiGraphics graphics, int x, int y, int w) {
            row(graphics, x, y, w, "af9.orbital.console.status",
                    Component.translatable("af9.console.status." + console.status).getString(),
                    statusColor(console.status));
            long needed = console.needed;
            boolean enough = needed <= 0 || console.available >= needed;
            row(graphics, x, y + 10, w, "af9.console.power",
                    compact(console.available) + "/" + (needed > 0 ? compact(needed) : "-") + " EU/t",
                    enough ? TEXT : BAD);
            bar(graphics, x, y + 18, w, 3, needed <= 0 ? (console.available > 0 ? 1 : 0) :
                    Math.min(1.0, (double) console.available / needed), enough ? GOOD : BAD);
            // the hatch's tier, and beside it the tier the ores of the circuit take where that is higher
            boolean low = console.needTier > console.tier && console.tier >= 0;
            String tierText = console.tier < 0 ? "-" : low ?
                    tierName(console.tier) + " < " + tierName(console.needTier) :
                    GTValues.VNF[Math.min(console.tier, GTValues.VNF.length - 1)];
            row(graphics, x, y + 26, w, "af9.orbital.console.tier", tierText, low ? BAD : TEXT);
            row(graphics, x, y + 36, w, "af9.orbital.console.switch",
                    Component.translatable(console.workingEnabled ? "af9.orbital.console.online" :
                            "af9.orbital.console.offline").getString(),
                    console.workingEnabled ? GOOD : MUTED);
            row(graphics, x, y + 46, w, "af9.voidminer.console.runs_label", compact(console.runs), TEXT);
        }
    }
}
