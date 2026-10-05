package com.af9.core.machine.console;

import com.af9.core.machine.ProcessMachine;
import com.af9.core.machine.VoidMinerMachine;

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
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * The Void Miner's screen, the Orbital Lithography Station's layout (see {@link SidePanelsUIWidget}):
 * <ul>
 * <li>left, the shaft from above: the four areas as tiles (click one to switch), the drill descending while a run
 * is on, ore sparks rising off the rock, the state and the run-time bar;</li>
 * <li>right, the run: the area, the recipe's items (in, out), energy per run, the on/off switch, the run counter
 * and a hint for the current state;</li>
 * <li>beside the player inventory ({@link SidePanel}): left the process (area, power, state), right the system
 * (status, power, tier, switch, runs).</li>
 * </ul>
 * The server samples the machine every tick and sends the state only when something changed.
 */
public class VoidMinerConsoleWidget extends ConsoleWidget {

    public static final int WIDTH = 384;
    public static final int HEIGHT = 148;
    /** Left field and right panel of the page. */
    public static final int FIELD_X = 4, FIELD_Y = 4, FIELD_W = 240, FIELD_H = 140;
    public static final int PANEL_X = 250, PANEL_Y = 4, PANEL_W = 130, PANEL_H = 140;
    public static final int TILE_Y = 8, TILE_H = 20;
    public static final int SWITCH_X = PANEL_X + 5, SWITCH_Y = 80, SWITCH_W = PANEL_W - 10, SWITCH_H = 16;
    public static final int RESET_W = 30, RESET_H = 9, RESET_X = PANEL_X + PANEL_W - 5 - RESET_W, RESET_Y = 110;
    /** The shaft: centre, half-width, top and bottom of the rock. */
    private static final int SHAFT_X = FIELD_X + FIELD_W / 2, SHAFT_TOP = 44, SHAFT_BOTTOM = 112;

    private final VoidMinerMachine machine;

    // last state sent to / received by the client
    private int status = -1;
    private int tier;
    private int progress;
    private int duration;
    private boolean workingEnabled;
    private long available;
    private long needed;
    private long energyPerRun;
    private long runs;
    /** The recipe shown: "id*count" per item, inputs and outputs. */
    private String recipeIn = "";
    private String recipeOut = "";

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
        int[] now = { machine.getStatus(), machine.isFormed() ? machine.getTier() : -1,
                logic.isWorking() ? logic.getProgress() : 0, logic.isWorking() ? logic.getDuration() : 0,
                machine.isWorkingEnabled() ? 1 : 0 };
        int[] before = { status, tier, progress, duration, workingEnabled ? 1 : 0 };
        long[] nowLong = { machine.getAvailableEUt(), machine.getNeededEUt(), machine.getEnergyPerRun(),
                machine.getRuns() };
        long[] beforeLong = { available, needed, energyPerRun, runs };
        String newIn = machine.shownRecipeItems(true);
        String newOut = machine.shownRecipeItems(false);
        boolean changed = !Arrays.equals(now, before) || !Arrays.equals(nowLong, beforeLong) ||
                !Objects.equals(newIn, recipeIn) || !Objects.equals(newOut, recipeOut);
        status = now[0];
        tier = now[1];
        progress = now[2];
        duration = now[3];
        workingEnabled = now[4] != 0;
        available = nowLong[0];
        needed = nowLong[1];
        energyPerRun = nowLong[2];
        runs = nowLong[3];
        recipeIn = newIn;
        recipeOut = newOut;
        return changed;
    }

    @Override
    protected void writeState(FriendlyByteBuf buffer) {
        for (int value : new int[] { status, tier, progress, duration }) buffer.writeVarInt(value);
        buffer.writeBoolean(workingEnabled);
        for (long value : new long[] { available, needed, energyPerRun, runs }) buffer.writeVarLong(value);
        buffer.writeUtf(recipeIn);
        buffer.writeUtf(recipeOut);
    }

    @Override
    protected void readState(FriendlyByteBuf buffer) {
        status = buffer.readVarInt();
        tier = buffer.readVarInt();
        progress = buffer.readVarInt();
        duration = buffer.readVarInt();
        workingEnabled = buffer.readBoolean();
        available = buffer.readVarLong();
        needed = buffer.readVarLong();
        energyPerRun = buffer.readVarLong();
        runs = buffer.readVarLong();
        recipeIn = buffer.readUtf();
        recipeOut = buffer.readUtf();
    }

    private int modeColor() {
        GTRecipeType[] types = machine.getRecipeTypes();
        return machine.modeColor(0);
    }

    private String modeKey() {
        GTRecipeType[] types = machine.getRecipeTypes();
        return ProcessMachine.modeKey(types[0]);
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

    /** The shaft from above: area tiles, the drill, ore sparks, the state and the run time. */
    @OnlyIn(Dist.CLIENT)
    private void drawField(GuiGraphics graphics, int x, int y, float partialTicks) {
        Font font = font();
        graphics.fill(x, y, x + FIELD_W, y + FIELD_H, 0xFF070A11);
        border(graphics, x, y, FIELD_W, FIELD_H, EDGE);
        for (int gx = x + 12; gx < x + FIELD_W; gx += 16) graphics.fill(gx, y + 1, gx + 1, y + FIELD_H - 1, 0x0CFFFFFF);
        for (int gy = y + 12; gy < y + FIELD_H; gy += 16) graphics.fill(x + 1, gy, x + FIELD_W - 1, gy + 1, 0x0CFFFFFF);

        boolean running = status == STATUS_RUNNING;
        double fraction = duration <= 0 ? 0 : Math.min(1.0, (progress + (running ? partialTicks : 0)) / duration);
        int cx = getPosition().x + SHAFT_X;
        int color = modeColor();
        long now = Util.getMillis();

        // the shaft walls
        for (int sy = SHAFT_TOP; sy <= SHAFT_BOTTOM; sy += 2) {
            graphics.fill(cx - 14, y + sy - FIELD_Y, cx - 13, y + sy - FIELD_Y + 1, EDGE);
            graphics.fill(cx + 13, y + sy - FIELD_Y, cx + 14, y + sy - FIELD_Y + 1, EDGE);
        }
        // the drill, descending while a run is on
        int drill = SHAFT_TOP + (int) Math.round(fraction * (SHAFT_BOTTOM - SHAFT_TOP - 6));
        graphics.fill(cx - 2, y + drill - FIELD_Y, cx + 3, y + drill - FIELD_Y + 5,
                running ? withAlpha(color, 0xE0) : DIM);
        graphics.fill(cx - 1, y + drill - FIELD_Y + 5, cx + 2, y + drill - FIELD_Y + 8,
                running ? color : DIM);
        // ore sparks rising off the rock
        for (int i = 0; i < 14; i++) {
            int sx = cx - 10 + (i * 37 + (int) (now / 90)) % 21 - (running ? (int) ((now / 60 + i * 13) % 9) : 0);
            int sparkY = y + SHAFT_BOTTOM - FIELD_Y - 4 - ((int) (now / 70 + i * 29) % (SHAFT_BOTTOM - SHAFT_TOP - 8));
            boolean lit = running && (i + now / 400) % 3 != 0;
            graphics.fill(sx, sparkY, sx + 1, sparkY + 1, lit ? withAlpha(color, 0xC0) : TRACK);
        }

        // state and run time
        String state = running ? Component.translatable("af9.voidminer.console.mining").getString() + "  " +
                Math.round(fraction * 100) + "%" : Component.translatable("af9.console.status." + status).getString();
        graphics.drawString(font, state, cx - font.width(state) / 2, y + 117, running ? color : statusColor(status),
                false);
        drawRuntime(graphics, x + 8, y + 122, FIELD_W - 16, running ? (int) Math.round(fraction * duration) : 0,
                duration, running, color);
    }

    /** The run: area, recipe items, energy per run, switch, counter, hint. */
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
        drawSmall(graphics, Component.translatable("af9.voidminer.console.recipe").getString(), x + 5, y + 20,
                MUTED, false);
        List<ItemStack> in = stacks(recipeIn), out = stacks(recipeOut);
        if (in.isEmpty() && out.isEmpty()) {
            graphics.drawString(font, Component.translatable("af9.voidminer.console.no_recipe").getString(), x + 5,
                    y + 32, MUTED, false);
        } else {
            int ix = x + 5;
            for (ItemStack stack : in) {
                drawItem(graphics, font, stack, ix, y + 27);
                ix += 18;
            }
            drawSmall(graphics, ">", ix + 3, y + 32, color, false);
            ix += 12;
            for (ItemStack stack : out) {
                drawItem(graphics, font, stack, ix, y + 27);
                ix += 18;
            }
            if (!out.isEmpty()) {
                drawSmall(graphics, fit(out.get(0).getHoverName().getString(), (PANEL_W - 10) * 4 / 3), x + 5,
                        y + 47, TEXT, false);
            }
        }
        drawSmall(graphics, Component.translatable("af9.voidminer.console.energy").getString(), x + 5, y + 59,
                MUTED, false);
        String energyText = energyPerRun > 0 ? compact(energyPerRun) + " EU" : "-";
        drawSmall(graphics, energyText, x + PANEL_W - 5 - font.width(energyText) * 3 / 4, y + 59, TEXT, false);

        // the on/off switch
        int sx = x0 + SWITCH_X, sy = y0 + SWITCH_Y;
        graphics.fill(sx, sy, sx + SWITCH_W, sy + SWITCH_H, workingEnabled ? withAlpha(GOOD, 0x28) : 0xFF0B0F17);
        border(graphics, sx, sy, SWITCH_W, SWITCH_H, workingEnabled ? GOOD : DIM);
        graphics.fill(sx + 5, sy + 5, sx + 11, sy + 11, workingEnabled ? GOOD : DIM);
        String switchText = Component.translatable(workingEnabled ? "af9.orbital.console.online" :
                "af9.orbital.console.offline").getString();
        graphics.drawString(font, switchText, sx + 7 + (SWITCH_W - 7 - font.width(switchText)) / 2, sy + 4,
                workingEnabled ? GOOD : MUTED, false);

        // drilling fluid note
        drawSmall(graphics, Component.translatable("af9.voidminer.console.fluid").getString(), x + 5, y + 97,
                MUTED, false);
        drawSmall(graphics, Component.translatable("af9.voidminer.console.fluid_amount").getString(), x + 5, y + 104,
                needed > 0 ? TEXT : MUTED, false);

        String counter = Component.translatable("af9.voidminer.console.runs", compact(runs)).getString();
        drawSmall(graphics, fit(counter, (x0 + RESET_X - 3 - (x + 5)) * 4 / 3), x + 5, y0 + RESET_Y + 2, MUTED,
                false);
        drawButton(graphics, x0 + RESET_X, y0 + RESET_Y, RESET_W, RESET_H,
                Component.translatable("af9.litho.console.reset").getString());

        // what to do now
        Component hint = Component.translatable("af9.voidminer.hint." + status);
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

    //////////////////////////////////////
    // ******** Side panels *********//
    //////////////////////////////////////

    /**
     * A panel beside the player inventory, drawn from the console's synced state: left the process (area, power,
     * state), right the system (status, power, tier, switch, runs).
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
            graphics.fill(x + 1, y + 1, x + w - 1, y + 2, withAlpha(console.modeColor(), 0xAA));
            drawSmall(graphics, Component.translatable(system ? "af9.orbital.console.system" :
                    "af9.orbital.console.process").getString(), x + 4, y + 5, TEXT, false);
            if (system) drawSystem(graphics, x + 4, y + 16, w - 8);
            else drawProcess(graphics, x + 4, y + 16, w - 8);
            super.drawInBackground(graphics, mouseX, mouseY, partialTicks);
        }

        @OnlyIn(Dist.CLIENT)
        private void drawProcess(GuiGraphics graphics, int x, int y, int w) {
            boolean running = console.status == STATUS_RUNNING;
            row(graphics, x, y + 10, w, "af9.console.power",
                    compact(console.available) + "/" + (console.needed > 0 ? compact(console.needed) : "-"),
                    console.needed <= 0 || console.available >= console.needed ? TEXT : BAD);
            row(graphics, x, y + 20, w, "af9.voidminer.console.state",
                    Component.translatable("af9.console.status." + console.status).getString(),
                    statusColor(console.status));
            // the shaft in the panel: drill light running down while it mines
            long now = Util.getMillis();
            for (int i = 0; i < w; i += 3) {
                boolean lit = running && ((i + now / 40) % 30 < 8);
                graphics.fill(x + i, y + 32, x + i + 2, y + 33, lit ? console.modeColor() : TRACK);
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
            row(graphics, x, y + 53, w, "af9.voidminer.console.runs_label", compact(console.runs), TEXT);
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
