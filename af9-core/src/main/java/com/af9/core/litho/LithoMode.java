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
 * <p>
 * Each mode exposes with the light source its real node used (mercury i-line, KrF, ArF, ArF immersion) through a lens
 * of the given numerical aperture, and needs the photoresist made for that light.
 * <p>
 * The line itself comes in five versions (longer projection lens, better light source); a version runs the modes up to
 * its own level ({@link #level()}), and runs lower modes faster, with better yield and a finer optical shrink (more
 * transistors per die). The line prints into wafer packages (kubejs:&lt;chip&gt;_wafer_package), GT's wafers stay as
 * they are.
 */
public enum LithoMode {

    MUV("muv", 350, ChatFormatting.LIGHT_PURPLE, 0xFFB978FF, GTValues.MV, "silicon", "i_line", 365, 0.60,
            "photoresist"),
    HUV("huv", 250, ChatFormatting.BLUE, 0xFF5A8CFF, GTValues.HV, "phosphorus", "krf", 248, 0.60, "krf_photoresist"),
    EUV("euv", 200, ChatFormatting.AQUA, 0xFF46D7EB, GTValues.EV, "naquadah", "krf", 248, 0.70, "krf_photoresist"),
    XUV("xuv", 100, ChatFormatting.GREEN, 0xFF6EEB6E, GTValues.IV, "neutronium", "arf", 193, 0.85, "arf_photoresist"),
    LUV("luv", 50, ChatFormatting.GOLD, 0xFFFFBE3C, GTValues.LuV, "neutronium", "arf_immersion", 193, 1.35,
            "arf_photoresist");

    /**
     * NBT compound: packages {AF9Litho:{Node, Version, Transistors (per die), Dies (per wafer)}}, chips
     * {AF9Litho:{Node}}.
     */
    public static final String TAG = "AF9Litho";
    public static final String TAG_NODE = "Node";
    public static final String TAG_VERSION = "Version";
    public static final String TAG_TRANSISTORS = "Transistors";
    public static final String TAG_DIES = "Dies";
    /** Highest line version (one per mode). */
    public static final int MAX_VERSION = 5;
    /** Per version above a mode's own: run time x0.8, bonus-package chance +20 points, +10 % transistors per die. */
    public static final double VERSION_SPEEDUP = 0.8;
    public static final int VERSION_YIELD_BONUS = 2000;
    public static final double VERSION_SHRINK = 0.10;
    /** Amps every mode draws: two 2A energy hatches. */
    public static final int AMPERAGE = 4;
    private static final int REFERENCE_NODE = 350;

    /** kubejs: item paths of the wafer packages the line prints (with {@link #TAG}). */
    public static final List<String> PACKAGES = List.of(
            "ilc_wafer_package", "ram_wafer_package", "cpu_wafer_package", "ulpic_wafer_package",
            "lpic_wafer_package", "simple_soc_wafer_package", "nand_wafer_package", "nor_wafer_package",
            "mpic_wafer_package", "soc_wafer_package", "advanced_soc_wafer_package",
            "highly_advanced_soc_wafer_package", "nano_cpu_wafer_package", "qbit_cpu_wafer_package",
            "hpic_wafer_package", "uhpic_wafer_package");
    /** gtceu: item paths of the chips the cutter makes from the packages (with {Node}). */
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
    /** Exposure light source, the key of its af9.litho.light.* lang entries. */
    public final String light;
    public final int wavelengthNm;
    /** Numerical aperture of the projection lens (above 1 only with water immersion). */
    public final double numericalAperture;
    /** Photoresist material the mode coats with (gtceu:&lt;resist&gt;). */
    public final String resist;

    LithoMode(String id, int nodeNm, ChatFormatting color, int argb, int hatchTier, String substrate, String light,
              int wavelengthNm, double numericalAperture, String resist) {
        this.id = id;
        this.nodeNm = nodeNm;
        this.color = color;
        this.argb = argb;
        this.hatchTier = hatchTier;
        this.substrate = substrate;
        this.light = light;
        this.wavelengthNm = wavelengthNm;
        this.numericalAperture = numericalAperture;
        this.resist = resist;
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

    /** Rayleigh's k1 = node x NA / wavelength: how hard the lens is pushed (0.25 is the single-exposure limit). */
    public double k1() {
        return nodeNm * numericalAperture / wavelengthNm;
    }

    /** 1-based index used by the af9:litho_mode item model predicate (0 = not lithographed). */
    public int modelIndex() {
        return ordinal() + 1;
    }

    /** Line version this mode needs: MUV 1 ... LUV 5. */
    public int level() {
        return ordinal() + 1;
    }

    /** Run-time factor of a line {@code surplus} versions above this mode (0.8 per version). */
    public static double speedFactor(int surplus) {
        return Math.pow(VERSION_SPEEDUP, Math.max(0, surplus));
    }

    /** Transistors-per-die factor of a line {@code surplus} versions above the mode (optical shrink, +10 % each). */
    public static double shrinkFactor(int surplus) {
        return 1 + VERSION_SHRINK * Math.max(0, surplus);
    }

    /** Added chance (of 10000) of the bonus packages for a line {@code surplus} versions above the mode. */
    public static int yieldBonus(int surplus) {
        return VERSION_YIELD_BONUS * Math.max(0, surplus);
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
