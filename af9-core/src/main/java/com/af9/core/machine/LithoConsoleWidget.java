package com.af9.core.machine;

import com.af9.core.litho.Coolant;
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
    /** Orbital station: coolant of the running / next print (grade, 0 none), -1 on machines without coolant. */
    private int coolant = -1;
    /** Air cooling: cooling units the hatches give / the print's heat load (0: none needed), doublings above it. */
    private int coolCapacity;
    private int coolLoad;
    private int coolSteps;
    private boolean coolLapsed;
    /** Share of the OPC demand the computation meets, percent (-1: no demand or no source). */
    private int opc = -1;
    /** The shown mode's prints are multi-patterned (exposed twice, one version above the machine's own). */
    private boolean multi;
    /** The Array Mk2's beam focus, permille; -1 on machines without one. */
    private int focus = -1;
    /** The focus lock: 0 off, 1 armed, 2 latched. */
    private int focusLock;
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

    public static Component[] tileTooltip(LithoMode mode) {
        Component resist = mode.resist.equals("dry_resist") ?
                Component.translatable("item.af9.dry_resist_cartridge") :
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
        if (!machine.isVacuumSealed()) return machine.notReadyStatus();
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
        boolean airCooled = machine.needsAirCooling(active);
        int newCoolCapacity = airCooled ? machine.getCoolingCapacity() : 0;
        int newCoolLoad = airCooled ? active.heatLoad() : 0;
        int newCoolSteps = machine.coolingSteps(active);
        boolean newCoolLapsed = machine.hasCoolingLapsed() && logic.isWorking();
        double opcRatio = machine.getOpcRatio(active, logic.isWorking());
        int newOpc = opcRatio < 0 ? -1 : (int) Math.round(opcRatio * 100);
        boolean newMulti = machine.isMultiPatterned(active);
        int newFocus = machine.focusPermille();
        int newFocusLock = machine.focusLockState();
        int newCoolant = -1;
        if (machine instanceof OrbitalLithographyMachine station && active.minCoolant() != null) {
            Coolant used = station.currentCoolant(active);
            newCoolant = used == null ? 0 : used.grade();
        }
        boolean changed = newStatus != status || newMode != mode || newVersion != version ||
                newAvailable != available || newProgress != progress || newDuration != duration ||
                newCleanliness != cleanliness || newPrintVacuum != printVacuum || newBreak != breakChance ||
                newPrinted != printed ||
                newBroken != broken || newVacuum != vacuum || newCoolant != coolant ||
                newCoolCapacity != coolCapacity || newCoolLoad != coolLoad || newCoolSteps != coolSteps ||
                newCoolLapsed != coolLapsed || newOpc != opc || newMulti != multi || newFocus != focus || newFocusLock != focusLock ||
                !Objects.equals(newProduct, product);
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
        coolant = newCoolant;
        coolCapacity = newCoolCapacity;
        coolLoad = newCoolLoad;
        coolSteps = newCoolSteps;
        coolLapsed = newCoolLapsed;
        opc = newOpc;
        multi = newMulti;
        focus = newFocus;
        focusLock = newFocusLock;
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
        buffer.writeVarInt(coolant);
        buffer.writeVarInt(coolCapacity);
        buffer.writeVarInt(coolLoad);
        buffer.writeVarInt(coolSteps);
        buffer.writeBoolean(coolLapsed);
        buffer.writeVarInt(opc + 1);
        buffer.writeBoolean(multi);
        buffer.writeVarInt(focus + 1);
        buffer.writeVarInt(focusLock);
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
        coolant = buffer.readVarInt();
        coolCapacity = buffer.readVarInt();
        coolLoad = buffer.readVarInt();
        coolSteps = buffer.readVarInt();
        coolLapsed = buffer.readBoolean();
        opc = buffer.readVarInt() - 1;
        multi = buffer.readBoolean();
        focus = buffer.readVarInt() - 1;
        focusLock = buffer.readVarInt();
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
        } else if (multi || opc >= 0) {
            // multi-patterning and the computation behind the print's OPC
            String quality = (multi ? Component.translatable("af9.litho.console.multipatterned").getString() + "  " : "") +
                    (opc >= 0 ? Component.translatable("af9.litho.console.opc", opc).getString() : "");
            drawSmall(graphics, quality, lx, y0 + 84, MUTED, false);
        }
        // Array Mk2: the beam focus (aligns before the Mk2's own work starts, drifts while it runs)
        if (focus >= 0) {
            int tierColor = focus >= OrbitalLithographyMachine.FOCUS_LOCKED ? GOOD :
                    focus >= OrbitalLithographyMachine.FOCUS_SHARP ? INFO :
                            focus >= OrbitalLithographyMachine.FOCUS_READY ? WARN : BAD;
            String tierKey = focus >= OrbitalLithographyMachine.FOCUS_LOCKED ? "af9.litho.console.focus.locked" :
                    focus >= OrbitalLithographyMachine.FOCUS_SHARP ? "af9.litho.console.focus.sharp" :
                            focus >= OrbitalLithographyMachine.FOCUS_READY ? "af9.litho.console.focus.ready" :
                                    "af9.litho.console.focus.aligning";
            drawSmall(graphics, Component.translatable(focusLock == 0 ? "af9.litho.console.focus" :
                    "af9.litho.console.focus_lock").getString(), lx, y0 + 90, MUTED, false);
            if (focusLock == 2) tierKey = "af9.litho.console.focus.latched";
            String focusText = Math.round(focus / 10.0) + "%  " + Component.translatable(tierKey).getString();
            drawSmall(graphics, focusText, lx + lw - font.width(focusText) * 3 / 4, y0 + 90, tierColor, false);
            bar(graphics, lx, y0 + 97, lw, 3, focus / (double) OrbitalLithographyMachine.FOCUS_MAX, tierColor);
        }
        // Line and Scanner: the air conditioning against the print's heat load
        if (coolLoad > 0) {
            drawSmall(graphics, Component.translatable("af9.litho.console.cooling").getString(), lx, y0 + 92, MUTED,
                    false);
            String text;
            int color;
            if (coolLapsed) {
                text = Component.translatable("af9.litho.console.cooling_lapsed").getString();
                color = BAD;
            } else {
                text = coolCapacity + "/" + coolLoad + " CU" + (coolSteps > 0 ? " +" + coolSteps : "");
                color = coolCapacity < coolLoad ? BAD : coolSteps > 0 ? GOOD : TEXT;
            }
            drawSmall(graphics, text, lx + lw - font.width(text) * 3 / 4, y0 + 92, color, false);
        }
        // orbital station: the coolant of the running (or next) print and how many grades it gains
        if (coolant >= 0 && active.minCoolant() != null) {
            drawSmall(graphics, Component.translatable("af9.litho.console.coolant").getString(), lx, y0 + 92, MUTED,
                    false);
            String text;
            int color;
            if (coolant == 0) {
                text = Component.translatable("af9.litho.console.coolant_none",
                        Component.translatable("af9.litho.coolant." + active.minCoolant().id)).getString();
                color = BAD;
            } else {
                Coolant used = Coolant.values()[coolant - 1];
                int steps = used.steps(active);
                text = Component.translatable("af9.litho.coolant." + used.id).getString() +
                        (steps > 0 ? " +" + steps : "");
                color = steps > 0 ? GOOD : TEXT;
            }
            drawSmall(graphics, text, lx + lw - font.width(text) * 3 / 4, y0 + 92, color, false);
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
