package com.af9.core.compute;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * The coolants of the computation arrays (through Coolant Hatches) and the heat one mB of each takes away. Distilled
 * water is the MV array's (the Coolant Hatch takes it only while it is part of an array); the supercooled fluids of the
 * Supercooling Cryostat take far more per mB.
 */
@SuppressWarnings("removal") // new ResourceLocation(ns, path) is the only constructor on 1.20.1
public enum ComputeCoolant {

    DISTILLED_WATER("distilled_water", 8),
    HYDROGEN("supercooled_hydrogen", 64),
    ARGON("supercooled_argon", 96),
    XENON("supercooled_xenon", 160),
    ENDION("supercooled_endion", 256);

    public final ResourceLocation fluid;
    /** Heat one mB takes away. */
    public final int heatPerMb;

    ComputeCoolant(String fluid, int heatPerMb) {
        this.fluid = new ResourceLocation("gtceu", fluid);
        this.heatPerMb = heatPerMb;
    }

    public static ComputeCoolant of(FluidStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        ResourceLocation id = ForgeRegistries.FLUIDS.getKey(stack.getFluid());
        for (ComputeCoolant coolant : values()) {
            if (coolant.fluid.equals(id)) return coolant;
        }
        return null;
    }

    public String langKey() {
        return "af9.compute.coolant." + name().toLowerCase(java.util.Locale.ROOT);
    }
}
