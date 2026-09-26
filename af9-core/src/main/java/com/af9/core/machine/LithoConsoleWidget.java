package com.af9.core.machine;

import com.af9.core.litho.LithoMode;
import com.af9.core.machine.console.ConsoleWidget;

import com.lowdragmc.lowdraglib.gui.texture.ColorBorderTexture;
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.widget.ButtonWidget;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Console of the lithography machines (line, scanner, orbital station): the power gauge in the header next to the
 * status, mode tiles as a status indicator (the active mode lit, modes above the machine's version locked; the mode
 * is switched in GT's side tab), the vacuum's cleanliness bar and pump state, the break chance, what is being printed,
 * the run-time bar and the print counters.
 */
public class LithoConsoleWidget extends ConsoleWidget {

    public static final int WIDTH = 230;
    public static final int HEIGHT = 144;
    public static final int TILE_Y = 18;
    public static final int TILE_H = 22;
    public static final int TILE_GAP = 2;
    public static final int RESET_W = 36;
    public static final int RESET_H = 9;
    public static final int RESET_X = WIDTH - 4 - RESET_W;
    public static final int RESET_Y = 132;

    private final LithoMachine machine;

    // last state sent to / received by the client
    private int status = -1;
    private int mode;
    private int version;
    private long available;
    private int progress;
    private int duration;
    private int cleanliness; // x10
    private int printVacuum; // x10: lowest vacuum of the running print
    private int breakChance; // x10000
    private long printed;
    private long broken;
    private int vacuum;
    private String product = "";

    public LithoConsoleWidget(LithoMachine machine, int x, int y) {
        super(x, y, WIDTH, HEIGHT);
        this.machine = machine;
    }

    /** The console, a hover area per mode tile (tooltip only: the tiles show the mode) and the counter reset. */
    public static WidgetGroup create(LithoMachine machine) {
        var group = new WidgetGroup(0, 0, WIDTH, HEIGHT);
        group.addWidget(new LithoConsoleWidget(machine, 0, 0));
        List<LithoMode> modes = machine.getModes();
        for (int i = 0; i < modes.size(); i++) {
            var tile = new Widget(tileX(i, modes.size()), TILE_Y, tileWidth(modes.size()), TILE_H);
            tile.setHoverTooltips(tileTooltip(modes.get(i)));
            group.addWidget(tile);
        }
        var reset = new ButtonWidget(RESET_X, RESET_Y, RESET_W, RESET_H, IGuiTexture.EMPTY, click -> {
            if (!click.isRemote) machine.resetCounters();
        });
        reset.setHoverTexture(new ColorBorderTexture(1, 0xFFFFFFFF));
        reset.setHoverTooltips(Component.translatable("af9.litho.console.reset_tooltip"));
        group.addWidget(reset);
        return group;
    }

    public static int tileWidth(int count) {
        return (WIDTH - 8 - TILE_GAP * (count - 1)) / count;
    }

    public static int tileX(int index, int count) {
        return 4 + index * (tileWidth(count) + TILE_GAP);
    }

    private static Component[] tileTooltip(LithoMode mode) {
        Component resist = mode.resist.equals("dry_resist") ?
                Component.translatable("item.kubejs.dry_resist_cartridge") :
                Component.translatable("material.gtceu." + mode.resist);
        Component level = switch (mode.machine) {
            case ORBITAL -> Component.translatable("af9.litho.console.tile_orbit");
            case SCANNER -> Component.translatable("af9.litho.console.tile_version_scanner", mode.level());
            default -> Component.translatable("af9.litho.console.tile_version", mode.level());
        };
        return new Component[] {
                Component.translatable("af9.litho.mode." + mode.id).withStyle(mode.color),
                level,
                Component.translatable("af9.litho.console.tile_power",
                        Component.translatable("af9.litho.hatch." + mode.hatchTier), mode.amperage()),
                Component.translatable("af9.litho.console.tile_substrate",
                        Component.translatable("af9.litho.substrate." + mode.substrate)),
                Component.translatable("af9.litho.console.tile_light",
                        Component.translatable("af9.litho.light." + mode.light + ".long")),
                Component.translatable("af9.litho.console.tile_optics",
                        String.format(Locale.ROOT, "%.2f", mode.numericalAperture),
                        String.format(Locale.ROOT, "%.2f", mode.k1())),
                Component.translatable("af9.litho.console.tile_resist", resist),
                Component.translatable("af9.litho.console.tile_break",
                        LithoMode.formatPercent(mode.baseBreak / 10000.0),
                        LithoMode.formatPercent(mode.baseBreak / 10000.0 + LithoMode.DIRT_BREAK)) };
    }

    //////////////////////////////////////
    // *********** Sync ***********//
    //////////////////////////////////////

    /** Console status of a lithography machine (also used by the Jade tooltip). */
    public static int statusOf(LithoMachine machine) {
        if (!machine.isFormed()) return STATUS_OFFLINE;
        if (machine.hasMaintenanceProblems()) return STATUS_MAINTENANCE;
        var logic = machine.getRecipeLogic();
        if (!logic.isWorkingEnabled()) return STATUS_PAUSED;
        if (logic.isWorking()) return STATUS_RUNNING;
        LithoMode active = machine.getActiveMode();
        int blocked = machine.blockedStatus(active);
        if (blocked >= 0) return blocked;
        if (logic.isWaiting() || machine.getAvailableEUt() < active.eut()) return STATUS_NO_POWER;
        if (!machine.isVacuumSealed()) return STATUS_PUMPING_DOWN;
        return STATUS_IDLE;
    }

    @Override
    protected boolean sample() {
        LithoMode active = machine.getActiveMode();
        var logic = machine.getRecipeLogic();
        ResourceLocation current = machine.getCurrentProduct();
        int newStatus = statusOf(machine);
        int newMode = active.ordinal();
        int newVersion = machine.getVersion();
        long newAvailable = machine.getAvailableEUt();
        int newProgress = logic.isWorking() ? logic.getProgress() : 0;
        int newDuration = logic.isWorking() ? logic.getDuration() : 0;
        int newCleanliness = (int) Math.round(machine.getCleanliness() * 10);
        int newPrintVacuum = (int) Math.round(machine.getPrintVacuum() * 10);
        int newBreak = (int) Math.round(machine.currentBreakChance(active) * 10000);
        long newPrinted = machine.getPrinted();
        long newBroken = machine.getBroken();
        int newVacuum = machine.getVacuumState();
        String newProduct = current == null ? "" : current.toString();
        boolean changed = newStatus != status || newMode != mode || newVersion != version ||
                newAvailable != available || newProgress != progress || newDuration != duration ||
                newCleanliness != cleanliness || newPrintVacuum != printVacuum || newBreak != breakChance || newPrinted != printed ||
                newBroken != broken || newVacuum != vacuum || !Objects.equals(newProduct, product);
        status = newStatus;
        mode = newMode;
        version = newVersion;
        available = newAvailable;
        progress = newProgress;
        duration = newDuration;
        cleanliness = newCleanliness;
        printVacuum = newPrintVacuum;
        breakChance = newBreak;
        printed = newPrinted;
        broken = newBroken;
        vacuum = newVacuum;
        product = newProduct;
        return changed;
    }

    @Override
    protected void writeState(FriendlyByteBuf buffer) {
        buffer.writeVarInt(status);
        buffer.writeVarInt(mode);
        buffer.writeVarInt(version);
        buffer.writeVarLong(available);
        buffer.writeVarInt(progress);
        buffer.writeVarInt(duration);
        buffer.writeVarInt(cleanliness);
        buffer.writeVarInt(printVacuum);
        buffer.writeVarInt(breakChance);
        buffer.writeVarLong(printed);
        buffer.writeVarLong(broken);
        buffer.writeVarInt(vacuum);
        buffer.writeUtf(product);
    }

    @Override
    protected void readState(FriendlyByteBuf buffer) {
        status = buffer.readVarInt();
        mode = buffer.readVarInt();
        version = buffer.readVarInt();
        available = buffer.readVarLong();
        progress = buffer.readVarInt();
        duration = buffer.readVarInt();
        cleanliness = buffer.readVarInt();
        printVacuum = buffer.readVarInt();
        breakChance = buffer.readVarInt();
        printed = buffer.readVarLong();
        broken = buffer.readVarLong();
        vacuum = buffer.readVarInt();
        product = buffer.readUtf();
    }

    /** Console text of a vacuum state ({@link LithoMachine#getVacuumState()}). */
    public static String vacuumKey(int state) {
        return switch (state) {
            case LithoMachine.VACUUM_PUMPING -> "af9.litho.console.pump_on";
            case LithoMachine.VACUUM_SEALED -> "af9.litho.console.pump_sealed";
            case LithoMachine.VACUUM_VENTING -> "af9.litho.console.pump_leak";
            default -> "af9.litho.console.pump_off";
        };
    }

    public static int vacuumColor(int state) {
        return switch (state) {
            case LithoMachine.VACUUM_PUMPING -> INFO;
            case LithoMachine.VACUUM_SEALED -> GOOD;
            case LithoMachine.VACUUM_VENTING -> BAD;
            default -> MUTED;
        };
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
        LithoMode active = LithoMode.values()[Math.max(0, Math.min(mode, LithoMode.values().length - 1))];
        List<LithoMode> modes = machine.getModes();

        String title = Component.translatable(machine.titleKey()).getString();
        String suffix = version > 0 ? "V" + version : "";
        drawFrame(graphics, title, suffix, active.argb, status, active.argb);
        drawHeaderPower(graphics, title, suffix, status, available, active.eut());

        // mode tiles: node on top, substrate symbol (or the version a locked mode needs) below
        for (int i = 0; i < modes.size(); i++) {
            LithoMode tileMode = modes.get(i);
            boolean locked = !tileMode.onOrbitalStation() && tileMode.level() > version;
            boolean powered = status != STATUS_OFFLINE && !locked && available >= tileMode.eut();
            String detail = locked ? "V" + tileMode.level() :
                    Component.translatable("af9.litho.substrate_short." + tileMode.substrate).getString();
            drawTile(graphics, x0 + tileX(i, modes.size()), y0 + TILE_Y, tileWidth(modes.size()), TILE_H,
                    tileMode.nodeNm + "nm", detail, tileMode.argb, tileMode == active, powered, locked);
        }

        // left: vacuum and pump state, break chance (from the lowest vacuum of the running print)
        int lx = x0 + 6;
        int lw = 108;
        double clean = cleanliness / 10.0;
        drawSmall(graphics, Component.translatable("af9.litho.console.vacuum").getString(), lx, y0 + 44, MUTED,
                false);
        String cleanText = String.format(Locale.ROOT, "%.1f / 100", clean);
        drawSmall(graphics, cleanText, lx + lw - font.width(cleanText) * 3 / 4, y0 + 44, levelColor(clean), false);
        bar(graphics, lx, y0 + 51, lw, 7, clean / 100.0, levelColor(clean));
        border(graphics, lx - 1, y0 + 50, lw + 2, 9, EDGE);
        int pumpState = status == STATUS_OFFLINE ? LithoMachine.VACUUM_OFF : vacuum;
        drawSmall(graphics, Component.translatable(vacuumKey(pumpState)).getString(), lx, y0 + 61,
                vacuumColor(pumpState), false);

        double chance = breakChance / 10000.0;
        drawSmall(graphics, Component.translatable("af9.litho.console.break").getString(), lx, y0 + 73, MUTED, false);
        int chanceColor = chance <= 0.05 ? GOOD : chance <= 0.2 ? WARN : BAD;
        String chanceText = LithoMode.formatPercent(chance);
        graphics.drawString(font, chanceText, lx + lw - font.width(chanceText), y0 + 71, chanceColor, false);
        double low = printVacuum / 10.0;
        if (status == STATUS_RUNNING && low < 99.95) {
            drawSmall(graphics, Component.translatable("af9.litho.console.print_vacuum",
                    String.format(Locale.ROOT, "%.1f", low)).getString(), lx, y0 + 84, BAD, false);
        } else if (status == STATUS_PUMPING_DOWN) {
            drawSmall(graphics, Component.translatable("af9.litho.console.wait_seal").getString(), lx, y0 + 84, INFO,
                    false);
        }

        // right: what is printed, substrate, light, version bonus / orbit
        int rx = x0 + 120;
        int rw = WIDTH - 126;
        drawSmall(graphics, Component.translatable("af9.litho.console.output").getString(), rx, y0 + 44, MUTED,
                false);
        ItemStack stack = productStack();
        if (!stack.isEmpty()) {
            graphics.renderItem(stack, rx, y0 + 51);
            graphics.drawString(font, fit(stack.getHoverName().getString(), rw - 20), rx + 19, y0 + 51, TEXT, false);
        } else {
            graphics.drawString(font, Component.translatable("af9.litho.console.nothing").getString(), rx + 19,
                    y0 + 51, MUTED, false);
        }
        drawSmall(graphics, active.nodeNm + " nm  " +
                Component.translatable("af9.litho.light." + active.light).getString(), rx + 19, y0 + 61, active.argb,
                false);
        drawSmall(graphics, Component.translatable("af9.litho.console.substrate").getString(), rx, y0 + 71, MUTED,
                false);
        graphics.drawString(font, fit(Component.translatable("af9.litho.substrate." + active.substrate).getString(),
                rw), rx, y0 + 78, TEXT, false);
        if (active.onOrbitalStation()) {
            boolean orbit = status != STATUS_NO_ORBIT;
            drawSmall(graphics, Component.translatable(orbit ? "af9.litho.console.orbit_ok" :
                    "af9.litho.console.orbit_missing").getString(), rx, y0 + 90, orbit ? GOOD : BAD, false);
        } else {
            int surplus = version - active.level();
            if (surplus < 0) {
                drawSmall(graphics, Component.translatable("af9.litho.console.needs_version", active.level())
                        .getString(), rx, y0 + 90, BAD, false);
            } else {
                drawSmall(graphics, Component.translatable("af9.litho.console.bonus",
                        LithoMode.formatFactor(1 / LithoMode.speedFactor(surplus)),
                        LithoMode.formatFactor(Math.pow(LithoMode.VERSION_BREAK_FACTOR, surplus))).getString(),
                        rx, y0 + 90, surplus > 0 ? GOOD : MUTED, false);
            }
        }

        // run time over the whole width
        drawRuntime(graphics, x0 + 6, y0 + 101, WIDTH - 12, progress, duration, status == STATUS_RUNNING,
                active.argb);

        // counters and reset
        long total = printed + broken;
        String yield = total == 0 ? "-" : Math.round(100.0 * printed / total) + "%";
        drawSmall(graphics, Component.translatable("af9.litho.console.counters", compact(printed), compact(broken),
                yield).getString(), x0 + 6, y0 + RESET_Y + 2, MUTED, false);
        drawButton(graphics, x0 + RESET_X, y0 + RESET_Y, RESET_W, RESET_H,
                Component.translatable("af9.litho.console.reset").getString());
    }

    @OnlyIn(Dist.CLIENT)
    private ItemStack productStack() {
        if (product.isEmpty()) return ItemStack.EMPTY;
        ResourceLocation id = ResourceLocation.tryParse(product);
        Item item = id == null ? null : ForgeRegistries.ITEMS.getValue(id);
        return item == null || item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item);
    }
}
