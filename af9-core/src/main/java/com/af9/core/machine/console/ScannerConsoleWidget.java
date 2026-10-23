package com.af9.core.machine.console;

import com.af9.core.litho.LithoMode;
import com.af9.core.machine.LithoConsoleWidget;
import com.af9.core.machine.LithoMachine;
import com.af9.core.machine.PhotolithographyLineMachine;
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
import java.util.Locale;
import java.util.Objects;

/**
 * The screen of the Photolithography Line (Mk1) and Scanner (Mk2) alike (see {@link ScannerUIWidget} for the frame around
 * it), the orbital station's layout. The two machines differ only in what is physical: the scanner has its ArF laser in
 * a slot of this screen, the line's light source is a block of its structure.
 * <ul>
 * <li>left, the exposure field: the node the machine is set to (GT's mode button on the left of the screen switches it),
 * the wafer from above in the middle, exposed die by die in the node's colour, the state and the run-time bar;</li>
 * <li>right, the exposure panel: the node and its light, what is printing on which substrate, the ArF laser slot
 * (kept, never used up), the version's speed and break bonus, the on/off switch and a hint for the current state (GT
 * adds the batch button on the left of the screen);</li>
 * <li>beside the player inventory ({@link SidePanel}): left the process (vacuum, version, immersion, laser), right the
 * system (status, power, energy hatch tier, batch, switch).</li>
 * </ul>
 * The server samples the machine every tick and sends the state only when something changed.
 */
public class ScannerConsoleWidget extends ConsoleWidget {

    public static final int WIDTH = 384;
    public static final int HEIGHT = 148;
    /** Left field and right panel of the page. */
    public static final int FIELD_X = 4, FIELD_Y = 4, FIELD_W = 240, FIELD_H = 140;
    public static final int PANEL_X = 250, PANEL_Y = 4, PANEL_W = 130, PANEL_H = 140;
    /** The ArF laser slot. */
    public static final int SLOT_X = PANEL_X + 5, SLOT_Y = 58;
    public static final int SWITCH_X = PANEL_X + 5, SWITCH_Y = 86, SWITCH_W = PANEL_W - 10, SWITCH_H = 14;
    /** The wafer (centre) in the field: its progress ring stays between the node's name and the state text. */
    private static final int WAFER_X = FIELD_X + FIELD_W / 2, WAFER_Y = FIELD_Y + 70;
    private static final WaferView WAFER = new WaferView(28, 8);
    /** Structure length of version 1 (the line adds 1 block a version, the scanner 2: the spec's aisles per slice). */
    public static final int LENGTH_V1 = 10;

    private final PhotolithographyLineMachine machine;

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
    /** The line's light source: the highest version it allows (1 mercury lamp, 2 KrF, 3 ArF), 0 where none is built. */
    private int lightCap;
    /** The pumps found no energy in the hatches. */
    private boolean starved;
    /** Air cooling (units given / heat load, doublings above it, lapsed), OPC percent (-1: none). */
    private int coolCapacity;
    private int coolLoad;
    private int coolSteps;
    private boolean coolLapsed;
    private int opc = -1;
    /** The shown mode's prints are multi-patterned (exposed twice, one version above the scanner's own). */
    private boolean multi;
    private String product = "";

    public ScannerConsoleWidget(PhotolithographyLineMachine machine, int x, int y) {
        super(x, y, WIDTH, HEIGHT);
        this.machine = machine;
    }

    /**
     * The page: this console, the laser slot and the on/off switch. The slot works on the handler's storage: the handler
     * refuses inserts (no pipe access).
     */
    public static WidgetGroup createPage(PhotolithographyLineMachine machine) {
        var page = new WidgetGroup(0, 0, WIDTH, HEIGHT);
        page.addWidget(new ScannerConsoleWidget(machine, 0, 0));
        if (machine instanceof PhotolithographyScannerMachine scanner) {
            page.addWidget(new SlotWidget(scanner.laserSlot.storage, 0, SLOT_X, SLOT_Y, true, true)
                    .setBackgroundTexture(GuiTextures.SLOT)
                    .setHoverTooltips(Component.translatable("af9.scanner.console.laser_tooltip")));
        }
        var power = new ButtonWidget(SWITCH_X, SWITCH_Y, SWITCH_W, SWITCH_H, IGuiTexture.EMPTY, click -> {
            if (!click.isRemote) machine.setWorkingEnabled(!machine.isWorkingEnabled());
        });
        power.setHoverTexture(new ColorBorderTexture(1, 0xFFFFFFFF));
        power.setHoverTooltips(Component.translatable(machine instanceof PhotolithographyScannerMachine ?
                "af9.scanner.console.switch_tooltip" : "af9.line.console.switch_tooltip"));
        page.addWidget(power);
        return page;
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
                machine.getInputTier(),
                logic.isWorking() ? logic.getProgress() : 0, logic.isWorking() ? logic.getDuration() : 0,
                (int) Math.round(machine.getCleanliness() * 10), (int) Math.round(machine.getPrintVacuum() * 10),
                machine.getVacuumState(), (int) Math.round(machine.currentBreakChance(active) * 10000),
                machine.isWorkingEnabled() ? 1 : 0, machine.isBatchEnabled() ? 1 : 0,
                running == null ? 0 : LithoMachine.printsIn(running), lightReady() ? 1 : 0,
                machine.needsAirCooling(active) ? machine.getCoolingCapacity() : 0,
                machine.needsAirCooling(active) ? active.heatLoad() : 0, machine.coolingSteps(active),
                machine.hasCoolingLapsed() && logic.isWorking() ? 1 : 0,
                opcPercent(machine.getOpcRatio(active, logic.isWorking())),
                machine.isMultiPatterned(active) ? 1 : 0, machine.getSpec().hasLightBlocks() ? machine.getLightCap() : 0,
                machine.pumpsStarved() ? 1 : 0 };
        int[] before = { status, mode, version, tier, progress, duration, cleanliness, printVacuum, vacuum,
                breakChance, workingEnabled ? 1 : 0, batchEnabled ? 1 : 0, batch, laser ? 1 : 0, coolCapacity,
                coolLoad, coolSteps, coolLapsed ? 1 : 0, opc, multi ? 1 : 0, lightCap, starved ? 1 : 0 };
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
        coolCapacity = now[14];
        coolLoad = now[15];
        coolSteps = now[16];
        coolLapsed = now[17] == 1;
        opc = now[18];
        multi = now[19] == 1;
        lightCap = now[20];
        starved = now[21] == 1;
        available = newAvailable;
        printed = machine.getPrinted();
        broken = machine.getBroken();
        product = newProduct;
        return changed;
    }

    /** The scanner's laser is in its slot; the line's light source is a block of its structure, so it is there when formed. */
    private boolean lightReady() {
        return machine instanceof PhotolithographyScannerMachine scanner ? scanner.hasLaser() : machine.isFormed();
    }

    private boolean hasLaserSlot() {
        return machine instanceof PhotolithographyScannerMachine;
    }

    private static int opcPercent(double ratio) {
        return ratio < 0 ? -1 : (int) Math.round(ratio * 100);
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
        buffer.writeVarInt(coolCapacity);
        buffer.writeVarInt(coolLoad);
        buffer.writeVarInt(coolSteps);
        buffer.writeBoolean(coolLapsed);
        buffer.writeVarInt(opc + 1);
        buffer.writeBoolean(multi);
        buffer.writeVarInt(lightCap);
        buffer.writeBoolean(starved);
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
        coolCapacity = buffer.readVarInt();
        coolLoad = buffer.readVarInt();
        coolSteps = buffer.readVarInt();
        coolLapsed = buffer.readBoolean();
        opc = buffer.readVarInt() - 1;
        multi = buffer.readBoolean();
        lightCap = buffer.readVarInt();
        starved = buffer.readBoolean();
        available = buffer.readVarLong();
        printed = buffer.readVarLong();
        broken = buffer.readVarLong();
        product = buffer.readUtf();
    }

    private LithoMode active() {
        return LithoMode.values()[Math.max(0, Math.min(mode, LithoMode.values().length - 1))];
    }

    /** Lens slices of the version: the machine's first version's count, one more per version. */
    private int slices(int version) {
        return machine.getSpec().lensSlicesV1() + Math.max(0, version - 1);
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

    /** The exposure field: the node, the wafer in the middle, state and run time. */
    @OnlyIn(Dist.CLIENT)
    private void drawField(GuiGraphics graphics, int x, int y, LithoMode active, float partialTicks) {
        Font font = font();
        int x0 = getPosition().x;
        int y0 = getPosition().y;
        graphics.fill(x, y, x + FIELD_W, y + FIELD_H, 0xFF070A11);
        border(graphics, x, y, FIELD_W, FIELD_H, EDGE);
        for (int gx = x + 12; gx < x + FIELD_W; gx += 16) graphics.fill(gx, y + 1, gx + 1, y + FIELD_H - 1, 0x0CFFFFFF);
        for (int gy = y + 12; gy < y + FIELD_H; gy += 16) graphics.fill(x + 1, gy, x + FIELD_W - 1, gy + 1, 0x0CFFFFFF);

        // the node the machine is set to, big in the node's colour, or the version a locked node needs
        boolean locked = active.level() > version;
        String node = active.nodeNm + " NM";
        graphics.pose().pushPose();
        graphics.pose().translate(x + FIELD_W / 2F, y + 6, 0);
        graphics.pose().scale(2F, 2F, 1F);
        graphics.drawString(font, node, -font.width(node) / 2, 0, locked ? BAD : active.argb, false);
        graphics.pose().popPose();
        String detail = locked ? Component.translatable("af9.scanner.console.needs", active.level()).getString() :
                Component.translatable("af9.litho.substrate." + active.substrate).getString() + "  " +
                        Component.translatable("af9.litho.light." + active.light).getString();
        drawSmall(graphics, fit(detail, FIELD_W * 4 / 3 - 16), x + FIELD_W / 2, y + 24, locked ? BAD : MUTED, true);

        boolean running = status == STATUS_RUNNING;
        double fraction = duration <= 0 ? 0 : Math.min(1.0, (progress + (running ? partialTicks : 0)) / duration);
        long time = System.currentTimeMillis();
        int color = active.argb;

        // the wafer from above in the middle, exposed die by die; the beam from the top of the field to the die
        int cx = x0 + WAFER_X, cy = y0 + WAFER_Y;
        int[] die = WAFER.draw(graphics, cx, cy, fraction, running, status == STATUS_OFFLINE, color, time);
        if (die != null) WaferView.drawBeam(graphics, cx, y + 33, die[0], die[1], color, time);

        // state and run time
        int midX = x + FIELD_W / 2;
        String state = running ?
                Component.translatable("af9.scanner.console.exposing").getString() + "  " +
                        Math.round(fraction * 100) + "%" :
                Component.translatable("af9.console.status." + status).getString();
        graphics.drawString(font, state, midX - font.width(state) / 2, y + 112, running ? color : statusColor(status),
                false);
        bar(graphics, x + 8, y + 124, FIELD_W - 16, 4, running ? fraction : 0, color);
        String timeText = running ? seconds(progress) + " / " + seconds(duration) :
                Component.translatable("af9.console.no_run").getString();
        drawSmall(graphics, timeText, midX, y + 130, running ? TEXT : MUTED, true);
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
        drawSmall(graphics, Component.translatable(hasLaserSlot() ? "af9.scanner.console.laser" :
                "af9.litho.console.light").getString(), x0 + SLOT_X, y + 46, MUTED, false);
        int lightX = hasLaserSlot() ? 21 : 0;
        int bx0 = x + 70;
        drawSmall(graphics, fit(lightText(), (bx0 - (x0 + SLOT_X) - lightX - 3) * 4 / 3), x0 + SLOT_X + lightX,
                y0 + SLOT_Y + 6, lightColor(active), false);
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
        // what to do now (up to 3 lines), then the air conditioning and the OPC the computation gave (up to 2)
        String hintKey = "af9.scanner.hint." + status;
        if (status == STATUS_NO_POWER && starved) hintKey = "af9.scanner.hint.3p";
        else if (status == STATUS_LOCKED && !hasLaserSlot()) hintKey = "af9.line.hint.6";
        Component hint = Component.translatableWithFallback(hintKey,
                Component.translatable("af9.console.status." + status).getString());
        int line = 0;
        for (FormattedCharSequence part : font.split(hint, (PANEL_W - 10) * 4 / 3)) {
            if (line >= 3) break;
            graphics.pose().pushPose();
            graphics.pose().translate(x + 5, y + 101 + line * 7, 0);
            graphics.pose().scale(0.75F, 0.75F, 1F);
            graphics.drawString(font, part, 0, 0, status == STATUS_RUNNING ? MUTED : statusColor(status), false);
            graphics.pose().popPose();
            line++;
        }
        String quality = (multi ? Component.translatable("af9.litho.console.multipatterned").getString() + "  " : "") +
                (coolLoad > 0 ? (coolLapsed ? Component.translatable("af9.litho.console.cooling_lapsed")
                .getString() : Component.translatable("af9.litho.console.cooling_short", coolCapacity, coolLoad)
                .getString()) + "  " : "") +
                (opc >= 0 ? Component.translatable("af9.litho.console.opc", opc).getString() : "");
        boolean coolBad = coolLoad > 0 && (coolLapsed || coolCapacity < coolLoad);
        line = 0;
        for (FormattedCharSequence part : font.split(Component.literal(quality), (PANEL_W - 10) * 4 / 3)) {
            if (line >= 2) break;
            graphics.pose().pushPose();
            graphics.pose().translate(x + 5, y + 125 + line * 7, 0);
            graphics.pose().scale(0.75F, 0.75F, 1F);
            graphics.drawString(font, part, 0, 0, coolBad ? BAD : MUTED, false);
            graphics.pose().popPose();
            line++;
        }
    }

    /** The scanner's laser (in its slot or missing) or the line's light source block (the one built into it). */
    private String lightText() {
        if (hasLaserSlot()) {
            return Component.translatable(laser ? "af9.scanner.console.laser_in" :
                    "af9.scanner.console.laser_missing").getString();
        }
        return lightCap > 0 ? Component.translatable("af9.litho.console.light_source." + Math.min(lightCap, 3))
                .getString() : "-";
    }

    /** Green when the light source can expose the node (the line's: its cap reaches the node's version). */
    private int lightColor(LithoMode active) {
        if (hasLaserSlot()) return laser ? GOOD : BAD;
        if (lightCap <= 0) return MUTED;
        return lightCap >= active.level() - (multi ? 1 : 0) ? GOOD : BAD;
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
            boolean dipped = console.status == STATUS_RUNNING && low < 99.95;
            if (dipped) {
                pump = Component.translatable("af9.litho.console.print_vacuum",
                        String.format(Locale.ROOT, "%.1f", low)).getString();
            }
            drawSmall(graphics, fit(pump, w * 4 / 3), x, y + 13, dipped ? BAD :
                    LithoConsoleWidget.vacuumColor(pumpState), false);
            // the version: the structure's length and its lens slices (the scanner's window sections)
            int version = console.version;
            row(graphics, x, y + 23, w, "af9.scanner.console.version", version > 0 ? "V" + version : "-",
                    version > 0 ? TEXT : MUTED);
            if (version > 0) {
                drawSmall(graphics, fit(Component.translatable(console.hasLaserSlot() ?
                        "af9.scanner.console.length" : "af9.litho.console.length_lens",
                        LENGTH_V1 + (version - 1) * console.machine.getSpec().aislesPerSlice(),
                        console.slices(version)).getString(), w * 4 / 3), x, y + 30, MUTED, false);
            }
            int next = y + 40;
            // immersion: only the scanner's 65 nm prints are exposed through water
            if (console.hasLaserSlot()) {
                row(graphics, x, next, w, "af9.scanner.console.immersion",
                        Component.translatable(active.immersion() ? "af9.scanner.console.immersion_on" :
                                "af9.scanner.console.immersion_off").getString(),
                        active.immersion() ? INFO : MUTED);
                next += 9;
            }
            row(graphics, x, next, w, console.hasLaserSlot() ? "af9.scanner.console.laser" :
                    "af9.litho.console.light", console.lightText(), console.lightColor(active));
        }

        @OnlyIn(Dist.CLIENT)
        private void drawSystem(GuiGraphics graphics, int x, int y, int w, LithoMode active) {
            row(graphics, x, y, w, "af9.orbital.console.status",
                    Component.translatable("af9.console.status." + console.status).getString(),
                    statusColor(console.status));
            // power: what the energy hatches supply against what the node's prints draw, both in EU/t
            long needed = active.eut();
            boolean enough = console.available >= needed && !console.starved;
            row(graphics, x, y + 10, w, "af9.console.power",
                    compact(console.available) + "/" + compact(needed), enough ? TEXT : BAD);
            bar(graphics, x, y + 18, w, 3, needed <= 0 ? 0 : Math.min(1.0, (double) console.available / needed),
                    enough ? GOOD : BAD);
            // the node's need; the pumps are what runs without a print, so say when the hatches hold no energy
            drawSmall(graphics, console.starved ?
                    Component.translatable("af9.litho.console.hatches_empty").getString() :
                    Component.translatable("af9.litho.console.needs_power", active.amperage(),
                            GTValues.VN[active.hatchTier]).getString(), x, y + 23, console.starved ? BAD : MUTED,
                    false);
            // the voltage tier of the energy hatches (the controller's own tier says nothing)
            boolean tierOk = console.tier >= active.hatchTier;
            row(graphics, x, y + 33, w, "af9.litho.console.hatch_tier",
                    console.tier < 0 ? "-" : GTValues.VNF[Math.min(console.tier, GTValues.VNF.length - 1)],
                    console.tier < 0 ? MUTED : tierOk ? TEXT : BAD);
            // batch mode: the switch, and the prints in the running batch
            String batchText = !console.batchEnabled ?
                    Component.translatable("af9.orbital.console.batch_off").getString() :
                    Component.translatable("af9.orbital.console.batch_on").getString() +
                            (console.batch > 1 ? " " + console.batch + "x" : "");
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
