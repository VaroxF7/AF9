package com.af9.core.compat.jade;

import com.af9.core.machine.DtpfModuleMachine;
import com.af9.core.machine.PlasmaForgeMachine;
import com.af9.core.machine.console.ConsoleWidget;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import snownee.jade.api.ITooltip;
import snownee.jade.api.ui.BoxStyle;
import snownee.jade.api.ui.IElementHelper;

import java.util.Locale;

/**
 * The Jade tooltip of the Dimensionally Transcendent Plasma Forge, in four finished layouts ({@link #STYLE}): the status
 * line comes first in all of them (the provider writes it), then
 * <ol>
 * <li><b>Gauges</b>: a field bar with its plasma, a check line each for the plasma and the stabilisers, the run bar;</li>
 * <li><b>Checklist</b>: what the forge needs to run, each with a mark, and the next thing to do;</li>
 * <li><b>Compact</b>: three lines (field / plasma / power in one coloured line, the run bar, the forge's numbers);</li>
 * <li><b>Readout</b>: sections (field, forge, modules) with a value on each line.</li>
 * </ol>
 * The server side ({@link #write}) is the same for all of them.
 */
public final class DtpfJade {

    /** The layout in use, 1 to 4. */
    public static final int STYLE = 4;

    private static final int GOOD = 0xFF4ADE80, WARN = 0xFFFBBF24, BAD = 0xFFEF4444;
    private static final String YES = "✔", NO = "✘";

    private DtpfJade() {}

    /** What the client needs, from the machine (server). */
    public static void write(CompoundTag tag, PlasmaForgeMachine forge) {
        tag.putInt("fieldMb", forge.getFieldStrength());
        tag.putInt("fieldState", forge.getFieldState());
        tag.putBoolean("fed", forge.isPlasmaFed());
        tag.putInt("pull", forge.getPlasmaPull());
        tag.putBoolean("aux", forge.isAuxPaid());
        tag.putLong("auxEut", PlasmaForgeMachine.AUX_EUT);
        tag.putInt("eutPct", (int) Math.round(PlasmaForgeMachine.ENERGY_PERCENT * forge.getRamp()));
        tag.putInt("timePct", (int) Math.round(PlasmaForgeMachine.TIME_PERCENT * forge.getRamp()));
        tag.putInt("heat", forge.getFieldHeat());
        int[] mods = new int[DtpfModuleMachine.Kind.values().length];
        for (DtpfModuleMachine.Kind kind : DtpfModuleMachine.Kind.values()) {
            mods[kind.ordinal()] = forge.moduleCount(kind);
        }
        tag.putIntArray("mods", mods);
    }

    /** Below the status line (client). {@code run} is the run-time bar, null while nothing runs. */
    public static void append(ITooltip tooltip, IElementHelper helper, CompoundTag tag, snownee.jade.api.ui.IElement run,
                              int style) {
        int mb = tag.getInt("fieldMb");
        double pct = 100.0 * mb / PlasmaForgeMachine.FIELD_MAX;
        boolean fed = tag.getBoolean("fed");
        boolean aux = tag.getBoolean("aux");
        int state = tag.getInt("fieldState");
        boolean ready = mb >= PlasmaForgeMachine.FIELD_READY;
        switch (style) {
            case 2 -> checklist(tooltip, tag, pct, fed, aux, state, ready, run);
            case 3 -> compact(tooltip, tag, pct, fed, aux, run);
            case 4 -> readout(tooltip, tag, mb, pct, fed, aux, state, run);
            default -> gauges(tooltip, helper, tag, mb, pct, fed, aux, state, run);
        }
    }

    // ------------------------------------------------------------------------------------------------ 1 Gauges
    private static void gauges(ITooltip tooltip, IElementHelper helper, CompoundTag tag, int mb, double pct, boolean fed,
                               boolean aux, int state, snownee.jade.api.ui.IElement run) {
        tooltip.add(helper.progress((float) (mb / (double) PlasmaForgeMachine.FIELD_MAX),
                Component.translatable("af9.jade.dtpf.field", num(pct), stateName(state)),
                helper.progressStyle().color(fieldColor(pct, mb)).textColor(-1), box(), true));
        tooltip.add(check(fed, Component.translatable(fed ? "af9.jade.dtpf.plasma_fed" : "af9.jade.dtpf.plasma_dry",
                tag.getInt("pull"))));
        tooltip.add(check(aux, Component.translatable(aux ? "af9.jade.dtpf.aux_paid" : "af9.jade.dtpf.aux_short",
                ConsoleWidget.compact(tag.getLong("auxEut")))));
        if (run != null) tooltip.add(run);
        tooltip.add(footer(tag));
    }

    // ---------------------------------------------------------------------------------------------- 2 Checklist
    private static void checklist(ITooltip tooltip, CompoundTag tag, double pct, boolean fed, boolean aux, int state,
                                  boolean ready, snownee.jade.api.ui.IElement run) {
        tooltip.add(check(ready, Component.translatable("af9.jade.dtpf.need_field", num(pct),
                (int) (100.0 * PlasmaForgeMachine.FIELD_READY / PlasmaForgeMachine.FIELD_MAX))));
        tooltip.add(check(fed, Component.translatable("af9.jade.dtpf.need_plasma", tag.getInt("pull"))));
        tooltip.add(check(aux, Component.translatable("af9.jade.dtpf.need_aux",
                ConsoleWidget.compact(tag.getLong("auxEut")))));
        if (run != null) {
            tooltip.add(run);
        } else {
            String next = !aux ? "af9.jade.dtpf.next_aux" : !fed && !ready ? "af9.jade.dtpf.next_plasma" :
                    !ready ? "af9.jade.dtpf.next_wait" : "af9.jade.dtpf.next_go";
            tooltip.add(Component.translatable(next).withStyle(ready && aux ? ChatFormatting.GREEN : ChatFormatting.YELLOW));
        }
        tooltip.add(modules(tag));
    }

    // ------------------------------------------------------------------------------------------------ 3 Compact
    private static void compact(ITooltip tooltip, CompoundTag tag, double pct, boolean fed, boolean aux,
                                snownee.jade.api.ui.IElement run) {
        int mb = tag.getInt("fieldMb");
        MutableComponent line = Component.translatable("af9.jade.dtpf.c_field", num(pct))
                .withStyle(style -> style.withColor(fieldColor(pct, mb) & 0xFFFFFF));
        line.append(sep()).append(mark(fed, "af9.jade.dtpf.c_plasma")).append(sep()).append(mark(aux, "af9.jade.dtpf.c_power"));
        tooltip.add(line);
        if (run != null) tooltip.add(run);
        tooltip.add(footer(tag));
    }

    // ------------------------------------------------------------------------------------------------ 4 Readout
    private static void readout(ITooltip tooltip, CompoundTag tag, int mb, double pct, boolean fed, boolean aux, int state,
                                snownee.jade.api.ui.IElement run) {
        tooltip.add(section("af9.jade.dtpf.s_field"));
        tooltip.add(row("af9.jade.dtpf.r_strength", Component.translatable("af9.jade.dtpf.r_strength_v", num(pct),
                ConsoleWidget.compact(mb)).withStyle(style -> style.withColor(fieldColor(pct, mb) & 0xFFFFFF))));
        tooltip.add(row("af9.jade.dtpf.r_plasma", Component.translatable(fed ? "af9.jade.dtpf.r_plasma_fed" :
                "af9.jade.dtpf.r_plasma_dry", tag.getInt("pull")).withStyle(fed ? ChatFormatting.GREEN : ChatFormatting.RED)));
        tooltip.add(row("af9.jade.dtpf.r_power", Component.translatable(aux ? "af9.jade.dtpf.r_power_ok" :
                "af9.jade.dtpf.r_power_short", ConsoleWidget.compact(tag.getLong("auxEut")))
                .withStyle(aux ? ChatFormatting.GREEN : ChatFormatting.RED)));
        if (run != null) tooltip.add(run);
        tooltip.add(section("af9.jade.dtpf.s_forge"));
        tooltip.add(row("af9.jade.dtpf.r_heat", Component.literal(String.format(Locale.ROOT, "%,d K", tag.getInt("heat")))
                .withStyle(ChatFormatting.GOLD)));
        tooltip.add(row("af9.jade.dtpf.r_ramp", Component.translatable("af9.jade.dtpf.r_ramp_v", tag.getInt("eutPct"),
                tag.getInt("timePct")).withStyle(ChatFormatting.AQUA)));
        int[] mods = tag.getIntArray("mods");
        int total = 0;
        for (int count : mods) total += count;
        if (total > 0) {
            tooltip.add(section("af9.jade.dtpf.s_modules"));
            for (DtpfModuleMachine.Kind kind : DtpfModuleMachine.Kind.values()) {
                if (kind.ordinal() < mods.length && mods[kind.ordinal()] > 0) {
                    tooltip.add(Component.literal("  " + mods[kind.ordinal()] + "x ").withStyle(ChatFormatting.WHITE)
                            .append(Component.translatable(moduleKey(kind)).withStyle(ChatFormatting.GRAY)));
                }
            }
        }
    }

    // ---------------------------------------------------------------------------------------------------- bits
    private static MutableComponent check(boolean ok, Component text) {
        return Component.literal((ok ? YES : NO) + " ").withStyle(ok ? ChatFormatting.GREEN : ChatFormatting.RED)
                .append(text.copy().withStyle(ok ? ChatFormatting.GRAY : ChatFormatting.RED));
    }

    private static MutableComponent mark(boolean ok, String key) {
        return Component.literal(ok ? YES : NO).withStyle(ok ? ChatFormatting.GREEN : ChatFormatting.RED)
                .append(Component.literal(" ")).append(Component.translatable(key).withStyle(ChatFormatting.GRAY));
    }

    private static Component sep() {
        return Component.literal("  ").withStyle(ChatFormatting.DARK_GRAY);
    }

    private static MutableComponent section(String key) {
        return Component.literal("▪ ").withStyle(ChatFormatting.DARK_AQUA)
                .append(Component.translatable(key).withStyle(ChatFormatting.AQUA));
    }

    private static MutableComponent row(String key, Component value) {
        return Component.literal("  ").append(Component.translatable(key).withStyle(ChatFormatting.GRAY))
                .append(Component.literal(" ")).append(value);
    }

    /** "EU -0%, time -0% . 1,000 K . P0 H0 S0 F0" in grey. */
    private static MutableComponent footer(CompoundTag tag) {
        return Component.translatable("af9.jade.dtpf.footer", tag.getInt("eutPct"), tag.getInt("timePct"),
                String.format(Locale.ROOT, "%,d", tag.getInt("heat")), modulesText(tag)).withStyle(ChatFormatting.DARK_GRAY);
    }

    private static MutableComponent modules(CompoundTag tag) {
        return Component.translatable("af9.jade.dtpf.modules", modulesText(tag),
                String.format(Locale.ROOT, "%,d", tag.getInt("heat"))).withStyle(ChatFormatting.DARK_GRAY);
    }

    private static String modulesText(CompoundTag tag) {
        int[] mods = tag.getIntArray("mods");
        StringBuilder sb = new StringBuilder();
        String letters = "PHSF";
        for (int i = 0; i < letters.length(); i++) {
            if (i > 0) sb.append(' ');
            sb.append(letters.charAt(i)).append(i < mods.length ? mods[i] : 0);
        }
        return sb.toString();
    }

    private static String moduleKey(DtpfModuleMachine.Kind kind) {
        return switch (kind) {
            case PARALLEL -> "af9.jade.dtpf.m_parallel";
            case SUPERHEATER -> "af9.jade.dtpf.m_superheater";
            case SEQUENCER -> "af9.jade.dtpf.m_sequencer";
            case FORGE -> "af9.jade.dtpf.m_forge";
        };
    }

    private static Component stateName(int state) {
        return Component.translatable("af9.dtpf.console.field." + Math.max(0, Math.min(3, state)));
    }

    private static int fieldColor(double pct, int mb) {
        return mb >= PlasmaForgeMachine.FIELD_READY ? (pct >= 80 ? GOOD : WARN) :
                mb >= PlasmaForgeMachine.FIELD_HOLD ? WARN : BAD;
    }

    private static String num(double value) {
        return String.format(Locale.ROOT, "%.0f", value);
    }

    private static BoxStyle box() {
        BoxStyle style = new BoxStyle();
        style.borderColor = 0xFF555555;
        return style;
    }
}
