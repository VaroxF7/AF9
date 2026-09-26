package com.af9.core.machine.console;

import com.af9.core.litho.Coolant;
import com.af9.core.litho.LithoMode;
import com.af9.core.machine.LithoConsoleWidget;
import com.af9.core.machine.LithoMachine;
import com.af9.core.machine.OrbitalLithographyMachine;

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
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * The Orbital Lithography Station's screen (see {@link OrbitalStationUIWidget} for the frame around it):
 * <ul>
 * <li>left, the exposure field: the station's four nodes, the wafer being printed, exposed die by die in the node's
 * colour under the X-ray / EUV beam, a progress ring around it, the state and the run-time bar;</li>
 * <li>right, the exposure panel: the node, what is printing, the reticle slot and the EUV Light Source slot (both
 * kept, never used up), the on/off switch and the batch mode switch, the break chance, the counters and a hint for the
 * current state;</li>
 * <li>beside the player inventory ({@link SidePanel}): left the process (start-up, coolant, computation, magnetic field,
 * orbit), right the system (status, power, tier, batch).</li>
 * </ul>
 * The server samples the machine every tick and sends the state only when something changed.
 */
public class OrbitalConsoleWidget extends ConsoleWidget {

    public static final int WIDTH = 384;
    public static final int HEIGHT = 148;
    /** Left field and right panel of the page. */
    public static final int FIELD_X = 4, FIELD_Y = 4, FIELD_W = 240, FIELD_H = 140;
    public static final int PANEL_X = 250, PANEL_Y = 4, PANEL_W = 130, PANEL_H = 140;
    public static final int TILE_Y = 8, TILE_H = 20;
    /** Reticle slot, EUV Light Source slot. */
    public static final int SLOT_X = PANEL_X + 5, EUV_X = PANEL_X + 67, SLOT_Y = 58;
    public static final int SWITCH_X = PANEL_X + 5, SWITCH_Y = 80, SWITCH_W = 80, SWITCH_H = 16;
    public static final int BATCH_X = SWITCH_X + SWITCH_W + 4, BATCH_W = PANEL_W - 10 - SWITCH_W - 4;
    public static final int RESET_W = 30, RESET_H = 9, RESET_X = PANEL_X + PANEL_W - 5 - RESET_W, RESET_Y = 110;
    /** Wafer drawing: centre, radius, die size. */
    private static final int WAFER_X = FIELD_X + FIELD_W / 2, WAFER_Y = 76, WAFER_R = 36, DIE = 8;
    private static final int[][] DIES = dieOrder();

    private final OrbitalLithographyMachine machine;

    // last state sent to / received by the client
    private int status = -1;
    private int mode;
    private long available;
    private int tier;
    private int progress;
    private int duration;
    private int startup; // percent x10
    private int vacuum;
    private int printVacuum; // x10
    private int breakChance; // x10000
    private long printed;
    private long broken;
    /** Coolant of the running / next print: grade, 0 none usable. */
    private int coolant;
    private boolean workingEnabled;
    /** Batch mode switched on, and the prints in the running batch (0: nothing running). */
    private boolean batchEnabled;
    private int batch;
    /** EUV Light Source: 0 the node does not use one, 1 in the slot, 2 missing. */
    private int euv;
    /** Reticle slot: 1 loaded, 0 empty. */
    private int reticle;
    /** Computation the network behind the computation hatches supplies at most, CWU/t. */
    private int compute;
    private boolean field;
    private boolean orbit;
    private String product = "";

    public OrbitalConsoleWidget(OrbitalLithographyMachine machine, int x, int y) {
        super(x, y, WIDTH, HEIGHT);
        this.machine = machine;
    }

    /**
     * The page: this console, the node tiles' tooltips, the reticle and EUV slots, the on/off switch and the counter
     * reset. The slots work on the handlers' storage: the handlers refuse inserts (no pipe access).
     */
    public static WidgetGroup createPage(OrbitalLithographyMachine machine) {
        var page = new WidgetGroup(0, 0, WIDTH, HEIGHT);
        var console = new OrbitalConsoleWidget(machine, 0, 0);
        page.addWidget(console);
        List<LithoMode> modes = machine.getModes();
        for (int i = 0; i < modes.size(); i++) {
            var tile = new Widget(tileX(i, modes.size()), TILE_Y, tileWidth(modes.size()), TILE_H);
            tile.setHoverTooltips(LithoConsoleWidget.tileTooltip(modes.get(i)));
            page.addWidget(tile);
        }
        page.addWidget(new SlotWidget(machine.reticleSlot.storage, 0, SLOT_X, SLOT_Y, true, true)
                .setBackgroundTexture(GuiTextures.SLOT)
                .setHoverTooltips(Component.translatable("af9.orbital.console.reticle_tooltip")));
        page.addWidget(new SlotWidget(machine.euvSlot.storage, 0, EUV_X, SLOT_Y, true, true)
                .setBackgroundTexture(GuiTextures.SLOT)
                .setHoverTooltips(Component.translatable("af9.orbital.console.euv_tooltip")));
        var power = new ButtonWidget(SWITCH_X, SWITCH_Y, SWITCH_W, SWITCH_H, IGuiTexture.EMPTY, click -> {
            if (!click.isRemote) machine.setWorkingEnabled(!machine.isWorkingEnabled());
        });
        power.setHoverTexture(new ColorBorderTexture(1, 0xFFFFFFFF));
        power.setHoverTooltips(Component.translatable("af9.orbital.console.switch_tooltip"));
        page.addWidget(power);
        var batch = new ButtonWidget(BATCH_X, SWITCH_Y, BATCH_W, SWITCH_H, IGuiTexture.EMPTY, click -> {
            if (!click.isRemote) machine.setBatchEnabled(!machine.isBatchEnabled());
        });
        batch.setHoverTexture(new ColorBorderTexture(1, 0xFFFFFFFF));
        batch.setHoverTooltips(Component.translatable("af9.orbital.console.batch_tooltip"));
        page.addWidget(batch);
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
        Coolant used = machine.currentCoolant(active);
        boolean euvNeeded = active.light.startsWith("euv");
        int[] now = {
                LithoConsoleWidget.statusOf(machine), active.ordinal(), machine.isFormed() ? machine.getTier() : -1,
                logic.isWorking() ? logic.getProgress() : 0, logic.isWorking() ? logic.getDuration() : 0,
                (int) Math.round(machine.getStartupPercent() * 10), machine.getVacuumState(),
                (int) Math.round(machine.getPrintVacuum() * 10),
                (int) Math.round(machine.currentBreakChance(active) * 10000), used == null ? 0 : used.grade(),
                machine.isWorkingEnabled() ? 1 : 0, machine.isBatchEnabled() ? 1 : 0,
                running == null ? 0 : LithoMachine.printsIn(running),
                !euvNeeded ? 0 : machine.euvSlot.getStackInSlot(0).isEmpty() ? 2 : 1,
                machine.isFieldActive() ? 1 : 0, machine.isInOrbit() ? 1 : 0,
                machine.reticleSlot.getStackInSlot(0).isEmpty() ? 0 : 1,
                active.computation() > 0 ? machine.availableComputation() : 0 };
        long newAvailable = machine.getAvailableEUt();
        String newProduct = current == null ? "" : current.toString();
        int[] before = { status, mode, tier, progress, duration, startup, vacuum, printVacuum, breakChance,
                coolant, workingEnabled ? 1 : 0, batchEnabled ? 1 : 0, batch, euv, field ? 1 : 0, orbit ? 1 : 0,
                reticle, compute };
        boolean changed = !java.util.Arrays.equals(now, before) || newAvailable != available ||
                machine.getPrinted() != printed || machine.getBroken() != broken ||
                !Objects.equals(newProduct, product);
        status = now[0];
        mode = now[1];
        tier = now[2];
        progress = now[3];
        duration = now[4];
        startup = now[5];
        vacuum = now[6];
        printVacuum = now[7];
        breakChance = now[8];
        coolant = now[9];
        workingEnabled = now[10] == 1;
        batchEnabled = now[11] == 1;
        batch = now[12];
        euv = now[13];
        field = now[14] == 1;
        orbit = now[15] == 1;
        reticle = now[16];
        compute = now[17];
        available = newAvailable;
        printed = machine.getPrinted();
        broken = machine.getBroken();
        product = newProduct;
        return changed;
    }

    @Override
    protected void writeState(FriendlyByteBuf buffer) {
        for (int value : new int[] { status, mode, tier, progress, duration, startup, vacuum, printVacuum,
                breakChance, coolant, batch, euv, reticle, compute }) {
            buffer.writeVarInt(value);
        }
        buffer.writeBoolean(workingEnabled);
        buffer.writeBoolean(batchEnabled);
        buffer.writeBoolean(field);
        buffer.writeBoolean(orbit);
        buffer.writeVarLong(available);
        buffer.writeVarLong(printed);
        buffer.writeVarLong(broken);
        buffer.writeUtf(product);
    }

    @Override
    protected void readState(FriendlyByteBuf buffer) {
        status = buffer.readVarInt();
        mode = buffer.readVarInt();
        tier = buffer.readVarInt();
        progress = buffer.readVarInt();
        duration = buffer.readVarInt();
        startup = buffer.readVarInt();
        vacuum = buffer.readVarInt();
        printVacuum = buffer.readVarInt();
        breakChance = buffer.readVarInt();
        coolant = buffer.readVarInt();
        batch = buffer.readVarInt();
        euv = buffer.readVarInt();
        reticle = buffer.readVarInt();
        compute = buffer.readVarInt();
        workingEnabled = buffer.readBoolean();
        batchEnabled = buffer.readBoolean();
        field = buffer.readBoolean();
        orbit = buffer.readBoolean();
        available = buffer.readVarLong();
        printed = buffer.readVarLong();
        broken = buffer.readVarLong();
        product = buffer.readUtf();
    }

    private LithoMode active() {
        return LithoMode.values()[Math.max(0, Math.min(mode, LithoMode.values().length - 1))];
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

    /** The exposure field: node tiles, the wafer under the beam, progress ring, state and run time. */
    @OnlyIn(Dist.CLIENT)
    private void drawField(GuiGraphics graphics, int x, int y, LithoMode active, float partialTicks) {
        Font font = font();
        graphics.fill(x, y, x + FIELD_W, y + FIELD_H, 0xFF070A11);
        border(graphics, x, y, FIELD_W, FIELD_H, EDGE);
        // faint grid
        for (int gx = x + 12; gx < x + FIELD_W; gx += 16) graphics.fill(gx, y + 1, gx + 1, y + FIELD_H - 1, 0x0CFFFFFF);
        for (int gy = y + 12; gy < y + FIELD_H; gy += 16) graphics.fill(x + 1, gy, x + FIELD_W - 1, gy + 1, 0x0CFFFFFF);

        List<LithoMode> modes = machine.getModes();
        for (int i = 0; i < modes.size(); i++) {
            LithoMode tileMode = modes.get(i);
            drawTile(graphics, getPosition().x + tileX(i, modes.size()), y + TILE_Y - FIELD_Y,
                    tileWidth(modes.size()), TILE_H, tileMode.nodeNm + "nm",
                    Component.translatable("af9.litho.substrate_short." + tileMode.substrate).getString(),
                    tileMode.argb, tileMode == active, status != STATUS_OFFLINE, false);
        }

        boolean running = status == STATUS_RUNNING;
        double fraction = duration <= 0 ? 0 : Math.min(1.0, (progress + (running ? partialTicks : 0)) / duration);
        int cx = getPosition().x + WAFER_X, cy = getPosition().y + WAFER_Y;
        long time = System.currentTimeMillis();
        int color = active.argb;

        // the wafer: a silicon disc with its flat, the die grid, exposed dies in the node's colour
        for (int dy = -WAFER_R; dy <= WAFER_R - 3; dy++) {
            int half = (int) Math.sqrt((double) WAFER_R * WAFER_R - dy * dy);
            int shade = 0x1A2233 + ((WAFER_R - dy) / 12) * 0x020202;
            graphics.fill(cx - half, cy + dy, cx + half, cy + dy + 1, 0xFF000000 | shade);
        }
        int exposed = running ? (int) (fraction * DIES.length) : 0;
        for (int i = 0; i < DIES.length; i++) {
            int dx = cx + DIES[i][0], dy = cy + DIES[i][1];
            int fill;
            if (i < exposed) fill = withAlpha(color, 0x70);
            else if (i == exposed && running) fill = withAlpha(color, 0x90 + (int) (0x60 * pulse(time, 300)));
            else fill = status == STATUS_OFFLINE ? 0x10FFFFFF : 0x18FFFFFF;
            graphics.fill(dx + 1, dy + 1, dx + DIE, dy + DIE, fill);
        }
        if (running && exposed < DIES.length) {
            // the scan slit crossing the die being exposed, and the beam from the optics above
            int dx = cx + DIES[exposed][0], dy = cy + DIES[exposed][1];
            double within = fraction * DIES.length - exposed;
            int slit = dy + 1 + (int) (within * (DIE - 1));
            graphics.fill(dx, slit, dx + DIE + 1, slit + 1, 0xFFFFFFFF);
            drawBeam(graphics, cx, y + TILE_Y + TILE_H + 2, dx + DIE / 2, dy + DIE / 2, color, time);
        }
        // progress ring
        int ring = WAFER_R + 6;
        int dots = 96;
        for (int i = 0; i < dots; i++) {
            double angle = -Math.PI / 2 + i * 2 * Math.PI / dots;
            int px = cx + (int) Math.round(Math.cos(angle) * ring), py = cy + (int) Math.round(Math.sin(angle) * ring);
            boolean lit = running && i < fraction * dots;
            graphics.fill(px - 1, py - 1, px + 1, py + 1, lit ? color : TRACK);
        }
        if (running) {
            // a glint running round the ring
            double angle = (time % 2000) / 2000.0 * 2 * Math.PI;
            int px = cx + (int) Math.round(Math.cos(angle) * ring), py = cy + (int) Math.round(Math.sin(angle) * ring);
            graphics.fill(px - 2, py - 2, px + 2, py + 2, withAlpha(0xFFFFFF, 0xC0));
        }

        // state and run time
        String state = running ?
                Component.translatable("af9.orbital.console.exposing").getString() + "  " +
                        Math.round(fraction * 100) + "%" :
                Component.translatable("af9.console.status." + status).getString();
        graphics.drawString(font, state, cx - font.width(state) / 2, y + 117, running ? color : statusColor(status),
                false);
        bar(graphics, x + 8, y + 128, FIELD_W - 16, 4, running ? fraction : 0, color);
        String timeText = running ? seconds(progress) + " / " + seconds(duration) :
                Component.translatable("af9.console.no_run").getString();
        drawSmall(graphics, timeText, cx, y + 133, running ? TEXT : MUTED, true);
    }

    /** A soft beam of the node's colour from the optics down to the die being exposed. */
    @OnlyIn(Dist.CLIENT)
    private static void drawBeam(GuiGraphics graphics, int fromX, int fromY, int toX, int toY, int color,
                                 long time) {
        graphics.fill(fromX - 6, fromY - 2, fromX + 6, fromY, withAlpha(color, 0xD0));
        int steps = Math.max(1, toY - fromY);
        for (int i = 0; i <= steps; i++) {
            float t = (float) i / steps;
            int bx = Math.round(Mth.lerp(t, fromX, toX)), by = Math.round(Mth.lerp(t, fromY, toY));
            int alpha = (int) (0x30 + 0x40 * pulse(time + i * 20L, 400));
            graphics.fill(bx - 1, by, bx + 2, by + 1, withAlpha(color, alpha));
            graphics.fill(bx, by, bx + 1, by + 1, withAlpha(0xFFFFFF, alpha));
        }
    }

    /** The exposure panel: node, product, reticle and EUV slots, switch, break chance, counters, hint. */
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

        drawSmall(graphics, Component.translatable("af9.orbital.console.printing").getString(), x + 5, y + 18, MUTED,
                false);
        ItemStack stack = productStack();
        if (!stack.isEmpty()) {
            graphics.renderItem(stack, x + 5, y + 25);
            graphics.drawString(font, fit(stack.getHoverName().getString(), PANEL_W - 30), x + 24, y + 29, TEXT, false);
        } else {
            graphics.drawString(font, Component.translatable("af9.litho.console.nothing").getString(), x + 24, y + 29,
                    MUTED, false);
        }

        // reticle slot (left) and EUV Light Source slot (right), each with its state beside it
        int stateWidth = (EUV_X - SLOT_X - 24) * 4 / 3;
        drawSmall(graphics, Component.translatable("af9.orbital.console.reticle").getString(), x0 + SLOT_X, y + 46,
                MUTED, false);
        drawSmall(graphics, fit(Component.translatable(reticle == 1 ? "af9.orbital.console.reticle_in" :
                "af9.orbital.console.reticle_empty").getString(), stateWidth), x0 + SLOT_X + 21, y0 + SLOT_Y + 6,
                reticle == 1 ? GOOD : BAD, false);
        drawSmall(graphics, Component.translatable("af9.orbital.console.euv").getString(), x0 + EUV_X, y + 46, MUTED,
                false);
        String euvKey = switch (euv) {
            case 1 -> "af9.orbital.console.euv_in";
            case 2 -> "af9.orbital.console.euv_missing";
            default -> "af9.orbital.console.euv_unused";
        };
        drawSmall(graphics, fit(Component.translatable(euvKey).getString(), (PANEL_X + PANEL_W - EUV_X - 25) * 4 / 3),
                x0 + EUV_X + 21, y0 + SLOT_Y + 6, euv == 1 ? GOOD : euv == 2 ? BAD : MUTED, false);

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
        String batchText = Component.translatable("af9.orbital.console.batch").getString();
        drawSmall(graphics, batchText, bx + BATCH_W / 2, sy + 5, batchEnabled ? INFO : MUTED, true);

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
        Component hint = Component.translatable("af9.orbital.hint." + status);
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

    /** 0 ... 1 ... 0 over the period. */
    private static float pulse(long time, int period) {
        return (float) (0.5 + 0.5 * Math.sin(time * 2 * Math.PI / period));
    }

    /** Die offsets from the wafer centre inside the disc (clear of the flat), in serpentine exposure order. */
    private static int[][] dieOrder() {
        List<int[]> dies = new ArrayList<>();
        int cells = WAFER_R / DIE + 1;
        for (int row = -cells; row < cells; row++) {
            List<int[]> line = new ArrayList<>();
            for (int col = -cells; col < cells; col++) {
                int dx = col * DIE, dy = row * DIE;
                boolean inside = true;
                for (int[] corner : new int[][] { { dx, dy }, { dx + DIE, dy }, { dx, dy + DIE },
                        { dx + DIE, dy + DIE } }) {
                    if (corner[0] * corner[0] + corner[1] * corner[1] > (WAFER_R - 1) * (WAFER_R - 1)) inside = false;
                }
                if (dy + DIE > WAFER_R - 3) inside = false;
                if (inside) line.add(new int[] { dx, dy });
            }
            if ((row & 1) == 1) java.util.Collections.reverse(line);
            dies.addAll(line);
        }
        return dies.toArray(new int[0][]);
    }

    //////////////////////////////////////
    // ******** Side panels *********//
    //////////////////////////////////////

    /**
     * A panel beside the player inventory, drawn from the console's synced state: left the process (start-up, coolant,
     * computation, magnetic field, orbit), right the system (status, power, tier, batch).
     */
    public static class SidePanel extends Widget {

        private final OrbitalConsoleWidget console;
        private final boolean system;

        public SidePanel(OrbitalConsoleWidget console, boolean system, int x, int y, int width, int height) {
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
            // start-up: off / starting (the seconds left) / ready; in orbit there is no vacuum to pump
            double percent = console.startup / 10.0;
            int state = console.status == STATUS_OFFLINE ? LithoMachine.VACUUM_OFF : console.vacuum;
            boolean ready = state == LithoMachine.VACUUM_SEALED;
            boolean starting = state == LithoMachine.VACUUM_PUMPING;
            int startColor = ready ? GOOD : starting ? INFO : MUTED;
            row(graphics, x, y, w, "af9.orbital.console.startup", ready ?
                    Component.translatable("af9.orbital.console.startup_ready").getString() :
                    starting ? Math.round(percent) + "%" :
                            Component.translatable("af9.orbital.console.startup_off").getString(), startColor);
            bar(graphics, x, y + 8, w, 3, percent / 100.0, startColor);
            int left = (int) Math.ceil(OrbitalLithographyMachine.STARTUP_SECONDS * (100 - percent) / 100.0);
            String startText = ready ? Component.translatable("af9.orbital.console.startup_online").getString() :
                    starting ? Component.translatable("af9.orbital.console.startup_left", left).getString() :
                            Component.translatable("af9.orbital.console.startup_waiting").getString();
            drawSmall(graphics, startText, x, y + 13, startColor, false);

            // coolant: its state on the row (needed / the minimum / grades colder), the fluid in full below it
            String coolantState;
            String coolantFluid = null;
            int coolantColor;
            if (active.minCoolant() == null) {
                coolantState = "-";
                coolantColor = MUTED;
            } else if (console.coolant == 0) {
                coolantState = Component.translatable("af9.orbital.console.coolant_needs").getString();
                coolantFluid = Component.translatable("af9.orbital.console.coolant_or_colder",
                        Component.translatable("af9.litho.coolant." + active.minCoolant().id)).getString();
                coolantColor = BAD;
            } else {
                Coolant used = Coolant.values()[console.coolant - 1];
                int steps = used.steps(active);
                coolantState = steps > 0 ?
                        Component.translatable("af9.orbital.console.coolant_colder", steps).getString() :
                        Component.translatable("af9.orbital.console.coolant_min").getString();
                coolantFluid = Component.translatable("af9.litho.coolant." + used.id).getString();
                coolantColor = steps > 0 ? GOOD : TEXT;
            }
            row(graphics, x, y + 22, w, "af9.litho.console.coolant", coolantState, coolantColor);
            if (coolantFluid != null) {
                drawSmall(graphics, fit(coolantFluid, w * 4 / 3), x, y + 29, coolantColor, false);
            }
            // computation: what the hatches' network supplies at most / what the node needs
            int needed = active.computation();
            row(graphics, x, y + 38, w, "af9.orbital.console.compute",
                    needed > 0 ? compact(console.compute) + "/" + needed + " CWU/t" : "-",
                    needed <= 0 ? MUTED : console.compute >= needed ? GOOD : BAD);
            row(graphics, x, y + 47, w, "af9.orbital.console.field",
                    Component.translatable(console.field ? "af9.orbital.console.field_on" :
                            "af9.orbital.console.field_off").getString(),
                    console.field ? GOOD : MUTED);
            row(graphics, x, y + 56, w, "af9.orbital.console.orbit",
                    Component.translatable(console.orbit ? "af9.orbital.console.orbit_yes" :
                            "af9.orbital.console.orbit_no").getString(),
                    console.orbit ? GOOD : BAD);
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
