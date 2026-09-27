package com.af9.core.machine.console;

import com.af9.core.litho.LithoMode;
import com.af9.core.machine.LithoConsoleWidget;
import com.af9.core.machine.LithoMachine;
import com.af9.core.machine.PhotolithographyScannerMachine;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.gui.widget.SlotWidget;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;

import com.lowdragmc.lowdraglib.gui.texture.ColorBorderTexture;
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.widget.ButtonWidget;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;

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

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * The Photolithography Scanner's screen (see {@link ScannerUIWidget} for the frame around it), the orbital station's
 * layout:
 * <ul>
 * <li>left, the exposure field: the scanner's two nodes (click one to switch to it; a node above the scanner's version
 * shows the version it needs), the optical column (the ArF laser, the reticle stage scanning, the projection lens with
 * one element per window section, the immersion water of 65 nm, the wafer stage) and the wafer from above, exposed
 * die by die in the node's colour, the state and the run-time bar;</li>
 * <li>right, the exposure panel: the node and its light, what is printing on which substrate, the ArF laser slot
 * (kept, never used up), the version's speed and break bonus, the on/off and batch switches, the break chance, the
 * counters and a hint for the current state;</li>
 * <li>beside the player inventory ({@link SidePanel}): left the process (vacuum, version, immersion, laser), right the
 * system (status, power, tier, batch, switch).</li>
 * </ul>
 * The server samples the machine every tick and sends the state only when something changed.
 */
public class ScannerConsoleWidget extends ConsoleWidget {

    public static final int WIDTH = 384;
    public static final int HEIGHT = 148;
    /** Left field and right panel of the page. */
    public static final int FIELD_X = 4, FIELD_Y = 4, FIELD_W = 240, FIELD_H = 140;
    public static final int PANEL_X = 250, PANEL_Y = 4, PANEL_W = 130, PANEL_H = 140;
    public static final int TILE_Y = 8, TILE_H = 20;
    /** The ArF laser slot. */
    public static final int SLOT_X = PANEL_X + 5, SLOT_Y = 58;
    public static final int SWITCH_X = PANEL_X + 5, SWITCH_Y = 80, SWITCH_W = 80, SWITCH_H = 16;
    public static final int BATCH_X = SWITCH_X + SWITCH_W + 4, BATCH_W = PANEL_W - 10 - SWITCH_W - 4;
    public static final int RESET_W = 30, RESET_H = 9, RESET_X = PANEL_X + PANEL_W - 5 - RESET_W, RESET_Y = 110;
    /** The optical column (centre x, top, bottom) and the wafer (centre) in the field. */
    private static final int COLUMN_X = FIELD_X + 58, COLUMN_TOP = 34, COLUMN_BOTTOM = 110;
    private static final int WAFER_X = FIELD_X + 172, WAFER_Y = 72;
    private static final WaferView WAFER = new WaferView(32, 8);
    /** Structure length of version 1, and the blocks one version adds. */
    public static final int LENGTH_V1 = 10, LENGTH_STEP = 2;

    private final PhotolithographyScannerMachine machine;

    // last state sent to / received by the client
    private int status = -1;
    private int mode;
    private int version;
    private int tier;
    private long available;
    private int progress;
    private int duration;
    private int cleanliness; // x10
    private int printVacuum; // x10: lowest vacuum of the running print
    private int vacuum;
    private int breakChance; // x10000
    private long printed;
    private long broken;
    private boolean workingEnabled;
    private boolean batchEnabled;
    /** Prints in the running batch (0: nothing running). */
    private int batch;
    private boolean laser;
    private String product = "";

    public ScannerConsoleWidget(PhotolithographyScannerMachine machine, int x, int y) {
        super(x, y, WIDTH, HEIGHT);
        this.machine = machine;
    }

    /**
     * The page: this console, the node tiles (a click switches to the node), the laser slot, the on/off and batch
     * switches and the counter reset. The slot works on the handler's storage: the handler refuses inserts (no pipe
     * access).
     */
    public static WidgetGroup createPage(PhotolithographyScannerMachine machine) {
        var page = new WidgetGroup(0, 0, WIDTH, HEIGHT);
        page.addWidget(new ScannerConsoleWidget(machine, 0, 0));
        List<LithoMode> modes = machine.getModes();
        for (int i = 0; i < modes.size(); i++) {
            int index = i;
            var tile = new ButtonWidget(tileX(i, modes.size()), TILE_Y, tileWidth(modes.size()), TILE_H,
                    IGuiTexture.EMPTY, click -> {
                        if (click.isRemote || machine.getActiveRecipeType() == index) return;
                        // as GT's mode button: switch, then let the recipe logic look again
                        machine.setActiveRecipeType(index);
                        machine.getRecipeLogic().updateTickSubscription();
                    });
            tile.setHoverTexture(new ColorBorderTexture(1, 0xFFFFFFFF));
            Component[] lines = LithoConsoleWidget.tileTooltip(modes.get(i));
            Component[] tooltip = Arrays.copyOf(lines, lines.length + 1);
            tooltip[lines.length] = Component.translatable("af9.scanner.console.select");
            tile.setHoverTooltips(tooltip);
            page.addWidget(tile);
        }
        page.addWidget(new SlotWidget(machine.laserSlot.storage, 0, SLOT_X, SLOT_Y, true, true)
                .setBackgroundTexture(GuiTextures.SLOT)
                .setHoverTooltips(Component.translatable("af9.scanner.console.laser_tooltip")));
        var power = new ButtonWidget(SWITCH_X, SWITCH_Y, SWITCH_W, SWITCH_H, IGuiTexture.EMPTY, click -> {
            if (!click.isRemote) machine.setWorkingEnabled(!machine.isWorkingEnabled());
        });
        power.setHoverTexture(new ColorBorderTexture(1, 0xFFFFFFFF));
        power.setHoverTooltips(Component.translatable("af9.scanner.console.switch_tooltip"));
        page.addWidget(power);
        var batchSwitch = new ButtonWidget(BATCH_X, SWITCH_Y, BATCH_W, SWITCH_H, IGuiTexture.EMPTY, click -> {
            if (!click.isRemote) machine.setBatchEnabled(!machine.isBatchEnabled());
        });
        batchSwitch.setHoverTexture(new ColorBorderTexture(1, 0xFFFFFFFF));
        batchSwitch.setHoverTooltips(Component.translatable("af9.orbital.console.batch_tooltip"));
        page.addWidget(batchSwitch);
        var reset = new ButtonWidget(RESET_X, RESET_Y, RESET_W, RESET_H, IGuiTexture.EMPTY, click -> {
            if (!click.isRemote) machine.resetCounters();
        });
        reset.setHoverTexture(new ColorBorderTexture(1, 0xFFFFFFFF));
        reset.setHoverTooltips(Component.translatable("af9.litho.console.reset_tooltip"));
        page.addWidget(reset);
        return page;
    }

    public static int tileWidth(int count) {
        return (FIELD_W - 12 - 3 * (count - 1)) / count;
    }

    public static int tileX(int index, int count) {
        return FIELD_X + 6 + index * (tileWidth(count) + 3);
    }

    //////////////////////////////////////
    // *********** Sync ***********//
    //////////////////////////////////////

    @Override
    protected boolean sample() {
        LithoMode active = machine.getActiveMode();
        var logic = machine.getRecipeLogic();
        ResourceLocation current = machine.getCurrentProduct();
        GTRecipe running = logic.isWorking() ? logic.getLastRecipe() : null;
        int[] now = {
                LithoConsoleWidget.statusOf(machine), active.ordinal(), machine.getVersion(),
                machine.isFormed() ? machine.getTier() : -1,
                logic.isWorking() ? logic.getProgress() : 0, logic.isWorking() ? logic.getDuration() : 0,
                (int) Math.round(machine.getCleanliness() * 10), (int) Math.round(machine.getPrintVacuum() * 10),
                machine.getVacuumState(), (int) Math.round(machine.currentBreakChance(active) * 10000),
                machine.isWorkingEnabled() ? 1 : 0, machine.isBatchEnabled() ? 1 : 0,
                running == null ? 0 : LithoMachine.printsIn(running), machine.hasLaser() ? 1 : 0 };
        int[] before = { status, mode, version, tier, progress, duration, cleanliness, printVacuum, vacuum,
                breakChance, workingEnabled ? 1 : 0, batchEnabled ? 1 : 0, batch, laser ? 1 : 0 };
        long newAvailable = machine.getAvailableEUt();
        String newProduct = current == null ? "" : current.toString();
        boolean changed = !Arrays.equals(now, before) || newAvailable != available ||
                machine.getPrinted() != printed || machine.getBroken() != broken ||
                !Objects.equals(newProduct, product);
        status = now[0];
        mode = now[1];
        version = now[2];
        tier = now[3];
        progress = now[4];
        duration = now[5];
        cleanliness = now[6];
        printVacuum = now[7];
        vacuum = now[8];
        breakChance = now[9];
        workingEnabled = now[10] == 1;
        batchEnabled = now[11] == 1;
        batch = now[12];
        laser = now[13] == 1;
        available = newAvailable;
        printed = machine.getPrinted();
        broken = machine.getBroken();
        product = newProduct;
        return changed;
    }

    @Override
    protected void writeState(FriendlyByteBuf buffer) {
        for (int value : new int[] { status, mode, version, tier, progress, duration, cleanliness, printVacuum,
                vacuum, breakChance, batch }) {
            buffer.writeVarInt(value);
        }
        buffer.writeBoolean(workingEnabled);
        buffer.writeBoolean(batchEnabled);
        buffer.writeBoolean(laser);
        buffer.writeVarLong(available);
        buffer.writeVarLong(printed);
        buffer.writeVarLong(broken);
        buffer.writeUtf(product);
    }

    @Override
    protected void readState(FriendlyByteBuf buffer) {
        status = buffer.readVarInt();
        mode = buffer.readVarInt();
        version = buffer.readVarInt();
        tier = buffer.readVarInt();
        progress = buffer.readVarInt();
        duration = buffer.readVarInt();
        cleanliness = buffer.readVarInt();
        printVacuum = buffer.readVarInt();
        vacuum = buffer.readVarInt();
        breakChance = buffer.readVarInt();
        batch = buffer.readVarInt();
        workingEnabled = buffer.readBoolean();
        batchEnabled = buffer.readBoolean();
        laser = buffer.readBoolean();
        available = buffer.readVarLong();
        printed = buffer.readVarLong();
        broken = buffer.readVarLong();
        product = buffer.readUtf();
    }

    private LithoMode active() {
        return LithoMode.values()[Math.max(0, Math.min(mode, LithoMode.values().length - 1))];
    }

    /** Window sections (lens slices) of the version: 2 at version 1, one more per version. */
    private static int slices(int version) {
        return Math.max(2, version + 1);
    }

    //////////////////////////////////////
    // ********** Drawing *********//
    //////////////////////////////////////

    @Override
    @OnlyIn(Dist.CLIENT)
    public void drawInBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        int x0 = getPosition().x;
        int y0 = getPosition().y;
        LithoMode active = active();
        graphics.fill(x0, y0, x0 + WIDTH, y0 + HEIGHT, BG);
        border(graphics, x0, y0, WIDTH, HEIGHT, EDGE);
        drawField(graphics, x0 + FIELD_X, y0 + FIELD_Y, active, partialTicks);
        drawPanel(graphics, x0 + PANEL_X, y0 + PANEL_Y, active);
        super.drawInBackground(graphics, mouseX, mouseY, partialTicks);
    }

    /** The exposure field: node tiles, optical column, the wafer, state and run time. */
    @OnlyIn(Dist.CLIENT)
    private void drawField(GuiGraphics graphics, int x, int y, LithoMode active, float partialTicks) {
        Font font = font();
        int x0 = getPosition().x;
        int y0 = getPosition().y;
        graphics.fill(x, y, x + FIELD_W, y + FIELD_H, 0xFF070A11);
        border(graphics, x, y, FIELD_W, FIELD_H, EDGE);
        for (int gx = x + 12; gx < x + FIELD_W; gx += 16) graphics.fill(gx, y + 1, gx + 1, y + FIELD_H - 1, 0x0CFFFFFF);
        for (int gy = y + 12; gy < y + FIELD_H; gy += 16) graphics.fill(x + 1, gy, x + FIELD_W - 1, gy + 1, 0x0CFFFFFF);

        // node tiles: the node and its substrate, or the version a locked node needs
        List<LithoMode> modes = machine.getModes();
        for (int i = 0; i < modes.size(); i++) {
            LithoMode tileMode = modes.get(i);
            boolean locked = tileMode.level() > version;
            boolean powered = status != STATUS_OFFLINE && !locked && available >= tileMode.eut();
            String detail = locked ? Component.translatable("af9.scanner.console.needs_short", tileMode.level())
                    .getString() :
                    Component.translatable("af9.litho.substrate_short." + tileMode.substrate).getString();
            drawTile(graphics, x0 + tileX(i, modes.size()), y0 + TILE_Y, tileWidth(modes.size()), TILE_H,
                    tileMode.nodeNm + "nm", detail, tileMode.argb, tileMode == active, powered, locked);
        }

        boolean running = status == STATUS_RUNNING;
        double fraction = duration <= 0 ? 0 : Math.min(1.0, (progress + (running ? partialTicks : 0)) / duration);
        long time = System.currentTimeMillis();
        int color = active.argb;
        drawColumn(graphics, x0 + COLUMN_X, y0 + COLUMN_TOP, y0 + COLUMN_BOTTOM, active, running, time);

        // the wafer from above, exposed die by die; the beam from the lens down to the die
        int cx = x0 + WAFER_X, cy = y0 + WAFER_Y;
        int[] die = WAFER.draw(graphics, cx, cy, fraction, running, status == STATUS_OFFLINE, color, time);
        if (die != null) WaferView.drawBeam(graphics, cx, y0 + TILE_Y + TILE_H + 4, die[0], die[1], color, time);

        // state and run time
        int midX = x + FIELD_W / 2;
        String state = running ?
                Component.translatable("af9.scanner.console.exposing").getString() + "  " +
                        Math.round(fraction * 100) + "%" :
                Component.translatable("af9.console.status." + status).getString();
        graphics.drawString(font, state, midX - font.width(state) / 2, y + 117, running ? color : statusColor(status),
                false);
        bar(graphics, x + 8, y + 128, FIELD_W - 16, 4, running ? fraction : 0, color);
        String timeText = running ? seconds(progress) + " / " + seconds(duration) :
                Component.translatable("af9.console.no_run").getString();
        drawSmall(graphics, timeText, midX, y + 133, running ? TEXT : MUTED, true);
    }

    /**
     * The optical column from the side: the ArF laser on top, the reticle stage scanning to and fro while it exposes,
     * the projection lens (one element per window section), the immersion water film (65 nm) and the wafer stage.
     */
    @OnlyIn(Dist.CLIENT)
    private void drawColumn(GuiGraphics graphics, int cx, int top, int bottom, LithoMode active, boolean running,
                            long time) {
        int color = active.argb;
        boolean lit = running && laser;
        // the laser
        int laserColor = laser ? 0xFF6B4FA0 : 0xFF3A2F4A;
        graphics.fill(cx - 30, top, cx + 30, top + 11, 0xFF141B2A);
        border(graphics, cx - 30, top, 60, 11, laser ? withAlpha(laserColor, 0xFF) : BAD);
        graphics.fill(cx - 27, top + 3, cx - 21, top + 8, lit ? withAlpha(0xFFFFFF, 0xE0) : laserColor);
        drawSmall(graphics, laser ? "ArF 193nm" : Component.translatable("af9.scanner.console.no_laser").getString(),
                cx + 3, top + 3, laser ? MUTED : BAD, true);
        // the beam down the column
        int beamTop = top + 11;
        int reticleY = top + 18;
        int lensTop = reticleY + 10;
        int slices = slices(version);
        int lensGap = Math.min(10, (bottom - 16 - lensTop) / Math.max(1, slices));
        int lensBottom = lensTop + slices * lensGap;
        int stageY = bottom - 6;
        if (lit) {
            float glow = WaferView.pulse(time, 500);
            for (int by = beamTop; by < stageY; by++) {
                // wide above the lens, narrowing through it onto the wafer
                int half = by < lensTop ? 6 : by > lensBottom ? 2 :
                        6 - (int) Math.round(4.0 * (by - lensTop) / Math.max(1, lensBottom - lensTop));
                graphics.fill(cx - half, by, cx + half, by + 1, withAlpha(color, 0x28 + (int) (0x30 * glow)));
            }
            graphics.fill(cx, beamTop, cx + 1, stageY, withAlpha(0xFFFFFF, 0x70));
        }
        // the reticle stage: the mask scans across the slit while a die is exposed
        int shift = running ? (int) Math.round(10 * Math.sin(time * 2 * Math.PI / 1600)) : 0;
        graphics.fill(cx - 26, reticleY + 2, cx + 26, reticleY + 5, 0xFF2A3346);
        int rx = cx - 12 + shift;
        graphics.fill(rx, reticleY, rx + 24, reticleY + 7, 0xFFB8D8E6);
        for (int i = 0; i < 5; i++) graphics.fill(rx + 3 + i * 4, reticleY + 2, rx + 5 + i * 4, reticleY + 5, 0xFF343A42);
        drawSmall(graphics, Component.translatable("af9.scanner.console.reticle").getString(), cx + 28, reticleY + 1,
                DIM, false);
        // the projection lens: one element per window section
        for (int i = 0; i < slices; i++) {
            int ly = lensTop + i * lensGap;
            int half = 20 - i * 2;
            graphics.fill(cx - half, ly + 1, cx + half, ly + 3, version > 0 ? 0xFF9FC6DB : 0xFF3B4A57);
            graphics.fill(cx - half + 3, ly, cx + half - 3, ly + 4, version > 0 ? 0x889FC6DB : 0x553B4A57);
        }
        drawSmall(graphics, Component.translatable("af9.scanner.console.lens", slices).getString(), cx + 28,
                lensTop + 1, DIM, false);
        // immersion water under the last element (65 nm)
        if (active.immersion()) {
            graphics.fill(cx - 10, stageY - 3, cx + 10, stageY - 1, lit ? 0xFF3BA0E8 : 0xFF245A80);
            drawSmall(graphics, Component.translatable("af9.scanner.console.water").getString(), cx + 28, stageY - 5,
                    DIM, false);
        }
        // the wafer stage with its wafer
        graphics.fill(cx - 28, stageY, cx + 28, stageY + 5, 0xFF2A3346);
        graphics.fill(cx - 14, stageY - 1, cx + 14, stageY + 1, running ? withAlpha(color, 0xC0) : 0xFF4A5670);
    }

    /** The exposure panel: node, product, laser slot, version bonus, switches, break chance, counters, hint. */
    @OnlyIn(Dist.CLIENT)
    private void drawPanel(GuiGraphics graphics, int x, int y, LithoMode active) {
        Font font = font();
        int x0 = getPosition().x;
        int y0 = getPosition().y;
        graphics.fill(x, y, x + PANEL_W, y + PANEL_H, PANEL);
        border(graphics, x, y, PANEL_W, PANEL_H, EDGE);
        graphics.fill(x + 1, y + 1, x + PANEL_W - 1, y + 2, withAlpha(active.argb, 0xAA));

        graphics.drawString(font, active.nodeNm + " nm", x + 5, y + 5, active.argb, false);
        String light = Component.translatable("af9.litho.light." + active.light).getString();
        drawSmall(graphics, light, x + PANEL_W - 5 - font.width(light) * 3 / 4, y + 7, MUTED, false);

        drawSmall(graphics, Component.translatable("af9.litho.console.output").getString(), x + 5, y + 18, MUTED,
                false);
        ItemStack stack = productStack();
        if (!stack.isEmpty()) {
            graphics.renderItem(stack, x + 5, y + 25);
            graphics.drawString(font, fit(stack.getHoverName().getString(), PANEL_W - 30), x + 24, y + 26, TEXT, false);
        } else {
            graphics.drawString(font, Component.translatable("af9.litho.console.nothing").getString(), x + 24, y + 26,
                    MUTED, false);
        }
        String substrate = Component.translatable("af9.scanner.console.on",
                Component.translatable("af9.litho.substrate." + active.substrate)).getString();
        drawSmall(graphics, fit(substrate, (PANEL_W - 30) * 4 / 3), x + 24, y + 35, MUTED, false);

        // the laser slot (left) and the version's bonus (right)
        drawSmall(graphics, Component.translatable("af9.scanner.console.laser").getString(), x0 + SLOT_X, y + 46,
                MUTED, false);
        drawSmall(graphics, fit(Component.translatable(laser ? "af9.scanner.console.laser_in" :
                "af9.scanner.console.laser_missing").getString(), 40), x0 + SLOT_X + 21, y0 + SLOT_Y + 6,
                laser ? GOOD : BAD, false);
        int bx0 = x + 70;
        int bw = PANEL_W - 75;
        drawSmall(graphics, Component.translatable("af9.scanner.console.version").getString(), bx0, y + 46, MUTED,
                false);
        int surplus = version - active.level();
        if (version <= 0) {
            drawSmall(graphics, "-", bx0, y + 56, MUTED, false);
        } else if (surplus < 0) {
            drawSmall(graphics, fit(Component.translatable("af9.scanner.console.needs", active.level()).getString(),
                    bw * 4 / 3), bx0, y + 56, BAD, false);
        } else {
            graphics.drawString(font, "V" + version, bx0, y + 54, surplus > 0 ? GOOD : TEXT, false);
            drawSmall(graphics, fit(Component.translatable("af9.scanner.console.speed",
                    LithoMode.formatFactor(1 / LithoMode.speedFactor(surplus))).getString(), bw * 4 / 3), bx0, y + 64,
                    surplus > 0 ? GOOD : MUTED, false);
            drawSmall(graphics, fit(Component.translatable("af9.scanner.console.breaks",
                    LithoMode.formatFactor(Math.pow(LithoMode.VERSION_BREAK_FACTOR, surplus))).getString(),
                    bw * 4 / 3), bx0, y + 71, surplus > 0 ? GOOD : MUTED, false);
        }

        // the on/off switch
        int sx = x0 + SWITCH_X, sy = y0 + SWITCH_Y;
        graphics.fill(sx, sy, sx + SWITCH_W, sy + SWITCH_H, workingEnabled ? withAlpha(GOOD, 0x28) : 0xFF0B0F17);
        border(graphics, sx, sy, SWITCH_W, SWITCH_H, workingEnabled ? GOOD : DIM);
        graphics.fill(sx + 5, sy + 5, sx + 11, sy + 11, workingEnabled ? GOOD : DIM);
        String switchText = Component.translatable(workingEnabled ? "af9.orbital.console.online" :
                "af9.orbital.console.offline").getString();
        graphics.drawString(font, switchText, sx + 7 + (SWITCH_W - 7 - font.width(switchText)) / 2, sy + 4,
                workingEnabled ? GOOD : MUTED, false);
        // batch mode switch
        int bx = x0 + BATCH_X;
        graphics.fill(bx, sy, bx + BATCH_W, sy + SWITCH_H, batchEnabled ? withAlpha(INFO, 0x28) : 0xFF0B0F17);
        border(graphics, bx, sy, BATCH_W, SWITCH_H, batchEnabled ? INFO : DIM);
        drawSmall(graphics, Component.translatable("af9.orbital.console.batch").getString(), bx + BATCH_W / 2, sy + 5,
                batchEnabled ? INFO : MUTED, true);

        double chance = breakChance / 10000.0;
        drawSmall(graphics, Component.translatable("af9.litho.console.break").getString(), x + 5, y + 97, MUTED,
                false);
        String chanceText = LithoMode.formatPercent(chance);
        graphics.drawString(font, chanceText, x + PANEL_W - 5 - font.width(chanceText), y + 95,
                chance <= 0.05 ? GOOD : chance <= 0.2 ? WARN : BAD, false);

        long total = printed + broken;
        String yield = total == 0 ? "-" : Math.round(100.0 * printed / total) + "%";
        String counters = Component.translatable("af9.orbital.console.counters", compact(printed), compact(broken),
                yield).getString();
        drawSmall(graphics, fit(counters, (x0 + RESET_X - 3 - (x + 5)) * 4 / 3), x + 5, y0 + RESET_Y + 2, MUTED,
                false);
        drawButton(graphics, x0 + RESET_X, y0 + RESET_Y, RESET_W, RESET_H,
                Component.translatable("af9.litho.console.reset").getString());

        // what to do now
        Component hint = Component.translatableWithFallback("af9.scanner.hint." + status,
                Component.translatable("af9.console.status." + status).getString());
        int line = 0;
        for (FormattedCharSequence part : font.split(hint, (PANEL_W - 10) * 4 / 3)) {
            if (line >= 3) break;
            graphics.pose().pushPose();
            graphics.pose().translate(x + 5, y + 119 + line * 7, 0);
            graphics.pose().scale(0.75F, 0.75F, 1F);
            graphics.drawString(font, part, 0, 0, status == STATUS_RUNNING ? MUTED : statusColor(status), false);
            graphics.pose().popPose();
            line++;
        }
    }

    @OnlyIn(Dist.CLIENT)
    private ItemStack productStack() {
        if (product.isEmpty()) return ItemStack.EMPTY;
        ResourceLocation id = ResourceLocation.tryParse(product);
        Item item = id == null ? null : ForgeRegistries.ITEMS.getValue(id);
        return item == null || item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item);
    }

    //////////////////////////////////////
    // ******** Side panels *********//
    //////////////////////////////////////

    /**
     * A panel beside the player inventory, drawn from the console's synced state: left the process (vacuum, version,
     * immersion, laser), right the system (status, power, tier, batch, switch).
     */
    public static class SidePanel extends Widget {

        private final ScannerConsoleWidget console;
        private final boolean system;

        public SidePanel(ScannerConsoleWidget console, boolean system, int x, int y, int width, int height) {
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
            LithoMode active = console.active();
            graphics.fill(x, y, x + w, y + h, PANEL);
            border(graphics, x, y, w, h, EDGE);
            graphics.fill(x + 1, y + 1, x + w - 1, y + 2, withAlpha(active.argb, 0xAA));
            drawSmall(graphics, Component.translatable(system ? "af9.orbital.console.system" :
                    "af9.orbital.console.process").getString(), x + 4, y + 5, TEXT, false);
            if (system) drawSystem(graphics, x + 4, y + 16, w - 8, active);
            else drawProcess(graphics, x + 4, y + 16, w - 8, active);
            super.drawInBackground(graphics, mouseX, mouseY, partialTicks);
        }

        @OnlyIn(Dist.CLIENT)
        private void drawProcess(GuiGraphics graphics, int x, int y, int w, LithoMode active) {
            // the vacuum: cleanliness, pump state, the dip of the running print
            double clean = console.cleanliness / 10.0;
            row(graphics, x, y, w, "af9.litho.console.vacuum", String.format(Locale.ROOT, "%.1f", clean),
                    levelColor(clean));
            bar(graphics, x, y + 8, w, 3, clean / 100.0, levelColor(clean));
            int pumpState = console.status == STATUS_OFFLINE ? LithoMachine.VACUUM_OFF : console.vacuum;
            String pump = Component.translatable(LithoConsoleWidget.vacuumKey(pumpState)).getString();
            double low = console.printVacuum / 10.0;
            if (console.status == STATUS_RUNNING && low < 99.95) {
                pump = Component.translatable("af9.litho.console.print_vacuum",
                        String.format(Locale.ROOT, "%.1f", low)).getString();
            }
            drawSmall(graphics, fit(pump, w * 4 / 3), x, y + 13, console.status == STATUS_RUNNING && low < 99.95 ?
                    BAD : LithoConsoleWidget.vacuumColor(pumpState), false);
            // the version: its length and window sections
            int version = console.version;
            row(graphics, x, y + 22, w, "af9.scanner.console.version", version > 0 ? "V" + version : "-",
                    version > 0 ? TEXT : MUTED);
            if (version > 0) {
                drawSmall(graphics, fit(Component.translatable("af9.scanner.console.length",
                        LENGTH_V1 + (version - 1) * LENGTH_STEP, slices(version)).getString(), w * 4 / 3), x, y + 29,
                        MUTED, false);
            }
            // immersion (65 nm) and the laser
            row(graphics, x, y + 38, w, "af9.scanner.console.immersion",
                    Component.translatable(active.immersion() ? "af9.scanner.console.immersion_on" :
                            "af9.scanner.console.immersion_off").getString(),
                    active.immersion() ? INFO : MUTED);
            row(graphics, x, y + 47, w, "af9.scanner.console.laser",
                    Component.translatable(console.laser ? "af9.scanner.console.laser_in" :
                            "af9.scanner.console.laser_missing").getString(),
                    console.laser ? GOOD : BAD);
        }

        @OnlyIn(Dist.CLIENT)
        private void drawSystem(GuiGraphics graphics, int x, int y, int w, LithoMode active) {
            row(graphics, x, y, w, "af9.orbital.console.status",
                    Component.translatable("af9.console.status." + console.status).getString(),
                    statusColor(console.status));
            long needed = active.eut();
            boolean enough = console.available >= needed;
            row(graphics, x, y + 10, w, "af9.console.power",
                    compact(console.available) + "/" + compact(needed), enough ? TEXT : BAD);
            bar(graphics, x, y + 18, w, 3, needed <= 0 ? 0 : Math.min(1.0, (double) console.available / needed),
                    enough ? GOOD : BAD);
            drawSmall(graphics, "EU/t, " + active.amperage() + "A " + GTValues.VN[active.hatchTier], x, y + 23, MUTED,
                    false);
            row(graphics, x, y + 33, w, "af9.orbital.console.tier",
                    console.tier < 0 ? "-" : GTValues.VNF[Math.min(console.tier, GTValues.VNF.length - 1)],
                    TEXT);
            String batchText = !console.batchEnabled ?
                    Component.translatable("af9.orbital.console.batch_off").getString() :
                    console.batch > 1 ? console.batch + "x" :
                            Component.translatable("af9.orbital.console.batch_on").getString();
            row(graphics, x, y + 43, w, "af9.orbital.console.batch", batchText,
                    !console.batchEnabled ? MUTED : console.batch > 1 ? GOOD : INFO);
            row(graphics, x, y + 53, w, "af9.orbital.console.switch",
                    Component.translatable(console.workingEnabled ? "af9.orbital.console.online" :
                            "af9.orbital.console.offline").getString(),
                    console.workingEnabled ? GOOD : MUTED);
        }

        /** Label left, value right, both small. */
        @OnlyIn(Dist.CLIENT)
        private static void row(GuiGraphics graphics, int x, int y, int w, String labelKey, String value, int color) {
            Font font = font();
            drawSmall(graphics, Component.translatable(labelKey).getString(), x, y, MUTED, false);
            drawSmall(graphics, value, x + w - font.width(value) * 3 / 4, y, color, false);
        }
    }
}
