package com.af9.core.litho;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * The supercooled fluids of the Supercooling Cryostat as coolant grades of the Orbital Lithography Station, weakest
 * first (in the order they become available: hydrogen, then the air gases argon and xenon, then the End's endion).
 * Each orbital node needs a minimum grade and gains up to its best grade (see {@link LithoMode#minCoolant()}).
 */
@SuppressWarnings("removal") // new ResourceLocation(ns, path) is the only constructor on 1.20.1
public enum Coolant {

    HYDROGEN("hydrogen"),
    ARGON("argon"),
    XENON("xenon"),
    ENDION("endion");

    /** Per grade above the node's minimum (up to its best): break chance and run time multiplied by these. */
    public static final double BREAK_FACTOR = 0.8;
    public static final double TIME_FACTOR = 0.9;

    public final String id;
    /** gtceu:supercooled_&lt;id&gt; */
    public final ResourceLocation fluidId;

    Coolant(String id) {
        this.id = id;
        this.fluidId = new ResourceLocation("gtceu", "supercooled_" + id);
    }

    /** 1 (hydrogen) ... 4 (endion). */
    public int grade() {
        return ordinal() + 1;
    }

    public String langKey() {
        return "material.gtceu.supercooled_" + id;
    }

    public Fluid fluid() {
        Fluid fluid = ForgeRegistries.FLUIDS.getValue(fluidId);
        return fluid == null ? Fluids.EMPTY : fluid;
    }

    /** The coolant of a fluid stack, or null. */
    public static Coolant of(FluidStack stack) {
        if (stack.isEmpty()) return null;
        ResourceLocation id = ForgeRegistries.FLUIDS.getKey(stack.getFluid());
        for (Coolant coolant : values()) {
            if (coolant.fluidId.equals(id)) return coolant;
        }
        return null;
    }

    /** Grades of this coolant that count for a node: above its minimum, up to its best (0 below the minimum too). */
    public int steps(LithoMode mode) {
        Coolant min = mode.minCoolant();
        Coolant best = mode.bestCoolant();
        if (min == null || best == null || grade() < min.grade()) return 0;
        return Math.min(grade(), best.grade()) - min.grade();
    }
}
