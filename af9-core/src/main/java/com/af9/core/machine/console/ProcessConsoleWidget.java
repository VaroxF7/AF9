package com.af9.core.machine.console;

import com.af9.core.machine.ProcessMachine;

import com.gregtechceu.gtceu.api.recipe.GTRecipeType;

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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Console of the AF9 process multiblocks: mode tiles, power gauge against the running recipe's EU/t, what is being
 * made, the machine's own readouts ({@link ProcessMachine#infoLines()}), coolant for cooled machines and the run-time
 * bar.
 */
public class ProcessConsoleWidget extends ConsoleWidget {

    public static final int WIDTH = 220;
    /** With the usual four readout lines; {@link #heightOf} for a machine with more. */
    public static final int HEIGHT = 128;
    public static final int TILE_Y = 18;
    public static final int TILE_H = 18;
    public static final int TILE_GAP = 2;
    private static final int USUAL_LINES = 4;
    private static final int LINE_H = 8;

    private final ProcessMachine machine;

    // last state sent to / received by the client
    private int status = -1;
    private int mode;
    private long available;
    private long needed;
    private int progress;
    private int duration;
    private long coolant;
    private String output = "";
    private List<Component> lines = new ArrayList<>();

    public ProcessConsoleWidget(ProcessMachine machine, int x, int y) {
        super(x, y, WIDTH, heightOf(machine));
        this.machine = machine;
    }

    /** The console's height for a machine: a readout line more is a line's height more. */
    public static int heightOf(ProcessMachine machine) {
        return HEIGHT + Math.max(0, machine.consoleLines() - USUAL_LINES) * LINE_H;
    }

    /** The console with a hover area per mode tile (tooltip only: the tiles show the mode, GT's side tab sets it). */
    public static WidgetGroup create(ProcessMachine machine) {
        var group = new WidgetGroup(0, 0, WIDTH, heightOf(machine));
        group.addWidget(new ProcessConsoleWidget(machine, 0, 0));
        GTRecipeType[] types = machine.getRecipeTypes();
        for (int i = 0; i < types.length; i++) {
            var tile = new Widget(tileX(i, types.length), TILE_Y, tileWidth(types.length), TILE_H);
            tile.setHoverTooltips(Component.translatable(ProcessMachine.modeKey(types[i])),
                    Component.translatable(ProcessMachine.modeKey(types[i]) + ".desc"));
            group.addWidget(tile);
        }
        return group;
    }

    public static int tileWidth(int count) {
        return (WIDTH - 8 - TILE_GAP * (count - 1)) / Math.max(1, count);
    }

    public static int tileX(int index, int count) {
        return 4 + index * (tileWidth(count) + TILE_GAP);
    }

    //////////////////////////////////////
    // *********** Sync ***********//
    //////////////////////////////////////

    @Override
    protected boolean sample() {
        var logic = machine.getRecipeLogic();
        int newStatus = machine.getStatus();
        int newMode = machine.getActiveRecipeType();
        long newAvailable = machine.getAvailableEUt();
        long newNeeded = machine.getNeededEUt();
        int newProgress = logic.isWorking() ? logic.getProgress() : 0;
        int newDuration = logic.isWorking() ? logic.getDuration() : 0;
        long newCoolant = machine.usesCoolant() ? machine.getCoolantAmount() : 0;
        String newOutput = machine.getCurrentOutput();
        List<Component> newLines = machine.infoLines();
        int maxLines = Math.max(USUAL_LINES, machine.consoleLines());
        if (newLines.size() > maxLines) newLines = newLines.subList(0, maxLines);
        boolean changed = newStatus != status || newMode != mode || newAvailable != available ||
                newNeeded != needed || newProgress != progress || newDuration != duration || newCoolant != coolant ||
                !Objects.equals(newOutput, output) || !newLines.equals(lines);
        status = newStatus;
        mode = newMode;
        available = newAvailable;
        needed = newNeeded;
        progress = newProgress;
        duration = newDuration;
        coolant = newCoolant;
        output = newOutput;
        lines = new ArrayList<>(newLines);
        return changed;
    }

    @Override
    protected void writeState(FriendlyByteBuf buffer) {
        buffer.writeVarInt(status);
        buffer.writeVarInt(mode);
        buffer.writeVarLong(available);
        buffer.writeVarLong(needed);
        buffer.writeVarInt(progress);
        buffer.writeVarInt(duration);
        buffer.writeVarLong(coolant);
        buffer.writeUtf(output);
        buffer.writeVarInt(lines.size());
        for (Component line : lines) buffer.writeComponent(line);
    }

    @Override
    protected void readState(FriendlyByteBuf buffer) {
        status = buffer.readVarInt();
        mode = buffer.readVarInt();
        available = buffer.readVarLong();
        needed = buffer.readVarLong();
        progress = buffer.readVarInt();
        duration = buffer.readVarInt();
        coolant = buffer.readVarLong();
        output = buffer.readUtf();
        int count = buffer.readVarInt();
        List<Component> read = new ArrayList<>(count);
        for (int i = 0; i < count; i++) read.add(buffer.readComponent());
        lines = read;
    }

    //////////////////////////////////////
    // ********** Drawing *********//
    //////////////////////////////////////

    @Override
    @OnlyIn(Dist.CLIENT)
    public void drawInBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        super.drawInBackground(graphics, mouseX, mouseY, partialTicks);
        var font = font();
        int x0 = getPosition().x;
        int y0 = getPosition().y;
        GTRecipeType[] types = machine.getRecipeTypes();
        int active = Math.max(0, Math.min(mode, types.length - 1));
        int accent = machine.modeColor(active);

        drawFrame(graphics, Component.translatable(machine.titleKey()).getString(), "", accent, status, accent);

        for (int i = 0; i < types.length; i++) {
            String name = Component.translatable(ProcessMachine.modeKey(types[i]) + ".short").getString();
            drawTile(graphics, x0 + tileX(i, types.length), y0 + TILE_Y, tileWidth(types.length), TILE_H, name, "",
                    machine.modeColor(i), i == active, status != STATUS_OFFLINE, false);
        }

        // left: power against the running recipe
        int lx = x0 + 6;
        int lw = 96;
        drawSmall(graphics, Component.translatable("af9.console.power").getString(), lx, y0 + 42, MUTED, false);
        boolean enough = needed == 0 || available >= needed;
        bar(graphics, lx, y0 + 49, lw, 5, needed == 0 ? (available > 0 ? 1 : 0) :
                Math.min(1.0, (double) available / needed), enough ? GOOD : BAD);
        drawSmall(graphics, compact(available) + " / " + (needed == 0 ? "-" : compact(needed)) + " EU/t", lx, y0 + 57,
                enough ? TEXT : BAD, false);
        if (machine.usesCoolant()) {
            drawSmall(graphics, Component.translatable("af9.console.coolant").getString(), lx, y0 + 67, MUTED, false);
            graphics.drawString(font, compact(coolant) + " mB", lx, y0 + 74, coolant > 0 ? INFO : BAD, false);
        }

        // right: output and the machine's readouts
        int rx = x0 + 108;
        int rw = WIDTH - 114;
        drawSmall(graphics, Component.translatable("af9.console.output").getString(), rx, y0 + 42, MUTED, false);
        drawOutput(graphics, rx, y0 + 49, rw);
        for (int i = 0; i < lines.size(); i++) {
            drawSmall(graphics, fit(lines.get(i).getString(), rw * 4 / 3), rx, y0 + 68 + i * LINE_H, TEXT, false);
        }

        drawRuntime(graphics, x0 + 6, y0 + 101 + getSize().height - HEIGHT, WIDTH - 12, progress, duration,
                status == STATUS_RUNNING, accent);
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
