package com.af9.core.machine.console;

import com.af9.core.elevator.ClimberRide;
import com.af9.core.elevator.PlanetCatalog;
import com.af9.core.elevator.SpaceElevatorMachine;
import com.af9.core.elevator.SpaceMissionMachine;

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
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.locale.Language;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * The Space Elevator's screen, the Orbital Lithography Station's layout (see {@link SidePanelsUIWidget}). The one
 * thing picked on it is the mission:
 * <ul>
 * <li>left, the ascent: the four Mining Drones as tiles (the one that flies is lit: the drone in the slot picks the
 * expedition), the tower on the ground, the cable up from it to orbit with its running light and the climber where its
 * ride has it, the asteroid of the run (a liquid mission: the planet) and the drones flying out to it and back with
 * its cargo, the state and the run-time bar;</li>
 * <li>right, the run: the drone; the mission selector, two rows of arrows: the target (the asteroids, or a planet type
 * for a liquid mission) and under it the planet's fluid with the buckets a mission brings (on an ore mission: the ore of
 * the run, in stacks); the drone slot (the drone stays in it), the on/off switch and the size switch (basic or extended
 * structure), the expeditions that fly of those the modules could, the counters, and a hint that says what the
 * elevator lacks while nothing flies;</li>
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
    /** The mission selector: the target row between its arrows, and the arrows of the fluid's row under it. */
    public static final int TARGET_Y = 19, TARGET_H = 10, CARGO_H = 16, ARROW_W = 9;
    public static final int ARROW_LEFT_X = PANEL_X + 5, ARROW_RIGHT_X = PANEL_X + PANEL_W - 5 - ARROW_W;
    /** The ore of the run (so many icons at most), the drone slot, the switches, the counter reset. */
    public static final int ORE_Y = 31, ORES = 6;
    public static final int SLOT_X = PANEL_X + 5, SLOT_Y = 53;
    public static final int SWITCH_X = PANEL_X + 5, SWITCH_Y = 76, SWITCH_W = 80, SWITCH_H = 16;
    public static final int SIZE_X = SWITCH_X + SWITCH_W + 4, SIZE_W = PANEL_W - 10 - SWITCH_W - 4;
    public static final int RESET_W = 30, RESET_H = 9, RESET_X = PANEL_X + PANEL_W - 5 - RESET_W, RESET_Y = 110;
    /** The Mining Drones there are, and their colours (as the Mining Modules' screens: blue, green, orange; pink). */
    public static final int DRONES = 4;
    private static final int[] TIER_COLORS = { 0xFF7DD3FC, 0xFF86EFAC, 0xFFFDBA74, 0xFFF0ABFC };
    /** A liquid mission's colour, and the planet types' (2 to 9) for a fluid that has no colour of its own. */
    private static final int LIQUID = 0xFF5EEAD4;
    private static final int[] PLANET_COLORS = { 0xFFB4623C, 0xFF7A8496, 0xFFC8A45A, 0xFFD9B38C, 0xFF8FB5D9, 0xFF6FA8A0,
            0xFF9C8FD9, 0xFF5EEAD4 };
    /** The scene: the ground, the tower on it, where orbit is, the asteroid. */
    private static final int GROUND_Y = 112, TOWER_X = FIELD_X + 44, TOWER_H = 22, ORBIT_Y = 40;
    private static final int ROCK_X = FIELD_X + 178, ROCK_Y = 66, ROCK_R = 13;
    /** Drones drawn on a run at most (a run can be of many more expeditions). */
    private static final int SHOWN_DRONES = 8;

    private final SpaceMissionMachine machine;

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
    /** The mission picked: a liquid one, and its planet type and gas type; the fluid of the run that is on (its code). */
    private boolean liquid;
    private int planet;
    private int gas;
    private int runCargo;
    private long available;
    private long needed;
    /** Millibuckets in the hatches, and what one expedition takes. */
    private long hydrogen;
    private long hydrogenNeed;
    private long coolant;
    private long coolantNeed;
    private long flown;
    private long mined;
    private long pumped;
    /** The fluid row's arrows: only there while a liquid mission is picked. */
    private Widget[] cargoArrows = new Widget[0];
    private String coolantFluid = "";
    private String asteroid = "";
    /** The ore of the run: "id*count" per item. */
    private String ore = "";

    public SpaceElevatorConsoleWidget(SpaceMissionMachine machine, int x, int y) {
        super(x, y, WIDTH, HEIGHT);
        this.machine = machine;
    }

    /**
     * The page: this console, the drone tiles' tooltips, the mission selector's arrows, the drone slot, the on/off
     * switch, the size switch and the counter reset. The slot works on the handler's storage: the handler refuses
     * inserts (no pipe access).
     */
    public static WidgetGroup createPage(SpaceMissionMachine machine) {
        var page = new WidgetGroup(0, 0, WIDTH, HEIGHT);
        var console = new SpaceElevatorConsoleWidget(machine, 0, 0);
        page.addWidget(console);
        // the mission: the target through the asteroids and the planet types, the fluid through the planet's fluids
        for (int step : new int[] { -1, 1 }) {
            page.addWidget(arrow(step < 0 ? ARROW_LEFT_X : ARROW_RIGHT_X, TARGET_Y, TARGET_H,
                    () -> machine.cycleTarget(step),
                    Component.translatable("af9.space_elevator.console.target_tooltip.0"),
                    Component.translatable("af9.space_elevator.console.target_tooltip.1")
                            .withStyle(ChatFormatting.GRAY),
                    Component.translatable("af9.space_elevator.console.target_tooltip.2")
                            .withStyle(ChatFormatting.DARK_GRAY)));
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

    /** An arrow of the mission selector: the console draws it, this takes its clicks. */
    private static ButtonWidget arrow(int x, int y, int height, Runnable action, Component... tooltips) {
        var arrow = new ButtonWidget(x, y, ARROW_W, height, IGuiTexture.EMPTY, click -> {
            if (!click.isRemote) action.run();
        });
        arrow.setHoverTexture(new ColorBorderTexture(1, 0xFFFFFFFF));
        arrow.setHoverTooltips(tooltips);
        return arrow;
    }

    /** What a drone's expedition takes and brings: its tile's tooltip. */
    private static List<Component> tileTooltip(SpaceMissionMachine machine, int tier) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable("af9.space_elevator.console.drone", SpaceElevatorMachine.mark(tier)));
        lines.add(Component.translatable("af9.space_elevator.console.tile.reach." + tier)
                .withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable("af9.space_elevator.console.tile.planets." + tier)
                .withStyle(ChatFormatting.GRAY));
        SpaceMissionMachine.Expedition needs = machine.expedition(tier);
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
                SpaceMissionMachine.minStacks(tier), SpaceMissionMachine.maxStacks(tier))
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
        SpaceMissionMachine.Expedition needs = machine.expedition(chosen);
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
                machine.isSkyClear() ? 1 : 0, machine.isLiquidMission() ? 1 : 0, machine.getPlanetType(),
                machine.getGasType(), machine.flyingCargo() };
        int[] before = { status, drone, motorTier, modules, powered, expeditions, topModule, flying, possible,
                progress, duration, tier, amps, volts, workingEnabled ? 1 : 0, extended ? 1 : 0, droneInSlot ? 1 : 0,
                skyClear ? 1 : 0, liquid ? 1 : 0, planet, gas, runCargo };
        long[] nowLong = { newAvailable, eut, hydrogenStock, needs == null ? 0 : needs.hydrogen(), coolantStock,
                needs == null ? 0 : needs.coolantAmount(), machine.getFlown(), machine.getMined(),
                machine.getPumped() };
        long[] beforeLong = { available, needed, hydrogen, hydrogenNeed, coolant, coolantNeed, flown, mined, pumped };
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
        liquid = now[18] == 1;
        planet = now[19];
        gas = now[20];
        runCargo = now[21];
        available = nowLong[0];
        needed = nowLong[1];
        hydrogen = nowLong[2];
        hydrogenNeed = nowLong[3];
        coolant = nowLong[4];
        coolantNeed = nowLong[5];
        flown = nowLong[6];
        mined = nowLong[7];
        pumped = nowLong[8];
        coolantFluid = newFluid;
        asteroid = newAsteroid;
        ore = newOre;
        return changed;
    }

    @Override
    protected void writeState(FriendlyByteBuf buffer) {
        for (int value : new int[] { status, drone, motorTier, modules, powered, expeditions, topModule, flying,
                possible, progress, duration, tier, amps, volts, planet, gas, runCargo }) {
            buffer.writeVarInt(value);
        }
        buffer.writeBoolean(workingEnabled);
        buffer.writeBoolean(extended);
        buffer.writeBoolean(droneInSlot);
        buffer.writeBoolean(skyClear);
        buffer.writeBoolean(liquid);
        for (long value : new long[] { available, needed, hydrogen, hydrogenNeed, coolant, coolantNeed, flown,
                mined, pumped }) {
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
        planet = buffer.readVarInt();
        gas = buffer.readVarInt();
        runCargo = buffer.readVarInt();
        workingEnabled = buffer.readBoolean();
        extended = buffer.readBoolean();
        droneInSlot = buffer.readBoolean();
        skyClear = buffer.readBoolean();
        liquid = buffer.readBoolean();
        available = buffer.readVarLong();
        needed = buffer.readVarLong();
        hydrogen = buffer.readVarLong();
        hydrogenNeed = buffer.readVarLong();
        coolant = buffer.readVarLong();
        coolantNeed = buffer.readVarLong();
        flown = buffer.readVarLong();
        mined = buffer.readVarLong();
        pumped = buffer.readVarLong();
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

    /**
     * The ascent: drone tiles, the tower, the cable and the climber, the asteroid (or the planet) and the drones,
     * state, run time.
     */
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

        // where the mission goes: the run's while one is on, else the one picked. A liquid mission: a planet in its
        // fluid's colour; else the asteroid, lit while a run goes to it, its ore glinting
        int ax = x0 + ROCK_X, ay = y0 + ROCK_Y;
        PlanetCatalog.Cargo there = running ? PlanetCatalog.byCode(runCargo) : cargo();
        boolean liquidRun = running && there != null;
        String rock;
        if (there != null) {
            int body = planetColor(there);
            for (int dy = -ROCK_R; dy <= ROCK_R; dy++) {
                int half = (int) Math.sqrt((double) ROCK_R * ROCK_R - dy * dy);
                int band = Math.floorMod(dy, 5) == 2 ? shade(body, 0.7F) : Math.floorMod(dy, 7) == 4 ?
                        shade(body, 1.2F) : body;
                graphics.fill(ax - half, ay + dy, ax + half + 1, ay + dy + 1, running ? band : shade(band, 0.4F));
            }
            rock = running ? fluidName(there.fluid()) :
                    Component.translatable("af9.space_elevator.console.target.planet", there.planet()).getString();
        } else {
            for (int dy = -ROCK_R; dy <= ROCK_R; dy++) {
                int half = (int) Math.sqrt((double) ROCK_R * ROCK_R - dy * dy);
                int left = half - Math.floorMod(dy * 37, 3), right = half - Math.floorMod(dy * 53, 4);
                graphics.fill(ax - left, ay + dy, ax + right + 1, ay + dy + 1, running ? 0xFF4A5261 : 0xFF202733);
            }
            int crater = running ? 0xFF353C49 : 0xFF181D26;
            graphics.fill(ax - 6, ay - 5, ax - 2, ay - 2, crater);
            graphics.fill(ax + 2, ay + 2, ax + 7, ay + 5, crater);
            graphics.fill(ax - 4, ay + 6, ax - 1, ay + 8, crater);
            rock = running ? asteroidName() : Component.translatable("af9.space_elevator.console.no_target")
                    .getString();
        }
        drawSmall(graphics, fit(rock, 144), ax, ay + ROCK_R + 5, running ? TEXT : DIM, true);

        if (running) {
            for (int i = 0; i < 5 && !liquidRun; i++) {
                if ((now / 250 + i * 2L) % 5 != 0) continue;
                int gx = ax - 7 + Math.floorMod(i * 41, 15), gy = ay - 7 + Math.floorMod(i * 29, 15);
                graphics.fill(gx, gy, gx + 1, gy + 1, 0xFFFFE08A);
            }
            // the drones: out to the asteroid in the first part of the run, at it, home with the cargo in the last
            double way = fraction < 0.45 ? fraction / 0.45 : fraction > 0.55 ? (1 - fraction) / 0.45 : 1;
            boolean home = fraction > 0.55;
            int shown = Math.min(Math.max(flying, 1), SHOWN_DRONES);
            for (int i = 0; i < shown; i++) {
                // in a loose flight: each a little behind and beside the one before
                double s = Mth.clamp(way - i * 0.025, 0, 1);
                double px = Mth.lerp(s, tx + 5, ax - ROCK_R - 3);
                double py = Mth.lerp(s, orbit, ay) - Math.sin(Math.PI * s) * 9 + (i - (shown - 1) / 2.0) * 3 * s;
                int dx = (int) Math.round(px), dy = (int) Math.round(py);
                int body = !home ? color : liquidRun ? LIQUID : 0xFFFFE08A;
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

    /**
     * The run: the drone, the mission (its target, and its fluid or the run's ore), the drone slot, the switches, the
     * flights, the counters, the hint.
     */
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

        // the mission: its target between the arrows
        int ty = y0 + TARGET_Y;
        drawArrow(graphics, x0 + ARROW_LEFT_X, ty, TARGET_H, false);
        drawArrow(graphics, x0 + ARROW_RIGHT_X, ty, TARGET_H, true);
        String target = liquid ?
                Component.translatable("af9.space_elevator.console.target.planet", planet).getString() :
                Component.translatable("af9.space_elevator.console.target.asteroids").getString();
        drawSmall(graphics, target, x + PANEL_W / 2, ty + 2, liquid ? LIQUID : TEXT, true);

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
        // the ore brought home, or on a liquid mission the fluid
        String counters = liquid ?
                Component.translatable("af9.space_elevator.console.pumped", compact(pumped / 1000)).getString() :
                Component.translatable("af9.space_elevator.console.mined", compact(mined / 64)).getString();
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
            case STATUS_RUNNING -> runCargo != 0 ? Component.translatable("af9.space_elevator.hint.2.liquid") :
                    Component.translatable("af9.space_elevator.hint.2");
            // a drone is there, but the planet picked lies beyond it
            case STATUS_NO_DRONE -> liquid && drone > 0 ?
                    Component.translatable("af9.space_elevator.hint.18.reach", planet,
                            SpaceElevatorMachine.mark(PlanetCatalog.droneFor(planet))) :
                    Component.translatable("af9.space_elevator.hint.18");
            case STATUS_OUTPUT_FULL -> liquid ? Component.translatable("af9.space_elevator.hint.20.liquid") :
                    Component.translatable("af9.space_elevator.hint.20");
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

    /** The fluid picked for the liquid missions (the client knows the table too), null on an ore mission. */
    private PlanetCatalog.Cargo cargo() {
        return liquid ? PlanetCatalog.find(planet, gas) : null;
    }

    /** An arrow of the mission selector. */
    @OnlyIn(Dist.CLIENT)
    private static void drawArrow(GuiGraphics graphics, int x, int y, int height, boolean right) {
        graphics.fill(x, y, x + ARROW_W, y + height, 0xFF0B0F17);
        border(graphics, x, y, ARROW_W, height, DIM);
        int middle = y + height / 2;
        for (int i = 0; i < 3; i++) {
            int column = right ? x + 3 + i : x + ARROW_W - 4 - i;
            graphics.fill(column, middle - 2 + i, column + 1, middle + 3 - i, TEXT);
        }
    }

    /** A fluid as a tank shows it: its still texture in its colour. */
    @OnlyIn(Dist.CLIENT)
    private static void drawFluid(GuiGraphics graphics, Fluid fluid, int x, int y) {
        graphics.fill(x - 1, y - 1, x + 17, y + 17, 0xFF0B0F17);
        IClientFluidTypeExtensions look = IClientFluidTypeExtensions.of(fluid);
        ResourceLocation still = look.getStillTexture();
        int tint = look.getTintColor();
        if (still == null) {
            graphics.fill(x, y, x + 16, y + 16, tint | 0xFF000000);
            return;
        }
        TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(still);
        graphics.setColor((tint >> 16 & 0xFF) / 255F, (tint >> 8 & 0xFF) / 255F, (tint & 0xFF) / 255F, 1F);
        graphics.blit(x, y, 0, 16, 16, sprite);
        graphics.setColor(1F, 1F, 1F, 1F);
    }

    /** A planet's colour: its fluid's, or the planet type's when the fluid has none (a texture in its own colours). */
    @OnlyIn(Dist.CLIENT)
    private static int planetColor(PlanetCatalog.Cargo cargo) {
        int tint = IClientFluidTypeExtensions.of(cargo.fluid()).getTintColor() | 0xFF000000;
        if ((tint & 0xFFFFFF) != 0xFFFFFF) return tint;
        return PLANET_COLORS[Mth.clamp(cargo.planet() - 2, 0, PLANET_COLORS.length - 1)];
    }

    /** A colour, darker (below 1) or lighter. */
    private static int shade(int argb, float factor) {
        int r = Mth.clamp(Math.round((argb >> 16 & 0xFF) * factor), 0, 255);
        int g = Mth.clamp(Math.round((argb >> 8 & 0xFF) * factor), 0, 255);
        int b = Mth.clamp(Math.round((argb & 0xFF) * factor), 0, 255);
        return argb & 0xFF000000 | r << 16 | g << 8 | b;
    }

    @OnlyIn(Dist.CLIENT)
    private static String fluidName(Fluid fluid) {
        return new FluidStack(fluid, 1).getDisplayName().getString();
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
