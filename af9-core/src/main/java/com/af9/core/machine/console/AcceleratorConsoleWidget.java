package com.af9.core.machine.console;

import com.af9.core.machine.ParticleAcceleratorMachine;
import com.af9.core.machine.ProcessMachine;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.recipe.GTRecipeType;

import com.lowdragmc.lowdraglib.gui.texture.ColorBorderTexture;
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.widget.ButtonWidget;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;

import net.minecraft.Util;
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
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

/**
 * The Particle Accelerator's screen, the Orbital Lithography Station's layout (see {@link SidePanelsUIWidget}):
 * <ul>
 * <li>left, the ring seen from above: the three modes as tiles (click one to switch), the beam pipe with its four
 * gates, the particle bunches racing round it ever faster as the run goes on, a progress ring, the state and the
 * run-time bar. Neutron irradiation: one bunch, a neutron spray off the target at the south gate on every lap;
 * heavy-ion collision: two bunches against each other, a flash where they meet; quark synthesis: the same, and the
 * condensate growing in the middle, its quarks circling it;</li>
 * <li>right, the run: the mode, the beam energy, the recipe's items (in, out), energy per run, the on/off switch, the
 * run counter and a hint for the current state;</li>
 * <li>beside the player inventory ({@link SidePanel}): left the process (coolant, magnets, beam, RF, mode), right the
 * system (status, power, tier, switch, runs).</li>
 * </ul>
 * The server samples the machine every tick and sends the state only when something changed.
 */
public class AcceleratorConsoleWidget extends ConsoleWidget {

    public static final int WIDTH = 384;
    public static final int HEIGHT = 148;
    /** Left field and right panel of the page. */
    public static final int FIELD_X = 4, FIELD_Y = 4, FIELD_W = 240, FIELD_H = 140;
    public static final int PANEL_X = 250, PANEL_Y = 4, PANEL_W = 130, PANEL_H = 140;
    public static final int TILE_Y = 8, TILE_H = 20;
    public static final int SWITCH_X = PANEL_X + 5, SWITCH_Y = 80, SWITCH_W = PANEL_W - 10, SWITCH_H = 16;
    public static final int RESET_W = 30, RESET_H = 9, RESET_X = PANEL_X + PANEL_W - 5 - RESET_W, RESET_Y = 110;
    /** The ring: centre, beam pipe radius, progress ring radius. */
    private static final int RING_X = FIELD_X + FIELD_W / 2, RING_Y = 78, BEAM_R = 33, PROGRESS_R = 40;

    private final ParticleAcceleratorMachine machine;

    // last state sent to / received by the client
    private int status = -1;
    private int mode;
    private int tier;
    private int progress;
    private int duration;
    private int beam;
    private boolean workingEnabled;
    private long available;
    private long needed;
    private long energyPerRun;
    private long coolant;
    private long runs;
    private String coolantFluid = "";
    /** The recipe shown: "id*count" per item, inputs and outputs. */
    private String recipeIn = "";
    private String recipeOut = "";

    // client animation
    private float phase;
    private long lastFrame = -1;
    private final List<long[]> bursts = new ArrayList<>();

    public AcceleratorConsoleWidget(ParticleAcceleratorMachine machine, int x, int y) {
        super(x, y, WIDTH, HEIGHT);
        this.machine = machine;
    }

    /** The page: this console, a button over each mode tile, the on/off switch and the counter reset. */
    public static WidgetGroup createPage(ParticleAcceleratorMachine machine) {
        var page = new WidgetGroup(0, 0, WIDTH, HEIGHT);
        page.addWidget(new AcceleratorConsoleWidget(machine, 0, 0));
        GTRecipeType[] types = machine.getRecipeTypes();
        for (int i = 0; i < types.length; i++) {
            int index = i;
            var tile = new ButtonWidget(tileX(i, types.length), TILE_Y, tileWidth(types.length), TILE_H,
                    IGuiTexture.EMPTY, click -> {
                        if (click.isRemote || machine.getActiveRecipeType() == index) return;
                        // as GT's mode button: switch, then let the recipe logic look again
                        machine.setActiveRecipeType(index);
                        machine.getRecipeLogic().updateTickSubscription();
                    });
            tile.setHoverTexture(new ColorBorderTexture(1, 0xFFFFFFFF));
            tile.setHoverTooltips(Component.translatable(ProcessMachine.modeKey(types[i])),
                    Component.translatable(ProcessMachine.modeKey(types[i]) + ".desc"),
                    Component.translatable("af9.accelerator.console.select"));
            page.addWidget(tile);
        }
        var power = new ButtonWidget(SWITCH_X, SWITCH_Y, SWITCH_W, SWITCH_H, IGuiTexture.EMPTY, click -> {
            if (!click.isRemote) machine.setWorkingEnabled(!machine.isWorkingEnabled());
        });
        power.setHoverTexture(new ColorBorderTexture(1, 0xFFFFFFFF));
        power.setHoverTooltips(Component.translatable("af9.accelerator.console.switch_tooltip"));
        page.addWidget(power);
        var reset = new ButtonWidget(RESET_X, RESET_Y, RESET_W, RESET_H, IGuiTexture.EMPTY, click -> {
            if (!click.isRemote) machine.resetRuns();
        });
        reset.setHoverTexture(new ColorBorderTexture(1, 0xFFFFFFFF));
        reset.setHoverTooltips(Component.translatable("af9.accelerator.console.reset_tooltip"));
        page.addWidget(reset);
        return page;
    }

    public static int tileWidth(int count) {
        return (FIELD_W - 12 - 3 * (count - 1)) / Math.max(1, count);
    }

    public static int tileX(int index, int count) {
        return FIELD_X + 6 + index * (tileWidth(count) + 3);
    }

    //////////////////////////////////////
    // *********** Sync ***********//
    //////////////////////////////////////

    @Override
    protected boolean sample() {
        var logic = machine.getRecipeLogic();
        int[] now = { machine.getStatus(), machine.getActiveRecipeType(), machine.isFormed() ? machine.getTier() : -1,
                logic.isWorking() ? logic.getProgress() : 0, logic.isWorking() ? logic.getDuration() : 0,
                (int) Math.round(machine.getBeamEnergyGeV()), machine.isWorkingEnabled() ? 1 : 0 };
        int[] before = { status, mode, tier, progress, duration, beam, workingEnabled ? 1 : 0 };
        long[] nowLong = { machine.getAvailableEUt(), machine.getNeededEUt(), machine.getEnergyPerRun(),
                machine.getCoolantAmount(), machine.getRuns() };
        long[] beforeLong = { available, needed, energyPerRun, coolant, runs };
        FluidStack fluid = machine.getCoolant();
        ResourceLocation fluidId = fluid.isEmpty() ? null : ForgeRegistries.FLUIDS.getKey(fluid.getFluid());
        String newFluid = fluidId == null ? "" : fluidId.toString();
        String newIn = machine.shownRecipeItems(true);
        String newOut = machine.shownRecipeItems(false);
        boolean changed = !Arrays.equals(now, before) || !Arrays.equals(nowLong, beforeLong) ||
                !Objects.equals(newFluid, coolantFluid) || !Objects.equals(newIn, recipeIn) ||
                !Objects.equals(newOut, recipeOut);
        status = now[0];
        mode = now[1];
        tier = now[2];
        progress = now[3];
        duration = now[4];
        beam = now[5];
        workingEnabled = now[6] == 1;
        available = nowLong[0];
        needed = nowLong[1];
        energyPerRun = nowLong[2];
        coolant = nowLong[3];
        runs = nowLong[4];
        coolantFluid = newFluid;
        recipeIn = newIn;
        recipeOut = newOut;
        return changed;
    }

    @Override
    protected void writeState(FriendlyByteBuf buffer) {
        for (int value : new int[] { status, mode, tier, progress, duration, beam }) buffer.writeVarInt(value);
        buffer.writeBoolean(workingEnabled);
        for (long value : new long[] { available, needed, energyPerRun, coolant, runs }) buffer.writeVarLong(value);
        buffer.writeUtf(coolantFluid);
        buffer.writeUtf(recipeIn);
        buffer.writeUtf(recipeOut);
    }

    @Override
    protected void readState(FriendlyByteBuf buffer) {
        status = buffer.readVarInt();
        mode = buffer.readVarInt();
        tier = buffer.readVarInt();
        progress = buffer.readVarInt();
        duration = buffer.readVarInt();
        beam = buffer.readVarInt();
        workingEnabled = buffer.readBoolean();
        available = buffer.readVarLong();
        needed = buffer.readVarLong();
        energyPerRun = buffer.readVarLong();
        coolant = buffer.readVarLong();
        runs = buffer.readVarLong();
        coolantFluid = buffer.readUtf();
        recipeIn = buffer.readUtf();
        recipeOut = buffer.readUtf();
    }

    private int modeColor() {
        return machine.modeColor(mode);
    }

    private String modeKey() {
        GTRecipeType[] types = machine.getRecipeTypes();
        return ProcessMachine.modeKey(types[Mth.clamp(mode, 0, types.length - 1)]);
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

    /** The ring from above: mode tiles, beam pipe and gates, bunches, the mode's effects, progress, state. */
    @OnlyIn(Dist.CLIENT)
    private void drawField(GuiGraphics graphics, int x, int y, float partialTicks) {
        Font font = font();
        graphics.fill(x, y, x + FIELD_W, y + FIELD_H, 0xFF070A11);
        border(graphics, x, y, FIELD_W, FIELD_H, EDGE);
        for (int gx = x + 12; gx < x + FIELD_W; gx += 16) graphics.fill(gx, y + 1, gx + 1, y + FIELD_H - 1, 0x0CFFFFFF);
        for (int gy = y + 12; gy < y + FIELD_H; gy += 16) graphics.fill(x + 1, gy, x + FIELD_W - 1, gy + 1, 0x0CFFFFFF);

        GTRecipeType[] types = machine.getRecipeTypes();
        for (int i = 0; i < types.length; i++) {
            String key = ProcessMachine.modeKey(types[i]);
            drawTile(graphics, getPosition().x + tileX(i, types.length), y + TILE_Y - FIELD_Y, tileWidth(types.length),
                    TILE_H, Component.translatable(key + ".short").getString(),
                    Component.translatable("af9.accelerator.mode." + i).getString(), machine.modeColor(i),
                    i == mode, status != STATUS_OFFLINE, false);
        }

        boolean running = status == STATUS_RUNNING;
        double fraction = duration <= 0 ? 0 : Math.min(1.0, (progress + (running ? partialTicks : 0)) / duration);
        int cx = getPosition().x + RING_X, cy = getPosition().y + RING_Y;
        int color = modeColor();
        long now = Util.getMillis();
        float step = lastFrame < 0 ? 0 : Math.min(0.1F, (now - lastFrame) / 1000F);
        lastFrame = now;

        // the beam pipe (a ring of dots) and its four gates
        for (int i = 0; i < 120; i++) {
            double angle = i * Mth.TWO_PI / 120;
            int px = cx + (int) Math.round(Math.cos(angle) * BEAM_R), py = cy + (int) Math.round(Math.sin(angle) * BEAM_R);
            graphics.fill(px - 1, py - 1, px + 1, py + 1, running ? withAlpha(color, 0x50) : 0xFF1B2433);
        }
        for (int g = 0; g < 4; g++) {
            double angle = g * Math.PI / 2;
            int px = cx + (int) Math.round(Math.cos(angle) * BEAM_R), py = cy + (int) Math.round(Math.sin(angle) * BEAM_R);
            graphics.fill(px - 4, py - 4, px + 4, py + 4, 0xFF0B0F17);
            border(graphics, px - 4, py - 4, 8, 8, running ? withAlpha(color, 0xC0) : EDGE);
        }

        // bunches: faster as the run goes on; lap events (a target hit, a collision) leave a burst
        float before = phase;
        if (running) phase += step * (1.2F + 7F * (float) fraction);
        if (mode == 0) {
            drawBunch(graphics, cx, cy, Mth.HALF_PI + phase, 1, color, running);
            // the spallation target at the south gate
            graphics.fill(cx - 2, cy + BEAM_R + 5, cx + 3, cy + BEAM_R + 9, running ? 0xFFB0B8C8 : DIM);
            if (running && Math.floor(before / Mth.TWO_PI) != Math.floor(phase / Mth.TWO_PI)) {
                bursts.add(new long[] { now, 0 });
            }
        } else {
            int second = mode == 1 ? 0xFFFF5A36 : 0xFF38E1FF;
            drawBunch(graphics, cx, cy, Mth.HALF_PI + phase, 1, color, running);
            drawBunch(graphics, cx, cy, Mth.HALF_PI - phase, -1, second, running);
            // they meet at the south and the north gate, every half lap
            if (running && Math.floor(before / Math.PI) != Math.floor(phase / Math.PI)) {
                bursts.add(new long[] { now, (long) Math.floor(phase / Math.PI) & 1 });
            }
        }
        drawBursts(graphics, cx, cy, now, color);
        if (mode == 2) drawCondensate(graphics, cx, cy, fraction, running, now);

        // progress ring
        int dots = 96;
        for (int i = 0; i < dots; i++) {
            double angle = -Math.PI / 2 + i * 2 * Math.PI / dots;
            int px = cx + (int) Math.round(Math.cos(angle) * PROGRESS_R);
            int py = cy + (int) Math.round(Math.sin(angle) * PROGRESS_R);
            graphics.fill(px - 1, py - 1, px + 1, py + 1, running && i < fraction * dots ? color : TRACK);
        }

        // state and run time
        String state = running ? Component.translatable("af9.accelerator.console.accelerating").getString() + "  " +
                Math.round(fraction * 100) + "%" : Component.translatable("af9.console.status." + status).getString();
        graphics.drawString(font, state, cx - font.width(state) / 2, y + 117, running ? color : statusColor(status),
                false);
        bar(graphics, x + 8, y + 128, FIELD_W - 16, 4, running ? fraction : 0, color);
        String timeText = running ? seconds(progress) + " / " + seconds(duration) :
                Component.translatable("af9.console.no_run").getString();
        drawSmall(graphics, timeText, cx, y + 133, running ? TEXT : MUTED, true);
    }

    /** A bunch of particles on the beam pipe: a bright head and a fading tail behind it. */
    @OnlyIn(Dist.CLIENT)
    private static void drawBunch(GuiGraphics graphics, int cx, int cy, float angle, int direction, int color,
                                  boolean running) {
        if (!running) return;
        for (int i = 10; i >= 0; i--) {
            double a = angle - direction * i * 0.09;
            int px = cx + (int) Math.round(Math.cos(a) * BEAM_R), py = cy + (int) Math.round(Math.sin(a) * BEAM_R);
            int alpha = i == 0 ? 0xFF : 0xC0 - i * 0x10;
            int size = i == 0 ? 2 : 1;
            graphics.fill(px - size, py - size, px + size, py + size, withAlpha(i == 0 ? 0xFFFFFF : color, alpha));
        }
    }

    /**
     * Lap events: a neutron spray off the target (mode 0), else a collision flash at the south (0) or north (1) gate:
     * a white star and sparks flying off, gone in 0.6 s.
     */
    @OnlyIn(Dist.CLIENT)
    private void drawBursts(GuiGraphics graphics, int cx, int cy, long now, int color) {
        for (Iterator<long[]> it = bursts.iterator(); it.hasNext();) {
            long[] burst = it.next();
            float age = (now - burst[0]) / 600F;
            if (age >= 1 || bursts.size() > 12) {
                it.remove();
                continue;
            }
            int bx = cx, by = burst[1] == 0 ? cy + BEAM_R : cy - BEAM_R;
            int alpha = (int) (0xFF * (1 - age));
            if (mode != 0) {
                int arm = (int) (2 + 5 * (1 - age));
                graphics.fill(bx - arm, by, bx + arm + 1, by + 1, withAlpha(0xFFFFFF, alpha));
                graphics.fill(bx, by - arm, bx + 1, by + arm + 1, withAlpha(0xFFFFFF, alpha));
            }
            for (int i = 0; i < 10; i++) {
                double a = i * Mth.TWO_PI / 10 + burst[0] % 7;
                double reach = (mode == 0 ? 6 : 3) + age * (mode == 0 ? 26 : 14);
                int px = bx + (int) Math.round(Math.cos(a) * reach), py = by + (int) Math.round(Math.sin(a) * reach);
                graphics.fill(px, py, px + 1, py + 1, withAlpha(mode == 0 ? 0xA8FFB8 : color, alpha));
            }
        }
    }

    /** Quark synthesis: the condensate in the middle, growing with the run, its three colour charges circling it. */
    @OnlyIn(Dist.CLIENT)
    private void drawCondensate(GuiGraphics graphics, int cx, int cy, double fraction, boolean running, long now) {
        int radius = (int) Math.round(2 + (running ? 9 * fraction : 0));
        float pulse = 0.5F + 0.5F * Mth.sin(now / 180F);
        for (int dy = -radius; dy <= radius; dy++) {
            int half = (int) Math.sqrt((double) radius * radius - dy * dy);
            graphics.fill(cx - half, cy + dy, cx + half + 1, cy + dy + 1,
                    withAlpha(modeColor(), running ? (int) (0x70 + 0x50 * pulse) : 0x30));
        }
        if (!running) return;
        int[] charges = { 0xFFFF4040, 0xFF40FF60, 0xFF4080FF };
        for (int i = 0; i < 3; i++) {
            double a = now / 300.0 + i * Mth.TWO_PI / 3;
            int px = cx + (int) Math.round(Math.cos(a) * (radius + 4));
            int py = cy + (int) Math.round(Math.sin(a) * (radius + 4));
            graphics.fill(px - 1, py - 1, px + 1, py + 1, charges[i]);
        }
    }

    /** The run: mode, beam energy, recipe items, energy per run, switch, counter, hint. */
    @OnlyIn(Dist.CLIENT)
    private void drawPanel(GuiGraphics graphics, int x, int y) {
        Font font = font();
        int x0 = getPosition().x;
        int y0 = getPosition().y;
        int color = modeColor();
        graphics.fill(x, y, x + PANEL_W, y + PANEL_H, PANEL);
        border(graphics, x, y, PANEL_W, PANEL_H, EDGE);
        graphics.fill(x + 1, y + 1, x + PANEL_W - 1, y + 2, withAlpha(color, 0xAA));

        graphics.drawString(font, fit(Component.translatable(modeKey()).getString(), PANEL_W - 10), x + 5, y + 5,
                color, false);
        drawSmall(graphics, Component.translatable("af9.accelerator.console.beam").getString(), x + 5, y + 17,
                MUTED, false);
        String beamText = beam > 0 ? beam + " GeV" : "-";
        graphics.drawString(font, beamText, x + PANEL_W - 5 - font.width(beamText), y + 15, beam > 0 ? INFO : MUTED,
                false);

        // the recipe: its items in, an arrow, its items out
        drawSmall(graphics, Component.translatable("af9.accelerator.console.recipe").getString(), x + 5, y + 28,
                MUTED, false);
        List<ItemStack> in = stacks(recipeIn), out = stacks(recipeOut);
        if (in.isEmpty() && out.isEmpty()) {
            graphics.drawString(font, Component.translatable("af9.accelerator.console.no_recipe").getString(), x + 5,
                    y + 40, MUTED, false);
        } else {
            int ix = x + 5;
            for (ItemStack stack : in) {
                drawItem(graphics, font, stack, ix, y + 35);
                ix += 18;
            }
            drawSmall(graphics, ">", ix + 3, y + 40, color, false);
            ix += 12;
            for (ItemStack stack : out) {
                drawItem(graphics, font, stack, ix, y + 35);
                ix += 18;
            }
            if (!out.isEmpty()) {
                drawSmall(graphics, fit(out.get(0).getHoverName().getString(), (PANEL_W - 10) * 4 / 3), x + 5,
                        y + 55, TEXT, false);
            }
        }
        drawSmall(graphics, Component.translatable("af9.accelerator.console.energy").getString(), x + 5, y + 65,
                MUTED, false);
        String energyText = energyPerRun > 0 ? compact(energyPerRun) + " EU" : "-";
        drawSmall(graphics, energyText, x + PANEL_W - 5 - font.width(energyText) * 3 / 4, y + 65, TEXT, false);

        // the on/off switch
        int sx = x0 + SWITCH_X, sy = y0 + SWITCH_Y;
        graphics.fill(sx, sy, sx + SWITCH_W, sy + SWITCH_H, workingEnabled ? withAlpha(GOOD, 0x28) : 0xFF0B0F17);
        border(graphics, sx, sy, SWITCH_W, SWITCH_H, workingEnabled ? GOOD : DIM);
        graphics.fill(sx + 5, sy + 5, sx + 11, sy + 11, workingEnabled ? GOOD : DIM);
        String switchText = Component.translatable(workingEnabled ? "af9.orbital.console.online" :
                "af9.orbital.console.offline").getString();
        graphics.drawString(font, switchText, sx + 7 + (SWITCH_W - 7 - font.width(switchText)) / 2, sy + 4,
                workingEnabled ? GOOD : MUTED, false);

        // coolant in the hatches
        drawSmall(graphics, Component.translatable("af9.accelerator.console.coolant").getString(), x + 5, y + 97,
                MUTED, false);
        String coolantText = coolant > 0 ? compact(coolant) + " mB" : "-";
        graphics.drawString(font, coolantText, x + PANEL_W - 5 - font.width(coolantText), y + 95,
                coolant > 0 ? GOOD : BAD, false);

        String counter = Component.translatable("af9.accelerator.console.runs", compact(runs)).getString();
        drawSmall(graphics, fit(counter, (x0 + RESET_X - 3 - (x + 5)) * 4 / 3), x + 5, y0 + RESET_Y + 2, MUTED,
                false);
        drawButton(graphics, x0 + RESET_X, y0 + RESET_Y, RESET_W, RESET_H,
                Component.translatable("af9.litho.console.reset").getString());

        // what to do now
        Component hint = Component.translatable("af9.accelerator.hint." + status);
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
    private static void drawItem(GuiGraphics graphics, Font font, ItemStack stack, int x, int y) {
        graphics.fill(x - 1, y - 1, x + 17, y + 17, 0xFF0B0F17);
        graphics.renderItem(stack, x, y);
        graphics.renderItemDecorations(font, stack, x, y);
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
            int count = star < 0 ? 1 : Integer.parseInt(part.substring(star + 1));
            stacks.add(new ItemStack(item, count));
        }
        return stacks;
    }

    @OnlyIn(Dist.CLIENT)
    private String coolantName() {
        ResourceLocation id = coolantFluid.isEmpty() ? null : ResourceLocation.tryParse(coolantFluid);
        Fluid fluid = id == null ? null : ForgeRegistries.FLUIDS.getValue(id);
        return fluid == null ? "" : new FluidStack(fluid, 1).getDisplayName().getString();
    }

    //////////////////////////////////////
    // ******** Side panels *********//
    //////////////////////////////////////

    /**
     * A panel beside the player inventory, drawn from the console's synced state: left the process (coolant, magnets,
     * beam, RF, mode), right the system (status, power, tier, switch, runs).
     */
    public static class SidePanel extends Widget {

        private final AcceleratorConsoleWidget console;
        private final boolean system;

        public SidePanel(AcceleratorConsoleWidget console, boolean system, int x, int y, int width, int height) {
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
            graphics.fill(x + 1, y + 1, x + w - 1, y + 2, withAlpha(console.modeColor(), 0xAA));
            drawSmall(graphics, Component.translatable(system ? "af9.orbital.console.system" :
                    "af9.orbital.console.process").getString(), x + 4, y + 5, TEXT, false);
            if (system) drawSystem(graphics, x + 4, y + 16, w - 8);
            else drawProcess(graphics, x + 4, y + 16, w - 8);
            super.drawInBackground(graphics, mouseX, mouseY, partialTicks);
        }

        @OnlyIn(Dist.CLIENT)
        private void drawProcess(GuiGraphics graphics, int x, int y, int w) {
            boolean cold = console.coolant > 0;
            boolean running = console.status == STATUS_RUNNING;
            // coolant: amount on the row, the fluid in full below it
            row(graphics, x, y, w, "af9.accelerator.console.coolant", cold ? compact(console.coolant) + " mB" :
                    Component.translatable("af9.accelerator.console.empty").getString(), cold ? GOOD : BAD);
            String fluid = cold ? console.coolantName() :
                    Component.translatable("af9.accelerator.console.fill").getString();
            drawSmall(graphics, fit(fluid, w * 4 / 3), x, y + 7, cold ? TEXT : BAD, false);
            row(graphics, x, y + 16, w, "af9.accelerator.console.magnets",
                    Component.translatable(cold ? "af9.accelerator.console.cold" : "af9.accelerator.console.warm")
                            .getString(),
                    cold ? GOOD : BAD);
            row(graphics, x, y + 25, w, "af9.accelerator.console.beam",
                    console.beam > 0 ? console.beam + " GeV" : "-", console.beam > 0 ? INFO : MUTED);
            row(graphics, x, y + 34, w, "af9.accelerator.console.rf",
                    Component.translatable(running ? "af9.accelerator.console.rf_on" :
                            "af9.accelerator.console.rf_standby").getString(),
                    running ? GOOD : MUTED);
            row(graphics, x, y + 43, w, "af9.accelerator.console.mode",
                    Component.translatable(console.modeKey() + ".short").getString(), console.modeColor());
            // the beam pipe in the panel: a line of dots running while it accelerates
            long now = Util.getMillis();
            for (int i = 0; i < w; i += 3) {
                boolean lit = running && ((i + now / 30) % 24 < 6);
                graphics.fill(x + i, y + 54, x + i + 2, y + 55, lit ? console.modeColor() : TRACK);
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
                    compact(console.available) + "/" + (needed > 0 ? compact(needed) : "-"), enough ? TEXT : BAD);
            bar(graphics, x, y + 18, w, 3, needed <= 0 ? (console.available > 0 ? 1 : 0) :
                    Math.min(1.0, (double) console.available / needed), enough ? GOOD : BAD);
            drawSmall(graphics, "EU/t", x, y + 23, MUTED, false);
            row(graphics, x, y + 33, w, "af9.orbital.console.tier",
                    console.tier < 0 ? "-" : GTValues.VNF[Math.min(console.tier, GTValues.VNF.length - 1)], TEXT);
            row(graphics, x, y + 43, w, "af9.orbital.console.switch",
                    Component.translatable(console.workingEnabled ? "af9.orbital.console.online" :
                            "af9.orbital.console.offline").getString(),
                    console.workingEnabled ? GOOD : MUTED);
            row(graphics, x, y + 53, w, "af9.accelerator.console.runs_label", compact(console.runs), TEXT);
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
