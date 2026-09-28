package com.af9.core.client.render;

import com.af9.core.bus.BusData;
import com.af9.core.bus.BusNetwork;
import com.af9.core.bus.BusScreenLayout;
import com.af9.core.bus.MachineBusModule;
import com.af9.core.machine.console.BusConsole;
import com.af9.core.machine.console.BusConsoles;

import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.client.renderer.monitor.IMonitorRenderer;
import com.gregtechceu.gtceu.common.machine.multiblock.electric.CentralMonitorMachine;
import com.gregtechceu.gtceu.common.machine.multiblock.electric.monitor.MonitorGroup;

import com.lowdragmc.lowdraglib.gui.widget.Widget;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.registries.ForgeRegistries;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The Machine Bus Module's screen on the Central Monitor ({@link MachineBusModule}): one machine in detail or a table of
 * the bus, laid out by {@link BusScreenLayout}. Draws from the module's NBT; the progress bar runs on from the last
 * snapshot while the machine works.
 */
@OnlyIn(Dist.CLIENT)
@SuppressWarnings("removal") // new ResourceLocation(String) is the only parser on 1.20.1
public class MachineBusRenderer implements IMonitorRenderer {

    private static final int BACKGROUND = 0xF20E151C, EDGE = 0xFF2C3A48, TEXT = 0xFFFFFFFF, DIM = 0xFF8A96A3,
            BAR = 0xFF1C2630, BUTTON = 0xFF263442, GREEN = 0xFF2F7A3E, RED = 0xFF7A2F2F, WORK = 0xFF40C060,
            SELECTED_ROW = 0x402F6DB5, ALT_ROW = 0x14FFFFFF, WARN = 0xFFE05050;
    /** Depths in blocks (GT puts the screen 0.01 in front of the monitors): background, fills, text at 0. */
    private static final float Z_BACK = -0.006f, Z_FILL = -0.003f;
    /** Console view: depth between two of the console's drawing calls, in blocks. */
    private static final float CONSOLE_STEP = 0.0003f;

    private final CompoundTag tag;

    public MachineBusRenderer(CompoundTag tag) {
        this.tag = tag;
    }

    @Override
    public void render(CentralMonitorMachine machine, MonitorGroup group, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight, int packedOverlay) {
        BusScreenLayout.Screen screen = BusScreenLayout.screen(machine, group);
        if (screen == null) return;
        float units = BusScreenLayout.unitsPerBlock(screen, tag);
        float w = screen.cols() * units, h = screen.rows() * units;
        poseStack.pushPose();
        poseStack.translate(screen.minX(), screen.minY(), 0);
        poseStack.scale(1 / units, 1 / units, 1 / units);
        Canvas c = new Canvas(poseStack, buffer, units, partialTick);
        c.rect(0, 0, w, h, BACKGROUND, Z_BACK);
        c.frame(0, 0, w, h, EDGE);

        ListTag devices = BusScreenLayout.devices(tag);
        int port = tag.getInt(MachineBusModule.PORT);
        if (port == MachineBusModule.PORT_NONE || devices.isEmpty()) {
            c.center(Component.translatable(port == MachineBusModule.PORT_NONE ? "af9.bus.screen.no_port" :
                    "af9.bus.screen.no_machines"), w / 2, h / 2 - 4, DIM);
        } else if (BusScreenLayout.view(tag) == BusScreenLayout.VIEW_TABLE) {
            drawTable(c, devices, w, h);
        } else if (BusScreenLayout.view(tag) == BusScreenLayout.VIEW_CONSOLE) {
            drawConsole(c, poseStack, buffer, units, w, h, partialTick);
        } else {
            drawDetail(c, BusScreenLayout.selected(tag), devices.size(), w, h);
        }
        poseStack.popPose();
    }

    //////////////////////////////////////
    // ************ Views **************//
    //////////////////////////////////////

    private void drawDetail(Canvas c, CompoundTag d, int count, float w, float h) {
        float pad = BusScreenLayout.PAD;
        int shown = MachineBusModule.shown(tag) & d.getInt(BusData.SHARED);
        boolean formed = d.getBoolean(BusData.FORMED);

        // header: status light, name, mode chip
        c.rect(pad, pad + 1, 7, 7, statusColor(d, shown), Z_FILL);
        float chipW = 0;
        if ((shown & BusData.RECIPE) != 0) chipW = drawChip(c, d, w - pad, pad - 1, true);
        c.text(c.fit(MachineBusModule.deviceName(d).getString(), w - 2 * pad - 11 - chipW - 4), pad + 11, pad, TEXT);
        c.rect(pad, pad + 11, w - 2 * pad, 1, EDGE, Z_FILL);

        // status line: state, version, batch
        StringBuilder status = new StringBuilder();
        if ((shown & BusData.STATUS) != 0) status.append(statusText(d).getString());
        if ((shown & BusData.SETTINGS) != 0) {
            if (d.contains(BusData.VERSION)) {
                if (!status.isEmpty()) status.append("  ·  ");
                status.append(Component.translatable("af9.bus.screen.version", d.getInt(BusData.VERSION),
                        d.getInt(BusData.MAX_VERSION), factor(d.getDouble(BusData.SPEED))).getString());
            }
            if (d.getBoolean(BusData.BATCH)) {
                if (!status.isEmpty()) status.append("  ·  ");
                status.append(Component.translatable("af9.bus.screen.batch").getString());
            }
        }
        boolean overloaded = tag.getBoolean(MachineBusModule.OVERLOADED);
        if (overloaded) {
            status.insert(0, Component.translatable("af9.bus.screen.overloaded").getString() +
                    (status.isEmpty() ? "" : "  ·  "));
        }
        c.text(c.fit(status.toString(), w - 2 * pad), pad, BusScreenLayout.STATUS_Y,
                overloaded ? WARN : formed ? DIM : RED);

        // progress
        if ((shown & BusData.PROGRESS) != 0 && formed) {
            float barW = w - 2 * pad;
            float y = BusScreenLayout.BAR_Y;
            c.rect(pad, y, barW, BusScreenLayout.BAR_H, BAR, Z_FILL);
            int max = d.getInt(BusData.MAX_PROGRESS);
            boolean running = max > 0 && d.getInt(BusData.STATUS_ID) == BusData.STATE_WORKING &&
                    d.getBoolean(BusData.ENABLED);
            float progress = max > 0 ? c.progress(d, running) : 0;
            int color = d.contains(BusData.MODE_COLOR) ? d.getInt(BusData.MODE_COLOR) | 0xFF000000 : WORK;
            if (progress > 0) c.rect(pad, y, barW * progress / max, BusScreenLayout.BAR_H,
                    running ? color : dim(color), Z_FILL * 0.5f);
            String label = max <= 0 ? Component.translatable("af9.bus.screen.no_run").getString() :
                    String.format(Locale.ROOT, "%d %%   %s", (int) (100 * progress / max),
                            seconds((max - progress) / 20f));
            c.center(Component.literal(label), w / 2, y + 2, TEXT);
        }

        // stat lines
        float buttonsY = h - BusScreenLayout.PAD - BusScreenLayout.BUTTON_H;
        float y = BusScreenLayout.STATS_Y;
        for (Component[] line : statLines(d, shown)) {
            if (y + BusScreenLayout.LINE_H > buttonsY - 2) break;
            float x = pad;
            for (int i = 0; i < line.length; i += 2) {
                c.text(line[i].getString(), x, y, DIM);
                float lx = x + c.width(line[i].getString()) + 3;
                c.text(line[i + 1].getString(), lx, y, TEXT);
                x = Math.max(w / 2, lx + c.width(line[i + 1].getString()) + 8);
            }
            y += BusScreenLayout.LINE_H;
        }

        // touch buttons
        for (BusScreenLayout.Button button : BusScreenLayout.buttons(tag, w, h)) {
            BusScreenLayout.Rect r = button.rect();
            int bg = BUTTON;
            String text;
            switch (button.id()) {
                case "power" -> {
                    boolean on = d.getBoolean(BusData.ENABLED);
                    bg = on ? RED : GREEN;
                    text = Component.translatable(on ? "af9.bus.button.stop" : "af9.bus.button.start").getString();
                }
                case "batch" -> {
                    bg = d.getBoolean(BusData.BATCH) ? GREEN : BUTTON;
                    text = Component.translatable("af9.bus.button.batch").getString();
                }
                case "mode_prev" -> text = "◀";
                case "mode_next" -> text = r.w() > 20 ? modeLabel(d) : "▶";
                case "list" -> text = Component.translatable("af9.bus.button.list").getString();
                default -> text = "";
            }
            if (button.id().equals("mode_next") && r.w() > 20 && d.contains(BusData.MODE_COLOR)) {
                bg = dim(d.getInt(BusData.MODE_COLOR) | 0xFF000000);
            }
            c.rect(r.x(), r.y(), r.w(), r.h(), bg, Z_FILL);
            c.frame(r.x(), r.y(), r.w(), r.h(), EDGE);
            c.center(Component.literal(c.fit(text, r.w() - 2)), r.x() + r.w() / 2, r.y() + 4, TEXT);
        }
        if (count > 1 && BusScreenLayout.buttons(tag, w, h).isEmpty()) {
            c.text(Component.translatable("af9.bus.screen.more", count - 1).getString(), pad, buttonsY + 4, DIM);
        }
    }

    /**
     * The selected machine's console, as on its own screen, centred: the client's copy of the console (made for the
     * client's copy of the machine, which must be loaded) with the state the module brought ({@link BusConsole}).
     */
    private void drawConsole(Canvas c, PoseStack poseStack, MultiBufferSource buffer, float units, float w, float h,
                             float partialTick) {
        Level level = Minecraft.getInstance().level;
        MetaMachine machine = level == null ? null :
                MetaMachine.getMachine(level, BlockPos.of(tag.getLong(MachineBusModule.CONSOLE_POS)));
        Widget console = machine == null ? null : ClientConsoles.get(machine, tag.getByteArray(MachineBusModule.CONSOLE));
        if (console == null) {
            c.center(Component.translatable("af9.bus.screen.console_unloaded"), w / 2, h / 2 - 4, DIM);
            return;
        }
        poseStack.pushPose();
        poseStack.translate((w - console.getSize().width) / 2f, (h - console.getSize().height) / 2f, Z_FILL * units);
        WorldGuiGraphics graphics = new WorldGuiGraphics(poseStack.last().pose(), buffer, CONSOLE_STEP * units);
        console.drawInBackground(graphics, Integer.MIN_VALUE / 2, Integer.MIN_VALUE / 2, partialTick);
        poseStack.popPose();
    }

    /** The consoles the monitors draw, one per machine, fed the state each time the module brings a new one. */
    private static final class ClientConsoles {

        private record Entry(MetaMachine machine, Widget console, byte[] applied) {}

        private static final Map<Long, Entry> CONSOLES = new HashMap<>();

        /** The machine's console with the state; null if it has none or the state does not read. */
        static Widget get(MetaMachine machine, byte[] state) {
            if (state.length == 0) return null;
            long key = machine.getPos().asLong();
            Entry entry = CONSOLES.get(key);
            if (entry == null || entry.machine() != machine) {
                Widget created = BusConsoles.create(machine);
                if (!(created instanceof BusConsole)) return null;
                if (CONSOLES.size() >= 32) CONSOLES.clear();
                entry = new Entry(machine, created, null);
            }
            if (entry.applied() != state) {
                try {
                    ((BusConsole) entry.console()).applySnapshot(state);
                } catch (RuntimeException e) {
                    CONSOLES.remove(key);
                    return null;
                }
                entry = new Entry(machine, entry.console(), state);
            }
            CONSOLES.put(key, entry);
            return entry.console();
        }
    }

    private void drawTable(Canvas c, ListTag devices, float w, float h) {
        float pad = BusScreenLayout.PAD;
        boolean overloaded = tag.getBoolean(MachineBusModule.OVERLOADED);
        c.text(Component.translatable(overloaded ? "af9.bus.screen.table_overloaded" : "af9.bus.screen.table",
                devices.size(), BusNetwork.MAX_MACHINES).getString(), pad, 5, overloaded ? WARN : TEXT);
        c.rect(pad, BusScreenLayout.HEADER_H - 3, w - 2 * pad, 1, EDGE, Z_FILL);
        int rows = Math.min(devices.size(), BusScreenLayout.tableRows(h));
        long selected = tag.getLong(MachineBusModule.SELECTED);
        for (int i = 0; i < rows; i++) {
            CompoundTag d = devices.getCompound(i);
            int shown = MachineBusModule.shown(tag) & d.getInt(BusData.SHARED);
            BusScreenLayout.Rect r = BusScreenLayout.row(i, w);
            if (d.getLong(BusData.POS) == selected) c.rect(r.x(), r.y(), r.w(), r.h(), SELECTED_ROW, Z_FILL);
            else if (i % 2 == 1) c.rect(r.x(), r.y(), r.w(), r.h(), ALT_ROW, Z_FILL);
            float ty = r.y() + 3;
            c.rect(r.x() + 2, ty + 1, 6, 6, statusColor(d, shown), Z_FILL * 0.5f);
            float nameEnd = w * 0.44f;
            c.text(c.fit(MachineBusModule.deviceName(d).getString(), nameEnd - r.x() - 14), r.x() + 12, ty, TEXT);
            if ((shown & BusData.RECIPE) != 0) drawChip(c, d, w * 0.46f, ty - 1, false);
            float barX = w * 0.64f, barEnd = w - pad - 34;
            if ((shown & BusData.PROGRESS) != 0 && d.getBoolean(BusData.FORMED) && barEnd > barX) {
                c.rect(barX, ty + 1, barEnd - barX, 6, BAR, Z_FILL * 0.5f);
                int max = d.getInt(BusData.MAX_PROGRESS);
                boolean running = max > 0 && d.getInt(BusData.STATUS_ID) == BusData.STATE_WORKING &&
                        d.getBoolean(BusData.ENABLED);
                float progress = max > 0 ? c.progress(d, running) : 0;
                int color = d.contains(BusData.MODE_COLOR) ? d.getInt(BusData.MODE_COLOR) | 0xFF000000 : WORK;
                if (progress > 0) c.rect(barX, ty + 1, (barEnd - barX) * progress / max, 6,
                        running ? color : dim(color), Z_FILL * 0.25f);
            }
            if ((shown & BusData.PRODUCTION) != 0) {
                String count = compact(d.contains(BusData.PRINTED) ? d.getLong(BusData.PRINTED) :
                        d.getLong(BusData.RUNS));
                c.text(count, w - pad - 2 - c.width(count), ty, DIM);
            }
        }
        if (devices.size() > rows && rows > 0) {
            c.text(Component.translatable("af9.bus.screen.more", devices.size() - rows).getString(), pad,
                    BusScreenLayout.HEADER_H + rows * BusScreenLayout.ROW_H + 2, DIM);
        }
    }

    /** The mode (lithography node) or recipe type chip; right-aligned at x if {@code alignRight}. Its width. */
    private float drawChip(Canvas c, CompoundTag d, float x, float y, boolean alignRight) {
        String text = modeLabel(d);
        if (text.isEmpty()) return 0;
        float cw = c.width(text) + 6;
        float cx = alignRight ? x - cw : x;
        int color = d.contains(BusData.MODE_COLOR) ? dim(d.getInt(BusData.MODE_COLOR) | 0xFF000000) : BUTTON;
        c.rect(cx, y, cw, 10, color, Z_FILL);
        c.text(text, cx + 3, y + 1, TEXT);
        return cw;
    }

    private static String modeLabel(CompoundTag d) {
        if (d.contains(BusData.MODE)) return d.getString(BusData.MODE).replace("nm", " nm");
        if (d.contains(BusData.RECIPE_KEY)) return Component.translatable(d.getString(BusData.RECIPE_KEY)).getString();
        return "";
    }

    private static Component[][] statLines(CompoundTag d, int shown) {
        List<Component[]> lines = new ArrayList<>();
        if ((shown & BusData.ENERGY) != 0 && d.contains(BusData.CAPACITY)) {
            lines.add(new Component[] { Component.translatable("af9.bus.screen.energy"),
                    Component.literal(compact(d.getLong(BusData.STORED)) + " / " + compact(d.getLong(BusData.CAPACITY)) +
                            " EU"),
                    Component.translatable("af9.bus.screen.draw"),
                    Component.literal(compact(d.getLong(BusData.EUT)) + " EU/t") });
        }
        if ((shown & BusData.RECIPE) != 0 && d.contains(BusData.PRODUCT)) {
            Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(d.getString(BusData.PRODUCT)));
            if (item != null) lines.add(new Component[] { Component.translatable("af9.bus.screen.making"),
                    item.getDescription() });
        }
        if ((shown & BusData.PRODUCTION) != 0) {
            if (d.contains(BusData.PRINTED)) {
                lines.add(new Component[] { Component.translatable("af9.bus.screen.printed"),
                        Component.literal(compact(d.getLong(BusData.PRINTED))),
                        Component.translatable("af9.bus.screen.broken"),
                        Component.literal(compact(d.getLong(BusData.BROKEN))) });
            } else {
                lines.add(new Component[] { Component.translatable("af9.bus.screen.runs"),
                        Component.literal(compact(d.getLong(BusData.RUNS))) });
            }
        }
        if ((shown & BusData.PROCESS) != 0) {
            if (d.contains(BusData.CLEANLINESS)) {
                lines.add(new Component[] { Component.translatable("af9.bus.screen.vacuum"),
                        Component.translatable("af9.bus.screen.vacuum_value",
                                String.format(Locale.ROOT, "%.1f", d.getDouble(BusData.CLEANLINESS)),
                                Component.translatable("af9.bus.screen.vacuum." + d.getInt(BusData.VACUUM))),
                        Component.translatable("af9.bus.screen.break"),
                        Component.literal(String.format(Locale.ROOT, "%.1f %%", 100 * d.getDouble(BusData.BREAK))) });
            } else if (d.contains(BusData.BEAM_GEV)) {
                lines.add(new Component[] { Component.translatable("af9.bus.screen.beam"),
                        Component.literal(String.format(Locale.ROOT, "%.0f GeV", d.getDouble(BusData.BEAM_GEV))) });
            } else if (d.contains(BusData.ARRAY_CWUT)) {
                lines.add(new Component[] { Component.translatable("af9.bus.screen.computation"),
                        Component.literal(d.getInt(BusData.ARRAY_CWUT) + " / " + d.getInt(BusData.ARRAY_RAW) +
                                " CWU/t"),
                        Component.translatable("af9.bus.screen.cooling"),
                        Component.literal(String.format(Locale.ROOT, "%.0f %%",
                                100 * d.getDouble(BusData.ARRAY_COOLING))) });
                lines.add(new Component[] { Component.translatable("af9.bus.screen.racks"),
                        Component.literal(String.valueOf(d.getInt(BusData.RACKS))),
                        Component.translatable("af9.bus.screen.heat"),
                        Component.literal(d.getInt(BusData.ARRAY_HEAT) + "/t") });
            }
        }
        return lines.toArray(new Component[0][]);
    }

    //////////////////////////////////////
    // ************ Values *************//
    //////////////////////////////////////

    private static int statusColor(CompoundTag d, int shown) {
        if (!d.getBoolean(BusData.FORMED)) return 0xFF505A64;
        if ((shown & BusData.STATUS) == 0) return 0xFF505A64;
        if (!d.getBoolean(BusData.ENABLED)) return 0xFFE05050;
        return switch (d.getInt(BusData.STATUS_ID)) {
            case BusData.STATE_WORKING -> 0xFF40E060;
            case BusData.STATE_WAITING -> 0xFFFFB020;
            case BusData.STATE_SUSPEND -> 0xFFE05050;
            default -> 0xFF8A96A3;
        };
    }

    private static Component statusText(CompoundTag d) {
        if (!d.getBoolean(BusData.FORMED)) return Component.translatable("af9.bus.screen.unformed");
        if (!d.getBoolean(BusData.ENABLED)) return Component.translatable("af9.bus.screen.paused");
        return Component.translatable("af9.bus.screen.status." + d.getInt(BusData.STATUS_ID));
    }

    private static int dim(int argb) {
        int r = ((argb >> 16) & 255) * 5 / 10, g = ((argb >> 8) & 255) * 5 / 10, b = (argb & 255) * 5 / 10;
        return 0xFF000000 | r << 16 | g << 8 | b;
    }

    private static String factor(double factor) {
        return String.format(Locale.ROOT, "×%.2f", factor);
    }

    private static String seconds(float seconds) {
        if (seconds >= 60) return String.format(Locale.ROOT, "%d:%02d", (int) seconds / 60, (int) seconds % 60);
        return String.format(Locale.ROOT, "%.1f s", Math.max(0, seconds));
    }

    /** 1,234 / 12.3k / 1.23M / 4.56G / 7.89T. */
    static String compact(long value) {
        long v = Math.abs(value);
        if (v < 10_000) return String.format(Locale.ROOT, "%,d", value);
        String[] units = { "k", "M", "G", "T", "P" };
        double d = value;
        int unit = -1;
        while (Math.abs(d) >= 1000 && unit < units.length - 1) {
            d /= 1000;
            unit++;
        }
        return String.format(Locale.ROOT, Math.abs(d) >= 100 ? "%.0f%s" : Math.abs(d) >= 10 ? "%.1f%s" : "%.2f%s",
                d, units[unit]);
    }

    //////////////////////////////////////
    // ************ Drawing ************//
    //////////////////////////////////////

    /** Rectangles and text on the canvas (units; y down). */
    private final class Canvas {

        private final PoseStack pose;
        private final MultiBufferSource buffer;
        private final float units;
        private final float partialTick;
        private final Font font = Minecraft.getInstance().font;

        Canvas(PoseStack pose, MultiBufferSource buffer, float units, float partialTick) {
            this.pose = pose;
            this.buffer = buffer;
            this.units = units;
            this.partialTick = partialTick;
        }

        /** A filled rectangle at a depth in blocks; both windings, so it shows whatever the face culling. */
        void rect(float x, float y, float w, float h, int argb, float zBlocks) {
            if (w <= 0 || h <= 0) return;
            VertexConsumer vc = buffer.getBuffer(RenderType.textBackground());
            Matrix4f m = pose.last().pose();
            float z = zBlocks * units;
            int a = argb >>> 24, r = (argb >> 16) & 255, g = (argb >> 8) & 255, b = argb & 255;
            int light = LightTexture.FULL_BRIGHT;
            vc.vertex(m, x, y, z).color(r, g, b, a).uv2(light).endVertex();
            vc.vertex(m, x, y + h, z).color(r, g, b, a).uv2(light).endVertex();
            vc.vertex(m, x + w, y + h, z).color(r, g, b, a).uv2(light).endVertex();
            vc.vertex(m, x + w, y, z).color(r, g, b, a).uv2(light).endVertex();
            vc.vertex(m, x, y, z).color(r, g, b, a).uv2(light).endVertex();
            vc.vertex(m, x + w, y, z).color(r, g, b, a).uv2(light).endVertex();
            vc.vertex(m, x + w, y + h, z).color(r, g, b, a).uv2(light).endVertex();
            vc.vertex(m, x, y + h, z).color(r, g, b, a).uv2(light).endVertex();
        }

        void frame(float x, float y, float w, float h, int argb) {
            rect(x, y, w, 1, argb, Z_FILL);
            rect(x, y + h - 1, w, 1, argb, Z_FILL);
            rect(x, y, 1, h, argb, Z_FILL);
            rect(x + w - 1, y, 1, h, argb, Z_FILL);
        }

        void text(String text, float x, float y, int argb) {
            font.drawInBatch(text, x, y, argb, false, pose.last().pose(), buffer, Font.DisplayMode.NORMAL, 0,
                    LightTexture.FULL_BRIGHT);
        }

        void center(Component text, float cx, float y, int argb) {
            String s = text.getString();
            text(s, cx - width(s) / 2f, y, argb);
        }

        float width(String text) {
            return font.width(text);
        }

        /** The text cut to a width, with an ellipsis when cut. */
        String fit(String text, float maxWidth) {
            if (maxWidth <= 0) return "";
            if (font.width(text) <= maxWidth) return text;
            return font.plainSubstrByWidth(text, (int) Math.max(0, maxWidth - font.width("…"))) + "…";
        }

        /** Progress now: the snapshot's, run on since it was taken while the machine works (a new run wraps). */
        float progress(CompoundTag d, boolean running) {
            int max = d.getInt(BusData.MAX_PROGRESS);
            float progress = d.getInt(BusData.PROGRESS_TICKS);
            if (running && Minecraft.getInstance().level != null) {
                float elapsed = Minecraft.getInstance().level.getGameTime() - tag.getLong(MachineBusModule.TIME) +
                        partialTick;
                progress = (progress + Math.max(0, elapsed)) % max;
            }
            return Math.min(max, Math.max(0, progress));
        }
    }
}
