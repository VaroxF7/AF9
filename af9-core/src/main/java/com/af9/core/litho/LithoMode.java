package com.af9.core.litho;

import com.gregtechceu.gtceu.api.GTValues;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Locale;

/**
 * The Photolithography Line's exposure modes. Each is its own GT recipe type (gtceu:lithography_*), defined in
 * kubejs/startup_scripts/gtceu/photolithography.js; the numbers here must match AF9_LITHO in
 * kubejs/server_scripts/mods/gtceu/photolithography.js (spec: docs/semiconductor-factory.md).
 * <p>
 * Every mode draws 4A of its own voltage tier (MV for MUV up to LuV for LUV), prints on its own substrate and each
 * step uses 1.5x the chemicals. Transistor density follows real scaling, (350 nm / node)^2, and dies per wafer grow
 * with sqrt(350 nm / node).
 */
public enum LithoMode {

    MUV("muv", 350, ChatFormatting.LIGHT_PURPLE, 0xFFB978FF, GTValues.MV, "silicon"),
    HUV("huv", 250, ChatFormatting.BLUE, 0xFF5A8CFF, GTValues.HV, "phosphorus"),
    EUV("euv", 200, ChatFormatting.AQUA, 0xFF46D7EB, GTValues.EV, "naquadah"),
    XUV("xuv", 100, ChatFormatting.GREEN, 0xFF6EEB6E, GTValues.IV, "neutronium"),
    LUV("luv", 50, ChatFormatting.GOLD, 0xFFFFBE3C, GTValues.LuV, "neutronium");

    /** NBT compound on wafers and chips: {AF9Litho:{Node:int, Transistors:int}}. */
    public static final String TAG = "AF9Litho";
    public static final String TAG_NODE = "Node";
    public static final String TAG_TRANSISTORS = "Transistors";
    /** Amps every mode draws: two 2A energy hatches. */
    public static final int AMPERAGE = 4;
    private static final int REFERENCE_NODE = 350;

    /** gtceu: item paths the line prints (with {@link #TAG}) and the chips its cutter recipes make from them. */
    public static final List<String> WAFERS = List.of(
            "ilc_wafer", "ram_wafer", "cpu_wafer", "ulpic_wafer", "lpic_wafer", "simple_soc_wafer",
            "nand_memory_wafer", "nor_memory_wafer", "mpic_wafer", "soc_wafer", "advanced_soc_wafer",
            "highly_advanced_soc_wafer", "nano_cpu_wafer", "qbit_cpu_wafer", "hpic_wafer", "uhpic_wafer");
    public static final List<String> CHIPS = List.of(
            "ilc_chip", "ram_chip", "cpu_chip", "ulpic_chip", "lpic_chip", "simple_soc",
            "nand_memory_chip", "nor_memory_chip", "mpic_chip", "soc", "advanced_soc",
            "highly_advanced_soc", "nano_cpu_chip", "qbit_cpu_chip", "hpic_chip", "uhpic_chip");

    public final String id;
    public final int nodeNm;
    public final ChatFormatting color;
    /** ARGB colour used by the controller console and the per-mode item textures. */
    public final int argb;
    /** Voltage tier of the two energy hatches this mode needs. */
    public final int hatchTier;
    /** Substrate material the mode prints on (gtceu:&lt;substrate&gt;_wafer). */
    public final String substrate;

    LithoMode(String id, int nodeNm, ChatFormatting color, int argb, int hatchTier, String substrate) {
        this.id = id;
        this.nodeNm = nodeNm;
        this.color = color;
        this.argb = argb;
        this.hatchTier = hatchTier;
        this.substrate = substrate;
    }

    public String recipeTypeId() {
        return "lithography_" + id;
    }

    /** EU/t the mode's recipes draw: 4A of its tier, 480 for MUV up to 122,880 for LUV. */
    public long eut() {
        return (long) GTValues.VA[hatchTier] * AMPERAGE;
    }

    public double transistorDensity() {
        double ratio = (double) REFERENCE_NODE / nodeNm;
        return ratio * ratio;
    }

    public double dieFactor() {
        return Math.sqrt((double) REFERENCE_NODE / nodeNm);
    }

    /** 1-based index used by the af9:litho_mode item model predicate (0 = not lithographed). */
    public int modelIndex() {
        return ordinal() + 1;
    }

    /** "x49", "x1.96", "x12.25": up to two decimals, no trailing zeros, always with a '.' separator. */
    public static String formatFactor(double factor) {
        String text = String.format(Locale.ROOT, "%.2f", factor);
        text = text.replaceAll("0+$", "").replaceAll("\\.$", "");
        return "x" + text;
    }

    public static LithoMode fromNode(int nodeNm) {
        for (LithoMode mode : values()) {
            if (mode.nodeNm == nodeNm) return mode;
        }
        return null;
    }

    public static LithoMode fromRecipeTypePath(String path) {
        for (LithoMode mode : values()) {
            if (mode.recipeTypeId().equals(path)) return mode;
        }
        return null;
    }

    public static CompoundTag getLithoTag(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.contains(TAG, CompoundTag.TAG_COMPOUND) ? tag.getCompound(TAG) : null;
    }

    public static LithoMode fromStack(ItemStack stack) {
        CompoundTag litho = getLithoTag(stack);
        return litho == null ? null : fromNode(litho.getInt(TAG_NODE));
    }
}
