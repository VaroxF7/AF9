package com.af9.core.litho;

import com.gregtechceu.gtceu.api.GTValues;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

/**
 * The Photolithography Line's exposure modes. Each is its own GT recipe type (gtceu:lithography_*), defined in
 * kubejs/startup_scripts/gtceu/photolithography.js; the numbers here must match the recipes in
 * kubejs/server_scripts/mods/gtceu/photolithography.js.
 * <p>
 * Every step doubles the energy and uses 1.5x the chemicals. Transistor density follows real scaling, (350 nm / node)^2,
 * and dies per wafer grow with sqrt(350 nm / node).
 */
public enum LithoMode {

    MUV("muv", 350, ChatFormatting.LIGHT_PURPLE, GTValues.MV, 4),
    HUV("huv", 250, ChatFormatting.BLUE, GTValues.HV, 2),
    EUV("euv", 200, ChatFormatting.AQUA, GTValues.HV, 4),
    XUV("xuv", 100, ChatFormatting.GREEN, GTValues.EV, 2),
    LUV("luv", 50, ChatFormatting.GOLD, GTValues.EV, 4);

    /** NBT compound on wafers and chips: {AF9Litho:{Node:int, Transistors:int}}. */
    public static final String TAG = "AF9Litho";
    public static final String TAG_NODE = "Node";
    public static final String TAG_TRANSISTORS = "Transistors";
    private static final int REFERENCE_NODE = 350;

    public final String id;
    public final int nodeNm;
    public final ChatFormatting color;
    /** Voltage tier of the energy hatches this mode needs; the line always has exactly two (2A each). */
    public final int hatchTier;
    public final int amperage;

    LithoMode(String id, int nodeNm, ChatFormatting color, int hatchTier, int amperage) {
        this.id = id;
        this.nodeNm = nodeNm;
        this.color = color;
        this.hatchTier = hatchTier;
        this.amperage = amperage;
    }

    public String recipeTypeId() {
        return "lithography_" + id;
    }

    public String displayName() {
        return name();
    }

    /** EU/t the mode's recipes draw: 480 for MUV, doubling each step. */
    public long eut() {
        return (long) GTValues.VA[hatchTier] * amperage;
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
