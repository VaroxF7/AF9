package com.af9.core.machine.console;

import com.af9.core.machine.PlasmaForgeMachine;

import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Objects;

/**
 * Console of the Dimensionally Transcendent Plasma Forge. Four gauges over what keeps the hall running: the magnetic
 * field (with the marks where a run is lost and where the forge is ready), its stabilisers (the 10 A of LuV
 * auxiliary power), the osmium plasma it burns, and the primary power of the recipes (one input); under them what is
 * being made, the running-time ramp and the run-time bar. Every text is cut or sized to its column
 * ({@code tools/lint}-free: the widths were checked against the font's glyph widths).
 */
public class PlasmaForgeConsoleWidget extends ConsoleWidget {

    public static final int WIDTH = 220, HEIGHT = 134;
    /** The two columns: left edge, width. */
    private static final int LX = 6, RX = 114, COL_W = 100;
    /** Rows of the gauges: the label, the bar, the line under it. */
    private static final int ROW_A = 19, ROW_B = 48, ROW_C = 77;

    private final PlasmaForgeMachine machine;

    // last state sent to / received by the client
    private int status = -1;
    private int field;
    private int fieldState;
    private boolean plasmaFed, auxPaid;
    private long plasmaStored, auxStored, available, needed;
    private int plasmaPull, inputs, ramp, progress, duration;
    private String output = "";

    public PlasmaForgeConsoleWidget(PlasmaForgeMachine machine, int x, int y) {
        super(x, y, WIDTH, HEIGHT);
        this.machine = machine;
    }

    /** The console with a hover tooltip over each gauge. */
    public static WidgetGroup create(PlasmaForgeMachine machine) {
        var group = new WidgetGroup(0, 0, WIDTH, HEIGHT);
        group.addWidget(new PlasmaForgeConsoleWidget(machine, 0, 0));
        group.addWidget(tip(LX, ROW_A, COL_W, 28, "field", PlasmaForgeMachine.FIELD_READY * 100 /
                PlasmaForgeMachine.FIELD_MAX, PlasmaForgeMachine.FIELD_HOLD * 100 / PlasmaForgeMachine.FIELD_MAX));
        group.addWidget(tip(RX, ROW_A, COL_W, 28, "aux", PlasmaForgeMachine.AUX_AMPS,
                compact(PlasmaForgeMachine.AUX_EUT)));
        group.addWidget(tip(LX, ROW_B, COL_W, 26, "plasma", machine.isMk2() ? 50 : 100, machine.isMk2() ? 25 : 50));
        group.addWidget(tip(RX, ROW_B, COL_W, 26, "power", 1, ""));
        group.addWidget(tip(RX, ROW_C, COL_W, 26, "ramp", PlasmaForgeMachine.ENERGY_PERCENT,
                PlasmaForgeMachine.TIME_PERCENT));
        return group;
    }

    private static Widget tip(int x, int y, int w, int h, String key, Object a, Object b) {
        var widget = new Widget(x, y, w, h);
        widget.setHoverTooltips(Component.translatable("af9.dtpf.console.tip." + key + ".0", a, b),
                Component.translatable("af9.dtpf.console.tip." + key + ".1", a, b),
                Component.translatable("af9.dtpf.console.tip." + key + ".2", a, b));
        return widget;
    }

    //////////////////////////////////////
    // *********** Sync ***********//
    //////////////////////////////////////

    @Override
    protected boolean sample() {
        var logic = machine.getRecipeLogic();
        int newStatus = machine.getStatus();
        int newField = machine.getFieldStrength();
        int newFieldState = machine.getFieldState();
        boolean newPlasmaFed = machine.isPlasmaFed();
        boolean newAuxPaid = machine.isAuxPaid();
        long newPlasmaStored = machine.isFormed() ? machine.getPlasmaStored() : 0;
        long newAuxStored = machine.isFormed() ? machine.getAuxiliaryStored() : 0;
        long newAvailable = machine.getAvailableEUt();
        long newNeeded = machine.getNeededEUt();
        int newPlasmaPull = machine.getPlasmaPull();
        int newInputs = machine.isFormed() ? machine.countPrimaryInputs() : 0;
        int newRamp = (int) Math.round(machine.getRamp() * 100);
        int newProgress = logic.isWorking() ? logic.getProgress() : 0;
        int newDuration = logic.isWorking() ? logic.getDuration() : 0;
        String newOutput = machine.getCurrentOutput();
        boolean changed = newStatus != status || newField != field || newFieldState != fieldState ||
                newPlasmaFed != plasmaFed || newAuxPaid != auxPaid || newPlasmaStored != plasmaStored ||
                newAuxStored != auxStored || newAvailable != available || newNeeded != needed ||
                newPlasmaPull != plasmaPull || newInputs != inputs || newRamp != ramp || newProgress != progress ||
                newDuration != duration || !Objects.equals(newOutput, output);
        status = newStatus;
        field = newField;
        fieldState = newFieldState;
        plasmaFed = newPlasmaFed;
        auxPaid = newAuxPaid;
        plasmaStored = newPlasmaStored;
        auxStored = newAuxStored;
        available = newAvailable;
        needed = newNeeded;
        plasmaPull = newPlasmaPull;
        inputs = newInputs;
        ramp = newRamp;
        progress = newProgress;
        duration = newDuration;
        output = newOutput;
        return changed;
    }

    @Override
    protected void writeState(FriendlyByteBuf buffer) {
        buffer.writeVarInt(status);
        buffer.writeVarInt(field);
        buffer.writeVarInt(fieldState);
        buffer.writeBoolean(plasmaFed);
        buffer.writeBoolean(auxPaid);
        buffer.writeVarLong(plasmaStored);
        buffer.writeVarLong(auxStored);
        buffer.writeVarLong(available);
        buffer.writeVarLong(needed);
        buffer.writeVarInt(plasmaPull);
        buffer.writeVarInt(inputs);
        buffer.writeVarInt(ramp);
        buffer.writeVarInt(progress);
        buffer.writeVarInt(duration);
        buffer.writeUtf(output);
    }

    @Override
    protected void readState(FriendlyByteBuf buffer) {
        status = buffer.readVarInt();
        field = buffer.readVarInt();
        fieldState = buffer.readVarInt();
        plasmaFed = buffer.readBoolean();
        auxPaid = buffer.readBoolean();
        plasmaStored = buffer.readVarLong();
        auxStored = buffer.readVarLong();
        available = buffer.readVarLong();
        needed = buffer.readVarLong();
        plasmaPull = buffer.readVarInt();
        inputs = buffer.readVarInt();
        ramp = buffer.readVarInt();
        progress = buffer.readVarInt();
        duration = buffer.readVarInt();
        output = buffer.readUtf();
    }

    //////////////////////////////////////
    // ********** Drawing *********//
    //////////////////////////////////////

    @Override
    @OnlyIn(Dist.CLIENT)
    public void drawInBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        super.drawInBackground(graphics, mouseX, mouseY, partialTicks);
        int x0 = getPosition().x;
        int y0 = getPosition().y;
        int accent = machine.modeColor(0);

        drawFrame(graphics, Component.translatable(machine.titleKey()).getString(), machine.isMk2() ? "Mk2" : "",
                accent, status, accent);

        // the magnetic field, with the mark where a run is lost and where the forge is ready
        int ready = PlasmaForgeMachine.FIELD_READY, hold = PlasmaForgeMachine.FIELD_HOLD,
                max = PlasmaForgeMachine.FIELD_MAX;
        int fieldColor = field >= ready ? GOOD : field >= hold ? WARN : BAD;
        label(graphics, "af9.dtpf.console.l.field", Math.round(100f * field / max) + " %", x0 + LX, y0 + ROW_A,
                fieldColor);
        bar(graphics, x0 + LX, y0 + ROW_A + 7, COL_W, 9, (double) field / max, fieldColor);
        mark(graphics, x0 + LX + COL_W * hold / max, y0 + ROW_A + 7, 9, DIM);
        mark(graphics, x0 + LX + COL_W * ready / max, y0 + ROW_A + 7, 9, TEXT);
        drawSmall(graphics, Component.translatable("af9.dtpf.console.field." + fieldState).getString(), x0 + LX,
                y0 + ROW_A + 19, fieldColor, false);
        drawSmallRight(graphics, Component.translatable("af9.dtpf.console.ready_at",
                100 * ready / max).getString(), x0 + LX + COL_W, y0 + ROW_A + 19, MUTED);

        // the stabilisers: the auxiliary power, a second of it in the hatches fills the bar
        long second = PlasmaForgeMachine.AUX_EUT * 20;
        int auxColor = auxPaid ? GOOD : BAD;
        label(graphics, "af9.dtpf.console.l.aux", compact(PlasmaForgeMachine.AUX_EUT) + " EU/t", x0 + RX,
                y0 + ROW_A, MUTED);
        bar(graphics, x0 + RX, y0 + ROW_A + 7, COL_W, 9, Math.min(1.0, (double) auxStored / second), auxColor);
        drawSmall(graphics, Component.translatable("af9.dtpf.console.stored", compact(auxStored)).getString(),
                x0 + RX, y0 + ROW_A + 19, TEXT, false);
        drawSmallRight(graphics, Component.translatable(auxPaid ? "af9.dtpf.console.paid" : "af9.dtpf.console.short")
                .getString(), x0 + RX + COL_W, y0 + ROW_A + 19, auxColor);

        // the osmium plasma: a minute of it in the hatches fills the bar
        int plasmaColor = plasmaFed ? GOOD : BAD;
        label(graphics, "af9.dtpf.console.l.plasma", plasmaPull + " mB/s", x0 + LX, y0 + ROW_B, MUTED);
        bar(graphics, x0 + LX, y0 + ROW_B + 7, COL_W, 6,
                Math.min(1.0, (double) plasmaStored / Math.max(1, plasmaPull * 60L)), plasmaColor);
        drawSmall(graphics, Component.translatable("af9.dtpf.console.stored_mb", compact(plasmaStored)).getString(),
                x0 + LX, y0 + ROW_B + 16, TEXT, false);
        drawSmallRight(graphics, Component.translatable(plasmaFed ? "af9.dtpf.console.burning" :
                "af9.dtpf.console.starved").getString(), x0 + LX + COL_W, y0 + ROW_B + 16, plasmaColor);

        // the primary power of the recipes: one input
        boolean enough = needed == 0 || available >= needed;
        boolean oneInput = inputs <= 1;
        int powerColor = enough && oneInput ? GOOD : BAD;
        label(graphics, "af9.dtpf.console.l.power", Component.translatable("af9.dtpf.console.inputs", inputs)
                .getString(), x0 + RX, y0 + ROW_B, oneInput ? MUTED : BAD);
        bar(graphics, x0 + RX, y0 + ROW_B + 7, COL_W, 6, needed == 0 ? (available > 0 ? 1 : 0) :
                Math.min(1.0, (double) available / needed), powerColor);
        drawSmall(graphics, compact(available) + " / " + (needed == 0 ? "-" : compact(needed)) + " EU/t",
                x0 + RX, y0 + ROW_B + 16, enough ? TEXT : BAD, false);

        // what is being made, and the ramp
        drawSmall(graphics, Component.translatable("af9.console.output").getString(), x0 + LX, y0 + ROW_C, MUTED,
                false);
        drawOutput(graphics, x0 + LX, y0 + ROW_C + 8, COL_W);
        label(graphics, "af9.dtpf.console.l.ramp", ramp + " %", x0 + RX, y0 + ROW_C, MUTED);
        bar(graphics, x0 + RX, y0 + ROW_C + 8, COL_W, 5, ramp / 100.0, INFO);
        drawSmall(graphics, Component.translatable("af9.dtpf.console.discount",
                Math.round(PlasmaForgeMachine.ENERGY_PERCENT * ramp / 100.0),
                Math.round(PlasmaForgeMachine.TIME_PERCENT * ramp / 100.0)).getString(), x0 + RX, y0 + ROW_C + 16,
                TEXT, false);

        drawRuntime(graphics, x0 + 6, y0 + 103, WIDTH - 12, progress, duration, status == STATUS_RUNNING, accent);
    }

    /** A gauge's label on the left and its value on the right, both small. */
    @OnlyIn(Dist.CLIENT)
    private static void label(GuiGraphics graphics, String key, String value, int x, int y, int valueColor) {
        drawSmall(graphics, Component.translatable(key).getString(), x, y, MUTED, false);
        drawSmallRight(graphics, value, x + COL_W, y, valueColor);
    }

    /** Small text ending at x. */
    @OnlyIn(Dist.CLIENT)
    private static void drawSmallRight(GuiGraphics graphics, String text, int x, int y, int color) {
        drawSmall(graphics, text, x - font().width(text) * 3 / 4, y, color, false);
    }

    @OnlyIn(Dist.CLIENT)
    private static void mark(GuiGraphics graphics, int x, int y, int height, int color) {
        graphics.fill(x, y, x + 1, y + height, color);
    }

    @OnlyIn(Dist.CLIENT)
    private void drawOutput(GuiGraphics graphics, int x, int y, int width) {
        var font = font();
        int split = output.indexOf(':');
        ResourceLocation id = split < 0 ? null : ResourceLocation.tryParse(output.substring(split + 1));
        if (id != null && output.startsWith("item:")) {
            Item item = ForgeRegistries.ITEMS.getValue(id);
            if (item != null && item != Items.AIR) {
                ItemStack stack = new ItemStack(item);
                graphics.renderItem(stack, x, y);
                graphics.drawString(font, fit(stack.getHoverName().getString(), width - 20), x + 19, y + 4, TEXT,
                        false);
                return;
            }
        } else if (id != null && output.startsWith("fluid:")) {
            Fluid fluid = ForgeRegistries.FLUIDS.getValue(id);
            if (fluid != null && fluid != Fluids.EMPTY) {
                graphics.drawString(font, fit(fluid.getFluidType().getDescription().getString(), width), x, y + 4,
                        INFO, false);
                return;
            }
        }
        graphics.drawString(font, Component.translatable("af9.console.nothing").getString(), x, y + 4, MUTED, false);
    }
}
