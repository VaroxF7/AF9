package com.af9.core.litho;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.recipe.GTRecipeType;

import net.minecraft.ChatFormatting;

import java.util.List;
import java.util.Locale;

/**
 * The lithography modes, one per wafer substrate. Each is its own GT recipe type (gtceu:lithography_&lt;node&gt; on the
 * lithography machines; the 1 nm X-ray FEL mode is gtceu:orbital_lithography), defined in
 * kubejs/startup_scripts/gtceu/photolithography.js; the numbers here must match AF9_WAFERS in
 * kubejs/server_scripts/mods/gtceu/photolithography.js (spec: docs/semiconductor-factory.md).
 * <p>
 * Three machines share the modes ({@link Machine}): the Photolithography Line (Mk1) prints 350-100 nm, the
 * Photolithography Scanner (Mk2) 80 and 65 nm, the Orbital Lithography Station 50 nm down to 1 nm (in orbit only).
 * <p>
 * A mode prints on its own substrate (the 350 nm mode on silicon wafers, 200 nm on phosphorus wafers, ...) and draws
 * 4A of its voltage tier; the orbital mode draws 50A of UHV (laser hatch). A print is always GT's own chip
 * wafer; a higher substrate gives more of them per blank (GT's engraving yields, extended).
 * <p>
 * Finer nodes break more wafers: every print can come out as the substrate's broken wafer, with the mode's base chance
 * plus up to 50 points from a dirty vacuum (see {@link #breakChance}).
 */
public enum LithoMode {

    // id, substrate, node, tier, console colour, text colour, light, wavelength nm, NA, resist, base break chance
    // (1/10000), machine, version of that machine the mode needs (0: none)
    N350("350nm", "silicon", 350, GTValues.MV, 0xFFB978FF, ChatFormatting.LIGHT_PURPLE, "i_line", 365, 0.60,
            "photoresist", 200, Machine.LINE, 1),
    N200("200nm", "phosphorus", 200, GTValues.HV, 0xFF5A8CFF, ChatFormatting.BLUE, "krf", 248, 0.70,
            "krf_photoresist", 300, Machine.LINE, 2),
    N100("100nm", "naquadah", 100, GTValues.EV, 0xFF46D7EB, ChatFormatting.AQUA, "arf", 193, 0.75,
            "arf_photoresist", 500, Machine.LINE, 3),
    N80("80nm", "trinium", 80, GTValues.IV, 0xFF6EEB6E, ChatFormatting.GREEN, "arf", 193, 0.93,
            "arf_photoresist", 700, Machine.SCANNER, 1),
    N65("65nm", "naquadria", 65, GTValues.LuV, 0xFFD4EB46, ChatFormatting.YELLOW, "arf_immersion", 193, 1.20,
            "arf_photoresist", 900, Machine.SCANNER, 2),
    N50("50nm", "neutronium", 50, GTValues.ZPM, 0xFFFFBE3C, ChatFormatting.GOLD, "arf_immersion", 193, 1.35,
            "arf_photoresist", 1200, Machine.ORBITAL, 0),
    N20("20nm", "transmuted_neutronium", 20, GTValues.UV, 0xFFFF7A3C, ChatFormatting.RED, "euv", 13.5, 0.33,
            "euv_photoresist", 1800, Machine.ORBITAL, 0),
    N7("7nm", "strange_matter", 7, GTValues.UHV, 0xFFFF5AA0, ChatFormatting.DARK_PURPLE, "euv_high_na", 13.5, 0.55,
            "euv_photoresist", 2500, Machine.ORBITAL, 0),
    N1("1nm", "chromodynium", 1, GTValues.UHV, 0xFFE6F0FF, ChatFormatting.WHITE, "xfel", 1.0, 0.50,
            "dry_resist", 3500, Machine.ORBITAL, 0);

    /** Which lithography machine runs a mode. */
    public enum Machine {
        /** Photolithography Line (Mk1): 350-100 nm, versions 1-3. */
        LINE,
        /** Photolithography Scanner (Mk2): 80 and 65 nm, versions 1-2. */
        SCANNER,
        /** Orbital Lithography Station: 50 nm down to 1 nm, in orbit only, no versions. */
        ORBITAL
    }

    /** Each machine's modes, in version order. */
    public static final List<LithoMode> LINE_MODES = List.of(N350, N200, N100);
    public static final List<LithoMode> SCANNER_MODES = List.of(N80, N65);
    public static final List<LithoMode> ORBITAL_MODES = List.of(N50, N20, N7, N1);
    /** Per machine version above a mode's own: run time x0.8 and break chance x0.75. */
    public static final double VERSION_SPEEDUP = 0.8;
    public static final double VERSION_BREAK_FACTOR = 0.75;
    /** Break chance a completely dirty vacuum (cleanliness 0) adds on top of the mode's base chance. */
    public static final double DIRT_BREAK = 0.5;
    public static final double MAX_BREAK = 0.95;
    /** Amps every mode draws (two 2A energy hatches); the X-ray FEL mode draws {@link #ORBITAL_AMPERAGE}. */
    public static final int AMPERAGE = 4;
    public static final int ORBITAL_AMPERAGE = 50;

    public final String id;
    /** Substrate id: the blank wafer (gtceu:/kubejs:&lt;substrate&gt;_wafer) and kubejs:broken_&lt;substrate&gt;_wafer. */
    public final String substrate;
    public final int nodeNm;
    /** Voltage tier of the energy (or laser) hatches the mode needs. */
    public final int hatchTier;
    /** ARGB colour of the mode in the consoles. */
    public final int argb;
    public final ChatFormatting color;
    /** Exposure light source, the key of its af9.litho.light.* lang entries. */
    public final String light;
    public final double wavelengthNm;
    /** Numerical aperture of the projection optics (above 1 only with water immersion). */
    public final double numericalAperture;
    /** Resist the mode coats with (a GT material, or the orbital mode's dry resist cartridge). */
    public final String resist;
    /** Break chance at a perfectly clean vacuum, out of 10000 (the chanced broken wafer the recipes show). */
    public final int baseBreak;
    /** The machine that prints this mode. */
    public final Machine machine;
    /** Version of that machine the mode needs (line: 1-3, scanner: 1-2, orbital station: 0). */
    private final int level;

    LithoMode(String id, String substrate, int nodeNm, int hatchTier, int argb, ChatFormatting color, String light,
              double wavelengthNm, double numericalAperture, String resist, int baseBreak, Machine machine,
              int level) {
        this.id = id;
        this.substrate = substrate;
        this.nodeNm = nodeNm;
        this.hatchTier = hatchTier;
        this.argb = argb;
        this.color = color;
        this.light = light;
        this.wavelengthNm = wavelengthNm;
        this.numericalAperture = numericalAperture;
        this.resist = resist;
        this.baseBreak = baseBreak;
        this.machine = machine;
        this.level = level;
    }

    /** The 1 nm X-ray FEL mode: its own recipe type (gtceu:orbital_lithography), 50A of UHV, dry resist. */
    public boolean isXfel() {
        return this == N1;
    }

    /** Printed by the Orbital Lithography Station (in orbit only). */
    public boolean onOrbitalStation() {
        return machine == Machine.ORBITAL;
    }

    public String recipeTypeId() {
        return isXfel() ? "orbital_lithography" : "lithography_" + id;
    }

    /**
     * Weakest coolant the orbital station prints this node with (null: no coolant, the line and the scanner). 50 nm
     * hydrogen, 20 nm argon, 7 nm xenon, 1 nm endion.
     */
    public Coolant minCoolant() {
        return switch (this) {
            case N50 -> Coolant.HYDROGEN;
            case N20 -> Coolant.ARGON;
            case N7 -> Coolant.XENON;
            case N1 -> Coolant.ENDION;
            default -> null;
        };
    }

    /** Coolant above which a colder one gains nothing more: 50 nm xenon, 20, 7 and 1 nm endion. */
    public Coolant bestCoolant() {
        return switch (this) {
            case N50 -> Coolant.XENON;
            case N20, N7, N1 -> Coolant.ENDION;
            default -> null;
        };
    }

    /** Coolant per print (per parallel), in mB. */
    public int coolantPerPrint() {
        return switch (this) {
            case N50 -> 100;
            case N20 -> 150;
            case N7 -> 250;
            case N1 -> 500;
            default -> 0;
        };
    }

    /** 1 nm: every print needs its own research (GT's Research Station, the data orb in a data hatch). */
    public boolean needsResearch() {
        return this == N1;
    }

    /** Computation a print draws from a computation hatch, CWU/t (0: none): 7 nm 32, 1 nm 96. */
    public int computation() {
        return switch (this) {
            case N7 -> 32;
            case N1 -> 96;
            default -> 0;
        };
    }

    /** Version of its machine this mode needs: line 350 nm 1 ... 100 nm 3, scanner 80 nm 1, 65 nm 2; orbital 0. */
    public int level() {
        return level;
    }

    /** Exposed through a water film under the last lens element (the ArF immersion nodes). */
    public boolean immersion() {
        return light.endsWith("immersion");
    }

    public int amperage() {
        return isXfel() ? ORBITAL_AMPERAGE : AMPERAGE;
    }

    /** EU/t the mode's recipes draw: 4A of its tier (480 for 350 nm), 50A of UHV for the orbital mode. */
    public long eut() {
        return (long) GTValues.VA[hatchTier] * amperage();
    }

    /** Rayleigh's k1 = node x NA / wavelength: how hard the optics are pushed (0.25 is the single-exposure limit). */
    public double k1() {
        return nodeNm * numericalAperture / wavelengthNm;
    }

    /** kubejs:broken_&lt;substrate&gt;_wafer, what a failed print turns into. */
    public String brokenWafer() {
        return "broken_" + substrate + "_wafer";
    }

    /**
     * Chance that a print breaks: the base chance plus up to {@link #DIRT_BREAK} for a dirty vacuum, x0.75 per line
     * version above the mode, at most {@link #MAX_BREAK}.
     *
     * @param cleanliness vacuum cleanliness, 0-100
     * @param surplus     machine versions above the mode's own (0 on the orbital station)
     */
    public double breakChance(double cleanliness, int surplus) {
        double clean = Math.max(0, Math.min(100, cleanliness));
        double chance = baseBreak / 10000.0 + (100 - clean) / 100.0 * DIRT_BREAK;
        chance *= Math.pow(VERSION_BREAK_FACTOR, Math.max(0, surplus));
        return Math.max(0, Math.min(MAX_BREAK, chance));
    }

    /** Run-time factor of a line {@code surplus} versions above the mode (0.8 per version). */
    public static double speedFactor(int surplus) {
        return Math.pow(VERSION_SPEEDUP, Math.max(0, surplus));
    }

    /** "x1.56": up to two decimals, no trailing zeros, always with a '.' separator. */
    public static String formatFactor(double factor) {
        String text = String.format(Locale.ROOT, "%.2f", factor);
        text = text.replaceAll("0+$", "").replaceAll("\\.$", "");
        return "x" + text;
    }

    /** "4.2%": one decimal. */
    public static String formatPercent(double fraction) {
        return String.format(Locale.ROOT, "%.1f%%", fraction * 100);
    }

    public static LithoMode fromRecipeTypePath(String path) {
        for (LithoMode mode : values()) {
            if (mode.recipeTypeId().equals(path)) return mode;
        }
        return null;
    }

    public static LithoMode of(GTRecipeType type) {
        return type == null ? null : fromRecipeTypePath(type.registryName.getPath());
    }

    public static LithoMode fromSubstrate(String substrate) {
        for (LithoMode mode : values()) {
            if (mode.substrate.equals(substrate)) return mode;
        }
        return null;
    }
}
