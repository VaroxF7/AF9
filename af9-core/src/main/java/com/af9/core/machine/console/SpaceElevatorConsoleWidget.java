package com.af9.core.machine.console;

import com.af9.core.elevator.ClimberRide;
import com.af9.core.elevator.SpaceElevatorMachine;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.gui.widget.SlotWidget;
import com.gregtechceu.gtceu.common.data.GTMaterials;
import com.gregtechceu.gtceu.utils.FormattingUtil;
import com.gregtechceu.gtceu.utils.GTUtil;

import com.lowdragmc.lowdraglib.gui.texture.ColorBorderTexture;
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.widget.ButtonWidget;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;

import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.locale.Language;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * The Space Elevator's screen, the Orbital Lithography Station's layout (see {@link SidePanelsUIWidget}), with nothing
 * on it to configure:
 * <ul>
 * <li>left, the ascent: the four Mining Drones as tiles (the one that flies is lit: the drone in the slot picks the
 * expedition), the tower on the ground, the cable up from it to orbit with its running light and the climber where its
 * ride has it, the asteroid of the run and the drones flying out to it and back with its ore, the state and the run-time
 * bar;</li>
 * <li>right, the run: the drone, the asteroid and its ore in stacks, the drone slot (the drone stays in it), the on/off
 * switch and the size switch (basic or extended structure), the expeditions that fly of those the modules could, the
 * counters, and a hint that says what the elevator lacks while nothing flies;</li>
 * <li>beside the player inventory ({@link SidePanel}): left the process (motors, modules, flights, hydrogen, coolant,
 * the sky above the cable), right the system (status, power, tier, size, switch).</li>
 * </ul>
 * The server samples the machine every tick and sends the state only when something changed.
 */
public class SpaceElevatorConsoleWidget extends ConsoleWidget {

    public static final int WIDTH = 384;
    public static final int HEIGHT = 148;
    /** Left field and right panel of the page. */
    public static final int FIELD_X = 4, FIELD_Y = 4, FIELD_W = 240, FIELD_H = 140;
    public static final int PANEL_X = 250, PANEL_Y = 4, PANEL_W = 130, PANEL_H = 140;
    public static final int TILE_Y = 8, TILE_H = 20;
    /** The ore of the run (so many icons at most), the drone slot, the switches, the counter reset. */
    public static final int ORE_Y = 31, ORES = 6;
    public static final int SLOT_X = PANEL_X + 5, SLOT_Y = 53;
    public static final int SWITCH_X = PANEL_X + 5, SWITCH_Y = 76, SWITCH_W = 80, SWITCH_H = 16;
    public static final int SIZE_X = SWITCH_X + SWITCH_W + 4, SIZE_W = PANEL_W - 10 - SWITCH_W - 4;
    public static final int RESET_W = 30, RESET_H = 9, RESET_X = PANEL_X + PANEL_W - 5 - RESET_W, RESET_Y = 110;
    /** The Mining Drones there are, and their colours (as the Mining Modules' screens: blue, green, orange; pink). */
    public static final int DRONES = 4;
    private static final int[] TIER_COLORS = { 0xFF7DD3FC, 0xFF86EFAC, 0xFFFDBA74, 0xFFF0ABFC };
    /** The scene: the ground, the tower on it, where orbit is, the asteroid. */
    private static final int GROUND_Y = 112, TOWER_X = FIELD_X + 44, TOWER_H = 22, ORBIT_Y = 40;
    private static final int ROCK_X = FIELD_X + 178, ROCK_Y = 66, ROCK_R = 13;
    /** Drones drawn on a run at most (a run can be of many more expeditions). */
    private static final int SHOWN_DRONES = 8;

    private final SpaceElevatorMachine machine;

    // last state sent to / received by the client
    private int status = -1;
    /** The drone that flies (1 to 4), 0 none. */
    private int drone;
    private int motorTier;
    private int modules;
    private int powered;
    private int expeditions;
    private int topModule;
    /** Expeditions of the run that is on; those a run could be of now. */
    private int flying;
    private int possible;
    private int progress;
    private int duration;
    private int tier;
    /** One expedition's energy: amps of this voltage tier. */
    private int amps;
    private int volts;
    private boolean workingEnabled;
    private boolean extended;
    private boolean droneInSlot;
    private boolean skyClear;
    private long available;
    private long needed;
    /** Millibuckets in the hatches, and what one expedition takes. */
    private long hydrogen;
    private long hydrogenNeed;
    private long coolant;
    private long coolantNeed;
    private long flown;
    private long mined;
    private String coolantFluid = "";
    private String asteroid = "";
    /** The ore of the run: "id*count" per item. */
    private String ore = "";

    public SpaceElevatorConsoleWidget(SpaceElevatorMachine machine, int x, int y) {
        super(x, y, WIDTH, HEIGHT);
        this.machine = machine;
    }

    /**
     * The page: this console, the drone tiles' tooltips, the drone slot, the on/off switch, the size switch and the
     * counter reset. The slot works on the handler's storage: the handler refuses inserts (no pipe access).
     */
    public static WidgetGroup createPage(SpaceElevatorMachine machine) {
        var page = new WidgetGroup(0, 0, WIDTH, HEIGHT);
        page.addWidget(new SpaceElevatorConsoleWidget(machine, 0, 0));
        for (int i = 0; i < DRONES; i++) {
            var tile = new Widget(tileX(i), TILE_Y, tileWidth(), TILE_H);
            tile.setHoverTooltips(tileTooltip(machine, i + 1));
            page.addWidget(tile);
        }
        page.addWidget(new SlotWidget(machine.droneSlot.storage, 0, SLOT_X, SLOT_Y, true, true)
                .setBackgroundTexture(GuiTextures.SLOT)
                .setHoverTooltips(Component.translatable("af9.space_elevator.console.drone_tooltip.0"),
                        Component.translatable("af9.space_elevator.console.drone_tooltip.1")
                                .withStyle(ChatFormatting.GRAY)));
        var power = new ButtonWidget(SWITCH_X, SWITCH_Y, SWITCH_W, SWITCH_H, IGuiTexture.EMPTY, click -> {
            if (!click.isRemote) machine.setWorkingEnabled(!machine.isWorkingEnabled());
        });
        power.setHoverTexture(new ColorBorderTexture(1, 0xFFFFFFFF));
        power.setHoverTooltips(Component.translatable("af9.space_elevator.console.switch_tooltip.0"),
                Component.translatable("af9.space_elevator.console.switch_tooltip.1").withStyle(ChatFormatting.GRAY));
        page.addWidget(power);
        var size = new ButtonWidget(SIZE_X, SWITCH_Y, SIZE_W, SWITCH_H, IGuiTexture.EMPTY, click -> {
            if (!click.isRemote) machine.setExtended(!machine.isExtended());
        });
        size.setHoverTexture(new ColorBorderTexture(1, 0xFFFFFFFF));
        size.setHoverTooltips(Component.translatable("af9.space_elevator.console.size_tooltip.0"),
                Component.translatable("af9.space_elevator.console.size_tooltip.1").withStyle(ChatFormatting.GRAY),
                Component.translatable("af9.space_elevator.console.size_tooltip.2").withStyle(ChatFormatting.GRAY),
                Component.translatable("af9.space_elevator.console.size_tooltip.3")
                        .withStyle(ChatFormatting.DARK_GRAY));
        page.addWidget(size);
        var reset = new ButtonWidget(RESET_X, RESET_Y, RESET_W, RESET_H, IGuiTexture.EMPTY, click -> {
            if (!click.isRemote) machine.resetCounters();
        });
        reset.setHoverTexture(new ColorBorderTexture(1, 0xFFFFFFFF));
        reset.setHoverTooltips(Component.translatable("af9.space_elevator.console.reset_tooltip"));
        page.addWidget(reset);
        return page;
    }

    /** What a drone's expedition takes and brings: its tile's tooltip. */
    private static List<Component> tileTooltip(SpaceElevatorMachine machine, int tier) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable("af9.space_elevator.console.drone", SpaceElevatorMachine.mark(tier)));
        lines.add(Component.translatable("af9.space_elevator.console.tile.reach." + tier)
                .withStyle(ChatFormatting.GRAY));
        SpaceElevatorMachine.Expedition needs = machine.expedition(tier);
        if (needs != null) {
            lines.add(Component.translatable("af9.space_elevator.console.tile.hydrogen", needs.hydrogen() / 1000)
                    .withStyle(ChatFormatting.AQUA));
            lines.add(Component.translatable("af9.space_elevator.console.tile.coolant",
                    needs.coolantAmount() / 1000, new FluidStack(needs.coolant(), 1).getDisplayName())
                    .withStyle(ChatFormatting.AQUA));
            lines.add(Component.translatable("af9.space_elevator.console.tile.energy", needs.amperage(),
                    GTValues.VN[voltageTier(needs.voltage())], FormattingUtil.formatNumbers(needs.eut()))
                    .withStyle(ChatFormatting.YELLOW));
            lines.add(Component.translatable("af9.space_elevator.console.tile.time", needs.duration() / 1200)
                    .withStyle(ChatFormatting.GRAY));
        }
        lines.add(Component.translatable("af9.space_elevator.console.tile.stacks",
                SpaceElevatorMachine.minStacks(tier), SpaceElevatorMachine.maxStacks(tier))
                .withStyle(ChatFormatting.GREEN));
        lines.add(Component.translatable("af9.space_elevator.console.tile.pick").withStyle(ChatFormatting.DARK_GRAY));
        return lines;
    }

    private static int voltageTier(long voltage) {
        return Mth.clamp(GTUtil.getTierByVoltage(voltage), 0, GTValues.VN.length - 1);
    }

    public static int tileWidth() {
        return (FIELD_W - 12 - 3 * (DRONES - 1)) / DRONES;
    }

    public static int tileX(int index) {
        return FIELD_X + 6 + index * (tileWidth() + 3);
    }

    /** The colour of a drone tier; muted without a drone. */
    public static int tierColor(int tier) {
        return tier < 1 || tier > TIER_COLORS.length ? MUTED : TIER_COLORS[tier - 1];
    }

    //////////////////////////////////////
    // *********** Sync ***********//
    //////////////////////////////////////

    @Override
    protected boolean sample() {
        var logic = machine.getRecipeLogic();
        boolean formed = machine.isFormed();
        int chosen = machine.chosenDrone();
        SpaceElevatorMachine.Expedition needs = machine.expedition(chosen);
        long eut = needs == null ? 0 : needs.eut();
        long newAvailable = machine.getAvailableEUt();
        long hydrogenStock = machine.stockOf(GTMaterials.Hydrogen.getFluid());
        Fluid coolantKind = needs == null ? Fluids.EMPTY : needs.coolant();
        long coolantStock = needs == null ? 0 : machine.stockOf(coolantKind);
        // the expeditions a run could be of now: what the modules fly, the energy and the fluids allow
        long could = needs == null ? 0 : Math.min(machine.getExpeditions(), eut <= 0 ? 0 : newAvailable / eut);
        if (needs != null && needs.hydrogen() > 0) could = Math.min(could, hydrogenStock / needs.hydrogen());
        if (needs != null && needs.coolantAmount() > 0) could = Math.min(could, coolantStock / needs.coolantAmount());
        int[] now = { machine.getStatus(), chosen, machine.getMotorTier(), machine.getModules(),
                machine.getPoweredModules(), machine.getExpeditions(), machine.getTopModule(),
                machine.flyingExpeditions(), (int) could, logic.isWorking() ? logic.getProgress() : 0,
                logic.isWorking() ? logic.getDuration() : 0, formed ? machine.getTier() : -1,
                needs == null ? 0 : (int) needs.amperage(), needs == null ? 0 : voltageTier(needs.voltage()),
                machine.isWorkingEnabled() ? 1 : 0, machine.isExtended() ? 1 : 0, machine.isDroneInSlot() ? 1 : 0,
                machine.isSkyClear() ? 1 : 0 };
        int[] before = { status, drone, motorTier, modules, powered, expeditions, topModule, flying, possible,
                progress, duration, tier, amps, volts, workingEnabled ? 1 : 0, extended ? 1 : 0, droneInSlot ? 1 : 0,
                skyClear ? 1 : 0 };
        long[] nowLong = { newAvailable, eut, hydrogenStock, needs == null ? 0 : needs.hydrogen(), coolantStock,
                needs == null ? 0 : needs.coolantAmount(), machine.getFlown(), machine.getMined() };
        long[] beforeLong = { available, needed, hydrogen, hydrogenNeed, coolant, coolantNeed, flown, mined };
        ResourceLocation fluidId = coolantKind == Fluids.EMPTY ? null : ForgeRegistries.FLUIDS.getKey(coolantKind);
        String newFluid = fluidId == null ? "" : fluidId.toString();
        String newAsteroid = machine.flyingAsteroid();
        String newOre = machine.flyingOre();
        boolean changed = !Arrays.equals(now, before) || !Arrays.equals(nowLong, beforeLong) ||
                !Objects.equals(newFluid, coolantFluid) || !Objects.equals(newAsteroid, asteroid) ||
                !Objects.equals(newOre, ore);
        status = now[0];
        drone = now[1];
        motorTier = now[2];
        modules = now[3];
        powered = now[4];
        expeditions = now[5];
        topModule = now[6];
        flying = now[7];
        possible = now[8];
        progress = now[9];
        duration = now[10];
        tier = now[11];
        amps = now[12];
        volts = now[13];
        workingEnabled = now[14] == 1;
        extended = now[15] == 1;
        droneInSlot = now[16] == 1;
        skyClear = now[17] == 1;
        available = nowLong[0];
        needed = nowLong[1];
        hydrogen = nowLong[2];
        hydrogenNeed = nowLong[3];
        coolant = nowLong[4];
        coolantNeed = nowLong[5];
        flown = nowLong[6];
        mined = nowLong[7];
        coolantFluid = newFluid;
        asteroid = newAsteroid;
        ore = newOre;
        return changed;
    }

    @Override
    protected void writeState(FriendlyByteBuf buffer) {
        for (int value : new int[] { status, drone, motorTier, modules, powered, expeditions, topModule, flying,
                possible, progress, duration, tier, amps, volts }) {
            buffer.writeVarInt(value);
        }
        buffer.writeBoolean(workingEnabled);
        buffer.writeBoolean(extended);
        buffer.writeBoolean(droneInSlot);
        buffer.writeBoolean(skyClear);
        for (long value : new long[] { available, needed, hydrogen, hydrogenNeed, coolant, coolantNeed, flown,
                mined }) {
            buffer.writeVarLong(value);
        }
        buffer.writeUtf(coolantFluid);
        buffer.writeUtf(asteroid);
        buffer.writeUtf(ore);
    }

    @Override
    protected void readState(FriendlyByteBuf buffer) {
        status = buffer.readVarInt();
        drone = buffer.readVarInt();
        motorTier = buffer.readVarInt();
        modules = buffer.readVarInt();
        powered = buffer.readVarInt();
        expeditions = buffer.readVarInt();
        topModule = buffer.readVarInt();
        flying = buffer.readVarInt();
        possible = buffer.readVarInt();
        progress = buffer.readVarInt();
        duration = buffer.readVarInt();
        tier = buffer.readVarInt();
        amps = buffer.readVarInt();
        volts = buffer.readVarInt();
        workingEnabled = buffer.readBoolean();
        extended = buffer.readBoolean();
        droneInSlot = buffer.readBoolean();
        skyClear = buffer.readBoolean();
        available = buffer.readVarLong();
        needed = buffer.readVarLong();
        hydrogen = buffer.readVarLong();
        hydrogenNeed = buffer.readVarLong();
        coolant = buffer.readVarLong();
        coolantNeed = buffer.readVarLong();
        flown = buffer.readVarLong();
        mined = buffer.readVarLong();
        coolantFluid = buffer.readUtf();
        asteroid = buffer.readUtf();
        ore = buffer.readUtf();
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
        drawField(graphics, x0 + FIELD_X, y0 + FIELD_Y, partialTicks);
        drawPanel(graphics, x0 + PANEL_X, y0 + PANEL_Y);
        super.drawInBackground(graphics, mouseX, mouseY, partialTicks);
    }

    /** The ascent: drone tiles, the tower, the cable and the climber, the asteroid and the drones, state, run time. */
    @OnlyIn(Dist.CLIENT)
    private void drawField(GuiGraphics graphics, int x, int y, float partialTicks) {
        Font font = font();
        int x0 = getPosition().x;
        int y0 = getPosition().y;
        long now = Util.getMillis();
        boolean running = status == STATUS_RUNNING;
        boolean formed = status != STATUS_OFFLINE;
        int color = tierColor(drone);
        double fraction = duration <= 0 ? 0 : Math.min(1.0, (progress + (running ? partialTicks : 0)) / duration);
        graphics.fill(x, y, x + FIELD_W, y + FIELD_H, 0xFF070A11);
        border(graphics, x, y, FIELD_W, FIELD_H, EDGE);

        // the sky: stars, a few of them twinkling
        for (int i = 0; i < 46; i++) {
            int sx = x + 3 + Math.floorMod(i * 7919 + 13, FIELD_W - 6);
            int sy = y + 28 + Math.floorMod(i * 104729 + 71, GROUND_Y - FIELD_Y - 34);
            boolean twinkle = (now / 400 + i * 3L) % 11 == 0;
            graphics.fill(sx, sy, sx + 1, sy + 1, twinkle ? 0xFFFFFFFF : i % 3 == 0 ? 0x66FFFFFF : 0x33FFFFFF);
        }

        for (int i = 0; i < DRONES; i++) {
            drawTile(graphics, x0 + tileX(i), y + TILE_Y - FIELD_Y, tileWidth(), TILE_H,
                    SpaceElevatorMachine.mark(i + 1),
                    Component.translatable("af9.space_elevator.console.reach." + (i + 1)).getString(),
                    tierColor(i + 1), drone == i + 1, formed, false);
        }

        // the ground, and the tower on it: a frame that tapers to the cable
        int ground = y0 + GROUND_Y, tx = x0 + TOWER_X, top = ground - TOWER_H, orbit = y0 + ORBIT_Y;
        graphics.fill(x + 1, ground, x + FIELD_W - 1, ground + 1, EDGE);
        graphics.fill(x + 1, ground + 1, x + FIELD_W - 1, ground + 4, 0xFF0D131E);
        for (int row = 0; row < TOWER_H; row++) {
            int half = 12 - row * 9 / TOWER_H;
            int body = !formed ? 0xFF1A2233 : row % 5 == 2 ? 0xFF2B2E34 : 0xFF1F5FA8;
            graphics.fill(tx - half, ground - 1 - row, tx + half + 1, ground - row, body);
        }
        // orbit, where the climber turns round and the drones leave from
        for (int ox = x + 4; ox < x + FIELD_W - 4; ox += 4) graphics.fill(ox, orbit, ox + 2, orbit + 1, 0x30FFFFFF);
        drawSmall(graphics, Component.translatable("af9.space_elevator.console.orbit").getString(),
                x + FIELD_W - 30, orbit - 7, DIM, false);

        if (formed) {
            // the cable, the light that runs up it every three seconds, and the climber where its ride has it
            int cableTop = y + 27;
            graphics.fill(tx, cableTop, tx + 1, top, 0xFF4B5567);
            int light = top - (int) ((now % 3000) / 3000.0 * (top - cableTop));
            graphics.fill(tx, Math.max(cableTop, light - 3), tx + 1, light, 0xFF00A6FF);
            float height = Mth.clamp(machine.climberHeight(partialTicks) / ClimberRide.ORBIT, 0F, 1F);
            int rest = top - 8;
            int cy = rest - Math.round(height * (rest - orbit));
            graphics.fill(tx - 6, cy, tx + 7, cy + 1, 0xFFD2A542);
            graphics.fill(tx - 1, cy - 1, tx + 2, cy + 2, 0xFFBFC5CC);
            graphics.fill(tx - 7, cy - 3, tx - 5, cy, 0xFFF3F5F7);
            graphics.fill(tx + 6, cy - 3, tx + 8, cy, 0xFF2AA0C6);
        }

        // the asteroid: lit while a run goes to it, its ore glinting
        int ax = x0 + ROCK_X, ay = y0 + ROCK_Y;
        for (int dy = -ROCK_R; dy <= ROCK_R; dy++) {
            int half = (int) Math.sqrt((double) ROCK_R * ROCK_R - dy * dy);
            int left = half - Math.floorMod(dy * 37, 3), right = half - Math.floorMod(dy * 53, 4);
            graphics.fill(ax - left, ay + dy, ax + right + 1, ay + dy + 1, running ? 0xFF4A5261 : 0xFF202733);
        }
        int crater = running ? 0xFF353C49 : 0xFF181D26;
        graphics.fill(ax - 6, ay - 5, ax - 2, ay - 2, crater);
        graphics.fill(ax + 2, ay + 2, ax + 7, ay + 5, crater);
        graphics.fill(ax - 4, ay + 6, ax - 1, ay + 8, crater);
        String rock = running ? asteroidName() : Component.translatable("af9.space_elevator.console.no_target")
                .getString();
        drawSmall(graphics, fit(rock, 144), ax, ay + ROCK_R + 5, running ? TEXT : DIM, true);

        if (running) {
            for (int i = 0; i < 5; i++) {
                if ((now / 250 + i * 2L) % 5 != 0) continue;
                int gx = ax - 7 + Math.floorMod(i * 41, 15), gy = ay - 7 + Math.floorMod(i * 29, 15);
                graphics.fill(gx, gy, gx + 1, gy + 1, 0xFFFFE08A);
            }
            // the drones: out to the asteroid in the first part of the run, at it, home with the ore in the last
            double way = fraction < 0.45 ? fraction / 0.45 : fraction > 0.55 ? (1 - fraction) / 0.45 : 1;
            boolean home = fraction > 0.55;
            int shown = Math.min(Math.max(flying, 1), SHOWN_DRONES);
            for (int i = 0; i < shown; i++) {
                // in a loose flight: each a little behind and beside the one before
                double s = Mth.clamp(way - i * 0.025, 0, 1);
                double px = Mth.lerp(s, tx + 5, ax - ROCK_R - 3);
                double py = Mth.lerp(s, orbit, ay) - Math.sin(Math.PI * s) * 9 + (i - (shown - 1) / 2.0) * 3 * s;
                int dx = (int) Math.round(px), dy = (int) Math.round(py);
                int body = home ? 0xFFFFE08A : color;
                // a short trail behind it: it flies right on the way out, left on the way home
                if (s < 1) {
                    int trail = home ? dx + 2 : dx - 3;
                    graphics.fill(trail, dy, trail + 3, dy + 1, withAlpha(body, 0x70));
                }
                graphics.fill(dx, dy - 1, dx + 2, dy + 1, s >= 1 && (now / 150 + i) % 2 == 0 ? 0xFFFFFFFF : body);
            }
        }

        // state and run time
        int cx = x + FIELD_W / 2;
        String state = running ?
                Component.translatable("af9.space_elevator.console.flying", flying).getString() + "  " +
                        Math.round(fraction * 100) + "%" :
                Component.translatable("af9.console.status." + status).getString();
        graphics.drawString(font, state, cx - font.width(state) / 2, y + 117, running ? color : statusColor(status),
                false);
        bar(graphics, x + 8, y + 128, FIELD_W - 16, 4, running ? fraction : 0, color);
        String timeText = running ? seconds(progress) + " / " + seconds(duration) :
                Component.translatable("af9.console.no_run").getString();
        drawSmall(graphics, timeText, cx, y + 133, running ? TEXT : MUTED, true);
    }

    /** The run: drone, asteroid and ore, the drone slot, the switches, the flights, the counters, the hint. */
    @OnlyIn(Dist.CLIENT)
    private void drawPanel(GuiGraphics graphics, int x, int y) {
        Font font = font();
        int x0 = getPosition().x;
        int y0 = getPosition().y;
        boolean running = status == STATUS_RUNNING;
        int color = tierColor(drone);
        graphics.fill(x, y, x + PANEL_W, y + PANEL_H, PANEL);
        border(graphics, x, y, PANEL_W, PANEL_H, EDGE);
        graphics.fill(x + 1, y + 1, x + PANEL_W - 1, y + 2, withAlpha(color, 0xAA));

        // the drone that flies, and how far it reaches
        String title = drone > 0 ?
                Component.translatable("af9.space_elevator.console.drone", SpaceElevatorMachine.mark(drone))
                        .getString() :
                Component.translatable("af9.space_elevator.console.no_drone").getString();
        graphics.drawString(font, fit(title, PANEL_W - 10), x + 5, y + 5, color, false);

        // the asteroid of the run and its ore, in stacks
        drawSmall(graphics, Component.translatable("af9.space_elevator.console.asteroid").getString(), x + 5,
                y + 18, MUTED, false);
        List<ItemStack> ores = stacks(ore);
        if (!running || ores.isEmpty()) {
            drawSmall(graphics, Component.translatable("af9.space_elevator.console.nothing").getString(), x + 5,
                    y0 + ORE_Y + 5, DIM, false);
        } else {
            String name = fit(asteroidName(), (PANEL_W - 52) * 4 / 3);
            drawSmall(graphics, name, x + PANEL_W - 5 - font.width(name) * 3 / 4, y + 18, TEXT, false);
            for (int i = 0; i < Math.min(ORES, ores.size()); i++) {
                drawOre(graphics, font, ores.get(i), x + 5 + i * 20, y0 + ORE_Y);
            }
        }

        // beside the drone slot: where the drone is, and the expeditions that fly of those the modules could
        int rx = x0 + SLOT_X + 23, rw = PANEL_X + PANEL_W - 5 - (SLOT_X + 23);
        String where = Component.translatable(droneInSlot ? "af9.space_elevator.console.drone_slot" :
                drone > 0 ? "af9.space_elevator.console.drone_bus" : "af9.space_elevator.console.drone_none")
                .getString();
        row(graphics, rx, y0 + SLOT_Y + 2, rw, "af9.space_elevator.console.drone_label", where,
                droneInSlot ? GOOD : drone > 0 ? INFO : BAD);
        int flights = running ? flying : possible;
        row(graphics, rx, y0 + SLOT_Y + 11, rw, "af9.space_elevator.console.flights", flights + " / " + expeditions,
                flights <= 0 ? BAD : flights < expeditions ? WARN : GOOD);

        // the on/off switch
        int sx = x0 + SWITCH_X, sy = y0 + SWITCH_Y;
        graphics.fill(sx, sy, sx + SWITCH_W, sy + SWITCH_H, workingEnabled ? withAlpha(GOOD, 0x28) : 0xFF0B0F17);
        border(graphics, sx, sy, SWITCH_W, SWITCH_H, workingEnabled ? GOOD : DIM);
        graphics.fill(sx + 5, sy + 5, sx + 11, sy + 11, workingEnabled ? GOOD : DIM);
        String switchText = Component.translatable(workingEnabled ? "af9.orbital.console.online" :
                "af9.orbital.console.offline").getString();
        graphics.drawString(font, switchText, sx + 7 + (SWITCH_W - 7 - font.width(switchText)) / 2, sy + 4,
                workingEnabled ? GOOD : MUTED, false);
        // the size switch: the structure the elevator is checked for
        int bx = x0 + SIZE_X;
        graphics.fill(bx, sy, bx + SIZE_W, sy + SWITCH_H, extended ? withAlpha(INFO, 0x28) : 0xFF0B0F17);
        border(graphics, bx, sy, SIZE_W, SWITCH_H, extended ? INFO : DIM);
        drawSmall(graphics, extended ? "47x47" : "35x35", bx + SIZE_W / 2, sy + 5, extended ? INFO : MUTED, true);

        // the counters
        drawSmall(graphics, Component.translatable("af9.space_elevator.console.flown").getString(), x + 5, y + 97,
                MUTED, false);
        String flownText = compact(flown);
        graphics.drawString(font, flownText, x + PANEL_W - 5 - font.width(flownText), y + 95, TEXT, false);
        String counters = Component.translatable("af9.space_elevator.console.mined", compact(mined / 64))
                .getString();
        drawSmall(graphics, fit(counters, (x0 + RESET_X - 3 - (x + 5)) * 4 / 3), x + 5, y0 + RESET_Y + 2, MUTED,
                false);
        drawButton(graphics, x0 + RESET_X, y0 + RESET_Y, RESET_W, RESET_H,
                Component.translatable("af9.litho.console.reset").getString());

        // what to do now
        int line = 0;
        for (FormattedCharSequence part : font.split(hint(), (PANEL_W - 10) * 4 / 3)) {
            if (line >= 3) break;
            graphics.pose().pushPose();
            graphics.pose().translate(x + 5, y + 119 + line * 7, 0);
            graphics.pose().scale(0.75F, 0.75F, 1F);
            graphics.drawString(font, part, 0, 0, status == STATUS_RUNNING ? MUTED : statusColor(status), false);
            graphics.pose().popPose();
            line++;
        }
    }

    /** What the elevator lacks, with the numbers of the drone that would fly. */
    @OnlyIn(Dist.CLIENT)
    private Component hint() {
        return switch (status) {
            case STATUS_NO_POWER -> Component.translatable("af9.space_elevator.hint.3",
                    FormattingUtil.formatNumbers(needed), amps,
                    GTValues.VN[Mth.clamp(volts, 0, GTValues.VN.length - 1)], FormattingUtil.formatNumbers(available));
            case STATUS_NO_COOLANT -> Component.translatable("af9.space_elevator.hint.8", coolantNeed / 1000,
                    coolantName());
            case STATUS_NO_MODULE -> modules > 0 && topModule > motorTier ?
                    Component.translatable("af9.space_elevator.hint.17.motors", SpaceElevatorMachine.mark(topModule),
                            SpaceElevatorMachine.mark(topModule), SpaceElevatorMachine.mark(motorTier)) :
                    Component.translatable("af9.space_elevator.hint.17");
            case STATUS_NO_FUEL -> Component.translatable("af9.space_elevator.hint.19", hydrogenNeed / 1000);
            default -> Component.translatable("af9.space_elevator.hint." + status);
        };
    }

    /** An ore of the run with its stacks on it. */
    @OnlyIn(Dist.CLIENT)
    private static void drawOre(GuiGraphics graphics, Font font, ItemStack stack, int x, int y) {
        graphics.fill(x - 1, y - 1, x + 17, y + 17, 0xFF0B0F17);
        graphics.renderItem(stack, x, y);
        graphics.renderItemDecorations(font, stack, x, y, compact(Math.max(1, Math.round(stack.getCount() / 64F))));
    }

    /** "id*count;id*count" back to stacks. */
    @OnlyIn(Dist.CLIENT)
    private static List<ItemStack> stacks(String encoded) {
        List<ItemStack> stacks = new ArrayList<>();
        if (encoded.isEmpty()) return stacks;
        for (String part : encoded.split(";")) {
            int star = part.lastIndexOf('*');
            ResourceLocation id = ResourceLocation.tryParse(star < 0 ? part : part.substring(0, star));
            Item item = id == null ? null : ForgeRegistries.ITEMS.getValue(id);
            if (item == null || item == Items.AIR) continue;
            int count = 1;
            try {
                if (star >= 0) count = Integer.parseInt(part.substring(star + 1));
            } catch (NumberFormatException exception) {
                // a single one then
            }
            stacks.add(new ItemStack(item, count));
        }
        return stacks;
    }

    /**
     * The asteroid by a name: GT's name of the vein it is made from (its recipe viewer page's), else the vein's id put
     * in words; the exotic asteroid has its own.
     */
    @OnlyIn(Dist.CLIENT)
    private String asteroidName() {
        if (asteroid.isEmpty()) return "-";
        if (asteroid.equals("exotic")) {
            return Component.translatable("af9.space_elevator.console.exotic").getString();
        }
        ResourceLocation id = ResourceLocation.tryParse(asteroid);
        String path = id == null ? asteroid : id.getPath();
        String key = "gtceu.jei.ore_vein." + path;
        if (Language.getInstance().has(key)) return Language.getInstance().getOrDefault(key);
        StringBuilder words = new StringBuilder();
        for (String word : path.split("_")) {
            if (word.isEmpty()) continue;
            if (words.length() > 0) words.append(' ');
            words.append(word.substring(0, 1).toUpperCase(Locale.ROOT)).append(word.substring(1));
        }
        return words.toString();
    }

    @OnlyIn(Dist.CLIENT)
    private String coolantName() {
        ResourceLocation id = coolantFluid.isEmpty() ? null : ResourceLocation.tryParse(coolantFluid);
        Fluid fluid = id == null ? null : ForgeRegistries.FLUIDS.getValue(id);
        return fluid == null || fluid == Fluids.EMPTY ? "" : new FluidStack(fluid, 1).getDisplayName().getString();
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
     * A panel beside the player inventory, drawn from the console's synced state: left the process (motors, modules,
     * flights, hydrogen, coolant, the sky above the cable), right the system (status, power, tier, size, switch).
     */
    public static class SidePanel extends Widget {

        private final SpaceElevatorConsoleWidget console;
        private final boolean system;

        public SidePanel(SpaceElevatorConsoleWidget console, boolean system, int x, int y, int width, int height) {
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
            graphics.fill(x + 1, y + 1, x + w - 1, y + 2, withAlpha(tierColor(console.drone), 0xAA));
            drawSmall(graphics, Component.translatable(system ? "af9.orbital.console.system" :
                    "af9.orbital.console.process").getString(), x + 4, y + 5, TEXT, false);
            if (system) drawSystem(graphics, x + 4, y + 16, w - 8);
            else drawProcess(graphics, x + 4, y + 16, w - 8);
            super.drawInBackground(graphics, mouseX, mouseY, partialTicks);
        }

        @OnlyIn(Dist.CLIENT)
        private void drawProcess(GuiGraphics graphics, int x, int y, int w) {
            SpaceElevatorConsoleWidget c = console;
            boolean formed = c.status != STATUS_OFFLINE;
            // the motors' tier and the module slots it powers
            row(graphics, x, y, w, "af9.space_elevator.console.motors", !formed || c.motorTier < 1 ? "-" :
                    SpaceElevatorMachine.mark(c.motorTier) + " (" + SpaceElevatorMachine.moduleSlots(c.motorTier) +
                            ")",
                    formed ? TEXT : MUTED);
            // modules the motors power, of those in the slots
            row(graphics, x, y + 9, w, "af9.space_elevator.console.modules", formed ? c.powered + "/" + c.modules :
                    "-", !formed ? MUTED : c.powered <= 0 ? BAD : c.powered < c.modules ? WARN : GOOD);
            row(graphics, x, y + 18, w, "af9.space_elevator.console.flights", formed ?
                    Integer.toString(c.expeditions) : "-", !formed ? MUTED : c.expeditions > 0 ? TEXT : BAD);
            // hydrogen and the coolant: what the hatches hold / what an expedition takes, in buckets
            row(graphics, x, y + 27, w, "af9.space_elevator.console.hydrogen", buckets(c.hydrogen, c.hydrogenNeed),
                    fluidColor(c.hydrogen, c.hydrogenNeed, formed));
            row(graphics, x, y + 36, w, "af9.console.coolant", buckets(c.coolant, c.coolantNeed),
                    fluidColor(c.coolant, c.coolantNeed, formed));
            String fluid = c.coolantName();
            if (!fluid.isEmpty()) drawSmall(graphics, fit(fluid, w * 4 / 3), x, y + 43, MUTED, false);
            row(graphics, x, y + 52, w, "af9.space_elevator.console.sky",
                    Component.translatable(c.skyClear ? "af9.space_elevator.console.sky_clear" :
                            "af9.space_elevator.console.sky_blocked").getString(),
                    !formed ? MUTED : c.skyClear ? GOOD : BAD);
        }

        @OnlyIn(Dist.CLIENT)
        private void drawSystem(GuiGraphics graphics, int x, int y, int w) {
            SpaceElevatorConsoleWidget c = console;
            row(graphics, x, y, w, "af9.orbital.console.status",
                    Component.translatable("af9.console.status." + c.status).getString(), statusColor(c.status));
            // what the hatches supply / what one expedition takes
            long needed = c.needed;
            boolean enough = needed <= 0 || c.available >= needed;
            row(graphics, x, y + 10, w, "af9.console.power",
                    compact(c.available) + "/" + (needed > 0 ? compact(needed) : "-"), enough ? TEXT : BAD);
            bar(graphics, x, y + 18, w, 3, needed <= 0 ? (c.available > 0 ? 1 : 0) :
                    Math.min(1.0, (double) c.available / needed), enough ? GOOD : BAD);
            String each = needed <= 0 ? "EU/t" : Component.translatable("af9.space_elevator.console.each", c.amps,
                    GTValues.VN[Mth.clamp(c.volts, 0, GTValues.VN.length - 1)]).getString();
            drawSmall(graphics, fit(each, w * 4 / 3), x, y + 23, MUTED, false);
            row(graphics, x, y + 33, w, "af9.orbital.console.tier",
                    c.tier < 0 ? "-" : GTValues.VNF[Math.min(c.tier, GTValues.VNF.length - 1)], TEXT);
            row(graphics, x, y + 43, w, "af9.space_elevator.console.size", c.extended ? "47 x 47" : "35 x 35",
                    c.extended ? INFO : TEXT);
            row(graphics, x, y + 53, w, "af9.orbital.console.switch",
                    Component.translatable(c.workingEnabled ? "af9.orbital.console.online" :
                            "af9.orbital.console.offline").getString(),
                    c.workingEnabled ? GOOD : MUTED);
        }

        /** "640/64 B": buckets in the hatches, and those one expedition takes (when a drone says how many). */
        private static String buckets(long have, long need) {
            return compact(have / 1000) + (need > 0 ? "/" + compact(need / 1000) : "") + " B";
        }

        private static int fluidColor(long have, long need, boolean formed) {
            return !formed || need <= 0 ? MUTED : have >= need ? GOOD : BAD;
        }
    }
}
